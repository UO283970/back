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
import tfg.books.back.model.books.Book.ReadingState;

import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class AddBookToDefaultListTest {
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
    public void PU87_testAddBookToDefaultList() throws ExecutionException, InterruptedException {
        String listId = "0";
        String bookId = "newBookId";

        ReadingState addBookToDefaultList = listService.addBookToDefaultList(listId,bookId);

        assertThat(addBookToDefaultList).isNotNull();
        assertThat(addBookToDefaultList.toString()).isEqualTo(AppFirebaseConstants.DEFAULT_LISTS.get(0));

        DocumentSnapshot doc = firestore.collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1").
                collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION)
                .document(listId)
                .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION)
                .document(bookId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();

        DocumentSnapshot doc2 =  firestore.collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .collection(AppFirebaseConstants.BOOKS_DEFAULT_LIST_RELATION_COLLECTION)
                .document(bookId)
                .get()
                .get();

        assertThat(doc2.exists()).isTrue();
    }

    @Test
    public void PU88_testAddBookToDefaultList(){
        String listId = "notAListId";
        String bookId = "newBookId";

        ReadingState addBookToDefaultList = listService.addBookToDefaultList(listId,bookId);

        assertThat(addBookToDefaultList).isNotNull();
        assertThat(addBookToDefaultList).isEqualTo(ReadingState.NOT_IN_LIST);
    }

    @Test
    public void PU89_testAddBookToDefaultList() {
        String listId = "notAListId";
        String bookId = "newBookId";
        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(listService)
                .addBookToDefaultList(listId,bookId);

        assertThrows(RuntimeException.class, () -> {
            listService.addBookToDefaultList(listId,bookId);
        });
    }
}
