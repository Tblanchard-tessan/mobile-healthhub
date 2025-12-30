# Cloud Function pour Smart Watch Hub avec ODBC Driver 17

FROM python:3.11-slim

# Installer les dépendances système pour ODBC
RUN apt-get update && apt-get install -y \
    curl \
    gnupg \
    unixodbc \
    unixodbc-dev \
    && rm -rf /var/lib/apt/lists/*

# Installer Microsoft ODBC Driver 17 pour SQL Server
RUN curl https://packages.microsoft.com/keys/microsoft.asc | apt-key add - && \
    curl https://packages.microsoft.com/config/debian/11/prod.list > /etc/apt/sources.list.d/mssql-release.list && \
    apt-get update && \
    ACCEPT_EULA=Y apt-get install -y msodbcsql17 && \
    rm -rf /var/lib/apt/lists/*

# Définir le répertoire de travail
WORKDIR /workspace

# Copier les fichiers source
COPY requirements.txt requirements.txt
COPY main.py main.py
COPY config.py config.py
COPY models.py models.py
COPY validators.py validators.py

# Installer les dépendances Python
RUN pip install --no-cache-dir -r requirements.txt

# Définir le port d'écoute (required par Cloud Run)
ENV PORT=8080

# Command pour lancer la Cloud Function
CMD exec functions-framework --target=uploadHealthMetrics --debug --port=$PORT
