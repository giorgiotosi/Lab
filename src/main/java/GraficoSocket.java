import com.fasterxml.jackson.databind.ObjectMapper;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ServerEndpoint("/data/{tabId}")
public class GraficoSocket {

    private static final ObjectMapper mapper = new ObjectMapper();

    // Sessioni websocket attive per ogni tabId
    private static final ConcurrentMap<String, Set<Session>> sessions = new ConcurrentHashMap<>();


    private String tabId;

    @OnOpen
    public void onOpen(Session session, @PathParam("tabId") String tabId) {
        this.tabId = tabId;
        sessions.compute(tabId, (key, existingSet) -> {
            if (existingSet == null) existingSet = ConcurrentHashMap.newKeySet();
            existingSet.add(session);
            return existingSet;
        });
        System.out.println("🟢 WebSocket aperta per tabId " + tabId + " (session " + session.getId() + ")");
    }

    @OnClose
    public void onClose(Session session, @PathParam("tabId") String tabId) {
        sessions.computeIfPresent(tabId, (key, existingSet) -> {
            existingSet.remove(session);
            return existingSet.isEmpty() ? null : existingSet;
        });
        System.out.println("🔴 WebSocket chiusa per tabId " + tabId + " (session " + session.getId() + ")");
    }


    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Errore su session " + (session != null ? session.getId() : "null") + ": " + throwable.getMessage());
        throwable.printStackTrace();
    }


    // ✅ Metodo per inviare tutti i dati frontend
    public static void inviaDati(String tabId, String json) {
        Set<Session> sessionSet = sessions.get(tabId);
        if (sessionSet != null) {
            sessionSet.forEach(session -> {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(json);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            System.out.println("⚠️ Nessuna sessione attiva per tabId " + tabId);
        }
    }



    // ✅ Classe dati aggregati da mandare al frontend
    public static class DatiCompositi {
        public List<Double> x;
        public List<Double> y;
        public Map<String, Double> datiBasic;
        public List<Integer> datiCRI;

        public DatiCompositi(List<Double> x, List<Double> y,
                             Map<String, Double> datiBasic, List<Integer> datiCRI) {
            this.x = x;
            this.y = y;
            this.datiBasic = datiBasic;
            this.datiCRI = datiCRI;
        }
    }
}
