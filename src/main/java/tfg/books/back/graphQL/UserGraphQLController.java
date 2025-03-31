package tfg.books.back.graphQL;

import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import tfg.books.back.model.LoginUser;
import tfg.books.back.model.User;
import tfg.books.back.services.UserService;

import java.util.List;

@Controller
public class UserGraphQLController {

    private final UserService userService;

    public UserGraphQLController(UserService userService) {
        this.userService = userService;
    }

    @QueryMapping
    public LoginUser login(@Argument("email") String email, @Argument("password") String password) {
        return userService.login(email, password);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public String logout() throws FirebaseAuthException {
        return userService.logout();
    }

    @QueryMapping
    public String refreshToken(@Argument("oldRefreshToken") String oldRefreshToken) {
        return userService.refreshToken(oldRefreshToken);
    }

    @QueryMapping
    public List<User> getUserSearchInfo(@Argument("userQuery") String userQuery) {
        return userService.getUserSearchInfo(userQuery);
    }

    @QueryMapping
    public User getAuthenticatedUserInfo() {
        return userService.getAuthenticatedUserInfo();
    }

    @QueryMapping
    public User getAllUserInfo(@Argument("userId") String userId) {
        return userService.getAllUserInfo(userId);
    }

    @MutationMapping
    public Boolean updateUser(@Argument("userAlias") String userAlias, @Argument("userName") String userName,
                              @Argument("profilePictureURL") String profilePictureURL,
                              @Argument("description") String description)
            throws FirebaseAuthException {
        return userService.update(userAlias, userName, profilePictureURL, description);
    }

    @MutationMapping
    public LoginUser createUser(@Argument("email") String email, @Argument("password") String password,
                                @Argument("userAlias") String userAlias, @Argument("userName") String userName,
                                @Argument("profilePictureURL") String profilePictureURL)
            throws FirebaseAuthException {
        return userService.create(email, password, userAlias, userName, profilePictureURL);
    }

    @MutationMapping
    public Boolean deleteUser()
            throws FirebaseAuthException {
        return userService.delete();
    }

    @MutationMapping
    public Boolean followUser(@Argument("friendId") String friendId)
            throws FirebaseAuthException {
        return userService.followUser(friendId);
    }

    @MutationMapping
    public Boolean cancelFollow(@Argument("friendId") String friendId)
            throws FirebaseAuthException {
        return userService.cancelFollow(friendId);
    }

    @MutationMapping
    public Boolean acceptRequest(@Argument("friendId") String friendId)
            throws FirebaseAuthException {
        return userService.acceptRequest(friendId);
    }

    @MutationMapping
    public Boolean cancelRequest(@Argument("friendId") String friendId)
            throws FirebaseAuthException {
        return userService.cancelRequest(friendId);
    }
    @MutationMapping
    public Boolean deleteFromFollower(@Argument("friendId") String friendId)
            throws FirebaseAuthException {
        return userService.deleteFromFollower(friendId);
    }
}
