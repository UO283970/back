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

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class GetUserSearchInfoTest {

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
    public void PU120_GetMinUserInfo() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userQuery = "userAliasTest".toLowerCase();

        List<UserForSearch> userForSearch = userService.getUserSearchInfo(userQuery);

        assertThat(userForSearch).isNotNull();
        assertThat(userForSearch.isEmpty()).isFalse();
        assertThat(userForSearch.size()).isEqualTo(3);
    }

    @Test
    public void PU121_GetMinUserInfo() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userQuery = "userNameTest";

        List<UserForSearch> userForSearch = userService.getUserSearchInfo(userQuery);

        assertThat(userForSearch).isNotNull();
        assertThat(userForSearch.isEmpty()).isFalse();
        assertThat(userForSearch.size()).isEqualTo(3);
    }

    @Test
    public void PU122_GetMinUserInfo() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userQuery = "userAlias".toLowerCase();

        List<UserForSearch> userForSearch = userService.getUserSearchInfo(userQuery);

        assertThat(userForSearch).isNotNull();
        assertThat(userForSearch.isEmpty()).isFalse();
        assertThat(userForSearch.size()).isEqualTo(3);
    }

    @Test
    public void PU123_GetMinUserInfo() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userQuery = "userName";

        List<UserForSearch> userForSearch = userService.getUserSearchInfo(userQuery);

        assertThat(userForSearch).isNotNull();
        assertThat(userForSearch.isEmpty()).isFalse();
        assertThat(userForSearch.size()).isEqualTo(3);
    }

    @Test
    public void PU124_GetMinUserInfo() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userQuery = "notAnExistingUser";

        List<UserForSearch> userForSearch = userService.getUserSearchInfo(userQuery);

        assertThat(userForSearch).isNotNull();
        assertThat(userForSearch.isEmpty()).isTrue();
    }

    @Test
    public void PU125_GetMinUserInfo() {
        String userQuery = "notAnExistingUser";

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .getUserSearchInfo(userQuery);

        assertThrows(RuntimeException.class, () -> {
            userService.getUserSearchInfo(userQuery);
        });
    }
}
