import java.rmi.Remote;
import java.rmi.RemoteException;

public interface EventNotificationInterface extends Remote{
    // used to update client users list on every login/logout/registration
    // of a new user
    void updateOnlineUsers(String username, boolean online) throws RemoteException;
    // used in case a user becomes a member of a new projects 
    // -> he configures his chat multicast
    void newChat(String address, int port) throws RemoteException;
    // used in case a user deletes a project
    // -> all members of that project must delete related chat !
    void removeChat(String address, int port) throws RemoteException; 

}
