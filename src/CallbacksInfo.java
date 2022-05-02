public class CallbacksInfo {
    private EventNotificationInterface client;
    private String username;

    public CallbacksInfo(EventNotificationInterface client, String username){
        this.client = client;
        this.username = username;
    }

    public EventNotificationInterface getClient(){
        return this.client;
    }
    public String getUser(){
        return this.username;
    }

}
