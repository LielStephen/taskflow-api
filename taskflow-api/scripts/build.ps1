$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$mainSource = Join-Path $root "src/main/java"
$buildRoot = Join-Path $root "build"
$mainClasses = Join-Path $buildRoot "classes/main"
$libs = Join-Path $buildRoot "libs"
$jarPath = Join-Path $libs "taskflow-api.jar"

if (Test-Path $buildRoot) {
    Remove-Item -Recurse -Force $buildRoot
}

New-Item -ItemType Directory -Force -Path $mainClasses, $libs | Out-Null
$mainFiles = Get-ChildItem -Path $mainSource -Recurse -Filter *.java | Select-Object -ExpandProperty FullName

if (-not $mainFiles) {
    throw "No Java source files found under $mainSource"
}

javac -encoding UTF-8 -d $mainClasses $mainFiles
jar --create --file $jarPath --main-class com.lielstephen.taskflow.TaskflowApplication -C $mainClasses .

Write-Host "Built $jarPath"

