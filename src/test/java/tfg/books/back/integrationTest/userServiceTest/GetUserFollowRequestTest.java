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
public class GetUserFollowRequestTest {

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
    public void PU147_GetUserFollowRequest() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId3");

        List<UserForSearch> userFollowRequest = userService.getUserFollowRequest();

        assertThat(userFollowRequest).isNotNull();
        assertThat(userFollowRequest.isEmpty()).isFalse();
        assertThat(userFollowRequest.size()).isEqualTo(1);
    }

    @Test
    public void PU148_GetUserFollowRequest() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        List<UserForSearch> userFollowRequest = userService.getUserFollowRequest();

        assertThat(userFollowRequest).isNotNull();
        assertThat(userFollowRequest.isEmpty()).isTrue();
    }

    @Test
    public void PU149_GetUserFollowRequest() {
        List<UserForSearch> userFollowRequest = userService.getUserFollowRequest();

        assertThat(userFollowRequest).isNotNull();
        assertThat(userFollowRequest.isEmpty()).isTrue();
    }

    @Test
    public void PU150_GetUserFollowRequest() {
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .getUserFollowRequest();

        assertThrows(RuntimeException.class, () -> {
            userService.getUserFollowRequest();
        });
    }
}
