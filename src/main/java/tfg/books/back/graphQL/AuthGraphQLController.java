package tfg.books.back.graphQL;

import java.security.InvalidParameterException;
import javax.security.auth.login.AccountException;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

import tfg.books.back.requests.FirebaseSignInResponse;
import tfg.books.back.requests.RefreshTokenResponse;
import tfg.books.back.services.FirebaseAuthClient;
import tfg.books.back.services.UserService;
import tfg.books.back.model.User;

@Controller
public class AuthGraphQLController {

    private final FirebaseAuthClient firebaseAuthClient;
    private final UserService userService;
    private final Firestore firestore;

    public AuthGraphQLController(FirebaseAuthClient firebaseAuthClient, UserService userService,Firestore firestore) {
        this.firebaseAuthClient = firebaseAuthClient;
        this.userService = userService;
        this.firestore = firestore;
    }

    @QueryMapping
    public UserLogin login(@Argument String email, @Argument String password) throws FirebaseAuthException {
        FirebaseSignInResponse response = firebaseAuthClient.login(email, password);
        if (response.idToken() != null || !response.idToken().equals("")) {
            return new UserLogin(response.idToken(), response.refreshToken());
        } else {
            throw new InvalidParameterException("User not found");
        }

    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public String logout() throws FirebaseAuthException {
        return userService.logout();
    }

    @QueryMapping
    public String refreshTocken(@Argument String oldRefreshTocken) throws FirebaseAuthException {
        RefreshTokenResponse response = firebaseAuthClient.exchangeRefreshToken(oldRefreshTocken);
        if (response.id_token() != null || !response.id_token().equals("")) {
            return response.id_token();
        } else {
            throw new InvalidParameterException("User not found");
        }

    }

    @MutationMapping
    public String creteUser(@Argument String email, @Argument String password)
            throws FirebaseAuthException, AccountException {
        UserRecord userRecord = userService.create(email, password);
        if (userRecord != null) {
            FirebaseSignInResponse response = firebaseAuthClient.login(email, password);
            
            firestore.collection("users").document(userRecord.getUid()).set(new User(userRecord.getEmail()));
            return response.idToken();
        } else {
            throw new InvalidParameterException("User not found");
        }

    }

    record UserLogin(String tockenId, String refreshTocken) {
    }
}
