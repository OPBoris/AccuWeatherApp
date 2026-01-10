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

                            String city = args;

                            if (args.toUpperCase().endsWith(" F")) {
                                currentUnit = "F";
                                city = args.substring(0, args.length() - 2).trim();
                            } else if (args.toUpperCase().endsWith(" C")) {
                                currentUnit = "C";
                                city = args.substring(0, args.length() - 2).trim();
                            }

                            String response = weatherService.getWeatherByCity(city, currentUnit, currentUser.getUsername());
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

                    case "GET_REPORT":
                        if (parts.length > 1) {
                            String args = parts[1].trim();
                            String[] reportArgs = args.split("\\s+");

                            if (reportArgs.length == 3) {
                                String rCity = reportArgs[0];
                                String rStart = reportArgs[1];
                                String rEnd = reportArgs[2];

                                String report = weatherService.getHistoricalReport(rCity, rStart, rEnd);
                                writer.println(report.startsWith("ERROR")
                                        ? report
                                        : "REPORT_LATEX:" + report);
                            } else {
                                writer.println("ERROR: Format: GET_REPORT <City> <Start> <End>");
                            }
                        } else {
                            writer.println("ERROR: Missing report arguments");
                        }
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
