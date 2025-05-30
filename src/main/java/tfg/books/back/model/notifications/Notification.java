package tfg.books.back.model.notifications;

import com.google.firebase.database.annotations.NotNull;
import tfg.books.back.model.user.UserForProfile;

public record Notification(@NotNull String notificationId, @NotNull UserForProfile user, @NotNull NotificationsTypes notificationType,@NotNull String timeStamp) {
}
