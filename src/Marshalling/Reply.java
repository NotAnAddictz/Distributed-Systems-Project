package Marshalling;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class Reply {
    public short status; // 200 Success, 
    public String response;

    public byte[] marshal() {
        ByteBuffer statusBuf =  ByteBuffer.allocate(2);
        statusBuf.putShort(this.status).flip();
        byte[] str = response.getBytes(StandardCharsets.UTF_8);
        byte[] messageArray = new byte[statusBuf.capacity()+str.length];
        ByteBuffer byteBuffer = ByteBuffer.wrap(messageArray);
        byteBuffer.put(statusBuf);
        byteBuffer.put(str);
        return byteBuffer.array();
    }

    public void unmarshal(byte[] byteArray) {
        byte[] status = new byte[2];
        System.arraycopy(byteArray, 0, status, 0, 2);
        this.status = ByteBuffer.wrap(status).order(ByteOrder.BIG_ENDIAN).getShort();
        int size = byteArray.length;
        byte[] response = new byte[size - 2];
        System.arraycopy(byteArray, 2, response, 0, size - 2);
        this.response = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(response)).toString();
    }
}
