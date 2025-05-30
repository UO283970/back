package tfg.books.back.graphQLControllers;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tfg.books.back.model.books.Book.ReadingState;
import tfg.books.back.model.list.BookList;
import tfg.books.back.model.list.BookList.BookListPrivacy;
import tfg.books.back.services.ListService;

import java.util.List;

@Controller
public class ListGraphQLController {

    private final ListService listService;

    public ListGraphQLController(ListService listService) {
        this.listService = listService;
    }

    @QueryMapping
    public List<BookList> getBasicListInfoList(@Argument("userId") String userId) {
        return listService.getBasicListInfoList(userId);
    }

    @QueryMapping
    public BookList getAllListInfo(@Argument("listId") String listId) {
        return listService.getAllListInfo(listId);
    }

    @QueryMapping
    public BookList getUserDefaultList(@Argument("userId") String userId,@Argument("listId") String listId) {
        return listService.getUserDefaultList(userId, listId);
    }

    @QueryMapping
    public List<BookList> getUserDefaultListsList(@Argument("userId") String userId) {
        return listService.getDefaultUserLists(userId);
    }

    @QueryMapping
    public String getDefaultListsWithBook(@Argument("bookId") String bookId) {
        return listService.getDefaultListsWithBook(bookId);
    }

    @QueryMapping
    public List<String> getListsWithBook(@Argument("bookId") String bookId) {
        return listService.getListsWithBook(bookId);
    }

    @QueryMapping
    public String getImageForDefaultList(@Argument("listId") String listId) {
        return listService.getImageForDefaultList("",listId);
    }

    @QueryMapping
    public String getImageForList(@Argument("listId") String listId) {
        return listService.getImageForList(listId);
    }

    @MutationMapping
    public String createList(@Argument("listName") String listName,
                                      @Argument("description") String description, @Argument("bookListPrivacy") BookListPrivacy bookListprivacy) {
        return listService.createList(listName,description,bookListprivacy);
    }

    @MutationMapping
    public Boolean updateList(@Argument("listId") String listId, @Argument("listName") String listName,
                               @Argument("description") String description, @Argument("bookListPrivacy") BookListPrivacy bookListprivacy) {
        return listService.updateList(listId,listName,description,bookListprivacy);
    }

    @MutationMapping
    public Boolean deleteList(@Argument("listId") String listId) {
        return listService.deleteList(listId);
    }

    @MutationMapping
    public ReadingState addBookToDefaultList(@Argument("listId") String listId, @Argument("bookId") String bookId) {
        return listService.addBookToDefaultList(listId, bookId);
    }

    @MutationMapping
    public Boolean removeBookFromDefaultList(@Argument("listId") String listId, @Argument("bookId") String bookId) {
        return listService.removeBookToDefaultList(listId,bookId);
    }

    @MutationMapping
    public Boolean addBookToList(@Argument("listIds") List<String> listIds, @Argument("bookId") String bookId) {
        return listService.addBookToList(listIds, bookId);
    }

    @MutationMapping
    public Boolean removeBookFromList(@Argument("listIds") List<String> listIds, @Argument("bookId") String bookId) {
        return listService.removeBookFromList(listIds,bookId);
    }
}
