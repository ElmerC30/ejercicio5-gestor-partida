package mx.itson.parchis;

/**
 * Las reglas del Parchís. Si cambia una regla, solo cambia esta clase.
 */
public class ReglasParchis {

    /** Casillas del recorrido común. */
    public static final int TOTAL_CASILLAS = 68;

    /** Última posición de una ficha en el recorrido común; después sigue su pasillo. */
    public static final int FIN_RECORRIDO = 66;

    /** Posición de una ficha que llegó a la meta. */
    public static final int META = 74;

    /** Posición de una ficha que está en su base. */
    public static final int EN_BASE = -1;

    /** Fichas de cada jugador. */
    public static final int FICHAS = 4;

    /** Con un 5 sale una ficha de la base. */
    public boolean puedeSalirDeBase(int dado) {
        return dado == 5;
    }

    /** Verdadero si la ficha en {@code posicion} puede moverse con ese dado. */
    public boolean puedeMover(int posicion, int dado) {
        return motivoNoPuedeMover(posicion, dado) == null;
    }

    /** Por qué la ficha no puede moverse, o {@code null} si sí puede. */
    public String motivoNoPuedeMover(int posicion, int dado) {
        if (posicion == META) {
            return "Esa ficha ya llegó a la meta.";
        }
        if (posicion == EN_BASE && !puedeSalirDeBase(dado)) {
            return "Para sacar una ficha de la base necesitas un 5.";
        }
        if (posicion >= 0 && posicion + dado > META) {
            return "Esa ficha necesita exactamente un " + (META - posicion) + " para llegar a la meta.";
        }
        return null;
    }

    /** Verdadero si el jugador puede mover al menos una ficha con ese dado. */
    public boolean tieneMovimiento(Jugador jugador, int dado) {
        for (int posicion : jugador.getFichas()) {
            if (puedeMover(posicion, dado)) {
                return true;
            }
        }
        return false;
    }

    /** Posición a la que llega una ficha al moverse con ese dado. */
    public int nuevaPosicion(int posicion, int dado) {
        return posicion == EN_BASE ? 0 : posicion + dado;
    }

    /** Con 6 se vuelve a tirar, máximo tres veces seguidas. */
    public boolean vuelveATirar(int dado, int seisSeguidos) {
        return dado == 6 && seisSeguidos < 3;
    }

    /** Verdadero si la ficha está en el recorrido común, donde puede capturar y ser capturada. */
    public boolean estaEnRecorrido(int posicion) {
        return posicion >= 0 && posicion <= FIN_RECORRIDO;
    }

    /** Casilla del recorrido (0 a 67) donde está una ficha de ese color. */
    public int casillaAbsoluta(ColorFicha color, int posicion) {
        return (color.getCasillaSalida() + posicion) % TOTAL_CASILLAS;
    }

    /** Gana quien lleva sus cuatro fichas a la meta. */
    public boolean haGanado(Jugador jugador) {
        for (int posicion : jugador.getFichas()) {
            if (posicion != META) {
                return false;
            }
        }
        return true;
    }
}
