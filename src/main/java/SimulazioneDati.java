import java.util.*;

public class SimulazioneDati {

    public static void inviaDatiSimulati(int tabId) {
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        Random rand = new Random();

        double xStart = 360.0;
        double xEnd = 830.0;
        int n = 471;
        double passo = (xEnd - xStart) / (n - 1);

        for (int i = 0; i < n; i++) {
            xList.add(xStart + i * passo);
        }

        double y = rand.nextDouble() * 250;
        yList.add(y);
        for (int i = 1; i < n; i++) {
            double variazione = (rand.nextDouble() * 50) - 25;
            y = Math.max(0, Math.min(250, y + variazione));
            yList.add(y);
        }

        // Simula valori basic
        Map<String, Double> basic = new LinkedHashMap<>();
        basic.put("CCT", 3000 + rand.nextDouble() * 4000);
        basic.put("Irr", 100 + rand.nextDouble() * 200);
        basic.put("X", rand.nextDouble() * 100);
        basic.put("Y", rand.nextDouble() * 100);
        basic.put("Z", rand.nextDouble() * 100);
        basic.put("x", rand.nextDouble());
        basic.put("y", rand.nextDouble());

        // Simula CRI (15 valori)
        List<Integer> cri = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            cri.add(rand.nextInt(100));
        }

        // Crea JSON finale
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put("x", xList);
        jsonMap.put("y", yList);
        jsonMap.put("basic", basic);
        jsonMap.put("cri", cri);

        String json = new com.google.gson.Gson().toJson(jsonMap);

        GraficoSocket.inviaDati(String.valueOf(tabId), json);
    }

}
