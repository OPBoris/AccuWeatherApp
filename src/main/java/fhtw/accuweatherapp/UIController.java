package fhtw.accuweatherapp;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import server_client.ClientConnection;

import java.util.List;

public class UIController {
    @FXML private TextField txt_field_city;
    @FXML private TextArea txt_field_cur_weather;
    @FXML private ComboBox<String> combox_last_used;
    @FXML private MenuButton menu_user;
    @FXML private CheckBox check_feels_like;
    @FXML private CheckBox check_humidity;
    @FXML private CheckBox check_wind;

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
        switchUser("MORITZ");
    }

    @FXML
    protected void onJan() {
        switchUser("JAN");
    }

    @FXML
    protected void onBoris() {
        switchUser("BORIS");
    }

    @FXML
    protected void onSearch() {
        String city = txt_field_city.getText();

        if (city != null && !city.trim().isEmpty()) {
            txt_field_cur_weather.setText("Searching for weather data for: " + city);
            runCommand("GET_WEATHER " + city.trim() + " " + unit);
            updateCityComboBox();
        } else {
            txt_field_cur_weather.setText("Please enter a city.");
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
        runCommand("SET_UNIT C");
    }

    @FXML
    protected void onUnitF() {
        unit = "F";
        runCommand("SET_UNIT F");
    }

    @FXML
    protected void onCheckFeelsLike() {
        feelsLikeChecked = !feelsLikeChecked;
        runCommandAndRefresh("CHECK_FEELS_LIKE");
    }

    @FXML
    protected void onCheckHumidity() {
        humidityChecked = !humidityChecked;
        runCommandAndRefresh("CHECK_HUMIDITY");
    }

    @FXML
    protected void onCheckWind() {
        windChecked = !windChecked;
        runCommandAndRefresh("CHECK_WIND");
    }

    @FXML
    protected void onuimode() {
        txt_field_cur_weather.setText("Pressed on UI Mode button.");
    }

    @FXML
    protected void onsetasstandard() {
        String city = txt_field_city.getText().trim();
        if (!city.isEmpty()) {
            runCommand("SET_STANDARD " + city.trim());
        }else {
            txt_field_cur_weather.setText("Bitte eine Stadt eingeben.");
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

    private void runCommandAndRefresh(String command) {
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                String resp = connection.sendCommand(command);

                String city = txt_field_city.getText();
                if (city != null && !city.trim().isEmpty()) {
                    return connection.sendCommand("GET_WEATHER " + city.trim() + " " + unit);
                }
                return resp;
            }
        };

        task.setOnSucceeded(e -> {
            String resp = task.getValue();
            if(resp != null) {
                txt_field_cur_weather.setText(resp);
            }
        });

        new Thread(task).start();
    }

    private void switchUser(String command) {
        txt_field_cur_weather.setText("Wechsle Nutzer...");

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                return connection.sendCommand(command);
            }
        };

        task.setOnSucceeded(e -> {
            String response = task.getValue();

            if (response != null && response.startsWith("SETTINGS;")) {
                String[] parts = response.split(";");
                if (parts.length >= 5) {
                    String username = parts[1];
                    boolean showHum = Boolean.parseBoolean(parts[2]);
                    boolean showWind = Boolean.parseBoolean(parts[3]);
                    boolean showFeels = Boolean.parseBoolean(parts[4]);
                    String standardCity;
                    if (parts.length > 5) {
                        standardCity = parts[5];
                    } else {
                        standardCity = "";
                    }
                    if (parts.length > 6) {
                        this.unit = parts[6];
                    }

                    menu_user.setText(username);
                    updateCityComboBox();
                    txt_field_cur_weather.setText("User switched to: " + username);

                    this.humidityChecked = showHum;
                    if(check_humidity != null) check_humidity.setSelected(showHum);

                    this.windChecked = showWind;
                    if(check_wind != null) check_wind.setSelected(showWind);

                    this.feelsLikeChecked = showFeels;
                    if(check_feels_like != null) check_feels_like.setSelected(showFeels);

                    if (!standardCity.isEmpty()) {
                        txt_field_city.setText(standardCity);
                        runCommand("GET_WEATHER " + standardCity + " " + unit);
                    } else {
                        refreshWeatherIfPossible();
                    }
                }
            } else {
                txt_field_cur_weather.setText(response);
            }
        });

        task.setOnFailed(e -> txt_field_cur_weather.setText("Error user switching."));
        new Thread(task).start();
    }

    private void refreshWeatherIfPossible() {
        String city = txt_field_city.getText();
        if (city != null && !city.trim().isEmpty()) {
            runCommand("GET_WEATHER " + city.trim() + " " + unit);
        }
    }
}