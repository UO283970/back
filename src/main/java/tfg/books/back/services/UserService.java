package tfg.books.back.services;

import javax.security.auth.login.AccountException;

import org.springframework.stereotype.Service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;

@Service
public class UserService {
    private static final String DUPLICATE_ACCOUNT_ERROR = "EMAIL_EXISTS";
    
    private final FirebaseAuth firebaseAuth;
    private final AuthenticatedUserIdProvider authenticatedUserIdProvider;

    public UserService(FirebaseAuth firebaseAuth, AuthenticatedUserIdProvider authenticatedUserIdProvider) {
        this.firebaseAuth = firebaseAuth;
        this.authenticatedUserIdProvider = authenticatedUserIdProvider;
    }

    public UserRecord create(String emailId, String password) throws AccountException, FirebaseAuthException {
        CreateRequest request = new CreateRequest();
        request.setEmail(emailId);
        request.setPassword(password);

        try {
            return firebaseAuth.createUser(request);
        } catch (FirebaseAuthException exception) {
            if (exception.getMessage().contains(DUPLICATE_ACCOUNT_ERROR)) {
                throw new AccountException("Account with given email-id already exists");
            }
            throw exception;
        }
    }

    record User(String tockenId, String refreshTocken) {
    }

    public String logout() throws FirebaseAuthException {
        String userId = authenticatedUserIdProvider.getUserId();
        firebaseAuth.revokeRefreshTokens(userId);
        return userId;
    }

    public UserRecord retrieve() throws FirebaseAuthException {
        String userId = authenticatedUserIdProvider.getUserId();
        return firebaseAuth.getUser(userId);
    }
}
