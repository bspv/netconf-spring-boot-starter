package com.bazzi.netconf.entity;

import java.util.Objects;

public class SshConf {
    private String username; // 远程连接的用户名
    private String password; //远程连接用户的密码
    private String host;  //远程连接服务器地址
    private int port = 830; //远程连接服务器端口

    public SshConf() {
    }

    public SshConf(String username, String password, String host, int port) {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SshConf sshConf = (SshConf) o;
        return port == sshConf.port && Objects.equals(username, sshConf.username) && Objects.equals(password, sshConf.password) && Objects.equals(host, sshConf.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password, host, port);
    }

    @Override
    public String toString() {
        return "SshConf{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
