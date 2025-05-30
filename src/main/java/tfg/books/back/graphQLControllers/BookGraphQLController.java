package tfg.books.back.graphQLControllers;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tfg.books.back.model.books.Book;
import tfg.books.back.model.books.ExtraInfoForBook;
import tfg.books.back.services.BookAPIService;

import java.util.List;

@Controller
public class BookGraphQLController {

    private final BookAPIService bookAPIService;

    public BookGraphQLController(BookAPIService bookAPIService) {
        this.bookAPIService = bookAPIService;
    }

    @QueryMapping
    public List<Book> searchBooks(@Argument("userQuery") String userQuery,
                                  @Argument("searchFor") String searchFor, @Argument("subject") String subject) {
        return bookAPIService.searchBooks(userQuery, searchFor, subject);
    }

    @QueryMapping
    public List<Book> nextPageBooks(@Argument("userQuery") String userQuery, @Argument("page") int page, @Argument(
            "searchFor") String searchFor, @Argument("subject") String subject) {
        return bookAPIService.nextPageBooks(userQuery, page, searchFor, subject);
    }

    @QueryMapping
    public ExtraInfoForBook getExtraInfoForBook(@Argument("bookId") String bookId) {
        return bookAPIService.getExtraInfoForBook(bookId);
    }
}
