# Tutorial básico — ForgeRock local con ForgeOps, Docker y KIND

## BLOQUE 1 — Prerrequisitos

Objetivo: comprobar que el equipo tiene todo lo necesario antes de tocar ForgeRock.

### 1. Docker

Docker debe estar instalado y ejecutándose.

```bash
docker version
docker info
```

**Esperado:** Docker responde correctamente.

---

### 2. Git

Git se utilizará para descargar ForgeOps.

```bash
git --version
```

---

### 3. kubectl

`kubectl` será la herramienta para administrar Kubernetes.

```bash
kubectl version --client
```

**Esperado:** aparece la versión del cliente.

---

### 4. KIND

KIND crea un cluster Kubernetes local sobre Docker.

```bash
kind version
```

**Esperado:** aparece la versión instalada.

---

### 5. Repositorio ForgeOps

Clonar ForgeOps desde GitHub:

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
git status
```

**Esperado:** Git reconoce el repositorio.

---

### 7. Verificar que kubectl está conectado al cluster

Todavía no necesitamos ForgeRock para esta prueba.

```bash
kubectl config current-context
```

Si el cluster todavía no existe, este paso se realiza después de crear KIND.

---

### Checklist del bloque 1

```text
Docker       ✅
Git          ✅
kubectl      ✅
KIND         ✅
ForgeOps     ✅
Versión      ✅
```

No continuar hasta tener todos los puntos correctos.

# BLOQUE 2 — Preparar el ambiente

Objetivo: crear el cluster Kubernetes y preparar ForgeRock.

## 1. Crear cluster KIND

Nuestro cluster:

```text
willca-local
```

Crear:

```bash
kind create cluster --name willca-local
```

---

## 2. Verificar KIND

```bash
kind get clusters
```

**Esperado:**

```text
willca-local
```

---

## 3. Verificar Kubernetes

```bash
kubectl config current-context
```

**Esperado:**

```text
kind-willca-local
```

Ver nodos:

```bash
kubectl get nodes
```

**Esperado:**

```text
STATUS
Ready
```

---

## 4. Crear namespace ForgeRock

```bash
kubectl create namespace forgerock
```

Verificar:

```bash
kubectl get namespaces
```

**Esperado:** aparece:

```text
forgerock
```

---

## 5. Preparar ForgeOps

Trabajar desde:

```bash
cd forgeops
```

Verificar estructura:

```bash
ls
```

Debemos tener elementos como:

```text
bin/
docker/
kustomize/
...
```

---

## 6. Desplegar ForgeRock

Ejecutar el procedimiento de despliegue definido por el perfil utilizado.

Después verificar:

```bash
kubectl get pods -n forgerock
```

---

## 7. Verificar componentes

Esperamos tener:

```text
DS IDRepo      Running
DS CTS         Running
AM             Running
AMster         Completed
IG             Running
```

---

## 8. Verificar AM

```bash
kubectl get svc am -n forgerock
```

El Service de AM es `ClusterIP`.

Para acceder desde el Mac:

```bash
kubectl port-forward svc/am 8080:80 -n forgerock
```

---

## Checklist del bloque 2

```text
KIND cluster       ✅
Kubernetes         ✅
namespace          ✅
DS IDRepo          ✅
DS CTS             ✅
AM                 ✅
AMster             ✅
IG                 ✅
AM accesible       ✅
```

En este punto **ForgeRock ya está levantado**.

# BLOQUE 3 — Implementación y casos encontrados

Objetivo: implementar el flujo y documentar los problemas reales encontrados durante las pruebas.

## PARTE A — Preparar AM

### 1. Verificar OAuth2

Consultar Discovery:

```bash
curl -i \
  -H "Host: am" \
  http://localhost:8080/am/oauth2/realms/root/.well-known/openid-configuration
