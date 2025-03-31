package tfg.books.back.model;

import com.google.firebase.database.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BookList{
    @NotNull
    private final String userId;
    @NotNull
    private final String listName;
    @NotNull
    private final List<String> bookListIds;
    @NotNull
    private final String description;
    @NotNull
    private final BookListPrivacy bookListPrivacy;
    public BookList(@NotNull String userId,@NotNull String listName, @NotNull List<String> bookListIds, @NotNull String description,@NotNull BookListPrivacy bookListprivacy) {
        this.userId = userId;
        this.listName = listName;
        this.bookListIds = bookListIds;
        this.description = description;
        this.bookListPrivacy = bookListprivacy;
    }

    public BookList() {
        this.userId = "";
        this.listName = "";
        this.bookListIds = new ArrayList<>();
        this.description = "";
        this.bookListPrivacy = BookListPrivacy.PUBLIC;
    }

    public BookList(@NotNull String userId,@NotNull  String listName, @NotNull List<String> bookListIds , @NotNull BookListPrivacy bookListprivacy){
        this(userId,listName,bookListIds,"",bookListprivacy);
    }

    public String getUserId() {
        return userId;
    }

    public List<String> getBookListIds() {
        return bookListIds;
    }

    public BookListPrivacy getBookListPrivacy() {
        return bookListPrivacy;
    }

    public BookListPrivacy getBookListprivacy() {
        return this.bookListPrivacy;
    }

    public String getListName() {
        return this.listName;
    }

    public List<String> getNumberOfBooks() {
        return this.bookListIds;
    }

    public String getDescription() {
        return this.description;
    }

    public enum BookListPrivacy {
        PRIVATE,
        PUBLIC,
        ONLY_FOLLOWERS
    }

}
