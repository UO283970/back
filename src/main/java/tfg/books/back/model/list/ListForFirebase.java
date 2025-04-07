package tfg.books.back.model.list;

import com.google.firebase.database.annotations.NotNull;

public record ListForFirebase(@NotNull String listName, @NotNull String description,
                              @NotNull BookList.BookListPrivacy bookListPrivacy, @NotNull String listUserId) {
}
