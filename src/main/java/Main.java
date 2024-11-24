public class Main {
    public static void main(String[] args) {
        FindLight finder = new FindLight();
        finder.setup();
        try (LightController light = new LightController()) {
            // Set light temperature to 5500 Kelvin
            light.setLightKelvin(5500);
            Thread.sleep(1000);
            // Set light color to R:30 G:50 B:80
            light.setLightColor(30, 50, 80);
            Thread.sleep(1000);
            // Set light brightness to 100
            light.setBrightness(100);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
