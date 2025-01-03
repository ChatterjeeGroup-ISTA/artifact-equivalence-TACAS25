import java.util.Vector;

public class Distribution
{
    String type;
    Vector<Rational> params;
    Polynomial flip;

    Distribution(String type, Vector<Rational> params, Polynomial flip)
    {
        this.type = type;
        this.params = params;
        this.flip=flip;
    }



    Polynomial moment(int n) throws Exception
    {
        if(n==0)
            return new Polynomial(Rational.one);
        if(type.equals("uniform"))
        {
            Rational a = params.elementAt(0), b =params.elementAt(1);
            Rational num = Rational.power(b,n+1);
            num = Rational.minus(num,Rational.power(a,n+1));

            Rational div = Rational.minus(b,a);
            div = Rational.mul(div,new Rational(n+1,1));

            return new Polynomial(Rational.div(num,div));
        }
        else if(type.equals("discrete"))
        {
            int mid = params.size()/2;
            Rational ret = new Rational(0,1);
            for(int i=0;i<mid;i++)
            {
                Rational prob = params.elementAt(i);
                Rational val =  params.elementAt(i+mid);
                val=Rational.power(val,n);
                val=Rational.mul(val,prob);
                ret=Rational.add(ret,val);
            }
            return new Polynomial(ret);
        }
        else if(type.equals("normal"))
        {
            Rational mu = params.elementAt(0), sigma2=Rational.power(params.elementAt(1),2);
            if(n==1)
                return new Polynomial(params.elementAt(0));
            else if(n==2)
            {
                Rational ret = sigma2.deepCopy();
                ret=Rational.add(ret,Rational.power(mu,2));
                return new Polynomial(ret);
            }
            else if(n==3)
            {
                Rational ret = Rational.power(mu,3);     //mu^3
                Rational tmp = Rational.mul(mu,sigma2); //mu*sigma^2
                ret = Rational.add(ret,Rational.mul(tmp,new Rational(3,1)));   // ret = mu^3 + 3*mu*sigma2
                return new Polynomial(ret);
            }
            else if(n==4)
            {

                Rational t1 = Rational.power(mu,4);     // mu^4
                Rational t2 = Rational.mul(new Rational(6,1), Rational.mul(Rational.power(mu,2),sigma2)); // 6*mu^2*sigma^2
                Rational t3 = Rational.mul(new Rational(3,1),Rational.power(sigma2,2));         // 3*sigma^4
                return new Polynomial(Rational.add(Rational.add(t1,t2),t3));
            }
            else if(n==5)
            {
                Rational t1 = Rational.power(mu,5);     // mu^5
                Rational t2 = Rational.mul(new Rational(10,1),Rational.mul(Rational.power(mu,3),sigma2)); // 10 mu^3 sigma^2
                Rational t3 = Rational.mul(new Rational(15,1), Rational.mul(mu,Rational.power(sigma2,2))); // 15 mu sigma^4
                return new Polynomial(Rational.add(Rational.add(t1,t2),t3));
            }
            return null;
        }
        else if(type.equals("unifInt"))
        {
            Rational res = Rational.zero.deepCopy();
            Rational start = params.elementAt(0),end=params.elementAt(1);
            if(start.denominator!=1 || end.denominator!=1 || start.numerator>end.numerator)
                throw new Exception("unifInt(a,b) must have integer parameters where a<=b!");
            Rational prob = new Rational(1,end.numerator-start.numerator+1);
            for(long i=start.numerator;i<=end.numerator;i++)
            {
                Rational term =  Rational.power(new Rational(i, 1), n);
                term=Rational.mul(term,prob);
                res=Rational.add(res,term);
            }
            return new Polynomial(res);
        }
        else if(type.equals("flip"))
        {
            return flip;
        }
        else if(type.equals("beta"))
        {
            Polynomial ret = new Polynomial(Rational.one);
            Rational alpha = params.elementAt(0),beta=params.elementAt(1);
            for(int i=0;i<n;i++)
            {
                Rational num = Rational.add(alpha, new Rational(i,1));
                Rational den = Rational.add(Rational.add(alpha,beta),new Rational(i,1));
                ret.multiplyByValue(Rational.div(num,den));
            }
            return ret;
        }
        return null;
    }
    Rational lower_bound()
    {
        if(type.equals("uniform"))
            return params.elementAt(0);
        if(type.equals("discrete"))
        {
            int mid = params.size()/2;
            Rational min = params.elementAt(mid);
            for(int i=0;i<mid;i++)
                min=Rational.min(min,params.elementAt(mid+i));
            return min;
        }
        if(type.equals("unifInt"))
            return params.elementAt(0);
        if(type.equals("flip"))
            return Rational.zero;
        if(type.equals("beta"))
            return Rational.zero;
        return null;
    }

    Rational upper_bound()
    {
        if(type.equals("uniform"))
            return params.elementAt(1);
        if(type.equals("discrete"))
        {
            int mid = params.size()/2;
            Rational max = params.elementAt(mid);
            for(int i=0;i<mid;i++)
                max=Rational.max(max,params.elementAt(mid+i));
            return max;
        }
        if(type.equals("unifInt"))
            return params.elementAt(1);
        if(type.equals("flip"))
            return Rational.one;
        if(type.equals("beta"))
            return Rational.one;
        return null;
    }

    public String toString()
    {
        StringBuilder ret = new StringBuilder(type + "(");
        if(type.equals("flip"))
            ret.append(flip.toNormalString()).append(")");
        else
        {
            for (Rational p : params)
                ret.append(p.toNormalString()).append(",");
            ret = new StringBuilder(ret.substring(0, ret.length() - 1) + ")");
        }
        return ret.toString();
    }

    public static boolean isDistribution(String x)
    {
        return x.equals("uniform") || x.equals("discrete") || x.equals("normal") || x.equals("unifInt") || x.equals("flip") || x.equals("beta");
    }

}
