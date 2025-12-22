package fhtw.accuweatherapp;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import server_client.WeatherService;

public class UIController {
    @FXML
    private TextField txt_field_city;

    @FXML
    private TextArea txt_field_cur_weather;

    private final WeatherService weatherService;
    private String currentUnit = "C";
    private String currentUser = "Guest";

    public UIController() {
        this.weatherService = new WeatherService();
    }

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

        if (city == null || city.trim().isEmpty()) {
            txt_field_cur_weather.setText("Please enter a city name.");
            return;
        }

        txt_field_cur_weather.setText("Loading weather data for " + city + "...");

        new Thread(() -> {
            try {
                String weatherData = weatherService.getWeatherByCity(city, currentUnit, currentUser);
                javafx.application.Platform.runLater(() -> txt_field_cur_weather.setText(weatherData));
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> txt_field_cur_weather.setText("ERROR: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    protected void onReport() {
        txt_field_cur_weather.setText("Pressed on Report button.");

    }

    @FXML
    protected void onOffline() {
        String history = weatherService.getRecentCities(currentUser);
        if (history == null || history.isEmpty()) {
            txt_field_cur_weather.setText("No history available for user: " + currentUser);
        } else {
            txt_field_cur_weather.setText("Recent searches for " + currentUser + ":\n\n" + history.replace(",", "\n"));
        }
    }

    @FXML
    protected void onUnitC() {
        currentUnit = "C";
        txt_field_cur_weather.setText("Temperature unit: Celsius");
    }

    @FXML
    protected void onUnitF() {
        currentUnit = "F";
        txt_field_cur_weather.setText("Temperature unit: Fahrenheit");
    }
}