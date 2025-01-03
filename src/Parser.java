// *** multiple deterministic assignments and multiple assume statements are allowed, every other type of transition must have a single statement ***
import java.io.File;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

public class Parser
{
//    public static Set<String> allVars = new HashSet<>();
    public static Vector<String> tokens = new Vector<>();
    public static Set<String> allVars = new HashSet<>();
    public static int nondetCount = 0;


    static
    {
        allVars.add("1");
    }

    public static void restart()
    {
        tokens.clear();
        nondetCount = 0;
    }

    public static Node parseProg(int beginIndex, int endIndex, TransitionSystem T) throws Exception
    {
        if (!getToken(beginIndex).equals("START"))
            throw new Exception("program should start with START");
        Node cur = new Node(null, beginIndex, endIndex, "prog");

        T.startNode = T.addOrReturnState(Integer.parseInt(getToken(beginIndex + 2)));

        int preBegin = beginIndex+4;
        int closedBracePre = -1;
        // PRE:
        if(!getToken(preBegin).equals("PRE") || !getToken(preBegin+1).equals(":") || !getToken(preBegin+2).equals("{"))
        {
            closedBracePre=preBegin-2;
            System.err.println("no PreCondition");
        }
        else
        {
            for (int i = preBegin; i <= endIndex; i++)
                if (getToken(i).equals("}"))
                {
                    closedBracePre = i;
                    break;
                }
            if (closedBracePre == -1)
                throw new Exception("open brace for precondition not closed");
            Node PreNode = parseBexpr(cur, preBegin+3, closedBracePre - 1,T);
            T.pre_condition = PreNode.guard.getClauses().firstElement();
        }
        //INIT:
        int init_begin = closedBracePre +2; //"pre"};
        if(!getToken(init_begin).equals("INIT") || !getToken(preBegin+1).equals(":") || !getToken(preBegin+2).equals("{"))
            System.err.println("no initial point was given");
        else
        {
            int closedBraceInit=-1;
            for (int i = init_begin; i <= endIndex; i++)
                if (getToken(i).equals("}"))
                {
                    closedBraceInit = i;
                    break;
                }
            if (closedBraceInit == -1)
                throw new Exception("open brace for INIT not closed");
            Node initNode = parseInit(cur, init_begin+3, closedBraceInit - 1,T);
            T.init = initNode.mapping;
            Node tmpNode = parseBexpr(cur, init_begin+3, closedBraceInit - 1,T);
            T.pre_condition.add(tmpNode.guard.getClauses().firstElement());
        }
        // Transition System:
        int lastFROM = -1;
        for (int i = beginIndex; i <= endIndex; i++)
            if (getToken(i).equals("FROM"))
            {
                if (lastFROM != -1)
                    throw new Exception(" \"TO: index\" expected before @" + i);
                lastFROM = i;
            }
            else if (getToken(i).equals("TO"))
            {
                Node ch = parseTransition(cur, lastFROM, i + 3,T);
                lastFROM = -1;
            }
        return cur;
    }

    public static Node parseInit(Node par, int beginIndex, int endIndex,TransitionSystem T) throws Exception
    {
        Node cur = new Node(par,beginIndex,endIndex,"init");
        for(int i=beginIndex;i<=endIndex;i++)
        {
            if(getToken(i).equals("=="))
            {
                String varName = getToken(i-1);
                StringBuilder val_str = new StringBuilder();
                for(int j=i+1;j<=endIndex;j++)
                {
                    if (getToken(j).equals("&"))
                        break;
                    val_str.append(getToken(j));
                }
                Rational val = Rational.parseRational(val_str.toString());
                cur.mapping.put(varName,val);
            }
        }
        return cur;
    }

