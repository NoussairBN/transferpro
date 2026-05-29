# MoneyTransfer JEE

Systeme de transfert d'argent inspire de CashPlus / WafaCash, construit avec Jakarta EE 10 sur WildFly 40.

---

## Quick Start

### Prerequis

- **Java 21** (JDK, pas JRE)
- **Maven 3.9+**
- **Docker Desktop**
- **Git**

### Credentials locaux

Avant le premier lancement :

```bash
cp .env.example .env
```

Sous Windows et Linux/macOS, le script de setup cree automatiquement `.env` a partir de `.env.example` s'il n'existe pas.
Le fichier `.env` est **local** et ne doit pas etre pousse sur GitHub.

### Windows (automatique)

```powershell
git clone <repo-url>
cd money-transfer-setup
# PowerShell en Administrateur
Set-ExecutionPolicy Bypass -Scope Process -Force
.\setup-windows.ps1
```

Le script installe Java, Maven, IntelliJ, verifie Docker, compile le projet, lance les conteneurs et deploie le WAR automatiquement.

### Linux / macOS (automatique)

```bash
git clone <repo-url>
cd money-transfer-setup
chmod +x setup.sh
bash setup.sh
```

### Manuel (toutes plateformes)

```bash
# 1. Compiler
cd project
mvn clean package -DskipTests

# 2. Lancer PostgreSQL + WildFly
cd ..
cp .env.example .env   # a faire une seule fois si .env n'existe pas
docker compose up -d --build

# 3. Attendre 15 secondes puis deployer
docker cp project/target/money-transfer.war mt_wildfly:/opt/jboss/wildfly/standalone/deployments/

# 4. Verifier
curl http://localhost:8080/money-transfer/api/health
```

---

## Architecture

```
                    ┌─────────────┐
                    │   Client    │
                    │ (Postman /  │
                    │  Frontend)  │
                    └──────┬──────┘
                           │ HTTP :8080
                    ┌──────▼──────┐
                    │   WildFly   │
                    │   (Docker)  │
                    │             │
                    │  JAX-RS API │
                    │  EJB / CDI  │
                    │  JPA        │
                    └──────┬──────┘
                           │ JDBC :5432
                    ┌──────▼──────┐
                    │ PostgreSQL  │
                    │  (Docker)   │
                    └─────────────┘
```

### Stack technique

| Composant | Technologie |
|-----------|-------------|
| Runtime | Java 21 (Temurin) |
| Serveur | WildFly 40 (Jakarta EE 10+) |
| Base de donnees | PostgreSQL 15 |
| ORM | JPA / Hibernate 6 |
| API REST | JAX-RS (RESTEasy) |
| Auth | JWT (Auth0 java-jwt) |
| Mots de passe | BCrypt (jBCrypt) |
| PDF | iText 7 |
| Build | Maven |
| Conteneurs | Docker Compose |

---

## Structure du projet

```
money-transfer-setup/
├── docker/
│   └── wildfly/
│       └── Dockerfile          # Image WildFly + driver PostgreSQL + DataSource
├── project/                    # Code source Maven
│   ├── src/main/java/ma/transfert/
│   │   ├── model/              # Entites JPA
│   │   ├── dao/                # Acces base de donnees
│   │   ├── service/            # Logique metier
│   │   ├── rest/               # Endpoints API REST
│   │   ├── security/           # JWT, filtres auth
│   │   └── util/               # Helpers (codes, calculs frais)
│   ├── src/main/resources/
│   │   └── META-INF/
│   │       └── persistence.xml # Config JPA / DataSource
│   ├── src/test/               # Tests JUnit 5
│   └── pom.xml
├── scripts/
│   └── schema.sql              # Schema PostgreSQL + donnees initiales
├── docker-compose.yml
├── setup-windows.ps1           # Setup automatique Windows
└── setup.sh                    # Setup automatique Linux/macOS
```

---

## Etat actuel du projet

Le projet est actuellement au stade **Sprints 1, 2 et 3 termines**.

### Ce qui est deja fait

**Sprint 1 — DEV-1 (Setup & Fondations)**
- environnement Java 21 / Maven / Docker pret
- PostgreSQL, WildFly et PgAdmin via Docker Compose
- build Maven OK, WAR deployable dans WildFly
- connexion a la base via la DataSource `MoneyTransferDS`
- schema SQL cree automatiquement
- endpoint `GET /money-transfer/api/health` fonctionnel
- entites JPA : User, Agency, Transfer, AuditLog

**Sprint 3 — DEV-2 (Module Transferts)**
- TransferService : creation, calcul frais, OTP, machine a etats
- TransferDAO, OTPService, FeeCalculatorService, NotificationService
- API REST complete : POST/GET/pay/cancel/confirm/track
- DTOs, exceptions metier

