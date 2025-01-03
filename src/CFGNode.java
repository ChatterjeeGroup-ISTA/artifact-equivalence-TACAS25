import java.util.Vector;

public class CFGNode implements Comparable<CFGNode>
{
    public Vector<Transition> out;
    int id;
    boolean isObserve;
    DNFPoly observation;
    boolean isScore;
    public PolynomialPredicate pre_condition;
    Polynomial rank;
    String type;
    CFGNode(int ind)
    {
        id = ind;
        out = new Vector<>();
        pre_condition = new PolynomialPredicate();
        type = null;
        isObserve=false;
        observation=null;
        isScore=false;
    }

    @Override
    public int compareTo(CFGNode a)
    {
        return this.id-a.id;
    }



}