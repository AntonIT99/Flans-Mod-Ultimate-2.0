<#
.SYNOPSIS
    Runs IntelliJ IDEA's headless code inspections and prints a compact warning list for agents.

.DESCRIPTION
    Uses the local IntelliJ installation (bin\idea64.exe inspect) with the shared project profile
    .idea\inspectionProfiles\Project_Default.xml, so the results match the warnings shown in the editor.

    Two things keep repeated runs cheap:
      * Unless -Full is given, the project profile is used with the expensive whole-project inspections
        disabled (duplicate code, unused declarations, spelling/grammar, vulnerable libraries, Javadoc).
        Those force a whole-program analysis however narrow the scope is, and their results were discarded
        by this script anyway. The derived profile is generated from the project profile on every run, so it
        cannot drift from it.
      * Results are cached under build\idea-inspect\cache, keyed by the IDE build, the profile, the target
        and the content of HEAD plus every locally modified file. An unchanged tree answers instantly.

    Each run uses its own config/system directory, so the inspection can run while the IDE is open and alongside
    another run, and so it never analyses a cached file snapshot instead of what is on disk.

.EXAMPLE
    powershell -File scripts\idea-inspect.ps1                       # files with uncommitted VCS changes
    powershell -File scripts\idea-inspect.ps1 -Path src\main\java\com\flansmodultimate\client\render
    powershell -File scripts\idea-inspect.ps1 -MinSeverity WARNING -Json
    powershell -File scripts\idea-inspect.ps1 -Full                 # add the whole-project inspections back
    powershell -File scripts\idea-inspect.ps1 -Rebuild              # ignore the result cache
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
    [switch]$Json,
    # Also run the slow whole-project inspections.
    [switch]$Full,
    # Ignore and do not write the result cache.
    [switch]$NoCache,
    # Ignore the result cache and take the plugins from the IntelliJ installation again.
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$stopwatch = [Diagnostics.Stopwatch]::StartNew()

# Whole-project ("global") inspections: they analyse the whole dependency graph no matter how small the scope,
# and this script dropped their output anyway. -Full keeps them.
$globalInspections = @(
    'DuplicatedCode', 'DuplicatedCode_lang',  # pairwise comparison across every file in the project
    'unused',                                 # UnusedDeclaration; needs the project-wide usage graph
    'SpellCheckingInspection', 'GrazieInspection', 'LanguageDetectionInspection',  # prose, not code
    'VulnerableLibrariesLocal',               # queries a vulnerability database over the network
    'JavaDoc', 'JavadocDeclaration', 'MissingJavadoc',
    'EmptyDirectory'
)
# Inspections that contradict a documented convention of this project, so they are noise in every mode.
$conflictingInspections = @(
    # The Minecraft Development plugin wants '_' or '$' in added mixin member names; AGENTS.md requires a mod id
    # prefix without '$' instead, so every handler and @Unique member in the project trips this.
    'AddedMixinMembersNamePattern'
)
# Plugins copied from the user's IntelliJ profile into the warm config. The Minecraft Development plugin
# understands mixins and Minecraft APIs, which removes a class of false positives and adds mixin checks.
# Lombok and Kotlin ship with the IDE, so they load without being copied.
$wantedPlugins = @('Minecraft Development')

function Get-StringHash([string]$text)
{
    $sha = [Security.Cryptography.SHA256]::Create()
    try
    {
        return [BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($text))).Replace('-', '')
    }
    finally
    {
        $sha.Dispose()
    }
}

