package tfg.books.back.integrationTest.userServiceTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tfg.books.back.BackApplication;
import tfg.books.back.FirebaseTestConfig;
import tfg.books.back.graphQLControllers.UserGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;
import tfg.books.back.model.user.LoginUser;
import tfg.books.back.model.user.UserErrorLogin;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class LoginTest {

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private UserGraphQLController userService;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        configurationTest.preloadData();
    }

    @Test
    public void PU101_Login(){
        String email = "test@test.com";
        String pass = "Test1234$";

        LoginUser userLogin = userService.login(email, pass);

        assertThat(userLogin).isNotNull();
        assertThat(userLogin.tokenId().isBlank()).isFalse();
        assertThat(userLogin.refreshToken().isBlank()).isFalse();
        assertThat(userLogin.userLoginErrors().isEmpty()).isTrue();
    }

    @Test
    public void PU102_Login(){
        String email = "notAnEmail";
        String pass = "Test1234$";

        LoginUser userLogin = userService.login(email, pass);

        assertThat(userLogin).isNotNull();
        assertThat(userLogin.tokenId().isBlank()).isTrue();
        assertThat(userLogin.refreshToken().isBlank()).isTrue();
        assertThat(userLogin.userLoginErrors().isEmpty()).isFalse();
        assertThat(userLogin.userLoginErrors().get(0)).isEqualTo(UserErrorLogin.INVALID_EMAIL);

        email = "";

        userLogin = userService.login(email, pass);

        assertThat(userLogin).isNotNull();
        assertThat(userLogin.tokenId().isBlank()).isTrue();
        assertThat(userLogin.refreshToken().isBlank()).isTrue();
        assertThat(userLogin.userLoginErrors().isEmpty()).isFalse();
        assertThat(userLogin.userLoginErrors().get(0)).isEqualTo(UserErrorLogin.EMPTY_EMAIL);
    }

    @Test
    public void PU103_Login(){
        String email = "test@test.com";
        String pass = "";

        LoginUser userLogin = userService.login(email, pass);

        assertThat(userLogin).isNotNull();
        assertThat(userLogin.tokenId().isBlank()).isTrue();
        assertThat(userLogin.refreshToken().isBlank()).isTrue();
        assertThat(userLogin.userLoginErrors().isEmpty()).isFalse();
        assertThat(userLogin.userLoginErrors().get(0)).isEqualTo(UserErrorLogin.EMPTY_PASSWORD);
    }

    @Test
    public void PU104_Login(){
        String email = "test@test.com";
        String pass = "notUsersPass1$";

        LoginUser userLogin = userService.login(email, pass);

        assertThat(userLogin).isNotNull();
        assertThat(userLogin.tokenId().isBlank()).isTrue();
        assertThat(userLogin.refreshToken().isBlank()).isTrue();
        assertThat(userLogin.userLoginErrors().isEmpty()).isFalse();
        assertThat(userLogin.userLoginErrors().get(0)).isEqualTo(UserErrorLogin.USER_NOT_FOUND);
    }

    @Test
    public void PU105_Login() {
        String email = "test@test.com";
        String pass = "Test1234$";
        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .login(email, pass);

        assertThrows(RuntimeException.class, () -> {
            userService.login(email, pass);
        });
    }
}
