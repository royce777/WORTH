import java.io.Serializable;

public class Pair<T, V> implements Serializable {
    private T firstEl;
    private V secondEl;

    public Pair(T firstEl, V secondEl){
        this.firstEl = firstEl;
        this.secondEl = secondEl;
    }
    public T getfirstEl(){
        return this.firstEl;
    }
    public V getSecondEl(){
        return this.secondEl;
    }
}
