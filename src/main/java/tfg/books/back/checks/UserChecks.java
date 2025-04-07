package tfg.books.back.checks;

import tfg.books.back.model.userModels.UserErrorLogin;
import tfg.books.back.model.userModels.UserErrorRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UserChecks {

    public static List<UserErrorLogin> loginCheck(String email, String password) {
        List<UserErrorLogin> userErrorLogins = new ArrayList<>();


        UserErrorLogin emailError = emailCheck(email);
        if (emailError != null) {
            userErrorLogins.add(emailError);
        }

        UserErrorLogin passWordErrors = passwordCheck(password);
        if (passWordErrors != null) {
            userErrorLogins.add(passWordErrors);
        }

        return userErrorLogins;
    }

    public static List<UserErrorRegister> registerCheck(String email, String password, String repeatedPassword,
                                                        String userAlias) {
        List<UserErrorRegister> userErrorLogins = new ArrayList<>();


        UserErrorLogin emailError = emailCheck(email);
        if (emailError != null) {
            userErrorLogins.add(UserErrorRegister.valueOf(emailError.toString()));
        }

        UserErrorRegister passWordErrors = passwordCheck(password, repeatedPassword);
        if (passWordErrors != null) {
            userErrorLogins.add(passWordErrors);
        }

        UserErrorRegister userAliasError = userAliasCheck(userAlias);
        if (userAliasError != null) {
            userErrorLogins.add(userAliasError);
        }

        return userErrorLogins;
    }

    private static UserErrorLogin emailCheck(String email) {
        if (email.isBlank()) {
            return UserErrorLogin.EMPTY_EMAIL;
        }

        Pattern pattern = Pattern.compile("[a-zA-Z0-9_.±]+@[a-zA-Z0-9-]+.[a-zA-Z0-9-.]+");
        if (!pattern.matcher(email).find()) {
            return UserErrorLogin.INVALID_EMAIL;
        }

        return null;
    }

    private static UserErrorLogin passwordCheck(String password) {

        if (password.isBlank()) {
            return UserErrorLogin.EMPTY_PASSWORD;
        }

        return null;
    }

    private static UserErrorRegister passwordCheck(String password, String repeatedPassword) {

        if (password.isBlank()) {
            return UserErrorRegister.EMPTY_PASSWORD;
        }

        if(!password.equals(repeatedPassword)){
            return UserErrorRegister.REPEATED_PASSWORD;
        }

        if (password.length() < 8) {
            return UserErrorRegister.LONGITUDE_PASSWORD;
        }

        Boolean hasUpperCase = Pattern.compile("[A-Z]").matcher(password).find();
        Boolean hasLowerCase =  Pattern.compile("[a-z]").matcher(password).find();
        Boolean hasNumbers =  Pattern.compile("[1-9]").matcher(password).find();
        Boolean hasNonAlphas =  Pattern.compile("\\w").matcher(password).find();

        if(!(hasUpperCase && hasLowerCase && hasNumbers && hasNonAlphas)){
            return UserErrorRegister.INVALID_PASSWORD;
        }

        return null;
    }

    private static UserErrorRegister userAliasCheck(String userAlias) {

        if (userAlias.isBlank()) {
            return UserErrorRegister.EMPTY_USER_ALIAS;
        }

        return null;
    }

}
