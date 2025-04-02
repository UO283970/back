package tfg.books.back.model;

import com.google.firebase.database.annotations.NotNull;

public record UserActivityUpdate(@NotNull String activityText, @NotNull int score) {
}
