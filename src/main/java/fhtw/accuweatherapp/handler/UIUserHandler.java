package fhtw.accuweatherapp.handler;

import javafx.concurrent.Task;
import javafx.scene.control.MenuButton;
import server_client.ClientConnection;
import fhtw.accuweatherapp.Callback;



public class UIUserHandler {

    private final ClientConnection connection;

    public UIUserHandler(ClientConnection connection) {
        this.connection = connection;
    }

    public void switchUser(String username, Callback<UserSettings> onSuccess,
                           Callback<String> onError) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return connection.sendCommand(username);
            }
        };

        task.setOnSucceeded(e -> {
            String response = task.getValue();

            if (response != null && response.startsWith("SETTINGS;")) {
                String[] parts = response.split(";");
                if (parts.length >= 5) {
                    UserSettings settings = new UserSettings();
                    settings.username = parts[1];
                    settings.showHumidity = Boolean.parseBoolean(parts[2]);
                    settings.showWind = Boolean.parseBoolean(parts[3]);
                    settings.showFeelsLike = Boolean.parseBoolean(parts[4]);
                    if (parts.length > 5) {
                        settings.standardCity = parts[5];
                    } else {
                        settings.standardCity = "";
                    }

                    if (parts.length > 6) {
                        settings.unit = parts[6];
                    } else {
                        settings.unit = "C";
                    }

                    onSuccess.call(settings);
                } else {
                    onError.call("Invalid settings format from server.");
                }
            } else {
                String errorMessage;
                if (response != null) {
                    errorMessage = response;
                } else {
                    errorMessage = "No response from server.";
                }
                onError.call(errorMessage);
            }
        });

        task.setOnFailed(e -> onError.call("Error user switching."));

        new Thread(task, "switch-user").start();
    }


    public void updateUserMenu(MenuButton menuButton, String username) {
        if (menuButton != null) {
            menuButton.setText(username);
        }
    }


    public static class UserSettings {
        public String username;
        public boolean showHumidity;
        public boolean showWind;
        public boolean showFeelsLike;
        public String standardCity;
        public String unit;
    }
}
