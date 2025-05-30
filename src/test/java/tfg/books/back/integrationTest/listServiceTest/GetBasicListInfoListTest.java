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
import tfg.books.back.model.list.BookList.BookListPrivacy;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class GetBasicListInfoListTest {

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
    public void PU30_testGetBasicListInfoList(){
        List<BookList> basicListInto = listService.getBasicListInfoList("userId1");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.size()).isEqualTo(1);
        assertThat(basicListInto.get(0).getListId()).isEqualTo("listId1");
    }

    @Test
    public void PU31_testGetBasicListInfoList(){
        List<BookList> basicListInto = listService.getBasicListInfoList("userId4");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.isEmpty()).isTrue();
    }

    @Test
    public void PU32_testGetBasicListInfoList(){
        List<BookList> basicListInto = listService.getBasicListInfoList("notAUser");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.isEmpty()).isTrue();
    }

    @Test
    public void PU33_testGetBasicListInfoList(){
        List<BookList> basicListInto = listService.getBasicListInfoList("");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.size()).isEqualTo(1);
        assertThat(basicListInto.get(0).getListId()).isEqualTo("listId1");
    }

    @Test
    public void PU34_testGetBasicListInfoList(){
        List<BookList> basicListInto = listService.getBasicListInfoList("userId2");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.size()).isEqualTo(1);
        assertThat(basicListInto.get(0).getListId()).isEqualTo("listId2");
        assertThat(basicListInto.get(0).getBookListPrivacy()).isEqualTo(BookListPrivacy.ONLY_FOLLOWERS);
    }

    @Test
    public void PU35_testGetBasicListInfoList(){
        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(listService)
                .getBasicListInfoList("userId");

        assertThrows(RuntimeException.class, () -> {
            listService.getBasicListInfoList("userId");
        });
    }

}
