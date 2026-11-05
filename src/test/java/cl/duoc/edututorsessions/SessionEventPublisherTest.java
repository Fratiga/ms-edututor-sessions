package cl.duoc.edututorsessions;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

// El productor debe usar el id de la sesión como key (mismo id, misma
// partición, orden garantizado) y no tumbar la operación si Kafka falla.
class SessionEventPublisherTest {

	@SuppressWarnings("unchecked")
	private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
	private final SessionEventPublisher publisher = new SessionEventPublisher(kafka);

	@Test
	void usaElIdDeLaSesionComoKey() {
		Sesion sesion = new Sesion("STU-204", 1L);
		when(kafka.send(eq("sessions.events"), any(String.class), any())).thenReturn(new CompletableFuture<>());

		publisher.publicarCreada(sesion);

		verify(kafka).send(eq("sessions.events"), eq(String.valueOf(sesion.getId())), any());
	}

	@Test
	void unEnvioFallidoNoLanzaExcepcion() {
		Sesion sesion = new Sesion("STU-204", 1L);
		CompletableFuture<org.springframework.kafka.support.SendResult<String, Object>> fallido = new CompletableFuture<>();
		fallido.completeExceptionally(new RuntimeException("broker caido"));
		when(kafka.send(eq("sessions.events"), any(String.class), any())).thenReturn(fallido);

		assertThatCode(() -> publisher.publicarCambioEstado(sesion, EstadoSesion.SOLICITADA)).doesNotThrowAnyException();
	}

	@Test
	void unKafkaQueNoRespondeNoTumbaLaOperacion() {
		Sesion sesion = new Sesion("STU-204", 1L);
		when(kafka.send(eq("sessions.events"), any(String.class), any()))
			.thenThrow(new org.apache.kafka.common.errors.TimeoutException("metadata no disponible"));

		assertThatCode(() -> publisher.publicarCreada(sesion)).doesNotThrowAnyException();
	}
}
