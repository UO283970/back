package tfg.books.back.model.books;

import com.google.firebase.database.annotations.NotNull;

import java.util.List;

public record BookForSearch(@NotNull String title, @NotNull String bookId, @NotNull String publishYear,
                            @NotNull String author, @NotNull String coverImageURL, @NotNull String pages,
                            @NotNull String description, @NotNull List<String> subjects) {
}
