package cl.duoc.edututorsessions;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Sesion {

	// SOLICITADA → CONFIRMADA → ASIGNADA → EN_CURSO → REALIZADA
	//                                   ↘ CANCELADA (desde cualquier estado previo a EN_CURSO)
	private static final Map<EstadoSesion, Set<EstadoSesion>> TRANSICIONES_VALIDAS = new EnumMap<>(EstadoSesion.class);
	static {
		TRANSICIONES_VALIDAS.put(EstadoSesion.SOLICITADA, EnumSet.of(EstadoSesion.CONFIRMADA, EstadoSesion.CANCELADA));
		TRANSICIONES_VALIDAS.put(EstadoSesion.CONFIRMADA, EnumSet.of(EstadoSesion.ASIGNADA, EstadoSesion.CANCELADA));
		TRANSICIONES_VALIDAS.put(EstadoSesion.ASIGNADA, EnumSet.of(EstadoSesion.EN_CURSO, EstadoSesion.CANCELADA));
		TRANSICIONES_VALIDAS.put(EstadoSesion.EN_CURSO, EnumSet.of(EstadoSesion.REALIZADA));
		TRANSICIONES_VALIDAS.put(EstadoSesion.REALIZADA, EnumSet.noneOf(EstadoSesion.class));
		TRANSICIONES_VALIDAS.put(EstadoSesion.CANCELADA, EnumSet.noneOf(EstadoSesion.class));
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String estudianteId;

	private Long servicioId;

	private String tutorId;

	@Enumerated(EnumType.STRING)
	private EstadoSesion estado;

	private Instant fechaSolicitud;

	// Complementarios: el frontend permite elegir un horario preferido y dejar
	// notas al solicitar, sin alterar la máquina de estados original.
	private Instant fechaHora;

	private String observaciones;

	protected Sesion() {
	}

	public Sesion(String estudianteId, Long servicioId) {
		this.estudianteId = estudianteId;
		this.servicioId = servicioId;
		this.estado = EstadoSesion.SOLICITADA;
		this.fechaSolicitud = Instant.now();
	}

	// Regla de invariante: una sesión no puede pasar a EN_CURSO sin tutor
	// asignado, y ninguna transición fuera del mapa de arriba es válida.
	public void transitionTo(EstadoSesion destino) {
		if (destino == EstadoSesion.EN_CURSO && this.tutorId == null) {
			throw new IllegalStateTransitionException(
				"No se puede iniciar una sesión sin tutor asignado: estado actual=" + this.estado);
		}
		if (!TRANSICIONES_VALIDAS.get(this.estado).contains(destino)) {
			throw new IllegalStateTransitionException(this.estado, destino);
		}
		this.estado = destino;
	}

	public void asignarTutor(String tutorId) {
		this.tutorId = tutorId;
	}

	public Long getId() {
		return id;
	}

	public String getEstudianteId() {
		return estudianteId;
	}

	public Long getServicioId() {
		return servicioId;
	}

	public String getTutorId() {
		return tutorId;
	}

	public EstadoSesion getEstado() {
		return estado;
	}

	public Instant getFechaSolicitud() {
		return fechaSolicitud;
	}

	public Instant getFechaHora() {
		return fechaHora;
	}

	public void setFechaHora(Instant fechaHora) {
		this.fechaHora = fechaHora;
	}

	public String getObservaciones() {
		return observaciones;
	}

	public void setObservaciones(String observaciones) {
		this.observaciones = observaciones;
	}
}