function Install-WantedPlugins([string]$configDir)
{
    New-Item -ItemType Directory -Force "$configDir\plugins" | Out-Null
    $missing = @($wantedPlugins | Where-Object { -not (Test-Path "$configDir\plugins\$_") })
    # <product code>-<build branch>: branch 262 means 2026.2, and the code decides the directory prefix.
    if (-not $missing -or $buildStamp -notmatch '^([A-Z]+)-(\d\d)(\d)\.')
    {
        return
    }
    $version = "20$($Matches[2]).$($Matches[3])"
    $prefix = if ($Matches[1] -eq 'IC') { 'IdeaIC' } else { 'IntelliJIdea' }
    $userPlugins = @("$env:APPDATA\JetBrains\$prefix$version\plugins", "$env:APPDATA\JetBrains\*$version\plugins") |
        ForEach-Object { Get-Item $_ -ErrorAction SilentlyContinue } | Select-Object -First 1
    foreach ($plugin in $missing)
    {
        if ($userPlugins -and (Test-Path (Join-Path $userPlugins $plugin)))
        {
            Copy-Item -Recurse -Force (Join-Path $userPlugins $plugin) "$configDir\plugins\$plugin"
        }
        else
        {
            [Console]::Error.WriteLine("Plugin '$plugin' is not installed in the user's IntelliJ profile; running without it.")
        }
    }
}

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
    $IdeaHome = $candidates | Where-Object { Test-Path "$_\bin\idea64.exe" } | Select-Object -First 1
}
if (-not $IdeaHome -or -not (Test-Path "$IdeaHome\bin\idea64.exe"))
{
    throw 'IntelliJ IDEA not found. Set IDEA_HOME or pass -IdeaHome.'
}
# build.txt holds the product code and build, for example 'IU-262.10968.63'. The install directory name is not
# a reliable product indicator, so the plugin directory and the warm index are keyed off this instead.
$buildStamp = (Get-Content -Raw "$IdeaHome\build.txt").Trim()

# ---------------------------------------------------------------------------------------------------------------
# What to inspect
# ---------------------------------------------------------------------------------------------------------------
# inspect -changes shelves and unshelves the working tree, which stages changes and can overwrite them, so changed
# files come from git instead: one run inspects a scope made of their directories and the results are limited to those
# files. Starting the IDE once per directory would take about half a minute each.
$dirty = @(@(git -C $root diff --name-only HEAD) + @(git -C $root ls-files --others --exclude-standard) |
    Where-Object { $_ -and (Test-Path (Join-Path $root $_)) } | Sort-Object -Unique)
$changed = $null
if ($Path)
{
    $targetPath = (Resolve-Path (Join-Path $root $Path)).Path
    $target = @('-d', $targetPath)
    $targetKey = "dir:" + $targetPath.Substring($root.Length).Replace([char]92, '/')
}
else
{
    $changed = @($dirty | Where-Object { $_ -match '\.(java|kt)$' })
    if (-not $changed)
    {
        'No changed Java or Kotlin files to inspect.'
        return
    }
    # Scope entries are 'dir/*' (files directly inside) or 'dir//*' (recursive). IntelliJ matches them against the path
    # below the module content root, and a Gradle import decides whether that root is src/<sourceSet> or the project
    # directory, so both spellings are offered; the entries are OR-ed and one that matches nothing costs nothing. An
    # entry matching more than intended is harmless too, because the results are filtered back to $changed below.
    $entries = @($changed | ForEach-Object { (Split-Path $_ -Parent).Replace([char]92, '/') } | Sort-Object -Unique |
        ForEach-Object { "$_/*"; if ($_ -match '^src/[^/]+/(.+)$') { "$($Matches[1])/*" } else { "*/$_/*" } } | Sort-Object -Unique)
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
    $targetKey = "files:" + ($changed -join '|')
}

# ---------------------------------------------------------------------------------------------------------------
# The profile actually handed to the IDE
# ---------------------------------------------------------------------------------------------------------------
$profileFile = Join-Path $root $Profile
if (-not (Test-Path $profileFile))
{
    throw "Inspection profile not found: $profileFile"
}
# Derived from the project profile on every run, so a change there is picked up and the two cannot drift.
$xml = [xml](Get-Content -Raw $profileFile)
$profileNode = $xml.SelectSingleNode('//profile')
$disable = if ($Full) { $conflictingInspections } else { $globalInspections + $conflictingInspections }
foreach ($class in $disable)
{
    foreach ($existing in @($profileNode.SelectNodes("inspection_tool[@class='$class']")))
    {
        $profileNode.RemoveChild($existing) | Out-Null
    }
    $tool = $xml.CreateElement('inspection_tool')
    $tool.SetAttribute('class', $class)
    $tool.SetAttribute('enabled', 'false')
    $tool.SetAttribute('level', 'WARNING')
    $tool.SetAttribute('enabled_by_default', 'false')
    $profileNode.AppendChild($tool) | Out-Null
}
$profileContent = $xml.OuterXml

