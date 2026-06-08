# ==============================================================
#  MoneyTransfer JEE - Setup Automatique Windows
#  Lancer en PowerShell ADMINISTRATEUR :
#  Set-ExecutionPolicy Bypass -Scope Process -Force
#  .\setup-windows.ps1
# ==============================================================

$ErrorActionPreference = "Stop"

function log     { param($msg) Write-Host "[OK] $msg" -ForegroundColor Green }
function warn    { param($msg) Write-Host "[WARN] $msg" -ForegroundColor Yellow }
function section { param($msg) Write-Host "`n=== $msg ===" -ForegroundColor Cyan }
function fail    { param($msg) Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

if (-NOT ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    fail "Lance ce script en PowerShell ADMINISTRATEUR"
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   MoneyTransfer JEE - Setup Windows"           -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# ==============================================================
# ETAPE 0 - .env local
# ==============================================================
section "Configuration locale"
$envFile = Join-Path $PSScriptRoot ".env"
$envExampleFile = Join-Path $PSScriptRoot ".env.example"
if (-not (Test-Path $envFile) -and (Test-Path $envExampleFile)) {
    Copy-Item $envExampleFile $envFile
    warn ".env cree a partir de .env.example - mets a jour les mots de passe avant de partager le projet"
} elseif (Test-Path $envFile) {
    log ".env detecte"
} else {
    warn ".env.example non trouve"
}

# ==============================================================
# ETAPE 1 - Java
# ==============================================================
section "Java"
try {
    $javaOut = java -version 2>&1 | Out-String
    $versionNumber = [regex]::Match($javaOut, '"(\d+)').Groups[1].Value
    if ([int]$versionNumber -ge 21) {
        log "Java $versionNumber detecte - OK"
    } else {
        warn "Java $versionNumber trop ancien - Installation Java 21..."
        winget install --id EclipseAdoptium.Temurin.21.JDK --silent --accept-package-agreements --accept-source-agreements
        log "Java 21 installe"
    }
} catch {
    warn "Java non trouve - Installation Java 21..."
    winget install --id EclipseAdoptium.Temurin.21.JDK --silent --accept-package-agreements --accept-source-agreements
    log "Java 21 installe"
}

# ==============================================================
# ETAPE 2 - Chocolatey
# ==============================================================
section "Chocolatey"
if (Get-Command choco -ErrorAction SilentlyContinue) {
    log "Chocolatey deja installe"
} else {
    log "Installation Chocolatey..."
    Set-ExecutionPolicy Bypass -Scope Process -Force
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
    Invoke-Expression ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    log "Chocolatey installe"
}

# ==============================================================
# ETAPE 3 - Maven
# ==============================================================
section "Maven"
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    log "Maven deja installe"
} else {
    log "Installation Maven..."
    choco install maven --yes --no-progress
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    log "Maven installe"
}

# ==============================================================
# ETAPE 4 - IntelliJ IDEA Community
# ==============================================================
section "IntelliJ IDEA Community"
$intellijPath = Test-Path "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition*"
if ($intellijPath) {
    log "IntelliJ IDEA deja installe"
} else {
    log "Installation IntelliJ IDEA Community..."
    winget install --id JetBrains.IntelliJIDEA.Community --silent --accept-package-agreements --accept-source-agreements
    log "IntelliJ IDEA installe"
}

# ==============================================================
# ETAPE 5 - Docker
# ==============================================================
section "Docker Desktop"
if (Get-Command docker -ErrorAction SilentlyContinue) {
    $dockerVersion = (docker --version).ToString()
    log "Docker deja installe : $dockerVersion"
} else {
    log "Installation Docker Desktop..."
    winget install --id Docker.DockerDesktop --silent --accept-package-agreements --accept-source-agreements
    log "Docker Desktop installe"
    $global:needRestart = $true
}

# ==============================================================
# ETAPE 6 - Git
# ==============================================================
section "Git"
if (Get-Command git -ErrorAction SilentlyContinue) {
    log "Git deja installe : $(git --version)"
} else {
    log "Installation Git..."
    winget install --id Git.Git --silent --accept-package-agreements --accept-source-agreements
    log "Git installe"
}

# ==============================================================
# ETAPE 7 - JAVA_HOME
# ==============================================================
section "JAVA_HOME"
$javaHome = [System.Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine")
$isJRE = $javaHome -and ($javaHome -match "jre")
$isOldJava = $javaHome -and ($javaHome -match "jdk-8|jdk-11|jre-8")

if (-not $javaHome -or $isJRE -or $isOldJava) {
    $possiblePaths = @(
        "C:\Program Files\Eclipse Adoptium\jdk-21*",
        "C:\Program Files\Java\jdk-21*",
        "C:\Program Files\Microsoft\jdk-21*"
    )
    $found = $null
    foreach ($path in $possiblePaths) {
        $found = Get-Item $path -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) { break }
    }
    if ($found) {
        [System.Environment]::SetEnvironmentVariable("JAVA_HOME", $found.FullName, "Machine")
        $env:JAVA_HOME = $found.FullName
        $env:Path = $env:JAVA_HOME + "\bin;" + $env:Path
        log "JAVA_HOME defini : $($found.FullName)"
    } else {
        warn "JDK 21 non trouve - configure JAVA_HOME manuellement"
    }
} else {
    log "JAVA_HOME OK : $javaHome"
}

# ==============================================================
# ETAPE 8 - Structure du projet
# ==============================================================
section "Structure du projet"
$projectPath = Join-Path $PSScriptRoot "project"
$folders = @(
    "src\main\java\ma\transfert\dao",
    "src\main\java\ma\transfert\service",
    "src\main\java\ma\transfert\security",
    "src\main\java\ma\transfert\util",
    "src\test\java\ma\transfert"
)
foreach ($folder in $folders) {
    $fullPath = Join-Path $projectPath $folder
    if (-not (Test-Path $fullPath)) {
        New-Item -ItemType Directory -Force -Path $fullPath | Out-Null
    }
}
log "Structure du projet complete"

# ==============================================================
# ETAPE 9 - Build Maven
# ==============================================================
section "Build Maven"
$pomFile = Join-Path $projectPath "pom.xml"
if (Test-Path $pomFile) {
    Push-Location $projectPath
    log "Compilation en cours..."
    mvn clean package -DskipTests -q 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        log "Build reussi - WAR genere"
    } else {
        warn "Build echoue - verifie JAVA_HOME et relance"
    }
    Pop-Location
} else {
    warn "pom.xml non trouve"
}

