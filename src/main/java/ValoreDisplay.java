/*import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

public class ValoreDisplay extends JPanel {
    private final GraficoXYPanel graficoXY;
    private final JPanel centroPanel = new JPanel(new CardLayout());
    private final BarChartPanel graficoCRI;
    private final List<Integer> valoriCRI; // Questo può rimanere per il grafico CRI

    // Ora incapsuliamo tutti i dati specifici della misura in un unico oggetto
    public final DatiMisura datiCorrenti; // Rendi pubblici solo se devi accedere da Main dopo la creazione

    private final LinkedHashMap<String, Double> tuttiDati = new LinkedHashMap<>();

    private final List<String> chiaviFisse = Arrays.asList(
            "CCT", "Irr", "X", "Y", "Z", "x", "y", "C1 of X", "C2 of X",
            "u'", "v'", "CRI", "CRI Tolerance", "Fidelity", "Fidelity log",
            "TLCI", "DUV", "Integration time"
    );

    private final JPanel valoriPanel = new JPanel(new GridBagLayout());
    private final Color grigioScuro = new Color(0x3b3b3b);

    // Liste titoli per modalità
    private final List<String> titoliBasic = Arrays.asList(
            "CCT", "Irr", "X", "Y", "Z", "x", "y"
    );
    private final List<String> titoliCRI = Arrays.asList(
            "CRI", "CRI Tolerance", "Fidelity", "Fidelity log"
    );

    private List<String> titoliSelezionati = new ArrayList<>(titoliBasic);

    // Modifica il costruttore per accettare DatiMisura
    public ValoreDisplay(DatiMisura datiMisura) {
        this.datiCorrenti = datiMisura; // Salva i dati passati
        this.valoriCRI = datiMisura.valoriCRI; // Estrai valoriCRI per il grafico CRI
        this.graficoCRI = new BarChartPanel(this.valoriCRI);

        setLayout(new BorderLayout());
        setBackground(grigioScuro);

        // Inizializza tuttiDati con i valori di datiMisura.datiValori
        for (String chiave : chiaviFisse) {
            tuttiDati.put(chiave, 0.0);
        }
        if (datiMisura.datiValori != null) {
            for (Map.Entry<String, Double> entry : datiMisura.datiValori.entrySet()) {
                if (tuttiDati.containsKey(entry.getKey())) {
                    tuttiDati.put(entry.getKey(), entry.getValue());
                }
            }
        }

        // Grafico XY a sinistra
        // Usa i dati da datiMisura
        graficoXY = new GraficoXYPanel(datiMisura.xList, datiMisura.yList);
        add(graficoXY, BorderLayout.WEST);

        // Panel valori a destra con CardLayout (basic/cri)
        centroPanel.setBackground(grigioScuro);

        valoriPanel.setBackground(grigioScuro);
        aggiornaValori();

        centroPanel.add(valoriPanel, "basic");
        centroPanel.add(graficoCRI, "cri");

        JPanel destraPanel = new JPanel(new BorderLayout());
        destraPanel.setBackground(grigioScuro);

        // Pannello top con radio e bottone
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(grigioScuro);
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        radioPanel.setBackground(grigioScuro);
        JRadioButton basicBtn = new JRadioButton("Basic");
        JRadioButton criBtn = new JRadioButton("CRI");

        Stream.of(basicBtn, criBtn).forEach(rb -> {
            rb.setForeground(Color.WHITE);
            rb.setBackground(grigioScuro);
            rb.setFocusPainted(false);
            radioPanel.add(rb);
        });

        basicBtn.setSelected(true);
        ButtonGroup group = new ButtonGroup();
        group.add(basicBtn);
        group.add(criBtn);
        topPanel.add(radioPanel, BorderLayout.WEST);

        // Bottone impostazioni
        JButton settingsButton;
        URL imageUrl = getClass().getResource("ingranaggi.png");
        if (imageUrl != null) {
            ImageIcon originalIcon = new ImageIcon(imageUrl);
            Image image = originalIcon.getImage();
            Image scaled = image.getScaledInstance(23, 23, Image.SCALE_SMOOTH);
            settingsButton = new JButton(new ImageIcon(scaled));
        } else {
            settingsButton = new JButton("⚙️");
            settingsButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        }

        settingsButton.setPreferredSize(new Dimension(30, 30));
        settingsButton.setBackground(Color.WHITE);
        settingsButton.setFocusPainted(false);
        settingsButton.setBorder(BorderFactory.createEmptyBorder());
        settingsButton.setContentAreaFilled(false);
        settingsButton.setOpaque(true);
        settingsButton.addActionListener(e ->
                new ConfiguratoreDialog(SwingUtilities.getWindowAncestor(this), tuttiDati.keySet(), titoliSelezionati, this));
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setBackground(grigioScuro);
        rightPanel.add(settingsButton);
        topPanel.add(rightPanel, BorderLayout.EAST);

        // Cambia pannello con radio
        basicBtn.addActionListener(e -> ((CardLayout) centroPanel.getLayout()).show(centroPanel, "basic"));
        criBtn.addActionListener(e -> ((CardLayout) centroPanel.getLayout()).show(centroPanel, "cri"));

        destraPanel.add(topPanel, BorderLayout.NORTH);
        destraPanel.add(centroPanel, BorderLayout.CENTER);

        add(destraPanel, BorderLayout.CENTER);
    }

    public void aggiornaValori() {
        valoriPanel.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < titoliSelezionati.size(); i++) {
            String titolo = titoliSelezionati.get(i);
            Double valore = tuttiDati.get(titolo);
            if (valore == null) continue;

            String testo = String.format("%.3f", valore);

            JLabel titoloLabel = new JLabel(titolo + ":");
            titoloLabel.setForeground(Color.WHITE);
            titoloLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));

            JLabel valoreLabel = new JLabel(testo, SwingConstants.CENTER);
            valoreLabel.setOpaque(true);
            valoreLabel.setBackground(grigioScuro);
            valoreLabel.setForeground(Color.WHITE);
            valoreLabel.setFont(new Font("Segoe UI", Font.ITALIC, 36));

            FontMetrics fm = valoreLabel.getFontMetrics(valoreLabel.getFont());
            int labelWidth = fm.stringWidth(testo) + 20;
            valoreLabel.setPreferredSize(new Dimension(labelWidth, 35));

            gbc.gridx = 0;
            gbc.gridy = i;
            valoriPanel.add(titoloLabel, gbc);
            gbc.gridx = 1;
            valoriPanel.add(valoreLabel, gbc);
        }

        valoriPanel.revalidate();
        valoriPanel.repaint();
    }

    public void aggiornaValoriDinamici(Map<String, Double> nuoviValori, List<Integer> nuoviCRI,
                                       List<Double> xList, List<Double> yList) {

        // Aggiorno grafico
        System.out.println("xList size: " + xList.size());
        System.out.println("yList size: " + yList.size());

        if (xList != null && yList != null && xList.size() == yList.size()) {
            graficoXY.aggiornaDati(xList, yList);
        }

        // Aggiorna campioni TCS, grafico a barre
        if (nuoviCRI != null && nuoviCRI.size() == 15) {
            for (int i = 0; i < 15; i++) {
                valoriCRI.set(i, nuoviCRI.get(i));
            }
            graficoCRI.aggiornaValori(nuoviCRI); // AGGIORNA il grafico visivamente
        }

        for (String chiave : chiaviFisse) {
            if (nuoviValori.containsKey(chiave)) {
                tuttiDati.put(chiave, nuoviValori.get(chiave));
            } else {
                tuttiDati.put(chiave, 0.0);
            }
        }

        aggiornaValori();
    }
}*/