#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Docker Image Optimization Experiment ===${NC}"

# 빌드 시간 측정을 위한 함수
measure_build() {
    local dockerfile=$1
    local tag=$2
    echo -e "\n${BLUE}Building with $dockerfile...${NC}"
    
    start_time=$(date +%s)
    docker build -t "$tag" -f "$dockerfile" .
    end_time=$(date +%s)
    
    elapsed=$((end_time - start_time))
    echo "$elapsed"
}

# 1. Worst Case 빌드
worst_time=$(measure_build "auth-service/Dockerfile.worst" "auth-service-worst")

# 2. Best Case 빌드
best_time=$(measure_build "auth-service/Dockerfile.best" "auth-service-best")

# 이미지 크기 확인
worst_size=$(docker images --format "{{.Size}}" auth-service-worst | head -n 1)
best_size=$(docker images --format "{{.Size}}" auth-service-best | head -n 1)

echo -e "\n${GREEN}=== Comparison Results ===${NC}"
printf "%-20s | %-15s | %-15s\n" "Metric" "Worst Case" "Best Case"
printf "%-20s | %-15s | %-15s\n" "--------------------" "---------------" "---------------"
printf "%-20s | %-15s | %-15s\n" "Build Time" "${worst_time}s" "${best_time}s"
printf "%-20s | %-15s | %-15s\n" "Image Size" "$worst_size" "$best_size"

echo -e "\n${BLUE}Hint: Check layers with 'docker history auth-service-best'${NC}"
