package common;

import java.nio.charset.StandardCharsets;

public class Marshaller {
    // Convert String to byte array
    public byte[] marshal(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    // Convert byte array to String
    public String unmarshal(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

}