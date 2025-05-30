package tfg.books.back.integrationTest.userServiceTest;

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
import tfg.books.back.graphQLControllers.UserGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;
import tfg.books.back.model.user.UserForApp;

import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class GetAllUserInfoTest {
    private WireMockServer wireMockServer;

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private UserGraphQLController userService;

    @MockitoBean
    private AuthenticatedUserIdProvider authenticatedUserIdProvider;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        configurationTest.preloadData();

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
    public void PU126_GetMinUserInfo() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userId = "userId2";

        UserForApp authenticatedUserInfo = userService.getAllUserInfo(userId);

        assertThat(authenticatedUserInfo).isNotNull();
        assertThat(authenticatedUserInfo.userId()).isEqualTo("userId2");
        assertThat(authenticatedUserInfo.userAlias()).isEqualTo("userAliasTest".toLowerCase());
        assertThat(authenticatedUserInfo.userDefaultLists().isEmpty()).isFalse();
        assertThat(authenticatedUserInfo.userLists().isEmpty()).isFalse();

        userId = "userId4";
        authenticatedUserInfo = userService.getAllUserInfo(userId);

        assertThat(authenticatedUserInfo).isNotNull();
        assertThat(authenticatedUserInfo.userId()).isEqualTo("userId4");
        assertThat(authenticatedUserInfo.userAlias()).isEqualTo("userAliasTest".toLowerCase());
        assertThat(authenticatedUserInfo.userDefaultLists().isEmpty()).isTrue();
        assertThat(authenticatedUserInfo.userLists().isEmpty()).isTrue();
    }

    @Test
    public void PU130_GetMinUserInfo() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userId = "notAUserId";

        UserForApp authenticatedUserInfo = userService.getAllUserInfo(userId);

        assertThat(authenticatedUserInfo).isNull();
    }

    @Test
    public void PU131_GetMinUserInfo() {
        String userId = "userId2";

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .getAllUserInfo(userId);

        assertThrows(RuntimeException.class, () -> {
            userService.getAllUserInfo(userId);
        });
    }
}
