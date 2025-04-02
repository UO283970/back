package tfg.books.back.model.list;

import tfg.books.back.model.Book;

import java.util.List;

public record ListWithId(String id, String listName, List<Book> books, String description,
                         BookList.BookListPrivacy bookListPrivacy, String userId) {
}
