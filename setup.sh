#!/bin/bash
# =============================================================
#  MoneyTransfer JEE — Script de setup automatique
#  Usage : bash setup.sh
#  Testé sur : Ubuntu 22.04 / macOS 13+
# =============================================================

set -e  # stop on first error

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

WILDFLY_VERSION="40.0.0.Final"
WILDFLY_URL="https://github.com/wildfly/wildfly/releases/download/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.zip"
JAVA_MIN_VERSION=21
PROJECT_DIR="$(pwd)/money-transfer"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
ENV_EXAMPLE_FILE="$SCRIPT_DIR/.env.example"

if [ ! -f "$ENV_FILE" ] && [ -f "$ENV_EXAMPLE_FILE" ]; then
  cp "$ENV_EXAMPLE_FILE" "$ENV_FILE"
fi

if [ -f "$ENV_FILE" ]; then
  set -a
  . "$ENV_FILE"
  set +a
fi

DB_NAME="${POSTGRES_DB:-money_transfer_db}"
DB_USER="${POSTGRES_USER:-mt_user}"
DB_PASSWORD="${POSTGRES_PASSWORD:-change-me-db-password}"
WILDFLY_ADMIN_USER="${WILDFLY_ADMIN_USER:-admin}"
WILDFLY_ADMIN_PASS="${WILDFLY_ADMIN_PASSWORD:-change-me-wildfly-password}"

