# Compatibility entry point: inspection safety is now covered against Qodana SARIF and fake Gradle scans.
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'test-qodana-inspect.ps1')
exit $LASTEXITCODE
