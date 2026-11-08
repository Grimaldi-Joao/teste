# Motor de IA de Atendimento

Backend multi-tenant em Java/Spring Boot que simula atendimento de leads pelo WhatsApp. O serviço recebe mensagens por HTTP, agrupa rajadas, mantém memória persistente do lead, aplica as regras de qualificação de energia solar, executa ações estruturadas e permite intervenção humana.

## Como executar

Requisito: Docker com Docker Compose.

```bash
docker compose up --build
```

A API ficará disponível em `http://localhost:8080`. Esse único comando usa o adapter local `template`, não exige chave nem faz chamadas pagas. O banco H2 é persistido no volume Docker `motor-data`.

Para usar um provedor externo, copie `.env.example` para `.env`, preencha apenas o `.env` e recrie o container. O `.env` está no `.gitignore` e não deve ser versionado.

Também é possível executar com Java 21 e Maven:

```bash
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/actuator/health
```

## Exemplo rápido

Envie uma mensagem:

```bash
curl -X POST http://localhost:8080/webhook/mensagem \
  -H "Content-Type: application/json" \
  -d '{
    "message_id": "wamid.123",
    "tenant_id": "sol-nascente",
    "telefone": "5581999990000",
    "texto": "oi, quero saber de energia solar",
    "timestamp": "2026-10-27T14:03:11-03:00"
  }'
```

Por padrão, a conversa é processada depois da janela de rajada de 8 segundos. Consulte o resultado:

```bash
curl http://localhost:8080/conversas/sol-nascente/5581999990000
```

O tempo pode ser reduzido localmente com `BURST_WINDOW=1s`.

## Endpoints

| Método | Endpoint | Finalidade |
|---|---|---|
| `POST` | `/webhook/mensagem` | Recebe mensagens do lead ou humano |
| `GET` | `/conversas/{tenantId}/{telefone}` | Retorna histórico, ficha, ações, agenda e métricas dos turnos |
| `POST` | `/intervencao` | Humano assume ou religa a IA |
| `GET` | `/metricas/{tenantId}` | Métricas agregadas do tenant |
| `POST` | `/avaliacao/executar` | Executa os cinco cenários do Anexo C |
| `GET` | `/actuator/health` | Estado da aplicação |

O campo `autor` do webhook aceita `lead` ou `humano`; quando omitido, assume `lead`. O campo opcional `simulacao: true` impede o envio pelo adapter de notificação, mas mantém o registro do que seria enviado.

Intervenção humana:

```bash
curl -X POST http://localhost:8080/intervencao \
  -H "Content-Type: application/json" \
  -d '{
    "tenant_id": "sol-nascente",
    "telefone": "5581999990000",
    "acao": "assumir"
  }'
```

Troque `assumir` por `religar` para devolver a conversa à IA. Mensagens humanas recebidas durante a intervenção ficam no mesmo histórico e são incluídas no contexto quando a IA volta.

## Arquitetura

```text
Webhook / Intervenção / Consultas
              │
              ▼
      Serviços de aplicação
              │
       ┌──────┴────────┐
       │ Agregador DB  │  janela configurável e message_id único
       └──────┬────────┘
              ▼
      Motor da conversa
       ├─ extrator conservador de fatos
       ├─ motor determinístico de qualificação
       ├─ executor/auditoria de ações
       ├─ agenda e notificação por adapters
       └─ LlmClient
           ├─ template local
           └─ OpenAI-compatible
              │
              ▼
       H2/PostgreSQL + métricas
```

Principais componentes:

- `MessageIngestionService`: valida o tenant, garante idempotência e agenda o processamento.
- `BurstScheduler`: busca conversas cuja janela expirou.
- `ConversationProcessor`: coordena extração, qualificação, ações, resposta e observabilidade.
- `LeadFactExtractor`: só produz candidatos com evidência nas mensagens do turno.
- `QualificationEngine`: contém as regras de negócio do Anexo B, fora do prompt.
- `LlmClient`: porta que permite mudar de provedor sem alterar o motor.
- `ResponseGuard`: rejeita respostas que prometam desconto, preço ou economia proibida.

## Decisões técnicas

### Rajada e idempotência

