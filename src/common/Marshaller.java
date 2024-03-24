package common;

import java.nio.charset.StandardCharsets;

public class Marshaller {
    private static final String DELIMITER = "::";

    public byte[] marshal(int funcID, String... args) {
        String concatenatedString = Integer.toString(funcID);
        for (int i = 0; i < args.length; i++) {
            concatenatedString += DELIMITER + args[i];
        }
        return concatenatedString.getBytes();
    }

    public String[] unmarshal(byte[] marshaledData) {
        String concatenatedString = new String(marshaledData);
        return concatenatedString.split(DELIMITER);
    }

    public byte[] readFileMarshal(int funcID, String fileName, int offset, int readBytes){
        return marshal(funcID, fileName, Integer.toString(offset), Integer.toString(readBytes));
    }

    public byte[] writeFileMarshal(int funcID, String fileName, int offset, String write){
        return marshal(funcID, fileName, Integer.toString(offset), write);
    }
}