import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Chat {
    private String ip;
    private int port;
    private ArrayList<String> messages;
    private MulticastSocket socket;
    private AtomicBoolean active;

    public Chat(String ip, int port, MulticastSocket socket){
        this.ip = ip;
        this.port = port;
        this.socket = socket;
        messages = new ArrayList<>();
        active = new AtomicBoolean(true);
    }
    public void stop(){
        active.set(false);
    }
    public boolean status(){
        return active.get();
    }
    public MulticastSocket getSocket(){
        return this.socket;
    }
    public void addMessage(String message){
        messages.add(message);
    }
    public List<String> getChat(){
        return this.messages;
    }
    public String getAddress(){
        return this.ip;
    }
    public int getPort(){
        return this.port;
    }
    public void printMessages(){
        for(String mess : messages) System.out.println(mess);
    }
    public void send(String message) throws IOException {
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(ip), port);
        socket.send(packet);
    }
}
