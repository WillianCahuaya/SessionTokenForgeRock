# Guía completa: levantar ForgeRock localmente con ForgeOps + Docker + KIND

## 1. Objetivo

El objetivo es levantar un entorno ForgeRock local para poder estudiar y probar este flujo:

```text
Mobile Simulator
      │
      ▼
     AM
      │
      │ Token A
      ▼
     IG
      │
      │ Token B
      ▼
   Backend
```

En la primera etapa solamente queremos conseguir:

```text
Mobile Simulator
      ↓
AM
      ↓
Token A
      ↓
TTL ≈ 1 hora
```

La segunda etapa será:

```text
Token A
   ↓
IG
   ↓
Token B
   ↓
TTL = 1 minuto
```

---

# 2. Arquitectura del laboratorio

Nuestro entorno local tiene varias capas:

```text
Mac
│
├── Docker Desktop
│
├── KIND
│    └── Kubernetes cluster
│
└── Git repository
     └── ForgeOps
          │
          └── Kubernetes
               │
               └── namespace: forgerock
                    │
                    ├── DS IDRepo
                    ├── DS CTS
                    ├── AM
                    ├── AMster
                    └── IG
```

### ¿Qué es cada componente?

**Docker**

Es el runtime de contenedores que utilizamos en el Mac.

**KIND**

Kubernetes IN Docker. Crea nodos Kubernetes como contenedores Docker. KIND genera el contexto de Kubernetes que luego utilizamos con `kubectl`.

**ForgeOps**

Es el repositorio que contiene Dockerfiles, configuración, scripts, Kustomize y demás artefactos necesarios para desplegar y personalizar la plataforma Ping Identity sobre Kubernetes.

**AM**

Access Management. Gestiona autenticación, OAuth2/OIDC, sesiones y emisión de tokens.

**DS**

Directory Services. Es la capa de directorio utilizada por la plataforma.

**AMster**

Importa configuraciones de AM desde los archivos de ForgeOps.

**IG**

Identity Gateway. Es un servidor/gateway que recibe peticiones HTTP y las procesa mediante routes, filters y handlers.

---

# 3. Requisitos del equipo

Para este laboratorio utilizamos:

```text
Mac
Docker Desktop
Git
kubectl
KIND
ForgeOps
```

ForgeOps documenta como referencia para entornos locales una VM/engine Docker con al menos 4 CPUs, 10 GB de RAM y 60 GB de disco. Aunque la documentación oficial utiliza Minikube para el ejemplo local, esos recursos son una buena referencia para un laboratorio con KIND también.

Esto es especialmente importante porque durante nuestro laboratorio AMster llegó a quedarse:

```text
Pending
```

debido a:

```text
Insufficient memory
```

---

# 4. Docker Desktop

En macOS necesitamos tener Docker ejecutándose.

### Verificación

```bash
docker version
docker info
```

### Resultado esperado

Docker debe responder correctamente y mostrar información del Engine.

Por ejemplo:

```text
Client:
 Version: ...

Server:
 Engine:
   Version: ...
```

Si Docker no está levantado, KIND y las imágenes de ForgeRock no podrán funcionar.

---

# 5. Instalar Git

Git será utilizado para descargar ForgeOps.

### Verificación

```bash
git --version
```

Resultado esperado:

```text
git version ...
```

---

# 6. Instalar kubectl

`kubectl` es el cliente que nos permite comunicarnos con Kubernetes.

### Verificación

```bash
kubectl version --client
```

Debe devolver la versión del cliente.

ForgeOps 7.5 documenta `kubectl` como uno de los requisitos del entorno.

---

# 7. Instalar KIND

KIND crea el clúster Kubernetes local utilizando Docker.

En macOS puede instalarse con Homebrew:

```bash
brew install kind
```

### Verificación

```bash
kind version
```

Guardar este valor es importante para documentar exactamente qué versión se utilizó.

