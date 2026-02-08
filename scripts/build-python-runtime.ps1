param(
  [string]$PythonVersion = "3.10.13",
  [string]$TargetDir = "python"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$target = Join-Path $root $TargetDir

$versionNoDots = $PythonVersion.Replace(".", "")
$zipName = "python-$PythonVersion-embed-amd64.zip"
$zipUrl = "https://www.python.org/ftp/python/$PythonVersion/$zipName"

Write-Host "== Download embeddable Python ($PythonVersion) =="
$zipPath = Join-Path $env:TEMP $zipName
Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath

Write-Host "== Extract Python runtime =="
if (Test-Path $target) {
  Remove-Item -Recurse -Force $target
}
New-Item -ItemType Directory -Force -Path $target | Out-Null
Expand-Archive -Path $zipPath -DestinationPath $target -Force

$pthFile = Join-Path $target "python$versionNoDots._pth"
if (Test-Path $pthFile) {
  Write-Host "== Enable site-packages =="
  (Get-Content $pthFile) | ForEach-Object { $_ -replace '^#import site','import site' } | Set-Content $pthFile
}

Write-Host "== Install pip =="
$pythonExe = Join-Path $target "python.exe"
$getPip = Join-Path $env:TEMP "get-pip.py"
Invoke-WebRequest -Uri "https://bootstrap.pypa.io/get-pip.py" -OutFile $getPip
& $pythonExe $getPip

Write-Host "== Install OCR dependencies =="
& $pythonExe -m pip install -U pip
& $pythonExe -m pip install -r (Join-Path $root "ocr-worker\requirements-min.txt")

Write-Host "== Done =="
Write-Host "Python runtime ready at: $target"
