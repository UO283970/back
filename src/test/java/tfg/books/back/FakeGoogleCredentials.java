package tfg.books.back;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import java.util.Date;

public class FakeGoogleCredentials extends GoogleCredentials {

    private final AccessToken token;

    public FakeGoogleCredentials() {
        this.token = new AccessToken("fake-token", new Date(System.currentTimeMillis() + 3600 * 1000));
    }

    @Override
    public String getAuthenticationType() {
        return "fake";
    }

    @Override
    public AccessToken refreshAccessToken() {
        return token;
    }

}
