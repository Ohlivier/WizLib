import java.net.InetAddress;
import java.net.UnknownHostException;

public class FindIP {

    public static void find_ip_light() {
        final byte[] ip;
        try {
            ip = InetAddress.getLocalHost().getAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i <= 254; i++) {
            final int j = i;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        ip[3] = (byte) j;
                        InetAddress address = InetAddress.getByAddress(ip);
                        String output = address.toString().substring(1);
                        if (address.isReachable(5)) {
                            System.out.println(output + " is on network");
                        } else {
                            System.out.println("Not reachable: " + output);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    }
            }).start();
        }
    }

    public static void main(String[] args) {
        find_ip_light();
    }
}
