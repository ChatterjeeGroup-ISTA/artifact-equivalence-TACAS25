import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.*;

import gurobi.*;


public class Handelman
{
    public static Map<String,Double> result=new TreeMap<>();
    public static int dCount=0;
    public static Map<String,Integer> smt_out = new TreeMap<>();
    PolynomialPredicate constraints;
    Polynomial objective;

    int from;
    TransitionSystem TS;

    Handelman(int from, TransitionSystem T)
    {
        this.from = from;
        this.TS=T;
        constraints = new PolynomialPredicate();
        objective = null;
    }

    void addConstraint(Polynomial poly) throws Exception
    {
        if(poly.degree()>1)
            throw new Exception("non-linear constraint in LHS?");
        poly.removeZeros();
        constraints.add(poly);
    }

    void addConstraint(PolynomialPredicate polyPredicate) throws Exception
    {
        if(polyPredicate==null)
            return;
        if(polyPredicate.contains_template_var())
            throw new Exception("non-linear constraint in LHS?");
        for(Polynomial term: polyPredicate.exprs)
            term.removeZeros();
        constraints.add(polyPredicate);
    }

    void setObjective(Polynomial poly)
    {
        poly.removeZeros();
        objective = poly;
    }

    public Vector<Polynomial> generate_equalities() throws Exception
    {
        Vector<Polynomial> ret = new Vector<>();
        Vector<Polynomial> Mon = new Vector<>();
        Mon.add(new Polynomial(Rational.one));
        for(int i=1;i<=Main.mu;i++)
        {
            Vector<Polynomial> fresh = new Vector<>();
            for(Polynomial con:constraints.exprs)
                for(Polynomial prev:Mon)
                    fresh.add(Polynomial.mul(con,prev));
            fresh.add(new Polynomial(Rational.one));
            Mon=fresh;
        }
        Polynomial LHS = new Polynomial();
        for(Polynomial mon:Mon)
        {
            Monomial temp_var = new Monomial("d_"+dCount,1);
            mon.multiplyByMonomial(temp_var);
            dCount++;
            LHS.add(mon);
        }
//        System.err.println("LHS: "+LHS.toNormalString());
//        System.err.println("RHS: "+objective.toNormalString());
        Set<Monomial> allMonomials = new TreeSet<>();//Monomial.getAllMonomials(TS.allVars,Math.max(LHS.degree(), objective.degree()));
        allMonomials.addAll(LHS.getProgramVariableMonomials());
        allMonomials.addAll(objective.getProgramVariableMonomials());
        allMonomials.add(Monomial.one);
        for(Monomial m:allMonomials)
        {
            Polynomial L = LHS.getCoef(m).deepCopy();
            Polynomial R = objective.getCoef(m).deepCopy();
//            System.err.println("m: "+m.toNormalString());
//            System.err.println("L: "+L.toNormalString());
//            System.err.println("R: "+R.toNormalString());
//            System.err.println("------------------------");
            L.minus(R);
            ret.add(L);
        }
        return ret;
    }
    public static double run_solver(TransitionSystem upper, TransitionSystem lower, Vector<Polynomial> equalities, String solver, Vector<Handelman> handelmanVector,Polynomial cost) throws Exception
    {
//        System.err.println(upper.startNode.rank.toNormalString());
        if(solver.equals("gurobi"))
            return run_gurobi(upper,lower,equalities,cost);
        else
            return run_smt_solver(upper,lower,equalities,cost);
    }

