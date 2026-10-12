Requesting Token A from ForgeRock AM...
{"access_token":"1NmH7J9KaIkAEbXlajf5O9MvCV4","refresh_token":"VZJzpW-nxWaxzz-HiO9GNQ8TSCg","scope":"profile","token_type":"Bearer","expires_in":3599}%(base) kathy@MacBook-Air-de-Katherine SessionTokenForgeRock %

curl -i \
 http://localhost:8081/rs-tokenintrospect \
 -H "Authorization: Bearer VmGxxYu0hc7Y4qYxQvEwvWbswy8"

kubectl port-forward svc/ig 8081:80 -n forgerock

                "ssoToken": "${contexts.oauth2.accessToken.info.sessionCookie}"

python3 -m json.tool \
docker/ig/config-profiles/cdk/config/config.json \

> /dev/null && echo "config.json OK"

echo 'export AMADMIN_PASSWORD="9HKHQeCZNBL8GEx7qwkmrCpq"' >> ~/.zshrc

export AMADMIN_PASSWORD='9HKHQeCZNBL8GEx7qwkmrCpq'
source ~/.zshrc

export ADMIN*TOKEN='uej8r7euP45eUUJKUXJvsCQJBPw.\_AAJTSQACMDIAAlNLABxLZ20rT2N3OXJTZnBySVRlMkxlMFZHdERTOXc9AAR0eXBlAANDVFMAAlMxAAIwMQ..*'
source ~/.zshrc

python3 -m json.tool \
docker/ig/config-profiles/cdk/config/routes-service/16-apis-integration-session.json > /dev/null && \
echo "16-apis-integration-session.json OK"

./bin/forgeops build ig --config-profile cdk --push-to none

kind load docker-image ig:latest --name willca-local

kubectl delete pod -l app=ig -n forgerock

kubectl get pods -n forgerock -l app=ig -w

kubectl logs deployment/ig -n forgerock --tail=200

kubectl logs -f deployment/ig -n forgerock --timestamps | grep --line-buffered -E 'Redis Script Filter|register|ciam|INFO|WARN|ERROR'

kubectl logs -f deployment/ig -n forgerock --since=5m --timestamps

kubectl logs -f deployment/ciam-api -n forgerock --since=5m --timestamps

CIAM_DEPLOY

Paso 2. Construir la aplicación Quarkus
./gradlew clean build

Paso 3. Construir la imagen Docker
docker build \
 -f src/main/docker/Dockerfile.jvm \
 -t ciam-api:latest .

Comprueba que se haya creado la imagen:
docker images ciam-api

Paso 4. Cargar la imagen en Kind
kind load docker-image ciam-api:latest \
 --name willca-local

Paso 5. Crear el Deployment y el Service
kubectl rollout restart deployment/ciam-api -n forgerock
kubectl apply -f k8s/ciam-api.yaml

Paso 6. Verificar que esté desplegado
kubectl get pods -n forgerock -l app=ciam-api
kubectl get deployment ciam-api -n forgerock
kubectl get service ciam-api -n forgerock
