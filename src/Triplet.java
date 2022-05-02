import java.io.Serializable;

public class Triplet<T,V,B> extends Pair<T,V> implements Serializable {
    private B thirdEl;

    public Triplet(T fistEl, V secondEl, B thirdEl){
        super(fistEl,secondEl);
        this.thirdEl = thirdEl;
    }

    public B getThirdEl(){
        return this.thirdEl;
    }
}
