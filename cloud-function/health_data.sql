-- Table de données de santé pour Smart Watch HUB
CREATE TABLE health_data (
    -- Clé primaire auto-incrémentée
    Id BIGINT IDENTITY(1,1) PRIMARY KEY,

    -- Identifiants (anonymes, GDPR-compliant)
    UserId NVARCHAR(100) NOT NULL,           -- UUID anonyme généré par l'app
    DeviceId NVARCHAR(50) NOT NULL,          -- MAC address de la montre

    -- Timestamp
    Timestamp DATETIME2(3) NOT NULL,         -- Précision millisecondes

    -- Métriques cardiaques
    HeartRate INT NULL,                      -- BPM (30-220)
    BPSystolic INT NULL,                     -- mmHg (60-280)
    BPDiastolic INT NULL,                    -- mmHg (30-150)
    SpO2 INT NULL,                           -- % (70-100)

    -- Activité physique
    Steps INT NULL,                          -- Nombre de pas
    Calories INT NULL,                       -- Kcal brûlées
    Distance INT NULL,                       -- Mètres parcourus

    -- Santé métabolique
    Temperature FLOAT NULL,                  -- °C (35.0-41.0)
    BloodGlucose FLOAT NULL,                 -- mmol/L ou mg/dL

    -- Sommeil
    TotalSleep INT NULL,                     -- Minutes totales
    DeepSleep INT NULL,                      -- Minutes de sommeil profond
    LightSleep INT NULL,                     -- Minutes de sommeil léger

    -- Stress et métabolisme
    Stress INT NULL,                         -- Niveau 0-100
    MET FLOAT NULL,                          -- Metabolic Equivalent of Task
    MAI INT NULL,                            -- Movement Activity Index

    -- État du dispositif
    IsWearing BIT NOT NULL DEFAULT 1,        -- Montre portée ou non

    -- Audit et déduplication
    RecordHash NVARCHAR(32) NOT NULL,        -- SHA256 (16 premiers chars)
    CreatedAt DATETIME2(0) DEFAULT GETUTCDATE(),  -- Timestamp insertion serveur

    -- Contrainte d'unicité pour idempotence (empêche doublons)
    CONSTRAINT UQ_HealthData_Unique UNIQUE (UserId, DeviceId, Timestamp, RecordHash)
);

-- Index pour requêtes fréquentes (par utilisateur, par période)
CREATE INDEX IX_HealthData_UserId_Timestamp
    ON health_data(UserId, Timestamp DESC);

-- Index pour requêtes par device
CREATE INDEX IX_HealthData_DeviceId_Timestamp
    ON health_data(DeviceId, Timestamp DESC);

-- Index pour requêtes récentes (dernières 24h, 7j, etc.)
CREATE INDEX IX_HealthData_Timestamp
    ON health_data(Timestamp DESC);
