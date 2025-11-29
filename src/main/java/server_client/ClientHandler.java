package server_client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        System.out.println("Processing client request: " + clientSocket.getInetAddress().getHostAddress());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String clientMessage;

            while ((clientMessage = reader.readLine()) != null) {
                String trimmedMessage = clientMessage.trim();
                System.out.println("Received command: " + trimmedMessage);

                if ("PING".equalsIgnoreCase(trimmedMessage)) {
                    writer.println("PONG");
                    System.out.println("Server responded: PONG");
                }
                else if (trimmedMessage.startsWith("GET_WEATHER:")) {
                    String city = trimmedMessage.substring("GET_WEATHER:".length()).trim();

                    String weatherData = "Status: OK | Processing weather for " + city + ".";
                    writer.println(weatherData);
                }
                else if ("QUIT".equalsIgnoreCase(trimmedMessage)) {
                    writer.println("BYE");
                    break;
                }
                else {
                    writer.println("ERROR: Unknown command. Supported: PING, GET_WEATHER:[CITY], QUIT");
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected or I/O error: " + clientSocket.getInetAddress().getHostAddress() + " - " + e.getMessage());
            try {
                clientSocket.close();
                System.out.println("Connection closed for client: " + clientSocket.getInetAddress().getHostAddress());
            } catch (IOException ex) {
                System.err.println("Error closing client socket: " + ex.getMessage());
            }
        }
    }
}
