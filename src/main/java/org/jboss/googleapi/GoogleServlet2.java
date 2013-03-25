package org.jboss.googleapi;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.PeopleFeed;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class GoogleServlet2 extends HttpServlet {

    private static final String CLIENT_ID = "1003123187137.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "XBo85Pur98HUBnlQrYzcST63";
    private static final String REDIRECT_URI = "http://server.local.network.cz:8080/facebookTest/test4";
    private static final String[] SCOPES = {"https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile", "https://www.googleapis.com/auth/plus.login",
            "https://www.googleapis.com/auth/plus.login"};

    /**
     * Default HTTP transport to use to make HTTP requests.
     */
    private static final HttpTransport TRANSPORT = new NetHttpTransport();
    /**
     * Default JSON factory to use to deserialize JSON.
     */
    private static final JacksonFactory JSON_FACTORY = new JacksonFactory();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        System.out.println("GoogleServlet2: doGet for URL " + req.getRequestURI());

        String state = (String)req.getSession().getAttribute("state");
        if (state == null) {
            initialInteraction(req, resp);
            return;
        } else if ("started".equals(state)) {
            String state2 = req.getParameter("state");

            // Validation of 'state'. Useful to prevent some types of attack
            if (!"/somethinggg".equals(state2)) {
                throw new ServletException("State is not equals to original value!!!");
            }

            String code = req.getParameter("code");
            GoogleTokenResponse tokenResponse = exchangeCodeForAccessToken(req, resp, code);

            callOperations(tokenResponse, resp.getWriter());

            // Clear state
            req.getSession().removeAttribute("state");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        System.out.println("GoogleServlet2: doPost");
        doGet(req, resp);
    }

    private boolean initialInteraction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Generate the URL to which we will direct users
        String authorizeUrl = new GoogleAuthorizationCodeRequestUrl(CLIENT_ID,
                REDIRECT_URI, Arrays.asList(SCOPES)).setState("/somethinggg").build();
        System.out.println("URL to send to Google+: " + authorizeUrl);

        try {
            request.getSession().setAttribute("state", "started");
            response.sendRedirect(authorizeUrl);
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private GoogleTokenResponse exchangeCodeForAccessToken(HttpServletRequest request, HttpServletResponse response, String code) throws IOException {
        // Upgrade the authorization code into an access and refresh token.
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(TRANSPORT, JSON_FACTORY, CLIENT_ID,
                CLIENT_SECRET, code, REDIRECT_URI).execute();

        // Create a credential representation of the token data.
        GoogleCredential credential = new GoogleCredential.Builder()
                .setJsonFactory(JSON_FACTORY)
                .setTransport(TRANSPORT)
                .setClientSecrets(CLIENT_ID, CLIENT_SECRET).build()
                .setFromTokenResponse(tokenResponse);


        // Check that the token is valid.
        Oauth2 oauth2 = new Oauth2.Builder(TRANSPORT, JSON_FACTORY, credential).build();
        Tokeninfo tokenInfo = oauth2.tokeninfo().setAccessToken(credential.getAccessToken()).execute();

        // If there was an error in the token info, abort.
        if (tokenInfo.containsKey("error")) {
            throw new RuntimeException(tokenInfo.get("error").toString());
        }

        // Make sure the token we got is for the intended user.
        System.out.println("Token issued for user " + tokenInfo.getUserId());

        // Make sure the token we got is for our app.
        if (!tokenInfo.getIssuedTo().equals(CLIENT_ID)) {
            throw new RuntimeException("Token's client ID does not match app's. clientID from tokenINFO: " + tokenInfo.getIssuedTo());
        }

        return tokenResponse;
    }

    private void callOperations(GoogleTokenResponse tokenData, PrintWriter writer) throws IOException {
        // Build credential from stored token data.
        GoogleCredential credential = new GoogleCredential.Builder()
                .setJsonFactory(JSON_FACTORY)
                .setTransport(TRANSPORT)
                .setClientSecrets(CLIENT_ID, CLIENT_SECRET).build()
                .setFromTokenResponse(tokenData);

        // Create a new authorized API client.
        Plus service = new Plus.Builder(TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("Some app name")
                .build();

        // Get a list of people that this user has shared with this app.
        PeopleFeed people = service.people().list("me", "visible").execute();

        System.out.println(people.toString());
        writer.println(people);
    }
}
