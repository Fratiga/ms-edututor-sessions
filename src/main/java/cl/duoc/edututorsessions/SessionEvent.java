package cl.duoc.edututorsessions;

import java.time.Instant;
import java.util.Map;

// Mismo envelope común acordado en el documento de arquitectura, aplicado
// esta vez a eventos de dominio (no comandos): type, eventId, timestamp,
// traceId, correlationId, payload.
public record SessionEvent(
	String type,
	String eventId,
	Instant timestamp,
	String traceId,
	String correlationId,
	Map<String, Object> payload
) {
}
