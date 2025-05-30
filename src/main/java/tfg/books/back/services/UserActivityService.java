package tfg.books.back.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.database.annotations.NotNull;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import tfg.books.back.config.RestTemplateConfig;
import tfg.books.back.firebase.AppFirebaseConstants;
import tfg.books.back.firebase.AuthenticatedUserIdProvider;
import tfg.books.back.model.books.Book;
import tfg.books.back.model.books.BookCustomSerializer;
import tfg.books.back.model.user.UserForProfile;
import tfg.books.back.model.userActivity.UserActivity;
import tfg.books.back.model.userActivity.UserActivity.UserActivityType;
import tfg.books.back.model.userActivity.UserActivityFirebase;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class UserActivityService {

    private final AuthenticatedUserIdProvider authenticatedUserIdProvider;
    private final Firestore firestore;
    private final UserService userService;
    private final RestTemplateConfig restTemplateConfig;
    private final String googleBooksBaseUrl;

    public UserActivityService(AuthenticatedUserIdProvider authenticatedUserIdProvider, Firestore firestore,
                               UserService userService,RestTemplateConfig restTemplateConfig,
                               @Value("${google.books.api.base-url:https://www.googleapis.com/books/v1/volumes/{bookId}}")
                               String googleBooksBaseUrl) {
        this.authenticatedUserIdProvider = authenticatedUserIdProvider;
        this.firestore = firestore;
        this.userService = userService;
        this.restTemplateConfig = restTemplateConfig;
        this.googleBooksBaseUrl = googleBooksBaseUrl;

    }

    public List<UserActivity> getAllFollowedActivity(@NotNull String timestamp) {
        String userId = authenticatedUserIdProvider.getUserId();
        List<UserForProfile> followingList = userService.getUsersListOFUser(userId,
                AppFirebaseConstants.USERS_FOLLOWING_COLLECTION);
        List<String> userIds = new ArrayList<>();

        for (UserForProfile user : followingList) {
            userIds.add(user.userId());
        }
        List<UserActivity> userFollowActivitiesForApp = new ArrayList<>();

        if (!userIds.isEmpty()) {
            try {
                Query userFollowActivities =
                        firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).whereIn("userId", userIds).orderBy("timestamp",
                                Query.Direction.DESCENDING).limit(10);

                if (!timestamp.isBlank()) {
                    userFollowActivities = userFollowActivities.startAfter(Timestamp.parseTimestamp(timestamp));
                }

                for (QueryDocumentSnapshot userActivity : userFollowActivities.get().get()) {
                    UserActivity newUserActivity = userActivity.toObject(UserActivity.class);
                    newUserActivity.setId(userActivity.getId());

                    newUserActivity.setLocalDateTime(LocalDateTime.ofInstant(newUserActivity.getTimestamp().toDate().toInstant(), ZoneId.systemDefault()).toString());

                    newUserActivity.setUser(userService.getUserMinimalInfo(newUserActivity.getUserId()));

                    String bookFromApi = restTemplateConfig.restTemplate().exchange(googleBooksBaseUrl.replace("{bookId}",
                                    newUserActivity.getBookId()),
                            HttpMethod.GET, null, String.class).getBody();

                    GsonBuilder builder = new GsonBuilder();
                    builder.registerTypeAdapter(Book.class, new BookCustomSerializer());
                    Gson gson = builder.create();

                    Book book = gson.fromJson(bookFromApi, Book.class);

                    book.setBookId(newUserActivity.getBookId());
                    newUserActivity.setBook(book);

                    userFollowActivitiesForApp.add(newUserActivity);

                }
                return userFollowActivitiesForApp;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return userFollowActivitiesForApp;
    }

    public List<UserActivity> getAllReviewsForBook(String bookId) {
        List<UserActivity> userFollowActivitiesForApp = new ArrayList<>();

        if (bookId.isBlank()) {
            return userFollowActivitiesForApp;
        }

        try {
            QuerySnapshot userFollowActivities =
                    firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                            .whereEqualTo("bookId", bookId).whereEqualTo("userActivityType",
                                    UserActivity.UserActivityType.REVIEW.toString()).orderBy("timestamp",
                                    Query.Direction.DESCENDING).get().get();

            for (QueryDocumentSnapshot userActivity : userFollowActivities) {
                UserActivity newUserActivity = userActivity.toObject(UserActivity.class);
                newUserActivity.setId(userActivity.getId());

                newUserActivity.setLocalDateTime(LocalDateTime.ofInstant(newUserActivity.getTimestamp().toDate().toInstant(), ZoneId.systemDefault()).toString());

                newUserActivity.setUser(userService.getUserMinimalInfo(newUserActivity.getUserId()));

                Book book = new Book();
                book.setBookId(newUserActivity.getBookId());
                newUserActivity.setBook(book);

                userFollowActivitiesForApp.add(newUserActivity);

            }


            return userFollowActivitiesForApp;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    public Boolean addActivity(@NotNull String activityText, @NotNull int score,
                               @NotNull String bookId, @NotNull UserActivityType userActivityType) {
        String userId = authenticatedUserIdProvider.getUserId();

        try {
            UserActivityType.valueOf(userActivityType.toString());
        } catch (IllegalArgumentException e) {
            return false;
        }

        if ((activityText.isBlank() && userActivityType.equals(UserActivityType.REVIEW)) || score < 0 || score > 10 || bookId.isBlank()) {
            return false;
        }

        String activityId = userId + "|" + bookId + "|" + userActivityType;

        try {
            if (userActivityType.equals(UserActivityType.RATING)) {
                DocumentReference activityExist =
                        firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).document(activityId);

                if (activityExist.get().get().exists()) {
                    if (score == 0) {
                        deleteActivity(activityId);
                    } else {
                        Integer lastScore = activityExist.get().get().get("score", int.class);
                        Timestamp timestamp = activityExist.get().get().getTimestamp("timestamp");
                        Timestamp timeNow = Timestamp.now();

                        assert timestamp != null;
                        if (timeNow.toDate().getTime() - timestamp.toDate().getTime() > TimeUnit.HOURS.toMillis(2)) {
                            activityExist.update("timestamp", timeNow);
                        }
                        activityExist.update("score", score);

                        assert lastScore != null;
                        updateBookScore(score, bookId, lastScore);
                    }

                    return true;
                } else {
                    String activityIdReviewCheck = userId + "|" + bookId + "|" + UserActivityType.REVIEW;

                    DocumentReference reviewExist =
                            firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).document(activityIdReviewCheck);

                    if (reviewExist.get().get().exists()) {
                        Integer lastScore = reviewExist.get().get().get("score", int.class);
                        Timestamp timestamp = reviewExist.get().get().getTimestamp("timestamp");
                        Timestamp timeNow = Timestamp.now();

                        assert timestamp != null;
                        if (timeNow.toDate().getTime() - timestamp.toDate().getTime() > TimeUnit.HOURS.toMillis(2)) {
                            reviewExist.update("timestamp", timeNow);
                        }
                        reviewExist.update("score", score);

                        assert lastScore != null;

                        DocumentReference bookExists =
                                firestore.collection(AppFirebaseConstants.BOOKS_COLLECTION).document(bookId);

                        if (bookExists.get().get().exists()) {
                            if (score != 0 && lastScore == 0) {
                                updateBookScore(score, bookId, lastScore);
                                bookExists.update("totalUsers", FieldValue.increment(1)).get();
                            } else if (lastScore != 0) {
                                updateBookScore(score, bookId, lastScore);
                            } else {
                                bookExists.update("score", FieldValue.increment(-lastScore)).get();
                                bookExists.update("totalUsers", FieldValue.increment(-1)).get();
                            }
                        } else if (score != 0) {
                            bookExists.set(Map.of("score", score,
                                    "totalUsers", 1)).get();
                        }

                        return true;
                    }
                }
            } else if (userActivityType.equals(UserActivityType.REVIEW)) {

                String activityIdCheck = userId + "|" + bookId + "|" + UserActivityType.RATING;

                DocumentReference ratingExists =
                        firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).document(activityIdCheck);

                if (ratingExists.get().get().exists()) {
                    deleteActivity(activityIdCheck);
                }
            }

            if (score != 0) {
                DocumentReference bookExists =
                        firestore.collection(AppFirebaseConstants.BOOKS_COLLECTION).document(bookId);
                if (bookExists.get().get().exists()) {
                    bookExists.update("score", FieldValue.increment(score));
                    bookExists.update("totalUsers", FieldValue.increment(1));
                } else {
                    bookExists.set(Map.of("score", score,
                            "totalUsers", 1));
                }
            }

            firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).document(activityId)
                    .create(new UserActivityFirebase(activityText, score, userId, bookId, userActivityType,
                            Timestamp.now())).get();

            return true;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateBookScore(int score, String bookId, int lastScore) {
        Double bookMeanScore;
        try {
            bookMeanScore =
                    firestore.collection(AppFirebaseConstants.BOOKS_COLLECTION).document(bookId).get().get().get(
                            "score", double.class);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        assert bookMeanScore != null;

        firestore.collection(AppFirebaseConstants.BOOKS_COLLECTION).document(bookId)
                .update("score", (bookMeanScore - lastScore) + score);
    }

    public Boolean deleteActivity(@NotNull String activityId) {
        DocumentReference docRef =
                firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).document(activityId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                String bookId = document.getString("bookId");
                Integer score = document.get("score", int.class);

                assert bookId != null;
                assert score != null;

                DocumentReference bookExists =
                        firestore.collection(AppFirebaseConstants.BOOKS_COLLECTION).document(bookId);
                if (bookExists.get().get().exists()) {
                    bookExists.update("score", FieldValue.increment(-score));
                    bookExists.update("totalUsers", FieldValue.increment(-1));
                }

                docRef.delete().get();
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return false;
    }
}
