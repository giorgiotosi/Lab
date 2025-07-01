/*import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.List;
import java.util.Set;

public class ConfiguratoreDialog extends JDialog {
    private ValoreDisplay valoreDisplay;
    private JLabel erroreLabel;
    private JList<String> listaDisponibili;
    private DefaultListModel<String> modelDisponibili;
    private JList<String> listaVisualizzati;
    private DefaultListModel<String> modelVisualizzati;

    public ConfiguratoreDialog(Window parent, Set<String> tuttiTitoli, List<String> titoliAttivi, ValoreDisplay valoreDisplay) {
        super(parent, "Configura visualizzazione", ModalityType.APPLICATION_MODAL);
        this.valoreDisplay = valoreDisplay;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        add(mainPanel);

        modelDisponibili = new DefaultListModel<>();
        modelVisualizzati = new DefaultListModel<>();

        for (String titolo : tuttiTitoli) {
            if (titoliAttivi.contains(titolo))
                modelVisualizzati.addElement(titolo);
            else
                modelDisponibili.addElement(titolo);
        }

        listaDisponibili = new JList<>(modelDisponibili);
        listaVisualizzati = new JList<>(modelVisualizzati);

        JScrollPane scrollDisponibili = new JScrollPane(listaDisponibili);
        JScrollPane scrollVisualizzati = new JScrollPane(listaVisualizzati);
        Dimension listPreferredSize = new Dimension(200, 250);
        scrollDisponibili.setPreferredSize(listPreferredSize);
        scrollVisualizzati.setPreferredSize(listPreferredSize);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.BOTH;

        JLabel titleLabel = new JLabel("Configura visualizzazione");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(titleLabel, gbc);

        JLabel dispLabel = new JLabel("Valori disponibili:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(dispLabel, gbc);

        JLabel visLabel = new JLabel("Valori mostrati:");
        gbc.gridx = 2;
        gbc.gridy = 1;
        mainPanel.add(visLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        mainPanel.add(scrollDisponibili, gbc);

        JPanel transferButtonPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        JButton destraBtn = createIconButton("arrow-right.png", "➡️");
        JButton sinistraBtn = createIconButton("left-arrow.png", "⬅️");
        transferButtonPanel.add(destraBtn);
        transferButtonPanel.add(sinistraBtn);

        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(transferButtonPanel, gbc);

        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(scrollVisualizzati, gbc);

        JPanel reorderButtonPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        JButton upBtn = createIconButton("arrow-up.png", "⬆️");
        JButton downBtn = createIconButton("arrow-down.png", "⬇️");
        reorderButtonPanel.add(upBtn);
        reorderButtonPanel.add(downBtn);

        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(reorderButtonPanel, gbc);

        destraBtn.addActionListener(e -> {
            for (String sel : listaDisponibili.getSelectedValuesList()) {
                modelDisponibili.removeElement(sel);
                modelVisualizzati.addElement(sel);
            }
        });

        sinistraBtn.addActionListener(e -> {
            for (String sel : listaVisualizzati.getSelectedValuesList()) {
                modelVisualizzati.removeElement(sel);
                modelDisponibili.addElement(sel);
            }
        });

        upBtn.addActionListener(e -> {
            int selectedIndex = listaVisualizzati.getSelectedIndex();
            if (selectedIndex > 0) {
                String selectedValue = modelVisualizzati.remove(selectedIndex);
                modelVisualizzati.add(selectedIndex - 1, selectedValue);
                listaVisualizzati.setSelectedIndex(selectedIndex - 1);
            }
        });

        downBtn.addActionListener(e -> {
            int selectedIndex = listaVisualizzati.getSelectedIndex();
            if (selectedIndex != -1 && selectedIndex < modelVisualizzati.getSize() - 1) {
                String selectedValue = modelVisualizzati.remove(selectedIndex);
                modelVisualizzati.add(selectedIndex + 1, selectedValue);
                listaVisualizzati.setSelectedIndex(selectedIndex + 1);
            }
        });

        erroreLabel = new JLabel("Max 7 valori");
        erroreLabel.setForeground(Color.RED);
        erroreLabel.setVisible(false); // Nascosta di default

        GridBagConstraints gbcErrore = new GridBagConstraints();
        gbcErrore.gridx = 3; // a destra del pulsante Salva
        gbcErrore.gridy = 3;
        gbcErrore.anchor = GridBagConstraints.WEST;
        gbcErrore.insets = new Insets(0, 10, 0, 0);
        mainPanel.add(erroreLabel, gbcErrore);


        JButton salvaBtn = new JButton("Salva");
        salvaBtn.setFocusPainted(false);
        salvaBtn.addActionListener(e -> {
            if (modelVisualizzati.getSize() > 7) {
                erroreLabel.setVisible(true); // Mostra messaggio errore
            } else {
                erroreLabel.setVisible(false);
                titoliAttivi.clear();
                for (int i = 0; i < modelVisualizzati.size(); i++) {
                    titoliAttivi.add(modelVisualizzati.get(i));
                }
                valoreDisplay.aggiornaValori();// Aggiorna UI padre
                dispose(); // Chiudi dialogo
            }
        });


        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(salvaBtn, gbc);

        pack();
        setVisible(true);
    }

    private JButton createIconButton(String iconFileName, String fallbackText) {
        JButton button;
        URL imageUrl = getClass().getResource(iconFileName);
        if (imageUrl != null) {
            ImageIcon originalIcon = new ImageIcon(imageUrl);
            Image image = originalIcon.getImage();
            Image scaledImage = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            button = new JButton(new ImageIcon(scaledImage));
        } else {
            System.err.println("Icona non trovata: " + iconFileName);
            button = new JButton(fallbackText);
            button.setFont(new Font("Arial", Font.BOLD, 14));
        }

        int buttonDim = 35;
        button.setPreferredSize(new Dimension(buttonDim, buttonDim));
        button.setMinimumSize(new Dimension(buttonDim, buttonDim));
        button.setMaximumSize(new Dimension(buttonDim, buttonDim));
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        return button;
    }
}*/
