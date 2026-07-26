minikube docker-env | Invoke-Expression

docker build --build-arg VITE_API_URL=http://sayless.local -t frontend:latest ".\frontend"
docker build -t auth-service:latest ".\auth-service"
docker build -t task-service:latest ".\task-service"
docker build -t friend-service:latest ".\friend-service"
docker build -t notification-service:latest ".\notification-service"
docker build -t gateway-service:latest ".\gateway-service"

kubectl apply -f ".\k8s\"

kubectl rollout restart deployment/frontend deployment/gateway-service deployment/auth-service deployment/task-service deployment/friend-service deployment/notification-service

kubectl rollout status deployment/frontend
kubectl rollout status deployment/gateway-service
kubectl rollout status deployment/auth-service
kubectl rollout status deployment/task-service
kubectl rollout status deployment/friend-service
kubectl rollout status deployment/notification-service

Write-Host "All services rebuilt and redeployed." -ForegroundColor Green
