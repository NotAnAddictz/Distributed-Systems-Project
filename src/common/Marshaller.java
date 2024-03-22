package common;

import java.nio.charset.StandardCharsets;

public class Marshaller {
    private static final String DELIMITER = "::";

    // Convert String to byte array
    public byte[] marshal(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    // Convert byte array to String
    public String unmarshal(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] readFileMarshal(int chosen, String fileName, int offset, int readBytes){
        String concatenatedString = chosen + DELIMITER + fileName + DELIMITER + offset + DELIMITER + readBytes;
        return concatenatedString.getBytes();
    }

    public String[] readFileUnmarshal(byte[] marshaledData) {
        String concatenatedString = new String(marshaledData);
        return concatenatedString.split(DELIMITER);
    }

}