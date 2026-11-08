param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$webResponse = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$BaseUrl/avaliacao/executar"
$webResponse.RawContentStream.Position = 0
$reader = New-Object System.IO.StreamReader(
    $webResponse.RawContentStream,
    [System.Text.Encoding]::UTF8
)
$responseJson = $reader.ReadToEnd()
$reader.Dispose()
$report = $responseJson | ConvertFrom-Json
$json = $report | ConvertTo-Json -Depth 10
[System.IO.File]::WriteAllText(
    (Join-Path $PWD "evaluation-report.json"),
    $json,
    [System.Text.UTF8Encoding]::new($true)
)

$lines = @(
    "# Relatorio de avaliacao",
    "",
    "Executado em: $($report.executedAt)",
    "",
    "Resultado: $($report.approved)/$($report.total) cenarios aprovados",
    ""
)
foreach ($scenario in $report.scenarios) {
    $status = if ($scenario.approved) { "PASSOU" } else { "FALHOU" }
    $lines += "## $($scenario.id) - $($scenario.name): $status"
    $lines += ""
    foreach ($check in $scenario.checks) {
        $checkStatus = if ($check.passed) { "x" } else { " " }
        $lines += "- [$checkStatus] $($check.description) - $($check.evidence)"
    }
    $lines += ""
}
[System.IO.File]::WriteAllLines(
    (Join-Path $PWD "evaluation-report.md"),
    $lines,
    [System.Text.UTF8Encoding]::new($true)
)

Write-Host "Relatorios gerados: evaluation-report.json e evaluation-report.md"
Write-Host "Aprovados: $($report.approved)/$($report.total)"