**Sprint 2 — DEV-3 (Module Auth & Utilisateurs)**
- PasswordUtil (BCrypt), JWTUtil (Auth0), JWTAuthFilter
- UserService : inscription, profil, changement mdp, suspension
- AuthService : login → JWT, register avec connexion auto
- API REST : POST /auth/login, POST /auth/register
- API REST : GET/PUT /users/me, changement mdp, routes admin
- 39 tests JUnit 5 + Mockito

### Livrables atteints

- Application deployable connectee a la base
- API transferts fonctionnelle (creation, suivi, paiement OTP)
- Authentification JWT fonctionnelle (login, register, profil)

### Ce qui reste pour la suite

- CRUD agences (DEV-4 : AgencyService, AgencyResource)
- Upload documents + PDF recus (DEV-5 : DocumentService, iText)
- Notifications Email/SMS (DEV-6 : NotificationService reel)
- Audit metier complet (DEV-6 : AuditLog service)
- Interface JSF (DEV-5 : pages login, dashboard)

### Couches applicatives

```
rest/       →  Recoit les requetes HTTP, valide le JSON
                    ↓
security/   →  Verifie le JWT, controle les permissions
                    ↓
service/    →  Applique les regles metier (frais, solde, OTP)
                    ↓
dao/        →  Lit / ecrit en base de donnees
                    ↓
model/      →  Entites JPA mappees sur les tables PostgreSQL
```

Chaque couche ne parle qu'a celle du dessous. `rest/` ne parle jamais directement a `dao/`.

### Packages deja prets

- `model/` : entites JPA
- `rest/` : point d'entree REST
- `dao/` : acces base de donnees
- `service/` : logique metier
- `security/` : auth / filtres
- `util/` : helpers

### Entites JPA deja creees

Les entites JPA sont les classes Java qui representent les tables de la base de donnees.
Elles servent de base pour coder les futures fonctionnalites.

- `User` : represente un utilisateur du systeme
- `Agency` : represente une agence de transfert
- `Transfer` : represente un transfert d'argent
- `AuditLog` : represente une trace d'action / historique

Correspondance avec la base :

- `User` -> table `users`
- `Agency` -> table `agencies`
- `Transfer` -> table `transfers`
- `AuditLog` -> table `audit_logs`

---

## Conteneurs Docker

| Conteneur | Image | Port | Role |
|-----------|-------|------|------|
| `mt_postgres` | postgres:15-alpine | 5432 | Base de donnees |
| `mt_wildfly` | Custom (Dockerfile) | 8080, 9990 | Serveur d'application |
| `mt_pgadmin` | dpage/pgadmin4 | 5050 | Interface graphique DB |

Le Dockerfile WildFly inclut automatiquement le driver JDBC PostgreSQL et la DataSource `MoneyTransferDS` — aucune configuration manuelle necessaire.

### Commandes Docker utiles

```bash
docker compose up -d --build    # Demarrer (rebuild si Dockerfile modifie)
docker compose down             # Arreter tous les conteneurs
docker compose logs -f wildfly  # Voir les logs WildFly en temps reel
docker ps                       # Lister les conteneurs actifs
docker exec -it mt_wildfly bash # Entrer dans le conteneur WildFly

# Redeployer apres modification du code
mvn clean package -DskipTests -f project/pom.xml
docker cp project/target/money-transfer.war mt_wildfly:/opt/jboss/wildfly/standalone/deployments/
```

---

## Base de donnees

### Connexion

| Parametre | Valeur |
|-----------|--------|
| Host | localhost |
| Port | 5432 |
| Base | Voir `.env` (`POSTGRES_DB`) |
| User | Voir `.env` (`POSTGRES_USER`) |
| Password | Voir `.env` (`POSTGRES_PASSWORD`) |

### Connexion depuis IntelliJ

`View` → `Tool Windows` → `Database` → `+` → `Data Source` → `PostgreSQL`
Renseigner les valeurs ci-dessus.

### Connexion depuis PgAdmin

Ouvrir `http://localhost:5050`, login avec les valeurs de `.env` :
`PGADMIN_DEFAULT_EMAIL` / `PGADMIN_DEFAULT_PASSWORD`.
Ajouter un serveur : host = `mt_postgres`, port = `5432`, user = la valeur `POSTGRES_USER`.

### Schema

4 tables principales :

- **users** — Particuliers, agents d'agence, admins (avec KYC et statut)
- **agencies** — Points de transfert physiques (solde caisse, limite journaliere)
- **transfers** — Transferts d'argent (montant, frais, OTP, statut, tracking code)
- **audit_logs** — Journal de toutes les actions (qui, quoi, quand, IP)

Le schema est applique automatiquement au premier lancement de PostgreSQL via `scripts/schema.sql`.

### Compte admin par defaut

| Champ | Valeur |
|-------|--------|
| Email | Voir `.env` (`APP_ADMIN_EMAIL`) |
| Password | Voir `.env` (`APP_ADMIN_PASSWORD`) |
| Role | ADMIN |

---

## API REST

