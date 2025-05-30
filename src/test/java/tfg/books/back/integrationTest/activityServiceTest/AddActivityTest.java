package tfg.books.back.integrationTest.activityServiceTest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import tfg.books.back.model.userActivity.UserActivity.UserActivityType;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class AddActivityTest {

    private WireMockServer wireMockServer;

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private ActivitiesGraphQLController userActivityService;

    @MockitoSpyBean
    private Firestore firestore;

    @MockitoBean
    private AuthenticatedUserIdProvider authenticatedUserIdProvider;

    private static Stream<Arguments> getAllReviewsForBookFail() {
        return Stream.of(
                Arguments.of("", 7, "bookId", UserActivity.UserActivityType.REVIEW),
                Arguments.of("", -1, "bookId", UserActivity.UserActivityType.RATING),
                Arguments.of("", 11, "bookId", UserActivity.UserActivityType.RATING),
                Arguments.of("", 2, "", UserActivity.UserActivityType.RATING)
        );
    }

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
    public void PU09_testAddActivity() throws ExecutionException, InterruptedException {

        String bookId = "book";
        int score = 5;
        UserActivity.UserActivityType userActivityType = UserActivity.UserActivityType.RATING;
        Boolean activities = userActivityService.addActivity("", score, bookId, userActivityType);
        assertThat(activities).isTrue();

        String userId = "userId1";
        String expectedDocId = userId + "|" + bookId + "|" + userActivityType;

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                .document(expectedDocId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        assertThat(doc.getString("userId")).isEqualTo(userId);
        assertThat(doc.getString("bookId")).isEqualTo(bookId);
        assertThat(doc.getLong("score")).isEqualTo(score);
        assertThat(doc.getString("userActivityType")).isEqualTo(userActivityType.toString());
    }

    @ParameterizedTest
    @MethodSource("getAllReviewsForBookFail")
    public void PU10_PU13_testAddActivity(String bookId, int score, String text,
                                          UserActivityType userActivityType) throws ExecutionException,
            InterruptedException {

        Boolean activities = userActivityService.addActivity(text, score, bookId, userActivityType);
        assertThat(activities).isFalse();

        String userId = "userId1";
        String expectedDocId = userId + "|" + bookId + "|" + userActivityType;

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                .document(expectedDocId)
                .get()
                .get();

        assertThat(doc.exists()).isFalse();
    }

    @Test
    public void PU14_testAddActivity() throws ExecutionException, InterruptedException {

        String bookId = "bookId";
        int score = 0;
        UserActivityType userActivityType = UserActivityType.RATING;
        Boolean activities = userActivityService.addActivity("", score, bookId, userActivityType);
        assertThat(activities).isTrue();

        String userId = "userId1";
        String expectedDocId = userId + "|" + bookId + "|" + userActivityType;

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                .document(expectedDocId)
                .get()
                .get();

        assertThat(doc.exists()).isFalse();
    }

    @Test
    public void PU15_testAddActivity() throws ExecutionException, InterruptedException {
        String bookId = "bookId3";
        int score = 5;
        UserActivityType userActivityType = UserActivityType.RATING;

        String userId = "userId1";
        String expectedDocId = userId + "|" + bookId + "|" + userActivityType;

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                .document(expectedDocId)
                .get()
                .get();

        Timestamp timestamp = doc.getTimestamp("timestamp");

        assertThat(doc.exists()).isTrue();

        Boolean activities = userActivityService.addActivity("", score, bookId, userActivityType);
        assertThat(activities).isTrue();

        DocumentSnapshot doc2 = firestore
                .collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                .document(expectedDocId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        assertThat(doc.getString("userId")).isEqualTo(userId);
        assertThat(doc.getString("bookId")).isEqualTo(bookId);
        assertThat(doc.getLong("score")).isEqualTo(score);
        assertThat(doc.getString("userActivityType")).isEqualTo(userActivityType.toString());

        assertThat(doc2.getTimestamp("timestamp")).isEqualTo(timestamp);
    }

    @Test
    public void PU16_testAddActivity() throws ExecutionException, InterruptedException {

        String bookId = "bookId2";
        int score = 5;
        UserActivityType userActivityType = UserActivityType.RATING;
        Boolean activities = userActivityService.addActivity("", score, bookId, userActivityType);
        assertThat(activities).isTrue();

        String userId = "userId1";
        String expectedDocId = userId + "|" + bookId + "|" + userActivityType;

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                .document(expectedDocId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        assertThat(doc.getString("userId")).isEqualTo(userId);
        assertThat(doc.getString("bookId")).isEqualTo(bookId);
        assertThat(doc.getLong("score")).isEqualTo(score);
        assertThat(doc.getString("userActivityType")).isEqualTo(userActivityType.toString());

        Timestamp timestamp = doc.getTimestamp("timestamp");
        Timestamp now = Timestamp.now();

        assert timestamp != null;
        long diffMillis = now.toDate().getTime() - timestamp.toDate().getTime();
        long twoHoursMillis = TimeUnit.HOURS.toMillis(2);
        assertThat(diffMillis <= twoHoursMillis).isTrue();
    }

    @Test
    public void PU17_testAddActivity() throws ExecutionException, InterruptedException {

        String bookId = "bookId2";
        int score = 5;
        UserActivityType userActivityType = UserActivityType.REVIEW;
        String userId = "userId1";
        String expectedDocId = userId + "|" + bookId + "|" + UserActivityType.RATING;

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                .document(expectedDocId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();

        Boolean activities = userActivityService.addActivity("Has text", score, bookId, userActivityType);
        assertThat(activities).isTrue();

        String expectedDocId2 = userId + "|" + bookId + "|" + userActivityType;

        DocumentSnapshot doc2 = firestore
                .collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                .document(expectedDocId2)
                .get()
                .get();

        assertThat(doc2.exists()).isTrue();
        assertThat(doc2.getString("userId")).isEqualTo(userId);
        assertThat(doc2.getString("bookId")).isEqualTo(bookId);
        assertThat(doc2.getLong("score")).isEqualTo(score);
        assertThat(doc2.getString("userActivityType")).isEqualTo(userActivityType.toString());

        String expectedDocId3 = userId + "|" + bookId + "|" + UserActivityType.RATING;

        DocumentSnapshot doc3 = firestore
                .collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                .document(expectedDocId3)
                .get()
                .get();

        assertThat(doc3.exists()).isFalse();
    }

    @Test
    public void PU18_testAddActivity() throws ExecutionException, InterruptedException {

        String bookId = "book";
        int score = 5;
        UserActivityType userActivityType = UserActivityType.REVIEW;
        Boolean activities = userActivityService.addActivity("Has text", score, bookId, userActivityType);
        assertThat(activities).isTrue();

        String userId = "userId1";
        String expectedDocId = userId + "|" + bookId + "|" + userActivityType;

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                .document(expectedDocId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        assertThat(doc.getString("userId")).isEqualTo(userId);
        assertThat(doc.getString("bookId")).isEqualTo(bookId);
        assertThat(doc.getLong("score")).isEqualTo(score);
        assertThat(doc.getString("userActivityType")).isEqualTo(userActivityType.toString());
    }

    @Test
    public void PU19_testAddActivity(){

        String bookId = "book";
        int score = 5;
        UserActivityType fakeType = mock(UserActivityType.class);

        when(fakeType.toString()).thenReturn("NOT_CREATED");
        Boolean activities = userActivityService.addActivity("Has text", score, bookId, fakeType);

        assertThat(activities).isFalse();
    }

    @Test
    public void PU20_testAddActivity() {
        String bookId = "book";
        int score = 5;
        UserActivityType userActivityType = UserActivityType.REVIEW;

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userActivityService)
                .addActivity("Has text", score, bookId, userActivityType);

        assertThrows(RuntimeException.class, () -> {
            userActivityService.addActivity("Has text", score, bookId, userActivityType);
        });
    }

}
