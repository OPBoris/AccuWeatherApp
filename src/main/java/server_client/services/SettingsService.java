package server_client.services;

import java.io.*;

public class SettingsService {

    private static final String DB_FOLDER = "src/main/DB";

    public SettingsService() {

    }

    public synchronized void saveUserSettings(String username, boolean showHumidity, boolean showWind, boolean showFeelsLike,
                                              String unit, String standardCity) {
        if (username == null || username.isEmpty() || username.equalsIgnoreCase("Guest")) return;


        String filename = DB_FOLDER + "/settings_" + username + ".csv";
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)))) {
            writer.write("showHumidity,showWind,showFeelsLike,unit,standardCity");
            writer.newLine();

            String line = showHumidity + "," + showWind + "," + showFeelsLike + "," + unit + "," + standardCity;
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error saving user settings: " + e.getMessage());
        }
    }

    public String[] loadUserSettings(String username) {
        String[] settings = {"false", "false", "false", "C", ""};

        if (username == null || username.isEmpty()) return settings;

        String filename = DB_FOLDER + "/settings_" + username + ".csv";
        File file = new File(filename);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                reader.readLine();
                String line = reader.readLine();
                if (line != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        settings[0] = parts[0];
                        settings[1] = parts[1];
                        settings[2] = parts[2];
                        settings[3] = parts[3];
                    }
                    if (parts.length >= 5) {
                        settings[4] = parts[4];
                    }
                }
            } catch (IOException e) {
                System.err.println("Error loading settings for " + username + ": " + e.getMessage());
            }
        }
        return settings;
    }
}