    public static double run_gurobi(TransitionSystem upper, TransitionSystem lower, Vector<Polynomial> equalities, Polynomial cost) throws Exception
    {

        // exists valuation of variables X such that relational(X) > 0                       STRICT INEQUALITY
        // indeed we can look for a valuation X that maximizes relational(X)
        //creating Gurobi model:
        GRBEnv env = new GRBEnv(true);
        env.set(GRB.DoubleParam.FeasibilityTol,1e-9);
        env.set("logFile","log.log");
        env.set(GRB.IntParam.OutputFlag, 0);
        env.start();

        GRBModel model = new GRBModel(env);

        //create variables:
        Map<String,GRBVar> var_to_grb = new TreeMap<>();
        for(int i=0;i<Martingale.cCount;i++)
            var_to_grb.put("c_"+i,model.addVar(-GRB.INFINITY,GRB.INFINITY,0,GRB.CONTINUOUS,"c_"+i));
        for(int i=0;i<Martingale.sCount;i++)
        {
            var_to_grb.put("s_" + i, model.addVar(-GRB.INFINITY, GRB.INFINITY, 0, GRB.CONTINUOUS, "s_" + i));
        }
        for(int i=0;i<dCount;i++)
        {
            var_to_grb.put("d_" + i, model.addVar(-GRB.INFINITY, GRB.INFINITY, 0, GRB.CONTINUOUS, "d_" + i));
            GRBLinExpr expr = new Polynomial("d_"+i).translate_to_GRBLin(var_to_grb);
            model.addConstr(expr,GRB.GREATER_EQUAL,0,"const_d_"+i);
        }

        {
            var_to_grb.put("__C", model.addVar(-GRB.INFINITY, GRB.INFINITY, 0, GRB.CONTINUOUS, "__C"));
            GRBLinExpr expr = new Polynomial("__C").translate_to_GRBLin(var_to_grb);
            model.addConstr(expr, GRB.GREATER_EQUAL, 0, "const__C");
        }

        for(String var:Parser.allVars)
            if(!var.equals("1"))
                var_to_grb.put(var,model.addVar(-GRB.INFINITY,GRB.INFINITY,0,GRB.CONTINUOUS,var));

        // add equality constraints:
        int index=0;
        for(Polynomial poly:equalities)
        {
            GRBLinExpr expr = poly.translate_to_GRBLin(var_to_grb);
            model.addConstr(expr,GRB.EQUAL,0,"const"+index);
            index++;
        }

//        // TODO: remove?
//        {
//            Polynomial tmp = new Polynomial();
//            for (int i = 0; i < Martingale.sCount; i++)
//                tmp.add(new Polynomial("s_" + i));
//            GRBLinExpr expr = tmp.translate_to_GRBLin(var_to_grb);
//            model.addConstr(expr,GRB.GREATER_EQUAL,1,"const"+index);
//            index++;
//        }
        // x in pre-condition:
        index=0;
        for(Polynomial poly:upper.pre_condition.exprs)
        {
            GRBLinExpr expr = poly.translate_to_GRBLin(var_to_grb);
            model.addConstr(expr,GRB.GREATER_EQUAL,0,"pre"+index);
            index++;
        }

        for(Polynomial poly:lower.pre_condition.exprs)
        {
            GRBLinExpr expr = poly.translate_to_GRBLin(var_to_grb);
            model.addConstr(expr,GRB.GREATER_EQUAL,0,"pre"+index);
            index++;
        }

        if(Main.reset_or_return.equals("return"))
        {
            // force LESM(init)+f(init)>=0
            Polynomial LESM = lower.startNode.rank.deepCopy();
            LESM.add(cost.deepCopy());
            for (String var : lower.init.keySet())
                LESM.replaceVarWithPoly(var, new Polynomial(lower.init.get(var)));
            model.addConstr(LESM.translate_to_GRBLin(var_to_grb), GRB.GREATER_EQUAL, 0, "LESM_f_nneg");
        }
        // Set Objective:
        Polynomial poly_obj = lower.startNode.rank.deepCopy();
        poly_obj.minus(upper.startNode.rank);
        for(String var:upper.init.keySet())
            poly_obj.replaceVarWithPoly(var, new Polynomial(upper.init.get(var)));
        for(String var:lower.init.keySet())
            poly_obj.replaceVarWithPoly(var,new Polynomial(lower.init.get(var)));
        // if(poly_obj.degree()<=1 && Main.distance_or_equivalence.equals("equivalence"))
            model.addConstr(poly_obj.translate_to_GRBLin(var_to_grb),GRB.LESS_EQUAL,10000,"objbjbjb");  // NOTE: an upperbound on differential cost, so that the system is not unbounded anymore.
        GRBQuadExpr obj = poly_obj.translate_to_GRBQuad(var_to_grb);
        System.err.println(poly_obj.toNormalString());

//        {
//            Polynomial tmp1 = new Polynomial("s_1"), tmp0 = new Polynomial("s_0");
//            model.addConstr(tmp0.translate_to_GRBLin(var_to_grb),GRB.EQUAL,0,"tmp0");
//            model.addConstr(tmp1.translate_to_GRBLin(var_to_grb),GRB.EQUAL,0,"tmp1");
//        }

        model.setObjective(obj,GRB.MAXIMIZE);
        model.set(GRB.IntParam.NonConvex, 2);
        model.optimize();

        model.write("model.lp");
//        Map<String,Double> var_to_val=new HashMap<>();
        for(int i=0;i<Martingale.cCount;i++)
        {
            String var = "c_"+i;
//            if(var.startsWith("d"))
//                continue;
            GRBVar grbVar = var_to_grb.get(var);
            System.err.println(grbVar.get(GRB.StringAttr.VarName)
                    + " " +grbVar.get(GRB.DoubleAttr.X));
            result.put(var,grbVar.get(GRB.DoubleAttr.X));
        }
        for(int i=0;i<Martingale.sCount;i++)
        {
            String var = "s_"+i;
//            if(var.startsWith("d"))
//                continue;
            GRBVar grbVar = var_to_grb.get(var);
            System.err.println(grbVar.get(GRB.StringAttr.VarName)
                    + " " +grbVar.get(GRB.DoubleAttr.X));
            result.put(var,grbVar.get(GRB.DoubleAttr.X));
        }
        System.err.println("Obj: " + model.get(GRB.DoubleAttr.ObjVal));
        System.err.println();

        return model.get(GRB.DoubleAttr.ObjVal);
    }

