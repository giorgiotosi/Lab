import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import javax.websocket.DeploymentException;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.core.type.TypeReference;
import org.glassfish.tyrus.server.Server;


public class Main {

    public static Map<Integer, DatiMisura> datiMisureMap = new LinkedHashMap<>();
    public static Map<String, Integer> associazioni = new HashMap<>();
    public static List<String> nomiPorteDisponibili = new ArrayList<>();

    public static void main(String[] args) throws IOException, InterruptedException, DeploymentException {
        Thread wsThread = new Thread(() -> {
            Server wsServer = new Server("localhost", 8025, "/", null, GraficoSocket.class);
            try {
                wsServer.start();
                System.out.println("WebSocket server avviato.");
                Thread.currentThread().join();  // blocca il thread qui, ricezione perenne
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        wsThread.start();


        SerialPort[] porte = SerialPort.getCommPorts();
        for (SerialPort porta : porte) {
            nomiPorteDisponibili.add(porta.getSystemPortName());
        }

        startHttpServerPerAssociazioni(3400);

        System.out.println("⏳ In attesa della mappa dal frontend...");
        while (associazioni.isEmpty()) {
            Thread.sleep(200);
        }
        System.out.println("✅ Mappa ricevuta. Avvio del sistema...");

        List<Proiettore> porteSeriali = new ArrayList<>();
        avvio(porteSeriali, associazioni);
        Thread.sleep(10);


        TcpServer server = new TcpServer(3333);
        server.start();

//        for (Proiettore p : TcpServer.mappaProiettori.values()) {
//            p.proceduraOUTPUT_CH();
//        }

        try {
            Thread.sleep(1000000); // 10 secondi
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        server.stop();
        wsThread.stop();
    }

    private static void avvio(List<Proiettore> proiettori, Map<String, Integer> associazioni) throws InterruptedException {
        int count = 0;
        for (String p : associazioni.keySet()) {
            Proiettore ps = new Proiettore(p, associazioni.get(p));
            proiettori.add(ps);

            if (proiettori.get(count).prepareAndOpenPort()) {
                Thread t = new Thread(proiettori.get(count));
                t.start();
                Thread.sleep(50);
            } else {
                System.err.println("Non riesco ad aprire la porta " + p);
            }
            count++;
        }

        TcpServer.mappaProiettori = proiettori.stream()
                .collect(Collectors.toMap(p -> p.getIdClientTCP(), p -> p));
    }

    public static void startHttpServerPerAssociazioni(int porta) {
        try {
            HttpServer serverHttp = HttpServer.create(new java.net.InetSocketAddress(porta), 0);

            serverHttp.createContext("/api/porte", exchange -> {
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                    exchange.sendResponseHeaders(204, -1); // No content
                    return;
                }
                if ("GET".equals(exchange.getRequestMethod())) {
                    ObjectMapper mapper = new ObjectMapper();
                    String response = mapper.writeValueAsString(nomiPorteDisponibili);
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*"); // CORS
                    byte[] bytes = response.getBytes();
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.getResponseBody().close();
                } else {
                    exchange.sendResponseHeaders(405, 0);
                    exchange.getResponseBody().close();
                }
            });

            serverHttp.createContext("/api/associazioni", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    if ("OPTIONS".equals(exchange.getRequestMethod())) {
                        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                        exchange.sendResponseHeaders(204, -1);
                        return;
                    }
                    if ("POST".equals(exchange.getRequestMethod())) {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Integer> nuovaMappa = mapper.readValue(exchange.getRequestBody(), new TypeReference<Map<String, Integer>>() {});
                        associazioni.clear();
                        associazioni.putAll(nuovaMappa);
                        System.out.println("✅ Mappa ricevuta dal frontend:");
                        associazioni.forEach((k, v) -> System.out.println("  " + k + " → " + v));
                        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                        exchange.sendResponseHeaders(200, 0);
                        exchange.getResponseBody().close();
                    } else {
                        exchange.sendResponseHeaders(405, 0);
                        exchange.getResponseBody().close();
                    }
                }
            });

            serverHttp.createContext("/api/procedura/taratura", exchange -> {
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }
                if ("POST".equals(exchange.getRequestMethod())) {
                    new Thread(() -> {
                        TcpServer.mappaProiettori.values().forEach(proiettore -> {
                            try {
                                proiettore.proceduraTaratura();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    }).start();

                    String response = "Procedura Taratura avviata";
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                } else {
                    exchange.sendResponseHeaders(405, 0);
                    exchange.getResponseBody().close();
                }
            });


            serverHttp.createContext("/api/procedura/outputch", exchange -> {
                if ("OPTIONS".equals(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
                    exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                    exchange.sendResponseHeaders(204, -1);
                    exchange.close();
                    return;
                }
                if ("POST".equals(exchange.getRequestMethod())) {
                    new Thread(() -> {
                        TcpServer.mappaProiettori.values().forEach(proiettore -> {
                            try {
                                proiettore.proceduraOUTPUT_CH();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    }).start();

                    String response = "Procedura OUTPUT_CH avviata";
                    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                } else {
                    exchange.sendResponseHeaders(405, 0);
                    exchange.getResponseBody().close();
                }
            });




            new Thread(() -> {
                System.out.println("🔌 HTTP server in ascolto su porta " + porta + "...");
                serverHttp.start();
            }).start();
        } catch (IOException e) {
            System.err.println("❌ Errore avvio HTTP server: " + e.getMessage());
        }
    }
}
