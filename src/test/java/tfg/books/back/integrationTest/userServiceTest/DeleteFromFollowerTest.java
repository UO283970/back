package tfg.books.back.integrationTest.userServiceTest;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
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
public class DeleteFromFollowerTest {
    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private UserGraphQLController userService;

    @MockitoBean
    private AuthenticatedUserIdProvider authenticatedUserIdProvider;

    @MockitoSpyBean
    private Firestore firestore;

    @MockitoSpyBean
    private FirebaseAuth firebaseAuth;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException, FirebaseAuthException {
        configurationTest.preloadData();
    }

    @Test
    public void PU189_testDeleteFromFollower() throws ExecutionException, InterruptedException {
        String connectedUserId = "userId3";

        when(authenticatedUserIdProvider.getUserId()).thenReturn(connectedUserId);

        String userId = "userId1";

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(connectedUserId)
                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION)
                .document(userId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();

        Boolean deleteFromFollower = userService.deleteFromFollower(userId);

        assertThat(deleteFromFollower).isNotNull();
        assertThat(deleteFromFollower).isTrue();

        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(connectedUserId)
                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION)
                .document(userId)
                .get()
                .get();

        assertThat(doc.exists()).isFalse();
        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(userId)
                .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION)
                .document(connectedUserId)
                .get()
                .get();

        assertThat(doc.exists()).isFalse();
    }

    @Test
    public void PU190_testDeleteFromFollower(){
        String connectedUserId = "userId3";

        when(authenticatedUserIdProvider.getUserId()).thenReturn(connectedUserId);

        String userId = "notAValidUser";

        Boolean deleteFromFollower = userService.deleteFromFollower(userId);

        assertThat(deleteFromFollower).isNotNull();
        assertThat(deleteFromFollower).isFalse();
    }

    @Test
    public void PU191_testDeleteFromFollower(){
        String userId = "userId1";

        Boolean deleteFromFollower = userService.deleteFromFollower(userId);

        assertThat(deleteFromFollower).isNotNull();
        assertThat(deleteFromFollower).isFalse();
    }


    @Test
    public void PU192_testDeleteFromFollower() throws ExecutionException, InterruptedException {
        String connectedUserId = "userId3";

        when(authenticatedUserIdProvider.getUserId()).thenReturn(connectedUserId);

        String userId = "userId1";

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(connectedUserId)
                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION)
                .document(userId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .cancelRequest(userId);

        assertThrows(RuntimeException.class, () -> {
            userService.cancelRequest(userId);
        });

        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(connectedUserId)
                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION)
                .document(userId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(userId)
                .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION)
                .document(connectedUserId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
    }
}
