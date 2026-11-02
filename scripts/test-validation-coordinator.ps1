# Exercises the validation queue with fake Gradle/IDE tools in .gradle; no Minecraft, Gradle build, or IDE is launched.
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$fixtureRoot = Join-Path (Join-Path $repoRoot '.gradle') ('validation-coordinator-' + [Guid]::NewGuid().ToString('N'))
$resultsDir = Join-Path $fixtureRoot 'results'
New-Item -ItemType Directory -Force $resultsDir | Out-Null
$env:FMU_VALIDATION_TEST_LOG = $resultsDir
$fakeTool = @'
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Inputs)
$ErrorActionPreference = 'Stop'
$name = $Inputs[0]
$started = [DateTime]::UtcNow.Ticks
[IO.File]::WriteAllText((Join-Path $env:FMU_VALIDATION_TEST_LOG "$name.started"), "$PID")
Start-Sleep -Milliseconds ([int]$Inputs[1])
$ended = [DateTime]::UtcNow.Ticks
@{ name = $name; started = $started; ended = $ended; inputs = $Inputs } | ConvertTo-Json -Compress |
    Set-Content -Encoding UTF8 (Join-Path $env:FMU_VALIDATION_TEST_LOG "$name.json")
exit ([int]$Inputs[2])
'@
foreach ($checkout in @('first', 'second'))
{
    $directory = Join-Path $fixtureRoot $checkout
    New-Item -ItemType Directory -Force (Join-Path $directory 'scripts') | Out-Null
    Copy-Item -LiteralPath (Join-Path $repoRoot 'scripts/run-validation.ps1') -Destination (Join-Path $directory 'scripts/run-validation.ps1')
    [IO.File]::WriteAllText((Join-Path $directory 'fake-tool.ps1'), $fakeTool)
    [IO.File]::WriteAllText((Join-Path $directory 'scripts/idea-inspect.ps1'), $fakeTool)
    [IO.File]::WriteAllText((Join-Path $directory 'gradlew.bat'), "@echo off`r`npowershell -NoProfile -ExecutionPolicy Bypass -File `"%~dp0fake-tool.ps1`" %*`r`nexit /b %errorlevel%`r`n")
}

function Start-Validation([string]$Checkout, [string]$Tool, [string]$Name, [int]$Delay, [int]$ExpectedExit = 0, [string]$Extra = '', [int]$Wait = 10)
{
    $coordinator = Join-Path $fixtureRoot "$Checkout/scripts/run-validation.ps1"
    $line = "-NoProfile -ExecutionPolicy Bypass -File `"$coordinator`" -WaitSeconds $Wait $Tool $Name $Delay $ExpectedExit $Extra"
    $process = Start-Process powershell -ArgumentList $line -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $resultsDir "$Name.stdout") -RedirectStandardError (Join-Path $resultsDir "$Name.stderr")
    $null = $process.Handle
    return $process
}

function Wait-Started([string]$Name)
{
    $clock = [Diagnostics.Stopwatch]::StartNew()
    while (-not (Test-Path (Join-Path $resultsDir "$Name.started")))
    {
        if ($clock.Elapsed.TotalSeconds -gt 15) { throw "Tool '$Name' did not start" }
        Start-Sleep -Milliseconds 100
    }
}

function Finish-Validation($Process, [string]$Name, [int]$ExpectedExit = 0)
{
    if (-not $Process.WaitForExit(20000)) { throw "Coordinator '$Name' did not finish" }
    $Process.Refresh()
    if ($Process.ExitCode -ne $ExpectedExit)
    {
        Get-Content (Join-Path $resultsDir "$Name.stderr")
        throw "Coordinator '$Name' exited $($Process.ExitCode), expected $ExpectedExit"
    }
    return Get-Content -Raw (Join-Path $resultsDir "$Name.json") | ConvertFrom-Json
}

