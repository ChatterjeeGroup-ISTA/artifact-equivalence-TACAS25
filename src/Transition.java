import java.util.Vector;

public class Transition    //from "v.first" to "v.second" with guard "g" and update "varName := update"
{
    CFGNode from, to;
    PolynomialPredicate detGuard;

    Vector<String> detVarName;
    Vector<Polynomial> detUpdate;

    Vector<String> probVarName;
    Vector<Distribution> probUpdate;

    Polynomial score;
    Rational prob;
    boolean hasGroup;

    Transition(CFGNode a, CFGNode b)
    {
        from = a;
        to = b;
        detGuard = new PolynomialPredicate();
        detVarName = new Vector<>();
        detUpdate = new Vector<>();
        probVarName = new Vector<>();
        probUpdate = new Vector<>();
        prob = null;
        hasGroup = false;
    }



    public void replaceVarsWithPoly(Vector<String> vars, Vector<Polynomial> upd) throws Exception
    {
        detGuard.replaceVarsWithPoly(vars,upd);
        for(Polynomial lc: detUpdate)
            lc.replaceVarsWithPoly(vars,upd);
    }

    public Transition deepCopy()
    {
        Transition ret = new Transition(from, to);
        ret.detGuard = detGuard.deepCopy();
        ret.detVarName.addAll(detVarName);
        for (Polynomial lc : detUpdate)
            if (lc != null)
                ret.detUpdate.add(lc.deepCopy());
            else
                ret.detUpdate.add(null);
        return ret;
    }

    public String toString()
    {
        String res = "";
        res += "from: " + from.id + "\nto: " + to.id + "\n";
        if (detGuard != null)
            res += "detGuard: " + detGuard.toNormalString() + "\n";
        for (int i = 0; i < detVarName.size(); i++)
            if (detUpdate.elementAt(i) != null)
                res += detVarName.elementAt(i) + " := " + detUpdate.elementAt(i).toNormalString() + "\n";
            else
                res += detVarName.elementAt(i) + " := nondet()\n";
        for(int i=0;i<probVarName.size();i++)
            if (probUpdate.elementAt(i) != null)
                res += probVarName.elementAt(i) + " := " + probUpdate.elementAt(i)+ "\n";
        if(prob!=null)
            res+="prob: "+prob.toNormalString()+ "\n";
        if(score!=null)
        {
            res+="score: "+score.toNormalString()+"\n";
        }
        return res;
    }
}