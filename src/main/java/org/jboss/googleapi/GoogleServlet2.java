package org.jboss.googleapi;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;


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
import com.google.api.services.plus.model.Person;

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

            // Validation of 'state'. Useful to prevent some types of attack (should be random value instead)
            if (!"/somethinggg".equals(state2)) {
                throw new ServletException("State is not equals to original value!!!");
            }

            String code = req.getParameter("code");
            GoogleTokenResponse tokenResponse = exchangeCodeForAccessToken(req, resp, code);

            // Finish OAuth workflow and save access token
            req.getSession().setAttribute("state", "finished");
            req.getSession().setAttribute("token", tokenResponse);

            callOperations(req, resp, tokenResponse);
        } else if ("finished".equals(state)) {
            GoogleTokenResponse tokenResponse = (GoogleTokenResponse)req.getSession().getAttribute("token");

            callOperations(req, resp, tokenResponse);
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

    private void callOperations(HttpServletRequest request, HttpServletResponse response, GoogleTokenResponse tokenData) throws IOException {

        HttpSession session = request.getSession();
        PrintWriter writer = response.getWriter();

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


        processPeople(service, request, session, writer);

    }

    private void processPeople(Plus service, HttpServletRequest request, HttpSession session, PrintWriter writer) throws IOException {
        // Get a list of people that this user has shared with this app. See https://developers.google.com/+/api/latest/people/list for details
        Plus.People.List list = service.people().list("me", "visible");
        // Possible values are "alphabetical", "best"
        list.setOrderBy("alphabetical");
        // Number of results per page
        list.setMaxResults(10L);

        // Try to obtain last pagination token
        PaginationContext pgContext = (PaginationContext)session.getAttribute("paginationContext");
        if (pgContext == null) {
            pgContext = new PaginationContext();
        }

        // Try to update pgContext with number of current page
        String pageParam = request.getParameter("page");
        if (pageParam != null) {
            if ("prev".equals(pageParam)) {
                pgContext.decreaseCurrentPage();
            } else if ("next".equals(pageParam)) {
                pgContext.increaseCurrentPage();
            } else {
                throw new RuntimeException("Illegal value of prequest parameter page. Value was " + pageParam);
            }
        }

        list.setPageToken(pgContext.getTokenOfCurrentPage());

        PeopleFeed peopleFeed = list.execute();
        List<Person> people = peopleFeed.getItems();

        writer.println("<h3>Your google+ friends</h3>");
        writer.println("Total number of friends: " + peopleFeed.getTotalItems() + "<br>");

        for (Person person : people) {
            String displayName = person.getDisplayName();
            String imageURL = person.getImage().getUrl();
            String personUrl = person.getUrl();

            writer.println("<a href=\"" + personUrl + "\"><img src=\"" + imageURL + "\" title=\"" + displayName + "\" /></a>");
        }

        // Obtain next token to session if it's available
        String nextPageToken = peopleFeed.getNextPageToken();
        int currentPage = pgContext.getCurrentPage();

        writer.println("<br>Current page: " + currentPage + "<br>");
        // Show link for previous page
        if (currentPage > 1) {
            writer.println("<a href=\"" + request.getRequestURI() + "?page=prev\">Previous page</a>");
        }
        if (nextPageToken != null) {
            pgContext.setTokenForPage(pgContext.getCurrentPage() + 1, nextPageToken);
            writer.println("<a href=\"" + request.getRequestURI() + "?page=next\">Next page</a><br>");
        }

        session.setAttribute("paginationContext", pgContext);
    }
}
