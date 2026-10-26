package mx.itson.parchis;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Parchís para dos jugadores, rojo y azul, en una sola computadora.
 *
 * Dibuja el tablero y lleva los turnos. Las reglas las decide {@link ReglasParchis}, cada jugada
 * se avisa al rival por una {@link Comunicacion} y las estadísticas se guardan en MySQL.
 */
public class GestorPartida extends JPanel implements MouseListener, ActionListener {

    // Base de datos
    private static final String URL_BD = "jdbc:mysql://localhost:3306/parchis?createDatabaseIfNotExist=true&connectTimeout=2000";
    private static final String USUARIO_BD = "root";
    private static final String CLAVE_BD = "";

    // Tablero: cuadrícula de 19 x 19 celdas
    private static final int CELDAS = 19;
    private static final int CASILLAS_PASILLO = 7;

    private final ReglasParchis reglas;
    private final Comunicacion comunicacion;

    private final List<Jugador> jugadores = List.of(
            new Jugador("Jugador 1", ColorFicha.ROJO),
            new Jugador("Jugador 2", ColorFicha.AZUL));
    private ColorFicha turno = ColorFicha.ROJO;
    private int dado = 0;
    private boolean yaTiro = false;
    private int seisSeguidos = 0;
    private boolean terminada = false;
    private boolean hayBaseDeDatos = false;
    private boolean avisoSinRed = false;
    private final ArrayList<String> jugadas = new ArrayList<>();
    private final Random azar = new Random();

    private JLabel etiquetaTurno;
    private JLabel etiquetaDado;
    private JLabel etiquetaEstadisticas;
    private JButton botonDado;
    private JButton botonNueva;
    private JTextArea bitacora;

    // Medidas del tablero en pantalla; se calculan al dibujar
    private int tamCelda;
    private int origenX;
    private int origenY;

