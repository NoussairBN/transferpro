FROM quay.io/wildfly/wildfly:latest-jdk21

ARG POSTGRES_DB
ARG POSTGRES_USER
ARG POSTGRES_PASSWORD
ARG WILDFLY_ADMIN_USER
ARG WILDFLY_ADMIN_PASSWORD

USER root

# 1. Telecharger le driver PostgreSQL JDBC (Optimized version 42.7.11)
RUN mkdir -p /tmp/wildfly-build
RUN curl -L https://jdbc.postgresql.org/download/postgresql-42.7.11.jar \
    -o /tmp/wildfly-build/postgresql-42.7.11.jar

# 2. Creer le dossier de module utilisateur (Clean WildFly module location)
RUN mkdir -p /opt/jboss/wildfly/modules/org/postgresql/main && \
    cp /tmp/wildfly-build/postgresql-42.7.11.jar \
       /opt/jboss/wildfly/modules/org/postgresql/main/

# 3. Fichier module.xml pour declarer le driver
RUN cat > /opt/jboss/wildfly/modules/org/postgresql/main/module.xml <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.5" name="org.postgresql">
    <resources>
        <resource-root path="postgresql-42.7.11.jar"/>
    </resources>
    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
    </dependencies>
</module>
EOF

# 4. Script CLI pour configurer la DataSource
RUN cat > /tmp/wildfly-build/configure-ds.cli <<EOF
embed-server --server-config=standalone.xml --std-out=echo
batch
/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql,driver-module-name=org.postgresql,driver-class-name=org.postgresql.Driver,driver-xa-datasource-class-name=org.postgresql.xa.PGXADataSource)
/subsystem=datasources/data-source=MoneyTransferDS:add(jndi-name=java:/jdbc/MoneyTransferDS,driver-name=postgresql,connection-url=jdbc:postgresql://mt_postgres:5432/\${POSTGRES_DB},user-name=\${POSTGRES_USER},password=\${POSTGRES_PASSWORD},min-pool-size=5,max-pool-size=20,valid-connection-checker-class-name=org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker,exception-sorter-class-name=org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter,enabled=true,use-java-context=true)
run-batch
stop-embedded-server
EOF

# Executer le script CLI pour configurer la DataSource
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/wildfly-build/configure-ds.cli

# Creer un utilisateur admin pour la console d'administration de WildFly
RUN /opt/jboss/wildfly/bin/add-user.sh -u "${WILDFLY_ADMIN_USER}" -p "${WILDFLY_ADMIN_PASSWORD}" --silent

# 5. Copier votre fichier WAR compile par Maven directement dans les deploiements WildFly
COPY ./project/target/money-transfer.war /opt/jboss/wildfly/standalone/deployments/

# Configuration du dossier d'upload KYC + attributions des droits globaux
RUN mkdir -p /opt/jboss/wildfly/standalone/log /opt/jboss/wildfly/standalone/tmp /opt/jboss/uploads && \
    chown -R jboss:root /opt/jboss/wildfly /opt/jboss/uploads && \
    chmod -R 775 /opt/jboss/uploads && \
    rm -rf /tmp/wildfly-build

USER jboss

# Lancer WildFly accessible sur toutes les interfaces réseaux externes
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]