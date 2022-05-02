public class ServerUnreachableException extends  RuntimeException{
    public ServerUnreachableException(String errorMessage, Throwable err){
        super(errorMessage,err);
    }
}
