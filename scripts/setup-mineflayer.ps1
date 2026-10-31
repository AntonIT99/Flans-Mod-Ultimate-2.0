param([switch]$RegisterCodex)

$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$directory = Join-Path $repo 'tools/mineflayer'
$target = Get-Content (Join-Path $directory 'target.json') -Raw | ConvertFrom-Json
& npm.cmd ci --prefix $directory --ignore-scripts --no-audit --no-fund
if ($LASTEXITCODE -ne 0) { throw 'Installing Mineflayer dependencies failed.' }
& node (Join-Path $directory 'prepare.mjs')
if ($LASTEXITCODE -ne 0) { throw 'Preparing the Mineflayer test server failed.' }
if ($RegisterCodex) {
    $serverName = 'mineflayer_' + $target.version.Replace('.', '_')
    $node = (Get-Command node.exe).Source
    & codex mcp add $serverName -- $node (Join-Path $directory 'bridge.mjs')
    if ($LASTEXITCODE -ne 0) { throw 'Registering the Mineflayer MCP server failed.' }
}
Write-Host "Ready: Mineflayer for Minecraft $($target.version) / $($target.loader), port $($target.port)."
Write-Host 'Launch the isolated server: ./gradlew.bat runServer -PwithMineflayer'
