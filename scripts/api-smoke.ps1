param(
    [string]$SearchUrl = "http://localhost:7003",
    [string]$IndexingUrl = "http://localhost:7002",
    [string]$IngestionUrl = "http://localhost:7001",
    [string]$ControlUrl = "http://localhost:7000"
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

$controlHealth = Invoke-WebRequest "$ControlUrl/health" -UseBasicParsing
Assert-Status $controlHealth 200

$filtered = Invoke-WebRequest "$SearchUrl/search?q=adventure&author=Jane%20Austen&language=English&year=1998&offset=0&limit=20" -UseBasicParsing
Assert-Status $filtered 200

$empty = Invoke-WebRequest "$SearchUrl/search?q=term-that-does-not-exist-12345&offset=0&limit=20" -UseBasicParsing
Assert-Status $empty 200
if (-not ($empty.Content -match '"total"\s*:\s*0')) {
    throw "Empty search did not return zero results"
}

$invalidLimit = $null
try {
    Invoke-WebRequest "$SearchUrl/search?q=adventure&limit=0" -UseBasicParsing | Out-Null
} catch {
    $invalidLimit = $_.Exception.Response
}
if ($null -eq $invalidLimit -or $invalidLimit.StatusCode.value__ -ne 400) {
    throw "Invalid limit was not rejected with HTTP 400"
}

$invalidBook = $null
try {
    Invoke-WebRequest "$IngestionUrl/ingest/not-a-number" -Method Post -UseBasicParsing | Out-Null
} catch {
    $invalidBook = $_.Exception.Response
}
if ($null -eq $invalidBook -or $invalidBook.StatusCode.value__ -ne 400) {
    throw "Invalid ingestion book ID was not rejected with HTTP 400"
}

$workflow = Invoke-WebRequest "$ControlUrl/workflow/status/1342" -UseBasicParsing
Assert-Status $workflow 200

Write-Output "Stage 2 API smoke checks passed."
