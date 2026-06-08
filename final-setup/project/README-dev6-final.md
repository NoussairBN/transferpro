# README FINAL — DEV-6 : Notifications, Audit, Tests & Documentation API
## Projet TransferPro — Application de transfert d'argent JEE

---

## 👤 Rôle DEV-6 dans l'équipe

Le projet TransferPro est développé en équipe avec des rôles distincts (DEV-1 à DEV-6). En tant que **DEV-6**, j'étais responsable de la **qualité et de l'observabilité** de l'application :

| Responsabilité | Description |
|---|---|
| **AuditLogService** | Créer de zéro le service de traçabilité des actions |
| **NotificationService** | Compléter le service de notifications (SMS/Email) |
| **Tests JUnit 5** | Écrire les tests unitaires pour tous les modules |
| **Health Check** | Endpoint de santé du serveur |
| **Documentation API** | Créer la documentation OpenAPI (Swagger) |

---

## ⚙️ Environnement de travail

| Outil | Version | Rôle |
|---|---|---|
| Java JDK | 25.0.2 | Compilation du code |
| Apache Maven | 3.9.16 | Gestion des dépendances et build |
| Docker Desktop | 29.5.2 | Conteneurs PostgreSQL + WildFly |
| IntelliJ IDEA | Community Edition | IDE de développement |
| WildFly | 40.0.0.Final | Serveur d'application Java EE |
| PostgreSQL | 15 (via Docker) | Base de données |

> **Note :** WildFly embarque Java 21 en interne. Le code est compilé avec `maven.compiler.release=21` même si le JDK local est Java 25.

---

## 📁 Structure du projet

```
transferpro-master/
└── final-setup/
    ├── .env                              ← Variables d'environnement (BDD, admin)
    ├── docker-compose.yml                ← Orchestration des 3 conteneurs Docker
    ├── docker/                           ← Dockerfiles personnalisés
    └── project/
        ├── pom.xml
        └── src/
            ├── main/java/ma/transfert/
            │   ├── service/
            │   │   ├── AuditLogService.java       ← ✅ CRÉÉ par DEV-6
            │   │   ├── NotificationService.java   ← ✅ COMPLÉTÉ par DEV-6
            │   │   ├── FeeCalculatorService.java
            │   │   ├── OTPService.java
            │   │   └── TransferService.java
            │   └── rest/
            │       ├── HealthResource.java        ← ✅ COMPLÉTÉ par DEV-6
            │       └── OpenApiResource.java       ← ✅ CRÉÉ par DEV-6
            └── test/java/ma/transfert/service/
                ├── FeeCalculatorServiceTest.java  ← ✅ CRÉÉ par DEV-6
                ├── OTPServiceTest.java            ← ✅ CRÉÉ par DEV-6
                ├── TransferServiceTest.java       ← ✅ CRÉÉ par DEV-6
                ├── NotificationServiceTest.java   ← ✅ CRÉÉ par DEV-6
                └── DocumentServiceTest.java       ← ✅ CRÉÉ par DEV-6
```

---

## 🚀 Étapes réalisées — Du début à la fin

---

### ÉTAPE 1 — Extraction et configuration

```powershell
mkdir C:\Projets
Expand-Archive -Path "$HOME\Downloads\transferpro-master.zip" -DestinationPath "C:\Projets\transferpro"
cd C:\Projets\transferpro\transferpro-master\final-setup
Copy-Item .env.example .env
```

Contenu du fichier `.env` :

```env
POSTGRES_DB=transferpro_db
POSTGRES_USER=transferpro_user
POSTGRES_PASSWORD=transferpro_pass
PGADMIN_DEFAULT_EMAIL=admin@transferpro.ma
PGADMIN_DEFAULT_PASSWORD=admin123
WILDFLY_ADMIN_USER=admin
WILDFLY_ADMIN_PASSWORD=Admin1234!
APP_ADMIN_EMAIL=admin@transferpro.ma
APP_ADMIN_PASSWORD=Admin1234!
```

---

### ÉTAPE 2 — Correction Java + Compilation

Modifier `project/pom.xml` pour cibler Java 21 (compatible WildFly) :

```xml
<maven.compiler.release>21</maven.compiler.release>
```

Fix Mockito / Java 25 dans le bloc `maven-surefire-plugin` :

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.3</version>
    <configuration>
        <argLine>-Dnet.bytebuddy.experimental=true</argLine>
    </configuration>
