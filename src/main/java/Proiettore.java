import com.fazecast.jSerialComm.SerialPort;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

public class Proiettore implements Runnable {

    private int numColori;

    //TAR
//    private int tarMin; // tar[0]
//    private int tarBas; // tar[1]
//    private int tarMed; // tar[2]
//    private int tarAlt; // tar[3]
//    private int tarMax; // tar[4]
    private List<int[]> TAR = new ArrayList<>();
    private static final int[] divTar = new int[]{32767, 16384, 2048, 512, 256};
    private static final int[] tar = new int[]{1,2,16,64,128};

    /********************************************************/
    /* Table A-1: RDM Command Classes (Slot 20)             */
    /********************************************************/
    public class E120Constants {
        public static final int E120_DISCOVERY_COMMAND = 0x10;
        public static final int E120_DISCOVERY_COMMAND_RESPONSE = 0x11;
        public static final int E120_GET_COMMAND = 0x20;
        public static final int E120_GET_COMMAND_RESPONSE = 0x21;
        public static final int E120_SET_COMMAND = 0x30;
        public static final int E120_SET_COMMAND_RESPONSE = 0x31;
    }



    public static final byte E120_DISCOVERY_COMMAND_RESPONSE = 0x11;
    public static int tn = 0x00;
    public volatile byte[] uidDst;
    public static final byte E120_DISCOVERY_COMMAND = 0x10;
    //public static final int E120_PID_DISC_UNIQUE_BRANCH = 0x8001; // Deve essere int per 0x8001
    public static final byte E120_DISC_UNIQUE_BRANCH = 0x001;
    public static final int COEMAR_TAR_MIN = 0x8002;
    public static final int COEMAR_TAR_BAS = 0x8003;
    public static final int COEMAR_TAR_MED = 0x8004;
    public static final int COEMAR_TAR_ALT = 0x8005;
    public static final int COEMAR_TAR_MAX = 0x8006;
    public static final int COEMAR_TAR_CCT_DUV = 0x8015;
    public static final int COEMAR_CCT = 0x801A;
    public static final int COEMAR_OUTPUTCH = 0x801B;
    public static final int COEMAR_TAR_TAB_CCT = 0x8020;
    private int portaTcp; // porta TCP associata (es: 5000, 5001, ecc.)
    //UID = 0x434d494d4d49;
    public static final byte[] SRC_UID = new byte[]{
            (byte) 0x43,
            (byte) 0x4D,
            (byte) 0x49,
            (byte) 0x4D,
            (byte) 0x4D,
            (byte) 0x49
    };

    public BlockingQueue<PacchettoDaClient> getQueue() {
        return queue;
    }


    private static boolean first_dati=true;
    private final LinkedBlockingDeque<PacchettoDaClient> queue = new LinkedBlockingDeque<>();
    private final ByteArrayOutputStream bufferIn = new ByteArrayOutputStream();
    private final SerialPort porta;
    static final int RDMbaudrate = 250000;
    static final int RDMbaudrateLOW = 115200;
    public byte[] buffer_trasmissione;
    private final byte[] uidSrc = SRC_UID;
    private byte[] rdmBufferIn = new byte[513];  // buffer di ricezione
    private int rdmOffsetRead = 0;
    private final int RDMMINSIZEPKT = 24;
    private boolean fResetDataRead = false;
    boolean fNewDataRead = false;
    public String port;
    int idClientTCP;

    public int getIdClientTCP() {
        return idClientTCP;
    }

    public void aggiungiPacchetto(PacchettoDaClient pacchettoDaClient){
        queue.add(pacchettoDaClient);
    }

    public PacchettoDaClient restituisciUltimoPacchetto() throws InterruptedException {
        PacchettoDaClient result;

        queue.clear(); // scarto quelli vecchi
        queue.takeLast(); // bloccante, prende quello nuovo e lo scarta

        result = queue.takeLast(); // prende quello nuovo ancora

        while (!queue.isEmpty()) { // se ne arrivano altri mentre stavo processando quello nuovo prendo
            // l'ultimo di questi
            result = queue.takeLast();
        }

        return result;
    }

