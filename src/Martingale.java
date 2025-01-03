import java.util.Set;
import java.util.Vector;

public class Martingale
{
    public static int cCount = 0,sCount=0,tCount=0;
    public static void MakeTemplate(TransitionSystem T)
    {
        for (CFGNode n : T.states)
        {
            if(n.type.equals("terminal"))
            {
                n.rank = new Polynomial();
                continue;
            }
            Polynomial rank = new Polynomial(); // c_0 * 1 + c_1 * m_1 + c_2 * m_2 .... + c_n * m_n >=0
            Set<Monomial> allMonomials= Monomial.getAllMonomials(T.allVars,Main.degree);
            for (Monomial m : allMonomials)
            {
                m.addVar("c_"+cCount,1);
                rank.add(m,Rational.one); // p+= var * c_cCount
                cCount++;
            }
            n.rank = rank;
        }
    }

    public static Polynomial MakeCostTemplate()
    {
        Polynomial cost = new Polynomial();
        Set<Monomial> allMonomials= Monomial.getAllMonomials(Parser.allVars,Main.degree);
        for (Monomial m : allMonomials)
        {
            if(m.degree()==0)
                continue;
            m.addVar("s_"+sCount,1);
            cost.add(m,Rational.one); // p+= var * c_cCount
            sCount++;
        }
        return cost;
    }

    public static Vector<Handelman> generate(TransitionSystem T1, TransitionSystem T2, Polynomial cost, String OST_condition) throws Exception
    {
        Vector<Handelman> upper = new Vector<>(), lower = new Vector<>(), OST_upper = new Vector<>(), OST_lower = new Vector<>();
        generate_UESM(T1,cost,upper,OST_condition,OST_upper);
        generate_LESM(T2,cost,lower,OST_condition, OST_lower);
        Vector<Handelman> union = new Vector<>();
        union.addAll(upper);
        union.addAll(OST_upper);
        union.addAll(lower);
        union.addAll(OST_lower);
        return union;
    }

    public static Handelman lipschitz_condition(TransitionSystem T, Polynomial cost) throws Exception
    {
        //f(lip_x_var)-f(lip_y_var) <= |lip_x_var - lip_y_var|
        Vector<Polynomial> var_to_lip_x = new Vector<>(), var_to_lip_y = new Vector<>();
        Vector<Polynomial> tVars = new Vector<>();
        Vector<String> allVars = new Vector<>();
        for(String var:Parser.allVars)
            if(!var.equals("1"))
            {
                var_to_lip_x.add(new Polynomial("lip_x_"+var));
                var_to_lip_y.add(new Polynomial("lip_y_"+var));
                tVars.add(new Polynomial("t_"+tCount));
                tCount++;
                allVars.add(var);
            }
        Polynomial cost_x = cost.deepCopy(), cost_y=cost.deepCopy();
        cost_x.replaceVarsWithPoly(allVars,var_to_lip_x);           // f(lip_x_var)
        cost_y.replaceVarsWithPoly(allVars,var_to_lip_y);           // f(lip_y_var)


        Polynomial obj = new Polynomial();
        for(Polynomial poly: tVars)
            obj.add(poly);              // obj = sum t_i
        obj.minus(cost_x);
        obj.add(cost_y);

        Handelman ret = new Handelman(-1,T);
        ret.setObjective(obj);
        PolynomialPredicate inv1 = new PolynomialPredicate();
        if(T.aspic_invariant.containsKey(T.terminalNode.id))
            inv1.add(T.aspic_invariant.get(T.terminalNode.id).deepCopy());
        if(T.sting_invariant.containsKey(T.terminalNode.id))
            inv1.add(T.sting_invariant.get(T.terminalNode.id).deepCopy());
        PolynomialPredicate inv2 = inv1.deepCopy();
        inv1.replaceVarsWithPoly(allVars,var_to_lip_x);
        inv2.replaceVarsWithPoly(allVars,var_to_lip_y);
        ret.addConstraint(inv1);
        ret.addConstraint(inv2);

        for(int i=0;i<tVars.size();i++)
        {
            Polynomial constraint = new Polynomial();
            constraint.add(tVars.elementAt(i));
            constraint.minus(var_to_lip_x.elementAt(i));
            constraint.add(var_to_lip_y.elementAt(i));
            ret.addConstraint(constraint);

            constraint = new Polynomial();
            constraint.add(tVars.elementAt(i));
            constraint.add(var_to_lip_x.elementAt(i));
            constraint.minus(var_to_lip_y.elementAt(i));
            ret.addConstraint(constraint);
        }
        return  ret;
    }

