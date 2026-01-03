package server_client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final WeatherService weatherService;
    private final User currentUser;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.weatherService = new WeatherService();
        this.currentUser = new GuestUser();
        System.out.println("Processing client request: " + clientSocket.getInetAddress().getHostAddress());
        System.out.println("Client created: " + currentUser);
    }

    @Override
    public void run() {

        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String clientMessage;

            while ((clientMessage = reader.readLine()) != null) {
                if (clientMessage.trim().isEmpty()) continue;

                String[] parts = clientMessage.trim().split("\\s+", 2);
                String command = parts[0].toUpperCase();

                String username = currentUser.getUsername();

                System.out.println("Received command: " + clientMessage);

                switch (command) {
                    case "SEARCH_CITIES":
                        if (parts.length > 1) {
                            String partialName = parts[1].trim();
                            String suggestions = weatherService.searchCities(partialName);
                            writer.println("SUGGESTIONS:" + suggestions);
                        } else {
                            writer.println("SUGGESTIONS:");
                        }
                        break;

                    case "GET_WEATHER":
                        if (parts.length > 1) {
                            String args = parts[1].trim();

                            String city = args;
                            String unit = "C";

                            if (args.toUpperCase().endsWith(" F")) {
                                unit = "F";
                                city = args.substring(0, args.length() - 2).trim();
                            } else if (args.toUpperCase().endsWith(" C")) {
                                unit = "C";
                                city = args.substring(0, args.length() - 2).trim();
                            }

                            String response = weatherService.getWeatherForCity(city, unit, username);
                            writer.println(response);
                        } else {
                            writer.println("ERROR: Missing city name");
                        }
                        break;

                    case "GET_HISTORY":
                        String history = weatherService.getRecentCities(username);
                        writer.println("HISTORY:" + history);
                        break;

                    case "QUIT":
                        writer.println("BYE");
                        return;

                    default:
                        writer.println("ERROR: Unknown command '" + command + "'");
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error in communication with the client: " + e.getMessage());
        } finally {
            try {
                System.out.println("Connection closed for user: " + currentUser.getUsername());
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}
