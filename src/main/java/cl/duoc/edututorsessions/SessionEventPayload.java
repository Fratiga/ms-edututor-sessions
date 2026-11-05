package cl.duoc.edututorsessions;

// Qué pasó con una sesión. estadoAnterior es null cuando la sesión nace.
public record SessionEventPayload(
	Long sessionId,
	String estudianteId,
	Long servicioId,
	String tutorId,
	String estadoAnterior,
	String estadoNuevo
) {
}
