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
import tfg.books.back.graphQLControllers.UserGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class ResetPasswordTest {

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private UserGraphQLController userService;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException, FirebaseAuthException {
        configurationTest.preloadData();
        when(firebaseAuth.generatePasswordResetLink(anyString())).thenReturn("");
    }

    @Test
    public void PU106_ResetPassword() throws FirebaseAuthException {
        String email = "test@test.com";

        Boolean resetPassword = userService.resetPassword(email);

        assertThat(resetPassword).isNotNull();
        assertThat(resetPassword).isTrue();
    }

    @Test
    public void PU107_ResetPassword(){
        String email = "notAUserEmail";

        Boolean resetPassword = userService.resetPassword(email);

        assertThat(resetPassword).isNotNull();
        assertThat(resetPassword).isFalse();
    }

    @Test
    public void PU108_Login() {
        String email = "test@test.com";
        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .resetPassword(email);

        assertThrows(RuntimeException.class, () -> {
            userService.resetPassword(email);
        });
    }
}
