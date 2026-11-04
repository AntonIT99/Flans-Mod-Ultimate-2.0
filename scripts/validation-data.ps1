param([Parameter(Mandatory = $true)][string[]]$Files, [switch]$Apply)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'validation-scope.ps1')
$selected = @(Resolve-ValidationFiles $root $Files)
$checked = 0
foreach ($relative in $selected)
{
    $file = Join-Path $root $relative
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { continue } # Deleted files still enter diff checks.
    $extension = [IO.Path]::GetExtension($file)
    if ($extension -eq '.ps1')
    {
        $tokens = $null
        $errors = $null
        $null = [Management.Automation.Language.Parser]::ParseFile($file, [ref]$tokens, [ref]$errors)
        if ($errors.Count) { throw "PowerShell syntax errors in ${relative}: $($errors -join '; ')" }
        $checked++
    }
    if ($extension -eq '.json')
    {
        $text = [IO.File]::ReadAllText($file)
        if ([string]::IsNullOrWhiteSpace($text)) { throw "Empty JSON document: $relative" }
        $null = ConvertFrom-Json -InputObject $text -ErrorAction Stop
        # Match the existing Spotless JSON rule for resource numeric arrays; preserve all other layout.
        if ($relative -match '^src/[^/]+/resources/')
        {
            $number = '-?(?:0|[1-9][0-9]*)(?:\.[0-9]+)?(?:[eE][+-]?[0-9]+)?'
            $pattern = '"(?:\\.|[^"\\])*"|\[\s*' + $number + '(?:\s*,\s*' + $number + ')*\s*\]'
            $formatted = [regex]::Replace($text.Replace("`r`n", "`n"), $pattern, [Text.RegularExpressions.MatchEvaluator]{
                param($match)
                if ($match.Value.StartsWith('"')) { return $match.Value }
                return '[' + (([regex]::Matches($match.Value, $number) | ForEach-Object { $_.Value }) -join ', ') + ']'
            })
            if ($formatted -cne $text)
            {
                if (-not $Apply) { throw "Formatting required in $relative; rerun with -Apply." }
                [IO.File]::WriteAllText($file, $formatted, [Text.UTF8Encoding]::new($false))
            }
        }
        $checked++
    }
    if ($extension -eq '.md' -or (Split-Path $file -Leaf) -eq '.gitignore')
    {
        $text = [IO.File]::ReadAllText($file)
        $formatted = [regex]::Replace($text.Replace("`r`n", "`n"), '(?m)[ \t]+$', '').TrimEnd("`n") + "`n"
        if ($text.Length -eq 0) { $formatted = '' }
        if ($formatted -cne $text)
        {
            if (-not $Apply) { throw "Formatting required in $relative; rerun with -Apply." }
            [IO.File]::WriteAllText($file, $formatted, [Text.UTF8Encoding]::new($false))
        }
        $checked++
    }
}
Test-ValidationDiff $root $selected
"Checked $($selected.Count) task-owned paths ($checked syntax/format checks); diff checks passed. Other data formats require their owning validators."
