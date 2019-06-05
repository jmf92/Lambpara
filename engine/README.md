# Procesamiento
En esta sección se describe el proceso para generar los jobs de Flink y enviarlos al cluster desplegado en GKE.

## Flink
### Dependencias
El proyecto está construido bajo estas dependencias:  

| Herramienta   | Versión       |
| :---          |   :---:       |
| Java          | 1.8           |
| Maven         | 3.6.0         |
| Apache Flink  | 1.8           |

### Proceso
#### PowerConsumption
Este proceso se encarga de calcular el consumo, tanto energético como económico, de cada sensor. 
Para ello junta las medidas emitidas por los sensores con el precio de la electricidad y produce los mensajes en un topic.

#### PulsarToInflux
Toma los mensajes de un topic, generados por el proceso anterior y los vuelca en InnfluxDB una base de datos basada en series temporales
que nos permitirá hacer agregaciones.

### Ejecución
Primero debemos generar el jar que contendrá los dos jobs.

```

cd /project/root/path

export PROJECT=`pwd`
export JAVA_HOME=/path/to/JDK1.8

# Compilamos los jobs
cd ${PROJECT}/engine/flink/lambpara-flink
mvn clean package

```

Una vez generado el job, deberemos obtener la URL del jobmanager y lanzar cada job:
Para este paso deberemos descargar una distribución en concordancia con la versión desplegada para subir los jobs.

```

# PowerConsumption
export FLINK_DISTRO=/path/to/flink/distro
export JOBMNG=value_from_GCP_console_or_Kubectl
cd ${FLINK_DISTRO}
bin/flink run -m ${JOBMNG}:8081/ -c com.kschool.tfm.engine.flink.PowerConsumption \
                                 ${PROJECT}/engine/flink/lambpara-flink/target/lambpara-flink-1.0-SNAPSHOT.jar \
                                 --pulsar-url pulsar://broker:6650 \
                                 --norm-topic norm \
                                 --price-topic price \
                                 --subscription power_test \
                                 --output-topic power

# PulsarToInflux
bin/flink run -m 35.239.122.248:8081/ -c com.kschool.tfm.engine.flink.PulsarToInflux \
	                           ${PROJECT}/engine/flink/lambpara-flink/target/lambpara-flink-1.0-SNAPSHOT.jar \
                                  --pulsar-url pulsar://broker:6650 \
                                  --input-topic power \
                                  --subscription influx_test \
	                              --influx-url http://influxdb:8086 \
                                  --influx-db lambpara

```

A tener en cuenta:
 * **el valor de pulsar-url e influx-url NO DEBEN MODIFICARSE**, se están ejecutando dentro del cluster de GKE y por tanto eso son los nombres que toman los PODs que ejecutan los procesos.
 * el resto de valores son modificables si los modificáis en los sitios pertinentes:
   * --influx-db en el CREATE DATABASE del fichero [deployInfluxGrafana.sh](/deployment/gke/influx-grafana/deployInfluxGrafana.sh)
   * --norm-topic en la propiedad output del fichero [norm-function.yml](/engine/pulsar/normalization/conf/norm-function.yml)
   * --output-topic (PowerConsumption) cualquier valor recordando que se le debe dar el mismo valor al --input-topic (PulsarToInflux)
 
 
## Pulsar Function
### Dependencias
En esta parte no hay dependencias ya que la función se ejecuta en los brokers y éstos ya tienen instaladas las librerías necesarias. 
Eso si, en el desarrollo, se debe utilizar una [versión](https://pulsar.apache.org/docs/en/client-libraries-python/#installation-using-pipversión) de Python soportada.

### Proceso
Esta función desarollada en Python toma los mensajes del topic raw, los enriquece, los transforma a formato JSON y los vuelca en el topic norm.
El nombre de dichos topics son modificables a través del fichero, [norm-function.yml](/engine/pulsar/normalization/conf/norm-function.yml), mediante las propiedades:
* inputs (topic raw en nuestro caso)
* output (topic norm en nuestro)

### Ejecución
Una vez [desplegada](/deployment/README.md#pulsar), en el cluster de Pulsar, no es necesario ningún paso adicional para ejecutar este proceso.