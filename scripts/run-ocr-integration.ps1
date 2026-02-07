param(
  [string]$Python = "python",
  [string]$JavaHome = "",
  [int]$Port = 8080
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$buildDir = Join-Path $root "build"
$imgPath = Join-Path $buildDir "mrz.png"
$previewOut = Join-Path $buildDir "ocr-preview.json"
$excelOut = Join-Path $buildDir "passport_export.xlsx"

New-Item -ItemType Directory -Force -Path $buildDir | Out-Null

Write-Host "== Ensure OCR models =="
if (-not (Test-Path (Join-Path $root "ocr-models\paddle"))) {
  & $Python (Join-Path $root "ocr-worker\download_models.py") --model-dir (Join-Path $root "ocr-models")
}

Write-Host "== Create sample MRZ image =="
& $Python (Join-Path $root "scripts\make_mrz_image.py") $imgPath

Write-Host "== Start Spring Boot =="
if ($JavaHome -ne "") {
  $env:JAVA_HOME = $JavaHome
}
$env:SPRING_APPLICATION_JSON = '{"ocr":{"mode":"paddle"}}'

$boot = Start-Process -FilePath (Join-Path $root "gradlew.bat") -ArgumentList "bootRun" -PassThru -WindowStyle Hidden

Write-Host "== Wait for server =="
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
  try {
    $resp = Invoke-WebRequest -Uri "http://localhost:$Port/" -UseBasicParsing -TimeoutSec 2
    if ($resp.StatusCode -eq 200) {
      $ready = $true
      break
    }
  } catch {
    Start-Sleep -Seconds 1
  }
}

if (-not $ready) {
  Stop-Process -Id $boot.Id -Force
  throw "Server did not become ready on port $Port"
}

Write-Host "== Call /api/ocr/preview =="
curl.exe -s -o $previewOut -F "images=@$imgPath" "http://localhost:$Port/api/ocr/preview"

Write-Host "== Call /api/ocr/export =="
curl.exe -s -o $excelOut -F "images=@$imgPath" "http://localhost:$Port/api/ocr/export"

Write-Host "== Stop server =="
Stop-Process -Id $boot.Id -Force

Write-Host "Preview JSON: $previewOut"
Write-Host "Excel file : $excelOut"
