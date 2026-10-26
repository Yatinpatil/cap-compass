$ErrorActionPreference = 'Stop'

$javaCandidates = @(
    'C:\Program Files\Android\Android Studio\jbr',
    "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
    $env:JAVA_HOME
)

$javaHome = $javaCandidates |
    Where-Object { $_ -and (Test-Path (Join-Path $_ 'bin\java.exe')) } |
    Select-Object -First 1

if (-not $javaHome) {
    throw 'Java 17 or newer was not found. Install Android Studio or set JAVA_HOME.'
}

if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
    throw 'Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT.'
}

$env:JAVA_HOME = $javaHome

$projectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $projectRoot

try {
    & npm run build
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    & npx cap sync android
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

    Push-Location (Join-Path $projectRoot 'android')
    try {
        & .\gradlew.bat assembleDebug
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    }
    finally {
        Pop-Location
    }

    Write-Output (Join-Path $projectRoot 'android\app\build\outputs\apk\debug\app-debug.apk')
}
finally {
    Pop-Location
}
