package server_client;

import server_client.exceptions.WeatherAppException;
import server_client.services.SettingsService;
import server_client.services.WeatherService;
import user.GuestUser;
import user.RegularUser;
import user.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;


public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final WeatherService weatherService;
    private final SettingsService settingsService;
    private User currentUser;
    private final User Moritz = new RegularUser("Moritz");
    private final User Jan = new RegularUser("Jan");
    private final User Boris = new RegularUser("Boris");
    private String currentUnit;
    private String standardCity = "";
    private boolean showHumidity = false;
    private boolean showWind = false;
    private boolean showFeelsLike = false;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.weatherService = new WeatherService();
        this.settingsService = new SettingsService();
        this.currentUser = new GuestUser();
        this.currentUnit = "C";
        System.out.println("Processing client request: " + clientSocket.getInetAddress().getHostAddress());
        System.out.println("Client created: " + currentUser);
    }

    private void loadSettingsForCurrentUser() {
        if (currentUser instanceof GuestUser) {
            showHumidity = false;
            showWind = false;
            showFeelsLike = false;
            currentUnit = "C";
            standardCity = "";
        } else {
            String[] settings = settingsService.loadUserSettings(currentUser.getUsername());
            showHumidity = Boolean.parseBoolean(settings[0]);
            showWind = Boolean.parseBoolean(settings[1]);
            showFeelsLike = Boolean.parseBoolean(settings[2]);
            currentUnit = settings[3];
            if (settings.length > 4) {
                standardCity = settings[4];
            } else {
                standardCity = "";
            }
            System.out.println("Loaded settings for " + currentUser.getUsername() + ": H=" + showHumidity + ", W="
                    + showWind + ", FL=" + showFeelsLike + ", Unit=" + currentUnit + ", StandardCity=" + standardCity);
        }
    }

    private void saveSettingsForCurrentUser() {
        if (!(currentUser instanceof GuestUser)) {
            settingsService.saveUserSettings(currentUser.getUsername(), showHumidity, showWind, showFeelsLike, currentUnit, standardCity);
        }
    }

    private void sendMessage(BufferedWriter writer, String message) throws IOException {
        writer.write(message);
        writer.newLine();
        writer.flush();
    }

    @Override
    public void run() {

        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            String clientMessage;

            while ((clientMessage = reader.readLine()) != null) {
                if (clientMessage.trim().isEmpty()) continue;

                String[] parts = clientMessage.trim().split("\\s+", 2);
                String command = parts[0].toUpperCase();


                System.out.println("Received command: " + clientMessage);

                switch (command) {
                    case "GET_WEATHER":
                        if (parts.length > 1) {
                            String city = parseAndSetFlags(parts[1].trim());
                            try {
                                String response = weatherService.getWeatherByCity(city, currentUnit,
                                        currentUser.getUsername(), showHumidity, showWind, showFeelsLike);
                                sendMessage(writer, response);
                            } catch (WeatherAppException e) {
                                sendMessage(writer, "ERROR: " + e.getMessage());
                            }
                            sendMessage(writer, "###END###");
                        } else {
                            sendMessage(writer, "ERROR: Missing city name");
                            sendMessage(writer, "###END###");
                        }
                        break;

                    case "GET_FORECAST":
                        if (parts.length > 1) {
                            String city = parseAndSetFlags(parts[1].trim());
                            try {
                                String forecastResponse = weatherService.getForecastByCity(city, currentUnit,
                                        showFeelsLike, showHumidity, showWind);
                                sendMessage(writer, forecastResponse);
                            } catch (WeatherAppException e) {
                                sendMessage(writer, "ERROR: " + e.getMessage());
                            }
                            sendMessage(writer, "###END###");
                        } else {
                            sendMessage(writer, "ERROR: Missing city name");
                            sendMessage(writer, "###END###");
                        }
                        break;

                    case "GET_HISTORICAL":
                        if (parts.length > 1) {
                            String city = parseAndSetFlags(parts[1].trim());
                            try {
                                String historicalResponse = weatherService.getHistoricalWeatherByCity(city, currentUnit);
                                sendMessage(writer, historicalResponse);
                            } catch (WeatherAppException e) {
                                sendMessage(writer, "ERROR: " + e.getMessage());
                            }
                            sendMessage(writer, "###END###");
                        } else {
                            sendMessage(writer, "ERROR: Missing city name");
                            sendMessage(writer, "###END###");
                        }
                        break;

                    case "IS_FAVORITE":
                        if (parts.length > 1) {
                            String city = parts[1].trim();
                            boolean isFav = weatherService.isFavorite(city, currentUser.getUsername());
                            sendMessage(writer, "IS_FAVORITE:" + isFav);
                            sendMessage(writer, "###END###");
                        } else {
                            sendMessage(writer, "ERROR: Missing city name");
                            sendMessage(writer, "###END###");
                        }
                        break;

                    case "ADD_FAVORITE":
                        if (parts.length > 1) {
                            String city = parts[1].trim();
                            boolean success = weatherService.addFavorite(city, currentUser.getUsername());
                            if (success) {
                                sendMessage(writer, "OK: Favorite added");
                            } else {
                                sendMessage(writer, "ERROR: Could not add favorite");
                            }
                            sendMessage(writer, "###END###");
                        } else {
                            sendMessage(writer, "ERROR: Missing city name");
                            sendMessage(writer, "###END###");
                        }
                        break;

                    case "REMOVE_FAVORITE":
                        if (parts.length > 1) {
                            String city = parts[1].trim();
                            boolean removeSuccess = weatherService.removeFavorite(city, currentUser.getUsername());
                            if (removeSuccess) {
                                sendMessage(writer, "OK: Favorite removed");
                            } else {
                                sendMessage(writer, "ERROR: Favorite not found");
                            }
                            sendMessage(writer, "###END###");
                        } else {
                            sendMessage(writer, "ERROR: Missing city name");
                            sendMessage(writer, "###END###");
                        }
                        break;

                    case "GET_FAVORITES":
                        String favorites = weatherService.getFavorites(currentUser.getUsername());
                        sendMessage(writer, "FAVORITES:" + favorites);
                        sendMessage(writer, "###END###");
                        break;

                    case "GET_HISTORY":
                        try {
                            String history = weatherService.getRecentCities(currentUser.getUsername());
                            sendMessage(writer, "HISTORY:" + history);
                        } catch (WeatherAppException e) {
                            sendMessage(writer, "ERROR: " + e.getMessage());
                        }
                        sendMessage(writer, "###END###");
                        break;

                    case "EXPORT_HISTORY":
                        if (parts.length > 1) {
                            String city = parseAndSetFlags(parts[1].trim());
                            try {
                                String exportResult = weatherService.exportHistoricalDataToCSVByCity(
                                        city, currentUnit, currentUser.getUsername()
                                );
                                sendMessage(writer, exportResult);
                            } catch (WeatherAppException e) {
                                sendMessage(writer, "ERROR: " + e.getMessage());
                            }
                            sendMessage(writer, "###END###");
                        } else {
                            sendMessage(writer, "ERROR: Missing city name for export");
                            sendMessage(writer, "###END###");
                        }
                        break;

                    case "SAVE_OFFLINE":

                        if (parts.length > 1) {
                            String city = parseAndSetFlags(parts[1].trim());
                            try {
                                String saveResult = weatherService.saveOfflineData(
                                        city, currentUnit, currentUser.getUsername()
                                );
                                sendMessage(writer, saveResult);
                            } catch (WeatherAppException e) {
                                sendMessage(writer, "ERROR: " + e.getMessage());
                            }
                            sendMessage(writer, "###END###");
                        } else {
                            sendMessage(writer, "ERROR: Missing city name for offline save");
                            sendMessage(writer, "###END###");
                        }
                        break;

                    case "LOAD_OFFLINE":
                        try {
                            String offlineData = weatherService.loadOfflineData(currentUser.getUsername());
                            sendMessage(writer, offlineData);
                        } catch (WeatherAppException e) {
                            sendMessage(writer, "ERROR: " + e.getMessage());
                        }
                        sendMessage(writer, "###END###");
                        break;

                    case "CHECK_ONLINE":

                        boolean isOnline = weatherService.isOnline();
                        if (isOnline) {
                            sendMessage(writer, "ONLINE");
                        } else {
                            sendMessage(writer, "OFFLINE");
                        }
                        sendMessage(writer, "###END###");
                        break;

                    case "MORITZ":
                        currentUser = Moritz;
                        loadSettingsForCurrentUser();
                        sendMessage(writer, "SETTINGS;" + currentUser.getUsername() + ";" + showHumidity + ";"
                                + showWind + ";" + showFeelsLike + ";" + this.standardCity + ";" + this.currentUnit);
                        sendMessage(writer, "###END###");
                        break;

                    case "JAN":
                        currentUser = Jan;
                        loadSettingsForCurrentUser();
                        sendMessage(writer, "SETTINGS;" + currentUser.getUsername() + ";" + showHumidity + ";"
                                + showWind + ";" + showFeelsLike + ";" + this.standardCity + ";" + this.currentUnit);
                        sendMessage(writer, "###END###");
                        break;

                    case "BORIS":
                        currentUser = Boris;
                        loadSettingsForCurrentUser();
                        sendMessage(writer, "SETTINGS;" + currentUser.getUsername() + ";" + showHumidity + ";" +
                                showWind + ";" + showFeelsLike + ";" + this.standardCity + ";" + this.currentUnit);
                        sendMessage(writer, "###END###");
                        break;

                    case "CHECK_WIND":
                        showWind = !showWind;
                        saveSettingsForCurrentUser();
                        sendMessage(writer, "OK: showWind=" + showWind);
                        sendMessage(writer, "###END###");
                        break;

                    case "CHECK_HUMIDITY":
                        showHumidity = !showHumidity;
                        saveSettingsForCurrentUser();
                        sendMessage(writer, "OK: showHumidity=" + showHumidity);
                        sendMessage(writer, "###END###");
                        break;

                    case "CHECK_FEELS_LIKE":
                        showFeelsLike = !showFeelsLike;
                        saveSettingsForCurrentUser();
                        sendMessage(writer, "OK: showFeelsLike=" + showFeelsLike);
                        sendMessage(writer, "###END###");
                        break;

                    case "SET_UNIT":
                        if (parts.length > 1) {
                            String newUnit = parts[1].toUpperCase();
                            if (newUnit.equals("C") || newUnit.equals("F")) {
                                currentUnit = newUnit;
                                saveSettingsForCurrentUser();
                                sendMessage(writer, "Unit set to " + newUnit);
                            } else {
                                sendMessage(writer, "ERROR: Invalid unit");
                            }
                        }
                        sendMessage(writer, "###END###");
                        break;

                    case "SET_STANDARD":
                        if (currentUser instanceof GuestUser) {
                            sendMessage(writer, "ERROR: Guest users cannot save settings.");
                        } else if (parts.length > 1) {
                            this.standardCity = parts[1].trim();
                            settingsService.saveUserSettings(currentUser.getUsername(), showHumidity, showWind, showFeelsLike, currentUnit, this.standardCity);
                            sendMessage(writer, "Standard city set to " + standardCity);
                        } else {
                            sendMessage(writer, "ERROR: Missing city name");
                        }
                        sendMessage(writer, "###END###");
                        break;

                    default:
                        sendMessage(writer, "ERROR: Unknown command '" + command + "'");
                        sendMessage(writer, "###END###");
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

    private String parseAndSetFlags(String args) {
        this.showFeelsLike = args.contains("FEELS_LIKE=true");
        this.showHumidity = args.contains("HUMIDITY=true");
        this.showWind = args.contains("WIND=true");

        String cleanArgs = args.replaceAll("FEELS_LIKE=(true|false)", "")
                .replaceAll("HUMIDITY=(true|false)", "")
                .replaceAll("WIND=(true|false)", "")
                .trim();

        String substring = cleanArgs.substring(0, cleanArgs.length() - 2);
        if (cleanArgs.toUpperCase().endsWith(" F")) {
            this.currentUnit = "F";
            return substring.trim();
        } else if (cleanArgs.toUpperCase().endsWith(" C")) {
            this.currentUnit = "C";
            return substring.trim();
        }

        return cleanArgs;
    }
}

