package com.kschool.tfm.engine.flink;

import com.kschool.tfm.engine.flink.model.PowerModel;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBConfig;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBPoint;
import org.apache.flink.streaming.connectors.influxdb.InfluxDBSink;
import org.apache.flink.streaming.connectors.pulsar.PulsarSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class PulsarToInflux {
    private static final Logger LOG = LoggerFactory.getLogger(PulsarToInflux.class);


    public static void main(String[] args) throws Exception {
        // parse input arguments
        final ParameterTool parameterTool = ParameterTool.fromArgs(args);

        if (parameterTool.getNumberOfParameters() < 5) {
            System.out.println("Missing parameters!");
            System.out.println("Usage: PulsarToInflux --pulsar-url <pulsar-service-url> --input-topic <topic> --subscription <pulsar_subscription> --influx-url <influx_url> --influx-db <influx_db>");
            return;
        }

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().disableSysoutLogging();
        env.getConfig().setRestartStrategy(RestartStrategies.fixedDelayRestart(4, 10000));
        env.enableCheckpointing(5000);
        env.getConfig().setGlobalJobParameters(parameterTool);
        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

        String pulsarUrl = parameterTool.getRequired("pulsar-url");
        String inputTopic = parameterTool.getRequired("input-topic");
        String subscription = parameterTool.get("subscription", "flink-pulsar");
        String influxUrl = parameterTool.getRequired("influx-url");
        String influxDB = parameterTool.getRequired("influx-db");

        int parallelism = parameterTool.getInt("parallelism", 1);


        System.out.println("Parameters:");
        System.out.println("\tPulsarUrl:\t" + pulsarUrl);
        System.out.println("\tSubscription:\t" + subscription);
        System.out.println("\tInfluxDB url:\t" + influxUrl);
        System.out.println("\tInfluxDB database:\t" + influxDB);
        System.out.println("\tParallelism:\t" + parallelism);

        PulsarSourceBuilder<String> powerBuilder = PulsarSourceBuilder.builder(new SimpleStringSchema())
                                                                        .serviceUrl(pulsarUrl)
                                                                        .topic(inputTopic)
                                                                        .subscriptionName(subscription);
        SourceFunction<String> src = powerBuilder.build();
        DataStream<String> ds = env.addSource(src);

        ObjectMapper mapper = new ObjectMapper();
        DataStream<PowerModel> pw = ds.map((MapFunction<String, PowerModel>) line -> mapper.readValue(line, PowerModel.class));
        DataStream<InfluxDBPoint> dataStream = pw.map(
                new RichMapFunction<PowerModel, InfluxDBPoint>() {
                    String measurement = "power";

                    @Override
                    public InfluxDBPoint map(PowerModel pm) throws Exception {
                        long timestamp = pm.timestamp*1000;

                        HashMap<String, String> tags = new HashMap<>();
                        tags.put("sensor_id", pm.sensor_id);
                        tags.put("sensor_name", pm.sensor_name);
                        tags.put("room", pm.room);

                        HashMap<String, Object> fields = new HashMap<>();
                        fields.put("V", pm.V);
                        fields.put("I", pm.I);
                        fields.put("Pkw", pm.Pkw);
                        fields.put("power_cons", pm.power_cons);

                        return new InfluxDBPoint(measurement, timestamp, tags, fields);
                    }
                }
        );


        // Configure influx sink and add to stream
        InfluxDBConfig influxDBConfig = InfluxDBConfig.builder(influxUrl, "root", "root", influxDB)
                .batchActions(1000)
                .flushDuration(100, TimeUnit.MILLISECONDS)
                .enableGzip(true)
                .build();
        dataStream.addSink(new InfluxDBSink(influxDBConfig));

        env.execute("Move events from Pulsar topic to InfluxDB database");
    }
}
