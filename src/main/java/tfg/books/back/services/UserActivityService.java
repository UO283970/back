package tfg.books.back.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.database.annotations.NotNull;
import org.springframework.stereotype.Service;
import tfg.books.back.firebase.AppFirebaseConstants;
import tfg.books.back.firebase.AuthenticatedUserIdProvider;
import tfg.books.back.model.books.Book;
import tfg.books.back.model.userActivity.UserActivity;
import tfg.books.back.model.userActivity.UserActivityFirebase;
import tfg.books.back.model.userModels.UserForProfile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class UserActivityService {

    private final AuthenticatedUserIdProvider authenticatedUserIdProvider;
    private final Firestore firestore;
    private final UserService userService;

    public UserActivityService(AuthenticatedUserIdProvider authenticatedUserIdProvider, Firestore firestore,
                               UserService userService) {
        this.authenticatedUserIdProvider = authenticatedUserIdProvider;
        this.firestore = firestore;
        this.userService = userService;

    }

    public List<UserActivity> getAllFollowedActivity() {
        String userId = authenticatedUserIdProvider.getUserId();
        List<UserForProfile> followingList = userService.getUsersListOFUser(userId,
                AppFirebaseConstants.USERS_FOLLOWING_COLLECTION);
        List<String> userIds = new ArrayList<>();

        for (UserForProfile user : followingList) {
            userIds.add(user.userId());
        }
        List<UserActivity> userFollowActivitiesForApp = new ArrayList<>();

        if (!userIds.isEmpty()) {
            try {
                QuerySnapshot userFollowActivities =
                        firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).whereIn("userId", userIds).get().get();

                for (QueryDocumentSnapshot userActivity : userFollowActivities) {
                    UserActivity newUserActivity = userActivity.toObject(UserActivity.class);
                    newUserActivity.setId(userActivity.getId());

                    newUserActivity.setLocalDateTime(LocalDateTime.ofInstant(newUserActivity.getTimestamp().toDate().toInstant(), ZoneId.systemDefault()).toString());

                    newUserActivity.setUser(userService.getUserMinimalInfo(newUserActivity.getUserId()));

                    newUserActivity.setBook(new Book());

                    userFollowActivitiesForApp.add(newUserActivity);

                }


                return userFollowActivitiesForApp;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return userFollowActivitiesForApp;
    }


    public Boolean addActivity(@NotNull String activityText, @NotNull int score,
                               @NotNull String bookId, @NotNull UserActivity.UserActivityType userActivityType) {
        String userId = authenticatedUserIdProvider.getUserId();
        String generatedID = UUID.randomUUID().toString();

        firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).document(generatedID)
                .create(new UserActivityFirebase(activityText, score, userId, bookId, userActivityType,
                        Timestamp.now()));

        return true;
    }

    public Boolean deleteActivity(@NotNull String activityId) {
        DocumentReference docRef =
                firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).document(activityId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                docRef.delete();
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    public Boolean updateActivity(@NotNull String activityId, @NotNull String activityText, @NotNull int score) {
        DocumentReference docRef =
                firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).document(activityId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                docRef.update("activityText", activityText);
                docRef.update("score", score);
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return false;
    }
}
