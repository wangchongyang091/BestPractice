package com.riil.util.protocol.agent;

import com.alibaba.fastjson.JSONObject;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.time.FastDateFormat;
import org.bouncycastle.util.encoders.Base64;

/**
 * User: wangchongyang on 2017/6/27 0027.
 */
public class CAgentProxy {

    static {
        try {
            System.loadLibrary("riil_collecter");
            nativeInit("F:\\Materia\\others\\resource\\ruijie\\raceTime\\tools\\ScriptMoni\\scripts");
        } catch (Throwable e) {
            System.out.println("Load dll failure：" + e);
        }
    }

    public static void main(String[] args) {
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                callAgent();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private static void callAgent() {
        System.out.println("before nativeTaskExecute:" + FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()));
        String taskResult = nativeTaskExecute(JSONObject.toJSONString(getAgentConnInfo()), -1);
        System.out.println("after nativeTaskExecute:" + FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()));
        final JSONObject jsonObject = JSONObject.parseObject(taskResult);
        String result = null;
        try {
            result = base64Decode(jsonObject.getString("result"));
        } catch (UnsupportedEncodingException e) {
            System.out.println("error:" + e);
        }
        System.out.println("errorcode:" + jsonObject.getString("errorcode") + "\nresult:" + result);
    }

    private static AgentConnInfo getAgentConnInfo() {
        final AgentConnInfo connInfo = new AgentConnInfo();
        connInfo.setAgentAction("remoterun");
        connInfo.setAgentProtocol("SSH");
        connInfo.setConnectRetry(0);
        connInfo.setConnectTimeOut(0);
        connInfo.setIcmps("");
        connInfo.setIp("172.17.160.189");
        connInfo.setNamespace("");
        connInfo.setParameters("");
        connInfo.setPassword("1qa@WS3ed");
        connInfo.setPort(22);
        connInfo.setProtocol("AGENT");
        connInfo.setReadRetry(0);
        connInfo.setReadTimeOut(0);
        connInfo.setRunpath("/usr/java/jdk1.7.0_79/bin/moni.sh");
        connInfo.setShell("");
        connInfo.setUsername("root");
        connInfo.setWql("");
        return connInfo;
    }

    private static void initialize() {
        try {
            System.loadLibrary("riil_collecter");
            nativeInit("F:\\Materia\\others\\resource\\ruijie\\raceTime\\tools\\ScriptMoni\\scripts");
        } catch (Throwable e) {
            System.out.println("Load dll failure：" + e);
        }
    }

    private static native void nativeInit(String scriptLocation);

    private static native String nativeTaskExecute(String info, int result);

    private static String base64Decode(final String str) throws UnsupportedEncodingException {
        return new String(Base64.decode(str.getBytes()), CharEncoding.UTF_8);
    }

}
