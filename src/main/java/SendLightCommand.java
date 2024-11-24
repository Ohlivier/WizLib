import java.io.IOException;
import java.net.*;

public class SendLightCommand {

    private String LIGHT_IP = "192.168.0.186";
    private DatagramSocket socket;
    private InetAddress address;
    private byte[] buf;

    public SendLightCommand() throws UnknownHostException, SocketException {
        socket = new DatagramSocket();
        address = InetAddress.getByName(LIGHT_IP);
    }

    public String sendCommand(String command) throws IOException {
        buf = command.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 38899);
        socket.send(packet);
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        return received;
    }

}
