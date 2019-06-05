# Despliegue
Tras seguir los pasos de esta guía tendremos todos los componentes (Pulsar, Flink, InfluxDB y Grafana), que componen la plataforma, desplegados en GKE.


## Prerrequisitos
Partimos de que para desplegar los componentes en GKE es necesario tener una **cuenta en GCP**. Además es necesario tener las siguientes herramientas con las versiones que se indican:

| Herramienta   | Versión       |
| :---          |   :---:       |
| gcloud SDK    | 245.0.0       |
| Kubectl       | 1.14.1        |
| Helm          | 3.0.0-alpha   |

Y a recordar que las versiones de los diferentes componentes a desplegar son:

| Componente    | Versión       |
| :---          |   :---:       |
| Kubernetes(GKE)| 1.13         |
| Apache Flink  | 1.8           |
| Apache Pulsar | 2.3.1         |
| InfluxDB      | 1.7.3         |
| Grafana       | 6.2.0         |


## Instalación
Una vez creado el proyecto en GCP y situados en el directorio donde se ha clonado el poyecto deberemos ejecutar los siguientes comandos:
En mi caso el proyecto creado es **lambpara** (**RECORDATORIO**: este es el id del proyecto dentro de la consola de GCP)

```
# Iniciamos y configuramos el proyecto mediante gcloud
export PROJECT=`pwd`
cd ${PROJECT}
gcloud init
gcloud config set project lambpara

# Creamos el cluster
gcloud container clusters create lambpara-gke-cluster \
                --zone=us-central1-a \
		--num-nodes=1 \
                --cluster-version=1.13

```

Con los comandos anteriores se ha creado un cluster, **lambpara-gke-cluster**, con un pool de nodos de tamaño 1.
Una vez aquí, nos conectaremos a la consola de GCP y eliminaremos el pool creado por defecto. Una vez realizado ese paso, empezados a desplegar los diferentes componentes.

### Pulsar
 ```
 # Creamos el pool con los nodos necesarios para Pulsar
 gcloud container node-pools create pulsar-pool \
                 --cluster lambpara-gke-cluster \
                 --zone=us-central1-a \
                 --machine-type=n1-standard-2 \
                 --num-nodes=3 \
                 --local-ssd-count=1
 
 cd ${PROJECT}/deployment/gke/pulsar/
 
 # Zookeeper (En este paso deberemos esperar a que el ensemble de ZK se termine de crear)
 kubectl apply -f zookeeper.yaml
 
 # Initialize metadata
 kubectl apply -f cluster-metadata.yaml
 
 # Bookeeper
 kubectl apply -f bookie.yaml
 
 # Broker
 kubectl apply -f broker.yaml
 
 # Crear el namespace functions para poder utilizar la Pulsar Function que normaliza los datos
 alias pulsar-admin='kubectl exec pulsar-admin -it -- bin/pulsar-admin'
 pulsar-admin namespaces create public/functions
 
 # Re-desplegar brooker service
 kubectl apply -f broker_functions.yaml
 
 # Proxy
 kubectl apply -f proxy.yaml
 
 # Monitoring
 kubectl apply -f monitoring.yaml
 
 # Desplegar pulsar function
 #   Copiamos la function y su configuración en la raiz del contenedor admin
 kubectl cp ${PROJECT}/engine/pulsar/normalization/ default/pulsar-admin:/
 
 #   Creamos la función
 pulsar-admin functions create --functionConfigFile /normalization/conf/norm-function.yml --py /normalization/norm.py --logTopic persistent://public/default/norm-log
              
 ```

¡Listo! Ya tenemos nuestro primer componente listo para recibir mensajes


### Flink

Procedamos con nuestro siguiente componente, el motor de procesamiento.
Nuevamente se muestra paso a paso lo que se debe ejecutar en la terminal:
```
cd ${PROJECT}/deployment/gke/flink/

# JobManager
kubectl apply -f jobmanager-service.yaml
kubectl apply -f jobmanager-deployment.yaml

# TasksManager
kubectl apply -f taskmanager-deployment.yaml
```

### InfluxDB y Grafana
En este caso nos apoyaremos en un script que nos desplegará los componentes con la herramienta Helm y además nos creará la base de datos, **lambpara**, en InfluxDB, nos conectará el POD de grafana con nuestro local para poder conectarnos a la interfaz y nos proporcionará las credenciales para autenticarnos en ésta.

```
cd ${PROJECT}/deployment/gke/influx-grafana

# Creamos el pool influx-grafana
gcloud container node-pools create influx-grafana-pool \
                --cluster lambpara-gke-cluster \
                --zone=us-central1-a \
                --machine-type=n1-standard-1 \
                --num-nodes=2
                
# Ejecutamos el script de despliegue
./deployInfluxGrafana.sh
```


Et voila! Ya tenemos nuestra plataforma desplegada en GKE. 
Ya sólo quedaría [nutrir la plataforma de datos](/data_acquisition/README.md) y [subir los jobs de Flink encargados del procesamiento](/engine/README.md).
