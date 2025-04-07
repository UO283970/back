package tfg.books.back.GrpahQLResolver;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import tfg.books.back.GrpahQLErrors.AppErrorConstants;
import tfg.books.back.GrpahQLErrors.GraphQLCustomError;

@Component
public class CustomGraphQLResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(@NonNull Throwable ex, @NonNull DataFetchingEnvironment env) {
        if (ex instanceof GraphQLCustomError) {
            return GraphQLError.newError().message(ex.getMessage()).errorType(((GraphQLCustomError) ex).getErrorConstants()).build();
        } else if (ex instanceof AuthenticationException) {
            return GraphQLError.newError().message(ex.getMessage()).errorType(AppErrorConstants.AUTHENTICATION_ERROR).build();
        }

        return GraphQLError.newError().message(ex.getMessage()).errorType(AppErrorConstants.UNKNOWN_ERROR).build();
    }

}