    public GestorPartida(ReglasParchis reglas, Comunicacion comunicacion) {
        this.reglas = reglas;
        this.comunicacion = comunicacion;
        setPreferredSize(new Dimension(620, 620));
        setBackground(new Color(0xF3F4EF));
        addMouseListener(this);
        crearTablaEstadisticas();
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("espectador")) {
            ComunicacionSocket.escuchar(ComunicacionSocket.PUERTO_POR_DEFECTO);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            GestorPartida juego = new GestorPartida(new ReglasParchis(),
                    new ComunicacionSocket(ComunicacionSocket.HOST_POR_DEFECTO, ComunicacionSocket.PUERTO_POR_DEFECTO));
            JFrame ventana = new JFrame("Parchís");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.add(juego, BorderLayout.CENTER);
            ventana.add(juego.crearPanelLateral(), BorderLayout.EAST);
            ventana.pack();
            ventana.setLocationRelativeTo(null);
            juego.cargarEstadisticas();
            juego.agregarBitacora("Empieza " + juego.jugadorEnTurno().getNombre()
                    + " (" + juego.jugadorEnTurno().getColor() + ").");
            juego.actualizarEtiquetas();
            ventana.setVisible(true);
        });
    }

    /** Arma el panel de la derecha: turno, dado, botones, bitácora y estadísticas. */
    public JPanel crearPanelLateral() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.setPreferredSize(new Dimension(300, 620));

        JPanel arriba = new JPanel(new GridLayout(0, 1, 0, 8));
        JLabel titulo = new JLabel("Parchís");
        titulo.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        etiquetaTurno = new JLabel(" ");
        etiquetaTurno.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        etiquetaDado = new JLabel("Dado: -");
        etiquetaDado.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        botonDado = new JButton("Lanzar dado");
        botonDado.addActionListener(this);
        botonNueva = new JButton("Nueva partida");
        botonNueva.addActionListener(this);
        arriba.add(titulo);
        arriba.add(etiquetaTurno);
        arriba.add(etiquetaDado);
        arriba.add(botonDado);
        arriba.add(botonNueva);
        panel.add(arriba, BorderLayout.NORTH);

        bitacora = new JTextArea();
        bitacora.setEditable(false);
        bitacora.setLineWrap(true);
        bitacora.setWrapStyleWord(true);
        bitacora.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        panel.add(new JScrollPane(bitacora), BorderLayout.CENTER);

        etiquetaEstadisticas = new JLabel(" ");
        etiquetaEstadisticas.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        panel.add(etiquetaEstadisticas, BorderLayout.SOUTH);
        return panel;
    }

    // ------------------------------------------------------------------ jugadas

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == botonDado) {
            lanzarDado();
        } else if (e.getSource() == botonNueva) {
            nuevaPartida();
        }
    }

    private void lanzarDado() {
        if (terminada) {
            JOptionPane.showMessageDialog(this, "La partida terminó. Empieza una nueva.");
            return;
        }
        if (yaTiro) {
            JOptionPane.showMessageDialog(this, "Ya tiraste: elige una ficha.");
            return;
        }
        procesarDado(azar.nextInt(6) + 1);
    }

    /** Aplica el valor del dado al jugador en turno. */
    void procesarDado(int valor) {
        Jugador jugador = jugadorEnTurno();
        dado = valor;
        yaTiro = true;
        if (dado == 6) {
            seisSeguidos++;
        } else {
            seisSeguidos = 0;
        }
        agregarBitacora(jugador.getNombre() + " sacó " + dado + ".");
        enviarAlRival("DADO;" + jugador.getColor() + ";" + dado);

        if (!reglas.tieneMovimiento(jugador, dado)) {
            agregarBitacora("No puede mover ninguna ficha.");
            yaTiro = false;
            terminarTiro();
        }
        actualizarEtiquetas();
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (terminada || !yaTiro) {
            return;
        }
        Jugador jugador = jugadorEnTurno();
        int[] fichas = jugador.getFichas();
        for (int i = 0; i < fichas.length; i++) {
            int[] centro = centroFicha(jugador.getColor(), i, fichas[i]);
            int dx = e.getX() - centro[0];
            int dy = e.getY() - centro[1];
            int radio = tamCelda / 2;
            if (dx * dx + dy * dy <= radio * radio) {
                String motivo = reglas.motivoNoPuedeMover(fichas[i], dado);
                if (motivo != null) {
                    JOptionPane.showMessageDialog(this, motivo);
                    return;
                }
                moverFicha(jugador, i);
                return;
            }
        }
    }

    private void moverFicha(Jugador jugador, int numero) {
        int[] fichas = jugador.getFichas();
        boolean estabaEnBase = fichas[numero] == ReglasParchis.EN_BASE;
        fichas[numero] = reglas.nuevaPosicion(fichas[numero], dado);
        if (estabaEnBase) {
            agregarBitacora(jugador.getNombre() + " sacó la ficha " + (numero + 1) + " de su base.");
        } else {
            agregarBitacora(jugador.getNombre() + " avanzó " + dado + (dado == 1 ? " casilla" : " casillas")
                    + " con la ficha " + (numero + 1) + ".");
        }
        jugadas.add(jugador.getColor() + "," + numero + "," + fichas[numero]);

        // Captura: solo en el recorrido común, no en el pasillo
        if (reglas.estaEnRecorrido(fichas[numero])) {
            int casilla = reglas.casillaAbsoluta(jugador.getColor(), fichas[numero]);
            Jugador rival = rivalDe(jugador);
            int[] fichasRival = rival.getFichas();
            for (int j = 0; j < fichasRival.length; j++) {
                if (reglas.estaEnRecorrido(fichasRival[j])
                        && reglas.casillaAbsoluta(rival.getColor(), fichasRival[j]) == casilla) {
                    fichasRival[j] = ReglasParchis.EN_BASE;
                    agregarBitacora("¡Capturó la ficha " + (j + 1) + " de " + rival.getNombre() + "! Regresa a su base.");
                    enviarAlRival("CAPTURA;" + rival.getColor() + ";" + j);
                }
            }
        }
        enviarAlRival("MOVIMIENTO;" + jugador.getColor() + ";" + numero + ";" + fichas[numero]);

        if (fichas[numero] == ReglasParchis.META) {
            agregarBitacora("La ficha " + (numero + 1) + " de " + jugador.getNombre() + " llegó a la meta.");
        }
        yaTiro = false;
        if (reglas.haGanado(jugador)) {
            terminarPartida(jugador);
            return;
        }
        terminarTiro();
        actualizarEtiquetas();
        repaint();
    }

    /** Después de un tiro: con 6 vuelve a tirar el mismo jugador; si no, pasa el turno. */
    private void terminarTiro() {
        if (reglas.vuelveATirar(dado, seisSeguidos)) {
            agregarBitacora("Sacó 6: vuelve a tirar.");
        } else {
            cambiarTurno();
        }
    }

    private Jugador jugadorEnTurno() {
        return jugadorDe(turno);
    }

    private Jugador jugadorDe(ColorFicha color) {
        for (Jugador jugador : jugadores) {
            if (jugador.getColor() == color) {
                return jugador;
            }
        }
        throw new IllegalArgumentException("Nadie juega con " + color);
    }

    private Jugador rivalDe(Jugador jugador) {
        return jugadorDe(jugador.getColor().siguiente());
    }

    private void cambiarTurno() {
        turno = turno.siguiente();
        seisSeguidos = 0;
        agregarBitacora("Turno de " + jugadorEnTurno().getNombre() + " (" + turno + ").");
    }

    private void terminarPartida(Jugador ganador) {
        terminada = true;
        agregarBitacora("¡" + ganador.getNombre() + " ganó la partida!");
        enviarAlRival("FIN;" + ganador.getColor());
        guardarEstadisticas(ganador);
        cargarEstadisticas();
        actualizarEtiquetas();
        repaint();
        JOptionPane.showMessageDialog(this, "¡Ganó " + ganador.getNombre() + " (" + ganador.getColor() + ")!");
    }

    private void nuevaPartida() {
        for (Jugador jugador : jugadores) {
            jugador.reiniciarFichas();
        }
        turno = ColorFicha.ROJO;
        dado = 0;
        yaTiro = false;
        seisSeguidos = 0;
        terminada = false;
        jugadas.clear();
        bitacora.setText("");
        agregarBitacora("Nueva partida. Empieza " + jugadorEnTurno().getNombre() + ".");
        actualizarEtiquetas();
        repaint();
    }

    private void actualizarEtiquetas() {
        Jugador jugador = jugadorEnTurno();
        if (terminada) {
            etiquetaTurno.setText("Partida terminada");
            etiquetaTurno.setForeground(Color.DARK_GRAY);
        } else {
            etiquetaTurno.setText("Turno: " + jugador.getNombre() + " (" + turno + ")");
            etiquetaTurno.setForeground(turno.getColorAwt());
        }
        etiquetaDado.setText("Dado: " + (dado == 0 ? "-" : String.valueOf(dado)) + (yaTiro ? " · elige una ficha" : ""));
        botonDado.setEnabled(!terminada && !yaTiro);
    }

    private void agregarBitacora(String texto) {
        bitacora.append(texto + "\n");
        bitacora.setCaretPosition(bitacora.getDocument().getLength());
    }

    // ------------------------------------------------------------------ red

    /** Avisa la jugada al rival. Si no contesta, se sigue jugando aquí. */
    private void enviarAlRival(String mensaje) {
        if (!comunicacion.enviar(mensaje) && !avisoSinRed) {
            avisoSinRed = true;
            agregarBitacora("Sin conexión con " + comunicacion.getDestino()
                    + ". Se sigue jugando en esta computadora.");
        }
    }

    // ------------------------------------------------------------------ base de datos

    private void crearTablaEstadisticas() {
        try (Connection conexion = DriverManager.getConnection(URL_BD, USUARIO_BD, CLAVE_BD);
                Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("CREATE TABLE IF NOT EXISTS estadisticas ("
                    + "color VARCHAR(10) PRIMARY KEY, "
                    + "jugadas INT NOT NULL DEFAULT 0, "
                    + "ganadas INT NOT NULL DEFAULT 0)");
            hayBaseDeDatos = true;
        } catch (SQLException e) {
            hayBaseDeDatos = false;
            System.out.println("Sin base de datos: " + e.getMessage());
        }
    }

    private void cargarEstadisticas() {
        if (!hayBaseDeDatos) {
            etiquetaEstadisticas.setText("<html>Estadísticas: sin base de datos</html>");
            return;
        }
        String texto = "<html><b>Estadísticas</b>";
        try (Connection conexion = DriverManager.getConnection(URL_BD, USUARIO_BD, CLAVE_BD);
                Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery(
                        "SELECT color, jugadas, ganadas FROM estadisticas ORDER BY color DESC")) {
            while (resultado.next()) {
                texto += "<br>" + resultado.getString("color") + ": " + resultado.getInt("ganadas")
                        + " ganadas de " + resultado.getInt("jugadas");
            }
        } catch (SQLException e) {
            texto += "<br>No se pudieron leer: " + e.getMessage();
        }
        etiquetaEstadisticas.setText(texto + "</html>");
    }

    private void guardarEstadisticas(Jugador ganador) {
        if (!hayBaseDeDatos) {
            agregarBitacora("Sin base de datos: no se guardaron las estadísticas.");
            return;
        }
        String sql = "INSERT INTO estadisticas (color, jugadas, ganadas) VALUES (?, 1, ?) "
                + "ON DUPLICATE KEY UPDATE jugadas = jugadas + 1, ganadas = ganadas + ?";
        try (Connection conexion = DriverManager.getConnection(URL_BD, USUARIO_BD, CLAVE_BD);
                PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            for (Jugador jugador : jugadores) {
                int gano = jugador == ganador ? 1 : 0;
                sentencia.setString(1, jugador.getColor().getNombre());
                sentencia.setInt(2, gano);
                sentencia.setInt(3, gano);
                sentencia.executeUpdate();
            }
        } catch (SQLException e) {
            agregarBitacora("No se guardaron las estadísticas: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        tamCelda = (Math.min(getWidth(), getHeight()) - 20) / CELDAS;
        origenX = (getWidth() - tamCelda * CELDAS) / 2;
        origenY = (getHeight() - tamCelda * CELDAS) / 2;

        // Bases: cada color en la suya, arriba; las de abajo no se usan
        for (Jugador jugador : jugadores) {
            dibujarBase(g2, 0, jugador.getColor().getColumnaBase(), jugador.getColor().getColorAwt());
        }
        dibujarBase(g2, 11, 0, new Color(0xD9DDD3));
        dibujarBase(g2, 11, 11, new Color(0xD9DDD3));

        // Recorrido, con la salida de cada jugador en su color
        for (int i = 0; i < ReglasParchis.TOTAL_CASILLAS; i++) {
            int[] celda = coordenadaCasilla(i);
            Color relleno = Color.WHITE;
            for (Jugador jugador : jugadores) {
                if (i == jugador.getCasillaSalida()) {
                    relleno = jugador.getColor().getColorClaro();
                }
            }
            pintarCelda(g2, celda[0], celda[1], relleno);
        }

        // Pasillos hacia la meta
        for (int i = 0; i < CASILLAS_PASILLO; i++) {
            for (Jugador jugador : jugadores) {
                int[] celda = jugador.getColor().coordenadaPasillo(i);
                pintarCelda(g2, celda[0], celda[1], jugador.getColor().getColorClaro());
            }
            // Los pasillos de los colores que no juegan
            pintarCelda(g2, 9, 17 - i, new Color(0xE7EAE3));
            pintarCelda(g2, 17 - i, 9, new Color(0xE7EAE3));
        }

        // Meta: el cuadro de 3 x 3 del centro
        g2.setColor(new Color(0x14233A));
        g2.fillRect(origenX + 8 * tamCelda, origenY + 8 * tamCelda, 3 * tamCelda, 3 * tamCelda);
        g2.setColor(new Color(0xA9B6C6));
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, tamCelda * 3 / 10)));
        FontMetrics medidas = g2.getFontMetrics();
        g2.drawString("META", origenX + 10 * tamCelda + tamCelda / 2 - medidas.stringWidth("META") / 2,
                origenY + 10 * tamCelda + tamCelda / 2 + medidas.getAscent() / 2 - 1);

        // Fichas
        for (Jugador jugador : jugadores) {
            dibujarFichas(g2, jugador);
        }
    }

    private void dibujarBase(Graphics2D g2, int fila, int columna, Color color) {
        g2.setColor(color);
        g2.fillRect(origenX + columna * tamCelda, origenY + fila * tamCelda, 8 * tamCelda, 8 * tamCelda);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(origenX + (columna + 1) * tamCelda, origenY + (fila + 1) * tamCelda,
                6 * tamCelda, 6 * tamCelda, tamCelda, tamCelda);
    }

    private void pintarCelda(Graphics2D g2, int fila, int columna, Color relleno) {
        int x = origenX + columna * tamCelda;
        int y = origenY + fila * tamCelda;
        g2.setColor(relleno);
        g2.fillRect(x, y, tamCelda, tamCelda);
        g2.setColor(new Color(0xB9C0B5));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(x, y, tamCelda, tamCelda);
    }

    private void dibujarFichas(Graphics2D g2, Jugador jugador) {
        int[] fichas = jugador.getFichas();
        boolean esSuTurno = jugador.getColor() == turno;
        int radio = tamCelda * 2 / 5;
        for (int i = 0; i < fichas.length; i++) {
            int[] centro = centroFicha(jugador.getColor(), i, fichas[i]);

            // Resaltar las fichas que se pueden mover
            if (esSuTurno && yaTiro && !terminada && reglas.puedeMover(fichas[i], dado)) {
                g2.setColor(new Color(0x2A7A50));
                g2.setStroke(new BasicStroke(3f));
                g2.drawOval(centro[0] - radio - 4, centro[1] - radio - 4, (radio + 4) * 2, (radio + 4) * 2);
            }

            g2.setColor(jugador.getColor().getColorAwt());
            g2.fillOval(centro[0] - radio, centro[1] - radio, radio * 2, radio * 2);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(centro[0] - radio, centro[1] - radio, radio * 2, radio * 2);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, radio)));
            String numero = String.valueOf(i + 1);
            FontMetrics medidas = g2.getFontMetrics();
            g2.drawString(numero, centro[0] - medidas.stringWidth(numero) / 2,
                    centro[1] + medidas.getAscent() / 2 - 1);
        }
    }

    // ------------------------------------------------------------------ geometría del tablero

    /** Fila y columna de la casilla {@code i} del recorrido (0 a 67), en el sentido de las manecillas del reloj. */
    private int[] coordenadaCasilla(int i) {
        if (i <= 7) return new int[] {8, i};
        if (i <= 15) return new int[] {15 - i, 8};
        if (i == 16) return new int[] {0, 9};
        if (i <= 24) return new int[] {i - 17, 10};
        if (i <= 32) return new int[] {8, i - 14};
        if (i == 33) return new int[] {9, 18};
        if (i <= 41) return new int[] {10, 52 - i};
        if (i <= 49) return new int[] {i - 31, 10};
        if (i == 50) return new int[] {18, 9};
        if (i <= 58) return new int[] {69 - i, 8};
        if (i <= 66) return new int[] {10, 66 - i};
        return new int[] {9, 0};
    }

    /** Centro en pixeles de una ficha según su color, su número y su posición. */
    private int[] centroFicha(ColorFicha color, int numero, int posicion) {
        double fila;
        double columna;
        if (posicion == ReglasParchis.EN_BASE) {
            // En la base: cuatro lugares dentro del cuadro de su color
            double[][] lugares = {{2.6, 2.6}, {2.6, 5.4}, {5.4, 2.6}, {5.4, 5.4}};
            fila = lugares[numero][0];
            columna = color.getColumnaBase() + lugares[numero][1];
        } else if (posicion == ReglasParchis.META) {
            // En la meta: cada color ocupa su lado del cuadro central
            int[] lugar = color.lugarMeta(numero);
            fila = lugar[0] + 0.5;
            columna = lugar[1] + 0.5;
        } else {
            int[] celda = reglas.estaEnRecorrido(posicion)
                    ? coordenadaCasilla(reglas.casillaAbsoluta(color, posicion))
                    : color.coordenadaPasillo(posicion - ReglasParchis.FIN_RECORRIDO - 1);
            // Un pequeño desplazamiento para que dos fichas en la misma casilla se distingan
            double desplazamiento = (numero - 1.5) * 0.12;
            fila = celda[0] + 0.5 + desplazamiento;
            columna = celda[1] + 0.5 + desplazamiento;
        }
        return new int[] {(int) Math.round(origenX + columna * tamCelda), (int) Math.round(origenY + fila * tamCelda)};
    }

    // ------------------------------------------------------------------ eventos del ratón sin usar

    @Override
    public void mouseClicked(MouseEvent e) { }

    @Override
    public void mouseReleased(MouseEvent e) { }

    @Override
    public void mouseEntered(MouseEvent e) { }

    @Override
    public void mouseExited(MouseEvent e) { }
}
