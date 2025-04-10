package tfg.books.back.services;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Service
public class BookAPIService {

    private final Firestore firestore;
    private final AuthenticatedUserIdProvider authenticatedUserIdProvider;
    @Autowired
    RestTemplateConfig restTemplateConfig;

    public BookAPIService(Firestore firestore, AuthenticatedUserIdProvider authenticatedUserIdProvider) {
        this.firestore = firestore;
        this.authenticatedUserIdProvider = authenticatedUserIdProvider;
    }


    public List<Book> searchBooks(@NotNull String userQuery) {
        List<Book> resultOfQueryBooks = new ArrayList<>();

        String url = "https://www.googleapis.com/books/v1/volumes?q=intitle:{userQuery}&printType=books&orderBy" +
                "=relevance&key=AIzaSyBsCPK1yUlM5-Uq7yom_D74kNcJ9H2BP1M";

        String bookFromApi = restTemplateConfig.restTemplate().exchange(url.replace("{userQuery}", userQuery),
                HttpMethod.GET, null, String.class).getBody();

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Book.class, new BookCustomSerializer());
        Gson gson = builder.create();

        assert bookFromApi != null;
        List<JsonElement> resultAsJSON =
                JsonParser.parseString(bookFromApi).getAsJsonObject().get("items").getAsJsonArray().asList();

        for (JsonElement bookDocs : resultAsJSON) {
            Book bookForSearch = gson.fromJson(bookDocs, Book.class);

            DocumentSnapshot bookDocumentRelation = null;
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
            }else{
                bookForSearch.setReadingState(Book.ReadingState.NOT_IN_LIST);
            }

            resultOfQueryBooks.add(bookForSearch);
        }

        return resultOfQueryBooks;
    }

}
