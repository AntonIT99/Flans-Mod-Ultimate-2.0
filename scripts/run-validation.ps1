<#
.SYNOPSIS
    Queues Gradle and headless IntelliJ validation within this checkout.

.DESCRIPTION
    A named mutex is specific to this checkout's absolute path. Separate worktrees can validate concurrently,
    while cooperating callers in one checkout wait rather than sharing build outputs or .idea mutations.
    Direct Gradle and inspection invocations bypass this queue. This does not isolate source edits.

.EXAMPLE
    powershell -NoProfile -File scripts/run-validation.ps1 Gradle compileNpcsJava test --tests 'com.wolffsmod.npcs.sim.*'
    powershell -NoProfile -File scripts/run-validation.ps1 Idea -Json
    powershell -NoProfile -File scripts/run-validation.ps1 -WaitSeconds 30 Gradle spotlessCheck
#>
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('Gradle', 'Idea')]
    [string]$Tool,
    [ValidateRange(0, 86400)]
    [int]$WaitSeconds = 600,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ToolArguments
)

$ErrorActionPreference = 'Stop'
$checkoutRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$lockKey = $checkoutRoot.Replace('\', '/').TrimEnd('/').ToLowerInvariant()
$hasher = [Security.Cryptography.SHA256]::Create()
try
{
    $lockHash = [BitConverter]::ToString($hasher.ComputeHash([Text.Encoding]::UTF8.GetBytes($lockKey))).Replace('-', '')
}
finally
{
    $hasher.Dispose()
}

$validationMutex = [Threading.Mutex]::new($false, "FlansModUltimateValidation-$lockHash")
$ownsLock = $false
$toolExitCode = 1
$waitClock = [Diagnostics.Stopwatch]::StartNew()
try
{
    while (-not $ownsLock)
    {
        try
        {
            $ownsLock = $validationMutex.WaitOne(0)
        }
        catch [Threading.AbandonedMutexException]
        {
            $ownsLock = $true
        }
        if ($ownsLock)
        {
            break
        }
        if ($waitClock.Elapsed.TotalSeconds -ge $WaitSeconds)
        {
            throw "Timed out waiting for validation in '$checkoutRoot'. Another caller owns the checkout; no tool was started."
        }
        if ($waitClock.ElapsedMilliseconds -lt 100)
        {
            [Console]::Error.WriteLine("Another validation owns '$checkoutRoot'; waiting up to $WaitSeconds seconds. Use a separate worktree to validate concurrently.")
        }
        Start-Sleep -Milliseconds 200
    }

    Push-Location $checkoutRoot
    try
    {
        if ($Tool -eq 'Gradle')
        {
            & (Join-Path $checkoutRoot 'gradlew.bat') @ToolArguments
        }
        else
        {
            & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'idea-inspect.ps1') @ToolArguments
        }
        $toolExitCode = $LASTEXITCODE
    }
    finally
    {
        Pop-Location
    }
}
finally
{
    if ($ownsLock)
    {
        $validationMutex.ReleaseMutex()
    }
    $validationMutex.Dispose()
}
exit $toolExitCode
