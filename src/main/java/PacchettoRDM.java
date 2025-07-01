public class PacchettoRDM {



    private final byte START_CODE = (byte) 0xCC;
    private final byte SUB_START_CODE = (byte) 0x01;

    private byte[] uidDest;    // 6 byte
    private byte[] uidSource;  // 6 byte
    private byte tn;           // Transaction Number
    private byte cc;           // Command Class
    private int pid;           // 2 byte
    private byte[] pd;         // Parameter Data

    public PacchettoRDM(byte[] uidDest, byte[] uidSource, byte tn, byte cc, int pid, byte[] pd) {
        if (uidDest == null) {
            System.out.println("uidDest e' null");
            throw new IllegalArgumentException("uidDest non può essere null");
        }

        if (uidSource == null) {
            System.out.println("uidSource e' null");
            throw new IllegalArgumentException("uidSource non può essere null");
        }

        if (uidDest.length != 6  || uidSource.length != 6)
            throw new IllegalArgumentException("UID devono essere lunghi 6 byte.");
        this.uidDest = uidDest;
        this.uidSource = uidSource;
        this.tn = tn;
        this.cc = cc;
        this.pid = pid;
        this.pd = pd;
    }

    public byte[] buildPacket() {
        int pdl = pd.length;
        int totalLength = 24 + pdl + 2;

        byte[] packet = new byte[totalLength];

        packet[0] = START_CODE;
        packet[1] = SUB_START_CODE;
        packet[2] = (byte) (totalLength-2);

        System.arraycopy(uidDest, 0, packet, 3, 6);      // 3–8
        System.arraycopy(uidSource, 0, packet, 9, 6);    // 9–14

        packet[15] = tn;
        packet[16] = 0x00; // Port ID + response type
        packet[17] = 0x00; // Message count
        packet[18] = 0x00; // Sub Device MSB
        packet[19] = 0x00; // Sub Device LSB
        packet[20] = cc;
        packet[21] = (byte)((pid >> 8) & 0xFF); // PID MSB
        packet[22] = (byte)(pid & 0xFF);        // PID LSB
        packet[23] = (byte) pdl;

        // Copia Parameter Data a partire da posizione 24
        System.arraycopy(pd, 0, packet, 24, pdl);

        // Checksum
        long checksum = 0;
        for (int j = 0; j < totalLength - 2; j++) {
            checksum += (packet[j] & 0xFF);
            // limita a 16 bit dopo ogni somma
        }

        int checksumIndex = totalLength - 2;
        packet[checksumIndex] = (byte)((checksum >> 8) & 0xFF);   // MSB
        packet[checksumIndex + 1] = (byte)(checksum & 0xFF);      // LSB

        return packet;
    }

    public static byte[] buildPayloadTar(int command, byte index, int value) {
        byte[] result = new byte[5];
        result[0] = (byte) ((command >> 8) & 0xFF);  // MSB di command
        result[1] = (byte) (command & 0xFF);         // LSB di command
        result[2] = index;
        result[3] = (byte) ((value >> 8) & 0xFF);    // MSB di value
        result[4] = (byte) (value & 0xFF);           // LSB di value
        return result;
    }

    public static byte[] buildPayloadCCTDuv(int command, byte index, double cct, double duv) {
        byte[] result = new byte[5];
        result[0] = (byte) ((command >> 8) & 0xFF);  // MSB di command
        result[1] = (byte) (command & 0xFF);         // LSB di command
        result[2] = index;

        result[3] = (byte) ((int) cct);             // CCT già tarato (0–255)
        result[4] = (byte) ((int) duv);             // DUV già tarato (0–255)
        return result;
    }

    public static byte[] buildPayloadSetCCTSave(int cct) {
        byte[] result = new byte[3];

        result[0] = (byte) ((cct >> 8) & 0xFF);  // MSB del CCT
        result[1] = (byte) (cct & 0xFF);         // LSB del CCT
        result[2] = (byte) 0xFF;                 // Byte di salvataggio

        return result;
    }

    public static byte[] buildPayloadSet8020(int command, byte index, byte[] values, int cct) {
        if (values == null || values.length != 8) {
            throw new IllegalArgumentException("values deve contenere esattamente 8 byte");
        }

        byte[] result = new byte[13];

        // Comando 0x8020
        result[0] = (byte) ((command >> 8) & 0xFF); // MSB
        result[1] = (byte) (command & 0xFF);        // LSB

        // Index
        result[2] = index;

        // Copia gli 8 byte di value
        System.arraycopy(values, 0, result, 3, 8);

        // CCT a 16 bit (MSB + LSB)
        result[11] = (byte) ((cct >> 8) & 0xFF); // MSB
        result[12] = (byte) (cct & 0xFF);        // LSB

        return result;
    }




}

