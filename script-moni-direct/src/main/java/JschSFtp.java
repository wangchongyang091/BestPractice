import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;

public enum JschSFtp {
    instance;
    private static final String USERNAME = "root";
    private static final String PASSWORD = "1qa@WS3ed";
    private static final String HOST = "172.17.160.189";
    private static final int PORT = 22;
    private static final String SRC = "F:/Materia/others/resource/ruijie/raceTime/tools/ScriptMoni/script/moni.sh";
    private static final String REMOTE_DIR_PATH = "/usr/java/jdk1.7.0_79/bin";
    private static final String REMOTE_SCRIPT_FILE_NAME = "moni.sh";
    private static final String DST = REMOTE_DIR_PATH + File.separator + REMOTE_SCRIPT_FILE_NAME;
    private static final Object lock = new Object();
    private static Session session = null;

    private static void createSession() {
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(USERNAME, HOST, PORT);
            session.setPassword(PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
        }
    }

    private static void validateScriptFileIsExist() {
        ChannelSftp chSftp = null;
        try {
            chSftp = (ChannelSftp) session.openChannel("sftp");
            chSftp.connect();
            chSftp.ls(DST);
        } catch (JSchException e) {
            System.out.println("create ChannelSftp failed :" + e);
        } catch (SftpException e) {
            System.out.println("The " + DST + " file does not exist." + e);
        } finally {
            if (null != chSftp && chSftp.isConnected()) {
                chSftp.disconnect();
            }
        }
    }

    private void scheduleAtFixed() throws Exception {
        instance.createSession();
//        uploadFile4Remote();
        final ScheduledExecutorService scheduledExecSvc = Executors.newScheduledThreadPool(5);
        scheduledExecSvc.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                execScript();
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private void uploadFile4Remote() {
        instance.createSession();
        Channel channel = null;
        ChannelSftp chSftp = null;
        try {
            channel = session.openChannel("sftp");
            chSftp = (ChannelSftp) channel;
            chSftp.connect();
            if (!isExist(chSftp, REMOTE_DIR_PATH, REMOTE_SCRIPT_FILE_NAME)) {
                chSftp.put(SRC, DST);
                System.out.println("upload file:" + DST + " success.");
            }

        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
        } finally {
            if (chSftp != null && chSftp.isConnected()) {
                chSftp.quit();
                chSftp.disconnect();
            }
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
        }
    }

    private boolean isExist(ChannelSftp chSftp, String remoteDirPath, String remoteScriptFilePath) throws Exception {
        try {
            chSftp.ls(remoteDirPath);
        } catch (SftpException e) {
            System.out.println("The " + remoteDirPath + " directory does not exist." + e);
            try {
                chSftp.mkdir(remoteDirPath);
            } catch (SftpException e1) {
                throw new Exception("The " + remoteDirPath + " directory create failure.", e1);
            }
        }
        try {
            chSftp.ls(remoteDirPath + "/" + remoteScriptFilePath);
        } catch (Exception e) {
            throw new FileNotFoundException("\"The \" + remoteScriptFilePath + \" file does not exist.\" + e");
        }
        return true;
    }

    public void execScript() {
        synchronized (lock) {
            System.out.println(Thread.currentThread().getName() + " before execScript:" + FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()));
            if (null == session) {
                createSession();
            }
            ChannelExec chExec = null;
            try {
                if (!session.isConnected()) {
                    session = null;
                    createSession();
                }
                validateScriptFileIsExist();
                chExec = (ChannelExec) session.openChannel("exec");
                chExec.setCommand("chmod a+x " + DST);
                chExec.setCommand("sh " + REMOTE_DIR_PATH + "/" + REMOTE_SCRIPT_FILE_NAME);
                chExec.connect();
//            String result = instance.inputStreamHandle(chExec);
//            System.out.println("result=" + result);
                System.out.printf("%s %s after execScript:%s%n\n", Thread.currentThread().getName(), instance.inputStreamHandle(chExec), FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss").format(System.currentTimeMillis()));
            } catch (JSchException | IOException e) {
                System.out.println(ExceptionUtils.getStackTrace(e));
            } finally {
                if (chExec != null && chExec.isConnected()) {
                    chExec.disconnect();
                }
            }
        }
    }

    private String inputStreamHandle(ChannelExec channel) throws IOException {
        try {
            InputStream stdout = channel.getInputStream();
            InputStream stderr = channel.getErrStream();
            ByteArrayOutputStream bufOut = new ByteArrayOutputStream();
            ByteArrayOutputStream bufErr = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            boolean run = true;
            while (run) {
                if (stdout.available() > 0) {
                    int count = stdout.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        bufOut.write(buffer, 0, count);
                    }
                }
                if (stderr.available() > 0) {
                    int count = stderr.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        bufErr.write(buffer, 0, count);
                    }
                }
                if (channel.isClosed()) {
                    if (stdout.available() > 0 || stderr.available() > 0) {
                        continue;
                    }
                    run = false;
                }
            }
            String bufErrString = bufErr.toString("UTF-8");
            final String bufOutString = bufOut.toString("UTF-8");
            return String.format("out:%serr:%s", bufOutString, bufErrString);
//            return bufOut.toString("UTF-8");
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
}
