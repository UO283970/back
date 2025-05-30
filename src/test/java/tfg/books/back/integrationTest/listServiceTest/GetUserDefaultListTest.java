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

import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class GetUserDefaultListTest {
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
    public void PU49_testGetUserDefaultList(){
        BookList basicListInto = listService.getUserDefaultList("userId1","0");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.getListId()).isEqualTo("0");
        assertThat(basicListInto.getListName()).isEqualTo(AppFirebaseConstants.DEFAULT_LISTS.get(0));
    }

    @Test
    public void PU50_testGetUserDefaultList(){
        BookList basicListInto = listService.getUserDefaultList("userId1","notAListId");

        assertThat(basicListInto).isNull();
    }

    @Test
    public void PU51_testGetUserDefaultList(){
        BookList basicListInto = listService.getUserDefaultList("notAUserId","0");

        assertThat(basicListInto).isNull();
    }

    @Test
    public void PU52_testGetUserDefaultList(){
        BookList basicListInto = listService.getUserDefaultList("","0");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.getListId()).isEqualTo("0");
        assertThat(basicListInto.getListName()).isEqualTo(AppFirebaseConstants.DEFAULT_LISTS.get(0));
    }

    @Test
    public void PU53_testGetUserDefaultList(){
        BookList basicListInto = listService.getUserDefaultList("userId4","0");

        assertThat(basicListInto).isNull();
    }

    @Test
    public void PU54_testGetUserDefaultList(){
        BookList basicListInto = listService.getUserDefaultList("userId3","0");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.getListId()).isEqualTo("0");
        assertThat(basicListInto.getListName()).isEqualTo(AppFirebaseConstants.DEFAULT_LISTS.get(0));
    }

    @Test
    public void PU55_testGetUserDefaultList(){
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId4");
        BookList basicListInto = listService.getUserDefaultList("userId4","0");

        assertThat(basicListInto).isNotNull();
        assertThat(basicListInto.getListId()).isEqualTo("0");
        assertThat(basicListInto.getListName()).isEqualTo(AppFirebaseConstants.DEFAULT_LISTS.get(0));
    }

    @Test
    public void PU56_testGetUserDefaultList(){
        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(listService)
                .getUserDefaultList("userId1","0");

        assertThrows(RuntimeException.class, () -> {
            listService.getUserDefaultList("userId1","0");
        });
    }

}
