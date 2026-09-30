<#
.SYNOPSIS
Compiles the consumer fixture from src/example/java as an independent project.

.DESCRIPTION
This is the cross-project build evidence for the public API: it compiles the same source that the
in-repo example source set builds, but it takes the CRL Hitbox classes only from the packaged dev
artifact and the platform classes only from the local Minecraft/Forge jar, writing its output outside
the repository's own build output directories. It never puts those directories on its classpath, so a
passing run proves the public API is consumable from a separate project.

All paths are repository-relative or taken from the environment; nothing host-specific is stored in
this file. The script does not run the game, does not load the mod, and does not publish anything.

.NOTES
Run after '.\gradlew.bat build' so the dev artifact exists.
#>
[CmdletBinding()]
param(
    [string]$Jdk = $env:JAVA_HOME,
    [string]$DevJar = 'build/libs/crlhitbox-0.1.0-SNAPSHOT-dev.jar',
    [string]$OutputDirectory = 'build/consumer-classes',
    [string]$Sources = 'src/example/java',
    [string]$CacheRoot = $env:GRADLE_USER_HOME
)

$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($Jdk)) {
    throw 'set JAVA_HOME or pass -Jdk <path to a JDK 25 home>'
}
if (-not (Test-Path (Join-Path $Jdk 'bin/javac.exe'))) {
    throw "javac not found under the JDK home '$Jdk'"
}
if (-not (Test-Path $DevJar)) {
    throw "dev artifact not found: $DevJar (run '.\gradlew.bat build' first)"
}

# Locate the platform jar through the Gradle cache root rather than a recorded absolute path.
$searchRoots = @()
if (-not [string]::IsNullOrWhiteSpace($CacheRoot)) {
    $searchRoots += Join-Path $CacheRoot 'caches/unimined/net/minecraft/minecraft'
}
$searchRoots += Join-Path (Get-Location) 'build/platform-jars'
$searchRoots = $searchRoots | Where-Object { Test-Path $_ }

$platformJar = $null
foreach ($root in $searchRoots) {
    $candidate = Get-ChildItem $root -Recurse -Filter 'forge-*-mcp.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'sources|searge' } |
        Where-Object { $_.FullName -match '0\.6\.8-alpha' } |
        Select-Object -First 1
    if ($candidate) {
        $platformJar = $candidate
        break
    }
}
if (-not $platformJar) {
    throw "no 0.6.8-alpha mcp jar found under: $($searchRoots -join ', '); set GRADLE_USER_HOME or pass -CacheRoot"
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$sources = Get-ChildItem $Sources -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
if ($sources.Count -eq 0) {
    throw "no consumer sources under $Sources"
}

$classpath = "$DevJar;$($platformJar.FullName)"
Write-Host "dev artifact : $((Get-Item $DevJar).FullName)"
Write-Host "platform jar : $($platformJar.FullName)"
Write-Host "output       : $((Resolve-Path $OutputDirectory).Path)"

& (Join-Path $Jdk 'bin/javac.exe') -encoding UTF-8 -cp $classpath -d $OutputDirectory $sources
if ($LASTEXITCODE -ne 0) {
    throw "independent consumer compilation failed with exit code $LASTEXITCODE"
}

$classes = Get-ChildItem $OutputDirectory -Recurse -Filter '*.class'
Write-Host "compiled $($classes.Count) class file(s) outside the repository build output:"
foreach ($class in $classes) {
    Write-Host ("  {0}  {1} bytes  {2}" -f $class.Name, $class.Length,
            (Get-FileHash $class.FullName -Algorithm SHA256).Hash)
}
