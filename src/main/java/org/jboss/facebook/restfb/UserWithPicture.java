package org.jboss.facebook.restfb;

import com.restfb.Facebook;
import com.restfb.types.NamedFacebookType;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserWithPicture extends NamedFacebookType {

    @Facebook("picture")
    private Picture picture;

    public Picture getPicture() {
        return picture;
    }

    public static class Picture {

        @Facebook("data")
        private Data data;

        public Data getData() {
            return data;
        }
    }

    public static class Data {

        @Facebook ("url")
        private String url;

        @Facebook("is_silhouette")
        private Boolean isSilhouette;

        public String getUrl() {
            return url;
        }

        public Boolean isSilhouette() {
            return isSilhouette;
        }

    }
}
