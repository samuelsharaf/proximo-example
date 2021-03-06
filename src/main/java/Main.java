import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

public class Main extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    URL proximo = new URL(System.getenv("PROXIMO_URL"));
    String userInfo = proximo.getUserInfo();
    String user = userInfo.substring(0, userInfo.indexOf(':'));
    String password = userInfo.substring(userInfo.indexOf(':') + 1);

    System.setProperty("socksProxyHost", proximo.getHost());
    Authenticator.setDefault(new ProxyAuthenticator(user, password));

    String urlStr = "http://httpbin.org/ip";

    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpGet request = new HttpGet(urlStr);

    CloseableHttpResponse response = httpClient.execute(request);
    try {
      resp.getWriter().print("Hello from Java! " + handleResponse(response));
    } catch (Exception e) {
      resp.getWriter().print(e.getMessage());
    }
  }

  private static String handleResponse(CloseableHttpResponse response) throws IOException {
    StatusLine statusLine = response.getStatusLine();
    HttpEntity entity = response.getEntity();
    if (statusLine.getStatusCode() >= 300) {
      throw new HttpResponseException(
          statusLine.getStatusCode(),
          statusLine.getReasonPhrase());
    }
    if (entity == null) {
      throw new ClientProtocolException("Response contains no content");
    }
    return readStream(entity.getContent());
  }

  private static String readStream(InputStream is) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String output = "";
    String tmp = reader.readLine();
    while (tmp != null) {
      output += tmp;
      tmp = reader.readLine();
    }
    return output;
  }

  private class ProxyAuthenticator extends Authenticator {
    private final PasswordAuthentication passwordAuthentication;

    private ProxyAuthenticator(String user, String password) {
      passwordAuthentication = new PasswordAuthentication(user, password.toCharArray());
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      return passwordAuthentication;
    }
  }
}
