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

	public SesionController(SesionRepository repository) {
		this.repository = repository;
	}

	// GET /api/sessions?status=...&from=...&to=...
	@GetMapping
	public List<Sesion> listar(
			@RequestParam(required = false) EstadoSesion status,
			@RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to) {
		return repository.buscar(status, from, to);
	}

	// GET /api/sessions/{id}
	@GetMapping("/{id}")
	public Sesion obtener(@PathVariable Long id) {
		return buscarOFallar(id);
	}

	// POST /api/sessions (crear sesión)
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Sesion crear(@Valid @RequestBody CrearSesionRequest req) {
		return repository.save(new Sesion(req.estudianteId(), req.servicioId()));
	}

	// PUT /api/sessions/{id}/status
	@PutMapping("/{id}/status")
	public Sesion cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest req) {
		Sesion sesion = buscarOFallar(id);
		if (req.status() == EstadoSesion.ASIGNADA && req.tutorId() != null) {
			sesion.asignarTutor(req.tutorId());
		}
		sesion.transitionTo(req.status());
		return repository.save(sesion);
	}

	private Sesion buscarOFallar(Long id) {
		return repository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesión no encontrada: " + id));
	}

	public record CrearSesionRequest(@NotNull String estudianteId, @NotNull Long servicioId) {
	}

	public record CambiarEstadoRequest(@NotNull EstadoSesion status, String tutorId) {
	}
}
