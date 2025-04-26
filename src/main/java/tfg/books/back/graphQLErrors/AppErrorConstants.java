package tfg.books.back.graphQLErrors;

import graphql.ErrorClassification;

public enum AppErrorConstants implements ErrorClassification {

    INVALID_LOGIN_CREDENTIALS,
    AUTHENTICATION_ERROR,
    UNKNOWN_ERROR

}
