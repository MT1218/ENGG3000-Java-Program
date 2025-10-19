package mcp;

import java.util.Timer;
import java.util.TimerTask;

public class Heartbeat extends Thread {
    private Send heartBeatSendObject;
    private Timer heartBeatTimer;
    private Gui userInterface;

    // Heartbeat interval in milliseconds (2 seconds)
    private static final long HEARTBEAT_INTERVAL = 2000;
    // Initial delay before first heartbeat (1 second)
    private static final long INITIAL_DELAY = 1000;

    Heartbeat(Send heartbeatObject, Gui userInterface) {
        this.heartBeatSendObject = heartbeatObject;
        this.userInterface = userInterface;
        // Make it a daemon thread
        this.heartBeatTimer = new Timer(true);
    }

    @Override
    public void run() {
        // scheduleAtFixedRate for repeating task
        heartBeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    heartBeatSendObject.sendMessage("heartbeat");
                    if (userInterface != null) {
                        userInterface.updateMessageLog(
                                "SENT: heartbeat");
                    }
                } catch (Exception e) {
                    System.err.println("Error sending heartbeat: " + e.getMessage());
                }
            }
        }, INITIAL_DELAY, HEARTBEAT_INTERVAL);
    }

    public void stopHeartbeat() {
        if (heartBeatTimer != null) {
            heartBeatTimer.cancel();
            System.out.println("Heartbeat stopped");
        }
    }
}
