import java.io.BufferedWriter;
import java.io.IOException;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.*;
import java.rmi.ConnectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Client {
    private static final int RMIport = 6789; //RMI port
    private static final int TCPport = 6790; //TCP port for connection
    private static final String ServerAddress = "127.0.0.1";

    private List<User> users; //Listing users in the system
    private String username; //Saving the logged in username
    private final ArrayList<Chat> chats;
    private Socket socket;
    private BufferedWriter writer;
    private ObjectInputStream inputStream;
    private ThreadPoolExecutor chatExec;
    public boolean logged;

    public Client(){
        this.username = null;
        this.chats = new ArrayList<>();
        this.logged = false;
    }

    public List<User> getUsers(){
        return this.users;
    }

    public void addChat(Chat chat){
        chats.add(chat);
        chatExec.execute(new ChatThread(chat));
    }

    public void removeChat(String address, int port){
        Chat toRemove = null;
        for(Chat c : chats){
            if(c.getAddress().equals(address) && c.getPort() == port){
                toRemove = c;
            }
        }
        if(toRemove!=null) {
            toRemove.stop();
            chats.remove(toRemove);
        }
    }
	@SuppressWarnings("unchecked")
    public void start() {
        try {
            //Setup RMI
            System.out.println("Setting up RMI");
            Registry registry = LocateRegistry.getRegistry(RMIport);
            RegInterface stub = (RegInterface) registry.lookup("USER-REG");
            //Input scanner
            Scanner scan = new Scanner(System.in);
            System.out.println("Scanner ok");
            //Setup Callback System
            EventNotificationInterface cbInterface = new EventNotificationImp(this);
            EventNotificationInterface stubCallBack = (EventNotificationInterface) UnicastRemoteObject.exportObject(cbInterface, 0);
            // Scan commands
            System.out.print("$>");
            String line = scan.nextLine();
            //split string into tokens
            String[] tokens = line.split("\\s+"); // s+ matches one or sequence of whitespaces
            while (!tokens[0].equalsIgnoreCase("EXIT")) {
                if(tokens[0].equalsIgnoreCase("REGISTER") && tokens.length == 3){
                    if (logged) {
                        System.out.println("You are already logged");
                    }
                    else {
                        int res = stub.regUser(tokens[1], tokens[2]);
                        if (res == 0) System.out.println("New user successfully registered");
                        else printError(res);
                    }
                }
                else if (tokens[0].equalsIgnoreCase("LOGIN") && tokens.length == 3) {
                    if (logged) {
                        System.out.println("You are already logged");
                    }
                    else {
                        int res = login(line);
                        if (res == 0) {
                            logged = true;
                            stub.regCallback(stubCallBack, username);
                            System.out.println("Success !");
                        } else printError(res);
                    }
                }
                else if(tokens[0].equalsIgnoreCase("LOGOUT")){
                    if (!logged) {
                        System.out.println("You are not logged");
                    }
                    else {
                        int res = logout(tokens[0].toLowerCase());
                        if (res == 0) System.out.println("You logged out.");
                        else printError(res);
                    }
                }
                else if (tokens[0].equalsIgnoreCase("LISTUSERS") && tokens.length == 1) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        listUsers();
                    }

                } else if (tokens[0].equalsIgnoreCase("LISTONLINEUSERS") && tokens.length == 1) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        listOnlineUsers();
                    }
                } else if (tokens[0].equalsIgnoreCase("LISTPROJECTS") && tokens.length == 1) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        line = line + " " + username;
                        writer.write(line.toLowerCase());
                        writer.newLine();
                        writer.flush();
                        Pair<Integer,List<String>> res = (Pair<Integer,List<String>>) inputStream.readObject();
                        if(res.getfirstEl() == 0){
                            System.out.println("Your projects : ");
                            for(String proj : res.getSecondEl()){
                                System.out.println(proj);
                            }
                        }
                        else{
                            printError(res.getfirstEl());
                        }
                    }
                } else if (tokens[0].equalsIgnoreCase("CREATEPROJECT") && tokens.length == 2) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        line = line + " " + username;
                        writer.write(line.toLowerCase());
                        writer.newLine();
                        writer.flush();
                        int res = (Integer) inputStream.readObject();
                        if(res == 0) System.out.println("Project " + tokens[1] +" created.");
                        else printError(res);
                    }
                } else if (tokens[0].equalsIgnoreCase("ADDMEMBER") && tokens.length == 3) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        line = line + " " + username;
                        writer.write(line.toLowerCase());
                        writer.newLine();
                        writer.flush();
                        int res = (Integer) inputStream.readObject();
                        if(res == 0) System.out.println("New member has been added to " + tokens[1]);
                        else printError(res);
                    }
                } else if (tokens[0].equalsIgnoreCase("SHOWMEMBERS") && tokens.length == 2) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        line = line + " " + username;
                        writer.write(line.toLowerCase());
                        writer.newLine();
                        writer.flush();
                        Pair<Integer,List<String>> res = (Pair<Integer,List<String>>) inputStream.readObject();
                        if(res.getfirstEl()==0){
                            System.out.println("Members of project " + tokens[1] + ":");
                            for(String proj : res.getSecondEl()){
                                System.out.println(proj);
                            }
                        }
                        else printError(res.getfirstEl());
                    }
                } else if (tokens[0].equalsIgnoreCase("SHOWCARDS") && tokens.length == 2) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        line = line + " " + username;
                        writer.write(line.toLowerCase());
                        writer.newLine();
                        writer.flush();
                        Pair<Integer,List<Pair<String,String>>> res = (Pair<Integer,List<Pair<String,String>>>) inputStream.readObject();
                        if(res.getfirstEl()==0){
                            for(Pair<String,String> card : res.getSecondEl()){
                                System.out.println("Card: " + card.getfirstEl()+" |Status: "+card.getSecondEl());
                            }
                        }
                        else printError(res.getfirstEl());
                    }
                } else if (tokens[0].equalsIgnoreCase("SHOWCARD") && tokens.length == 3) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        line = line + " " + username;
                        writer.write(line.toLowerCase());
                        writer.newLine();
                        writer.flush();
                        Pair<Integer,Triplet<String,String,String>> res = (Pair<Integer,Triplet<String,String,String>>) inputStream.readObject();
                        if(res.getfirstEl() == 0){
                            Triplet<String,String,String> card = res.getSecondEl();
                            System.out.println("Card: " + card.getfirstEl() + " |Description: " + card.getSecondEl() + " |Status: " + card.getThirdEl());
                        }
                        else printError(res.getfirstEl());
                    }
                } else if (tokens[0].equalsIgnoreCase("ADDCARD") && tokens.length == 4) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        line = line + " " + username;
                        writer.write(line.toLowerCase());
                        writer.newLine();
                        writer.flush();
                        int res = (Integer) inputStream.readObject();
                        if(res == 0){
                            System.out.println("Card successfully added.");
                        }
                        else printError(res);
                    }
                } else if (tokens[0].equalsIgnoreCase("MOVECARD") && tokens.length == 5) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        line = line + " " + username;
                        writer.write(line.toLowerCase());
                        writer.newLine();
                        writer.flush();
                        int res = (Integer) inputStream.readObject();
                        if(res == 0){
                            System.out.println("Card moved.");
                        }
                        else printError(res);
                    }
                } else if (tokens[0].equalsIgnoreCase("GETCARDHISTORY") && tokens.length == 3) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        line = line + " " + username;
                        writer.write(line.toLowerCase());
                        writer.newLine();
                        writer.flush();
                        Pair<Integer,List<Card.Status>> res = (Pair<Integer,List<Card.Status>>) inputStream.readObject();
                        if(res.getfirstEl() == 0){
                            System.out.println("Card history : ");
                            for(Card.Status stat : res.getSecondEl()){
                                System.out.println(stat.name());
                            }
                        }
                        else printError(res.getfirstEl());
                    }
                } else if (tokens[0].equalsIgnoreCase("READCHAT") && tokens.length == 2) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        line = line + " " + username;
                        writer.write(line.toLowerCase());
                        writer.newLine();
                        writer.flush();
                        Pair<Integer,String> res = (Pair<Integer,String>) inputStream.readObject();
                        if(res.getfirstEl() == 0){
                            for(Chat chat : chats){
                                if(chat.getAddress().equals(res.getSecondEl())) {
                                    chat.printMessages();
                                }
                            }
                        }
                        else printError(res.getfirstEl());
                    }
                }
                else if (tokens[0].equalsIgnoreCase("SENDCHATMSG") && tokens.length == 2) {
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        line = line + " " + username;
                        writer.write(line.toLowerCase());
                        writer.newLine();
                        writer.flush();
                        Pair<Integer,String> res = (Pair<Integer,String>) inputStream.readObject();
                        if(res.getfirstEl() == 0){
                            if(sendChatMsg(res.getSecondEl())==0){
                                System.out.println("Message has been sent !");
                            }
                            else System.out.println("An error occurred while sending your message !");
                        }
                        else printError(res.getfirstEl());
                    }
                }
                else if(tokens[0].equalsIgnoreCase("CANCELPROJECT") && tokens.length == 2){
                    if(!logged){
                        System.out.println("ERROR: Unauthorized operation.");
                    }
                    else {
                        line = line + " " + username;
                        writer.write(line.toLowerCase());
                        writer.newLine();
                        writer.flush();
                        int res = (Integer) inputStream.readObject();
                        if(res == 0){
                            System.out.println("Project canceled.");
                        }
                        else printError(res);
                    }
                }
                else if(tokens[0].equalsIgnoreCase("HELP")){
                    printCommands();
                }
                else{
                    System.out.println("ERROR: unknown command, use 'help' ");
                }
                System.out.print("$>");
                line = scan.nextLine();
                tokens = line.split("\\s+");
                //System.out.println("tokens length = " + tokens.length);
            }
            //CATCH EXCEPTION AND TERMINATE CHAT THREADS
        }catch(EOFException | ServerUnreachableException | SocketException | ConnectException e){
            System.out.println("ERROR : Server unreachable, try later");
            terminateChats();
        } catch (Exception e) {  e.printStackTrace(); }
    }
	@SuppressWarnings("unchecked")
    public int login(String cmd){
        String[] tokens = cmd.split("\\s+");
        try{
            // Connection TCP socket
            this.socket = new Socket(ServerAddress, TCPport);
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            inputStream = new ObjectInputStream(socket.getInputStream());
            //System.out.println("LOGIN : Streams and socket are ready.");
            // write login command to server
            writer.write(cmd);
            writer.newLine();
            writer.flush();
            //System.out.println("LOGIN command sent to server");
            Triplet<Integer,List<User>,List<Pair<String,Integer>>> res =(Triplet<Integer,List<User>,List<Pair<String,Integer>>>) inputStream.readObject();
            int code = res.getfirstEl();
            if(code==0){
                chatExec = new ThreadPoolExecutor(10,200,20000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
                username = tokens[1];
                users = res.getSecondEl();
                for(Pair<String,Integer> c : res.getThirdEl()){
                    MulticastSocket sock = new MulticastSocket(c.getSecondEl());
                    sock.joinGroup(InetAddress.getByName(c.getfirstEl()));
                    Chat cc = new Chat(c.getfirstEl(),c.getSecondEl(),sock);
                    chats.add(cc);
                    chatExec.execute(new ChatThread(cc));
                }
                return code;
            }
            return code;
        } catch(SocketException | ConnectException e){
            throw new ServerUnreachableException("Cannot connect to server", e);
        }
        catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
        return 99;
    }

    public int logout(String cmd){
        try {
            cmd = cmd + " " + username;
            writer.write(cmd);
            writer.newLine();
            writer.flush();
            int res = (Integer) inputStream.readObject();
            if (res == 0) {
                // CLOSE CONNECTION AFTER LOGOUT
                writer.close();
                inputStream.close();
                socket.close();
                //System.out.println("LOGOUT : Streams and socket closed.");
                username = null;
                logged = false;
                terminateChats();
            }
            return res;
        }catch(SocketException | ConnectException e){
            throw new ServerUnreachableException("Cannot connect to server", e);
        }
        catch (IOException | ClassNotFoundException e) {
            System.out.println("An error occurred");
            throw new ServerUnreachableException("Cannot connect to server", e);
        }
    }

    public void terminateChats(){
        for(Chat c : chats){
            c.stop();
        }
        if(chatExec != null) {
            chatExec.shutdown();
            //System.out.println("Chat executor terminated !");
        }
    }

    public void listUsers(){
        for(User u : users){
            if(u.online)
                System.out.println(u.getUsername() + " STATUS: online");
            else System.out.println(u.getUsername() + " STATUS: offline");
        }
    }
    public void listOnlineUsers(){
        for(User u : users){
            if(u.online)
                System.out.println(u.getUsername() + " STATUS: online");
        }
    }

    public int sendChatMsg(String address){
        DatagramPacket packet;
        Scanner scan;
        for(Chat c : chats){
            if(c.getAddress().equals(address)){
                scan = new Scanner(System.in);
                System.out.println("Insert your message: ");
                String tmp = scan.nextLine();
                if(tmp.length()==0){
                    //scan.close();
                    System.out.println("ERROR: you cannot send empty messages !");
                    return -1;
                }
                String message = username + " said: " + tmp;
                byte[] buffer = message.getBytes();
                try {
                    packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(address),c.getPort());
                    c.getSocket().send(packet);
                    return 0;
                }
                catch(IOException e){
                    e.printStackTrace();
                    return -1;
                }
            }
        }
        return -1;
    }

    public void printCommands(){
        System.out.println("WORTH CLI Interaction commands: ");
        System.out.println("COMMANDS ARE NOT CASE SENSITIVE !");
        System.out.println("-------------------------------------------------------------");
        System.out.println("REGISTER 'username' 'password' -> Command used to register a new user.");
        System.out.println("LOGIN 'username' 'password' -> Command used to login.");
        System.out.println("LOGOUT -> Command used to logout.");
        System.out.println("LISTUSERS -> Command used to list all the users");
        System.out.println("LISTONLINEUSERS -> Command used to list online users.");
        System.out.println("LISTPROJECTS -> Command used to list the projects where user is member");
        System.out.println("CREATEPROJECT 'project_name' -> Command used to create a new project");
        System.out.println("ADDMEMBER 'project_name' 'new_member_nickname' -> Command used to add a user as a member to the project");
        System.out.println("SHOWMEMBERS 'project_name' -> Command used to list the members of the project");
        System.out.println("SHOWCARDS 'project_name' -> Command used to list the cards of the project");
        System.out.println("SHOWCARD 'project_name' 'card_name' -> Command used to show information about a specific card in the project");
        System.out.println("ADDCARD 'project_name' 'card_name' -> Command used to add a new card to the project");
        System.out.println("MOVECARD 'project_name' 'card_name' 'source_list' 'destination_list' -> Command used to move a card from a list to another (POSSIBLE LISTS: TODO, INPROGRESS, TOBEREVISED, DONE)");
        System.out.println("GETCARDHISTORY 'project_name' 'card_name' -> Command used to get the history of the movements of the specified card in the project");
        System.out.println("READCHAT 'project_name' -> Command used to read the messages sent in the project's chat");
        System.out.println("SENDCHATMSG 'project_name' -> Command used to send a message in the project's chat");
        System.out.println("CANCELPROJECT 'project_name' -> Command used to cancel a project.");
        System.out.println("-------------------------------------------------------------");
    }

    public void printError(int err){
        String response;
        switch(err) {
            case 0:
                response = "This should not happenen (ERRCODE = 0 )";
                break;
            case 9:
                response = "ERROR: project already exists";
                break;
            case 10:
                response = "ERROR: user already registered";
                break;
            case 11:
                response = "ERROR: user not registered";
                break;
            case 12:
                response = "ERROR: project doesn't exist";
                break;
            case 13:
                response = "ERROR: user is not a project member";
                break;
            case 14:
                response = "ERROR: newMember already a project member";
                break;
            case 15:
                response = "ERROR: card not found";
                break;
//            case 16:
//                response = "ERROR: card not found";
//                break;
            case 17:
                response = "ERROR: project is not completed";
                break;
            case 18:
                response = "ERROR: user already logged";
                break;
            case 19:
                response = "ERROR: wrong password";
                break;
            case 20:
                response = "ERROR: user offline";
                break;
            case 21:
                response = "ERROR: user doesn't participate in any project";
                break;
            case 22:
                response = "ERROR: no users on the platform";
                break;
            case 23:
                response = "ERROR: no users online";
                break;
            case 24:
                response = "ERROR: invalid card movement source list ";
                break;
            case 25:
                response = "ERROR: card is in another list actually ";
                break;
            case 26:
                response = "ERROR: invalid card destination list ";
                break;
            case 27:
                response = "ERROR: card is already done, cant move ";
                break;
            case 28:
                response = "ERROR: card cannot be added ( check its name ) ";
                break;
            default:
                response = "WARNING: UNKNOWN ERROR CODE";
                break;
        }
        System.out.println(response);
    }

    public static void main(String[] args) {
        Client client = new Client();
        System.out.println("Starting client !");
        client.start();
        System.out.println("Client is terminating...");
        System.exit(0);
    }
}
