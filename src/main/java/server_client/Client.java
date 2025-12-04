package server_client;

import fhtw.accuweatherapp.UI;
import javafx.application.Application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("--- Starting AccuWeather Test Client ---");

        try (
                Socket socket = new Socket(HOST, SERVER_PORT);
                /*PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Scanner scanner = new Scanner(System.in);*/
        ) {
            System.out.println("Successfully connected to server at " + HOST + ":" + SERVER_PORT);
            Application.launch(UI.class, args);

        } catch (IOException e) {
            System.err.println("Error connecting or communicating with server: " + e.getMessage());
            System.err.println("Check that WeatherServer is running and listening on port " + SERVER_PORT + " (host: " + HOST + ")");

        } finally {
            System.out.println("Client closed.");
        }
    }
}
