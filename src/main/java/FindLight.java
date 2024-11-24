import javax.management.InstanceNotFoundException;
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

    Scanner in;
    private final ExecutorService executorService;


    public FindLight() {
        this.in = new Scanner(System.in);
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
    }

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
    }

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
                continue;
            }
        }
        throw new InstanceNotFoundException("No WIZ Light Found");
    }

    public boolean testLight(String ip) {
        LOGGER.info("Testing light " + ip);

        try (LightController light = new LightController(ip)) {
            testLight(light);
            return getUserConfirmation();
        } catch (Exception e) {
            LOGGER.warning("Failed to test light at " + ip + ": " + e.getMessage());
            return false;
        }
    }

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

    public boolean getUserConfirmation() {
        System.out.println("Did the light turn BLUE then GREEN then RED? (y/n)");
        String answer = in.nextLine();
        return answer.equalsIgnoreCase("y");
    }

    public void testLight(LightController light) {
        int DELAY = 1000;
        try {
            light.set_light_color(0, 0, 255);  // Blue
            Thread.sleep(DELAY);
            light.set_light_color(0, 255, 0);  // Green
            Thread.sleep(DELAY);
            light.set_light_color(255, 0, 0);  // Red
            Thread.sleep(DELAY);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
