package fhtw.accuweatherapp;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import server_client.ClientConnection;

import java.util.List;

public class UIController {
    @FXML
    private TextField txt_field_city;

    @FXML
    private TextArea txt_field_cur_weather;

    @FXML
    private ComboBox<String> combox_last_used;

    @FXML
    private MenuButton menu_user;

    private String unit = "C";
    private boolean humidityChecked = false;
    private boolean windChecked = false;
    private boolean feelsLikeChecked = false;

    private final ClientConnection connection = new ClientConnection("localhost", 8080);

    @FXML
    public void initialize() {
        combox_last_used.setOnAction(event -> {
            String selectedCity = combox_last_used.getValue();
            if (selectedCity != null && !selectedCity.equals(txt_field_city.getText())) {
                txt_field_city.setText(selectedCity);
                onSearch();
            }
        });
        updateCityComboBox();
    }

    @FXML
    protected void onSafeFavourite() {
        txt_field_cur_weather.setText("Pressed on Favourite.");
    }

    @FXML
    protected void onMoritz() {
        runCommand("MORITZ");
        updateCityComboBox();
        menu_user.setText("Moritz");
    }

    @FXML
    protected void onJan() {
        runCommand("JAN");
        updateCityComboBox();
        menu_user.setText("Jan");
    }

    @FXML
    protected void onBoris() {
        runCommand("BORIS");
        updateCityComboBox();
        menu_user.setText("Boris");
    }

    @FXML
    protected void onSearch() {
        String city = txt_field_city.getText();

        if (city != null && !city.trim().isEmpty()) {
            txt_field_cur_weather.setText("Suche nach Wetter für: " + city);
            runCommand("GET_WEATHER " + city.trim() + " " + unit);
            updateCityComboBox();
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

    private void updateCityComboBox() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                String response = connection.sendCommand("GET_HISTORY");
                if (response != null && response.startsWith("HISTORY:")) {
                    String cities = response.substring(8).trim();
                    if (!cities.isEmpty()) {
                        return java.util.Arrays.asList(cities.split(","));
                    }
                }
                return new java.util.ArrayList<>();
            }
        };

        task.setOnSucceeded(event -> {
            combox_last_used.setItems(
                    javafx.collections.FXCollections.observableArrayList(task.getValue())
            );
            combox_last_used.getSelectionModel().clearSelection();
            combox_last_used.setValue(null);
        });

        task.setOnFailed(event -> {
            System.err.println("Fehler beim Laden der Historie: " + task.getException().getMessage());
            combox_last_used.setItems(javafx.collections.FXCollections.observableArrayList());
            combox_last_used.getSelectionModel().clearSelection();
            combox_last_used.setValue(null);
        });


        new Thread(task).start();
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