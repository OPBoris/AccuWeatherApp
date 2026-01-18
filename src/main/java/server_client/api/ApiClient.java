package server_client.api;

import server_client.exceptions.WeatherAppException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class ApiClient {

    public ApiClient() {
    }

    public String makeOpenMeteoCall(String urlString) throws WeatherAppException {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();

            } else {
                throw new WeatherAppException("Open-Meteo API Error: HTTP " + responseCode);
            }

        } catch (java.net.SocketTimeoutException e) {
            throw new WeatherAppException("Connection timeout to weather API.", e);
        } catch (IOException e) {
            throw new WeatherAppException("Network error while retrieving data.", e);
        } catch (Exception e) {
            throw new WeatherAppException("Unknown error in API client.", e);
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}

