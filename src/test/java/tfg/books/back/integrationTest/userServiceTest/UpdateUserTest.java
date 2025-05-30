package tfg.books.back.integrationTest.userServiceTest;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.StorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tfg.books.back.BackApplication;
import tfg.books.back.FirebaseTestConfig;
import tfg.books.back.firebase.AppFirebaseConstants;
import tfg.books.back.firebase.AuthenticatedUserIdProvider;
import tfg.books.back.graphQLControllers.UserGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;
import tfg.books.back.model.user.User.UserPrivacy;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class UpdateUserTest {

    @Mock
    UserRecord mockUserRecord;

    @Autowired
    private ConfigurationTest configurationTest;

    @MockitoSpyBean
    private UserGraphQLController userService;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoBean
    private StorageClient storageClient;

    @MockitoBean
    private Bucket mockBucket;

    @MockitoBean
    private Blob mockBlob;

    @MockitoBean
    private AuthenticatedUserIdProvider authenticatedUserIdProvider;

    @MockitoSpyBean
    private Firestore firestore;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException {
        configurationTest.preloadData();
        when(authenticatedUserIdProvider.getUserId()).thenReturn("userId1");

        byte[] content = "dummy content".getBytes();

        when(storageClient.bucket()).thenReturn(mockBucket);
        when(mockBucket.create(any(),eq(content), any())).thenReturn(mockBlob);
        when(mockBlob.getMediaLink()).thenReturn("https://fake-url.com/test.jpg");
    }

    @Test
    public void PU166_UpdateUser() throws ExecutionException, InterruptedException {
        String userAlias = "newUserAlias";
        String userName = "newUserName";
        String picture = "newPicture";
        String desc = "newDesc";
        UserPrivacy userPrivacy = UserPrivacy.PUBLIC;

        String userLogin = userService.updateUser(userAlias, userName, picture, desc, userPrivacy);

        assertThat(userLogin).isNotNull();
        assertThat(userLogin.isBlank()).isFalse();


        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        assertThat(doc.getString("userAlias")).isEqualTo(userAlias);
        assertThat(doc.getString("userName")).isEqualTo(userName);
        assertThat(doc.getString("description")).isEqualTo(desc);
    }

    @Test
    public void PU167_UpdateUser() throws ExecutionException, InterruptedException {
        String userAlias = "";
        String userName = "newUserName";
        String picture = "newPicture";
        String desc = "newDesc";
        UserPrivacy userPrivacy = UserPrivacy.PUBLIC;

        String userLogin = userService.updateUser(userAlias, userName, picture, desc, userPrivacy);

        assertThat(userLogin).isNotNull();
        assertThat(userLogin.isBlank()).isTrue();


        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        assertThat(doc.getString("userAlias")).isNotEqualTo(userAlias);

        userAlias = "userAliasTest2";

        userService.updateUser(userAlias, userName, picture, desc, userPrivacy);

        userAlias = "userAliasTest".toLowerCase();
        userLogin = userService.updateUser(userAlias, userName, picture, desc, userPrivacy);

        assertThat(userLogin).isNotNull();
        assertThat(userLogin.isBlank()).isTrue();

        doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        assertThat(doc.getString("userAlias")).isNotEqualTo(userAlias);
    }

    @Test
    public void PU168_UpdateUser() throws ExecutionException, InterruptedException {
        String userAlias = "newUserAlias";
        String userName = "newUserName";
        String picture = "newPicture";
        String desc = "newDesc";
        UserPrivacy userPrivacy  = mock(UserPrivacy.class);
        when(userPrivacy.toString()).thenReturn("NOT_CREATED");

        String userLogin = userService.updateUser(userAlias, userName, picture, desc, userPrivacy);

        assertThat(userLogin).isNotNull();
        assertThat(userLogin.isBlank()).isTrue();


        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        assertThat(doc.getString("userAlias")).isNotEqualTo(userAlias);
        assertThat(doc.getString("userName")).isNotEqualTo(userName);
        assertThat(doc.getString("description")).isNotEqualTo(desc);
    }

    @Test
    public void PU169_UpdateUser() throws ExecutionException, InterruptedException {
        String userAlias = "newUserAlias";
        String userName = "newUserName";
        String picture = "newPicture";
        String desc = "newDesc";
        UserPrivacy userPrivacy = UserPrivacy.PUBLIC;

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .updateUser(userAlias, userName, picture, desc, userPrivacy);

        assertThrows(RuntimeException.class, () -> {
            userService.updateUser(userAlias, userName, picture, desc, userPrivacy);
        });


        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("userId1")
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        assertThat(doc.getString("userAlias")).isNotEqualTo(userAlias);
        assertThat(doc.getString("userName")).isNotEqualTo(userName);
        assertThat(doc.getString("description")).isNotEqualTo(desc);
    }
}
