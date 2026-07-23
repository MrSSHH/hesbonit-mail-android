@echo off
set "psfile=%temp%\build_android_%random%.ps1"
more +7 "%~f0" > "%psfile%"
powershell -NoProfile -ExecutionPolicy Bypass -File "%psfile%"
del "%psfile%"
pause
exit /b

# =========================================================================
# HIGH-SPEED AUTOMATED TOOL INSTALLER & GRADLE BUILD SCRIPT
# =========================================================================

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

# 0b. Auto-create or fix gradle.properties for AndroidX & UTF-8
if (-not (Test-Path "gradle.properties")) {
    Set-Content -Path "gradle.properties" -Value "android.useAndroidX=true`norg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8" -Encoding ascii
} else {
    $gp = Get-Content "gradle.properties" -Raw
    if ($gp -notmatch "android.useAndroidX=true") {
        Add-Content "gradle.properties" "`nandroid.useAndroidX=true`norg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8"
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
"sdk.dir=$EscapedSdkPath" | Out-File -FilePath "$ProjectRoot\local.properties" -Encoding ascii

# 5. Accept Android Licenses & Install Android SDK 34
Write-Host "`n[4/6] Accepting Android SDK Licenses..." -ForegroundColor Cyan
cmd.exe /c "set JAVA_HOME=$JdkDir&& echo y | `"$SdkManager`" --licenses" | Out-Null

Write-Host "[5/6] Installing Android Platform 34 & Build Tools..." -ForegroundColor Cyan
cmd.exe /c "set JAVA_HOME=$JdkDir&& `"$SdkManager`" `"platforms;android-34`" `"build-tools;34.0.0`" `"platform-tools`"" | Out-Null

# 6. Build Debug APK
Write-Host "`n[6/6] Compiling and Building Android APK..." -ForegroundColor Cyan
cmd.exe /c "set JAVA_HOME=$JdkDir&& `"$GradleDir\bin\gradle.bat`" assembleDebug"

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