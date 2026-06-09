# TransferPro — Système de Transfert d'Argent (JEE)

> Inspiré de CashPlus / WafaCash · Jakarta EE 10 · WildFly 40 · PostgreSQL

---

## 👥 Équipe & Responsabilités

| Dev | Rôle | Module | Statut |
|-----|------|--------|--------|
| **DEV-1** | Architecte / Lead | Setup Maven, Docker, Entités JPA, Schema SQL | ✅ Sprint 1 terminé |
| **DEV-2** | Module Transferts | TransferService, TransferDAO, API REST, OTP, Frais | ✅ Sprint 3 terminé |
| **DEV-3** | Module Utilisateurs & Auth | UserService, AuthService, JWT, BCrypt, API REST Auth | ✅ Sprint 2 terminé |
| **DEV-4** | Module Agences | AgencyService, AgencyDAO, AgencyResource, Caisse | ✅ Sprint 4 terminé |
| **DEV-5** | Documents & UI | DocumentService, PDF (iText), Pages JSF, KYC |  ✅ Sprint 5 terminé |
| **DEV-6** | Notifications & Tests | NotificationService, AuditLog, JUnit 5, OpenAPI | ✅ Sprint 6 terminé |

---

## 🚀 Démarrage rapide

### Prérequis
- Java 21 (JDK)
- Maven 3.9+
- Docker Desktop

### Lancer le projet

```powershell
# Windows (PowerShell Admin)
cd final-setup
Set-ExecutionPolicy Bypass -Scope Process -Force
.\setup-windows.ps1
```

```bash
# Linux / macOS
cd final-setup
chmod +x setup.sh
bash setup.sh
```

### Vérifier que tout fonctionne

```bash
curl http://localhost:8080/money-transfer/api/health
# → {"status":"UP","version":"1.0.0","application":"MoneyTransfer JEE"}
```

---

## 📡 API REST disponible

Base URL : `http://localhost:8080/money-transfer/api`

### Auth (DEV-3) — Public
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/auth/register` | Créer un compte |
| POST | `/auth/login` | Connexion → token JWT |

### Utilisateurs (DEV-3) — JWT requis
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/users/me` | Mon profil |
| PUT | `/users/me` | Modifier mon profil |
| POST | `/users/me/password` | Changer le mot de passe |
| GET | `/users/{id}` | Profil par ID (admin) |
| POST | `/users/{id}/suspend` | Suspendre un compte (admin) |
| POST | `/users/{id}/activate` | Réactiver un compte (admin) |

### Transferts (DEV-2) — JWT requis
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/transfers` | Créer un transfert |
| GET | `/transfers/{code}` | Détails d'un transfert |
| GET | `/transfers/track/{code}/status` | Suivi public (sans JWT) |
| POST | `/transfers/{code}/confirm` | Confirmer |
| POST | `/transfers/{code}/available` | Mettre à disposition |
| POST | `/transfers/{code}/pay?otp=XXXX` | Payer (vérif OTP) |
| POST | `/transfers/{code}/cancel` | Annuler |

### System
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/health` | État de l'application |

---

## 🔐 Utiliser les endpoints sécurisés

```bash
# 1. S'inscrire
curl -X POST http://localhost:8080/money-transfer/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Hassan","lastName":"Alami","email":"hassan@test.ma","phone":"0612345678","password":"motdepasse123"}'

# 2. Se connecter → récupérer le token
curl -X POST http://localhost:8080/money-transfer/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"hassan@test.ma","password":"motdepasse123"}'
# → {"token":"eyJ...", "userId":1, "role":"INDIVIDUAL"}

# 3. Utiliser le token
curl http://localhost:8080/money-transfer/api/users/me \
  -H "Authorization: Bearer eyJ..."
```

---

## 🏗️ Architecture

```
final-setup/
├── docker/wildfly/        ← Image WildFly + driver JDBC
├── project/src/main/java/ma/transfert/
│   ├── model/             ← Entités JPA (User, Agency, Transfer, AuditLog)
│   ├── dao/               ← Accès base de données (BaseDAO, UserDAO, TransferDAO...)
│   ├── service/           ← Logique métier (AuthService, UserService, TransferService...)
│   ├── rest/              ← Endpoints REST (AuthResource, UserResource, TransferResource...)
│   ├── security/          ← JWT + BCrypt (JWTUtil, JWTAuthFilter, PasswordUtil)
│   ├── dto/               ← Objets de transfert de données
│   └── exception/         ← Gestion des erreurs (BusinessException)
├── scripts/schema.sql     ← Schéma PostgreSQL
├── docker-compose.yml
└── README_setup.md        ← Guide d'installation détaillé
```

---

## 🛠️ Accès aux interfaces

| Service | URL | Identifiants |
|---------|-----|--------------|
| API REST | http://localhost:8080/money-transfer/api | — |
| WildFly Console | http://localhost:9990 | admin / Admin@123! |
| PgAdmin | http://localhost:5050 | admin@local.com / admin |

---

## 🌿 Convention Git

```
master                  ← code stable intégré
feature/dev1-setup      ← DEV-1 (terminé, mergé)
feature/dev2-transfers  ← DEV-2 (terminé, mergé)
feature/dev3-auth       ← DEV-3 (terminé, mergé)
feature/dev4-agencies   ← DEV-4 (à créer)
feature/dev5-documents  ← DEV-5 (à créer)
feature/dev6-notif      ← DEV-6 (à créer)
```

**Format des commits :**
```
feat(auth): description courte
fix(transfer): correction bug
docs: mise à jour README
test(user): ajout tests unitaires
```

---

## 📋 Planning Sprints

| Sprint | Semaines | Objectif | Livrable | Statut |
|--------|----------|----------|----------|--------|
| 1 | 1-2 | Setup + Fondations | App deployable connectée DB | ✅ |
| 2 | 3-4 | Auth + Utilisateurs | Login JWT fonctionnel | ✅ |
| 3 | 5-6 | Transferts | Création et suivi de transferts | ✅ |
| 4 | 7-8 | Agences + Documents | CRUD agences, upload, PDF | 🔲 |
| 5 | 9-10 | Notifications + Audit | Système complet | 🔲 |
| 6 | 11-12 | HA + Sécurité + Soutenance | Démo live | 🔲 |
