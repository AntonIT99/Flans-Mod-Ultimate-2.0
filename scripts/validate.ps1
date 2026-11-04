<#
.SYNOPSIS
    Selects deterministic, explicitly scoped validation without automatic model calls or heavy escalation.
.EXAMPLE
    powershell -NoProfile -File scripts/validate.ps1 -Profile Focused -Task 'Banner editing' -Files 'src/main/java/example.java;src/test/java/exampleTest.java' -Tests '*ExampleTest' -Apply -Plan
#>
param(
    [Parameter(Mandatory = $true)][ValidateSet('Data', 'Focused', 'Analysis', 'Deep', 'Release')][string]$Profile,
    [Parameter(Mandatory = $true)][string]$Task,
    [Parameter(Mandatory = $true)][string[]]$Files,
    [string[]]$SourceSets,
    [string[]]$Tests,
    [string]$NoTestsReason,
    [string[]]$Artifacts,
    [switch]$Apply,
    [switch]$Plan,
    [ValidateSet('Compact', 'Full', 'Json')][string]$OutputMode = 'Compact'
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'validation-scope.ps1')
$selected = @(Resolve-ValidationFiles $root $Files)
$Tests = @($Tests | ForEach-Object { $_ -split '[;,]' } | Where-Object { $_ })
$Artifacts = @($Artifacts | ForEach-Object { $_ -split '[;,]' } | Where-Object { $_ })
$SourceSets = @($SourceSets | ForEach-Object { $_ -split '[;,]' } | Where-Object { $_ })
if (-not $SourceSets.Count)
{
    $SourceSets = @($selected | ForEach-Object {
        if ($_ -match '^src/([^/]+)/java/')
        {
            $directory = $Matches[1]
            $module = Join-Path $root "src/$directory/fmu-module.gradle"
            $definition = if (Test-Path -LiteralPath $module) { Get-Content -LiteralPath $module -Raw } else { '' }
            if ($definition -match "sourceSet:\s*'([^']+)'") { $Matches[1] } else { $directory }
        }
    } | Sort-Object -Unique)
}
foreach ($source in $SourceSets) { if ($source -notmatch '^[A-Za-z][A-Za-z0-9]*$') { throw "Invalid source set: $source" } }
foreach ($artifact in $Artifacts) { if ($artifact -notmatch '^[A-Za-z][A-Za-z0-9]*Jar$' -and $artifact -ne 'jar') { throw "Select explicit jar tasks, not arbitrary commands: $artifact" } }
if ($Profile -eq 'Data' -and ($selected -match '\.(java|kt|gradle|properties)$')) { throw 'Data does not validate Java or build configuration; select a compilation/build profile.' }
if ($Profile -eq 'Release' -and ($Tests.Count -or $NoTestsReason)) { throw 'Release runs the full suite; do not pass test filters or an omission reason.' }
if ($Profile -ne 'Data' -and $Profile -ne 'Release' -and -not $SourceSets.Count) { throw 'Select affected -SourceSets, or use Data for documentation/data/tooling syntax.' }
if ($Profile -in @('Focused', 'Analysis', 'Deep') -and -not $Tests.Count -and -not $NoTestsReason) { throw 'Provide focused -Tests or an explicit -NoTestsReason; tests are never silently omitted.' }
$tasks = @()
if ($Profile -ne 'Data')
{
    if ($Apply) { $tasks += 'spotlessApply' }
    $tasks += 'spotlessCheck'
    foreach ($source in $SourceSets)
    {
        $tasks += $(if ($source -eq 'main') { 'compileJava' } else { 'compile' + $source.Substring(0, 1).ToUpperInvariant() + $source.Substring(1) + 'Java' })
    }
    if ($Profile -eq 'Release') { $tasks += 'build' }
    elseif ($Tests.Count)
    {
        $tasks += 'test'
        foreach ($testFilter in $Tests) { $tasks += @('--tests', $testFilter) }
    }
    if ($Profile -in @('Analysis', 'Deep'))
    {
        foreach ($source in $SourceSets) { $tasks += 'sonarlint' + $source.Substring(0, 1).ToUpperInvariant() + $source.Substring(1) }
    }
    $tasks += $Artifacts
}
$javaFiles = @($selected | Where-Object { $_ -match '\.(java|kt)$' -and (Test-Path -LiteralPath (Join-Path $root $_)) })
if ($Profile -eq 'Deep' -and -not $javaFiles.Count) { throw 'Deep requires existing Java/Kotlin files for inspections.' }
$planResult = @{ profile = $Profile; task = $Task; files = $selected; sourceSets = $SourceSets; gradle = $tasks; inspections = ($Profile -eq 'Deep'); inspectionBackend = $(if ($Profile -eq 'Deep') { 'Qodana' } else { $null }); testsOmittedReason = $NoTestsReason; apply = [bool]$Apply }
if ($Plan) { $planResult | ConvertTo-Json -Depth 5; exit 0 }
$checks = if ($Profile -eq 'Data') { 'format/syntax/diff' } else { $tasks -join ' ' }
[Console]::Error.WriteLine("$Task | $Profile | $($selected.Count) paths | $checks$(if ($NoTestsReason) { " | tests omitted: $NoTestsReason" })")
$coordinator = Join-Path $PSScriptRoot 'run-validation.ps1'
if ($Profile -ne 'Data')
{
    $scopeDirectory = Join-Path $root '.gradle/validation-scopes'
    $null = New-Item -ItemType Directory -Force $scopeDirectory
    $scopeFile = Join-Path $scopeDirectory ([Guid]::NewGuid().ToString('N') + '.gradle')
    $paths = @($selected | ForEach-Object { "'" + (Join-Path $root $_).Replace('\', '/').Replace("'", "\'") + "'" }) -join ', '
    $scopeTemplate = @'
gradle.projectsEvaluated {
    def owned = [__PATHS__]
    rootProject.spotless {
        java { target(owned.findAll { it.endsWith('.java') }) }
        format('json') { target(owned.findAll { it ==~ /.*\/src\/[^\/]+\/resources\/.*\.json/ }) }
        format('misc') { target(owned.findAll { it.endsWith('.md') || it.endsWith('.gradle') || it.endsWith('.properties') || it.endsWith('/.gitignore') }) }
    }
    def canonical = owned.collect { new File(it).canonicalPath } as Set
    rootProject.tasks.matching { it.name.startsWith('sonarlint') }.configureEach {
        exclude { !it.directory && !canonical.contains(it.file.canonicalPath) }
    }
}
'@
    [IO.File]::WriteAllText($scopeFile, $scopeTemplate.Replace('__PATHS__', $paths), [Text.UTF8Encoding]::new($false))
    try
    {
        & $coordinator -Task "$Task | $Profile" -OutputMode $OutputMode Gradle @tasks --init-script $scopeFile
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    finally { Remove-Item -LiteralPath $scopeFile -ErrorAction SilentlyContinue }
}
$dataArguments = @('-Files', ($selected -join ';'))
if ($Apply) { $dataArguments += '-Apply' }
& $coordinator -Task "$Task | $Profile | syntax/diff" -OutputMode $OutputMode Data @dataArguments
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($Profile -eq 'Deep')
{
    & $coordinator -Task "$Task | Deep | inspections" -OutputMode $OutputMode Qodana -Files ($javaFiles -join ';')
    exit $LASTEXITCODE
}
exit 0
