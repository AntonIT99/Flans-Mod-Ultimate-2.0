# Policy and scope regression tests with fake Gradle; no IDE, Java build, or Minecraft is launched.
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$fixture = Join-Path $root ('.gradle/validation-profiles-' + [Guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Force "$fixture/scripts", "$fixture/src/main/java", "$fixture/src/test/java", "$fixture/src/custom/java"
foreach ($name in @('validate.ps1', 'run-validation.ps1', 'validation-process.ps1', 'validation-scope.ps1', 'validation-data.ps1', 'validation-reports.ps1'))
{
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot $name) -Destination "$fixture/scripts/$name"
}
[IO.File]::WriteAllText("$fixture/README.md", "# Fixture`n")
[IO.File]::WriteAllText("$fixture/src/main/java/Example.java", 'class Example {}')
[IO.File]::WriteAllText("$fixture/src/test/java/ExampleTest.java", 'class ExampleTest {}')
[IO.File]::WriteAllText("$fixture/src/custom/java/Custom.java", 'class Custom {}')
[IO.File]::WriteAllText("$fixture/src/custom/fmu-module.gradle", "sourceSet: 'named'")
git -C $fixture init -q
git -C $fixture add README.md src
git -C $fixture -c user.name=Fixture -c user.email=fixture@example.invalid -c commit.gpgSign=false -c core.hooksPath=.git/hooks commit -q -m fixture
[IO.File]::WriteAllText("$fixture/unowned.json", '{invalid')
[IO.File]::WriteAllText("$fixture/README.md", "# Fixture  `r`n")
$fake = @'
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Inputs)
$root = $PSScriptRoot
$Inputs | ConvertTo-Json | Set-Content "$root/fake-arguments.json"
$index = [Array]::IndexOf($Inputs, '--init-script')
if ($index -ge 0) { Copy-Item -LiteralPath $Inputs[$index + 1] -Destination "$root/observed-scope.gradle" }
foreach ($task in @($Inputs | Where-Object { $_ -match '^sonarlint[A-Z]' }))
{
    if (Test-Path "$root/omit-report") { continue }
    $null = New-Item -ItemType Directory -Force "$root/build/reports/sonarLint/$task"
    [IO.File]::WriteAllText("$root/build/reports/sonarLint/$task/$task.xml", '<checkstyle><file name="Example.java"><error line="1" source="fixture:rule" message="Fixture finding"/></file></checkstyle>')
}
'> Task :compileJava'
exit 0
'@
[IO.File]::WriteAllText("$fixture/fake-gradle.ps1", $fake)
[IO.File]::WriteAllText("$fixture/gradlew.bat", "@echo off`r`npowershell -NoProfile -File `"%~dp0fake-gradle.ps1`" %*`r`nexit /b %errorlevel%`r`n")
$runner = "$fixture/scripts/validate.ps1"
function Read-Plan([string]$Profile, [string]$Files, [string]$Tests = '*ExampleTest', [string]$Reason = '')
{
    $options = @('-Profile', $Profile, '-Task', 'Fixture', '-Files', $Files, '-Plan')
    if ($Tests) { $options += @('-Tests', $Tests) }
    if ($Reason) { $options += @('-NoTestsReason', $Reason) }
    $result = & powershell -NoProfile -File $runner @options
    if ($LASTEXITCODE -ne 0) { throw "Plan failed: $Profile" }
    return $result | ConvertFrom-Json
}
function Assert-Rejected([string[]]$Arguments)
{
    $ErrorActionPreference = 'Continue'
    & powershell -NoProfile -File $runner @Arguments 2>&1 | Out-Null
    $code = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($code -eq 0) { throw "Invalid profile selection accepted: $Arguments" }
}
$data = Read-Plan Data README.md
if ($data.gradle.Count -or $data.inspections) { throw 'Data scheduled heavyweight tools' }
$focused = Read-Plan Focused 'src/main/java/Example.java;src/test/java/ExampleTest.java'
if ($focused.gradle -notcontains 'compileJava' -or $focused.gradle -notcontains 'test' -or ($focused.gradle -match 'sonarlint|build') -or $focused.inspections) { throw 'Focused validation escalated or omitted required tasks' }
$analysis = Read-Plan Analysis src/main/java/Example.java
if ($analysis.gradle -notcontains 'sonarlintMain' -or $analysis.inspections) { throw 'Analysis selected incorrect tools' }
$deep = Read-Plan Deep src/main/java/Example.java
if (-not $deep.inspections -or $deep.inspectionBackend -ne 'Qodana') { throw 'Deep omitted Gradle-managed Qodana inspections' }
$release = Read-Plan Release README.md ''
if ($release.gradle -notcontains 'build') { throw 'Release omitted build' }
$custom = Read-Plan Focused src/custom/java/Custom.java
if ($custom.gradle -notcontains 'compileNamedJava') { throw 'Module descriptor source-set mapping was lost' }
foreach ($arguments in @(
    @('-Profile', 'Data', '-Task', 'Fixture', '-Files', '../outside.md', '-Plan'),
    @('-Profile', 'Data', '-Task', 'Fixture', '-Files', 'missing.md', '-Plan'),
    @('-Profile', 'Data', '-Task', 'Fixture', '-Files', 'src/main/java/Example.java', '-Plan'),
    @('-Profile', 'Focused', '-Task', 'Fixture', '-Files', 'src/main/java/Example.java', '-Plan')
)) { Assert-Rejected $arguments }
& powershell -NoProfile -File $runner -Profile Data -Task 'Fixture data' -Files README.md -Apply
if ($LASTEXITCODE -ne 0 -or (Test-Path "$fixture/fake-arguments.json")) { throw 'Data started Gradle or failed' }
if ([IO.File]::ReadAllText("$fixture/README.md") -cne "# Fixture`n" -or [IO.File]::ReadAllText("$fixture/unowned.json") -cne '{invalid') { throw 'Data failed to limit edits/checks to the owned file' }
$null = New-Item -ItemType Directory -Force "$fixture/src/main/resources"
[IO.File]::WriteAllText("$fixture/src/main/resources/fixture.json", '{"numbers": [ 1 , 2 ], "text": "[1 , 2]"}')
& powershell -NoProfile -File $runner -Profile Data -Task 'Fixture JSON' -Files src/main/resources/fixture.json -Apply
if ($LASTEXITCODE -ne 0 -or [IO.File]::ReadAllText("$fixture/src/main/resources/fixture.json") -cne '{"numbers": [1, 2], "text": "[1 , 2]"}') { throw 'Resource JSON formatting changed a string or lost the existing array convention' }
Assert-Rejected @('-Profile', 'Data', '-Task', 'Fixture', '-Files', 'unowned.json')
[IO.File]::WriteAllText("$fixture/empty.json", '')
Assert-Rejected @('-Profile', 'Data', '-Task', 'Fixture', '-Files', 'empty.json')
[IO.File]::WriteAllText("$fixture/broken.ps1", 'if (')
Assert-Rejected @('-Profile', 'Data', '-Task', 'Fixture', '-Files', 'broken.ps1')
$nested = "$fixture/src/nested"
$null = New-Item -ItemType Directory -Force $nested
git -C $nested init -q
[IO.File]::WriteAllText("$nested/definition.txt", "Name Fixture`n")
git -C $nested add definition.txt
git -C $nested -c user.name=Fixture -c user.email=fixture@example.invalid -c commit.gpgSign=false -c core.hooksPath=.git/hooks commit -q -m fixture
[IO.File]::WriteAllText("$nested/definition.txt", "Name Fixture  `n")
Assert-Rejected @('-Profile', 'Data', '-Task', 'Fixture', '-Files', 'src/nested/definition.txt')
& powershell -NoProfile -File $runner -Profile Focused -Task 'Fixture focused' -Files src/main/java/Example.java -Tests '*ExampleTest' -Apply
if ($LASTEXITCODE -ne 0) { throw 'Focused fake validation failed' }
$arguments = Get-Content -Raw "$fixture/fake-arguments.json" | ConvertFrom-Json
$scope = Get-Content -Raw "$fixture/observed-scope.gradle"
if ($arguments -notcontains '--tests' -or $arguments -notcontains '*ExampleTest' -or $arguments -contains 'build' -or $scope -match 'unowned.json' -or $scope -notmatch 'Example.java') { throw 'Focused execution lost scope or test filters' }
$envelopes = @(& powershell -NoProfile -File $runner -Profile Analysis -Task 'Fixture analysis' -Files src/main/java/Example.java -Tests '*ExampleTest' -OutputMode Json |
    ForEach-Object { $_ | ConvertFrom-Json })
if ($LASTEXITCODE -ne 0 -or $envelopes.Count -ne 2 -or $envelopes[0].analysis.Count -ne 1 -or $envelopes[0].analysis.Classification -ne 'unclassified') { throw 'Analysis lost findings in compact/JSON reporting' }
[IO.File]::WriteAllText("$fixture/scripts/qodana-inspect.ps1", @'
param([string]$Files, [string]$StateFile, [string]$RunId)
if (-not $StateFile -or -not $RunId) { throw 'Inspection supervisor context missing' }
[IO.File]::WriteAllText((Join-Path $PSScriptRoot '../qodana.called'), $Files)
'No Qodana problems in fixture.'
exit 0
'@)
$deepEnvelopes = @(& powershell -NoProfile -File $runner -Profile Deep -Task 'Fixture Deep' -Files src/main/java/Example.java -Tests '*ExampleTest' -OutputMode Json |
    ForEach-Object { $_ | ConvertFrom-Json })
if ($LASTEXITCODE -ne 0 -or $deepEnvelopes.Count -ne 3 -or $deepEnvelopes[2].tool -ne 'Qodana' -or
    (Get-Content -Raw "$fixture/qodana.called") -ne 'src/main/java/Example.java') { throw 'Deep did not route its exact scope to supervised Qodana' }
Remove-Item -LiteralPath "$fixture/build/reports/sonarLint/sonarlintMain/sonarlintMain.xml"
[IO.File]::WriteAllText("$fixture/omit-report", 'true')
$coordinator = "$fixture/scripts/run-validation.ps1"
$missingReport = & powershell -NoProfile -File $coordinator -OutputMode Json Gradle sonarlintMain | ConvertFrom-Json
if ($LASTEXITCODE -eq 0 -or $missingReport.state -ne 'failed' -or $missingReport.error -notmatch 'no report') { throw 'Missing analysis report was accepted as clean' }
Remove-Item -LiteralPath "$fixture/src/main/java/Example.java"
$deletion = Read-Plan Focused src/main/java/Example.java
if ($deletion.files -notcontains 'src/main/java/Example.java') { throw 'Tracked deletions lost validation coverage' }
"PASS: profile gating, plans, explicit tests, module mapping, path boundaries/deletions, Data without Gradle, and scoped fake execution. Evidence: $fixture"
