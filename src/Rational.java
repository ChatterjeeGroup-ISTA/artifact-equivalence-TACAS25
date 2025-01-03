public class Rational implements Comparable<Rational>
{
    public static final Rational one = new Rational(1, 1), zero = new Rational(0, 1);

    long numerator;
    long denominator;

    public long gcd(long a, long b)
    {
        if (b == 0) return a;
        return gcd(b, a % b);
    }

    Rational(long numerator, long denominator)
    {
        if (numerator == 0)
        {
            this.numerator = 0;
            this.denominator = 1;
            return;
        }
        if (denominator < 0)
        {
            denominator *= -1;
            numerator *= -1;
        }
        long g = gcd(numerator, denominator);
        this.numerator = numerator / g;
        this.denominator = denominator / g;
        normalize();
    }

    public static Rational negate(Rational a)
    {
        return new Rational(-a.numerator, a.denominator).normalize();
    }

    public static Rational inverse(Rational a) throws Exception
    {
        if (a.numerator == 0)
            throw new Exception("getting inverse of " + a + " which is not defined");
        return new Rational(a.denominator, a.numerator);
    }

    public static Rational add(Rational a, Rational b)
    {
        return new Rational(a.numerator * b.denominator + b.numerator * a.denominator, a.denominator * b.denominator).normalize();
    }


    public static Rational minus(Rational a, Rational b)
    {
        return add(a, negate(b));
    }

    public static Rational mul(Rational a, Rational b) throws Exception
    {
        long num = Math.multiplyExact(a.numerator,b.numerator);
        long den = Math.multiplyExact(a.denominator,b.denominator);

        return new Rational(num,den).normalize();
    }

    public static Rational div(Rational a, Rational b) throws Exception
    {
        return mul(a, inverse(b)).normalize();
    }

    public boolean equals(Rational a)
    {
        return (a.numerator == numerator && a.denominator == denominator);
    }

    public boolean isNonNegative()
    {
        return (numerator >= 0);
    }

    @Override
    public int compareTo(Rational a)
    {
        long ret = (numerator * a.denominator - a.numerator * denominator);
        if(ret<0)
            return -1;
        else if(ret==0)
            return 0;
        else
            return 1;
    }


    public String toNormalString()
    {
        normalize();
        if (denominator == 1)
            return "" + numerator;
        return "(" + numerator + "/" + denominator + ")";
    }

    public Rational normalize()
    {
        if (denominator < 0)
        {
            numerator *= -1;
            denominator *= -1;
        }
        return this;
    }

    public String toString()
    {
        normalize();
        String num="", den = "" + denominator;
        if (numerator < 0)
            num = "(- " + (-numerator) + ")";
        else
            num = "" + numerator;
        if (denominator == 1)
            return num;
        return "(/ " + num + " " + denominator + ")";
    }

    public Rational deepCopy()
    {
        return new Rational(numerator,denominator);
    }

    public static Rational parseRational(String input)
    {
        if(!input.contains("/"))
            return new Rational(Long.parseLong(input),1);
        else
        {
            String num = input.substring(0,input.indexOf("/"));
            String den = input.substring(input.indexOf("/")+1);
            return new Rational(Long.parseLong(num),Long.parseLong(den));
        }
    }

    public static long intPow(long a, long b)
    {
        if(b==0)
            return 1;
        long res = intPow(a,b/2);
        res*=res;
        if(b%2==1)
            res*=a;
        return res;
    }
    public static Rational power(Rational a,long n)
    {
        return new Rational(intPow(a.numerator,n),intPow(a.denominator,n));
    }

    public static Rational min(Rational a, Rational b)
    {
        if(a.compareTo(b)>0)
            return b;
        else
            return a;
    }

    public static Rational max(Rational a, Rational b)
    {
        if(a.compareTo(b)>0)
            return a;
        else
            return b;
    }

    public double toDouble()
    {
        return (double)numerator/(double)denominator;
    }
}