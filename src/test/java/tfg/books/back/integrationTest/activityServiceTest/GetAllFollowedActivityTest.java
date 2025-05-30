package tfg.books.back.integrationTest.activityServiceTest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.assertj.core.api.AssertionsForClassTypes;
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
import tfg.books.back.graphQLControllers.ActivitiesGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;
import tfg.books.back.model.userActivity.UserActivity;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class GetAllFollowedActivityTest {
    private WireMockServer wireMockServer;

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private ActivitiesGraphQLController userActivityService;

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
    public void PU01_testGetAllFollowedActivity() {
        List<UserActivity> activities = userActivityService.getAllFollowedActivity("");
        assertThat(activities).isNotEmpty();
        AssertionsForClassTypes.assertThat(activities.size()).isEqualTo(2);
        AssertionsForClassTypes.assertThat(activities.get(0).getUserId()).isEqualTo("userId2");
        AssertionsForClassTypes.assertThat(activities.get(0).getBookId()).isEqualTo("bookId");
    }

    @Test
    public void PU01_PU02_testGetAllFollowedActivity() {
        List<UserActivity> activities = userActivityService.getAllFollowedActivity("2025-04-20T00:00:00Z");
        assertThat(activities).isNotEmpty();
        AssertionsForClassTypes.assertThat(activities.size()).isEqualTo(1);
        AssertionsForClassTypes.assertThat(activities.get(0).getUserId()).isEqualTo("userId2");
        AssertionsForClassTypes.assertThat(activities.get(0).getBookId()).isEqualTo("bookId2");
    }

    @Test
    public void PU03_testGetAllFollowedActivity() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId3");

        List<UserActivity> activities = userActivityService.getAllFollowedActivity("");
        assertThat(activities).isEmpty();
    }

    @Test
    public void PU04_testGetAllFollowedActivity() {
        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userActivityService)
                .getAllFollowedActivity("");

        assertThrows(RuntimeException.class, () -> {
            userActivityService.getAllFollowedActivity("");
        });
    }
}
