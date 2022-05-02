import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersManager extends RemoteServer implements RegInterface{

	private static final long serialVersionUID = -6559344702339809559L;
	
	private final ArrayList<CallbacksInfo> clientCallbacks;
	//runtime users data structures
    private HashMap<String, String> passwords;
	private HashMap<String, User> users;

	//persistent files
	private static final String dataDir = "./data";
	private static String pwPath ;
	private static String usrPath ;

    
    public UsersManager() throws IOException {
		Files.createDirectories(Paths.get(dataDir));
    	usrPath = dataDir + "/users.json";
    	pwPath = dataDir + "/passwords.json";
		//if persistency files are not present
        if(!Files.exists(Paths.get(pwPath)) || !Files.exists(Paths.get(usrPath))){
            System.out.println("Persistency files not found (users)");
            passwords = new HashMap<>();
			users = new HashMap<>();
        }
        else{
            System.out.println("Persistency files found (users)");
			passwords = new HashMap<>();
            users = new HashMap<>();
            
            Gson gson = new Gson();

            //pw file
            try {
				Type type = new TypeToken<HashMap<String, String>>(){}.getType();
				Reader reader = Files.newBufferedReader(Paths.get(pwPath));
				passwords = gson.fromJson(reader, type);
			} catch (IOException e) {
				e.printStackTrace();
            }
            //usr file
			try {
				Type type = new TypeToken<HashMap<String, User>>(){}.getType();
				Reader reader = Files.newBufferedReader(Paths.get(usrPath));
				users = gson.fromJson(reader, type);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		clientCallbacks = new ArrayList<>();
    }
    
    public synchronized int regUser(String username, String password) throws RemoteException{
		if (username.equals("") || password.equals("")) return 9;
		username = username.toLowerCase();
		if (passwords.containsKey(username)) return 10; // ERR user already registered
		if (passwords.put(username, hashPw(username + password)) == null) {
			if (!users.containsKey(username)) {
				users.put(username, new User(username));
				updateOnlineUsers(username,false);
				//update users and passwords persistency files
				updatePwJSON();
				updateUsrJSON();
				System.out.println("Persistency system: new user registration !");
				return 0; // ERR OK
			}
			else return 10; // ERR user already registered
		}
		else return 19; // ERR wrong password
	}

	public synchronized Triplet<Integer,List<User>,List<Pair<String,Integer>>> usrLogin(String username, String password, ProjectsManager pManager){
		username = username.toLowerCase();
		if (passwords.containsKey(username)) {
			if (passwords.get(username).equals(hashPw(username + password))) {
				if (users.get(username).online) return new Triplet<>(18,null,null); // ERR user already logged
				users.get(username).online = true;
				HashMap<String,Project> projects = pManager.getProjects();
				List<User> usrs = new ArrayList<>(users.values());
				List<Pair<String,Integer>> chats = new ArrayList<>();
				for(Map.Entry<String,Project> p : projects.entrySet()){
					Project project = p.getValue();
					if(project.isMember(username.toLowerCase()))
						chats.add(new Pair<>(project.getAddress(),project.getPort()));
				}
				updateOnlineUsers(username,true);
				return new Triplet<>(0,usrs,chats); //ERR OK
			}
			else return new Triplet<>(19,null,null); // ERR wrong password
		} else {
			return new Triplet<>(11,null,null); // ERR user not registered
		}
	}

	public synchronized int usrLogout(String username) {
		username = username.toLowerCase();
		if (users.containsKey(username)) {
			if(users.get(username).online) {
				users.get(username).online = false;
				updateOnlineUsers(username,false);
				return 0; //ERR OK
			}
			return 20; // ERR user offline
		} else {
			return 11; // ERR user not registered on the platform
		}
	}


	private static String hashPw(String password) {
		byte[] hash = null;
		try {
			//one-way hashing
			//hashing
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		if (hash!=null) {
			//creo la stringa ottenuta dal byte array restituito dal digest
			StringBuffer hexStr = new StringBuffer();
		    for (int i = 0; i < hash.length; i++) {
		    	String hex = Integer.toHexString(0xff & hash[i]);
		    	if(hex.length() == 1) hexStr.append('0');
		        hexStr.append(hex);
		    }
		    return hexStr.toString();
		}
		else return null;
	}

	public boolean checkUser(String username){
		return (users.containsKey(username.toLowerCase()));
	}


	public void updatePwJSON(){
		try{
			Gson gson = new Gson();
			Writer writer = new FileWriter(pwPath);
			gson.toJson(passwords, writer);
			writer.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}



	public void updateUsrJSON(){
		try{
			Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
			Writer writer = new FileWriter(usrPath);
			gson.toJson(users, writer);
			writer.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}


	public synchronized void updateOnlineUsers(String username,boolean online){
    	List<CallbacksInfo> toRemove = new ArrayList<>();
    	for(CallbacksInfo c : clientCallbacks){
    		EventNotificationInterface client = c.getClient();
    		try{
    			client.updateOnlineUsers(username,online);
			}
    		catch(ConnectException e){
				//System.out.println("Connect exception avoided");
				// Server cant connect to that client -> remove him from cb.
				toRemove.add(c);
			}
    		catch(RemoteException e){
				e.printStackTrace();
			}
		}
    	for(CallbacksInfo c : toRemove){
    		clientCallbacks.remove(c);
		}
	}
	public synchronized void regCallback(EventNotificationInterface client, String username) throws RemoteException{
        clientCallbacks.add(new CallbacksInfo(client, username));
    }

    public synchronized  void unregCallback(String username){
		CallbacksInfo toRemove = null;
		for(CallbacksInfo c : clientCallbacks){
			if(c.getUser().equalsIgnoreCase(username)){
				toRemove = c;
			}
		}
		if(toRemove!=null) clientCallbacks.remove(toRemove);
	}

    public synchronized void notifyChatConnection(Project project,String username) throws RemoteException{
        //System.out.println("Notifying new chat configuration to "+ username + " ...");
        for(CallbacksInfo c : clientCallbacks) {
            EventNotificationInterface client = c.getClient();
            if(c.getUser().equalsIgnoreCase(username))
                client.newChat(project.getAddress(), project.getPort());
        }
        //System.out.println("New chat notification completed.");
    }

    public synchronized void revokeChatConnections(Project project) throws RemoteException {
        //System.out.println("Revoking chat connections related to project: "+ project.getName());
        for(CallbacksInfo c : clientCallbacks) {
            EventNotificationInterface client = c.getClient();
            if(project.isMember(c.getUser()))
                client.removeChat(project.getAddress(), project.getPort());
        }
        //System.out.println("Project: " + project.getName() + " chat connections revoked.");
    }
}
