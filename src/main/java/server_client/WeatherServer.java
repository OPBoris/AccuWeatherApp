package server_client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherServer {
    private static final int PORT = 8080;
    private static final ExecutorService clientPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        System.out.println("--- AccuWeather Server starting on port " + PORT + " ---");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novi klijent povezan: " + clientSocket.getInetAddress().getHostAddress());

                clientPool.execute(new ClientHandler(clientSocket));
            }

        } catch (IOException e) {
            System.err.println("FATAL ERROR: Unable to start or accept the connection.");

            System.err.println("Message error: " + e.getMessage());
            e.printStackTrace();
            clientPool.shutdown();
        }
    }
}