```

Verificar el `token_endpoint`.

En nuestro entorno:

```text
/am/oauth2/access_token
```

---

### 2. Crear sesión administrativa

Autenticar:

```text
amadmin
```

Resultado:

```text
tokenId
```

Este token sirve para administrar AM.

---

### 3. Crear usuario de prueba

```text
username = testuser
password = Password123!
```

Verificar que:

```text
inetUserStatus = Active
```

---

## PARTE B — Crear cliente OAuth2 del Mobile

Partir de:

```text
OAuth2Clients/oauth2.json
```

Crear:

```text
OAuth2Clients/oauth2-mobile-simulator.json
```

Configuración principal:

```text
clientId            = mobile-simulator
clientType          = Confidential
grantType           = password
accessTokenLifetime = 3600
```

Importar con:

```bash
./bin/forgeops install amster
```

Verificar en AM que aparece:

```text
mobile-simulator
```

---

## PARTE C — Configurar Password Grant

Configurar:

```text
passwordGrantAuthService = ldapService
```

y utilizar:

```text
auth_chain=ldapService
```

---

## PARTE D — Crear Mobile Simulator

Fuera de ForgeOps:

```text
mobile-simulator/
└── request-token-a.sh
```

Responsabilidad:

```text
simular Mobile
    ↓
solicitar Token A a AM
```

---

## PARTE E — Obtener Token A

Ejecutar:

```bash
./mobile-simulator/request-token-a.sh
```

Esperado:

```json
{
    "access_token": "...",
    "refresh_token": "...",
    "scope": "profile",
    "token_type": "Bearer",
    "expires_in": 3599
}
```

Resultado:

```text
Token A ✅
TTL ≈ 1 hora ✅
```

---

# PARTE F — Problemas encontrados

Estos casos deben quedar documentados porque son parte del aprendizaje.

### Caso 1 — Realm not found

Problema:

```text
Realm not found
```

Causa:

Se utilizó inicialmente:

```text
/am/oauth2/realms/root/access_token
```

La instalación publica:

```text
/am/oauth2/access_token
```

Solución:

Usar el endpoint descubierto mediante `.well-known/openid-configuration`.

---

### Caso 2 — FQDN localhost

Problema:

```text
FQDN "localhost" is not valid
```

Solución:

Usar:

```text
Host: am
```

y:

```text
--resolve am:8080:127.0.0.1
```

---

### Caso 3 — invalid_client

Problema:

```json
{
    "error": "invalid_client"
}
```

Causa:

El cliente OAuth2 todavía no estaba registrado correctamente en AM.

Solución:

Importar nuevamente mediante AMster y verificar:

```text
mobile-simulator
```

en AM.

---

### Caso 4 — invalid_grant

Problema:

```json
{
    "error": "invalid_grant"
}
```

Diagnóstico mediante logs de AM:

```text
INVALID_PASSWORD
```

Se verificó posteriormente la autenticación mediante `ldapService`.

---

### Caso 5 — invalid_scope

Problema:

```json
{
    "error": "invalid_scope"
}
```

Causa:

Se solicitó:

```text
openid profile
```

pero el cliente no tenía `openid`.

Solución:

```text
scope=profile
```

---

### Caso 6 — AMster Pending

Problema:

```text
amster   Pending
```

Diagnóstico:

```text
Insufficient memory
```

Solución temporal:

```bash
kubectl scale deployment ig -n forgerock --replicas=0
```

Ejecutar nuevamente AMster y posteriormente:

```bash
kubectl scale deployment ig -n forgerock --replicas=1
```

---

# PARTE G — Preparación de IG

Una vez funcionando Token A, comenzamos la segunda etapa.

## 1. Entender IG

IG es el servidor/gateway.

Las routes son su configuración.

```text
IG
│
├── config.json
│
└── routes-service/
```

---

## 2. Route existente

Revisar:

```text
03-rs-tokenintrospect.json
```

Esta route demuestra:

```text
Token
 ↓
OAuth2ResourceServerFilter
 ↓
TokenIntrospectionAccessTokenResolver
 ↓
AM
```

---

## 3. Crear Token B

El objetivo será:

```text
POST /token/exchange
```

Flujo:

```text
Mobile
   ↓
Token A
   ↓
IG
   ↓
Introspection
   ↓
Token B
   ↓
Backend
```

Token B:

```text
TTL = 60 segundos
```

---

## 4. Claves de Token B

IG será el emisor.

```text
token-b-signing-key.pem
token-b-verifying-key.pem
```

```text
Private Key
    ↓
IG
    ↓
firma Token B

Public Key
    ↓
Backend / APIM
    ↓
verifica Token B
```

---

## Estado final de esta etapa

```text
ForgeRock levantado           ✅
AM funcionando                ✅
IG funcionando                ✅
Usuario de prueba             ✅
OAuth2 Client                 ✅
Mobile Simulator              ✅
Token A                       ✅
Token A ≈ 1 hora              ✅

Token A → IG → Token B        ⏳
```