KIND oficialmente recomienda verificar la versión instalada y permite crear el cluster con:

```bash
kind create cluster
```

o con un nombre específico mediante:

```bash
kind create cluster --name <nombre>
```

---

# 8. Crear el cluster KIND

Nuestro laboratorio utiliza el nombre:

```text
willca-local
```

Crear el cluster:

```bash
kind create cluster --name willca-local
```

KIND crea el nodo Kubernetes como un contenedor Docker.

Conceptualmente:

```text
Docker
│
└── willca-local-control-plane
        │
        └── Kubernetes
```

### Verificación KIND

```bash
kind get clusters
```

Resultado esperado:

```text
willca-local
```

### Verificación Kubernetes

```bash
kubectl cluster-info
```

### Verificación del nodo

```bash
kubectl get nodes
```

Resultado esperado:

```text
NAME                        STATUS   ROLES
willca-local-control-plane  Ready    control-plane
```

El estado importante es:

```text
Ready
```

---

# 9. Entender el contexto Kubernetes

KIND crea y configura automáticamente un contexto para `kubectl`.

Comprobarlo:

```bash
kubectl config get-contexts
```

Debemos poder ver un contexto parecido a:

```text
kind-willca-local
```

Comprobar el contexto actual:

```bash
kubectl config current-context
```

Resultado esperado:

```text
kind-willca-local
```

Esto significa:

```text
kubectl
   ↓
KIND willca-local
   ↓
Kubernetes
```

---

# 10. Descargar ForgeOps desde GitHub

ForgeOps es el repositorio que utilizamos para obtener las configuraciones y scripts de la plataforma.

La documentación oficial de ForgeOps 7.5 indica clonar el repositorio público `forgeops` y trabajar sobre una rama release de la familia 7.5. La documentación actual de 7.5 utiliza `release/7.5-20251119` como referencia.

Clonar:

```bash
git clone https://github.com/ForgeRock/forgeops.git
```

Entrar:

```bash
cd forgeops
```

Para reproducir el laboratorio sobre la versión de ForgeOps 7.5 documentada:

```bash
git checkout release/7.5-20251119
```

### Verificación

```bash
git branch --show-current
```

Y:

```bash
git status
```

Debemos saber exactamente qué branch estamos utilizando.

> Nota: el repositorio puede cambiar con el tiempo. Para una reproducción exacta del laboratorio conviene guardar también el commit:

```bash
git rev-parse HEAD
```

---

# 11. Verificar la estructura de ForgeOps

### Aqui previamente se creo un repo personalizado llamado:

https://github.com/WillianCahuaya/SessionTokenForgeRock

Desde:

```text
SessionTokenForgeRock/forgeops
```

podemos ver:

```bash
ls
```

ForgeOps contiene directorios como:

```text
bin/
docker/
kustomize/
config/
docs/
...
```

La documentación oficial describe el repositorio como una combinación de Dockerfiles, configuraciones, perfiles, Kustomize, Helm y scripts de despliegue.

---

# 12. Preparar el namespace de ForgeRock

Creamos:

```bash
kubectl create namespace forgerock
```

Verificar:

```bash
kubectl get namespaces
```

Debe aparecer:

```text
forgerock
```

También podemos verificar:

```bash
kubectl get pods -n forgerock
```

---

# 13. Desplegar ForgeRock

ForgeOps proporciona el comando `forgeops install` para generar y desplegar los manifests de Kubernetes. La documentación indica que el comando utiliza Kustomize para instalar los componentes.

En nuestro laboratorio utilizamos la configuración CDK/single-instance.

El despliegue termina creando componentes como:

```text
DS
AM
AMster
IG
```

---

# 14. Verificar los Pods

El comando que usamos constantemente es:

```bash
kubectl get pods -n forgerock
```

Durante un entorno correctamente desplegado esperamos algo similar a:

