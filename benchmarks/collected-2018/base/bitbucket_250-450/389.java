// https://searchcode.com/api/result/121612091/

package calc;

import java.io.Serializable;
import java.util.ArrayList;
import kayles.Configuration;
import utils.Pair;

/**
 * The Optimizer class contains the datastructures and the methods to perform
 * the optimisation. The static object staticInstance holds all runtime data.
 * 
 * @author sjoerd
 */
public class Optimizer implements Serializable {
    public static Optimizer staticInstance = new Optimizer();
    
    /* reference to the root configuration */
    public Configuration root;
    
    /* a list of all configurations that count for the analysis */
    public ArrayList<Configuration> allconfigs = new ArrayList<Configuration>();
    
    /* a list of all configurations that have been evaluated */
    public ArrayList<Configuration> evaluatedConfigs = new ArrayList<Configuration>();
    
    /* a list of configurations that are binding the latest result */
    public ArrayList<Configuration> tightConfigs = new ArrayList<Configuration>();
    
    /* a list of external constraints */
    public ArrayList<Constraint> externalConstraints = new ArrayList<Constraint>();
    
    
    /**
     * External constraints can be added to the system of constraints.
     * @param c the constraint to be added
     */
    public void addxternalConstraint(Constraint c){
        externalConstraints.add(c);
    }

    /**
     * Add a configuration to the system
     * @param c the configuration to add
     */
    public void addConfig(Configuration c){
        allconfigs.add(c);
    }
    
 
    /**
     * Isomorphism checking is included to reduce the number of configurations.
     * all pairs of configurations are checked. From all isomorphic sets one is kept
     * and all others are set to the 'isomorphic' state.
     */
    public void isoCheck(){
        ArrayList<Configuration> toberemoved = new ArrayList<Configuration>();
        for(Configuration c1:allconfigs){
            if(c1.iso==null && c1.getChildCount()==0){
                for(Configuration c2:allconfigs){
                    if(c1!=c2 && c2.iso==null && c2.getChildCount()==0){
                        if(c1.findIsomorphism(c2)){
                            toberemoved.add(c1);
                        }
                    }
                }
            }
        }
        allconfigs.removeAll(toberemoved);
    }
    
    /**
     * Call run() to run the golden ration search for the current set of configurations.
     * @return a pair (x,alpha) for the optimal location
     */
    public Pair<Double> run(){
        // tolerance is the sqrt of the smallest double-value
        Pair<Double> res = GRS(0.0, 0.61803399, 1.0, 0.00000000023283064365); 
        System.out.println("GRS returned: "+res.t1);
        System.out.println("for alpha: "+res.t2);
        findTightConstraints(res.t2);
        return res;
    }
    
    /**
     * Shorthand for case 1
     * @return (4,1-)
     */
    public Constraint getCase1(){
        Constraint c = new Constraint();
        c.addTerm(4, 0);
        c.addTerm(1, -1);
        return c;
    }
    
    /**
     * Shorthand for case 2
     * @return (3+,1-)
     */
    public Constraint getCase2(){
        Constraint c = new Constraint();
        c.addTerm(3, 1);
        c.addTerm(1, -1);
        return c;
    }
    
    /**
     * Shorthand for Case 3.1
     * @return (2+,2+,2)
     */
    public Constraint getCase31(){
        Constraint c = new Constraint();
        c.addTerm(2, 1);
        c.addTerm(2, 1);
        c.addTerm(2, 0);
        return c;
    }
    
    /**
     * Shorthand for case 3.2
     * @return (3+2,3+,3+,3,3)
     */
    public Constraint getCase32(){    
        Constraint c = new Constraint();
        c.addTerm(3, 2);
        c.addTerm(3, 1);
        c.addTerm(3, 1);
        c.addTerm(3, 0);
        c.addTerm(3, 0);
        return c;
    }
    
    /**
     * Shorthand for  case 3.3
     * @return (3+2,3+2,3+,3,3)
     */
    public Constraint getCase33(){
        Constraint c = new Constraint();
        c.addTerm(3, 2);
        c.addTerm(3, 2);
        c.addTerm(3, 1);
        c.addTerm(3, 0);
        c.addTerm(3, 0);
        return c;
    }
    
    /**
     * Shorthand for case 3.4
     * @return (4+,4+,4+,4+,4+2,4,4,4)
     */
    public Constraint getCase34(){
        Constraint c = new Constraint();
        c.addTerm(4, 1);
        c.addTerm(4, 1);
        c.addTerm(4, 1);
        c.addTerm(4, 1);
        c.addTerm(4, 2);
        c.addTerm(4, 0);
        c.addTerm(4, 0);
        c.addTerm(4, 0);
        return c;
    }
    
    /**
     * Shorthand for case 3.5
     * @return (4+,1-)
     */
    public Constraint getCase35(){
        Constraint c = new Constraint();
        c.addTerm(4, 1);
        c.addTerm(1, -1);
        return c;
    }
    
    /**
     * Shorthand for case 3.6
     * @return (2+,3,4,5,4-)
     */
    public Constraint getCase36(){
        Constraint c = new Constraint();
        c.addTerm(2, 1);
        c.addTerm(3, 0);
        c.addTerm(4, 0);
        c.addTerm(5, 0);
        c.addTerm(4, -1);
        return c;
    }
    
