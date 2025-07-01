import java.util.Arrays;

public class PacchettoDaClient {
    public int indice;

    private double[] spectrum;
    private double lux;
    private double TristimulusX;
    private double TristimulusX1;
    private double TristimulusX2;
    private double TristimulusY;
    private double TristimulusZ;
    private double x;
    private double y;
    private double u_primo;
    private double v_primo;
    private double CCT;
    private double[] TCS;
    private double CRI;
    private double CRI_tolerance;
    private double fidelityIndex;
    private double fidelityIndexLog;
    private double[] colorVectorGrafic;
    private double gamutAreaIndex;
    private double TLCI;
    private double duv;
    private double integrationTime;

    public PacchettoDaClient(String input) {
        indice = Integer.parseInt(input.split("#")[0]);

        int pos = 0;
        while (pos < input.length()) {
            // Trova il prossimo #
            if (input.charAt(pos) != '#') {
                pos++;
                continue;
            }

            int startIndex = pos;
            int commaIndex = input.indexOf(',', startIndex);
            if (commaIndex == -1) break; // formato errato

            String key = input.substring(startIndex, commaIndex);

            int nextHash = input.indexOf('#', commaIndex + 1);

            String values;
            if (nextHash == -1) {
                values = input.substring(commaIndex + 1).trim();
                pos = input.length();
            } else {
                values = input.substring(commaIndex + 1, nextHash).trim();
                pos = nextHash;
            }

            // Switch dentro il ciclo
            switch (key) {
                case "#1":
                    spectrum = parseDoubleArray(values);
                    break;
                case "#2":
                    lux = Double.parseDouble(values);
                    break;
                case "#3":
                    TristimulusX = Double.parseDouble(values);
                    break;
                case "#4":
                    TristimulusX1 = Double.parseDouble(values);
                    break;
                case "#5":
                    TristimulusX2 = Double.parseDouble(values);
                    break;
                case "#6":
                    TristimulusY = Double.parseDouble(values);
                    break;
                case "#7":
                    TristimulusZ = Double.parseDouble(values);
                    break;
                case "#8":
                    x = Double.parseDouble(values);
                    break;
                case "#9":
                    y = Double.parseDouble(values);
                    break;
                case "#10":
                    u_primo = Double.parseDouble(values);
                    break;
                case "#11":
                    v_primo = Double.parseDouble(values);
                    break;
                case "#12":
                    CCT = Double.parseDouble(values);
                    break;
                case "#13":
                    TCS = parseDoubleArray(values);
                    break;
                case "#14":
                    CRI = Double.parseDouble(values);
                    break;
                case "#15":
                    CRI_tolerance = Double.parseDouble(values);
                    break;
                case "#16":
                    fidelityIndex = Double.parseDouble(values);
                    break;
                case "#17":
                    fidelityIndexLog = Double.parseDouble(values);
                    break;
                case "#18":
                    colorVectorGrafic = parseDoubleArray(values);
                    break;
                case "#19":
                    gamutAreaIndex = Double.parseDouble(values);
                    break;
                case "#20":
                    TLCI = Double.parseDouble(values);
                    break;
                case "#21":
                    duv = Double.parseDouble(values);
                    break;
                case "#22":
                    integrationTime = Double.parseDouble(values);
                    break;
                default:
                    // Ignora chiavi non riconosciute
                    break;
            }
        }
    }

    public double[] getSpectrum() {
        return spectrum;
    }

    public double getLux() {
        return lux;
    }

    public double getTristimulusX() {
        return TristimulusX;
    }

    public double getTristimulusX1() {
        return TristimulusX1;
    }

    public double getTristimulusX2() {
        return TristimulusX2;
    }

    public double getTristimulusY() {
        return TristimulusY;
    }

    public double getTristimulusZ() {
        return TristimulusZ;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getU_primo() {
        return u_primo;
    }

    public double getV_primo() {
        return v_primo;
    }

    public double getCCT() {
        return CCT;
    }

    public double[] getTCS() {
        return TCS;
    }

    public double getCRI() {
        return CRI;
    }

    public double getCRI_tolerance() {
        return CRI_tolerance;
    }

    public double getFidelityIndex() {
        return fidelityIndex;
    }

    public double getFidelityIndexLog() {
        return fidelityIndexLog;
    }

    public double[] getColorVectorGrafic() {
        return colorVectorGrafic;
    }

    public double getGamutAreaIndex() {
        return gamutAreaIndex;
    }

    public double getTLCI() {
        return TLCI;
    }

    public double getDuv() {
        return duv;
    }

    public double getIntegrationTime() {
        return integrationTime;
    }

    @Override
    public String toString() {
        return "PacchettoDaClient{\n" +
                "indice = " + indice + "\n" +
                "#1 spectrum=" + Arrays.toString(spectrum) + "\n" +
                "#2 lux=" + lux + "\n" +
                "#3 TristimulusX=" + TristimulusX + "\n" +
                "#4 TristimulusX1=" + TristimulusX1 + "\n" +
                "#5 TristimulusX2=" + TristimulusX2 + "\n" +
                "#6 TristimulusY=" + TristimulusY + "\n" +
                "#7 TristimulusZ=" + TristimulusZ + "\n" +
                "#8 x=" + x + "\n" +
                "#9 y=" + y + "\n" +
                "#10 u_primo=" + u_primo + "\n" +
                "#11 v_primo=" + v_primo + "\n" +
                "#12 CCT=" + CCT + "\n" +
                "#13 TCS=" + Arrays.toString(TCS) + "\n" +
                "#14 CRI=" + CRI + "\n" +
                "#15 CRI_tolerance=" + CRI_tolerance + "\n" +
                "#16 fidelityIndex=" + fidelityIndex + "\n" +
                "#17 fidelityIndexLog=" + fidelityIndexLog + "\n" +
                "#18 colorVectorGrafic=" + Arrays.toString(colorVectorGrafic) + "\n" +
                "#19 gamutAreaIndex=" + gamutAreaIndex + "\n" +
                "#20 TLCI=" + TLCI + "\n" +
                "#21 duv=" + duv + "\n" +
                "#22 integrationTime=" + integrationTime + "\n" +
                '}';
    }

    private double[] parseDoubleArray(String s) {
        String[] parts = s.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Double.parseDouble(parts[i].trim());
        }
        return result;
    }

}
