import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.*;

public class ProjectsManager {

    private final Gson gson;
    private final UsersManager uManager;
    private HashMap<String,Project> projects;
    private AddressGenerator addrGen;
    private final ArrayList<Triplet<String,Integer,MulticastSocket>> mSockets; // multicast chat comunications
    private final static String dataDir = "./data";
    private static String projPath;
    private static String addressGeneratorPath;


    public ProjectsManager(UsersManager uManager) throws IOException {
        //setup files paths
        Files.createDirectories(Paths.get(dataDir));
        projPath = dataDir + "/projects.json";
        addressGeneratorPath = dataDir + "/addrgen.json";
        // configure gson
        gson = new Gson();
        this.uManager = uManager;
        mSockets = new ArrayList<>();
        if(!Files.exists(Paths.get(projPath))){
            System.out.println("Persistency files not found (projects)");
            projects = new HashMap<>();
        }
        else{
            try{
                System.out.println("Persistency files found (projects)");
                Type type = new TypeToken<HashMap<String,Project>>(){}.getType();
                Reader reader = Files.newBufferedReader(Paths.get(projPath));
                projects = gson.fromJson(reader, type);
                for(Map.Entry<String,Project> p : projects.entrySet()){
                    Project project = p.getValue();
                    project.setupCards();
                    project.loadCards();
                    String notifyIp = project.getAddress();
                    int port = project.getPort();
                    MulticastSocket sock = new MulticastSocket(port);
                    sock.joinGroup(InetAddress.getByName(notifyIp));
                    sock.setSoTimeout(5000);
                    mSockets.add(new Triplet<>(notifyIp, port, sock));
                }
            }catch(IOException e){
                e.printStackTrace();
            }     
        }
        if(!Files.exists(Paths.get(addressGeneratorPath))){
            System.out.println("Persistency files not found (AddressGenerator)");
            addrGen = new AddressGenerator(224, 0, 0, 0);
        }
        else{
            try{
                System.out.println("Persistency files found (AddressGenerator)");
                Type type = new TypeToken<AddressGenerator>(){}.getType();
                Reader reader = Files.newBufferedReader(Paths.get(addressGeneratorPath));
                addrGen = gson.fromJson(reader, type);
            }catch(IOException e){
                e.printStackTrace();
            }     
        }
    }

    public HashMap<String,Project> getProjects(){
        return this.projects;
    }


    public synchronized Pair<Integer,List<String>> listProjects(String username){
        ArrayList<String> res = new ArrayList<>();
        for(Map.Entry<String,Project> p : projects.entrySet()){
            if(p.getValue().isMember(username)){
                res.add(p.getValue().getName());
            }
        }
        if(res.isEmpty()) return new Pair<>(21,null);
        // ERR user doesnt participate in any project
        return new Pair<>(0,res);
    }

    public boolean checkProject(String projName){
        return projects.containsKey(projName.toLowerCase());
    }
    
    public synchronized int createProject(String projName, String username) {
        if(projName.length()==0) throw new IllegalArgumentException("createProject : empty project name!");
        if(checkProject(projName)) // check if project already exists
            return 9;//ERR project already exists !
        String notifyIP = addrGen.generate();    //--COMPLETED
        Random r = new Random();
        int port = r.nextInt((65535-1025+1))+1025; // random port in range [1025,65535]
        Project project = new Project(projName.toLowerCase(),username.toLowerCase(), notifyIP, port);
        project.createDirectory();
        System.out.println("New project created : " + projName.toLowerCase() +" by " + username.toLowerCase());
        projects.put(projName.toLowerCase(), project);
        updateProjectsJSON(); // updates projects persistency
        updateAddressGenJson(); // updates addGen persistency
        try{
            uManager.notifyChatConnection(project, username);
        }catch(RemoteException e){
            e.printStackTrace();
        }
        try{
            MulticastSocket sock = new MulticastSocket(port);
            sock.joinGroup(InetAddress.getByName(notifyIP));
            sock.setSoTimeout(5000);
            mSockets.add(new Triplet<>(notifyIP,port,sock));
        } catch(IOException e){
            e.printStackTrace();
        }
        return 0; // ERR OK
    }

    public synchronized int addMember(String projName, String newMember, String username){
        if(!uManager.checkUser(newMember))
            return 11; // ERR user not registered on the platform
        if(!checkProject(projName))
            return 12; // ERR project doesn't exist
        Project project = projects.get(projName);
        if(project.isMember(username)){
            if(project.addMember(newMember)){
                updateProjectsJSON();
                try{
                    uManager.notifyChatConnection(project, newMember);
                }catch(RemoteException e){
                    e.printStackTrace();
                }
                return 0; 
            }
            return 14; // ERR newMember already a project member !
        }
        return 13; // ERR username not a project member !
    }

    public synchronized Pair<Integer,List<String>> showMembers(String projName, String username){
        List<String> members;
        if(!checkProject(projName)){
            Integer code = 12;
            return new Pair<>(code,null); // ERR project doesn't exist
        }
        Project project = projects.get(projName.toLowerCase());
        if(project.isMember(username)){
            members = project.getMembers();
            if(members == null) return new Pair<>(20,null); // ERR null members list
            else return new Pair<>(0,members); // ERR OK
        }
        else return new Pair<>(13,null); // ERR username not a project member !
    }

