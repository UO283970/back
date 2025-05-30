package tfg.books.back.integrationTest.activityServiceTest;

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
import tfg.books.back.graphQLControllers.ActivitiesGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;
import tfg.books.back.model.userActivity.UserActivity;

import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class DeleteActivityTest {

    private WireMockServer wireMockServer;

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private ActivitiesGraphQLController userActivityService;

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
    public void PU21_testDeleteActivity() throws ExecutionException, InterruptedException {

        String expectedDocId = "userId1" + "|" + "bookId2" + "|" + UserActivity.UserActivityType.RATING;

        Boolean activities = userActivityService.deleteActivity(expectedDocId);

        assertThat(activities).isTrue();

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                .document(expectedDocId)
                .get()
                .get();

        assertThat(doc.exists()).isFalse();
    }

    @Test
    public void PU22_testDeleteActivity(){

        String expectedDocId = "userId" + "|" + "book" + "|" + UserActivity.UserActivityType.RATING;

        Boolean activities = userActivityService.deleteActivity(expectedDocId);

        assertThat(activities).isFalse();
    }

    @Test
    public void PU23_testDeleteActivity() {
        String expectedDocId = "userId1" + "|" + "bookId2" + "|" + UserActivity.UserActivityType.RATING;

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userActivityService)
                .deleteActivity(expectedDocId);

        assertThrows(RuntimeException.class, () -> {
            userActivityService.deleteActivity(expectedDocId);
        });
    }
}