    public static void add_OST_handelman(CFGNode state, Transition tau, Polynomial cost, Handelman h, String OST_condition, Vector<Handelman> handelmanVector, Vector<Handelman> OST_vec, TransitionSystem T) throws Exception
    {
//        if(OST_condition.equals("C4"))                      // Cauchy Schwarz idea
//        {
//            if (state.type.equals("det"))
//            {
//                Polynomial cur = state.rank.deepCopy();
//                cur.add(cost.deepCopy());
//                Polynomial next = tau.to.rank.deepCopy();
//                next.add(cost.deepCopy());
//                for(String var: T.allVars)
//                    if(!var.equals("1"))
//                        next.replaceVarWithPoly(var,new Polynomial(var+"'"));
//                Polynomial C4 = cur.deepCopy();
//                C4.minus(next);
//                C4.multiplyByPolynomial(C4);
//                Vector<String> primed = new Vector<>();
//                for(String var: tau.detVarName)
//                    primed.add(var+"'");
//                C4.replaceVarsWithPoly(primed,tau.detUpdate);
//
//                for(String var: T.allVars)
//                    if(!tau.detVarName.contains(var) && !var.equals("1"))
//                        C4.replaceVarWithPoly(var+"'",new Polynomial(var));
//                C4 = C4.negate();
//                C4.add(Main.C);
//                Handelman res = h.deepCopy();
//                res.setObjective(C4);
//                OST_vec.add(res);
//            }
//            else if (state.type.equals("probGuard"))
//            {
//                Polynomial C4 = new Polynomial();
//                for(Transition t: T.outgoing.get(state.id))
//                {
//                    Polynomial cur = state.rank.deepCopy();
//                    cur.add(cost.deepCopy());
//
//                    Polynomial next = t.to.rank.deepCopy();
//                    next.add(cost.deepCopy());
//
//                    Polynomial tmp = cur.deepCopy();
//                    tmp.minus(next);
//
//                    tmp.multiplyByPolynomial(tmp);
//                    tmp.multiplyByValue(t.prob);
//
//                    C4.add(tmp);
//                }
//
//                C4 = C4.negate();
//                C4.add(Main.C);
//                Handelman res = h.deepCopy();
//                res.setObjective(C4);
//                OST_vec.add(res);
//            }
//            else if (state.type.equals("probUpd"))
//            {
//                Polynomial cur = state.rank.deepCopy();
//                cur.add(cost.deepCopy());
//
//                Polynomial next = tau.to.rank.deepCopy();
//                next.add(cost.deepCopy());
//
//                for(String var: T.allVars)
//                    if(!var.equals("1"))
//                        next.replaceVarWithPoly(var,new Polynomial(var+"'"));
//
//                Polynomial C4 = cur.deepCopy();
//                C4.minus(next);
//                C4.multiplyByPolynomial(C4);
//
//                Vector<String> primed = new Vector<>();
//                for(String var: tau.probVarName)
//                    primed.add(var+"'");
//                C4.replaceVarsWithDistr(primed.elementAt(0),tau.probUpdate.elementAt(0));
//
//                for(String var: T.allVars)
//                    if(!tau.probVarName.contains(var) && !var.equals("1"))
//                        C4.replaceVarWithPoly(var+"'",new Polynomial(var));
//                C4 = C4.negate();
//                C4.add(Main.C);
//                Handelman res = h.deepCopy();
//                res.setObjective(C4);
//                OST_vec.add(res);
//            }
//
//        }
//        else
        if(OST_condition.equals("C3"))
        {
            {
                Handelman h1 = h.deepCopy();
                Polynomial obj = h.objective.deepCopy();
                obj.add(Main.C);
                h1.setObjective(obj);
                OST_vec.add(h1);

                Handelman h2 = h.deepCopy();
                obj = h.objective.deepCopy();
                obj.minus(Main.C);
                obj=obj.negate();
                h2.setObjective(obj);
                OST_vec.add(h2);
            }
            Handelman h_tmp = new Handelman(state.id, T);
            h_tmp.addConstraint(T.aspic_invariant.get(state.id));
            h_tmp.addConstraint(T.sting_invariant.get(state.id));


            if(state.type.equals("det"))
                ; //skip
            else if(state.type.equals("probGuard"))
            {
                for(Transition trans: T.outgoing.get(state.id))
                {
                    Handelman h1 = h_tmp.deepCopy();
                    Polynomial diff = trans.from.rank.deepCopy();
                    diff.minus(trans.to.rank.deepCopy());
                    Polynomial obj = diff.deepCopy();       // cur - next
                    obj.add(Main.C);                        // cur - next + C >= 0
                    h1.setObjective(obj);
                    if (T.name.equals("input2"))
                        OST_vec.add(h1);

                    Handelman h2 = h1.deepCopy();
                    obj = diff.deepCopy();              // cur - next
                    obj.minus(Main.C);                  // cur - next - C <= 0
                    obj=obj.negate();                   // negate of above >= 0
                    h2.setObjective(obj);
                    if (T.name.equals("input1"))
                        OST_vec.add(h2);
                }
            }
            else if(state.type.equals("probUpd"))
            {
                if(tau.probUpdate.firstElement().type.equals("discrete"))
                {
                    Distribution dist = tau.probUpdate.firstElement();
                    int len = dist.params.size();
                    for(int i=len/2;i<len;i++)
                    {
                        Rational r = dist.params.elementAt(i);
                        Handelman h1 = h_tmp.deepCopy();
                        Polynomial diff = state.rank.deepCopy();
                        diff.minus(tau.to.rank.deepCopy());

                        Vector<Polynomial> upd = new Vector<>();
                        upd.add(new Polynomial(r));
                        Polynomial tick = compute_cost(cost,tau.probVarName,upd);           // next f - cur f
                        diff.minus(tick);                                                   // cur h + cur f - next h  - next f

                        Polynomial obj = diff.deepCopy();
                        obj.add(Main.C);                                                    // cur h + cur f - next h  - next f + C >=0
                        h1.setObjective(obj);
                        if(T.name.equals("input2"))
                            OST_vec.add(h1);

                        Handelman h2 = h1.deepCopy();
                        obj = diff.deepCopy();
                        obj.minus(Main.C);                                                  // cur h + cur f - next h - next f - C <=0
                        obj=obj.negate();                                                   // negate of above >= 0
                        h2.setObjective(obj);
                        if(T.name.equals("input1"))
                            OST_vec.add(h2);
                    }
                }
                else // Normal, Uniform
                {
                    String varName = tau.probVarName.firstElement();
                    Distribution upd = tau.probUpdate.firstElement();

                    Polynomial cur = state.rank.deepCopy();
                    cur.add(cost.deepCopy());               // cur h + cur f

                    Polynomial next = tau.to.rank.deepCopy();
                    next.add(cost.deepCopy());
                    next.makePrimed(tau.probVarName);       // next h + next f   [update variable is primed]

                    Polynomial diff = cur.deepCopy();
                    diff.minus(next);                       // cur h + cur f - next h - next f

                    Handelman h1 = h_tmp.deepCopy();
                    if(upd.lower_bound()!=null)
                    {
                        Polynomial lower = new Polynomial(varName+"'");
                        lower.minus(new Polynomial(upd.lower_bound()));
                        h1.addConstraint(lower);
                    }
                    if(upd.upper_bound()!=null)
                    {
                        Polynomial upper = new Polynomial(upd.upper_bound());
                        upper.minus(new Polynomial(varName+"'"));
                        h1.addConstraint(upper);
                    }
                    Polynomial obj = diff.deepCopy();
                    obj.add(Main.C);                                                    // cur h + cur f - next h  - next f + C >=0
                    h1.setObjective(obj);
                    if(T.name.equals("input2"))
                        OST_vec.add(h1);

                    Handelman h2 = h1.deepCopy();
                    obj = diff.deepCopy();
                    obj.minus(Main.C);                                                  // cur h + cur f - next h - next f - C <=0
                    obj=obj.negate();                                                   // negate of above >= 0
                    h2.setObjective(obj);
                    if(T.name.equals("input1"))
                        OST_vec.add(h2);
                }
            }
        }
        else if(OST_condition.equals("C2"))
        {
            Handelman res1 = h.deepCopy();
            Polynomial obj = state.rank.deepCopy();
            obj.add(cost);                              // cur h + cur f
            obj.add(Main.C);                            // cur h + cur f + C >=0
            res1.setObjective(obj);
            OST_vec.add(res1);

            Handelman res2 = h.deepCopy();
            obj = state.rank.deepCopy();
            obj.add(cost);                               // cur h + cur f
            obj.minus(Main.C);                           // cur h + cur f - C
            obj=obj.negate();                            // - cur h - cur f + C >=0
            res2.setObjective(obj);
            OST_vec.add(res2);
        }
    }
    public static void generate_UESM(TransitionSystem T, Polynomial cost, Vector<Handelman> handelmanVector, String OST_contidion, Vector<Handelman> OST_hand) throws Exception
    {
        for(CFGNode state:T.states)
        {
            if(state.type.equals("det") || state.type.equals("nondet"))
            {
                for(Transition tau:T.outgoing.get(state.id))
                {
                    Handelman h = new Handelman(state.id,T);
                    h.addConstraint(tau.detGuard.deepCopy());
                    h.addConstraint(T.aspic_invariant.get(state.id));                                 //add invariant
                    h.addConstraint(T.sting_invariant.get(state.id));


                    Polynomial obj = state.rank.deepCopy();
                    Polynomial succ = tau.to.rank.deepCopy();                                   // succ = pre-expectation
                    succ.replaceVarsWithPoly(tau.detVarName,tau.detUpdate);                     // replace updates

                    Polynomial tick = compute_cost(cost,tau.detVarName,tau.detUpdate);          // compute amount of change in cost function
                    succ.add(tick);                                                             // add to pre-expectation

                    obj.minus(succ);                                                            // current value - pre-expectation >=0
                    h.setObjective(obj);
                    handelmanVector.add(h);

                    add_OST_handelman(state,tau,cost,h,OST_contidion,handelmanVector,OST_hand,T);
                }

            }
            else if(state.type.equals("probGuard"))
            {
                Handelman h = new Handelman(state.id,T);
                Polynomial cur = state.rank.deepCopy();
                Polynomial succ = new Polynomial();
                for(Transition tau:T.outgoing.get(state.id))
                {
                    Polynomial tmp = tau.to.rank.deepCopy();
                    tmp.multiplyByValue(tau.prob);
                    succ.add(tmp);
                }
                cur.minus(succ);

                h.addConstraint(T.aspic_invariant.get(state.id));
                h.addConstraint(T.sting_invariant.get(state.id));
                h.setObjective(cur);
                handelmanVector.add(h);
                add_OST_handelman(state,null,cost,h,OST_contidion, handelmanVector,OST_hand,T);
            }
            else if(state.type.equals("probUpd"))
            {
                Transition tau = T.outgoing.get(state.id).firstElement();

                Handelman h = new Handelman(state.id,T);
                h.addConstraint(T.aspic_invariant.get(state.id));                                 // add invariant
                h.addConstraint(T.sting_invariant.get(state.id));

//                Vector<Polynomial> upd = new Vector<>();
//                for(int i=0;i<tau.probVarName.size();i++)
//                    upd.add(new Polynomial(tau.probUpdate.elementAt(i).expectation()));     // update = replacing with expected value
                Polynomial cur = state.rank.deepCopy();
                Polynomial succ = tau.to.rank.deepCopy();
                succ.replaceVarsWithDistr(tau.probVarName.elementAt(0),tau.probUpdate.elementAt(0));                              // replace updates

                Polynomial tick = compute_prob_cost(cost,tau.probVarName,tau.probUpdate);          // compute amount of change in cost function
                succ.add(tick);                                                             // add to pre-expectation

                cur.minus(succ);                                                            // current value - pre-expectation >=0

                h.setObjective(cur);
                handelmanVector.add(h);

                add_OST_handelman(state,tau,cost,h,OST_contidion,handelmanVector,OST_hand,T);
            }
            else if(state.type.equals("score"))
            {
                Handelman h = new Handelman(state.id,T);
                Polynomial cur = state.rank.deepCopy();
                Polynomial succ = new Polynomial();
                for(Transition tau:T.outgoing.get(state.id))
                {
                    Polynomial tmp = tau.to.rank.deepCopy();
                    tmp.replaceVarsWithPoly(tau.detVarName,tau.detUpdate);
                    tmp.multiplyByPolynomial(tau.score);
                    succ.add(tmp);
                }
                cur.minus(succ);

                h.addConstraint(T.aspic_invariant.get(state.id));
                h.addConstraint(T.sting_invariant.get(state.id));
                h.setObjective(cur);
                handelmanVector.add(h);
                add_OST_handelman(state,null,cost,h,OST_contidion, handelmanVector,OST_hand,T);
            }
        }
    }
    public static void generate_LESM(TransitionSystem T, Polynomial cost, Vector<Handelman> handelmanVector, String OST_condition, Vector<Handelman> OST_hand) throws Exception
    {
        generate_UESM(T,cost,handelmanVector,OST_condition, OST_hand);
        for(Handelman h:handelmanVector)
            h.objective=h.objective.negate();
    }

