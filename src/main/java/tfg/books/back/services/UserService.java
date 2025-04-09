package tfg.books.back.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.database.annotations.NotNull;
import org.springframework.stereotype.Service;
import tfg.books.back.checks.UserChecks;
import tfg.books.back.firebase.AppFirebaseConstants;
import tfg.books.back.firebase.AuthenticatedUserIdProvider;
import tfg.books.back.firebase.FirebaseAuthClient;
import tfg.books.back.model.Book;
import tfg.books.back.model.list.BookList;
import tfg.books.back.model.list.DefaultListForFirebase;
import tfg.books.back.model.userActivity.UserActivity;
import tfg.books.back.model.userModels.*;
import tfg.books.back.model.userModels.User.UserFollowState;
import tfg.books.back.model.userModels.User.UserPrivacy;
import tfg.books.back.requests.FirebaseSignInResponse;
import tfg.books.back.requests.RefreshTokenResponse;

import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {
    private static final String DUPLICATE_ACCOUNT_ERROR = "EMAIL_EXISTS";


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
        List<UserErrorLogin> userErrorLogin = UserChecks.loginCheck(email, password);
        if (!userErrorLogin.isEmpty()) {
            return new LoginUser("", "", List.of());
        }

        FirebaseSignInResponse response = firebaseAuthClient.login(email, password);

        if (response.idToken() != null && !response.idToken().isEmpty()) {
            return new LoginUser(response.idToken(), response.refreshToken(), userErrorLogin);
        } else {
            return new LoginUser("", "", List.of(UserErrorLogin.USER_NOT_FOUND));
        }
    }

    public Boolean tokenCheck() {
        return !authenticatedUserIdProvider.getUserId().isBlank();
    }
    public String refreshToken(@NotNull String oldRefreshToken) {
        RefreshTokenResponse response = firebaseAuthClient.exchangeRefreshToken(oldRefreshToken);
        if (response.id_token() != null && !response.id_token().isEmpty()) {
            return response.id_token();
        } else {
            throw new InvalidParameterException("User not found");
        }

    }

    public RegisterUser create(@NotNull String email, @NotNull String password, @NotNull String repeatedPassword,
                               @NotNull String userAlias, @NotNull String userName,
                               @NotNull String profilePictureURL) throws FirebaseAuthException {
        CreateRequest request = new CreateRequest();
        request.setEmail(email);
        request.setPassword(password);

        try {
            List<UserErrorRegister> userErrorRegisterList = UserChecks.registerCheck(email, password,
                    repeatedPassword, userAlias);
            QuerySnapshot userAliasRepeat = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).whereEqualTo(
                    "userAlias", userAlias).get().get();

            if (!userAliasRepeat.isEmpty()) {
                userErrorRegisterList.add(UserErrorRegister.REPEATED_USER_ALIAS);
            }

            if (!userErrorRegisterList.isEmpty()) {
                return new RegisterUser("", "", userErrorRegisterList);
            }

            UserRecord userRecord = firebaseAuth.createUser(request);
            if (userRecord != null) {

                FirebaseSignInResponse response = firebaseAuthClient.login(email, password);

                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userRecord.getUid()).
                        set(new BasicInfoUser(userRecord.getEmail(), userName, userAlias, profilePictureURL, "",
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
            throw exception;
        } catch (ExecutionException | InterruptedException e) {
            return new RegisterUser("", "", List.of(UserErrorRegister.UNKNOWN__));
        }
    }

    public Boolean update(@NotNull String userAlias, @NotNull String userName, @NotNull String profilePictureURL,
                          @NotNull String description, @NotNull UserPrivacy privacyLevel) {
        String userId = authenticatedUserIdProvider.getUserId();
        DocumentReference document = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);

        try {
            if (!document.get().get().get("userAlias").toString().equals(userAlias)) {
                QuerySnapshot userAliasRepeat = null;

                userAliasRepeat = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).whereEqualTo(
                        "userAlias", userAlias).get().get();


                if (userAliasRepeat != null && !userAliasRepeat.isEmpty()) {
                    return false;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }

        document.update("userAlias", userAlias);
        document.update("userName", userName);
        document.update("profilePictureURL", profilePictureURL);
        document.update("description", description);
        document.update("userPrivacy", privacyLevel.toString());


        return true;
    }

    public boolean delete() throws FirebaseAuthException {
        String userId = authenticatedUserIdProvider.getUserId();
        firebaseAuth.deleteUser(userId);
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
                for (DocumentReference activity :
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).collection(AppFirebaseConstants.USERS_ACTIVITIES_COLLECTION).listDocuments()) {
                    //TODO: Lista de actividades firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION)
                    // .document(activity).delete();
                }
                for (DocumentReference list :
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).collection(AppFirebaseConstants.USERS_LISTS_COLLECTION).listDocuments()) {
                    listService.deleteList(list.getId());
                }
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).delete();
                firebaseAuth.revokeRefreshTokens(userId);
                firebaseAuth.deleteUser(userId);
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public String logout() throws FirebaseAuthException {
        String userId = authenticatedUserIdProvider.getUserId();
        firebaseAuth.revokeRefreshTokens(userId);
        return userId;
    }

    public UserFollowState followUser(@NotNull String friendId) {
        String userId = authenticatedUserIdProvider.getUserId();
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
                    firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                            .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(friendId).set(new HashMap<String, String>());
                    return UserFollowState.FOLLOWING;
                }
            } else {
                return UserFollowState.NOT_FOLLOW;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Boolean cancelFollow(@NotNull String friendId) {
        String userId = authenticatedUserIdProvider.getUserId();
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId)
                        .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).document(userId).delete();
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(friendId).delete();
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(userId).delete();
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
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId)
                        .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).document(userId).delete();
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(friendId).set(new HashMap<String, String>());
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(userId).set(new HashMap<String, String>());
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
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId)
                        .collection(AppFirebaseConstants.USERS_REQUESTS_COLLECTION).document(userId).delete();
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
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        DocumentReference docRef2 = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        ApiFuture<DocumentSnapshot> future2 = docRef2.get();

        try {
            DocumentSnapshot document = future.get();
            DocumentSnapshot document2 = future2.get();
            if (document.exists() && document2.exists()) {
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(friendId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWING_COLLECTION).document(userId).delete();
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(friendId).delete();
            } else {
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public List<UserForSearch> getUserSearchInfo(@NotNull String userQuery) {
        List<QueryDocumentSnapshot> userDocument;
        List<UserForSearch> searchUsers = new ArrayList<>();

        try {
            userDocument = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).whereGreaterThanOrEqualTo(
                    "userName", userQuery).whereLessThan("userName", userQuery + '\uf8ff').get().get().getDocuments();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Unexpected error \n" + e);
        }

        for (QueryDocumentSnapshot document : userDocument) {
            if(!document.getId().equals(authenticatedUserIdProvider.getUserId())){
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
                return new UserForSearch(userId,userObtained.userName(),userObtained.userAlias(),userObtained.profilePictureURL());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public UserForApp getAuthenticatedUserInfo() {
        String userId = authenticatedUserIdProvider.getUserId();
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


                List<BookList> defaultUserList = new ArrayList<>();
                QuerySnapshot listOfDocuments =
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                                collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).get().get();

                for (QueryDocumentSnapshot listDocument : listOfDocuments) {
                    BookList actualBookList = listDocument.toObject(BookList.class);
                    actualBookList.setListId(listDocument.getId());
                    defaultUserList.add(actualBookList);
                }

                int count = 0;
                for (BookList list : defaultUserList) {
                    list.setNumberOfBooks(Long.valueOf(firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                            collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(String.valueOf(count))
                            .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).count().get().get().getCount()).intValue());
                    count++;
                }

                QuerySnapshot userLists = firestore.collection(AppFirebaseConstants.LIST_COLLECTION).whereEqualTo(
                        "listUserId", userId).get().get();

                List<BookList> bookList = new ArrayList<>();

                for (QueryDocumentSnapshot query : userLists) {
                    BookList list = query.toObject(BookList.class);
                    list.setListId(query.getId());
                    bookList.add(list);
                }

                for (BookList list : bookList) {
                    list.setNumberOfBooks(Long.valueOf(firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(list.getListId())
                            .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).count().get().get().getCount()).intValue());
                }

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

                List<BookList> defaultUserList = new ArrayList<>();
                List<BookList> userLists = new ArrayList<>();

                if (!basicInfoUser.userPrivacy().equals(UserPrivacy.PRIVATE) && userFollowState.equals(UserFollowState.FOLLOWING)) {
                    QuerySnapshot listOfDocuments =
                            firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                                    collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).get().get();

                    for (QueryDocumentSnapshot listDocument : listOfDocuments) {
                        BookList actualBookList = listDocument.toObject(BookList.class);
                        actualBookList.setListId(listDocument.getId());
                        defaultUserList.add(actualBookList);
                    }

                    int count = 0;
                    for (BookList list : defaultUserList) {
                        list.setNumberOfBooks(Long.valueOf(firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                                collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(String.valueOf(count))
                                .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).count().get().get().getCount()).intValue());
                        count++;
                    }
                    userLists = listService.getBasicListInfoList(userId);
                }

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
        if(userId.isBlank()){
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
        if(userId.isBlank()){
            userId = authenticatedUserIdProvider.getUserId();
        }

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        List<UserActivity> userFollowActivitiesForApp = new ArrayList<>();


        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                QuerySnapshot userFollowActivities = firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).whereEqualTo("userId",
                        userId).whereEqualTo("userActivityType", UserActivity.UserActivityType.REVIEW).get().get();

                for (QueryDocumentSnapshot userActivity : userFollowActivities) {
                    UserActivity newUserActivity = userActivity.toObject(UserActivity.class);
                    newUserActivity.setId(userActivity.getId());

                    newUserActivity.setLocalDateTime(LocalDateTime.ofInstant(newUserActivity.getTimestamp().toDate().toInstant(), ZoneId.systemDefault()).toString());

                    newUserActivity.setUser(getUserMinimalInfo(newUserActivity.getUserId()));

                    newUserActivity.setBook(new Book());

                    userFollowActivitiesForApp.add(newUserActivity);

                }

                return userFollowActivitiesForApp;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return userFollowActivitiesForApp;
    }


}
