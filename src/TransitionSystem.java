import java.util.*;

public class TransitionSystem
{
    public String name;
    public Set<CFGNode> states;
    public Map<Integer,Vector<Transition>> outgoing;
    public Vector<Transition> transitions;
    public Set<String> allVars;
    public PolynomialPredicate pre_condition;
    public Map<String, Rational> init;
    public CFGNode startNode, terminalNode;
    public Map<Integer, CFGNode> idToNode;
    public Map<Integer, PolynomialPredicate> aspic_invariant,sting_invariant;
    TransitionSystem(String name)
    {
        transitions = new Vector<>();
        this.name = name;
        allVars = new HashSet<>();
        allVars.add("1");
        pre_condition = new PolynomialPredicate();
        startNode = null;
        terminalNode = null;
        states = new TreeSet<>();
        idToNode = new TreeMap<>();
        outgoing = new TreeMap<>();
        init = new TreeMap<>();
        aspic_invariant=new TreeMap<>();
        sting_invariant=new TreeMap<>();
    }

    public void addTransition(Transition tau)
    {
        addState(tau.from);
        addState(tau.to);
        transitions.add(tau);

        outgoing.get(tau.from.id).add(tau);
    }

    public void addState(CFGNode a)
    {
        states.add(a);
        if(!outgoing.containsKey(a.id))
            outgoing.put(a.id,new Vector<>());
    }

    public CFGNode addOrReturnState(int id)
    {
        if(idToNode.containsKey(id))
            return idToNode.get(id);
        else
        {
            CFGNode ret = new CFGNode(id);
            idToNode.put(id,ret);
            return ret;
        }
    }

    public void get_aspic_invariants() throws Exception
    {
        aspic_invariant = InvUtil.get_invariants_aspic(this);
    }

    public void get_sting_invariants() throws Exception
    {
        sting_invariant = InvUtil.get_invariants_sting(this);
    }

    public void verify() throws Exception
    {
        for(CFGNode n:states)
        {
            int isDet= 0, isProbGuard=0, isProbUpd=0,isNondet=0,isScore=0;
            Rational totProb=Rational.zero.deepCopy();
            Vector<Transition> out = outgoing.get(n.id);
            if(out.isEmpty())
            {
                n.type = "terminal";
                terminalNode = n;
                continue;
            }
            for(Transition tau:out)
            {
                if (tau.detGuard.size() != 0)
                    isDet=1;
                if (!tau.detVarName.isEmpty())
                    isDet=1;
                if (tau.prob!=null)
                {
                    isProbGuard = 1;
                    totProb=Rational.add(totProb,tau.prob);
                }
                if(!tau.probVarName.isEmpty())
                    isProbUpd++;
                if(tau.detGuard.size()==0 && tau.detVarName.isEmpty() && tau.prob==null && tau.probVarName.isEmpty())
                    isNondet=1;
                if(tau.score!=null)
                    isScore++;
            }
            if(isDet+isProbGuard+isProbUpd+isNondet>1)
                throw new Exception("State "+n.id+" in "+this.name+" violates syntax/semantics assumptions");
            if(isProbGuard==1 && !totProb.equals(Rational.one))
                throw new Exception("Probabilities around "+n.id+" in "+this.name+" do not sum to 1");
            if(isDet>0)
                n.type="det";
            else if(isProbGuard>0)
                n.type="probGuard";
            else if(isProbUpd>0)
                n.type="probUpd";
            else if(isScore>0)
                n.type="score";
            else
                n.type="nondet";
        }
    }

    public void config_observe(String reset_or_return)
    {
        if (reset_or_return.equals("return"))
        {
            CFGNode obs_node = new CFGNode(10000000);
            obs_node.type = "terminal";
            addState(obs_node);
            for (CFGNode n : states)
                if (n.isObserve)
                {
                    DNFPoly obs = n.observation;
                    DNFPoly neg_obs = n.observation.negate();
                    for (PolynomialPredicate pp : neg_obs.getClauses())
                    {
                        Transition t = new Transition(n, obs_node);
                        for (String var : allVars)
                            if (!var.equals("1"))
                            {
                                t.detVarName.add(var);
                                t.detUpdate.add(new Polynomial(Rational.zero));             // var = 0
                            }
                        t.detGuard.add(pp.deepCopy());
                        addTransition(t);
                        System.err.println("-------------new transition--------------");
                        System.err.println(t);
                    }
                }
        } else  // if (reset_or_return.equals("reset"))
        {
            for (CFGNode n : states)
                if (n.isObserve)
                {
                    DNFPoly obs=n.observation;
                    DNFPoly neg_obs = n.observation.negate();
                    for(PolynomialPredicate pp: neg_obs.getClauses())
                    {
                        Transition t = new Transition(n,startNode);
                        t.detGuard.add(pp.deepCopy());
                        for(String var:allVars)
                            if(!var.equals("1"))
                            {
                                t.detVarName.add(var);
                                t.detUpdate.add(new Polynomial(init.get(var)));
                            }
                        addTransition(t);
                        System.err.println("-------------new transition--------------");
                        System.err.println(t);
                    }
                }
                else if(n.isScore)
                {
                    Transition t=outgoing.get(n.id).firstElement();
                    Transition t_prime = new Transition(n,this.startNode);
                    Polynomial sc = new Polynomial(Monomial.one);
                    sc.minus(t.score);
                    t_prime.score=sc; // = 1-t.score
                    for(String var:allVars)
                        if(!var.equals("1"))
                        {
                            t_prime.detVarName.add(var);
                            t_prime.detUpdate.add(new Polynomial(init.get(var)));
                        }
                    System.err.println("------------new transition--------------");
                    System.err.println(t_prime);
                    addTransition(t_prime);
                }
        }
    }

    public String toString()
    {
        StringBuilder ret = new StringBuilder();
        ret.append("States: ");
        for(CFGNode state:states)
            ret.append("(").append(state.id).append(",").append(state.type).append("), ");
        ret.append("\n");
        ret.append("Start State: ").append(startNode.id).append("\n");
        ret.append("Pre-condition: ").append(pre_condition.toNormalString()).append("\n");
        for(Transition t:transitions)
        {
            ret.append("----------------------\n");
            ret.append(t.toString());
            ret.append("----------------------\n");
        }
        return ret.toString();
    }

}
