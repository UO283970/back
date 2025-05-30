package tfg.books.back;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Profile("test")
@TestConfiguration
public class FirebaseTestConfig {

    @Value("${firebase.database.url}")
    private String databaseUrl;

    @PostConstruct
    public void initializeFirebase(){
        System.setProperty("FIREBASE_AUTH_EMULATOR_HOST", "localhost:9099");

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(new FakeGoogleCredentials())
                .setDatabaseUrl(databaseUrl)
                .setProjectId("project-id")
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    public FirebaseAuth getFirebaseAuth(FirebaseApp app){
        return FirebaseAuth.getInstance(app);
    }

}
