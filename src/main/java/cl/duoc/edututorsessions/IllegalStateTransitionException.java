package cl.duoc.edututorsessions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class IllegalStateTransitionException extends RuntimeException {

	public IllegalStateTransitionException(String message) {
		super(message);
	}

	public IllegalStateTransitionException(EstadoSesion actual, EstadoSesion destino) {
		super("Transición inválida: " + actual + " -> " + destino);
	}
}
