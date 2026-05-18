$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$jarPath = Join-Path $root "build/libs/taskflow-api.jar"

if (-not (Test-Path $jarPath)) {
    & (Join-Path $PSScriptRoot "build.ps1")
}

java -jar $jarPath

