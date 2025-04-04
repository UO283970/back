package tfg.books.back.graphQL;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tfg.books.back.model.Book.ReadingState;
import tfg.books.back.model.list.BasicListInfo;
import tfg.books.back.model.list.BookList;
import tfg.books.back.model.list.BookList.BookListPrivacy;
import tfg.books.back.model.list.ListWithId;
import tfg.books.back.services.ListService;

import java.util.List;

@Controller
public class ListGraphQLController {

    private final ListService listService;

    public ListGraphQLController(ListService listService) {
        this.listService = listService;
    }

    @QueryMapping
    public List<BasicListInfo> getBasicListInfo(@Argument("userId") String userId) {
        return listService.getBasicListInfoList(userId);
    }

    @QueryMapping
    public ListWithId getAllListInfo(@Argument("id") String id) {
        return listService.getAllListInfo(id);
    }

    @QueryMapping
    public BookList getUserDefaultList(@Argument("userId") String userId,@Argument("listId") String listId) {
        return listService.getUserDefaultList(userId, listId);
    }

    @QueryMapping
    public List<BookList> getUserDefaultLists(@Argument("userId") String userId) {
        return listService.getDefaultUserLists(userId);
    }

    @QueryMapping
    public List<ListWithId> searchLists(@Argument("userQuery") String userQuery) {
        return listService.searchLists(userQuery);
    }


    @MutationMapping
    public BookList createList(@Argument("listName") String listName,
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
    public Boolean addBookToList(@Argument("listId") String listId, @Argument("bookId") String bookId) {
        return listService.addBookToList(listId, bookId);
    }

    @MutationMapping
    public Boolean removeBookFromList(@Argument("listId") String listId, @Argument("bookId") String bookId) {
        return listService.removeBookFromList(listId,bookId);
    }
}
