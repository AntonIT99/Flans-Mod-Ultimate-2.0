<#
.SYNOPSIS
    Runs the native Windows Qodana JVM Community inspection and prints a compact finding list.

.DESCRIPTION
    This worker is intended to run inside scripts/run-validation.ps1's Qodana/Idea mutex. It invokes the
    Gradle qodanaScan task through the shared process supervisor, with run-specific results and an optional
    checkout-local engine cache. It never uploads to Qodana Cloud or applies fixes.
#>
param(
    [string]$Path,
    [string[]]$Files,
    [string]$MinSeverity = 'WEAK WARNING',
    [string]$Profile = '.idea\inspectionProfiles\Project_Default.xml',
    [Alias('InspectionTimeoutSeconds')]
    [ValidateRange(1, 86400)]
    [int]$TimeoutSeconds = 1200,
    [switch]$Json,
    [switch]$Full,
    [switch]$NoCache,
    [switch]$Rebuild,
    [string]$RunId,
    [string]$StateFile
)

$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..')).TrimEnd([IO.Path]::DirectorySeparatorChar)
$validationProcess = Join-Path $PSScriptRoot 'validation-process.ps1'
if (-not (Test-Path -LiteralPath $validationProcess -PathType Leaf))
{
    throw "Validation process helper not found: $validationProcess"
}
. $validationProcess
$clock = [Diagnostics.Stopwatch]::StartNew()

