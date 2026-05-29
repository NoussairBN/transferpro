## README pour le Module Transfert (DEV-2)

Créez ce fichier `README.md` à la racine de votre projet :

```markdown
# Money Transfer JEE - Module Transfert (DEV-2)

## 🚀 API de Transfert d'Argent - Documentation

Système de transfert d'argent développé avec Jakarta EE 10, WildFly 40 et PostgreSQL.

---

## 📋 Prérequis pour les autres développeurs

### Installation nécessaire :
- **Docker Desktop** (Windows/Mac) ou **Docker Engine** (Linux)
- **Git** (pour cloner le projet)
- **Java 21** (JDK)
- **Maven 3.9+**
- **Postman** (optionnel, pour tester les API)

---

## 🐳 Infrastructure Docker (Partagée)

Toute l'infrastructure est **conteneurisée et partagée** via Docker Compose :

| Conteneur | Image | Port | Rôle |
|-----------|-------|------|------|
| `mt_postgres` | postgres:15-alpine | 5432 | Base de données |
| `mt_wildfly` | Custom (WildFly 40) | 8080, 9990 | Serveur d'application |
| `mt_pgadmin` | dpage/pgadmin4 | 5050 | Interface DB graphique |

### ✅ Avantages pour l'équipe :
- **Identique pour tous** : même environnement, même configuration
- **Pas d'installation manuelle** : tout est dans Docker
- **Portabilité** : fonctionne sur Windows, Mac, Linux
- **Reproductible** : `docker compose up` et tout démarre

---

## 🚀 Démarrage rapide pour un nouveau développeur

### 1. Cloner le projet
```bash
git clone <url-du-repo>
cd final-setup
```

### 2. Configurer les variables d'environnement
```bash
cp .env.example .env
# Éditer .env avec vos valeurs (ou garder celles par défaut)
```

### 3. Démarrer l'infrastructure Docker
```bash
docker compose up -d --build
```

### 4. Compiler et déployer l'application
```bash
cd project
mvn clean package -DskipTests
cd ..
docker cp project/target/money-transfer.war mt_wildfly:/opt/jboss/wildfly/standalone/deployments/
```

### 5. Vérifier que tout fonctionne
```bash
curl http://localhost:8080/money-transfer/api/health
# Réponse: {"status":"UP","version":"1.0.0","application":"MoneyTransfer JEE"}
```

---

## 📡 Endpoints API REST

Base URL : `http://localhost:8080/money-transfer/api`

### Transferts

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/transfers` | Créer un transfert |
| GET | `/transfers/agency/{agencyId}` | Liste des transferts d'une agence |
| GET | `/transfers/track/{trackingCode}/status` | Suivre un transfert |
| GET | `/transfers/{trackingCode}` | Détails d'un transfert |
| POST | `/transfers/{trackingCode}/confirm` | Confirmer un transfert |
| POST | `/transfers/{trackingCode}/available` | Mettre à disposition |
| POST | `/transfers/{trackingCode}/pay?otp={code}` | Payer un transfert |
| POST | `/transfers/{trackingCode}/cancel` | Annuler un transfert |

### Health Check

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/health` | Vérifier l'état de l'application |

---

## 📝 Exemples de requêtes Postman

### 1. Créer un transfert
```json
POST /api/transfers
Content-Type: application/json

{
    "amount": 1500.00,
    "senderName": "Hassan Alami",
    "senderPhone": "0612345678",
    "receiverName": "Fatima Zahra",
    "receiverPhone": "0687654321",
    "receiverEmail": "fatima@example.com"
}
```

### 2. Suivre un transfert
```
GET /api/transfers/track/TRF-20260528-E3138186/status
```

### 3. Payer un transfert
```
POST /api/transfers/TRF-20260528-E3138186/pay?otp=92733794
```

---

## 🔄 Cycle de vie d'un transfert

```
PENDING → CONFIRMED → AVAILABLE → PAID
         ↓
      CANCELLED
```

| Statut | Description |
|--------|-------------|
| PENDING | Créé, en attente |
| CONFIRMED | Confirmé, fonds réservés |
| AVAILABLE | Prêt pour retrait (OTP envoyé) |
| PAID | Payé - Terminé |
| CANCELLED | Annulé |
| EXPIRED | Expiré (30 jours) |

---

## 🛠️ Commandes utiles pour l'équipe

### Gestion des conteneurs
```bash
# Démarrer tous les services
docker compose up -d

# Arrêter tous les services
docker compose down

# Voir les logs
docker compose logs -f wildfly

# Redémarrer WildFly
docker restart mt_wildfly
```

### Accès aux interfaces
| Service | URL | Identifiants |
|---------|-----|--------------|
| API REST | http://localhost:8080/money-transfer/api | - |
| WildFly Console | http://localhost:9990 | admin / Admin@123! |
| PgAdmin | http://localhost:5050 | admin@local.com / admin |

### Commandes Maven
```bash
cd project
mvn clean compile          # Compiler
mvn clean package -DskipTests  # Générer le WAR
mvn test                   # Lancer les tests
```

### Redéployer après modification
```bash
cd project
mvn clean package -DskipTests
cd ..
docker cp project/target/money-transfer.war mt_wildfly:/opt/jboss/wildfly/standalone/deployments/
```

### Accès à la base de données
```bash
# Se connecter à PostgreSQL
docker exec -it mt_postgres psql -U postgres -d transferdb

# Voir les transferts
SELECT * FROM transfers ORDER BY id DESC;
```

---

## 📂 Structure du projet

```
final-setup/
├── docker/
│   └── wildfly/
│       ├── Dockerfile          # Image WildFly personnalisée
│       └── setup.cli           # Configuration WildFly
├── project/
│   └── src/main/java/ma/transfert/
│       ├── model/              # Entités JPA
│       ├── dao/                # Accès base de données
│       ├── service/            # Logique métier
│       ├── rest/               # Endpoints REST
│       └── dto/                # Transferts de données
├── scripts/
│   └── schema.sql              # Schéma PostgreSQL
├── docker-compose.yml
├── .env.example                # Variables d'environnement
└── README.md
```

---

## ✅ Vérification pour les nouveaux développeurs

Après exécution des commandes, tout doit être vert :

```bash
# 1. Vérifier les conteneurs (3 conteneurs doivent être "Up")
docker compose ps

# 2. Vérifier l'API
curl http://localhost:8080/money-transfer/api/health

# 3. Vérifier la base de données
docker exec -it mt_postgres psql -U postgres -d transferdb -c "SELECT 1"
```

---