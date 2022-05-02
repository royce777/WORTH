import com.google.gson.Gson;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class Project {

    private final String name;

    private final HashSet<String> members;

    private transient HashSet<Card> cards;

    private final HashSet<String> toDo;
    private final HashSet<String> inProgress;
    private final HashSet<String> toBeRevised;
    private final HashSet<String> done;
    private final String multicastAddr;
    private final int port;
    private static final String dataDir = "./data";

    private final String projDir;

    public Project(String name, String user, String multicast, int port) {
        this.name = name.toLowerCase();
        
        this.members = new HashSet<>();
        members.add(user);
        this.projDir = dataDir +"/" + name;
        this.cards = new HashSet<>();
        this.toDo = new HashSet<>();
        this.inProgress = new HashSet<>();
        this.toBeRevised = new HashSet<>();
        this.done = new HashSet<>();
        this.multicastAddr=multicast;
        this.port = port;
    }
    public void setupCards(){
        this.cards = new HashSet<Card>();
    }

    public void createDirectory(){ // creates project directory if it does not exist
        try {
            Files.createDirectory(Paths.get(projDir));
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public String getName(){
        return this.name;
    }

    public boolean addMember(String username){
        return members.add(username.toLowerCase());
    }
    
    public boolean isMember(String username){
        return members.contains(username.toLowerCase());
    }

    public List<String> getMembers(){
        return new ArrayList<>(this.members);
    }
    
    public String getAddress(){
        return this.multicastAddr;
    }

    public int getPort(){
        return this.port;
    }

    public boolean isCompleted(){
        return (cards.size() == done.size());
    }

    public List<Pair<String,String>> showCards(){
        List<Pair<String,String>> cards = new ArrayList<>();
        for(Card c : this.cards){
            Pair<String,String> cardInfo = new Pair<>(c.getName(),c.getStatus());
            cards.add(cardInfo);
        }
        return cards;
    }

    public Triplet<String,String,String> getCardInfo(String cardName){
        for(Card c : cards){
            if(c.getName().equalsIgnoreCase(cardName)){
                return new Triplet<>(c.getName(),c.getDescription(),c.getStatus());
            }
        }
        return null;
    }

    public List<Card.Status> getCardHistory(String cardName){
        List<Card.Status> history = null;
        for(Card card : cards){
            if(card.getName().equalsIgnoreCase(cardName)){
                history = card.getMovements();
            }
        }
        return history;
    }

    public boolean createCard(String cardName, String description) {
        if(cardName.length()>0){
            boolean res = !toDo.contains(cardName.toLowerCase()) && !inProgress.contains(cardName.toLowerCase()) && !toBeRevised.contains(cardName.toLowerCase()) && !done.contains(cardName.toLowerCase());
            if(res){
                Card card = new Card(cardName.toLowerCase(), description);
                cards.add(card);
                toDo.add(cardName.toLowerCase());
                String cardPath = projDir + "/"+ cardName.toLowerCase()+".json";
                File cFile = new File(cardPath);
                try{
                    cFile.createNewFile();
                }
                catch(IOException e){
                    e.printStackTrace();
                }
                // update JSON file of the card
                Gson gson = new Gson();
                String cardJson = gson.toJson(card);
                try {
                    //ByteBuffer to contain JSONarray
                    ByteBuffer buf = ByteBuffer.wrap(cardJson.getBytes(StandardCharsets.UTF_8));
                    try {
                        Files.deleteIfExists(Paths.get(cardPath)); 
                        Files.createFile(Paths.get(cardPath)); 
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    //NIO channel to write
                    FileChannel outChannel = FileChannel.open(Paths.get(cardPath), StandardOpenOption.WRITE);
                    
                    while(buf.hasRemaining()) {
                        outChannel.write(buf);
                    }
                    outChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
            else return false;
        }
        return false;

    }

    public int moveCard(String cardName, String src, String dest){
        Card.Status stat;
        try{
            stat = Card.Status.valueOf(src.toUpperCase());
        }
        catch(IllegalArgumentException e){
            return 24; // !!!!!!!! ERR invalid src list
        }
        int err = -1;
        for(Card card : cards){
            if(card.getName().equals(cardName.toLowerCase())){
                if (!dest.equalsIgnoreCase("TODO") && !dest.equalsIgnoreCase("INPROGRESS") && !dest.equalsIgnoreCase("TOBEREVISED") && !dest.equalsIgnoreCase("DONE")) return 26; // invalid dest list
                switch(stat.name().toUpperCase()) {
                    case "TODO" :
                        if(!card.getStatus().equalsIgnoreCase("TODO")) return 25; //ERR CARD IN ANOTHER LIST
                        if(!dest.equalsIgnoreCase("INPROGRESS")) return 26; //ERR INVALID DEST LIST
                        card.moveCard(dest.toUpperCase());
                        toDo.remove(cardName.toLowerCase());
                        inProgress.add(cardName.toLowerCase());
                        err = 0; // ERR OK
                        break;
                    case "INPROGRESS" :
                        if(!card.getStatus().equalsIgnoreCase("INPROGRESS")) return 25; //ERR CARD IN ANOTHER LIST
                        if(dest.equalsIgnoreCase("TOBEREVISED")){
                            card.moveCard("TOBEREVISED");
                            inProgress.remove(cardName.toLowerCase());
                            toBeRevised.add(cardName.toLowerCase());
                            err = 0;
                        }
                        else if(dest.equalsIgnoreCase("DONE")){
                            card.moveCard("DONE");
                            inProgress.remove(cardName.toLowerCase());
                            done.add(cardName.toLowerCase());
                            err = 0;
                        }
                        else err = 26; //ERR INVALID DEST LIST
                        break;
                    case "TOBEREVISED" :
                        if(!card.getStatus().equalsIgnoreCase("TOBEREVISED")) return 25; //ERR CARD IN ANOTHER LIST
                        if(dest.equalsIgnoreCase("INPROGRESS")){
                            card.moveCard("INPROGRESS");
                            toBeRevised.remove(cardName.toLowerCase());
                            inProgress.add(cardName.toLowerCase());
                            err =0;
                        }
                        else if(dest.equalsIgnoreCase("DONE")){
                            card.moveCard("DONE");
                            toBeRevised.remove(cardName.toLowerCase());
                            done.add(cardName.toLowerCase());
                            err = 0;
                        }
                        else err = 26; //ERR INVALID DEST LIST
                        break;
                    case "DONE" :
                        err = 27; //ERR DONE IS FINAL STATE, CANT MOVE !
                        break;
                }
                String cardPath = projDir + "/"+ cardName.toLowerCase()+".json";
                
                try {
                    Writer writer = new FileWriter(cardPath);
                    new Gson().toJson(card, writer);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return err;
            }  
        }
        return 15; // ERR invalid card name
    }

    public void delete(){
        File directory = new File(projDir);
        String[] files = directory.list();
        if(files != null) {
            for (String s : files) {
                File currFile = new File(directory.getPath(), s);
                currFile.delete();
            }
        }
        directory.delete();
    }

    public void loadCards (){ // loads cards information from JSON into Card objects.
        File f = new File (projDir);
        String[]  cardFiles = f.list();
        if(cardFiles == null) { // projDir doesn't exist
            try {
                Files.createDirectory(Paths.get(projDir));
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
        else {
            Gson gson = new Gson();
            for (String file : cardFiles) {
                try {
                    String cardPath = projDir+"/"+file;
                    Reader reader = Files.newBufferedReader(Paths.get(cardPath));
                    Card card = gson.fromJson(reader, Card.class);
                    cards.add(card);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
