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
import tfg.books.back.model.list.BookList;

import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class UpdateListTest {
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
    public void PU77_testUpdateList() throws ExecutionException, InterruptedException {
        String listId = "listId1";
        String listName = "newListName";
        String listDesc = "newListDesc";
        BookList.BookListPrivacy bookListPrivacy= BookList.BookListPrivacy.PUBLIC;

        Boolean updateList = listService.updateList(listId,listName,listDesc,bookListPrivacy);

        assertThat(updateList).isNotNull();
        assertThat(updateList).isTrue();

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.LIST_COLLECTION)
                .document(listId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        assertThat(doc.getString("listUserId")).isEqualTo("userId1");
        assertThat(doc.getString("listName")).isEqualTo("newListName");
        assertThat(doc.getString("description")).isEqualTo("newListDesc");
    }

    @Test
    public void PU78_testUpdateList(){
        String listId = "listId1";
        String listName = "newListName";
        String listDesc = "newListDesc";
        BookList.BookListPrivacy bookListPrivacy = mock(BookList.BookListPrivacy.class);
        when(bookListPrivacy.toString()).thenReturn("NOT_CREATED");

        Boolean updateList = listService.updateList(listId, listName, listDesc, bookListPrivacy);

        assertThat(updateList).isNotNull();
        assertThat(updateList).isFalse();
    }

    @Test
    public void PU79_testUpdateList(){
        String listId = "listId1";
        String listName = "";
        String listDesc = "newListDesc";
        BookList.BookListPrivacy bookListPrivacy= BookList.BookListPrivacy.PUBLIC;

        Boolean updateList = listService.updateList(listId, listName, listDesc, bookListPrivacy);

        assertThat(updateList).isNotNull();
        assertThat(updateList).isFalse();
    }

    @Test
    public void PU80_testUpdateList(){
        String listId = "listId1";
        String listName = "listNameTestuserId1";
        String listDesc = "newListDesc";
        BookList.BookListPrivacy bookListPrivacy= BookList.BookListPrivacy.PUBLIC;

        Boolean updateList = listService.updateList(listId, listName, listDesc, bookListPrivacy);

        assertThat(updateList).isNotNull();
        assertThat(updateList).isFalse();
    }



    @Test
    public void PU81_testUpdateList() throws ExecutionException, InterruptedException {
        String listId = "listId1";
        String listName = "listNameTestuserId2";
        String listDesc = "newListDesc";
        BookList.BookListPrivacy bookListPrivacy= BookList.BookListPrivacy.PUBLIC;

        Boolean updateList = listService.updateList(listId,listName,listDesc,bookListPrivacy);

        assertThat(updateList).isNotNull();
        assertThat(updateList).isTrue();

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.LIST_COLLECTION)
                .document(listId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        assertThat(doc.getString("listUserId")).isEqualTo("userId1");
        assertThat(doc.getString("listName")).isEqualTo(listName);
        assertThat(doc.getString("description")).isEqualTo(listDesc);
    }

    @Test
    public void PU82_testUpdateList() {
        String listId = "listId1";
        String listName = "listNameTestuserId2";
        String listDesc = "newListDesc";
        BookList.BookListPrivacy bookListPrivacy= BookList.BookListPrivacy.PUBLIC;

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(listService)
                .updateList(listId,listName,listDesc,bookListPrivacy);

        assertThrows(RuntimeException.class, () -> {
            listService.updateList(listId,listName,listDesc,bookListPrivacy);
        });
    }
}
