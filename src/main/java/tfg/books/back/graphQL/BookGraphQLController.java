package tfg.books.back.graphQL;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tfg.books.back.model.Book;
import tfg.books.back.services.BookAPIService;

import java.util.List;

@Controller
public class BookGraphQLController {

    private final BookAPIService bookAPIService;

    public BookGraphQLController(BookAPIService bookAPIService){
        this.bookAPIService = bookAPIService;
    }

    @QueryMapping
    public List<Book> searchBooks(@Argument("userQuery") String userQuery){
        return bookAPIService.searchBooks(userQuery);
    }
}
