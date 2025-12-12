package fhtw.accuweatherapp;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class UIController {
    @FXML
    private TextField txt_field_city;

    @FXML
    private TextArea txt_field_cur_weather;

    @FXML
    protected void onSafeFavourite() {
        txt_field_cur_weather.setText("Pressed on Favourite.");
    }

    @FXML
    protected void onMoritz() {
        txt_field_cur_weather.setText("Pressed on Moritz.");
    }

    @FXML
    protected void onJan() {
        txt_field_cur_weather.setText("Pressed on Jan.");
    }

    @FXML
    protected void onBoris() {
        txt_field_cur_weather.setText("Pressed on Boris.");
    }

    @FXML
    protected void onSearch() {
        String city = txt_field_city.getText();

        if (city != null && !city.trim().isEmpty()) {
            txt_field_cur_weather.setText("Suche nach Wetter für: " + city);
        } else {
            txt_field_cur_weather.setText("Bitte geben Sie eine Stadt ein.");
        }
    }

    @FXML
    protected void onReport() {
        txt_field_cur_weather.setText("Pressed on Report button.");

    }

    @FXML
    protected void onOffline() {
        txt_field_cur_weather.setText("Pressed on Offline button.");
    }

    @FXML
    protected void onUnitC() {
        txt_field_cur_weather.setText("Pressed on Unit C button.");
    }

    @FXML
    protected void onUnitF() {
        txt_field_cur_weather.setText("Pressed on Unit F button.");
    }
}