package mx.itson.parchis;

import java.util.Arrays;

/**
 * Un jugador de la partida: su nombre, su color y la posición de sus cuatro fichas.
 *
 * El color se recibe como objeto en lugar de crear una subclase por cada color.
 */
public class Jugador {

    private final String nombre;
    private final ColorFicha color;

    /**
     * Posición de cada ficha: -1 en la base, de 0 a 66 en el recorrido, de 67 a 73 en el
     * pasillo de su color y 74 en la meta.
     */
    private final int[] fichas = new int[ReglasParchis.FICHAS];

    public Jugador(String nombre, ColorFicha color) {
        this.nombre = nombre;
        this.color = color;
        reiniciarFichas();
    }

    public String getNombre() {
        return nombre;
    }

    public ColorFicha getColor() {
        return color;
    }

    public int[] getFichas() {
        return fichas;
    }

    /** La casilla del recorrido donde salen sus fichas de la base. */
    public int getCasillaSalida() {
        return color.getCasillaSalida();
    }

    /** Regresa las cuatro fichas a la base. */
    public void reiniciarFichas() {
        Arrays.fill(fichas, ReglasParchis.EN_BASE);
    }
}
