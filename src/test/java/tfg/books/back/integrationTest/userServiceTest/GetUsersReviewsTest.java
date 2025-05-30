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
import tfg.books.back.model.userActivity.UserActivity;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class GetUsersReviewsTest {
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
    public void PU142_GetUsersReviews() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userId = "userId1";

        List<UserActivity> userReviews = userService.getUsersReviews(userId);

        assertThat(userReviews).isNotNull();
        assertThat(userReviews.isEmpty()).isFalse();
        assertThat(userReviews.size()).isEqualTo(1);
    }

    @Test
    public void PU143_GetUsersReviews() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userId = "userId3";

        List<UserActivity> userReviews = userService.getUsersReviews(userId);

        assertThat(userReviews).isNotNull();
        assertThat(userReviews.isEmpty()).isTrue();
    }

    @Test
    public void PU144_GetUsersReviews() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userId = "";

        List<UserActivity> userReviews = userService.getUsersReviews(userId);

        assertThat(userReviews).isNotNull();
        assertThat(userReviews.isEmpty()).isFalse();
        assertThat(userReviews.size()).isEqualTo(1);
    }

    @Test
    public void PU145_GetUsersReviews() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userId = "notAUserIr";

        List<UserActivity> userReviews = userService.getUsersReviews(userId);

        assertThat(userReviews).isNotNull();
        assertThat(userReviews.isEmpty()).isTrue();
    }

    @Test
    public void PU146_GetUsersReviews() {
        String userId = "userId1";

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .getUsersReviews(userId);

        assertThrows(RuntimeException.class, () -> {
            userService.getUsersReviews(userId);
        });
    }
}
