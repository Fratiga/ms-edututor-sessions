package cl.duoc.edututorsessions;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

// Publica comandos hacia ms-edututor-notify por RabbitMQ (exchange cmd.direct,
// declarado por rabbitmq-admin). Kafka lleva lo que YA pasó (SessionEvent);
// aquí se pide una acción: avisar al estudiante, mandar el ticket al tutor o
// generar la constancia.
//
// Un fallo del broker no debe tumbar la operación de sesión (ya quedó guardada
// en Oracle), así que solo se registra.
@Component
public class SessionCommandPublisher {

	private static final Logger log = LoggerFactory.getLogger(SessionCommandPublisher.class);

	private final RabbitTemplate rabbitTemplate;
	private final RabbitCommandProperties rabbit;

	public SessionCommandPublisher(RabbitTemplate rabbitTemplate, RabbitCommandProperties rabbit) {
		this.rabbitTemplate = rabbitTemplate;
		this.rabbit = rabbit;
	}

	public void notificarCreada(Sesion sesion) {
		enviar(rabbit.email().directKey(), "SESSION_REQUESTED", sesion, "Recibimos tu solicitud de sesión.");
	}

	public void notificarCambioEstado(Sesion sesion) {
		switch (sesion.getEstado()) {
			case CONFIRMADA -> enviar(rabbit.email().directKey(), "SESSION_CONFIRMED", sesion, "Tu sesión fue confirmada.");
			case ASIGNADA -> enviar(rabbit.session().directKey(), "SESSION_TICKET", sesion, "Se te asignó una sesión.");
			case REALIZADA -> enviar(rabbit.certificate().directKey(), "SESSION_CERTIFICATE", sesion,
				"Sesión realizada: generar constancia.");
			case CANCELADA -> enviar(rabbit.email().directKey(), "SESSION_CANCELLED", sesion, "Tu sesión fue cancelada.");
			default -> {
			}
		}
	}

	private void enviar(String rutaDeEnvio, String tipo, Sesion sesion, String mensaje) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("sessionId", sesion.getId());
		payload.put("estudianteId", sesion.getEstudianteId());
		payload.put("servicioId", sesion.getServicioId());
		payload.put("tutorId", sesion.getTutorId());
		payload.put("estado", sesion.getEstado().name());
		payload.put("mensaje", mensaje);

		ComandoEnvelope comando = new ComandoEnvelope(
			tipo,
			UUID.randomUUID().toString(),
			Instant.now(),
			UUID.randomUUID().toString(),
			String.valueOf(sesion.getId()),
			payload);

		try {
			rabbitTemplate.convertAndSend(rabbit.exchange().direct(), rutaDeEnvio, comando);
		}
		catch (RuntimeException e) {
			log.warn("No se pudo publicar el comando {} ({}) en RabbitMQ: {}", tipo, rutaDeEnvio, e.getMessage());
		}
	}
}
