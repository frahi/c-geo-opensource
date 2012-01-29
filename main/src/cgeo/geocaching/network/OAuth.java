package cgeo.geocaching.network;

import cgeo.geocaching.Settings;
import cgeo.geocaching.cgBase;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OAuth {
    public static void signOAuth(final String host, final String path, final String method, final boolean https, final Parameters params, final String token, final String tokenSecret) {
        params.put(
                "oauth_consumer_key", Settings.getKeyConsumerPublic(),
                "oauth_nonce", CryptUtils.md5(Long.toString(System.currentTimeMillis())),
                "oauth_signature_method", "HMAC-SHA1",
                "oauth_timestamp", Long.toString(new Date().getTime() / 1000),
                "oauth_token", StringUtils.defaultString(token),
                "oauth_version", "1.0");
        params.sort();

        final List<String> paramsEncoded = new ArrayList<String>();
        for (final NameValuePair nameValue : params) {
            paramsEncoded.add(nameValue.getName() + "=" + cgBase.urlencode_rfc3986(nameValue.getValue()));
        }

        final String keysPacked = Settings.getKeyConsumerSecret() + "&" + StringUtils.defaultString(tokenSecret); // both even if empty some of them!
        final String requestPacked = method + "&" + cgBase.urlencode_rfc3986((https ? "https" : "http") + "://" + host + path) + "&" + cgBase.urlencode_rfc3986(StringUtils.join(paramsEncoded.toArray(), '&'));
        params.put("oauth_signature", cgBase.base64Encode(CryptUtils.hashHmac(requestPacked, keysPacked)));
    }
}
