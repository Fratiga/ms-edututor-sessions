package cl.duoc.edututorsessions;

import java.time.Instant;

// Mismo envelope común acordado en el documento de arquitectura, aplicado
// esta vez a eventos de dominio (no comandos): type, eventId, timestamp,
// traceId, correlationId, payload. El payload es un DTO tipado: el formato JSON
// es el contrato con los consumidores (audit y report).
public record SessionEvent(
	String type,
	String eventId,
	Instant timestamp,
	String traceId,
	String correlationId,
	SessionEventPayload payload
) {

	public static final String CREATED = "SESSION_CREATED";
	public static final String STATE_CHANGED = "SESSION_STATE_CHANGED";
}