# ---------------------------------------------------------------------------------------------------------------
# Result cache
# ---------------------------------------------------------------------------------------------------------------
$base = Join-Path $root 'build\idea-inspect'
$cacheDir = Join-Path $base 'cache'
# Identical inputs give identical results: the IDE build, the profile, the target, the commit, and the content of
# every locally modified file (what HEAD does not already pin down). Severity filtering happens after the cache,
# so -MinSeverity is not part of the key.
$head = (git -C $root rev-parse HEAD 2>$null)
$dirtyFingerprint = @($dirty | ForEach-Object {
        $dirtyFile = Join-Path $root $_
        if (Test-Path -PathType Leaf $dirtyFile) { "$_=$((Get-FileHash -Algorithm SHA256 $dirtyFile).Hash)" } else { "$_=dir" }
    }) -join "`n"
# A huge working tree would cost more to fingerprint than to inspect.
$cacheable = -not $NoCache -and -not $Rebuild -and $head -and $dirty.Count -le 400
$cacheFile = $null
if ($cacheable)
{
    $cacheFile = Join-Path $cacheDir ((Get-StringHash ($buildStamp, $Profile, $(if ($Full) { 'full' } else { 'fast' }), (Get-StringHash $profileContent), $targetKey, $head, $dirtyFingerprint -join "`n")).Substring(0, 32) + '.json')
}

$rank = @{ 'ERROR' = 4; 'WARNING' = 3; 'WEAK WARNING' = 2; 'INFORMATION' = 1 }
$min = $rank[$MinSeverity.ToUpper()]
$problems = $null
$fromCache = $false
if ($cacheFile -and (Test-Path $cacheFile))
{
    try
    {
        $problems = @(Get-Content -Raw $cacheFile | ConvertFrom-Json)
        $fromCache = $true
        (Get-Item $cacheFile).LastWriteTime = Get-Date
    }
    catch
    {
        $problems = $null
    }
}

