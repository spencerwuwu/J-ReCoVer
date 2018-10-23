// https://searchcode.com/api/result/107417361/

package com.trevorstevens.javasat;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.NoSuchElementException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Arrays;
import static java.lang.Math.abs;

/**
 * The Class Formula.
 *
 * @author Trevor Stevens
 * @author Brian Schreiber
 */
public class Formula {

    private final Deque<Boolean> booleanStack;
    private final Deque<HashObject> hashObjectStack;
    private final double rankArray[][];
    private final Map<Integer, Double> powerMap;
    private Clause[] clauseList;
    private final Map<Integer, HashObject> hashMap;

    private HashObject hashObj;
    private int numVariables,numClauses;
    private int shift;
    private boolean clauseSizeZeroResult;
    private boolean justBackTracked;

    /**
     * Instantiates a new formula.
     *
     * @param fileName the file name
     */
    Formula(final String fileName) {
        importCNF(fileName);
        rankArray = new double[2][numVariables];
        booleanStack = new ArrayDeque<Boolean>(numVariables);
        hashObjectStack = new ArrayDeque<HashObject>(numVariables);
        powerMap = new HashMap<Integer, Double>();
        hashMap = new HashMap<Integer, HashObject>(1 + (int) (numVariables / 0.75));
        populateHashMap();
        rankVariables();
    }

    /**
     * Import cnf file and store clauses in
     * clauseList.
     *
     * @param fileName the file name
     */
    private void importCNF(final String fileName) {
        Scanner sc = null;
        try {
            sc = new Scanner(new BufferedInputStream(new FileInputStream(fileName)), "UTF-8");
        } catch (FileNotFoundException e) {
            System.exit(1);
        }

        if(sc.findWithinHorizon("p cnf", 0) == null)
            System.exit(2);

        numVariables = sc.nextInt();
        numClauses = sc.nextInt();
        clauseList = new Clause[numClauses];

        int start = 0;
        int clause = 0;
        int end = 0;
        int[] intBuffer = new int[numVariables*numClauses];
        for(int i = 0; sc.hasNextInt(); i++) {
            intBuffer[i] = sc.nextInt();
            if(intBuffer[i] == 0){
                clauseList[clause] = new Clause(Arrays.copyOfRange(intBuffer, start, end));
                start = i + 1;
                end = start;
                clause++;
            } else {
                end++;
            }
        }

        sc.close();

    }

    /**
     * Populate the hash map.
     */
    private void populateHashMap() {
        Clause clauseAtI;
        HashObject hashTmp;
        int clauseVar,clauseVarKey,i,j,clauseAtISize;
        for (i = 0; i < numClauses; i++) {
            clauseAtI = clauseList[i];
            clauseAtISize = clauseAtI.size();
            for (j = 0; j < clauseAtISize; j++) {
                clauseVar = clauseAtI.get(j);
                clauseVarKey = abs(clauseVar); //abs of variable for key
                hashTmp = hashMap.get(clauseVarKey);
                if (hashTmp == null) {
                    hashTmp = new HashObject(clauseVarKey);
                    hashMap.put(clauseVarKey, hashTmp);
                }
                if (clauseVar > 0) {
                    hashTmp.addClausePos(clauseAtI);
                } else {
                    hashTmp.addClauseNeg(clauseAtI);
                }
            }
        }
    }

    /**
     * Rank the variables for the
     * first time.
     */
    private void rankVariables() {
        int i;

        for (i = 1; i <= numVariables; i++) {     // Creates List
            rankArray[0][i - 1] = i;            // Stores the Variable in the first column
            // rankArray[1][i - 1] = 0.0f;//sum;          // Stores the Ranking in the second column
        }
    }

