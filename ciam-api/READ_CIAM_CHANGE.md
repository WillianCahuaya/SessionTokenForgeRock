# CIAM_DEPLOY

## Paso 2. Construir la aplicación Quarkus

```bash
./gradlew clean build
```

## Paso 3. Construir la imagen Docker

```bash
docker build \
  -f src/main/docker/Dockerfile.jvm \
  -t ciam-api:latest .
```

Comprobar que la imagen se haya creado:

```bash
docker images ciam-api
```

## Paso 4. Cargar la imagen en Kind

Cargar la imagen en el clúster local `willca-local`:

```bash
kind load docker-image ciam-api:latest \
  --name willca-local
```

## Paso 5. Desplegar la aplicación en Kubernetes

Reiniciar el Deployment existente:

```bash
kubectl rollout restart deployment/ciam-api -n forgerock
```

Aplicar la configuración de Kubernetes:

```bash
kubectl apply -f k8s/ciam-api.yaml
```

## Paso 6. Verificar el despliegue

Comprobar los Pods:

```bash
kubectl get pods -n forgerock -l app=ciam-api
```

Comprobar el Deployment:

```bash
kubectl get deployment ciam-api -n forgerock
```

Comprobar el Service:

```bash
kubectl get service ciam-api -n forgerock
```

Verificar el estado del despliegue:

```bash
kubectl rollout status deployment/ciam-api -n forgerock
```

Consultar los logs de la aplicación:

```bash
kubectl logs deployment/ciam-api -n forgerock
```
