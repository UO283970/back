package tfg.books.back.integrationTest.userServiceTest;

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
public class LogoutTest {

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private UserGraphQLController userService;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoBean
    private AuthenticatedUserIdProvider authenticatedUserIdProvider;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        configurationTest.preloadData();
    }

    @Test
    public void PU111_Logout() throws FirebaseAuthException {
        doNothing().when(firebaseAuth).revokeRefreshTokens(anyString());
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String logout = userService.logout();

        assertThat(logout).isNotNull();
        assertThat(logout.isBlank()).isFalse();
    }

    @Test
    public void PU112_Logout() throws FirebaseAuthException {
        doNothing().when(firebaseAuth).revokeRefreshTokens(anyString());

        String logout = userService.logout();

        assertThat(logout).isNull();
    }

    @Test
    public void PU113_Logout() throws FirebaseAuthException {
        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .logout();

        assertThrows(RuntimeException.class, () -> {
            userService.logout();
        });
    }
}
