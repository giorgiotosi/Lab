/*import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;


class BarChartPanel extends JPanel {
    private final Map<String, Integer> valoriCRI = new LinkedHashMap<>();

    private final Map<String, Color> coloriCRI = new HashMap<String, Color>() {{
        put("R1", new Color(255, 0, 0));
        put("R2", new Color(0, 255, 0));
        put("R3", new Color(0, 0, 255));
        put("R4", new Color(255, 255, 0));
        put("R5", new Color(255, 165, 0));
        put("R6", new Color(128, 0, 128));
        put("R7", new Color(0, 255, 255));
        put("R8", new Color(128, 128, 128));
        put("R9", new Color(255, 192, 203));  // rosa
        put("R10", new Color(0, 128, 0));     // verde scuro
        put("R11", new Color(0, 0, 128));     // blu scuro
        put("R12", new Color(128, 128, 0));   // oliva
        put("R13", new Color(255, 140, 0));   // arancio scuro
        put("R14", new Color(75, 0, 130));    // indaco
    }};
    private static final String[] keys = {
            "R1", "R2", "R3", "R4", "R5", "R6", "R7", "R8",
            "R9", "R10", "R11", "R12", "R13", "R14"
    };

    // Nuove variabili per i margini personalizzabili
    private int margineSinistro = 65;  // Spazio a sinistra per asse Y e titolo
    private int margineDestro = 40;   // Spazio a destra
    private int margineSuperiore = 40; // Spazio in alto
    private int margineInferiore = 50; // Spazio in basso per asse X e titolo

    public BarChartPanel(List<Integer> valori) {
        if (valori == null || valori.size() != 14)
            throw new IllegalArgumentException("La lista deve contenere esattamente 14 valori");

        setBackground(new Color(0x3b3b3b));
        setPreferredSize(new Dimension(375, 400));

        for (int i = 0; i < keys.length; i++) {
            valoriCRI.put(keys[i], valori.get(i));
        }
    }

    public void aggiornaValori(List<Integer> nuoviValori ){

        if (nuoviValori == null || nuoviValori.size() != 14)
            throw new IllegalArgumentException("La lista deve contenere esattamente 14 valori");

        for (int i = 0; i < keys.length; i++) {
            valoriCRI.put(keys[i], nuoviValori.get(i));
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int larghezza = getWidth();
        int altezza = getHeight();

        // Calcolo della linea di base dell'asse X (in base al margine inferiore)
        int baseLineY = altezza - margineInferiore;
        // Altezza disponibile per il grafico (tra margine superiore e linea di base)
        int altezzaGrafico = baseLineY - margineSuperiore;

        // --- Disegno degli Assi ---
        g2.setColor(Color.WHITE);
        // Asse X
        g2.drawLine(margineSinistro, baseLineY, larghezza - margineDestro, baseLineY);
        // Asse Y
        g2.drawLine(margineSinistro, margineSuperiore, margineSinistro, baseLineY);

        // --- Tacche e Numeri Asse Y (0 - 100 ogni 10) ---
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // Font per le etichette dei numeri
        FontMetrics fmYLabels = g2.getFontMetrics(); // FontMetrics per i numeri dell'asse Y

        for (int i = 0; i <= 10; i++) {
            // Calcola la posizione Y della tacca/numero
            int y = baseLineY - (i * altezzaGrafico / 10);

            // Disegna la tacca
            g2.drawLine(margineSinistro - 5, y, margineSinistro, y);

            // Disegna il numero
            String label = String.valueOf(i * 10);
            int labelWidth = fmYLabels.stringWidth(label);
            // Posiziona il numero a sinistra dell'asse Y
            g2.drawString(label, margineSinistro - labelWidth - 10, y + fmYLabels.getAscent() / 2 - 2);
        }

        // --- Disegno delle Barre ---
        int numBarre = valoriCRI.size();
        // Larghezza disponibile per le barre e i loro spazi
        int spazioDisponibilePerBarre = larghezza - margineSinistro - margineDestro;

        // Calcolo larghezza barra e gap (con un rapporto 70% barra, 30% gap per unità)
        int unitWidth = (int) (spazioDisponibilePerBarre / (numBarre + (numBarre > 0 ? (numBarre - 1) * 0.5 : 0))); // Circa 1.5 unità per barra + gap
        int larghezzaBarra = (int) (unitWidth * 0.9);
        int gap = (int) (unitWidth * 0.5);

        // Assicurati che le barre non siano troppo strette
        if (larghezzaBarra < 5 && numBarre > 0) {
            larghezzaBarra = 5;
            gap = (spazioDisponibilePerBarre - (numBarre * larghezzaBarra)) / (numBarre + 1);
            if (gap < 0) gap = 0; // Evita gap negativi
        }

        int xBarraCorrente = margineSinistro + gap; // Inizia a disegnare le barre dopo un gap dal margine sinistro

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // Font per le etichette delle barre e i valori
        FontMetrics fmBarLabels = g2.getFontMetrics(); // FontMetrics per etichette barre

        for (Map.Entry<String, Integer> entry : valoriCRI.entrySet()) {
            String label = entry.getKey();
            int valore = entry.getValue();
            // Calcola l'altezza della barra in proporzione all'altezza disponibile del grafico
            int altezzaBarra = (valore * altezzaGrafico) / 100;

            g2.setColor(coloriCRI.get(label));
            g2.fillRect(xBarraCorrente, baseLineY - altezzaBarra, larghezzaBarra, altezzaBarra);

            // --- Etichetta del Valore sopra la barra ---
            g2.setColor(Color.WHITE);
            String valoreStr = String.valueOf(valore);
            int valoreWidth = fmBarLabels.stringWidth(valoreStr);
            // Posiziona il valore centrato sopra la barra
            g2.drawString(valoreStr, xBarraCorrente + (larghezzaBarra - valoreWidth) / 2, baseLineY - altezzaBarra - 5);


            // --- Etichetta della Barra sotto l'asse X ---
            g2.setColor(Color.WHITE);
            int labelWidth = fmBarLabels.stringWidth(label);
            // Posiziona l'etichetta centrata sotto la barra
            g2.drawString(label, xBarraCorrente + (larghezzaBarra - labelWidth) / 2, baseLineY + 15);

            xBarraCorrente += larghezzaBarra + gap; // Sposta alla posizione della prossima barra
        }

        // --- Titolo Asse X ---
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        FontMetrics fmXTitle = g2.getFontMetrics();
        String xAxisTitle = "Ri"; // Puoi cambiarlo in "Hue - Angle Bin (j)" se preferisci
        int xAxisTitleWidth = fmXTitle.stringWidth(xAxisTitle);
        // Posiziona il titolo X centrato nell'area X del grafico
        g2.drawString(xAxisTitle, margineSinistro + (spazioDisponibilePerBarre - xAxisTitleWidth) / 2, altezza - (margineInferiore / 2) + 15);

        // --- Titolo Asse Y (ruotato) ---
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        String yAxisTitle = "Local color Fidelity (Rf,hj)"; // Ho aggiunto il testo completo come nella tua immagine
        FontMetrics fmYTitle = g2.getFontMetrics();
        int yAxisTitleWidth = fmYTitle.stringWidth(yAxisTitle);

        // Trasla l'origine per la rotazione: al centro del margine sinistro
        g2.translate(margineSinistro / 2, altezza / 2);
        g2.rotate(-Math.PI / 2); // Ruota di -90 gradi (in senso antiorario)

        // Disegna il titolo Y, posizionandolo rispetto alla nuova origine ruotata
        // Il "-25" (o un valore simile) sposta il testo più a "sinistra" (verso l'esterno del pannello)
        // per creare spazio dai numeri dell'asse Y. Regola questo valore per la distanza desiderata.
        g2.drawString(yAxisTitle, -yAxisTitleWidth / 2, -fmYTitle.getDescent() - 7);

        g2.rotate(Math.PI / 2); // Riporta la rotazione allo stato originale
        g2.translate(-margineSinistro / 2, -altezza / 2); // Riporta l'origine allo stato originale
    }
}*/