    /**
     * Shorthand for case 4.1
     * @return (3+,1-)
     */
    public Constraint getCase41(){
        Constraint c = new Constraint();
        c.addTerm(3, 1);
        c.addTerm(1, -1);
        return c;
    }
    
    /**
     * Shorthand for case 4.2
     * @return (3+,1-)
     */
    public Constraint getCase42(){
        Constraint c = new Constraint();
        c.addTerm(3, 1);
        c.addTerm(1, -1);
        return c;
    }
    
    /**
     * Shorthand for case 4.3
     * @return (2+,2+,2+)
     */
    public Constraint getCase43(){
        Constraint c = new Constraint();
        c.addTerm(2, 1);
        c.addTerm(2, 1);
        c.addTerm(2, 1);
        return c;
    }
    
    /**
     * Shorthand for case 5
     * @return (1+2,1)
     */
    public Constraint getCase5(){
        Constraint c = new Constraint();
        c.addTerm(1, 2);
        c.addTerm(1, 0);
        return c;
    }
    
    /**
     * Shorthand for case 6.1
     * @return (2+,2+)
     */
    public Constraint getCase61(){
        Constraint c = new Constraint();
        c.addTerm(2, 1);
        c.addTerm(2, 1);
        return c;
    }
    
    /**
     * Shorthand for Case 6.2
     * @return (1+,1+)
     */
    public Constraint getCase62(){
        Constraint c = new Constraint();
        c.addTerm(1, 1);
        c.addTerm(1, 1);
        return c;
    }
        
    /**
     * Golden Ratio Search. Shamelessly stolen from "Numerical Recipies"
     * ported to Java
     * @param xa lower value for x
     * @param xb intermediate value for x(optimally at the golden ration between xa and xc)
     * @param xa upper value for x
     * @param tol tollerance for the approximation, optimally the square root of the minimum value that the datatype (double in our case) can have
     * @return a pair (x,) that is optimal. The result is guaranteed to be correct rather than optimal
     */
    final double r = 0.61803399;
    public Pair<Double> GRS(double xa,double xb,double xc,double tol){
        evaluatedConfigs = new ArrayList<Configuration>(allconfigs);
        double f1,f2,x0,x1,x2,x3;
        x0=xa;
        x3=xc;
        if(Math.abs(xc-xb)>Math.abs(xb-xa)){
            x1=xb;
            x2=xb+(1-r)*(xc-xb);
        }else{
            x2=xb;
            x1=xb-(1-r)*(xb-xa);
        }
        f1=findSmallestSatisfyingX(x1);
        f2=findSmallestSatisfyingX(x2);
        
        while(Math.abs(x3-x0)> tol*(x1+x2)){
            if(f2<f1){
                x0=x1;
                x1=x2;
                x2=r*x1+(1-r)*x3;
                f1=f2;
                f2=findSmallestSatisfyingX(x2);
            }else{
                x3=x2;
                x2=x1;
                x1=r*x2+(1-r)*x0;
                f2=f1;
                f1=findSmallestSatisfyingX(x1);
            }
        }
        double alpha = (f1<f2)? x1:x2;
        double x = (f1<f2)?f1:f2;
        return new Pair(x,alpha);
    }
    
    /**
     * subroutine, that determines for a given alpha, the smallest statisfying x
     * this is implemented using a simple bisection method.
     * @param alpha the currect value of 
     * @return the smallest value of x that satisfies all constraints
     */
    private double findSmallestSatisfyingX(double alpha){        
        double hi = 2.0;
        double lo = 1.0;
        double xmid=0;
        
        for(int i=0;i<32;i++){
            xmid = (hi+lo)/2;
            if(checkPoint(xmid, alpha)){
                hi=xmid;
            }else{
                lo=xmid;
            }
        }
        return hi;
    }
    
    /**
     * finds the constraints that are binding in the current situation (or very close)
     * @param alpha the optimal alpha (found with GRS())
     */
    private void findTightConstraints(double alpha){
        //find x again
        double hi = 2.0;
        double lo = 1.0;
        double xmid=0;
        
        for(int i=0;i<16;i++){// not too high, otherwise, some tight constraints don't get found
            xmid = (hi+lo)/2;
            if(checkPoint(xmid, alpha)){
                hi=xmid;
            }else{
                lo=xmid;
            }
            
            if(checkPoint(lo, alpha)){
                System.err.println("lo is good!!! that is bad!");
            }
            if(hi==lo){
                System.err.println("hi==lo!!!!!! ("+hi+")");
                break;
            }
        }
        // reset the list
        tightConfigs.clear();
        
        // find the constraints that were broken
        for(Configuration c:allconfigs){
            if(!c.getConstraint().check(lo,alpha)){
                tightConfigs.add(c);
            }
        }
        for(Constraint c:externalConstraints){
            if(!c.check(lo, alpha)){
                System.out.println("tight external constraint: "+c.getVector());
            }
        }
    }
    
     /**
     * litle procedure that checks whether a given point in <x,alpha> space
     * satisfies all constraints or not
     * @param x the x-coord to check
     * @param alpha the -coord to check
     * @return true if all constraints are met, false otherwise
     */
    private boolean checkPoint(double x,double alpha){
        for(Configuration c:allconfigs){
            if(!c.getConstraint().check(x, alpha)){
                return false;
            }
        }
        for(Constraint c:externalConstraints){
            if(!c.check(x, alpha)){
                return false;
            }
        }
        return true;
    }
}




