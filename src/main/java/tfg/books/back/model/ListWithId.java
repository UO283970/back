package tfg.books.back.model;

import java.util.List;

public record ListWithId(String id, String listName, List<String> numberOfBooks, String description,
                         BookList.BookListPrivacy bookListPrivacy) {
}
