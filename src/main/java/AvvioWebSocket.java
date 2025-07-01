import org.glassfish.tyrus.server.Server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AvvioWebSocket {
    public static void main(String[] args) {
        Server server = new Server("localhost", 8025, "/", null, GraficoSocket.class);
        try {
            server.start();
            System.out.println("WebSocket server in ascolto...");

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            final int maxInvii = 50; // ad esempio 10 invii per tab
            final int[] counter = {0};

            scheduler.scheduleAtFixedRate(() -> {
                if (counter[0] < maxInvii) {
                    for (int tabId = 1; tabId <= 10; tabId++) {
                        SimulazioneDati.inviaDatiSimulati(tabId
                        );
                    }
                    counter[0]++;
                    System.out.println("Invio dati ciclo " + counter[0]);
                } else {
                    scheduler.shutdown();
                    System.out.println("Terminato invio dati per tutti i tab.");
                }
            }, 10, 2, TimeUnit.SECONDS); // ogni 2 secondi manda dati

            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
