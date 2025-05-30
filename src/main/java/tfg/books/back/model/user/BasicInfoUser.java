package tfg.books.back.model.user;

import com.google.firebase.database.annotations.NotNull;

public record BasicInfoUser(@NotNull String email, @NotNull String userName, @NotNull String userAlias,
                            @NotNull String profilePictureURL, @NotNull String description,
                            @NotNull User.UserPrivacy userPrivacy) {
}
