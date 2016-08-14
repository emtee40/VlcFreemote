package com.nico.vlcfreemote.net_utils;

public class Server {
    public final String ip;
    public final Integer sshPort;
    public final Integer vlcPort;
    public String password;

    public Server(String ip, Integer vlcPort, Integer sshPort) {
        this.ip = ip;
        this.vlcPort = vlcPort;
        this.sshPort = sshPort;
        this.password = "";
    }

    public void setPassword(final String password) {
        this.password = password;
    }
    public String getPassword() {
        return password;
    }
}
