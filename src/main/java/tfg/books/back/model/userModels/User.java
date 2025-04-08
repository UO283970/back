package tfg.books.back.model.userModels;

import com.google.firebase.database.annotations.NotNull;
import tfg.books.back.model.list.BookList;

import java.util.ArrayList;
import java.util.List;

public class User {

    @NotNull
    private final String email;
    @NotNull
    private final String userName;
    @NotNull
    private final String userAlias;
    @NotNull
    private final String profilePictureURL;
    @NotNull
    private final String description;
    @NotNull
    private final UserPrivacy userPrivacy;
    @NotNull
    private final UserFollowState userFollowState;
    @NotNull
    private final List<String> followersUsers;
    @NotNull
    private final List<String> followingUsers;
    @NotNull
    private final List<String> userActivities;
    @NotNull
    private final List<BookList> userDefaultLists;
    @NotNull
    private final List<String> userLists;
    @NotNull
    private final List<String> followRequestList;

    public User() {
        this.email = "";
        this.userName = "";
        this.userAlias = "";
        this.profilePictureURL = "";
        this.description = "";
        this.userPrivacy = UserPrivacy.PUBLIC;
        this.userFollowState = UserFollowState.FOLLOWING;
        this.followersUsers = new ArrayList<>();
        this.followingUsers = new ArrayList<>();
        this.userActivities = new ArrayList<>();
        this.userDefaultLists = new ArrayList<>();
        this.userLists = new ArrayList<>();
        this.followRequestList = new ArrayList<>();
    }

    public User(@NotNull String email, @NotNull String userName, @NotNull String userAlias,
                @NotNull String profilePictureURL, @NotNull String description,
                @NotNull UserPrivacy userPrivacy, @NotNull UserFollowState userFollowState, @NotNull List<String> followedUsers,
                @NotNull List<String> followingUsers, @NotNull List<String> userActivities,
                @NotNull List<BookList> userDefaultLists, @NotNull List<String> userLists,
                @NotNull List<String> followRequestList) {
        this.email = email;
        this.userName = userName;
        this.userAlias = userAlias;
        this.profilePictureURL = profilePictureURL;
        this.description = description;
        this.userPrivacy = userPrivacy;
        this.userFollowState = userFollowState;
        this.followersUsers = followedUsers;
        this.followingUsers = followingUsers;
        this.userActivities = userActivities;
        this.userDefaultLists = userDefaultLists;
        this.userLists = userLists;
        this.followRequestList = followRequestList;
    }

    public User(@NotNull String email, @NotNull String userName, @NotNull String profilePictureURL,
                @NotNull String userAlias) {
        this(email, userName, userAlias, profilePictureURL, "", UserPrivacy.PUBLIC,UserFollowState.FOLLOWING,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>());
    }

    public List<BookList> getUserDefaultLists() {
        return userDefaultLists;
    }

    public String getProfilePictureURL() {
        return profilePictureURL;
    }

    public UserFollowState getUserFollowState() {
        return userFollowState;
    }

    public List<String> getUserLists() {
        return userLists;
    }

    public List<String> getFollowRequestList() {
        return followRequestList;
    }

    public String getEmail() {
        return this.email;
    }

    public String getUserName() {
        return this.userName;
    }

    public String getUserAlias() {
        return this.userAlias;
    }

    public String getDescription() {
        return this.description;
    }

    public String getUserPrivacy() {
        if (this.userPrivacy == null) {
            return "";
        }
        return this.userPrivacy.toString();
    }

    public List<String> getFollowersUsers() {
        return this.followersUsers;
    }

    public List<String> getFollowingUsers() {
        return this.followingUsers;
    }

    public List<String> getUserActivities() {
        return this.userActivities;
    }

    public enum UserPrivacy {
        PRIVATE,
        PUBLIC
    }

    public enum UserFollowState {
        FOLLOWING,
        NOT_FOLLOW,
        REQUESTED,
        OWN
    }

}