    public double restituisciMediaUltimiTrePacchetti() throws InterruptedException {
        PacchettoDaClient result;
        queue.clear(); // scarto quelli vecchi
        queue.takeLast(); // bloccante, prende quello nuovo e lo scarta

        List<PacchettoDaClient> buffer = new ArrayList<>(3);

        while (true) {
            PacchettoDaClient nuovo = queue.takeLast(); // blocca finché non arriva
            if (buffer.size() == 3) buffer.remove(0);   // rimuovi il più vecchio
            buffer.add(nuovo);

            if (buffer.size() == 3 && differenzaRelativaMax(buffer) < 0.02) {
                return mediaValoriPacchetti(buffer);
            }
        }
    }

    private double differenzaRelativaMax(List<PacchettoDaClient> buffer) {

        double lux1 = buffer.get(0).getLux();
        double lux2 = buffer.get(1).getLux();
        double lux3 = buffer.get(2).getLux();

        double d1 = differenzaRelativa(lux1, lux2);
        double d2 = differenzaRelativa(lux2, lux3);
        double d3 = differenzaRelativa(lux1, lux3);

        return Math.max(d1, Math.max(d2, d3));
    }

    private double differenzaRelativa(double a, double b) {
        if (a == 0.0 && b == 0.0) return 0.0; // caso limite, entrambi zero
        return Math.abs(a - b) / Math.max(Math.abs(a), Math.abs(b));
    }

    private double mediaValoriPacchetti(List<PacchettoDaClient> ultimi3) {
        double result;

        double lux1 = ultimi3.get(0).getLux();
        double lux2 = ultimi3.get(1).getLux();
        double lux3 = ultimi3.get(2).getLux();

        result = (lux1+lux2+lux3)/3;

        return result;
    }

