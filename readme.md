# WizLib
Simple Java library to control WIZ lights on a local network

## How-To

Use FindLight.java to find the IP address of the WIZ light on your local network

FindLight.java will then write this IP address to lights.json

After this LightController can be used to send commands to your local WIZ light

## Example

```java
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
            // Set light brightness to 10
            light.setBrightness(100);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

```


Credits: https://seanmcnally.net/wiz-config.html
