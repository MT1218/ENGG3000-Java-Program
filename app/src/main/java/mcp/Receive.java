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
    try {
      espReceiveSocket = new DatagramSocket(espReceivePortNumber);
      System.out.println("Receive socket initialized on port " + espReceivePortNumber);
    } catch (SocketException e) {
      System.out.println("CRITICAL ERROR - Failed to create receive socket: " + e);
      System.out.println("Port " + espReceivePortNumber + " is likely already in use.");

      espReceiveSocket = null;

      if (userInterface != null) {
        userInterface.updateMessageLog(
            "ERROR: Failed to bind to port " + espReceivePortNumber + " - " + e.getMessage());
      }
    }
    receiveBuffer = new byte[1024];
  }

  @Override
  public void run() {
    System.out.println("Receive thread started - listening for ESP32 messages...");

    if (espReceiveSocket == null) {
      System.out.println("Cannot start receive thread - socket creation failed");
      if (userInterface != null) {
        userInterface.updateMessageLog("ERROR: Cannot receive messages - socket failed to initialize");
      }
      return;
    }

    while (true) {
      try {
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        espReceiveSocket.receive(receivePacket);

        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
        System.out.println("Received from ESP32: " + receivedMessage);

        // Handle STATUS messages (with or without space after colon)
        if (receivedMessage.startsWith("STATUS:") || receivedMessage.startsWith("STATUS :")) {
          parseStatusMessage(receivedMessage);
          userInterface.updateMessageLog("RECEIVED: " + receivedMessage);
        }
        // Handle WEIGHT_CHECK messages
        else if (receivedMessage.startsWith("WEIGHT_CHECK:") || receivedMessage.startsWith("WEIGHT_CHECK :")) {
          int colonIndex = receivedMessage.indexOf(":");
          if (colonIndex != -1 && colonIndex + 1 < receivedMessage.length()) {
            String weight = receivedMessage.substring(colonIndex + 1).trim();
            userInterface.updateWeightReading(weight);
            userInterface.updateMessageLog("RECEIVED: WEIGHT_CHECK: " + weight);
          }
        }
        // Handle MODE_CHANGE messages
        else if (receivedMessage.startsWith("MODE_CHANGE:") || receivedMessage.startsWith("MODE_CHANGE :")) {
          int colonIndex = receivedMessage.indexOf(":");
          if (colonIndex != -1 && colonIndex + 1 < receivedMessage.length()) {
            String message = receivedMessage.substring(colonIndex + 1).trim();
            userInterface.updateMessageLog("RECEIVED: MODE_CHANGE: " + message);
          }
        }
        // Handle INFO messages
        else if (receivedMessage.startsWith("INFO:") || receivedMessage.startsWith("INFO :")) {
          int colonIndex = receivedMessage.indexOf(":");
          if (colonIndex != -1 && colonIndex + 1 < receivedMessage.length()) {
            String message = receivedMessage.substring(colonIndex + 1).trim();
            userInterface.updateMessageLog("RECEIVED: INFO: " + message);
          }
        }
        // Handle WARNING messages
        else if (receivedMessage.startsWith("WARNING:") || receivedMessage.startsWith("WARNING :")) {
          int colonIndex = receivedMessage.indexOf(":");
          if (colonIndex != -1 && colonIndex + 1 < receivedMessage.length()) {
            String message = receivedMessage.substring(colonIndex + 1).trim();
            userInterface.updateMessageLog("RECEIVED: WARNING: " + message);
          }
        }
        // Handle ERROR messages
        else if (receivedMessage.startsWith("ERROR:") || receivedMessage.startsWith("ERROR :")) {
          int colonIndex = receivedMessage.indexOf(":");
          if (colonIndex != -1 && colonIndex + 1 < receivedMessage.length()) {
            String message = receivedMessage.substring(colonIndex + 1).trim();
            userInterface.updateMessageLog("RECEIVED: ERROR: " + message);
          }
        }
        // Handle COMMAND_EXECUTION messages
        else if (receivedMessage.startsWith("COMMAND_EXECUTION:")
            || receivedMessage.startsWith("COMMAND_EXECUTION :")) {
          int colonIndex = receivedMessage.indexOf(":");
          if (colonIndex != -1 && colonIndex + 1 < receivedMessage.length()) {
            String message = receivedMessage.substring(colonIndex + 1).trim();
            userInterface.updateMessageLog("RECEIVED: EXECUTED: " + message);
          }
        }
        // Handle SYSTEM_UPDATE messages
        else if (receivedMessage.startsWith("SYSTEM_UPDATE:") || receivedMessage.startsWith("SYSTEM_UPDATE :")) {
          int colonIndex = receivedMessage.indexOf(":");
          if (colonIndex != -1 && colonIndex + 1 < receivedMessage.length()) {
            String message = receivedMessage.substring(colonIndex + 1).trim();
            userInterface.updateMessageLog("RECEIVED: SYSTEM: " + message);
          }
        }
        // Handle other messages
        else {
          userInterface.updateMessageLog("RECEIVED: " + receivedMessage);
        }
      } catch (IOException e) {
        System.out.println("IOException in receive: " + e.getMessage());
        if (userInterface != null) {
          userInterface.updateMessageLog("Network error: " + e.getMessage());
        }
      } catch (Exception e) {
        System.out.println("Unexpected error in receive: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  private void parseStatusMessage(String statusMessage) {
    try {
      System.out.println("DEBUG: Parsing status message: " + statusMessage);

      // Find where the actual data starts (after "STATUS:" or "STATUS :")
      int dataStart = statusMessage.indexOf("MODE:");
      if (dataStart == -1) {
        System.out.println("ERROR: Could not find MODE: in status message");
        return;
      }

      // Extract the data portion
      String data = statusMessage.substring(dataStart);
      System.out.println("DEBUG: Data portion: " + data);

      String[] parts = data.split("\\|");
      System.out.println("DEBUG: Split into " + parts.length + " parts");

      String mode = "UNKNOWN";
      String bridgeState = "UNKNOWN";
      String gateState = "UNKNOWN";
      String roadDistance = "0";
      String boatDistance = "0";
      String bridgeMovementDistance = "0";
      String boatClearanceDistance = "0";
      String roadLight = "UNKNOWN";
      String boatLight = "UNKNOWN";
      String bridgeLight = "OFF";
      String manualBridgeLights = "NO";
      String sequenceState = "UNKNOWN";
      String movementState = "UNKNOWN";
      String queueSize = "";
      String executing = "";

      for (String part : parts) {
        part = part.trim();
        if (part.isEmpty())
          continue;

        int colonIndex = part.indexOf(":");
        if (colonIndex == -1)
          continue;

        String key = part.substring(0, colonIndex).trim();
        String value = part.substring(colonIndex + 1).trim();

        System.out.println("DEBUG: Key=" + key + ", Value=" + value);

        switch (key) {
          case "MODE":
            mode = value;
            break;
          case "BRIDGE":
            bridgeState = value;
            break;
          case "GATE":
            gateState = value;
            break;
          case "ROAD_DISTANCE":
            roadDistance = value;
            break;
          case "BOAT_DISTANCE":
            boatDistance = value;
            break;
          case "BRIDGE_MOVEMENT_DISTANCE":
            bridgeMovementDistance = value;
            break;
          case "BOAT_CLEARANCE_DISTANCE":
            boatClearanceDistance = value;
            break;
          case "ROAD_LIGHT":
            roadLight = value;
            break;
          case "BOAT_LIGHT":
            boatLight = value;
            break;
          case "BRIDGE_LIGHT":
            bridgeLight = value;
            break;
          case "MANUAL_BRIDGE_LIGHTS":
            manualBridgeLights = value;
            break;
          case "SEQUENCE":
            sequenceState = value;
            break;
          case "MOVEMENT_STATE":
            movementState = value;
            break;
          case "QUEUE":
            queueSize = value;
            break;
          case "EXECUTING":
            executing = value;
            break;
        }
      }

      System.out.println("DEBUG: Parsed values - Mode:" + mode + " Bridge:" + bridgeState +
          " Gate:" + gateState + " RoadLight:" + roadLight + " BoatLight:" + boatLight +
          " Sequence:" + sequenceState + " Movement:" + movementState);

      // Update GUI with parsed status
      userInterface.updateSystemStatus(
          mode, bridgeState, gateState, roadDistance, boatDistance,
          bridgeMovementDistance, boatClearanceDistance, roadLight, boatLight,
          bridgeLight, manualBridgeLights, sequenceState, movementState, queueSize, executing);

      // Don't log full status message to reduce clutter - it's shown in the stats
      // panel

    } catch (Exception e) {
      System.out.println("ERROR parsing status message: " + e.getMessage());
      e.printStackTrace();
      userInterface.updateMessageLog("Error parsing status: " + statusMessage);
    }
  }
}
