// https://searchcode.com/api/result/93340907/

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package GetCmdOpt;

import java.util.HashMap;
import java.util.Map;

/**
 * My attempt to reduce/remove the verbosity of my "parsecmdline" classes
 * The program will take strings and interpret the results, placing the values
 * in an attribute that can be accessed by extending classes
 * @author bickhart
 */
public abstract class GetCmdOpt {
    /**
     * An attribute that stores the "mode" for programs that have more than one mode
     */
    protected String mode = null;
    /**
     * The attribute that stores the key-value pairs for command line input options
     */
    protected Map<String, String> values = new HashMap<>();
    /**
     * The usage statement for the program
     */
    protected String usage = null;
    
    
    
    /**
     * This method strips the command line argument string array down to its basic elements
     * to search for requested options.
     * @param args The argument String array from the main routine
     * @param keys A String of command line options to search for in the main args array. Must conform
     * to a two character code, with the first character indicating the flag to search for, and the second
     * character indicating the type of argument it is (either a value option (":") or a boolean flag
     * ("|"). Example: "A|B:" searches for -A as a boolean flag and -B as an option with a value.
     * @throws Exception
     */
    public void ProcessCmdString(String[] args, String keys) throws Exception{
        // keys will be in a two character format like so:
        // A: or A|
        // A: <- tag will contain a string
        // A| <- tag is a boolean flag
        if(keys.length() % 2 != 0)
            throw new Exception("[GETOPT] Keys do not follow two character convention!");
        String[] tags = keys.split("(?!^)");
        for(int i = 0; i < tags.length; i += 2){
            String k = tags[i];
            String format = tags[i+1];
            if(!format.equals(":") && !format.equals("|"))
                throw new Exception("[GETOPT] The key value must be a \":\" or a \"|\"!");
            
            // Set boolean flags to false by default
            if(format.equals("|"))
                this.values.put(k, "false");
            
            for(int x = 0; x < args.length; x++){
                if(args[x].equals("-" + k)){
                    if(format.equals(":"))
                        this.populateValueString(k, args[x+1]);
                    else
                        this.populateBooleanFlag(k);
                }
            }
        }
    }
    /**
     * This method strips the command line argument string array down to its basic elements
     * to search for requested options. This is the option to select if the first commandline
     * argument is meant to contain a mode designation
     * @param args The argument String array from the main routine
     * @param keys A String of command line options to search for in the main args array. Must conform
     * to a two character code, with the first character indicating the flag to search for, and the second
     * character indicating the type of argument it is (either a value option (":") or a boolean flag
     * ("|"). Example: "A|B:" searches for -A as a boolean flag and -B as an option with a value.
     * @param mode Just a placeholder to enable polymorphism of the method. Just enter Boolean.true
     * @throws Exception
     */
    public void ProcessCmdString(String[] args, String keys, boolean mode) throws Exception{
        // This is a modification that strips the first argument and sets it as the mode of the program
        String[] newArgs = new String[args.length -1];
        System.arraycopy(args, 1, newArgs, 0, args.length - 2);
        this.mode = args[0];
        this.ProcessCmdString(newArgs, keys);
    }
    
    
    /**
     * This method associates existing command line keys with input string values.
     * This is done simply for programmer readability when calling the "GetValue" method
     * @param k A concatenated string of key values that will be replaced by larger strings
     * @param a The larger strings to replace the aforementioned "k" keys. WARNING: must be the same size as
     * the number of characters in the "k" string!
     * @throws Exception 
     */
    public void AssociateKeyWithLargerString(String k, String ... a) throws Exception{
        String[] keys = k.split("(?!^)");
        if(keys.length != a.length)
            throw new Exception("[GETOPT] Have not assigned appropriate number of keys!");
        
        for(int x = 0; x < keys.length; x++){
            if(this.values.containsKey(keys[x]))
                this.values.put(a[x], this.values.get(keys[x]));
        }
    }
    /**
     * This is a simple check to ensure that the user has input the mandatory
     * command line keys into the command line arguments
     * @param keys a string containing the concatenated keys the programmer wants to check. ie. "ACGT" checks
     * to see if the "-A", "-C", "-G" and "-T" flags/values are set
     * @return "true" if mandatory values are all set. "false" if a value is missing
     */
    public boolean SimpleParityCheck(String keys){
        String[] tags = keys.split("(?!^)");
        for(String k : tags){
            if(!this.values.containsKey(k))
                return false;
        }
        return true;
    }
    private void populateValueString(String k, String value){
        this.values.put(k, value);
    }
    private void populateBooleanFlag(String k){
        this.values.put(k, "true");
    }
    
    /*
     * Getters
     */
    
    /**
     * Returns the usage statement if one has been entered by the programmer
     * @return the usage string if set by the programmer
     */
    public String GetUsage(){
        return this.usage;
    }
    
    /**
     * Returns a key if it is in the hash
     * @param k The key to search for in the command line options
     * @return The string representation of the argument input value
     */
    public String GetValue(String k){
        if(this.values.containsKey(k))
            return this.values.get(k);
        else
            return null;
    }
    
    /**
     * Checks to see if an option has been set for this flag
     * @param k cmd line option to check
     * @return "True" if the option exists; "False" if it does not
     */
    public boolean HasOpt(String k){
        return this.values.containsKey(k);
    }
    
    /**
     * This is a simple "setter" for the usage string
     * @param usage The string that will print when the program encounters a "help" flag
     * or has the improper amount of arguments
     */
    public void SetUsageStatement(String usage){
        this.usage = usage;
    }
    
    /**
     * This is an encapsulated way for the programmer to set individual values in 
     * the options hash.
     * @param key Key to set/replace
     * @param value Value to associate
     */
    public void SetValue(String key, String value){
        this.values.put(key, value);
    }
}

