package tfg.books.back.model;

import com.google.firebase.database.annotations.NotNull;
import tfg.books.back.model.userModels.UserForSearch;

public class UserActivity {

    @NotNull
    private final String text;
    @NotNull
    private final int userScore;
    @NotNull
    private final UserForSearch user;
    @NotNull
    private final Book book;
    @NotNull
    private final UserActivityType userActivityType;
    @NotNull
    String id;


    public UserActivity(String text, int userScore, UserForSearch user, Book book, UserActivityType userActivityType) {
        this.text = text;
        this.userScore = userScore;
        this.user = user;
        this.book = book;
        this.userActivityType = userActivityType;
    }

    public UserActivity() {
        this.id = "";
        this.text = "";
        this.userScore = -1;
        this.user = null;
        this.book = new Book();
        this.userActivityType = UserActivityType.REVIEW;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public int getUserScore() {
        return userScore;
    }

    public UserForSearch getUser() {
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
