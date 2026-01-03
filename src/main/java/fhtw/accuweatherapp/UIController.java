package fhtw.accuweatherapp;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import server_client.ClientConnection;

public class UIController {
    @FXML
    private TextField txt_field_city;

    @FXML
    private TextArea txt_field_cur_weather;

    private String unit = "C";
    private String currentUser = "Guest";

    private final ClientConnection connection = new ClientConnection("localhost", 8080);

    @FXML
    protected void onSafeFavourite() {
        txt_field_cur_weather.setText("Pressed on Favourite.");
    }

    @FXML
    protected void onMoritz() {
        currentUser = "Moritz";
        txt_field_cur_weather.setText("User switched to: " + currentUser);
    }

    @FXML
    protected void onJan() {
        currentUser = "Jan";
        txt_field_cur_weather.setText("User switched to: " + currentUser);
    }

    @FXML
    protected void onBoris() {
        currentUser = "Boris";
        txt_field_cur_weather.setText("User switched to: " + currentUser);
    }

    @FXML
    protected void onSearch() {
        String city = txt_field_city.getText();

        if (city != null && !city.trim().isEmpty()) {
            txt_field_cur_weather.setText("Suche nach Wetter für: " + city);
            runCommand("GET_WEATHER " + city.trim() + " " + unit);
        } else {
            txt_field_cur_weather.setText("Bitte geben Sie eine Stadt ein.");
        }
    }

    @FXML
    protected void onReport() {
        runCommand("GET_HISTORY");
    }

    @FXML
    protected void onOffline() {
        txt_field_cur_weather.setText("Pressed on Offline button.");
    }

    @FXML
    protected void onUnitC() {
        unit = "C";
        txt_field_cur_weather.setText("Pressed on Unit C button.");
    }

    @FXML
    protected void onUnitF() {
        unit = "F";
        txt_field_cur_weather.setText("Unit °F.");
    }

    private void runCommand(String command) {
        txt_field_cur_weather.setText("Sende: " + command + " ...");
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return connection.sendCommand(command);
            }
        };
        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if(resp != null) {
                txt_field_cur_weather.setText(resp);
            } else {
                txt_field_cur_weather.setText("Keine Antwort vom Server erhalten.");
            }
        });
        task.setOnFailed(e -> txt_field_cur_weather.setText("Verbindungsfehler: " + task.getException().getMessage()));
        new Thread(task, "server-call").start();
    }
}