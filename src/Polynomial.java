import gurobi.GRBLinExpr;
import gurobi.GRBQuadExpr;
import gurobi.GRBVar;

import java.util.*;

public class Polynomial
{
    Map<Monomial, Rational> terms;

    Polynomial()
    {
        terms = new TreeMap<>();
    }

    Polynomial(Rational c)
    {
        terms = new TreeMap<>();
        terms.put(Monomial.one, c);
    }

    Polynomial(String var)
    {
        terms = new TreeMap<>();
        terms.put(new Monomial(var), Rational.one);
    }

    Polynomial(String var, Rational c)
    {
        terms = new TreeMap<>();
        terms.put(new Monomial(var), c);
    }

    Polynomial(Monomial m)
    {
        terms = new TreeMap<>();
        terms.put(m, Rational.one);
    }


    Polynomial(Monomial m, Rational c)
    {
        terms = new TreeMap<>();
        terms.put(m, c);
    }

//    Polynomial(Set<String> vars)  //generates the polynomial \sum c_i x_i
//    {
//        terms = new TreeMap<>();
//        for (String var : vars)
//        {
//            Monomial m = new Monomial();
//            m.addVar("c_" + RankingFunction.cCount, 1);
//            RankingFunction.cCount++;
//            m.addVar(var, 1);
//            add(m, Rational.one);
//        }
//    }

//    Polynomial(Set<String> vars, int degree)
//    {
//        terms = new TreeMap<>();
//        Set<Monomial> monomials = Monomial.getAllMonomials(vars, degree);
//        for (Monomial m : monomials)
//        {
//            m.addVar("c_" + RankingFunction.cCount, 1);
//            RankingFunction.cCount++;
//            add(m, Rational.one);
//        }
//    }

    void add(Monomial m, Rational c)
    {
        if (terms.containsKey(m))
            terms.put(m, Rational.add(terms.get(m), c));
        else
            terms.put(m, c);
    }

    void add(Polynomial poly)
    {
        Polynomial tmp = poly.deepCopy();
        for (Monomial term : tmp.terms.keySet())
            add(term, tmp.terms.get(term));
    }

    void minus(Polynomial poly)
    {
        add(poly.negate());
    }

    public void multiplyByValue(Rational val) throws Exception
    {
        for (Monomial term : terms.keySet())
            terms.put(term, Rational.mul(terms.get(term), val));
    }

    public void multiplyByMonomial(Monomial m)
    {
        Map<Monomial, Rational> tmp = new HashMap<>();
        for (Monomial term : terms.keySet())
            tmp.put(Monomial.mul(term, m), terms.get(term));
        terms = tmp;
    }

    public void multiplyByPolynomial(Polynomial poly) throws Exception
    {
        Polynomial res = new Polynomial();
        for (Monomial m : poly.terms.keySet())
            for (Monomial n : this.terms.keySet())
                res.add(Monomial.mul(m, n), Rational.mul(poly.terms.get(m), this.terms.get(n)));
        terms = res.terms;
    }

    boolean isConstant()
    {
        removeZeros();
        return (terms.size() <= 1 && (terms.size() != 1 || terms.containsKey(Monomial.one)));
    }

    void removeZeros()
    {
        Vector<Monomial> allTerms = new Vector<>(terms.keySet());
        for (Monomial term : allTerms)
            if (terms.get(term).equals(Rational.zero))
                terms.remove(term);
    }

    Polynomial deepCopy()
    {
        removeZeros();
        Polynomial ret = new Polynomial();
        for (Monomial term : terms.keySet())
            ret.add(term.deepCopy(), terms.get(term));
        return ret;
    }

