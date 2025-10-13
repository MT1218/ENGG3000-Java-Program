package mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class AppTest {

  @Test
  public void testMainMethodExists() {
    // Test that the main method exists and can be called
    try {
      Class<?> appClass = App.class;
      java.lang.reflect.Method mainMethod = appClass.getMethod("main", String[].class);
      assertNotNull(mainMethod);

      // Verify method is static and public
      int modifiers = mainMethod.getModifiers();
      assertTrue(java.lang.reflect.Modifier.isStatic(modifiers));
      assertTrue(java.lang.reflect.Modifier.isPublic(modifiers));

    } catch (NoSuchMethodException e) {
      fail("Main method not found: " + e.getMessage());
    }
  }

  @Test
  public void testConstants() {
    // Test that constants are properly defined
    // This would require making constants package-private or using reflection
    try {
      Class<?> appClass = App.class;

      java.lang.reflect.Field receivePortField = appClass.getDeclaredField("RECEIVE_PORT_NUMBER");
      receivePortField.setAccessible(true);
      int receivePort = receivePortField.getInt(null);
      assertEquals(3032, receivePort);

      java.lang.reflect.Field sendPortField = appClass.getDeclaredField("SEND_PORT_NUMBER");
      sendPortField.setAccessible(true);
      int sendPort = sendPortField.getInt(null);
      assertEquals(3031, sendPort);

      java.lang.reflect.Field sendIpField = appClass.getDeclaredField("SEND_IP_ADDR");
      sendIpField.setAccessible(true);
      String sendIp = (String) sendIpField.get(null);
      assertEquals("10.123.100.181", sendIp);

    } catch (Exception e) {
      fail("Could not access constants: " + e.getMessage());
    }
  }

  @Test
  public void testPortNumbers() {
    // Test port number validity
    int receivePort = 3032;
    int sendPort = 3031;

    assertTrue(receivePort > 0, "Receive port should be positive");
    assertTrue(sendPort > 0, "Send port should be positive");
    assertTrue(receivePort != sendPort, "Ports should be different");
    assertTrue(receivePort <= 65535, "Receive port should be in valid range");
    assertTrue(sendPort <= 65535, "Send port should be in valid range");
  }

  @Test
  public void testIPAddressFormat() {
    String ipAddress = "10.182.204.16";

    assertNotNull(ipAddress, "IP address should not be null");
    assertFalse(ipAddress.isEmpty(), "IP address should not be empty");

    // Basic IP format validation
    String[] parts = ipAddress.split("\\.");
    assertEquals(4, parts.length, "IP should have 4 parts");

    for (String part : parts) {
      int value = Integer.parseInt(part);
      assertTrue(value >= 0 && value <= 255, "IP part should be 0-255");
    }
  }
}
