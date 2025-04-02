package tfg.books.back.model.list;

public record BasicListInfo(String listId, String listName, int numberOfBooks,BookList.BookListPrivacy bookListPrivacy, String userId) {
}
