#!/usr/bin/env bash

# 1) Deploy influxDB and create database lambpara
helm install --namespace "influxdb" "influxdb" stable/influxdb && \
export INFLUX_POD=$(kubectl get pods --namespace default -l app=influxdb -o jsonpath="{ .items[0].metadata.name }") && \
kubectl exec -it $INFLUX_POD --namespace default -- influx -execute "CREATE DATABASE lambpara" -host localhost -port 8086


# 2) Deploy grafana
# and forward traffic from localhost:3000 to POD_GRAFANA:3000
# this allow connection to grafana from localhost
helm install --namespace "grafana" "grafana" stable/grafana && \
export GRAFANA_POD=$(kubectl get pods --namespace default -l "app=grafana,release=grafana" -o jsonpath="{.items[0].metadata.name}") && \
echo "Grafana user:" && \
kubectl get secret --namespace default grafana -o jsonpath="{.data.admin-user}" | base64 --decode ; echo && \
echo "Grafana password:" && \
kubectl get secret --namespace default grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo && \
kubectl --namespace default port-forward $GRAFANA_POD 3000:3000