log()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()   { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()  { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }
section(){ echo -e "\n${BLUE}=== $1 ===${NC}"; }

# ─────────────────────────────────────────────
# 0. Détection OS
# ─────────────────────────────────────────────
detect_os() {
  if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="linux"
    if command -v apt-get &>/dev/null; then PKG="apt"; fi
  elif [[ "$OSTYPE" == "darwin"* ]]; then
    OS="mac"
    PKG="brew"
  else
    error "OS non supporté : $OSTYPE. Utilise Linux ou macOS."
  fi
  log "OS détecté : $OS ($PKG)"
}

# ─────────────────────────────────────────────
# 1. Java 21+
# ─────────────────────────────────────────────
install_java() {
  section "Java"
  if command -v java &>/dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge "$JAVA_MIN_VERSION" ]; then
      log "Java $JAVA_VERSION déjà installé"
      return
    fi
    warn "Java $JAVA_VERSION trop ancien — installation de Java 21"
  fi

  if [ "$PKG" = "apt" ]; then
    sudo apt-get update -qq
    sudo apt-get install -y openjdk-21-jdk
  elif [ "$PKG" = "brew" ]; then
    brew install openjdk@21
    sudo ln -sfn $(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
    echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
  fi
  log "Java 21 installé"
}

# ─────────────────────────────────────────────
# 2. Maven
# ─────────────────────────────────────────────
install_maven() {
  section "Maven"
  if command -v mvn &>/dev/null; then
    log "Maven $(mvn -v | head -1) déjà installé"
    return
  fi
  if [ "$PKG" = "apt" ]; then
    sudo apt-get install -y maven
  elif [ "$PKG" = "brew" ]; then
    brew install maven
  fi
  log "Maven installé"
}

# ─────────────────────────────────────────────
# 3. PostgreSQL
# ─────────────────────────────────────────────
install_postgres() {
  section "PostgreSQL"
  if command -v psql &>/dev/null; then
    log "PostgreSQL déjà installé"
  else
    if [ "$PKG" = "apt" ]; then
      sudo apt-get install -y postgresql postgresql-contrib
      sudo systemctl enable postgresql
      sudo systemctl start postgresql
    elif [ "$PKG" = "brew" ]; then
      brew install postgresql@15
      brew services start postgresql@15
      echo 'export PATH="/opt/homebrew/opt/postgresql@15/bin:$PATH"' >> ~/.zshrc
      export PATH="/opt/homebrew/opt/postgresql@15/bin:$PATH"
    fi
    log "PostgreSQL installé"
  fi

  # Créer user + base de données
  if [ "$OS" = "linux" ]; then
    sudo -u postgres psql -tc "SELECT 1 FROM pg_user WHERE usename='$DB_USER'" | grep -q 1 || \
      sudo -u postgres psql -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';"
    sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" | grep -q 1 || \
      sudo -u postgres psql -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;"
  else
    psql postgres -tc "SELECT 1 FROM pg_user WHERE usename='$DB_USER'" | grep -q 1 || \
      psql postgres -c "CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';"
    psql postgres -tc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" | grep -q 1 || \
      psql postgres -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"
    psql postgres -c "GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;"
  fi
  log "Base de données '$DB_NAME' créée (user: $DB_USER)"
}

# ─────────────────────────────────────────────
# 4. WildFly
# ─────────────────────────────────────────────
install_wildfly() {
  section "WildFly $WILDFLY_VERSION"
  WILDFLY_HOME="$HOME/wildfly-${WILDFLY_VERSION}"

  if [ -d "$WILDFLY_HOME" ]; then
    log "WildFly déjà installé dans $WILDFLY_HOME"
  else
    log "Téléchargement de WildFly $WILDFLY_VERSION..."
    curl -L "$WILDFLY_URL" -o /tmp/wildfly.zip
    unzip -q /tmp/wildfly.zip -d "$HOME"
    rm /tmp/wildfly.zip
    log "WildFly extrait dans $WILDFLY_HOME"
  fi

  # Exporter WILDFLY_HOME
  export WILDFLY_HOME="$HOME/wildfly-${WILDFLY_VERSION}"
  grep -q "WILDFLY_HOME" ~/.bashrc 2>/dev/null || echo "export WILDFLY_HOME=$WILDFLY_HOME" >> ~/.bashrc
  grep -q "WILDFLY_HOME" ~/.zshrc  2>/dev/null || echo "export WILDFLY_HOME=$WILDFLY_HOME" >> ~/.zshrc

  # Créer admin user
  "$WILDFLY_HOME/bin/add-user.sh" -u "$WILDFLY_ADMIN_USER" -p "$WILDFLY_ADMIN_PASS" --silent 2>/dev/null || true
  log "Admin WildFly créé : $WILDFLY_ADMIN_USER / $WILDFLY_ADMIN_PASS"

  # Télécharger driver PostgreSQL JDBC
  JDBC_JAR="postgresql-42.7.1.jar"
  JDBC_URL="https://jdbc.postgresql.org/download/$JDBC_JAR"
  if [ ! -f "$WILDFLY_HOME/standalone/deployments/$JDBC_JAR" ]; then
    log "Téléchargement du driver PostgreSQL JDBC..."
    curl -L "$JDBC_URL" -o "$WILDFLY_HOME/standalone/deployments/$JDBC_JAR"
    touch "$WILDFLY_HOME/standalone/deployments/$JDBC_JAR.dodeploy"
    log "Driver JDBC déployé"
  fi

  # Configurer la DataSource dans standalone.xml via CLI
  configure_wildfly_datasource
}

configure_wildfly_datasource() {
  section "Configuration DataSource WildFly"
  WILDFLY_HOME="$HOME/wildfly-${WILDFLY_VERSION}"

  # Démarrer WildFly en background
  "$WILDFLY_HOME/bin/standalone.sh" &
  WILDFLY_PID=$!
  log "WildFly en cours de démarrage (PID $WILDFLY_PID)..."
  sleep 15

  # CLI commands
  CLI="$WILDFLY_HOME/bin/jboss-cli.sh"
  $CLI --connect --command="
    if (outcome != success) of /subsystem=datasources/data-source=MoneyTransferDS:read-resource
      data-source add \
        --name=MoneyTransferDS \
        --jndi-name=java:/jdbc/MoneyTransferDS \
        --driver-name=postgresql-42.7.1.jar \
        --connection-url=jdbc:postgresql://localhost:5432/$DB_NAME \
        --user-name=$DB_USER \
        --password=$DB_PASSWORD \
        --valid-connection-checker-class-name=org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker \
        --exception-sorter-class-name=org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter
    end-if
  " 2>/dev/null || warn "DataSource peut-être déjà configurée"

  # Stopper WildFly
  $CLI --connect --command=":shutdown" 2>/dev/null || kill $WILDFLY_PID 2>/dev/null || true
  wait $WILDFLY_PID 2>/dev/null || true
  log "DataSource MoneyTransferDS configurée"
}

# ─────────────────────────────────────────────
# 5. Générer le projet Maven
# ─────────────────────────────────────────────
generate_project() {
  section "Génération du projet Maven"

  if [ -d "$PROJECT_DIR" ]; then
    warn "Le dossier $PROJECT_DIR existe déjà — skip génération"
    return
  fi

  cp -r "$(dirname "$0")/project" "$PROJECT_DIR"
  log "Projet copié dans $PROJECT_DIR"

  # Remplacer les placeholders dans persistence.xml
  sed -i.bak "s/__DB_NAME__/$DB_NAME/g;s/__DB_USER__/$DB_USER/g;s/__DB_PASSWORD__/$DB_PASSWORD/g" \
    "$PROJECT_DIR/src/main/resources/META-INF/persistence.xml" 2>/dev/null || true

  log "Projet généré dans $PROJECT_DIR"
}

# ─────────────────────────────────────────────
# 6. Schéma SQL
# ─────────────────────────────────────────────
run_sql_schema() {
  section "Schéma SQL"
  SQL_FILE="$(dirname "$0")/scripts/schema.sql"
  if [ "$OS" = "linux" ]; then
    sudo -u postgres psql -d "$DB_NAME" -f "$SQL_FILE"
  else
    psql -U "$DB_USER" -d "$DB_NAME" -f "$SQL_FILE"
  fi
  log "Schéma SQL appliqué"
}

# ─────────────────────────────────────────────
# 7. Build Maven
# ─────────────────────────────────────────────
build_project() {
  section "Build Maven"
  cd "$PROJECT_DIR"
  mvn clean package -DskipTests -q
  log "Build réussi — WAR généré dans target/"
}

# ─────────────────────────────────────────────
# 8. Déploiement sur WildFly
# ─────────────────────────────────────────────
deploy_to_wildfly() {
  section "Déploiement sur WildFly"
  WILDFLY_HOME="$HOME/wildfly-${WILDFLY_VERSION}"
  WAR="$PROJECT_DIR/target/money-transfer.war"

  # Démarrer WildFly
  "$WILDFLY_HOME/bin/standalone.sh" &
  sleep 12

  # Déployer
  "$WILDFLY_HOME/bin/jboss-cli.sh" --connect \
    --command="deploy $WAR --force" 2>/dev/null

  log "Application déployée !"
  log "URL : http://localhost:8080/money-transfer/api/health"
  log "Console WildFly : http://localhost:9990 ($WILDFLY_ADMIN_USER / $WILDFLY_ADMIN_PASS)"
}

# ─────────────────────────────────────────────
# MAIN
# ─────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════╗"
echo "║   MoneyTransfer JEE — Setup Automatique  ║"
echo "╚══════════════════════════════════════════╝"
echo ""

detect_os
install_java
install_maven
install_postgres
install_wildfly
generate_project
run_sql_schema
build_project
deploy_to_wildfly

echo ""
echo "╔══════════════════════════════════════════╗"
echo "║   Setup terminé avec succes !            ║"
echo "║                                          ║"
echo "║   API : http://localhost:8080/           ║"
echo "║          money-transfer/api/health       ║"
echo "║   Admin: http://localhost:9990           ║"
echo "║   DB   : $DB_NAME               ║"
echo "╚══════════════════════════════════════════╝"
