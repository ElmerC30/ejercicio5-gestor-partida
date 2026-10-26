package mx.itson.parchis;

import java.awt.Color;

/**
 * Los colores que juegan. Todo lo que depende del color (su nombre, cómo se pinta, dónde sale,
 * dónde está su base, su pasillo y su lugar en la meta) vive solo aquí.
 */
public enum ColorFicha {

    ROJO("rojo", new Color(0xC23B2E), 1, 0,
            new int[][] {{9, 8}, {10, 8}, {10, 9}, {9, 9}}) {
        @Override
        public int[] coordenadaPasillo(int i) {
            return new int[] {9, 1 + i};
        }
    },

    AZUL("azul", new Color(0x2563A8), 18, 11,
            new int[][] {{8, 9}, {8, 10}, {9, 10}, {8, 8}}) {
        @Override
        public int[] coordenadaPasillo(int i) {
            return new int[] {1 + i, 9};
        }
    };

    private final String nombre;
    private final Color colorAwt;
    private final int casillaSalida;
    private final int columnaBase;
    private final int[][] lugaresMeta;

    ColorFicha(String nombre, Color colorAwt, int casillaSalida, int columnaBase, int[][] lugaresMeta) {
        this.nombre = nombre;
        this.colorAwt = colorAwt;
        this.casillaSalida = casillaSalida;
        this.columnaBase = columnaBase;
        this.lugaresMeta = lugaresMeta;
    }

    /** El nombre del color, en minúsculas; es el que se muestra, se envía por la red y se guarda. */
    public String getNombre() {
        return nombre;
    }

    public Color getColorAwt() {
        return colorAwt;
    }

    /** El mismo color mezclado con blanco, para las casillas de salida y los pasillos. */
    public Color getColorClaro() {
        return new Color(
                (colorAwt.getRed() + 2 * 255) / 3,
                (colorAwt.getGreen() + 2 * 255) / 3,
                (colorAwt.getBlue() + 2 * 255) / 3);
    }

    /** La casilla del recorrido donde salen sus fichas de la base. */
    public int getCasillaSalida() {
        return casillaSalida;
    }

    /** Columna de la cuadrícula donde empieza el cuadro de su base. */
    public int getColumnaBase() {
        return columnaBase;
    }

    /** Fila y columna que ocupa la ficha {@code numero} cuando llega a la meta. */
    public int[] lugarMeta(int numero) {
        return lugaresMeta[numero];
    }

    /** Fila y columna de la casilla {@code i} (0 a 6) de su pasillo hacia la meta. */
    public abstract int[] coordenadaPasillo(int i);

    /** El color al que le toca después de este. */
    public ColorFicha siguiente() {
        ColorFicha[] colores = values();
        return colores[(ordinal() + 1) % colores.length];
    }

    @Override
    public String toString() {
        return nombre;
    }
}
