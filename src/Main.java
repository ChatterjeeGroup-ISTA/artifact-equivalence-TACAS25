import java.util.Vector;

public class Main
{
//    public static Rational eps = Rational.one;
    public static Polynomial eps = new Polynomial(new Rational(1,1000));//new Polynomial(new Rational(1,100));//new Polynomial("__eps");
    public static Polynomial C = new Polynomial("__C");
    public static int mu = 2, degree = 0;

    public static String fileName1 = "", fileName2="", solver = "", workingdir="",
            solversDir="", varType="", OST_condition = "", sting = "",aspic="",reset_or_return="",distance_or_equivalence="";

    public static void main(String[] args) throws Exception
    {
        long startTime = System.currentTimeMillis();

        fileName1 = args[0];
        fileName2 = args[1];
        solver = "gurobi";

        solversDir = "solvers";
        varType = "Real"; // Real or Int
        mu=degree = Integer.parseInt(args[2]);

        OST_condition = args[3];
        sting=args[4];
        aspic = args[5];
        workingdir = args[6];

        reset_or_return = args[7];

        distance_or_equivalence = args[8];
        //tmp:
//        mu = degree = 2;


        StringBuilder params = new StringBuilder();
        for(int i=2;i<=5;i++)
            params.append(args[i]).append(" ");
        System.out.println(params);

        System.err.println("------------Reading First Transition System T1------------");
        TransitionSystem T1 = Parser.parse(fileName1,"input1");
        fileName1 = fileName1.replace("/", "_");

        System.err.println("------------Reading Second Transition System T2------------");
        TransitionSystem T2 = Parser.parse(fileName2,"input2");
        fileName2 = fileName2.replace("/", "_");

        System.err.println("------------Verifying T1 transitions------------");
        T1.verify();
        T1.config_observe(reset_or_return);
        System.err.println("------------Verifying T2 transitions------------");
        T2.verify();
        T2.config_observe(reset_or_return);


        if(!aspic.equals("noaspic"))
        {
            System.err.println("------------Extracting Invariants for T1 from ASPIC------------");
            T1.get_aspic_invariants();
            for (CFGNode n : T1.states)
                if (T1.aspic_invariant != null && T1.aspic_invariant.get(n.id) != null)
                    System.err.println("State: " + n.id + "  ASPIC-Invariant: " + T1.aspic_invariant.get(n.id).toNormalString());
            System.err.println("------------Extracting Invariants for T2 from ASPIC------------");
            T2.get_aspic_invariants();
            for (CFGNode n : T2.states)
                if (T2.aspic_invariant != null && T2.aspic_invariant.get(n.id) != null)
                    System.err.println("State: " + n.id + "  ASPIC-Invariant: " + T2.aspic_invariant.get(n.id).toNormalString());
        }
        if(!sting.equals("nosting"))
        {
            System.err.println("------------Extracting Invariants for T1 from STING------------");
            T1.get_sting_invariants();
            for(CFGNode n: T1.states)
                if(T1.sting_invariant.containsKey(n.id))
                    System.err.println("State: "+n.id+"  Sting-Invariant: "+T1.sting_invariant.get(n.id).toNormalString());

            System.err.println("------------Extracting Invariants for T2 from STING------------");
            T2.get_sting_invariants();
            for(CFGNode n: T2.states)
                if(T2.sting_invariant.containsKey(n.id))
                    System.err.println("State: "+n.id+"  Sting-Invariant: "+T2.sting_invariant.get(n.id).toNormalString());
        }
//
        System.err.println("------------Setting Template for Cost Function------------");
        Polynomial cost = Martingale.MakeCostTemplate();
        System.err.println("Cost: "+cost.toNormalString());
        System.err.println("------------Setting Template for UESM of T1------------");
        Martingale.MakeTemplate(T1);
        for(CFGNode n:T1.states)
            System.err.println("State: "+n.id+"  -->  "+n.rank.toNormalString());
        System.err.println("------------Setting Template for LESM of T2------------");
        Martingale.MakeTemplate(T2);
        for(CFGNode n:T2.states)
            System.err.println("State: "+n.id+"  -->  "+n.rank.toNormalString());



        Vector<Handelman> handelmanVector = Martingale.generate(T1,T2,cost,OST_condition);
        if(reset_or_return.equals("return"))
        {
            System.err.println("------------Forcing Cost Function To Be non-negative upon termination in T1------------");
            Handelman NN1 = Martingale.nonNegativity(T1,cost);
            System.err.println("------------Forcing Cost Function To Be non-negative upon termination in T2------------");
            Handelman NN2 = Martingale.nonNegativity(T2,cost);
            handelmanVector.add(NN1);
            handelmanVector.add(NN2);
        }

        if(distance_or_equivalence.equals("distance"))
        {
            System.err.println("------------Forcing Cost Function To Be 1-Lipschitz on T1------------");
            Handelman lipschitz1 = Martingale.lipschitz_condition(T1,cost);
            handelmanVector.add(lipschitz1);
            System.err.println("------------Forcing Cost Function To Be 1-Lipschitz on T2------------");
            Handelman lipschitz2 = Martingale.lipschitz_condition(T2,cost);
            handelmanVector.add(lipschitz2);
        }

        Vector<Polynomial> equalities = new Vector<>();
        for(Handelman h:handelmanVector)
        {
            System.err.println(h.toString());
            equalities.addAll(h.generate_equalities());
        }
        System.err.println("------------Running "+solver+" on generated system of inequalities------------");
        double result = Handelman.run_solver(T1,T2,equalities,solver,handelmanVector,cost);
        if (result > 0.001)
        {
            System.out.println("Successfully refuted equivalence!");
            if(distance_or_equivalence.equals("distance"))
                System.out.println("Distance found: "+result);
        }
        else
            System.out.println("Failed");
        long endTime = System.currentTimeMillis();
        System.out.println("total time used: " + (endTime - startTime));

//        {
//            String tmp=cost.toNormalString();
//            for (String var : Handelman.result.keySet())
//                tmp = tmp.replace(var+" ", Handelman.result.get(var).toString());
//            System.err.println("f: "+tmp);
//            for (CFGNode n : T1.states)
//            {
//                String p = n.rank.toNormalString();
//                for (String var : Handelman.result.keySet())
//                    p = p.replace(var+" ", Handelman.result.get(var).toString());
//                System.err.println("State: " + n.id + "  -->  " + p);
//            }
//            for (CFGNode n : T2.states)
//            {
//                String p = n.rank.toNormalString();
//                for (String var : Handelman.result.keySet())
//                    p = p.replace(var+" ", Handelman.result.get(var).toString());
//                System.err.println("State: " + n.id + "  -->  " + p);
//            }
//        }
    }
}