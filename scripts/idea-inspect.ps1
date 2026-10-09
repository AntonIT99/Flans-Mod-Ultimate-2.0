<#
.SYNOPSIS
    Runs IntelliJ IDEA's headless code inspections and prints a compact warning list for agents.

.DESCRIPTION
    Uses the local IntelliJ installation (bin\inspect.bat) with the shared project profile
    .idea\inspectionProfiles\Project_Default.xml, so the results match the warnings shown in the editor.
    A separate config/system directory is used so the inspection can run while the IDE is open.
    The first run indexes the project and JDK/libraries and is slow; later runs reuse the cache.

.EXAMPLE
    powershell -File scripts\idea-inspect.ps1                       # files with uncommitted VCS changes
    powershell -File scripts\idea-inspect.ps1 -Path src\main\java\com\flansmodultimate\client\render
    powershell -File scripts\idea-inspect.ps1 -MinSeverity WARNING -Json
#>
param(
    # Directory to inspect, relative to the repository root. Omit to inspect locally changed files only.
    [string]$Path,
    # Lowest severity to report: ERROR, WARNING, WEAK WARNING, INFORMATION (TYPO and lower are always hidden).
    [string]$MinSeverity = 'WEAK WARNING',
    [string]$Profile = '.idea\inspectionProfiles\Project_Default.xml',
    # Override the IntelliJ install dir; defaults to $env:IDEA_HOME, then the usual install locations.
    [string]$IdeaHome = $env:IDEA_HOME,
    # Print machine-readable JSON instead of text.
    [switch]$Json
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

if (-not $IdeaHome)
{
    $candidates = @(
        "$env:LOCALAPPDATA\Programs\IntelliJ IDEA Ultimate",
        "$env:LOCALAPPDATA\Programs\IntelliJ IDEA Community Edition",
        "$env:LOCALAPPDATA\Programs\IntelliJ IDEA"
    ) + (Get-ChildItem 'C:\Program Files\JetBrains' -Directory -Filter 'IntelliJ IDEA*' -ErrorAction SilentlyContinue | ForEach-Object FullName)
    $IdeaHome = $candidates | Where-Object { Test-Path "$_\bin\inspect.bat" } | Select-Object -First 1
}
if (-not $IdeaHome -or -not (Test-Path "$IdeaHome\bin\inspect.bat"))
{
    throw 'IntelliJ IDEA not found. Set IDEA_HOME or pass -IdeaHome.'
}

# Isolated IDE dirs: a second IDE instance cannot share config/system with the running editor.
$work = Join-Path $root 'build\idea-inspect'
$output = Join-Path $work 'out'
New-Item -ItemType Directory -Force $work | Out-Null
if (Test-Path $output)
{
    Remove-Item -Recurse -Force $output
}
$props = Join-Path $work 'idea.properties'
$cfg = (Join-Path $work 'config') -replace '\\', '/'
$sys = (Join-Path $work 'system') -replace '\\', '/'
Set-Content -Encoding ASCII $props @(
    "idea.config.path=$cfg",
    "idea.system.path=$sys",
    "idea.plugins.path=$cfg/plugins",
    "idea.log.path=$sys/log"
)
$env:IDEA_PROPERTIES = $props

$inspectArgs = @($root, (Join-Path $root $Profile), $output, '-v0', '-format', 'json')
if ($Path)
{
    $inspectArgs += @('-d', (Resolve-Path (Join-Path $root $Path)).Path)
}
else
{
    $inspectArgs += '-changes'
}

$log = Join-Path $work 'inspect.log'
& "$IdeaHome\bin\inspect.bat" @inspectArgs *> $log
if (-not (Test-Path $output))
{
    Get-Content $log -Tail 40
    throw "Inspection produced no output (exit code $LASTEXITCODE). Full log: $log"
}

$rank = @{ 'ERROR' = 4; 'WARNING' = 3; 'WEAK WARNING' = 2; 'INFORMATION' = 1 }
$min = $rank[$MinSeverity.ToUpper()]
$problems = foreach ($file in Get-ChildItem $output -Filter '*.json')
{
    # Skip metadata, prose/spelling checks, and the aggregate duplicate of DuplicatedCode.
    if ($file.BaseName -match '^(\.descriptions|SpellCheckingInspection|Grazie.*|.*_aggregate)$')
    {
        continue
    }
    $data = Get-Content -Raw $file.FullName | ConvertFrom-Json
    foreach ($p in $data.problems)
    {
        $sev = "$($p.problem_class.severity)".ToUpper()
        if (-not $rank.ContainsKey($sev) -or $rank[$sev] -lt $min)
        {
            continue
        }
        $path = "$($p.file)" -replace '^file://\$PROJECT_DIR\$/', ''
        # Mixin handlers are invoked by injection, so IntelliJ (without the Minecraft Development plugin) flags them as unused.
        if ($file.BaseName -eq 'unused' -and $path -match '/mixin/')
        {
            continue
        }
        [pscustomobject]@{
            file       = $path
            line       = $p.line
            severity   = $sev
            inspection = $file.BaseName
            message    = ("$($p.description)" -replace '<[^>]+>', '' -replace '&lt;', '<' -replace '&gt;', '>' -replace '&amp;', '&')
        }
    }
}
$problems = @($problems | Sort-Object file, line)

if ($Json)
{
    ConvertTo-Json -InputObject $problems -Depth 3
}
elseif ($problems.Count -eq 0)
{
    'No IntelliJ inspection problems at or above ' + $MinSeverity + '.'
}
else
{
    foreach ($p in $problems)
    {
        "$($p.file):$($p.line): [$($p.severity)] $($p.inspection): $($p.message)"
    }
    "$($problems.Count) problem(s). Raw report: $output"
}
