package org.jetlang.remote.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class SocketConnector {

    private final String host;
    private final int port;
    private boolean tcpNoDelay = true;
    private int readTimeoutInMs = 3000;
    private int connectTimeoutInMs = 4000;
    private int receiveBufferSize = -1;
    private int sendBufferSize = -1;

    public SocketConnector(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getReadTimeoutInMs() {
        return readTimeoutInMs;
    }

    public void setReadTimeoutInMs(int readTimeoutInMs) {
        this.readTimeoutInMs = readTimeoutInMs;
    }

    public void setConnectTimeoutInMs(int connectTimeoutInMs) {
        this.connectTimeoutInMs = connectTimeoutInMs;
    }

    public int getConnectTimeoutInMs() {
        return connectTimeoutInMs;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    public Socket connect() throws IOException {
        InetSocketAddress endpoint = new InetSocketAddress(host, port);
        Socket socket = configureSocket();
        socket.connect(endpoint, connectTimeoutInMs);
        return socket;
    }

    public Socket configureSocket() throws SocketException {
        Socket socket = new Socket();
        socket.setTcpNoDelay(tcpNoDelay);
        socket.setSoTimeout(readTimeoutInMs);
        if (receiveBufferSize > 0)
            socket.setReceiveBufferSize(receiveBufferSize);
        if (sendBufferSize > 0)
            socket.setSendBufferSize(sendBufferSize);
        return socket;
    }
}
