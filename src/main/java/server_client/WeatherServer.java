package server_client;
//BIBs entfernt
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class WeatherServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        System.out.println("--- AccuWeather Server starting on port " + PORT + " ---");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress().getHostAddress());

                    ClientHandler handler = new ClientHandler(clientSocket);
                    Thread clientThread = new Thread(handler);

                    clientThread.start();
                } catch (IOException e) {
                    System.err.println("Connection error with a client: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("FATAL ERROR: Unable to start or accept the connection.");
            System.err.println("Message error: " + e.getMessage());
        }
    }
}
