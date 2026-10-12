Paso 2. Reiniciar AM en Kubernetes
Desde la raíz del proyecto forgeops, ejecuta:

kubectl rollout restart deployment/am -n forgerock

Después, espera a que termine el reinicio:

kubectl rollout status deployment/am -n forgerock --timeout=180s

El pod de AM está intentando detenerse, pero no termina. Vamos a revisar qué está pasando antes de hacer otro cambio.
Ejecuta este comando:

kubectl get pods -n forgerock -o wide

AMSTER MANUAL
CREATE
kubectl run amster-manual -n forgerock --image=us-docker.pkg.dev/forgeops-public/images/amster:7.5.2 --restart=Never --command -- sleep 3600
DELETE
kubectl delete pod amster-manual -n forgerock

AM
Autenticación, OAuth 2.0 y gestión de sesiones

IG
Gateway de identidad; recibe y procesa solicitudes HTTP

DS Identity
Almacena datos de identidad de usuarios

DS CTS
Almacena información de sesiones y tokens compartidos

CIAM API
API de identidad y acceso de tu entorno

Amster
Herramienta para importar configuración a AM
