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
import tfg.books.back.model.user.UserForProfile;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class GetFollowersOfUserTest {

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
    public void PU132_GetFollowersOfUser() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userId = "userId2";

        List<UserForProfile> authenticatedUserInfo = userService.getFollowersOfUser(userId);

        assertThat(authenticatedUserInfo).isNotNull();
        assertThat(authenticatedUserInfo.isEmpty()).isFalse();
        assertThat(authenticatedUserInfo.size()).isEqualTo(1);
    }

    @Test
    public void PU133_GetFollowersOfUser() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userId = "userId1";

        List<UserForProfile> authenticatedUserInfo = userService.getFollowersOfUser(userId);

        assertThat(authenticatedUserInfo).isNotNull();
        assertThat(authenticatedUserInfo.isEmpty()).isTrue();
    }

    @Test
    public void PU134_GetFollowersOfUser() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId2");

        String userId = "";

        List<UserForProfile> authenticatedUserInfo = userService.getFollowersOfUser(userId);

        assertThat(authenticatedUserInfo).isNotNull();
        assertThat(authenticatedUserInfo.isEmpty()).isFalse();
        assertThat(authenticatedUserInfo.size()).isEqualTo(1);
    }

    @Test
    public void PU135_GetFollowersOfUser() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        String userId = "notAUserId";

        List<UserForProfile> authenticatedUserInfo = userService.getFollowersOfUser(userId);

        assertThat(authenticatedUserInfo).isNotNull();
        assertThat(authenticatedUserInfo.isEmpty()).isTrue();
    }

    @Test
    public void PU136_GetFollowersOfUser() {
        String userId = "notAUserId";

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .getFollowersOfUser(userId);

        assertThrows(RuntimeException.class, () -> {
            userService.getFollowersOfUser(userId);
        });
    }
}
