package tfg.books.back.model;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.database.annotations.NotNull;

public class BookList{
    public enum BookListprivacy{
        PRIVATE,
        PUBLIC,
        ONLYFOLLOWERS
    }


    @NotNull
    private String listName;
    @NotNull
    private List<String> bookListIds;
    @NotNull
    private String description;
    @NotNull
    private BookListprivacy bookListprivacy;

    public BookList(@NotNull String listName, @NotNull List<String> bookListIds, @NotNull String description,@NotNull BookListprivacy bookListprivacy) {
        this.listName = listName;
        this.bookListIds = bookListIds;
        this.description = description;
        this.bookListprivacy = bookListprivacy;
    }

    public BookList() {
        this.listName = "";
        this.bookListIds = new ArrayList<String>();
        this.description = "";
        this.bookListprivacy = BookListprivacy.PUBLIC;
    }

    public BookList(@NotNull  String listName, @NotNull List<String> bookListIds , @NotNull BookListprivacy bookListprivacy){
        this(listName,new ArrayList<String>(),"",bookListprivacy);
    }

    public BookListprivacy getBookListprivacy() {
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
