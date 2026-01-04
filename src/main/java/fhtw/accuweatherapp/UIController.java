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
    private boolean humidityChecked = false;
    private boolean windChecked = false;
    private boolean feelsLikeChecked = false;

    private final ClientConnection connection = new ClientConnection("localhost", 8080);

    @FXML
    protected void onSafeFavourite() {
        txt_field_cur_weather.setText("Pressed on Favourite.");
    }

    @FXML
    protected void onMoritz() {
        runCommand("MORITZ");
    }

    @FXML
    protected void onJan() {
        runCommand("JAN");
    }

    @FXML
    protected void onBoris() {
        runCommand("BORIS");
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

    @FXML
    protected void onCheckFeelsLike() {
        feelsLikeChecked = !feelsLikeChecked;
        if (feelsLikeChecked) {
            txt_field_cur_weather.setText("Checked Feels Like.");
        } else {
            txt_field_cur_weather.setText("Unchecked Feels Like.");
        }
    }

    @FXML
    protected void onCheckHumidity() {
        humidityChecked = !humidityChecked;
        if (humidityChecked) {
            txt_field_cur_weather.setText("Checked Humidity.");
        } else {
            txt_field_cur_weather.setText("Unchecked Humidity.");
        }
    }

    @FXML
    protected void onCheckWind() {
        windChecked = !windChecked;
        if (windChecked) {
            txt_field_cur_weather.setText("Checked Wind.");
        } else {
            txt_field_cur_weather.setText("Unchecked Wind.");
        }
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
        task.setOnFailed(e -> {
            Throwable exception = task.getException();
            if (exception != null) {
                txt_field_cur_weather.setText("Verbindungsfehler: " + exception.getMessage());
            } else {
                txt_field_cur_weather.setText("Verbindungsfehler: Unbekannter Fehler");
            }
        });
        new Thread(task, "server-call").start();
    }
}