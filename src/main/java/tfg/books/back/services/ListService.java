package tfg.books.back.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.database.annotations.NotNull;
import org.springframework.stereotype.Service;
import tfg.books.back.exceptions.DataNotRetrievedException;
import tfg.books.back.firebase.AppFirebaseConstants;
import tfg.books.back.firebase.AuthenticatedUserIdProvider;
import tfg.books.back.model.Book;
import tfg.books.back.model.Book.ReadingState;
import tfg.books.back.model.list.BasicListInfo;
import tfg.books.back.model.list.BookList;
import tfg.books.back.model.list.ListForFirebase;
import tfg.books.back.model.list.ListWithId;

import java.nio.file.AccessDeniedException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class ListService {


    private final Firestore firestore;
    private final AuthenticatedUserIdProvider authenticatedUserIdProvider;

    public ListService(Firestore firestore, AuthenticatedUserIdProvider authenticatedUserIdProvider) {
        this.firestore = firestore;
        this.authenticatedUserIdProvider = authenticatedUserIdProvider;
    }

    public List<BasicListInfo> getBasicListInfoList(@NotNull String userId) {
        if (userId.isEmpty()) {
            userId = authenticatedUserIdProvider.getUserId();
        }
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        List<BasicListInfo> bookListList = new ArrayList<>();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                if (userId.equals(authenticatedUserIdProvider.getUserId())) {
                    return firestore.collection(AppFirebaseConstants.LIST_COLLECTION)
                            .whereEqualTo("userId", userId).get().get().toObjects(BasicListInfo.class);
                }
                bookListList = firestore.collection(AppFirebaseConstants.LIST_COLLECTION)
                        .whereEqualTo("userId", userId).whereNotEqualTo("bookListPrivacy",
                                BookList.BookListPrivacy.PRIVATE)
                        .get().get().toObjects(BasicListInfo.class);

                if (firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(userId).get().get().exists()) {
                    return bookListList;
                }

                return bookListList.stream().filter(b -> b.bookListPrivacy().equals(BookList.BookListPrivacy.PUBLIC)).toList();

            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }


        return bookListList;
    }

    public ListWithId getAllListInfo(@NotNull String id) {
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                BookList bookList = document.toObject(BookList.class);
                if (checkUserOwnership(id) || Objects.requireNonNull(bookList).getBookListPrivacy().equals(BookList.BookListPrivacy.PUBLIC)
                        || checkListVisibilityConnectedUser(Objects.requireNonNull(bookList).getBookListPrivacy(),
                        bookList.getUserId())) {

                    List<Book> listOfBooks = firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(id)
                            .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).get().get().toObjects(Book.class);

                    assert bookList != null;
                    return new ListWithId(id, bookList.getListName(), listOfBooks, bookList.getDescription(),
                            bookList.getBookListPrivacy(), bookList.getUserId());
                }
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new DataNotRetrievedException("The data could not be retrieved properly");
        }

        return null;
    }

    public BookList getUserDefaultList(@NotNull String userId, @NotNull String listId) {
        List<BookList> userDefaultLists = getDefaultUserLists(userId);
        for (BookList l : userDefaultLists) {
            if (l.getListId().equals(listId)) {
                return l;
            }
        }
        return null;
    }

    public List<ListWithId> searchLists(@NotNull String userQuery) {
        try {
            return firestore.collection(AppFirebaseConstants.LIST_COLLECTION)
                    .whereGreaterThanOrEqualTo("userName", userQuery)
                    .whereLessThan("userName", userQuery + '\uf8ff').get().get().toObjects(ListWithId.class);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    public BookList createList(@NotNull String listName, @NotNull String description,
                               @NotNull BookList.BookListPrivacy bookListprivacy) {
        String generatedID = UUID.randomUUID().toString();
        String userId = authenticatedUserIdProvider.getUserId();
        ListForFirebase generatedList = new ListForFirebase(listName, description, bookListprivacy, userId);
        firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(generatedID).set(generatedList);


        return new BookList(generatedID, userId, listName, description, bookListprivacy, new ArrayList<>());
    }

    public Boolean updateList(@NotNull String listId, @NotNull String listName, @NotNull String description,
                              @NotNull BookList.BookListPrivacy bookListprivacy) {
        if (checkUserOwnership(listId)) {
            String userId = authenticatedUserIdProvider.getUserId();
            ListForFirebase generatedList = new ListForFirebase(listName, description, bookListprivacy, userId);
            firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId).set(generatedList);
            return true;
        }
        return false;
    }

    public Boolean deleteList(@NotNull String listId) {
        if (checkUserOwnership(listId)) {
            firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId).delete();
            return true;
        }
        return false;
    }

    public ReadingState addBookToDefaultList(@NotNull String listId, @NotNull String bookId) {
        for (BookList list : getDefaultUserLists("")) {
            removeBookToDefaultList(list.getListId(), bookId);
        }

        String userId = authenticatedUserIdProvider.getUserId();

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                DocumentReference bookList =
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                                collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(listId);
                bookList.collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).document(bookId).set(new HashMap<String, String>());
                return ReadingState.valueOf(bookList.get().get().getString("listName"));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return ReadingState.NOT_IN_LIST;
    }

    public Boolean removeBookToDefaultList(@NotNull String listId, @NotNull String bookId) {
        String userId = authenticatedUserIdProvider.getUserId();

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                        collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(listId).
                        collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).document(bookId).delete();
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    public Boolean addBookToList(@NotNull String listId, @NotNull String bookId) {
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists() && checkUserOwnership(listId)) {
                firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId).update("bookList",
                        FieldValue.arrayUnion(bookId));
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(authenticatedUserIdProvider.getUserId()).
                        collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(listId);

            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public Boolean removeBookFromList(@NotNull String listId, @NotNull String bookId) {
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists() && checkUserOwnership(listId)) {
                firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId).update("bookList",
                        FieldValue.arrayRemove(bookId));
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(authenticatedUserIdProvider.getUserId()).
                        collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(listId).delete();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    private boolean checkListVisibilityConnectedUser(@NotNull BookList.BookListPrivacy bookListPrivacy,
                                                     @NotNull String userId) {
        if (bookListPrivacy.equals(BookList.BookListPrivacy.ONLY_FOLLOWERS)) {
            return checkUserFollows(userId);
        }

        return bookListPrivacy != BookList.BookListPrivacy.PRIVATE;
    }

    private Boolean checkUserOwnership(@NotNull String listId) {
        String userId = authenticatedUserIdProvider.getUserId();
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId);
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

    public Boolean checkUserFollows(@NotNull String userId) {
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        String authenticatedUserId = authenticatedUserIdProvider.getUserId();
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                DocumentReference docRef2 =
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(authenticatedUserId);
                ApiFuture<DocumentSnapshot> future2 = docRef2.get();
                DocumentSnapshot document2 = future2.get();
                return document2.exists();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    public List<BookList> getDefaultUserLists(@NotNull String userId) {
        if (userId.isEmpty()) {
            userId = authenticatedUserIdProvider.getUserId();
        }
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        List<BookList> bookLists = new ArrayList<>();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                QuerySnapshot listOfDocuments =
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                                collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).get().get();
                for (QueryDocumentSnapshot listDocument : listOfDocuments) {
                    BookList actualBookList = listDocument.toObject(BookList.class);
                    actualBookList.setListId(document.getId());
                    bookLists.add(actualBookList);
                }
                return bookLists;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return new ArrayList<>();
    }
}
