# README — DEV-6 : Notifications, Audit & Tests
## TransferPro — Money Transfer JEE

---

## 👤 Rôle DEV-6

Dans le projet TransferPro, le rôle DEV-6 est responsable de :
- La création du service d'audit (`AuditLogService`)
- La complétion du service de notifications (`NotificationService`)
- L'écriture de tous les tests JUnit 5
- La vérification du bon fonctionnement global du projet

---

## ⚙️ Environnement de travail

| Outil | Version |
|---|---|
| Java JDK | 25.0.2 |
| Apache Maven | 3.9.16 |
| Docker Desktop | 29.5.2 |
| IntelliJ IDEA | Community Edition |
| WildFly | 40.0.0.Final (JDK 21) |
| PostgreSQL | 15 (via Docker) |

---

## 📁 Structure du projet

```
transferpro-master/
└── final-setup/
    ├── .env                        ← Configuration des variables d'environnement
    ├── docker-compose.yml          ← Lance PostgreSQL + WildFly + PgAdmin
    └── project/                    ← Code source Java EE
        ├── pom.xml
        └── src/
            ├── main/java/ma/transfert/
            │   ├── model/
            │   │   └── AuditLog.java
            │   ├── service/
            │   │   ├── AuditLogService.java      ← CRÉÉ par DEV-6
            │   │   ├── NotificationService.java  ← COMPLÉTÉ par DEV-6
            │   │   ├── FeeCalculatorService.java
            │   │   ├── OTPService.java
            │   │   └── TransferService.java
            │   └── rest/
            │       └── HealthResource.java
            └── test/java/ma/transfert/service/
                ├── FeeCalculatorServiceTest.java ← CRÉÉ par DEV-6
                ├── OTPServiceTest.java           ← CRÉÉ par DEV-6
                ├── TransferServiceTest.java      ← CRÉÉ par DEV-6
                ├── NotificationServiceTest.java  ← CRÉÉ par DEV-6
                └── DocumentServiceTest.java      ← CRÉÉ par DEV-6
```

---

## 🚀 Étapes réalisées

### Étape 1 — Extraction et configuration du projet

