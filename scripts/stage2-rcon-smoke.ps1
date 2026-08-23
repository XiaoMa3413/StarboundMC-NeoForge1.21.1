param(
    [string]$RconHost = "127.0.0.1",
    [int]$Port = 25575,
    [string]$Password = "starboundmc-stage2-smoke"
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
        if ($read -eq 0) {
            throw "RCON connection closed while reading a packet."
        }
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
    $lengthBytes = Read-Exact 4
    $length = [BitConverter]::ToInt32($lengthBytes, 0)
    $payload = Read-Exact $length
    $bodyLength = $length - 10
    [PSCustomObject]@{
        Id = [BitConverter]::ToInt32($payload, 0)
        Type = [BitConverter]::ToInt32($payload, 4)
        Body = [System.Text.Encoding]::UTF8.GetString($payload, 8, $bodyLength)
    }
}

function Invoke-Rcon([string]$command) {
    Send-Packet 2 2 $command
    return (Read-Packet).Body
}

$blockIds = @(
    "matter_manipulator_workbench", "teleporter", "ship_console", "ship_engine",
    "captain_chair", "fuel_controller", "ship_crate", "ship_door", "tungsten_ore",
    "titanium_ore", "durasteel_ore", "star_core_ore", "titanium_alloy_furnace"
)

$itemIds = @(
    "matter_manipulator", "matter_manipulator_module", "matter_manipulator_workbench",
    "teleporter", "ship_console", "captain_chair", "fuel_controller", "ship_crate",
    "ship_door", "ship_engine", "tungsten_ore", "titanium_ore", "durasteel_ore",
    "star_core_ore", "titanium_alloy_furnace", "raw_tungsten", "raw_titanium",
    "raw_durasteel", "raw_star_core", "tungsten_ingot", "titanium_ingot",
    "durasteel_ingot", "star_core_fragment"
)

try {
    Send-Packet 1 3 $Password
    $auth = Read-Packet
    if ($auth.Id -eq -1) {
        throw "RCON authentication failed."
    }

    for ($index = 0; $index -lt $blockIds.Count; $index++) {
        $response = Invoke-Rcon "setblock $index 100 0 starboundmc:$($blockIds[$index])"
        if ($response -match "Unknown|incorrect|Could not parse|expected") {
            throw "Failed to place $($blockIds[$index]): $response"
        }
        Write-Output "block $($blockIds[$index]): $response"
    }

    $blockEntities = @{
        5 = "starboundmc:fuel_controller"
        6 = "starboundmc:ship_crate"
        7 = "starboundmc:ship_door"
        12 = "starboundmc:titanium_alloy_furnace"
    }
    foreach ($entry in $blockEntities.GetEnumerator()) {
        $response = Invoke-Rcon "data get block $($entry.Key) 100 0 id"
        if (-not $response.Contains($entry.Value)) {
            throw "Block entity $($entry.Value) was not created: $response"
        }
        Write-Output "block entity $($entry.Value): $response"
    }

    foreach ($itemId in $itemIds) {
        $response = Invoke-Rcon "item replace block 6 100 0 container.0 with starboundmc:$itemId 1"
        if ($response -match "Unknown|incorrect|Could not parse|expected|not a container") {
            throw "Failed to resolve item ${itemId}: $response"
        }
        Write-Output "item starboundmc:${itemId}: $response"
    }

    $seat = Invoke-Rcon "summon starboundmc:seat 4 100 0"
    if ($seat -match "Unknown|incorrect|Could not parse|expected") {
        throw "Failed to summon seat: $seat"
    }
    Write-Output "entity starboundmc:seat: $seat"
}
finally {
    for ($index = 0; $index -lt $blockIds.Count; $index++) {
        try { $null = Invoke-Rcon "setblock $index 100 0 air" } catch { }
    }
    try { $null = Invoke-Rcon "kill @e[type=minecraft:item,x=0,y=98,z=-2,dx=12,dy=4,dz=4]" } catch { }
    try { $null = Invoke-Rcon "stop" } catch { }
    $stream.Dispose()
    $client.Dispose()
}
