package tfg.books.back.model;

import com.google.firebase.database.annotations.NotNull;

public class UserActivity {
    @NotNull
    private final String text;
    @NotNull
    private final int userScore;
    @NotNull
    private final User user;
    @NotNull
    private final Book book;

    public UserActivity(String text, int userScore, User user, Book book) {
        this.text = text;
        this.userScore = userScore;
        this.user = user;
        this.book = book;
    }

    public UserActivity() {
        this.text = "";
        this.userScore = -1;
        this.user = new User();
        this.book = new Book();
    }

    public String getText() {
        return text;
    }

    public int getUserScore() {
        return userScore;
    }

    public User getUser() {
        return user;
    }

    public Book getBook() {
        return book;
    }
}
