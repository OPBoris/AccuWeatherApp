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
        } else {
            boolean[] settings = weatherService.loadUserSettings(currentUser.getUsername());
            showHumidity = settings[0];
            showWind = settings[1];
            showFeelsLike = settings[2];
            System.out.println("Loaded settings for " + currentUser.getUsername() + ": H=" + showHumidity + ", W=" + showWind + ", FL=" + showFeelsLike);
        }
    }

    private void saveSettingsForCurrentUser() {
        if (!(currentUser instanceof GuestUser)) {
            weatherService.saveUserSettings(currentUser.getUsername(), showHumidity, showWind, showFeelsLike);
        }
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

                            String city = args;

                            if (args.toUpperCase().endsWith(" F")) {
                                currentUnit = "F";
                                city = args.substring(0, args.length() - 2).trim();
                            } else if (args.toUpperCase().endsWith(" C")) {
                                currentUnit = "C";
                                city = args.substring(0, args.length() - 2).trim();
                            }

                            String response = weatherService.getWeatherByCity(city, currentUnit, currentUser.getUsername(), showHumidity, showWind, showFeelsLike);
                            writer.println(response);
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

                    case "MORITZ":
                        currentUser = Moritz;
                        loadSettingsForCurrentUser();
                        writer.println("SETTINGS;" + currentUser.getUsername() + ";" + showHumidity + ";" + showWind + ";" + showFeelsLike);
                        writer.println("###END###");
                        break;

                    case "JAN":
                        currentUser = Jan;
                        loadSettingsForCurrentUser();
                        writer.println("SETTINGS;" + currentUser.getUsername() + ";" + showHumidity + ";" + showWind + ";" + showFeelsLike);
                        writer.println("###END###");
                        break;

                    case "BORIS":
                        currentUser = Boris;
                        loadSettingsForCurrentUser();
                        writer.println("SETTINGS;" + currentUser.getUsername() + ";" + showHumidity + ";" + showWind + ";" + showFeelsLike);
                        writer.println("###END###");
                        break;

                    case "CHECK_WIND":
                        showWind = !showWind;
                        saveSettingsForCurrentUser();
                        writer.println("OK: showWind=" + showWind);
                        writer.println("###END###");
                        break;

                    case "CHECK_HUMIDITY":
                        showHumidity = !showHumidity;
                        saveSettingsForCurrentUser();
                        writer.println("OK: showWind=" + showWind);
                        writer.println("###END###");
                        break;

                    case "CHECK_FEELS_LIKE":
                        showFeelsLike = !showFeelsLike;
                        saveSettingsForCurrentUser();
                        writer.println("OK: showWind=" + showWind);
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
