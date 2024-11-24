import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) throws IOException {
        SendLightCommand light = new SendLightCommand();
        System.out.println(light.sendCommand("{\"id\":1,\"method\":\"setState\",\"params\":{\"state\":true}}\n"));
    }
}
