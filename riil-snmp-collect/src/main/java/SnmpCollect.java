import com.google.common.collect.Maps;
import com.riil.util.protocol.snmp.SnmpCollectorFactory;
import com.riil.util.protocol.snmp.SnmpConnInfo;
import com.riil.util.protocol.snmp.SnmpDefaultCollector;
import com.riil.util.protocol.snmp.SnmpTable;
import java.io.FileReader;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.time.FastDateFormat;

/**
 * User: wangchongyang on 2017/8/20 0020.
 */
public class SnmpCollect {
    private static boolean isLoop;
    private static long interval;
    private static String oids;
    private static String isAddIndex;
    private static String ip;
    private static String readOnlyCommunity;
    private static String version;
    private static String collectType;
    private static String port;
    private static String timeout;
    private static String authPrivatePassword;
    private static String authPrivateProtocol;
    private static String password;
    private static String authProtocol;
    private static String authPassword;
    private static String securityName;
    private static String securityLevel;
    private static String retry;
    private static String timeout4PDU;

    public static void main(String[] args) {
        Map<String, String> connInfoMap = Maps.newHashMap();
        initialize(args, false);
        connInfoMap.put("ip", ip);
        connInfoMap.put("port", port);
        connInfoMap.put("readTimeout", timeout);
//        connInfoMap.put("timeout4PDU", timeout4PDU);
        connInfoMap.put("readRetry", retry);
        connInfoMap.put("password", password);
        connInfoMap.put("version", version);
        connInfoMap.put("readOnlyCommunity", readOnlyCommunity);
        connInfoMap.put("collectType", collectType);
        connInfoMap.put("authProtocol", authProtocol);
        connInfoMap.put("authPassword", authPassword);
        connInfoMap.put("authPrivatePassword", authPrivatePassword);
        connInfoMap.put("authPrivateProtocol", authPrivateProtocol);
        connInfoMap.put("securityLevel", securityLevel);
        connInfoMap.put("securityName", securityName);
        final SnmpConnInfo snmpConnInfo = new SnmpConnInfo(connInfoMap);
        snmpConnInfo.setTimeout4PDU(timeout4PDU);
        System.out.println("timeout=>" + snmpConnInfo.getTimeout());
        System.out.println("timeout4PDU=>" + snmpConnInfo.getTimeout4PDU());
        snmpConnInfo.setOids(oids);
        final SnmpDefaultCollector collector = SnmpCollectorFactory.INSTANCE.createCollector(snmpConnInfo);
        if (isLoop) {
            System.out.println("Every " + interval + "ms execute,please wait...");
            final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);
            executorService.scheduleAtFixedRate(
                    new Runnable() {
                        public void run() {
                            execute(isAddIndex, snmpConnInfo, collector);
                        }
                    }, 0, interval, TimeUnit.MILLISECONDS);
        } else {
            execute(isAddIndex, snmpConnInfo, collector);
            System.exit(0);
        }
    }

    private static void initialize(final String[] args, final boolean isDebug) {
        try {
            if (isDebug) {
                final ResourceBundle resourceBundle = ResourceBundle.getBundle("config");
                ip = resourceBundle.getString("ip");
                readOnlyCommunity = resourceBundle.getString("readOnlyCommunity");
                version = resourceBundle.getString("version");
                oids = resourceBundle.getString("oids");
                isAddIndex = resourceBundle.getString("isAddIndex");
                collectType = resourceBundle.getString("collectType");
                port = resourceBundle.getString("port");
                timeout = resourceBundle.getString("timeout");
                timeout4PDU = resourceBundle.getString("timeout4PDU");
                retry = resourceBundle.getString("retry");
                isLoop = Boolean.parseBoolean(resourceBundle.getString("isLoop"));
                interval = Long.parseLong(resourceBundle.getString("interval"));
                securityLevel = resourceBundle.getString("securityLevel");
                securityName = resourceBundle.getString("securityName");
                authPassword = resourceBundle.getString("authPassword");
                authProtocol = resourceBundle.getString("authProtocol");
                authPrivateProtocol = resourceBundle.getString("authPrivateProtocol");
                authPrivatePassword = resourceBundle.getString("authPrivatePassword");
                password = resourceBundle.getString("password");
            } else {
                final Properties properties = new Properties();
                FileReader fileReader;
                fileReader = new FileReader(args[0]);
                properties.load(fileReader);
                ip = properties.getProperty("ip");
                readOnlyCommunity = properties.getProperty("readOnlyCommunity");
                version = properties.getProperty("version");
                oids = properties.getProperty("oids");
                isAddIndex = properties.getProperty("isAddIndex");
                collectType = properties.getProperty("collectType");
                port = properties.getProperty("port");
                timeout = properties.getProperty("timeout");
                timeout4PDU = properties.getProperty("timeout4PDU");
                retry = properties.getProperty("retry");
                isLoop = Boolean.parseBoolean(properties.getProperty("isLoop"));
                interval = Long.parseLong(properties.getProperty("interval"));
                securityLevel = properties.getProperty("securityLevel");
                securityName = properties.getProperty("securityName");
                authPassword = properties.getProperty("authPassword");
                authProtocol = properties.getProperty("authProtocol");
                authPrivateProtocol = properties.getProperty("authPrivateProtocol");
                authPrivatePassword = properties.getProperty("authPrivatePassword");
                password = properties.getProperty("password");
            }
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
            System.exit(1);
        }
    }

    private static void execute(final String isAddIndex, final SnmpConnInfo snmpConnInfo, final SnmpDefaultCollector collector) {
        final long currentTime = System.currentTimeMillis();
        final String formatExecTime = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss").format(currentTime);
        SnmpTable matric = new SnmpTable();
        try {
            switch (snmpConnInfo.getCollectType()) {
                case GET:
                    if ("1".equals(isAddIndex)) {
                        matric = collector.getWithOid(snmpConnInfo);
                    } else {
                        matric = collector.get(snmpConnInfo);
                    }
                    break;
                case WALK:
                    matric = collector.walk(snmpConnInfo);
                    break;
                case GETNEXT:
                    matric = collector.getNext(snmpConnInfo);
                    break;
                case GETTABLE:
                    matric = collector.getTableValue(snmpConnInfo);
                    break;
                default:
                    matric = collector.get(snmpConnInfo);
            }
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
        }
        final long costTime = System.currentTimeMillis() - currentTime;
        System.out.println(formatExecTime + "=>" + matric + "\ncost=>" + costTime + "ms");
    }
}
