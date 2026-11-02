param([switch]$RegisterCodex)

$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$bridgeDirectory = Join-Path $repo 'tools/minecraft-mcp'
$target = Get-Content (Join-Path $bridgeDirectory 'target.json') -Raw | ConvertFrom-Json

& npm.cmd ci --prefix $bridgeDirectory --ignore-scripts --no-audit --no-fund
if ($LASTEXITCODE -ne 0) { throw 'Installing Minecraft MCP bridge dependencies failed.' }

Push-Location $repo
try {
    & ./gradlew.bat prepareMinecraftMcp -PwithoutMinecraftMcp
    if ($LASTEXITCODE -ne 0) { throw 'Preparing the Minecraft MCP mod failed.' }
}
finally {
    Pop-Location
}

if ($RegisterCodex) {
    $serverName = 'minecraft_' + $target.version.Replace('.', '_')
    $node = (Get-Command node.exe).Source
    & codex mcp add $serverName -- $node (Join-Path $bridgeDirectory 'bridge.mjs')
    if ($LASTEXITCODE -ne 0) { throw 'Registering the Codex MCP server failed.' }
}

Write-Host "Ready: Minecraft $($target.version) / $($target.loader), port $($target.port)."
Write-Host 'Refresh the Gradle project or run ./gradlew.bat genIntellijRuns to update IDE launches.'
Write-Host 'Launch: ./gradlew.bat runClientMcp with MCP, or ./gradlew.bat runClient without it. Both use the normal run directory.'