    public static Node parseTransition(Node par, int beginIndex, int endIndex,TransitionSystem T) throws Exception
    {
        if (!getToken(endIndex).equals(";"))
            throw new Exception("Transition must end with ; @" + beginIndex + "-" + endIndex);
        Node cur = new Node(par, beginIndex, endIndex, "Transition");
        int vIndex = Integer.parseInt(getToken(beginIndex + 2)), uIndex = Integer.parseInt(getToken(endIndex - 1));
        CFGNode vNode = T.addOrReturnState(vIndex);
        CFGNode uNode = T.addOrReturnState(uIndex);

        Vector<Transition> transitionVector = new Vector<>();
        transitionVector.add(new Transition(vNode, uNode));
        int lastColon = beginIndex + 3;
        for (int i = beginIndex + 4; i <= endIndex - 4; i++)
        {
            if (getToken(i).equals(";"))
            {
                Node ch = parseStmt(cur, lastColon + 1, i - 1,T);
                if (ch.type.equals("assume") || ch.type.equals("observe"))
                {
                    if (ch.type.equals("observe"))
                    {
                        vNode.isObserve = true;
                        vNode.observation=ch.guard.deepCopy();
                    }
                    if (ch.guard.size() == 1)
                        for (Transition t : transitionVector)
                            t.detGuard.add(ch.guard.getClauses().elementAt(0));
                    else if (ch.guard.size() > 1)
                    {
                        Vector<Transition> tmp = new Vector<>();
                        for (int j = 0; j < ch.guard.size(); j++)
                        {
                            for (Transition t : transitionVector)
                            {
                                Transition tp = t.deepCopy();
                                tp.detGuard.add(ch.guard.getClauses().elementAt(j));
                                tmp.add(tp);
                            }
                        }
                        transitionVector = tmp;
                    }
                }
                else if(ch.type.equals("prob"))
                {
                    for(Transition t:transitionVector)
                        t.prob = ch.prob;
                }
                else if(ch.type.equals("score"))
                {
                    vNode.isScore=true;
                    for(Transition t:transitionVector)
                    {
                        t.score = ch.score.deepCopy();
                        t.score.multiplyByValue(new Rational(1,ch.scoreBound));
                    }
                }
                else
                {
                    for (Transition t : transitionVector)
                    {
                        if(ch.expr!=null)
                        {
                            t.detVarName.add(ch.varName);
                            t.detUpdate.add(ch.expr);
                        }
                        else
                        {
                            t.probVarName.add(ch.varName);
                            t.probUpdate.add(ch.distr);         // if stochastic assignment
                        }
                    }
                }
                lastColon = i;
            }
        }
        for (Transition t : transitionVector)
            T.addTransition(t);
        return cur;
    }

    public static Node parseStmt(Node par, int beginIndex, int endIndex, TransitionSystem T) throws Exception
    {
        if (getToken(beginIndex).equals("assume"))
        {
            Node cur = new Node(par, beginIndex, endIndex, "assume");
            Node ch = parseBexpr(cur, beginIndex + 2, endIndex - 1,T);
            cur.guard = ch.guard;
            return cur;
        }
        else if (getToken(beginIndex).equals("observe"))
        {
            Node cur = new Node(par, beginIndex, endIndex, "observe");
            Node ch = parseBexpr(cur, beginIndex + 2, endIndex - 1,T);
            cur.guard = ch.guard;
            return cur;
        }
        else if(getToken(beginIndex).equals("score"))
        {
            Node cur = new Node(par, beginIndex, endIndex, "score");
            int comma = -1;
            for(int i=beginIndex;i<=endIndex;i++)
                if(getToken(i).equals(","))
                    comma=i;
            Node ch1 = parseExpr(cur,beginIndex+2,comma-1,T);
            long bound = Long.parseLong(getToken(comma+1));
            cur.score=ch1.expr;
            cur.scoreBound=bound;
            return cur;
        }
        else if(getToken(beginIndex).equals("prob"))
        {

            Node cur = new Node(par, beginIndex, endIndex, "prob");
            Node ch = parseProb(cur, beginIndex + 2, endIndex - 1);
            cur.prob = ch.prob;
            return cur;
        }
        else
        {
            Node cur = new Node(par, beginIndex, endIndex, "assignment");
            if (!getToken(beginIndex + 1).equals(":="))
                throw new Exception("assignment without := @" + beginIndex + "-" + endIndex);
            int sgn = beginIndex + 1;

            String varName = getToken(beginIndex);
            T.allVars.add(varName);

            Node ch = parseUpdate(cur,sgn+1,endIndex,T);
            if(ch.type.equals("poly"))
            {
                cur.varName = varName;
                cur.expr = ch.expr;
            }
            else
            {
                cur.varName = varName;
                cur.distr = ch.distr;
            }
            return cur;
        }
    }

