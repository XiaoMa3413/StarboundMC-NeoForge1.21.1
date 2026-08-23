param(
    [string]$RconHost = "127.0.0.1",
    [int]$Port = 25575,
    [string]$Password = "starboundmc-stage6-smoke"
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

try {
    Send-Packet 1 3 $Password
    if ((Read-Packet).Id -eq -1) { throw "RCON authentication failed." }

    $null = Invoke-Rcon "forceload add 20 0 40 0"
    $null = Invoke-Rcon "setblock 40 100 0 starboundmc:titanium_alloy_furnace"
    $merge = Invoke-Rcon 'data merge block 40 100 0 {Items:[{id:"starboundmc:raw_durasteel",count:1},{id:"minecraft:coal",count:1}]}'
    if ($merge -match "Unable|expected|incorrect") {
        throw "Could not seed the alloy furnace inventory: $merge"
    }
    Start-Sleep -Seconds 12
    $furnace = Invoke-Rcon "data get block 40 100 0 Items"
    if (-not $furnace.Contains("starboundmc:durasteel_ingot")) {
        throw "Alloy furnace did not smelt through its restored ticker/menu inventory: $furnace"
    }
    Write-Output "alloy furnace ticker: $furnace"

    $null = Invoke-Rcon "fill 20 100 0 23 104 0 minecraft:air"
    $null = Invoke-Rcon "fill 20 100 0 23 100 0 minecraft:obsidian"
    $null = Invoke-Rcon "fill 20 104 0 23 104 0 minecraft:obsidian"
    $null = Invoke-Rcon "fill 20 101 0 20 103 0 minecraft:obsidian"
    $null = Invoke-Rcon "fill 23 101 0 23 103 0 minecraft:obsidian"
    $null = Invoke-Rcon "setblock 30 100 0 minecraft:gold_block"
    $null = Invoke-Rcon "setblock 21 101 0 minecraft:fire"
    Start-Sleep -Seconds 2
    $portalProbe = Invoke-Rcon "execute if block 21 101 0 minecraft:nether_portal run setblock 30 100 0 minecraft:diamond_block"
    if (-not [string]::IsNullOrWhiteSpace($portalProbe) -and $portalProbe -notmatch "Test failed|0") {
        throw "Vanilla Nether portal was unexpectedly created: $portalProbe"
    }
    Write-Output "nether portal spawn canceled: $portalProbe"
}
finally {
    try { $null = Invoke-Rcon "fill 20 100 0 30 104 0 minecraft:air" } catch { }
    try { $null = Invoke-Rcon "setblock 40 100 0 minecraft:air" } catch { }
    try { $null = Invoke-Rcon "forceload remove 20 0 40 0" } catch { }
    try { $null = Invoke-Rcon "kill @e[type=minecraft:item,x=20,y=98,z=-2,dx=22,dy=8,dz=4]" } catch { }
    try { $null = Invoke-Rcon "stop" } catch { }
    $stream.Dispose()
    $client.Dispose()
}
