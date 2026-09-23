package cl.duoc.edututorsessions;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/sessions")
public class SesionController {

	private final SesionRepository repository;
	private final SessionEventPublisher eventPublisher;

	public SesionController(SesionRepository repository, SessionEventPublisher eventPublisher) {
		this.repository = repository;
		this.eventPublisher = eventPublisher;
	}

	// GET /api/sessions?status=...&from=...&to=...
	@GetMapping
	public List<SesionResponse> listar(
			@RequestParam(required = false) EstadoSesion status,
			@RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to) {
		return repository.buscar(status, from, to).stream().map(SesionResponse::from).toList();
	}

	// GET /api/sessions/{id}
	@GetMapping("/{id}")
	public SesionResponse obtener(@PathVariable Long id) {
		return SesionResponse.from(buscarOFallar(id));
	}

	// POST /api/sessions (crear sesión)
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SesionResponse crear(@Valid @RequestBody CrearSesionRequest req) {
		Sesion sesion = new Sesion(req.estudianteId(), req.servicioId());
		sesion.setFechaHora(req.fechaHora());
		sesion.setObservaciones(req.observaciones());
		// El tutor preferido queda registrado, pero la sesión sigue naciendo
		// SOLICITADA — la máquina de estados no cambia, solo queda "pre-asignado".
		if (req.tutorId() != null) {
			sesion.asignarTutor(req.tutorId());
		}
		sesion = repository.save(sesion);
		eventPublisher.publicarCreada(sesion);
		return SesionResponse.from(sesion);
	}

	// PUT /api/sessions/{id}/status
	@PutMapping("/{id}/status")
	public SesionResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest req) {
		Sesion sesion = buscarOFallar(id);
		EstadoSesion estadoAnterior = sesion.getEstado();
		if (req.status() == EstadoSesion.ASIGNADA && req.tutorId() != null) {
			sesion.asignarTutor(req.tutorId());
		}
		sesion.transitionTo(req.status());
		sesion = repository.save(sesion);
		eventPublisher.publicarCambioEstado(sesion, estadoAnterior);
		return SesionResponse.from(sesion);
	}

	private Sesion buscarOFallar(Long id) {
		return repository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesión no encontrada: " + id));
	}

	public record CrearSesionRequest(@NotNull String estudianteId, @NotNull Long servicioId, String tutorId,
			Instant fechaHora, String observaciones) {
	}

	public record CambiarEstadoRequest(@NotNull EstadoSesion status, String tutorId) {
	}

	// DTO externo: id/servicioId como String para el contrato del frontend
	// (tutorNombre/servicioNombre no viajan aca a proposito — este servicio no
	// conoce el catalogo ni un directorio de tutores; el frontend los resuelve
	// contra los datos que ya tiene de /api/catalog/services).
	public record SesionResponse(String id, String servicioId, String estudianteId, String tutorId,
			EstadoSesion estado, Instant fechaHora, Instant fechaSolicitud, String observaciones) {
		static SesionResponse from(Sesion s) {
			return new SesionResponse(String.valueOf(s.getId()), String.valueOf(s.getServicioId()),
				s.getEstudianteId(), s.getTutorId(), s.getEstado(), s.getFechaHora(), s.getFechaSolicitud(),
				s.getObservaciones());
		}
	}
}
