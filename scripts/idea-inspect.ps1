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
        "$env:LOCALAPPDATA\Programs\IntelliJ IDEA",
        # Sandboxed shells may redirect LOCALAPPDATA away from the user's installed programs.
        "$env:USERPROFILE\AppData\Local\Programs\IntelliJ IDEA Ultimate",
        "$env:USERPROFILE\AppData\Local\Programs\IntelliJ IDEA Community Edition",
        "$env:USERPROFILE\AppData\Local\Programs\IntelliJ IDEA"
    ) + (Get-ChildItem 'C:\Program Files\JetBrains' -Directory -Filter 'IntelliJ IDEA*' -ErrorAction SilentlyContinue | ForEach-Object FullName)
    $IdeaHome = $candidates | Where-Object { Test-Path "$_\bin\inspect.bat" } | Select-Object -First 1
}
if (-not $IdeaHome -or -not (Test-Path "$IdeaHome\bin\inspect.bat"))
{
    throw 'IntelliJ IDEA not found. Set IDEA_HOME or pass -IdeaHome.'
}

# Isolated IDE dirs: a second IDE instance cannot share config/system with the running editor or with another
# inspection run, so every run gets its own directory. Starting from an empty system dir also keeps the headless IDE
# from inspecting a stale cached file snapshot (VFS) instead of the files on disk.
$base = Join-Path $root 'build\idea-inspect'
$work = Join-Path $base "run-$PID"
$output = Join-Path $work 'out'
New-Item -ItemType Directory -Force $work | Out-Null
# Runs killed by a timeout cannot clean up after themselves.
Get-ChildItem $base -Directory -Filter 'run-*' | Where-Object { $_.LastWriteTime -lt (Get-Date).AddHours(-2) } |
    Remove-Item -Recurse -Force -ErrorAction SilentlyContinue

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

# inspect.bat -changes shelves and unshelves the working tree, which stages changes and can overwrite them, so changed
# files come from git instead: one run inspects a scope made of their directories and the results are limited to those
# files. Starting the IDE once per directory would take about half a minute each.
$changed = $null
if ($Path)
{
    $target = @('-d', (Resolve-Path (Join-Path $root $Path)).Path)
}
else
{
    $changed = @(git -C $root diff --name-only HEAD) + @(git -C $root ls-files --others --exclude-standard) |
        Where-Object { $_ -match '\.(java|kt)$' -and (Test-Path (Join-Path $root $_)) } | Sort-Object -Unique
    if (-not $changed)
    {
        Remove-Item -Recurse -Force $work
        'No changed Java or Kotlin files to inspect.'
        return
    }
    # Scope entries are 'dir/*' (files directly inside) or 'dir//*' (recursive). IntelliJ matches them against the path
    # below the module content root, which is src/<sourceSet> here; '*' also spans directories, for other layouts.
    $entries = @($changed | ForEach-Object { (Split-Path $_ -Parent).Replace([char]92, '/') } | Sort-Object -Unique |
        ForEach-Object { if ($_ -match '^src/[^/]+/(.+)$') { "$($Matches[1])/*" } else { "*/$_/*" } } | Sort-Object -Unique)
    # A Windows command line is capped at 32767 characters: merge the deepest directories into their parents until
    # the scope fits.
    while ((($entries | Measure-Object Length -Sum).Sum + 9 * $entries.Count) -gt 28000)
    {
        $depth = ($entries | ForEach-Object { ($_ -split '/').Count } | Measure-Object -Maximum).Maximum
        if ($depth -le 3)
        {
            break
        }
        $entries = @($entries | ForEach-Object { if (($_ -split '/').Count -eq $depth) { ($_ -replace '/[^/]+//?\*$', '') + '//*' } else { $_ } } | Sort-Object -Unique)
        $recursive = @($entries | Where-Object { $_.EndsWith('//*') } | ForEach-Object { $_.Substring(0, $_.Length - 2) })
        $entries = @($entries | Where-Object { $e = $_; -not ($recursive | Where-Object { $e.StartsWith($_) -and $e -ne "$_/*" }) })
    }
    $target = @('-scope', (($entries | ForEach-Object { "file:$_" }) -join '||'))
}
# The headless IDE may rewrite shared .idea settings (for example codeStyles); put them back afterwards.
$ideaDir = Join-Path $root '.idea'
$ideaBackup = @{}
Get-ChildItem $ideaDir -Recurse -File -Filter '*.xml' | ForEach-Object { $ideaBackup[$_.FullName] = [IO.File]::ReadAllBytes($_.FullName) }

$log = Join-Path $work 'inspect.log'
try
{
    $inspectArgs = @($root, (Join-Path $root $Profile), $output, '-v0', '-format', 'json') + $target
    # inspect.bat is only 'idea.bat inspect'; cmd.exe would read the '||' of the scope as an operator and caps a
    # command line at 8191 characters, so the launcher is started directly.
    $argumentLine = (@('inspect') + $inspectArgs | ForEach-Object { '"' + $_ + '"' }) -join ' '
    $process = Start-Process "$IdeaHome\bin\idea64.exe" -ArgumentList $argumentLine -Wait -PassThru -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError "$log.err"
    if (-not (Test-Path $output))
    {
        Get-Content $log, "$log.err" -Tail 40
        throw "Inspection produced no output (exit code $($process.ExitCode)). Full log: $log"
    }
}
finally
{
    foreach ($entry in $ideaBackup.GetEnumerator())
    {
        if (-not (Test-Path $entry.Key) -or -not [Linq.Enumerable]::SequenceEqual([byte[]][IO.File]::ReadAllBytes($entry.Key), [byte[]]$entry.Value))
        {
            [IO.File]::WriteAllBytes($entry.Key, $entry.Value)
        }
    }
    # Indexes and caches are several hundred MB per run; only the report and the log are worth keeping.
    Remove-Item -Recurse -Force (Join-Path $work 'config'), (Join-Path $work 'system') -ErrorAction SilentlyContinue
}
$rank = @{ 'ERROR' = 4; 'WARNING' = 3; 'WEAK WARNING' = 2; 'INFORMATION' = 1 }
$min = $rank[$MinSeverity.ToUpper()]
$problems = foreach ($file in Get-ChildItem $output -Recurse -Filter '*.json')
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
        if ($changed -and $changed -notcontains $path)
        {
            continue
        }
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
    "$($problems.Count) problem(s)."
}

Remove-Item -Recurse -Force $work -ErrorAction SilentlyContinue
