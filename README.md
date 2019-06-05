# Lambpara
Repositorio donde se aloja el código y despliegue de una plataforma para el trabajo Fin de Máster en Arquitecturas Big Data de Kschool

## ¿Qué es Lambpara?
Lambpara es una plataforma para la **monitorización del consumo energético en el hogar**. Ofrece datos de consumo, tanto económico como energético, en "tiempo real" recolectando las medidas eléctricas que envían los sensores conectados del hogar.

Información más detallada en este [enlace](https://app.gitbook.com/@jaime-mf-92/s/workspace/).
En ese enlace se puede encontrar mas información sobre:

* [Arquitectura y componentes](https://app.gitbook.com/@jaime-mf-92/s/workspace/arquitectura)
* [Modelo de datos](https://app.gitbook.com/@jaime-mf-92/s/workspace/modelo-datos)
* [Procesos desarrollados](https://app.gitbook.com/@jaime-mf-92/s/workspace/procesos)



## Infraestructura
### Despliegue
Siguiendo esta [guía](/deployment/README.md) se tendrá desplegado en GKE las siguientes herramientas y componentes.

###Herramientas


* Cluster Pulsar 

| Componente    | Número       |
| :---          |   :---:      |
| Broker        | 3            |
| Bookeeper     | 3            |
| Zookeeper     | 3            |
| Proxy         | 3            |
| Prometheus    | 1            |
| Grafana       | 1            |



* Cluster Flink

| Componente    | Número       |
| :---          |   :---:      |
| JobManager    | 1            |
| TaskManager   | 2            |



* Visualización

| Componente    | Número       |
| :---          |   :---:      |
| InfluxDB      | 1            |
| Grafana       | 1            |

Con la infraestructura ya preparada se puede procede a subir los jobs encargados del procesamiento, mas info [aquí](/engine/README.md).

Por último se puede utilizar esta [guía](/data_acquisition/README.md) para simular sensores que envían datos a la plataforma.



## Agradecimientos
Los datos utilizados para generar las medidas han sido extraídas del dataset:

Makonin, Stephen, 2016, "AMPds2: The Almanac of Minutely Power dataset (Version 2)", https://doi.org/10.7910/DVN/FIE0S4, Harvard Dataverse, V1, UNF:6:0uqZaBkSWdyv27JqTHFWPg== [fileUNF] 