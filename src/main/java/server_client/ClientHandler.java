package server_client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.StringWriter;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        System.out.println("Client request processing: " + clientSocket.getInetAddress().getHostAddress());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String clientMessage;


            while ((clientMessage = reader.readLine()) != null) {
                String trimmedMessage = clientMessage.trim();
                System.out.println("Received command: " + trimmedMessage);

                if ("PING".equalsIgnoreCase(trimmedMessage)) {
                    writer.println("PONG");
                    System.out.println("Server responded: PONG");
                }
                else if (trimmedMessage.startsWith("GET_WEATHER:")) {
                    String city = trimmedMessage.substring("GET_WEATHER:".length()).trim();

                    String weatherData = "Status: OK | Current time " + city + " is being processed.";
                    writer.println(weatherData);
                }
                else if ("QUIT".equalsIgnoreCase(trimmedMessage)) {
                    writer.println("BYE");
                    break;
                }
                else {

                    writer.println("ERROR: Unknown command. Supported: PING, GET_WEATHER:[CITY], QUIT");
                }
            }
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.err.println("Client disconnected or I/O error: " + clientSocket.getInetAddress().getHostAddress() + " - " + e.getMessage());
            System.err.println(sw);
        } finally {
            try {
                clientSocket.close();
                System.out.println("Connection closed for client: " + clientSocket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                System.err.println(sw);
            }
        }
    }
}
