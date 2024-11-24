public class Main {
    public static void main(String[] args) {
        FindLight finder = new FindLight();
        finder.setup();
        try (LightController light = new LightController()) {
            light.set_light_kelvin(5500);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
