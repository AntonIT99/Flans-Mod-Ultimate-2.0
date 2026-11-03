param()

$ErrorActionPreference = 'Stop'
$scriptPath = Join-Path $PSScriptRoot 'qodana-inspect.ps1'
$testRoot = Join-Path ([IO.Path]::GetTempPath()) ('qodana-inspect-test-' + [Guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $testRoot
$passed = 0

function Assert([bool]$condition, [string]$message)
{
    if (-not $condition) { throw $message }
    $script:passed++
}

function Assert-Throws([scriptblock]$action, [string]$message)
{
    try { & $action }
    catch { $script:passed++; return }
    throw $message
}

try
{
    $tokens = $null
    $parseErrors = $null
    $ast = [Management.Automation.Language.Parser]::ParseFile($scriptPath, [ref]$tokens, [ref]$parseErrors)
    if ($parseErrors.Count) { throw "qodana-inspect.ps1 has PowerShell parse errors: $($parseErrors -join '; ')" }
    $source = Get-Content -Raw -LiteralPath $scriptPath
    Assert ($source.Contains("'qodanaScan'")) 'Worker does not invoke the qodanaScan task.'
    Assert ($source.Contains("'--project-cache-dir'")) 'Gradle launcher is not isolated from nested Tooling API project-cache locks.'
    Assert ($source.Contains("Remove-Item Env:QODANA_TOKEN")) 'Worker does not prevent Qodana Cloud upload.'
    Assert ($source.Contains('-StateFile $StateFile')) 'Worker does not forward process liveness to its coordinator state file.'
    foreach ($name in @('Resolve-ProjectPaths', 'Get-CommonInspectionDirectory', 'Get-ProfileName', 'New-QodanaConfiguration', 'Get-SarifInspectionProblems'))
    {
        $function = $ast.Find({ param($node) $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq $name }, $true)
        if (-not $function) { throw "Could not find production function $name in $scriptPath" }
        Invoke-Expression $function.Extent.Text
    }

    $scopeRoot = Join-Path $testRoot 'scope-root'
    $sourceDirectory = Join-Path $scopeRoot 'src/main/java'
    $null = New-Item -ItemType Directory -Path $sourceDirectory -Force
    Set-Content -Encoding ASCII -LiteralPath (Join-Path $sourceDirectory 'A.java') 'class A {}'
    Set-Content -Encoding ASCII -LiteralPath (Join-Path $sourceDirectory 'B.kt') 'class B'
    Set-Content -Encoding ASCII -LiteralPath (Join-Path $scopeRoot 'notes.txt') 'notes'
    $explicit = @(Resolve-ProjectPaths $scopeRoot @('src/main/java/A.java;src/main/java/B.kt', 'src/main/java/A.java') -RequireFiles)
    Assert ($explicit.Count -eq 2 -and $explicit[0].RelativePath -eq 'src/main/java/A.java' -and $explicit[1].RelativePath -eq 'src/main/java/B.kt') `
        'Explicit Java/Kotlin files did not resolve and deduplicate.'
    Assert-Throws { Resolve-ProjectPaths $scopeRoot @('../outside.java') -RequireFiles } 'A source outside the repository was accepted.'
    Assert-Throws { Resolve-ProjectPaths $scopeRoot @('notes.txt') -RequireFiles } 'A non-source file was accepted.'
    Assert-Throws { Resolve-ProjectPaths $scopeRoot @('src/main/java/Missing.java') -RequireFiles } 'A missing source file was accepted.'
    $directory = @(Resolve-ProjectPaths $scopeRoot @('src/main/java'))
    Assert ($directory.Count -eq 1 -and -not $directory[0].IsFile -and (Get-CommonInspectionDirectory $scopeRoot $directory) -eq 'src/main/java') `
        'Directory scope was not preserved.'
    $profileFile = Join-Path $scopeRoot 'Project_Default.xml'
    Set-Content -LiteralPath $profileFile -Encoding UTF8 '<component><profile><option name="myName" value="Project Default" /></profile></component>'
    Assert ((Get-ProfileName $profileFile) -eq 'Project Default') 'The shared IntelliJ profile name was not read from its XML option.'

    $configuration = New-QodanaConfiguration 'Project Default' $false @('src/main/java/A.java')
    Assert ($configuration -match 'plugins:\n  - id: com\.demonwav\.minecraft-dev' -and $configuration.Contains('DuplicatedCode') -and
        $configuration.Contains('AddedMixinMembersNamePattern') -and $configuration.Contains('!src/main/java/A.java')) `
        'Generated Qodana config lost managed Minecraft plugin, inspection defaults or exact file scope.'
    $fullConfiguration = New-QodanaConfiguration 'Project Default' $true @()
    Assert (-not $fullConfiguration.Contains('DuplicatedCode') -and $fullConfiguration.Contains('AddedMixinMembersNamePattern')) `
        '-Full did not restore expensive inspections while retaining the project convention.'

    $report = Join-Path $testRoot 'qodana.sarif.json'
    $sarif = @{
        version = '2.1.0'
        runs = @(@{
            tool = @{ driver = @{ rules = @(@{ id = 'SampleInspection'; properties = @{ inspectionId = 'SampleInspection' } }) } }
            invocations = @(@{ executionSuccessful = $true })
            results = @(
                @{ ruleId = 'SampleInspection'; level = 'warning'; message = @{ text = 'sample warning' }; locations = @(@{ physicalLocation = @{ artifactLocation = @{ uri = 'src/main/java/A.java' }; region = @{ startLine = 7 } } }) },
                @{ ruleId = 'SampleInspection'; level = 'warning'; message = @{ text = 'sample warning' }; locations = @(@{ physicalLocation = @{ artifactLocation = @{ uri = 'src/main/java/A.java' }; region = @{ startLine = 7 } } }) },
                @{ ruleId = 'SampleInspection'; level = 'note'; message = @{ text = 'other file' }; locations = @(@{ physicalLocation = @{ artifactLocation = @{ uri = 'src/main/java/B.kt' }; region = @{ startLine = 3 } } }) }
            )
        })
    }
    $sarif | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $report -Encoding UTF8
    $problems = @(Get-SarifInspectionProblems $report $scopeRoot @('src/main/java/A.java'))
    Assert ($problems.Count -eq 1 -and $problems[0].severity -eq 'WARNING' -and $problems[0].line -eq 7) `
        'Valid SARIF findings were not filtered to explicit files, mapped or deduplicated.'

    $sarif.runs[0].results[0].locations[0].physicalLocation.artifactLocation.uri = 'file:///' + (Join-Path $sourceDirectory 'A.java').Replace('\', '/')
    $sarif.runs[0].results = @($sarif.runs[0].results[0])
    $sarif | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $report -Encoding UTF8
    $uriProblem = @(Get-SarifInspectionProblems $report $scopeRoot @('src/main/java/A.java'))
    Assert ($uriProblem.Count -eq 1 -and $uriProblem[0].file -eq 'src/main/java/A.java') 'A local file URI SARIF path was rejected or misreported.'

    $sarif.runs[0].results[0].locations[0].physicalLocation.artifactLocation.uri = '../outside.java'
    $sarif | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $report -Encoding UTF8
    Assert-Throws { Get-SarifInspectionProblems $report $scopeRoot @() } 'A traversal path in SARIF was accepted.'

    $sarif.runs[0].invocations[0].executionSuccessful = $false
    $sarif | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $report -Encoding UTF8
    Assert-Throws { Get-SarifInspectionProblems $report $scopeRoot @() } 'SARIF with an unsuccessful invocation was accepted as clean.'
    Set-Content -LiteralPath $report -Encoding UTF8 '{ invalid json'
    Assert-Throws { Get-SarifInspectionProblems $report $scopeRoot @() } 'Malformed SARIF was accepted.'
    Remove-Item -LiteralPath $report
    Assert-Throws { Get-SarifInspectionProblems $report $scopeRoot @() } 'A missing SARIF report was accepted.'

    "Passed $passed Qodana worker assertions."
}
finally
{
    Remove-Item -LiteralPath $testRoot -Recurse -Force -ErrorAction SilentlyContinue
}
