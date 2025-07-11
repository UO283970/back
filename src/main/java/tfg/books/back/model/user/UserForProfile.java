package tfg.books.back.model.user;

import com.google.firebase.database.annotations.NotNull;

public record UserForProfile(@NotNull String userId, @NotNull String userName, @NotNull String userAlias, @NotNull String profilePictureURL, @NotNull
                             User.UserFollowState userFollowState) {
}
