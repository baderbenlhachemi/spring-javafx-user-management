[CmdletBinding()]
param(
    [Parameter(DontShow)]
    [string]$MavenWrapperPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$runningOnWindows = [System.Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT

if ([string]::IsNullOrWhiteSpace($MavenWrapperPath)) {
    $wrapperName = if ($runningOnWindows) { "mvnw.cmd" } else { "mvnw" }
    $MavenWrapperPath = Join-Path $repositoryRoot $wrapperName
}

if (-not (Test-Path -LiteralPath $MavenWrapperPath -PathType Leaf)) {
    Write-Error "Maven wrapper was not found at '$MavenWrapperPath'."
    exit 1
}

function Invoke-ModuleTests {
    param(
        [Parameter(Mandatory)]
        [string]$ModuleName,

        [Parameter(Mandatory)]
        [string[]]$MavenArguments
    )

    Write-Host "Verifying $ModuleName..."

    if ($runningOnWindows) {
        & $MavenWrapperPath @MavenArguments
    }
    else {
        & bash $MavenWrapperPath @MavenArguments
    }

    if ($LASTEXITCODE -ne 0) {
        throw "$ModuleName verification failed with exit code $LASTEXITCODE."
    }
}

$exitCode = 0
Push-Location $repositoryRoot

try {
    Invoke-ModuleTests -ModuleName "backend" -MavenArguments @("test")
    Invoke-ModuleTests -ModuleName "JavaFX client" -MavenArguments @("-f", "javafx-client\pom.xml", "test")
    Write-Host "Repository verification passed."
}
catch {
    Write-Error $_.Exception.Message
    $exitCode = 1
}
finally {
    Pop-Location
}

exit $exitCode
