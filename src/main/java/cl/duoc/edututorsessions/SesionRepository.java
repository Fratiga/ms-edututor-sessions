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
}