    public void replaceVarsWithDistr(String var, Distribution update) throws Exception
    {
        Polynomial ret = new Polynomial();
        for(Monomial term: terms.keySet())
        {
            Monomial mon=term.deepCopy();
            Rational coef=terms.get(term);
            int p=mon.getPower(var);
            mon=mon.removeOneVar(var);
            Polynomial tmp = Polynomial.mul(new Polynomial(mon),update.moment(p));
            tmp.multiplyByValue(coef);
            ret.add(tmp);
//                coef=Rational.mul(coef, upd.moment(p));
        }
        terms=ret.terms;
    }

    public void replaceVarsWithValue(Map<String,Rational> vals) throws Exception
    {
        for(String var:vals.keySet())
        {
            Polynomial val = new Polynomial(vals.get(var));
            replaceVarWithPoly(var,val);
        }
        removeZeros();
    }

    void replaceVarWithPoly(String var, Polynomial poly) throws Exception
    {
        Vector<Monomial> allTerms = new Vector<>(terms.keySet());
        for (Monomial term : allTerms)
            if (term.containsVar(var))
            {
                Rational coef = terms.get(term);
                Monomial m = term.removeOneVar(var);

                Polynomial left = new Polynomial(m);
                Polynomial right = poly.deepCopy();

                for (int i = 1; i < term.getPower(var); i++)
                    right.multiplyByPolynomial(poly);
                left.multiplyByPolynomial(right);
                left.multiplyByValue(coef);

                terms.remove(term);

                add(left);
            }
    }

    public void replaceVarsWithPoly(Vector<String> vars, Vector<Polynomial> upd) throws Exception
    {
        Map<Monomial, Rational> primed = new TreeMap<>();
        for(Monomial m: terms.keySet())
            primed.put(m.primed(vars), terms.get(m));
        terms = primed;
        for(int i=0;i<vars.size();i++)
            replaceVarWithPoly(vars.elementAt(i)+"'", upd.elementAt(i));

    }

    public void makePrimed(Vector<String> vars) throws Exception
    {
        Map<Monomial, Rational> primed = new TreeMap<>();
        for(Monomial m: terms.keySet())
            primed.put(m.primed(vars), terms.get(m));
        terms = primed;
    }
    public Set<Monomial> getProgramVariableMonomials()
    {
        Set<Monomial> ret= new TreeSet<>();
        for(Monomial m:terms.keySet())
            ret.add(m.getProgramVarsPart());
        return ret;
    }

    public Polynomial getCoef(Monomial m)
    {
        Polynomial ret=new Polynomial();
        for(Monomial monomial:terms.keySet())
        {
            if(monomial.getProgramVarsPart().equals(m))
                ret.add(monomial.unknownPart(),terms.get(monomial));
        }
        return ret;
    }

    public boolean equals(Polynomial poly)
    {
        for (Monomial m : terms.keySet())
            if (!poly.terms.containsKey(m) || !poly.terms.get(m).equals(terms.get(m)))
                return false;
        for(Monomial m: poly.terms.keySet())
            if(!terms.containsKey(m) || !terms.get(m).equals(poly.terms.get(m)))
                return false;
        return true;
    }

    public Polynomial negate()
    {
        Polynomial ret = new Polynomial();
        for (Monomial term : terms.keySet())
            ret.add(term.deepCopy(), Rational.negate(terms.get(term)));
        return ret;
    }

    public  static  Polynomial mul(Polynomial left,Polynomial right) throws Exception
    {
        Polynomial ret=left.deepCopy();
        ret.multiplyByPolynomial(right);
        return ret;
    }

    public Polynomial removeEps()
    {
        Monomial toRemove=null;
        for(Monomial m:terms.keySet())
            if(m.containsVar("__eps"))
                toRemove=m;
        if(toRemove==null)
            return deepCopy();
        Polynomial ret = deepCopy();
        ret.terms.remove(toRemove);
        return ret;
    }

    public boolean containsEps()
    {
        for(Monomial m:terms.keySet())
            if(m.containsVar("__eps"))
                return true;
        return false;
    }

