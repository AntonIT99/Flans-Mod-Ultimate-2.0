function Get-ValidationFindings
{
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$Root,
        [Parameter(Mandatory = $true)]
        [string[]]$Tasks
    )

    $ErrorActionPreference = 'Stop'
    $rootPath = [IO.Path]::GetFullPath($Root)
    $reports = @()
    $findings = @()
    $seen = @{}
    $selectedTasks = @($Tasks | Where-Object { $_ } | Sort-Object -Unique)
    if (-not $selectedTasks)
    {
        throw 'At least one explicit SonarLint task is required.'
    }

    foreach ($task in $selectedTasks)
    {
        if ($task -notmatch '^sonarlint[A-Z][A-Za-z0-9]*$')
        {
            throw "Not a supported SonarLint task name: $task"
        }
        $report = Join-Path $rootPath "build\reports\sonarLint\$task\$task.xml"
        if (-not (Test-Path -LiteralPath $report -PathType Leaf))
        {
            throw "Completed SonarLint task '$task' has no report: $report"
        }
        $reports += $report
        try
        {
            $settings = [Xml.XmlReaderSettings]::new()
            $settings.DtdProcessing = [Xml.DtdProcessing]::Prohibit
            $reader = [Xml.XmlReader]::Create($report, $settings)
            try
            {
                $document = [Xml.XmlDocument]::new()
                $document.XmlResolver = $null
                $document.Load($reader)
            }
            finally
            {
                $reader.Dispose()
            }
        }
        catch
        {
            throw "Malformed SonarLint report for '$task': $report ($($_.Exception.Message))"
        }
        if ($document.DocumentElement.LocalName -ne 'checkstyle')
        {
            throw "Malformed SonarLint report for '$task': expected a checkstyle root in $report"
        }

        foreach ($fileNode in $document.SelectNodes('/checkstyle/file'))
        {
            $fileName = $fileNode.GetAttribute('name')
            if (-not $fileName)
            {
                throw "Malformed SonarLint report for '$task': file entry has no name in $report"
            }
            foreach ($errorNode in $fileNode.SelectNodes('error'))
            {
                $line = 0
                if (-not [int]::TryParse($errorNode.GetAttribute('line'), [ref]$line) -or $line -lt 1 -or
                    -not $errorNode.HasAttribute('source') -or -not $errorNode.HasAttribute('message'))
                {
                    throw "Malformed SonarLint finding for '$task' in $report"
                }
                $displayFile = $fileName
                if ([IO.Path]::IsPathRooted($fileName))
                {
                    $fullFile = [IO.Path]::GetFullPath($fileName)
                    if ($fullFile.StartsWith($rootPath.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase))
                    {
                        $displayFile = $fullFile.Substring($rootPath.TrimEnd('\').Length + 1).Replace([char]92, '/')
                    }
                }
                $rule = $errorNode.GetAttribute('source')
                $message = $errorNode.GetAttribute('message')
                $key = @($displayFile, $line, $rule, $message) -join "`0"
                if ($seen.ContainsKey($key))
                {
                    continue
                }
                $seen[$key] = $true
                $findings += [pscustomobject]@{
                    File = $displayFile
                    Line = $line
                    Rule = $rule
                    Message = $message
                    Classification = 'unclassified'
                }
            }
        }
    }

    $findings = @($findings | Sort-Object File, Line, Rule, Message)
    [pscustomobject]@{
        Count = $findings.Count
        Findings = @($findings | Select-Object -First 20)
        Reports = $reports
        Classification = 'unclassified'
    }
}
