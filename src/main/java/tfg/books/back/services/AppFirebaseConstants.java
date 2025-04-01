package tfg.books.back.services;

import java.util.List;

public class AppFirebaseConstants{
    public static final String USERS_COLLECTION = "users";
    public static final String USERS_FOLLOWERS_COLLECTION = "followersUsers";
    public static final String USERS_FOLLOWING_COLLECTION = "followingUsers";
    public static final String USERS_ACTIVITIES_COLLECTION = "userActivities";
    public static final String USERS_DEFAULT_LISTS_COLLECTION = "userDefaultLists";
    public static final String USERS_LISTS_COLLECTION = "userLists";
    public static final String USERS_REQUESTS_COLLECTION = "followRequestLists";
    public static final String LIST_COLLECTION = "lists";
    public static final String INSIDE_BOOKS_LIST_COLLECTION = "books";
    public static final String ACTIVITIES_COLLECTION = "activities";
    public static final List<String> DEFAULT_LISTS = List.of("READING","DROPPED","WAITING","READ","PLAN_TO_READ");
}
