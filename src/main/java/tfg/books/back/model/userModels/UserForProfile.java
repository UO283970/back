package tfg.books.back.model.userModels;

import com.google.firebase.database.annotations.NotNull;

public record UserForProfile(@NotNull String id, @NotNull String userName, @NotNull String userAlias, @NotNull String profilePictureURL, @NotNull
                             User.UserFollowState userFollowState) {
}
