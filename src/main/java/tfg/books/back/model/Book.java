package tfg.books.back.model;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.database.annotations.NotNull;

public class Book {

    public enum ReadingState {
        NOT_IN_LIST,
        READING,
        DROPPED,
        WAITING,
        READ,
        PLAN_TO_READ
    }

    @NotNull
    private final String tittle;
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
    private final String imageUrl;

    public Book(@NotNull String tittle, @NotNull String author, @NotNull int pages, @NotNull Double meanScore,
            @NotNull int userScore, @NotNull List<String> subjects, @NotNull String details,
            @NotNull ReadingState readingState, @NotNull String imageUrl) {
        this.tittle = tittle;
        this.author = author;
        this.pages = pages;
        this.meanScore = meanScore;
        this.userScore = userScore;
        this.subjects = subjects;
        this.details = details;
        this.readingState = readingState;
        this.imageUrl = imageUrl;
    }

    public Book(@NotNull String tittle, @NotNull String author, @NotNull int pages, @NotNull Double meanScore, @NotNull ReadingState readingState, @NotNull String imageUrl){
        this(tittle,author,pages,meanScore, 0, new ArrayList<String>(), "",readingState,imageUrl);
    }

    public String getTittle() {
        return this.tittle;
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

    public String getImageUrl() {
        return this.imageUrl;
    }
    
}
