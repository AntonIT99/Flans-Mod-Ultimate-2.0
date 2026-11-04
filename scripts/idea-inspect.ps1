<#
.SYNOPSIS
    Compatibility entry point for Gradle-managed Qodana inspections; no installed IntelliJ is used.
#>
param(
    [string]$Path,
    [string[]]$Files,
    [string]$MinSeverity = 'WEAK WARNING',
    [string]$Profile = '.idea\inspectionProfiles\Project_Default.xml',
    [ValidateRange(1, 86400)][int]$TimeoutSeconds = 1200,
    [switch]$Json,
    [switch]$Full,
    [switch]$NoCache,
    [switch]$Rebuild
)
$options = @('-MinSeverity', $MinSeverity, '-Profile', $Profile, '-InspectionTimeoutSeconds', "$TimeoutSeconds")
if ($Path) { $options += @('-Path', $Path) }
if ($Files) { $options += @('-Files', ($Files -join ';')) }
foreach ($flag in @('Json', 'Full', 'NoCache', 'Rebuild'))
{
    if ($PSBoundParameters[$flag]) { $options += "-$flag" }
}
$outerTimeout = [Math]::Min(86400, $TimeoutSeconds + 120)
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'run-validation.ps1') -Task 'Qodana inspections' -TimeoutSeconds $outerTimeout Qodana @options
exit $LASTEXITCODE