</plugin>
```

Compiler :

```powershell
cd C:\Projets\transferpro\transferpro-master\final-setup\project
mvn clean package -DskipTests
# Résultat : BUILD SUCCESS → target/money-transfer.war généré
```

---

### ÉTAPE 3 — Lancement Docker

```powershell
cd C:\Projets\transferpro\transferpro-master\final-setup
docker compose up -d --build
docker ps
```

| Conteneur | Service | Port |
|---|---|---|
| `mt_postgres` | PostgreSQL 15 | 5432 |
| `mt_wildfly` | WildFly 40 | 8080 / 9990 |
| `mt_pgadmin` | PgAdmin 4 | 5050 |

**Déploiement du WAR :**

```powershell
docker cp project\target\money-transfer.war mt_wildfly:/opt/jboss/wildfly/standalone/deployments/
```

---

### ÉTAPE 4 — Création de AuditLogService

**Fichier :** `src/main/java/ma/transfert/service/AuditLogService.java`

Service de journalisation asynchrone de toutes les actions sensibles.

```java
@Stateless
public class AuditLogService {

    @PersistenceContext(unitName = "MoneyTransferPU")
    private EntityManager em;

    @Asynchronous
    public void log(User user, String action, String entityType, Long entityId, String details) {
        AuditLog entry = AuditLog.of(user, action, entityType, entityId, details);
        em.persist(entry);
    }

    @Asynchronous
    public void logWithIp(User user, String action, String entityType, Long entityId,
                          String details, String ipAddress) {
        AuditLog entry = AuditLog.of(user, action, entityType, entityId, details);
        entry.setIpAddress(ipAddress);
        em.persist(entry);
    }

    @Asynchronous
    public void logSystem(String action, String entityType, Long entityId, String details) {
        log(null, action, entityType, entityId, details);
    }
}
```

| Annotation | Rôle |
|---|---|
| `@Stateless` | EJB sans état, partageable entre threads |
| `@Asynchronous` | Non-bloquant — le log s'écrit en arrière-plan |
| `@PersistenceContext` | Injection de l'EntityManager JPA |
| `unitName = "MoneyTransferPU"` | Nom exact dans `persistence.xml` |

---

### ÉTAPE 5 — Complétion de NotificationService

**Méthode ajoutée :** `sendCancellationNotification()`

```java
@Asynchronous
public void sendCancellationNotification(String phone, String email, String trackingCode) {
    String message = String.format("Votre transfert %s a été annulé.", trackingCode);
    if (phone != null && !phone.isEmpty()) {
        logger.info(String.format("📱 [SMS SIMULÉ] → %s : %s", phone, message));
    }
    if (email != null && !email.isEmpty()) {
        logger.info(String.format("📧 [EMAIL SIMULÉ] → %s : %s", email, message));
    }
}
```

| Méthode | Événement |
|---|---|
| `sendOTPNotification()` | Transfert créé → OTP au bénéficiaire |
| `sendPaymentConfirmation()` | Transfert retiré en agence |
| `sendTransferCreatedNotification()` | Confirmation à l'expéditeur |
| `sendTransferExpiredNotification()` | Transfert non retiré après 30j |
| `sendCancellationNotification()` | Transfert annulé ← **ajouté par DEV-6** |

---

### ÉTAPE 6 — Tests JUnit 5 (5 fichiers créés)

---

#### FeeCalculatorServiceTest — 5 tests

Teste le barème des frais :

| Montant | Frais attendus |
|---|---|
| 500 MAD | 25,00 MAD |
| 3 000 MAD | 35,00 MAD |
| 7 000 MAD | 50,00 MAD |
| null | IllegalArgumentException |
| 49 MAD (sous le min) | IllegalArgumentException |

---

#### OTPServiceTest — 54 tests

- OTP fait exactement 8 chiffres
- Ne contient que des chiffres `\d{8}`
- N'est jamais null
- `@RepeatedTest(50)` — valide sur 50 générations
- Unique sur 500 générations (< 2% collisions)

---

#### TransferServiceTest — 7 tests

Cycle de vie d'un transfert :

```
PENDING → CONFIRMED → AVAILABLE → PAID
   └─────────────────────────────→ CANCELLED
```

- Création → DTO avec statut PENDING et frais calculés
- PENDING → CONFIRMED
- Code inconnu → RuntimeException
- Bon OTP → PAID
- Mauvais OTP → RuntimeException
- PENDING → CANCELLED
- Code inconnu → RuntimeException

Technique : **Mockito** — `TransferDAO`, `OTPService`, `FeeCalculatorService` mockés.

---

#### NotificationServiceTest — 6 tests

Vérifie que toutes les méthodes de notification ne lèvent aucune exception, même avec des paramètres null. Utilise `assertDoesNotThrow()`.

---

#### DocumentServiceTest — 6 tests

- `application/pdf` → accepté
- `image/gif` → IllegalArgumentException
- `application/exe` → IllegalArgumentException
- Utilisateur inexistant → IllegalArgumentException
- `validateDocument()` → statut VALIDATED
- `rejectDocument()` → statut REJECTED, KYC user mis à jour

---

### ÉTAPE 7 — Documentation OpenAPI

**Fichier créé :** `src/main/java/ma/transfert/rest/OpenApiResource.java`

WildFly 40 ne supporte pas MicroProfile OpenAPI nativement. Création d'un endpoint JAX-RS qui retourne directement le YAML de documentation.

**URL :** `http://localhost:8080/money-transfer/api/openapi`

