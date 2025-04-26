package tfg.books.back.model.list;

import tfg.books.back.model.books.Book;

import java.util.List;

public record ListWithId(String listId, String listName, List<Book> listOfBooks, String description,
                         BookList.BookListPrivacy bookListPrivacy, String listUserId) {
}
