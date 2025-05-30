package tfg.books.back.integrationTest;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.client.RestTemplate;
import tfg.books.back.firebase.AppFirebaseConstants;
import tfg.books.back.model.list.BookList.BookListPrivacy;
import tfg.books.back.model.notifications.NotificationsTypes;
import tfg.books.back.model.user.PassEncryption;
import tfg.books.back.model.user.User.UserPrivacy;
import tfg.books.back.model.userActivity.UserActivity.UserActivityType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@TestConfiguration
public class ConfigurationTest {
    private final Firestore firestore;
    private final PassEncryption passEncryption;

    public ConfigurationTest(Firestore firestore, PassEncryption passEncryption) {
        this.firestore = firestore;
        this.passEncryption = passEncryption;
    }

    @BeforeEach
    public void preloadData() throws ExecutionException, InterruptedException {
        for (CollectionReference collection : firestore.listCollections()) {
            firestore.recursiveDelete(collection).get();
        }
        cleanAuthEmulator();

        try {
            createUserInEmulator("test@test.com","Test1234$");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //User creation
        String userId1 = "userId1";
        createUser(userId1, UserPrivacy.PUBLIC);
        String userId2 = "userId2";
        createUser(userId2, UserPrivacy.PUBLIC);
        String userId3 = "userId3";
        createUser(userId3, UserPrivacy.PRIVATE);
        String userId4 = "userId4";
        createUser(userId4, UserPrivacy.PRIVATE);

        //List creation
        String listId1 = "listId1";
        createList(listId1, userId1, BookListPrivacy.PUBLIC);
        createDefaultLists(userId1);
        String listId2 = "listId2";
        createList(listId2, userId2, BookListPrivacy.ONLY_FOLLOWERS);
        createDefaultLists(userId2);
        String listId3 = "listId3";
        createList(listId3, userId3, BookListPrivacy.PRIVATE);
        createDefaultLists(userId3);
        String listId4 = "listId4";
        createList(listId4, userId4, BookListPrivacy.ONLY_FOLLOWERS);
        createDefaultLists(userId4);

        //Activities creation
        createActivity("", userId1, UserActivityType.RATING, Timestamp.parseTimestamp("2025-02-20T00:00" +
                ":00Z"), "bookId2");
        createActivity("Has text", userId1, UserActivityType.REVIEW, Timestamp.now(), "bookId");
        createActivity("Has text", userId1, UserActivityType.RATING,
                Timestamp.of(new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10))), "bookId3");
        createActivity("", userId2, UserActivityType.RATING, Timestamp.now(), "bookId");
        createActivity("Has text", userId2, UserActivityType.REVIEW, Timestamp.parseTimestamp("2025-02" +
                "-20T00:00:00Z"), "bookId2");

        //Book creation
        createBookFirebase("bookId", 5, 6);
        createBookFirebase("bookId2", 3, 7);
        createBookFirebase("bookId3", 5, 6);

        //Follow relationships
        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId1)
                .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(userId2).set(Collections.emptyMap()).get();
        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId2)
                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(userId1).set(Collections.emptyMap()).get();

        Map<String, Object> notification = Map.of("notificationType", NotificationsTypes.FOLLOWED, "timeStamp",
                Timestamp.now(), "userId", userId2);
        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId1)
                .collection(AppFirebaseConstants.USERS_NOTIFICATIONS_COLLECTION).document(userId1 + "|" + userId2 +
                        "|" + NotificationsTypes.FOLLOWED).set(notification).get();
        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId2)
                .collection(AppFirebaseConstants.USERS_NOTIFICATIONS_COLLECTION).document(userId2 + "|" + userId1 +
                        "|" + NotificationsTypes.FOLLOW).set(notification).get();


        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId1)
                .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(userId3).set(Collections.emptyMap()).get();
        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId3)
                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(userId1).set(Collections.emptyMap()).get();

        //Follow Request
        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId3)
                .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).document(userId1).set(Collections.emptyMap()).get();

        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId4)
                .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).document(userId1).set(Collections.emptyMap()).get();
    }

    public void cleanAuthEmulator() {
        String projectId = "project-id";

        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:9099/emulator/v1/projects/" + projectId + "/accounts";

        restTemplate.delete(url);
    }

    public void createUserInEmulator(String email, String password) throws IOException, InterruptedException {
        String url = "http://localhost:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key";
        String body = String.format("""
                {
                  "email": "%s",
                  "password": "%s",
                  "returnSecureToken": true
                }
                """, email, passEncryption.encrypt(password));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.body());
        }
    }

    private void createUser(String userId, UserPrivacy userPrivacy) {
        Map<String, Object> user = new HashMap<>();

        user.put("description", "This is a desc");
        user.put("email", "test@test.com");
        user.put("profilePictureURL", "userPhoto");
        user.put("userAlias", "userAliasTest".toLowerCase());
        user.put("userName", "userNameTest");
        user.put("userPrivacy", userPrivacy);

        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).set(user);
    }

    private void createList(String listId, String userId, BookListPrivacy bookListPrivacy) {
        Map<String, Object> list = new HashMap<>();

        list.put("bookListPrivacy", bookListPrivacy);
        list.put("description", "This is a desc");
        list.put("listName", "listNameTest" + userId);
        list.put("listUserId", userId);
        list.put("timestamp", Timestamp.now());

        firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId).set(list);

        Map<String, Object> books = new HashMap<>();
        books.put("timestamp", Timestamp.now());

        firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId)
                .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).document("bookId").set(books);

        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                .collection(AppFirebaseConstants.BOOKS_USER_LIST_RELATION_COLLECTION).document("bookId")
                .collection(AppFirebaseConstants.INSIDE_LISTS_BOOK_COLLECTION).document(listId).set(Collections.emptyMap());
    }

    private void createDefaultLists(String userId) {
        Map<String, Object> list = new HashMap<>();

        list.put("bookListPrivacy", BookListPrivacy.PUBLIC);
        list.put("description", "This is a desc");

        for (int i = 0; i < 5; i++) {
            list.put("listName", AppFirebaseConstants.DEFAULT_LISTS.get(i));

            firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                    .collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(String.valueOf(i)).set(list);

            Map<String, Object> books = new HashMap<>();
            books.put("timestamp", Timestamp.now());

            firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                    .collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(String.valueOf(i))
                    .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).document("bookId" + i).set(books);

            Map<String, Object> booksToDefault = new HashMap<>();
            booksToDefault.put("listId", String.valueOf(i));

            firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                    .collection(AppFirebaseConstants.BOOKS_DEFAULT_LIST_RELATION_COLLECTION)
                    .document("bookId" + i).set(booksToDefault);
        }
    }

    private void createActivity(String text, String userId, UserActivityType userActivityType,
                                Timestamp timestamp, String bookId) {
        Map<String, Object> activity = new HashMap<>();

        activity.put("activityText", text);
        activity.put("bookId", bookId);
        activity.put("score", 5);
        activity.put("timestamp", timestamp);
        activity.put("userActivityType", userActivityType);
        activity.put("userId", userId);

        firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).document(userId + "|" + bookId + "|" + userActivityType).set(activity);
    }

    private void createBookFirebase(String bookId, int score, int totalUsers) {
        Map<String, Object> book = new HashMap<>();

        book.put("score", score);
        book.put("totalUsers", totalUsers);

        firestore.collection(AppFirebaseConstants.BOOKS_COLLECTION).document(bookId).set(book);
    }


}
