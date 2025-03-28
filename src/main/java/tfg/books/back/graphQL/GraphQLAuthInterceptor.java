package tfg.books.back.graphQL;

import java.util.Optional;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import reactor.core.publisher.Mono;

@Component
public class GraphQLAuthInterceptor implements WebGraphQlInterceptor {
    private static final String USER_ID_CLAIM = "user_id";
    private final FirebaseAuth firebaseAuth;

    public GraphQLAuthInterceptor(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.replace("Bearer ", "");
            Optional<String> userId = extractUserIdFromToken(token);

            if (userId.isPresent()) {
                var authentication = new UsernamePasswordAuthenticationToken(userId.get(), null, null);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }else{
                return null;
            }

        }

        return chain.next(request);
    }
    

    private Optional<String> extractUserIdFromToken(String token) {
        try {
            FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(token, true);
            String userId = String.valueOf(firebaseToken.getClaims().get(USER_ID_CLAIM));
            return Optional.of(userId);
        } catch (FirebaseAuthException exception) {
            return Optional.empty();
        }
    }
}