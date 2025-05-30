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
import tfg.books.back.firebase.AppFirebaseConstants;
import tfg.books.back.firebase.AuthenticatedUserIdProvider;
import tfg.books.back.graphQLControllers.ListGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;
import tfg.books.back.model.list.BookList;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class GetDefaultUserListsTest {

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
    public void PU42_testGetDefaultUserLists(){
        List<BookList> basicListInto = listService.getUserDefaultListsList("userId1");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.size()).isEqualTo(5);
        assertThat(basicListInto.get(0).getListId()).isEqualTo("0");
        assertThat(basicListInto.get(0).getListName()).isEqualTo(AppFirebaseConstants.DEFAULT_LISTS.get(0));
    }

    @Test
    public void PU43_testGetDefaultUserLists(){
        List<BookList> basicListInto = listService.getUserDefaultListsList("notAUserId");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.isEmpty()).isTrue();
    }

    @Test
    public void PU44_testGetDefaultUserLists(){
        List<BookList> basicListInto = listService.getUserDefaultListsList("");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.size()).isEqualTo(5);
        assertThat(basicListInto.get(0).getListId()).isEqualTo("0");
        assertThat(basicListInto.get(0).getListName()).isEqualTo(AppFirebaseConstants.DEFAULT_LISTS.get(0));
    }

    @Test
    public void PU45_testGetDefaultUserLists(){
        List<BookList> basicListInto = listService.getUserDefaultListsList("userId4");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.isEmpty()).isTrue();
    }

    @Test
    public void PU46_testGetDefaultUserLists(){
        List<BookList> basicListInto = listService.getUserDefaultListsList("userId3");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.size()).isEqualTo(5);
        assertThat(basicListInto.get(0).getListId()).isEqualTo("0");
        assertThat(basicListInto.get(0).getListName()).isEqualTo(AppFirebaseConstants.DEFAULT_LISTS.get(0));
    }

    @Test
    public void PU47_testGetDefaultUserLists(){
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId3");
        List<BookList> basicListInto = listService.getUserDefaultListsList("userId3");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.size()).isEqualTo(5);
        assertThat(basicListInto.get(0).getListId()).isEqualTo("0");
        assertThat(basicListInto.get(0).getListName()).isEqualTo(AppFirebaseConstants.DEFAULT_LISTS.get(0));
    }

    @Test
    public void PU48_testGetDefaultUserLists(){
        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(listService)
                .getUserDefaultListsList("userId1");

        assertThrows(RuntimeException.class, () -> {
            listService.getUserDefaultListsList("userId1");
        });
    }

}
