import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;

public class EventNotificationImp extends RemoteObject implements EventNotificationInterface {
    private Client client;
    public EventNotificationImp(Client client){
        super();
        this.client = client;
    }

    @Override
    public void updateOnlineUsers(String username, boolean online) throws RemoteException {
        boolean found = false;
        for(User u : client.getUsers()){
            if(u.getUsername().equalsIgnoreCase(username)){
                u.setStatus(online);
                found = true;
            }
        }
        if(!found) client.getUsers().add(new User(username));
    }
    public void newChat(String address, int port){
        MulticastSocket sock;
        try {
            sock = new MulticastSocket(port);
            sock.joinGroup(InetAddress.getByName(address));
            sock.setSoTimeout(2000);
            Chat chat = new Chat(address,port,sock);
            client.addChat(chat);
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
    public void removeChat(String address, int port){
        client.removeChat(address, port);
    }
}
