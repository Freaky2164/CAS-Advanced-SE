# build-pdf.ps1 — Baut das Image (einmalig) und erzeugt die PDF im Container.
# Aufruf aus beliebigem Verzeichnis:
#     .\pdf\build-pdf.ps1
# Optional: -Input <md> -Output <pdf> -Rebuild
[CmdletBinding()]
param(
  [string]$Input  = "seminararbeit-architektur.md",
  [string]$Output = "seminararbeit-architektur.pdf",
  [switch]$Rebuild
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path   # ...\03-architektur\pdf
$ArchDir   = Split-Path -Parent $ScriptDir                     # ...\03-architektur
$Image     = "fhma-seminar-pdf:latest"

# Docker-Daemon erreichbar?
docker info *> $null
if ($LASTEXITCODE -ne 0) {
  Write-Error "Docker-Daemon nicht erreichbar. Bitte Docker Desktop starten und erneut ausfuehren."
  exit 1
}

$exists = (docker images -q $Image)
if ($Rebuild -or [string]::IsNullOrWhiteSpace($exists)) {
  Write-Host ">> Baue Image $Image ..." -ForegroundColor Cyan
  docker build -t $Image $ScriptDir
  if ($LASTEXITCODE -ne 0) { Write-Error "Image-Build fehlgeschlagen."; exit 1 }
}

Write-Host ">> Erzeuge PDF aus $Input -> $Output" -ForegroundColor Cyan
# Das Architektur-Verzeichnis wird nach /work gemountet (enthaelt die .md und adrs/).
docker run --rm -v "${ArchDir}:/work" $Image $Input $Output
if ($LASTEXITCODE -ne 0) { Write-Error "PDF-Erzeugung fehlgeschlagen."; exit 1 }

$pdfPath = Join-Path $ArchDir $Output
if (Test-Path $pdfPath) {
  $kb = [math]::Round((Get-Item $pdfPath).Length / 1KB, 1)
  Write-Host ">> Fertig: $pdfPath ($kb KB)" -ForegroundColor Green
} else {
  Write-Error "PDF wurde nicht gefunden: $pdfPath"; exit 1
}
