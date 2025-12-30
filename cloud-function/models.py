"""Modèles SQLAlchemy pour la table health_data."""

from sqlalchemy import Column, String, Integer, Float, DateTime, BigInteger, Boolean
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import func
from datetime import datetime

Base = declarative_base()


class HealthMetric(Base):
    """Modèle de données pour les métriques de santé."""

    __tablename__ = "health_data"

    # Clé primaire
    id = Column(BigInteger, primary_key=True, autoincrement=True)

    # Identifiants (anonymes, GDPR-compliant)
    user_id = Column(String(100), nullable=False, name="UserId")
    device_id = Column(String(50), nullable=False, name="DeviceId")

    # Timestamp
    timestamp = Column(DateTime(timezone=False), nullable=False, name="Timestamp")

    # Métriques cardiaques
    heart_rate = Column(Integer, nullable=True, name="HeartRate")
    bp_systolic = Column(Integer, nullable=True, name="BPSystolic")
    bp_diastolic = Column(Integer, nullable=True, name="BPDiastolic")
    spo2 = Column(Integer, nullable=True, name="SpO2")

    # Activité physique
    steps = Column(Integer, nullable=True, name="Steps")
    calories = Column(Integer, nullable=True, name="Calories")
    distance = Column(Integer, nullable=True, name="Distance")

    # Santé métabolique
    temperature = Column(Float, nullable=True, name="Temperature")
    blood_glucose = Column(Float, nullable=True, name="BloodGlucose")

    # Sommeil
    total_sleep = Column(Integer, nullable=True, name="TotalSleep")
    deep_sleep = Column(Integer, nullable=True, name="DeepSleep")
    light_sleep = Column(Integer, nullable=True, name="LightSleep")

    # Stress et métabolisme
    stress = Column(Integer, nullable=True, name="Stress")
    met = Column(Float, nullable=True, name="MET")
    mai = Column(Integer, nullable=True, name="MAI")

    # État du dispositif
    is_wearing = Column(Boolean, nullable=False, default=True, name="IsWearing")

    # Audit et déduplication
    record_hash = Column(String(32), nullable=False, name="RecordHash")
    created_at = Column(DateTime(timezone=False), server_default=func.getutcdate(), name="CreatedAt")

    def __repr__(self):
        return (
            f"<HealthMetric(id={self.id}, user_id={self.user_id}, "
            f"device_id={self.device_id}, timestamp={self.timestamp}, "
            f"heart_rate={self.heart_rate})>"
        )

    def to_dict(self):
        """Convertir le modèle en dictionnaire."""
        return {
            "id": self.id,
            "userId": self.user_id,
            "deviceId": self.device_id,
            "timestamp": int(self.timestamp.timestamp() * 1000),
            "heartRate": self.heart_rate,
            "bpSystolic": self.bp_systolic,
            "bpDiastolic": self.bp_diastolic,
            "spO2": self.spo2,
            "steps": self.steps,
            "calories": self.calories,
            "distance": self.distance,
            "temperature": self.temperature,
            "bloodGlucose": self.blood_glucose,
            "totalSleep": self.total_sleep,
            "deepSleep": self.deep_sleep,
            "lightSleep": self.light_sleep,
            "stress": self.stress,
            "met": self.met,
            "mai": self.mai,
            "isWearing": self.is_wearing,
            "recordHash": self.record_hash,
            "createdAt": self.created_at.isoformat() if self.created_at else None,
        }