`message_id` possui restrição única no banco. Cada nova mensagem atualiza `process_after` para `agora + BURST_WINDOW`; portanto, mensagens próximas adiam o mesmo processamento. O worker adquire lock pessimista na conversa, lê todas as mensagens pendentes e produz uma única resposta.

Esse desenho é simples, persistente e suficiente para uma instância. O lock também evita que dois workers processem simultaneamente a mesma conversa.

### Memória sem invenção

A ficha é uma entidade estruturada, e não apenas texto dentro do prompt. O extrator só atualiza campos identificados nas mensagens atuais. Padrões ambíguos são deliberadamente ignorados ou enviados para humano. Cada alteração gera uma ação `atualizar_ficha` com campo e valor auditáveis.

O LLM não decide sozinho a qualificação. A decisão final é feita por Java com dados persistidos:

- casa própria, conta acima de R$ 250 e cidade atendida: qualificado;
- aluguel ou apartamento: desqualificado, oferecendo assinatura;
- conta até R$ 250 ou cidade não atendida: desqualificado;
- inventário, titularidade ambígua ou conta desconhecida: atendimento humano.

### Ações estruturadas

As ações são tipos fechados (`ActionType`) e possuem handlers no motor. `consultar_agenda` é executada antes de a resposta mencionar horários. Para agendar, o motor registra `consultar_agenda`, `marcar_agenda`, tag, estágio e notificação. Todas as ações guardam turno, payload, modo de simulação e horário.

### Multi-tenant

As configurações ficam em `src/main/resources/tenants`. Foram incluídos:

- `sol-nascente`: tom informal e pode dizer que é IA;
- `energia-serena`: tom formal e não pode dizer que é IA.

Toda conversa é identificada por `tenant_id + telefone`. Um tenant não configurado é rejeitado.

### Banco

H2 em arquivo foi escolhido para que a avaliação rode com um único comando e sem outro container. O profile `postgres` e o driver já estão disponíveis para migração:

```bash
SPRING_PROFILES_ACTIVE=postgres \
DB_URL=jdbc:postgresql://localhost:5432/motor_ia \
DB_USERNAME=motor \
DB_PASSWORD=motor \
mvn spring-boot:run
```

## Provedor de LLM

O projeto funciona sem chave usando `LLM_PROVIDER=template`. Esse modo mantém o fluxo totalmente determinístico para demonstração.

Para Google AI Studio (Gemini), crie um `.env` a partir do `.env.example`:

```dotenv
LLM_PROVIDER=openai-compatible
LLM_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
LLM_API_KEY=sua-chave-do-google-ai-studio
LLM_MODEL=gemini-3.8-flash
LLM_INPUT_COST_PER_MILLION=0
LLM_OUTPUT_COST_PER_MILLION=0
```

Depois recrie o serviço com `docker compose up --build --force-recreate`. Para Groq, OpenRouter, Ollama ou outro endpoint compatível com a API de chat da OpenAI, use o mesmo adapter e troque URL, chave e modelo:

```bash
LLM_PROVIDER=openai-compatible
LLM_BASE_URL=https://seu-provedor/v1
LLM_API_KEY=sua-chave
LLM_MODEL=nome-do-modelo
docker compose up --build
```

Para calcular custo, configure também:

```bash
LLM_INPUT_COST_PER_MILLION=0
LLM_OUTPUT_COST_PER_MILLION=0
```

Se o provedor lançar um erro, o motor usa a resposta segura local e registra o modelo como `safe-fallback`; a conversa não fica sem resposta. O valor `template-v1` indica execução local, enquanto `gemini-3.8-flash` confirma que o Gemini respondeu.

## Testes

```bash
mvn test
```

O `LlmClient` é substituído por um mock nos testes. A suíte cobre:

- idempotência;
- rajada com uma única resposta;
- intervenção e religamento com histórico humano;
- ausência de campos inventados em prompt injection;
- os cinco cenários completos do Anexo C.

## Avaliação do Anexo C

Com a aplicação rodando:

```powershell
./scripts/evaluate.ps1
```

ou:

```bash
./scripts/evaluate.sh
```

O endpoint usa o `LlmClient` configurado naquele momento. Assim, com `openai-compatible`, a avaliação passa pelo provedor real; com `template`, ela é gratuita e determinística.

Resultado reproduzido em 8 de novembro de 2026 pelo script, com banco em memória e adapter `template`:

