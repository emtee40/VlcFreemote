package com.nicolasbrailo.vlcfreemote.model;

public class Server {
    public final String ip;
    public final Integer sshPort;
    public final Integer vlcPort;
    private String password;
    private String lastPath;

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

    public void setLastPath(String lastPath) { this.lastPath = lastPath; }
    public String getLastPath() { return this.lastPath; }
}