    public static Polynomial compute_cost(Polynomial cost, Vector<String> varName, Vector<Polynomial> upd)  throws Exception // next value - cur value
    {
        Polynomial tick = cost.deepCopy();
        tick.replaceVarsWithPoly(varName,upd);
        tick.minus(cost);
        return tick;
    }

    public static Polynomial compute_prob_cost(Polynomial cost, Vector<String> varName, Vector<Distribution> upd) throws Exception
    {
        Polynomial tick = cost.deepCopy();
        tick.replaceVarsWithDistr(varName.elementAt(0),upd.elementAt(0));
        tick.minus(cost);
        return tick;
    }

    public static Handelman nonNegativity(TransitionSystem T, Polynomial cost) throws Exception
    {
        Polynomial obj = cost.deepCopy();
        Handelman ret = new Handelman(T.terminalNode.id,T);
        ret.setObjective(obj);
        PolynomialPredicate inv1 = new PolynomialPredicate();
        if(T.aspic_invariant.containsKey(T.terminalNode.id))
            inv1.add(T.aspic_invariant.get(T.terminalNode.id).deepCopy());
        if(T.sting_invariant.containsKey(T.terminalNode.id))
            inv1.add(T.sting_invariant.get(T.terminalNode.id).deepCopy());
        ret.addConstraint(inv1);
        return  ret;
    }
//





}