    /**
     * Re-rank variables.
     */
    public void reRankVariables() {
        int maxValueKey = 0;

        double sum = 0.0d;
        double maxValue = 0.0d;

        int pSize, nSize, bigger, s, i;
        for (i = shift; i < numVariables; i++) {
            hashObj = hashMap.get((int) rankArray[0][i]);
            if (hashObj == null) {
                // rankArray[1][i] = 0; //not sure if good/bad
                continue;
            }
            pSize = hashObj.posSize();
            nSize = hashObj.negSize();
            bigger = nSize < pSize ? pSize : nSize;
            for (s = 0; s < bigger; s++) {
                if (s < pSize) {
                    sum += getCachedPow(hashObj.getP(s).size());
                }
                if (s < nSize) {
                    sum += getCachedPow(hashObj.getN(s).size());
                }
            }

            if(maxValue < sum ){ //finds if sum is the largest so far
                maxValueKey = i;
                maxValue = sum;
            }

            rankArray[1][i] = sum;          // Stores the Ranking in the second column
            sum = 0.0d;
        }

        //Switch the maxValueKey to the shift position
        double currentMaxKey = rankArray[0][shift];
        double currentMaxRank = rankArray[1][shift];

        rankArray[0][shift] = rankArray[0][maxValueKey];
        rankArray[1][shift] = rankArray[1][maxValueKey];

        rankArray[0][maxValueKey] = currentMaxKey;
        rankArray[1][maxValueKey] = currentMaxRank;
    }

    private double getCachedPow(final int exponent){
        Double tmp = powerMap.get(exponent);
        if(tmp == null) {
            tmp = Math.pow(2, (-exponent));
             powerMap.put(exponent, tmp);
        }
        return tmp;
    }

    private int unitPropCheck(){
        int unitVar = 0;
        int unitKey = lengthOneCheck();
        if(unitKey != 0){
            int absUnitKey = abs(unitKey);
            unitVar = (unitKey < 0 ) ? -((int) rankArray[0][absUnitKey] ): (int) rankArray[0][absUnitKey];
            shiftToUnit(absUnitKey);
        }
        return unitVar;
    }

    private void shiftToUnit(final int absUnitKey){
        double currentMaxKey = rankArray[0][shift];
        double currentMaxRank = rankArray[1][shift];

        rankArray[0][shift] = rankArray[0][absUnitKey];
        rankArray[1][shift] = rankArray[1][absUnitKey];

        rankArray[0][absUnitKey] = currentMaxKey;
        rankArray[1][absUnitKey] = currentMaxRank;
    }

    /**
     * Forward tracks down the tree.
     */
   public void forwardTrack() {
       Clause clause;
       HashObject nextVarObj;
       boolean booleanValue;
       int var, absKey, actualSize, j, i, opsitListSize, listSize, varNeg, key;

       var = unitPropCheck();
       if (var != 0) {
           absKey = abs(var);
       } else {
           reRankVariables();
           var = (int) rankArray[0][shift];
           absKey = abs(var);
       }
       
       nextVarObj =  hashMap.get(absKey);
       hashMap.remove(absKey);
       
       /*
        * This statement determines whether
        * to branch true or false by checking if it has
        * just back tracked or not.
        */
       booleanValue = (!justBackTracked && var > 0);
       var = absKey; //abs(var); // always positive: p or n
       varNeg = booleanValue ? -var : var;//var * -1 : var; //flip for negitive: pos * -1 = n : neg * -1 = p
       if (booleanValue) {
           listSize = nextVarObj.posSize();
           opsitListSize = nextVarObj.negSize();
       } else {
           listSize = nextVarObj.negSize();
           opsitListSize = nextVarObj.posSize();
       }

       for (i = 0; i < listSize; i++) {
           clause = (booleanValue) ? nextVarObj.getP(i) : nextVarObj.getN(i);
           actualSize = clause.actualSize();
           for (j = 0; j < actualSize; j++) {
               key = clause.get(j);
               if (key != 0 && (absKey = abs(key)) != var) {
                   hashObj = (HashObject) hashMap.get(absKey);
                   if (hashObj != null) {
                       hashObj.removeClause(clause);
                   }
               }
           }
       }

       /*
        * This loop removes all varNeg occurrences
        * from all clauses in clauseList.
        */
       for (i = 0; i < opsitListSize; i++) {
          ( (booleanValue) ? nextVarObj.getN(i) : nextVarObj.getP(i) ).removeVar(varNeg);
       }

       hashObjectStack.addFirst(nextVarObj);
       booleanStack.addFirst(booleanValue);
       justBackTracked = false;
       shift++;
   }

    /**
     * Back tracks up the tree.
     */
    public void backTrack() throws NoSuchElementException {
        //  Reduce runtime overhead && clean up code
        while (!booleanStack.removeFirst()) {
            shift--;
            rePopulate((int) rankArray[0][shift], hashObjectStack.removeFirst(), false);
        }
        shift--;
        rePopulate((int) rankArray[0][shift], hashObjectStack.removeFirst(), true);
        justBackTracked = true;
    }

