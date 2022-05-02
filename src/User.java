import com.google.gson.annotations.Expose;

import java.io.Serializable;

public class User implements Serializable {
    @Expose()
    public String username;

    //  USER CONNECTION ATTRIBUTES -> CAN'T BE SERIALIZED (only with GSON)
    @Expose(serialize = false)
    public boolean online;

	

    public User(String username){
        this.username = username.toLowerCase();
        this.online = false;
    }
    public void setStatus(boolean status){
        this.online = status;
    }

    public String getUsername(){
        return this.username;
    }
}
