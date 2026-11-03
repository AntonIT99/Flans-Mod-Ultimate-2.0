<#
.SYNOPSIS
    Queues validation, keeps complete logs, and reports bounded results for coding agents.
.EXAMPLE
    powershell -NoProfile -File scripts/run-validation.ps1 -Task 'Banner edit' Gradle compileJava
    powershell -NoProfile -File scripts/run-validation.ps1 Status
    powershell -NoProfile -File scripts/run-validation.ps1 -RunId <id from Status> Cancel
#>
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('Gradle', 'Qodana', 'Idea', 'Data', 'Status', 'Cancel')][string]$Tool,
    [ValidateRange(0, 86400)][int]$WaitSeconds = 600,
    [ValidateRange(1, 86400)][int]$TimeoutSeconds = 1800,
    [ValidateRange(1, 3600)][int]$StatusSeconds = 30,
    [string]$Task = 'Validation',
    [string]$RunId,
    [ValidateSet('Compact', 'Full', 'Json')][string]$OutputMode = 'Compact',
    [Parameter(ValueFromRemainingArguments = $true)][string[]]$ToolArguments
)

$ErrorActionPreference = 'Stop'
$checkoutRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$stateDirectory = Join-Path $checkoutRoot '.gradle/validation'
$ownerFile = Join-Path $stateDirectory 'owner.json'
if ($Tool -in @('Status', 'Cancel'))
{
    if (-not (Test-Path -LiteralPath $ownerFile))
    {
        if ($Tool -eq 'Cancel') { throw 'No validation is running.' }
        if ($OutputMode -eq 'Json') { '{"state":"idle"}' } else { 'Validation idle.' }
        exit 0
    }
    $owner = Get-Content -LiteralPath $ownerFile -Raw | ConvertFrom-Json
    # The named event, rather than a possibly recycled PID, identifies the exact invocation.
    try { $cancelEvent = [Threading.EventWaitHandle]::OpenExisting($owner.cancelEvent) }
    catch
    {
        if ($Tool -eq 'Cancel') { throw 'That validation is no longer running; no process was stopped.' }
        if ($OutputMode -eq 'Json') { '{"state":"inactive"}' } else { 'Validation inactive (stale owner record).' }
        exit 0
    }
    try
    {
        if ($Tool -eq 'Cancel')
        {
            if (-not $RunId -or $RunId -ne $owner.runId) { throw 'Cancel requires the exact -RunId reported by Status; no process was stopped.' }
            $null = $cancelEvent.Set()
            "Cancellation requested for $($owner.task) [$RunId]; the supervisor will stop its descendants before releasing the queue."
        }
        else
        {
            $runtime = $null
            try { $runtime = Get-Content -LiteralPath $owner.runtime -Raw | ConvertFrom-Json } catch { }
            $status = @{ state = 'running'; runId = $owner.runId; task = $owner.task; pid = $owner.pid; command = $owner.command; started = $owner.started; runtime = $runtime }
            if ($OutputMode -eq 'Json') { $status | ConvertTo-Json -Depth 4 -Compress }
            else { "$($owner.task) [$($owner.runId)] | $($owner.command) | PID $($owner.pid) | phase $($runtime.phase) | elapsed $($runtime.elapsedSeconds)s | quiet $($runtime.quietSeconds)s (liveness only)" }
        }
    }
    finally { $cancelEvent.Dispose() }
    exit 0
}

