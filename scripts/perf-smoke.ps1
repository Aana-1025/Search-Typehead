$baseUrl = "http://localhost:8082"

function Invoke-TimedJson {
    param(
        [string]$Label,
        [scriptblock]$Action
    )

    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $result = & $Action
    $stopwatch.Stop()

    Write-Host "$Label took $($stopwatch.ElapsedMilliseconds) ms"
    if ($null -ne $result) {
        $result | ConvertTo-Json -Depth 6
    }
}

Write-Host "Starting lightweight performance smoke run against $baseUrl"

1..2 | ForEach-Object {
    Invoke-TimedJson "suggest iph #$_" {
        Invoke-RestMethod -Uri "$baseUrl/suggest?q=iph"
    } | Out-Null
}

1..2 | ForEach-Object {
    Invoke-TimedJson "suggest spring boot #$_" {
        Invoke-RestMethod -Uri "$baseUrl/suggest?q=spring%20boot"
    } | Out-Null
}

1..5 | ForEach-Object {
    Invoke-TimedJson "search metrics test iphone #$_" {
        Invoke-RestMethod `
            -Method POST `
            -Uri "$baseUrl/search" `
            -ContentType "application/json" `
            -Body '{"query":"metrics test iphone"}'
    } | Out-Null
}

Invoke-TimedJson "manual batch flush" {
    Invoke-RestMethod -Method POST -Uri "$baseUrl/batch/flush"
}

Invoke-TimedJson "trending 24h" {
    Invoke-RestMethod -Uri "$baseUrl/trending?window=24h&limit=10"
}

Invoke-TimedJson "metrics summary" {
    Invoke-RestMethod -Uri "$baseUrl/metrics/summary"
}