    public void proceduraTaratura() throws InterruptedException {

        // chiamo la GET di OUTPUT_CH per trovare numColori
        sendBytes(this.Command(new byte[]{}, Proiettore.COEMAR_OUTPUTCH,(byte)E120Constants.E120_GET_COMMAND ));

        double[] Lux = new double[numColori];
        for(int Cx=0; Cx<numColori; Cx++){
            TAR.add(new int[5]);

            //pd azzerato con dimmer a full:
            byte[] pd = new byte[]{
                    (byte) 0xFF, (byte) 0xFF,
                    (byte) 0X00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00
            };

            pd[2*Cx+3]=(byte) 0xFF; // setto a 128 il colore Cx

            this.sendBytes(this.Command(pd, Proiettore.COEMAR_OUTPUTCH, (byte) 0x30));
            Lux[Cx] = restituisciMediaUltimiTrePacchetti();
            for(int i = 0; i<TAR.get(Cx).length; i++){
                // su quel valore per il colore corrente chiamo SET_OUTPUTCH su tar[i]

                //pd[2*Cx+3]=(byte) tar[i]; // setto a tar[i] il colore Cx // qua non so se posso settare così
                pd[2 * Cx + 3 - 1] = (byte) ((tar[i] >> 8) & 0xFF); // MSB
                pd[2 * Cx + 3    ] = (byte) ( tar[i]       & 0xFF); // LSB

                this.sendBytes(this.Command(pd, Proiettore.COEMAR_OUTPUTCH, (byte) 0x30));

                double Lux1 = restituisciMediaUltimiTrePacchetti();
                double target = Lux[i] / divTar[i];
                List<Double> historyTar = new ArrayList<>();

                if(Lux1<target){
                    for (int j = 0; j< 2; j++){
                        TAR.get(Cx)[i] += 100;
                        byte[] payloadTAR = new byte[5];
                        payloadTAR[0] = (byte) 0x80;
                        payloadTAR[2] = (byte) Cx;
                        int value = tar[i];
                        byte valueMSB = (byte) ((value >> 8) & 0xFF);
                        byte valueLSB = (byte) (value & 0xFF);
                        payloadTAR[3] = valueMSB;
                        payloadTAR[4] = valueLSB;

                        switch (i) {
                            case 0:
                                payloadTAR[1] = (byte) 0x02;
                                this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_MIN, (byte)E120Constants.E120_SET_COMMAND));
                                break;
                            case 1:
                                payloadTAR[1] = (byte) 0x03;
                                this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_BAS, (byte)E120Constants.E120_SET_COMMAND));
                                break;
                            case 2:
                                payloadTAR[1] = (byte) 0x04;
                                this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_MED, (byte)E120Constants.E120_SET_COMMAND));
                                break;
                            case 3:
                                payloadTAR[1] = (byte) 0x05;
                                this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_ALT, (byte)E120Constants.E120_SET_COMMAND));
                                break;
                            case 4:
                                payloadTAR[1] = (byte) 0x06;
                                this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_MAX, (byte)E120Constants.E120_SET_COMMAND));
                                break;
                            default:
                                System.out.println("Valore non gestito");
                        }

                        double Lux2 = restituisciMediaUltimiTrePacchetti();
                        double x = (100*target) / Math.abs(Lux2-Lux1); //--> Math.abs(Lux2-Lux1) : 100 = target : x
                        TAR.get(Cx)[i] = Math.toIntExact(Math.round(x));
                        value = TAR.get(Cx)[i];
                        valueMSB = (byte) ((value >> 8) & 0xFF);
                        valueLSB = (byte) (value & 0xFF);
                        payloadTAR[3] = valueMSB;
                        payloadTAR[4] = valueLSB;
                        switch (i) {
                            case 0:
                                this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_MIN, (byte)E120Constants.E120_SET_COMMAND));
                                break;
                            case 1:
                                this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_BAS, (byte)E120Constants.E120_SET_COMMAND));
                                break;
                            case 2:
                                this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_MED, (byte)E120Constants.E120_SET_COMMAND));
                                break;
                            case 3:
                                this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_ALT, (byte)E120Constants.E120_SET_COMMAND));
                                break;
                            case 4:
                                this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_MAX, (byte)E120Constants.E120_SET_COMMAND));
                                break;
                            default:
                                System.out.println("Valore non gestito");
                        }
                        Lux1 = restituisciMediaUltimiTrePacchetti();

                    }
                }

                while(Lux1<target){
                    TAR.get(Cx)[i] += 1;


                    byte[] payloadTAR = new byte[5];
                    payloadTAR[0] = (byte) 0x80;
                    payloadTAR[2] = (byte) Cx;
                    int value = TAR.get(Cx)[i];
                    byte valueMSB = (byte) ((value >> 8) & 0xFF);
                    byte valueLSB = (byte) (value & 0xFF);
                    payloadTAR[3] = valueMSB;
                    payloadTAR[4] = valueLSB;
                    switch (i) {
                        case 0:
                            payloadTAR[1] = (byte) 0x02;
                            this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_MIN, (byte)E120Constants.E120_SET_COMMAND));
                            break;
                        case 1:
                            payloadTAR[1] = (byte) 0x03;
                            this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_BAS, (byte)E120Constants.E120_SET_COMMAND));
                            break;
                        case 2:
                            payloadTAR[1] = (byte) 0x04;
                            this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_MED, (byte)E120Constants.E120_SET_COMMAND));
                            break;
                        case 3:
                            payloadTAR[1] = (byte) 0x05;
                            this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_ALT, (byte)E120Constants.E120_SET_COMMAND));
                            break;
                        case 4:
                            payloadTAR[1] = (byte) 0x06;
                            this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_MAX, (byte)E120Constants.E120_SET_COMMAND));
                            break;
                        default:
                            System.out.println("Valore non gestito");
                    }

                    double Lux2 = restituisciMediaUltimiTrePacchetti();
                    double x = (100*target) / Math.abs(Lux2-Lux1); //--> Math.abs(Lux2-Lux1) : 100 = target : x
                    TAR.get(Cx)[i] = Math.toIntExact(Math.round(x));
                    historyTar.add(x);
                    value = TAR.get(Cx)[i];
                    valueMSB = (byte) ((value >> 8) & 0xFF);
                    valueLSB = (byte) (value & 0xFF);
                    payloadTAR[3] = valueMSB;
                    payloadTAR[4] = valueLSB;
                    switch (i) {
                        case 0:
                            this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_MIN, (byte)E120Constants.E120_SET_COMMAND));
                            break;
                        case 1:
                            this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_BAS, (byte)E120Constants.E120_SET_COMMAND));
                            break;
                        case 2:
                            this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_MED, (byte)E120Constants.E120_SET_COMMAND));
                            break;
                        case 3:
                            this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_ALT, (byte)E120Constants.E120_SET_COMMAND));
                            break;
                        case 4:
                            this.sendBytes(this.Command(payloadTAR, Proiettore.COEMAR_TAR_MAX, (byte)E120Constants.E120_SET_COMMAND));
                            break;
                        default:
                            System.out.println("Valore non gestito");
                    }
                    Lux1 = restituisciMediaUltimiTrePacchetti();
                }
                double tar = (historyTar.get(historyTar.size()-1) + historyTar.get(historyTar.size()-2))/2;
                TAR.get(Cx)[i] = Math.toIntExact(Math.round(tar));

            }


        }

    }

    public void proceduraOUTPUT_CH() throws InterruptedException {

Thread.sleep(10000);

        int pid = Proiettore.COEMAR_OUTPUTCH;
        // ROSSO
        byte[] pd = new byte[]{
                (byte) 0xFF, (byte) 0xFF,
                (byte) 0X00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00
        };
        this.sendBytes(this.Command(pd, pid, (byte) 0x30));
        // TODO: abbassare gli sleep dividendo per 100 e mettendoli dopo l'invio al frontend...sperando non si spacchi nulla
        Thread.sleep(5000);
        /*try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    //aggiornaFrontend();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }*/
        inviaDatiRealiAlFrontend(idClientTCP);
        //TODO: "inviaDatiRealiAlFrontend" va modificato perche prenda restituisciMediaUltimiTrePacchetti al posto di restituisciUltimoPacchetto dove necessario


        // ROSSO
        pd[2] = (byte) 0xFF;
        pd[3] = (byte) 0xFF;

        this.sendBytes(this.Command(pd, pid, (byte) 0x30));
        Thread.sleep(5000);
        /*try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    //aggiornaFrontend();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }*/

        inviaDatiRealiAlFrontend(idClientTCP);

        // VERDE
        pd[2] = (byte) 0x00;
        pd[3] = (byte) 0x00;

        pd[4] = (byte) 0xFF;
        pd[5] = (byte) 0xFF;

        this.sendBytes(this.Command(pd, pid, (byte) 0x30));
        Thread.sleep(5000);
        /*try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    //aggiornaFrontend();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }*/


        inviaDatiRealiAlFrontend(idClientTCP);

        // BLU
        pd[4] = (byte) 0x00;
        pd[5] = (byte) 0x00;

        pd[6] = (byte) 0xFF;
        pd[7] = (byte) 0xFF;

        this.sendBytes(this.Command(pd, pid, (byte) 0x30));
        Thread.sleep(5000);
        /*try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    //aggiornaFrontend();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }*/

        inviaDatiRealiAlFrontend(idClientTCP);


        // CIANO
        pd[6] = (byte) 0x00;
        pd[7] = (byte) 0x00;

        pd[8] = (byte) 0xFF;
        pd[9] = (byte) 0xFF;

        this.sendBytes(this.Command(pd, pid, (byte) 0x30));
        Thread.sleep(5000);
        /*try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    //aggiornaFrontend();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }*/


        inviaDatiRealiAlFrontend(idClientTCP);

        // LIME
        pd[8] = (byte) 0x00;
        pd[9] = (byte) 0x00;

        pd[10] = (byte) 0xFF;
        pd[11] = (byte) 0xFF;

        this.sendBytes(this.Command(pd, pid, (byte) 0x30));
        Thread.sleep(5000);
        /*try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    //aggiornaFrontend();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }*/


        inviaDatiRealiAlFrontend(idClientTCP);

        // AMBRA
        pd[10] = (byte) 0x00;
        pd[11] = (byte) 0x00;

        pd[12] = (byte) 0xFF;
        pd[13] = (byte) 0xFF;

        this.sendBytes(this.Command(pd, pid, (byte) 0x30));
        Thread.sleep(5000);
        /*try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    //aggiornaFrontend();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }*/

        inviaDatiRealiAlFrontend(idClientTCP);


        pd[12] = (byte) 0x00;
        pd[13] = (byte) 0x00;

        this.sendBytes(this.Command(pd, pid, (byte) 0x30));
        Thread.sleep(5000);
        /*try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    //aggiornaFrontend();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }*/




        this.closePort();

    }

    public void inviaDatiRealiAlFrontend(int tabId) throws InterruptedException {
        System.out.println(">>> Inizio aggiornaFrontend()");


        PacchettoDaClient pacchettoDaClient = restituisciUltimoPacchetto();
        if (pacchettoDaClient == null) {
            System.out.println("⚠️ restituisciUltimoPacchetto() ha restituito null, esco.");
            return;
        }


        System.out.println("PacchettoDaClient ricevuto: " + pacchettoDaClient);



        // Spettro
        double[] spettroArray = pacchettoDaClient.getSpectrum();
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();

        double xStart = 360.0;
        double xEnd = 830.0;
        int n = spettroArray != null ? spettroArray.length : 0;
        double passo = (xEnd - xStart) / (n - 1);

        for (int i = 0; i < n; i++) {
            xList.add(xStart + i * passo);
        }
        if (spettroArray != null) {
            for (double v : spettroArray) {
                yList.add(v);
            }
        }

        // TCS
        double[] tcsArray = pacchettoDaClient.getTCS();
        if (tcsArray == null) {
            System.out.println("⚠️ tcsArray è null");
        } else {
            System.out.println("TCSArray lunghezza: " + tcsArray.length);
        }
        List<Integer> cri = Arrays.stream(tcsArray)
                .mapToInt(d -> (int) Math.round(d))
                .boxed()
                .collect(Collectors.toList());

        // Dati valori
        Map<String, Double> datiValori = new LinkedHashMap<>();
        datiValori.put("CCT", pacchettoDaClient.getCCT());
        datiValori.put("Irr", pacchettoDaClient.getLux());
        datiValori.put("X", pacchettoDaClient.getTristimulusX());
        datiValori.put("Y", pacchettoDaClient.getTristimulusY());
        datiValori.put("Z", pacchettoDaClient.getTristimulusZ());
        datiValori.put("x", pacchettoDaClient.getX());
        datiValori.put("y", pacchettoDaClient.getY());
        datiValori.put("C1 of X", pacchettoDaClient.getTristimulusX1());
        datiValori.put("C2 of X", pacchettoDaClient.getTristimulusX2());
        datiValori.put("u'", pacchettoDaClient.getU_primo());
        datiValori.put("v'", pacchettoDaClient.getV_primo());
        datiValori.put("CRI", pacchettoDaClient.getCRI());
        datiValori.put("CRI Tolerance", pacchettoDaClient.getCRI_tolerance());
        datiValori.put("Fidelity", pacchettoDaClient.getFidelityIndex());
        datiValori.put("Fidelity log", pacchettoDaClient.getFidelityIndexLog());
        datiValori.put("TLCI", pacchettoDaClient.getTLCI());
        datiValori.put("DUV", pacchettoDaClient.getDuv());
        datiValori.put("Integration time", pacchettoDaClient.getIntegrationTime());

        System.out.println("Dati valori estratti:");
        datiValori.forEach((k,v) -> System.out.println("  " + k + " = " + v));

        // Mappa finale per JSON
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put("x", xList);
        jsonMap.put("y", yList);
        jsonMap.put("basic", datiValori);
        jsonMap.put("cri", cri);

        String json = new com.google.gson.Gson().toJson(jsonMap);

        // Invio dati via WebSocket (usa la tua classe)
        GraficoSocket.inviaDati(String.valueOf(tabId), json);

    }


    public Proiettore(String port, int idClientTCP) {
        this.port = port;
        this.porta = SerialPort.getCommPort(port);
        this.idClientTCP = idClientTCP;
    }

    public boolean prepareAndOpenPort() {
        porta.setComPortParameters(RDMbaudrate, 8, SerialPort.TWO_STOP_BITS, SerialPort.NO_PARITY);
        porta.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        return porta.openPort();
    }

    public void closePort() {
        porta.closePort();
    }


    public void sendBytes(byte[] payload) {
        try {
            porta.setBreak();
            Thread.sleep(1);
            porta.clearBreak();
            buffer_trasmissione = payload;
            int bytesWritten = porta.writeBytes(buffer_trasmissione, buffer_trasmissione.length);
            if (bytesWritten == -1) {
                System.out.println("Errore nella scrittura sulla porta seriale");
            } else {
                System.out.println("Bytes scritti: " + bytesWritten);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isValidDiscoveryResponse(byte[] packet) {
        if (packet.length == 24) {
            int expectedChecksum = 0;

            for (int i = 8; i <= 19; i++) {
                expectedChecksum += (packet[i] & 0xFF);
            }

            expectedChecksum = expectedChecksum & 0xFFFF;

            if (convertBytesToChecksumInt(parseChecksumFromResponse(packet)) == expectedChecksum) {
                System.out.println("Pacchetto Discovery valido");
                return true; // Pacchetto Discovery valido
            }
            System.out.println("Pacchetto Discovery NON valido");
            return false; // Pacchetto Discovery valido
        }
        System.out.println("Pacchetto Discovery NON valido");
        return false; // Pacchetto Discovery valido
    }

    public boolean isValidRdmResponse(byte[] packet) {

        if (packet == null || packet.length < 24) {
            System.out.println("Pacchetto troppo corto o nullo.");
            return false;
        }


        // Start Code e Sub Start Code
        if (packet[0] != (byte) 0xCC || packet[1] != (byte) 0x01) {
            System.out.println("Start code non valido.");
            return false;
        }

        int lengthFromPacket = packet[2] & 0xFF;  // unsigned
        int totalLength = lengthFromPacket + 2;   // come in buildPacket

        if (packet.length != totalLength) {
            System.out.println("Lunghezza pacchetto non corrisponde.");
            return false;
        }

        // Calcolo checksum
        long checksum = 0;
        for (int i = 0; i < totalLength - 2; i++) {
            checksum += (packet[i] & 0xFF);
            checksum &= 0xFFFF; // limit a 16 bit
        }

        int checksumIndex = totalLength - 2;
        int packetChecksum = ((packet[checksumIndex] & 0xFF) << 8) | (packet[checksumIndex + 1] & 0xFF);

        if ((checksum & 0xFFFF) != packetChecksum) {
            System.out.println("Checksum non valido.");
            return false;
        }

        return true;
    }


    public byte[] Command(byte[] pd, int pid, byte cc) {
        if (tn > 255) tn = 0;
        System.out.println("uidDst = " + Arrays.toString(uidDst));
        System.out.println("uidSrc = " + Arrays.toString(uidSrc));

        PacchettoRDM pacchetto = new PacchettoRDM(uidDst, uidSrc, (byte) tn++, cc, pid, pd);
        byte[] rdmBytes = pacchetto.buildPacket();
        System.out.println("Bytes da inviare:");
        System.out.println(Arrays.toString(rdmBytes));

        return rdmBytes;
    }

    public static void stampaHex(byte[] array) {
        for (byte b : array) {
            System.out.printf("%02X ", (int)b);
        }
        System.out.println();
    }


    public void resetRX() throws InterruptedException {
        rdmOffsetRead = 0;
        fResetDataRead = false;
        fNewDataRead = false;
        Arrays.fill(rdmBufferIn, (byte) 0);

    }


    public byte[] parseUidFromResponse(byte[] data) {
        if (data == null || data.length != 24) return null; // UID deve stare almeno da byte 3 a 8
        byte[] encodedBytes = Arrays.copyOfRange(data, 8, 20);

        byte[] uid = new byte[6];

        for (int i = 0; i < 6; i++) {
            int byte1 = encodedBytes[i * 2] & 0x55;
            int byte2 = encodedBytes[i * 2 + 1] & 0xAA;
            uid[i] = (byte) (byte1 | byte2);
        }

        return uid;
    }


    public byte[] parseChecksumFromResponse(byte[] data) {
        if (data == null || data.length != 24) return null; // Assumiamo ancora risposta di 24 byte

        // Estrai i 4 byte codificati del checksum: data[21], data[22], data[23], data[24]
        // Il range è da 'from' (incluso) a 'to' (escluso).
        byte[] encodedChecksumBytes = Arrays.copyOfRange(data, 20, 24); // ECS3, ECS2, ECS1, ECS0

        // Il checksum finale è di 2 byte (16 bit)
        byte[] actualChecksum = new byte[2];

        // Decodifica Cheksum1 (MSB)
        // encodedChecksumBytes[0] = ECS3 (data[21]) -> OR with 0xAA
        // encodedChecksumBytes[1] = ECS2 (data[22]) -> OR with 0x55
        int msb_byte1 = encodedChecksumBytes[0] & 0x55;
        int msb_byte2 = encodedChecksumBytes[1] & 0xAA;
        actualChecksum[0] = (byte) (msb_byte1 | msb_byte2); // Questo è Cheksum1 (MSB)

        // Decodifica Cheksum0 (LSB)
        // encodedChecksumBytes[2] = ECS1 (data[23]) -> OR with 0xAA
        // encodedChecksumBytes[3] = ECS0 (data[24]) -> OR with 0x55
        int lsb_byte1 = encodedChecksumBytes[2] & 0x55;
        int lsb_byte2 = encodedChecksumBytes[3] & 0xAA;
        actualChecksum[1] = (byte) (lsb_byte1 | lsb_byte2); // Questo è Cheksum0 (LSB)

        return actualChecksum;
    }

    public static int convertBytesToChecksumInt(byte[] checksumBytes) {
        if (checksumBytes == null || checksumBytes.length != 2) {
            // Gestisci l'errore o lancia un'eccezione se l'array non è valido
            throw new IllegalArgumentException("Checksum byte array must be non-null and have a length of 2.");
        }

        // Il primo byte è MSB, il secondo è LSB
        // (byte[0] & 0xFF) lo converte in un int positivo da 0 a 255
        // << 8 lo sposta di 8 posizioni a sinistra per diventare la parte alta a 16 bit
        // (byte[1] & 0xFF) lo converte in un int positivo da 0 a 255
        // L'operazione | unisce i due valori
        return (((checksumBytes[0] & 0xFF) << 8) | (checksumBytes[1] & 0xFF));
    }

    public byte[] readPacket() throws InterruptedException {
        //if (!fNewDataRead) return null;
        //rdmBufferIn è un array più grande (es. 1024 byte) usato per ricevere i dati dalla seriale.
        //rdmOffsetRead rappresenta quanti byte validi ci sono attualmente nel buffer.
        byte[] packet = Arrays.copyOf(rdmBufferIn, rdmOffsetRead);
        resetRX();
        return packet;
    }


    public void getBytes() {
        int bytesAvailable = porta.bytesAvailable();
        //System.out.println("Bytes disponibili: " + bytesAvailable);

        if (bytesAvailable > 0) {
            byte[] tempBuffer = new byte[bytesAvailable];
            System.out.println("Buffer temporaneo creato di dimensione: " + tempBuffer.length);

            int bytesRead = porta.readBytes(tempBuffer, tempBuffer.length);
            System.out.println("Bytes letti: " + bytesRead);

            if (bytesRead > 0) {
                bufferIn.write(tempBuffer, 0, bytesRead);
                System.out.println("Dati scritti su bufferIn. Lunghezza dati scritti: " + bytesRead);

                processBuffer();
                System.out.println("processBuffer() chiamata");
            } else {
                System.out.println("Nessun byte letto (bytesRead <= 0)");
            }
        } else {

        }
    }


    private void processBuffer() {
        byte[] data = bufferIn.toByteArray();
//        System.out.println(">> processBuffer() chiamata");
//        System.out.println("data.length: " + data.length);

        if (data.length < 24) {
//            System.out.println("⚠️ Buffer troppo corto per processare pacchetti");
        }

        int index = 0;

        while (data.length - index >= 24) { // dimensione minima plausibile
//            System.out.printf("➡️ Controllo a index %d: byte = 0x%02X%n", index, data[index]);

            boolean pacchettoRDM = false;
            boolean pacchettoDISCOVERY = false;

            // Verifica se è un pacchetto RDM (Start Code 0xCC)
            if ((data[index] & 0xFF) == 0xCC) {
//                System.out.println("Trovato possibile pacchetto RDM");
                pacchettoRDM = true;
            }

            // Verifica se è una possibile risposta DISCOVERY (Pattern noto)
            if ((data[index] & 0xFF) == 0xFE &&          // Slot 1 (index + 0) = 0xFE
                    (data[index + 1] & 0xFF) == 0xFE &&      // Slot 2 (index + 1) = 0xFE
                    (data[index + 2] & 0xFF) == 0xFE &&      // Slot 3 (index + 2) = 0xFE
                    (data[index + 3] & 0xFF) == 0xFE &&      // Slot 4 (index + 3) = 0xFE
                    (data[index + 4] & 0xFF) == 0xFE &&      // Slot 5 (index + 4) = 0xFE
                    (data[index + 5] & 0xFF) == 0xFE &&      // Slot 6 (index + 5) = 0xFE
                    (data[index + 6] & 0xFF) == 0xFE &&      // Slot 7 (index + 6) = 0xFE
                    (data[index + 7] & 0xFF) == 0xAA) {      // Slot 8 (index + 7) = 0xAA (Preamble separator)
//                System.out.println("Trovato possibile pacchetto DISCOVERY");
                pacchettoDISCOVERY = true;
            }

            if (!pacchettoRDM && !pacchettoDISCOVERY) {
                index++;
                continue;
            }

            // Pacchetto RDM
            if (pacchettoRDM) {
                if (data.length - index < 24) {
//                    System.out.println("Pacchetto RDM troppo corto, esco");
                    break; // pacchetto troppo corto
                }

                int length = (data[index + 2] & 0xFF);
                int totalLength = length + 2;

                if (index + totalLength > data.length) {
//                    System.out.println("Pacchetto RDM incompleto, esco");
                    break; // incompleto
                }

                byte[] packet = Arrays.copyOfRange(data, index, index + totalLength);

                if (isValidRdmResponse(packet)) {
                    System.out.println("✅ Pacchetto RDM valido");
                    fNewDataRead = true;
                    rdmOffsetRead = packet.length;
                    rdmBufferIn = Arrays.copyOf(packet, rdmOffsetRead);
                } else {
                    System.out.println("❌ Pacchetto RDM non valido, scarto");
                }

                index += totalLength;
                continue;
            }

            // Pacchetto DISCOVERY
            int discLen = 24; // può essere 24 o più, verifica con specifica
            if (index + discLen > data.length) {
                System.out.println("Pacchetto DISCOVERY incompleto, esco");
                break; // incompleto
            }

            byte[] packet = Arrays.copyOfRange(data, index, index + discLen);
            if (isValidDiscoveryResponse(packet)) {
                System.out.println("✅ Pacchetto DISCOVERY trovato");
                fNewDataRead = true;
                rdmOffsetRead = packet.length;
                rdmBufferIn = Arrays.copyOf(packet, rdmOffsetRead);
            }

            index += discLen;
        }

        // Ricostruisci il buffer con i byte residui non ancora completi
        bufferIn.reset();
        if (index < data.length) {
//            System.out.println("Byte residui rimasti nel buffer: " + (data.length - index));
            bufferIn.write(data, index, data.length - index);
        }
    }


    public static Proiettore findPortaSeriale(List<Proiettore> porte, String port) {
        Proiettore result = null;
        for (Proiettore p : porte) {
            if (p.port.equalsIgnoreCase(port)) {
                result = p;
                break;
            }
        }
        return result;
    }


    @Override
    public void run() {
        System.out.println("Thread avviato per porta: " + porta.getSystemPortName());

        byte[] lowerBoundUid = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        byte[] upperBoundUid = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        byte[] pd = new byte[12];
        System.arraycopy(lowerBoundUid, 0, pd, 0, 6);
        System.arraycopy(upperBoundUid, 0, pd, 6, 6);

        PacchettoRDM discovery = new PacchettoRDM(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF},
                Proiettore.SRC_UID, (byte) 0x00, Proiettore.E120_DISCOVERY_COMMAND, Proiettore.E120_DISC_UNIQUE_BRANCH, pd);


        sendBytes(discovery.buildPacket());

        long timeout = 1000;
        long startTime = System.currentTimeMillis();


        while (System.currentTimeMillis() - startTime < timeout) {

            getBytes(); // Questo metodo riempie rdmBufferIn e imposta fNewDataRead
            if (fNewDataRead) {
                byte[] responsePacket = null; // Ottieni il pacchetto letto
                try {
                    responsePacket = readPacket();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                //uidDst = nei byte dal 9 al 20 della risposta di discovery
                uidDst = parseUidFromResponse(responsePacket);
                System.out.println("uidDst " + Arrays.toString(uidDst));
                break;
            }

        }

        while (true) {
            while (System.currentTimeMillis() - startTime < timeout) {

                getBytes(); // Questo metodo riempie rdmBufferIn e imposta fNewDataRead
                if (fNewDataRead) {

                    try {
                        byte[] responsePacket = readPacket(); // Ottieni il pacchetto letto

                        int pidFromPacket = ((responsePacket[21] & 0xFF) << 8) | (responsePacket[22] & 0xFF);

                        if(responsePacket[20] == (byte)E120Constants.E120_GET_COMMAND_RESPONSE &&
                                pidFromPacket == COEMAR_OUTPUTCH){
                            int numBit = responsePacket[23]*8;
                            numColori = (numBit-16)/16;
                            System.out.println("NumColori trovato = " + numColori);
                        }


                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }

            }
            startTime = System.currentTimeMillis();
        }

    }
}
