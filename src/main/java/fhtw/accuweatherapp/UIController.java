package fhtw.accuweatherapp;

import fhtw.accuweatherapp.handler.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import server_client.clients.ClientConnection;


public class UIController {


    @FXML
    private TextField txt_field_city;
    @FXML
    private TextArea txt_field_cur_weather;
    @FXML
    private ComboBox<String> combox_last_used;
    @FXML
    private MenuButton menu_user;
    @FXML
    private CheckBox check_feels_like;
    @FXML
    private CheckBox check_humidity;
    @FXML
    private CheckBox check_wind;
    @FXML
    private BorderPane main_pane;
    @FXML
    private Button btn_uimode;
    @FXML
    private TextArea txt_field_day1;
    @FXML
    private TextArea txt_field_day2;
    @FXML
    private TextArea txt_field_day3;
    @FXML
    private TextArea txt_field_day4;
    @FXML
    private TextArea txt_field_day5;
    @FXML
    private ListView<String> list_favorites;
    @FXML
    private Button btn_safe_favourite;


    private final ClientConnection connection = new ClientConnection("localhost", 8080);


    private final UIFavoritesHandler favoritesHandler = new UIFavoritesHandler(connection);
    private final UIWeatherHandler weatherHandler = new UIWeatherHandler(connection);
    private final UISettingsHandler settingsHandler = new UISettingsHandler(connection);
    private final UIHistoryHandler historyHandler = new UIHistoryHandler(connection);
    private final UIOfflineHandler offlineHandler = new UIOfflineHandler(connection);
    private final UIUserHandler userHandler = new UIUserHandler(connection);

