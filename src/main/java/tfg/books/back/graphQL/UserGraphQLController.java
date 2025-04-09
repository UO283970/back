package tfg.books.back.graphQL;

import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import tfg.books.back.firebase.AppFirebaseConstants;
import tfg.books.back.model.userActivity.UserActivity;
import tfg.books.back.model.userModels.*;
import tfg.books.back.model.userModels.User.UserFollowState;
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
    public Boolean tokenCheck(){
        return userService.tokenCheck();
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
    public List<UserForSearch> getUserSearchInfo(@Argument("userQuery") String userQuery) {
        return userService.getUserSearchInfo(userQuery);
    }

    @QueryMapping
    public UserForApp getAuthenticatedUserInfo() {
        return userService.getAuthenticatedUserInfo();
    }

    @QueryMapping
    public UserForApp getAllUserInfo(@Argument("userId") String userId) {
        return userService.getAllUserInfo(userId);
    }

    @QueryMapping
    public List<UserForProfile> getFollowersOfUser(@Argument("userId") String userId) {
        return userService.getUsersListOFUser(userId, AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION);
    }


    @QueryMapping
    public List<UserForProfile> getFollowingListUser(@Argument("userId") String userId) {
        return userService.getUsersListOFUser(userId, AppFirebaseConstants.USERS_FOLLOWING_COLLECTION);
    }

    @QueryMapping
    public List<UserActivity> getUsersReviews(@Argument("userId") String userId) {
        return userService.getUsersReviews(userId);
    }

    @MutationMapping
    public RegisterUser createUser(@Argument("email") String email, @Argument("password") String password,
                                   @Argument("repeatedPassword") String repeatedPassword, @Argument("userAlias") String userAlias,
                                   @Argument("userName") String userName, @Argument("profilePictureURL") String profilePictureURL)
            throws FirebaseAuthException {
        return userService.create(email, password, repeatedPassword, userAlias.toLowerCase(), userName, profilePictureURL);
    }

    @MutationMapping
    public Boolean updateUser(@Argument("userAlias") String userAlias, @Argument("userName") String userName,
                              @Argument("profilePictureURL") String profilePictureURL,
                              @Argument("description") String description, @Argument("privacyLevel")User.UserPrivacy privacyLevel)
            throws FirebaseAuthException {
        return userService.update(userAlias, userName, profilePictureURL, description, privacyLevel);
    }

    @MutationMapping
    public Boolean deleteUser()
            throws FirebaseAuthException {
        return userService.delete();
    }

    @MutationMapping
    public UserFollowState followUser(@Argument("friendId") String friendId)
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
