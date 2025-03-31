package tfg.books.back.graphQL;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tfg.books.back.model.BookList.BookListPrivacy;
import tfg.books.back.model.ListWithId;
import tfg.books.back.services.ListService;

@Controller
public class ListGraphQLController {

    private final ListService listService;

    public ListGraphQLController(ListService listService) {
        this.listService = listService;
    }

    @QueryMapping
    public ListWithId getBasicListInfo(@Argument("id") String id) {
        return listService.getBasicListInfo(id);
    }

    @QueryMapping
    public ListWithId getAllListInfo(@Argument("id") String id) {
        return listService.getAllListInfo(id);
    }

    @QueryMapping
    public ListWithId getUserDefaultList(@Argument("id") String userId,@Argument("id") String listId) {
        return listService.getUserDefaultList(userId, listId);
    }


    @MutationMapping
    public ListWithId createList(@Argument("listName") String listName,
                                 @Argument("description") String description, @Argument("bookListPrivacy") BookListPrivacy bookListprivacy) {
        return listService.createList(listName,description,bookListprivacy);
    }

    @MutationMapping
    public Boolean updateList(@Argument("listId") String listId, @Argument("listName") String listName,
                               @Argument("description") String description, @Argument("bookListPrivacy") BookListPrivacy bookListprivacy) {
        return listService.updateList(listId,listName,description,bookListprivacy);
    }

    @MutationMapping
    public Boolean updateList(@Argument("listId") String listId) {
        return listService.deleteList(listId);
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
