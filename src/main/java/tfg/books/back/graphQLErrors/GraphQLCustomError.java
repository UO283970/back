package tfg.books.back.graphQLErrors;

public class GraphQLCustomError extends RuntimeException {

    private final AppErrorConstants errorConstants;

    public GraphQLCustomError(String message, AppErrorConstants errorConstants){
        super(message);
        this.errorConstants = errorConstants;
    }

    public AppErrorConstants getErrorConstants() {
        return errorConstants;
    }
}
