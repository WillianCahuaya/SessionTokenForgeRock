# IG_DEPLOY

## 1. Validar archivo JSON

Comprueba que la configuración de la ruta de integración tenga una sintaxis JSON válida antes de construir la imagen.

```bash
python3 -m json.tool \
  docker/ig/config-profiles/cdk/config/routes-service/16-apis-integration-session.json \
  > /dev/null && echo "16-apis-integration-session.json OK"
```

## 2. Construir imagen de IG

Construye la imagen de IG utilizando el perfil de configuración `cdk`, sin publicarla en un registro.

```bash
./bin/forgeops build ig --config-profile cdk --push-to none
```

## 3. Cargar imagen en Kind

Carga la imagen local en el clúster `willca-local` para que Kubernetes pueda utilizarla.

```bash
kind load docker-image ig:latest --name willca-local
```

## 4. Recrear el pod de IG

Elimina el pod existente para que Kubernetes lo cree nuevamente. El Deployment debe estar configurado para utilizar la imagen actualizada.

```bash
kubectl delete pod -l app=ig -n forgerock
```

Verificar el estado del pod:

```bash
kubectl get pods -n forgerock -l app=ig -w
```

## 5. Consultar logs

**Últimas 200 líneas de IG:**

```bash
kubectl logs deployment/ig -n forgerock --tail=200
```

**Logs de IG en tiempo real, filtrando eventos de integración:**

```bash
kubectl logs -f deployment/ig -n forgerock --timestamps \
  | grep --line-buffered -E 'Redis Script Filter|register|ciam|INFO|WARN|ERROR'
```

**Buscar errores recientes de IG:**

```bash
kubectl logs deployment/ig -n forgerock --since=10m --timestamps \
  | grep -iE 'error|exception|caused by|internal token|failed'
```

**Logs recientes de IG y CIAM API:**

```bash
kubectl logs -f deployment/ig -n forgerock --since=5m --timestamps
kubectl logs -f deployment/ciam-api -n forgerock --since=5m --timestamps
```

## 6. Configurar variable de entorno en macOS

Define la contraseña de administración de AM en la sesión actual de la terminal:

```bash
export AMADMIN_PASSWORD='REEMPLAZAR_POR_LA_CONTRASENA'
source ~/.zshrc
```

## 7. Generar claves RSA para el JWT interno

Crear el directorio de secretos:

```bash
mkdir -p secrets
cd secrets
```

**Clave privada:** IG la utiliza para firmar los JWT con RS256.

```bash
openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out jwt-private.pem
```

**Clave pública:** los backends la utilizan para verificar la firma del JWT.

```bash
openssl pkey \
  -in jwt-private.pem \
  -pubout \
  -out jwt-public.pem
```

**Importante:** proteger la clave privada y no incluirla en Git. La clave pública debe distribuirse de forma confiable a los backends que validan los tokens.
