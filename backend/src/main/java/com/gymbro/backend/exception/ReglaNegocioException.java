package com.gymbro.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando una operacion viola una regla de negocio del sistema.
 *
 * Por ejemplo: intentar eliminar un usuario que ya fue eliminado, o
 * asignar una rutina a un socio suspendido.
 *
 * @ResponseStatus(BAD_REQUEST) hace que Spring responda automaticamente
 * con codigo HTTP 400 cuando esta excepcion sale de un controlador sin
 * ser atrapada. Es el codigo que corresponde: la peticion estaba mal
 * formulada segun las reglas del negocio, no fallo el servidor.
 *
 * Hereda de RuntimeException y no de Exception. Eso significa que Java
 * no obliga a declararla ni a envolver cada llamada en try/catch: se
 * propaga sola hasta el controlador, que es donde se convierte en
 * respuesta HTTP. Asi la logica de negocio queda limpia de manejo de
 * errores repetitivo.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    /**
     * Construye el mensaje con un formato uniforme que identifica
     * claramente que regla se incumplio.
     *
     * Ejemplo de salida:
     * Regla de negocio INCUMPLIDA [Usuario ya eliminado]: el usuario
     * con ID 4 ya se encuentra inactivo.
     */
    public static ReglaNegocioException de(String regla, String detalle) {
        return new ReglaNegocioException(
                "Regla de negocio INCUMPLIDA [" + regla + "]: " + detalle);
    }
}