package server_client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final WeatherService weatherService;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.weatherService = new WeatherService();
    }

    @Override
    public void run() {
        System.out.println("Processing client request: " + clientSocket.getInetAddress().getHostAddress());

        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String clientMessage;

            while ((clientMessage = reader.readLine()) != null) {
                String trimmedMessage = clientMessage.trim();
                if (clientMessage.trim().isEmpty()) continue;

                String[] parts = clientMessage.trim().split("\\s+", 2);
                String command = parts[0].toUpperCase();

                System.out.println("Received command: " + trimmedMessage);

                if ("PING".equals(command)) {
                    writer.println("PONG");
                }

                else if ("GET_WEATHER".equals(command)) {
                    if (parts.length > 1 ){
                        String city = parts[1].trim();
                        String response = weatherService.getWeatherForCity(city);
                        writer.println(response);
                    } else {
                        writer.println("ERROR: City name is missing.");
                    }

                }

                else if ("GET_HISTORY".equals(command)) {
                    String history = weatherService.getRecentCities();
                    writer.println("HISTORY:" + history);
                }

                else if ("QUIT".equals(command)) {
                    writer.println("BYE");
                    break;
                }

                else {
                    writer.println("ERROR: Unknown command.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error in communication with the client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}
