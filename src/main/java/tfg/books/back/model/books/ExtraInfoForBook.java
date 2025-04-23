package tfg.books.back.model.books;

import com.google.firebase.database.annotations.NotNull;

import java.util.List;

public record ExtraInfoForBook(@NotNull int userScore, @NotNull Double meanScore, @NotNull int numberOfReviews, @NotNull
                               List<String> userProfilePictures,@NotNull int progress,@NotNull int numberOfRatings) {
}