    public static Node parseUpdate(Node par, int beginIndex, int endIndex, TransitionSystem T) throws Exception
    {
        if(Distribution.isDistribution(getToken(beginIndex)))
        {
            if(!getToken(beginIndex+1).equals("(") || !getToken(endIndex).equals(")"))
                throw new Exception("distribution name must be followed by ( and ended by )");
            Node cur = new Node(par,beginIndex,endIndex,"distribution");
            String type = getToken(beginIndex);
            Vector<Rational> params = new Vector<>();
            StringBuilder cur_rational= new StringBuilder();
            Polynomial flip = null;
            if(type.equals("flip"))
            {
                Node ch = parseExpr(cur,beginIndex+2,endIndex-1,T);
                flip=ch.expr;
                Distribution distr = new Distribution(type,params,flip);
                cur.distr=distr;
            }
            else
            {
                for (int i = beginIndex + 2; i <= endIndex; i++)
                {
                    if (getToken(i).equals(",") || getToken(i).equals(")"))
                    {
                        params.add(Rational.parseRational(cur_rational.toString()));
                        cur_rational = new StringBuilder();
                    }
                    else
                        cur_rational.append(getToken(i));
                }

                Distribution distr = new Distribution(type, params,flip);
                cur.distr = distr;
            }
            return cur;
        }
        else
        {
            Node cur = new Node(par,beginIndex,endIndex,"poly");
            Node ch = parseExpr(cur,beginIndex,endIndex,T);
            cur.expr=ch.expr;
            return cur;
        }
    }

    public static Node parseProb(Node par, int beginIndex, int endIndex) throws Exception       // prob(x/y)
    {
        if(endIndex>beginIndex+2)
            throw new Exception("non-numeric probability in prob(.)");

        StringBuilder sum = new StringBuilder();
        for(int i=beginIndex;i<=endIndex;i++)
            sum.append(getToken(i));
        Rational prob = Rational.parseRational(sum.toString()); //new Rational(Integer.parseInt(getToken(beginIndex)),Integer.parseInt(getToken(endIndex)));
        Node cur = new Node(par,beginIndex,endIndex,"Prob number");
        cur.prob = prob;
        return cur;
    }

    public static Node parseBexpr(Node par, int beginIndex, int endIndex, TransitionSystem T) throws Exception
    {

        Node cur = new Node(par, beginIndex, endIndex, "Bexpr");

        for (int i = beginIndex; i <= endIndex; i++)
            if (getToken(i).equals("nondet"))
                return cur;

        Vector<Integer> ors = new Vector<>();
        Vector<Integer> ands = new Vector<>();


        ors.add(beginIndex - 1);
        ands.add(beginIndex - 1);

        int openPar = 0;
        for (int i = beginIndex; i <= endIndex; i++)
            if (getToken(i).equals("("))
                openPar++;
            else if (getToken(i).equals(")"))
                openPar--;
            else if (openPar == 0 && getToken(i).equals("|") && getToken(i + 1).equals("|"))
            {
                ors.add(i + 1);
                i++;
            }
            else if (openPar == 0 && getToken(i).equals("&") && getToken(i + 1).equals("&"))
            {
                ands.add(i + 1);
                i++;
            }
        ors.add(endIndex + 2);
        ands.add(endIndex + 2);
        if (ors.size() > 2)
        {
            for (int i = 1; i < ors.size(); i++)
            {
                Node ch = parseBexpr(cur, ors.elementAt(i - 1) + 1, ors.elementAt(i) - 2,T);
                cur.guard.or(ch.guard);
            }
            return cur;
        }
        if (ands.size() > 2)
        {
            for (int i = 1; i < ands.size(); i++)
            {
                Node ch = parseBexpr(cur, ands.elementAt(i - 1) + 1, ands.elementAt(i) - 2,T);
                if(cur.guard.size()==0)
                    cur.guard.or(ch.guard);
                else
                    cur.guard.and(ch.guard);
            }
            return cur;
        }

        boolean isCompletlyInsidePar = true;
        openPar = 0;
        for (int i = beginIndex; i <= endIndex; i++)
        {
            if (getToken(i).equals("("))
                openPar++;
            else if (getToken(i).equals(")"))
                openPar--;
            if (openPar == 0 && i != endIndex)
            {
                isCompletlyInsidePar = false;
                break;
            }
        }
        if (isCompletlyInsidePar)
        {
            Node ch = parseBexpr(cur, beginIndex + 1, endIndex - 1,T);
            if(cur.guard.size()==0)
                cur.guard.or(ch.guard);
            else
                cur.guard.and(ch.guard);
            return cur;
        }
        if (getToken(beginIndex).equals("!"))
        {
            Node ch = parseBexpr(cur, beginIndex + 1, endIndex,T);
            cur.guard = ch.guard.negate();
            return cur;
        }
        Node ch = parseLiteral(cur, beginIndex, endIndex,T);
        if(cur.guard.size()==0)
            cur.guard.or(ch.guard);
        else
            cur.guard.and(ch.guard);
        return cur;
    }

