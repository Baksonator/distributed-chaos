package app;

public class ServentInfoBootstrap {

    private final String ipAddress;
    private final int listenerPort;

    public ServentInfoBootstrap(String ipAddress, int listenerPort) {
        this.ipAddress = ipAddress;
        this.listenerPort = listenerPort;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getListenerPort() {
        return listenerPort;
    }

}
