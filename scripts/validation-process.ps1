# Shared process supervision. The worker waits until it belongs to a Windows job before launching any tool.
param([string]$WorkerInput, [string]$WorkerGate)

if ($WorkerInput)
{
    $ErrorActionPreference = 'Stop'
    $gate = [Threading.EventWaitHandle]::OpenExisting($WorkerGate)
    try
    {
        if (-not $gate.WaitOne(30000)) { throw 'Validation supervisor did not release the worker.' }
        $invocation = Import-Clixml -LiteralPath $WorkerInput
        Set-Location -LiteralPath $invocation.Directory
        $nativeArguments = $invocation.Arguments
        & $invocation.FilePath @nativeArguments
        exit $LASTEXITCODE
    }
    finally { $gate.Dispose() }
}

if (-not ('FmuValidationJob' -as [type]))
{
    Add-Type -TypeDefinition @'
using System;
using System.ComponentModel;
using System.Runtime.InteropServices;
public sealed class FmuValidationJob : IDisposable
{
    [StructLayout(LayoutKind.Sequential)] struct BasicLimits
    {
        public long ProcessTime, JobTime;
        public uint Flags;
        public UIntPtr MinWorkingSet, MaxWorkingSet;
        public uint ActiveLimit;
        public UIntPtr Affinity;
        public uint Priority, Scheduling;
    }
    [StructLayout(LayoutKind.Sequential)] struct ExtendedLimits
    {
        public BasicLimits Basic;
        public ulong ReadOperations, WriteOperations, OtherOperations, ReadBytes, WriteBytes, OtherBytes;
        public UIntPtr ProcessMemory, JobMemory, PeakProcessMemory, PeakJobMemory;
    }
    [StructLayout(LayoutKind.Sequential)] struct Accounting
    {
        public long UserTime, KernelTime, PeriodUserTime, PeriodKernelTime;
        public uint PageFaults, TotalProcesses, ActiveProcesses, TerminatedProcesses;
    }
    [DllImport("kernel32.dll", SetLastError=true)] static extern IntPtr CreateJobObject(IntPtr attributes, string name);
    [DllImport("kernel32.dll", SetLastError=true)] static extern bool SetInformationJobObject(IntPtr job, int kind, ref ExtendedLimits limits, int size);
    [DllImport("kernel32.dll", SetLastError=true)] static extern bool QueryInformationJobObject(IntPtr job, int kind, out Accounting info, int size, IntPtr length);
    [DllImport("kernel32.dll", SetLastError=true)] static extern bool AssignProcessToJobObject(IntPtr job, IntPtr process);
    [DllImport("kernel32.dll", SetLastError=true)] static extern bool TerminateJobObject(IntPtr job, uint code);
    [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr handle);
    IntPtr handle;
    public FmuValidationJob()
    {
        handle = CreateJobObject(IntPtr.Zero, null);
        if (handle == IntPtr.Zero) throw new Win32Exception();
        var limits = new ExtendedLimits();
        limits.Basic.Flags = 0x2000; // JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
        if (!SetInformationJobObject(handle, 9, ref limits, Marshal.SizeOf(limits)))
        {
            int error = Marshal.GetLastWin32Error();
            Dispose();
            throw new Win32Exception(error);
        }
    }
    public void Assign(IntPtr process)
    {
        if (!AssignProcessToJobObject(handle, process)) throw new Win32Exception();
    }
    public void Stop()
    {
        if (!TerminateJobObject(handle, 124)) throw new Win32Exception();
        var clock = System.Diagnostics.Stopwatch.StartNew();
        while (true)
        {
            Accounting info;
            if (!QueryInformationJobObject(handle, 1, out info, Marshal.SizeOf(typeof(Accounting)), IntPtr.Zero)) throw new Win32Exception();
            if (info.ActiveProcesses == 0) return;
            if (clock.Elapsed.TotalSeconds > 15) throw new TimeoutException("Validation process cleanup did not finish.");
            System.Threading.Thread.Sleep(50);
        }
    }
    public void Dispose()
    {
        if (handle != IntPtr.Zero) { CloseHandle(handle); handle = IntPtr.Zero; }
    }
}
'@
}

