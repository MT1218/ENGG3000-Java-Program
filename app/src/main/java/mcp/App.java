package mcp;

public class App {
  private static final int RECEIVE_PORT_NUMBER = 3032;
  private static final int SEND_PORT_NUMBER = 3031;
  private static final String SEND_IP_ADDR = "10.123.100.181";

  public static void main(String[] args) throws Exception {
    // Create the GUI object first to get reference
    Gui userInterface = new Gui();

    // Create and run the thread to receive messages
    Receive mcpReceiveThread = new Receive(RECEIVE_PORT_NUMBER, userInterface);
    mcpReceiveThread.start();

    // Create the object to send messages
    Send mcpSendObject = new Send(SEND_PORT_NUMBER, SEND_IP_ADDR);

    // Initialise the GUI with the send object
    userInterface.initializeSender(mcpSendObject);
  }
}
