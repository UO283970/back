package tfg.books.back.model;

import com.google.firebase.database.annotations.NotNull;

public class BookList {
    @NotNull
    private String listName;
    @NotNull
    private int numberOfBooks;
    @NotNull
    private String description;

    public BookList(@NotNull String listName, @NotNull int numberOfBooks, @NotNull String description) {
        this.listName = listName;
        this.numberOfBooks = numberOfBooks;
        this.description = description;
    }

    public BookList(@NotNull  String listName,@NotNull int numberOfBooks){
        this(listName,numberOfBooks,"");
    }

    public String getListName() {
        return this.listName;
    }
    public int getNumberOfBooks() {
        return this.numberOfBooks;
    }

    public String getDescription() {
        return this.description;
    }

}
