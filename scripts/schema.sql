-- =============================================================
--  MoneyTransfer — Schéma PostgreSQL
--  Exécuter avec : psql -U mt_user -d money_transfer_db -f schema.sql
-- =============================================================

-- Extensions PostgreSQL non compatibles avec Oracle SQL
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─────────────────────────────────────────────────────────────
-- AGENCIES
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS agencies (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20)  NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    address         VARCHAR(200) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(150),
    cash_balance    NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    daily_limit     NUMERIC(15,2) NOT NULL DEFAULT 500000.00,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE','SUSPENDED','CLOSED')),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────
-- USERS
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    phone           VARCHAR(20)  NOT NULL UNIQUE,
    cin             VARCHAR(20)  UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(30)  NOT NULL
                        CHECK (role IN ('INDIVIDUAL','AGENCY_AGENT','ADMIN')),
    kyc_status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                        CHECK (kyc_status IN ('PENDING','VERIFIED','REJECTED')),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE','SUSPENDED','BLOCKED')),
    agency_id       BIGINT REFERENCES agencies(id) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMP
);

-- ─────────────────────────────────────────────────────────────
-- TRANSFERS (Version corrigée pour JPA)
-- ─────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS transfers;

CREATE TABLE transfers (
    id                      BIGSERIAL PRIMARY KEY,
    tracking_code           VARCHAR(50) NOT NULL UNIQUE,
    amount                  NUMERIC(15,2) NOT NULL CHECK (amount >= 50 AND amount <= 50000),
    fees                    NUMERIC(10,2) DEFAULT 0.00,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING','CONFIRMED','AVAILABLE','PAID','EXPIRED','CANCELLED')),
    otp_code                VARCHAR(8),
    sender_name             VARCHAR(100) NOT NULL,
    sender_phone            VARCHAR(20),
    receiver_name           VARCHAR(100) NOT NULL,
    receiver_phone          VARCHAR(20) NOT NULL,
    receiver_email          VARCHAR(100),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    paid_at                 TIMESTAMP,
    expires_at              TIMESTAMP,
    notes                   VARCHAR(500),
    version                 INTEGER DEFAULT 0,
    sending_agency_id       BIGINT REFERENCES agencies(id),
    receiving_agency_id     BIGINT REFERENCES agencies(id),
    user_id                 BIGINT REFERENCES users(id)
);

-- ─────────────────────────────────────────────────────────────
-- AUDIT LOGS
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       BIGINT,
    details         TEXT,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────
-- INDEX (performance)
-- ─────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_transfers_tracking   ON transfers(tracking_code);
CREATE INDEX IF NOT EXISTS idx_transfers_otp        ON transfers(otp_code);
CREATE INDEX IF NOT EXISTS idx_transfers_status     ON transfers(status);
CREATE INDEX IF NOT EXISTS idx_transfers_created    ON transfers(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transfers_sending    ON transfers(sending_agency_id);
CREATE INDEX IF NOT EXISTS idx_transfers_receiving  ON transfers(receiving_agency_id);
CREATE INDEX IF NOT EXISTS idx_transfers_user       ON transfers(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_user           ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action         ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_created        ON audit_logs(created_at DESC);

-- ─────────────────────────────────────────────────────────────
-- DONNÉES INITIALES (seed)
-- ─────────────────────────────────────────────────────────────

-- Agences de test
INSERT INTO agencies (code, name, address, city, cash_balance, daily_limit) VALUES
  ('AGC-CASA-001', 'Agence Casablanca Centre',  'Bd Mohammed V, Casablanca',   'Casablanca', 250000.00, 500000.00),
  ('AGC-RABAT-001','Agence Rabat Agdal',         'Av. Fal Ould Oumeir, Rabat',  'Rabat',      180000.00, 500000.00),
    ('AGC-FES-001',  'Agence Fès Médina',          'Rue Serrajine, Fès',          'Fès',        120000.00, 300000.00);

-- Admin par défaut (mot de passe: Admin#1234 — BCrypt)
INSERT INTO users (first_name, last_name, email, phone, password_hash, role, kyc_status, status)
VALUES ('Admin', 'Système', 'admin@moneytransfer.ma', '0600000000',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK9BQ.5Bq',
        'ADMIN', 'VERIFIED', 'ACTIVE')
;

-- Agent de test pour l'agence Casablanca
INSERT INTO users (first_name, last_name, email, phone, password_hash, role, kyc_status, status, agency_id)
VALUES ('Agent', 'Casablanca', 'agent@casablanca.ma', '0612345678',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj/RK9BQ.5Bq',
        'AGENCY_AGENT', 'VERIFIED', 'ACTIVE', 1)
;

SELECT 'Schema créé avec succès !' as message;