function Resolve-ProjectPaths([string]$RepositoryRoot, [string[]]$RequestedPaths, [switch]$RequireFiles)
{
    $rootPath = [IO.Path]::GetFullPath($RepositoryRoot).TrimEnd([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar)
    $resolved = @()
    foreach ($requested in $RequestedPaths)
    {
        foreach ($entry in ($requested -split '[;,]' | Where-Object { $_.Trim() }))
        {
            $value = $entry.Trim()
            $candidate = if ([IO.Path]::IsPathRooted($value)) { $value } else { Join-Path $rootPath $value }
            $fullPath = [IO.Path]::GetFullPath($candidate)
            if (-not $fullPath.StartsWith($rootPath + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase))
            {
                throw "Inspection path must be under the repository root: $value"
            }
            if (-not (Test-Path -LiteralPath $fullPath))
            {
            if ($RequireFiles) { throw "Inspection path does not exist: $value" }
                continue
            }
            $isFile = Test-Path -LiteralPath $fullPath -PathType Leaf
            if ($RequireFiles -and (-not $isFile -or [IO.Path]::GetExtension($fullPath) -notmatch '^\.(java|kt)$'))
            {
                throw "Inspection file must be an existing Java or Kotlin source: $value"
            }
            if (-not $isFile -or [IO.Path]::GetExtension($fullPath) -match '^\.(java|kt)$')
            {
                $resolved += [pscustomobject]@{
                    FullPath = $fullPath
                    RelativePath = $fullPath.Substring($rootPath.Length + 1).Replace([char]92, '/')
                    IsFile = $isFile
                }
            }
        }
    }
    @($resolved | Sort-Object RelativePath -Unique)
}

function Get-CommonInspectionDirectory([string]$RepositoryRoot, $scope)
{
    $directories = @($scope | ForEach-Object {
        if ($_.IsFile) { [IO.Path]::GetDirectoryName($_.FullPath) } else { $_.FullPath }
    })
    if (-not $directories.Count) { return $null }
    $common = $directories[0]
    foreach ($directory in $directories | Select-Object -Skip 1)
    {
        while (-not $directory.StartsWith($common.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase) -and
            -not $directory.Equals($common, [StringComparison]::OrdinalIgnoreCase))
        {
            $parent = [IO.Directory]::GetParent($common)
            if (-not $parent) { return $RepositoryRoot }
            $common = $parent.FullName
        }
    }
    if ($common.Equals($RepositoryRoot, [StringComparison]::OrdinalIgnoreCase)) { return $null }
    $common.Substring($RepositoryRoot.Length + 1).Replace([char]92, '/')
}

function Get-ProfileName([string]$ProfileFile)
{
    $xml = [xml](Get-Content -LiteralPath $ProfileFile -Raw)
    $profileNode = $xml.SelectSingleNode('//profile/option[@name="myName"]')
    $name = if ($profileNode) { [string]$profileNode.value } else { [string]$xml.profile.name }
    if (-not $name) { throw "Inspection profile has no name: $ProfileFile" }
    $name
}

function New-QodanaConfiguration([string]$ProfileName, [bool]$IncludeGlobal, [string[]]$IncludedFiles)
{
    $lines = @(
        'version: "1.0"',
        'linter: qodana-jvm-community',
        'withinDocker: false',
        'plugins:',
        '  - id: com.demonwav.minecraft-dev',
        'profile:',
        "  name: '$($ProfileName.Replace("'", "''"))'",
        '  inspections:'
    )
    if (-not $IncludeGlobal)
    {
        foreach ($inspection in @(
            'DuplicatedCode', 'DuplicatedCode_lang', 'unused', 'SpellCheckingInspection', 'GrazieInspection',
            'LanguageDetectionInspection', 'VulnerableLibrariesLocal', 'JavaDoc', 'JavadocDeclaration',
            'MissingJavadoc', 'EmptyDirectory'
        ))
        {
            $lines += "    - inspection: $inspection"
            $lines += '      enabled: false'
        }
    }
    # This project names mixin members with a mod-id prefix, as required by its repository rules.
    $lines += '    - inspection: AddedMixinMembersNamePattern'
    $lines += '      enabled: false'
    if ($IncludedFiles.Count)
    {
        $lines += '    - group: ALL'
        $lines += '      ignore:'
        $lines += '        - "**"'
        foreach ($file in $IncludedFiles)
        {
            $lines += '        - "!' + $file.Replace('"', '\"') + '"'
        }
    }
    $lines -join "`n" | ForEach-Object { $_ + "`n" }
}

function Get-SarifInspectionProblems([string]$ReportPath, [string]$RepositoryRoot, [string[]]$AllowedFiles)
{
    if (-not (Test-Path -LiteralPath $ReportPath -PathType Leaf))
    {
        throw "Qodana completed without qodana.sarif.json: $ReportPath"
    }
    try { $sarif = Get-Content -LiteralPath $ReportPath -Raw | ConvertFrom-Json }
    catch { throw "Qodana SARIF is malformed: $ReportPath ($($_.Exception.Message))" }
    if ($sarif.version -notmatch '^2\.1\.0$' -or -not $sarif.runs -or $sarif.runs.Count -ne 1)
    {
        throw "Qodana SARIF has an unsupported or incomplete structure: $ReportPath"
    }
    $run = $sarif.runs[0]
    if (-not $run.invocations -or $run.invocations.Count -lt 1 -or $run.invocations[0].executionSuccessful -ne $true)
    {
        throw "Qodana SARIF does not confirm a successful inspection execution: $ReportPath"
    }
    if (-not $run.PSObject.Properties['results'] -or -not $run.tool.driver.PSObject.Properties['rules'])
    {
        throw "Qodana SARIF is missing its results or inspection rule metadata: $ReportPath"
    }

    $allowed = @{}
    foreach ($file in $AllowedFiles) { $allowed[$file.Replace([char]92, '/').ToLowerInvariant()] = $true }
    $rules = @{}
    foreach ($rule in @($run.tool.driver.rules)) { $rules[[string]$rule.id] = $rule }
    $problems = @()
    foreach ($result in $run.results)
    {
        $location = @($result.locations)[0].physicalLocation
        $artifact = [string]$location.artifactLocation.uri
        if (-not $artifact -or $null -eq $location.region.startLine -or -not $result.ruleId -or -not $result.message.text)
        {
            throw 'Qodana SARIF contains a finding without path, line, inspection id, or message.'
        }
        $path = [Uri]::UnescapeDataString($artifact.Replace('\', '/'))
        if ($path.StartsWith('file:', [StringComparison]::OrdinalIgnoreCase)) { $path = ([Uri]$path).LocalPath.Replace([char]92, '/') }
        if ($path -match '^[A-Za-z]:/' -or $path.StartsWith('/'))
        {
            $fullPath = [IO.Path]::GetFullPath($path)
            $rootPath = [IO.Path]::GetFullPath($RepositoryRoot).TrimEnd([IO.Path]::DirectorySeparatorChar)
            if (-not $fullPath.StartsWith($rootPath + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase))
            {
                throw "Qodana SARIF reports a file outside the repository: $path"
            }
            $relative = $fullPath.Substring($rootPath.Length + 1).Replace([char]92, '/')
        }
        else
        {
            $fullPath = [IO.Path]::GetFullPath((Join-Path $RepositoryRoot $path))
            $rootPath = [IO.Path]::GetFullPath($RepositoryRoot).TrimEnd([IO.Path]::DirectorySeparatorChar)
            if (-not $fullPath.StartsWith($rootPath + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase))
            {
                throw "Qodana SARIF reports a file outside the repository: $path"
            }
            $relative = $fullPath.Substring($rootPath.Length + 1).Replace([char]92, '/')
        }
        if ($AllowedFiles.Count -and -not $allowed.ContainsKey($relative.ToLowerInvariant())) { continue }

        $rule = $rules[[string]$result.ruleId]
        $level = [string]$result.level
        if (-not $level) { $level = [string]$rule.defaultConfiguration.level }
        $severity = switch ($level.ToLowerInvariant())
        {
            'error' { 'ERROR' }
            'warning' { 'WARNING' }
            'note' { 'WEAK WARNING' }
            default { throw "Qodana SARIF has an unknown severity '$level' for $($result.ruleId)." }
        }
        $inspection = [string]$rule.properties.inspectionId
        if (-not $inspection) { $inspection = [string]$result.ruleId }
        $problems += [pscustomobject]@{
            file = $relative
            line = [int]$location.region.startLine
            severity = $severity
            inspection = $inspection
            message = [string]$result.message.text
        }
    }
    $seen = @{}
    @($problems | Sort-Object file, line, inspection, message | Where-Object {
        $key = @($_.file, $_.line, $_.severity, $_.inspection, $_.message) -join "`0"
        if ($seen.ContainsKey($key)) { return $false }
        $seen[$key] = $true
        return $true
    })
}

function Resolve-ChangedJavaFiles([string]$RepositoryRoot)
{
    $changed = @(@(git -C $RepositoryRoot diff --name-only HEAD) + @(git -C $RepositoryRoot ls-files --others --exclude-standard) |
        Where-Object { $_ -and $_ -match '\.(java|kt)$' -and (Test-Path -LiteralPath (Join-Path $RepositoryRoot $_) -PathType Leaf) } |
        Sort-Object -Unique)
    @(Resolve-ProjectPaths $RepositoryRoot $changed)
}

if ($Path -and $Files) { throw 'Use either -Path or -Files, not both.' }
$severityRank = @{ 'ERROR' = 4; 'WARNING' = 3; 'WEAK WARNING' = 2; 'INFORMATION' = 1 }
if (-not $severityRank.ContainsKey($MinSeverity.ToUpperInvariant())) { throw "Unknown minimum severity: $MinSeverity" }
$minRank = $severityRank[$MinSeverity.ToUpperInvariant()]
$profilePath = if ([IO.Path]::IsPathRooted($Profile)) { [IO.Path]::GetFullPath($Profile) } else { [IO.Path]::GetFullPath((Join-Path $root $Profile)) }
if (-not $profilePath.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or
    -not (Test-Path -LiteralPath $profilePath -PathType Leaf))
{
    throw "Inspection profile must exist inside the repository: $Profile"
}
$profileName = Get-ProfileName $profilePath

$explicitFileScope = @()
$scope = @()
if ($Files)
{
    $explicitFileScope = @(Resolve-ProjectPaths $root $Files -RequireFiles | Where-Object IsFile)
    if (-not $explicitFileScope.Count) { throw 'No Java or Kotlin source files were selected.' }
    $scope = $explicitFileScope
}
elseif ($Path)
{
    $scope = @(Resolve-ProjectPaths $root @($Path))
    if (-not $scope.Count -or $scope[0].IsFile) { throw "-Path must name a project directory: $Path" }
}
else
{
    $scope = @(Resolve-ChangedJavaFiles $root)
    if (-not $scope.Count) { throw 'No changed Java or Kotlin files were found. Pass -Files or -Path.' }
    $explicitFileScope = $scope
}

$runId = if ($RunId) { $RunId } else { [Guid]::NewGuid().ToString('N') }
if ($runId -notmatch '^[A-Za-z0-9_.-]+$') { throw 'RunId may contain only letters, digits, period, underscore, and hyphen.' }
$base = Join-Path $root 'build\qodana\inspect'
$runRoot = Join-Path $base $runId
$results = Join-Path $runRoot 'results'
$logs = Join-Path $runRoot 'logs'
$null = New-Item -ItemType Directory -Force $results, $logs
$reportPath = Join-Path $results 'qodana.sarif.json'
$stdout = Join-Path $logs 'gradle.stdout.log'
$stderr = Join-Path $logs 'gradle.stderr.log'
$configPathArg = $null
$configFile = Join-Path $runRoot 'qodana.yaml'
$config = New-QodanaConfiguration $profileName ([bool]$Full) @($explicitFileScope | ForEach-Object RelativePath)
Set-Content -LiteralPath $configFile -Value $config -Encoding UTF8
$configPathArg = $configFile.Substring($root.Length + 1).Replace([char]92, '/')
$onlyDirectory = Get-CommonInspectionDirectory $root $scope
$cache = if ($NoCache -or $Rebuild) { Join-Path $runRoot 'private-cache' } else { Join-Path $root 'build\qodana\cache' }
$cli = Join-Path $root 'build\qodana\bin\qodana.exe'
$gradle = Join-Path $root 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradle -PathType Leaf)) { throw "Gradle wrapper not found: $gradle" }
if ((Test-Path -LiteralPath $results -PathType Container) -and @(Get-ChildItem -LiteralPath $results -Force).Count)
{
    throw "Qodana results directory is not empty; refusing to accept a stale SARIF report: $results"
}
$launcherCache = Join-Path $root '.gradle\qodana-launcher'
$arguments = @(
    'qodanaScan',
    '--project-cache-dir', $launcherCache,
    "-PqodanaResultsPath=$results",
    "-PqodanaCachePath=$cache",
    "-PqodanaPath=$cli"
)
if ($configPathArg) { $arguments += "-PqodanaConfigPath=$configPathArg" }
if ($onlyDirectory) { $arguments += "-PqodanaOnlyDirectory=$onlyDirectory" }
$arguments += @('--no-daemon', '--console=plain')

# Qodana Community has no paid-linter token requirement. Remove any caller token to guarantee that this
# local inspection cannot submit results to Qodana Cloud.
$savedToken = $env:QODANA_TOKEN
Remove-Item Env:QODANA_TOKEN -ErrorAction SilentlyContinue
try
{
    $exitCode = Invoke-ValidationProcess -FilePath $gradle -Arguments $arguments -TimeoutSeconds $TimeoutSeconds `
        -Label 'Qodana inspection' -StandardOutput $stdout -StandardError $stderr -QuietOutput -StateFile $StateFile
}
finally
{
    if ($null -ne $savedToken) { $env:QODANA_TOKEN = $savedToken }
}
if ($exitCode -ne 0) { throw "Qodana Gradle task failed with exit code $exitCode. Logs: $stdout ; $stderr" }

$allowedFiles = @($explicitFileScope | ForEach-Object RelativePath)
$problems = @(Get-SarifInspectionProblems $reportPath $root $allowedFiles)
$visible = @($problems | Where-Object { $severityRank[$_.severity] -ge $minRank })
$limit = 2000
$display = @($visible | Select-Object -First $limit)
if ($Json)
{
    ConvertTo-Json -InputObject @($display) -Depth 8 -Compress
}
else
{
    foreach ($problem in $display)
    {
        "{0}:{1}: [{2}] {3}: {4}" -f $problem.file, $problem.line, $problem.severity, $problem.inspection, $problem.message
    }
    if ($visible.Count -gt $display.Count) { "... $($visible.Count - $display.Count) additional findings omitted from console output." }
    if ($visible.Count)
    {
        "Qodana found $($visible.Count) problem(s) at or above $($MinSeverity.ToUpperInvariant()). ($([int]$clock.Elapsed.TotalSeconds)s, native scan)"
    }
    else
    {
        "No Qodana problems at or above $($MinSeverity.ToUpperInvariant()). ($([int]$clock.Elapsed.TotalSeconds)s, native scan)"
    }
}
