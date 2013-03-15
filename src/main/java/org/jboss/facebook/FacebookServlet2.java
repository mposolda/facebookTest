package org.jboss.facebook;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookServlet2 extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        System.out.println("FBS2: doGet for URL " + req.getRequestURI());
        String accessToken = "AAAEg4fC2gHABANEjKqcuI4Nxc4N43F6KFYlWXy001jmqg2YCdKZA0k906ZAQgpZAqIrE0FIclCsy4eV6ZCIHdmEZCCzpdP3zFQyYPbZAdyOQZDZD";
        String response = FacebookServlet.sendRequestAndGetResponse("https://graph.facebook.com/me?access_token=" + accessToken);
        resp.getWriter().println(response);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        System.out.println("FBS2: doPost");
        doGet(req, resp);
    }
}
