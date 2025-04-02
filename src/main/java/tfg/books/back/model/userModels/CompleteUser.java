package tfg.books.back.model.userModels;

import com.google.firebase.database.annotations.NotNull;
import tfg.books.back.model.list.BookList;

import java.util.List;

public record CompleteUser(@NotNull String email, @NotNull String userName, @NotNull String userAlias,
                           @NotNull String profilePictureURL, @NotNull String description,
                           @NotNull User.UserPrivacy userPrivacy, @NotNull User.UserFollowState userFollowState,
                           @NotNull List<String> followersUsers, @NotNull List<String> followingUsers,
                           @NotNull List<String> userActivities, @NotNull List<BookList> userDefaultLists,
                           @NotNull List<BookList> userLists, @NotNull List<String> followRequestList) {}
