package tfg.books.back.integrationTest.userServiceTest;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class DeleteUserTest {
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
        doNothing().when(firebaseAuth).deleteUser(anyString());
    }

    @Test
    public void PU170_DeleteUser() throws ExecutionException, InterruptedException, FirebaseAuthException {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");
        boolean delete = userService.deleteUser();

        assertThat(delete).isNotNull();
        assertThat(delete).isTrue();

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isFalse();

        doc = firestore
                .collection(AppFirebaseConstants.LIST_COLLECTION)
                .document("listId1")
                .get()
                .get();

        assertThat(doc.exists()).isFalse();

        QuerySnapshot doc2 = firestore
                .collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                .whereEqualTo("userId", "userId1")
                .get()
                .get();

        assertThat(doc2.getDocuments().isEmpty()).isTrue();


        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId2")
                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isFalse();
    }

    @Test
    public void PU171_DeleteUser() throws FirebaseAuthException {
        boolean userLogin = userService.deleteUser();

        assertThat(userLogin).isNotNull();
        assertThat(userLogin).isFalse();

    }

    @Test
    public void PU172_DeleteUser() throws FirebaseAuthException, ExecutionException, InterruptedException {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .deleteUser();

        assertThrows(RuntimeException.class, () -> {
            userService.deleteUser();
        });

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
    }
}
