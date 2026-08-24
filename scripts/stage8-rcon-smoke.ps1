param(
    [string]$RconHost = "127.0.0.1",
    [int]$Port = 25575,
    [string]$Password = "starboundmc-stage8-smoke",
    [switch]$AllowMissingConsole
)

$ErrorActionPreference = "Stop"
$client = [System.Net.Sockets.TcpClient]::new()
$client.Connect($RconHost, $Port)
$stream = $client.GetStream()

function Read-Exact([int]$count) {
    $buffer = [byte[]]::new($count)
    $offset = 0
    while ($offset -lt $count) {
        $read = $stream.Read($buffer, $offset, $count - $offset)
        if ($read -eq 0) { throw "RCON connection closed while reading a packet." }
        $offset += $read
    }
    return $buffer
}

function Send-Packet([int]$requestId, [int]$type, [string]$body) {
    $bodyBytes = [System.Text.Encoding]::UTF8.GetBytes($body)
    $payloadLength = 10 + $bodyBytes.Length
    $packet = [byte[]]::new(4 + $payloadLength)
    [Array]::Copy([BitConverter]::GetBytes($payloadLength), 0, $packet, 0, 4)
    [Array]::Copy([BitConverter]::GetBytes($requestId), 0, $packet, 4, 4)
    [Array]::Copy([BitConverter]::GetBytes($type), 0, $packet, 8, 4)
    [Array]::Copy($bodyBytes, 0, $packet, 12, $bodyBytes.Length)
    $stream.Write($packet, 0, $packet.Length)
}

function Read-Packet {
    $length = [BitConverter]::ToInt32((Read-Exact 4), 0)
    $payload = Read-Exact $length
    [PSCustomObject]@{
        Id = [BitConverter]::ToInt32($payload, 0)
        Body = [System.Text.Encoding]::UTF8.GetString($payload, 8, $length - 10)
    }
}

function Invoke-Rcon([string]$command) {
    Send-Packet 2 2 $command
    return (Read-Packet).Body
}

function Assert-CommandSucceeded([string]$label, [string]$response) {
    if ([string]::IsNullOrWhiteSpace($response) -or $response -match "Unknown|Incorrect|could not|failed") {
        throw "$label failed: $response"
    }
    Write-Output "$label`: $response"
}

try {
    Send-Packet 1 3 $Password
    if ((Read-Packet).Id -eq -1) { throw "RCON authentication failed." }

    foreach ($dimension in @("ship", "frozen", "barren", "molten")) {
        $response = Invoke-Rcon "execute in starboundmc:$dimension run forceload add 0 0"
        Assert-CommandSucceeded "$dimension dimension" $response
    }
    Start-Sleep -Seconds 5

    $teleporter = Invoke-Rcon "execute in starboundmc:ship if block 0 101 0 starboundmc:teleporter run setblock 15 120 15 minecraft:gold_block"
    if ($teleporter -notmatch "Changed the block|Changed block") {
        throw "Procedural ship teleporter was not generated: $teleporter"
    }
    Write-Output "procedural ship teleporter: $teleporter"

    $console = Invoke-Rcon "execute in starboundmc:ship if block 0 102 7 starboundmc:ship_console run setblock 14 120 15 minecraft:diamond_block"
    if ($console -notmatch "Changed the block|Changed block") {
        if (-not $AllowMissingConsole) {
            throw "Procedural ship console was not generated: $console"
        }
        Write-Output "procedural ship console: not present (allowed for a player-modified legacy ship)"
    }
    else {
        Write-Output "procedural ship console: $console"
    }

    $moltenSky = Invoke-Rcon "execute in starboundmc:molten if block 0 127 0 minecraft:air run setblock 15 127 15 minecraft:gold_block"
    if ($moltenSky -notmatch "Changed the block|Changed block") {
        throw "Molten terrain unexpectedly has a Nether-style ceiling: $moltenSky"
    }
    Write-Output "molten open sky: $moltenSky"
}
finally {
    try { $null = Invoke-Rcon "execute in starboundmc:ship run setblock 15 120 15 minecraft:air" } catch { }
    try { $null = Invoke-Rcon "execute in starboundmc:ship run setblock 14 120 15 minecraft:air" } catch { }
    try { $null = Invoke-Rcon "execute in starboundmc:molten run setblock 15 127 15 minecraft:air" } catch { }
    try {
        foreach ($dimension in @("ship", "frozen", "barren", "molten")) {
            $null = Invoke-Rcon "execute in starboundmc:$dimension run forceload remove 0 0"
        }
    } catch { }
    try { $null = Invoke-Rcon "stop" } catch { }
    $stream.Dispose()
    $client.Dispose()
}