1. Extraction du fichier `transferpro-master.zip` dans `C:\Projets\transferpro\`
2. Création du fichier `.env` à partir de `.env.example` avec les variables :
   ```
   POSTGRES_DB=transferpro_db
   POSTGRES_USER=transferpro_user
   POSTGRES_PASSWORD=transferpro_pass
   WILDFLY_ADMIN_USER=admin
   WILDFLY_ADMIN_PASSWORD=Admin1234!
   ```
3. Correction de la version Java dans `pom.xml` :
   - `<maven.compiler.release>21</maven.compiler.release>` (compatible WildFly)

---

### Étape 2 — Compilation du projet

```bash
cd transferpro-master/final-setup/project
mvn clean package -DskipTests
```

**Résultat :** `BUILD SUCCESS` — génère `target/money-transfer.war`

---

### Étape 3 — Lancement de l'environnement Docker

```bash
cd transferpro-master/final-setup
docker compose up -d --build
```

**3 conteneurs lancés :**

| Conteneur | Service | Port |
|---|---|---|
| `mt_postgres` | Base de données PostgreSQL | 5432 |
| `mt_wildfly` | Serveur d'application WildFly | 8080 |
| `mt_pgadmin` | Interface visuelle PostgreSQL | 5050 |

**Déploiement du WAR :**
```bash
docker cp project/target/money-transfer.war mt_wildfly:/opt/jboss/wildfly/standalone/deployments/
```

**Vérification :** http://localhost:8080/money-transfer/api/health → `{"status":"UP"}`

---

### Étape 4 — Création de AuditLogService

**Fichier :** `src/main/java/ma/transfert/service/AuditLogService.java`

Service de journalisation asynchrone de toutes les actions sensibles de l'application.

**Caractéristiques :**
- Annoté `@Stateless` et `@Asynchronous` pour ne jamais bloquer la transaction principale
- Utilise l'`EntityManager` JPA pour persister dans la table `audit_logs`
- Nom de la persistence unit : `MoneyTransferPU`

**3 méthodes :**

| Méthode | Description |
|---|---|
| `log(user, action, entityType, entityId, details)` | Log standard avec utilisateur |
| `logWithIp(user, action, ..., ipAddress)` | Log avec adresse IP (pour les connexions) |
| `logSystem(action, entityType, entityId, details)` | Log système sans utilisateur (batch, expiration) |

**Exemples d'utilisation :**
```java
auditLogService.log(user, "LOGIN_SUCCESS", "User", user.getId(), "Connexion réussie");
auditLogService.log(user, "TRANSFER_CREATED", "Transfer", transfer.getId(), "Montant: 500 MAD");
auditLogService.logSystem("TRANSFER_EXPIRED", "Transfer", transfer.getId(), "Expiré après 30 jours");
```

---

### Étape 5 — Complétion de NotificationService

**Fichier :** `src/main/java/ma/transfert/service/NotificationService.java`

Ajout de la méthode manquante `sendCancellationNotification()` :

```java
@Asynchronous
public void sendCancellationNotification(String phone, String email, String trackingCode) {
    // Simule l'envoi SMS
    // Simule l'envoi Email
    // Log la notification
}
```

**Toutes les méthodes disponibles :**

| Méthode | Déclenchée quand |
|---|---|
| `sendOTPNotification()` | Transfert créé → envoi du code OTP au bénéficiaire |
| `sendPaymentConfirmation()` | Transfert payé en agence |
| `sendTransferCreatedNotification()` | Confirmation à l'expéditeur |
| `sendTransferExpiredNotification()` | Transfert non retiré après 30 jours |
| `sendCancellationNotification()` | Transfert annulé ← **ajouté par DEV-6** |

> En mode développement : SMS et emails sont **simulés** (affichés dans les logs WildFly).

---

### Étape 6 — Écriture des tests JUnit 5

#### Fix Java 25 / Mockito

Java 25 n'est pas encore supporté nativement par Mockito. Fix appliqué dans `pom.xml` :

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.3</version>
    <configuration>
        <argLine>-Dnet.bytebuddy.experimental=true</argLine>
    </configuration>
</plugin>
```

---

#### FeeCalculatorServiceTest (5 tests)

Teste le calcul des frais de transfert selon le barème :

| Montant | Frais |
|---|---|
| 50 — 1000 MAD | 25.00 MAD fixe |
| 1001 — 5000 MAD | 35.00 MAD fixe |
| 5001 — 10000 MAD | 50.00 MAD fixe |
| 10001 — 20000 MAD | 0.75% |
| > 20000 MAD | 0.50% |

**Tests :**
- `fees_500_returns25()` — montant 500 MAD → 25 MAD
- `fees_3000_returns35()` — montant 3000 MAD → 35 MAD
- `fees_7000_returns50()` — montant 7000 MAD → 50 MAD
- `fees_null_throws()` — montant null → `IllegalArgumentException`
- `fees_belowMin_throws()` — montant 49 MAD → `IllegalArgumentException`

---

#### OTPServiceTest (54 tests)

Teste la génération des codes OTP :

- `otp_hasLength8()` — OTP fait exactement 8 caractères
- `otp_onlyDigits()` — OTP contient uniquement des chiffres `\d{8}`
- `otp_notNull()` — OTP n'est jamais null
- `otp_alwaysValid()` — OTP toujours valide (répété 50 fois)
- `otp_unique()` — OTPs uniques sur 500 générations (taux collision < 2%)

---

#### TransferServiceTest (7 tests)

Teste les transitions de statuts des transferts avec Mockito :

```
PENDING → CONFIRMED → AVAILABLE → PAID
                    ↓
                CANCELLED
```

