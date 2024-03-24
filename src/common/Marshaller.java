package common;

import java.nio.charset.StandardCharsets;

public class Marshaller {
    private static final String DELIMITER = "::";

    // Convert String to byte array
    // public byte[] marshal(String str) {
    // return str.getBytes(StandardCharsets.UTF_8);
    // }

    // Convert byte array to String
    // public String unmarshal(byte[] bytes) {
    // return new String(bytes, StandardCharsets.UTF_8);
    // }

    public byte[] readFileMarshal(int funcID, String fileName, int offset, int readBytes) {
        return marshal(funcID, fileName, Integer.toString(offset), Integer.toString(readBytes));
    }

    public byte[] monitorFileMarshal(int funcID, String fileName, int duration) {
        return marshal(funcID, fileName, Integer.toString(duration));
    }

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
}