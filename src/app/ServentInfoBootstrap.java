package app;

import java.util.Objects;

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

    @Override
    public String toString() {
        return "[" + ipAddress + "|" + listenerPort + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServentInfoBootstrap that = (ServentInfoBootstrap) o;
        return listenerPort == that.listenerPort &&
                Objects.equals(ipAddress, that.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, listenerPort);
    }

}
