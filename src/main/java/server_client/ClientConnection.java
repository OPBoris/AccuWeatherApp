package server_client;

import java.io.*;
import java.net.Socket;

public class ClientConnection implements Closeable {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean connected = false;

    public ClientConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private synchronized void ensureConnected() throws IOException {
        if (connected && socket != null && socket.isConnected() && !socket.isClosed()) {
            return;
        }

        closeInternal();

        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        connected = true;
    }

    public synchronized String sendCommand(String command) throws IOException {
        try {
            ensureConnected();
            writer.println(command);
            writer.flush();

            String line;
            String response = "";
            while ((line = reader.readLine()) != null && !line.equals("###END###")) {
                response += line + "\n";
            }

            if (response.isEmpty()) {
                connected = false;
                throw new IOException("Server closed connection");
            }
            return response.trim();
        } catch (IOException e) {
            connected = false;
            closeInternal();
            throw e;
        }
    }

    private void closeInternal() {
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (Exception ignored) {}
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public synchronized void close() throws IOException {
        connected = false;
        closeInternal();
    }
}
