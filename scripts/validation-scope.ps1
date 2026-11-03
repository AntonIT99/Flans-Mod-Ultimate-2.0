# Pure helpers shared by validation profiles and their lightweight checks.
function Resolve-ValidationFiles([string]$Root, [string[]]$Files)
{
    $prefix = $Root.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    $resolved = @($Files | ForEach-Object { $_ -split '[;,]' } | Where-Object { $_ } | ForEach-Object {
        $candidate = [IO.Path]::GetFullPath((Join-Path $Root $_))
        if (-not $candidate.StartsWith($prefix, [StringComparison]::OrdinalIgnoreCase)) { throw "File escapes checkout: $_" }
        if (Test-Path -LiteralPath $candidate -PathType Container) { throw "Select files, not directories: $_" }
        $relative = $candidate.Substring($prefix.Length).Replace('\', '/')
        if (-not (Test-Path -LiteralPath $candidate))
        {
            $repository = Get-ValidationRepository $candidate
            $tracked = @(& git -C $repository ls-files -- $candidate.Substring($repository.Length + 1).Replace('\', '/'))
            if (-not $tracked.Count) { throw "File does not exist and is not a tracked deletion: $_" }
        }
        $relative
    } | Sort-Object -Unique)
    if (-not $resolved.Count) { throw 'Select the task-owned files with -Files; comma or semicolon separates paths.' }
    return $resolved
}

function Get-ValidationRepository([string]$File)
{
    $directory = Split-Path $File -Parent
    while (-not (Test-Path -LiteralPath (Join-Path $directory '.git')))
    {
        $parent = Split-Path $directory -Parent
        if (-not $parent -or $parent -eq $directory) { throw "No owning repository for $File" }
        $directory = $parent
    }
    return $directory
}

function Test-ValidationDiff([string]$Root, [string[]]$Files)
{
    $groups = @{}
    foreach ($relative in $Files)
    {
        $absolute = Join-Path $Root $relative
        $directory = Get-ValidationRepository $absolute
        if (-not $groups.ContainsKey($directory)) { $groups[$directory] = @() }
        $groups[$directory] += $absolute.Substring($directory.Length + 1).Replace('\', '/')
    }
    foreach ($repository in $groups.Keys)
    {
        & git -C $repository diff --check HEAD -- @($groups[$repository])
        if ($LASTEXITCODE -ne 0) { throw "Whitespace errors in task scope: $repository" }
    }
}
