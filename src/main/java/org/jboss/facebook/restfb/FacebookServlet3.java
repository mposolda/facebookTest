package org.jboss.facebook.restfb;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.types.Comment;
import com.restfb.types.NamedFacebookType;
import com.restfb.types.StatusMessage;
import com.restfb.types.User;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookServlet3 extends HttpServlet {

    // "email"
    private static final String MY_ACCESS_TOKEN1 = "AAACEdEose0cBABSccdQpZCPkitZBcUsj8BSYJXDZB3pxQ6uK9oVfR9vubLp7wywisjoWNWhPCZAXB0t6IbyhJBWGVxNhuvAsElcan9lZB6fBxndEyZCI0n";
    private static final int ITEMS_PER_PAGE = 10;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        System.out.println("FBS3: doGet for URL " + req.getRequestURI());
        resp.setContentType("text/html");
        HttpSession session = req.getSession();
        PrintWriter out = resp.getWriter();

        FacebookClient facebookClient = new DefaultFacebookClient(MY_ACCESS_TOKEN1);
        User user = facebookClient.fetchObject("me", User.class);
        out.println("User: " + user.getName() + "<br>");
        out.println("username: " + user.getUsername() + "<br>");
        out.println("email: " + user.getEmail() + "<br>");
        out.println("<hr>");

        // Count total number of friends, TODO: cache
        Integer friendsCount = (Integer)session.getAttribute("friendsCount");
        if (friendsCount == null) {
            Connection<NamedFacebookType> myFriends = facebookClient.fetchConnection("me/friends", NamedFacebookType.class);
            friendsCount = myFriends.getData().size();
            session.setAttribute("friendsCount", friendsCount);
        }

        Integer pageNumber;
        if (req.getParameter("page") != null) {
            pageNumber = Integer.parseInt(req.getParameter("page"));
            session.setAttribute("page", pageNumber);
        } else {
            pageNumber = (Integer)session.getAttribute("page");
        }
        if (pageNumber == null) {
            pageNumber = 1;
        }

        Integer pageCount = ((friendsCount-1) / ITEMS_PER_PAGE) + 1;
        Integer indexStart = (pageNumber - 1) * ITEMS_PER_PAGE;
        List<NamedFacebookType> friendsToDisplay = facebookClient.fetchConnection("me/friends", NamedFacebookType.class, Parameter.with("offset", indexStart), Parameter.with("limit", ITEMS_PER_PAGE)).getData();


        out.println("Count of friends: " + friendsCount + "<br>");
        out.println("Page: " + pageNumber + "<br>");
        out.println("Select page: ");
        for (int i=1 ; i<=pageCount ; i++) {
            String url = req.getContextPath() + req.getServletPath() + "?page=" + i;
            out.print("<a href=\"" + url + "\">" + i + "</a> ");
        }
        out.println("<br>");



        // TODO: performance
        for (NamedFacebookType current : friendsToDisplay) {
            UserWithPicture friendWithPicture = facebookClient.fetchObject(current.getId(), UserWithPicture.class, Parameter.with("fields", "id,name,picture"));
            String urlForPersonDetail = req.getContextPath() + req.getServletPath() + "?friendId=" + friendWithPicture.getId();
            out.println("<a href=\"" + urlForPersonDetail + "\"><img src=\"" + friendWithPicture.getPicture().getData().getUrl() + "\" title=\"" +  friendWithPicture.getName() + "\" /></a>");
        }
        out.println("<hr>");

        String friendId = req.getParameter("friendId");
        if (friendId != null) {
            User friend = facebookClient.fetchObject(friendId, User.class);
            out.println("Friend name: " + friend.getName());
            Connection<StatusMessage> statusMessageConnection = facebookClient.fetchConnection(friendId + "/statuses", StatusMessage.class, Parameter.with("limit", 5));
            List<StatusMessage> statuses = statusMessageConnection.getData();

            for (StatusMessage statusMessage : statuses) {
                out.println("Message: " + statusMessage.getMessage() + "<br>");
                out.println("Author: " + statusMessage.getFrom().getName() + "<br>");
                out.println("Time: " + statusMessage.getUpdatedTime() + "<br>");
                out.println("Likes: " + statusMessage.getLikes().size() + "<br>");
                for (NamedFacebookType like : statusMessage.getLikes()) {
                    out.println("liker: " + like.getName() + "<br>");
                }

                List<Comment> comments = statusMessage.getComments();
                for (Comment comment : comments) {
                    out.println("Comment: " + comment.getMessage() + "<br>");
                    out.println("Comment author: " + comment.getFrom().getName() + "<br>");
                    out.println("Comment time: " + comment.getCreatedTime() + "<br>");
                    out.println("Comment likes count: " + comment.getLikeCount() + "<br>");
                }
            }
            System.out.println(statuses);
        }

        System.out.println("end");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        System.out.println("FBS3: doPost");
        doGet(req, resp);
    }
}