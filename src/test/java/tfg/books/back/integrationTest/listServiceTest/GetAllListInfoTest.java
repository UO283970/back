package tfg.books.back.integrationTest.listServiceTest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
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
import tfg.books.back.firebase.AuthenticatedUserIdProvider;
import tfg.books.back.graphQLControllers.ListGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;
import tfg.books.back.model.list.BookList;

import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class GetAllListInfoTest {
    private WireMockServer wireMockServer;

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private ListGraphQLController listService;

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
    public void PU36_testGetAllListInfo(){
        BookList allListInfo = listService.getAllListInfo("listId1");

        assertThat(allListInfo).isNotNull();
        assertThat(allListInfo.getListId()).isEqualTo("listId1");
        assertThat(allListInfo.getListOfBooks().size()).isEqualTo(1);
        assertThat(allListInfo.getListOfBooks().get(0).getBookId()).isEqualTo("bookId");
    }

    @Test
    public void PU37_testGetAllListInfo(){
        BookList allListInfo = listService.getAllListInfo("notAnListId");

        assertThat(allListInfo).isNull();
    }

    @Test
    public void PU38_testGetAllListInfo(){
        BookList allListInfo = listService.getAllListInfo("listId4");

        assertThat(allListInfo).isNull();
    }

    @Test
    public void PU39_testGetAllListInfo(){
        BookList allListInfo = listService.getAllListInfo("listId2");

        assertThat(allListInfo).isNotNull();
        assertThat(allListInfo.getListId()).isEqualTo("listId2");
        assertThat(allListInfo.getListOfBooks().size()).isEqualTo(1);
        assertThat(allListInfo.getListOfBooks().get(0).getBookId()).isEqualTo("bookId");
    }

    @Test
    public void PU40_testGetAllListInfo(){
        BookList allListInfo = listService.getAllListInfo("listId3");

        assertThat(allListInfo).isNull();
    }

    @Test
    public void PU41_testGetAllListInfo(){
        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(listService)
                .getAllListInfo("listId3");

        assertThrows(RuntimeException.class, () -> {
            listService.getAllListInfo("listId3");
        });
    }
}
