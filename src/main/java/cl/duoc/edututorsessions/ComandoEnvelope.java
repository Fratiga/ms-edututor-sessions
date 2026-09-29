package cl.duoc.edututorsessions;

import java.time.Instant;
import java.util.Map;

// Mismo envelope que espera ms-edututor-notify (ComandoEnvelope): los campos
// deben coincidir porque se serializa a JSON y el consumidor lo deserializa
// por nombre.
public record ComandoEnvelope(
	String type,
	String eventId,
	Instant timestamp,
	String traceId,
	String correlationId,
	Map<String, Object> payload
) {
}
