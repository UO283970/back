package tfg.books.back.firebase;

import java.util.List;

public class AppFirebaseConstants{
    public static final String USERS_COLLECTION = "users";
    public static final String USERS_FOLLOWERS_COLLECTION = "followersUsers";
    public static final String USERS_FOLLOWING_COLLECTION = "followingUsers";
    public static final String USERS_DEFAULT_LISTS_COLLECTION = "userDefaultLists";
    public static final String USERS_REQUESTS_COLLECTION = "followRequestList";
    public static final String USERS_NOTIFICATIONS_COLLECTION = "notifications";
    public static final String LIST_COLLECTION = "lists";
    public static final String INSIDE_BOOKS_LIST_COLLECTION = "books";
    public static final String BOOKS_DEFAULT_LIST_RELATION_COLLECTION = "booksToDefaultList";
    public static final String BOOKS_USER_LIST_RELATION_COLLECTION = "booksToUserList";
    public static final String INSIDE_LISTS_BOOK_COLLECTION = "listsForBook";
    public static final String ACTIVITIES_COLLECTION = "activities";
    public static final String BOOKS_COLLECTION = "books";
    public static final List<String> DEFAULT_LISTS = List.of("READING","READ","PLAN_TO_READ","DROPPED","WAITING");
}
