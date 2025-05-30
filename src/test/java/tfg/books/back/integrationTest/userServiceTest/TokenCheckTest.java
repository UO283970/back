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
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class TokenCheckTest {

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private UserGraphQLController userService;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoBean
    private AuthenticatedUserIdProvider authenticatedUserIdProvider;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException, FirebaseAuthException {
        configurationTest.preloadData();
    }

    @Test
    public void PU109_TokenCheck(){
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");
        Boolean tokenCheck = userService.tokenCheck();

        assertThat(tokenCheck).isNotNull();
        assertThat(tokenCheck).isTrue();
    }

    @Test
    public void PU110_TokenCheck(){
        Boolean tokenCheck = userService.tokenCheck();

        assertThat(tokenCheck).isNotNull();
        assertThat(tokenCheck).isFalse();
    }
}
