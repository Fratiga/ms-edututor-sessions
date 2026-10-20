package cl.duoc.edututorsessions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// El tutor ya tiene otra sesión asignada que se cruza con el horario pedido.
@ResponseStatus(HttpStatus.CONFLICT)
public class TutorOcupadoException extends RuntimeException {

	public TutorOcupadoException(String mensaje) {
		super(mensaje);
	}
}
