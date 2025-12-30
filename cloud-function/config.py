"""Configuration SQLAlchemy pour Azure SQL Database."""

import os
from google.cloud import secret_manager
from sqlalchemy import create_engine, event
from sqlalchemy.pool import QueuePool
import logging

logger = logging.getLogger(__name__)


def get_secret(secret_id: str, project_id: str = "smart-watch-hub") -> str:
    """Récupérer un secret de Google Secret Manager."""
    try:
        client = secret_manager.SecretManagerServiceClient()
        name = f"projects/{project_id}/secrets/{secret_id}/versions/latest"
        response = client.access_secret_version(request={"name": name})
        return response.payload.data.decode("UTF-8")
    except Exception as e:
        logger.error(f"Erreur lors de la récupération du secret {secret_id}: {str(e)}")
        raise


def get_database_url() -> str:
    """Construire la URL de connexion Azure SQL Database."""

    # Récupérer les credentials depuis Google Secret Manager
    try:
        server = get_secret("azure-sql-server")
        database = get_secret("azure-sql-database")
        user = get_secret("azure-sql-user")
        password = get_secret("azure-sql-password")
    except Exception as e:
        logger.error(f"Impossible de charger les secrets: {str(e)}")
        raise

    # Format de connexion ODBC pour Azure SQL via SQLAlchemy
    # SQLAlchemy + pyodbc format
    connection_string = (
        f"mssql+pyodbc://{user}:{password}@{server}:1433/{database}?"
        f"driver=ODBC+Driver+17+for+SQL+Server&"
        f"Encrypt=yes&"
        f"TrustServerCertificate=no&"
        f"Connection+Timeout=30"
    )

    return connection_string


def create_db_engine():
    """Créer et configurer le moteur SQLAlchemy avec pool de connexions."""

    database_url = get_database_url()

    # Créer le moteur avec pool de connexions
    engine = create_engine(
        database_url,
        poolclass=QueuePool,
        pool_size=5,              # Nombre de connexions toujours ouvertes
        max_overflow=10,          # Connexions supplémentaires si besoin
        pool_recycle=3600,        # Recycler les connexions après 1h
        pool_pre_ping=True,       # Tester la connexion avant utilisation
        echo=False,               # Pas de logs SQL (pour perfs)
        connect_args={
            "timeout": 30,
            "autocommit": False,
        }
    )

    # Event listener pour les erreurs de connexion
    @event.listens_for(engine, "connect")
    def receive_connect(dbapi_conn, connection_record):
        """Configurer la connexion."""
        dbapi_conn.setdecoding(True)

    @event.listens_for(engine, "pool_connect")
    def receive_pool_connect(dbapi_conn, connection_record):
        logger.debug("Pool connection established")

    return engine


# Instance globale du moteur (réutilisée entre les appels)
_db_engine = None


def get_db_engine():
    """Obtenir ou créer l'instance du moteur de base de données."""
    global _db_engine

    if _db_engine is None:
        try:
            _db_engine = create_db_engine()
            logger.info("Database engine created successfully")
        except Exception as e:
            logger.error(f"Erreur lors de la création du moteur de base de données: {str(e)}")
            raise

    return _db_engine


def test_db_connection():
    """Tester la connexion à la base de données."""
    try:
        engine = get_db_engine()
        with engine.connect() as conn:
            result = conn.execute("SELECT 1")
            logger.info("✅ Connexion à Azure SQL Database réussie")
            return True
    except Exception as e:
        logger.error(f"❌ Erreur de connexion: {str(e)}")
        return False
