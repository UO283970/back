package tfg.books.back.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.cloud.storage.Bucket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.cloud.StorageClient;
import com.google.firebase.database.annotations.NotNull;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import tfg.books.back.check.UserCheck;
import tfg.books.back.config.RestTemplateConfig;
import tfg.books.back.firebase.AppFirebaseConstants;
import tfg.books.back.firebase.AuthenticatedUserIdProvider;
import tfg.books.back.firebase.FirebaseAuthClient;
import tfg.books.back.model.books.Book;
import tfg.books.back.model.books.BookCustomSerializer;
import tfg.books.back.model.list.BookList;
import tfg.books.back.model.list.DefaultListForFirebase;
import tfg.books.back.model.notifications.Notification;
import tfg.books.back.model.notifications.NotificationsTypes;
import tfg.books.back.model.user.*;
import tfg.books.back.model.user.User.UserFollowState;
import tfg.books.back.model.user.User.UserPrivacy;
import tfg.books.back.model.userActivity.UserActivity;
import tfg.books.back.requests.FirebaseSignInResponse;
import tfg.books.back.requests.RefreshTokenResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    private static final String DUPLICATE_ACCOUNT_ERROR = "EMAIL_EXISTS";


    private final FirebaseAuthClient firebaseAuthClient;
    private final FirebaseAuth firebaseAuth;
    private final AuthenticatedUserIdProvider authenticatedUserIdProvider;
    private final Firestore firestore;
    private final StorageClient storage;
    private final ListService listService;
    private final PassEncryption passEncryption;
    private final String googleBooksBaseUrl;

    @Autowired
    RestTemplateConfig restTemplateConfig;

    public UserService(FirebaseAuth firebaseAuth, AuthenticatedUserIdProvider authenticatedUserIdProvider,
                       Firestore firestore, FirebaseAuthClient firebaseAuthClient, StorageClient storage,
                       ListService listService, PassEncryption passEncryption,
                       @Value("${google.books.api.base-url:https://www.googleapis.com/books/v1/volumes/{bookId}}")
                       String googleBooksBaseUrl) {
        this.firebaseAuthClient = firebaseAuthClient;
        this.firebaseAuth = firebaseAuth;
        this.authenticatedUserIdProvider = authenticatedUserIdProvider;
        this.firestore = firestore;
        this.storage = storage;
        this.listService = listService;
        this.passEncryption = passEncryption;
        this.googleBooksBaseUrl = googleBooksBaseUrl;
    }

    public LoginUser login(@NotNull String email, @NotNull String password) {
        List<UserErrorLogin> userErrorLogin = UserCheck.loginCheck(email, password);
        if (!userErrorLogin.isEmpty()) {
            return new LoginUser("", "", userErrorLogin);
        }

        FirebaseSignInResponse response = firebaseAuthClient.login(email, passEncryption.encrypt(password));

        if (response.idToken() != null && !response.idToken().isEmpty()) {
            return new LoginUser(response.idToken(), response.refreshToken(), userErrorLogin);
        } else {
            return new LoginUser("", "", List.of(UserErrorLogin.USER_NOT_FOUND));
        }
    }

    public Boolean tokenCheck() {
        return authenticatedUserIdProvider.getUserId() != null;
    }

    public String refreshToken(@NotNull String oldRefreshToken) {
        RefreshTokenResponse response = firebaseAuthClient.exchangeRefreshToken(oldRefreshToken);
        if (response.id_token() != null && !response.id_token().isEmpty()) {
            return response.id_token();
        } else {
            throw new RuntimeException("User not found");
        }

    }

    public Boolean resetPassword(@NotNull String email){
        try {
            QuerySnapshot document = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).whereEqualTo("email", email).get().get();

            if(!document.isEmpty()){
                firebaseAuth.generatePasswordResetLink(email);
                return true;
            }

            return false;
        } catch (FirebaseAuthException | ExecutionException | InterruptedException e) {
            return false;
        }
    }

    public RegisterUser checkUserEmailAndPass(@NotNull String email, @NotNull String password, @NotNull String repeatedPassword){
        List<UserErrorRegister> userErrorRegisterList = UserCheck.registerUserAndPassCheck(email, password,
                repeatedPassword);
        QuerySnapshot userEmailRepeat;
        try {
            userEmailRepeat = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).whereEqualTo(
                    "email", email).get().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (!userEmailRepeat.isEmpty()) {
            userErrorRegisterList.add(UserErrorRegister.ACCOUNT_EXISTS);
        }

        if (!userErrorRegisterList.isEmpty()) {
            return new RegisterUser("", "", userErrorRegisterList);
        }

        return new RegisterUser("", "", List.of());

    }

    public RegisterUser create(@NotNull String email, @NotNull String password, @NotNull String repeatedPassword,
                               @NotNull String userAlias, @NotNull String userName,
                               @NotNull String profilePictureURL){
        userAlias = userAlias.toLowerCase();

        try {
            List<UserErrorRegister> userErrorRegisterList = UserCheck.registerCheck(email, password,
                    repeatedPassword, userAlias);
            QuerySnapshot userAliasRepeat = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).whereEqualTo(
                    "userAlias", userAlias).get().get();
            QuerySnapshot userEmailRepeat = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).whereEqualTo(
                    "email", email).get().get();

            if (!userAliasRepeat.isEmpty()) {
                userErrorRegisterList.add(UserErrorRegister.REPEATED_USER_ALIAS);
            }

            if (!userEmailRepeat.isEmpty()) {
                userErrorRegisterList.add(UserErrorRegister.ACCOUNT_EXISTS);
            }

            if (!userErrorRegisterList.isEmpty()) {
                return new RegisterUser("", "", userErrorRegisterList);
            }

            CreateRequest request = new CreateRequest();
            request.setEmail(email);
            request.setPassword(passEncryption.encrypt(password));

            UserRecord userRecord = firebaseAuth.createUser(request);
            if (userRecord != null) {

                FirebaseSignInResponse response = firebaseAuthClient.login(email, passEncryption.encrypt(password));

                byte[] decodedBytes = Base64.getDecoder().decode(profilePictureURL);
                String imageName = "images/" + userRecord.getUid() + ".jpg";

                Bucket bucket = storage.bucket();

                bucket.create(imageName, decodedBytes, "image/jpeg");

                bucket.get(imageName);

                String imageUrl = String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                        bucket.getName(), URLEncoder.encode(imageName, StandardCharsets.UTF_8));

                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userRecord.getUid()).
                        set(new BasicInfoUser(email, userName, userAlias, imageUrl, "",
                                UserPrivacy.PUBLIC));

                int count = 0;
                for (String lisNames : AppFirebaseConstants.DEFAULT_LISTS) {
                    firestore.collection(AppFirebaseConstants.USERS_COLLECTION).
                            document(userRecord.getUid()).collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION)
                            .document(String.valueOf(count)).set(new DefaultListForFirebase(lisNames, "",
                                    BookList.BookListPrivacy.PUBLIC));
                    count++;
                }

                return new RegisterUser(response.idToken(), response.refreshToken(), List.of());
            } else {
                return new RegisterUser("", "", List.of(UserErrorRegister.UNKNOWN__));
            }
        } catch (FirebaseAuthException exception) {
            if (exception.getMessage().contains(DUPLICATE_ACCOUNT_ERROR)) {
                return new RegisterUser("", "", List.of(UserErrorRegister.ACCOUNT_EXISTS));
            }
        } catch (ExecutionException | InterruptedException e) {
            return new RegisterUser("", "", List.of(UserErrorRegister.UNKNOWN__));
        }
        return new RegisterUser("", "", List.of(UserErrorRegister.UNKNOWN__));
    }

    public String update(@NotNull String userAlias, @NotNull String userName, @NotNull String profilePictureURL,
                         @NotNull String description, @NotNull UserPrivacy privacyLevel) {
        String userId = authenticatedUserIdProvider.getUserId();
        DocumentReference document = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);

        try {
            UserPrivacy.valueOf(privacyLevel.toString());
        } catch (IllegalArgumentException e) {
            return "";
        }

        if (userAlias.isBlank()) {
            return "";
        }


        String imageUrl = "";
        try {
            if (!Objects.requireNonNull(document.get().get().get("userAlias")).toString().equals(userAlias)) {
                QuerySnapshot userAliasRepeat;

                userAliasRepeat = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).whereEqualTo(
                        "userAlias", userAlias).get().get();

                if (userAliasRepeat != null && !userAliasRepeat.isEmpty()) {
                    return "";
                }
                document.update("userAlias", userAlias).get();
                document.update("userName", userName).get();
                document.update("profilePictureURL", profilePictureURL).get();
                document.update("description", description).get();
                document.update("userPrivacy", privacyLevel.toString()).get();

                byte[] decodedBytes = Base64.getDecoder().decode(profilePictureURL);
                String imageName = "images/" + userId + ".jpg";

                Bucket bucket = storage.bucket();

                bucket.create(imageName, decodedBytes, "image/jpeg");

                bucket.get(imageName);

                imageUrl = String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                        bucket.getName(), URLEncoder.encode(imageName, StandardCharsets.UTF_8));

                document.update("profilePictureURL", imageUrl).get();
            }
        } catch (InterruptedException | ExecutionException e) {
            return "";
        }

        return imageUrl;
    }

    public boolean delete() throws FirebaseAuthException {
        String userId = authenticatedUserIdProvider.getUserId();
        if(userId == null || userId.isBlank()){
            return false;
        }

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                for (DocumentReference follow :
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).listDocuments()) {
                    firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(follow.getId()).collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(userId).delete();
                }
                for (DocumentReference following :
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).listDocuments()) {
                    firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(following.getId()).collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(userId).delete();
                }
                for (QueryDocumentSnapshot activity :
                        firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).whereEqualTo("userId",
                                userId).get().get()) {
                    firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).document(activity.getId()).delete();
                }
                for (QueryDocumentSnapshot list :
                        firestore.collection(AppFirebaseConstants.LIST_COLLECTION).whereEqualTo("listUserId", userId).get().get()) {
                    listService.deleteList(list.getId());
                }
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).delete();
                firebaseAuth.deleteUser(userId);
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean deleteNotification(@NotNull String notificationId){
        String userId = authenticatedUserIdProvider.getUserId();
        if(userId == null || userId.isBlank()){
            return false;
        }

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                .collection(AppFirebaseConstants.USERS_NOTIFICATIONS_COLLECTION).document(notificationId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                docRef.delete();
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public String logout() {
        String userId = authenticatedUserIdProvider.getUserId();
        try {
            firebaseAuth.revokeRefreshTokens(userId);
        } catch (FirebaseAuthException e) {
            throw new RuntimeException(e);
        }
        return userId;
    }

    public UserFollowState followUser(@NotNull String friendId) {
        String userId = authenticatedUserIdProvider.getUserId();
        if(userId == null || userId.isBlank()){
            return UserFollowState.NOT_FOLLOW;
        }


        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                if (User.UserPrivacy.valueOf(document2.getString("userPrivacy")).equals(User.UserPrivacy.PRIVATE)) {
                    firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId)
                            .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).document(userId).set(new HashMap<String, String>());
                    return UserFollowState.REQUESTED;
                } else {
                    firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId)
                            .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(userId).set(new HashMap<String, String>());
                    sendNotification(friendId, NotificationsTypes.FOLLOWED, userId);

                    firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                            .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(friendId).set(new HashMap<String, String>());
                    sendNotification(userId, NotificationsTypes.FOLLOW, friendId);

                    return UserFollowState.FOLLOWING;
                }
            } else {
                return UserFollowState.NOT_FOLLOW;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendNotification(String toUserId, NotificationsTypes followed, String fromUserId) {
        String notificationId = toUserId + "|" + fromUserId + "|" + followed.toString();

        try {
            DocumentReference notificationExist =
                    firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(toUserId)
                    .collection(AppFirebaseConstants.USERS_NOTIFICATIONS_COLLECTION).document(notificationId);

            if (notificationExist.get().get().exists()) {
                Timestamp timestamp = notificationExist.get().get().getTimestamp("timeStamp");
                Timestamp timeNow = Timestamp.now();

                assert timestamp != null;
                if (timeNow.toDate().getTime() - timestamp.toDate().getTime() > TimeUnit.HOURS.toMillis(1)) {
                    notificationExist.update("timeStamp", timeNow);
                }
            } else {
                Map<String, Object> notificationMap = Map.of("notificationType", followed.toString(), "timeStamp",
                        Timestamp.now(), "userId", fromUserId);

                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(toUserId)
                        .collection(AppFirebaseConstants.USERS_NOTIFICATIONS_COLLECTION).document(notificationId).set(notificationMap);
            }

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean cancelFollow(@NotNull String friendId) {
        String userId = authenticatedUserIdProvider.getUserId();
        if(userId == null || userId.isBlank()){
            return false;
        }

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId)
                        .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).document(userId).delete().get();
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(friendId).delete().get();
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(userId).delete().get();
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
        if(userId == null || userId.isBlank()){
            return false;
        }

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).document(friendId).delete();

                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(userId).set(new HashMap<String, String>());
                sendNotification(friendId, NotificationsTypes.FOLLOW, userId);

                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(friendId).set(new HashMap<String, String>());
                sendNotification(userId, NotificationsTypes.FOLLOWED, friendId);

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
        if(userId == null || userId.isBlank()){
            return false;
        }

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).document(friendId).delete().get();
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
        if(userId == null || userId.isBlank()){
            return false;
        }

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(userId).delete().get();
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(friendId).delete().get();
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public UserForSearch getMinUserInfo() {
        String userId = authenticatedUserIdProvider.getUserId();

        try {
            DocumentSnapshot user = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).get().get();
            UserForSearch userWithoutId = user.toObject(UserForSearch.class);

            assert userWithoutId != null;
            return new UserForSearch(userId,userWithoutId.userName(),userWithoutId.userAlias(),userWithoutId.profilePictureURL());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public List<UserForSearch> getUserSearchInfo(@NotNull String userQuery) {
        List<QueryDocumentSnapshot> userDocument;
        List<UserForSearch> searchUsers = new ArrayList<>();

        try {
            Set<QueryDocumentSnapshot> documentNoRepeat = new LinkedHashSet<>();

            documentNoRepeat.addAll(firestore.collection(AppFirebaseConstants.USERS_COLLECTION).whereGreaterThanOrEqualTo(
                    "userName", userQuery).whereLessThan("userName", userQuery + '\uf8ff').get().get().getDocuments());

            documentNoRepeat.addAll(firestore.collection(AppFirebaseConstants.USERS_COLLECTION).whereGreaterThanOrEqualTo(
                    "userAlias", userQuery).whereLessThan("userAlias", userQuery + '\uf8ff').get().get().getDocuments());

            userDocument = documentNoRepeat.stream().toList();

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unexpected error \n" + e);
        }

        for (QueryDocumentSnapshot document : userDocument) {
            if (!document.getId().equals(authenticatedUserIdProvider.getUserId())) {
                UserForSearch userForSearch = document.toObject(UserForSearch.class);
                searchUsers.add(new UserForSearch(document.getId(), userForSearch.userName(), userForSearch.userAlias(),
                        userForSearch.profilePictureURL()));
            }
        }

        return searchUsers;
    }

    public UserForSearch getUserMinimalInfo(@NotNull String userId) {
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                UserForSearch userObtained = document.toObject(UserForSearch.class);
                assert userObtained != null;
                return new UserForSearch(userId, userObtained.userName(), userObtained.userAlias(),
                        userObtained.profilePictureURL());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public UserForApp getAuthenticatedUserInfo() {
        String userId = authenticatedUserIdProvider.getUserId();
        if(userId == null ||userId.isBlank()){
            return null;
        }

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                BasicInfoUser basicInfoUser = document.toObject(BasicInfoUser.class);

                int numberOfFollowers =
                        Long.valueOf(firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                                collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).count().get().get().getCount()).intValue();
                int numberOfFollowing =
                        Long.valueOf(firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                                collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).count().get().get().getCount()).intValue();
                int numberOfReviews =
                        Long.valueOf(firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).whereEqualTo(
                                "userId", userId).whereEqualTo("userActivityType",
                                UserActivity.UserActivityType.REVIEW).count().get().get().getCount()).intValue();


                List<BookList> defaultUserList = listService.getDefaultUserLists(userId);

                List<BookList> bookList = listService.getBasicListInfoList(userId);


                UserFollowState userFollowState = UserFollowState.OWN;


                assert basicInfoUser != null;
                return new UserForApp(userId, basicInfoUser.userName(), basicInfoUser.userAlias(),
                        basicInfoUser.profilePictureURL(), basicInfoUser.description(), basicInfoUser.userPrivacy(),
                        numberOfFollowers, numberOfFollowing, numberOfReviews, defaultUserList, bookList,
                        userFollowState);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public UserForApp getAllUserInfo(@NotNull String userId) {
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                BasicInfoUser basicInfoUser = document.toObject(BasicInfoUser.class);
                assert basicInfoUser != null;

                int numberOfFollowers =
                        Long.valueOf(firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).count().get().get().getCount()).intValue();
                int numberOfFollowing =
                        Long.valueOf(firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                                .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).count().get().get().getCount()).intValue();
                int numberOfReviews =
                        Long.valueOf(firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).whereEqualTo(
                                "userId", userId).whereEqualTo("userActivityType",
                                UserActivity.UserActivityType.REVIEW).count().get().get().getCount()).intValue();

                String connectedUserId = authenticatedUserIdProvider.getUserId();
                UserFollowState userFollowState = UserFollowState.NOT_FOLLOW;
                if (userId.equals(connectedUserId)) {
                    userFollowState = UserFollowState.OWN;
                } else if (firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(connectedUserId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(userId).get().get().exists()) {
                    userFollowState = UserFollowState.FOLLOWING;
                } else if (firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).document(connectedUserId).get().get().exists()) {
                    userFollowState = UserFollowState.REQUESTED;
                }

                List<BookList> defaultUserList = listService.getDefaultUserLists(userId);
                List<BookList> userLists = listService.getBasicListInfoList(userId);

                return new UserForApp(userId, basicInfoUser.userName(), basicInfoUser.userAlias(),
                        basicInfoUser.profilePictureURL(), basicInfoUser.description(), basicInfoUser.userPrivacy(),
                        numberOfFollowers, numberOfFollowing, numberOfReviews, defaultUserList, userLists,
                        userFollowState);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public List<UserForProfile> getUsersListOFUser(@NotNull String userId, @NotNull String collectionName) {
        if (userId.isBlank()) {
            userId = authenticatedUserIdProvider.getUserId();
        }

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        List<UserForProfile> followersList = new ArrayList<>();


        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                Iterable<DocumentReference> userFollowingList =
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).collection(collectionName).listDocuments();
                for (DocumentReference followerUserId : userFollowingList) {
                    User actualUser = firestore.collection(AppFirebaseConstants.USERS_COLLECTION)
                            .document(followerUserId.getId()).get().get().toObject(User.class);

                    assert actualUser != null;

                    if (followerUserId.getId().equals(authenticatedUserIdProvider.getUserId())) {
                        actualUser.setUserFollowState(UserFollowState.OWN);
                    } else if (firestore.collection(AppFirebaseConstants.USERS_COLLECTION)
                            .document(authenticatedUserIdProvider.getUserId())
                            .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(followerUserId.getId()).get().get().exists()) {
                        actualUser.setUserFollowState(UserFollowState.FOLLOWING);
                    } else if (firestore.collection(AppFirebaseConstants.USERS_COLLECTION)
                            .document(followerUserId.getId())
                            .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).document(authenticatedUserIdProvider.getUserId()).get().get().exists()) {
                        actualUser.setUserFollowState(UserFollowState.REQUESTED);
                    }

                    followersList.add(new UserForProfile(followerUserId.getId(), actualUser.getUserName(),
                            actualUser.getUserAlias(), actualUser.getProfilePictureURL(),
                            actualUser.getUserFollowState()));
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return followersList;
    }

    public List<UserActivity> getUsersReviews(@NotNull String userId) {
        if (userId.isBlank()) {
            userId = authenticatedUserIdProvider.getUserId();
        }

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        List<UserActivity> userFollowActivitiesForApp = new ArrayList<>();


        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                QuerySnapshot userFollowActivities =
                        firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).whereEqualTo("userId",
                                userId).whereEqualTo("userActivityType", UserActivity.UserActivityType.REVIEW).get().get();

                for (QueryDocumentSnapshot userActivity : userFollowActivities) {
                    UserActivity newUserActivity = userActivity.toObject(UserActivity.class);
                    newUserActivity.setId(userActivity.getId());

                    newUserActivity.setLocalDateTime(LocalDateTime.ofInstant(newUserActivity.getTimestamp().toDate().toInstant(), ZoneId.systemDefault()).toString());

                    newUserActivity.setUser(getUserMinimalInfo(newUserActivity.getUserId()));

                    String bookFromApi = restTemplateConfig.restTemplate().exchange(googleBooksBaseUrl.replace("{bookId}",
                                    newUserActivity.getBookId()),
                            HttpMethod.GET, null, String.class).getBody();

                    GsonBuilder builder = new GsonBuilder();
                    builder.registerTypeAdapter(Book.class, new BookCustomSerializer());
                    Gson gson = builder.create();

                    Book book = gson.fromJson(bookFromApi, Book.class);

                    book.setBookId(newUserActivity.getBookId());
                    newUserActivity.setBook(book);

                    userFollowActivitiesForApp.add(newUserActivity);

                }

                return userFollowActivitiesForApp;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return userFollowActivitiesForApp;
    }


    public List<UserForSearch> getUserFollowRequest() {
        String userId = authenticatedUserIdProvider.getUserId();
        List<UserForSearch> userRequests = new ArrayList<>();
        if(userId == null || userId.isBlank()){
            return userRequests;
        }

        Iterable<DocumentReference> userRequest =
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).listDocuments();

        for (DocumentReference documentReference : userRequest) {
            try {
                User user = firestore.collection(AppFirebaseConstants.USERS_COLLECTION)
                        .document(documentReference.getId()).get().get().toObject(User.class);

                assert user != null;

                userRequests.add(new UserForSearch(documentReference.getId(), user.getUserName(),
                        user.getUserAlias(), user.getProfilePictureURL()));

            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return userRequests;
    }

    public List<Notification> getUserNotifications(@NotNull String timeStamp) {
        String userId = authenticatedUserIdProvider.getUserId();
        if(userId == null || userId.isBlank()){
            return new ArrayList<>();
        }

        List<Notification> userNotifications = new ArrayList<>();

        try {
            Query baseQuery = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                    .collection(AppFirebaseConstants.USERS_NOTIFICATIONS_COLLECTION).orderBy("timeStamp",
                            Query.Direction.DESCENDING).limit(20);

            if (!timeStamp.isBlank()) {
                baseQuery = baseQuery.startAfter(Timestamp.parseTimestamp(timeStamp));
            }

            List<QueryDocumentSnapshot> userNotification = baseQuery.get().get().getDocuments();

            for (QueryDocumentSnapshot documentReference : userNotification) {
                String notificationUserId = documentReference.getString("userId");

                if(notificationUserId == null){
                    return new ArrayList<>();
                }

                User user = firestore.collection(AppFirebaseConstants.USERS_COLLECTION)
                        .document(notificationUserId).get().get().toObject(User.class);

                if(user == null){
                    return new ArrayList<>();
                }

                if (firestore.collection(AppFirebaseConstants.USERS_COLLECTION)
                        .document(authenticatedUserIdProvider.getUserId())
                        .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(notificationUserId).get().get().exists()) {
                    user.setUserFollowState(UserFollowState.FOLLOWING);
                } else if (firestore.collection(AppFirebaseConstants.USERS_COLLECTION)
                        .document(notificationUserId)
                        .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).document(authenticatedUserIdProvider.getUserId()).get().get().exists()) {
                    user.setUserFollowState(UserFollowState.REQUESTED);
                }

                userNotifications.add(new Notification(documentReference.getId(), new UserForProfile(userId,
                        user.getUserName(), user.getUserAlias(), user.getProfilePictureURL(),
                        user.getUserFollowState()),
                        NotificationsTypes.valueOf(documentReference.getString("notificationType")),
                        Objects.requireNonNull(documentReference.getTimestamp("timeStamp")).toString()));
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return userNotifications;
    }
}
