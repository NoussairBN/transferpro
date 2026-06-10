# TransferPro

<div align="center">

**Système de Transfert d'Argent — Architecture Jakarta EE**

*Inspiré de CashPlus · WafaCash · Western Union*

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Jakarta EE](https://img.shields.io/badge/Jakarta%20EE-10-blue)](https://jakarta.ee/)
[![WildFly](https://img.shields.io/badge/WildFly-40-red)](https://www.wildfly.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)](https://docs.docker.com/compose/)

</div>

---

## Table des matières

- [À propos](#à-propos)
- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Stack technique](#stack-technique)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Interfaces disponibles](#interfaces-disponibles)
- [API REST](#api-rest)
- [Comptes de test](#comptes-de-test)
- [Structure du projet](#structure-du-projet)
- [Équipe](#équipe)

---

## À propos

**TransferPro** est une application web de transfert d'argent développée avec l'architecture Java Enterprise Edition (JEE), dans le cadre du module JEE à l'ENSA Marrakech (2025-2026).

Le système reproduit les fonctionnalités essentielles d'un service de transfert d'argent en agence :

- Un expéditeur initie un transfert depuis l'interface web ou l'API
- Un code OTP unique est généré pour le retrait
- Le bénéficiaire retire les fonds en agence en présentant le code OTP
- L'administrateur supervise les transferts, valide les identités (KYC) et gère les agences

---

## Fonctionnalités

### Utilisateur
- ✅ Inscription et connexion sécurisée (JWT + BCrypt)
- ✅ Envoi d'argent avec calcul automatique des frais
- ✅ Suivi en temps réel des transferts (code de suivi public)
- ✅ Upload de documents KYC (CNI, passeport, justificatif de domicile)
- ✅ Téléchargement de reçus PDF

### Administrateur
- ✅ Validation / rejet des documents KYC
- ✅ Gestion du cycle de vie des transferts (confirmer, rendre disponible)
- ✅ Gestion des comptes utilisateurs (suspension, réactivation)
- ✅ Tableau de bord avec KPIs en temps réel

### Agences
- ✅ CRUD complet des agences
- ✅ Gestion de la caisse (crédit / débit)
- ✅ Assignation des agents
- ✅ Tableau de bord par agence

### Sécurité
- ✅ Authentification JWT (Auth0 java-jwt)
- ✅ Contrôle d'accès basé sur les rôles (RBAC)
- ✅ Hachage des mots de passe BCrypt
- ✅ Codes OTP générés avec `SecureRandom`
- ✅ Protection contre les injections SQL (Named Queries JPA)

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                     Client                          │
│          (Navigateur / Postman / API)               │
└────────────────────┬────────────────────────────────┘
                     │ HTTP :8080
┌────────────────────▼────────────────────────────────┐
│                   WildFly 40                        │
│                                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐  │
│  │  JSF / UI   │  │  JAX-RS API  │  │ JWT Filter│  │
│  └──────┬──────┘  └──────┬───────┘  └─────┬─────┘  │
│         │                │                │         │
│  ┌──────▼────────────────▼────────────────▼──────┐  │
│  │              EJB Session Beans                │  │
│  │   AuthService · TransferService · AgencyService  │
│  │   DocumentService · UserService               │  │
│  └──────────────────────┬────────────────────────┘  │
│                         │                           │
│  ┌──────────────────────▼────────────────────────┐  │
│  │              JPA / Hibernate                  │  │
│  │   UserDAO · TransferDAO · AgencyDAO           │  │
│  │   DocumentDAO · AuditLogDAO                   │  │
│  └──────────────────────┬────────────────────────┘  │
└─────────────────────────┼───────────────────────────┘
                          │ JDBC :5432
┌─────────────────────────▼───────────────────────────┐
│                  PostgreSQL 15                      │
│         users · agencies · transfers                │
│         documents · audit_logs                      │
└─────────────────────────────────────────────────────┘
```

### Couches applicatives

| Couche | Technologie | Rôle |
|--------|-------------|------|
| Présentation | JSF · PrimeFaces 13 | Interface utilisateur |
| API | JAX-RS (RESTEasy) | Endpoints REST |
| Sécurité | JWT · BCrypt · RBAC | Authentification & autorisation |
| Métier | EJB Session Beans · JTA | Logique métier & transactions |
| Persistance | JPA · Hibernate 7 | Accès aux données |
| Stockage | PostgreSQL 15 | Base de données relationnelle |

---

## Stack technique

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Langage | Java | 21 |
| Plateforme | Jakarta EE | 10 |
| Serveur | WildFly | 40 |
| Base de données | PostgreSQL | 15 |
| ORM | JPA / Hibernate | 7 |
| API REST | JAX-RS / RESTEasy | 7 |
| UI | JSF / PrimeFaces | 4.0 / 13 |
| Authentification | JWT (Auth0) | 4.4 |
| Mots de passe | BCrypt (jBCrypt) | 0.4 |
| PDF | iText | 7 |
| Build | Maven | 3.9+ |
| Conteneurs | Docker Compose | — |

---

## Prérequis

Avant de lancer le projet, assurez-vous d'avoir installé :

- **Java 21 JDK** — [Télécharger Temurin 21](https://adoptium.net/)
- **Maven 3.9+** — [Télécharger Maven](https://maven.apache.org/download.cgi)
- **Docker Desktop** — [Télécharger Docker](https://www.docker.com/products/docker-desktop/)

Vérification :
```bash
java -version   # doit afficher 21+
mvn -version    # doit afficher Java version: 21
docker -v       # doit afficher Docker version
```

---

## Installation

### Option 1 — Script automatique (recommandé)

```powershell
# Windows (PowerShell en mode Administrateur)
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

### Option 2 — Installation manuelle

```bash
# 1. Cloner le dépôt
git clone <url-du-repo>
cd <nom-du-repo>/final-setup

# 2. Configurer les variables d'environnement
cp .env.example .env

# 3. Compiler le projet
cd project
mvn clean package -DskipTests
cd ..

# 4. Lancer les conteneurs Docker
docker compose up -d --build

# 5. Attendre ~20 secondes puis vérifier
curl http://localhost:8080/money-transfer/api/health
# → {"status":"UP","version":"1.0.0","application":"MoneyTransfer JEE"}
```

### Redéployer après modification du code

```bash
cd project
mvn clean package -DskipTests
cd ..
docker cp project/target/money-transfer.war mt_wildfly:/opt/jboss/wildfly/standalone/deployments/
```

---

## Interfaces disponibles

| Interface | URL | Identifiants |
|-----------|-----|-------------|
| **Application web** | http://localhost:8080/money-transfer/login.xhtml | voir ci-dessous |
| **API REST** | http://localhost:8080/money-transfer/api | JWT requis |
| **Console WildFly** | http://localhost:9990 | Voir `.env` |
| **PgAdmin** | http://localhost:5050 | Voir `.env` |

### Pages de l'interface web

| Page | URL | Accès |
|------|-----|-------|
| Connexion / Inscription | `/login.xhtml` | Public |
| Tableau de bord utilisateur | `/user-dashboard.xhtml` | Connecté |
| Tableau de bord admin | `/admin-dashboard.xhtml` | ADMIN |
| Documents KYC | `/kyc-upload.xhtml` | Connecté |
| Historique transferts | `/receipts.xhtml` | Connecté |

---

## API REST

Base URL : `http://localhost:8080/money-transfer/api`

### Authentification

```bash
# Inscription
curl -X POST .../api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Hassan","lastName":"Alami","email":"hassan@test.ma","phone":"0612345678","password":"motdepasse123"}'

# Connexion → récupérer le token JWT
curl -X POST .../api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"hassan@test.ma","password":"motdepasse123"}'
```

### Endpoints disponibles

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| POST | `/auth/register` | — | Créer un compte |
| POST | `/auth/login` | — | Connexion → JWT |
| GET | `/users/me` | JWT | Mon profil |
| POST | `/transfers` | JWT | Créer un transfert |
| GET | `/transfers/{code}` | JWT | Détails d'un transfert |
| GET | `/transfers/track/{code}/status` | — | Suivi public |
| POST | `/transfers/{code}/confirm` | JWT | Confirmer |
| POST | `/transfers/{code}/available` | JWT | Rendre disponible |
| POST | `/transfers/{code}/pay?otp=XXXX` | JWT | Payer avec OTP |
| POST | `/transfers/{code}/cancel` | JWT | Annuler |
| GET | `/agencies` | JWT | Liste des agences |
| GET | `/agencies/{id}/dashboard` | JWT | KPIs agence |
| GET | `/health` | — | État du serveur |

---

## Comptes de test

| Rôle | Email | Mot de passe |
|------|-------|-------------|
| Administrateur | `testadmin@test.ma` | `Test1234` |
| Utilisateur | `youssef@test.ma` | `Test1234` |
| Agent Casablanca | `agent@casablanca.ma` | `Admin#1234` |

> Pour créer un nouveau compte : utiliser l'onglet **Créer un compte** sur la page de connexion (mot de passe minimum 8 caractères).

---

## Structure du projet

```
transferpro/
└── final-setup/
    ├── docker/wildfly/           ← Dockerfile WildFly personnalisé
    ├── project/
    │   └── src/main/
    │       ├── java/ma/transfert/
    │       │   ├── model/        ← Entités JPA
    │       │   ├── dao/          ← Couche d'accès aux données
    │       │   ├── service/      ← Logique métier (EJB)
    │       │   ├── rest/         ← Endpoints JAX-RS
    │       │   ├── security/     ← JWT, BCrypt, filtres
    │       │   ├── jsf/          ← Managed Beans JSF
    │       │   └── dto/          ← Objets de transfert
    │       ├── resources/
    │       │   └── META-INF/persistence.xml
    │       └── webapp/           ← Pages XHTML (JSF)
    ├── scripts/schema.sql        ← Schéma PostgreSQL + données initiales
    ├── docker-compose.yml
    ├── .env.example
    ├── setup-windows.ps1
    ├── setup.sh
    ├── README-dev2.md            ← Module Transferts
    ├── README-dev3.md            ← Module Auth & Utilisateurs
    ├── README-dev4.md            ← Module Agences
    └── README-dev5.md            ← Module Documents & UI
```

---

## Équipe

Projet réalisé par 6 développeurs dans le cadre du module **Java Enterprise Edition** — ENSA Marrakech · Année universitaire 2025-2026.

| Dev | Module | Statut |
|-----|--------|--------|
| DEV-1 | Architecture, Setup, Entités JPA, Docker | ✅ |
| DEV-2 | Transferts, OTP, Calcul des frais, API REST | ✅ |
| DEV-3 | Authentification, JWT, BCrypt, Utilisateurs | ✅ |
| DEV-4 | Agences, Caisse, Agents, Dashboard agence | ✅ |
| DEV-5 | Documents KYC, PDF iText, Interface JSF | ✅ |
| DEV-6 | Notifications, Audit, Tests JUnit 5 | ✅ |

**Encadré par :** Pr. A. NEJEOUI
