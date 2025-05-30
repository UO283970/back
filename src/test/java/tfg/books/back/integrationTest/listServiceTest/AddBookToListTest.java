package tfg.books.back.integrationTest.listServiceTest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tfg.books.back.BackApplication;
import tfg.books.back.FirebaseTestConfig;
import tfg.books.back.firebase.AppFirebaseConstants;
import tfg.books.back.firebase.AuthenticatedUserIdProvider;
import tfg.books.back.graphQLControllers.ListGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class AddBookToListTest {
    private WireMockServer wireMockServer;

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private ListGraphQLController listService;

    @MockitoSpyBean
    private Firestore firestore;

    @MockitoBean
    private AuthenticatedUserIdProvider authenticatedUserIdProvider;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        configurationTest.preloadData();
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();

        WireMock.configureFor("localhost", 8089);

        wireMockServer.stubFor(get(urlPathMatching("/books/v1/volumes/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"id\": \"bookId\", \"volumeInfo\": { \"title\": \"Fake Book Title\" } }")));
    }

    @AfterEach
    public void stop() {
        wireMockServer.stop();
    }

    @Test
    public void PU93_testAddBookToList() throws ExecutionException, InterruptedException {
        String listId = "listId1";
        String bookId = "newBookId";

        Boolean addBookToList = listService.addBookToList(List.of(listId),bookId);

        assertThat(addBookToList).isNotNull();
        assertThat(addBookToList).isTrue();

        DocumentSnapshot doc = firestore.collection(AppFirebaseConstants.LIST_COLLECTION)
                .document(listId).
                collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION)
                .document(bookId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();

        DocumentSnapshot doc2 =  firestore.collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .collection(AppFirebaseConstants.BOOKS_USER_LIST_RELATION_COLLECTION)
                .document(bookId)
                .collection(AppFirebaseConstants.INSIDE_LISTS_BOOK_COLLECTION)
                .document(listId)
                .get()
                .get();

        assertThat(doc2.exists()).isTrue();
    }

    @Test
    public void PU94_testAddBookToList() throws ExecutionException, InterruptedException {
        String listId = "listId1";
        String listId2 = "notAList";
        String bookId = "newBookId";

        Boolean addBookToList = listService.addBookToList(List.of(listId,listId2),bookId);

        assertThat(addBookToList).isNotNull();
        assertThat(addBookToList).isTrue();

        DocumentSnapshot doc = firestore.collection(AppFirebaseConstants.LIST_COLLECTION)
                .document(listId).
                collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION)
                .document(bookId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();

        DocumentSnapshot doc2 =  firestore.collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .collection(AppFirebaseConstants.BOOKS_USER_LIST_RELATION_COLLECTION)
                .document(bookId)
                .collection(AppFirebaseConstants.INSIDE_LISTS_BOOK_COLLECTION)
                .document(listId)
                .get()
                .get();

        assertThat(doc2.exists()).isTrue();
    }

    @Test
    public void PU95_testAddBookToList() throws ExecutionException, InterruptedException {
        String listId = "listId1";
        String listId2 = "listId2";
        String bookId = "newBookId";

        Boolean addBookToList = listService.addBookToList(List.of(listId,listId2),bookId);

        assertThat(addBookToList).isNotNull();
        assertThat(addBookToList).isTrue();

        DocumentSnapshot doc = firestore.collection(AppFirebaseConstants.LIST_COLLECTION)
                .document(listId).
                collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION)
                .document(bookId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();

        DocumentSnapshot doc2 =  firestore.collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .collection(AppFirebaseConstants.BOOKS_USER_LIST_RELATION_COLLECTION)
                .document(bookId)
                .collection(AppFirebaseConstants.INSIDE_LISTS_BOOK_COLLECTION)
                .document(listId)
                .get()
                .get();

        assertThat(doc2.exists()).isTrue();


        DocumentSnapshot doc3 = firestore.collection(AppFirebaseConstants.LIST_COLLECTION)
                .document(listId2).
                collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION)
                .document(bookId)
                .get()
                .get();

        assertThat(doc3.exists()).isFalse();
    }



    @Test
    public void PU96_testAddBookToList() {
        String listId = "listId1";
        String listId2 = "listId2";
        String bookId = "newBookId";

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(listService)
                .addBookToList(List.of(listId,listId2),bookId);

        assertThrows(RuntimeException.class, () -> {
            listService.addBookToList(List.of(listId,listId2),bookId);
        });
    }
}
