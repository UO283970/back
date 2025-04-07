package tfg.books.back.model.list;

import com.google.firebase.database.annotations.NotNull;
import tfg.books.back.model.Book;

import java.util.ArrayList;
import java.util.List;

public class BookList {

    @NotNull
    private final String userId;
    @NotNull
    private final String listName;
    @NotNull
    private final String description;
    @NotNull
    private final String listImage;
    @NotNull
    private final BookListPrivacy bookListPrivacy;
    @NotNull
    private final List<Book> books;
    @NotNull
    private String listId;
    @NotNull
    private int numberOfBooks;

    public BookList(@NotNull String listId,@NotNull String userId, @NotNull String listName, @NotNull String description,
                    @NotNull BookListPrivacy bookListprivacy, @NotNull List<Book> books) {
        this.listId = listId;
        this.userId = userId;
        this.listName = listName;
        this.description = description;
        this.bookListPrivacy = bookListprivacy;
        this.books = books;
        this.listImage = "";
    }

    public BookList() {
        this.listId = "";
        this.userId = "";
        this.listName = "";
        this.description = "";
        this.bookListPrivacy = BookListPrivacy.PUBLIC;
        this.books = new ArrayList<>();
        this.listImage = "";
    }

    public String getListId() {
        return listId;
    }

    public void setListId(@NotNull String listId){
        this.listId = listId;
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

    public void setNumberOfBooks(int numberOfBooks) {
        this.numberOfBooks = numberOfBooks;
    }

    public enum BookListPrivacy {
        PRIVATE,
        PUBLIC,
        ONLY_FOLLOWERS
    }

}
