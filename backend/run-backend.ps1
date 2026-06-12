$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:SPRING_PROFILES_ACTIVE='local'

if ([string]::IsNullOrWhiteSpace($env:KIS_ENABLED)) {
    $env:KIS_ENABLED='false'
}

if ($env:KIS_ENABLED -eq 'true' -and ([string]::IsNullOrWhiteSpace($env:KIS_APP_KEY) -or [string]::IsNullOrWhiteSpace($env:KIS_APP_SECRET))) {
    Write-Error 'KIS_ENABLED=true requires KIS_APP_KEY and KIS_APP_SECRET environment variables.'
    exit 1
}

Set-Location $PSScriptRoot
.\gradlew.bat bootRun
