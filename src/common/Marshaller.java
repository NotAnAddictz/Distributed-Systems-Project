package common;

import java.nio.charset.StandardCharsets;

public class Marshaller {
    private static final String DELIMITER = "::";

    public byte[] marshal(int funcID, String... args) {
        String concatenatedString = Integer.toString(funcID);
        for (int i = 0; i < args.length; i++) {
            concatenatedString += DELIMITER + args[i];
        }
        concatenatedString = concatenatedString.trim();
        return concatenatedString.getBytes(StandardCharsets.UTF_8);
    }

    public String[] unmarshal(byte[] marshaledData) {
        String concatenatedString = new String(marshaledData, StandardCharsets.UTF_8);
        String[] strArr = concatenatedString.split(DELIMITER);
        for (int i = 0; i < strArr.length; i++) {
            strArr[i] = strArr[i].trim();
        }
        return strArr;
    }

    public byte[] readFileMarshal(int funcID, String fileName, int offset, int readBytes) {
        return marshal(funcID, fileName, Integer.toString(offset), Integer.toString(readBytes));
    }

    public byte[] writeFileMarshal(int funcID, String fileName, int offset, String write) {
        return marshal(funcID, fileName, Integer.toString(offset), write);
    }

    public byte[] monitorFileMarshal(int funcID, String fileName, int duration) {
        return marshal(funcID, fileName, Integer.toString(duration));
    }
}