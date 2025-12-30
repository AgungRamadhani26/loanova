#!/bin/bash

# Backup script for SQL Server database

set -e

BACKUP_DIR="./backup"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="loanova_db_${TIMESTAMP}.bak"

echo "🔄 Creating database backup..."

# Create backup directory if not exists
mkdir -p $BACKUP_DIR

# Backup database inside container
docker exec loanova_sqlserver /opt/mssql-tools/bin/sqlcmd \
  -S localhost -U sa -P YourStrong@Passw0rd \
  -Q "BACKUP DATABASE loanova_db TO DISK='/var/opt/mssql/backup/${BACKUP_FILE}' WITH FORMAT, INIT, NAME='Full Backup'"

# Copy backup file to host
docker cp loanova_sqlserver:/var/opt/mssql/backup/${BACKUP_FILE} ${BACKUP_DIR}/${BACKUP_FILE}

echo "✅ Backup completed: ${BACKUP_DIR}/${BACKUP_FILE}"

# Remove old backups (keep last 7 days)
find $BACKUP_DIR -name "loanova_db_*.bak" -mtime +7 -delete

echo "🧹 Old backups cleaned up"
