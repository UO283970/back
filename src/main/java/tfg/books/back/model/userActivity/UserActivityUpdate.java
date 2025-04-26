package tfg.books.back.model.userActivity;

import com.google.firebase.database.annotations.NotNull;

public record UserActivityUpdate(@NotNull String activityText, @NotNull int score) {
}
