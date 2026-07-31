#!/bin/bash
set -e

# ============================================================
# Docker Deployment Command for Linux Server
# Path: /DATA/deployments/backend/service_internal
# ============================================================

printf "FROM account_online:1.0\nCOPY internal-service.jar /app/internal-service.jar\nENTRYPOINT [\"java\",\"-XX:+UseContainerSupport\",\"-XX:InitialRAMPercentage=25.0\",\"-XX:MaxRAMPercentage=75.0\",\"-XX:+UseG1GC\",\"-Duser.timezone=Asia/Phnom_Penh\",\"-jar\",\"/app/internal-service.jar\"]" | sudo docker build -t internal_service:1.1 -f - . && { sudo docker ps -aq -f name=^/internal_service_container$ | grep -q . && sudo docker stop internal_service_container && sudo docker rm internal_service_container; true; } && sudo docker run -d --name internal_service_container --restart unless-stopped -p 5050:5050 -v /Oracle_BI/deployment/internal_service/logs:/app/logs internal_service:1.1 && echo "✅ internal_service:1.1 running on port 5050"