Endpoints documentés : `/health`, `/auth/login`, `/auth/register`, `/transfers`, `/transfers/{code}/confirm`, `/transfers/{code}/pay`, `/transfers/{code}/cancel`, `/documents/upload`

---

## ✅ Résultats finaux

### Tests unitaires

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

### Tests fonctionnels end-to-end (PowerShell)

**1. Health check :**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/money-transfer/api/health" -Method GET
```
```
status      : UP
application : MoneyTransfer JEE
version     : 1.0.0
timestamp   : 2026-06-08T21:43:03.729142880
```

**2. Inscription :**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/money-transfer/api/auth/register" -Method POST -ContentType "application/json" -Body '{"email":"agent2@test.ma","password":"Test1234!","firstName":"Ali","lastName":"Benali","phone":"0611111111"}'
```
```
token    : eyJhbGciOiJIUzI1NiJ9...
role     : INDIVIDUAL
email    : agent2@test.ma
fullName : Ali Benali
userId   : 3
```

**3. Connexion :**
```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/money-transfer/api/auth/login" -Method POST -ContentType "application/json" -Body '{"email":"agent2@test.ma","password":"Test1234!"}'
$token = $response.token
```
```
Token OK : eyJhbGciOiJIUzI1NiJ9...
```

**4. Création d'un transfert :**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/money-transfer/api/transfers" -Method POST -ContentType "application/json" -Headers @{Authorization="Bearer $token"} -Body '{"amount":500,"senderName":"Ali Benali","senderPhone":"0612345678","receiverName":"Sara Alami","receiverPhone":"0698765432"}'
```
```
trackingCode     : TRF-20260608-046BB638
amount           : 500
fees             : 25.00
status           : PENDING
totalAmount      : 525.00
sendingAgencyName: Agence Casablanca Centre
expiresAt        : {2026, 7, 8, 21...}
```

---

## 🌐 URLs de l'application

| URL | Description | Résultat |
|---|---|---|
| http://localhost:8080/money-transfer/api/health | Statut serveur | ✅ `{"status":"UP"}` |
| http://localhost:8080/money-transfer/api/openapi | Documentation API YAML | ✅ YAML complet |
| http://localhost:9990 | Console admin WildFly | ✅ Interface graphique |
| http://localhost:5050 | PgAdmin (base de données) | ✅ Interface graphique |

---

## 🔧 Commandes de référence

```powershell
# Lancer tous les tests
cd C:\Projets\transferpro\transferpro-master\final-setup\project
mvn test

# Lancer un test spécifique
mvn test -Dtest="TransferServiceTest"
mvn test -Dtest="FeeCalculatorServiceTest"

# Compiler sans les tests
mvn clean package -DskipTests

# Démarrer Docker
cd C:\Projets\transferpro\transferpro-master\final-setup
docker compose up -d

# Arrêter Docker
docker compose down

# Vérifier les conteneurs
docker ps

# Voir les logs WildFly
docker logs mt_wildfly --tail 30

# Redéployer le WAR après modification
cd C:\Projets\transferpro\transferpro-master\final-setup\project
mvn clean package -DskipTests
cd C:\Projets\transferpro\transferpro-master\final-setup
docker cp project\target\money-transfer.war mt_wildfly:/opt/jboss/wildfly/standalone/deployments/
```

---

## 📌 Points clés pour la soutenance

1. **AuditLogService est `@Asynchronous`** — les logs s'écrivent en arrière-plan sans jamais bloquer les transactions métier.

2. **NotificationService est découplé** — si le serveur email est indisponible, l'application ne plante pas. Les notifications sont simulées dans les logs en mode dev.

3. **126 tests, 0 échec** — couverture complète des services FeeCalculator, OTP, Transfer, Notification et Document.

4. **Fix Mockito + Java 25** — `net.bytebuddy.experimental=true` dans Surefire pour contourner l'incompatibilité entre Mockito 5.8 et Java 25.

5. **OpenAPI sans MicroProfile** — WildFly 40 ne fournit pas `/openapi` par défaut. Endpoint JAX-RS manuel créé pour servir le YAML.

6. **Persistence unit `MoneyTransferPU`** — nom exact dans `persistence.xml`. Une erreur ici provoque un échec de déploiement WildFly.

7. **Tests end-to-end validés** — inscription, login JWT, création de transfert avec frais calculés automatiquement (500 MAD → 25 MAD de frais → total 525 MAD).
