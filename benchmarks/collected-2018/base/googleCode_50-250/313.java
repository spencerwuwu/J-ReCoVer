// https://searchcode.com/api/result/14165507/

/**
 * 
 */
package strain.demo;

import static java.lang.Math.max;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import strain.domain.InconsistentDomainException;


public class Solver<T>{

        private static int depth = 0;
        private static int maxDepth = 0;
        
        public static int getDepth(){
            return depth;
        }
        
        public static int getMaxDepth(){
            return maxDepth;
        }
        
        public Comparator<DiscreteVariable<T>> getDomainSizeComparator(){
            return new Comparator<DiscreteVariable<T>>(){
                public int compare(final DiscreteVariable<T> v1, final DiscreteVariable<T> v2) {
                    return v1.getDomain().size() - v2.getDomain().size();
                }
            };
        }
        // keep a prioritized list of the variables whose domains have been reduced...
        // prefer to apply more constraints to highly constrained variables, and those with small domains,
        // emptying them as quickly as possible.  (fail fast)
        
        Map<String, DiscreteVariable<T>> variables = new HashMap<String, DiscreteVariable<T>>();
        List<Constraint<T>> constraints = new ArrayList<Constraint<T>>();
        
        public void add(final DiscreteVariable<T> variable) {
            if (this.variables.containsKey(variable.getName())){
                throw new IllegalArgumentException("attempted to add variable with name " + variable.getName() + " to solver, already present");
            }
            
            this.variables.put(variable.getName(), variable);
        }
        
        public void add(final Constraint<T> constraint) {
            Constraint<T> solverCopy = constraint.solverCopy(this);
            this.constraints.add(solverCopy);
        }
        
        public DiscreteVariable<T> getVariable(final String name){
            if (!this.variables.containsKey(name)){
                throw new IllegalArgumentException("tried to get variable " + name + " from solver, not present");
            }
                
            return this.variables.get(name);
        }
        
        private boolean domainReductionPass() throws InconsistentDomainException{
            boolean result = false;
            for(Constraint<T> constraint: this.constraints){
                result = result || constraint.apply();
            }
            return result;
        }
        
        private boolean allBound(){
            for(DiscreteVariable<T> variable: this.variables.values()){
                if (!variable.isBound()){
                    return false;
                }
            }
            return true;
        }
        
        private Solver<T> copy(){
            Solver<T> result = new Solver<T>();
            for(DiscreteVariable<T> v: this.variables.values()){
                result.add(v.copy());
            }
            for(Constraint<T> constraint: this.constraints){
                result.add(constraint);
            }
            return result;
        }
        
        public boolean solve() {
            ++depth;
            maxDepth = max(maxDepth, depth);
            try{
                // reduce current state in place (may as well before choices are made)?
                while(domainReductionPass()){
//                    System.out.println("reduction pass...");
                }
            } catch (InconsistentDomainException ide){
                --depth;
                return false;
            }
            if (allBound()){
                --depth;
                return true;
            }
            
            // pick a variable (heuristically one with many constraints and/or a small domain)
            // make a choice that reduces that variables domain (reserve complementary choices for later)
            // propagate the results of that domain reduction to other variables
            //   pick a constraint (based on...?)
            //   propagate that constraint
            // continue propagating constraints until no further domain reductions occur
            // repeat...
            List<DiscreteVariable<T>> orderedVariables = new ArrayList<DiscreteVariable<T>>(this.variables.values());
            Collections.sort(orderedVariables, getDomainSizeComparator());
            for(DiscreteVariable<T> variable: orderedVariables){
                if (!variable.isBound()){
                    while(true){
                        T value = variable.getDomain().iterator().next();
                        Solver<T> solver = copy();
                        try {
                            solver.getVariable(variable.getName()).bind(value);
                            if (solver.solve()){
                                for(DiscreteVariable<T> v: this.variables.values()){
                                    v.bind(solver.getVariable(v.getName()).getBoundValue());
                                }
                                --depth;
                                return true;
                            }
                            variable.remove(value);
                        } catch (InconsistentDomainException e) {
                            return false;
                        }
                    }
                }
            }
            --depth;
            return false;
        }
        
        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            
            for(DiscreteVariable<T> variable: this.variables.values()){
                sb.append(variable.toString()).append("\n");
            }
            for(Constraint<T> constraint: this.constraints){
                sb.append(constraint.toString()).append("\n");
            }
            
            return sb.toString();
        }
        
    }
