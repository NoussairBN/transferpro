# Module Auth & Utilisateurs (DEV-3)

> **Package :** `ma.transfert.security` + `ma.transfert.service` (Auth/User) + `ma.transfert.rest` (Auth/User)
> **Tests :** 39 tests JUnit 5 + Mockito — 0 failure

---

## 📋 Ce que ce module fournit

- **Inscription** d'un nouvel utilisateur avec validation
- **Connexion** email/password → token **JWT** (24h)
- **Filtre de sécurité** : toutes les requêtes avec `Authorization: Bearer <token>` sont authentifiées automatiquement
- **Gestion du profil** : consultation, mise à jour, changement de mot de passe
- **Gestion admin** : suspension / réactivation de comptes

---

## 📡 Endpoints REST

Base URL : `http://localhost:8080/money-transfer/api`

### 🔓 Routes publiques (pas de JWT)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/auth/register` | Créer un compte |
| POST | `/auth/login` | Se connecter → obtenir un JWT |

### 🔒 Routes protégées (JWT requis)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/users/me` | Mon profil |
| PUT | `/users/me` | Modifier prénom / nom / téléphone |
| POST | `/users/me/password` | Changer le mot de passe |
| GET | `/users/{id}` | Profil d'un utilisateur (admin) |
| POST | `/users/{id}/suspend` | Suspendre un compte (admin) |
| POST | `/users/{id}/activate` | Réactiver un compte (admin) |

---

## 📝 Exemples de requêtes

### 1. S'inscrire

```http
POST /api/auth/register
Content-Type: application/json

{
    "firstName": "Hassan",
    "lastName": "Alami",
    "email": "hassan@test.ma",
    "phone": "0612345678",
    "password": "motdepasse123"
}
```

**Réponse 201 :**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "email": "hassan@test.ma",
    "fullName": "Hassan Alami",
    "role": "INDIVIDUAL"
}
```

---

### 2. Se connecter

```http
POST /api/auth/login
Content-Type: application/json

{
    "email": "hassan@test.ma",
    "password": "motdepasse123"
}
```

**Réponse 200 :**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "email": "hassan@test.ma",
    "fullName": "Hassan Alami",
    "role": "INDIVIDUAL"
}
```

**Réponse 401 (mauvais identifiants) :**
```json
{ "error": "Email ou mot de passe incorrect" }
```

---

### 3. Utiliser le token JWT

Ajouter ce header à **toutes les requêtes protégées** :
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

### 4. Consulter son profil

```http
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Réponse 200 :**
```json
{
    "id": 1,
    "firstName": "Hassan",
    "lastName": "Alami",
    "email": "hassan@test.ma",
    "phone": "0612345678",
    "role": "INDIVIDUAL",
    "kycStatus": "PENDING",
    "status": "ACTIVE"
}
```

---

### 5. Modifier son profil

```http
PUT /api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
    "firstName": "Hassan",
    "lastName": "Alami",
    "phone": "0699999999"
}
```

---

### 6. Changer le mot de passe

```http
POST /api/users/me/password
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
    "oldPassword": "motdepasse123",
    "newPassword": "nouveaumdp456"
}
```

---

## 🔐 Comment fonctionne la sécurité

```
Requête HTTP
    │
    ▼
JWTAuthFilter  (intercepte TOUTES les requêtes)
    │
    ├─ Pas de header Authorization ?  → continue normalement (route publique)
    │
    ├─ Token valide ?  → injecte SecurityContext (userId + role)
    │                    → le endpoint reçoit l'identité de l'utilisateur
    │
    └─ Token invalide / expiré ?  → 401 Unauthorized immédiat
```

Pour utiliser l'identité dans un autre endpoint (DEV-4, DEV-5, DEV-6) :

```java
@GET
@Path("/mon-endpoint")
public Response monEndpoint(@Context SecurityContext sc) {
    // Récupérer l'userId depuis le token JWT
    Long userId = Long.parseLong(sc.getUserPrincipal().getName());

    // Vérifier le rôle
    boolean isAdmin = sc.isUserInRole("ADMIN");
    boolean isAgent = sc.isUserInRole("AGENCY_AGENT");

    // ... logique métier
}
```

---

## 🏗️ Architecture du module

```
security/
├── PasswordUtil.java     ← BCrypt hash/verify (facteur coût 12)
├── JWTUtil.java          ← Génération et validation JWT (Auth0, 24h)
└── JWTAuthFilter.java    ← Filtre JAX-RS @Priority(AUTHENTICATION)

service/
├── UserService.java      ← Inscription, profil, mot de passe, statuts
└── AuthService.java      ← Login → LoginResult{token, userId, role...}

rest/
├── AuthResource.java     ← POST /auth/login + /auth/register
└── UserResource.java     ← GET/PUT /users/me + routes admin
```

---

## 👤 Rôles disponibles

| Rôle | `UserRole` | Description |
|------|-----------|-------------|
| Particulier | `INDIVIDUAL` | Client inscrit (défaut à l'inscription) |
| Agent d'agence | `AGENCY_AGENT` | Travaille au guichet |
| Administrateur | `ADMIN` | Accès complet |

---

## ⚠️ Règles de validation

| Champ | Règle |
|-------|-------|
| Email | Doit contenir `@`, unique en base |
| Téléphone | Unique en base |
| Mot de passe | Minimum 8 caractères |
| Prénom / Nom | Non vide |
| Token JWT | Durée de vie 24h, signé HMAC256 |

---

## 🧪 Tests

```bash
cd final-setup/project

# Lancer tous les tests du module auth/user
mvn test -Dtest="PasswordUtilTest,JWTUtilTest,UserServiceTest,AuthServiceTest"

# Résultat attendu
# Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
# BUILD SUCCESS
```

| Classe de test | Tests | Ce qui est testé |
|----------------|-------|-----------------|
| `PasswordUtilTest` | 8 | hash BCrypt, verify, cas limites |
| `JWTUtilTest` | 10 | génération, vérification, extraction claims |
| `UserServiceTest` | 12 | register, findById, updateProfile, changePassword, suspend |
| `AuthServiceTest` | 9 | login succès/échec, compte suspendu, register auto-login |

---

## 🔗 Dépendances utilisées

```xml
<!-- JWT -->
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>4.4.0</version>
</dependency>

<!-- BCrypt -->
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

---

## 📌 Notes pour les autres devs (DEV-4, DEV-5, DEV-6)

> **Pour sécuriser vos endpoints**, utilisez simplement `@Context SecurityContext sc` dans vos méthodes JAX-RS. Le `JWTAuthFilter` fait tout le travail automatiquement.

> **L'utilisateur connecté** est accessible via `sc.getUserPrincipal().getName()` (= userId en String).

> **Vérifier un rôle** : `sc.isUserInRole("ADMIN")` ou `sc.isUserInRole("AGENCY_AGENT")`.

> **Route sans auth** : si votre endpoint est public, ignorez le `SecurityContext` — le filtre laisse passer les requêtes sans token.
