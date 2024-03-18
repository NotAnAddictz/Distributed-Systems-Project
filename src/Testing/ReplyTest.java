package Testing;

import Marshalling.Reply;

public class ReplyTest {
    public static void main(String[] args) throws Exception {
        String s = "200, Hello, World!";
        System.out.println(s);
        Reply reply = new Reply();
        reply.response = s;
        reply.status = 200;

        byte[] marshalled = reply.marshal();
        System.out.println("Marshalled the data");
        Reply response = new Reply();
        response.unmarshal(marshalled);
        System.out.println("Unmarshalled the data");
        System.out.printf("Status:%d; Response:%s", response.status, response.response);
    }
}
