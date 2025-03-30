package tfg.books.back.controllers;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tfg.books.back.requests.FirebaseSignInResponse;
import tfg.books.back.requests.RefreshTokenResponse;
import tfg.books.back.services.FirebaseAuthClient;
import tfg.books.back.services.UserService;

import javax.security.auth.login.AccountException;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final FirebaseAuthClient firebaseAuthClient;

    public UserController(UserService userService, FirebaseAuthClient firebaseAuthClient) {
        this.userService = userService;
        this.firebaseAuthClient = firebaseAuthClient;
    }

    @GetMapping
    public ResponseEntity<UserRecord> getUser() throws FirebaseAuthException {
        UserRecord userRecord = userService.retrieve();
        return ResponseEntity.ok(userRecord);
    }

    @PostMapping
    public ResponseEntity<Void> createUser(@RequestBody CreateUserRequest request) throws FirebaseAuthException, AccountException {
        //userService.create(request.emailId(), request.password(), userAlias, userName, profilePictureURL);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<FirebaseSignInResponse> loginUser(@RequestBody LoginUserRequest request) {
        FirebaseSignInResponse response = firebaseAuthClient.login(request.emailId(), request.password());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = firebaseAuthClient.exchangeRefreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser() throws FirebaseAuthException {
        userService.logout();
        return ResponseEntity.ok().build();
    }

    record CreateUserRequest(String emailId, String password) {}

    record LoginUserRequest(String emailId, String password) {}

    record RefreshTokenRequest(String refreshToken) {}

}
