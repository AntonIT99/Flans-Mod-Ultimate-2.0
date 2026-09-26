<#
.SYNOPSIS
Decompiles selected classes from a directory, .jar or .zip into the repository's
decompiled/ cache, renames MCP searge identifiers, and records each class in
decompiled/INVENTORY.txt.

.EXAMPLE
pwsh -File decompile.ps1 -Source "E:\...\Hero Shooter September" -Include "com/flansmod/client/model/NewAge/**" -Slug tap-hero-shooter-september -Mappings 1.7.10
#>
param(
    [Parameter(Mandatory)] [string] $Source,
    [Parameter(Mandatory)] [string] $Slug,
    # Class paths relative to the source root, with / separators, as a list or one
    # comma-separated string. * matches within a directory level, ** across levels.
    # Defaults to every class.
    [string[]] $Include = @("**"),
    [ValidateSet("1.7.10", "1.12", "none")] [string] $Mappings = "none",
    [string] $Repository = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..\..")).Path,
    [string] $WorkDirectory = (Join-Path ([IO.Path]::GetTempPath()) "fmu-decompile-$Slug"),
    [switch] $Force
)

$ErrorActionPreference = "Stop"

$Vineflower = "D:\Tools\Vineflower\vineflower-1.12.0.jar"
$Cfr = "D:\Tools\cfr\cfr-0.152.jar"
$MappingRoot = "D:\Minecraft\Dev Tools\Deobfuscator\Minecraft-Deobfuscator3000-1.2.3"
$MappingDirectories = @{ "1.7.10" = "1.7.10 stable mappings"; "1.12" = "1.12 stable mappings" }
$Heap = "8G"

if ($Slug -notmatch '^[a-z0-9][a-z0-9._-]*$') { throw "Slug must be lowercase letters, digits, '.', '_' or '-': $Slug" }
foreach ($required in @($Vineflower, $Cfr, $Source)) {
    if (-not (Test-Path -LiteralPath $required)) { throw "Required path does not exist: $required" }
}
$mappingPath = $null
if ($Mappings -ne "none") {
    $mappingPath = Join-Path $MappingRoot $MappingDirectories[$Mappings]
    if (-not (Test-Path -LiteralPath $mappingPath)) { throw "Mappings not found: $mappingPath" }
}

$cache = Join-Path $Repository "decompiled"
$target = Join-Path $cache $Slug
$inventory = Join-Path $cache "INVENTORY.txt"
$stageIn = Join-Path $WorkDirectory "in"
$stageOut = Join-Path $WorkDirectory "out"
Remove-Item -LiteralPath $WorkDirectory -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $stageIn, $stageOut, $target | Out-Null