    public int degree()
    {
        int ret =0;
        for(Monomial m: terms.keySet())
            ret = Integer.max(ret,m.degree());
        return ret;
    }

    public GRBLinExpr translate_to_GRBLin(Map<String, GRBVar> grbVarMap) throws Exception
    {
        if(degree()>1)
            throw new Exception("Polynomial "+toNormalString()+" cannot be translated to GRBLinear");
        GRBLinExpr ret = new GRBLinExpr();
        for(Monomial m:terms.keySet())
        {
            if(m.degree()==1)
            {
                String var = m.getListVars().firstElement();
                GRBVar grbVar =  grbVarMap.get(var);
                double coef=terms.get(m).toDouble();
                ret.addTerm(coef,grbVar);
            }
            else
            {
                double coef = terms.get(m).toDouble();
                ret.addConstant(coef);
            }
        }
        return ret;
    }

    public GRBQuadExpr translate_to_GRBQuad(Map<String,GRBVar> grbVarMap) throws Exception
    {
        if(degree()>2)
            throw new Exception("Polynomial "+toNormalString()+" cannot be translated to GRBQuad");
        GRBQuadExpr ret =new GRBQuadExpr();
        for(Monomial m:terms.keySet())
        {
            if(m.degree()==1)
            {
                String var = m.getListVars().firstElement();
                GRBVar grbVar =  grbVarMap.get(var);
                double coef=terms.get(m).toDouble();
                ret.addTerm(coef,grbVar);
            }
            else if(m.degree()==2)
            {
                String var1 = m.getListVars().elementAt(0),var2=m.getListVars().elementAt(1);
                GRBVar grbVar1 = grbVarMap.get(var1), grbVar2 = grbVarMap.get(var2);
                double coef = terms.get(m).toDouble();
                ret.addTerm(coef,grbVar1,grbVar2);
            }
        }
        return ret;
    }

    public boolean contains_template_var()
    {
        for(Monomial m:terms.keySet())
            if(m.contains_template_var())
                return true;
        return false;
    }



    public String toSMT()
    {
        String ret = "";
        if (terms.isEmpty())
            return "0";
        Vector<Monomial> monomials = new Vector<>(terms.keySet());
        if (monomials.size() == 1)
            return "(* " + terms.get(monomials.firstElement()) +" "+ monomials.firstElement().toString()+")";
        ret = "(+ ";
        for (Monomial m : monomials)
            ret += "(* "+ terms.get(m)+ " " +m.toString()+") ";
        ret += ")";
        return ret;
    }

    public Polynomial normalize() throws Exception
    {
        Polynomial ret = new Polynomial();
        ret.terms=this.terms;
        for(Monomial mon:terms.keySet())
            ret.multiplyByValue(new Rational(terms.get(mon).denominator,1));
        return ret;
    }

    public Rational normalizing_factor() throws Exception
    {
        Rational ret = new Rational(1,1);
        for(Monomial mon:terms.keySet())
            ret=Rational.mul(ret,new Rational(terms.get(mon).denominator,1));
        return ret;
    }

    public String toNormalString()
    {
        StringBuilder ret = new StringBuilder();
        if (terms.isEmpty())
            return "0";
        Vector<Monomial> monomials = new Vector<>(terms.keySet());
        for (Monomial m : monomials)
        {
            if(!ret.toString().equals(""))
            {
                if(terms.get(m).isNonNegative())
                    ret.append("  +  ");
                else
                    ret.append("  -  ");
            }
            else if(!terms.get(m).isNonNegative())
                ret.append("-  ");
            if (terms.get(m).isNonNegative())
                ret.append(terms.get(m).toNormalString());
            else
                ret.append(Rational.negate(terms.get(m)).toNormalString());
            if(!m.equals(Monomial.one))
                ret.append(" * ").append(m.toNormalString());
        }
        if(ret.toString().startsWith("-"))
            ret.insert(0,"0 ");
        return ret.toString();
    }
}