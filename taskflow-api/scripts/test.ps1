$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$mainSource = Join-Path $root "src/main/java"
$testSource = Join-Path $root "src/test/java"
$buildRoot = Join-Path $root "build"
$mainClasses = Join-Path $buildRoot "classes/main"
$testClasses = Join-Path $buildRoot "classes/test"
$separator = [IO.Path]::PathSeparator

New-Item -ItemType Directory -Force -Path $mainClasses, $testClasses | Out-Null

$mainFiles = Get-ChildItem -Path $mainSource -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
$testFiles = Get-ChildItem -Path $testSource -Recurse -Filter *.java | Select-Object -ExpandProperty FullName

if (-not $mainFiles) {
    throw "No Java source files found under $mainSource"
}

if (-not $testFiles) {
    throw "No Java test files found under $testSource"
}

javac -encoding UTF-8 -d $mainClasses $mainFiles
javac -encoding UTF-8 -cp $mainClasses -d $testClasses $testFiles

$classPath = "$mainClasses$separator$testClasses"
$testEntrypoints = @(
    "com.lielstephen.taskflow.TaskServiceTest",
    "com.lielstephen.taskflow.TaskApiIntegrationTest"
)

foreach ($testClass in $testEntrypoints) {
    java -ea -cp $classPath $testClass
}

Write-Host "All tests passed."

