param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$report = Invoke-RestMethod -Method Post -Uri "$BaseUrl/avaliacao/executar"
$json = $report | ConvertTo-Json -Depth 10
[System.IO.File]::WriteAllText(
    (Join-Path $PWD "evaluation-report.json"),
    $json,
    [System.Text.UTF8Encoding]::new($false)
)

$lines = @(
    "# Relatório de avaliação",
    "",
    "Executado em: $($report.executedAt)",
    "",
    "Resultado: $($report.approved)/$($report.total) cenários aprovados",
    ""
)
foreach ($scenario in $report.scenarios) {
    $status = if ($scenario.approved) { "PASSOU" } else { "FALHOU" }
    $lines += "## $($scenario.id) — $($scenario.name): $status"
    $lines += ""
    foreach ($check in $scenario.checks) {
        $checkStatus = if ($check.passed) { "x" } else { " " }
        $lines += "- [$checkStatus] $($check.description) — $($check.evidence)"
    }
    $lines += ""
}
[System.IO.File]::WriteAllLines(
    (Join-Path $PWD "evaluation-report.md"),
    $lines,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Relatórios gerados: evaluation-report.json e evaluation-report.md"
Write-Host "Aprovados: $($report.approved)/$($report.total)"
