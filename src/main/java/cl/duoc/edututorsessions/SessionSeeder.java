package cl.duoc.edututorsessions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Solo corre si la tabla está vacía (no duplica en reinicios normales).
// servicioId 1..5 asume que ms-edututor-catalog sembró sus 5 servicios en el
// mismo orden (CatalogSeeder) — son bases de datos separadas, así que esto es
// una suposición razonable para un volumen nuevo, no una relación garantizada
// por FK entre microservicios.
@Component
@Order(10)
public class SessionSeeder implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(SessionSeeder.class);

	private final SesionRepository repository;
	private final SessionEventPublisher eventPublisher;
	private boolean kafkaDisponible = true;

	public SessionSeeder(SesionRepository repository, SessionEventPublisher eventPublisher) {
		this.repository = repository;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void run(String... args) {
		if (repository.count() > 0) {
			return;
		}

		crear("STU-204", 1L, "T-18", EstadoSesion.ASIGNADA, en(0, 10, 30), "Repasar regla de la cadena antes del parcial.");
		crear("STU-118", 2L, "T-09", EstadoSesion.EN_CURSO, en(0, 13, 0), "Simulación de entrevista laboral.");
		crear("STU-311", 3L, "T-23", EstadoSesion.REALIZADA, en(-1, 17, 0), "Excelente progreso en nomenclatura.");
		crear("STU-087", 4L, "T-41", EstadoSesion.REALIZADA, en(-1, 9, 0), "Entregó avance del proyecto.");
		crear("STU-191", 5L, "T-33", EstadoSesion.CONFIRMADA, en(1, 16, 0), "Primera revisión del ensayo.");
		crear("STU-142", 1L, "T-18", EstadoSesion.CANCELADA, en(-2, 11, 0), "Reprogramar por examen universitario.");
		crear("STU-263", 2L, null, EstadoSesion.SOLICITADA, en(2, 18, 0), "Foco: presentaciones y networking.");

		log.info("Sesiones sembradas: {}", repository.count());
	}

	private Instant en(int diasDesdeHoy, int hora, int minuto) {
		return Instant.now().plus(diasDesdeHoy, ChronoUnit.DAYS)
			.truncatedTo(ChronoUnit.DAYS)
			.plus(hora, ChronoUnit.HOURS)
			.plus(minuto, ChronoUnit.MINUTES);
	}

	// Recorre la máquina de estados real (no la salta) y publica los mismos
	// eventos que publicaría un uso real, para que Auditoría/Reportes vean
	// datos consistentes si Kafka está disponible cuando esto corre.
	private void crear(String estudianteId, Long servicioId, String tutorId, EstadoSesion destino,
			Instant fechaHora, String observaciones) {
		Sesion nueva = new Sesion(estudianteId, servicioId);
		nueva.setFechaHora(fechaHora);
		nueva.setObservaciones(observaciones);
		final Sesion sesion = repository.save(nueva);
		publicar(() -> eventPublisher.publicarCreada(sesion));

		if (destino == EstadoSesion.SOLICITADA) {
			return;
		}
		if (destino == EstadoSesion.CANCELADA) {
			avanzar(sesion, EstadoSesion.CANCELADA);
			return;
		}

		avanzar(sesion, EstadoSesion.CONFIRMADA);
		if (destino == EstadoSesion.CONFIRMADA) {
			return;
		}
		sesion.asignarTutor(tutorId);
		avanzar(sesion, EstadoSesion.ASIGNADA);
		if (destino == EstadoSesion.ASIGNADA) {
			return;
		}
		avanzar(sesion, EstadoSesion.EN_CURSO);
		if (destino == EstadoSesion.EN_CURSO) {
			return;
		}
		avanzar(sesion, EstadoSesion.REALIZADA);
	}

	private void avanzar(Sesion sesion, EstadoSesion destino) {
		EstadoSesion anterior = sesion.getEstado();
		sesion.transitionTo(destino);
		repository.save(sesion);
		publicar(() -> eventPublisher.publicarCambioEstado(sesion, anterior));
	}

	// Kafka caído no debe impedir que el servicio arranque: send() bloquea hasta
	// 60 s esperando metadata y luego lanza. Tras el primer fallo se deja de
	// publicar en el resto de la siembra (los datos igual quedan en Oracle).
	private void publicar(Runnable envio) {
		if (!kafkaDisponible) {
			return;
		}
		try {
			envio.run();
		}
		catch (RuntimeException e) {
			kafkaDisponible = false;
			log.warn("Kafka no disponible durante la siembra; se omiten los eventos ({})", e.getMessage());
		}
	}
}
