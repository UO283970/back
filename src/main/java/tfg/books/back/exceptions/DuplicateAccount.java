package tfg.books.back.exceptions;

public class DuplicateAccount extends RuntimeException{

    public DuplicateAccount(String message){
        super(message);
    }
}
