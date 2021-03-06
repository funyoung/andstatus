/**
 * Copyright (C) 2013 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.origin;

import android.net.Uri;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;

import org.andstatus.app.net.Connection.ApiEnum;
import org.andstatus.app.util.MyLog;
import org.andstatus.app.util.TriState;

/**
 *  Microblogging system (twitter.com, identi.ca, ... ) where messages are being created
 *  (it's the "Origin" of the messages). 
 *  TODO: Currently the class is almost a stub and serves for several predefined origins only :-)
 * @author yvolk@yurivolkov.com
 *
 */
public class Origin {
    private static final String TAG = Origin.class.getSimpleName();

    public enum OriginEnum {
        /**
         * Predefined Origin for Twitter system 
         * <a href="https://dev.twitter.com/docs">Twitter Developers' documentation</a>
         */
        TWITTER(1, "twitter", ApiEnum.TWITTER1P1, OriginTwitter.class),
        /**
         * Predefined Origin for the pump.io system 
         * Till July of 2013 (and v.1.16 of AndStatus) the API was: 
         * <a href="http://status.net/wiki/Twitter-compatible_API">Twitter-compatible identi.ca API</a>
         * Since July 2013 the API is <a href="https://github.com/e14n/pump.io/blob/master/API.md">pump.io API</a>
         */
        PUMPIO(2, "pump.io", ApiEnum.PUMPIO, OriginPumpio.class),
        STATUSNET(3, "status.net", ApiEnum.STATUSNET_TWITTER, OriginStatusNet.class),
        UNKNOWN(0,"unknownMbSystem", ApiEnum.UNKNOWN_API, Origin.class);
        
        private long id;
        private String name;
        private ApiEnum api;
        private Class<? extends Origin> originClass;
        
        private OriginEnum(long id, String name, ApiEnum api, Class<? extends Origin> originClass) {
            this.id = id;
            this.name = name;
            this.api = api;
            this.originClass = originClass;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public ApiEnum getApi() {
            return api;
        }

        public Origin newOrigin() {
            Origin origin = null;
            try {
                origin = originClass.newInstance();
                origin.id = getId();
                origin.name = getName();
                origin.api = getApi();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return origin;        
        }
        
        @Override
        public String toString() {
            return id + "-" + name;
        }
        
        public static OriginEnum fromId( long id) {
            OriginEnum originEnum = UNKNOWN;
            for(OriginEnum oe : values()) {
                if (oe.id == id) {
                    originEnum = oe;
                    break;
                }
            }
            return originEnum;
        }

        public static OriginEnum fromName( String name) {
            OriginEnum originEnum = UNKNOWN;
            for(OriginEnum oe : values()) {
                if (oe.name.equalsIgnoreCase(name)) {
                    originEnum = oe;
                    break;
                }
            }
            return originEnum;
        }
    }
    
    /**
     * Default Originating system
     * TODO: Create a table of these "Origins" ?!
     */
    public static OriginEnum ORIGIN_ENUM_DEFAULT = OriginEnum.TWITTER;

    /** 
     * The URI is consistent with "scheme" and "host" in AndroidManifest
     * Pump.io doesn't work with this scheme: "andstatus-oauth://andstatus.org"
     */
    public static final Uri CALLBACK_URI = Uri.parse("http://oauth-redirect.andstatus.org");
    
    /**
     * Maximum number of characters in the message
     */
    protected static int CHARS_MAX_DEFAULT = 140;
    /**
     * Length of the link after changing to the shortened link
     * -1 means that length doesn't change
     * For Twitter.com see <a href="https://dev.twitter.com/docs/api/1.1/get/help/configuration">GET help/configuration</a>
     */
    private static int LINK_LENGTH = 23;
    
    protected String name = OriginEnum.UNKNOWN.getName();
    protected long id = 0;
    protected ApiEnum api = ApiEnum.UNKNOWN_API;

    /**
     * Default OAuth setting
     */
    protected boolean isOAuthDefault = true;
    /**
     * Can OAuth connection setting can be turned on/off from the default setting
     */
    protected boolean canChangeOAuth = false;
    protected boolean shouldSetNewUsernameManuallyIfOAuth = false;
    /**
     * Can user set username for the new user manually?
     * This is only for no OAuth
     */
    protected boolean shouldSetNewUsernameManuallyNoOAuth = false;
    
    protected int maxCharactersInMessage = CHARS_MAX_DEFAULT;
    protected String usernameRegEx = "[a-zA-Z_0-9/\\.\\-\\(\\)]+";
    
    public static Origin toExistingOrigin(String originName_in) {
        Origin origin = fromOriginName(originName_in);
        if (origin.getId() == 0) {
            origin = fromOriginId(ORIGIN_ENUM_DEFAULT.getId());
        }
        return origin;
    }
    
    /**
     * @return the Origin name, unique in the application
     */
    public String getName() {
        return name;
    }

    /**
     * @return the OriginId in MyDatabase. 0 means that this system doesn't exist
     */
    public long getId() {
        return id;
    }

    public ApiEnum getApi() {
        return api;
    }

    /**
     * Was this Origin stored for future reuse?
     */
    public boolean isPersistent() {
        return (getId() != 0);
    }
    
    public boolean isOAuthDefault() {
        return isOAuthDefault;
    }

    /**
     * @return the Can OAuth connection setting can be turned on/off from the default setting
     */
    public boolean canChangeOAuth() {
        return canChangeOAuth;
    }

    public boolean isUsernameValid(String username) {
        boolean ok = false;
        if (username != null && (username.length() > 0)) {
            ok = username.matches(usernameRegEx);
            if (!ok && MyLog.isLoggable(TAG, Log.INFO)) {
                Log.i(TAG, "The Username is not valid: \"" + username + "\" in " + name);
            }
        }
        return ok;
    }
    
    public boolean isUsernameValidToStartAddingNewAccount(String username, boolean isOAuthUser) {
        return false;
    }
    
    public static Origin fromOriginId(long id) {
        return OriginEnum.fromId(id).newOrigin();
    }

    public static Origin fromOriginName(String name) {
        return OriginEnum.fromName(name).newOrigin();
    }

    /**
     * Calculates number of Characters left for this message
     * taking shortened URL's length into account.
     * @author yvolk@yurivolkov.com
     */
    public int charactersLeftForMessage(String message) {
        int messageLength = 0;
        if (!TextUtils.isEmpty(message)) {
            messageLength = message.length();
            
            // Now try to adjust the length taking links into account
            SpannableString ss = SpannableString.valueOf(message);
            Linkify.addLinks(ss, Linkify.WEB_URLS);
            URLSpan[] spans = ss.getSpans(0, messageLength, URLSpan.class);
            long nLinks = spans.length;
            for (int ind1=0; ind1 < nLinks; ind1++) {
                int start = ss.getSpanStart(spans[ind1]);
                int end = ss.getSpanEnd(spans[ind1]);
                messageLength += LINK_LENGTH - (end - start);
            }
            
        }
        return (maxCharactersInMessage - messageLength);
    }
    
    public int alternativeTermForResourceId(int resId) {
        return resId;
    }
    
    public String messagePermalink(String userName, long messageId) {
        return "";
    }

    public OriginConnectionData getConnectionData(TriState triState) {
        OriginConnectionData connectionData = new OriginConnectionData();
        connectionData.api = api;
        connectionData.originId = id;
        connectionData.isOAuth = triState.toBoolean(isOAuthDefault);
        if (connectionData.isOAuth != isOAuthDefault) {
            if (!canChangeOAuth) {
                connectionData.isOAuth = isOAuthDefault;
            }
        }
        return connectionData;
    }
}
