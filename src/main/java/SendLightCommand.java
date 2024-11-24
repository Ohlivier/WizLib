import java.io.IOException;
import java.net.*;

import org.json.JSONObject;

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

    // TODO Delete this method
    public int get_current_brightness() {
        JSONObject getBrightness = new JSONObject(get_light_status()).getJSONObject("result");
        System.out.println(getBrightness);
        return getBrightness.getInt("dimming");
    }

    public boolean is_light_on() {
        JSONObject getStatus = new JSONObject(get_light_status()).getJSONObject("result");
        return getStatus.getBoolean("state");
    }

    /**
     * Prints the current status of the WIZ light
     */
    public String get_light_status() {
        return send_command("{\"method\":\"getPilot\",\"params\":{}}\n");
    }

    /**
     * Turns the WIZ light on
     */
    public void turn_light_on() {
        send_command("{\"id\":1,\"method\":\"setState\",\"params\":{\"state\":true}}\n");
    }

    /**
     * Turns the WIZ light off
     */
    public void turn_light_off() {
        send_command("{\"id\":1,\"method\":\"setState\",\"params\":{\"state\":false}}\n");
    }

    public void set_brightness(int brightness) {
        if (!is_light_on()) {
            System.out.println("Light is not on, cannot currently change the brightness");
            return;
        }
        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"dimming\":%d}}", brightness);
        send_command(command);
    }

    /**
     * Sets the WIZ light to a certain RGB value
     *
     * @param r The red RGB value (0-255)
     * @param g The green RGB value (0-255)
     * @param b The blue RGB value (0-255)
     */
    public void set_light_color(int r, int g, int b) {
        if (0 > r || 255 < r) {
            throw new IndexOutOfBoundsException("Invalid RED value: " + r + " (value should be between 0-255)");
        } else if (0 > g || 255 < g) {
            throw new IndexOutOfBoundsException("Invalid GREEN value: " + g + " (value should be between 0-255)");
        } else if (0 > b || 255 < b) {
            throw new IndexOutOfBoundsException("Invalid BLUE value: " + b + " (value should be between 0-255)");
        }

        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"r\":%d,\"g\":%d,\"b\":%d}}", r, g, b);
        send_command(command);
    }

    /**
     * Sets the WIZ light to a certain RGB value and brightness
     *
     * @param r          The red RGB value (0-255)
     * @param g          The green RGB value (0-255)
     * @param b          The blue RGB value (0-255)
     * @param brightness The brightness (10-100)
     */
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
        send_command(command);
    }


    /**
     * Sets light to a custom warm/cool white
     *
     * @param temp Temperature in kelvin (2200-6200)
     */
    public void set_light_kelvin(int temp) {
        if (2200 > temp || 6200 < temp) {
            throw new IndexOutOfBoundsException("Invalid TEMP value: " + temp + " (value should be between 2200-6200");
        }
        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"temp\":%d}}\n", temp);
        send_command(command);
    }


    /**
     * Sets light to a custom warm/cool white
     *
     * @param temp       Temperature in kelvin (2200-6200)
     * @param brightness The brightness (10-100)
     */
    public void set_light_kelvin_brightness(int temp, int brightness) {
        if (2200 > temp || 6200 < temp) {
            throw new IndexOutOfBoundsException("Invalid TEMP value: " + temp + " (value should be between 2200-6200");
        } else if (10 > brightness || 100 < brightness) {
            throw new IndexOutOfBoundsException("Invalid BRIGHTNESS value: " + brightness + " (value should be between 10-100)");
        }
        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"temp\":%d,\"dimming\":%d}}\n", temp, brightness);
        send_command(command);
    }
}
