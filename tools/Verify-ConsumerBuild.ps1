<#
.SYNOPSIS
Compiles the consumer fixture from src/example/java as an independent project.

.DESCRIPTION
This is the cross-project build evidence for the public API: it compiles the same source that the
in-repo example source set builds, but it takes the CRL Hitbox classes only from the packaged dev
artifact and the platform classes only from the local Minecraft/Forge jar, writing its output outside
the repository. It never uses the repository's own build output directories as an input, so a passing
run proves the public API is consumable from a separate project.

It does not run the game, does not load the mod, and does not publish anything.

.NOTES
Run after '.\gradlew.bat build' so the dev artifact exists.
#>
[CmdletBinding()]
param(
    [string]$Jdk = 'D:\Program Files\Zulu\zulu-25',
    [string]$DevJar = 'build/libs/crlhitbox-0.1.0-SNAPSHOT-dev.jar',
    [string]$OutputDirectory = 'D:\Code\CrlHitbox-consumer\classes',
    [string]$Sources = 'src/example/java'
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $DevJar)) {
    throw "dev artifact not found: $DevJar (run '.\gradlew.bat build' first)"
}
if (-not (Test-Path (Join-Path $Jdk 'bin/javac.exe'))) {
    throw "javac not found under $Jdk"
}

$candidates = Get-ChildItem 'D:\gradle\caches\unimined\net\minecraft\minecraft\1.12.2' -Recurse `
        -Filter 'forge-*-mcp.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch 'sources|searge' }
$platformJar = $candidates | Where-Object { $_.FullName -match '0\.6\.8-alpha' } | Select-Object -First 1
if (-not $platformJar) {
    $platformJar = $candidates | Select-Object -First 1
}
if (-not $platformJar) {
    throw 'local Minecraft/Forge mcp jar not found under D:\gradle\caches\unimined'
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$sources = Get-ChildItem $Sources -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
if ($sources.Count -eq 0) {
    throw "no consumer sources under $Sources"
}

$classpath = "$DevJar;$($platformJar.FullName)"
Write-Host "dev artifact : $((Get-Item $DevJar).FullName)"
Write-Host "platform jar : $($platformJar.FullName)"
Write-Host "output       : $OutputDirectory"

& (Join-Path $Jdk 'bin/javac.exe') -encoding UTF-8 -cp $classpath -d $OutputDirectory $sources
if ($LASTEXITCODE -ne 0) {
    throw "independent consumer compilation failed with exit code $LASTEXITCODE"
}

$classes = Get-ChildItem $OutputDirectory -Recurse -Filter '*.class'
Write-Host "compiled $($classes.Count) class file(s) outside the repository:"
foreach ($class in $classes) {
    Write-Host ("  {0}  {1} bytes  {2}" -f $class.Name, $class.Length,
            (Get-FileHash $class.FullName -Algorithm SHA256).Hash)
}
