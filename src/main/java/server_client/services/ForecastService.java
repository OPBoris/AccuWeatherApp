package server_client.services;

import com.fasterxml.jackson.databind.JsonNode;
import server_client.ApiClient;
import server_client.ApiUrls;
import server_client.WeatherCodeDecoder;


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
            JsonNode data = apiClient.makeOpenMeteoCall(url);
            return processForecast(data, unit, showFeelsLike, showHumidity, showWind);
        } catch (Exception e) {
            System.err.println("Error fetching weather data: " + e.getMessage());
            return "ERROR: Can't fetch weather data. Error: " + e.getMessage();
        }
    }

    private String processForecast(JsonNode forecastData, String unit,
                                   boolean showFeelsLike, boolean showHumidity, boolean showWind) {
        try {
            if (forecastData == null || !forecastData.has("daily")) {
                return "ERROR: Unable to fetch forecast data.";
            }

            StringBuilder result = new StringBuilder();
            JsonNode daily = forecastData.get("daily");

            JsonNode times = daily.get("time");
            JsonNode tempMax = daily.get("temperature_2m_max");
            JsonNode tempMin = daily.get("temperature_2m_min");
            JsonNode weatherCodes = daily.get("weather_code");
            JsonNode precipProb = daily.get("precipitation_probability_max");
            JsonNode windSpeed = daily.get("wind_speed_10m_max");
            JsonNode humidity = daily.get("relative_humidity_2m_max");
            JsonNode feelsLike = daily.get("apparent_temperature_max");

            int daysCount = Math.min(5, times.size());

            for (int dayIndex = 0; dayIndex < daysCount; dayIndex++) {
                String dateStr = times.get(dayIndex).asText();
                double tempMaxVal = tempMax.get(dayIndex).asDouble();
                double tempMinVal = tempMin.get(dayIndex).asDouble();
                int weatherCode = weatherCodes.get(dayIndex).asInt();
                int rainProbability = precipProb.get(dayIndex).asInt();
                double windSpeedVal = windSpeed.get(dayIndex).asDouble();


                double feelsLikeVal = (feelsLike != null && feelsLike.has(dayIndex) && !feelsLike.get(dayIndex).isNull())
                    ? feelsLike.get(dayIndex).asDouble()
                    : Double.NaN;

                int humidityVal = (humidity != null && humidity.has(dayIndex) && !humidity.get(dayIndex).isNull())
                    ? humidity.get(dayIndex).asInt()
                    : -1;

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
