[CmdletBinding()]
param(
    [Parameter(DontShow)]
    [string]$MavenWrapperPath,

    [Parameter(DontShow)]
    [string]$JpackagePath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$clientRoot = $PSScriptRoot
$repositoryRoot = (Resolve-Path (Join-Path $clientRoot "..")).Path
$runningOnWindows = [System.Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT
$runningOnMacOs = [System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform(
    [System.Runtime.InteropServices.OSPlatform]::OSX)

if ([string]::IsNullOrWhiteSpace($MavenWrapperPath)) {
    $wrapperName = if ($runningOnWindows) { "mvnw.cmd" } else { "mvnw" }
    $MavenWrapperPath = Join-Path $repositoryRoot $wrapperName
}

if (-not (Test-Path -LiteralPath $MavenWrapperPath -PathType Leaf)) {
    Write-Error "Maven wrapper was not found at '$MavenWrapperPath'."
    exit 1
}

if ([string]::IsNullOrWhiteSpace($JpackagePath)) {
    $jpackageExecutable = if ($runningOnWindows) { "jpackage.exe" } else { "jpackage" }
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $javaHomeCandidate = Join-Path $env:JAVA_HOME "bin/$jpackageExecutable"
        if (Test-Path -LiteralPath $javaHomeCandidate -PathType Leaf) {
            $JpackagePath = $javaHomeCandidate
        }
    }

    if ([string]::IsNullOrWhiteSpace($JpackagePath)) {
        $jpackageCommand = Get-Command $jpackageExecutable -ErrorAction SilentlyContinue
        if ($null -ne $jpackageCommand) {
            $JpackagePath = $jpackageCommand.Source
        }
    }
}

if ([string]::IsNullOrWhiteSpace($JpackagePath) -or
        -not (Test-Path -LiteralPath $JpackagePath -PathType Leaf)) {
    Write-Error "jpackage was not found. Set JAVA_HOME to a JDK 17 or newer installation that includes jpackage."
    exit 1
}

function Invoke-ClientMaven {
    param(
        [Parameter(Mandatory)]
        [string[]]$Arguments
    )

    if ($runningOnWindows) {
        & $MavenWrapperPath @Arguments
    }
    else {
        & bash $MavenWrapperPath @Arguments
    }

    if ($LASTEXITCODE -ne 0) {
        throw "JavaFX client Maven build failed with exit code $LASTEXITCODE."
    }
}

$exitCode = 0
Push-Location $repositoryRoot

try {
    Write-Host "Running JavaFX client tests..."
    Invoke-ClientMaven -Arguments @("-f", "javafx-client/pom.xml", "clean", "test")

    Write-Host "Preparing the JavaFX client packaging input..."
    Invoke-ClientMaven -Arguments @("-f", "javafx-client/pom.xml", "package", "-DskipTests")

    $targetDirectory = Join-Path $clientRoot "target"
    $packageInputDirectory = Join-Path $targetDirectory "package-input"
    $clientJar = Join-Path $targetDirectory "team-access-hub-client.jar"
    $packagedClientJar = Join-Path $packageInputDirectory "team-access-hub-client.jar"
    $distributionDirectory = Join-Path $targetDirectory "distribution"

    if (-not (Test-Path -LiteralPath $clientJar -PathType Leaf)) {
        throw "The client JAR was not produced at '$clientJar'."
    }

    Copy-Item -LiteralPath $clientJar -Destination $packagedClientJar -Force
    New-Item -ItemType Directory -Path $distributionDirectory -Force | Out-Null

    Write-Host "Creating the TeamAccessHub application image..."
    $jpackageArguments = @(
        "--type", "app-image",
        "--name", "TeamAccessHub",
        "--dest", $distributionDirectory,
        "--input", $packageInputDirectory,
        "--main-jar", "team-access-hub-client.jar",
        "--main-class", "com.badereddine.client.Launcher",
        "--app-version", "1.0.0",
        "--vendor", "Team Access Hub",
        "--description", "Team Access Hub desktop identity and access operations console"
    )
    & $JpackagePath @jpackageArguments

    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed with exit code $LASTEXITCODE."
    }

    $applicationImage = if ($runningOnMacOs) {
        Join-Path $distributionDirectory "TeamAccessHub.app"
    }
    else {
        Join-Path $distributionDirectory "TeamAccessHub"
    }

    $launcher = if ($runningOnWindows) {
        Join-Path $applicationImage "TeamAccessHub.exe"
    }
    elseif ($runningOnMacOs) {
        Join-Path $applicationImage "Contents/MacOS/TeamAccessHub"
    }
    else {
        Join-Path $applicationImage "bin/TeamAccessHub"
    }

    if (-not (Test-Path -LiteralPath $applicationImage -PathType Container) -or
            -not (Test-Path -LiteralPath $launcher -PathType Leaf)) {
        throw "jpackage completed without producing the expected launcher at '$launcher'."
    }

    Write-Host "Application image created at '$applicationImage'."
}
catch {
    Write-Error $_.Exception.Message
    $exitCode = 1
}
finally {
    Pop-Location
}

exit $exitCode