function ConvertTo-Regex([string] $glob) {
    $escaped = [Regex]::Escape($glob.Replace('\', '/'))
    $escaped = $escaped.Replace('\*\*/', '(?:.*/)?').Replace('\*\*', '.*').Replace('\*', '[^/]*').Replace('\?', '[^/]')
    return "^$escaped$"
}
# pwsh -File passes a list as one comma-separated string.
$Include = @($Include | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim().Trim('"', "'") } | Where-Object { $_ })
$patterns = @($Include | ForEach-Object { ConvertTo-Regex $_ })
function Test-Included([string] $relative) {
    foreach ($pattern in $patterns) { if ($relative -match $pattern) { return $true } }
    return $false
}

# Stage matching classes, remembering where each came from and its hash.
$staged = [ordered]@{}
$sourceItem = Get-Item -LiteralPath $Source
if ($sourceItem.PSIsContainer) {
    foreach ($file in Get-ChildItem -LiteralPath $Source -Filter "*.class" -File -Recurse) {
        $relative = [IO.Path]::GetRelativePath($Source, $file.FullName).Replace('\', '/')
        if (-not (Test-Included $relative)) { continue }
        $destination = Join-Path $stageIn $relative
        New-Item -ItemType Directory -Force -Path (Split-Path $destination) | Out-Null
        Copy-Item -LiteralPath $file.FullName -Destination $destination
        $staged[$relative] = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA1).Hash.ToLowerInvariant()
    }
}
else {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($sourceItem.FullName)
    try {
        foreach ($entry in $archive.Entries) {
            $relative = $entry.FullName.Replace('\', '/')
            if (-not $relative.EndsWith(".class") -or -not (Test-Included $relative)) { continue }
            $destination = Join-Path $stageIn $relative
            New-Item -ItemType Directory -Force -Path (Split-Path $destination) | Out-Null
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $destination, $true)
            $staged[$relative] = (Get-FileHash -LiteralPath $destination -Algorithm SHA1).Hash.ToLowerInvariant()
        }
    }
    finally { $archive.Dispose() }
}
if ($staged.Count -eq 0) { throw "No .class files matched $($Include -join ', ') under $Source" }

# Skip classes whose cached copy came from an identical class file, unless forced.
$existing = @{}
if (Test-Path -LiteralPath $inventory) {
    foreach ($line in Get-Content -LiteralPath $inventory) {
        if ($line.StartsWith("#") -or -not $line.Trim()) { continue }
        $fields = $line.Split("`t")
        if ($fields.Count -ge 8 -and $fields[1] -eq $Slug) { $existing[$fields[3]] = $fields[7] }
    }
}
$outerClasses = @($staged.Keys | Where-Object { -not ([IO.Path]::GetFileName($_)).Contains('$') })
$pending = @($outerClasses | Where-Object {
    $Force -or $existing[$_] -ne $staged[$_] -or -not (Test-Path -LiteralPath (Join-Path $target ($_ -replace '\.class$', '.java')))
})
Write-Host "Matched $($outerClasses.Count) classes; $($pending.Count) need decompiling."
if ($pending.Count -eq 0) { exit 0 }

Write-Host "Running Vineflower..."
$vineflowerLog = @(& java "-Xmx$Heap" -jar $Vineflower --folder --thread-count=1 --log-level=WARN "--preferred-line-length=2000" $stageIn $stageOut 2>&1 |
    ForEach-Object { $_.ToString() })
$vineflowerLog | ForEach-Object { Write-Host $_ }

# Vineflower can exit 0 while failing individual classes; retry those with CFR.
$failed = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
foreach ($line in $vineflowerLog) {
    if ($line -match "in class ([A-Za-z0-9_$/]+) couldn't be decompiled") { [void]$failed.Add(($Matches[1] -replace '\$.*$', '') + ".class") }
}
foreach ($relative in $pending) {
    if (-not (Test-Path -LiteralPath (Join-Path $stageOut ($relative -replace '\.class$', '.java')))) { [void]$failed.Add($relative) }
}
$decompiler = @{}
foreach ($relative in $pending) { $decompiler[$relative] = "vineflower-1.12.0" }
foreach ($relative in $failed) {
    Write-Host "CFR fallback: $relative"
    & java "-Xmx$Heap" -jar $Cfr (Join-Path $stageIn $relative) --outputdir $stageOut --extraclasspath $stageIn --silent true
    $decompiler[$relative] = if ($LASTEXITCODE -eq 0) { "cfr-0.152" } else { "failed" }
}

if ($mappingPath) {
    Write-Host "Applying $Mappings MCP mappings..."
    & py (Join-Path $PSScriptRoot "apply_mcp_mappings.py") $mappingPath $stageOut
    if ($LASTEXITCODE -ne 0) { throw "Mapping pass failed." }
}

# Move results into the cache and rewrite this slug's inventory rows.
$date = Get-Date -Format "yyyy-MM-dd"
$rows = [Collections.Generic.List[string]]::new()
foreach ($relative in $pending) {
    $javaRelative = $relative -replace '\.class$', '.java'
    $produced = Join-Path $stageOut $javaRelative
    if (-not (Test-Path -LiteralPath $produced)) { $decompiler[$relative] = "failed"; continue }
    $destination = Join-Path $target $javaRelative
    New-Item -ItemType Directory -Force -Path (Split-Path $destination) | Out-Null
    Move-Item -LiteralPath $produced -Destination $destination -Force
    $rows.Add(($date, $Slug, $sourceItem.FullName, $relative, "$Slug/$javaRelative", $decompiler[$relative], $Mappings, $staged[$relative]) -join "`t")
}

$header = @(
    "# Decompiled reference classes. Local only: decompiled/ is gitignored.",
    "# Columns: date<TAB>slug<TAB>source container<TAB>class path in source<TAB>java path under decompiled/<TAB>decompiler<TAB>mappings<TAB>class sha1"
)
$kept = @()
if (Test-Path -LiteralPath $inventory) {
    $replaced = [Collections.Generic.HashSet[string]]::new([string[]] @($rows | ForEach-Object { $_.Split("`t")[3] }))
    $kept = @(Get-Content -LiteralPath $inventory | Where-Object {
        -not $_.StartsWith("#") -and $_.Trim() -and -not ($_.Split("`t")[1] -eq $Slug -and $replaced.Contains($_.Split("`t")[3]))
    })
}
Set-Content -LiteralPath $inventory -Value (@($header) + @($kept + $rows | Sort-Object)) -Encoding utf8

$failures = @($decompiler.GetEnumerator() | Where-Object { $_.Value -eq "failed" } | ForEach-Object { $_.Key })
Write-Host "Decompiled $($rows.Count) classes into $target"
if ($failures.Count -gt 0) {
    Write-Warning "Failed ($($failures.Count)): $($failures -join ', ')"
    exit 2
}
Remove-Item -LiteralPath $WorkDirectory -Recurse -Force -ErrorAction SilentlyContinue
