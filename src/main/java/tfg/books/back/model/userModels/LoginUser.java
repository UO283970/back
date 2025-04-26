package tfg.books.back.model.userModels;

import com.google.firebase.database.annotations.NotNull;

import java.util.List;

public record LoginUser(@NotNull String tokenId, @NotNull String refreshToken,@NotNull List<UserErrorLogin> userLoginErrors) {}
