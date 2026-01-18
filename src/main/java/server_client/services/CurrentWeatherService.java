package server_client.services;

import server_client.api.ApiClient;
import server_client.api.ApiUrls;
import server_client.JsonParser;
import server_client.WeatherCodeDecoder;
import server_client.exceptions.WeatherAppException;


public class CurrentWeatherService {

    private final ApiClient apiClient;

    public CurrentWeatherService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }


    public String getCurrentWeather(double lat, double lon, String unit,
                                    boolean showHumidity, boolean showWind, boolean showFeelsLike) throws WeatherAppException {
        try {
            String tempUnit;
            if (unit.equalsIgnoreCase("F")) {
                tempUnit = "fahrenheit";
            } else {
                tempUnit = "celsius";
            }
            String url = String.format(ApiUrls.CURRENT_WEATHER, lat, lon, tempUnit);
            String data = apiClient.makeOpenMeteoCall(url);
            return processCurrentWeather(data, unit, showFeelsLike, showHumidity, showWind);
        } catch (WeatherAppException e) {
            throw e;
        } catch (Exception e) {
            throw new WeatherAppException("Error retrieving current weather.", e);
        }
    }


    private String processCurrentWeather(String jsonData, String unit,
                                         boolean showFeelsLike, boolean showHumidity, boolean showWind) throws WeatherAppException {
        if (jsonData == null || !jsonData.contains("\"current\"")) {
            throw new WeatherAppException("Weather data is incomplete or empty.");
        }

        StringBuilder sb = new StringBuilder();

        try {
            int currentStart = jsonData.indexOf("\"current\":");
            if (currentStart == -1) {
                return "ERROR: Unable to fetch weather data.";
            }

            int objectStart = jsonData.indexOf("{", currentStart);
            int objectEnd = JsonParser.findMatchingBrace(jsonData, objectStart);
            String currentBlock = jsonData.substring(objectStart, objectEnd + 1);

            double temp = JsonParser.parseDoubleValue(currentBlock, "temperature_2m");
            double feelsLike = JsonParser.parseDoubleValue(currentBlock, "apparent_temperature");
            int humidity = JsonParser.parseIntValue(currentBlock, "relative_humidity_2m");
            double windSpeed = JsonParser.parseDoubleValue(currentBlock, "wind_speed_10m");
            int weatherCode = JsonParser.parseIntValue(currentBlock, "weather_code");

            String weatherDescription = WeatherCodeDecoder.decode(weatherCode);

            sb.append("CURRENT WEATHER:\n");
            sb.append(String.format("Temperature: %.1f°%s\n", temp, unit));
            sb.append("Weather: ").append(weatherDescription).append("\n");

            if (showFeelsLike) {
                sb.append(String.format("Feels like: %.1f°%s\n", feelsLike, unit));
            }

            if (showHumidity) {
                sb.append(String.format("Humidity: %d%%\n", humidity));
            }

            if (showWind) {
                sb.append(String.format("Wind Speed: %.1f km/h\n", windSpeed));
            }

            return sb.toString();
        } catch (Exception e) {
            throw new WeatherAppException("Error processing weather data.", e);
        }
    }
}

