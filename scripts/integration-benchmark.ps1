param(
    [string]$SearchUrl = "http://localhost:7003",
    [string]$Concurrency = "1,2,4,8",
    [int]$RequestsPerClient = 10
)

$ErrorActionPreference = "Stop"
$rows = @()
$concurrencyValues = @($Concurrency -split '[,\s;]+' |
    Where-Object { $_ -match '^\d+$' } |
    ForEach-Object { [int]$_ } |
    Where-Object { $_ -gt 0 })
if ($concurrencyValues.Count -eq 0) {
    throw "Concurrency must contain one or more positive integers"
}

function Get-Percentile([double[]]$values, [double]$p) {
    if ($values.Count -eq 0) { return 0 }
    $index = [math]::Min($values.Count - 1, [math]::Ceiling($p * $values.Count) - 1)
    return [double]$values[$index]
}

foreach ($clients in $concurrencyValues) {
    $before = @(Get-Process java -ErrorAction SilentlyContinue)
    $cpuBefore = ($before | Measure-Object CPU -Sum).Sum
    $memoryBefore = ($before | Measure-Object WorkingSet64 -Sum).Sum
    $start = Get-Date
    $jobs = @()
    for ($i = 0; $i -lt $clients; $i++) {
        $jobs += Start-Job -ScriptBlock {
            param($url, $count)
            $latencies = @()
            for ($j = 0; $j -lt $count; $j++) {
                $requestStart = Get-Date
                $response = Invoke-WebRequest "$url/search?q=adventure&offset=0&limit=20" -UseBasicParsing
                if ($response.StatusCode -ne 200) {
                    throw "Search request returned HTTP $($response.StatusCode)"
                }
                $latencies += ((Get-Date) - $requestStart).TotalMilliseconds
            }
            $latencies
        } -ArgumentList $SearchUrl, $RequestsPerClient
    }
    $jobs | Wait-Job | Out-Null
    $latencies = @($jobs | Receive-Job)
    $jobs | Remove-Job
    $elapsed = ((Get-Date) - $start).TotalSeconds
    $total = $clients * $RequestsPerClient
    $after = @(Get-Process java -ErrorAction SilentlyContinue)
    $cpuAfter = ($after | Measure-Object CPU -Sum).Sum
    $memoryAfter = ($after | Measure-Object WorkingSet64 -Sum).Sum
    $sorted = @($latencies | Sort-Object)
    $rows += [pscustomobject]@{
        concurrency = $clients
        requests = $total
        elapsed_seconds = [math]::Round($elapsed, 3)
        throughput_requests_per_second = [math]::Round($total / [math]::Max($elapsed, 0.001), 3)
        latency_p50_ms = [math]::Round((Get-Percentile $sorted 0.50), 3)
        latency_p95_ms = [math]::Round((Get-Percentile $sorted 0.95), 3)
        latency_p99_ms = [math]::Round((Get-Percentile $sorted 0.99), 3)
        java_cpu_seconds_delta = [math]::Round((($cpuAfter - $cpuBefore) / 1000), 3)
        java_memory_mb = [math]::Round(($memoryAfter / 1MB), 2)
    }
}

$rows | Format-Table -AutoSize
$rows | Export-Csv "integration-benchmark.csv" -NoTypeInformation -Delimiter ';'
