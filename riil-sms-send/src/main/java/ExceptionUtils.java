import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * User: wangchongyang on 2017/6/7 0007.
 */
public final class ExceptionUtils {

    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}
