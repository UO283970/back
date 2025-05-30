package tfg.books.back.integrationTest.userServiceTest;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuthException;
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
import tfg.books.back.graphQLControllers.UserGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class CancelFollowTest {
    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private UserGraphQLController userService;

    @MockitoBean
    private AuthenticatedUserIdProvider authenticatedUserIdProvider;

    @MockitoSpyBean
    private Firestore firestore;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException, FirebaseAuthException {
        configurationTest.preloadData();
    }

    @Test
    public void PU177_testFollowUser() throws ExecutionException, InterruptedException {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");
        String userId = "userId2";

        Boolean cancelFollow = userService.cancelFollow(userId);

        assertThat(cancelFollow).isNotNull();
        assertThat(cancelFollow).isTrue();

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(userId)
                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isFalse();

        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION)
                .document(userId)
                .get()
                .get();

        assertThat(doc.exists()).isFalse();

        userId = "userId3";

        cancelFollow = userService.cancelFollow(userId);

        assertThat(cancelFollow).isNotNull();
        assertThat(cancelFollow).isTrue();

        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(userId)
                .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isFalse();
    }

    @Test
    public void PU178_testFollowUser() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");
        String userId = "notAValidUserId";

        Boolean cancelFollow = userService.cancelFollow(userId);

        assertThat(cancelFollow).isNotNull();
        assertThat(cancelFollow).isFalse();
    }

    @Test
    public void PU179_testFollowUser() throws ExecutionException, InterruptedException {
        String userId = "userId2";

        Boolean cancelFollow = userService.cancelFollow(userId);

        assertThat(cancelFollow).isNotNull();
        assertThat(cancelFollow).isFalse();

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(userId)
                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isTrue();

        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION)
                .document(userId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();

        userId = "userId3";

        cancelFollow = userService.cancelFollow(userId);

        assertThat(cancelFollow).isNotNull();
        assertThat(cancelFollow).isFalse();

        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(userId)
                .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
    }

    @Test
    public void PU180_testFollowUser() throws ExecutionException, InterruptedException {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");
        String userId = "userId2";

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .cancelFollow(userId);

        assertThrows(RuntimeException.class, () -> {
            userService.cancelFollow(userId);
        });

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(userId)
                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isTrue();

        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION)
                .document(userId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
    }
}
