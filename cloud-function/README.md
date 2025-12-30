# Cloud Function Smart Watch Hub

Google Cloud Function (Python) pour synchroniser les données de santé vers Azure SQL Database.

## 📋 Fichiers

- **main.py** - Cloud Function principal (endpoint HTTP)
- **config.py** - Configuration SQLAlchemy + Google Secret Manager
- **models.py** - Modèles SQLAlchemy pour la table health_data
- **validators.py** - Validation des données de santé
- **requirements.txt** - Dépendances Python

## 🚀 Déploiement

### Prérequis

```bash
# Installer Google Cloud SDK
# https://cloud.google.com/sdk/docs/install

# Vous authentifier
gcloud auth login

# Installer gcloud CLI
gcloud init
```

### Déployer la fonction

Depuis le répertoire `Smart_Watch/`:

```bash
bash gcp-setup-and-deploy.sh
```

Ce script va:
1. ✅ Créer le projet GCP `smart-watch-hub`
2. ✅ Activer les APIs nécessaires
3. ✅ Créer un Service Account
4. ✅ Configurer Google Secret Manager avec vos credentials Azure SQL
5. ✅ Déployer la Cloud Function
6. ✅ Afficher l'URL de la fonction

### Configuration Manuelle (Alternative)

Si vous préférez faire manuellement:

```bash
# 1. Créer le projet
gcloud projects create smart-watch-hub --name="Smart Watch Hub"
gcloud config set project smart-watch-hub

# 2. Activer les APIs
gcloud services enable cloudfunctions.googleapis.com cloudbuild.googleapis.com secretmanager.googleapis.com

# 3. Créer les secrets
echo "smartwatch.database.windows.net" | gcloud secrets create azure-sql-server --data-file=-
echo "health_db" | gcloud secrets create azure-sql-database --data-file=-
echo "admin@smartwatch" | gcloud secrets create azure-sql-user --data-file=-
echo "YourPassword123!" | gcloud secrets create azure-sql-password --data-file=-

# 4. Déployer
gcloud functions deploy uploadHealthMetrics \
  --gen2 \
  --runtime python311 \
  --region us-central1 \
  --source=./cloud-function \
  --entry-point=uploadHealthMetrics \
  --trigger-http \
  --allow-unauthenticated \
  --timeout 60 \
  --memory 512MB
```

## 🧪 Tester la Fonction

### Avec curl

```bash
FUNCTION_URL="https://us-central1-smart-watch-hub.cloudfunctions.net/uploadHealthMetrics"

curl -X POST $FUNCTION_URL \
  -H "Content-Type: application/json" \
  -d '{
    "metrics": [
      {
        "userId": "user-123",
        "deviceId": "AA:BB:CC:DD:EE:FF",
        "timestamp": 1704067200000,
        "heartRate": 72,
        "bpSystolic": 120,
        "bpDiastolic": 80,
        "spO2": 98,
        "steps": 1250,
        "calories": 85,
        "distance": 950,
        "temperature": 36.8,
        "bloodGlucose": 100.5,
        "totalSleep": 480,
        "deepSleep": 120,
        "lightSleep": 360,
        "stress": 25,
        "met": 1.2,
        "mai": 65,
        "isWearing": true,
        "recordHash": "abc123def456"
      }
    ],
    "correlationId": "test-12345"
  }'
```

### Avec Python

```python
import requests
import json
from datetime import datetime

FUNCTION_URL = "https://us-central1-smart-watch-hub.cloudfunctions.net/uploadHealthMetrics"

metrics = [
    {
        "userId": "user-123",
        "deviceId": "AA:BB:CC:DD:EE:FF",
        "timestamp": int(datetime.now().timestamp() * 1000),
        "heartRate": 72,
        "bpSystolic": 120,
        "bpDiastolic": 80,
        "spO2": 98,
        "steps": 1250,
        "calories": 85,
        "distance": 950,
        "temperature": 36.8,
        "bloodGlucose": 100.5,
        "totalSleep": 480,
        "deepSleep": 120,
        "lightSleep": 360,
        "stress": 25,
        "met": 1.2,
        "mai": 65,
        "isWearing": True,
        "recordHash": "abc123def456"
    }
]

payload = {
    "metrics": metrics,
    "correlationId": "test-12345"
}

response = requests.post(FUNCTION_URL, json=payload)
print(json.dumps(response.json(), indent=2))
```

## 📊 Voir les Logs

```bash
# Logs en temps réel
gcloud functions logs read uploadHealthMetrics --gen2 --region=us-central1 --follow

# Derniers 50 logs
gcloud functions logs read uploadHealthMetrics --gen2 --region=us-central1 --limit=50

# Logs avec Cloud Logging
gcloud logging read "resource.type=cloud_function AND resource.labels.function_name=uploadHealthMetrics" \
  --limit 50 \
  --format json
```

## 🔐 Google Secret Manager

Les credentials Azure SQL sont stockés sécurisés dans Google Secret Manager:

```bash
# Voir les secrets
gcloud secrets list

# Voir un secret (attention: affiche le contenu!)
gcloud secrets versions access latest --secret=azure-sql-password

# Mettre à jour un secret
echo "new-password" | gcloud secrets versions add azure-sql-password --data-file=-
```

## 🔗 Intégration Android

Mettre à jour `AzureApiConfig.kt`:

```kotlin
object AzureApiConfig {
    const val BASE_URL = "https://us-central1-smart-watch-hub.cloudfunctions.net/"
}
```

## 📈 Monitoring

### Cloud Logging

```bash
# Dashboard Cloud Logging
gcloud logging read "resource.type=cloud_function" --limit=100
```

### Métriques

```bash
# Nombre d'invocations
gcloud monitoring time-series list \
  --filter='metric.type="cloudfunctions.googleapis.com/function/execution_count"'
```

## 🚨 Dépannage

### Erreur de connexion Azure SQL

```
Error: connection failed - check credentials in Secret Manager
```

**Solution:**
```bash
# Vérifier les secrets
gcloud secrets list | grep azure

# Tester la connexion
gcloud functions logs read uploadHealthMetrics --limit=100
```

### Timeout

```
Function execution timed out
```

**Solution:**
- Augmenter le timeout (actuellement 60s)
- Vérifier la latence réseau vers Azure SQL
- Vérifier la firewall d'Azure SQL

### Permission Denied

```
Permission denied: google.secretmanager.secrets.versions.access
```

**Solution:**
```bash
# S'assurer que le Service Account a accès aux secrets
SA_EMAIL="smartwatch-function@smart-watch-hub.iam.gserviceaccount.com"

for secret in azure-sql-server azure-sql-database azure-sql-user azure-sql-password; do
  gcloud secrets add-iam-policy-binding $secret \
    --member=serviceAccount:$SA_EMAIL \
    --role=roles/secretmanager.secretAccessor
done
```

## 📝 Notes

- La fonction utilise SQLAlchemy pour la gestion des connexions (pool)
- Les credentials Azure SQL ne sont JAMAIS dans le code (Google Secret Manager)
- Chaque invocation réutilise les connexions existantes (plus rapide)
- Les données sont validées avant insertion (sécurité)
- MERGE SQL utilisé pour l'idempotence (pas de doublons)

## 🔗 Ressources

- [Google Cloud Functions Documentation](https://cloud.google.com/functions/docs)
- [Google Secret Manager](https://cloud.google.com/secret-manager/docs)
- [SQLAlchemy with Azure SQL](https://docs.sqlalchemy.org/en/20/dialects/mssql/)
