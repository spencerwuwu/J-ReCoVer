// https://searchcode.com/api/result/11878357/

package org.rust.lang;

import java.util.ArrayList;
import java.util.List;


public class Node
{

   //these are the key attributes of a node
   private Type type; //NAMEDLIST (a function), LIST, ITEM;
   private String value;
   private List<Node> children;
   private Node parent;
   private Elem elem;
   
   private char [] src;//original src prog
   private int srcStart, srcEnd; //index of start and end chars of this node

   public enum Type
   {
      NAMEDLIST, LIST, ITEM;
   }
   
   //callback for searching/filtering
   public interface IMatcher
   {
      /**
       * @return true if nodeToMatch is the node that you want
       */
      public boolean matches(Node nodeToMatch);
   }
   
   public Node(Type type, String value, Node parent)
   {
      this.type = type;
      
      if(type == Type.ITEM || type == Type.NAMEDLIST)
         Assert.verify(value!=null && !value.isEmpty());
      
      this.value = value;
      this.parent = parent;
      
   }
   
   public Node(Type type, String value, Node parent, char [] src, int start, int end)
   {  
      this.type = type;
      
      if(type == Type.ITEM || type == Type.NAMEDLIST)
         Assert.verify(value!=null && !value.isEmpty());
      
      this.value = value;
      this.parent = parent;

      this.src = src;
      this.srcStart = start;
      this.srcEnd = end;
   }

   public Elem getElem()
   {
      return elem;
   }

   public void setElem(Elem elem)
   {
      this.elem = elem;
   }


   private void addChild(Node node)
   {
      if(children == null)
         children = new ArrayList<Node>();
      
      children.add(node);
   }
 
   public String getValue()
   {
     return value;  
   }
   
   public Type getType()
   {
      return this.type;
   }
   
   public int parse()
   {
      
      /*
       * A program is a tree of nodes. Each Node has:
       *
       *    a type
       *    a value (can be null)
       *    a list of children (can be null)
       *
       * type can be:
       * 
       *    namedList
       *    list
       *    item
       * 
       * e.g.
       * 
       * sum(1 mul(5 2) (7 8))
       * 
       * 
       * which in tree form is:
       * 
       *             type        value
       *             ----        -----   
       * sum         namedList   sum   
       *    1        item        1
       *    mul      namedList   mul
       *       5     item        5                   
       *       2     item        2
       *    list     list        
       *       7     item        7
       *       8     item        8
       *       
       * 
       *  Everything else (e.g. higher level functionality) is defined in terms of these nodes.
       *  This is the same as any system like maths or language where one thing is defined
       *  hierarchically in terms of another or one component points to another component.
       *  The low level representation above using names and lists/items is close to that of a machine
       *  which uses addresses and memory or more specifically a stack, heap and registers (all memory)
       *  and some primitive operations (mul, sum, jmp) on the values in that memory.
       *  Note that its also really the same as a command line  where you have command arg1 arg2
       *  Its easy to flatten the tree above out to a stack (or list) and then have each node
       *  in the list contain a pointer/address of its first child and each child have a pointer
       *  back to its parent. Once you have a tree flattener that does this you can componentize its 
       *  functionality so that you can then think at the next level up and so on: Composition/trees.
       *  The key thing is that an interface to a component/node/level should be small, simple and consistent. 
       *  This makes it more reliable and more flexible to compose with at the next level up.
       *            
       */
      
 
      int p = srcStart;
      while(p <= srcEnd)
      {  
         if(Character.isWhitespace(src[p]) || isClosing(src[p]))
         {
            //skip any whitespace  or closing bracket to get to start of next node
            while(p <= srcEnd && (Character.isWhitespace(src[p]) || isClosing(src[p])))
               p++;
         }
         else if(isOpening(src[p]))
         {
            //Its a list or a namedList
            //If its an '(' then find its matching ')'
            //create a new child of its contained text and parse it. 
            //But first try and see if this child has a name before the opening '('
            String name = null;
            int nameStart = p - 1;
            if(nameStart >= 0 && isIdentifier(src[nameStart]))
            {
               //go to start of name
               while(nameStart >= srcStart && isIdentifier(src[nameStart]))
                  nameStart--;
               name = new String(src, nameStart + 1, p - (nameStart+1));
               srcStart = nameStart + 1;
               
            }
            
            int closing = findClosing(p); 
            if(closing == 0)
               Assert.fail("Cannot find closing bracket for expression starting at: " + new String(src, p, (srcEnd - p) + 1));
            
            if(closing - p > 1)
            {
               Type type = name==null? Type.LIST: Type.NAMEDLIST;
               Node child = new Node(type, name, this, src, p + 1, closing - 1);
               addChild(child);
               p = child.parse();
               int i = 1; ///for debugging
            }
            else
               p++;
         }
         else
         {
            //Its an item or a namedList
            //If its an identifier then read it until you get to next whitespace or '(' or ')'
            //and make a child of it
            int childStart = p;
            while(p <= srcEnd && isIdentifier(src[p]))
            {
               p++;
            }
          
            if(isOpening(src[p]))
            {
               //we have come to the end of an identifier but its a '('
               //e.g. we are at the '('   in "sum(2 3)"
               //in this case we just skip it as the second branch of this if statement will make
               //a named list of it
            }
            else
            {
               //this is an item so construct it
               int childEnd = p - 1;
               String value = new String(src, childStart, (childEnd - childStart) + 1);
               Node child = new Node(Type.ITEM, value, this, src, childStart, childEnd);
               addChild(child);
               p++;                  
            }                             
         }
      }
      
      return p; //at the end of the while p == srcEnd. 
      

   }
   
