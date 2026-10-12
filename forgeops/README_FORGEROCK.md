# Guía completa: levantar ForgeRock localmente con ForgeOps + Docker + KIND

## 1. Objetivo

El objetivo es levantar un entorno ForgeRock local para poder estudiar y probar este flujo:

```text
             🏢 FORGEROCK LAB
                     │
                     ▼
          1️⃣ 🗄️ DS — Directory Services
                     │
             ┌───────┴───────┐
             ▼               ▼
       👤 DS — IdRepo    🗄️ DS — CTS
          Identidades       Sesiones y tokens
             │               │
             └───────┬───────┘
                     ▼
          2️⃣ 🔐 AM — Access Management
             Autenticación, OAuth 2.0, sesiones y tokens
                     ▲
                     │
                     │ configuración
                     │
          3️⃣ ⚙️ Amster (tarea inicial)
                     │ Configura AM y termina
                     │
                     ▼
          4️⃣ 🚪 IG — Identity Gateway
             Rutas, filtros y validación
                     │
                     ▼
               🌐 CIAM API
                     │
                     ▼
          🧩 Servicios de negocio

```

## 🏢 ForgeRock Lab: el entorno completo.

### 🚪 IG(Identity Gateway):

Función: actuar como puerta de entrada entre los clientes y las API internas.

- Recibe peticiones HTTP/HTTPS.
- Ejecuta rutas y filtros para validar, transformar o bloquear peticiones.
- Puede validar tokens y consultar AM cuando la configuración lo requiere.
- Reenvía las peticiones al servicio de destino.

¿Debe permanecer ejecutándose? Sí, mientras deba atender peticiones.

En tu laboratorio: la ruta 16-apis-integration-session procesa las solicitudes de registro y eliminación de sesiones.

### 🔐 AM(Access Management):

Función: gestionar la autenticación y el acceso de usuarios y aplicaciones.

- Autentica usuarios.
- Gestiona sesiones SSO.
- Emite tokens OAuth 2.0 y puede proporcionar funciones de OpenID Connect.
- Permite configurar clientes, scopes y políticas de acceso.
- Expone servicios para consultar o validar tokens, según el flujo.

¿Debe permanecer ejecutándose? Sí, para atender las operaciones de identidad y acceso que dependen de él.

En tu laboratorio: IG utiliza AmService para comunicarse con AM.

### 👤 DS IdRepo (Disrectory Service Identity Repository):

Función: proporcionar el repositorio de identidades que AM utiliza para consultar datos de usuarios.

- Almacena identidades y atributos.
- Permite buscar y consultar usuarios.
- Proporciona información de identidad a AM.

¿Debe permanecer ejecutándose? Sí, mientras AM dependa de ese directorio para las operaciones correspondientes.

En tu laboratorio: corresponde al pod ds-idrepo-0.

### 🗄️ DS CTS (Directory Service Core Token Service):

Función: almacenar información de estado de sesiones y tokens que AM necesita mantener.

- Conserva datos asociados a sesiones.
- Puede almacenar información de tokens y otros estados gestionados por AM.
- Permite que AM consulte ese estado cuando lo necesita.

¿Debe permanecer ejecutándose? Sí, si AM está configurado para depender de CTS durante las operaciones que realizas.

En tu laboratorio: corresponde al pod ds-cts-0.

### ⚙️ Amster:

Función: automatizar la administración de la configuración de AM.

- Importa configuraciones previamente preparadas.
- Exporta configuraciones para respaldarlas o reutilizarlas.
- Facilita repetir despliegues en diferentes entornos.

¿Debe permanecer ejecutándose? Generalmente, no.

Amster puede ejecutarse durante la instalación o una tarea de configuración y finalizar cuando termina. La configuración que importó queda almacenada en AM; no depende de que Amster permanezca activo. Si necesitas cambiar o importar configuración de nuevo, puedes volver a ejecutarlo.

