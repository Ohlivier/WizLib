import java.io.File;
import java.io.IOException;
import java.net.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LightController implements AutoCloseable {

    private final DatagramSocket socket;
    private final InetAddress address;
    private final ObjectMapper objectMapper;

    public LightController() {
        this(new File("lights.json"));
    }

    public LightController(File jsonIpFile) {
        objectMapper = new ObjectMapper();
        try {
            JsonNode ip = objectMapper.readTree(jsonIpFile);
            socket = new DatagramSocket();
            String LIGHT_IP = ip.get("ip").asText();
            address = InetAddress.getByName(LIGHT_IP);
            socket.setSoTimeout(LightConstants.SOCKET_TIMEOUT_MS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LightController(String lightIP) {
        objectMapper = new ObjectMapper();
        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName(lightIP);
            socket.setSoTimeout(LightConstants.SOCKET_TIMEOUT_MS);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private String send_command(String command) {
        byte[] buf = command.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, LightConstants.PORT);
        try {
            socket.send(packet);
            byte[] receiveBuf = new byte[LightConstants.RECEIVE_BUFFER_SIZE];
            packet = new DatagramPacket(receiveBuf, receiveBuf.length);
            socket.receive(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(packet.getData(), 0, packet.getLength());
    }

    private JsonNode create_jsonNode_from_status() {
        try {
            return objectMapper.readTree(get_light_status());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the current brightness of the WIZ light, returns 0 if the light is off
     *
     * @return current brightness of the WIZ light (10-100)
     */
    public int get_current_brightness() {
        if (!is_light_on()) {
            return 0;
        } else {
            JsonNode status = create_jsonNode_from_status();
            return status.get("result").get("dimming").asInt();
        }
    }

    /**
     * Returns the current state of the WIZ Light
     * true: ON
     * false: OFF
     *
     * @return current state of WIZ light (boolean)
     */
    public boolean is_light_on() {
        JsonNode status = create_jsonNode_from_status();
        return status.get("result").get("state").asBoolean();
    }

    /**
     * Prints the current status of the WIZ light
     */
    private String get_light_status() {
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
        validate_rgb(r, g, b);

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
        validate_rgb(r, g, b);
        validate_brightness(brightness);

        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"r\":%d,\"g\":%d,\"b\":%d,\"dimming\": %d}}", r, g, b, brightness);
        send_command(command);
    }


    /**
     * Sets light to a custom warm/cool white
     *
     * @param temp Temperature in kelvin (2200-6200)
     */
    public void set_light_kelvin(int temp) {
        validate_temperature(temp);

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
        validate_temperature(temp);
        validate_brightness(brightness);

        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"temp\":%d,\"dimming\":%d}}\n", temp, brightness);
        send_command(command);
    }

    private void validate_rgb(int r, int g, int b) {
        if (LightConstants.MIN_RGB > r || LightConstants.MAX_RGB < r) {
            throw new IllegalArgumentException("Invalid RED value: " + r + " (value should be between 0-255)");
        } else if (LightConstants.MIN_RGB > g || LightConstants.MAX_RGB < g) {
            throw new IllegalArgumentException("Invalid GREEN value: " + g + " (value should be between 0-255)");
        } else if (LightConstants.MIN_RGB > b || LightConstants.MAX_RGB < b) {
            throw new IllegalArgumentException("Invalid BLUE value: " + b + " (value should be between 0-255)");
        }
    }

    private void validate_brightness(int brightness) {
        if (LightConstants.MIN_BRIGHTNESS > brightness || LightConstants.MAX_BRIGHTNESS < brightness) {
            throw new IllegalArgumentException("Invalid BRIGHTNESS value: " + brightness + " (value should be between 10-100)");
        }
    }

    private void validate_temperature(int temp) {
        if (LightConstants.MIN_TEMP > temp || LightConstants.MAX_TEMP < temp) {
            throw new IllegalArgumentException("Invalid TEMP value: " + temp + " (value should be between 2200-6200");
        }
    }

    @Override
    public String toString() {
        return String.format("Light status: %s\nCurrent brightness: %d\n",
                (is_light_on()) ? "ON" : "OFF", get_current_brightness());
    }

    @Override
    public void close() throws Exception {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