   public void infixToPrefix()
   {
      /*
         * Apart from the syntax above there will only be one piece of syntactic sugar, namely:
         * Repeated instances of the infix operator (+ - *  = > < == ) will be translated to 
         * prefix/namedList/function form.
         *  
         *  So (a + b + c) will be translated to +(a b c)
         *  myFunc(a + b + c) will be translated to myFunc( +(a b c) )
         *
         *  But (a + b * c) won't compile. The user should write ((a + b) * c)
         *  
         *  
      */         
      if(type == Type.LIST || type == Type.NAMEDLIST)
      {
         if( (children.size() > 2) && (children.size() % 2 != 0))
         {
            Operator operator = null;
            for(int i = 1; i <= children.size() - 2; i += 2)
            {
               Node child = children.get(i);
               if(child.type == Type.ITEM)
               {
                  Operator op = Operator.parse(child.getValue());
                  if( op!= null )
                  {
                     if(operator == null)
                     {
                        //first operator we have come across
                        operator = op;
                     }
                     else
                     {
                        if(op.type() != operator.type())
                        {
                           Assert.fail("Mixing of operators without brackets is not supported in: " + this.getOriginalSrc());
                        }
                     }
                  }
               }
            }
            if(operator != null)
            {
               //every second token was an operator
               //make a new list of children
                List<Node> newchildren = new ArrayList<Node>();
                for(int i = 0; i < children.size(); i += 2)
                {
                   newchildren.add(children.get(i));
                }
                
                this.type = Type.NAMEDLIST;
                this.value = operator.toString();
                this.children = newchildren;
            }
         }            
         for (Node child : children)
            child.infixToPrefix();
      }         
   }

   public void removeRedundantNodes()
   {
      //e.g.  reduce (3 * (((3 * ((3 + 2))))))
      //to (3 * (3 * (3 + 2)))
      //If a list (parentList) contains only one child which is itself a list (childList)
      //then point the parentList at childList's children
      if(this.children != null && this.children.size() == 1)
      {
         Node onlyChild = children.get(0);
         if(onlyChild.type == Type.LIST)
         {
            this.children = onlyChild.children;
            for (Node child : onlyChild.children)
               child.parent = this;
         }
      }      
      if(this.children  != null)
      {
         for (Node child : this.children)
            child.removeRedundantNodes();
      }
   }
   
   public Node evaluate(Node args)
   {
      switch (this.type)
      {
      case ITEM:
         return this;

      case LIST:
      {
         Node retVal = null;
         for (Node child : children)
         {
            retVal = child.evaluate(args);
         }
         // return value of last child call here
         // like the default return
         return retVal;
      }

      case NAMEDLIST:
      {
         // its a namedList i.e a call to a function or an assignment

         // SrcNode function = getFunction(value);

         // return function.evaluate(args);

         return evaluateCall();
      }

      default:
      {
         Assert.fail();
         return null;
      }

      }
   }
   
