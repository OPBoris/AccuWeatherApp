package server_client;

import user.GuestUser;
import user.RegularUser;
import user.User;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final WeatherService weatherService;
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
            String[] settings = weatherService.loadUserSettings(currentUser.getUsername());
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
            weatherService.saveUserSettings(currentUser.getUsername(), showHumidity, showWind, showFeelsLike, currentUnit, standardCity);
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
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8))
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

                            String city = args;

                            if (args.toUpperCase().endsWith(" F")) {
                                currentUnit = "F";
                                city = args.substring(0, args.length() - 2).trim();
                            } else if (args.toUpperCase().endsWith(" C")) {
                                currentUnit = "C";
                                city = args.substring(0, args.length() - 2).trim();
                            }

                            String response = weatherService.getWeatherByCity(city, currentUnit, currentUser.getUsername(), showHumidity, showWind, showFeelsLike);
                            sendMessage(writer, response);
                            sendMessage(writer, "###END###");
                        } else {
                            sendMessage(writer, "ERROR: Missing city name");
                            sendMessage(writer, "###END###");
                        }
                        break;

                    case "GET_HISTORY":
                        String history = weatherService.getRecentCities(currentUser.getUsername());
                        sendMessage(writer, "HISTORY:" + history);
                        sendMessage(writer, "###END###");
                        break;

                    case "MORITZ":
                        currentUser = Moritz;
                        loadSettingsForCurrentUser();
                        sendMessage(writer, "SETTINGS;" + currentUser.getUsername() + ";" + showHumidity + ";"
                                + showWind + ";" + showFeelsLike + ";" + this.standardCity+ ";" + this.currentUnit);
                        sendMessage(writer, "###END###");
                        break;

                    case "JAN":
                        currentUser = Jan;
                        loadSettingsForCurrentUser();
                        sendMessage(writer,"SETTINGS;" + currentUser.getUsername() + ";" + showHumidity + ";"
                                + showWind + ";" + showFeelsLike + ";" + this.standardCity+ ";" + this.currentUnit);
                        sendMessage(writer, "###END###");
                        break;

                    case "BORIS":
                        currentUser = Boris;
                        loadSettingsForCurrentUser();
                        sendMessage(writer,"SETTINGS;" + currentUser.getUsername() + ";" + showHumidity + ";" +
                                showWind + ";" + showFeelsLike + ";" + this.standardCity+ ";" + this.currentUnit);
                        sendMessage(writer, "###END###");
                        break;

                    case "CHECK_WIND":
                        showWind = !showWind;
                        saveSettingsForCurrentUser();
                        sendMessage(writer,"OK: showWind=" + showWind);
                        sendMessage(writer, "###END###");
                        break;

                    case "CHECK_HUMIDITY":
                        showHumidity = !showHumidity;
                        saveSettingsForCurrentUser();
                        sendMessage(writer,"OK: showWind=" + showWind);
                        sendMessage(writer, "###END###");
                        break;

                    case "CHECK_FEELS_LIKE":
                        showFeelsLike = !showFeelsLike;
                        saveSettingsForCurrentUser();
                        sendMessage(writer,"OK: showWind=" + showWind);
                        sendMessage(writer, "###END###");
                        break;

                    case "SET_UNIT":
                        if (parts.length > 1) {
                            String newUnit = parts[1].toUpperCase();
                            if (newUnit.equals("C") || newUnit.equals("F")) {
                                currentUnit = newUnit;
                                saveSettingsForCurrentUser();
                                sendMessage(writer,"Unit set to " + newUnit);
                            } else {
                                sendMessage(writer, "ERROR: Invalid unit");
                            }
                        }
                        sendMessage(writer, "###END###");
                        break;

                    case "SET_STANDARD":
                        if (currentUser instanceof GuestUser) {
                            sendMessage(writer,"ERROR: Guest users cannot save settings.");
                        }
                        else if (parts.length > 1) {
                            this.standardCity = parts[1].trim();
                            weatherService.saveUserSettings(currentUser.getUsername(), showHumidity, showWind, showFeelsLike, currentUnit, this.standardCity);
                            sendMessage(writer,"Standard city set to " + standardCity);
                        } else {
                            sendMessage(writer,"ERROR: Missing city name");
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
}
