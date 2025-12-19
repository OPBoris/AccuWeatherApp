package server_client;

import fhtw.accuweatherapp.UI;
import javafx.application.Application;

import java.io.IOException;
import java.net.Socket;

public class Client {
    private static final String HOST = "localhost";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("--- Starting AccuWeather Client ---");
        UI.main(args);
    }
}
