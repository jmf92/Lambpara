# Emulación

En este apartado se muestra como emular unos sensores que emitan medidas de variables eléctricas al cluster de Pulsar desplegado en GKE.


## Versiones
| Herramienta   | Versión       |
| :---          |   :---:       |
| Docker        | 18.09.6       |
| Docker-compose| 1.24.0        |


## Configuración
En este apartado es necesario modificar en los dockerfiles [sensor_dockerfile](/data_acquisition/data_generator/docker/sensor_dockerfile) y [priceGenerator_dockerfile](/data_acquisition/data_generator/docker/priceGenerator_dockerfile)  los siguientes parámetros:
* pulsarIP debemos configurar la IP del proxy expuesta al exterior(mediante la consola de GCP o kubectl)
* `[OPCIONAL`]pulsarTopic 

## Proceso
Ejecutando los comandos siguientes tendremos los diferentes sensores emulando una situación real

```

cd ${PROJECT}/data_acquisition/data_generator/docker
docker-compose up --build

```