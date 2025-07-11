package tfg.books.back.model.userActivity;

import com.google.cloud.Timestamp;
import com.google.firebase.database.annotations.NotNull;
import tfg.books.back.model.books.Book;
import tfg.books.back.model.user.UserForSearch;

public class UserActivity {

    @NotNull
    private final String activityText;
    @NotNull
    private final int score;
    @NotNull
    private final UserActivityType userActivityType;
    @NotNull
    String activityId;
    @NotNull
    private String userId;
    @NotNull
    private Timestamp timestamp;
    @NotNull
    private String bookId;
    @NotNull
    private String localDateTime;
    @NotNull
    private UserForSearch user;
    @NotNull
    private Book book;


    public UserActivity(String text, String userId, String bookId, int userScore, UserForSearch user, Book book, UserActivityType userActivityType, Timestamp timestamp) {
        this.activityText = text;
        this.userId = userId;
        this.bookId = bookId;
        this.score = userScore;
        this.user = user;
        this.book = book;
        this.userActivityType = userActivityType;
        this.timestamp = timestamp;
    }

    public UserActivity() {
        this.timestamp = Timestamp.now();
        this.bookId = "";
        this.userId = "";
        this.activityId = "";
        this.activityText = "";
        this.score = -1;
        this.user = null;
        this.book = new Book();
        this.userActivityType = UserActivityType.REVIEW;
    }

    public void setId(String id) {
        this.activityId = id;
    }

    public String getActivityText() {
        return activityText;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getScore() {
        return score;
    }

    public UserForSearch getUser() {
        return user;
    }

    public void setUser(UserForSearch user) {
        this.user = user;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public UserActivityType getUserActivityType() {
        return userActivityType;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(String localDateTime) {
        this.localDateTime = localDateTime;
    }

    public enum UserActivityType{
        REVIEW,
        RATING
    }
}
