"""
Cloud Function Google Cloud Platform pour Smart Watch Health Hub.
Synchronise les données de santé vers Azure SQL Database.

Endpoint: POST /uploadHealthMetrics
"""

import functions_framework
from flask import Request, jsonify
from sqlalchemy import insert, text
from sqlalchemy.orm import sessionmaker
from datetime import datetime
import hashlib
import logging
import json
from typing import Tuple, Dict, Any

# Imports locaux
from config import get_db_engine
from models import HealthMetric
from validators import validate_batch

# Configuration du logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Constantes
MAX_BATCH_SIZE = 500
MAX_RETRIES = 5


def create_session():
    """Créer une session SQLAlchemy."""
    try:
        engine = get_db_engine()
        SessionLocal = sessionmaker(bind=engine, expire_on_commit=False)
        return SessionLocal()
    except Exception as e:
        logger.error(f"Erreur lors de la création de la session: {str(e)}")
        raise


def insert_or_update_metrics(session, metrics: list, correlation_id: str) -> Dict[str, Any]:
    """
    Insérer ou mettre à jour les métriques via MERGE SQL.

    Args:
        session: Session SQLAlchemy
        metrics: Liste des métriques à insérer
        correlation_id: ID de corrélation pour le traçage

    Returns:
        Dictionnaire contenant les résultats
    """
    inserted_count = 0
    failed_count = 0
    errors = []

    try:
        for i, metric in enumerate(metrics):
            try:
                # Convertir timestamp (millisecondes) en datetime
                timestamp = datetime.utcfromtimestamp(metric['timestamp'] / 1000.0)

                # Créer l'objet HealthMetric
                health_metric = HealthMetric(
                    user_id=metric.get('userId'),
                    device_id=metric.get('deviceId'),
                    timestamp=timestamp,
                    heart_rate=metric.get('heartRate'),
                    bp_systolic=metric.get('bpSystolic'),
                    bp_diastolic=metric.get('bpDiastolic'),
                    spo2=metric.get('spO2'),
                    steps=metric.get('steps'),
                    calories=metric.get('calories'),
                    distance=metric.get('distance'),
                    temperature=metric.get('temperature'),
                    blood_glucose=metric.get('bloodGlucose'),
                    total_sleep=metric.get('totalSleep'),
                    deep_sleep=metric.get('deepSleep'),
                    light_sleep=metric.get('lightSleep'),
                    stress=metric.get('stress'),
                    met=metric.get('met'),
                    mai=metric.get('mai'),
                    is_wearing=metric.get('isWearing', True),
                    record_hash=metric.get('recordHash'),
                )

                # Utiliser MERGE pour upsert (insert or update)
                merge_sql = text("""
                    MERGE INTO health_data AS target
                    USING (SELECT :user_id AS UserId, :device_id AS DeviceId,
                                  :timestamp AS Timestamp, :record_hash AS RecordHash)
                           AS source
                    ON target.UserId = source.UserId
                       AND target.DeviceId = source.DeviceId
                       AND target.Timestamp = source.Timestamp
                       AND target.RecordHash = source.RecordHash
                    WHEN MATCHED THEN
                        UPDATE SET
                            HeartRate = :heart_rate,
                            BPSystolic = :bp_systolic,
                            BPDiastolic = :bp_diastolic,
                            SpO2 = :spo2,
                            Steps = :steps,
                            Calories = :calories,
                            Distance = :distance,
                            Temperature = :temperature,
                            BloodGlucose = :blood_glucose,
                            TotalSleep = :total_sleep,
                            DeepSleep = :deep_sleep,
                            LightSleep = :light_sleep,
                            Stress = :stress,
                            MET = :met,
                            MAI = :mai,
                            IsWearing = :is_wearing
                    WHEN NOT MATCHED THEN
                        INSERT (UserId, DeviceId, Timestamp, HeartRate, BPSystolic,
                                BPDiastolic, SpO2, Steps, Calories, Distance,
                                Temperature, BloodGlucose, TotalSleep, DeepSleep,
                                LightSleep, Stress, MET, MAI, IsWearing, RecordHash)
                        VALUES (:user_id, :device_id, :timestamp, :heart_rate,
                                :bp_systolic, :bp_diastolic, :spo2, :steps, :calories,
                                :distance, :temperature, :blood_glucose, :total_sleep,
                                :deep_sleep, :light_sleep, :stress, :met, :mai,
                                :is_wearing, :record_hash);
                """)

                params = {
                    'user_id': health_metric.user_id,
                    'device_id': health_metric.device_id,
                    'timestamp': health_metric.timestamp,
                    'heart_rate': health_metric.heart_rate,
                    'bp_systolic': health_metric.bp_systolic,
                    'bp_diastolic': health_metric.bp_diastolic,
                    'spo2': health_metric.spo2,
                    'steps': health_metric.steps,
                    'calories': health_metric.calories,
                    'distance': health_metric.distance,
                    'temperature': health_metric.temperature,
                    'blood_glucose': health_metric.blood_glucose,
                    'total_sleep': health_metric.total_sleep,
                    'deep_sleep': health_metric.deep_sleep,
                    'light_sleep': health_metric.light_sleep,
                    'stress': health_metric.stress,
                    'met': health_metric.met,
                    'mai': health_metric.mai,
                    'is_wearing': health_metric.is_wearing,
                    'record_hash': health_metric.record_hash,
                }

                session.execute(merge_sql, params)
                inserted_count += 1
                logger.debug(f"[{correlation_id}] Record {i} inserted/updated successfully")

            except Exception as e:
                failed_count += 1
                error_msg = f"Record {i}: {str(e)}"
                errors.append(error_msg)
                logger.error(f"[{correlation_id}] {error_msg}")

        # Commit la transaction
        session.commit()
        logger.info(f"[{correlation_id}] Batch committed: {inserted_count} inserted/updated, {failed_count} failed")

        return {
            'success': failed_count == 0,
            'inserted': inserted_count,
            'failed': failed_count,
            'errors': errors[:5]  # Retourner les 5 premiers erreurs
        }

    except Exception as e:
        session.rollback()
        logger.error(f"[{correlation_id}] Transaction failed: {str(e)}")
        raise Exception(f"Batch insert failed: {str(e)}") from e

    finally:
        session.close()


