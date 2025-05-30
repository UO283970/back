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
import tfg.books.back.model.user.UserForSearch;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class GetMinUserInfoTest {

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
    public void PU117_GetMinUserInfo() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        UserForSearch userForSearch = userService.getMinUserInfo();

        assertThat(userForSearch).isNotNull();
        assertThat(userForSearch.userId().isBlank()).isFalse();
        assertThat(userForSearch.userId()).isEqualTo("userId1");
    }

    @Test
    public void PU118_GetMinUserInfo(){
        assertThrows(RuntimeException.class, () -> {
            userService.getMinUserInfo();
        });
    }

    @Test
    public void PU119_GetMinUserInfo() {
        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .refreshToken("");

        assertThrows(RuntimeException.class, () -> {
            userService.getMinUserInfo();
        });
    }
}
