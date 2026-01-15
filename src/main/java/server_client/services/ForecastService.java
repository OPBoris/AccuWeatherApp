package server_client.services;

import server_client.ApiClient;
import server_client.ApiUrls;
import server_client.JsonParser;
import server_client.WeatherCodeDecoder;

import java.util.List;


public class ForecastService {

    private final ApiClient apiClient;

    public ForecastService(ApiClient apiClient) {
        this.apiClient = apiClient;
    }


    public String getForecast(double lat, double lon, String unit,
                              boolean showFeelsLike, boolean showHumidity, boolean showWind) {
        try {
            String tempUnit;
            if (unit.equalsIgnoreCase("F")) {
                tempUnit = "fahrenheit";
            } else {
                tempUnit = "celsius";
            }
            String url = String.format(ApiUrls.FORECAST, lat, lon, tempUnit);
            String data = apiClient.makeOpenMeteoCall(url);
            return processForecast(data, unit, showFeelsLike, showHumidity, showWind);
        } catch (Exception e) {
            System.err.println("Error fetching weather data: " + e.getMessage());
            return "ERROR: Can't fetch weather data. Error: " + e.getMessage();
        }
    }

    private String processForecast(String jsonData, String unit,
                                   boolean showFeelsLike, boolean showHumidity, boolean showWind) {
        try {
            if (jsonData == null || !jsonData.contains("\"daily\"")) {
                return "ERROR: Unable to fetch forecast data.";
            }

            StringBuilder result = new StringBuilder();

            int dailyStart = jsonData.indexOf("\"daily\":");
            if (dailyStart == -1) {
                return "ERROR: Unable to fetch forecast data.";
            }

            int objectStart = jsonData.indexOf("{", dailyStart);
            int objectEnd = JsonParser.findMatchingBrace(jsonData, objectStart);
            String dailyBlock = jsonData.substring(objectStart, objectEnd + 1);

            List<String> times = JsonParser.parseArrayValues(dailyBlock, "time");
            List<String> tempMax = JsonParser.parseArrayValues(dailyBlock, "temperature_2m_max");
            List<String> tempMin = JsonParser.parseArrayValues(dailyBlock, "temperature_2m_min");
            List<String> weatherCodes = JsonParser.parseArrayValues(dailyBlock, "weather_code");
            List<String> precipProb = JsonParser.parseArrayValues(dailyBlock, "precipitation_probability_max");
            List<String> windSpeed = JsonParser.parseArrayValues(dailyBlock, "wind_speed_10m_max");
            List<String> humidity = JsonParser.parseArrayValues(dailyBlock, "relative_humidity_2m_max");
            List<String> feelsLike = JsonParser.parseArrayValues(dailyBlock, "apparent_temperature_max");

            int daysCount = Math.min(5, times.size());

            for (int dayIndex = 0; dayIndex < daysCount; dayIndex++) {
                String dateStr = times.get(dayIndex).replace("\"", "");
                double tempMaxVal = Double.parseDouble(tempMax.get(dayIndex));
                double tempMinVal = Double.parseDouble(tempMin.get(dayIndex));
                int weatherCode = Integer.parseInt(weatherCodes.get(dayIndex));
                int rainProbability = Integer.parseInt(precipProb.get(dayIndex));
                double windSpeedVal = Double.parseDouble(windSpeed.get(dayIndex));

                double feelsLikeVal = Double.NaN;
                if (feelsLike != null && dayIndex < feelsLike.size() && !feelsLike.get(dayIndex).equals("null")) {
                    feelsLikeVal = Double.parseDouble(feelsLike.get(dayIndex));
                }

                int humidityVal = -1;
                if (humidity != null && dayIndex < humidity.size() && !humidity.get(dayIndex).equals("null")) {
                    humidityVal = Integer.parseInt(humidity.get(dayIndex));
                }

                String weatherDescription = WeatherCodeDecoder.decode(weatherCode);

                StringBuilder dayForecast = new StringBuilder();
                dayForecast.append(dateStr).append("\n");
                dayForecast.append("Max: ").append(String.format("%.1f", tempMaxVal)).append("°").append(unit).append("\n");
                dayForecast.append("Min: ").append(String.format("%.1f", tempMinVal)).append("°").append(unit).append("\n");
                dayForecast.append("Weather: ").append(weatherDescription).append("\n");
                dayForecast.append("Rain: ").append(rainProbability).append("%");
                if (showWind) {
                    dayForecast.append("\nWind: ").append(String.format("%.1f", windSpeedVal)).append(" km/h");
                }
                if (showFeelsLike && !Double.isNaN(feelsLikeVal)) {
                    dayForecast.append("\nFeels Like: ").append(String.format("%.1f", feelsLikeVal)).append("°").append(unit);
                }
                if (showHumidity && humidityVal >= 0) {
                    dayForecast.append("\nHumidity: ").append(humidityVal).append("%");
                }

                if (dayIndex > 0) {
                    result.append("|||");
                }
                result.append(dayForecast);
            }

            return result.toString();
        } catch (Exception e) {
            System.err.println("Error processing forecast: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
