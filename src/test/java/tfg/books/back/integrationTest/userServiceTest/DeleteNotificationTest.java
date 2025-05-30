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
import tfg.books.back.model.notifications.NotificationsTypes;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class DeleteNotificationTest {
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
    public void PU193_testDeleteFromFollower() throws ExecutionException, InterruptedException {
        String connectedUserId = "userId1";

        when(authenticatedUserIdProvider.getUserId()).thenReturn(connectedUserId);

        String notificationId = connectedUserId + "|" + "userId2" + "|" + NotificationsTypes.FOLLOWED;

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(connectedUserId)
                .collection(AppFirebaseConstants.USERS_NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();

        Boolean deleteNotification = userService.deleteNotification(notificationId);

        assertThat(deleteNotification).isNotNull();
        assertThat(deleteNotification).isTrue();

        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(connectedUserId)
                .collection(AppFirebaseConstants.USERS_NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .get()
                .get();

        assertThat(doc.exists()).isFalse();
    }

    @Test
    public void PU194_testDeleteFromFollower() throws ExecutionException, InterruptedException {
        String connectedUserId = "userId1";

        when(authenticatedUserIdProvider.getUserId()).thenReturn(connectedUserId);

        String notificationId = connectedUserId + "|" + "notANotification" + "|" + NotificationsTypes.FOLLOWED;

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(connectedUserId)
                .collection(AppFirebaseConstants.USERS_NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .get()
                .get();

        assertThat(doc.exists()).isFalse();

        Boolean deleteNotification = userService.deleteNotification(notificationId);

        assertThat(deleteNotification).isNotNull();
        assertThat(deleteNotification).isFalse();
    }

    @Test
    public void PU195_testDeleteFromFollower() throws ExecutionException, InterruptedException {

        String notificationId = "userId1" + "|" + "notANotification" + "|" + NotificationsTypes.FOLLOWED;

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .collection(AppFirebaseConstants.USERS_NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .get()
                .get();

        assertThat(doc.exists()).isFalse();

        Boolean deleteNotification = userService.deleteNotification(notificationId);

        assertThat(deleteNotification).isNotNull();
        assertThat(deleteNotification).isFalse();
    }

    @Test
    public void PU196_testDeleteFromFollower() throws ExecutionException, InterruptedException {
        String connectedUserId = "userId1";

        when(authenticatedUserIdProvider.getUserId()).thenReturn(connectedUserId);

        String notificationId = connectedUserId + "|" + "userId2" + "|" + NotificationsTypes.FOLLOWED;

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(connectedUserId)
                .collection(AppFirebaseConstants.USERS_NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .deleteNotification(notificationId);

        assertThrows(RuntimeException.class, () -> {
            userService.deleteNotification(notificationId);
        });

        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document(connectedUserId)
                .collection(AppFirebaseConstants.USERS_NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
    }
}
