package server_client.clients;

import java.io.*;
import java.net.Socket;

public class ClientConnection implements Closeable {
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
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
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        connected = true;
    }

    public synchronized String sendCommand(String command) throws IOException {
        try {
            ensureConnected();
            writer.write(command);
            writer.newLine();
            writer.flush();

            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null && !line.equals("###END###")) {
                response.append(line).append("\n");
            }

            if (response.isEmpty()) {
                throw new IOException("Server closed connection");
            }
            return response.toString().trim();
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
        } catch (Exception e) {
            System.err.println("Error closing reader/writer: " + e.getMessage());
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        connected = false;
        closeInternal();
    }
}
