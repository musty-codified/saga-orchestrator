#!/bin/bash

set -e

echo "Starting Clean Build and Deploy..."

echo "Moving to musty-create-order directory..."
cd musty-create-order
echo "Packaging JAR..."
mvn clean package -DskipTests
echo "Back to root directory..."
cd ..

echo "Moving to musty-process-payment directory..."
cd musty-process-payment
echo "Packaging JAR..."
mvn clean package -DskipTests
echo "Back to root directory..."
cd ..

echo "Moving to musty-notification-service directory..."
cd musty-notification-service
echo "Packaging JAR..."
mvn clean package -DskipTests
echo "Back to root directory..."
cd ..

docker compose build --no-cache

echo "Starting containers..."
docker compose up -d


