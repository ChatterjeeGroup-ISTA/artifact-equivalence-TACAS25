import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class InvUtil
{
    public static Set<Integer> unreachable = new TreeSet<>();

    public static Map<Integer,PolynomialPredicate> get_invariants_aspic(TransitionSystem T) throws Exception
    {
        String ts = make_fst(T);
        FileWriter fw = new FileWriter(Main.workingdir + "/" + T.name + ".fst");
        fw.write(ts);
        fw.close();
        return run_aspic(T);
    }

    public static Map<Integer, PolynomialPredicate> run_aspic(TransitionSystem T) throws Exception
    {
        Map<Integer, PolynomialPredicate> inv = new TreeMap<>();

        Process process = Runtime.getRuntime().exec("./" + Main.solversDir + "/aspicV3.4 -log 0 -cinv " + Main.workingdir + "/" + T.name + ".fst");
        process.waitFor(30, TimeUnit.SECONDS);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while (bufferedReader.ready())
        {
            String s = bufferedReader.readLine();
//            System.err.println(s);
            if (s.contains("----->") && !s.startsWith("tmp"))
            {
                int node_id = Integer.parseInt(s.substring(1, s.indexOf('-') - 1).replaceAll(" ", ""));

                String bexpr = s.substring(s.lastIndexOf("{"), s.lastIndexOf("}") + 1);
                bexpr = bexpr.replaceAll("\\{0\\}", "-1>=0");
                bexpr = bexpr.replaceAll("\\{", "");
                bexpr = bexpr.replaceAll("\\}", "");


//                System.err.println(node_id + " -> " + bexpr);
                if (bexpr.equals("-1>=0"))
                    unreachable.add(node_id);
                Parser.readTokens(bexpr);
                Node tmp = Parser.parseBexpr(null, 0, Parser.getTokenCount() - 1, T);
                PolynomialPredicate lp = tmp.guard.getClauses().firstElement();
                lp = get_small_part(lp);
                inv.put(node_id, lp);
            }
        }
        return inv;
    }

    public static PolynomialPredicate get_small_part(PolynomialPredicate pp)
    {
        PolynomialPredicate ret = new PolynomialPredicate();
        for(Polynomial poly: pp.exprs)
        {
            boolean add = true;
            for (Monomial m : poly.terms.keySet())
                if (poly.terms.get(m).numerator > 1000000 || poly.terms.get(m).denominator > 1000000)
                    add = false;
            if(add)
                ret.add(poly);
        }
        return ret;
    }

    public static String make_fst(TransitionSystem T)
    {
        Vector<String> tmpStates = new Vector<>();
        StringBuilder transitions = new StringBuilder();

        int index = 0;
        for (Transition tau : T.transitions)
        {
            if(tau.probVarName.isEmpty())
            {
                StringBuilder tran = new StringBuilder("transition t" + index + " := {\n"); index++;
                tran.append("from := l").append(tau.from.id).append(";\n");
                tran.append("to := l").append(tau.to.id).append(";\n");
                tran.append("guard := ");
//                System.err.println(tau.detGuard.toNormalString());
                tran.append(tau.detGuard.toNormalString_linear()).append(";\n");
                tran.append("action := ");
                if (tau.detVarName.isEmpty())
                    tran.append(";\n");
                else
                {
                    for (int i = 0; i < tau.detVarName.size(); i++)
                        if (tau.detUpdate.elementAt(i).degree() <= 1)
                            tran.append(tau.detVarName.elementAt(i)).append("' = ").append(tau.detUpdate.elementAt(i).toNormalString()).append(", ");
                        else
                            tran.append(tau.detVarName.elementAt(i)).append("' = ?, ");
                    tran = new StringBuilder(tran.substring(0, tran.length() - 2) + ";\n");
                }
                tran.append("};\n");

                transitions.append("\n").append(tran);
            }
            else
            {
                StringBuilder tran1 = new StringBuilder("transition t"+index + ":= {\n"); index++;
                tran1.append("from := l").append(tau.from.id).append(";\n");
                tran1.append("to := tmp").append(tau.from.id).append(";\n");   tmpStates.add("tmp"+tau.from.id);
                tran1.append("guard := true;\n");
                tran1.append("action := ");
                for(String var:tau.probVarName)
                    tran1.append(var).append("' =?, ");
                tran1 = new StringBuilder(tran1.substring(0, tran1.length() - 2) + ";\n");
                tran1.append("};\n");

                StringBuilder tran2 = new StringBuilder("transition t"+index + ":= {\n"); index++;
                tran2.append("from := tmp").append(tau.from.id).append(";\n");
                tran2.append("to := l").append(tau.to.id).append(";\n");
                tran2.append("guard := ");
                for(int i=0;i<tau.probVarName.size();i++)
                {
                    String varName = tau.probVarName.elementAt(i);
                    Distribution update = tau.probUpdate.elementAt(i);
                    Rational lower = update.lower_bound(),upper = update.upper_bound();
                    if(lower!=null)
                        tran2.append(lower.toNormalString()).append("<=").append(varName).append(" && ");
                    if(upper!=null)
                        tran2.append(varName).append("<=").append(upper.toNormalString()).append(" && ");
                    if(lower==null && upper==null)
                        tran2.append("true && ");
                }
                tran2 = new StringBuilder(tran2.substring(0, tran2.length() - 4) + ";\n");
                tran2.append("action := ;\n");
                tran2.append("};\n");

                transitions.append("\n").append(tran1);
                transitions.append("\n").append(tran2);
            }
        }
        StringBuilder ret = new StringBuilder("model input {\nvar ");
        for (String var : T.allVars)
            if (!var.equals("1"))
                ret.append(var).append(", ");
        ret = new StringBuilder(ret.substring(0, ret.length() - 2) + ";\nstates ");
        for (CFGNode n : T.states)
            ret.append("l").append(n.id).append(", ");
        for(String tmpState:tmpStates)
            ret.append(tmpState).append(", ");
        ret = new StringBuilder(ret.substring(0, ret.length() - 2) + ";\n");
        ret.append(transitions);
        ret.append("}\n\n");
        ret.append("strategy s1 {\n" + "Region init := {state= l").append(T.startNode.id).append(" && ").append(T.pre_condition.toNormalString()).append("};\n").append("}");
//        System.err.println(ts);
        return ret.toString();
    }



    public static Map<Integer,PolynomialPredicate> get_invariants_sting(TransitionSystem T) throws Exception
    {
        String ts = make_sting(T);
        FileWriter fw = new FileWriter(Main.workingdir + "/" + T.name + ".sting");
        fw.write(ts);
        fw.close();
        return run_sting(T);
    }

    public static String make_sting(TransitionSystem T) throws Exception
    {
        StringBuilder ret = new StringBuilder();
        ret.append("variable [");
        for(String var: T.allVars)
            if(!var.equals("1"))
                ret.append(var).append(" ");
        ret.append("]\n");
        ret.append("Location ").append("l").append(T.startNode.id).append("\n");
        for(Polynomial poly: T.pre_condition.exprs)
            ret.append("\t").append(poly.toNormalString()).append(" >= 0\n");
        ret.append("\n");
        int index = 0;
        for(Transition tau: T.transitions)
        {
            if(tau.probVarName.isEmpty())
            {
                ret.append("Transition ").append("t").append(index).append(": ").append("l").append(tau.from.id).append(", ").append("l").append(tau.to.id).append(",\n");
                for (Polynomial poly : tau.detGuard.exprs)
                    ret.append("\t").append(poly.normalize().toNormalString()).append(" >= 0\n");
                for (int i = 0; i < tau.detVarName.size(); i++)
                {
                    String var=tau.detVarName.elementAt(i);
                    Polynomial upd=tau.detUpdate.elementAt(i);
                    ret.append("\t").append(upd.normalizing_factor().numerator).append("*'").append(var).append(" = ").append(upd.normalize().toNormalString()).append("\n");
                }
                Vector<String> preserved = new Vector<>();
                for (String var : T.allVars)
                    if (!tau.detVarName.contains(var) && !var.equals("1"))
                        preserved.add(var);
                if (!preserved.isEmpty())
                {
                    ret.append("\tpreserve[");
                    for (int i = 0; i < preserved.size(); i++)
                    {
                        if (i != 0)
                            ret.append(", ");
                        ret.append(preserved.elementAt(i));
                    }
                    ret.append("]\n");
                }
                ret.append("\n");
                index++;
            }
            else
            {
                ret.append("Transition ").append("t").append(index).append(": ").append("l").append(tau.from.id).append(", ").append("l").append(tau.to.id).append(",\n");
                for(int i=0;i<tau.probVarName.size();i++)
                {
                    String var = tau.probVarName.elementAt(i);
                    Distribution upd = tau.probUpdate.elementAt(i);
                    Rational lower = upd.lower_bound(),upper=upd.upper_bound();
                    if(lower!=null)
                        ret.append("\t").append(lower.denominator).append("*'").append(var).append(">=").append(lower.numerator).append("\n");
                    if(upper!=null)
                        ret.append("\t").append(upper.denominator).append("*'").append(var).append("<=").append(upper.numerator).append("\n");

                }
                Vector<String> preserved = new Vector<>();
                for (String var : T.allVars)
                    if (!tau.probVarName.contains(var) && !var.equals("1"))
                        preserved.add(var);
                if(!preserved.isEmpty())
                {
                    ret.append("\tpreserve[");
                    for (int i = 0; i < preserved.size(); i++)
                    {
                        if (i != 0)
                            ret.append(", ");
                        ret.append(preserved.elementAt(i));
                    }
                    ret.append("]\n");
                }
                ret.append("\n");
                index++;
            }
        }
        ret.append("end");
        return ret.toString();
    }

    public static Map<Integer, PolynomialPredicate> run_sting(TransitionSystem T) throws Exception
    {
        Map<Integer, PolynomialPredicate> inv = new TreeMap<>();
        String[] command = {"./" + Main.solversDir + "/lsting"};
        ProcessBuilder pb = new ProcessBuilder(command).redirectInput(new File(Main.workingdir + "/" + T.name + ".sting"));
        Process process =pb.start();
        process.waitFor(300, TimeUnit.SECONDS);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        boolean read_inv = false;
        int cur_loc_id = -1;
        while (bufferedReader.ready())
        {
            String s = bufferedReader.readLine();
//            System.err.println(s);
            if(read_inv && s.startsWith("Location:"))
            {
                String[] tmp = s.split(":");
                String loc = tmp[tmp.length-1].substring(1);
                try {
                    cur_loc_id = Integer.parseInt(loc);
                } catch(NumberFormatException e){
                    continue;
                }
            }
            else if(read_inv && s.contains("Invariant: [["))
            {
                while(true)
                {
                    String bexpr = bufferedReader.readLine();
                    if(bexpr.contains("]]"))
                        break;
                    else if(!bexpr.isEmpty())
                    {

                        if (bexpr.contains("false"))
                        {
                            unreachable.add(cur_loc_id);
                            continue;
                        }
                        else if(bexpr.contains("True"))
                            continue;
                        bexpr = bexpr.replace(" = ", " == ");
//                        System.err.println(cur_loc_id+" -> "+bexpr);
                        Parser.readTokens(bexpr);
                        Node tmp = Parser.parseBexpr(null, 0, Parser.getTokenCount() - 1,T);
                        PolynomialPredicate lp = tmp.guard.getClauses().firstElement();
                        if(inv.containsKey(cur_loc_id))
                            inv.get(cur_loc_id).add(lp);
                        else
                            inv.put(cur_loc_id, lp);
                    }
                }
            }
            else if(s.contains("Transition Relation Ends"))
                read_inv = true;

        }
        return inv;
    }

}