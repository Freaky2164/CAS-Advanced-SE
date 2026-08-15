[CmdletBinding()]
param(
  [Parameter(Mandatory = $true)][string]$InputPath,
  [Parameter(Mandatory = $true)][string]$OutputPath
)

$ErrorActionPreference = "Stop"
$word = $null
$document = $null
try {
  $word = New-Object -ComObject Word.Application
  $word.Visible = $false
  $word.DisplayAlerts = 0
  $word.Options.UpdateFieldsAtPrint = $false
  $word.Options.UpdateLinksAtPrint = $false
  $document = $word.Documents.Open((Resolve-Path $InputPath).Path, $false, $true)
  $document.ExportAsFixedFormat([IO.Path]::GetFullPath($OutputPath), 17)
} finally {
  if ($document) { $document.Close($false) }
  if ($word) { $word.Quit() }
  [GC]::Collect()
  [GC]::WaitForPendingFinalizers()
}
