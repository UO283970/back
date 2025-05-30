package tfg.books.back;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.cloud.StorageClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Profile("test")
@TestConfiguration
public class FirebaseTestConfig {

    @Value("${firebase.database.url}")
    private String databaseUrl;

    @Bean
    public FirebaseApp  initializeFirebase(){
        System.setProperty("FIREBASE_AUTH_EMULATOR_HOST", "localhost:9099");

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(new FakeGoogleCredentials())
                    .setDatabaseUrl(databaseUrl)
                    .setProjectId("project-id")
                    .build();
            return FirebaseApp.initializeApp(options);
        } else {
            return FirebaseApp.getInstance();
        }
    }

    @Bean
    public FirebaseAuth getFirebaseAuth(FirebaseApp app){
        return FirebaseAuth.getInstance(app);
    }

    @Bean
    public Firestore firestore(FirebaseApp app) {
        return FirestoreClient.getFirestore(app);
    }

    @Bean
    public StorageClient storageClient(FirebaseApp app) {
        return StorageClient.getInstance(app);
    }

}
