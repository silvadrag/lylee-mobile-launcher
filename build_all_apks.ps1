$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $rootDir

$collectedDir = Join-Path $rootDir "release-collected"
if (!(Test-Path $collectedDir)) {
    New-Item -ItemType Directory -Path $collectedDir | Out-Null
}

$arches = @("all", "arm64", "arm", "x86", "x86_64")

foreach ($arch in $arches) {
    Write-Host "=========================================="
    Write-Host "Building APK for arch: $arch ..."
    Write-Host "=========================================="
    
    cmd.exe /c "gradlew.bat :FCL:assembleRelease -Darch=$arch"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build failed for arch: $arch"
        exit 1
    }
    
    $apkSourceDir = Join-Path $rootDir "FCL\build\outputs\apk\release"
    $apks = Get-ChildItem -Path $apkSourceDir -Filter "*.apk"
    foreach ($apk in $apks) {
        Write-Host "Copying $($apk.Name) to release-collected/ ..."
        Copy-Item -Path $apk.FullName -Destination (Join-Path $collectedDir $apk.Name) -Force
    }
}

Write-Host "=========================================="
Write-Host "All 5 APKs built successfully:"
Get-ChildItem -Path $collectedDir -Filter "*.apk" | Select-Object Name, Length, LastWriteTime | Format-Table -AutoSize
Write-Host "=========================================="
