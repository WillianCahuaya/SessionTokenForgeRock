Sí. La imagen local que creamos fue:

ig-local:2025.9.0

El comando de build fue:

docker build -t ig-local:2025.9.0 docker/ig

Eso usa:

docker/ig/Dockerfile

y construye la imagen a partir de tu configuración local de:

docker/ig/config-profiles/cdk/

Después cargamos esa imagen en KIND willca-local con:

kind load docker-image ig-local:2025.9.0 --name willca-local

Por eso en Kubernetes queríamos cambiar:

newName: us-docker.pkg.dev/forgeops-public/images/ig

por:

newName: ig-local

para que el deployment utilizara nuestra imagen construida localmente.