- `createTransfer_returnsDTO()` — création retourne un DTO avec statut PENDING
- `confirmTransfer_success()` — PENDING → CONFIRMED
- `confirmTransfer_notFound_throws()` — code inconnu → exception
- `payTransfer_validOTP_paid()` — bon OTP → PAID
- `payTransfer_wrongOTP_throws()` — mauvais OTP → exception
- `cancelTransfer_success()` — PENDING → CANCELLED
- `cancelTransfer_notFound_throws()` — code inconnu → exception

---

#### NotificationServiceTest (6 tests)

Vérifie que les méthodes de notification ne lèvent aucune exception :

- `sendOTP_noException()` — envoi OTP normal
- `sendOTP_nullParams_noException()` — envoi sans phone ni email → pas de crash
- `sendPayment_noException()` — confirmation paiement
- `sendCreated_noException()` — notification création transfert
- `sendExpired_noException()` — notification expiration
- `sendCancellation_noException()` — notification annulation

---

#### DocumentServiceTest (6 tests)

Teste la validation des documents KYC :

- `upload_pdf_ok()` — `application/pdf` accepté ✅
- `upload_gif_rejected()` — `image/gif` refusé → `IllegalArgumentException`
- `upload_exe_rejected()` — `application/exe` refusé → `IllegalArgumentException`
- `upload_unknownUser_throws()` — utilisateur inexistant → exception
- `validate_changesStatus()` — statut passe à `VALIDATED`
- `reject_changesStatus()` — statut passe à `REJECTED`, KYC user mis à jour

---

## ✅ Résultats finaux

```
Tests run: 126, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Classe de test | Tests | Résultat |
|---|---|---|
| JWTUtilTest | 10 | ✅ PASS |
| PasswordUtilTest | 8 | ✅ PASS |
| AgencyServiceTest | 9 | ✅ PASS |
| AuthServiceTest | 9 | ✅ PASS |
| UserServiceTest | 12 | ✅ PASS |
| FeeCalculatorServiceTest | 5 | ✅ PASS |
| OTPServiceTest | 54 | ✅ PASS |
| TransferServiceTest | 7 | ✅ PASS |
| NotificationServiceTest | 6 | ✅ PASS |
| DocumentServiceTest | 6 | ✅ PASS |
| **TOTAL** | **126** | ✅ **0 ÉCHEC** |

---

## 🔧 Commandes utiles

```bash
# Lancer tous les tests
cd transferpro-master/final-setup/project
mvn test

# Lancer un test spécifique
mvn test -Dtest="TransferServiceTest"
mvn test -Dtest="FeeCalculatorServiceTest"

# Compiler sans tests
mvn clean package -DskipTests

# Démarrer Docker
cd transferpro-master/final-setup
docker compose up -d

# Arrêter Docker
docker compose down

# Redéployer le WAR
docker cp project/target/money-transfer.war mt_wildfly:/opt/jboss/wildfly/standalone/deployments/

# Voir les logs WildFly
docker logs mt_wildfly
```

---

## 🌐 URLs de l'application

| URL | Description |
|---|---|
| http://localhost:8080/money-transfer/api/health | Statut du serveur |
| http://localhost:8080/money-transfer/api/transfers | API transferts |
| http://localhost:8080/money-transfer/api/auth/login | Authentification JWT |
| http://localhost:9990 | Console admin WildFly |
| http://localhost:5050 | PgAdmin (interface base de données) |

---

## 📌 Points importants pour la soutenance

1. **AuditLogService** est `@Asynchronous` → ne bloque jamais les transactions métier
2. **NotificationService** est découplé → fonctionne même si le SMTP est indisponible
3. **Les tests Mockito** utilisent `net.bytebuddy.experimental=true` car Java 25 est plus récent que la version supportée officiellement par Mockito 5.8
4. **126 tests passent** sans aucun échec ni erreur
5. Le serveur répond `{"status":"UP"}` sur `/api/health`
