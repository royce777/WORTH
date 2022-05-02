import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegInterface extends Remote{
	
	//REQUIRES: nickname != null && password!=null
	//MODIFIES: this
	//EFFECTS: this_users = this_users + {<nickname,password>}
	//THROWS:	RemoteException 
	//			NullpointerException 
	//RETURNS: error code in case of a problem.
	public int regUser(String nickname, String password) throws RemoteException, NullPointerException;
	public void regCallback(EventNotificationInterface client, String username) throws RemoteException;
	public void unregCallback(String username) throws RemoteException;
}