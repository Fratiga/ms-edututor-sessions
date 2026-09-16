package cl.duoc.edututorsessions;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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

	private static final String TOPIC = "sessions.events";

	private final KafkaTemplate<String, Object> kafkaTemplate;

	public SessionEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void publicarCreada(Sesion sesion) {
		publicar("SESSION_CREATED", sesion, null);
	}

	public void publicarCambioEstado(Sesion sesion, EstadoSesion estadoAnterior) {
		publicar("SESSION_STATE_CHANGED", sesion, estadoAnterior);
	}

	private void publicar(String type, Sesion sesion, EstadoSesion estadoAnterior) {
		Map<String, Object> payload = new java.util.HashMap<>();
		payload.put("sessionId", sesion.getId());
		payload.put("estudianteId", sesion.getEstudianteId());
		payload.put("servicioId", sesion.getServicioId());
		payload.put("tutorId", sesion.getTutorId());
		payload.put("estadoNuevo", sesion.getEstado().name());
		if (estadoAnterior != null) {
			payload.put("estadoAnterior", estadoAnterior.name());
		}

		SessionEvent evento = new SessionEvent(
			type,
			UUID.randomUUID().toString(),
			Instant.now(),
			UUID.randomUUID().toString(),
			UUID.randomUUID().toString(),
			payload);

		// Partition key = sessionId (como String, para que Kafka lo hashee consistente).
		kafkaTemplate.send(TOPIC, String.valueOf(sesion.getId()), evento);
	}
}
