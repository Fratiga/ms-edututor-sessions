package cl.duoc.edututorsessions;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

	@Query("""
		SELECT s FROM Sesion s
		WHERE (:estado IS NULL OR s.estado = :estado)
		AND (:desde IS NULL OR s.fechaSolicitud >= :desde)
		AND (:hasta IS NULL OR s.fechaSolicitud <= :hasta)
		""")
	List<Sesion> buscar(@Param("estado") EstadoSesion estado, @Param("desde") Instant desde, @Param("hasta") Instant hasta);

	// Sesiones de ese tutor que ocupan su agenda (asignadas o en curso) y caen
	// dentro de la ventana de tiempo dada, sin contar la sesión que se asigna.
	@Query("""
		SELECT COUNT(s) FROM Sesion s
		WHERE s.tutorId = :tutorId
		AND s.id <> :idSesion
		AND s.estado IN :estados
		AND s.fechaHora > :desde
		AND s.fechaHora < :hasta
		""")
	long contarConflictos(@Param("tutorId") String tutorId, @Param("idSesion") Long idSesion,
			@Param("estados") List<EstadoSesion> estados, @Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
