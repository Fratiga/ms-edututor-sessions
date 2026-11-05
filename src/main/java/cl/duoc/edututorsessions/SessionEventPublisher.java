package cl.duoc.edututorsessions;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// Publica hacia sessions.events, la fuente de verdad que consumen
// ms-edututor-audit y ms-edututor-report. Partition key = sessionId,
// según el documento: garantiza orden estricto de los eventos de una
// misma sesión dentro de la misma partición.
//
// Simplificación consciente: el documento describe un outbox transaccional
// (fila de sesión + evento en la misma transacción JDBC, publicados después
// por un proceso aparte) para evitar el problema de doble escritura. Aquí
// se publica directamente tras el save() por simplicidad — el riesgo real
// (guardar en Oracle pero no publicar a Kafka, o viceversa) queda abierto.
@Component
public class SessionEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(SessionEventPublisher.class);

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final String topic;

	public SessionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
			@Value("${edututor.kafka.topic.events}") String topic) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
	}

	public void publicarCreada(Sesion sesion) {
		publicar(SessionEvent.CREATED, sesion, null);
	}

	public void publicarCambioEstado(Sesion sesion, EstadoSesion estadoAnterior) {
		publicar(SessionEvent.STATE_CHANGED, sesion, estadoAnterior);
	}

	private void publicar(String type, Sesion sesion, EstadoSesion estadoAnterior) {
		SessionEventPayload payload = new SessionEventPayload(
			sesion.getId(),
			sesion.getEstudianteId(),
			sesion.getServicioId(),
			sesion.getTutorId(),
			estadoAnterior == null ? null : estadoAnterior.name(),
			sesion.getEstado().name());

		SessionEvent evento = new SessionEvent(
			type,
			UUID.randomUUID().toString(),
			Instant.now(),
			UUID.randomUUID().toString(),
			UUID.randomUUID().toString(),
			payload);

		// Partition key = sessionId (como String, para que Kafka lo hashee consistente).
		// Al confirmar el broker se registra dónde quedó el evento (partición y
		// offset): es el "comprobante" del envío y permite rastrearlo después en
		// Kafka UI o en la pantalla de auditoría.
		//
		// Si Kafka no responde, la sesión ya está guardada en Oracle: se deja la
		// advertencia en el log y la petición del usuario no falla ni se cuelga.
		try {
			kafkaTemplate.send(topic, String.valueOf(sesion.getId()), evento).whenComplete((resultado, error) -> {
				if (error != null) {
					log.warn("Evento {} de la sesión {} NO se publicó en {}: {}", type, sesion.getId(), topic,
						error.getMessage());
				}
				else {
					log.info("Evento {} sessionId={} publicado en {} p{} o{}", type, sesion.getId(), topic,
						resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
				}
			});
		}
		catch (RuntimeException e) {
			log.warn("Evento {} de la sesión {} NO se publicó en {}: {}", type, sesion.getId(), topic, e.getMessage());
		}
	}
}
