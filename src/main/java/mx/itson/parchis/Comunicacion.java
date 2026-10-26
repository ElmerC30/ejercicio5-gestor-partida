package mx.itson.parchis;

/**
 * Canal por el que se avisa cada jugada al rival. Quien lo usa no sabe si va por un socket,
 * por otro medio o a un objeto de prueba.
 */
public interface Comunicacion {

    /**
     * Envía un mensaje al rival.
     *
     * @return verdadero si se pudo enviar
     */
    boolean enviar(String mensaje);

    /** A dónde se envían los mensajes, para mostrarlo al usuario. */
    String getDestino();
}
