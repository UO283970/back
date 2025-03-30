package tfg.books.back.model;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.database.annotations.NotNull;

public class BookList{
    public enum BookListPrivacy {
        PRIVATE,
        PUBLIC,
        ONLY_FOLLOWERS
    }


    @NotNull
    private final String listName;
    @NotNull
    private final List<String> bookListIds;
    @NotNull
    private final String description;
    @NotNull
    private final BookListPrivacy bookListprivacy;

    public BookList(@NotNull String listName, @NotNull List<String> bookListIds, @NotNull String description,@NotNull BookListPrivacy bookListprivacy) {
        this.listName = listName;
        this.bookListIds = bookListIds;
        this.description = description;
        this.bookListprivacy = bookListprivacy;
    }

    public BookList() {
        this.listName = "";
        this.bookListIds = new ArrayList<>();
        this.description = "";
        this.bookListprivacy = BookListPrivacy.PUBLIC;
    }

    public BookList(@NotNull  String listName, @NotNull List<String> bookListIds , @NotNull BookListPrivacy bookListprivacy){
        this(listName,bookListIds,"",bookListprivacy);
    }

    public BookListPrivacy getBookListprivacy() {
        return this.bookListprivacy;
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

}
