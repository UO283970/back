package tfg.books.back.services;

import com.google.firebase.database.annotations.NotNull;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import tfg.books.back.config.RestTemplateConfig;
import tfg.books.back.model.BookCustomSerializer;
import tfg.books.back.model.BookForSearch;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookAPIService {

    @Autowired
    RestTemplateConfig restTemplateConfig;

    public List<BookForSearch> searchBooks(@NotNull String userQuery) {
        List<BookForSearch> resultOfQueryBooks = new ArrayList<>();

        String url = "https://www.googleapis.com/books/v1/volumes?q=intitle:{userQuery}&printType=books&orderBy" +
                "=relevance&key=AIzaSyBsCPK1yUlM5-Uq7yom_D74kNcJ9H2BP1M";

        String bookFromApi = restTemplateConfig.restTemplate().exchange(url.replace("{userQuery}", userQuery),
                HttpMethod.GET, null, String.class).getBody();

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(BookForSearch.class, new BookCustomSerializer());
        Gson gson = builder.create();

        assert bookFromApi != null;
        List<JsonElement> resultAsJSON =
                JsonParser.parseString(bookFromApi).getAsJsonObject().get("items").getAsJsonArray().asList();

        for (JsonElement bookDocs : resultAsJSON) {
            resultOfQueryBooks.add(gson.fromJson(bookDocs, BookForSearch.class));
        }

        return resultOfQueryBooks;
    }

}
