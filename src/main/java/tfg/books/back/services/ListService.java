package tfg.books.back.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.database.annotations.NotNull;
import org.springframework.stereotype.Service;
import tfg.books.back.exceptions.DataNotRetrievedException;
import tfg.books.back.model.BookList;
import tfg.books.back.model.ListWithId;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class ListService {

    private static final String LIST_COLLECTION = "lists";

    private final Firestore firestore;
    private final AuthenticatedUserIdProvider authenticatedUserIdProvider;
    private final UserService userService;

    public ListService(Firestore firestore, AuthenticatedUserIdProvider authenticatedUserIdProvider,
                       UserService userService) {
        this.firestore = firestore;
        this.authenticatedUserIdProvider = authenticatedUserIdProvider;
        this.userService = userService;
    }

    public ListWithId getBasicListInfo(@NotNull String id) {
        DocumentReference docRef = firestore.collection(LIST_COLLECTION).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        BookList bookList = null;

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                BookList.BookListPrivacy bookListPrivacy = BookList.BookListPrivacy.valueOf(document.getString(
                        "bookListPrivacy"));
                String userId = document.getString("userId");
                if (checkUserOwnership(id) || bookListPrivacy.equals(BookList.BookListPrivacy.PUBLIC) || checkListVisibilityConnectedUser(bookListPrivacy, userId)) {
                    String listName = document.getString("listName");
                    List<String> numberOfBooks = (List<String>) document.get("numberOfBooks");

                    bookList = new BookList(userId, listName, numberOfBooks, bookListPrivacy);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new DataNotRetrievedException("The data could not be retrieved properly");
        }

        assert bookList != null;
        return new ListWithId(id, bookList.getListName(), bookList.getNumberOfBooks(), "",
                bookList.getBookListprivacy());
    }

    public ListWithId getAllListInfo(@NotNull String id) {
        DocumentReference docRef = firestore.collection(LIST_COLLECTION).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        BookList bookList = null;

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                bookList = document.toObject(BookList.class);
                if (checkUserOwnership(id) || Objects.requireNonNull(bookList).getBookListPrivacy().equals(BookList.BookListPrivacy.PUBLIC)
                        || checkListVisibilityConnectedUser(Objects.requireNonNull(bookList).getBookListPrivacy(),
                        bookList.getUserId())) {
                    bookList = document.toObject(BookList.class);
                } else {
                    bookList = new BookList();
                }

            }
        } catch (InterruptedException | ExecutionException e) {
            throw new DataNotRetrievedException("The data could not be retrieved properly");
        }

        assert bookList != null;
        return new ListWithId(id, bookList.getListName(), bookList.getNumberOfBooks(), bookList.getDescription(),
                bookList.getBookListprivacy());
    }

    public ListWithId getUserDefaultList(@NotNull String userId, @NotNull String listId) {
        List<ListWithId> userDefaultLists = userService.getDefaultUserList(userId);
        for (ListWithId l : userDefaultLists){
            if (l.id().equals(listId)){
                return l;
            }
        }
        return null;
    }

    public List<BookList> searchLists(@NotNull String userQuery) {
        return (List<BookList>) firestore.collection(LIST_COLLECTION)
                .whereGreaterThanOrEqualTo("userName", userQuery)
                .whereLessThan("userName", userQuery + '\uf8ff');
    }


    public ListWithId createList(@NotNull String listName, @NotNull String description,
                                 @NotNull BookList.BookListPrivacy bookListprivacy) {
        String generatedID = UUID.randomUUID().toString();
        String userId = authenticatedUserIdProvider.getUserId();
        BookList generatedList = new BookList(userId, listName, new ArrayList<>(), description, bookListprivacy);
        firestore.collection(LIST_COLLECTION).document(generatedID).set(generatedList);

        return new ListWithId(generatedID, listName, generatedList.getNumberOfBooks(), description, bookListprivacy);
    }

    public Boolean updateList(@NotNull String listId, @NotNull String listName, @NotNull String description,
                              @NotNull BookList.BookListPrivacy bookListprivacy) {
        if (checkUserOwnership(listId)) {
            String userId = authenticatedUserIdProvider.getUserId();
            BookList generatedList = new BookList(userId, listName, new ArrayList<>(), description,
                    bookListprivacy);
            firestore.collection(LIST_COLLECTION).document(listId).set(generatedList);
            return true;
        }
        return false;
    }

    public Boolean deleteList(@NotNull String listId) {
        if (checkUserOwnership(listId)) {
            firestore.collection(LIST_COLLECTION).document(listId).delete();
            return true;
        }
        return false;
    }

    public Boolean addBookToList(@NotNull String listId, @NotNull String bookId) {
        DocumentReference docRef = firestore.collection(LIST_COLLECTION).document(listId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists() && checkUserOwnership(listId)) {
                firestore.collection(LIST_COLLECTION).document(listId).update("bookList",
                        FieldValue.arrayUnion(bookId));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public Boolean removeBookFromList(@NotNull String listId, @NotNull String bookId) {
        DocumentReference docRef = firestore.collection(LIST_COLLECTION).document(listId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists() && checkUserOwnership(listId)) {
                firestore.collection(LIST_COLLECTION).document(listId).update("bookList",
                        FieldValue.arrayRemove(bookId));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private boolean checkListVisibilityConnectedUser(@NotNull BookList.BookListPrivacy bookListPrivacy, @NotNull String userId) {
        if (bookListPrivacy.equals(BookList.BookListPrivacy.ONLY_FOLLOWERS)) {
            return userService.checkUserFollows(userId);
        }

        return bookListPrivacy != BookList.BookListPrivacy.PRIVATE;
    }

    private Boolean checkUserOwnership(@NotNull String listId) {
        String userId = authenticatedUserIdProvider.getUserId();
        DocumentReference docRef = firestore.collection(LIST_COLLECTION).document(listId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                BookList bookList = document.toObject(BookList.class);
                assert bookList != null;
                if (bookList.getUserId().equals(userId)) {
                    return true;
                } else {
                    throw new AccessDeniedException("You don't have access to this resource");
                }
            }
        } catch (InterruptedException | ExecutionException | AccessDeniedException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