```text
am-xxxxx                    1/1   Running
amster-xxxxx                0/1   Completed
ds-cts-0                    1/1   Running
ds-idrepo-0                 1/1   Running
ig-xxxxx                    1/1   Running
ldif-importer-xxxxx         0/1   Completed
```

### Cómo interpretar `STATUS`

```text
Running
```

El proceso está ejecutándose.

```text
Completed
```

El Job terminó correctamente.

```text
Pending
```

Kubernetes todavía no pudo ejecutar el Pod.

```text
CrashLoopBackOff
```

El contenedor está arrancando y fallando repetidamente.

---

# 15. Problema que encontramos: falta de memoria

Durante la instalación de AMster tuvimos:

```text
amster-xxxxx   Pending
```

Al ejecutar:

```bash
kubectl describe pod amster-xxxxx -n forgerock
```

apareció:

```text
FailedScheduling
Insufficient memory
```

Esto significa que Kubernetes quería colocar el Pod en el nodo, pero no existía memoria suficiente.

### Por qué ocurrió

Nuestro nodo KIND ya estaba ejecutando:

```text
AM
DS CTS
DS IDRepo
IG
```

y AMster necesitaba memoria adicional.

### Solución temporal utilizada

Reducimos temporalmente IG:

```bash
kubectl scale deployment ig -n forgerock --replicas=0
```

Esperamos:

```bash
kubectl get pods -n forgerock
```

Después comprobamos AMster:

```bash
kubectl get pods -n forgerock -l job-name=amster
```

Cuando terminó:

```text
amster-xxxxx   0/1   Completed
```

levantamos nuevamente IG:

```bash
kubectl scale deployment ig -n forgerock --replicas=1
```

Después:

```bash
kubectl get pods -n forgerock
```

y esperamos:

```text
ig-xxxxx   1/1   Running
```

---

# 16. AMster

AMster importa configuraciones almacenadas en el repositorio.

Por ejemplo:

```text
docker/amster/config-profiles/cdk/config/
```

Contiene configuraciones de AM como:

```text
OAuth2Clients/
Applications/
...
```

Para ejecutar la importación:

```bash
./bin/forgeops install amster
```

### Verificación

```bash
kubectl get pods -n forgerock -l job-name=amster
```

Resultado:

```text
amster-xxxxx   0/1   Completed
```

Esto significa:

```text
JSON del repositorio
       ↓
     AMster
       ↓
 configuración importada
       ↓
      AM
```

---

# 17. Verificar AM Service

Consultar:

```bash
kubectl get svc am -n forgerock
```

Nuestro resultado fue un Service de tipo:

```text
ClusterIP
```

Por ejemplo:

```text
NAME   TYPE       CLUSTER-IP      PORT(S)
am     ClusterIP  10.96.xxx.xxx   80/TCP
```

`ClusterIP` significa que AM está accesible dentro de Kubernetes.

Desde el Mac utilizamos:

```bash
kubectl port-forward svc/am 8080:80 -n forgerock
```

Ahora:

```text
Mac
localhost:8080
      ↓
Kubernetes Service am
      ↓
AM :80
```

---

# 18. Problema de FQDN de AM

AM no acepta `localhost` como FQDN válido en determinadas operaciones.

Una petición directa:

```bash
curl http://localhost:8080/am/...
```

podía devolver:

```text
FQDN "localhost" is not valid
```

Para nuestro laboratorio solucionamos esto indicando:

```http
Host: am
```

Por ejemplo:

```bash
curl -i \
  -H "Host: am" \
  http://localhost:8080/am/json/realms/root
```

Más adelante, para OAuth2 usamos `--resolve` para que el hostname `am` resolviera a `127.0.0.1`:

```bash
--resolve am:8080:127.0.0.1
```

De esta forma:

```text
URL:
http://am:8080

DNS local:
am → 127.0.0.1

Host HTTP:
am
```

Esto satisface la validación de FQDN de AM.

---

# 19. Verificar OAuth2 de AM

