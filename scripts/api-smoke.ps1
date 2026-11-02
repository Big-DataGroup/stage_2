param(
    [string]$SearchUrl = "http://localhost:7003",
    [string]$IndexingUrl = "http://localhost:7002",
    [string]$IngestionUrl = "http://localhost:7001"
)

$ErrorActionPreference = "Stop"

function Assert-Status($response, $expected) {
    if ($response.StatusCode -ne $expected) {
        throw "Expected HTTP $expected but received $($response.StatusCode)"
    }
}

$health = Invoke-WebRequest "$SearchUrl/health" -UseBasicParsing
Assert-Status $health 200

$invalid = $null
try {
    Invoke-WebRequest "$SearchUrl/search?q=" -UseBasicParsing | Out-Null
} catch {
    $invalid = $_.Exception.Response
}
if ($null -eq $invalid -or $invalid.StatusCode.value__ -ne 400) {
    throw "Invalid search parameters were not rejected with HTTP 400"
}

$status = Invoke-WebRequest "$IndexingUrl/index/status" -UseBasicParsing
Assert-Status $status 200

$ingestionHealth = Invoke-WebRequest "$IngestionUrl/health" -UseBasicParsing
Assert-Status $ingestionHealth 200

Write-Output "Stage 2 API smoke checks passed."
