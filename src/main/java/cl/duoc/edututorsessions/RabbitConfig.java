package cl.duoc.edututorsessions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

	// Spring Boot usa este bean en el RabbitTemplate. Instant como texto
	// ISO-8601, igual que en los eventos de Kafka.
	@Bean
	MessageConverter jsonMessageConverter() {
		ObjectMapper mapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return new Jackson2JsonMessageConverter(mapper);
	}
}
