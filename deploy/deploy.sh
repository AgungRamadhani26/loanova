#!/bin/bash

# Deploy Script untuk Linux Cloud
# Usage: ./deploy.sh [dev|prod]

set -e

ENV=${1:-dev}

echo "🚀 Deploying Loanova Application to $ENV environment..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Create necessary directories
mkdir -p backup
mkdir -p logs

# Copy environment file if not exists
if [ ! -f .env ]; then
    echo "📝 Creating .env file from .env.example..."
    cp .env.example .env
    echo "⚠️  Please edit .env file and update the passwords!"
    read -p "Press enter to continue after editing .env file..."
fi

# Stop existing containers
echo "🛑 Stopping existing containers..."
docker-compose down

# Pull latest images
echo "📥 Pulling latest images..."
docker-compose pull

# Build application
echo "🔨 Building application..."
if [ "$ENV" == "prod" ]; then
    docker-compose -f docker-compose.prod.yml build --no-cache
else
    docker-compose build --no-cache
fi

# Start services
echo "🚀 Starting services..."
if [ "$ENV" == "prod" ]; then
    docker-compose -f docker-compose.prod.yml up -d
else
    docker-compose up -d
fi

# Wait for services to be healthy
echo "⏳ Waiting for services to be healthy..."
sleep 10

# Check status
echo "📊 Checking services status..."
if [ "$ENV" == "prod" ]; then
    docker-compose -f docker-compose.prod.yml ps
else
    docker-compose ps
fi

# Show logs
echo ""
echo "✅ Deployment completed!"
echo ""
echo "📝 View logs with: docker-compose logs -f"
echo "🔍 Check health: curl http://localhost:9091/actuator/health"
echo "🛑 Stop services: docker-compose down"
echo ""
echo "Access the application at: http://localhost:9091"
