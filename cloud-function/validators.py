"""Validation des données de santé."""

import logging

logger = logging.getLogger(__name__)


class ValidationError(Exception):
    """Exception levée lors d'une erreur de validation."""
    pass


def validate_metric(metric: dict, index: int = 0) -> list:
    """
    Valider une métrique de santé.

    Args:
        metric: Dictionnaire contenant les données de la métrique
        index: Index de la métrique dans le batch (pour les messages d'erreur)

    Returns:
        Liste des erreurs trouvées (vide si valide)
    """
    errors = []

    # === Champs requis ===
    if not metric.get("userId"):
        errors.append(f"Record {index}: userId is required")

    if not metric.get("deviceId"):
        errors.append(f"Record {index}: deviceId is required")

    timestamp = metric.get("timestamp")
    if timestamp is None or timestamp <= 0:
        errors.append(f"Record {index}: invalid timestamp (must be positive)")

    record_hash = metric.get("recordHash")
    if not record_hash:
        errors.append(f"Record {index}: recordHash is required for deduplication")

    # === Validations physiologiques ===
    # Fréquence cardiaque
    if "heartRate" in metric and metric["heartRate"] is not None:
        if not isinstance(metric["heartRate"], (int, float)):
            errors.append(f"Record {index}: heartRate must be numeric")
        elif not (30 <= metric["heartRate"] <= 220):
            errors.append(f"Record {index}: heartRate out of range (30-220 bpm)")

    # Tension artérielle systolique
    if "bpSystolic" in metric and metric["bpSystolic"] is not None:
        if not isinstance(metric["bpSystolic"], (int, float)):
            errors.append(f"Record {index}: bpSystolic must be numeric")
        elif not (60 <= metric["bpSystolic"] <= 280):
            errors.append(f"Record {index}: bpSystolic out of range (60-280 mmHg)")

    # Tension artérielle diastolique
    if "bpDiastolic" in metric and metric["bpDiastolic"] is not None:
        if not isinstance(metric["bpDiastolic"], (int, float)):
            errors.append(f"Record {index}: bpDiastolic must be numeric")
        elif not (30 <= metric["bpDiastolic"] <= 150):
            errors.append(f"Record {index}: bpDiastolic out of range (30-150 mmHg)")

    # SpO2
    if "spO2" in metric and metric["spO2"] is not None:
        if not isinstance(metric["spO2"], (int, float)):
            errors.append(f"Record {index}: spO2 must be numeric")
        elif not (70 <= metric["spO2"] <= 100):
            errors.append(f"Record {index}: spO2 out of range (70-100%)")

    # Température
    if "temperature" in metric and metric["temperature"] is not None:
        if not isinstance(metric["temperature"], (int, float)):
            errors.append(f"Record {index}: temperature must be numeric")
        elif not (35.0 <= metric["temperature"] <= 41.0):
            errors.append(f"Record {index}: temperature out of range (35.0-41.0°C)")

    # Glucose sanguin
    if "bloodGlucose" in metric and metric["bloodGlucose"] is not None:
        if not isinstance(metric["bloodGlucose"], (int, float)):
            errors.append(f"Record {index}: bloodGlucose must be numeric")
        elif not (50 <= metric["bloodGlucose"] <= 500):
            errors.append(f"Record {index}: bloodGlucose out of range (50-500)")

    # === Activité et sommeil ===
    # Pas
    if "steps" in metric and metric["steps"] is not None:
        if not isinstance(metric["steps"], int):
            errors.append(f"Record {index}: steps must be integer")
        elif metric["steps"] < 0:
            errors.append(f"Record {index}: steps cannot be negative")

    # Calories
    if "calories" in metric and metric["calories"] is not None:
        if not isinstance(metric["calories"], (int, float)):
            errors.append(f"Record {index}: calories must be numeric")
        elif metric["calories"] < 0:
            errors.append(f"Record {index}: calories cannot be negative")

    # Distance
    if "distance" in metric and metric["distance"] is not None:
        if not isinstance(metric["distance"], (int, float)):
            errors.append(f"Record {index}: distance must be numeric")
        elif metric["distance"] < 0:
            errors.append(f"Record {index}: distance cannot be negative")

    # Sommeil total
    if "totalSleep" in metric and metric["totalSleep"] is not None:
        if not isinstance(metric["totalSleep"], int):
            errors.append(f"Record {index}: totalSleep must be integer")
        elif not (0 <= metric["totalSleep"] <= 1440):  # Max 24h = 1440 min
            errors.append(f"Record {index}: totalSleep out of range (0-1440 minutes)")

    # Sommeil profond
    if "deepSleep" in metric and metric["deepSleep"] is not None:
        if not isinstance(metric["deepSleep"], int):
            errors.append(f"Record {index}: deepSleep must be integer")
        elif not (0 <= metric["deepSleep"] <= 1440):
            errors.append(f"Record {index}: deepSleep out of range (0-1440 minutes)")

    # Sommeil léger
    if "lightSleep" in metric and metric["lightSleep"] is not None:
        if not isinstance(metric["lightSleep"], int):
            errors.append(f"Record {index}: lightSleep must be integer")
        elif not (0 <= metric["lightSleep"] <= 1440):
            errors.append(f"Record {index}: lightSleep out of range (0-1440 minutes)")

    # Stress
    if "stress" in metric and metric["stress"] is not None:
        if not isinstance(metric["stress"], int):
            errors.append(f"Record {index}: stress must be integer")
        elif not (0 <= metric["stress"] <= 100):
            errors.append(f"Record {index}: stress out of range (0-100)")

    # MET
    if "met" in metric and metric["met"] is not None:
        if not isinstance(metric["met"], (int, float)):
            errors.append(f"Record {index}: met must be numeric")
        elif not (0.0 <= metric["met"] <= 20.0):
            errors.append(f"Record {index}: met out of range (0.0-20.0)")

    # MAI
    if "mai" in metric and metric["mai"] is not None:
        if not isinstance(metric["mai"], int):
            errors.append(f"Record {index}: mai must be integer")
        elif not (0 <= metric["mai"] <= 100):
            errors.append(f"Record {index}: mai out of range (0-100)")

    # isWearing
    if "isWearing" in metric:
        if not isinstance(metric["isWearing"], bool):
            errors.append(f"Record {index}: isWearing must be boolean")

    return errors


def validate_batch(metrics: list) -> tuple:
    """
    Valider un batch complet de métriques.

    Args:
        metrics: Liste des métriques à valider

    Returns:
        (bool valide, list erreurs)
    """
    if not isinstance(metrics, list):
        return False, ["metrics must be an array"]

    if len(metrics) == 0:
        return False, ["metrics array cannot be empty"]

    if len(metrics) > 500:
        return False, [f"batch size {len(metrics)} exceeds maximum of 500"]

    all_errors = []
    for i, metric in enumerate(metrics):
        if not isinstance(metric, dict):
            all_errors.append(f"Record {i}: must be a JSON object")
            continue

        errors = validate_metric(metric, i)
        all_errors.extend(errors)

    is_valid = len(all_errors) == 0
    return is_valid, all_errors
