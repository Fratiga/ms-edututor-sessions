package cl.duoc.edututorsessions;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

// El JsonSerializer por defecto escribe java.time.Instant como epoch numérico
// (WRITE_DATES_AS_TIMESTAMPS, el default de Jackson), no como texto ISO-8601
// como pide el envelope del documento de arquitectura. Se fuerza el formato
// de texto para que timestamp sea legible igual en Kafka UI y en los
// consumidores.
@Configuration
public class KafkaConfig {

	@Bean
	ProducerFactory<String, Object> producerFactory(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
		ObjectMapper mapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		Map<String, Object> config = Map.of(
			ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
			ProducerConfig.ACKS_CONFIG, "all",
			ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

		return new DefaultKafkaProducerFactory<>(config,
			new org.apache.kafka.common.serialization.StringSerializer(),
			new JsonSerializer<>(mapper));
	}

	@Bean
	KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}
}