En tu laboratorio: el pod amster-g622w aparece como Completed, algo normal si ejecutó una tarea de inicialización y terminó correctamente.

#### 🌐 CIAM API (Customer Identity and Access Management API):

Función: exponer operaciones de identidad a los sistemas que las necesitan.

- Recibe peticiones de registro o eliminación de sesiones, según su implementación.
- Ejecuta la lógica definida por la aplicación.
- Puede comunicarse con otros sistemas o servicios internos.

¿Debe permanecer ejecutándose? Sí, mientras IG necesite invocar esa API.

En tu laboratorio: corresponde al pod ciam-api-66f5f48c49-kjgzq.

---

## 2. Arquitectura del laboratorio

Nuestro entorno local tiene varias capas:

```text
                💻 MACBOOK — HOST LOCAL
                           │
         ┌─────────────────┼────────────────┐
         │                 │                │
         ▼                 ▼                ▼
   📦 Git            🐳 Docker       ⌨️ Terminal
   Versionamiento       Desktop           Shell
         │                 │                │
         ▼                 ▼                ▼
📂 Repositorio     🧱 Contenedores   Comandos CLI
forgeops                   │                │
                           ▼                ├── kubectl
                     ☸️ kind                │   Cliente Kubernetes
                      Clúster local         │
                           │                ├── git
                           ▼                └── docker
                     ☸️ Kubernetes
                           │
                           ├── Namespace: forgerock
                           │
                           ├── 🗄️ DS — IdRepo
                           ├── 🗄️ DS — CTS
                           ├── 🔐 AM
                           ├── 🚪 IG
                           ├── ⚙️ Amster (tarea)
                           └── 🌐 CIAM API
```

---

### Requisitos del equipo

ForgeOps documenta como referencia para entornos locales una VM/engine Docker con al menos 4 CPUs, 12 GB de RAM y 60 GB de disco. Aunque la documentación oficial utiliza Minikube para el ejemplo local, esos recursos son una buena referencia para un laboratorio con KIND también.

---

## 3. Configurar ambiente

### Crear el cluster KIND

Nuestro laboratorio utiliza el nombre: willca-local

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

Verificación Kubernetes

```bash
kubectl cluster-info
```

Verificación del nodo

```bash
kubectl get nodes
```

---

### Entender el contexto Kubernetes

KIND crea y configura automáticamente un contexto para `kubectl`.

Comprobarlo:

```bash
kubectl config get-contexts
```

Comprobar el contexto actual:

```bash
kubectl config current-context
```

---

### Descargar ForgeOps desde GitHub

ForgeOps es el repositorio que utilizamos para obtener las configuraciones y scripts de la plataforma.

Clonar:

```bash
git clone https://github.com/ForgeRock/forgeops.git
cd forgeops
```

Para reproducir el laboratorio sobre la versión de ForgeOps 7.5 documentada:

```bash
git checkout release/7.5-20251119
```

---

### Verificar la estructura de ForgeOps

Aqui previamente se creo un repo personalizado llamado para guardar los modificado y levantado en el ambiente

https://github.com/WillianCahuaya/SessionTokenForgeRock

---

### Preparar el namespace de ForgeRock

Creamos:

```bash
kubectl create namespace forgerock
```

Verificar:

```bash
kubectl get namespaces
```

También podemos verificar:

```bash
kubectl get pods -n forgerock
```

---

### Desplegar ForgeRock

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

### Verificar los Pods

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

---

### AMster para ejecutar la importación:

```bash
./bin/forgeops install amster
```

Verificación

```bash
kubectl get pods -n forgerock -l job-name=amster
```

---

### Verificar AM Service

Consultar:

```bash
kubectl get svc am -n forgerock
```

Desde el Mac utilizamos:

```bash
kubectl port-forward svc/am 8080:80 -n forgerock
```

---

### Problema de FQDN de AM

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
