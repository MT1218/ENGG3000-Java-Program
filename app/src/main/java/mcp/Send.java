package mcp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Send {
  private DatagramSocket espSendSocket;
  private int espSendPortNumber;
  InetAddress espSendIpAddr;
  private Gui userInterface;
  private boolean sendNotifications = false;

  // Constructor to set destination port/ipaddr variables and initialise
  // espSendSocket
  Send(int espSendPortNumber, String espSendIpAddr, Gui userInterface) {
    if (userInterface != null) {
      this.userInterface = userInterface;
      sendNotifications = true;
    }
    this.espSendPortNumber = espSendPortNumber;
    try {
      this.espSendIpAddr = InetAddress.getByName(espSendIpAddr);
      espSendSocket = new DatagramSocket();
    } catch (UnknownHostException e) {
      System.out.println("Ran into an UnknownHostException: " + e);
    } catch (SocketException e) {
      System.out.println("Ran into an SocketException: " + e);
    }
  }

  public void sendMessage(String message) {
    try {
      // Create message based on the string and send it to the destination as per the
      // global variables
      byte[] sendBuffer = message.getBytes();

      DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, espSendIpAddr, espSendPortNumber);
      espSendSocket.send(sendPacket);

      if (sendNotifications) {
        userInterface.showNotification("Sent message to esp: " + message);
      }

      System.out.println("Sent message to esp: " + message);
    } catch (IOException e) {
      System.out.println("Ran into an IOException: " + e);
    }
  }
}
