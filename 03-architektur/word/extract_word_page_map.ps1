[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)][string]$InputPath,
  [Parameter(Mandatory = $true)][string]$OutputPath
)

$ErrorActionPreference = "Stop"

function Convert-ToRoman([int]$Number) {
  $values = @(
    @(1000, "M"), @(900, "CM"), @(500, "D"), @(400, "CD"),
    @(100, "C"), @(90, "XC"), @(50, "L"), @(40, "XL"),
    @(10, "X"), @(9, "IX"), @(5, "V"), @(4, "IV"), @(1, "I")
  )
  $result = ""
  foreach ($entry in $values) {
    while ($Number -ge $entry[0]) {
      $result += $entry[1]
      $Number -= $entry[0]
    }
  }
  return $result
}

function Clean-Text([string]$Text) {
  return (($Text -replace "[`r`n`t]", " ") -replace "\s+", " ").Trim([char]7, " ")
}

$word = $null
$document = $null
try {
  $word = New-Object -ComObject Word.Application
  $word.Visible = $false
  $word.DisplayAlerts = 0
  $document = $word.Documents.Open((Resolve-Path $InputPath).Path, $false, $true)
  $document.Repaginate()

  $targets = [System.Collections.Generic.List[string]]::new()
  $bodyPages = @{}
  foreach ($paragraph in @($document.Paragraphs)) {
    $styleName = [string]$paragraph.Style.NameLocal
    $text = Clean-Text $paragraph.Range.Text
    if ($styleName -like "DirectoryEntry*") {
      $target = ($text -replace "\s+\?\?$", "").Trim()
      if ($target) { $targets.Add($target) }
      continue
    }
    if (-not $text -or $bodyPages.ContainsKey($text)) { continue }
    $physical = [int]$paragraph.Range.Information(1)
    $adjusted = [int]$paragraph.Range.Information(3)
    $bodyPages[$text] = @{
      physical = $physical
      adjusted = $adjusted
    }
  }

  $pageMap = [ordered]@{}
  $chapterOnePhysical = [int]$bodyPages["1. Einleitung"].physical
  foreach ($target in $targets) {
    $match = $bodyPages[$target]
    if (-not $match) {
      $candidate = $bodyPages.Keys |
        Where-Object { $_ -like "$target*" -or $target -like "$_*" } |
        Select-Object -First 1
      if ($candidate) { $match = $bodyPages[$candidate] }
    }
    if (-not $match) {
      throw "Verzeichnisziel nicht im Word-Dokument gefunden: $target"
    }
    $pageMap[$target] = if ($target -eq "Zusammenfassung") {
      Convert-ToRoman $match.physical
    } else {
      [string]([int]$match.physical - $chapterOnePhysical + 1)
    }
  }

  $pageMap |
    ConvertTo-Json -Depth 3 |
    Set-Content -LiteralPath ([IO.Path]::GetFullPath($OutputPath)) -Encoding utf8
  Write-Host "$($pageMap.Count) exakte Word-Seitenverweise ermittelt"
} finally {
  $paragraph = $null
  if ($document) {
    $document.Close($false)
    [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($document)
  }
  if ($word) {
    $word.Quit()
    [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($word)
  }
  $document = $null
  $word = $null
  [GC]::Collect()
  [GC]::WaitForPendingFinalizers()
  [GC]::Collect()
  [GC]::WaitForPendingFinalizers()
}
