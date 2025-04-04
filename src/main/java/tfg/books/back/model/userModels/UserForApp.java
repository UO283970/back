package tfg.books.back.model.userModels;

import com.google.firebase.database.annotations.NotNull;
import tfg.books.back.model.list.BasicListInfo;
import tfg.books.back.model.list.BookList;

import java.util.List;

public record UserForApp(@NotNull String userId, @NotNull String userName, @NotNull String userAlias,
                         @NotNull String profilePictureURL, @NotNull String description,
                         @NotNull User.UserPrivacy userPrivacy, @NotNull int followedUsersCount,
                         @NotNull int followingUsersCount, @NotNull int userActivitiesCount,
                         @NotNull List<BookList> userDefaultLists, @NotNull List<BasicListInfo> userLists,
                         @NotNull User.UserFollowState userFollowState) {
}
