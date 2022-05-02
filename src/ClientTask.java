import com.google.gson.Gson;

import java.io.*;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class ClientTask implements Runnable {

    private final UsersManager uManager;
    private final ProjectsManager pManager;
	private final Socket TCPsocket;
    
    public ClientTask(UsersManager uManager,ProjectsManager pManager, Socket socket) {
		this.uManager=uManager;
        this.pManager=pManager;
		this.TCPsocket=socket;
    }
    
    @Override
    public void run() {
        //System.out.println(Thread.currentThread().getName() + " received new ClientTask");
        // boolean variable to check when user crashes ( if he does )
        String username = null;
        boolean logged = false;
        try{
            // communication channels
            BufferedReader reader = new BufferedReader(new InputStreamReader(TCPsocket.getInputStream()));
            ObjectOutputStream outputStream = new ObjectOutputStream(TCPsocket.getOutputStream());
            String line = reader.readLine();          
            //split string into tokens
            String[] tokens = line.split("\\s+"); // s+ matches one or sequence of whitespaces
            String cmd = tokens[0].toUpperCase();
            while(!(cmd.equals("LOGOUT") && tokens.length ==2)) {
                if (cmd.equals("LOGIN") && tokens.length == 3){
                    //System.out.println("WORTH Login request received.");
                    Triplet<Integer,List<User>,List<Pair<String,Integer>>> res = uManager.usrLogin(tokens[1], tokens[2], pManager);
                    outputStream.writeObject(res);
                    logged = true;
                    username = tokens[1];
                    // if login failed => terminate thread
                    if(res.getfirstEl()!=0){
                        logged = false;
                        username = null;
                        System.out.println("WORTH: Login attempt failed: " + Thread.currentThread().getName());
                    }
                }
                else if(cmd.equals("LISTPROJECTS") && tokens.length == 2){
                    Pair<Integer, List<String>> res = pManager.listProjects(tokens[1]);
                    outputStream.writeObject(res);
                }
                else if(cmd.equals("CREATEPROJECT") && tokens.length == 3){
                    int res = pManager.createProject(tokens[1], tokens[2]);
                    outputStream.writeObject(res);
                }
                else if(cmd.equals("ADDMEMBER") && tokens.length == 4){
                    int res = pManager.addMember(tokens[1],tokens[2],tokens[3]);
                    outputStream.writeObject(res);
                }
                else if(cmd.equals("SHOWMEMBERS") && tokens.length == 3){
                    Pair<Integer, List<String>> res = pManager.showMembers(tokens[1],tokens[2]);
                    outputStream.writeObject(res);
                }
                else if(cmd.equals("SHOWCARDS") && tokens.length == 3){
                    Pair<Integer,List<Pair<String,String>>> res = pManager.showCards(tokens[1],tokens[2]);
                    outputStream.writeObject(res);
                }
                else if(cmd.equals("SHOWCARD") && tokens.length == 4){
                    Pair<Integer,Triplet<String,String,String>> res = pManager.showCard(tokens[1],tokens[2],tokens[3]);
                    outputStream.writeObject(res);
                }
                else if(cmd.equals("ADDCARD") && tokens.length == 5){
                    int res = pManager.addCard(tokens[1],tokens[2],tokens[3],tokens[4]);
                    outputStream.writeObject(res);
                }
                else if(cmd.equals("MOVECARD") && tokens.length == 6){
                    int res = pManager.moveCard(tokens[1],tokens[2],tokens[3],tokens[4],tokens[5]);
                    outputStream.writeObject(res);
                }
                else if(cmd.equals("GETCARDHISTORY") && tokens.length == 4){
                    Pair<Integer,List<Card.Status>> res = pManager.getCardHistory(tokens[1],tokens[2],tokens[3]);
                    outputStream.writeObject(res);
                }
                else if(cmd.equals("READCHAT") && tokens.length == 3){
                    Pair<Integer,String> res = pManager.getChatInfo(tokens[1],tokens[2]);
                    outputStream.writeObject(res);
                }
                else if(cmd.equals("SENDCHATMSG") && tokens.length == 3){
                    Pair<Integer,String> res = pManager.getChatInfo(tokens[1],tokens[2]);
                    outputStream.writeObject(res);
                }
                else if(cmd.equals("CANCELPROJECT") && tokens.length == 3){
                    int res = pManager.cancelProject(tokens[1],tokens[2]);
                    outputStream.writeObject(res);
                }
                else{
                    System.out.println("WORTH: Unknown command received !!!!!");
                }

                line = reader.readLine();

                //System.out.println("ClientTask read: " + line);
                tokens = line.split("\\s+");
                cmd = tokens[0].toUpperCase();

            }
            // EXECUTE LOGOUT
            int res = uManager.usrLogout(tokens[1]);
            outputStream.writeObject(res);
            logged = false;
            username = null;
        }catch (NullPointerException | SocketException e){
            if(logged){
                int res = uManager.usrLogout(username);
                if(res != 0){
                    System.out.println("User: "+username+" crashed, emergency logout failed");
                }
                else System.out.println("User: "+username+" crashed, emergency logout was OK");
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }
        //System.out.println("ClientTask ended: " + Thread.currentThread().getName());
    }
}
