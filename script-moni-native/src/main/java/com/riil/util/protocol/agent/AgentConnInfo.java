package com.riil.util.protocol.agent;

/**
 * User: wangchongyang on 2017/6/27 0027.
 */
public class AgentConnInfo {
    private String agentAction;
    private String agentProtocol;
    private int connectRetry;
    private int connectTimeOut;
    private String icmps;
    private String ip;
    private String namespace;
    private String parameters;
    private String password;
    private int port;
    private String protocol;
    private int readRetry;
    private int readTimeOut;
    private String runpath;
    private String shell;
    private String username;
    private String wql;

    public String getAgentAction() {
        return agentAction;
    }

    public void setAgentAction(final String agentAction) {
        this.agentAction = agentAction;
    }

    public String getAgentProtocol() {
        return agentProtocol;
    }

    public void setAgentProtocol(final String agentProtocol) {
        this.agentProtocol = agentProtocol;
    }

    public int getConnectRetry() {
        return connectRetry;
    }

    public void setConnectRetry(final int connectRetry) {
        this.connectRetry = connectRetry;
    }

    public int getConnectTimeOut() {
        return connectTimeOut;
    }

    public void setConnectTimeOut(final int connectTimeOut) {
        this.connectTimeOut = connectTimeOut;
    }

    public String getIcmps() {
        return icmps;
    }

    public void setIcmps(final String icmps) {
        this.icmps = icmps;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(final String ip) {
        this.ip = ip;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(final String parameters) {
        this.parameters = parameters;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public int getReadRetry() {
        return readRetry;
    }

    public void setReadRetry(final int readRetry) {
        this.readRetry = readRetry;
    }

    public int getReadTimeOut() {
        return readTimeOut;
    }

    public void setReadTimeOut(final int readTimeOut) {
        this.readTimeOut = readTimeOut;
    }

    public String getRunpath() {
        return runpath;
    }

    public void setRunpath(final String runpath) {
        this.runpath = runpath;
    }

    public String getShell() {
        return shell;
    }

    public void setShell(final String shell) {
        this.shell = shell;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getWql() {
        return wql;
    }

    public void setWql(final String wql) {
        this.wql = wql;
    }
}