    public static double run_smt_solver(TransitionSystem upper, TransitionSystem lower, Vector<Polynomial> equalities,Polynomial cost) throws Exception
    {
        make_smt(upper,lower,equalities,cost,"work/system.smt2");
        boolean result = run_mathsat("work/system.smt2");
        if(result)
            System.err.println("Refuted Equivalence Successfully.");
        else
            System.err.println("Mathsat Failed or returned UNSAT.");
        return (result)?1:0;
    }

    public static boolean run_mathsat(String in) throws Exception
    {
        String config = "./" + Main.solversDir + "/mathsat";
        Process process = Runtime.getRuntime().exec(config + " " +in);
        process.waitFor();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while (bufferedReader.ready())
        {
            String s = bufferedReader.readLine();
            System.out.println(s);
            if (s.equals("sat"))
            {
                System.err.println("SAT!");
                while (bufferedReader.ready())
                {
                    String out=bufferedReader.readLine();
//                    process_smt_output(out);
                    System.err.println(out);
                }
                return true;
            } else if (s.contains("unknown"))
            {
                System.err.println("SMT solver returned unknown! exiting.");
                return false;
            }
        }
        return false;
    }

    public static void process_smt_output(String out)
    {
        out=out.replace("(","");
        out=out.replace(")","");

        int _ind=out.indexOf('_');
        int var_begin=-1,var_end=_ind+1;
        for(int i=_ind;i>=0;i--)
            if(out.charAt(i)=='s' || out.charAt(i)=='c')
            {
                var_begin = i;
                break;
            }
        while(Character.isDigit(out.charAt(var_end)))
            var_end++;
        String var = out.substring(var_begin,var_end);

        int val = Integer.parseInt(out.substring(var_end+1).replace(" ",""));
        smt_out.put(var,val);
    }

