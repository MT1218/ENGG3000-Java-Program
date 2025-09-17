package mcp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Receive extends Thread {
  private DatagramSocket espReceiveSocket;
  private byte[] receiveBuffer;
  private Gui userInterface;

  Receive(int espReceivePortNumber, Gui userInterface) {
    this.userInterface = userInterface;
    // Initialise the DatagramSocket to receive messages based on the given port
    // number
    try {
      espReceiveSocket = new DatagramSocket(espReceivePortNumber);
      System.out.println("Receive socket initialized on port " + espReceivePortNumber);
    } catch (SocketException e) {
      System.out.println("CRITICAL ERROR - Failed to create receive socket: " + e);
      System.out.println("Port " + espReceivePortNumber + " is likely already in use.");
      System.out.println("Try closing other instances of this program or use different ports.");

      // Set socket to null to prevent NullPointerException
      espReceiveSocket = null;

      if (userInterface != null) {
        userInterface.updateMessageLog(
            "ERROR: Failed to bind to port " + espReceivePortNumber + " - " + e.getMessage());
      }
    }
    // Initialise the buffer to receive messages
    receiveBuffer = new byte[1024];
  }

  @Override
  public void run() {
    System.out.println("Receive thread started - listening for ESP32 messages...");

    // Check if socket was created successfully
    if (espReceiveSocket == null) {
      System.out.println("Cannot start receive thread - socket creation failed");
      if (userInterface != null) {
        userInterface.updateMessageLog(
            "ERROR: Cannot receive messages - socket failed to initialize");
      }
      return; // Exit the thread
    }

    // Run until stopped
    while (true) {
      // Listen and wait for a message
      try {
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        espReceiveSocket.receive(receivePacket);

        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("Received from ESP32: " + receivedMessage);

        // Parse and update GUI with received status
        if (receivedMessage.startsWith("STATUS|")) {
          parseStatusMessage(receivedMessage);
          // Also log status updates to message log with key information
          String bridgeState = extractValue(receivedMessage, "BRIDGE");
          String gateState = extractValue(receivedMessage, "GATE");
          String roadLight = extractValue(receivedMessage, "ROAD_LIGHT");
          String boatLight = extractValue(receivedMessage, "BOAT_LIGHT");
          String sequence = extractValue(receivedMessage, "SEQUENCE");

          userInterface.updateMessageLog(
              "STATUS: Bridge="
                  + bridgeState
                  + ", Gate="
                  + gateState
                  + ", Road="
                  + roadLight
                  + ", Boat="
                  + boatLight
                  + ", Seq="
                  + sequence);
        } else {
          // Handle other messages (command confirmations, etc.)
          userInterface.updateMessageLog("ESP32: " + receivedMessage);
        }
      } catch (IOException e) {
        System.out.println("Ran into an IOException: " + e);
        if (userInterface != null) {
          userInterface.updateMessageLog("Network error: " + e.getMessage());
        }
      }
    }
  }

  private void parseStatusMessage(String statusMessage) {
    try {
      // Parse status message format:
      // STATUS|MODE:AUTOMATIC|BRIDGE:CLOSED|GATE:OPEN|ROAD_DISTANCE:0
      // |BOAT_DISTANCE:0|ROAD_LIGHT:RED|BOAT_LIGHT:RED|SEQUENCE:IDLE
      String[] parts = statusMessage.split("\\|");

      String mode = "UNKNOWN";
      String bridgeState = "UNKNOWN";
      String gateState = "UNKNOWN";
      String roadDistance = "0";
      String boatDistance = "0";
      String roadLight = "UNKNOWN";
      String boatLight = "UNKNOWN";

      for (String part : parts) {
        if (part.startsWith("MODE:")) {
          mode = part.substring(5);
        } else if (part.startsWith("BRIDGE:")) {
          bridgeState = part.substring(7);
        } else if (part.startsWith("GATE:")) {
          gateState = part.substring(5);
        } else if (part.startsWith("ROAD_DISTANCE:")) {
          roadDistance = part.substring(14);
        } else if (part.startsWith("BOAT_DISTANCE:")) {
          boatDistance = part.substring(14);
        } else if (part.startsWith("ROAD_LIGHT:")) {
          roadLight = part.substring(11);
        } else if (part.startsWith("BOAT_LIGHT:")) {
          boatLight = part.substring(11);
        }
      }

      // Update GUI with parsed status
      userInterface.updateSystemStatus(
          mode, bridgeState, gateState, roadDistance, boatDistance, roadLight, boatLight);

    } catch (Exception e) {
      System.out.println("Error parsing status message: " + e.getMessage());
      userInterface.updateMessageLog("Error parsing status: " + statusMessage);
    }
  }

  // Helper method to extract values from status message
  private String extractValue(String message, String key) {
    try {
      String[] parts = message.split("\\|");
      for (String part : parts) {
        if (part.startsWith(key + ":")) {
          return part.substring(key.length() + 1);
        }
      }
    } catch (Exception e) {
      System.out.println("Error extracting value for key: " + key + " - " + e.getMessage());
    }
    return "UNKNOWN";
  }
}
