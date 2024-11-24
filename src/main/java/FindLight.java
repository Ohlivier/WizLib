import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Scanner;

public class FindLight {

    Scanner in = new Scanner(System.in);

    public void setup() {
        String ipLight;
        try {
            ipLight = findIP();
        } catch (InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }

        System.out.println("IP OF LIGHT IS " + ipLight);
    }

    private String findIP() throws InstanceNotFoundException {
        ArrayList<String> ips;
        try {
            ips = checkHosts("192.168.0");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Checking ips");
        for (String ip : ips) {
            System.out.println("Checking " + ip);
            try {
                sendTestCommand(ip);
            } catch (Exception e) {
                continue;
            }
            System.out.println("Did light turn blue, green and then red? (y/n)");
            String answer = in.nextLine();
            if (answer.equalsIgnoreCase("y")) {
                return ip;
            }

        }
        throw new InstanceNotFoundException("No WIZ Light Found");
    }

    public ArrayList<String> checkHosts(String subnet) throws IOException {
        ArrayList<String> toReturn = new ArrayList<>();
        int timeout = 100;
        for (int i = 1; i < 255; i++) {
            String host = subnet + "." + i;
            System.out.println("Host: " + host);
            if (InetAddress.getByName(host).isReachable(timeout)) {
                System.out.println(host + " is reachable");
                toReturn.add(host);
            }
        }
        return toReturn;
    }

    public void sendTestCommand(String ip) {
        LightController light;
        try {
            light = new LightController(ip);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        light.set_light_color(50,50,255);
        light.set_light_color(50,255,50);
        light.set_light_color(255,50,50);

    }
}
