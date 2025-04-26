package tfg.books.back.model.userModels;

import com.google.firebase.database.annotations.NotNull;

public record UserForSearch(@NotNull String userId, @NotNull String userName, @NotNull String userAlias, @NotNull String profilePictureURL) {
}
