# Quick build & deploy to test server
$ErrorActionPreference = "Stop"
$mvn = "..\skybound\.tools\apache-maven-3.9.9\bin\mvn.cmd"
$target = (Get-ChildItem "C:\Users\Reil\Desktop" -Directory | Where-Object { $_.Name -match '5\)$' }).FullName
$pluginsDir = "$target\plugins"

Write-Host "Building SkyBound..." -ForegroundColor Cyan
& $mvn clean package -q 2>$null

$jar = "skybound-core\target\skybound-core-2.0.0-SNAPSHOT.jar"
if (!(Test-Path $jar)) {
    Write-Host "BUILD FAILED - jar not found" -ForegroundColor Red
    exit 1
}

if (!(Test-Path $pluginsDir)) {
    New-Item -ItemType Directory -Path $pluginsDir -Force | Out-Null
}

Copy-Item $jar "$pluginsDir\SkyBound.jar" -Force
Write-Host "Deployed to: $pluginsDir\SkyBound.jar" -ForegroundColor Green
Write-Host "Restart your server to load the plugin." -ForegroundColor Yellow
