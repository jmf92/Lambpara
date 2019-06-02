package com.kschool.tfm.engine.flink;

import static java.nio.charset.StandardCharsets.UTF_8;
import com.kschool.tfm.engine.flink.model.NormModel;
import com.kschool.tfm.engine.flink.model.PowerModel;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoMapFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.pulsar.FlinkPulsarProducer;
import org.apache.flink.streaming.connectors.pulsar.PulsarSourceBuilder;
import java.util.Objects;


/**
 * Implements a streaming program that
 * calculates power consumption for each sensor (Energy and expenses).
 *
 * Example usage:
 *   --service-url pulsar://localhost:6650 --norm-topic norm --price-topic price --output-topic out --subscription sub_test
 */
public class PowerConsumption {

	public static void main(String[] args) throws Exception {
		// parse input arguments
		final ParameterTool parameterTool = ParameterTool.fromArgs(args);

		if (parameterTool.getNumberOfParameters() < 4) {
			System.out.println("Missing parameters!");
			System.out.println("Usage: PowerConsumption --pulsar-url <pulsar-service-url> --norm-topic <topic> --price-topic <topic> --output-topic <topic> --subscription <pulsar_subscription>");
			return;
		}

		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.getConfig().disableSysoutLogging();
		env.getConfig().setRestartStrategy(RestartStrategies.fixedDelayRestart(4, 10000));
		env.enableCheckpointing(5000);
		env.getConfig().setGlobalJobParameters(parameterTool);
		env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

		String pulsarUrl = parameterTool.getRequired("pulsar-url");
		String normTopic = parameterTool.getRequired("norm-topic");
		String priceTopic = parameterTool.getRequired("price-topic");
		String subscription = parameterTool.get("subscription", "flink-pulsar");
		String outputTopic = parameterTool.getRequired("output-topic");
		int parallelism = parameterTool.getInt("parallelism", 1);


		System.out.println("Parameters:");
		System.out.println("\tPulasrUrl:\t" + pulsarUrl);
		System.out.println("\tNormTopic:\t" + normTopic);
		System.out.println("\tPriceTopic:\t" + priceTopic);
		System.out.println("\tSubscription:\t" + subscription);
		System.out.println("\tOutputTopic:\t" + outputTopic);
		System.out.println("\tParallelism:\t" + parallelism);

		// Pulsar source for topic: price
		PulsarSourceBuilder<String> priceBuilder = PulsarSourceBuilder.builder(new SimpleStringSchema())
				.serviceUrl(pulsarUrl)
				.topic(priceTopic)
				.subscriptionName(subscription);
		SourceFunction<String> srcPrice = priceBuilder.build();
		DataStream<String> price = env.addSource(srcPrice);

		// Pulsar source for topic: norm
		PulsarSourceBuilder<String> normBuilder = PulsarSourceBuilder.builder(new SimpleStringSchema())
				.serviceUrl(pulsarUrl)
				.topic(normTopic)
				.subscriptionName(subscription);
		SourceFunction<String> srcBuilder = normBuilder.build();
		DataStream<String> norm = env.addSource(srcBuilder);

		//Convert input JSON string to POJO for proccess it
		ObjectMapper mapper = new ObjectMapper();
		DataStream<NormModel> normToObject = norm.map((MapFunction<String, NormModel>) line -> mapper.readValue(line, NormModel.class));

		// Connect streams norm and price to calculate power consumption (Energy & Expenses)
		DataStream<PowerModel> power = price.broadcast().connect(normToObject).map(new CoMapFunction<String, NormModel, PowerModel>() {
			Double price = 0.0;
			Double Pkw = 0.0;

			// Update "electricity" price each time that message will be produce to price topic
			@Override
			public PowerModel map1(String price) throws Exception {
				this.price = Double.parseDouble(price);
				return null;
			}

			// Generate PowerModel POJO, each time data arrive to norm topic
			@Override
			public PowerModel map2(NormModel norm) throws Exception {
				this.Pkw = (norm.V*norm.I)/1000;
				return new PowerModel(norm.timestamp,
										norm.sensor_id,
										norm.sensor_name, norm.room,
										norm.lat, norm.lon,
										norm.V, norm.I,
										this.Pkw, this.price,
								this.Pkw*this.price/3600);
			}

		})
			.filter(Objects::nonNull)
			.keyBy("sensor_id");

		if (null != outputTopic) {
			power.addSink(new FlinkPulsarProducer<>(
					pulsarUrl,
					outputTopic,
					powerModel -> {
						try {
							// Parsing PowerModel to JSON string
							return mapper.writeValueAsString(powerModel).getBytes(UTF_8);
						} catch (JsonProcessingException e) {
							e.printStackTrace();
							return "{}".getBytes(UTF_8);
						}
					},//normModel.toString().getBytes(UTF_8),
					powerModel -> powerModel.sensor_id
			)).setParallelism(parallelism);
		} else {
			// print the results with a single thread, rather than in parallel
			power.print().setParallelism(1);
		}

		env.execute("Flink job to calculate power consumption (Energy & Expense)");
	}



}