   Node evaluateCall()
   {
      /*
      if(value.equals("set"))
      {
         
      }
      */         
      //this node is a call evaluate it
      //it value will be the fn name e.g. "sum" and its children will be the arguments

      
      Operator op = Operator.parse(value);
      if(op!=null)
      {
         if(op.isArithmetic())
            return applyOperator(op);
         
         if(op.type() == Operator.Type.ASSIGN)
         {
            applyAssign();
            return null;
         }
            
      }

      Assert.fail("Function not defined " + this.getOriginalSrc());
      return null;
   }

   void applyAssign()
   {
      
      // in the statement (i = 1) which is =(i 1) there are only
      //two args, the Identifier (i) and  the value (1)
      Assert.verify(children.size() == 2);
      
      Node nodeName = children.get(0);
      Node nodeValue = children.get(1).evaluate(null);
      
      Assert.verify(nodeName.getType() == Type.ITEM);
            
      //for the moment we only deal with vars - remove this assert when
      //we are doing functions (which will be named lists)
      Assert.verify(nodeValue.getType() == Type.ITEM);
      
      
      //see if this var already exists
      Ref ref = findReference(nodeName.value);
      
      if(ref == null)
      {
         //its the first time we have seen the statement i = 1
         //so this is a new pointer
         ref = new Ref(this, nodeName.value);
      }
    
      this.elem = ref;
      
      Elem elem = null;
      
      if(nodeValue.value.equals("block"))
      {
         elem = createElemFromBlockDef(nodeValue);
      }
      else if(!isPrimitive(nodeValue.value))
      {
         //This is an assignment of one reference to another (i=j)
         //In this case we want to look for the the definition of j and point to its element
         Ref original = findReference(nodeValue.value);
         
         if(original == null)
            Assert.fail("Undefined variable " + nodeValue.value + " in: " + getOriginalSrc());
         
         elem = original.getElem();
      }
      else
      {
         elem = createElemFromPrimitive(nodeValue.value);
      }
     
      ref.setElem(elem);
   }
   
   
   private Elem createElemFromBlockDef(Node blockDef)
   {  
      //TODO create a Block class (extends Elem) and parse the Node into it (or just parse 
      //the blocks signature and store it in a hash map)
      return null;
      
      
   }

   private boolean isInteger(String input)  
   {  
      try  
      {  
         Integer.parseInt(input);  
         return true;  
      }  
      catch(Exception e)  
      {  
         //TODO: not very efficient to use an exception
         //for this but it will do for the moment
         return false;  
      }  
   }  

   /**
    * @return true if input is a primitive (integer, string etc e.g. 1 or "abc" or 'a' or 1.34)   
    */
   private boolean isPrimitive(String input)  
   {
      //for the moment the only values we work with are integers so:
      return isInteger(input);
   }  
   
   
   /**
    * Create an appropriate element from a value where the value is e.g. 
    * (integer, string etc e.g. 1 or "abc" or 'a' or 1.34)
    */
   private Elem createElemFromPrimitive(String primitive)
   {
      //for the moment we just do ints
      return new IntVal(Integer.parseInt(primitive));
   }

   
   Node applyOperator(Operator op)
   {  
      //note that caller is only passed so that we can get the original debug info
      
      int accumulator = 0;
      if(op.type() == Operator.Type.MUL)
         accumulator = 1;
      
      for(Node arg: children)
      {
         
         Node result = arg.evaluate(null);
         
         if(result.type != Type.ITEM)
            Assert.fail("Bad argument: " + arg.getOriginalSrc() +  " passed to " + op.toString() +  ". Expected: " + Type.ITEM + " received: " + result.type + " in " + this.getOriginalSrc());
         
         int val = 0;
         
         // if the child is non-numeric then it must be a pointer/reference/variable name
         if(!isInteger(result.getValue()))
         {
            final String varName = result.getValue();
            
            Ref ref = findReference(varName);
            
            if(ref == null)
            {
               Assert.fail("Failed to look up value for variable " + varName + " in " + this.getOriginalSrc());
            }
            
            if(ref.getElem() != null && ref.getElem().getType() == Elem.Type.INTVAL)
            {
               IntVal intVal = (IntVal)ref.getElem();
               val = intVal.getVal();
            }
            else
            {
               Assert.fail("Failed to look up value for variable " + varName + " in " + this.getOriginalSrc());
            }
         }
         else
         {
            val = Integer.parseInt(result.getValue());
         }
         
         try
         {
            
            switch(op.type())
            {
            case SUM:
               accumulator += val;
               break;
            case MUL:
               accumulator *= val;
               break;       
            default:
               Assert.verify(false, "Unsupported operator " + op.toString());
            }
         }
         catch(NumberFormatException ex)
         {
            Assert.verify(false, "Non integer argument: " + arg.getOriginalSrc() +  " passed to sum in " + this.getOriginalSrc());
         }
      }
      
      return new Node(Type.ITEM, String.valueOf(accumulator), null);
   }

