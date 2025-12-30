#!/bin/bash

# Monitoring script untuk check health services

echo "🔍 Checking Loanova Services Health..."
echo ""

# Check Docker daemon
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker daemon is not running"
    exit 1
fi

# Check containers
echo "📦 Container Status:"
docker ps --filter name=loanova --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

# Check application health
echo "🏥 Application Health Check:"
if curl -s http://localhost:9091/actuator/health | grep -q "UP"; then
    echo "✅ Application: HEALTHY"
else
    echo "❌ Application: UNHEALTHY"
fi
echo ""

# Check SQL Server
echo "💾 SQL Server Health Check:"
if docker exec loanova_sqlserver /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P YourStrong@Passw0rd -Q "SELECT 1" > /dev/null 2>&1; then
    echo "✅ SQL Server: HEALTHY"
else
    echo "❌ SQL Server: UNHEALTHY"
fi
echo ""

# Check Redis
echo "🔴 Redis Health Check:"
if docker exec loanova_redis redis-cli -a redis@123 ping > /dev/null 2>&1; then
    echo "✅ Redis: HEALTHY"
else
    echo "❌ Redis: UNHEALTHY"
fi
echo ""

# Check disk usage
echo "💿 Disk Usage:"
docker system df
echo ""

# Check logs for errors
echo "📋 Recent Errors (last 10):"
docker-compose logs --tail=100 | grep -i error | tail -10 || echo "No errors found"
