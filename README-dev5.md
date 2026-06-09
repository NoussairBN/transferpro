# Module Documents & Interface Utilisateur (DEV-5)

> **Package :** `service/doc` · `jsf/` · `webapp/`  
> **Sprint 4** — Semaines 7-8

---

## 📋 Ce que ce module fournit

- **Workflow KYC complet** : upload de documents, validation / rejet par l'admin, mise à jour automatique du statut `kycStatus` de l'utilisateur
- **Génération de reçus PDF** avec iText 7, téléchargeables depuis l'interface
- **Interface JSF complète** : login/inscription, tableau de bord utilisateur, tableau de bord admin, historique des transferts, documents
- **Contrôle d'accès RBAC côté UI** : redirection automatique selon le rôle à la connexion

---

## 📁 Fichiers créés / modifiés

### Java
| Fichier | Description |
|---------|-------------|
| `service/doc/DocumentService.java` | Upload, validation et rejet KYC |
| `service/doc/PdfReceiptService.java` | Génération reçus PDF (iText 7) |
| `dao/DocumentDAO.java` | Accès base de données documents |
| `jsf/LoginBean.java` | Connexion + inscription (RBAC redirect) |
| `jsf/UserDashboardBean.java` | Tableau de bord utilisateur + formulaire transfert |
| `jsf/AdminBean.java` | Tableau de bord admin (KYC, transferts, users) |
| `jsf/KycBean.java` | Upload de documents KYC |
| `jsf/ReceiptBean.java` | Historique transferts + téléchargement PDF |

### Pages JSF
| Fichier | URL | Accès |
|---------|-----|-------|
| `login.xhtml` | `/login.xhtml` | Public |
| `user-dashboard.xhtml` | `/user-dashboard.xhtml` | INDIVIDUAL / AGENCY_AGENT |
| `admin-dashboard.xhtml` | `/admin-dashboard.xhtml` | ADMIN uniquement |
| `kyc-upload.xhtml` | `/kyc-upload.xhtml` | Connecté |
| `documents.xhtml` | `/documents.xhtml` | Connecté |
| `receipts.xhtml` | `/receipts.xhtml` | Connecté (vue adaptée selon rôle) |

---

## 🌐 Accès à l'interface

```
http://localhost:8080/money-transfer/login.xhtml
```

| Rôle | Email | Mot de passe | Redirigé vers |
|------|-------|-------------|---------------|
| Admin | `testadmin@test.ma` | `Test1234` | `admin-dashboard.xhtml` |
| Utilisateur | `youssef@test.ma` | `Test1234` | `user-dashboard.xhtml` |

> Pour créer un nouveau compte utilisateur, utiliser l'onglet **Créer un compte** sur la page de connexion (mot de passe minimum 8 caractères).

---

## 🔄 Workflow KYC

```
Utilisateur uploade un document (CNI, Passeport, Justificatif...)
        ↓
Document enregistré avec statut PENDING
        ↓
Admin voit le document dans l'onglet KYC du tableau de bord
        ↓
Admin clique VALIDER → statut passe à VALIDATED
Admin clique REJETER → statut passe à REJECTED
        ↓
Si tous les docs KYC validés → User.kycStatus = VERIFIED  ✅
Si un doc rejeté            → User.kycStatus = REJECTED  ❌
```

---

## 📄 Types de documents supportés

| Type | Usage |
|------|-------|
| `CNI_RECTO` | CIN recto |
| `CNI_VERSO` | CIN verso |
| `PASSPORT` | Passeport |
| `PROOF_OF_ADDRESS` | Justificatif de domicile |
| `RECEIPT` | Reçu de transfert généré |

Formats acceptés : **PDF, JPEG, PNG**

---

## 🧪 Tester le module manuellement

### 1. Tester le login / inscription
```
http://localhost:8080/money-transfer/login.xhtml
→ Onglet "Créer un compte" : remplir le formulaire
→ Onglet "Se connecter" : utiliser les identifiants créés
```

### 2. Tester le KYC (utilisateur)
```
→ Se connecter comme utilisateur
→ Aller dans "Documents KYC" (sidebar)
→ Choisir un type de document + uploader un fichier PNG
→ Le document apparaît avec statut PENDING
```

### 3. Valider le KYC (admin)
```
→ Se connecter comme admin (testadmin@test.ma / Test1234)
→ Onglet "KYC" → cliquer "✓ Valider"
→ Le statut passe à VALIDATED
→ La bannière verte "Identité vérifiée" apparaît chez l'utilisateur
```

### 4. Tester les reçus PDF
```
→ Se connecter comme utilisateur
→ Aller dans "Mes transferts"
→ Cliquer "📄 PDF" sur un transfert payé
→ Le PDF se télécharge automatiquement
```

---

## 🔗 Dépendances avec les autres modules

| Module | Ce que DEV-5 utilise |
|--------|---------------------|
| DEV-1 | Entités JPA, `persistence.xml` |
| DEV-2 | `TransferService`, `TransferDAO` (reçus PDF, historique) |
| DEV-3 | `AuthService`, `UserDAO` (login, register, KYC) |
| DEV-4 | `AgencyDAO` (liste des agences dans le formulaire de transfert) |

---

## ⚠️ Points d'attention

- Le `DocumentDAO.findByStatus()` utilise un `LEFT JOIN FETCH d.owner` obligatoire pour éviter une `LazyInitializationException` côté JSF
- Les dates JSF utilisent `bean.formatDate(localDateTime)` au lieu de `f:convertDateTime` (incompatible avec `LocalDateTime`)
- Les fichiers uploadés sont stockés dans `uploads/documents/` dans le conteneur WildFly

---

## 🌿 Branche Git

```bash
git checkout -b feature/dev5-documents
git add .
git commit -m "feat(docs): DocumentService, KYC workflow, JSF pages, PDF receipts"
git push origin feature/dev5-documents
```
