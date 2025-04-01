package tfg.books.back.model;

import com.google.firebase.database.annotations.NotNull;

public class BookList{
    @NotNull
    private final String userId;
    @NotNull
    private final String listName;
    @NotNull
    private final String description;
    @NotNull
    private final BookListPrivacy bookListPrivacy;
    public BookList(@NotNull String userId,@NotNull String listName, @NotNull String description,@NotNull BookListPrivacy bookListprivacy) {
        this.userId = userId;
        this.listName = listName;
        this.description = description;
        this.bookListPrivacy = bookListprivacy;
    }

    public BookList() {
        this.userId = "";
        this.listName = "";
        this.description = "";
        this.bookListPrivacy = BookListPrivacy.PUBLIC;
    }

    public BookList(@NotNull String userId,@NotNull  String listName , @NotNull BookListPrivacy bookListprivacy){
        this(userId,listName,"",bookListprivacy);
    }

    public String getUserId() {
        return userId;
    }

    public BookListPrivacy getBookListPrivacy() {
        return bookListPrivacy;
    }

    public String getListName() {
        return this.listName;
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