Base URL : `http://localhost:8080/money-transfer/api`

### Endpoints disponibles (Sprint 1)

| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/health` | Verification que l'application tourne |

### Endpoints prevus

| Sprint | Endpoints |
|--------|-----------|
| Sprint 2 | `POST /auth/login`, `POST /auth/register` |
| Sprint 3 | `POST /transfers`, `GET /transfers/{id}`, `GET /track/{code}` |
| Sprint 4 | CRUD `/agencies`, Upload documents, PDF recus |
| Sprint 5 | `/notifications`, `/audit`, `/corporate/batch` |

---

## Workflow apres setup

Une fois le setup termine :

1. Ouvrir `project/` dans IntelliJ IDEA
2. Coder dans `src/main/java/ma/transfert/`
3. Recompiler le projet
4. Redeployer le WAR dans WildFly
5. Tester le resultat

### Dossier a ouvrir dans IntelliJ

Le bon dossier a ouvrir pour developper est :

```text
project/
```

### Commandes apres modification du code

```powershell
cd project
mvn clean package -DskipTests
cd ..
docker cp .\project\target\money-transfer.war mt_wildfly:/opt/jboss/wildfly/standalone/deployments/
```

### Ou voir le resultat

- API : `http://localhost:8080/money-transfer/api`
- Health check : `http://localhost:8080/money-transfer/api/health`
- Console WildFly : `http://localhost:9990`
- PgAdmin : `http://localhost:5050`

### Voir les logs

```bash
docker compose logs -f wildfly
```

### Si un ami clone le projet sous Windows

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
.\setup-windows.ps1
```

Ensuite :

1. verifier `http://localhost:8080/money-transfer/api/health`
2. ouvrir `project/` dans IntelliJ
3. commencer la suite du developpement

---

## Commandes Maven

```bash
cd project

mvn clean compile              # Compiler sans packager
mvn clean package -DskipTests  # Construire le WAR
mvn test                       # Lancer les tests
mvn clean package wildfly:deploy  # Build + deploy sur WildFly local (hors Docker)
```

---

## Configuration JAVA_HOME

Le projet necessite un **JDK** (pas un JRE). Verifier avec :

```bash
java -version    # doit afficher 21+
mvn -version     # doit afficher Java version: 21 (pas 1.8)
```

Si `mvn -version` affiche Java 1.8 alors que `java -version` affiche 21, c'est que JAVA_HOME pointe sur le mauvais JDK :

```powershell
# Windows — corriger JAVA_HOME (PowerShell Admin)
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot", "Machine")
# Fermer et rouvrir le terminal
```

```bash
# Linux/macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

---

## Troubleshooting

### WildFly ne demarre pas

```bash
# Verifier que le port 8080 est libre
# Windows
netstat -ano | findstr :8080
# Linux
lsof -i :8080
```

### Erreur DataSource / JNDI

Le Dockerfile configure automatiquement la DataSource. Si l'erreur persiste :

```bash
# Verifier que PostgreSQL est healthy
docker ps
# Rebuilder l'image WildFly
docker compose down
docker compose up -d --build
```

### Build Maven echoue — "No compiler provided"

JAVA_HOME pointe sur un JRE au lieu d'un JDK. Voir la section Configuration JAVA_HOME ci-dessus.

### Docker Compose — "Dockerfile not found"

Verifier que le fichier existe :

```
docker/wildfly/Dockerfile    ← doit etre ici
```

Pas dans un sous-dossier. Pas nomme autrement.

---

## Planning Sprints

| Sprint | Semaines | Objectif | Livrable |
|--------|----------|----------|----------|
| 1 | 1-2 | Setup + Fondations | App deployable connectee a la DB |
| 2 | 3-4 | Auth + Utilisateurs | Login JWT fonctionnel |
| 3 | 5-6 | Transferts | Creation et suivi de transferts |
| 4 | 7-8 | Agences + Documents | CRUD agences, upload docs, PDF recus |
| 5 | 9-10 | Notifications + Audit | Systeme complet fonctionnel |
| 6 | 11-12 | HA + Securite + Soutenance | Demo live |

---

## Contribuer

### Avant push GitHub

- Garder `.env` local uniquement
- Pousser `.env.example`, pas `.env`
- Verifier que `.gitignore` est bien pris en compte
- Changer les mots de passe de `.env` avant une demo ou un partage reel

1. Creer une branche : `git checkout -b feature/nom-feature`
2. Coder dans le bon package (`dao/`, `service/`, `rest/`, etc.)
3. Tester : `mvn test`
4. Commiter : `git commit -m "feat: description"`
5. Push : `git push origin feature/nom-feature`
6. Creer une Pull Request

### Conventions

- Packages : `ma.transfert.[couche]`
- Entites : singulier (`User`, pas `Users`)
- REST endpoints : pluriel (`/transfers`, `/agencies`)
- Commits : `feat:`, `fix:`, `docs:`, `refactor:`
