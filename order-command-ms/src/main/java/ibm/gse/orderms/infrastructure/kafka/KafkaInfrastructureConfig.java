package ibm.gse.orderms.infrastructure.kafka;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class KafkaInfrastructureConfig {

	private static final Logger logger = LoggerFactory.getLogger(KafkaInfrastructureConfig.class.getName());

	private static Config config = ConfigProvider.getConfig();

	private static String ORDER_TOPIC;

	private static String ORDER_COMMAND_TOPIC;

	private static String ERROR_TOPIC;

	public static final long PRODUCER_TIMEOUT_SECS = 10;
	public static final long PRODUCER_CLOSE_TIMEOUT_SEC = 10;
	public static final Duration CONSUMER_POLL_TIMEOUT = Duration.ofSeconds(10);
	public static final Duration CONSUMER_CLOSE_TIMEOUT = Duration.ofSeconds(10);
	public static final long TERMINATION_TIMEOUT_SEC = 10;
	// TODO this is temporary once we use schema registry
	public static final String SCHEMA_VERSION = "1";

	public static String getOrderTopic() {
		ORDER_TOPIC = config.getValue("order.topic", String.class);
		logger.info("Get Order Topic: {}", ORDER_TOPIC);
		return ORDER_TOPIC;
	}

	public static String getOrderCommandTopic() {
		ORDER_COMMAND_TOPIC = config.getValue("ordercommand.topic", String.class);
		logger.info("Get Order Command Topic: {}", ORDER_COMMAND_TOPIC);
		return ORDER_COMMAND_TOPIC;
	}

	public static String getErrorTopic() {
		ERROR_TOPIC = config.getValue("error.topic",  String.class);
		logger.info("Get Error Topic: {}", ERROR_TOPIC);
		return ERROR_TOPIC;
	}

	public static Properties getProducerProperties(String clientId) {
		Properties properties = buildCommonProperties();
		properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
		properties.put(ProducerConfig.ACKS_CONFIG, "1");
		properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
		return properties;
	}

	public static Properties getConsumerProperties(String groupid,String clientid, boolean commit,String offset) {
		Properties properties = buildCommonProperties();
		properties.put(ConsumerConfig.GROUP_ID_CONFIG,  groupid);
		properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, Boolean.toString(commit));
		properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offset);
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.put(ConsumerConfig.CLIENT_ID_CONFIG, clientid);
		return properties;
	}


	/**
	 * Take into account the environment variables if set
	 *
	 * @return common kafka properties
	 */
	private static Properties buildCommonProperties() {
		Properties properties = new Properties();
		Map<String, String> env = System.getenv();

		if (env.get("KAFKA_BROKERS") == null) {
			throw new IllegalStateException("Missing environment variable KAFKA_BROKERS");
		}
		properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, env.get("KAFKA_BROKERS"));

		if (env.get("KAFKA_APIKEY") != null) {
			properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
			properties.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
			properties.put(SaslConfigs.SASL_JAAS_CONFIG,
					"org.apache.kafka.common.security.plain.PlainLoginModule required username=\"token\" password=\""
							+ env.get("KAFKA_APIKEY") + "\";");
			properties.put(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
			properties.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG, "TLSv1.2");
			properties.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "HTTPS");

			if ("true".equals(env.get("TRUSTSTORE_ENABLED"))){
				properties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, env.get("TRUSTSTORE_PATH"));
				properties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, env.get("TRUSTSTORE_PWD"));
			}
		}

		return properties;
	}

}
