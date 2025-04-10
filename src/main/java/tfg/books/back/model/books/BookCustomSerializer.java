package tfg.books.back.model.books;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BookCustomSerializer implements JsonDeserializer<Book> {
    @Override
    public Book deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject bookInfo = json.getAsJsonObject().get("volumeInfo").getAsJsonObject();

        String title = bookInfo.getAsJsonObject().get("title").getAsString();
        if (bookInfo.getAsJsonObject().get("subtitle") != null) {
            title += " - " + bookInfo.getAsJsonObject().get("subtitle").getAsString();
        }

        String bookId = json.getAsJsonObject().get("id").getAsString();

        String publishYear = "";
        if (bookInfo.getAsJsonObject().get("publishedDate") != null) {
            publishYear = bookInfo.getAsJsonObject().get("publishedDate").getAsString().split("-")[0];
        }

        String author = "";
        if (bookInfo.getAsJsonObject().get("authors") != null) {
            author = bookInfo.getAsJsonObject().get("authors").getAsJsonArray().get(0).getAsString();
        }

        String coverImageURL = "";
        if (bookInfo.getAsJsonObject().get("imageLinks") != null && bookInfo.getAsJsonObject().get("imageLinks").getAsJsonObject().get("thumbnail") != null) {
            coverImageURL =
                    bookInfo.getAsJsonObject().get("imageLinks").getAsJsonObject().get("thumbnail").getAsString();
        }

        String pages = "0";
        if (bookInfo.getAsJsonObject().get("pageCount") != null) {
            pages = bookInfo.getAsJsonObject().get("pageCount").getAsString();
        }

        String description = "";
        if (bookInfo.getAsJsonObject().get("description") != null) {
            description = bookInfo.getAsJsonObject().get("description").getAsString();
        }

        List<String> subjects = new ArrayList<>();
        if (bookInfo.getAsJsonObject().get("categories") != null) {
            for (JsonElement object : bookInfo.getAsJsonObject().get("categories").getAsJsonArray().asList()) {
                subjects.add(object.getAsString());
            }
        }

        return new Book(title, bookId, publishYear, author, coverImageURL, pages, description, subjects);
    }
}
