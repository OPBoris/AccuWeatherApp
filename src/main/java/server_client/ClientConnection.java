package server_client;

import java.io.*;
import java.net.Socket;

public class ClientConnection implements Closeable {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private synchronized void ensureConnected() throws IOException {
        if (socket != null && socket.isConnected() && !socket.isClosed()) return;
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    public synchronized String sendCommand(String command) throws IOException {
        ensureConnected();
        writer.println(command);
        return reader.readLine();
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            if (writer != null) writer.println("QUIT");
        } catch (Exception ignored) {}
        if (socket != null && !socket.isClosed()) socket.close();
    }
}
