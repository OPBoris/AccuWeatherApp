package server_client;

import user.GuestUser;
import user.RegularUser;
import user.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final WeatherService weatherService;
    private User currentUser;
    private final User Moritz = new RegularUser("Moritz");
    private final User Jan = new RegularUser("Jan");
    private final User Boris = new RegularUser("Boris");
    private String currentUnit;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.weatherService = new WeatherService();
        this.currentUser = new GuestUser();
        this.currentUnit = "C";
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

                //Just for debugging purposes
                System.out.println("Received command: " + clientMessage);

                switch (command) {
                    case "GET_WEATHER":
                        if (parts.length > 1) {
                            String args = parts[1].trim();

                            // Parse filter flags
                            boolean showFeelsLike = args.contains("FEELS_LIKE=true");
                            boolean showHumidity = args.contains("HUMIDITY=true");
                            boolean showWind = args.contains("WIND=true");

                            // Remove filter flags from args to get city and unit
                            String cleanArgs = args.replaceAll("FEELS_LIKE=(true|false)", "")
                                                   .replaceAll("HUMIDITY=(true|false)", "")
                                                   .replaceAll("WIND=(true|false)", "")
                                                   .trim();

                            String city = cleanArgs;

                            if (cleanArgs.toUpperCase().endsWith(" F")) {
                                currentUnit = "F";
                                city = cleanArgs.substring(0, cleanArgs.length() - 2).trim();
                            } else if (cleanArgs.toUpperCase().endsWith(" C")) {
                                currentUnit = "C";
                                city = cleanArgs.substring(0, cleanArgs.length() - 2).trim();
                            }

                            String response = weatherService.getWeatherByCity(city, currentUnit,
                                currentUser.getUsername(), showFeelsLike, showHumidity, showWind);
                            writer.println(response);
                            writer.println("###END###");
                        } else {
                            writer.println("ERROR: Missing city name");
                            writer.println("###END###");
                        }
                        break;

                    case "GET_FORECAST":
                        if (parts.length > 1) {
                            String args = parts[1].trim();

                            // Parse filter flags
                            boolean showFeelsLike = args.contains("FEELS_LIKE=true");
                            boolean showHumidity = args.contains("HUMIDITY=true");
                            boolean showWind = args.contains("WIND=true");

                            // Remove filter flags from args to get city and unit
                            String cleanArgs = args.replaceAll("FEELS_LIKE=(true|false)", "")
                                                   .replaceAll("HUMIDITY=(true|false)", "")
                                                   .replaceAll("WIND=(true|false)", "")
                                                   .trim();

                            String city = cleanArgs;

                            if (cleanArgs.toUpperCase().endsWith(" F")) {
                                currentUnit = "F";
                                city = cleanArgs.substring(0, cleanArgs.length() - 2).trim();
                            } else if (cleanArgs.toUpperCase().endsWith(" C")) {
                                currentUnit = "C";
                                city = cleanArgs.substring(0, cleanArgs.length() - 2).trim();
                            }

                            String forecastResponse = weatherService.getForecastByCity(city, currentUnit,
                                showFeelsLike, showHumidity, showWind);
                            writer.println(forecastResponse);
                            writer.println("###END###");
                        } else {
                            writer.println("ERROR: Missing city name");
                            writer.println("###END###");
                        }
                        break;

                    case "GET_HISTORICAL":
                        if (parts.length > 1) {
                            String args = parts[1].trim();

                            String city = args;

                            if (args.toUpperCase().endsWith(" F")) {
                                currentUnit = "F";
                                city = args.substring(0, args.length() - 2).trim();
                            } else if (args.toUpperCase().endsWith(" C")) {
                                currentUnit = "C";
                                city = args.substring(0, args.length() - 2).trim();
                            }

                            String historicalResponse = weatherService.getHistoricalWeatherByCity(city, currentUnit);
                            writer.println(historicalResponse);
                            writer.println("###END###");
                        } else {
                            writer.println("ERROR: Missing city name");
                            writer.println("###END###");
                        }
                        break;

                    case "GET_HISTORY":
                        String history = weatherService.getRecentCities(currentUser.getUsername());
                        writer.println("HISTORY:" + history);
                        writer.println("###END###");
                        break;

                    case "EXPORT_HISTORY":
                        if (parts.length > 1) {
                            String args = parts[1].trim();
                            String city = args;
                            String exportUnit = currentUnit;

                            if (args.toUpperCase().endsWith(" F")) {
                                exportUnit = "F";
                                city = args.substring(0, args.length() - 2).trim();
                            } else if (args.toUpperCase().endsWith(" C")) {
                                exportUnit = "C";
                                city = args.substring(0, args.length() - 2).trim();
                            }

                            String exportResult = weatherService.exportHistoricalDataToCSVByCity(
                                city, exportUnit, currentUser.getUsername()
                            );
                            writer.println(exportResult);
                            writer.println("###END###");
                        } else {
                            writer.println("ERROR: Missing city name for export");
                            writer.println("###END###");
                        }
                        break;

                    case "SAVE_OFFLINE":
                        // Save weather data for offline use
                        if (parts.length > 1) {
                            String args = parts[1].trim();
                            String city = args;
                            String offlineUnit = currentUnit;

                            // Parse unit from arguments
                            if (args.toUpperCase().endsWith(" F")) {
                                offlineUnit = "F";
                                city = args.substring(0, args.length() - 2).trim();
                            } else if (args.toUpperCase().endsWith(" C")) {
                                offlineUnit = "C";
                                city = args.substring(0, args.length() - 2).trim();
                            }

                            String saveResult = weatherService.saveOfflineData(
                                city, offlineUnit, currentUser.getUsername()
                            );
                            writer.println(saveResult);
                            writer.println("###END###");
                        } else {
                            writer.println("ERROR: Missing city name for offline save");
                            writer.println("###END###");
                        }
                        break;

                    case "LOAD_OFFLINE":
                        // Load offline weather data from cache
                        String offlineData = weatherService.loadOfflineData(currentUser.getUsername());
                        writer.println(offlineData);
                        writer.println("###END###");
                        break;

                    case "GET_OFFLINE_FORECAST":
                        // Get offline forecast for UI display
                        String offlineForecast = weatherService.getOfflineForecast(currentUser.getUsername());
                        writer.println(offlineForecast);
                        writer.println("###END###");
                        break;

                    case "CHECK_ONLINE":
                        // Check if internet connection is available
                        boolean isOnline = weatherService.isOnline();
                        writer.println(isOnline ? "ONLINE" : "OFFLINE");
                        writer.println("###END###");
                        break;

                    case "MORITZ":
                        currentUser = Moritz;
                        writer.println("User switched to: " + currentUser.getUsername());
                        writer.println("###END###");
                        break;

                    case "JAN":
                        currentUser = Jan;
                        writer.println("User switched to: " + currentUser.getUsername());
                        writer.println("###END###");
                        break;

                    case "BORIS":
                        currentUser = Boris;
                        writer.println("User switched to: " + currentUser.getUsername());
                        writer.println("###END###");
                        break;

                    default:
                        writer.println("ERROR: Unknown command '" + command + "'");
                        writer.println("###END###");
                        break;
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
