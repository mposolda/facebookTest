package org.jboss.facebook.restfb;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.types.FacebookType;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookServlet4 extends HttpServlet {

    private static final String MY_ACCESS_TOKEN1 = "BAACEdEose0cBALXHNwsZBspxfvey3nQYuwINyD0l7MmIRVRJWQZCrMlinZCUYZBZBWltfPbvEvHdyLXRkWs3PR0mFUkujUqFWuOSl0QozhtZAHSNSrcMatPNYLz7smF7R3yctCjcvsRloZBLWcoC0JFabqBSkdDxKZCvdkgjZChEhi0p3TXbeYjjskGvbjaoi5z0kgLNe9oW7064RpjqCBzQS";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        System.out.println("FBS4: doGet for URL " + req.getRequestURI());
        resp.setContentType("text/html; charset=\"utf-8\"");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();

        out.println("<h3>Publish some content to your facebook wall</h3>");
        out.println("<div style=\"font-size: 13px;\">Either message or link are required fields</div><br>");
        out.println("<form method=\"POST\">");
        out.println("<table>");
        out.println(renderInput("message", true, session));
        out.println("<tr><td></td><td></td></tr>");
        out.println("<tr><td colspan=2><div style=\"font-size: 13px;\">Other parameters, which are important only if you want to publish some link</div></td></tr>");
        out.println(renderInput("link", true, session));
        out.println(renderInput("picture", false, session));
        out.println(renderInput("name", false, session));
        out.println(renderInput("caption", false, session));
        out.println(renderInput("description", false, session));
        out.println("</table>");
        out.println("<input type=\"submit\" value=\"submit\" />");
        out.println("</form>");

        System.out.println("end get");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        System.out.println("FBS4: doPost");
        resp.setContentType("text/html; charset=\"utf-8\"");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();

        String message = getParameterAndSaveItToSession("message", req, session);
        String link = getParameterAndSaveItToSession("link", req, session);
        String picture = getParameterAndSaveItToSession("picture", req, session);
        String name = getParameterAndSaveItToSession("name", req, session);
        String capture = getParameterAndSaveItToSession("caption", req, session);
        String description = getParameterAndSaveItToSession("description", req, session);

        if (isEmpty(message) && isEmpty(link)) {
            out.println("Either message or link needs to be specified!<br>");
            out.println("<a href=\"" + req.getRequestURI() + "\">Back</a><br>");
            return;
        }

        System.out.println("message=" + message + ", link=" + link
                + ", picture=" + picture
                + ", name=" + name
                + ", caption=" + capture
                + ", description=" + description);


        FacebookClient facebookClient = new DefaultFacebookClient(MY_ACCESS_TOKEN1);
        List<Parameter> params = new ArrayList<Parameter>();
        appendParam(params, "message", message);
        appendParam(params, "link", link);
        appendParam(params, "picture", picture);
        appendParam(params, "name", name);
        appendParam(params, "caption", capture);
        appendParam(params, "description", description);

        try {
            FacebookType publishMessageResponse = facebookClient.publish("me/feed", FacebookType.class, params.toArray(new Parameter[] {}));
            if (publishMessageResponse.getId() != null) {
                System.out.println("Message published successfully to Facebook profile of user " + req.getRemoteUser() + " with ID " + publishMessageResponse.getId());
                out.println("Message published successfully to your Facebook profile!");
            }
        } catch (FacebookOAuthException foe) {
            String exMessage = "Error occured: " + foe.getErrorCode() + " - " + foe.getErrorType() + " - " + foe.getErrorMessage();
            System.out.println(exMessage);
            out.println(exMessage + "<br>");
            if (foe.getErrorMessage().contains("URL is not properly formatted")) {
                // do nothing special
            } else if (foe.getErrorMessage().contains("The user hasn't authorized the application to perform this action")) {
                out.println("You need at least privileges of scope: publish_stream<br>");
                System.out.println("You need at least privileges of scope: publish_stream");
            } else {
                foe.printStackTrace();
            }
        }

        System.out.println("end post");
    }

    private boolean isEmpty(String message) {
        return message == null || message.length() == 0;
    }

    private String getParameterAndSaveItToSession(String paramName, HttpServletRequest req, HttpSession session) {
        String paramValue = req.getParameter(paramName);
        if (paramValue != null) {
            session.setAttribute(paramName, paramValue);
        } else {
            paramValue = (String)session.getAttribute(paramName);
        }
        return paramValue;
    }

    private String renderInput(String inputName, boolean required, HttpSession session) {
        String label = inputName.substring(0, 1).toUpperCase() + inputName.substring(1);
        StringBuilder result = new StringBuilder("<tr><td>" + label + ": </td><td><input name=\"").
                append(inputName + "\"");

        // Try to read value from session
        String value = (String)session.getAttribute(inputName);
        if (value != null) {
            result.append(" value=\"" + value + "\"");
        }

        result.append(" />");
        if (required) {
            result = result.append(" *");
        }
        return result.append("</td></tr>").toString();
    }

    private void appendParam(List<Parameter> params, String paramName, String paramValue) {
        if (paramValue != null) {
            params.add(Parameter.with(paramName, paramValue));
        }
    }
}