@functions_framework.http
def uploadHealthMetrics(request: Request):
    """
    Cloud Function endpoint pour uploader les métriques de santé.

    Requête:
    --------
    POST /uploadHealthMetrics
    Content-Type: application/json

    Body:
    {
        "metrics": [
            {
                "userId": "a3f2e1d9-4c5b-6a7d-8e9f-0a1b2c3d4e5f",
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
        "correlationId": "unique-correlation-id"
    }

    Réponse:
    --------
    Status 200: Succès
    {
        "success": true,
        "syncedCount": 1,
        "failedCount": 0,
        "durationMs": 234,
        "correlationId": "unique-correlation-id"
    }
    """

    start_time = datetime.now()
    correlation_id = None

    # Configuration CORS
    if request.method == 'OPTIONS':
        headers = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type, X-Correlation-ID, X-App-Version',
            'Access-Control-Max-Age': '3600'
        }
        return ('', 204, headers)

    try:
        # === 1. Parser le request ===
        try:
            data = request.get_json()
            if not data:
                logger.error("Empty request body")
                return jsonify({'error': 'Invalid request: empty body'}), 400
        except Exception as e:
            logger.error(f"JSON parsing error: {str(e)}")
            return jsonify({'error': 'Invalid JSON format'}), 400

        metrics = data.get('metrics', [])
        correlation_id = data.get('correlationId', f"auto-{int(datetime.now().timestamp() * 1000)}")

        logger.info(f"[{correlation_id}] Received {len(metrics)} metrics from {request.remote_addr}")

        # === 2. Valider le payload ===
        is_valid, validation_errors = validate_batch(metrics)

        if not is_valid:
            logger.warning(f"[{correlation_id}] Validation failed: {len(validation_errors)} errors")
            return jsonify({
                'error': 'Validation failed',
                'details': validation_errors[:10]
            }), 400

        # === 3. Connecter à Azure SQL et insérer ===
        try:
            session = create_session()
            result = insert_or_update_metrics(session, metrics, correlation_id)
        except Exception as e:
            logger.error(f"[{correlation_id}] Database error: {str(e)}")
            return jsonify({
                'error': 'Database insertion failed',
                'message': str(e)
            }), 500

        # === 4. Construire la réponse ===
        duration_ms = int((datetime.now() - start_time).total_seconds() * 1000)

        response = {
            'success': result['success'],
            'syncedCount': result['inserted'],
            'failedCount': result['failed'],
            'durationMs': duration_ms,
            'correlationId': correlation_id
        }

        if result['errors']:
            response['errors'] = result['errors']

        # === 5. Déterminer le status code ===
        if result['success']:
            status_code = 200
            logger.info(f"[{correlation_id}] ✅ Success: {result['inserted']} synced in {duration_ms}ms")
        elif result['inserted'] > 0:
            status_code = 207  # Multi-Status (partial success)
            logger.warning(f"[{correlation_id}] ⚠️  Partial success: {result['inserted']} synced, {result['failed']} failed")
        else:
            status_code = 500
            logger.error(f"[{correlation_id}] ❌ Complete failure: {result['failed']} failed")

        return jsonify(response), status_code

    except Exception as e:
        duration_ms = int((datetime.now() - start_time).total_seconds() * 1000)
        log_id = correlation_id or "unknown"
        logger.error(f"[{log_id}] Unexpected error: {str(e)}", exc_info=True)

        return jsonify({
            'error': 'Internal server error',
            'message': str(e),
            'correlationId': correlation_id
        }), 500
