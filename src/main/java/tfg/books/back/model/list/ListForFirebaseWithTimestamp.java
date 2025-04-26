package tfg.books.back.model.list;

import com.google.cloud.Timestamp;
import com.google.firebase.database.annotations.NotNull;

public record ListForFirebaseWithTimestamp(@NotNull String listName, @NotNull String description,
                                           @NotNull BookList.BookListPrivacy bookListPrivacy, @NotNull String listUserId, @NotNull
                                           Timestamp timestamp) {
}
