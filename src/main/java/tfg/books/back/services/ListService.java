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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class ListService {

    private static final String LIST_COLLECTION = "lists";

    private final Firestore firestore;

    public ListService(Firestore firestore) {
        this.firestore = firestore;
    }

    public ListWithId getBasicListInfo(@NotNull String id){
        DocumentReference docRef = firestore.collection(LIST_COLLECTION).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        BookList bookList = null;

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                String listName = document.getString("listName");
                List<String> numberOfBooks = (List<String>) document.get("numberOfBooks");
                BookList.BookListPrivacy bookListprivacy = BookList.BookListPrivacy.valueOf(document.getString("bookListprivacy"));
                bookList = new BookList(listName, numberOfBooks, bookListprivacy);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new DataNotRetrievedException("The data could not be retrieved properly");
        }

        return new ListWithId(id, bookList.getListName(), bookList.getNumberOfBooks(), "", bookList.getBookListprivacy());
    }

    public ListWithId getAllListInfo(@NotNull String id) {
        DocumentReference docRef = firestore.collection(LIST_COLLECTION).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        BookList bookList = null;

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                bookList = document.toObject(BookList.class);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new DataNotRetrievedException("The data could not be retrieved properly");
        }

        return new ListWithId(id, bookList.getListName(), bookList.getNumberOfBooks(), bookList.getDescription(), bookList.getBookListprivacy());
    }

    public ListWithId createList(@NotNull String listName,
                                 @NotNull String description,@NotNull BookList.BookListPrivacy bookListprivacy) {
        String generatedID = UUID.randomUUID().toString();
        BookList generatedList = new BookList(listName, new ArrayList<String>(), description, bookListprivacy);
        firestore.collection(LIST_COLLECTION).document(generatedID).set(generatedList);

        return new ListWithId(generatedID, listName, generatedList.getNumberOfBooks(), description, bookListprivacy);
    }

    public ListWithId updateList(@NotNull String listId,@NotNull String listName,
                                 @NotNull String description,@NotNull BookList.BookListPrivacy bookListprivacy) {
        BookList generatedList = new BookList(listName, new ArrayList<String>(), description, bookListprivacy);
        firestore.collection(LIST_COLLECTION).document(listId).set(generatedList);

        return new ListWithId(listId, listName, generatedList.getNumberOfBooks(), description, bookListprivacy);
    }

    public String deleteList(@NotNull String listId) {
        firestore.collection(LIST_COLLECTION).document(listId).delete();

        return listId;
    }

    public Boolean addBookToList(@NotNull String listId,@NotNull String bookId) {
        DocumentReference docRef = firestore.collection(LIST_COLLECTION).document(listId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                firestore.collection(LIST_COLLECTION).document(listId).update("bookList", FieldValue.arrayUnion(bookId));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public Boolean removeBookToList(@NotNull String listId,@NotNull String bookId) {
        DocumentReference docRef = firestore.collection(LIST_COLLECTION).document(listId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                firestore.collection(LIST_COLLECTION).document(listId).update("bookList", FieldValue.arrayRemove(bookId));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
