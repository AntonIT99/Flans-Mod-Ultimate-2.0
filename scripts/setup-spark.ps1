param([switch]$RegisterCodex)

$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$toolDirectory = Join-Path $repo 'tools/spark'
$target = Get-Content (Join-Path $toolDirectory 'target.json') -Raw | ConvertFrom-Json

& npm.cmd ci --prefix $toolDirectory --ignore-scripts --no-audit --no-fund
if ($LASTEXITCODE -ne 0) { throw 'Installing spark analysis dependencies failed.' }

Push-Location $repo
try {
    & ./gradlew.bat prepareSpark
    if ($LASTEXITCODE -ne 0) { throw 'Preparing the spark mod failed.' }
    & node (Join-Path $toolDirectory 'smoke.mjs')
    if ($LASTEXITCODE -ne 0) { throw 'The spark analysis smoke check failed.' }
}
finally {
    Pop-Location
}

if ($RegisterCodex) {
    $serverName = 'spark_' + $target.minecraftVersion.Replace('.', '_')
    $node = (Get-Command node.exe).Source
    & codex mcp add $serverName -- $node (Join-Path $toolDirectory 'bridge.mjs')
    if ($LASTEXITCODE -ne 0) { throw 'Registering the spark MCP server failed.' }
}

Write-Host "Ready: spark $($target.modVersion) for Minecraft $($target.minecraftVersion) / $($target.loader)."
Write-Host 'Launch: ./gradlew.bat runClient -PwithMinecraftMcp (spark is enabled by default)'
