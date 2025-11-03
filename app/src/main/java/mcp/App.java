package mcp;

public class App {
  private static final int RECEIVE_PORT_NUMBER = 3032;
  private static final int SEND_PORT_NUMBER = 3031;
  // Put 127.0.0.1 for Wokwi Simulator
  private static final String SEND_IP_ADDR = "127.0.0.1";
  // private static final String SEND_IP_ADDR = "10.123.100.181";

  public static void main(String[] args) throws Exception {
    // Create the GUI object first to get reference
    Gui userInterface = new Gui();

    // Create and run the thread to receive messages
    Receive receiveThread = new Receive(RECEIVE_PORT_NUMBER, userInterface);
    receiveThread.start();

    // Create the object to send GUI messages
    Send guiSendObject = new Send(SEND_PORT_NUMBER, SEND_IP_ADDR, userInterface);
    // Create the object to send heartbeat messages
    Send heartBeatSendObject = new Send(SEND_PORT_NUMBER, SEND_IP_ADDR, null);

    // Initialise the GUI with the send object
    userInterface.initializeSender(guiSendObject);

    // Create and run the threat to send heartbeat messages
    Heartbeat heartBeatThread = new Heartbeat(heartBeatSendObject, userInterface);
    heartBeatThread.start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutting down...");
      heartBeatThread.stopHeartbeat();
      receiveThread.interrupt();
    }));
  }
}
