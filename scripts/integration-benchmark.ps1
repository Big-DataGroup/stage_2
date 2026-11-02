param(
    [string]$SearchUrl = "http://localhost:7003",
    [int[]]$Concurrency = @(1, 2, 4, 8),
    [int]$RequestsPerClient = 10
)

$ErrorActionPreference = "Stop"
$rows = @()

foreach ($clients in $Concurrency) {
    $start = Get-Date
    $jobs = @()
    for ($i = 0; $i -lt $clients; $i++) {
        $jobs += Start-Job -ScriptBlock {
            param($url, $count)
            for ($j = 0; $j -lt $count; $j++) {
                Invoke-WebRequest "$url/search?q=adventure&offset=0&limit=20" -UseBasicParsing | Out-Null
            }
        } -ArgumentList $SearchUrl, $RequestsPerClient
    }
    $jobs | Wait-Job | Out-Null
    $jobs | Receive-Job | Out-Null
    $jobs | Remove-Job
    $elapsed = ((Get-Date) - $start).TotalSeconds
    $total = $clients * $RequestsPerClient
    $rows += [pscustomobject]@{
        concurrency = $clients
        requests = $total
        elapsed_seconds = [math]::Round($elapsed, 3)
        throughput_requests_per_second = [math]::Round($total / [math]::Max($elapsed, 0.001), 3)
    }
}

$rows | Format-Table -AutoSize
$rows | Export-Csv "integration-benchmark.csv" -NoTypeInformation
