# Actualizar una Route de ForgeRock IG en el ambiente local

Este documento describe el procedimiento para crear o modificar una **route de ForgeRock Identity Gateway (IG)** y actualizarla en el ambiente local basado en Docker + KIND + Kubernetes.

## 1. Crear o modificar la Route

Ejemplo:

docker/ig/config-profiles/cdk/config/routes-service/00-kube.json

Ejemplo de una route:

```json
{
    "name": "00-kube",
    "condition": "${find(request.uri.path, '^/kube$')}",
    "handler": {
        "type": "StaticResponseHandler",
        "config": {
            "status": 200,
            "headers": {
                "Cache-Control": ["none"],
                "Content-Type": ["text/plain"],
                "X-Content-Type-Options": ["nosniff"]
            },
            "entity": "Kubernetes monitoring request, Used to disabled Logs. Your path is ${request.uri.path}"
        }
    },
    "monitor": {
        "enabled": false
    },
    "globalDecorators": {
        "timer": false
    }
}
```

En este ejemplo:

GET /kube => 00-kube.json => StaticResponseHandler => HTTP 200

---

## 2. Validar el JSON

Antes de construir la imagen, validar que el archivo tenga un JSON correcto:

```bash
python3 -m json.tool \
docker/ig/config-profiles/cdk/config/routes-service/00-kube.json > /dev/null && \
echo "00-kube.json OK"
```

Resultado esperado:

00-kube.json OK

Si existe un error de sintaxis, corregirlo antes de continuar.

---

## 3. Construir nuevamente la imagen de IG

ForgeOps utiliza el `Dockerfile` de IG para copiar la configuración dentro de la imagen.

Ejecutar:

```bash
./bin/forgeops build ig --config-profile cdk --push-to none
```

El proceso genera: => ig:latest

Resultado esperado al final:

naming to docker.io/library/ig:latest
Updated the image_defaulter with your new image for ig: "ig".

Esto significa que la nueva configuración de `routes-service` ya está incluida en la imagen.

---

## 4. Cargar la nueva imagen en KIND

La imagen creada en Docker Desktop todavía debe estar disponible dentro del nodo de KIND.

Ejecutar:

```bash
kind load docker-image ig:latest --name willca-local
```

Resultado esperado: =>
Image: "ig:latest" ... loading...

Esto copia la imagen hacia el nodo:

Docker Desktop =>
ig:latest =>
KIND =>
willca-local-control-plane

---

## 5. Recrear el Pod de IG

En este laboratorio la Deployment de IG está configurada para utilizar:

image: ig:latest =>
imagePullPolicy: IfNotPresent

Por lo tanto, Kubernetes puede utilizar la imagen que acabamos de cargar en KIND.

Para que IG arranque nuevamente leyendo la configuración actualizada:

```bash
kubectl delete pod -l app=ig -n forgerock
```

No estamos eliminando la Deployment.

Estamos eliminando únicamente el Pod para que Kubernetes cree uno nuevo automáticamente.

---

## 6. Esperar a que IG vuelva a estar disponible

Observar el Pod:

```bash
kubectl get pods -n forgerock -l app=ig -w
```

El estado puede pasar por: =>
0/1 Running

y finalmente: =>
1/1 Running

Resultado esperado: =>
ig-xxxxxxxxxx 1/1 Running 0 ...

---

## 7. Verificar que IG cargó la nueva Route

Revisar los logs:

```bash
kubectl logs deployment/ig -n forgerock --tail=200
```

Para buscar específicamente una route:

```bash
kubectl logs deployment/ig -n forgerock --tail=200 | \
grep -E "00-kube|ERROR|Unable to start"
```

Para `00-kube` esperamos:

Loaded the route with id '00-kube' registered with the name '00-kube'

Si aparece ese mensaje, significa que IG encontró y cargó correctamente la route.

También es importante que no aparezcan errores como:

ERROR
Unable to start
An error occurred while building the route

---

## 8. Recargar la conexion Mac con Docker

Como se reincio el docker, esto pierde la conexion de la maquina local con el ambiente levantado de de Forge Rock

Solo se debe bajar y levantar nuevamente la conexion

---

## 9. Probar la Route

Como el ambiente local tiene:

localhost:8081 =>
IG Service =>
IG :80

podemos probar:

```bash
curl -i http://localhost:8081/kube
```

Resultado esperado: =>
HTTP/1.1 200 OK

y en el body: =>
Kubernetes monitoring request, Used to disabled Logs. Your path is /kube

La expresión: =>  
${request.uri.path}

debe resolverse como: =>  
/kube

por lo que el resultado será similar a:

Kubernetes monitoring request, Used to disabled Logs. Your path is /kube

---

# Flujo completo

Cada vez que se cree o modifique una route, el flujo utilizado en este laboratorio es:

```text
Modificar / crear Route
        ↓
Validar JSON
        ↓
./bin/forgeops build ig
        ↓
Generar ig:latest
        ↓
kind load docker-image ig:latest
        ↓
Eliminar Pod de IG
        ↓
Kubernetes crea nuevo Pod
        ↓
IG inicia
        ↓
IG carga las Routes
        ↓
Verificar logs
        ↓
Probar endpoint
```
