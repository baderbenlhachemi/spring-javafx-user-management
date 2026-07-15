[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$verifyScript = Join-Path $PSScriptRoot "verify.ps1"
$powerShell = Join-Path $PSHOME "powershell.exe"
$temporaryDirectory = Join-Path ([System.IO.Path]::GetTempPath()) ("team-access-hub-verify-" + [guid]::NewGuid())
$fakeMavenWrapper = Join-Path $temporaryDirectory "mvnw.cmd"

function Assert-Equal {
    param(
        [Parameter(Mandatory)]
        $Expected,

        [Parameter(Mandatory)]
        $Actual,

        [Parameter(Mandatory)]
        [string]$Message
    )

    if ($Expected -ne $Actual) {
        throw "$Message Expected '$Expected' but found '$Actual'."
    }
}

function Invoke-VerificationScenario {
    param(
        [Parameter(Mandatory)]
        [string]$Name,

        [Parameter(Mandatory)]
        [string]$FailedModule,

        [Parameter(Mandatory)]
        [int]$ExpectedExitCode,

        [Parameter(Mandatory)]
        [string[]]$ExpectedCommands
    )

    $logPath = Join-Path $temporaryDirectory "$Name.log"
    $env:VERIFY_TEST_LOG = $logPath
    $env:VERIFY_TEST_FAILED_MODULE = $FailedModule

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"

    try {
        & $powerShell -NoProfile -ExecutionPolicy Bypass -File $verifyScript -MavenWrapperPath $fakeMavenWrapper *> $null
        $actualExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    $actualCommands = @()

    if (Test-Path -LiteralPath $logPath) {
        $actualCommands = @(Get-Content -LiteralPath $logPath)
    }

    Assert-Equal $ExpectedExitCode $actualExitCode "$Name returned the wrong exit code."
    Assert-Equal $ExpectedCommands.Count $actualCommands.Count "$Name invoked the wrong number of modules."

    for ($index = 0; $index -lt $ExpectedCommands.Count; $index++) {
        Assert-Equal $ExpectedCommands[$index] $actualCommands[$index] "$Name invoked modules in the wrong order."
    }
}

New-Item -ItemType Directory -Path $temporaryDirectory | Out-Null

try {
    @'
@echo off
>>"%VERIFY_TEST_LOG%" echo %*
if /I "%VERIFY_TEST_FAILED_MODULE%"=="backend" if "%~1"=="test" exit /b 17
if /I "%VERIFY_TEST_FAILED_MODULE%"=="client" if "%~1"=="-f" exit /b 23
exit /b 0
'@ | Set-Content -LiteralPath $fakeMavenWrapper -Encoding Ascii

    Invoke-VerificationScenario `
        -Name "success" `
        -FailedModule "none" `
        -ExpectedExitCode 0 `
        -ExpectedCommands @("test", "-f javafx-client\pom.xml test")

    Invoke-VerificationScenario `
        -Name "backend-failure" `
        -FailedModule "backend" `
        -ExpectedExitCode 1 `
        -ExpectedCommands @("test")

    Invoke-VerificationScenario `
        -Name "client-failure" `
        -FailedModule "client" `
        -ExpectedExitCode 1 `
        -ExpectedCommands @("test", "-f javafx-client\pom.xml test")
}
finally {
    Remove-Item Env:VERIFY_TEST_LOG -ErrorAction SilentlyContinue
    Remove-Item Env:VERIFY_TEST_FAILED_MODULE -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host "Verification script tests passed (3 scenarios)."
