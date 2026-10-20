package cl.duoc.edututorsessions;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Nombres de la mensajería de comandos, leídos de application.properties
// (prefijo edututor.rabbit). El publicador no escribe ningún nombre a mano.
@ConfigurationProperties(prefix = "edututor.rabbit")
public record RabbitCommandProperties(Exchange exchange, Route email, Route session, Route certificate) {

	public record Exchange(String direct) {
	}

	public record Route(String directKey) {
	}
}