    public static Node parseLiteral(Node par, int beginIndex, int endIndex, TransitionSystem T) throws Exception
    {
        int sgn = -1, type = -1; //types: 0: "<="  1: ">="   2: ">"   3: "<"   4: "=="    5: "!="
        for (int i = beginIndex; i <= endIndex; i++)
            if (getToken(i).equals("<="))
            {
                sgn = i;
                type = 0;
            }
            else if (getToken(i).equals(">="))
            {
                sgn = i;
                type = 1;
            }
            else if (getToken(i).equals(">"))
            {
                sgn = i;
                type = 2;
            }
            else if (getToken(i).equals("<"))
            {
                sgn = i;
                type = 3;
            }
            else if (getToken(i).equals("=="))
            {
                sgn = i;
                type = 4;
            }
            else if (getToken(i).equals("!="))
            {
                sgn = i;
                type = 5;
            }
        if (sgn == beginIndex || sgn == endIndex)
            throw new Exception("literal starts or ends with sign @" + beginIndex + "-" + endIndex);
        Node cur = new Node(par, beginIndex, endIndex, "literal");
        Node left = null;
        Node right=null;
        if (sgn == -1)
        {
            type = 5;
            left = parseExpr(cur, beginIndex, endIndex,T);
            right = new Node(cur, endIndex, endIndex, "0");
            right.expr = new Polynomial(Rational.zero);
        }
        else
        {
            left = parseExpr(cur, beginIndex, sgn - 1,T);
            right = parseExpr(cur, sgn + 1, endIndex,T);
        }
        if (type == 0)   //left<=right   -->    right-left>=0
        {
            Polynomial lc = right.expr.deepCopy();
            lc.minus(left.expr);
            PolynomialPredicate lp = new PolynomialPredicate();
            lp.add(lc);
            cur.guard.or(lp);
        }
        else if (type == 1)  //left>=right    -->    left-right>=0
        {
            Polynomial lc = left.expr.deepCopy();
            lc.minus(right.expr);
            PolynomialPredicate lp = new PolynomialPredicate();
            lp.add(lc);
            cur.guard.or(lp);
        }
        else if (type == 2) // left > right   ->   left -right >=eps   ->   left - right -eps >=0
        {
            Polynomial lc = left.expr.deepCopy();
            lc.minus(right.expr); // left - right
            lc.minus(Main.eps);  // left - right - eps
            PolynomialPredicate lp = new PolynomialPredicate();
            lp.add(lc);
            cur.guard.or(lp);
        }
        else if (type == 3) //left < right  -->   right - left > eps   -->   right - left -eps >=0
        {
            Polynomial lc = right.expr.deepCopy();
            lc.minus(left.expr); // right - left
            lc.minus(Main.eps);  // right - left - eps
            PolynomialPredicate lp = new PolynomialPredicate();
            lp.add(lc);
            cur.guard.or(lp);
        }
        else if (type == 4)  //left==right  -->  left-right>=0 and right-left>=0
        {
            Polynomial lc = right.expr.deepCopy();
            lc.minus(left.expr);

            Polynomial lc2 = left.expr.deepCopy();
            lc2.minus(right.expr);

            PolynomialPredicate lp = new PolynomialPredicate();
            lp.add(lc);
            lp.add(lc2);
            cur.guard.or(lp);
        }
        else
        {
            Polynomial lc = right.expr.deepCopy();
            lc.minus(left.expr);
            lc.minus(Main.eps);

            Polynomial lc2 = left.expr.deepCopy();
            lc2.minus(right.expr);
            lc2.minus(Main.eps);

            PolynomialPredicate lp1 = new PolynomialPredicate(), lp2 = new PolynomialPredicate();
            lp1.add(lc);
            lp2.add(lc2);
            cur.guard.or(lp1);
            cur.guard.or(lp2);
        }

        return cur;
    }

