package reduce_test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


public class Tester<T1,T2,T3,T4> {
  public boolean test(T1 key, T2[] solutionArray, ReducerC<T1, T2> reducer) throws IOException, InterruptedException{

    Context<T3,T4> oc1 = new Context<T3,T4>();
    Context<T3,T4> oc2 = new Context<T3,T4>();


    ArrayList<T2> list_one = new ArrayList<T2>(Arrays.asList(solutionArray));
    ArrayList<T2> list_two = new ArrayList<T2>(Arrays.asList(solutionArray));

    while (list_one.equals(solutionArray)) {
    	Collections.shuffle(list_one);
    }
    while (list_two.equals(list_one)) {
    	Collections.shuffle(list_two);
    }
    System.out.println("Input1: " + list_one);
    System.out.println("Input2: " + list_two);

    System.out.print("Output1: ");
    reducer.reduce(key, list_one, oc1);
    System.out.println(oc1.getValueList());
    System.out.print("Output2: ");
    reducer.reduce(key, list_two, oc2);
    System.out.println(oc2.getValueList());

    if (oc1.equals(oc2)) {
      System.out.println("RESULT: The reducer is commutative");
    } else {
      System.out.println("RESULT: The reducer is NOT commutative");
    }    

    return oc1.equals(oc2);
  } 
  
  public boolean test(T1 key, T2[] solutionArray, ReducerO<T1, T2, T3, T4> reducer) throws IOException, InterruptedException{

    OutputCollector<T3,T4> oc1 = new OutputCollector<T3,T4>();
    OutputCollector<T3,T4> oc2 = new OutputCollector<T3,T4>();

    ArrayList<T2> list_one = new ArrayList<T2>(Arrays.asList(solutionArray));
    ArrayList<T2> list_two = new ArrayList<T2>(Arrays.asList(solutionArray));

    while (list_one.equals(solutionArray)) {
    	Collections.shuffle(list_one);
    }
    while (list_two.equals(list_one)) {
    	Collections.shuffle(list_two);
    }

    System.out.println("Input1: " + list_one);
    System.out.println("Input2: " + list_two);

    System.out.print("Output1: ");
    reducer.reduce(key, list_one.iterator(), oc1, null);
    System.out.println(oc1.getValueList());
    System.out.print("Output2: ");
    reducer.reduce(key,list_two.iterator(), oc2, null);
    System.out.println(oc2.getValueList());

    if (oc1.equals(oc2)) {
      System.out.println("RESULT: The reducer is commutative");
    } else {
      System.out.println("RESULT: The reducer is NOT commutative");
    }    

    return oc1.equals(oc2);
  }   


}
