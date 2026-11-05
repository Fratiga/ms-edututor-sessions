package cl.duoc.edututorsessions;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

// Toda la configuración del cliente (bootstrap, acks, idempotencia,
// compresión, timeouts) vive en application.properties bajo spring.kafka.*.
// Aquí solo se agrega lo que no se puede expresar en propiedades: el serializer
// JSON.
//
// El JsonSerializer por defecto escribe java.time.Instant como epoch numérico
// (WRITE_DATES_AS_TIMESTAMPS, el default de Jackson), no como texto ISO-8601
// como pide el envelope del documento de arquitectura; se fuerza el texto para
// que timestamp sea legible igual en Kafka UI y en los consumidores. Tampoco
// se envía el header con el nombre de la clase Java del productor: los
// consumidores son servicios independientes y deserializan a su propio DTO.
@Configuration
public class KafkaConfig {

	@Bean
	ProducerFactory<String, Object> producerFactory(KafkaProperties propiedades) {
		ObjectMapper mapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		JsonSerializer<Object> valor = new JsonSerializer<>(mapper);
		valor.setAddTypeInfo(false);

		Map<String, Object> config = propiedades.buildProducerProperties();
		return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), valor);
	}

	@Bean
	KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
		return new KafkaTemplate<>(producerFactory);
	}
}
