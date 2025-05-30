package tfg.books.back.integrationTest.userServiceTest;

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
import tfg.books.back.model.notifications.Notification;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class GetUserNotificationsTest {

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private UserGraphQLController userService;

    @MockitoBean
    private AuthenticatedUserIdProvider authenticatedUserIdProvider;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        configurationTest.preloadData();
    }

    @Test
    public void PU151_GetUserNotifications() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        List<Notification> notifications = userService.getUserNotifications("");

        assertThat(notifications).isNotNull();
        assertThat(notifications.isEmpty()).isFalse();
        assertThat(notifications.size()).isEqualTo(1);
    }

    @Test
    public void PU152_GetUserNotifications() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId3");

        List<Notification> notifications = userService.getUserNotifications("");

        assertThat(notifications).isNotNull();
        assertThat(notifications.isEmpty()).isTrue();
    }

    @Test
    public void PU153_GetUserNotifications() {
        List<Notification> notifications = userService.getUserNotifications("");

        assertThat(notifications).isNotNull();
        assertThat(notifications.isEmpty()).isTrue();
    }

    @Test
    public void PU154_GetUserNotifications() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .getUserNotifications("");

        assertThrows(RuntimeException.class, () -> {
            userService.getUserNotifications("");
        });
    }
}