    public static Node parseExpr(Node par, int beginIndex, int endIndex, TransitionSystem T) throws Exception
    {
        Vector<Integer> signIndex = new Vector<>();
        Vector<String> signType = new Vector<>();
        if (!getToken(beginIndex).equals("-"))
        {
            signIndex.add(beginIndex - 1);
            signType.add("+");
        }
        int openPar = 0;
        for (int i = beginIndex; i <= endIndex; i++)
        {
            if (getToken(i).equals("("))
                openPar++;
            else if (getToken(i).equals(")"))
                openPar--;
            if (openPar == 0 && (getToken(i).equals("+")
                    || (getToken(i).equals("-") && (i - 1 < beginIndex || (i - 1 >= beginIndex && !getToken(i - 1).equals("*") && !getToken(i - 1).equals("+"))))))
            {
                signIndex.add(i);
                signType.add(getToken(i));
            }
        }
        signIndex.add(endIndex + 1);
        signType.add("+");

        Node cur = new Node(par, beginIndex, endIndex, "expr");
        cur.expr = new Polynomial();
        for (int i = 0; i + 1 < signIndex.size(); i++)
        {
            Node ch = parseTerm(cur, signIndex.elementAt(i) + 1, signIndex.elementAt(i + 1) - 1,T);
            if (signType.elementAt(i).equals("+"))
                cur.expr.add(ch.expr);
            else
                cur.expr.minus(ch.expr);
        }
        return cur;
    }

    public static Node parseTerm(Node par, int beginIndex, int endIndex, TransitionSystem T) throws Exception
    {
        if ((beginIndex == endIndex && isNumeric(getToken(beginIndex)))) //constant
        {
            Node cur = new Node(par, beginIndex, endIndex, "constant");
            long val = Long.parseLong(getToken(beginIndex));
            cur.expr = new Polynomial();
            cur.expr.add(Monomial.one, new Rational(val, 1));
            return cur;
        }
        else if (beginIndex == endIndex - 1 && isNumeric(getToken(endIndex))) //negative constant
        {
            Node cur = new Node(par, beginIndex, endIndex, "constant");
            long val = -Long.parseLong(getToken(endIndex));
            cur.expr = new Polynomial();
            cur.expr.add(Monomial.one, new Rational(val, 1));
            return cur;
        }
        else if (beginIndex == endIndex)    //var
        {
            Node cur = new Node(par, beginIndex, endIndex, "var");
            String var = getToken(beginIndex);
            T.allVars.add(var);
            if (Character.isDigit(var.charAt(0)))
                throw new Exception("Incorrect var name @" + beginIndex);
            cur.expr = new Polynomial();
            cur.expr.add(new Monomial(var), new Rational(1, 1));
            return cur;
        }
        else  // (...) or [] * []
        {
            Node cur = new Node(par, beginIndex, endIndex, "term mul");
            cur.expr = new Polynomial();
            Vector<Integer> sgnIndex = new Vector<>();
            Vector<String> sgnType = new Vector<>();
            sgnIndex.add(beginIndex - 1);
            sgnType.add("*");
            int openPar = 0;
            for (int i = beginIndex; i <= endIndex; i++)
                if (getToken(i).equals("("))
                    openPar++;
                else if (getToken(i).equals(")"))
                    openPar--;
                else if (openPar == 0 && (getToken(i).equals("*") || getToken(i).equals("/")))
                {
                    sgnIndex.add(i);
                    sgnType.add(getToken(i));
                }
                else if (getToken(i).equals("%"))
                {
                    throw new Exception("% is not supported. @" + beginIndex + "-" + endIndex);
                }
            sgnIndex.add(endIndex + 1);
            sgnType.add("*");
            if (sgnIndex.size() == 2) // (...)
            {
                Node ch = parseExpr(cur, beginIndex + 1, endIndex - 1,T);
                cur.expr = ch.expr;
                return cur;
            }
            else
            {
                cur.expr.add(Monomial.one, Rational.one);
                for (int i = 1; i < sgnIndex.size(); i++)
                {
                    Node ch = parseExpr(cur, sgnIndex.elementAt(i - 1) + 1, sgnIndex.elementAt(i) - 1,T);
                    if (sgnType.elementAt(i - 1).equals("*"))
                        cur.expr.multiplyByPolynomial(ch.expr);
                    else if (ch.expr.isConstant() && ch.expr.terms.containsKey(Monomial.one))
                        cur.expr.multiplyByValue(Rational.inverse(ch.expr.terms.get(Monomial.one)));
                    else
                        throw new Exception("Divison by variable is not possible @" + beginIndex + "-" + endIndex);
                }
                return cur;
            }
        }
    }


