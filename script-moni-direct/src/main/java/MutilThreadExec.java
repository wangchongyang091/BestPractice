import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: wangchongyang on 2017/7/5 0005.
 */
public class MutilThreadExec {
    public static void main(String[] args) throws Exception {
//        scheduleAtFixed();
        final ExecutorService execSvc = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 20; i++) {
            execSvc.execute(new Runnable() {
                @Override
                public void run() {
                    JschSFtp.instance.execScript();
                }
            });
        }
        execSvc.shutdown();
        System.out.println(execSvc.isShutdown() + "," + execSvc.isTerminated());
    }
}
