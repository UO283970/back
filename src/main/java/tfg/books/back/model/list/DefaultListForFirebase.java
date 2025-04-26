package tfg.books.back.model.list;

import com.google.firebase.database.annotations.NotNull;

public record DefaultListForFirebase(@NotNull String listName, @NotNull String description,
                                     @NotNull BookList.BookListPrivacy bookListPrivacy) {
}
