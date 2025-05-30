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
import tfg.books.back.model.user.RegisterUser;
import tfg.books.back.model.user.UserErrorRegister;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class CheckUserEmailAndPassTest {

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private UserGraphQLController userService;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        configurationTest.preloadData();
    }

    @Test
    public void PU155_CheckUserEmailAndPass() {
        String email = "newTest@newTest.com";
        String pass = "Test1234$";
        String repeatPass = "Test1234$";

        RegisterUser checkUserEmailAndPass = userService.checkUserEmailAndPass(email,pass,repeatPass);

        assertThat(checkUserEmailAndPass).isNotNull();
        assertThat(checkUserEmailAndPass.userRegisterErrors().isEmpty()).isTrue();
    }

    @Test
    public void PU156_CheckUserEmailAndPass() {
        String email = "notAnEmail";
        String pass = "Test1234$";
        String repeatPass = "Test1234$";

        RegisterUser checkUserEmailAndPass = userService.checkUserEmailAndPass(email,pass,repeatPass);

        assertThat(checkUserEmailAndPass).isNotNull();
        assertThat(checkUserEmailAndPass.userRegisterErrors().isEmpty()).isFalse();
        assertThat(checkUserEmailAndPass.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.INVALID_EMAIL);

        email = "";

        checkUserEmailAndPass = userService.checkUserEmailAndPass(email,pass,repeatPass);

        assertThat(checkUserEmailAndPass).isNotNull();
        assertThat(checkUserEmailAndPass.userRegisterErrors().isEmpty()).isFalse();
        assertThat(checkUserEmailAndPass.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.EMPTY_EMAIL);
    }

    @Test
    public void PU157_CheckUserEmailAndPass() {
        String email = "newTest@newTest.com";
        String pass = "notAValidPass";
        String repeatPass = "notAValidPass";

        RegisterUser checkUserEmailAndPass = userService.checkUserEmailAndPass(email,pass,repeatPass);

        assertThat(checkUserEmailAndPass).isNotNull();
        assertThat(checkUserEmailAndPass.userRegisterErrors().isEmpty()).isFalse();
        assertThat(checkUserEmailAndPass.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.INVALID_PASSWORD);

        pass = "";
        repeatPass = "";

        checkUserEmailAndPass = userService.checkUserEmailAndPass(email,pass,repeatPass);

        assertThat(checkUserEmailAndPass).isNotNull();
        assertThat(checkUserEmailAndPass.userRegisterErrors().isEmpty()).isFalse();
        assertThat(checkUserEmailAndPass.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.EMPTY_PASSWORD);

        pass = "pA1$";
        repeatPass = "pA1$";

        checkUserEmailAndPass = userService.checkUserEmailAndPass(email,pass,repeatPass);

        assertThat(checkUserEmailAndPass).isNotNull();
        assertThat(checkUserEmailAndPass.userRegisterErrors().isEmpty()).isFalse();
        assertThat(checkUserEmailAndPass.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.LONGITUDE_PASSWORD);
    }

    @Test
    public void PU158_CheckUserEmailAndPass() {
        String email = "newTest@newTest.com";
        String pass = "Test1234$";
        String repeatPass = "notEqualPass";

        RegisterUser checkUserEmailAndPass = userService.checkUserEmailAndPass(email,pass,repeatPass);

        assertThat(checkUserEmailAndPass).isNotNull();
        assertThat(checkUserEmailAndPass.userRegisterErrors().isEmpty()).isFalse();
        assertThat(checkUserEmailAndPass.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.REPEATED_PASSWORD);

    }

    @Test
    public void PU159_CheckUserEmailAndPass() {
        String email = "notAnEmail";
        String pass = "Test1234$";
        String repeatPass = "Test1234$";

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .checkUserEmailAndPass(email,pass,repeatPass);

        assertThrows(RuntimeException.class, () -> {
            userService.checkUserEmailAndPass(email,pass,repeatPass);
        });

    }
}
