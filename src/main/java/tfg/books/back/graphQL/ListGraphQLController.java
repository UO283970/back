package tfg.books.back.graphQL;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import tfg.books.back.exceptions.DataNotRetrievedException;
import tfg.books.back.model.BookList;
import tfg.books.back.model.BookList.BookListprivacy;

@Controller
public class ListGraphQLController {

    private static final String LIST_COLLECTION = "lists";

    private final Firestore firestore;

    public ListGraphQLController(Firestore firestore) {
        this.firestore = firestore;
    }

    @MutationMapping
    public ListWithId createList(@Argument("listName") String listName,
            @Argument("description") String description, @Argument("bookListPrivacy") BookListprivacy bookListprivacy) {
        String generatedID = UUID.randomUUID().toString();
        BookList generatedList = new BookList(listName, new ArrayList<String>(), description,bookListprivacy);
        firestore.collection(LIST_COLLECTION).document(generatedID).set(generatedList);

        return new ListWithId(generatedID, listName,generatedList.getNumberOfBooks(),description,bookListprivacy);
    }

    @QueryMapping
    public ListWithId getBasicListInfo(@Argument("id") String id){
        DocumentReference docRef = firestore.collection(LIST_COLLECTION).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        BookList bookList = null;

        try {
            DocumentSnapshot document = future.get();
            if(document.exists()){
                String listName = document.getString("listName");
                List<String> numberOfBooks = (List<String>) document.get("numberOfBooks");
                BookListprivacy bookListprivacy = BookListprivacy.valueOf(document.getString("bookListprivacy"));
                bookList = new BookList(listName, numberOfBooks,bookListprivacy);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new DataNotRetrievedException("The data could not be retrieved properly");
        }

        return new ListWithId(id, bookList.getListName(), bookList.getNumberOfBooks(),"",bookList.getBookListprivacy());
    }

    @QueryMapping
    public ListWithId getAllListInfo(@Argument("id") String id){
        DocumentReference docRef = firestore.collection(LIST_COLLECTION).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        BookList bookList = null;

        try {
            DocumentSnapshot document = future.get();
            if(document.exists()){
                bookList = document.toObject(BookList.class);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new DataNotRetrievedException("The data could not be retrieved properly");
        }

        return new ListWithId(id, bookList.getListName(), bookList.getNumberOfBooks(), bookList.getDescription(),bookList.getBookListprivacy());
    }


    record ListWithId(String id, String listName, List<String> numberOfBooks,String description,BookListprivacy bookListPrivacy){};
}