if ($Tool -in @('Qodana', 'Idea') -and -not $PSBoundParameters.ContainsKey('TimeoutSeconds')) { $TimeoutSeconds = 1800 }
if ($Tool -eq 'Data' -and -not $PSBoundParameters.ContainsKey('TimeoutSeconds')) { $TimeoutSeconds = 120 }
# Preserve the existing raw JSON inspection interface; -OutputMode Json instead returns a run envelope.
if ($Tool -in @('Qodana', 'Idea') -and $ToolArguments -contains '-Json' -and -not $PSBoundParameters.ContainsKey('OutputMode')) { $OutputMode = 'Full' }
. (Join-Path $PSScriptRoot 'validation-process.ps1')
$lockKey = $checkoutRoot.Replace('\', '/').TrimEnd('/').ToLowerInvariant()
$hasher = [Security.Cryptography.SHA256]::Create()
try { $lockHash = [BitConverter]::ToString($hasher.ComputeHash([Text.Encoding]::UTF8.GetBytes($lockKey))).Replace('-', '') }
finally { $hasher.Dispose() }
$validationMutex = [Threading.Mutex]::new($false, "FlansModUltimateValidation-$lockHash")
$ownsLock = $false
$cancelEvent = $null
$toolExitCode = 1
$waitClock = [Diagnostics.Stopwatch]::StartNew()
$nextWaitStatus = 0
try
{
    while (-not $ownsLock)
    {
        try { $ownsLock = $validationMutex.WaitOne(0) }
        catch [Threading.AbandonedMutexException] { $ownsLock = $true }
        if ($ownsLock) { break }
        if ($waitClock.Elapsed.TotalSeconds -ge $WaitSeconds) { throw "Timed out waiting for validation in '$checkoutRoot'; no tool was started." }
        if ($waitClock.Elapsed.TotalSeconds -ge $nextWaitStatus)
        {
            $ownerDescription = 'Owner details unavailable.'
            try
            {
                $owner = Get-Content -LiteralPath $ownerFile -Raw | ConvertFrom-Json
                $elapsed = [int]([DateTime]::UtcNow - [DateTime]::Parse($owner.started).ToUniversalTime()).TotalSeconds
                $ownerDescription = "Owner PID $($owner.pid), task $($owner.task), command $($owner.command), running ${elapsed}s."
            }
            catch { }
            [Console]::Error.WriteLine("$Task waiting: $([int]$waitClock.Elapsed.TotalSeconds)s / ${WaitSeconds}s. $ownerDescription")
            $nextWaitStatus += $StatusSeconds
        }
        Start-Sleep -Milliseconds 200
    }
    Push-Location $checkoutRoot
    try
    {
        $RunId = [Guid]::NewGuid().ToString('N')
        $runDirectory = Join-Path $checkoutRoot ".gradle/validation-runs/$RunId"
        $null = New-Item -ItemType Directory -Force $stateDirectory, $runDirectory
        $stdout = Join-Path $runDirectory 'stdout.log'
        $stderr = Join-Path $runDirectory 'stderr.log'
        $runtime = Join-Path $runDirectory 'runtime.json'
        $cancelName = "FmuValidationCancel-$RunId"
        $cancelEvent = [Threading.EventWaitHandle]::new($false, [Threading.EventResetMode]::ManualReset, $cancelName)
        $ownerTemporary = "$ownerFile.$RunId.tmp"
        @{ runId = $RunId; task = $Task; pid = $PID; command = "$Tool $($ToolArguments -join ' ')"; started = [DateTime]::UtcNow.ToString('o'); runtime = $runtime; cancelEvent = $cancelName } |
            ConvertTo-Json -Compress | Set-Content -LiteralPath $ownerTemporary -Encoding UTF8
        Move-Item -LiteralPath $ownerTemporary -Destination $ownerFile -Force
        $runClock = [Diagnostics.Stopwatch]::StartNew()
        $failure = $null
        $terminationState = 'failed'
        try
        {
            $processOptions = @{ TimeoutSeconds = $TimeoutSeconds; StatusSeconds = $StatusSeconds; Label = "$Task [$RunId] | $Tool"; StandardOutput = $stdout; StandardError = $stderr; QuietOutput = ($OutputMode -ne 'Full'); StateFile = $runtime; CancellationEvent = $cancelEvent }
            if ($Tool -eq 'Gradle')
            {
                if ($ToolArguments -contains '--daemon') { throw 'Queued validation uses --no-daemon for safe cancellation. Remove --daemon.' }
                $toolExitCode = Invoke-ValidationProcess -FilePath (Join-Path $checkoutRoot 'gradlew.bat') -Arguments (@($ToolArguments) + @('--no-daemon', '--console=plain')) @processOptions
            }
            elseif ($Tool -in @('Qodana', 'Idea'))
            {
                if ($ToolArguments -contains '-StateFile' -or $ToolArguments -contains '-RunId') { throw 'StateFile and RunId are reserved for the inspection supervisor.' }
                @{ phase = 'inspection worker setup'; elapsedSeconds = 0; quietSeconds = 0 } | ConvertTo-Json -Compress | Set-Content -LiteralPath $runtime -Encoding UTF8
                # The inner Gradle supervisor owns actual phase/output timestamps, rather than outer heartbeat traffic.
                $processOptions.StateFile = ''
                $processOptions.ProgressFile = $runtime
                $inspectionArguments = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $PSScriptRoot 'qodana-inspect.ps1')) + $ToolArguments + @('-StateFile', $runtime, '-RunId', $RunId)
                $toolExitCode = Invoke-ValidationProcess -FilePath 'powershell' -Arguments $inspectionArguments @processOptions
            }
            else
            {
                $toolExitCode = Invoke-ValidationProcess -FilePath 'powershell' -Arguments (@('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', (Join-Path $PSScriptRoot 'validation-data.ps1')) + $ToolArguments) @processOptions
            }
        }
        catch
        {
            $failure = $_.Exception.Message
            $toolExitCode = 1
            if ($_.Exception -is [OperationCanceledException]) { $terminationState = 'cancelled'; $toolExitCode = 130 }
            elseif ($_.Exception -is [TimeoutException]) { $terminationState = 'timed-out'; $toolExitCode = 124 }
        }
        $findings = $null
        $analysisTasks = @($ToolArguments | Where-Object { $_ -match '^sonarlint[A-Z][A-Za-z0-9]*$' } | Sort-Object -Unique)
        if ($Tool -eq 'Gradle' -and $toolExitCode -eq 0 -and $analysisTasks.Count)
        {
            try
            {
                . (Join-Path $PSScriptRoot 'validation-reports.ps1')
                $completedTasks = @($analysisTasks | Where-Object {
                    -not (Select-String -LiteralPath $stdout -Pattern ('^> Task :' + [regex]::Escape($_) + ' (NO-SOURCE|SKIPPED)$') -Quiet)
                })
                if ($completedTasks.Count) { $findings = Get-ValidationFindings -Root $checkoutRoot -Tasks $completedTasks }
            }
            catch { $failure = $_.Exception.Message; $toolExitCode = 1 }
        }
        $diagnostics = @()
        if ($toolExitCode -ne 0)
        {
            $diagnostics = @(@(Get-Content -LiteralPath $stdout -Tail 25 -ErrorAction SilentlyContinue) + @(Get-Content -LiteralPath $stderr -Tail 15 -ErrorAction SilentlyContinue) |
                Where-Object { $_ } | Select-Object -Unique | ForEach-Object { if ($_.Length -gt 600) { $_.Substring(0, 600) + '...' } else { $_ } })
        }
        elseif ($Tool -in @('Qodana', 'Idea') -and $OutputMode -ne 'Full')
        {
            $diagnostics = @(Get-Content -LiteralPath $stdout -TotalCount 30)
            $reportSummary = Get-Content -LiteralPath $stdout -Tail 1
            if ($reportSummary -and $diagnostics -notcontains $reportSummary) { $diagnostics += $reportSummary }
        }
        $result = @{ runId = $RunId; task = $Task; tool = $Tool; command = @($ToolArguments); state = $(if ($toolExitCode -eq 0) { 'passed' } else { $terminationState }); exitCode = $toolExitCode; elapsedSeconds = [Math]::Round($runClock.Elapsed.TotalSeconds, 1); stdout = $stdout; stderr = $stderr; error = $failure; diagnostics = $diagnostics; analysis = $findings }
        $result | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $runDirectory 'result.json') -Encoding UTF8
        if ($OutputMode -eq 'Json') { $result | ConvertTo-Json -Depth 5 -Compress }
        else
        {
            $summary = "$Task [$RunId] | $Tool $($result.state) | $($result.elapsedSeconds)s | logs: $runDirectory"
            if ($OutputMode -eq 'Full') { [Console]::Error.WriteLine($summary) }
            else
            {
                $summary
                if ($failure) { [Console]::Error.WriteLine($failure) }
                $diagnostics
                if ($findings)
                {
                    "SonarLint: $($findings.Count) unique findings (unclassified; review introduced vs existing before fixing)."
                    $findings.Findings | ForEach-Object { "$($_.File):$($_.Line): $($_.Rule): $($_.Message)" }
                    "Showing at most 20 findings; complete reports: $($findings.Reports -join '; ')"
                }
                if ($diagnostics.Count) { 'Excerpt only; complete output is in the logs.' }
            }
        }
    }
    finally { Pop-Location }
}
finally
{
    if ($cancelEvent) { $cancelEvent.Dispose() }
    if ($ownsLock)
    {
        Remove-Item -LiteralPath $ownerFile -ErrorAction SilentlyContinue
        $validationMutex.ReleaseMutex()
    }
    $validationMutex.Dispose()
}
exit $toolExitCode
