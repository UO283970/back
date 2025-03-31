package tfg.books.back.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.database.annotations.NotNull;
import org.springframework.stereotype.Service;
import tfg.books.back.exceptions.DuplicateAccount;
import tfg.books.back.model.ListWithId;
import tfg.books.back.model.LoginUser;
import tfg.books.back.model.User;
import tfg.books.back.requests.FirebaseSignInResponse;
import tfg.books.back.requests.RefreshTokenResponse;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {
    private static final String DUPLICATE_ACCOUNT_ERROR = "EMAIL_EXISTS";
    private static final String USERS_COLLECTION = "users";
    

    private final FirebaseAuthClient firebaseAuthClient;
    private final FirebaseAuth firebaseAuth;
    private final AuthenticatedUserIdProvider authenticatedUserIdProvider;
    private final Firestore firestore;
    private final ListService listService;

    public UserService(FirebaseAuth firebaseAuth, AuthenticatedUserIdProvider authenticatedUserIdProvider,
                       Firestore firestore, FirebaseAuthClient firebaseAuthClient, ListService listService) {
        this.firebaseAuthClient = firebaseAuthClient;
        this.firebaseAuth = firebaseAuth;
        this.authenticatedUserIdProvider = authenticatedUserIdProvider;
        this.firestore = firestore;
        this.listService = listService;
    }

    public LoginUser login(@NotNull String email, @NotNull String password) {
        FirebaseSignInResponse response = firebaseAuthClient.login(email, password);
        if (response.idToken() != null && !response.idToken().isEmpty()) {
            return new LoginUser(response.idToken(), response.refreshToken());
        } else {
            throw new InvalidParameterException("User not found");
        }

    }

    public LoginUser create(@NotNull String email, @NotNull String password, @NotNull String userAlias,
                            @NotNull String userName,
                            @NotNull String profilePictureURL) throws FirebaseAuthException {
        CreateRequest request = new CreateRequest();
        request.setEmail(email);
        request.setPassword(password);

        try {
            UserRecord userRecord = firebaseAuth.createUser(request);
            if (userRecord != null) {
                FirebaseSignInResponse response = firebaseAuthClient.login(email, password);
                firestore.collection(USERS_COLLECTION).document(userRecord.getUid())
                        .set(new tfg.books.back.model.User(userRecord.getEmail(), userName, profilePictureURL,
                                userAlias));
                return new LoginUser(response.idToken(), response.refreshToken());
            } else {
                throw new InvalidParameterException("User not found");
            }
        } catch (FirebaseAuthException exception) {
            if (exception.getMessage().contains(DUPLICATE_ACCOUNT_ERROR)) {
                throw new DuplicateAccount("Account with given email-id already exists");
            }
            throw exception;
        }
    }

    public Boolean update(@NotNull String userAlias, @NotNull String userName,
                          @NotNull String profilePictureURL, @NotNull String description) throws FirebaseAuthException {
        String userId = authenticatedUserIdProvider.getUserId();
        DocumentReference document = firestore.collection(USERS_COLLECTION).document(userId);
        document.update("userAlias", userAlias);
        document.update("userName", userName);
        document.update("profilePictureURL", profilePictureURL);
        document.update("description", description);

        return true;
    }

    public boolean delete() throws FirebaseAuthException {
        String userId = authenticatedUserIdProvider.getUserId();
        firebaseAuth.deleteUser(userId);
        DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                for (String follow : (List<String>) (document.get("followedUsers"))) {
                    firestore.collection(USERS_COLLECTION).document(follow).update("followUsers",
                            FieldValue.arrayRemove(userId));
                }
                for (String following : (List<String>) (document.get("followingUsers"))) {
                    firestore.collection(USERS_COLLECTION).document(following).update("followingUsers",
                            FieldValue.arrayRemove(userId));
                }
                for (String activity : (List<String>) (document.get("userActivities"))) {
                    //TODO: Lista de actividades firestore.collection("activities").document(activity).delete();
                }
                for (String list : (List<String>) (document.get("userLists"))) {
                    listService.deleteList(list);
                }
                firestore.collection("lists").document().delete();
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public String refreshToken(@NotNull String oldRefreshToken) {
        RefreshTokenResponse response = firebaseAuthClient.exchangeRefreshToken(oldRefreshToken);
        if (response.id_token() != null && !response.id_token().isEmpty()) {
            return response.id_token();
        } else {
            throw new InvalidParameterException("User not found");
        }

    }

    public String logout() throws FirebaseAuthException {
        String userId = authenticatedUserIdProvider.getUserId();
        firebaseAuth.revokeRefreshTokens(userId);
        return userId;
    }

    public UserRecord retrieve() throws FirebaseAuthException {
        String userId = authenticatedUserIdProvider.getUserId();
        return firebaseAuth.getUser(userId);
    }

    public Boolean followUser(@NotNull String friendId) {
        String userId = authenticatedUserIdProvider.getUserId();
        DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                if (User.UserPrivacy.valueOf(document2.getString("userPrivacy")).equals(User.UserPrivacy.PRIVATE)) {
                    firestore.collection(USERS_COLLECTION).document(friendId).update("followRequestList",
                            FieldValue.arrayUnion(userId));
                } else {
                    firestore.collection(USERS_COLLECTION).document(userId).update("followingUsers",
                            FieldValue.arrayUnion(friendId));
                    firestore.collection(USERS_COLLECTION).document(friendId).update("followedUsers",
                            FieldValue.arrayUnion(userId));
                }
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return true;

    }

    public Boolean cancelFollow(@NotNull String friendId) {
        String userId = authenticatedUserIdProvider.getUserId();
        DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                firestore.collection(USERS_COLLECTION).document(friendId).update("followRequestList",
                        FieldValue.arrayRemove(userId));
                firestore.collection(USERS_COLLECTION).document(userId).update("followingUsers",
                        FieldValue.arrayRemove(friendId));
                firestore.collection(USERS_COLLECTION).document(friendId).update("followedUsers",
                        FieldValue.arrayRemove(userId));
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public Boolean acceptRequest(@NotNull String friendId) {
        String userId = authenticatedUserIdProvider.getUserId();
        DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                firestore.collection(USERS_COLLECTION).document(friendId).update("followRequestList",
                        FieldValue.arrayRemove(userId));
                firestore.collection(USERS_COLLECTION).document(userId).update("followingUsers",
                        FieldValue.arrayUnion(friendId));
                firestore.collection(USERS_COLLECTION).document(friendId).update("followedUsers",
                        FieldValue.arrayUnion(userId));
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public Boolean cancelRequest(@NotNull String friendId) {
        String userId = authenticatedUserIdProvider.getUserId();
        DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                firestore.collection(USERS_COLLECTION).document(friendId).update("followRequestList",
                        FieldValue.arrayRemove(userId));
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public Boolean deleteFromFollower(@NotNull String friendId) {
        String userId = authenticatedUserIdProvider.getUserId();
        DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                firestore.collection(USERS_COLLECTION).document(userId).update("followedUsers",
                        FieldValue.arrayRemove(friendId));
                firestore.collection(USERS_COLLECTION).document(friendId).update("followingUsers",
                        FieldValue.arrayRemove(userId));
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public List<User> getUserSearchInfo(@NotNull String userQuery) {
        return (List<User>) firestore.collection(USERS_COLLECTION)
                .whereGreaterThanOrEqualTo("userName", userQuery)
                .whereLessThan("userName", userQuery + '\uf8ff');
    }

    public User getAuthenticatedUserInfo() {
        String userId = authenticatedUserIdProvider.getUserId();
        DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                return document.toObject(User.class);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return new User();
    }

    public User getAllUserInfo(@NotNull String userId) {
        DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                return document.toObject(User.class);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return new User();
    }

    public Boolean checkUserFollows(@NotNull String userId) {
        DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
        String authenticatedUserId = authenticatedUserIdProvider.getUserId();
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                return ((List<String>) Objects.requireNonNull(document.get("followersUsers"))).contains(authenticatedUserId);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    public List<ListWithId> getDefaultUserList(@NotNull String userId) {
        if (userId.isEmpty()) {
            userId = authenticatedUserIdProvider.getUserId();
        }
        DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                return ((List<ListWithId>) Objects.requireNonNull(document.get("userDefaultLists")));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return new ArrayList<>();


    }

}