    public static boolean isNumeric(String s)
    {
        for (int i = 0; i < s.length(); i++)
            if (!Character.isDigit(s.charAt(i)) && s.charAt(i) != '.')
                return false;
        return true;
    }


    public static int getTokenCount()
    {
        return tokens.size();
    }

    public static String getToken(int x)
    {
        return tokens.elementAt(x);
    }

    public static void readTokens(String program) throws Exception
    {
        tokens.clear();
        String extraSpace = "";
        for (int i = 0; i < program.length(); i++)
        {
            char c = program.charAt(i);
            if (c == '.' || Character.isAlphabetic(c) || Character.isDigit(c) || c == '_')
                extraSpace += c;
            else
            {
                extraSpace += " ";
                extraSpace += c;
                extraSpace += " ";
            }
        }

        Scanner scanner = new Scanner(extraSpace);
        while (scanner.hasNext())
        {
            String s = scanner.next();
            if (s.equals("="))
            {
                if (tokens.size() == 0)
                    throw new Exception("program cannot start with =");
                String last = tokens.lastElement();
                if (last.equals(":") || last.equals(">") || last.equals("<") || last.equals("=") || last.equals("!"))
                {
                    tokens.removeElementAt(getTokenCount() - 1);
                    last += s;
                    tokens.add(last);
                }
                else
                    tokens.add(s);
            }
            else
                tokens.add(s);
        }

    }

    public static void readFile(String fileName) throws Exception
    {
        File file = new File(fileName);
        Scanner in = new Scanner(file);

        String program = "";
        while (in.hasNextLine())
        {
            String s = in.nextLine();
            if (s.contains("//"))
                s = s.substring(0, s.indexOf("//"));
            if (s.contains("AT("))
            {
                int ind = s.indexOf("AT(");
                int openPar = 0, endOfAT = -1;
                for (int i = 0; i < s.length(); i++)
                {
                    if (s.charAt(i) == '(')
                        openPar++;
                    else if (s.charAt(i) == ')')
                    {
                        openPar--;
                        if (openPar == 0)
                        {
                            endOfAT = i;
                            break;
                        }
                    }
                }
                s = s.substring(0, ind) + s.substring(endOfAT + 1);
            }

            program += s + " ";
        }
        readTokens(program);
    }

    public static TransitionSystem parse(String filename, String TS_name) throws Exception
    {
        Parser.restart();
        readFile(filename);
        TransitionSystem T = new TransitionSystem(TS_name);
        parseProg(0, Parser.getTokenCount() - 1,T);
        allVars.addAll(T.allVars);
        return T;
    }
}