Descubrimos el endpoint OAuth2 real mediante OpenID Connect Discovery:

```bash
curl -i \
  -H "Host: am" \
  http://localhost:8080/am/oauth2/realms/root/.well-known/openid-configuration
```

La respuesta fue:

```text
HTTP 200
```

y nos mostró:

```text
token_endpoint:
https://forgerock.iam.example.com/am/oauth2/access_token
```

Por lo tanto nuestro laboratorio utiliza:

```text
/am/oauth2/access_token
```

y no:

```text
/am/oauth2/realms/root/access_token
```

Este paso es importante porque **siempre conviene descubrir el endpoint publicado por AM en lugar de asumirlo**.

---

# 20. Crear sesión administrativa `amadmin`

Para administrar AM necesitamos autenticarnos como:

```text
amadmin
```

La llamada REST devuelve:

```json
{
    "tokenId": "..."
}
```

Ese `tokenId` es una sesión administrativa.

Conceptualmente:

```text
amadmin
   ↓
AM
   ↓
admin session token
```

Ese token nos permitió posteriormente:

```text
crear usuario
consultar usuario
consultar OAuth2 clients
modificar configuración OAuth2
```

No es el Token A que utilizará el Mobile.

---

# 21. Crear usuario de prueba

Creamos:

```text
username:
testuser

password:
Password123!
```

Después verificamos:

```bash
curl ... /users/testuser
```

AM respondió:

```text
username = testuser
uid = testuser
inetUserStatus = Active
```

Por lo tanto:

```text
testuser
   ↓
AM Directory
   ↓
Active
```

---

# 22. Crear cliente OAuth2 para el Mobile

Partimos del cliente existente:

```text
docker/amster/config-profiles/cdk/config/realms/root/OAuth2Clients/oauth2.json
```

Creamos una copia:

```text
oauth2-mobile-simulator.json
```

La intención es que quede:

```text
clientId:
mobile-simulator

clientType:
Confidential

grantTypes:
password incluido

accessTokenLifetime:
3600
```

El `3600` representa:

```text
3600 segundos
= 60 minutos
= 1 hora
```

---

# 23. Importar el nuevo cliente

Después de modificar el JSON:

```bash
./bin/forgeops install amster
```

Verificamos la colección de clientes OAuth2.

El cliente finalmente apareció en AM como:

```text
_id = mobile-simulator
clientName = Mobile Simulator
clientType = Confidential
accessTokenLifetime = 3600
grantTypes incluye password
```

Esto confirma que:

```text
ForgeOps JSON
     ↓
AMster
     ↓
AM
```

está funcionando correctamente.

---

# 24. Configurar Password Grant

Para este laboratorio utilizamos:

```text
grant_type=password
```

Es un flujo de laboratorio que nos permite probar rápidamente:

```text
Mobile
  ↓
usuario + contraseña
  ↓
AM
  ↓
Access Token
```

Además configuramos en AM:

```text
passwordGrantAuthService = ldapService
```

La comprobación final en los logs de AM mostró que:

```text
mobile-simulator
      ↓
ldapService
      ↓
DataStore
      ↓
testuser
```

La autenticación fue correctamente ejecutada.

---

# 25. Mobile Simulator

El Mobile real no forma parte de ForgeRock.

Creamos un pequeño cliente fuera de `forgeops`:

```text
SessionTokenForgeRock/
│
├── forgeops/
│
└── mobile-simulator/
    └── request-token-a.sh
```

La función de ese script es solamente:

```text
simular Mobile
```

No contiene lógica de ForgeRock.

---

# 26. Primera versión del Mobile Simulator

El script ejecuta un:

```text
POST /am/oauth2/access_token
```

con:

```text
client_id
client_secret
username
password
grant_type=password
auth_chain=ldapService
scope=profile
```

Conceptualmente:

```text
Mobile Simulator
      │
      │ client credentials
      │ user credentials
      ▼
AM
```

---

# 27. Problemas durante la prueba del Token A