    public static void make_smt(TransitionSystem upper, TransitionSystem lower, Vector<Polynomial> equalities,Polynomial cost, String out) throws Exception
    {
        int index=0;
        StringBuilder tmp = new StringBuilder();
        tmp.append("(set-option :print-success false)\n").append("(set-option :produce-unsat-cores true)\n").append("(set-option :produce-models true)\n");
        for(int i=0;i<Martingale.cCount;i++)
            tmp.append("(declare-const c_").append(i).append(" Real)\n");
        for(int i=0;i<Martingale.sCount;i++)
        {
            tmp.append("(declare-const s_").append(i).append(" Real)\n");
//            tmp.append("(assert (! (>= s_").append(i).append(" (- 1)) ").append(" :named const").append(index).append("))\n");
//            index++;
//            tmp.append("(assert (! (<= s_").append(i).append(" 1) ").append(" :named const").append(index).append("))\n");
//            index++;
        }

        for(int i=0;i<dCount;i++)
        {
            tmp.append("(declare-const d_").append(i).append(" Real)\n");
            tmp.append("(assert (! (>= d_").append(i).append(" 0) ").append(" :named const").append(index).append("))\n");
            index++;
        }

        tmp.append("(declare-const __C").append(" Real)\n");
        tmp.append("(assert (! (>= __C").append(" 0) ").append(" :named const").append(index).append("))\n");
        index++;

        for(String var:Parser.allVars)
            if(!var.equals("1"))
                tmp.append("(declare-const ").append(var).append(" Real)\n");

        //add equalities:
        for(Polynomial poly:equalities)
        {
            tmp.append("(assert (! (= 0 ").append(poly.toSMT()).append(") :named const").append(index).append(" ))\n");
            index++;
        }

        // x in pre-condition:
        for(Polynomial poly:upper.pre_condition.exprs)
        {
            tmp.append("(assert (! (<= 0 ").append(poly.toSMT()).append(") :named const").append(index).append(" ))\n");
            index++;
        }

        for(Polynomial poly:lower.pre_condition.exprs)
        {
            tmp.append("(assert (! (<= 0 ").append(poly.toSMT()).append(") :named const").append(index).append(" ))\n");
            index++;
        }


        // LESM(init)+f(init)>=0
        Polynomial LESM=lower.startNode.rank.deepCopy();
        LESM.add(cost.deepCopy());
        for(String var:lower.init.keySet())
            LESM.replaceVarWithPoly(var,new Polynomial(lower.init.get(var)));

        // Objective:
        Polynomial poly_obj = lower.startNode.rank.deepCopy();
        poly_obj.minus(upper.startNode.rank);
        for(String var:upper.init.keySet())
            poly_obj.replaceVarWithPoly(var,new Polynomial(upper.init.get(var)));
        for(String var:lower.init.keySet())
            poly_obj.replaceVarWithPoly(var,new Polynomial(lower.init.get(var)));

//        System.err.println(poly_obj.toNormalString());
        tmp.append("(assert (! (< 0 ").append(poly_obj.toSMT()).append(" ) :named const").append(index).append(" ))\n");
        tmp.append("(check-sat)\n");
        tmp.append("(get-value (");
        for(int i=0;i<Martingale.sCount;i++)
            tmp.append("s_").append(i).append(" ");
        for(int i=0;i<Martingale.cCount;i++)
            tmp.append("c_").append(i).append(" ");
        tmp.append("))\n");

        FileWriter fw = new FileWriter(out);
        fw.write(tmp.toString());
        fw.close();

    }

    public Handelman deepCopy() throws Exception
    {
        Handelman ret = new Handelman(from,TS);
        if(objective!=null)
            ret.setObjective(objective.deepCopy());
        ret.addConstraint(constraints.deepCopy());
        return ret;
    }

    public String toString()
    {
        StringBuilder ret = new StringBuilder("------------------------\n");
        if(TS!=null)
        {
            ret.append("TS: ").append(TS.name).append("\n");
            ret.append("From: ").append(from).append("\n");
        }
        else
            ret.append("TS: Global\n");
        for(Polynomial p:constraints.exprs)
            ret.append(p.toNormalString()).append(">=0\n");
        ret.append("========================\n");
        ret.append(objective.toNormalString()).append(">=0").append("\n");
        ret.append("------------------------");
        return ret.toString();
    }
}
