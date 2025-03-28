package tfg.books.back.model;

import com.google.firebase.database.annotations.NotNull;

public class User {

    private enum UserPrivacy {
        PRIVATE,
        PUBLIC
    }

    private enum UserFollowState {
        FOLLOWING,
        FOLLOWED,
        OWN,
        REQUESTED
    }

    @NotNull
    private String email;
    @NotNull
    private String userName;
    @NotNull
    private String userAlias;
    @NotNull
    private String description;
    @NotNull
    private UserPrivacy userPrivacy;
    @NotNull
    private UserFollowState userFollowState;
    @NotNull
    private int followedUsersCount;
    @NotNull
    private int followingUsersCount;
    @NotNull
    private int userReviewsCount;

    public User(@NotNull String email, @NotNull String userName, @NotNull String userAlias, @NotNull String description,
            @NotNull UserPrivacy userPrivacy, @NotNull UserFollowState userFollowState, @NotNull int followedUsersCount,
            @NotNull int followingUsersCount, @NotNull int userReviewsCount) {
        this.email = email;
        this.userName = userName;
        this.userAlias = userAlias;
        this.description = description;
        this.userPrivacy = userPrivacy;
        this.userFollowState = userFollowState;
        this.followedUsersCount = followedUsersCount;
        this.followingUsersCount = followingUsersCount;
        this.userReviewsCount = userReviewsCount;
    }

    public User(@NotNull String email, @NotNull String userName, @NotNull String userAlias) {
        this(email, userName, userAlias, "", UserPrivacy.PUBLIC, UserFollowState.OWN, 0, 0, 0);
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

    public String getUserFollowState() {
        if (this.userFollowState == null) {
            return "";
        }

        return this.userFollowState.toString();
    }

    public int getFollowedUsersCount() {
        return this.followedUsersCount;
    }

    public int getFollowingUsersCount() {
        return this.followingUsersCount;
    }

    public int getUserReviewsCount() {
        return this.userReviewsCount;
    }

}
