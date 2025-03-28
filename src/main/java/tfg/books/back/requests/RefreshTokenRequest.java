package tfg.books.back.requests;

public record RefreshTokenRequest(String grant_type, String refresh_token) {
}
