/*
 * JBoss, a division of Red Hat
 * Copyright 2011, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.facebook;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookServlet extends HttpServlet
{

   private static final String APP_ID = "317629754933360";
   private static final String APP_SECRET = "e129e8fccdd3382af53c5fb05b09bc92";
   private static final String MY_URL = "http://server.local.network:8080/facebookTest/test1";


   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
   {
      System.out.println("FBS: doGet for URL " + req.getRequestURI());

      HttpSession session = req.getSession();


      // Managing first request
      String code = (String)session.getAttribute("Code");
      if (code == null)
      {
         String reqCode = req.getParameter("code");
         if (reqCode != null)
         {
            System.out.println("Received code from Facebook: " + reqCode);
            session.setAttribute("Code", reqCode);
            code = reqCode;
         }
      }

      // Access token
      String accessToken = (String)session.getAttribute("AccessToken");

      if (accessToken==null && code==null)
      {
         String url1 = "https://www.facebook.com/dialog/oauth?client_id=" + APP_ID  + "&scope=email&redirect_uri=" + MY_URL;
         url1 = resp.encodeRedirectURL(url1);
         System.out.println("Redirecting to facebook for dialog with URL " + url1);
         resp.sendRedirect(url1);
      }
      else if (accessToken == null)
      {
         String url2 = "https://graph.facebook.com/oauth/access_token?client_id=" + APP_ID + "&redirect_uri=" + MY_URL + "&client_secret=" + APP_SECRET + "&code=" + code;
         url2 = resp.encodeRedirectURL(url2);
         System.out.println("Contacting facebook with separate HTTP request for obtain accessToken " + url2);

         accessToken = loadAccessTokenAndExpires(url2);
         System.out.println("Saving access token from Facebook to session. Token is " + accessToken);
         session.setAttribute("AccessToken", accessToken);
         // resp.sendRedirect(url2);
      }

      if (accessToken != null)
      {
         String response = sendRequestAndGetResponse("https://graph.facebook.com/me?access_token=" + accessToken);
         resp.getWriter().println(response);
      }
   }

   @Override
   protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
   {
      System.out.println("FBS: doPost");
      doGet(req, resp);
   }

   private String loadAccessTokenAndExpires(String url) throws IOException
   {
      String token = sendRequestAndGetResponse(url);
      System.out.println("Obtaining line with token: " + token);
      token = parseToken(token);
      return token;
   }

   static String sendRequestAndGetResponse(String url) throws IOException
   {
      BufferedReader in = null;
      StringBuilder response = new StringBuilder();
      try
      {
         URL tempURL = new URL(url);
         URLConnection yc = tempURL.openConnection();
         in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
         String inputLine;

         while ((inputLine = in.readLine()) != null)
         {
           System.out.println("Obtaining line: " + inputLine);
           response.append(inputLine);
           response.append(System.getProperty("line.separator"));
         }

         return response.toString();
      }
      finally
      {
         in.close();
      }
   }

   private String parseToken(String input)
   {
      String ln = input.substring(13);
      int ind = ln.indexOf("&expires=");
      ln = ln.substring(0, ind);
      return ln;
   }
}
