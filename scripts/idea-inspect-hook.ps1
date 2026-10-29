<#
.SYNOPSIS
    Stop hook: makes an agent look at the IntelliJ inspection warnings of the Java it just changed.

.DESCRIPTION
    Registered as a Stop hook in .claude/settings.json. It reads the hook payload on stdin, runs
    scripts/idea-inspect.ps1 over the locally changed Java and Kotlin files, and, when there are warnings,
    answers with decision 'block' so the warnings reach the agent before it finishes.

    It is cheap in the cases that matter: with no changed Java or Kotlin file, and with a working tree the
    result cache already covers, it exits in well under a second. The cost is paid when Java changed and the
    answer is not cached, which is when the check is worth something.

    It blocks at most once per stop: the second time it is asked about the same tree it stays out of the way,
    so warnings that are deliberately left (pre-existing, or required by a project convention) cannot trap the
    agent in a loop.
#>
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

try
{
    $payload = [Console]::In.ReadToEnd() | ConvertFrom-Json
}
catch
{
    exit 0
}
# Claude Code sets this when the turn it is ending was itself started by this hook blocking.
if ($payload.stop_hook_active)
{
    exit 0
}

$changed = @(@(git -C $root diff --name-only HEAD) + @(git -C $root ls-files --others --exclude-standard) |
    Where-Object { $_ -match '\.(java|kt)$' -and (Test-Path (Join-Path $root $_)) })
if (-not $changed)
{
    exit 0
}

# Second guard, in case stop_hook_active is not set: one block per session and tree.
$markerDir = Join-Path $root 'build\idea-inspect\hook'
New-Item -ItemType Directory -Force $markerDir | Out-Null
Get-ChildItem $markerDir -File -ErrorAction SilentlyContinue |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-1) } | Remove-Item -Force -ErrorAction SilentlyContinue
$sha = [Security.Cryptography.SHA256]::Create()
try
{
    $key = [BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(
        @("$($payload.session_id)") + @($changed | Sort-Object | ForEach-Object { "$_=$((Get-FileHash -Algorithm SHA256 (Join-Path $root $_)).Hash)" }) -join "`n"))).Replace('-', '')
}
finally
{
    $sha.Dispose()
}
$marker = Join-Path $markerDir ($key.Substring(0, 32) + '.done')
if (Test-Path $marker)
{
    exit 0
}

$report = @(& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root 'scripts\idea-inspect.ps1') -MinSeverity WARNING 2>&1)
if ($LASTEXITCODE -ne 0)
{
    # The inspection itself failed (no IntelliJ, a broken index). Do not hold up the turn for it, but say so,
    # otherwise a check that quietly stopped working looks exactly like a clean run.
    ConvertTo-Json -Compress -InputObject ([pscustomobject]@{
            systemMessage = "scripts/idea-inspect.ps1 failed, so the IntelliJ check was skipped: $(($report | Select-Object -Last 3) -join ' ')"
        })
    exit 0
}
$problems = @($report | Where-Object { $_ -match '^\S+:\d+: \[' })
if (-not $problems)
{
    exit 0
}

New-Item -ItemType File -Force $marker | Out-Null
$reason = @(
    "IntelliJ reports $($problems.Count) inspection warning(s) in the Java you changed:",
    ''
    $problems
    ''
    'Fix the ones you introduced in the code you touched. Leave pre-existing warnings and the ones a project'
    'convention requires (see the mixin and Lombok rules in AGENTS.md), and say in your answer which you left'
    'and why. Then finish; this check will not interrupt you again.'
) -join "`n"
ConvertTo-Json -Compress -InputObject ([pscustomobject]@{ decision = 'block'; reason = $reason })
exit 0
