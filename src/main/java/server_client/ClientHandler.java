package server_client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
                System.out.println("Recieved command: " + trimmedMessage);

                if ("PING".equalsIgnoreCase(trimmedMessage)) {
                    writer.println("PONG");
                    System.out.println("Server odgovorio: PONG");
                }
                else if (trimmedMessage.startsWith("GET_WEATHER:")) {
                    String city = trimmedMessage.substring("GET_WEATHER:".length()).trim();

                    // TODO: P2 API
                    String weatherData = "Status: OK | Aktuell time " + city + " se obrađuje.";
                    writer.println(weatherData);
                }
                else if ("QUIT".equalsIgnoreCase(trimmedMessage)) {
                    writer.println("BYE");
                    break;
                }
                else {

                    writer.println("ERROR: Nepoznata komanda. Podržane: PING, GET_WEATHER:[GRAD], QUIT");
                }
            }
        } catch (IOException e) {
            // Ako dođe do prekida veze ili I/O greške
            System.err.println("Klijent se diskonektovao ili I/O greska: " + clientSocket.getInetAddress().getHostAddress() + " - " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // Uvek zatvori socket, bez obzira na ishod
                clientSocket.close();
                System.out.println("Konekcija zatvorena za klijenta: " + clientSocket.getInetAddress().getHostAddress());
            } catch (IOException e) {
                // Ignorišemo greške pri zatvaranju
                e.printStackTrace();
            }
        }
    }
}