$owner = Start-Validation first Gradle serialOwner 2500 7 '--tests "two words" -Pdemo=value'
Wait-Started serialOwner
$follower = Start-Validation first Idea serialFollower 0 0 '-Json'
$ownerResult = Finish-Validation $owner serialOwner 7
$followerResult = Finish-Validation $follower serialFollower
if ($followerResult.started -lt $ownerResult.ended) { throw 'Gradle and Idea overlapped within one checkout' }
if (($ownerResult.inputs[3..5] -join '|') -ne '--tests|two words|-Pdemo=value') { throw "Gradle options were changed: $($ownerResult.inputs -join '|')" }
if ($followerResult.inputs[3] -ne '-Json') { throw 'Inspection options were not forwarded' }
Write-Output 'PASS: same-checkout serialization, argument forwarding, and failure exit propagation/release'

$owner = Start-Validation first Gradle timeoutOwner 3500
Wait-Started timeoutOwner
$blocked = Start-Validation first Gradle timedOut 0 0 '' 0
if (-not $blocked.WaitForExit(15000)) { throw 'Queue timeout did not finish' }
$blocked.Refresh()
if ($blocked.ExitCode -eq 0 -or (Test-Path (Join-Path $resultsDir 'timedOut.started'))) { throw 'Timed-out caller started its tool' }
$null = Finish-Validation $owner timeoutOwner
$recovered = Start-Validation first Gradle afterTimeout 0
$null = Finish-Validation $recovered afterTimeout
Write-Output 'PASS: queue timeout starts no tool and a later invocation recovers'

$owner = Start-Validation first Gradle parallelOwner 5000
Wait-Started parallelOwner
$independent = Start-Validation second Gradle parallelOther 0
$otherResult = Finish-Validation $independent parallelOther
$ownerResult = Finish-Validation $owner parallelOwner
if ($otherResult.started -ge $ownerResult.ended) { throw 'Different checkouts unnecessarily shared a queue' }
Write-Output 'PASS: different checkouts can validate concurrently'

$hookRoot = Join-Path $fixtureRoot 'first'
Copy-Item -LiteralPath (Join-Path $repoRoot 'scripts/idea-inspect-hook.ps1') -Destination (Join-Path $hookRoot 'scripts/idea-inspect-hook.ps1')
git -C $hookRoot init -q
[IO.File]::WriteAllText((Join-Path $hookRoot 'baseline.txt'), 'fixture')
git -C $hookRoot add baseline.txt
git -C $hookRoot -c user.name=Fixture -c user.email=fixture@example.invalid -c commit.gpgSign=false -c core.hooksPath=.git/hooks commit -q -m fixture
[IO.File]::WriteAllText((Join-Path $hookRoot 'Example.java'), 'class Example {}')
[IO.File]::WriteAllText((Join-Path $hookRoot 'scripts/idea-inspect.ps1'), @'
param([string]$MinSeverity)
[Console]::Error.WriteLine('Fixture diagnostic on stderr')
'Example.java:1: [WARNING] FakeCheck: fixture warning'
exit 0
'@)
$payloadFile = Join-Path $fixtureRoot 'hook-payload.json'
[IO.File]::WriteAllText($payloadFile, '{"session_id":"validation-coordinator-test"}')
$owner = Start-Validation first Gradle hookOwner 3500
Wait-Started hookOwner
$hookPath = Join-Path $hookRoot 'scripts/idea-inspect-hook.ps1'
$hookProcess = Start-Process powershell -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$hookPath`"" -PassThru -WindowStyle Hidden -RedirectStandardInput $payloadFile -RedirectStandardOutput (Join-Path $resultsDir 'hook.stdout') -RedirectStandardError (Join-Path $resultsDir 'hook.stderr')
$null = $hookProcess.Handle
$null = Finish-Validation $owner hookOwner
if (-not $hookProcess.WaitForExit(20000)) { throw 'Hook did not finish' }
$hookProcess.Refresh()
if ($hookProcess.ExitCode -ne 0) { Get-Content (Join-Path $resultsDir 'hook.stderr'); throw 'Hook failed while queued' }
$hookResult = Get-Content -Raw (Join-Path $resultsDir 'hook.stdout') | ConvertFrom-Json
if ($hookResult.decision -ne 'block' -or $hookResult.reason -notmatch 'FakeCheck') { throw 'Hook lost its inspection result after queueing' }
Write-Output 'PASS: queued Stop hook preserves warning results despite native stderr notices'
Write-Output "Evidence: $resultsDir"