    @FXML
    public void initialize() {

        combox_last_used.setOnAction(event -> {
            String selectedCity = combox_last_used.getValue();
            if (selectedCity != null && !selectedCity.equals(txt_field_city.getText())) {
                txt_field_city.setText(selectedCity);
                onSearch();
            }
        });
        historyHandler.updateCityComboBox(combox_last_used);


        list_favorites.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 1) {
                String selectedCity = list_favorites.getSelectionModel().getSelectedItem();
                if (selectedCity != null) {
                    txt_field_city.setText(selectedCity);
                    onSearch();
                }
            }
        });
    }


    @FXML
    protected void onSearch() {
        String city = txt_field_city.getText();


        String validationError = weatherHandler.validateCity(city);
        if (validationError != null) {
            txt_field_cur_weather.setText(validationError);
            return;
        }

        String trimmedCity = city.trim();
        txt_field_cur_weather.setText("Loading weather data for: " + trimmedCity + "...");
        weatherHandler.setForecastLoading(txt_field_day1, txt_field_day2, txt_field_day3,
                txt_field_day4, txt_field_day5);


        loadCurrentWeather(trimmedCity);


        loadForecastOrHistory(trimmedCity);


        favoritesHandler.checkFavoriteStatus(trimmedCity,
                () -> favoritesHandler.updateStarStyle(btn_safe_favourite, settingsHandler.isDarkMode()));


        historyHandler.updateCityComboBox(combox_last_used);
    }

    private void loadCurrentWeather(String city) {
        weatherHandler.loadCurrentWeather(
                city,
                settingsHandler.getUnit(),
                settingsHandler.isFeelsLikeChecked(),
                settingsHandler.isHumidityChecked(),
                settingsHandler.isWindChecked(),
                resp -> txt_field_cur_weather.setText(resp),
                error -> txt_field_cur_weather.setText(error)
        );
    }

    private void loadForecastOrHistory(String city) {
        weatherHandler.loadForecastOrHistory(
                city,
                settingsHandler.getUnit(),
                settingsHandler.isHistoryChecked(),
                settingsHandler.isFeelsLikeChecked(),
                settingsHandler.isHumidityChecked(),
                settingsHandler.isWindChecked(),
                dataArray -> weatherHandler.populateForecastFields(dataArray,
                        txt_field_day1, txt_field_day2, txt_field_day3, txt_field_day4, txt_field_day5),
                error -> weatherHandler.setForecastError(error,
                        txt_field_day1, txt_field_day2, txt_field_day3, txt_field_day4, txt_field_day5)
        );
    }


    @FXML
    protected void onSafeFavourite() {
        favoritesHandler.toggleFavorite(
                txt_field_city.getText(),
                msg -> {
                    txt_field_cur_weather.setText(msg);
                    favoritesHandler.updateStarStyle(btn_safe_favourite, settingsHandler.isDarkMode());
                    favoritesHandler.loadFavorites(list_favorites);
                },
                error -> txt_field_cur_weather.setText(error)
        );
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

    private void switchUser(String username) {
        txt_field_cur_weather.setText("Switching user...");

        userHandler.switchUser(
                username,
                settings -> {

                    userHandler.updateUserMenu(menu_user, settings.username);


                    settingsHandler.applySettings(
                            settings.showHumidity,
                            settings.showWind,
                            settings.showFeelsLike,
                            settings.unit,
                            check_humidity,
                            check_wind,
                            check_feels_like
                    );

                    txt_field_cur_weather.setText("User switched to: " + settings.username);
                    historyHandler.updateCityComboBox(combox_last_used);


                    if (!settings.standardCity.isEmpty()) {
                        txt_field_city.setText(settings.standardCity);
                        loadCurrentWeather(settings.standardCity);
                        loadForecastOrHistory(settings.standardCity);
                        favoritesHandler.checkFavoriteStatus(settings.standardCity,
                                () -> favoritesHandler.updateStarStyle(btn_safe_favourite, settingsHandler.isDarkMode()));
                    } else {
                        refreshWeatherIfPossible();
                    }


                    favoritesHandler.loadFavorites(list_favorites);
                },
                error -> txt_field_cur_weather.setText(error)
        );
    }


    @FXML
    protected void onUnitC() {
        settingsHandler.setUnitCelsius(resp -> txt_field_cur_weather.setText("Unit set to Celsius."));
    }

    @FXML
    protected void onUnitF() {
        settingsHandler.setUnitFahrenheit(resp -> txt_field_cur_weather.setText("Unit set to Fahrenheit."));
    }

    @FXML
    protected void onCheckFeelsLike() {
        settingsHandler.toggleFeelsLike(this::refreshWeatherIfPossible);
    }

    @FXML
    protected void onCheckHumidity() {
        settingsHandler.toggleHumidity(this::refreshWeatherIfPossible);
    }

    @FXML
    protected void onCheckWind() {
        settingsHandler.toggleWind(this::refreshWeatherIfPossible);
    }

    @FXML
    protected void onCheckHistory() {
        settingsHandler.toggleHistoryMode(msg -> {
            txt_field_cur_weather.setText(msg);

            String city = txt_field_city.getText();
            if (city != null && city.trim().length() >= 3) {
                onSearch();
            }
        });
    }

    @FXML
    protected void onuimode() {
        settingsHandler.toggleDarkMode(main_pane, btn_uimode, txt_field_city,
                msg -> txt_field_cur_weather.setText(msg));
        favoritesHandler.updateStarStyle(btn_safe_favourite, settingsHandler.isDarkMode());
    }

    @FXML
    protected void onsetasstandard() {
        settingsHandler.setStandardCity(
                txt_field_city.getText(),
                resp -> txt_field_cur_weather.setText(resp),
                error -> txt_field_cur_weather.setText(error)
        );
    }


    @FXML
    protected void onReport() {
        if (settingsHandler.isHistoryChecked()) {

            txt_field_cur_weather.setText("Exporting 30-day historical data to CSV...");
            historyHandler.exportHistoricalDataCSV(
                    txt_field_city.getText(),
                    settingsHandler.getUnit(),
                    resp -> txt_field_cur_weather.setText(resp),
                    error -> txt_field_cur_weather.setText(error)
            );
        } else {

            historyHandler.showHistory(resp -> txt_field_cur_weather.setText(resp));
        }
    }


    @FXML
    protected void onOffline() {
        String city = txt_field_city.getText();

        if (city == null || city.trim().isEmpty()) {

            txt_field_cur_weather.setText("Loading offline data...");
            offlineHandler.loadAllOfflineData(
                    resp -> txt_field_cur_weather.setText(resp),
                    txt_field_day1, txt_field_day2, txt_field_day3, txt_field_day4, txt_field_day5
            );
        } else {

            txt_field_cur_weather.setText("Downloading offline data for: " + city.trim() + "...");
            offlineHandler.saveOfflineData(
                    city.trim(),
                    settingsHandler.getUnit(),
                    resp -> txt_field_cur_weather.setText(resp),
                    error -> txt_field_cur_weather.setText(error)
            );
        }
    }


    private void refreshWeatherIfPossible() {
        String city = txt_field_city.getText();
        if (city != null && city.trim().length() >= 3) {
            loadCurrentWeather(city.trim());
            loadForecastOrHistory(city.trim());
        }
    }
}
