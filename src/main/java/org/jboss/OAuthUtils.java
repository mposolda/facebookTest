package org.jboss;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthUtils {

    // HTTP related utils

    /**
     * Given a {@link java.util.Map} of params, construct a query string
     *
     * @param params
     * @return
     */
    public static String createQueryString(Map<String, String> params) {
        StringBuilder queryString = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String paramName = entry.getKey();
            String paramValue = entry.getValue();
            if (first) {
                first = false;
            } else {
                queryString.append("&");
            }
            queryString.append(paramName).append("=");
            String encodedParamValue;
            try {
                if (paramValue == null)
                    throw new RuntimeException("paramValue is null for paramName=" + paramName);
                encodedParamValue = URLEncoder.encode(paramValue, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            queryString.append(encodedParamValue);
        }
        return queryString.toString();
    }

    /**
     * Whole HTTP response as String from given URLConnection
     *
     * @param connection
     * @return whole HTTP response as String
     */
    public static String readUrlContent(URLConnection connection) {
        StringBuilder result = new StringBuilder();
        try {
            Reader reader = new InputStreamReader(connection.getInputStream());
            char[] buffer = new char[50];
            int nrOfChars;
            while ((nrOfChars = reader.read(buffer)) != -1) {
                result.append(buffer, 0, nrOfChars);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result.toString();
    }

    /**
     * Decode given String to map. For example for input: accessToken=123456&expires=20071458 it returns map with two keys
     * "accessToken" and "expires" and their corresponding values
     *
     * @param encodedData
     * @return map with output data
     */
    public static Map<String, String> formUrlDecode(String encodedData) {
        Map<String, String> params = new HashMap<String, String>();
        String[] elements = encodedData.split("&");
        for (String element : elements) {
            String[] pair = element.split("=");
            if (pair.length == 2) {
                String paramName = pair[0];
                String paramValue;
                try {
                    paramValue = URLDecoder.decode(pair[1], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                params.put(paramName, paramValue);
            } else {
                throw new RuntimeException("Unexpected name-value pair in response: " + element);
            }
        }
        return params;
    }
}
