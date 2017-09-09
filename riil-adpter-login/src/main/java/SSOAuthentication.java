import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/**
 * User: wangchongyang on 2017/6/8 0008.
 */
public class SSOAuthentication {
    public static final String RES_LIST = "http://172.17.160.33:80/adapter/res/list.json";
    private static final String GET_EVENT_LIST = "http://172.17.160.33:80/adapter/event/client/getEventList1.json";
    private static final String LOGIN_URL = "http://172.17.160.33:80/adapter/login?username=admin&password=riiladmin&_openCLIENT=RIIL";
    private static HttpContext m_httpContext = HttpClientContext.create();

    public static void main(String[] args) {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        System.out.println("Main httpClient:" + httpClient);
        HttpGet httpGet = new HttpGet(LOGIN_URL);
        CloseableHttpResponse response = null;
        try {
            final HttpResponse httpResponse = httpClient.execute(httpGet, m_httpContext);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                System.out.println("--------------------Login Success-------------------");
            }
            getData(httpClient, RES_LIST);
//            getData(HttpClients.createDefault(), GET_EVENT_LIST);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static void getData(CloseableHttpClient httpClient, String url) throws IOException {
        System.out.println("getData httpClient:" + httpClient);
        System.out.println("-------------" + url + "----------------");
//        HttpPost httpPost = new HttpPost(url);
        final HttpGet httpGet = new HttpGet(url);
        final CloseableHttpResponse response = httpClient.execute(httpGet, m_httpContext);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            System.out.println(EntityUtils.toString(response.getEntity()));
        } else {
            System.out.println("--------------Execute getData Failure---------");
        }
    }

}
