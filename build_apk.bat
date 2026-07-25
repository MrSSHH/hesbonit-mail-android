@echo off
:: =========================================================================
:: HIGH-SPEED AUTOMATED TOOL INSTALLER & GRADLE BUILD SCRIPT
:: =========================================================================
powershell -NoProfile -ExecutionPolicy Bypass -Command "$s=[System.IO.File]::ReadAllText('%~f0'); Invoke-Expression $s.Substring($s.IndexOf('#PS_' + 'START'))"
exit /b %ERRORLEVEL%

#PS_START
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$ProjectRoot = Get-Location
$ToolsDir = "$ProjectRoot\.tools"
$JdkDir = "$ToolsDir\jdk17"
$SdkDir = "$ToolsDir\android-sdk"
$GradleDir = "$ToolsDir\gradle-8.7"

function Fast-Download($url, $output) {
    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
        & curl.exe -L -s -S --output "$output" "$url"
    } else {
        Invoke-WebRequest -Uri $url -OutFile $output -UseBasicParsing
    }
}

# 0a. Ignore .tools in git
if (Test-Path ".gitignore") {
    $gitignore = Get-Content ".gitignore" -Raw
    if ($gitignore -notmatch "\.tools") {
        Add-Content ".gitignore" "`n.tools/"
    }
}

# 0b. Auto-create or fix gradle.properties
$gpPath = "$ProjectRoot\gradle.properties"
$gpContent = "android.useAndroidX=true`norg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8"
if (-not (Test-Path $gpPath)) {
    [System.IO.File]::WriteAllText($gpPath, $gpContent)
} else {
    $gp = Get-Content $gpPath -Raw
    if ($gp -notmatch "android.useAndroidX=true") {
        Add-Content $gpPath "`n$gpContent"
    }
}

# 1. Download & Setup Portable OpenJDK 17
if (-not (Test-Path "$JdkDir\bin\java.exe")) {
    Write-Host "`n[1/6] Fast-downloading OpenJDK 17 (Temurin)..." -ForegroundColor Cyan
    New-Item -ItemType Directory -Path "$ToolsDir" -Force | Out-Null
    $JdkZip = "$ToolsDir\jdk17.zip"
    $JdkUrl = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/adoptium?project=jdk"
    Fast-Download -url $JdkUrl -output $JdkZip
    
    Write-Host "Extracting OpenJDK 17..." -ForegroundColor Cyan
    Expand-Archive -Path $JdkZip -DestinationPath "$ToolsDir\jdk_extract" -Force
    $ExtractedFolder = Get-ChildItem -Path "$ToolsDir\jdk_extract" | Select-Object -First 1
    Move-Item -Path $ExtractedFolder.FullName -Destination "$JdkDir" -Force
    Remove-Item -Path $JdkZip -Force
    Remove-Item -Path "$ToolsDir\jdk_extract" -Recurse -Force
    Write-Host "JDK 17 ready!" -ForegroundColor Green
} else {
    Write-Host "`n[1/6] OpenJDK 17 already present." -ForegroundColor Green
}

# Force JDK 17 explicitly in environment
$env:JAVA_HOME = $JdkDir
$env:ANDROID_HOME = $SdkDir
$env:PATH = "$JdkDir\bin;$GradleDir\bin;$SdkDir\cmdline-tools\latest\bin;$SdkDir\platform-tools;$env:PATH"

# 2. Download & Setup Portable Gradle 8.7
if (-not (Test-Path "$GradleDir\bin\gradle.bat")) {
    Write-Host "`n[2/6] Fast-downloading Gradle 8.7..." -ForegroundColor Cyan
    $GradleZip = "$ToolsDir\gradle.zip"
    $GradleUrl = "https://services.gradle.org/distributions/gradle-8.7-bin.zip"
    Fast-Download -url $GradleUrl -output $GradleZip
    
    Write-Host "Extracting Gradle 8.7..." -ForegroundColor Cyan
    Expand-Archive -Path $GradleZip -DestinationPath "$ToolsDir" -Force
    Remove-Item -Path $GradleZip -Force
    Write-Host "Gradle 8.7 ready!" -ForegroundColor Green
} else {
    Write-Host "`n[2/6] Gradle 8.7 already present." -ForegroundColor Green
}

# 3. Download & Setup Android SDK Command Line Tools
$SdkManager = "$SdkDir\cmdline-tools\latest\bin\sdkmanager.bat"
if (-not (Test-Path $SdkManager)) {
    Write-Host "`n[3/6] Fast-downloading Android SDK Command Line Tools..." -ForegroundColor Cyan
    $SdkZip = "$ToolsDir\cmdline.zip"
    $SdkUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
    Fast-Download -url $SdkUrl -output $SdkZip
    
    Write-Host "Extracting Android Command Line Tools..." -ForegroundColor Cyan
    Expand-Archive -Path $SdkZip -DestinationPath "$ToolsDir\sdk_extract" -Force
    
    New-Item -ItemType Directory -Path "$SdkDir\cmdline-tools\latest" -Force | Out-Null
    Get-ChildItem -Path "$ToolsDir\sdk_extract\cmdline-tools\*" | Move-Item -Destination "$SdkDir\cmdline-tools\latest" -Force
    
    Remove-Item -Path $SdkZip -Force
    Remove-Item -Path "$ToolsDir\sdk_extract" -Recurse -Force
    Write-Host "Android Command Line Tools ready!" -ForegroundColor Green
} else {
    Write-Host "`n[3/6] Android Command Line Tools already present." -ForegroundColor Green
}

# 4. Configure local.properties
$EscapedSdkPath = $SdkDir.Replace("\", "\\")
[System.IO.File]::WriteAllText("$ProjectRoot\local.properties", "sdk.dir=$EscapedSdkPath")

# 5. Pre-accept Android Licenses directly on disk
Write-Host "`n[4/6] Pre-accepting Android SDK Licenses..." -ForegroundColor Cyan
$LicensesDir = "$SdkDir\licenses"
New-Item -ItemType Directory -Path $LicensesDir -Force | Out-Null
[System.IO.File]::WriteAllText("$LicensesDir\android-sdk-license", "`n89339ba1755934688242868438d119ad78e47319`nd56f518e8030a300085a80d45d8122393325010b`n24333f8a63718c309afe8d847f00351287710e6f")
[System.IO.File]::WriteAllText("$LicensesDir\android-sdk-preview-license", "`n84831b9409646a918e30573bab4c9c91346d8abd")

# 6. Install Android SDK Components (Output visible)
Write-Host "[5/6] Installing Android Platform 34 & Build Tools..." -ForegroundColor Cyan
& "$SdkManager" "platforms;android-34" "build-tools;34.0.0" "platform-tools"

# 7. Build Debug APK
Write-Host "`n[6/6] Compiling and Building Android APK..." -ForegroundColor Cyan
& "$GradleDir\bin\gradle.bat" assembleDebug

$ApkPath = "$ProjectRoot\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $ApkPath) {
    Write-Host "`n========================================================" -ForegroundColor Yellow
    Write-Host " BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host " APK File Location:" -ForegroundColor White
    Write-Host " $ApkPath" -ForegroundColor Cyan
    Write-Host "========================================================" -ForegroundColor Yellow
} else {
    Write-Host "`nBuild complete. Check app\build\outputs\apk for output." -ForegroundColor Yellow
}