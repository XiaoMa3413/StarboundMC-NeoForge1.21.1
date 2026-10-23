# Workspace-scoped Java/Gradle environment; does not change system settings.
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$GradleArgs)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$workspaceRoot = Split-Path $projectRoot -Parent
$jdkRoot = Join-Path $workspaceRoot '.setup\jdk21\jdk-21.0.12.1+1'
if (!(Test-Path (Join-Path $jdkRoot 'bin\javac.exe'))) {
    throw 'JDK 21 missing. Install the workspace JDK or update this script to your JDK 21 path.'
}
$previousJava = $env:JAVA_HOME
$previousGradle = $env:GRADLE_USER_HOME
try {
    $env:JAVA_HOME = $jdkRoot
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.gradle-home'
    Push-Location $projectRoot
    try {
        & .\gradlew.bat @GradleArgs
        $result = $LASTEXITCODE
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJava
    $env:GRADLE_USER_HOME = $previousGradle
}
exit $result