| Cenário | Resultado |
|---|---|
| C1 — caminho feliz | Passou: ficha completa, `qualificado`, `Agendado`, visita e notificação |
| C2 — imóvel alugado | Passou: `desqualificado`, alternativa por assinatura e sem notificação |
| C3 — rajada | Passou: uma resposta e nenhum dado já informado foi perguntado novamente |
| C4 — cliente antigo | Passou: `cliente-antigo` e transferência humana |
| C5 — manipulação | Passou: sem desconto/promessa e sem dados inventados |

Total: **5/5 cenários aprovados**.

Além da avaliação determinística, o caminho feliz foi executado manualmente pelo Postman com o Google AI Studio. O Gemini extraiu a ficha completa, qualificou o lead, ofereceu a agenda e confirmou `quinta às 14h`; o estado final foi `Agendado`, com `marcar_agenda` e `notificar_equipe` auditados. Os três turnos registraram o modelo `gemini-3.8-flash`.

O roteiro detalhado, com mensagens, fluxo interno e verificações de cada cenário, está em [`docs/anexo-c.md`](docs/anexo-c.md).

## Observabilidade e custo

Cada turno registra:

- modelo;
- tokens de entrada e saída;
- custo estimado;
- latência;
- quantidade e detalhes das ações.

`GET /metricas/{tenantId}` retorna conversas, custo total e médio, tokens, latência média, percentual qualificado e percentual transferido.

O custo é calculado por:

```text
(tokens_entrada × preço_entrada + tokens_saída × preço_saída) / 1.000.000
```

No adapter local e no free tier configurado com preços zero, o custo estimado é zero. Como referência concreta, o caminho feliz testado com Gemini consumiu 905 tokens de entrada e 121 de saída em três turnos. Aplicando preços hipotéticos de US$ 0,75/M de entrada e US$ 3,75/M de saída, a conversa custaria aproximadamente **US$ 0,00113**; os preços devem ser conferidos no provedor antes de configurar as variáveis.

Para reduzir custo em produção eu resumiria históricos longos, enviaria apenas os campos relevantes, usaria um modelo menor para extração/classificação, aproveitaria cache de contexto e cachearia respostas institucionais.

## Limitações conhecidas

- O extrator local cobre o domínio e os cenários fornecidos, mas não substitui NER completo para todas as formas de escrita.
- A notificação não chama WhatsApp real; o adapter apenas registra/loga o envio. Em simulação nem esse adapter é chamado.
- A agenda é um mock com três horários configuráveis.
- A chamada ao LLM ainda ocorre dentro da transação que processa a conversa e não possui timeout explícito. No teste real, os turnos do Gemini levaram entre 49 s e 172 s; como há um único scheduler, uma chamada lenta atrasa as demais conversas.
- O `ResponseGuard` combina instruções de sistema, regras determinísticas e bloqueio de expressões proibidas, mas não substitui uma política completa de segurança contra todas as variações de prompt injection.
- A checagem de idempotência também é protegida por restrição única no banco, mas duas entregas perfeitamente simultâneas podem fazer uma delas receber erro de conflito em vez da resposta amigável `duplicada`.
- O schema é atualizado pelo Hibernate para reduzir setup. Produção deve usar Flyway com migrations versionadas.
- O endpoint de avaliação persiste suas conversas; elas aparecem nas métricas.
- A autenticação e autorização não fazem parte do teste e não foram implementadas.
- A fila usa polling no banco. É adequada para a entrega, não para grande volume.

## Próximos passos para 100 empresas

1. Migrar para PostgreSQL com Flyway e política de isolamento por tenant.
2. Substituir o scheduler local por Kafka, SQS ou RabbitMQ com chave de partição por conversa.
3. Usar outbox transacional para notificações e eventos do CRM.
4. Adicionar autenticação, rate limiting, criptografia e política de retenção/LGPD.
5. Armazenar segredos em cofre e separar limites/custos por tenant.
6. Adicionar tracing distribuído, Prometheus/Grafana e alertas de qualidade/custo.
7. Criar versionamento de prompts e regras, avaliação contínua e revisão humana de amostras.
8. Retirar a chamada externa da transação, configurar timeouts, retries com backoff, circuit breaker e dead-letter queue.
