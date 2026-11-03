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
$stateIndex = [Array]::IndexOf($Inputs, '-StateFile')
if ($stateIndex -ge 0) { @{ phase = 'fixture inspection analysis'; quietSeconds = 2 } | ConvertTo-Json | Set-Content -LiteralPath $Inputs[$stateIndex + 1] }
'> Task :compileFake'
1..200 | ForEach-Object { "verbose fixture output $_" }
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
    Copy-Item -LiteralPath (Join-Path $repoRoot 'scripts/validation-process.ps1') -Destination (Join-Path $directory 'scripts/validation-process.ps1')
    [IO.File]::WriteAllText((Join-Path $directory 'fake-tool.ps1'), $fakeTool)
    [IO.File]::WriteAllText((Join-Path $directory 'scripts/qodana-inspect.ps1'), $fakeTool)
    [IO.File]::WriteAllText((Join-Path $directory 'gradlew.bat'), "@echo off`r`npowershell -NoProfile -ExecutionPolicy Bypass -File `"%~dp0fake-tool.ps1`" %*`r`nexit /b %errorlevel%`r`n")
}

function Start-Validation([string]$Checkout, [string]$Tool, [string]$Name, [int]$Delay, [int]$ExpectedExit = 0, [string]$Extra = '', [int]$Wait = 10, [int]$Timeout = 30, [string]$OutputMode = 'Compact')
{
    $coordinator = Join-Path $fixtureRoot "$Checkout/scripts/run-validation.ps1"
    $line = "-NoProfile -ExecutionPolicy Bypass -File `"$coordinator`" -Task `"Fixture $Name`" -OutputMode $OutputMode -WaitSeconds $Wait -TimeoutSeconds $Timeout -StatusSeconds 1 $Tool $Name $Delay $ExpectedExit $Extra"
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
if (@(Get-Content (Join-Path $resultsDir 'serialOwner.stdout')).Count -gt 45) { throw 'Compact output leaked full verbose logs' }
$jsonRun = Start-Validation first Gradle jsonResult 0 0 '' 10 30 Json
$null = Finish-Validation $jsonRun jsonResult
$envelope = Get-Content -Raw (Join-Path $resultsDir 'jsonResult.stdout') | ConvertFrom-Json
if ($envelope.state -ne 'passed' -or $envelope.task -ne 'Fixture jsonResult' -or -not (Test-Path $envelope.stdout)) { throw 'Invalid JSON run envelope' }
if (@(Get-Content $envelope.stdout).Count -lt 200) { throw 'Complete logs were not retained' }
Write-Output 'PASS: bounded compact output, JSON envelope, task labels, and retained full logs'
$phaseOwner = Start-Validation first Gradle phaseOwner 3500
Wait-Started phaseOwner
Start-Sleep -Milliseconds 1500
$controlScript = Join-Path $fixtureRoot 'first/scripts/run-validation.ps1'
$phaseStatus = & powershell -NoProfile -File $controlScript -OutputMode Json Status | ConvertFrom-Json
if ($phaseStatus.runtime.phase -ne ':compileFake' -or $phaseStatus.runtime.quietSeconds -lt 1) { throw 'Status did not expose the last Gradle task and quiet-output age' }
$null = Finish-Validation $phaseOwner phaseOwner
Write-Output 'PASS: Status exposes observed Gradle phase and output age without claiming progress'
$inspector = Start-Validation first Qodana inspectorProgress 2500
$null = Finish-Validation $inspector inspectorProgress
if ((Get-Content -Raw (Join-Path $resultsDir 'inspectorProgress.stderr')) -notmatch 'phase fixture inspection analysis') { throw 'Inspection heartbeat lost the actual nested scan phase' }
Write-Output 'PASS: Qodana heartbeats use delegated scan progress instead of outer supervisor liveness'

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

# A child launched by the fake tool must be contained too, including on abrupt supervisor termination.
$childTool = @'
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Inputs)
$name = $Inputs[0]
$child = Start-Process powershell -ArgumentList '-NoProfile -Command Start-Sleep -Seconds 120' -PassThru -WindowStyle Hidden
@{ parent = $PID; child = $child.Id } | ConvertTo-Json | Set-Content (Join-Path $env:FMU_VALIDATION_TEST_LOG "$name.pids")
[IO.File]::WriteAllText((Join-Path $env:FMU_VALIDATION_TEST_LOG "$name.started"), "$PID")
Start-Sleep -Seconds 120
'@
[IO.File]::WriteAllText((Join-Path $fixtureRoot 'first/fake-tool.ps1'), $childTool)
foreach ($mode in @('executionTimeout', 'cancelledSupervisor', 'requestedCancel'))
{
    $executionLimit = if ($mode -eq 'requestedCancel') { 30 } else { 5 }
    $owner = Start-Validation first Gradle $mode 0 0 '' 10 $executionLimit
    Wait-Started $mode
    $ownedPids = Get-Content -Raw (Join-Path $resultsDir "$mode.pids") | ConvertFrom-Json
    $unrelated = Start-Validation second Gradle "unrelated-$mode" 9000
    Wait-Started "unrelated-$mode"
    if ($mode -eq 'cancelledSupervisor') { Stop-Process -Id $owner.Id -Force }
    if ($mode -eq 'requestedCancel')
    {
        $controlScript = Join-Path $fixtureRoot 'first/scripts/run-validation.ps1'
        $status = & powershell -NoProfile -File $controlScript -OutputMode Json Status | ConvertFrom-Json
        if ($status.state -ne 'running' -or $status.task -ne 'Fixture requestedCancel' -or -not $status.runtime.workerPid) { throw 'Status omitted live task/process details' }
        $ErrorActionPreference = 'Continue'
        & powershell -NoProfile -File $controlScript -RunId wrong-run Cancel 2>&1 | Out-Null
        $wrongExit = $LASTEXITCODE
        $ErrorActionPreference = 'Stop'
        if ($wrongExit -eq 0 -or $owner.HasExited) { throw 'Cancellation accepted a wrong run ID' }
        & powershell -NoProfile -File $controlScript -RunId $status.runId Cancel | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Requested cancellation failed' }
    }
    if (-not $owner.WaitForExit(20000)) { throw "$mode did not terminate" }
    $owner.Refresh()
    if ($owner.ExitCode -eq 0) { throw "$mode incorrectly succeeded" }
    $cleanupClock = [Diagnostics.Stopwatch]::StartNew()
    while (@(Get-Process -Id $ownedPids.parent, $ownedPids.child -ErrorAction SilentlyContinue).Count -gt 0)
    {
        if ($cleanupClock.Elapsed.TotalSeconds -gt 10) { throw "$mode left owned children running" }
        Start-Sleep -Milliseconds 100
    }
    if ($unrelated.HasExited) { throw "$mode interrupted unrelated validation" }
    $null = Finish-Validation $unrelated "unrelated-$mode"
    if ($mode -eq 'executionTimeout')
    {
        $diagnostic = Get-Content -Raw (Join-Path $resultsDir "$mode.stderr")
        if ($diagnostic -notmatch 'timed out' -or $diagnostic -notmatch 'running: worker PID') { throw 'Execution timeout lost diagnostics or heartbeat' }
    }
    if ($mode -in @('executionTimeout', 'requestedCancel'))
    {
        # Completed runs keep their outcome independently of the removed live owner record.
        $completed = @(Get-ChildItem (Join-Path $fixtureRoot 'first/.gradle/validation-runs') -Filter result.json -Recurse |
            ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json } | Where-Object { $_.task -eq "Fixture $mode" })
        $expectedState = if ($mode -eq 'executionTimeout') { 'timed-out' } else { 'cancelled' }
        if ($completed.Count -ne 1 -or $completed[0].state -ne $expectedState) { throw 'Termination outcome was not recorded distinctly' }
    }
    [IO.File]::WriteAllText((Join-Path $fixtureRoot 'first/fake-tool.ps1'), $fakeTool)
    $recovered = Start-Validation first Gradle "recovered-$mode" 0
    $null = Finish-Validation $recovered "recovered-$mode"
    [IO.File]::WriteAllText((Join-Path $fixtureRoot 'first/fake-tool.ps1'), $childTool)
}
[IO.File]::WriteAllText((Join-Path $fixtureRoot 'first/fake-tool.ps1'), $fakeTool)
Write-Output 'PASS: timeouts, abrupt termination and requested cancellation stop owned descendants, preserve unrelated runs and recover; Status and exact run IDs work'

$hookRoot = Join-Path $fixtureRoot 'first'
Copy-Item -LiteralPath (Join-Path $repoRoot 'scripts/idea-inspect-hook.ps1') -Destination (Join-Path $hookRoot 'scripts/idea-inspect-hook.ps1')
git -C $hookRoot init -q
[IO.File]::WriteAllText((Join-Path $hookRoot 'baseline.txt'), 'fixture')
git -C $hookRoot add baseline.txt
git -C $hookRoot -c user.name=Fixture -c user.email=fixture@example.invalid -c commit.gpgSign=false -c core.hooksPath=.git/hooks commit -q -m fixture
[IO.File]::WriteAllText((Join-Path $hookRoot 'Example.java'), 'class Example {}')
[IO.File]::WriteAllText((Join-Path $hookRoot 'scripts/qodana-inspect.ps1'), @'
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
if ((Get-Content -Raw (Join-Path $resultsDir 'serialFollower.stderr')) -notmatch 'Owner PID.*serialOwner') { throw 'Queue status omitted owner information' }
Write-Output "Evidence: $resultsDir"