    public synchronized Pair<Integer,List<Pair<String,String>>> showCards(String projName, String username){
        if(checkProject(projName)){
            Project project = projects.get(projName.toLowerCase());
            if(project.isMember(username)){
                List<Pair<String,String>> cardsInfo = project.showCards();
                return new Pair<>(0,cardsInfo);
            }
            return new Pair<>(13,null);
            // ERR USER NOT PROJECT MEMBER
        }
        return new Pair<>(12,null);
        // ERR PROJECT DOESN'T EXIST
    }

    public synchronized Pair<Integer,Triplet<String,String,String>> showCard(String projName, String cardName, String username){
        if(checkProject(projName)){
            Project project = projects.get(projName.toLowerCase());
            if(project.isMember(username)){
                Triplet<String,String,String> cardInfo = project.getCardInfo(cardName);
                if(cardInfo!=null){
                    return new Pair<>(0,cardInfo); // ERR OK
                }
                else{
                    return new Pair<>(15,null); // ERR card not found
                }
            }
            return new Pair<>(13,null); // ERR user not project member
        }
        return new Pair<>(12,null); // ERR project doesn't exist
    }

    public synchronized int addCard(String projName, String cardName, String description, String username) throws NullPointerException{
        Project project = projects.get(projName.toLowerCase());
        if(project!=null){
            if(project.isMember(username)){
                boolean res = project.createCard(cardName, description);
                if(res){
                    updateProjectsJSON();
                    return 0;
                }
                return 28; // ERR card cannot be added
            }
            return 13; // ERR USER NOT PROJECT MEMBER
        }
        return 12; // ERR PROJECT DOESN'T EXIST
    }

    public synchronized int moveCard(String projName, String cardName, String src, String dest, String username){
        Project project = projects.get(projName.toLowerCase());
        if(project != null){
            if(project.isMember(username)){
                int res = project.moveCard(cardName, src, dest);
                if(res == 0){
                    updateProjectsJSON(); // update persistency file
                    // send notification in project members chat
                    String message = "User " + username + " moved card: "+ cardName + " from list: "+src+" to list: "+dest;
                    byte[] buffer = message.getBytes();
                    DatagramPacket packet;
                    for(Triplet<String,Integer,MulticastSocket> sock : mSockets){
                        if(sock.getfirstEl().equals(project.getAddress())){
                            try{
                                packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(sock.getfirstEl()), sock.getSecondEl());
                                sock.getThirdEl().send(packet);
                            } catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                    }
                    return res;
                }
                return res;
            }
            return 13; //ERR user not project member
        }
        return 12; // ERR project doesn't exist
    }

    public synchronized Pair<Integer,List<Card.Status>> getCardHistory(String projName, String cardName, String username){
        Project project = projects.get(projName.toLowerCase());
        if(project != null){
            if(project.isMember(username)){
                List<Card.Status> history = project.getCardHistory(cardName);
                if(history != null){
                    return new Pair<>(0,history); // ERR OK
                }
                else return new Pair<>(15,null); // ERR card not found
            }
            return new Pair<>(13,null); // ERR USER NOT PROJECT MEMBER
        }
        return new Pair<>(12,null); // ERR PROJECT DOESN'T EXIST
    }

    public synchronized Pair<Integer,String> getChatInfo(String projName, String username){
        Project project = projects.get(projName.toLowerCase());
        if(project != null) {
            if (project.isMember(username)) {
                return new Pair<>(0, project.getAddress());
            }
            return new Pair<>(13, null); // ERR USER NOT PROJECT MEMBER
        }
        return new Pair<>(12, null); // ERR PROJECT DOESN'T EXIST
    }

    public synchronized int cancelProject(String projName, String username){
        Project project = projects.get(projName.toLowerCase());
        if(project != null) {
            if (project.isMember(username)) {
                if (project.isCompleted()) {
                    projects.remove(projName.toLowerCase());
                    if (projects.isEmpty()) {
                        addrGen.reset(); // if no projects -> reset addresses !
                        updateAddressGenJson();
                    }
                    updateProjectsJSON();
                    project.delete();
                    try {
                        uManager.revokeChatConnections(project);
                    }catch(RemoteException e){
                        e.printStackTrace();
                    }
                    Triplet<String,Integer,MulticastSocket> toRemove = null;
                    for(Triplet<String,Integer,MulticastSocket> sock : mSockets){
                        if(sock.getfirstEl().equals(project.getAddress())){
                            try{
                                sock.getThirdEl().leaveGroup(InetAddress.getByName(project.getAddress()));
                                toRemove = sock;
                            }
                            catch(IOException e){
                                e.printStackTrace();
                            }
                        }
                    }
                    if(toRemove != null) mSockets.remove(toRemove);
                    return 0; // ERR OK
                }
                return 17; // ERR project is not completed
            }
            return 13; // ERR USER NOT PROJECT MEMBER
        }
        return 12; //ERR PROJECT DOESN'T EXIST
    }

    public void updateProjectsJSON(){
        try{
            Writer writer = new FileWriter(projPath);
            gson.toJson(projects, writer);
            writer.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public void updateAddressGenJson(){
        try{
            Writer writer = new FileWriter(addressGeneratorPath);
            gson.toJson(addrGen, writer);
            writer.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

}