    /**
     * Re-populate the hashMap
     *
     * @param key the key
     * @param rePopObj the object being repopulated
     * @param varSetTo the boolean value of the object
     */
    private void rePopulate(final int key,final HashObject rePopObj,final boolean varSetTo) {
        Clause clause;
        int var, negkey, rVarSize, rClauseSize, i, j, actualSize;
        if (varSetTo) {
            negkey = -key;
            rVarSize = rePopObj.negSize();
            rClauseSize = rePopObj.posSize();
            for (i = 0; i < rClauseSize; i++) {
                clause = rePopObj.getP(i);
                actualSize = clause.actualSize();
                for (j = 0; j < actualSize; j++) {
                    var = clause.get(j);
                    if (var != 0) {
                        hashObj = hashMap.get(abs(var));
                        if (hashObj != null){
                          if(var > 0) {
                              hashObj.addClausePos(clause);
                          } else {
                              hashObj.addClauseNeg(clause);
                          }
                        }
                    }
                }
            }
            for (i = 0; i < rVarSize; i++) {
                rePopObj.getN(i).addVar(negkey);
            }
            hashMap.put(key, rePopObj);
        } else {
            negkey = key;
            rVarSize = rePopObj.posSize();
            rClauseSize = rePopObj.negSize();
            for (i = 0; i < rClauseSize; i++) {
                clause = rePopObj.getN(i);
                actualSize = clause.actualSize();
                for (j = 0; j < actualSize; j++) {
                    var = clause.get(j);
                    if (var != 0) {
                        hashObj = hashMap.get(abs(var));
                        if (hashObj != null){
                           if( var > 0) {
                             hashObj.addClausePos(clause);
                           } else {
                             hashObj.addClauseNeg(clause);
                           }
                        }
                    }
                }
            }
            for (i = 0; i < rVarSize; i++) {
                rePopObj.getP(i).addVar(negkey);
            }
            hashMap.put(key, rePopObj);
        }
    }

    /**
     * Check for if a clause is size zero.
     *
     * @return true, if successful
     */
    public boolean clauseSizeZero() {
        final int length = clauseList.length;
        clauseSizeZeroResult = false;
        for (int i = 0; i < length; i++) {
            if ( clauseList[i].size() == 0) {
                clauseSizeZeroResult = true;
                break;
            }
        }
        return clauseSizeZeroResult;
    }

    /**
     * Returns result of last method call to clauseSizeZero
     * @return clauseSizeZero last result
     * @see clauseSizeZero()
     */

    public boolean getCachedClauseSizeZeroResult() {
        return clauseSizeZeroResult;
    }

    /**
     * Check if there it is a valid solution.
     *
     * @return true, if successful
     */
    public boolean validSolution() {
        return ( !clauseSizeZero() && (hashMap.isEmpty() || allEmptyKeyMap()));
    }

    /**
     * Check to see if all keys in hashMap are
     * empty.
     *
     * @return true, if successful
     */
    private boolean allEmptyKeyMap() {
        int i;
        HashObject tmp;
        for (i = shift; i < numVariables; i++) {
            tmp = hashMap.get((int) rankArray[0][i]);
            if (tmp != null && (!tmp.posEmpty() || !tmp.negEmpty())) {
                return false;
            }
        }
        return true;
    }
    /**
     * Finds and returns highest ranked unit variable
     * @return
     */
    private int lengthOneCheck() {
        HashObject tmp;
        int tmpVar, size, i, k;

        for (k = shift; k < numVariables; k++) {
            tmp = hashMap.get((int) rankArray[0][k]);
            if (tmp != null) {
                size = tmp.posSize();
                for (i = 0; i < size; i++) {
                    tmpVar = tmp.getP(i).lengthOne();
                    if (tmpVar != 0) {
                        return k;
                    }
                }
                size = tmp.negSize();
                for (i = 0; i < size; i++) {
                    tmpVar = tmp.getN(i).lengthOne();
                    if (tmpVar != 0) {
                        return -k;
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Print Solution Set.
     */
    public void printSolution() {
        final StringBuilder sb = new StringBuilder();
        final String space = " ";
        while (!hashObjectStack.isEmpty()) {
            if (booleanStack.removeFirst()) {
                sb.append(hashObjectStack.removeFirst().getVariableNumber());
            } else {
                // If false negate variable
                sb.append(-hashObjectStack.removeFirst().getVariableNumber());
            }
            sb.append(space);
        }
        System.out.println(sb.toString());
    }
}

