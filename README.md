# ENGG3000-Java-Program

 Java Swing application as a part of the ENGG3000 unit.
 Read more [about this project](http://mt1218.github.io/p/engg3000-vertical-lift-bridge/).
 Used for remote monitoring and control of the ESP32 bridge controller program.

## Summary

- **Monitoring**: Bridge/gate positions, sensor readings, traffic lights, sequence states.
- **Manual Override**: Control of bridge operations.
- **Multi-Threaded Communication**: Separate threads for sending, receiving, and heartbeat.
- **GUI**: Colored indicators, animated bridge, notifications.
- **Message Logging**: Timestamped event log.

## GUI in Action

![GUI Overview](gui.png)

## Requirements

- **Java 11+**
- **Gradle 7.0+**
- **Network access**

## Build and Run

### Using Gradle

```bash
# Build the project
./gradlew build

# Run the application
./gradlew run

# Create executable JAR
./gradlew jar
# Then run: java -jar build/libs/bridge-gui.jar
```

### From IDE

1. Open project in IDE
2. Import as Gradle project
3. Run `App.java` main method

## Usage

### Connecting to ESP32

1. Start the Java program.
2. Enter ESP32 IP address (e.g., `192.168.1.100`).
3. Enter ESP32 port (default: `3031`).
4. Run program.

**Note**: If connection fails, check:

- ESP32 is powered and connected to WiFi.
- IP address is correct (check ESP32 serial monitor for IP).
- Windows Defender Firewall allows UDP on port 3032.
- Both devices are on the same network.

### Automatic Mode

- System operates autonomously based on sensor input.
- All override controls are disabled (greyed out).
- View sensor readings, sequence progress, and light states.
- Can see the bridge visually/animated.

### Override Mode

1. Click **Override Mode** button
2. Confirm the mode switch dialog
3. Wait for ESP32 to enter safe state (if mid-operation)
4. All manual controls become enabled

## Communication Protocol

### Network Configuration

```java
// Java listening port
private static final int RECEIVE_PORT_NUMBER = 3032;
// ESP32 listening port
private static final int SEND_PORT_NUMBER = 3031;
// ESP32 IP address
private static final String SEND_IP_ADDR = "10.237.91.181";
```

### Folder Structure

```Tree
mcp/
├── App.java # Main entry point
├── Gui.java # Main GUI window with all controls
├── Send.java # UDP command sender
├── Receive.java # UDP message receiver (Runnable)
└── Heartbeat.java# ESP32 connection maintain (Runnable)
```

## Related Repository

[ESP32 Bridge Control Program](https://github.com/MT1218/ENGG3000-ESP32)