Al principio aparecieron varios errores, que son útiles como guía de diagnóstico.

### `Realm not found`

La URL utilizada inicialmente era:

```text
/am/oauth2/realms/root/access_token
```

El discovery mostró que nuestra instalación utiliza:

```text
/am/oauth2/access_token
```

---

### `FQDN "localhost" is not valid`

Se resolvió utilizando:

```text
am
```

como hostname y:

```bash
--resolve am:8080:127.0.0.1
```

---

### `invalid_client`

Significó que AM todavía no aceptaba correctamente el cliente OAuth2.

Verificamos el cliente en AM mediante la API administrativa.

Después de corregir el secreto y volver a importar con AMster:

```text
invalid_client
```

desapareció.

---

### `invalid_grant`

Significó:

```text
Resource owner authentication failed
```

Revisamos:

```text
testuser
```

y comprobamos:

```text
DataStore
    ↓
testuser
    ↓
OK
```

Posteriormente el log de AM mostró explícitamente:

```text
failureReason:
INVALID_PASSWORD
```

mientras usábamos un parámetro de servicio incorrecto.

La configuración definitiva quedó:

```text
passwordGrantAuthService = ldapService
```

y el request utilizó:

```text
auth_chain=ldapService
```

---

### `invalid_scope`

El cliente no tenía:

```text
openid
```

pero sí tenía:

```text
profile
```

Por eso:

```text
scope=openid profile
```

fallaba.

Cambiamos a:

```text
scope=profile
```

y funcionó.

---

# 28. Token A funcionando

La prueba final:

```bash
./mobile-simulator/request-token-a.sh
```

devolvió:

```json
{
    "access_token": "...",
    "refresh_token": "...",
    "scope": "profile",
    "token_type": "Bearer",
    "expires_in": 3599
}
```

Esto demuestra:

```text
Mobile Simulator
      ↓
      AM
      ↓
Token A
```

con:

```text
TTL ≈ 1 hora
```

La configuración es:

```text
accessTokenLifetime = 3600
```

y la respuesta puede mostrar:

```text
3599
3598
...
```

porque el tiempo empieza a correr desde la emisión.

---

# 29. ¿Por qué Token A es opaco?

Nuestro Token A actual no es JWT.

Ejemplo conceptual:

```text
fK_RhC3Qvq1GfUn9ywO6NuetBD0
```

No tiene:

```text
header.payload.signature
```

Esto ocurre porque tenemos:

```text
statelessTokensEnabled = false
```

Por ahora está bien.

Nuestro objetivo actual es aprender:

```text
Token A
   ↓
IG
   ↓
Token B
```

Más adelante podemos mejorar Token A para convertirlo en JWT y trabajar también con:

```text
userId
role
claims
JWKS
firma
public key
```

---

# 30. IG

IG ya viene incluido como componente ForgeRock.

No necesitamos crear un proyecto Java desde cero.

Pensamos en IG así:

```text
IG
│
├── Runtime Java
│
├── config.json
│
├── Secrets
│
└── Routes
```

Una route es una configuración que define cómo IG procesa una petición.

Por ejemplo:

```text
POST /token/exchange
        ↓
Route
        ↓
Filters
        ↓
Handler
```

---

# 31. Configuración actual de IG

Nuestro Dockerfile es:

```text
docker/ig/Dockerfile
```

y contiene:

```dockerfile
FROM gcr.io/forgerock-io/ig:2024.3.0

ARG CONFIG_PROFILE=cdk

COPY --chown=forgerock:root config-profiles/${CONFIG_PROFILE}/ /var/ig
COPY --chown=forgerock:root . /var/ig
```

Por lo tanto:

```text
docker/ig/config-profiles/cdk/
        ↓
      /var/ig/
```

La imagen que utilizamos para IG es:

```text
gcr.io/forgerock-io/ig:2024.3.0
```

---

# 32. Routes existentes

