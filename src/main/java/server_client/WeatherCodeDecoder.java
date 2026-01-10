package server_client;

/**
 * Utility class for decoding WMO Weather Codes
 * Based on WMO 4677 weather codes used by Open-Meteo
 */
public class WeatherCodeDecoder {

    /**
     * Decode WMO Weather Code to human-readable description
     *
     * @param code WMO weather code (0-99)
     * @return Human-readable weather description
     */
    public static String decode(int code) {
        switch (code) {
            case 0:
                return "Clear sky";
            case 1:
                return "Mainly clear";
            case 2:
                return "Partly cloudy";
            case 3:
                return "Overcast";
            case 45:
                return "Foggy";
            case 48:
                return "Depositing rime fog";
            case 51:
                return "Light drizzle";
            case 53:
                return "Moderate drizzle";
            case 55:
                return "Dense drizzle";
            case 61:
                return "Slight rain";
            case 63:
                return "Moderate rain";
            case 65:
                return "Heavy rain";
            case 71:
                return "Slight snow";
            case 73:
                return "Moderate snow";
            case 75:
                return "Heavy snow";
            case 80:
                return "Slight rain showers";
            case 81:
                return "Moderate rain showers";
            case 82:
                return "Violent rain showers";
            case 85:
                return "Slight snow showers";
            case 86:
                return "Heavy snow showers";
            case 95:
                return "Thunderstorm";
            case 96:
                return "Thunderstorm with slight hail";
            case 99:
                return "Thunderstorm with heavy hail";
            default:
                return "Unknown (" + code + ")";
        }
    }
}

