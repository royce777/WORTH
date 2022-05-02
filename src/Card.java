import java.util.ArrayList;

public class Card {

    public enum Status { TODO, INPROGRESS, TOBEREVISED, DONE}
    private String name;
    private String description;
    private Status currentStatus;
    private ArrayList<Status> movements;
    

    public Card(String name, String description){
        this.name = name.toLowerCase();
        this.description = description;
        this.currentStatus = Status.TODO;
        this.movements = new ArrayList<Status>();
        this.movements.add(Status.TODO);
        
    }

    public String getName(){
        return this.name;
    }
    public String getDescription(){
        return this.description;
    }
    public ArrayList<Status> getMovements(){
        return this.movements;
    }
    public String getStatus(){
        return this.currentStatus.name();
    }

    public void moveCard(String destination){
        this.currentStatus = Status.valueOf(destination.toUpperCase());
        this.movements.add(Status.valueOf(destination.toUpperCase()));
    }
}