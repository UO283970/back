package tfg.books.back.services;

import com.google.cloud.firestore.*;
import com.google.firebase.database.annotations.NotNull;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import tfg.books.back.config.RestTemplateConfig;
import tfg.books.back.firebase.AppFirebaseConstants;
import tfg.books.back.firebase.AuthenticatedUserIdProvider;
import tfg.books.back.model.books.Book;
import tfg.books.back.model.books.BookCustomSerializer;
import tfg.books.back.model.books.ExtraInfoForBook;
import tfg.books.back.model.userActivity.UserActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Service
public class BookAPIService {

    private final Firestore firestore;
    private final AuthenticatedUserIdProvider authenticatedUserIdProvider;
    private final UserService userService;
    @Autowired
    RestTemplateConfig restTemplateConfig;

    public BookAPIService(Firestore firestore, AuthenticatedUserIdProvider authenticatedUserIdProvider,
                          UserService userService) {
        this.firestore = firestore;
        this.authenticatedUserIdProvider = authenticatedUserIdProvider;
        this.userService = userService;
    }

    public List<Book> searchBooks(@NotNull String userQuery, @NotNull String searchFor, @NotNull String subject) {
        List<Book> resultOfQueryBooks = new ArrayList<>();

        String url = "https://www.googleapis.com/books/v1/volumes?q=" + searchFor + "{userQuery}{subject}&printType" +
                "=books&orderBy" +
                "=relevance&key=AIzaSyBsCPK1yUlM5-Uq7yom_D74kNcJ9H2BP1M&startIndex=0&maxResults=10";

        String bookFromApi = restTemplateConfig.restTemplate().exchange(url.replace("{userQuery}", userQuery)
                        .replace("{subject}", subject),
                HttpMethod.GET, null, String.class).getBody();

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Book.class, new BookCustomSerializer());
        Gson gson = builder.create();

        assert bookFromApi != null;
        List<JsonElement> resultAsJSON =
                JsonParser.parseString(bookFromApi).getAsJsonObject().get("items").getAsJsonArray().asList();

        for (JsonElement bookDocs : resultAsJSON) {
            Book bookForSearch = gson.fromJson(bookDocs, Book.class);

            DocumentSnapshot bookDocumentRelation;
            try {
                bookDocumentRelation =
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(authenticatedUserIdProvider.getUserId())
                                .collection(AppFirebaseConstants.BOOKS_DEFAULT_LIST_RELATION_COLLECTION)
                                .document(bookForSearch.getBookId()).get().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            if (bookDocumentRelation.exists()) {
                bookForSearch.setReadingState(Book.ReadingState.valueOf(
                        AppFirebaseConstants.DEFAULT_LISTS.get(
                                Integer.parseInt(Objects.requireNonNull(bookDocumentRelation.getString("listId")))
                        )));
            } else {
                bookForSearch.setReadingState(Book.ReadingState.NOT_IN_LIST);
            }

            resultOfQueryBooks.add(bookForSearch);
        }

        return resultOfQueryBooks;
    }

    public List<Book> nextPageBooks(@NotNull String userQuery, @NotNull int page, @NotNull String searchFor,
                                    @NotNull String subject) {
        List<Book> resultOfQueryBooks = new ArrayList<>();

        String url = "https://www.googleapis.com/books/v1/volumes?q=" + searchFor + "{userQuery}{subject}&printType" +
                "=books&orderBy" +
                "=relevance&key=AIzaSyBsCPK1yUlM5-Uq7yom_D74kNcJ9H2BP1M&startIndex={page}&maxResults=15";

        String bookFromApi =
                restTemplateConfig.restTemplate().exchange(url.replace("{userQuery}", userQuery).replace("{page}",
                                Integer.toString(page * 10)).replace("{subject}", subject),
                        HttpMethod.GET, null, String.class).getBody();

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Book.class, new BookCustomSerializer());
        Gson gson = builder.create();

        assert bookFromApi != null;
        List<JsonElement> resultAsJSON =
                JsonParser.parseString(bookFromApi).getAsJsonObject().get("items").getAsJsonArray().asList();

        for (JsonElement bookDocs : resultAsJSON) {
            Book bookForSearch = gson.fromJson(bookDocs, Book.class);

            DocumentSnapshot bookDocumentRelation;
            try {
                bookDocumentRelation =
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(authenticatedUserIdProvider.getUserId())
                                .collection(AppFirebaseConstants.BOOKS_DEFAULT_LIST_RELATION_COLLECTION)
                                .document(bookForSearch.getBookId()).get().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            if (bookDocumentRelation.exists()) {
                bookForSearch.setReadingState(Book.ReadingState.valueOf(
                        AppFirebaseConstants.DEFAULT_LISTS.get(
                                Integer.parseInt(Objects.requireNonNull(bookDocumentRelation.getString("listId")))
                        )));
            } else {
                bookForSearch.setReadingState(Book.ReadingState.NOT_IN_LIST);
            }

            resultOfQueryBooks.add(bookForSearch);
        }

        return resultOfQueryBooks;
    }

    public ExtraInfoForBook getExtraInfoForBook(@NotNull String bookId) {
        String userId = authenticatedUserIdProvider.getUserId();

        try {
            QuerySnapshot bookActivityReference =
                    firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).whereEqualTo("bookId",
                            bookId).get().get();
            DocumentSnapshot bookReference =
                    firestore.collection(AppFirebaseConstants.BOOKS_COLLECTION).document(bookId).get().get();

            if (!bookActivityReference.isEmpty()) {
                Double score = 0.0;

                List<QueryDocumentSnapshot> document = firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).whereEqualTo(
                        "bookId",
                        bookId).whereEqualTo("userId", userId).get().get().getDocuments();

                if(!document.isEmpty()){
                    score = document.get(0).getDouble("score");
                }

                Double totalUsers = bookReference.getDouble("totalUsers");
                assert score != null;
                assert totalUsers != null;

                Double meanScore = score / totalUsers;

                List<String> userProfilePictures = new ArrayList<>();

                Query userFollowActivities =
                        firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                                .whereEqualTo("bookId", bookId).whereEqualTo("userActivityType",
                                        UserActivity.UserActivityType.REVIEW.toString()).orderBy("timestamp",
                                        Query.Direction.DESCENDING);

                int numberOfReviews = Long.valueOf(userFollowActivities.count().get().get().getCount()).intValue();

                for (QueryDocumentSnapshot userActivity : userFollowActivities.limit(4).get().get()) {
                    UserActivity newUserActivity = userActivity.toObject(UserActivity.class);
                    newUserActivity.setId(userActivity.getId());

                    userProfilePictures.add(userService.getUserMinimalInfo(newUserActivity.getUserId()).profilePictureURL());
                }

                int progress = -1;
                DocumentReference progressDocument = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.BOOKS_DEFAULT_LIST_RELATION_COLLECTION).document(bookId);
                if(progressDocument.get().get().exists()){
                    progressDocument.get().get().getDouble("progress");
                }

                return new ExtraInfoForBook(score.intValue(), meanScore, numberOfReviews, userProfilePictures,progress, totalUsers.intValue());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return new ExtraInfoForBook(0, 0.0, 0, List.of(), -1, 0);
    }

}
