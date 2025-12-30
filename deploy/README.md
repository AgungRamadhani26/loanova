# Loanova Deployment Guide

Panduan deployment aplikasi Loanova ke Linux Cloud menggunakan Docker.

## Prerequisites

- Docker & Docker Compose installed
- Port 9091, 1433, 6379 tersedia
- Minimal 2GB RAM
- Minimal 10GB disk space

## Quick Start

### 1. Clone Repository

```bash
git clone https://github.com/AgungRamadhani26/loanova.git
cd loanova/deploy
```

### 2. Konfigurasi Environment (Optional)

```bash
cp .env.example .env
# Edit .env sesuai kebutuhan
nano .env
```

### 3. Build & Run

```bash
# Build dan jalankan semua services
docker-compose up -d

# Atau build dari scratch
docker-compose up -d --build
```

### 4. Check Status

```bash
# Cek status containers
docker-compose ps

# Cek logs
docker-compose logs -f app
docker-compose logs -f sqlserver
docker-compose logs -f redis
```

## Services

| Service         | Port | Description             |
| --------------- | ---- | ----------------------- |
| Spring Boot App | 9091 | Main application        |
| SQL Server      | 1433 | Database                |
| Redis           | 6379 | Cache & Token Blacklist |

## Health Check

Cek health aplikasi:

```bash
curl http://localhost:9091/actuator/health
```

## Test API

### Login

```bash
curl -X POST http://localhost:9091/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "SUMUT01",
    "password": "Admin@123"
  }'
```

### Get Users (Protected)

```bash
curl -X GET http://localhost:9091/api/users \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Management Commands

### Stop Services

```bash
docker-compose down
```

### Stop & Remove Volumes (HATI-HATI: Data akan hilang)

```bash
docker-compose down -v
```

### Restart Application Only

```bash
docker-compose restart app
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f app
docker-compose logs -f sqlserver
docker-compose logs -f redis
```

### Scale Application (Load Balancing)

```bash
docker-compose up -d --scale app=3
```

## Troubleshooting

### Application tidak start

```bash
# Cek logs
docker-compose logs app

# Cek health database
docker exec loanova_sqlserver /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P YourStrong@Passw0rd -Q "SELECT 1"

# Cek health redis
docker exec loanova_redis redis-cli -a redis@123 ping
```

### Connection Error ke Database

1. Pastikan SQL Server sudah ready (tunggu ~30 detik setelah start)
2. Cek password di docker-compose.yml sesuai dengan .env
3. Cek network connectivity

### Port Already in Use

```bash
# Cek port yang digunakan
netstat -ano | grep :9091
netstat -ano | grep :1433
netstat -ano | grep :6379

# Ubah port di docker-compose.yml
```

## Production Deployment

### 1. Update Configuration

Edit `docker-compose.yml`:

- Ganti password default
- Set `SPRING_JPA_SHOW_SQL=false`
- Set `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (setelah initial setup)
- Tambah SSL/TLS configuration
- Setup reverse proxy (nginx/traefik)

### 2. Security Hardening

```bash
# Jangan expose database port ke public
# Hapus atau comment port mapping untuk sqlserver & redis di docker-compose.yml
```

### 3. Backup Database

```bash
# Backup
docker exec loanova_sqlserver /opt/mssql-tools/bin/sqlcmd \
  -S localhost -U sa -P YourStrong@Passw0rd \
  -Q "BACKUP DATABASE loanova_db TO DISK='/var/opt/mssql/backup/loanova_db.bak'"

# Copy backup file keluar dari container
docker cp loanova_sqlserver:/var/opt/mssql/backup/loanova_db.bak ./backup/
```

### 4. Monitoring

```bash
# Install monitoring tools
# - Prometheus
# - Grafana
# - ELK Stack
```

## Cloud Deployment

### AWS EC2

1. Launch EC2 instance (Ubuntu 22.04)
2. Install Docker & Docker Compose
3. Clone repository
4. Setup security groups (allow ports 9091, 22)
5. Run `docker-compose up -d`

### Google Cloud Platform

1. Create Compute Engine instance
2. Install Docker & Docker Compose
3. Configure firewall rules
4. Deploy dengan docker-compose

### Azure VM

1. Create Ubuntu VM
2. Install Docker & Docker Compose
3. Configure Network Security Group
4. Deploy dengan docker-compose

## Environment Variables

| Variable                     | Default             | Description         |
| ---------------------------- | ------------------- | ------------------- |
| DB_PASSWORD                  | YourStrong@Passw0rd | SQL Server password |
| REDIS_PASSWORD               | redis@123           | Redis password      |
| JWT_SECRET                   | loanovaSecret...    | JWT secret key      |
| JWT_ACCESS_TOKEN_EXPIRATION  | 900000              | 15 minutes in ms    |
| JWT_REFRESH_TOKEN_EXPIRATION | 604800000           | 7 days in ms        |
| SERVER_PORT                  | 9091                | Application port    |

## Support

Untuk masalah atau pertanyaan:

- GitHub Issues: https://github.com/AgungRamadhani26/loanova/issues
- Email: support@loanova.com
