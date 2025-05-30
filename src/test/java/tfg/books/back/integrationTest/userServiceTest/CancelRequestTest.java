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
public class CancelRequestTest {
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
    public void PU185_testCancelRequest() throws ExecutionException, InterruptedException {
        String connectedUserId = "userId4";

        when(authenticatedUserIdProvider.getUserId()).thenReturn(connectedUserId);

        String userId = "userId1";

        Boolean cancelRequest = userService.cancelRequest(userId);

        assertThat(cancelRequest).isNotNull();
        assertThat(cancelRequest).isTrue();

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(connectedUserId)
                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION)
                .document(userId)
                .get()
                .get();

        assertThat(doc.exists()).isFalse();
    }

    @Test
    public void PU186_testCancelRequest(){
        String connectedUserId = "userId4";

        when(authenticatedUserIdProvider.getUserId()).thenReturn(connectedUserId);

        String userId = "notAUserId";

        Boolean cancelRequest = userService.cancelRequest(userId);

        assertThat(cancelRequest).isNotNull();
        assertThat(cancelRequest).isFalse();

    }

    @Test
    public void PU187_testCancelRequest(){
        String userId = "userId1";

        Boolean cancelRequest = userService.cancelRequest(userId);

        assertThat(cancelRequest).isNotNull();
        assertThat(cancelRequest).isFalse();

    }

    @Test
    public void PU188_testCancelRequest(){
        String connectedUserId = "userId4";

        when(authenticatedUserIdProvider.getUserId()).thenReturn(connectedUserId);

        String userId = "userId1";

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .cancelRequest(userId);

        assertThrows(RuntimeException.class, () -> {
            userService.cancelRequest(userId);
        });

    }

}
