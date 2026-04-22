#!/bin/bash

# Stop containers
docker-compose -f docker-compose.build.yml down
echo "Backend stopped"
