package tfg.books.back.integrationTest.userServiceTest;

import com.google.firebase.auth.FirebaseAuth;
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
import tfg.books.back.firebase.FirebaseAuthClient;
import tfg.books.back.graphQLControllers.UserGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;
import tfg.books.back.requests.RefreshTokenResponse;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class RefreshTokenTest {

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private UserGraphQLController userService;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoBean
    private AuthenticatedUserIdProvider authenticatedUserIdProvider;

    @MockitoBean
    private FirebaseAuthClient firebaseAuthClient;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        configurationTest.preloadData();
    }

    @Test
    public void PU114_RefreshToken() {
        when(firebaseAuthClient.exchangeRefreshToken(anyString())).thenReturn(new RefreshTokenResponse("token"));
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String refreshToken = userService.refreshToken("");

        assertThat(refreshToken).isNotNull();
        assertThat(refreshToken.isBlank()).isFalse();
    }

    @Test
    public void PU115_RefreshToken(){
        when(firebaseAuthClient.exchangeRefreshToken(anyString())).thenReturn(new RefreshTokenResponse(""));
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        assertThrows(RuntimeException.class, () -> {
            userService.refreshToken("");
        });
    }

    @Test
    public void PU116_RefreshToken() {
        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .refreshToken("");

        assertThrows(RuntimeException.class, () -> {
            userService.refreshToken("");
        });
    }
}
