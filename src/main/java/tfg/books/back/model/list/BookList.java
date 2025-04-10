package tfg.books.back.model.list;

import com.google.firebase.database.annotations.NotNull;
import tfg.books.back.model.books.Book;

import java.util.ArrayList;
import java.util.List;

public class BookList {

    @NotNull
    private final String listUserId;
    @NotNull
    private final String listName;
    @NotNull
    private final String description;
    @NotNull
    private final BookListPrivacy bookListPrivacy;
    @NotNull
    private String listImage;
    @NotNull
    private List<Book> listOfBooks;
    @NotNull
    private String listId;
    @NotNull
    private int numberOfBooks;

    public BookList(@NotNull String listId, @NotNull String listUserId, @NotNull String listName,
                    @NotNull String description,
                    @NotNull BookListPrivacy bookListprivacy, @NotNull List<Book> books) {
        this.listId = listId;
        this.listUserId = listUserId;
        this.listName = listName;
        this.description = description;
        this.bookListPrivacy = bookListprivacy;
        this.listOfBooks = books;
        this.listImage = "";
    }

    public BookList() {
        this.listId = "";
        this.listUserId = "";
        this.listName = "";
        this.description = "";
        this.bookListPrivacy = BookListPrivacy.PUBLIC;
        this.listOfBooks = new ArrayList<>();
        this.listImage = "";
    }

    public String getListId() {
        return listId;
    }

    public void setListId(@NotNull String listId) {
        this.listId = listId;
    }

    public String getListUserId() {
        return listUserId;
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

    public List<Book> getListOfBooks() {
        return listOfBooks;
    }

    public void setListOfBooks(List<Book> listOfBooks) {
        this.listOfBooks = listOfBooks;
    }

    public int getNumberOfBooks() {
        return numberOfBooks;
    }

    public void setNumberOfBooks(int numberOfBooks) {
        this.numberOfBooks = numberOfBooks;
    }

    public String getListImage() {
        return listImage;
    }

    public void setListImage(String listImage) {
        this.listImage = listImage;
    }

    public enum BookListPrivacy {
        PRIVATE,
        PUBLIC,
        ONLY_FOLLOWERS
    }

}
