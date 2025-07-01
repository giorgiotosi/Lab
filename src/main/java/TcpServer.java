import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class TcpServer {
    private int port;
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public static Map<Integer, Proiettore> mappaProiettori = new HashMap<>();

    // Lista thread client e socket per chiuderli a stop
    private List<Socket> clientSockets = new CopyOnWriteArrayList<>();

    public TcpServer(int port) {
        this.port = port;
    }

    public void start() {
        running = true;

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("Server in ascolto sulla porta " + port);

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        clientSockets.add(clientSocket); // tieni traccia

                        // Lancia un thread per gestire il client, così non blocchi il server
                        new Thread(() -> handleClient(clientSocket)).start();

                    } catch (SocketException e) {
                        if (!running) {
                            System.out.println("ServerSocket chiuso, esco dal ciclo.");
                        } else {
                            e.printStackTrace();
                        }
                        break;
                    } catch (IOException e) {
                        if (running) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Server terminato.");
        }).start();
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                PacchettoDaClient pacchettoDaClient = new PacchettoDaClient(line);
                String indice = line.split("#")[0];

                try {


                    int idClient = Integer.parseInt(indice)+1;

                    Proiettore proiettore = mappaProiettori.get(idClient);
                    if (proiettore != null) {
                        System.out.println("Proiettore con idClientTCP " + idClient + " trovato!!");
                        proiettore.aggiungiPacchetto(pacchettoDaClient);  // oppure un metodo tipo proiettore.invia(pacchetto)

                    } else {
                        //System.out.println("Proiettore con idClientTCP " + idClient + " non trovato");
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Errore di parsing: la parte prima del # non è un numero intero.");
                }
                //System.out.println("Ricevuto: " + line);
            }
        } catch (IOException e) {
            if (running) e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                clientSockets.remove(clientSocket);
            } catch (IOException ignored) {}
        }
    }

    public void stop() {
        running = false;

        // Chiudi tutti i client attivi
        for (Socket s : clientSockets) {
            try {
                s.close();
            } catch (IOException ignored) {}
        }
        clientSockets.clear();

        // Chiudi serverSocket per sbloccare accept()
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