   int applyOperator(int lhs, int rhs,  char operator)
   {
     switch(operator)
     {
     case '+':
        return lhs + rhs;
        
     case '*':
        return lhs * rhs;  
      
      default:
         throw new RuntimeException("unsupported operator: " + operator);
     }
   }

   
   /**
    * @return true if ch is a not whitespace or brackets
    * i.e. its comething like a, b, 9, < whatever
    */
   boolean isIdentifier(char ch)
   {
      return (!Character.isWhitespace(ch)) && (!isOpening(ch) && (!isClosing(ch)));
   }

   
   /**
    * @param return true if char is opening bracket
    * i.e. (, { or [
    * @return
    */
   boolean isOpening(char ch)
   {
      return ch == '(' || ch == '{' || ch == '[';
   }
   
   /**
    * @param return true if char is closing bracket
    * i.e. ), } or ]
    * @return
    */
   boolean isClosing(char ch)
   {
      return ch == ')' || ch == '}' || ch == ']';
   }

   
   public void print()
   {
      print(null);
   }
   public void print(String tabs)
   {
      if(tabs == null)
         tabs = new String();
      
      
      if(this.type == Type.ITEM)
         Rst.print("->" + tabs + value);
      else
      {
         //its a list or a named list. Print its title and then print the children
         String title = (this.type == Type.NAMEDLIST? value: "list" );

         Rst.print("->" + tabs + title);

         tabs += "   ";
         for (Node child : children)
         {               
            child.print(tabs);
         }
         
      }
   }
   


   public int findClosing(int opening)
   {
      int p = opening + 1;
      int openings = 1;
      while(p <= srcEnd)
      {
         if(isOpening(src[p]))
            openings++;
         
         if(isClosing(src[p]))
         {
            openings--;
            if(openings == 0)
               return p;
         }
         p++;
      }   
      return 0;
   }

   //search upwards in scope for a pointer
   //with the specified name
   private Ref findReference(final String name)
   {
      //search for the named pointer and get its value
      Node var = searchUpwards(new IMatcher(){
         @Override
         public boolean matches(Node nodeToMatch)
         {
            /*TODO: In the long run we need to get a lot
             * less generic here. We need hashmaps of pointers
             * that we can search quickly. 
            */
            if(nodeToMatch.getElem() != null && nodeToMatch.getElem().getType()== Elem.Type.REFERENCE)
            {
               Ref ref = (Ref)nodeToMatch.getElem();
               if(ref.getName().equals(name))
                  return true;
            }
            return false;
         }});       

      if(var == null)
         return null;
      
      Ref ref = (Ref)var.getElem();
      
      return ref;

   }
   
   /**
    * Search a nodes ascendants and each of their
    * children (but no deeper) for a match 
    * @param matcher
    * @return
    */
   public Node searchUpwards(IMatcher matcher)
   {
      if(this.parent != null)
      {
         if (this.parent.children != null)
         {
            for (Node child : this.parent.children)
            {
               if(matcher.matches(child))
                  return child;
            }
         }
         return parent.searchUpwards(matcher);
      }
      return null;
   }
 
   public String getOriginalSrc()
   {
      return new String(src, srcStart, (srcEnd - srcStart) + 1);
   }
   
   public String getTag()
   {
      return value;
   }
   
   public String toString()
   {
      
      return "type:" + type + " value:" + value; 
   }
   
}


