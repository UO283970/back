package tfg.books.back.model;

import com.google.firebase.database.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Book {

    @NotNull
    private final String bookId;
    @NotNull
    private final String title;
    @NotNull
    private final String author;
    @NotNull
    private final int pages;
    @NotNull
    private final Double meanScore;
    @NotNull
    private final int userScore;
    @NotNull
    private final List<String> subjects;
    @NotNull
    private final String details;
    @NotNull
    private final ReadingState readingState;
    @NotNull
    private final String coverImageURL;
    public Book(String bookId, @NotNull String tittle, @NotNull String author, @NotNull int pages, @NotNull Double meanScore,
                @NotNull int userScore, @NotNull List<String> subjects, @NotNull String details,
                @NotNull ReadingState readingState, @NotNull String imageUrl) {
        this.bookId = bookId;
        this.title = tittle;
        this.author = author;
        this.pages = pages;
        this.meanScore = meanScore;
        this.userScore = userScore;
        this.subjects = subjects;
        this.details = details;
        this.readingState = readingState;
        this.coverImageURL = imageUrl;
    }

    public Book(@NotNull String tittle, @NotNull String author, @NotNull int pages, @NotNull Double meanScore, @NotNull ReadingState readingState, @NotNull String imageUrl, String bookId){
        this(bookId, tittle,author,pages,meanScore, 0, new ArrayList<String>(), "",readingState,imageUrl);
    }

    public Book() {
        this.bookId = "";
        this.title = "";
        this.author = "";
        this.pages = 0;
        this.meanScore = 0.0;
        this.userScore = 0;
        this.subjects = new ArrayList<>();
        this.details = "";
        this.readingState = ReadingState.NOT_IN_LIST;
        this.coverImageURL = "";
    }

    public String getTitle() {
        return this.title;
    }

    public String getAuthor() {
        return this.author;
    }

    public int getPages() {
        return this.pages;
    }

    public Double getMeanScore() {
        return this.meanScore;
    }

    public int getUserScore() {
        return this.userScore;
    }

    public List<String> getSubjects() {
        return this.subjects;
    }

    public String getDetails() {
        return this.details;
    }

    public ReadingState getReadingState() {
        return this.readingState;
    }

    public String getCoverImageURL() {
        return this.coverImageURL;
    }

    public enum ReadingState {
        NOT_IN_LIST,
        READING,
        DROPPED,
        WAITING,
        READ,
        PLAN_TO_READ
    }
    
}
