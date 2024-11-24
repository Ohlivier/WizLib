import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class SendLightCommand {

    private final DatagramSocket socket;
    private final InetAddress address;

    public SendLightCommand() {
        try {
            socket = new DatagramSocket();
            String LIGHT_IP = "192.168.0.186";
            address = InetAddress.getByName(LIGHT_IP);
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private String send_command(String command) {
        byte[] buf = command.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 38899);
        try {
            socket.send(packet);
            byte[] receiveBuf = new byte[1024];
            packet = new DatagramPacket(receiveBuf, receiveBuf.length);
            socket.receive(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(packet.getData(), 0, packet.getLength());
    }

    public void get_light_status() {
        String status = send_command("{\"method\":\"getPilot\",\"params\":{}}\n");
        System.out.println(status);
    }

    public void turn_light_on() {
        String response = send_command("{\"id\":1,\"method\":\"setState\",\"params\":{\"state\":true}}\n");
        System.out.println(response);
    }

    public void turn_light_off() {
        String response = send_command("{\"id\":1,\"method\":\"setState\",\"params\":{\"state\":false}}\n");
        System.out.println(response);
    }

    public void set_light_color(int r, int g, int b) {
        if (0 > r || 255 < r) {
            throw new IndexOutOfBoundsException("Invalid RED value: " + r + " (value should be between 0-255)");
        } else if (0 > g || 255 < g) {
            throw new IndexOutOfBoundsException("Invalid GREEN value: " + g + " (value should be between 0-255)");
        } else if (0 > b || 255 < b) {
            throw new IndexOutOfBoundsException("Invalid BLUE value: " + b + " (value should be between 0-255)");
        }

        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"r\":%d,\"g\":%d,\"b\":%d,\"dimming\": 100}}", r, g, b);
        String response = send_command(command);
        System.out.println(response);
    }

    public void set_light_color(int r, int g, int b, int brightness) {
        if (0 > r || 255 < r) {
            throw new IndexOutOfBoundsException("Invalid RED value: " + r + " (value should be between 0-255)");
        } else if (0 > g || 255 < g) {
            throw new IndexOutOfBoundsException("Invalid GREEN value: " + g + " (value should be between 0-255)");
        } else if (0 > b || 255 < b) {
            throw new IndexOutOfBoundsException("Invalid BLUE value: " + b + " (value should be between 0-255)");
        } else if (10 > brightness || 100 < brightness) {
            throw new IndexOutOfBoundsException("Invalid BRIGHTNESS value: " + brightness + " (value should be between 10-100)");
        }

        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"r\":%d,\"g\":%d,\"b\":%d,\"dimming\": %d}}", r, g, b, brightness);
        String response = send_command(command);
        System.out.println(response);
    }

    public void set_light_kelvin(int temp, int brightness) {
        if (2200 > temp || 6200 < temp) {
            throw new IndexOutOfBoundsException("Invalid TEMP value: " + temp + " (value should be between 2200-6200");
        } else if (10 > brightness || 100 < brightness) {
            throw new IndexOutOfBoundsException("Invalid BRIGHTNESS value: " + brightness + " (value should be between 10-100)");
        }
        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"temp\":%d,\"dimming\":%d}}\n", temp, brightness);
        String response = send_command(command);
        System.out.println(response);
    }

}
