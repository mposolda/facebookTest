package org.jboss.google;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jboss.OAuthUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GoogleServlet extends HttpServlet {

    private static final String CLIENT_ID = "1003123187137.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "XBo85Pur98HUBnlQrYzcST63";
    private static final String REDIRECT_URI = "http://server.local.network.cz:8080/facebookTest/test3";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        System.out.println("GoogleServlet: doGet for URL " + req.getRequestURI());
        String state = (String)req.getSession().getAttribute("state");
        if (state == null) {
            initialInteraction(req, resp);
            return;
        } else if ("started".equals(state)) {
            String state2 = req.getParameter("state");

            // Validation of 'state'. Useful to prevent some types of attack
            if (!"/profile".equals(state2)) {
                throw new ServletException("State is not equals to original value!!!");
            }

            String code = req.getParameter("code");
            String accessToken = exchangeCodeForAccessToken(req, resp, code);

            String userProfile = getUserProfile(accessToken);

            // write info about user profile
            resp.getWriter().println(userProfile);

            // Clear state
            req.getSession().removeAttribute("state");
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        System.out.println("GoogleServlet: doPost");
        doGet(req, resp);
    }

    private boolean initialInteraction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        Map<String, String> params = new HashMap<String, String>();
        params.put("response_type", "code");
        params.put("client_id", CLIENT_ID);
        params.put("redirect_uri", REDIRECT_URI);
        params.put("scope", "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile");
        params.put("state", "/profile");

        String location = new StringBuilder("https://accounts.google.com/o/oauth2/auth").append("?").append(OAuthUtils.createQueryString(params))
                .toString();
        try {
            session.setAttribute("state", "started");
            response.sendRedirect(location);
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String exchangeCodeForAccessToken(HttpServletRequest request, HttpServletResponse response, String code) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("code", code);
        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET);
        params.put("redirect_uri", REDIRECT_URI);
        params.put("grant_type", "authorization_code");

        String location = "https://accounts.google.com/o/oauth2/token";
        String urlParameters = OAuthUtils.createQueryString(params).toString();


        URL url = new URL(location);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

        connection.setRequestProperty("Content-Length", "" +
                Integer.toString(urlParameters.getBytes().length));

        connection.setUseCaches (false);
        connection.setDoInput(true);
        connection.setDoOutput(true);

        //Send request
        DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        String response2 = OAuthUtils.readUrlContent(connection);

        try {
            JSONObject jsonObject = new JSONObject(response2);
            String accessToken = jsonObject.getString("access_token");
            String idToken = jsonObject.getString("id_token");
            String expires = jsonObject.getString("expires_in");

            return accessToken;
        } catch (JSONException jse) {
            throw new RuntimeException(jse);
        }
    }

    private String getUserProfile(String accessToken) throws IOException {
        String location = "https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + accessToken;
        URL url = new URL(location);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        String resp = OAuthUtils.readUrlContent(connection);
        return resp;
    }
}
