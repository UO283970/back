package tfg.books.back.model.user;

import com.google.firebase.database.annotations.NotNull;

import java.util.List;

public record RegisterUser(@NotNull String tokenId, @NotNull String refreshToken, @NotNull List<UserErrorRegister> userRegisterErrors) {}
