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

    /**
     * Connects to WIZ light using the IP key in lights.json
     */
    public LightController() {
        this(new File("lights.json"));
    }

    /**
     * Connects to WIZ light using a given JSON config file
     * @param jsonIpFile JSON config file containing the key ip
     */
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

    /**
     * Connects to a WIZ light using a given ip
     * @param lightIP IP of the light of which you wish to connect to
     */
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

    /**
     * Sends a UDP command to the WIZ light
     * @param command Command that should be sent to the WIZ light
     * @return The response of the UDP command
     */
    private String sendCommand(String command) {
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

    /**
     * Creates a JsonNode with help of the getLightStatus() method
     * @return JsonNode of getLightStatus()
     */
    private JsonNode createJsonNodeFromStatus() {
        try {
            return objectMapper.readTree(getLightStatus());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the current brightness of the WIZ light, returns 0 if the light is off
     *
     * @return current brightness of the WIZ light (10-100)
     */
    public int getCurrentBrightness() {
        if (!isLightOn()) {
            return 0;
        } else {
            JsonNode status = createJsonNodeFromStatus();
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
    public boolean isLightOn() {
        JsonNode status = createJsonNodeFromStatus();
        return status.get("result").get("state").asBoolean();
    }

    /**
     * Prints the current status of the WIZ light
     */
    private String getLightStatus() {
        return sendCommand("{\"method\":\"getPilot\",\"params\":{}}\n");
    }

    /**
     * Turns the WIZ light on
     */
    public void turnLightOn() {
        sendCommand("{\"id\":1,\"method\":\"setState\",\"params\":{\"state\":true}}\n");
    }

    /**
     * Turns the WIZ light off
     */
    public void turnLightOff() {
        sendCommand("{\"id\":1,\"method\":\"setState\",\"params\":{\"state\":false}}\n");
    }

    public void setBrightness(int brightness) {
        if (!isLightOn()) {
            System.out.println("Light is not on, cannot currently change the brightness");
            return;
        }

        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"dimming\":%d}}", brightness);
        sendCommand(command);
    }

    /**
     * Sets the WIZ light to a certain RGB value
     *
     * @param r The red RGB value (0-255)
     * @param g The green RGB value (0-255)
     * @param b The blue RGB value (0-255)
     */
    public void setLightColor(int r, int g, int b) {
        validateRgb(r, g, b);

        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"r\":%d,\"g\":%d,\"b\":%d}}", r, g, b);
        sendCommand(command);
    }

    /**
     * Sets the WIZ light to a certain RGB value and brightness
     *
     * @param r          The red RGB value (0-255)
     * @param g          The green RGB value (0-255)
     * @param b          The blue RGB value (0-255)
     * @param brightness The brightness (10-100)
     */
    public void setLightColor(int r, int g, int b, int brightness) {
        validateRgb(r, g, b);
        validateBrightness(brightness);

        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"r\":%d,\"g\":%d,\"b\":%d,\"dimming\": %d}}", r, g, b, brightness);
        sendCommand(command);
    }


    /**
     * Sets light to a custom warm/cool white
     *
     * @param temp Temperature in kelvin (2200-6200)
     */
    public void setLightKelvin(int temp) {
        validateTemperature(temp);

        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"temp\":%d}}\n", temp);
        sendCommand(command);
    }


    /**
     * Sets light to a custom warm/cool white
     *
     * @param temp       Temperature in kelvin (2200-6200)
     * @param brightness The brightness (10-100)
     */
    public void setLightKelvinBrightness(int temp, int brightness) {
        validateTemperature(temp);
        validateBrightness(brightness);

        String command = String.format("{\"id\":1,\"method\":\"setPilot\",\"params\":{\"temp\":%d,\"dimming\":%d}}\n", temp, brightness);
        sendCommand(command);
    }

    /**
     * Validates if the RGB values that are given to the method are valid RGB values
     * @param r value of RED rgb value
     * @param g value of GREEN rgb value
     * @param b value of BLUE rgb value
     */
    private void validateRgb(int r, int g, int b) {
        if (LightConstants.MIN_RGB > r || LightConstants.MAX_RGB < r) {
            throw new IllegalArgumentException("Invalid RED value: " + r + " (value should be between 0-255)");
        } else if (LightConstants.MIN_RGB > g || LightConstants.MAX_RGB < g) {
            throw new IllegalArgumentException("Invalid GREEN value: " + g + " (value should be between 0-255)");
        } else if (LightConstants.MIN_RGB > b || LightConstants.MAX_RGB < b) {
            throw new IllegalArgumentException("Invalid BLUE value: " + b + " (value should be between 0-255)");
        }
    }

    /**
     * Validates if the brightness value is a valid brightness value
     * @param brightness brightness value
     */
    private void validateBrightness(int brightness) {
        if (LightConstants.MIN_BRIGHTNESS > brightness || LightConstants.MAX_BRIGHTNESS < brightness) {
            throw new IllegalArgumentException("Invalid BRIGHTNESS value: " + brightness + " (value should be between 10-100)");
        }
    }

    /**
     * Validates if the temperature value is a valid temperature value
     * @param temp temperature value
     */
    private void validateTemperature(int temp) {
        if (LightConstants.MIN_TEMP > temp || LightConstants.MAX_TEMP < temp) {
            throw new IllegalArgumentException("Invalid TEMP value: " + temp + " (value should be between 2200-6200");
        }
    }

    /**
     * Returns a string of the current status of the WIZ light
     * @return pretty formatted String with status of WIZ light
     */
    @Override
    public String toString() {
        return String.format("Light status: %s\nCurrent brightness: %d\n",
                (isLightOn()) ? "ON" : "OFF", getCurrentBrightness());
    }

    /**
     * Closes Socket once the program closes
     */
    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
