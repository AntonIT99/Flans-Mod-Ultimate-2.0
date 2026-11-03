<#
.SYNOPSIS
    Makes this checkout's shared agent skills available to Claude Code on Windows.
.EXAMPLE
    powershell -NoProfile -File scripts/setup-agent-skills.ps1
.DESCRIPTION
    Run once after cloning, and separately in each worktree. Creates an ignored
    .claude/skills junction to .agents/skills without copying skill instructions.
    An existing correct junction is accepted; other entries are never replaced.
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$source = Join-Path $root '.agents/skills'
$destination = Join-Path $root '.claude/skills'

if (-not (Test-Path -LiteralPath $source -PathType Container))
{
    throw "Shared skills directory is missing: $source"
}

# Get-Item also detects dangling junctions, which must not be overwritten.
$existing = Get-Item -LiteralPath $destination -Force -ErrorAction SilentlyContinue
if ($null -ne $existing)
{
    $targets = @($existing.Target)
    if ($existing.LinkType -eq 'Junction' -and $targets.Count -eq 1 -and
        [string]::Equals($targets[0].TrimEnd('\', '/'), $source.TrimEnd('\', '/'), [StringComparison]::OrdinalIgnoreCase))
    {
        Write-Output "Claude Code skills are already linked: $destination -> $source"
        return
    }

    throw "Refusing to replace existing entry: $destination. Inspect it and move or remove it manually before running setup again."
}

$claudeDirectory = Join-Path $root '.claude'
if (-not (Test-Path -LiteralPath $claudeDirectory -PathType Container))
{
    New-Item -ItemType Directory -Path $claudeDirectory | Out-Null
}
New-Item -ItemType Junction -Path $destination -Target $source | Out-Null
Write-Output "Claude Code skills linked: $destination -> $source"