Encontramos:

```text
docker/ig/config-profiles/cdk/config/routes-service/
```

con:

```text
03-rs-tokenintrospect.json
03-rs-tokenintrospect-nocache.json
50-reverseproxy.json
```

La route:

```text
03-rs-tokenintrospect.json
```

ya demuestra cómo hacer:

```text
Bearer Token
     ↓
OAuth2ResourceServerFilter
     ↓
TokenIntrospectionAccessTokenResolver
     ↓
AM
```

y acceder a:

```text
contexts.oauth2.accessToken.info
```

---

# 33. Objetivo siguiente: Token Exchange

Ahora queremos construir:

```text
POST /token/exchange
```

El flujo será:

```text
Mobile
   │
   │ Authorization: Bearer Token A
   ▼
IG
   │
   ├── introspect Token A
   │
   ├── obtiene información
   │
   ├── genera Token B
   │
   └── Token B expira en 60 segundos
   ▼
Mobile / Backend
```

El archivo previsto será:

```text
docker/ig/config-profiles/cdk/config/routes-service/04-token-exchange.json
```

---

# 34. Claves para Token B

Como IG será el emisor del Token B, creamos:

```text
docker/ig/config-profiles/cdk/secrets/
├── token-b-signing-key.pem
└── token-b-verifying-key.pem
```

Conceptualmente:

```text
private key
    ↓
IG
    ↓
firma Token B
```

y:

```text
public key
    ↓
Backend / APIM
    ↓
verifica Token B
```

Estas claves pertenecen conceptualmente al emisor interno:

```text
IG
```

No al Mobile.

---

# 35. Estado actual

Actualmente tenemos:

```text
Docker
   ✅

KIND
   ✅

Kubernetes
   ✅

namespace forgerock
   ✅

DS IDRepo
   ✅

DS CTS
   ✅

AM
   ✅

AMster
   ✅

IG
   ✅

usuario testuser
   ✅

OAuth2 client mobile-simulator
   ✅

Mobile Simulator
   ✅

Token A
   ✅

Token A TTL ≈ 1 hora
   ✅
```

El siguiente objetivo es:

```text
Token A
   ↓
IG
   ↓
Token B
   ↓
TTL = 1 minuto
```

---

# 36. Comandos de diagnóstico básicos

Un junior debería memorizar primero estos:

### Ver Pods

```bash
kubectl get pods -n forgerock
```

### Ver Services

```bash
kubectl get svc -n forgerock
```

### Ver logs de AM

```bash
kubectl logs deployment/am -n forgerock
```

### Ver logs de IG

```bash
kubectl logs deployment/ig -n forgerock
```

### Describir Pod con problemas

```bash
kubectl describe pod <pod> -n forgerock
```

### Ver Jobs

```bash
kubectl get jobs -n forgerock
```

### Ver AMster

```bash
kubectl get pods -n forgerock -l job-name=amster
```

### Ejecutar AMster nuevamente

```bash
./bin/forgeops install amster
```

### Port-forward de AM

```bash
kubectl port-forward svc/am 8080:80 -n forgerock
```

### Ver cluster KIND

```bash
kind get clusters
```

### Ver contexto Kubernetes

```bash
kubectl config current-context
```

---

# 37. Mapa mental final

```text
GitHub
  │
  │ clone ForgeOps
  ▼
ForgeOps repository
  │
  ├── Dockerfiles
  ├── AM configuration
  ├── IG configuration
  ├── Kustomize
  └── scripts
  │
  ▼
Docker
  │
  ▼
KIND
  │
  ▼
Kubernetes
  │
  └── namespace: forgerock
       │
       ├── DS
       ├── AM
       ├── AMster
       └── IG
            │
            └── Routes
  │
  ▼
Mobile Simulator
  │
  │ OAuth2
  ▼
AM
  │
  ▼
Token A
  │
  │ próximo paso
  ▼
IG
  │
  ▼
Token B
```
