# Module Gestion des Agences (DEV-4)

> **Package :** `ma.transfert.dto` + `ma.transfert.service` (AgencyService) + `ma.transfert.rest` (AgencyResource)
> **Tests :** 9 tests JUnit 5 + Mockito — 0 failure

---

## 📋 Ce que ce module fournit

- **CRUD Agences** : Création, modification, consultation et gestion de l'unicité du code de l'agence (ex: `AGC-CAS-01`).
- **Gestion du statut** : Activation, suspension ou fermeture définitive d'une agence (`ACTIVE`, `SUSPENDED`, `CLOSED`).
- **Gestion de la caisse (Cash Management)** : Alimentation (crédit) et retrait (débit) du solde de caisse de l'agence avec validation stricte du solde disponible (pas de découvert de caisse).
- **Affectation des agents** : Assignation et retrait des agents de guichet (`AGENCY_AGENT`) à une agence, avec vérification du rôle et du statut actif de l'utilisateur.
- **Tableau de bord (Dashboard) par agence** : KPIs financiers et statistiques en temps réel :
  - Volumes financiers (Total des fonds envoyés, reçus, commissions d'agence perçues).
  - Statuts des transferts (Nombre de transferts en attente, confirmés, payés, annulés).
  - Informations opérationnelles (Nombre d'agents affectés, solde de caisse actuel, limite journalière).
- **Sécurisation par rôles (RBAC)** :
  - **ADMIN** : Droit de modification sur toutes les agences, gestion de la caisse, assignation des agents.
  - **AGENCY_AGENT** : Consultation des agences actives, accès exclusif au tableau de bord de sa propre agence.

---

## 📡 Endpoints REST

Base URL : `http://localhost:8080/money-transfer/api`

### 🔒 Routes protégées (JWT requis)

| Méthode | Endpoint | Rôle requis | Description |
|---------|----------|-------------|-------------|
| **GET** | `/agencies` | `ADMIN` | Lister toutes les agences (tous statuts) |
| **GET** | `/agencies/active` | `Tous connectés` | Lister les agences actives |
| **GET** | `/agencies/{id}` | `Tous connectés` | Détails d'une agence |
| **POST** | `/agencies` | `ADMIN` | Créer une nouvelle agence |
| **PUT** | `/agencies/{id}` | `ADMIN` | Modifier les infos d'une agence |
| **POST** | `/agencies/{id}/status` | `ADMIN` | Changer le statut d'une agence |
| **POST** | `/agencies/{id}/cash/add` | `ADMIN` | Alimenter la caisse d'une agence |
| **POST** | `/agencies/{id}/cash/remove` | `ADMIN` | Retirer du cash de la caisse |
| **POST** | `/agencies/{id}/agents/{agentId}` | `ADMIN` | Assigner un agent à l'agence |
| **DELETE** | `/agencies/agents/{agentId}` | `ADMIN` | Retirer un agent de son agence |
| **GET** | `/agencies/{id}/dashboard` | `ADMIN`, `AGENCY_AGENT` (de l'agence) | KPIs et statistiques de l'agence |
| **GET** | `/agencies/{id}/agents` | `ADMIN` | Liste des agents affectés à l'agence |

---

## 📝 Exemples de requêtes

Pour toutes les requêtes ci-dessous, ajouter le header :
`Authorization: Bearer <JWT_TOKEN>`

### 1. Créer une agence (ADMIN)

```http
POST /api/agencies
Content-Type: application/json

{
    "code": "AGC-CAS-01",
    "name": "Agence Casablanca Oasis",
    "address": "120 Boulevard de l'Oasis",
    "city": "Casablanca",
    "phone": "0522123456",
    "email": "oasis@transferpro.ma",
    "dailyLimit": 250000.00
}
```

**Réponse 201 Created :**
```json
{
    "id": 1,
    "code": "AGC-CAS-01",
    "name": "Agence Casablanca Oasis",
    "address": "120 Boulevard de l'Oasis",
    "city": "Casablanca",
    "phone": "0522123456",
    "email": "oasis@transferpro.ma",
    "cashBalance": 0.00,
    "dailyLimit": 250000.00,
    "status": "ACTIVE",
    "createdAt": "2026-05-30T22:15:00"
}
```

---

### 2. Alimenter la caisse (ADMIN)

```http
POST /api/agencies/1/cash/add
Content-Type: application/json

{
    "amount": 50000.00
}
```

**Réponse 200 OK :**
```json
{
    "message": "Caisse créditée avec succès"
}
```

---

### 3. Retirer du cash (ADMIN)

```http
POST /api/agencies/1/cash/remove
Content-Type: application/json

{
    "amount": 10000.00
}
```

**Réponse 200 OK :**
```json
{
    "message": "Caisse débitée avec succès"
}
```

> **Note :** Si le solde est insuffisant (ex: retrait de 100 000 MAD alors que la caisse a 40 000 MAD), l'API renvoie une erreur **400 Bad Request** :
> ```json
> { "error": "Solde de caisse insuffisant (Solde actuel: 40000 MAD)" }
> ```

---

### 4. Assigner un agent de guichet à l'agence (ADMIN)

```http
POST /api/agencies/1/agents/3
```

**Réponse 200 OK :**
```json
{
    "message": "Agent assigné à l'agence avec succès"
}
```

---

### 5. Consulter le tableau de bord (ADMIN ou Agent de l'agence 1)

```http
GET /api/agencies/1/dashboard
```

**Réponse 200 OK :**
```json
{
    "agencyId": 1,
    "agencyCode": "AGC-CAS-01",
    "agencyName": "Agence Casablanca Oasis",
    "status": "ACTIVE",
    "cashBalance": 40000.00,
    "dailyLimit": 250000.00,
    "totalTransfers": 150,
    "pendingTransfers": 5,
    "confirmedTransfers": 12,
    "paidTransfers": 130,
    "cancelledTransfers": 3,
    "totalVolumeSent": 325000.00,
    "totalVolumeReceived": 280000.00,
    "totalFeesCollected": 6150.00,
    "agentCount": 2
}
```

---

## 🏗️ Architecture du module

Le module suit l'architecture multicouche JEE standard du projet :

```
final-setup/project/src/main/java/ma/transfert/
│
├── dto/
│   ├── AgencyDTO.java               ← Représentation publique d'une agence
│   ├── AgencyRequestDTO.java        ← Payload de création et mise à jour
│   ├── AgencyDashboardDTO.java      ← Regroupement des KPIs et statistiques
│   ├── AgentDTO.java                ← Infos simplifiées sur un agent de guichet
│   └── CashOperationRequestDTO.java ← Payload pour alimenter / débiter la caisse
│
├── service/
│   └── AgencyService.java           ← Logique métier (CRUD, cash flow, RBAC interne)
│
└── rest/
    └── AgencyResource.java          ← Rest endpoints de gestion des agences
```

---

## 🧪 Tests

Les tests unitaires utilisent Mockito pour valider les règles de gestion sans dépendance à la base de données.

```bash
cd final-setup/project

# Exécuter les tests du module Agences
mvn test -Dtest="AgencyServiceTest"
```

### Détail des tests unitaires (`AgencyServiceTest.java`) :

| Méthode testée | Cas de test | Description |
|----------------|-------------|-------------|
| `createAgency` | `validData` | Création OK avec valeurs valides |
| `createAgency` | `duplicateCode` | Échec si le code unique de l'agence existe déjà |
| `createAgency` | `missingFields` | Échec si les informations obligatoires sont vides |
| `updateAgency` | `validData` | Mise à jour des champs autorisés |
| `addCash` | `validAmount` | Crédit de caisse et mise à jour du solde |
| `removeCash` | `sufficientFunds` | Débit de caisse autorisé |
| `removeCash` | `insufficientFunds`| Échec si le retrait dépasse le solde de la caisse |
| `assignAgent` | `validAgent` | Rattachement OK d'un agent actif à l'agence |
| `assignAgent` | `invalidRole` | Échec si l'utilisateur rattaché n'est pas un `AGENCY_AGENT` |

---

## 📌 Notes pour les autres développeurs

> **Sécurité du Dashboard** : Le tableau de bord (`/agencies/{id}/dashboard`) est accessible aux **ADMINs** pour toutes les agences, mais un **AGENCY_AGENT** ne peut accéder **qu'au tableau de bord de son agence de rattachement**. Une tentative d'accès à une autre agence renvoie un code **403 Forbidden**.

> **Limite journalière** : Chaque agence dispose d'une limite journalière (`dailyLimit`, par défaut `500 000 MAD`). Cette limite devra être validée lors de l'envoi de transferts (Module Transferts - DEV-5).
