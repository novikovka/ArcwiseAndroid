param(
    [ValidateSet('debug', 'release', 'check')][string]$Mode = 'debug',
    [switch]$Install
)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path $PSScriptRoot -Parent
Push-Location $projectDirectory
try {
    if (!$env:JAVA_HOME) {
        $jdkCandidates = @('D:\Program Files\Android\Android Studio\jbr', "$env:ProgramFiles\Android\Android Studio\jbr")
        $env:JAVA_HOME = $jdkCandidates | Where-Object { Test-Path (Join-Path $_ 'bin\javac.exe') } | Select-Object -First 1
    }
    if (!$env:JAVA_HOME) { throw 'Set JAVA_HOME to JDK 17 or newer.' }
    if (!$env:ANDROID_HOME) { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
    if ($Mode -eq 'release' -and !(Test-Path 'signing.properties')) { throw 'Create signing.properties using signing.properties.example before building a signed release.' }
    $gradleTasks = switch ($Mode) {
        'check' { @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug', ':app:assembleDebugAndroidTest') }
        'release' { @(':app:assembleRelease') }
        default { @(':app:assembleDebug') }
    }
    & .\gradlew.bat @gradleTasks --console plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle exited with code $LASTEXITCODE" }
    if ($Install) {
        $variant = if ($Mode -eq 'release') { 'release' } else { 'debug' }
        $apk = Join-Path $projectDirectory "app\build\outputs\apk\$variant\app-$variant.apk"
        & (Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe') install -r $apk
        if ($LASTEXITCODE -ne 0) { throw 'APK installation failed.' }
    }
} finally { Pop-Location }