# ==============================================================
# ETAPE 10 - Docker Compose (PostgreSQL + WildFly + PgAdmin)
# ==============================================================
section "Docker Compose"
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Push-Location $PSScriptRoot
    log "Construction et lancement des conteneurs..."
    docker compose down 2>&1 | Out-Null
    docker compose up -d --build
    if ($LASTEXITCODE -eq 0) {
        log "Conteneurs demarres"
        log "Attente de WildFly (15 secondes)..."
        Start-Sleep -Seconds 15
    } else {
        warn "Docker Compose echoue - verifie Docker Desktop"
    }
    Pop-Location
} else {
    warn "Docker non disponible - skip"
}

# ==============================================================
# ETAPE 11 - Deployer le WAR sur WildFly
# ==============================================================
section "Deploiement"
$warFile = Join-Path $projectPath "target\money-transfer.war"
if ((Test-Path $warFile) -and (Get-Command docker -ErrorAction SilentlyContinue)) {
    docker cp $warFile mt_wildfly:/opt/jboss/wildfly/standalone/deployments/
    if ($LASTEXITCODE -eq 0) {
        log "WAR deploye sur WildFly"
        log "Attente du deploiement (10 secondes)..."
        Start-Sleep -Seconds 10
    } else {
        warn "Deploiement echoue"
    }
} else {
    warn "WAR non trouve ou Docker non disponible"
}

# ==============================================================
# ETAPE 12 - Verification
# ==============================================================
section "Verification"
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/money-transfer/api/health" -UseBasicParsing -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        log "API fonctionne !"
        Write-Host $response.Content -ForegroundColor Green
    }
} catch {
    warn "API pas encore disponible - attends quelques secondes et ouvre :"
    Write-Host "  http://localhost:8080/money-transfer/api/health" -ForegroundColor White
}

# ==============================================================
# RESUME FINAL
# ==============================================================
Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "   Setup termine !"                              -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  API     : http://localhost:8080/money-transfer/api/health" -ForegroundColor White
Write-Host "  WildFly : http://localhost:9990 (identifiants dans .env)"  -ForegroundColor White
Write-Host "  PgAdmin : http://localhost:5050 (identifiants dans .env)"  -ForegroundColor White
Write-Host ""
Write-Host "  IntelliJ : Open > project\"                   -ForegroundColor White
Write-Host ""

if ($global:needRestart) {
    warn "Docker installe - REDEMARRE avant de relancer ce script"
}
