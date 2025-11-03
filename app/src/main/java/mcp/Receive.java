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
        // Handle EMERGENCY_STOP messages
        else if (receivedMessage.startsWith("EMERGENCY_STOP:") || receivedMessage.startsWith("EMERGENCY_STOP :")) {
          handleEmergencyStop(receivedMessage);
        }
        // Handle MODE_CHANGE messages
        else if (receivedMessage.startsWith("MODE_CHANGE:") || receivedMessage.startsWith("MODE_CHANGE :")) {
          handleModeChange(receivedMessage);
        }
        // Handle INFO messages
        else if (receivedMessage.startsWith("INFO:") || receivedMessage.startsWith("INFO :")) {
          handleInfoMessage(receivedMessage);
        }
        // Handle WARNING messages
        else if (receivedMessage.startsWith("WARNING:") || receivedMessage.startsWith("WARNING :")) {
          handleWarningMessage(receivedMessage);
        }
        // Handle ERROR messages
        else if (receivedMessage.startsWith("ERROR:") || receivedMessage.startsWith("ERROR :")) {
          handleErrorMessage(receivedMessage);
        }
        // Handle COMMAND_EXECUTION messages
        else if (receivedMessage.startsWith("COMMAND_EXECUTION:")
            || receivedMessage.startsWith("COMMAND_EXECUTION :")) {
          handleCommandExecution(receivedMessage);
        }
        // Handle SYSTEM_UPDATE messages
        else if (receivedMessage.startsWith("SYSTEM_UPDATE:") || receivedMessage.startsWith("SYSTEM_UPDATE :")) {
          handleSystemUpdate(receivedMessage);
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

  private void handleEmergencyStop(String message) {
    int colonIndex = message.indexOf(":");
    if (colonIndex != -1 && colonIndex + 1 < message.length()) {
      String content = message.substring(colonIndex + 1).trim();

      if (content.contains("activated")) {
        String notificationText = "EMERGENCY STOP ACTIVATED";
        userInterface.showNotification(notificationText);
        userInterface.updateMessageLog(
            "EMERGENCY STOP: System emergency stop has been activated - Mode: Override, State: Diagnostic");
      }
    }
  }

  private void handleModeChange(String message) {
    int colonIndex = message.indexOf(":");
    if (colonIndex != -1 && colonIndex + 1 < message.length()) {
      String content = message.substring(colonIndex + 1).trim();

      if (content.equals("override_mode_active")) {
        userInterface.showNotification("Mode changed to OVERRIDE");
        userInterface.updateMessageLog("MODE_CHANGE: Successfully switched to override mode");
      } else if (content.equals("automatic_mode_active")) {
        userInterface.showNotification("Mode changed to AUTOMATIC");
        userInterface.updateMessageLog("MODE_CHANGE: Successfully switched to automatic mode");
      } else if (content.equals("mode_change_completed")) {
        userInterface.showNotification("Mode change successful");
        userInterface.updateMessageLog("MODE_CHANGE: Mode change completed successfully");
      } else {
        userInterface.updateMessageLog("RECEIVED: MODE_CHANGE: " + content);
      }
    }
  }

  private void handleInfoMessage(String message) {
    int colonIndex = message.indexOf(":");
    if (colonIndex != -1 && colonIndex + 1 < message.length()) {
      String content = message.substring(colonIndex + 1).trim();

      if (content.contains("mode_change_queued")) {
        userInterface.showNotification("Mode change queued - waiting for safe state");
        userInterface.updateMessageLog("INFO: Mode change request queued, waiting for bridge to reach safe state");
      } else if (content.contains("full_test_starting")) {
        userInterface.showNotification("Full system test starting");
        userInterface.updateMessageLog("INFO: Full system test sequence initiated - please standby");
      } else if (content.contains("test_phase")) {
        // Parse test phase info
        String phase = extractValue(content, "PHASE:");
        String action = extractValue(content, "ACTION:");
        userInterface.showNotification("Test Phase " + phase + " - " + action);
        userInterface.updateMessageLog("INFO: Performing Test Phase " + phase + " - Action: " + action);
      } else {
        userInterface.updateMessageLog("RECEIVED: INFO: " + content);
      }
    }
  }

  private void handleWarningMessage(String message) {
    int colonIndex = message.indexOf(":");
    if (colonIndex != -1 && colonIndex + 1 < message.length()) {
      String content = message.substring(colonIndex + 1).trim();

      if (content.contains("command_queue_full")) {
        String queueSize = extractValue(content, "SIZE:");
        userInterface.showNotification("Command queue full (" + queueSize + ") - please wait");
        userInterface.updateMessageLog(
            "WARNING: Command queue is full (Size: " + queueSize + ") - wait for operations to complete");
      } else {
        userInterface.showNotification(content);
        userInterface.updateMessageLog("RECEIVED: WARNING: " + content);
      }
    }
  }

  private void handleErrorMessage(String message) {
    int colonIndex = message.indexOf(":");
    if (colonIndex != -1 && colonIndex + 1 < message.length()) {
      String content = message.substring(colonIndex + 1).trim();

      if (content.contains("override_denied_traffic_present")) {
        userInterface.showNotification("Cannot enter override - traffic present");
        userInterface.updateMessageLog("ERROR: Override mode denied - Clear all traffic before switching modes");
      } else if (content.contains("mode_change_timeout")) {
        userInterface.showNotification("Mode change timeout");
        userInterface.updateMessageLog("ERROR: Mode change request timed out - please try again");
      } else if (content.contains("bridge_opening_failed")) {
        userInterface.showNotification("Bridge failed to open");
        userInterface.updateMessageLog("ERROR: Bridge opening operation failed - unknown error occurred");
      } else if (content.contains("bridge_closing_failed")) {
        userInterface.showNotification("Bridge failed to close");
        userInterface.updateMessageLog("ERROR: Bridge closing operation failed - unknown error occurred");
      } else if (content.contains("bridge_unknown_state")) {
        userInterface.showNotification("Bridge in unknown state");
        userInterface.updateMessageLog("ERROR: Bridge is now in an unknown state - diagnostics required");
      } else if (content.contains("test_failed")) {
        String phase = extractValue(content, "PHASE:");
        String reason = extractValue(content, "REASON:");
        userInterface.showNotification("Test Phase " + phase + " failed - " + reason);
        userInterface.updateMessageLog("ERROR: Test Phase " + phase + " failed - Reason: " + reason);
      } else {
        userInterface.showNotification(content);
        userInterface.updateMessageLog("RECEIVED: ERROR: " + content);
      }
    }
  }

  private void handleCommandExecution(String message) {
    int colonIndex = message.indexOf(":");
    if (colonIndex != -1 && colonIndex + 1 < message.length()) {
      String content = message.substring(colonIndex + 1).trim();

      if (content.equals("allow_boat_traffic")) {
        userInterface.showNotification("Boat traffic sequence initiated");
        userInterface.updateMessageLog("COMMAND_EXECUTION: allow_boat_traffic command executed successfully");
      } else if (content.equals("allow_road_traffic")) {
        userInterface.showNotification("Road traffic sequence initiated");
        userInterface.updateMessageLog("COMMAND_EXECUTION: allow_road_traffic command executed successfully");
      } else if (content.equals("run_full_test_success")) {
        userInterface.showNotification("All tests completed successfully");
        userInterface.updateMessageLog("COMMAND_EXECUTION: Full system test completed - all phases passed");
      } else {
        userInterface.updateMessageLog("RECEIVED: EXECUTED: " + content);
      }
    }
  }

  private void handleSystemUpdate(String message) {
    int colonIndex = message.indexOf(":");
    if (colonIndex != -1 && colonIndex + 1 < message.length()) {
      String content = message.substring(colonIndex + 1).trim();

      if (content.contains("bridge_is_executing_sequence")) {
        userInterface.showNotification("Bridge executing sequence - please wait");
        userInterface.updateMessageLog("SYSTEM_UPDATE: Bridge is currently executing a sequence - wait for completion");
      } else if (content.equals("restarting")) {
        userInterface.showNotification("Bridge system restarting");
        userInterface.updateMessageLog("SYSTEM_UPDATE: ESP32 system restart initiated");
      } else if (content.equals("restart_required")) {
        userInterface.showNotification("Restart required");
        userInterface
            .updateMessageLog("SYSTEM_UPDATE: System restart required - no operations will execute until restart");
      } else if (content.contains("diagnostics_command_only")) {
        userInterface.showNotification("Diagnostics required - bridge state unknown");
        userInterface.updateMessageLog(
            "SYSTEM_UPDATE: Please run diagnostics - bridge state unknown, no other commands accepted");
      } else if (content.contains("diagnostic_mode")) {
        userInterface.showNotification("Diagnostic mode active");
        userInterface.updateMessageLog("SYSTEM_UPDATE: System entered diagnostic mode - determining bridge state");
      } else if (content.equals("recovered")) {
        userInterface.showNotification("Bridge state recovered");
        userInterface
            .updateMessageLog("SYSTEM_UPDATE: Bridge state successfully determined - normal operations resumed");
      } else if (content.contains("bridge_state_mismatch")) {
        userInterface.showNotification("Bridge state mismatch detected");
        userInterface
            .updateMessageLog("SYSTEM_UPDATE: Bridge state does not match expected state - verification in progress");
      } else if (content.contains("detected_boats_while_closing")) {
        userInterface.showNotification("Boats detected - reopening bridge");
        userInterface
            .updateMessageLog("SYSTEM_UPDATE: Boats detected during bridge closing - returning to BOATS_PASSING state");
      } else if (content.contains("bridge_overloaded")) {
        userInterface.showNotification("Bridge overloaded - skipping open");
        userInterface.updateMessageLog(
            "SYSTEM_UPDATE: Excessive weight detected on bridge - skipping open operation for safety");
      } else {
        userInterface.updateMessageLog("RECEIVED: SYSTEM: " + content);
      }
    }
  }

  private String extractValue(String content, String key) {
    int keyIndex = content.indexOf(key);
    if (keyIndex == -1)
      return "";

    int startIndex = keyIndex + key.length();
    int endIndex = content.indexOf("|", startIndex);

    if (endIndex == -1) {
      return content.substring(startIndex).trim();
    } else {
      return content.substring(startIndex, endIndex).trim();
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
