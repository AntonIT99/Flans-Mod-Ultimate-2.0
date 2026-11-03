$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'validation-reports.ps1')
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('validation-reports-test-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot | Out-Null
$passed = 0

function Assert([bool]$condition, [string]$message)
{
    if (-not $condition) { throw $message }
    $script:passed++
}

function Assert-Throws([scriptblock]$action, [string]$message)
{
    try { & $action } catch { $script:passed++; return }
    throw $message
}

function Write-Report([string]$task, [string]$content)
{
    $directory = Join-Path $testRoot "build\reports\sonarLint\$task"
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    Set-Content -Encoding UTF8 (Join-Path $directory "$task.xml") $content
}

try
{
    $finding = '<file name="C:\repo\src\main\java\A.java"><error line="7" source="java:S100" message="Naming issue" /></file>'
    Write-Report 'sonarlintMain' "<checkstyle>$finding</checkstyle>"
    $otherFinding = '<file name="src/test/Other.java"><error line="2" source="java:S200" message="Another issue" /></file>'
    Write-Report 'sonarlintTest' "<checkstyle>$finding$otherFinding</checkstyle>"
    # An unrequested source-set report can be stale or malformed; it must not be opened.
    Write-Report 'sonarlintNpcs' '<broken'
    $result = Get-ValidationFindings -Root $testRoot -Tasks @('sonarlintMain', 'sonarlintTest')
    Assert ($result.Count -eq 2) 'Findings were not deduplicated across the explicitly requested tasks.'
    Assert ($result.Findings.Count -eq 2 -and $result.Findings[0].Classification -eq 'unclassified') 'Concise unclassified findings were not returned.'
    Assert ($result.Reports.Count -eq 2 -and $result.Reports[0] -match 'sonarlintMain') 'Only requested report paths were not returned.'

    $manyFindings = 0..21 | ForEach-Object { "<file name=`"src/main/java/F$_.java`"><error line=`"1`" source=`"java:S$($_ + 1)`" message=`"Issue $_`" /></file>" }
    Write-Report 'sonarlintMany' "<checkstyle>$($manyFindings -join '')</checkstyle>"
    $limited = Get-ValidationFindings -Root $testRoot -Tasks @('sonarlintMany')
    Assert ($limited.Count -eq 22 -and $limited.Findings.Count -eq 20) 'The result did not cap returned findings at 20 while preserving the total count.'

    Assert-Throws { Get-ValidationFindings -Root $testRoot -Tasks @('sonarlintPacksmanager') } 'A missing completed-task report was accepted.'
    Write-Report 'sonarlintBroken' '<checkstyle>'
    Assert-Throws { Get-ValidationFindings -Root $testRoot -Tasks @('sonarlintBroken') } 'Malformed XML was accepted.'
    Assert-Throws { Get-ValidationFindings -Root $testRoot -Tasks @('build') } 'A non-SonarLint task name was accepted.'

    "Passed $passed validation report parser checks."
}
finally
{
    Remove-Item -LiteralPath $testRoot -Recurse -Force -ErrorAction SilentlyContinue
}
