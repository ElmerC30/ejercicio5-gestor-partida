package mx.itson.parchis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Envía cada jugada a la computadora del rival por un socket TCP, una línea por mensaje.
 */
public class ComunicacionSocket implements Comunicacion {

    public static final String HOST_POR_DEFECTO = "localhost";
    public static final int PUERTO_POR_DEFECTO = 5000;

    private final String host;
    private final int puerto;

    public ComunicacionSocket(String host, int puerto) {
        this.host = host;
        this.puerto = puerto;
    }

    @Override
    public boolean enviar(String mensaje) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), 300);
            PrintWriter salida = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            salida.println(mensaje);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String getDestino() {
        return host + ":" + puerto;
    }

    /** Modo espectador: escribe en la consola las jugadas que llegan por la red. */
    public static void escuchar(int puerto) {
        System.out.println("Esperando jugadas en el puerto " + puerto + "...");
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            while (true) {
                try (Socket cliente = servidor.accept();
                        BufferedReader entrada = new BufferedReader(
                                new InputStreamReader(cliente.getInputStream(), StandardCharsets.UTF_8))) {
                    String linea;
                    while ((linea = entrada.readLine()) != null) {
                        System.out.println("Llegó: " + linea);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("No se pudo abrir el puerto " + puerto + ": " + e.getMessage());
        }
    }
}