# ---------------------------------------------------------------------------------------------------------------
# Run the IDE
# ---------------------------------------------------------------------------------------------------------------
if (-not $fromCache)
{
    # Every run gets its own config and system directory. Keeping the system directory between runs was measurably
    # faster but reported warnings for file content that was no longer on disk: with the indexes of an earlier run in
    # place, the headless IDE analysed the file snapshot (VFS) they were built from. The test is to insert blank lines
    # at the top of a file and re-run: with a kept system directory the reported line numbers do not move. A second
    # instance also cannot share a config/system pair with the running editor, hence one pair per run.
    $pluginTemplate = Join-Path $base ('plugins\' + ($buildStamp -replace '[^A-Za-z0-9._-]', '_'))
    $run = Join-Path $base "run-$PID"
    $cfg = Join-Path $run 'config'
    $sys = Join-Path $run 'system'
    $output = Join-Path $run 'out'
    $log = Join-Path $run 'inspect.log'
    New-Item -ItemType Directory -Force $run, $cfg, $cacheDir | Out-Null
    # Runs killed by a timeout cannot clean up after themselves.
    Get-ChildItem $base -Directory -Filter 'run-*' |
        Where-Object { $_.FullName -ne $run -and $_.LastWriteTime -lt (Get-Date).AddHours(-2) } |
        Remove-Item -Recurse -Force -ErrorAction SilentlyContinue

    # The plugins are the one part worth keeping between runs, since they hold no snapshot of the sources: copied
    # from the user's IntelliJ profile once per IDE build, then into each run's config directory.
    if ($Rebuild)
    {
        Remove-Item -Recurse -Force $pluginTemplate -ErrorAction SilentlyContinue
    }
    Install-WantedPlugins $pluginTemplate
    if (Test-Path "$pluginTemplate\plugins")
    {
        Copy-Item -Recurse -Force "$pluginTemplate\plugins" "$cfg\plugins"
    }
    # The headless IDE may rewrite shared .idea settings (for example codeStyles); put them back afterwards.
    $ideaDir = Join-Path $root '.idea'
    $ideaBackup = @{}
    Get-ChildItem $ideaDir -Recurse -File -Filter '*.xml' | ForEach-Object { $ideaBackup[$_.FullName] = [IO.File]::ReadAllBytes($_.FullName) }
    try
    {
        $props = Join-Path $run 'idea.properties'
        Set-Content -Encoding ASCII $props @(
            "idea.config.path=$($cfg -replace '\\', '/')",
            "idea.system.path=$($sys -replace '\\', '/')",
            "idea.plugins.path=$($cfg -replace '\\', '/')/plugins",
            "idea.log.path=$($sys -replace '\\', '/')/log"
        )
        $env:IDEA_PROPERTIES = $props

        $runProfile = Join-Path $run 'profile.xml'
        Set-Content -Encoding UTF8 $runProfile $profileContent

        # inspect requires a clean output directory, and a recycled process id could leave one behind.
        Remove-Item -Recurse -Force $output -ErrorAction SilentlyContinue
        $inspectArgs = @($root, $runProfile, $output, '-v0', '-format', 'json') + $target
        # cmd.exe would read the '||' of the scope as an operator and caps a command line at 8191 characters,
        # so the launcher is started directly instead of through bin\inspect.bat.
        $argumentLine = (@('inspect') + $inspectArgs | ForEach-Object { '"' + $_ + '"' }) -join ' '
        $process = Start-Process "$IdeaHome\bin\idea64.exe" -ArgumentList $argumentLine -Wait -PassThru -WindowStyle Hidden -RedirectStandardOutput $log -RedirectStandardError "$log.err"
        if (-not (Test-Path $output))
        {
            Get-Content $log, "$log.err" -Tail 40
            throw "Inspection produced no output (exit code $($process.ExitCode)). Full log: $log"
        }

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
                if (-not $rank.ContainsKey($sev))
                {
                    continue
                }
                $path = "$($p.file)" -replace '^file://\$PROJECT_DIR\$/', ''
                if ($changed -and $changed -notcontains $path)
                {
                    continue
                }
                # Mixin handlers are invoked by injection, so without the Minecraft Development plugin the IDE flags
                # them as unused. Only -Full still runs that inspection.
                if ($file.BaseName -eq 'unused' -and $path -match '/mixin/' -and -not (Test-Path "$cfg\plugins\Minecraft Development"))
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
        # Where-Object drops the single $null that an empty loop result would otherwise carry into the array.
        $problems = @($problems | Where-Object { $_ } | Sort-Object file, line)
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
        Remove-Item -Recurse -Force $cfg, $sys -ErrorAction SilentlyContinue
    }
    Remove-Item -Recurse -Force $output -ErrorAction SilentlyContinue

    if ($cacheFile)
    {
        New-Item -ItemType Directory -Force $cacheDir | Out-Null
        ConvertTo-Json -InputObject $problems -Depth 3 -Compress | Set-Content -Encoding UTF8 $cacheFile
        # Keep the cache small: drop everything but the 200 most recently used entries.
        Get-ChildItem $cacheDir -Filter '*.json' | Sort-Object LastWriteTime -Descending | Select-Object -Skip 200 |
            Remove-Item -Force -ErrorAction SilentlyContinue
    }
}

# ---------------------------------------------------------------------------------------------------------------
# Report
# ---------------------------------------------------------------------------------------------------------------
$reported = @($problems | Where-Object { $_ -and $rank["$($_.severity)"] -ge $min } | Sort-Object file, line)
if ($Json)
{
    ConvertTo-Json -InputObject $reported -Depth 3
    return
}
$how = if ($fromCache) { 'cached' } else { "fresh index$(if ($Full) { ', full profile' })" }
if ($reported.Count -eq 0)
{
    "No IntelliJ inspection problems at or above $MinSeverity. ($([int]$stopwatch.Elapsed.TotalSeconds)s, $how)"
    return
}
foreach ($p in $reported)
{
    "$($p.file):$($p.line): [$($p.severity)] $($p.inspection): $($p.message)"
}
"$($reported.Count) problem(s). ($([int]$stopwatch.Elapsed.TotalSeconds)s, $how)"
