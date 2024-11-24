import com.fasterxml.jackson.databind.ObjectMapper;

import javax.management.InstanceNotFoundException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FindLight {
    private static final Logger LOGGER = Logger.getLogger(FindLight.class.getName());
    private static final String[] DEFAULT_SUBNETS = {"192.168.1", "192.168.0"};
    private static final int LOOKUP_TIMEOUT = 500;
    private static final int DELAY_LIGHT_COLOR_SWITCH = 1000;


    Scanner in;
    private final ExecutorService executorService;


    /**
     * Created a threadpool with N number of threads.
     * N is the amount of cores in the CPU of the computer where this program is ran on
     */
    public FindLight() {
        this.in = new Scanner(System.in);
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
    }

    /**
     * Calls the right methods for finding and setting the IP of the local WIZ light
     */
    public void setup() {
        String ipLight;
        try {
            ipLight = findIP();
        } catch (InstanceNotFoundException e) {
            LOGGER.log(Level.SEVERE, "NO WIZ LIGHT FOUND", e);
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }

        LOGGER.info("Found ip of WIZ light: " + ipLight);
        setJson(ipLight);
    }

    /**
     * Finds the IP of the WIZ light that's located on the local network
     * This currently only finds the IP of one single WIZ light
     *
     * @return The IP of the WIZ light on the Local Network
     * @throws InstanceNotFoundException throws whenever there couldn't be a WIZ light found on the local network
     */
    private String findIP() throws InstanceNotFoundException {
        ArrayList<String> reachableHosts;
        try {
            reachableHosts = checkHosts();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to check hosts", e);
            throw new RuntimeException(e);
        }
        LOGGER.info("Checking " + reachableHosts.size() + " hosts");

        for (String ip : reachableHosts) {
            try {
                if (testLight(ip)) {
                    return ip;
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to test device at " + ip);
            }
        }
        throw new InstanceNotFoundException("No WIZ Light Found");
    }

    /**
     * Method to test a light, it first makes the WIZ light show a certain sequence of colors.
     * Then it will ask the user for confirmation on this color sequence
     *
     * @param ip IP of the light to check
     * @return True if the IP that was checked is the right ip
     */
    public boolean testLight(String ip) {
        LOGGER.info("Testing light " + ip);

        try (LightController light = new LightController(ip)) {
            testLightColorSequence(light);
            return getUserConfirmation();
        } catch (Exception e) {
            LOGGER.warning("Failed to test light at " + ip + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Uses multithreading to check all the IP's in DEFAULT_SUBNET for reachable devices.
     *
     * @return An ArrayList consisting of IP's of reachable devices
     */
    public ArrayList<String> checkHosts() throws IOException {
        List<Future<String>> futures = new ArrayList<>();
        ArrayList<String> reachable = new ArrayList<>();


        for (String subnet : DEFAULT_SUBNETS) {
            LOGGER.info("Checking subnet " + subnet);
            for (int i = 1; i < 255; i++) {
                String host = subnet + "." + i;
                futures.add(executorService.submit(() -> checkHost(host)));
            }
        }

        for (Future<String> future : futures) {
            try {
                String result = future.get(LOOKUP_TIMEOUT * 2, TimeUnit.MILLISECONDS);
                if (result != null) {
                    reachable.add(result);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.warning("Failed to check host: " + e.getMessage());
            }
        }
        LOGGER.info("Found " + reachable.size() + " potential devices");
        return reachable;
    }

    /**
     * Checks if a host is reachable
     *
     * @param host IP of the host to check
     * @return IP of host if it's reachable else it will return null
     */
    private String checkHost(String host) {
        try {
            LOGGER.info("Checking host: " + host);
            if (InetAddress.getByName(host).isReachable(LOOKUP_TIMEOUT)) {
                System.out.println(host + " is reachable");
                return host;
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to check host " + host + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Asks the user for confirmation on whether their lights showed the right colors
     *
     * @return true or false depending on user input
     */
    public boolean getUserConfirmation() {
        System.out.println("Did the light turn BLUE then GREEN then RED? (y/n)");
        String answer = in.nextLine();
        return answer.equalsIgnoreCase("y");
    }

    /**
     * Tests the LIGHT using a color sequence (RED, GREEN, BLUE)
     * this is to check if the current ip is of the right light
     *
     * @param light Light to check
     */
    public void testLightColorSequence(LightController light) {
        try {
            light.setLightColor(0, 0, 255);  // Blue
            Thread.sleep(DELAY_LIGHT_COLOR_SWITCH);
            light.setLightColor(0, 255, 0);  // Green
            Thread.sleep(DELAY_LIGHT_COLOR_SWITCH);
            light.setLightColor(255, 0, 0);  // Red
            Thread.sleep(DELAY_LIGHT_COLOR_SWITCH);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Sets the IP value in lights.json to the ip of the light
     *
     * @param ip The ip of the WIZ light
     */
    public void setJson(String ip) {
        ObjectMapper objectMapper = new ObjectMapper();
        WizLight wizLight = new WizLight(ip);
        try {
            objectMapper.writeValue(new File("lights.json"), wizLight);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
