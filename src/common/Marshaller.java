package common;

import java.nio.charset.StandardCharsets;

public class Marshaller {
    private static final String DELIMITER = "::";

    public byte[] marshal(int funcId, int packetId, String... args) { // Convert funcId, packetId and arguments into a byte array
        String concatenatedString = Integer.toString(funcId) + DELIMITER + Integer.toString(packetId);
        for (int i = 0; i < args.length; i++) {
            concatenatedString += DELIMITER + args[i];
        }
        concatenatedString = concatenatedString.trim();
        return concatenatedString.getBytes(StandardCharsets.UTF_8);
    }

    public String[] unmarshal(byte[] marshaledData) { // Convert a byte array back into String array of funcId, packetId and data
        String concatenatedString = new String(marshaledData, StandardCharsets.UTF_8);
        String[] strArr = concatenatedString.split(DELIMITER);
        for (int i = 0; i < strArr.length; i++) {
            strArr[i] = strArr[i].trim();
        }
        return strArr;
    }
}