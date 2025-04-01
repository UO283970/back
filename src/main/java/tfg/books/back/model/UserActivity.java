package tfg.books.back.model;

import com.google.firebase.database.annotations.NotNull;
import tfg.books.back.model.userModels.User;

public class UserActivity {
    @NotNull
    private final String text;
    @NotNull
    private final int userScore;
    @NotNull
    private final User user;
    @NotNull
    private final Book book;
    @NotNull
    private final UserActivityType userActivityType;


    public UserActivity(String text, int userScore, User user, Book book, UserActivityType userActivityType) {
        this.text = text;
        this.userScore = userScore;
        this.user = user;
        this.book = book;
        this.userActivityType = userActivityType;
    }

    public UserActivity() {
        this.text = "";
        this.userScore = -1;
        this.user = new User();
        this.book = new Book();
        this.userActivityType = UserActivityType.REVIEW;
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

    public enum UserActivityType{
        REVIEW,
        RATING
    }
}
