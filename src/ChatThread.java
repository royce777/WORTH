import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;

public class ChatThread implements Runnable{
    private Chat chat;
    private DatagramPacket packet;
    byte[] buffer;

    public ChatThread(Chat chat){
        this.chat = chat;
        this.buffer = new byte[4096];
    }

    @Override
    public void run(){
        while(chat.status()){
            try {
                packet = new DatagramPacket(buffer, buffer.length);
                chat.getSocket().receive(packet);
                String message = new String(packet.getData(),0,packet.getLength());
                chat.addMessage(message);
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }
}
