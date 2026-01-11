package fhtw.accuweatherapp.handler;

import javafx.concurrent.Task;
import javafx.scene.control.MenuButton;
import server_client.ClientConnection;

import java.util.function.Consumer;


public class UIUserHandler {

    private final ClientConnection connection;

    public UIUserHandler(ClientConnection connection) {
        this.connection = connection;
    }

    public void switchUser(String username, Consumer<UserSettings> onSuccess,
                           Consumer<String> onError) {
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
                    settings.standardCity = parts.length > 5 ? parts[5] : "";
                    settings.unit = parts.length > 6 ? parts[6] : "C";

                    onSuccess.accept(settings);
                } else {
                    onError.accept("Invalid settings format from server.");
                }
            } else {
                onError.accept(response != null ? response : "No response from server.");
            }
        });

        task.setOnFailed(e -> {
            onError.accept("Error user switching.");
        });

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
