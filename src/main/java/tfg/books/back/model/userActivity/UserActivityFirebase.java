package tfg.books.back.model.userActivity;

import com.google.firebase.database.annotations.NotNull;

public record UserActivityFirebase(@NotNull String activityText, @NotNull int score, @NotNull String userId, @NotNull String bookId, @NotNull
                                   UserActivity.UserActivityType userActivityType,
                                   @NotNull com.google.cloud.Timestamp timestamp){}
