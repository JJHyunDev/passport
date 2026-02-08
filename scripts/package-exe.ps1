param(
  [string]$PythonHome = "",
  [string]$AppName = "PassportOcr",
  [string]$AppVersion = "0.1.0"
)

$ErrorActionPreference = "Stop"

Write-Host "== Build Spring Boot JAR =="
./gradlew.bat clean bootJar

$JarPath = Get-ChildItem -Path "build/libs" -Filter "*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $JarPath) {
  throw "No JAR found in build/libs"
}

$DistDir = "dist"
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null

$StageDir = Join-Path $DistDir "app-input"
if (Test-Path $StageDir) {
  Remove-Item -Recurse -Force $StageDir
}
New-Item -ItemType Directory -Force -Path $StageDir | Out-Null

Write-Host "== Stage application files =="
Copy-Item -Force $JarPath.FullName $StageDir
Copy-Item -Force "src/main/resources/application.yml" "$StageDir/application.yml"

Write-Host "== Stage OCR worker files =="
Copy-Item -Recurse -Force "ocr-worker" "$StageDir/ocr-worker"

if (Test-Path "ocr-models") {
  Write-Host "== Stage OCR models =="
  Copy-Item -Recurse -Force "ocr-models" "$StageDir/ocr-models"
} else {
  Write-Host "== OCR models not found (skip) =="
}

if (Test-Path "python") {
  Write-Host "== Stage Python runtime =="
  Copy-Item -Recurse -Force "python" "$StageDir/python"
} else {
  Write-Host "== Python runtime not found (skip) =="
}

if ((Test-Path "$StageDir/application.yml") -and (Test-Path "$StageDir/python")) {
  Write-Host "== Update Python path in application.yml =="
  (Get-Content "$StageDir/application.yml") |
    ForEach-Object { $_ -replace '^(\s*python:\s*).*$', '$1python\python.exe' } |
    Set-Content "$StageDir/application.yml"
}

Write-Host "== Package EXE =="
$jpackage = "$env:JAVA_HOME/bin/jpackage.exe"
if (-not (Test-Path $jpackage)) {
  throw "jpackage not found. Ensure JDK 17+ is installed and JAVA_HOME is set."
}

$arguments = @(
  "--type", "exe",
  "--dest", $DistDir,
  "--name", $AppName,
  "--app-version", $AppVersion,
  "--input", $StageDir,
  "--main-jar", $JarPath.Name,
  "--resource-dir", $DistDir,
  "--java-options", "-Dspring.config.additional-location=./application.yml"
)

& $jpackage @arguments

if ($PythonHome -ne "") {
  Write-Host "== Python runtime provided =="
  Write-Host "Set ocr.worker.python to: $PythonHome\\python.exe in application.yml"
} else {
  Write-Host "== Python runtime not bundled =="
  Write-Host "Ensure Python is installed and in PATH, or set ocr.worker.python in application.yml"
}

Write-Host "Done. Output in dist/"
