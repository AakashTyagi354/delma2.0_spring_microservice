#!/bin/bash
SERVICE=$1

if [ -z "$SERVICE" ]; then
  echo "Usage: ./rebuild.sh <service-name>"
  exit 1
fi

echo "Building JAR for $SERVICE..."
cd $SERVICE && mvn clean package -DskipTests && cd ..

echo "Building Docker image..."
docker build -t aakash354/delma-$SERVICE:latest ./$SERVICE

echo "Restarting container..."
docker compose up -d $SERVICE

echo "Logs:"
docker compose logs -f $SERVICE
