package server_client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final WeatherService weatherService;
    private User currentUser;

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
                String payload = (parts.length > 1) ? parts[1].trim() : "";

                String username = currentUser.getUsername();
                System.out.println("Received command: " + command + " from " + username);

                switch (command) {
                    case "PING":
                        writer.println("PONG");
                        break;

                    case "LOGIN":
                        if (!payload.isEmpty()) {
                            this.currentUser = new RegisteredUser(payload);
                            System.out.println("User logged in as: " + currentUser.getUsername());
                            writer.println("Welcome " + currentUser.getUsername());
                        } else {
                            writer.println("ERROR: Missing username for login");
                        }
                        break;

                    case "SEARCH_CITIES":
                        if (!payload.isEmpty()) {
                            String suggestions = weatherService.searchCities(payload);
                            writer.println("SUGGESTIONS:" + suggestions);
                        } else {
                            writer.println("SUGGESTIONS:");
                        }
                        break;

                    case "GET_WEATHER":
                        if (!payload.isEmpty()) {
                            String city = payload;
                            String unit = "C";

                            if (payload.toUpperCase().endsWith(" F")) {
                                unit = "F";
                                city = payload.substring(0, payload.length() - 2).trim();
                            } else if (payload.toUpperCase().endsWith(" C")) {
                                unit = "C";
                                city = payload.substring(0, payload.length() - 2).trim();
                            }

                            String response = weatherService.getWeatherForCity(city, unit, username);
                            writer.println(response);
                        } else {
                            writer.println("ERROR: Missing city name");
                        }
                        break;

                    case "ADD_FAVORITE":
                        if (!payload.isEmpty()) {
                            boolean success = weatherService.addFavorite(payload, username);
                            writer.println(success ? "Favorite added" : "ERROR: Could not add favorite");
                        } else {
                            writer.println("ERROR: Missing city name for favorite");
                        }
                        break;

                    case "REMOVE_FAVORITE":
                        if (!payload.isEmpty()) {
                            boolean success = weatherService.removeFavorite(payload, username);
                            writer.println(success ? "OK: Favorite removed" : "ERROR: Favorite not found");
                        } else {
                            writer.println("ERROR: Missing city name");
                        }
                        break;

                    case "GET_FAVORITES":
                        String favorites = weatherService.getFavorites(username);
                        writer.println("FAVORITES:" + favorites);
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