function Invoke-ValidationProcess
{
    param(
        [string]$FilePath, [string[]]$Arguments,
        [ValidateRange(1, 86400)][int]$TimeoutSeconds,
        [ValidateRange(1, 3600)][int]$StatusSeconds = 30,
        [string]$Label = 'Validation', [string]$StandardOutput, [string]$StandardError,
        [switch]$QuietOutput,
        [string]$StateFile,
        [string]$ProgressFile,
        [Threading.EventWaitHandle]$CancellationEvent
    )
    $runDirectory = Join-Path $PSScriptRoot ('../.gradle/validation-runs/' + [Guid]::NewGuid().ToString('N'))
    $null = New-Item -ItemType Directory -Force $runDirectory
    $inputFile = Join-Path $runDirectory 'invocation.xml'
    @{ FilePath = $FilePath; Arguments = $Arguments; Directory = (Get-Location).Path } | Export-Clixml -LiteralPath $inputFile
    if (-not $StandardOutput) { $StandardOutput = Join-Path $runDirectory 'stdout.log' }
    if (-not $StandardError) { $StandardError = Join-Path $runDirectory 'stderr.log' }
    $gateName = 'FmuValidation-' + [Guid]::NewGuid().ToString('N')
    $gate = [Threading.EventWaitHandle]::new($false, [Threading.EventResetMode]::ManualReset, $gateName)
    $job = [FmuValidationJob]::new()
    $process = $null
    $assigned = $false
    $readers = @()
    try
    {
        $workerScript = Join-Path $PSScriptRoot 'validation-process.ps1'
        $process = Start-Process powershell -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$workerScript`" -WorkerInput `"$inputFile`" -WorkerGate $gateName" -PassThru -WindowStyle Hidden -RedirectStandardOutput $StandardOutput -RedirectStandardError $StandardError
        $job.Assign($process.Handle)
        $assigned = $true
        $null = $gate.Set()
        foreach ($logPath in @($StandardOutput, $StandardError))
        {
            $stream = [IO.File]::Open($logPath, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::ReadWrite)
            $readers += [IO.StreamReader]::new($stream)
        }
        [Console]::Error.WriteLine("$Label started: worker PID $($process.Id), timeout ${TimeoutSeconds}s.")
        $clock = [Diagnostics.Stopwatch]::StartNew()
        $lastOutput = 0
        $phase = 'starting'
        $nextState = 0
        $nextStatus = $StatusSeconds
        while ($true)
        {
            for ($i = 0; $i -lt $readers.Count; $i++)
            {
                $chunk = $readers[$i].ReadToEnd()
                if ($chunk)
                {
                    $lastOutput = $clock.Elapsed.TotalSeconds
                    $taskMatches = [regex]::Matches($chunk, '(?m)^(?:> Task (?<phase>[^\r\n]+)|\[(?:IDE|Gradle)\]:\s*(?<phase>[^\r\n]+)|(?<phase>(?:Downloading|Keep running) [^\r\n]+))')
                    if ($taskMatches.Count) { $phase = $taskMatches[$taskMatches.Count - 1].Groups['phase'].Value.Trim() }
                    if ($phase.Length -gt 180) { $phase = $phase.Substring(0, 180) }
                }
                if ($chunk -and -not $QuietOutput) { if ($i -eq 0) { [Console]::Out.Write($chunk) } else { [Console]::Error.Write($chunk) } }
            }
            if ($process.HasExited) { break }
            if ($CancellationEvent -and $CancellationEvent.WaitOne(0)) { throw [OperationCanceledException]::new("$Label cancelled; validation is incomplete. Logs: $StandardOutput ; $StandardError") }
            if ($clock.Elapsed.TotalSeconds -ge $TimeoutSeconds) { throw [TimeoutException]::new("$Label timed out after ${TimeoutSeconds}s; validation is incomplete. Logs: $StandardOutput ; $StandardError") }
            if ($StateFile -and $clock.Elapsed.TotalSeconds -ge $nextState)
            {
                $snapshot = @{ workerPid = $process.Id; elapsedSeconds = [int]$clock.Elapsed.TotalSeconds; phase = $phase; quietSeconds = [int]($clock.Elapsed.TotalSeconds - $lastOutput); stdout = $StandardOutput; stderr = $StandardError }
                $temporaryState = "$StateFile.$PID.tmp"
                $snapshot | ConvertTo-Json -Compress | Set-Content -LiteralPath $temporaryState -Encoding UTF8
                Move-Item -LiteralPath $temporaryState -Destination $StateFile -Force
                $nextState = $clock.Elapsed.TotalSeconds + 1
            }
            if ($clock.Elapsed.TotalSeconds -ge $nextStatus)
            {
                $displayPhase = $phase
                $displayQuiet = [int]($clock.Elapsed.TotalSeconds - $lastOutput)
                if ($ProgressFile)
                {
                    try
                    {
                        $progress = Get-Content -LiteralPath $ProgressFile -Raw | ConvertFrom-Json
                        if ($progress.phase) { $displayPhase = $progress.phase }
                        if ($null -ne $progress.quietSeconds) { $displayQuiet = $progress.quietSeconds }
                    }
                    catch { } # A snapshot may be replaced between opening and reading it.
                }
                [Console]::Error.WriteLine("$Label running: worker PID $($process.Id), phase $displayPhase, elapsed $([int]$clock.Elapsed.TotalSeconds)s / ${TimeoutSeconds}s, quiet for ${displayQuiet}s (liveness only).")
                $nextStatus += $StatusSeconds
            }
            Start-Sleep -Milliseconds 200
        }
        $process.WaitForExit()
        for ($i = 0; $i -lt $readers.Count; $i++)
        {
            $chunk = $readers[$i].ReadToEnd()
            if ($chunk -and -not $QuietOutput) { if ($i -eq 0) { [Console]::Out.Write($chunk) } else { [Console]::Error.Write($chunk) } }
        }
        return $process.ExitCode
    }
    finally
    {
        # Includes grandchildren and also works when the supervisor is externally terminated (kill-on-close).
        try
        {
            if ($process -and -not $assigned -and -not $process.HasExited)
            {
                # Assignment failed before the gate opened; this worker cannot have launched descendants.
                $process.Kill()
                $process.WaitForExit()
            }
            $job.Stop()
        }
        finally
        {
            $job.Dispose()
            foreach ($reader in $readers) { $reader.Dispose() }
            $gate.Dispose()
            if ($process) { $process.Dispose() }
            Remove-Item -LiteralPath $inputFile -ErrorAction SilentlyContinue
        }
    }
}
