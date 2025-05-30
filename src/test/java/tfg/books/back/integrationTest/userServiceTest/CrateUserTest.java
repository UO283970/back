package tfg.books.back.integrationTest.userServiceTest;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
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
import tfg.books.back.graphQLControllers.UserGraphQLController;
import tfg.books.back.integrationTest.ConfigurationTest;
import tfg.books.back.model.user.RegisterUser;
import tfg.books.back.model.user.UserErrorRegister;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = {BackApplication.class, FirebaseTestConfig.class, ConfigurationTest.class})
public class CrateUserTest {
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

    @MockitoSpyBean
    private Firestore firestore;

    @BeforeEach
    public void setup() throws ExecutionException, InterruptedException, FirebaseAuthException {
        configurationTest.preloadData();

        when(firebaseAuth.createUser(any())).thenReturn(mockUserRecord);
        when(mockUserRecord.getUid()).thenReturn("newUserId");

        byte[] content = "dummy content".getBytes();

        when(storageClient.bucket()).thenReturn(mockBucket);
        when(mockBucket.create(any(),eq(content), any())).thenReturn(mockBlob);
        when(mockBlob.getMediaLink()).thenReturn("https://fake-url.com/test.jpg");
    }

    @Test
    public void PU160_CrateUser() throws IOException, InterruptedException, ExecutionException {
        String email = "newTest@newTest.com";
        String pass = "Test1234$";
        String repeatPass = "Test1234$";
        String userAlias = "newUserAliasTest";
        String userName = "newUserNameTest";
        String picture = "newUserPictureTest";

        configurationTest.createUserInEmulator(email,pass);
        RegisterUser registerUser = userService.createUser(email,pass,repeatPass,userAlias,userName,picture);

        assertThat(registerUser).isNotNull();
        assertThat(registerUser.userRegisterErrors().isEmpty()).isTrue();

        DocumentSnapshot doc = firestore
                .collection(AppFirebaseConstants.USERS_COLLECTION)
                .document("newUserId")
                .get()
                .get();

        assertThat(doc.exists()).isTrue();
        assertThat(doc.getString("email")).isEqualTo(email);
        assertThat(doc.getString("userAlias")).isEqualTo(userAlias.toLowerCase());
        assertThat(doc.getString("userName")).isEqualTo(userName);
    }

    @Test
    public void PU161_CrateUser(){
        String email = "notAnEmail";
        String pass = "Test1234$";
        String repeatPass = "Test1234$";
        String userAlias = "newUserAliasTest";
        String userName = "newUserNameTest";
        String picture = "newUserPictureTest";

        RegisterUser registerUser = userService.createUser(email,pass,repeatPass,userAlias,userName,picture);

        assertThat(registerUser).isNotNull();
        assertThat(registerUser.userRegisterErrors().isEmpty()).isFalse();
        assertThat(registerUser.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.INVALID_EMAIL);

        email = "";

        registerUser = userService.createUser(email,pass,repeatPass,userAlias,userName,picture);

        assertThat(registerUser).isNotNull();
        assertThat(registerUser.userRegisterErrors().isEmpty()).isFalse();
        assertThat(registerUser.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.EMPTY_EMAIL);

        email = "test@test.com";

        registerUser = userService.createUser(email,pass,repeatPass,userAlias,userName,picture);

        assertThat(registerUser).isNotNull();
        assertThat(registerUser.userRegisterErrors().isEmpty()).isFalse();
        assertThat(registerUser.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.ACCOUNT_EXISTS);
    }

    @Test
    public void PU162_CrateUser(){
        String email = "newTest@newTest.com";
        String pass = "notAValidPass";
        String repeatPass = "notAValidPass";
        String userAlias = "newUserAliasTest";
        String userName = "newUserNameTest";
        String picture = "newUserPictureTest";

        RegisterUser registerUser = userService.createUser(email,pass,repeatPass,userAlias,userName,picture);


        assertThat(registerUser).isNotNull();
        assertThat(registerUser.userRegisterErrors().isEmpty()).isFalse();
        assertThat(registerUser.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.INVALID_PASSWORD);

        pass = "";
        repeatPass = "";

        registerUser = userService.createUser(email,pass,repeatPass,userAlias,userName,picture);

        assertThat(registerUser).isNotNull();
        assertThat(registerUser.userRegisterErrors().isEmpty()).isFalse();
        assertThat(registerUser.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.EMPTY_PASSWORD);

        pass = "pA1$";
        repeatPass = "pA1$";

        registerUser = userService.createUser(email,pass,repeatPass,userAlias,userName,picture);

        assertThat(registerUser).isNotNull();
        assertThat(registerUser.userRegisterErrors().isEmpty()).isFalse();
        assertThat(registerUser.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.LONGITUDE_PASSWORD);
    }

    @Test
    public void PU163_CrateUser(){
        String email = "newTest@newTest.com";
        String pass = "Test1234$";
        String repeatPass = "notEqualPass";
        String userAlias = "newUserAliasTest";
        String userName = "newUserNameTest";
        String picture = "newUserPictureTest";

        RegisterUser registerUser = userService.createUser(email,pass,repeatPass,userAlias,userName,picture);

        assertThat(registerUser).isNotNull();
        assertThat(registerUser.userRegisterErrors().isEmpty()).isFalse();
        assertThat(registerUser.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.REPEATED_PASSWORD);
    }

    @Test
    public void PU164_CrateUser(){
        String email = "newTest@newTest.com";
        String pass = "Test1234$";
        String repeatPass = "Test1234$";
        String userAlias = "";
        String userName = "newUserNameTest";
        String picture = "newUserPictureTest";

        RegisterUser registerUser = userService.createUser(email,pass,repeatPass,userAlias,userName,picture);

        assertThat(registerUser).isNotNull();
        assertThat(registerUser.userRegisterErrors().isEmpty()).isFalse();
        assertThat(registerUser.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.EMPTY_USER_ALIAS);

        userAlias = "userAliasTest";
        registerUser = userService.createUser(email,pass,repeatPass,userAlias,userName,picture);

        assertThat(registerUser).isNotNull();
        assertThat(registerUser.userRegisterErrors().isEmpty()).isFalse();
        assertThat(registerUser.userRegisterErrors().get(0)).isEqualTo(UserErrorRegister.REPEATED_USER_ALIAS);
    }

    @Test
    public void PU165_CrateUser() {
        String email = "newTest@newTest.com";
        String pass = "Test1234$";
        String repeatPass = "Test1234$";
        String userAlias = "newUserAliasTest";
        String userName = "newUserNameTest";
        String picture = "newUserPictureTest";

        doThrow(new RuntimeException("Simulated Firestore failure"))
                .when(userService)
                .createUser(email,pass,repeatPass,userAlias,userName,picture);

        assertThrows(RuntimeException.class, () -> {
            userService.createUser(email,pass,repeatPass,userAlias,userName,picture);
        });
    }
}
