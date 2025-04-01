package tfg.books.back.model.userModels;

import com.google.firebase.database.annotations.NotNull;
import tfg.books.back.model.BookList;

import java.util.List;

public record UserForApp(@NotNull String id, @NotNull String userName, @NotNull String userAlias,
                         @NotNull String profilePictureURL, @NotNull String description,
                         @NotNull User.UserPrivacy userPrivacy, @NotNull int followersUsers,
                         @NotNull int followingUsers, @NotNull int userActivities,
                         @NotNull List<BookList> userDefaultLists, @NotNull List<BookList> userLists,
                         @NotNull User.UserFollowState userFollowState) {
}
