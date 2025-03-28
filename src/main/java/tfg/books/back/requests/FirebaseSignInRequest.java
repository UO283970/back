package tfg.books.back.requests;

public record FirebaseSignInRequest(String email, String password, boolean returnSecureToken) {}

