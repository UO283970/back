package tfg.books.back.services;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.database.annotations.NotNull;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import tfg.books.back.config.RestTemplateConfig;
import tfg.books.back.exceptions.DataNotRetrievedException;
import tfg.books.back.firebase.AppFirebaseConstants;
import tfg.books.back.firebase.AuthenticatedUserIdProvider;
import tfg.books.back.model.books.Book;
import tfg.books.back.model.books.Book.ReadingState;
import tfg.books.back.model.books.BookCustomSerializer;
import tfg.books.back.model.list.BookList;
import tfg.books.back.model.list.ListForFirebase;
import tfg.books.back.model.list.ListForFirebaseWithTimestamp;
import tfg.books.back.model.user.User;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class ListService {

    private final Firestore firestore;
    private final AuthenticatedUserIdProvider authenticatedUserIdProvider;
    private final RestTemplateConfig restTemplateConfig;
    private final String googleBooksBaseUrl;

    public ListService(Firestore firestore, AuthenticatedUserIdProvider authenticatedUserIdProvider,
                       RestTemplateConfig restTemplateConfig,
                       @Value("${google.books.api.base-url:https://www.googleapis.com/books/v1/volumes/{bookId}}")
                       String googleBooksBaseUrl) {
        this.firestore = firestore;
        this.authenticatedUserIdProvider = authenticatedUserIdProvider;
        this.restTemplateConfig = restTemplateConfig;
        this.googleBooksBaseUrl = googleBooksBaseUrl;
    }

    public List<BookList> getBasicListInfoList(@NotNull String userId) {
        if (userId.isEmpty()) {
            userId = authenticatedUserIdProvider.getUserId();
        }
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        List<BookList> bookListList = new ArrayList<>();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                if (userId.equals(authenticatedUserIdProvider.getUserId())) {
                    QuerySnapshot userList = firestore.collection(AppFirebaseConstants.LIST_COLLECTION)
                            .whereEqualTo("listUserId", userId).orderBy("timestamp", Query.Direction.DESCENDING).get().get();

                    for (QueryDocumentSnapshot query : userList) {
                        BookList list = query.toObject(BookList.class);
                        list.setListId(query.getId());

                        list.setNumberOfBooks(Long.valueOf(firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(query.getId())
                                .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).count().get().get().getCount()).intValue());

                        if (list.getNumberOfBooks() != 0) {
                            list.setListImage(getImageForList(query.getId()));
                        }

                        bookListList.add(list);
                    }


                    return bookListList;
                }

                boolean userFollows = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(authenticatedUserIdProvider.getUserId()).get().get().exists();

                if (!Objects.equals(document.getString("userPrivacy"), User.UserPrivacy.PRIVATE.toString()) || userFollows) {
                    QuerySnapshot userList = firestore.collection(AppFirebaseConstants.LIST_COLLECTION)
                            .whereEqualTo("listUserId", userId).whereNotEqualTo("bookListPrivacy",
                                    BookList.BookListPrivacy.PRIVATE)
                            .get().get();

                    for (QueryDocumentSnapshot query : userList) {
                        BookList list = query.toObject(BookList.class);
                        list.setListId(query.getId());


                        list.setNumberOfBooks(Long.valueOf(firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(query.getId())
                                .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).count().get().get().getCount()).intValue());

                        if (list.getNumberOfBooks() != 0) {
                            list.setListImage(getImageForList(query.getId()));
                        }

                        bookListList.add(list);
                    }

                    if (firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                            .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(authenticatedUserIdProvider.getUserId()).get().get().exists()) {
                        return bookListList;
                    }

                    return bookListList.stream().filter(b -> b.getBookListPrivacy().equals(BookList.BookListPrivacy.PUBLIC)).toList();
                }

            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }


        return bookListList;
    }

    public BookList getAllListInfo(@NotNull String id) {
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(id);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                BookList bookList = document.toObject(BookList.class);
                if (bookList != null) {
                    if (checkUserOwnership(id) || Objects.requireNonNull(bookList).getBookListPrivacy().equals(BookList.BookListPrivacy.PUBLIC)
                            || checkListVisibilityConnectedUser(Objects.requireNonNull(bookList).getBookListPrivacy(),
                            bookList.getListUserId())) {

                        bookList.setListId(id);

                        List<Book> listOfBooksOfList = new ArrayList<>();

                        QuerySnapshot listOfBooks =
                                firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(id)
                                        .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).orderBy(
                                                "timestamp").get().get();

                        for (QueryDocumentSnapshot book : listOfBooks) {

                            String bookFromApi = restTemplateConfig.restTemplate().exchange(googleBooksBaseUrl.replace("{bookId}",
                                            book.getId()),
                                    HttpMethod.GET, null, String.class).getBody();

                            GsonBuilder builder = new GsonBuilder();
                            builder.registerTypeAdapter(Book.class, new BookCustomSerializer());
                            Gson gson = builder.create();

                            Book bookInfo = gson.fromJson(bookFromApi, Book.class);

                            bookInfo.setBookId(book.getId());

                            DocumentSnapshot bookDocumentRelation;
                            try {
                                bookDocumentRelation =
                                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(authenticatedUserIdProvider.getUserId())
                                                .collection(AppFirebaseConstants.BOOKS_DEFAULT_LIST_RELATION_COLLECTION)
                                                .document(bookInfo.getBookId()).get().get();
                            } catch (InterruptedException | ExecutionException e) {
                                throw new RuntimeException(e);
                            }

                            if (bookDocumentRelation.exists()) {
                                bookInfo.setReadingState(Book.ReadingState.valueOf(
                                        AppFirebaseConstants.DEFAULT_LISTS.get(
                                                Integer.parseInt(Objects.requireNonNull(bookDocumentRelation.getString("listId")))
                                        )));
                            } else {
                                bookInfo.setReadingState(Book.ReadingState.NOT_IN_LIST);
                            }

                            Double score = 0.0;

                            List<QueryDocumentSnapshot> bookDocument =
                                    firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).whereEqualTo(
                                            "bookId",
                                            bookInfo.getBookId()).whereEqualTo("userId", bookList.getListUserId()).get().get().getDocuments();

                            if (!bookDocument.isEmpty()) {
                                score = bookDocument.get(0).getDouble("score");
                            }

                            assert score != null;
                            bookInfo.setUserScore(score.intValue());

                            listOfBooksOfList.add(bookInfo);

                        }

                        bookList.setListOfBooks(listOfBooksOfList);

                        return bookList;
                    }
                }
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new DataNotRetrievedException("The data could not be retrieved properly");
        }

        return null;
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

            boolean userFollows = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                    .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(authenticatedUserIdProvider.getUserId()).get().get().exists();


            if (document.exists() && (userId.equals(authenticatedUserIdProvider.getUserId()) || !Objects.equals(document.getString(
                    "userPrivacy"), User.UserPrivacy.PRIVATE.toString()) || userFollows)) {
                QuerySnapshot listOfDocuments =
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                                collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).get().get();

                for (QueryDocumentSnapshot listDocument : listOfDocuments) {
                    BookList actualBookList = listDocument.toObject(BookList.class);

                    actualBookList.setListId(listDocument.getId());
                    actualBookList.setNumberOfBooks(Long.valueOf(firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                            .collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(listDocument.getId())
                            .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).count().get().get().getCount()).intValue());

                    if (actualBookList.getNumberOfBooks() != 0) {
                        actualBookList.setListImage(getImageForDefaultList(userId, listDocument.getId()));

                        if (listDocument.getId().equals("0")) {
                            List<Book> listOfBooksOfList = new ArrayList<>();

                            QuerySnapshot listOfBooks =
                                    firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                                            .collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(listDocument.getId())
                                            .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).limit(3).orderBy(
                                                    "timestamp").get().get();

                            for (QueryDocumentSnapshot book : listOfBooks) {

                                String bookFromApi = restTemplateConfig.restTemplate().exchange(googleBooksBaseUrl.replace("{bookId}",
                                                book.getId()),
                                        HttpMethod.GET, null, String.class).getBody();

                                GsonBuilder builder = new GsonBuilder();
                                builder.registerTypeAdapter(Book.class, new BookCustomSerializer());
                                Gson gson = builder.create();

                                Book bookInfo = gson.fromJson(bookFromApi, Book.class);

                                listOfBooksOfList.add(bookInfo);
                            }

                            actualBookList.setListOfBooks(listOfBooksOfList);
                        }
                    }


                    bookLists.add(actualBookList);
                }
                return bookLists;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return new ArrayList<>();
    }

    public BookList getUserDefaultList(@NotNull String userId, @NotNull String listId) {
        if (userId.isEmpty()) {
            userId = authenticatedUserIdProvider.getUserId();
        }
        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            boolean userFollows = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                    .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(authenticatedUserIdProvider.getUserId()).get().get().exists();

            if (document.exists() && (userId.equals(authenticatedUserIdProvider.getUserId()) || !Objects.equals(document.getString(
                    "userPrivacy"), User.UserPrivacy.PRIVATE.toString()) || userFollows)) {
                DocumentReference listOfDocuments =
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                                collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(listId);


                BookList actualBookList = listOfDocuments.get().get().toObject(BookList.class);

                if(actualBookList == null){
                    return null;
                }
                actualBookList.setListId(listOfDocuments.getId());

                List<Book> listOfBooksOfList = new ArrayList<>();
                QuerySnapshot listOfBooks = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(listOfDocuments.getId())
                        .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).orderBy("timestamp").get().get();

                for (QueryDocumentSnapshot book : listOfBooks) {
                    String bookFromApi = restTemplateConfig.restTemplate().exchange(googleBooksBaseUrl.replace("{bookId}",
                                    book.getId()),
                            HttpMethod.GET, null, String.class).getBody();

                    GsonBuilder builder = new GsonBuilder();
                    builder.registerTypeAdapter(Book.class, new BookCustomSerializer());
                    Gson gson = builder.create();

                    Book bookInfo = gson.fromJson(bookFromApi, Book.class);

                    bookInfo.setBookId(book.getId());

                    DocumentSnapshot bookDocumentRelation;
                    try {
                        bookDocumentRelation =
                                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(authenticatedUserIdProvider.getUserId())
                                        .collection(AppFirebaseConstants.BOOKS_DEFAULT_LIST_RELATION_COLLECTION)
                                        .document(bookInfo.getBookId()).get().get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }

                    if (bookDocumentRelation.exists()) {
                        bookInfo.setReadingState(Book.ReadingState.valueOf(
                                AppFirebaseConstants.DEFAULT_LISTS.get(
                                        Integer.parseInt(Objects.requireNonNull(bookDocumentRelation.getString(
                                                "listId")))
                                )));
                    } else {
                        bookInfo.setReadingState(Book.ReadingState.NOT_IN_LIST);
                    }

                    Double score = 0.0;

                    List<QueryDocumentSnapshot> bookDocument =
                            firestore.collection(AppFirebaseConstants.ACTIVITIES_COLLECTION).whereEqualTo(
                                    "bookId",
                                    bookInfo.getBookId()).whereEqualTo("userId", userId).get().get().getDocuments();

                    if (!bookDocument.isEmpty()) {
                        score = bookDocument.get(0).getDouble("score");
                    }

                    assert score != null;
                    bookInfo.setUserScore(score.intValue());

                    listOfBooksOfList.add(bookInfo);

                }

                actualBookList.setListOfBooks(listOfBooksOfList);

                return actualBookList;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public String createList(@NotNull String listName, @NotNull String description,
                             @NotNull BookList.BookListPrivacy bookListprivacy) {
        String generatedID = UUID.randomUUID().toString();
        String userId = authenticatedUserIdProvider.getUserId();

        try {
            BookList.BookListPrivacy.valueOf(bookListprivacy.toString());
        } catch (IllegalArgumentException e) {
            return "";
        }

        try {
            if (!firestore.collection(AppFirebaseConstants.LIST_COLLECTION).whereEqualTo("listName", listName)
                    .whereEqualTo("listUserId", userId).get().get().isEmpty() || listName.isBlank()) {
                return "";
            }

            ListForFirebaseWithTimestamp generatedList = new ListForFirebaseWithTimestamp(listName, description,
                    bookListprivacy, userId, Timestamp.now());
            firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(generatedID).set(generatedList).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return generatedID;
    }

    public Boolean updateList(@NotNull String listId, @NotNull String listName, @NotNull String description,
                              @NotNull BookList.BookListPrivacy bookListprivacy) {

        try {
            BookList.BookListPrivacy.valueOf(bookListprivacy.toString());
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (checkUserOwnership(listId) || listName.isBlank()) {
            String userId = authenticatedUserIdProvider.getUserId();
            try {
                if (!firestore.collection(AppFirebaseConstants.LIST_COLLECTION).whereEqualTo("listName", listName)
                        .whereEqualTo("listUserId", userId).get().get().isEmpty() || listName.isBlank()) {
                    return false;
                }
                ListForFirebase generatedList = new ListForFirebase(listName, description, bookListprivacy, userId);
                firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId).set(generatedList).get();
                return true;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    public Boolean deleteList(@NotNull String listId) {
        if (checkUserOwnership(listId)) {
            try {
                firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId).delete().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            return true;
        }
        return false;
    }

    public ReadingState addBookToDefaultList(@NotNull String listId, @NotNull String bookId) {
        String userId = authenticatedUserIdProvider.getUserId();

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot bookDocument = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                    .collection(AppFirebaseConstants.BOOKS_DEFAULT_LIST_RELATION_COLLECTION).document(bookId).get().get();

            if (bookDocument.exists()) {
                removeBookToDefaultList(bookDocument.getString("listId"), bookId);
            }

            DocumentSnapshot document = future.get();
            if (document.exists()) {
                DocumentReference bookList =
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                                collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(listId);
                if(!bookList.get().get().exists()){
                    return ReadingState.NOT_IN_LIST;
                }

                bookList.collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).document(bookId).set(Collections.singletonMap("timestamp", Timestamp.now()));

                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.BOOKS_DEFAULT_LIST_RELATION_COLLECTION)
                        .document(bookId).set(Collections.singletonMap("listId", listId));

                return ReadingState.valueOf(bookList.get().get().getString("listName"));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return ReadingState.NOT_IN_LIST;
    }

    public Boolean removeBookToDefaultList(@NotNull String listId, @NotNull String bookId) {
        String userId = authenticatedUserIdProvider.getUserId();

        DocumentReference docRef = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                .collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(listId);
        ApiFuture<DocumentSnapshot> future = docRef.get();

        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId).
                        collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(listId).
                        collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).document(bookId).delete().get();

                firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                        .collection(AppFirebaseConstants.BOOKS_DEFAULT_LIST_RELATION_COLLECTION)
                        .document(bookId).delete().get();


                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    public Boolean addBookToList(@NotNull List<String> listIds, @NotNull String bookId) {
        WriteBatch batch = firestore.batch();

        for (String listId : listIds) {
            DocumentReference docRef = firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId);
            ApiFuture<DocumentSnapshot> future = docRef.get();

            try {
                DocumentSnapshot document = future.get();
                if (document.exists() && checkUserOwnership(listId)) {

                    batch.set(firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId)
                                    .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).document(bookId),
                            Collections.singletonMap("timestamp", Timestamp.now()));

                    batch.set(firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(authenticatedUserIdProvider.getUserId())
                            .collection(AppFirebaseConstants.BOOKS_USER_LIST_RELATION_COLLECTION)
                            .document(bookId).collection(AppFirebaseConstants.INSIDE_LISTS_BOOK_COLLECTION).document(listId), new HashMap<String, String>());
                }
                batch.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }


        return true;
    }

    public Boolean removeBookFromList(@NotNull List<String> listIds, @NotNull String bookId) {

        WriteBatch batch = firestore.batch();

        for (String listId : listIds) {

            DocumentReference docRef = firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId);
            ApiFuture<DocumentSnapshot> future = docRef.get();

            try {
                DocumentSnapshot document = future.get();
                if (document.exists() && checkUserOwnership(listId)) {

                    batch.delete(firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId)
                            .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).document(bookId));

                    batch.delete(firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(authenticatedUserIdProvider.getUserId())
                            .collection(AppFirebaseConstants.BOOKS_USER_LIST_RELATION_COLLECTION)
                            .document(bookId).collection(AppFirebaseConstants.INSIDE_LISTS_BOOK_COLLECTION).document(listId));
                }
                batch.commit().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
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
                if (bookList.getListUserId().equals(userId)) {
                    return true;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
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
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                                .collection(AppFirebaseConstants.USERS_FOLLOWERS_COLLECTION).document(authenticatedUserId);
                ApiFuture<DocumentSnapshot> future2 = docRef2.get();
                DocumentSnapshot document2 = future2.get();
                return document2.exists();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return false;
    }


    public String getDefaultListsWithBook(@NotNull String bookId) {
        String userId = authenticatedUserIdProvider.getUserId();
        String listId = "";

        if (!bookId.isEmpty()) {
            try {
                DocumentSnapshot bookDocumentRelation =
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                                .collection(AppFirebaseConstants.BOOKS_DEFAULT_LIST_RELATION_COLLECTION)
                                .document(bookId).get().get();
                if (bookDocumentRelation.exists()) {
                    listId = bookDocumentRelation.getString("listId");
                }

            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return listId;
    }

    public List<String> getListsWithBook(@NotNull String bookId) {
        String userId = authenticatedUserIdProvider.getUserId();
        List<String> listIds = new ArrayList<>();

        if (!bookId.isEmpty()) {
            try {
                QuerySnapshot bookDocumentRelation =
                        firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                                .collection(AppFirebaseConstants.BOOKS_USER_LIST_RELATION_COLLECTION)
                                .document(bookId).collection(AppFirebaseConstants.INSIDE_LISTS_BOOK_COLLECTION).get().get();

                for (DocumentSnapshot d : bookDocumentRelation) {
                    listIds.add(d.getId());
                }

            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        return listIds;
    }

    public String getImageForDefaultList(String userId, String listId) {
        String image = "";
        QuerySnapshot listOfBooks;
        if (userId.isBlank()) {
            userId = authenticatedUserIdProvider.getUserId();
        }

        try {
            listOfBooks = firestore.collection(AppFirebaseConstants.USERS_COLLECTION).document(userId)
                    .collection(AppFirebaseConstants.USERS_DEFAULT_LISTS_COLLECTION).document(listId)
                    .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).orderBy(
                            "timestamp", Query.Direction.ASCENDING).limit(1).get().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (listOfBooks != null && !listOfBooks.isEmpty()) {
            String bookFromApi = restTemplateConfig.restTemplate().exchange(googleBooksBaseUrl.replace("{bookId}",
                            listOfBooks.getDocuments().get(0).getId()),
                    HttpMethod.GET, null, String.class).getBody();

            assert bookFromApi != null;
            JsonElement resultAsJSON =
                    JsonParser.parseString(bookFromApi).getAsJsonObject().get("volumeInfo");

            if (resultAsJSON.getAsJsonObject().get("imageLinks") != null && resultAsJSON.getAsJsonObject()
                    .get("imageLinks").getAsJsonObject().get("thumbnail") != null) {
                image =
                        resultAsJSON.getAsJsonObject().get("imageLinks").getAsJsonObject().get("thumbnail"
                        ).getAsString();
            }
        }

        return image;
    }

    public String getImageForList(String listId) {
        String image = "";
        QuerySnapshot listOfBooks;
        try {
            listOfBooks = (firestore.collection(AppFirebaseConstants.LIST_COLLECTION).document(listId)
                    .collection(AppFirebaseConstants.INSIDE_BOOKS_LIST_COLLECTION).orderBy(
                            "timestamp", Query.Direction.ASCENDING).limit(1).get().get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (listOfBooks != null && !listOfBooks.isEmpty()) {
            String bookFromApi = restTemplateConfig.restTemplate().exchange(googleBooksBaseUrl.replace("{bookId}",
                            listOfBooks.getDocuments().get(0).getId()),
                    HttpMethod.GET, null, String.class).getBody();

            assert bookFromApi != null;
            JsonElement resultAsJSON =
                    JsonParser.parseString(bookFromApi).getAsJsonObject().get("volumeInfo");

            if (resultAsJSON.getAsJsonObject().get("imageLinks") != null && resultAsJSON.getAsJsonObject()
                    .get("imageLinks").getAsJsonObject().get("thumbnail") != null) {
                image =
                        resultAsJSON.getAsJsonObject().get("imageLinks").getAsJsonObject().get(
                                "thumbnail"
                        ).getAsString();
            }
        }

        return image;
    }
}
