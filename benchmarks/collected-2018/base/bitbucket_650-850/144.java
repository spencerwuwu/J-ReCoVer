// https://searchcode.com/api/result/60266257/

package hsplet.compiler;

//import org.objectweb.asm.KLabel;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import java.util.HashMap;

/**
 *
 * @author Kejardon
 * Intended parser for handling submethods from Compiler.java.
 */
public class SubMethodAdapter extends SSMAdapter {

    private class ArrayList<E> extends java.util.ArrayList<E> {
        public void removeRange(int i, int j) {
            super.removeRange(i, j);
        }
    }
    //Counter for number of subsubmethods created.
    //subsubmethods will be labeled "ssm"+SSMCount
    public static int SSMCount=0;
    private Compiler myCompiler;
    public SubMethodAdapter(Compiler comp, MethodVisitor mv) {
        super(mv);
        myCompiler=comp;
        markList.add(new Mark(Mark.START, 0, 0, 0, null, false));
    }

    public int maxSize=64000;
    public int maxSubSize=64000;
    private class PotentialSSM {
        public int startOpcode;
        public int endOpcode;
        public KLabel startLabel;
        public KLabel endLabel;
        public int maxSize;
        public boolean containsReturn;
        public String otherReturn;
        public PotentialSSM(int s, int e, KLabel sl, KLabel el, int m, boolean c, String S) {
            startOpcode=s;
            endOpcode=e;
            startLabel=sl;
            endLabel=el;
            maxSize=m;
            containsReturn=c;
            otherReturn=S;
            //System.out.println("Potential: "+s+" "+e);
        }
    }
    private class Mark {
        public static final int START=0;
        public static final int ENTRANCE=1;
        public static final int BRANCH=2;
        public static final int LABEL=3;
        int type;
        int stackSize;
        int location;
        int maxSizeSinceLastE;
        KLabel label;
        boolean returnSinceLastE;
        public Mark(int t, int s, int l, int m, KLabel la, boolean r) {
            type=t;
            stackSize=s;
            location=l;
            maxSizeSinceLastE=m;
            label=la;
            returnSinceLastE=r;
            //System.out.println("Mark"+t+": "+l+" "+s);
        }
    }
    private HashMap<KLabel, Mark> labelUsage=new HashMap<KLabel, Mark>();
    private ArrayList<MyOpcode> opcodeList=new ArrayList<MyOpcode>();
    private ArrayList<PotentialSSM> potentialSSMs=new ArrayList<PotentialSSM>();
    private ArrayList<Mark> markList=new ArrayList<Mark>();
    private ArrayList<Integer> stackTypes=new ArrayList<Integer>();
    /*{
        public boolean add(Integer I){System.out.println("Add "+(I.intValue()>baseStackType?getStackTypeDesc(I):I)); return super.add(I);}
    };*/
    //private int stackSize=0;
    private int maxSizeSinceLastE=0;
    private boolean returnSinceLastE=false;
    private boolean waitTillStackClear=false;
    private KLabel lastLabel=null;
    private int lastEStack=-1;
    private Integer NOTYPE=Integer.valueOf(-1);
    private Integer INTEGER=Integer.valueOf(0);
    private Integer DOUBLE=Integer.valueOf(1);
    private Integer BOOLEAN=Integer.valueOf(2);
    private Integer OBJECT=Integer.valueOf(3);
    /*
    private Integer CONTEXT=Integer.valueOf(4);
    private Integer VARIABLE=Integer.valueOf(5);
    private Integer OPERAND=Integer.valueOf(5);
    private Integer LITERAL=Integer.valueOf(6);
    private Integer RUNNABLE=Integer.valueOf(7);
    private Integer JUMPSTMNT=Integer.valueOf(8);
    private Integer FLAGOBJ=Integer.valueOf(10);
    private Integer SCALAR=Integer.valueOf(11);
    private Integer INTSCALAR=Integer.valueOf(12);
    */
    private HashMap<String, Integer> stackTypeMap=new HashMap<String, Integer>();
    private ArrayList<String> stackTypeList=new ArrayList<String>();
    private int baseStackType=4;
    private int nextStackType=baseStackType;
    private Integer getStackTypeInt(String desc) {
        Integer I=stackTypeMap.get(desc);
        if(I==null) {
            I=Integer.valueOf(nextStackType++);
            stackTypeList.add(desc);
            stackTypeMap.put(desc, I);
        }
        return I;
    }
    private String getStackTypeDesc(Integer I) {
        return stackTypeList.get(I.intValue()-baseStackType);
    }

    /* Unused in submethods but exist in outputted code:
     * case NEW:
     * case RETURN:
     * case ASTORE:
     */
    private int getMaxSize(int opcode) {
        switch(opcode) {
            //Handle in own code
            //case TABLESWITCH:
            //case Opcodes.ALOAD:
            case Opcodes.AASTORE:
            case Opcodes.ARETURN:
            case Opcodes.ACONST_NULL:
            case Opcodes.IADD:
            case Opcodes.IMUL:
            case Opcodes.AALOAD:
            case Opcodes.ICONST_M1:
            case Opcodes.POP:
            case Opcodes.SWAP:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.DUP:
                return 1;
            case Opcodes.ILOAD:
            case Opcodes.BIPUSH:
                return 2;
            case Opcodes.ISTORE:
                //after an ISTORE, wait till stack is empty to allow subroutine entrance/exits
                waitTillStackClear=true;
                return 2;
            case Opcodes.ANEWARRAY:
            case Opcodes.LDC:
            case Opcodes.SIPUSH:
            case Opcodes.IFNULL:
            case Opcodes.IINC:
            case Opcodes.IFLE:
            case Opcodes.PUTFIELD:
            case Opcodes.GETFIELD:
            case Opcodes.GETSTATIC:
            case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEVIRTUAL:
                return 3;
            case Opcodes.GOTO:
                return 5;
            case Opcodes.IFNE:
            case Opcodes.IFEQ:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ICMPGE:
                return 8;    //3 + 5 if long branch
            default:
                throw new UnsupportedOperationException("Opcode value for size: "+opcode);
        }
    }
    private void stackDecrease(int decrease) {
        //System.out.print("Decrease "+decrease+" to ");
        while(decrease>0){
            stackTypes.remove(stackTypes.size()-1);
            decrease--;
        }
        //System.out.println(stackTypes.size());
        //stackSize-=decrease;
        int stackSize=stackTypes.size();
        for(int i=markList.size()-1;i>=0&&markList.get(i).stackSize>stackSize;i--) {
            if(markList.get(i).type==Mark.ENTRANCE) {
                Mark removedMark=markList.remove(i);
                maxSizeSinceLastE+=removedMark.maxSizeSinceLastE;
                returnSinceLastE|=removedMark.returnSinceLastE;
                //System.out.println("MarkOutA "+removedMark.type+" "+removedMark.location);
            }
        }
    }
    private void stackIncrease(int increase) {
        //stackSize+=increase;
        while(increase>0){
            stackTypes.add(NOTYPE);
            increase--;
        }
    }
    private void handleStackChange(MyOpcode mo) {
        switch(mo.opcode) {
            //Handle in own code (do nothing)
            //case IINC:
            case Opcodes.ARETURN:
                returnSinceLastE=true;
            case Opcodes.TABLESWITCH:
            case Opcodes.IFNULL:
            case Opcodes.IFLE:
            case Opcodes.IFNE:
            case Opcodes.IFEQ:
            case Opcodes.POP:
            case Opcodes.ISTORE:
                stackDecrease(1);
                break;
            case Opcodes.ANEWARRAY:
                stackDecrease(1);
                stackTypes.add(getStackTypeInt(mo.otherData.toString()));
                break;
            case Opcodes.DUP: {
                Integer type=stackTypes.get(stackTypes.size()-1);
                stackDecrease(1);
                stackTypes.add(type);
                stackTypes.add(type);
                break;
            }
            case Opcodes.GETFIELD: {
                stackDecrease(1);
                String[] data=(String[])mo.otherData;
                if(data[2].endsWith(";")) {
                    stackTypes.add(getStackTypeInt(data[2].substring(data[2].indexOf(")")+1)));
                } else if(data[2].endsWith("I")) {
                    stackTypes.add(INTEGER);
                } else if(data[2].endsWith("Z")) {
                    stackTypes.add(BOOLEAN);
                } else if(data[2].endsWith("D")) {
                    stackTypes.add(DOUBLE);
                } else throw new UnsupportedOperationException("Unknown type for stack: "+data[2]);
                break;
            }
            case Opcodes.IADD:
            case Opcodes.IMUL:
                stackDecrease(2);
                stackTypes.add(INTEGER);
                break;
            case Opcodes.AALOAD:
                stackDecrease(2);
                stackTypes.add(OBJECT);
                break;
            case Opcodes.SWAP: {
                Integer type1=stackTypes.get(stackTypes.size()-1);
                Integer type2=stackTypes.get(stackTypes.size()-2);
                stackDecrease(2);
                stackTypes.add(type1);
                stackTypes.add(type2);
                break;
            }
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ICMPGE:
            case Opcodes.PUTFIELD:
                stackDecrease(2);
                break;
            case Opcodes.AASTORE:
                stackDecrease(3);
                break;
            case Opcodes.ACONST_NULL:
            case Opcodes.ALOAD:
                markEntrance();
                stackTypes.add(OBJECT);
                break;
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.ILOAD:
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                markEntrance();
                stackTypes.add(INTEGER);
                break;
            case Opcodes.LDC: {
                markEntrance();
                if(mo.otherData instanceof Integer) {
                    stackTypes.add(INTEGER);
                } else if(mo.otherData instanceof Double) {
                    stackTypes.add(DOUBLE);
                } else {
                    stackTypes.add(getStackTypeInt(Type.getDescriptor(mo.otherData.getClass())));
                }
                break;
            }
            case Opcodes.GETSTATIC: {
                markEntrance();
                String[] data=(String[])mo.otherData;
                if(data[2].endsWith(";")) {
                    stackTypes.add(getStackTypeInt(data[2].substring(data[2].indexOf(")")+1)));
                } else if(data[2].endsWith("I")) {
                    stackTypes.add(INTEGER);
                } else if(data[2].endsWith("Z")) {
                    stackTypes.add(BOOLEAN);
                } else if(data[2].endsWith("D")) {
                    stackTypes.add(DOUBLE);
                } else throw new UnsupportedOperationException("Unknown type for stack: "+data[2]);
                break;
            }
            case Opcodes.GOTO:
                markEntrance();
                break;
            default:
                throw new UnsupportedOperationException("Opcode value for stack: "+mo.opcode);
        }
        lastLabel=null;
    }
    private void markEntrance() {
        int stackSize=stackTypes.size();
        if(waitTillStackClear) {
            if(stackSize==0)
                waitTillStackClear=false;
            else
                return;
        }
        markList.add(new Mark(Mark.ENTRANCE, stackSize, opcodeList.size(), maxSizeSinceLastE, lastLabel, returnSinceLastE));
        maxSizeSinceLastE=0;
        returnSinceLastE=false;
        lastEStack=stackSize;
    }
    private int commitLargestMethod(int fromHere) {
        PotentialSSM largestFound=null;
        for(int i=potentialSSMs.size()-1;i>=0;i--) {
            PotentialSSM pot=potentialSSMs.get(i);
            if(pot.endOpcode<fromHere) break;
            if(pot.startOpcode<fromHere) continue;
            if((largestFound==null)||(largestFound.maxSize<pot.maxSize))
                largestFound=pot;
        }
        if(largestFound==null) return -1;
        
        //ALOAD_0, INVOKEVIRTUAL. +2 instruction, +4 bytes
        //ALOAD_0, INVOKEVIRTUAL, DUP, IFNULL SKIP, ARETURN, SKIP, POP. +7 instruction, +10 bytes (+5 +6 compared to no return)
        int instructionChange=largestFound.endOpcode-largestFound.startOpcode-2;
        int maxSizeChange=largestFound.maxSize-4;
        if(largestFound.containsReturn) {
            instructionChange-=5;
            maxSizeChange-=6;
        }
        //returning because of instructionChange isn't entirely a valid reason, but I do not expect it to be a necessary thing to deal with.
        if((maxSizeChange<1)||(instructionChange<0)) return -2;
        for(int i=markList.size()-1;i>=0;i--) {
            Mark toFix=markList.get(i);
            if(toFix.location<=largestFound.startOpcode) {
                i++;
                while(i<markList.size()) {
                    Mark next=markList.get(i);
                    if(next.type==Mark.ENTRANCE) {
                        next.maxSizeSinceLastE-=maxSizeChange;
                        if(next.maxSizeSinceLastE<0)
                            throw new RuntimeException("Mark size underflowed from submethod commit.");
                        break;
                    }
                    i++;
                }
                if(i==markList.size()) {
                    maxSizeSinceLastE-=maxSizeChange;
                    if(maxSizeSinceLastE<0)
                        throw new RuntimeException("Method size underflowed from submethod commit.");
                }
                break;
            }
            if(toFix.location<largestFound.endOpcode) {
                //I don't think this will happen but oh well.
                if(toFix.type==Mark.ENTRANCE) {
                    throw new RuntimeException("Unexpected entrance location to be removed. "+toFix.location+" "+toFix.maxSizeSinceLastE+" "+largestFound.startOpcode+" "+largestFound.endOpcode);
                }
                markList.remove(i);
                System.out.println("Removing "+(toFix.label==null?"null":""+toFix.label.myIndex+" "+toFix.label.currentCount+" "+toFix.label.branchesToHere+" "+toFix.label.isMainLabel));
                continue;
            }
            toFix.location-=instructionChange;
        }
        for(int i=potentialSSMs.size()-1;i>=0;i--) {
            PotentialSSM toFix=potentialSSMs.get(i);
            if(toFix==largestFound) {
                potentialSSMs.remove(i);
                break;
            }
            if(toFix.endOpcode>largestFound.startOpcode) {
                toFix.endOpcode-=instructionChange;
                toFix.startOpcode-=instructionChange;
            }
        }

        String methodName="me" + myCompiler.getExtraMVNum();
        String sig=(largestFound.containsReturn)?"()"+Compiler.FODesc:largestFound.otherReturn;
        MethodVisitor newMV = myCompiler.getExtraMV(methodName, sig);
        SSMAdapter adapterMV = new SSMAdapter(newMV, largestFound.startLabel, largestFound.endLabel);
        myCompiler.compileLocalVariables(adapterMV);
        for(MyOpcode op : opcodeList.subList(largestFound.startOpcode, largestFound.endOpcode)) {
            adapterMV.visit(op);
        }
        if(largestFound.endLabel!=null)
            adapterMV.visitLabel(adapterMV.replaceEL);
        //System.out.println(methodName+" "+sig+" "+opcodeList.size());
        if(!largestFound.containsReturn) {
            if(sig.endsWith("V"))
                adapterMV.visitInsn(Opcodes.RETURN);
            else if((sig.endsWith("I"))||(sig.endsWith("Z")))
                adapterMV.visitInsn(Opcodes.IRETURN);
            else if(sig.endsWith("D"))
                adapterMV.visitInsn(Opcodes.DRETURN);
            else if(sig.endsWith(";"))
                adapterMV.visitInsn(Opcodes.ARETURN);
            else
                throw new RuntimeException("Unknown return type from method! "+sig);
            opcodeList.set(largestFound.startOpcode++, new MyOpcode(Opcodes.ALOAD, 0));
            opcodeList.set(largestFound.startOpcode++, new MyOpcode(Opcodes.INVOKEVIRTUAL, new String[]{myCompiler.classIName, methodName, sig}));
        } else {
            adapterMV.visitInsn(Opcodes.ACONST_NULL);
            adapterMV.visitInsn(Opcodes.ARETURN);
            opcodeList.set(largestFound.startOpcode++, new MyOpcode(Opcodes.ALOAD, 0));
            opcodeList.set(largestFound.startOpcode++, new MyOpcode(Opcodes.INVOKEVIRTUAL, new String[]{myCompiler.classIName, methodName, "()"+Compiler.FODesc}));
            opcodeList.set(largestFound.startOpcode++, new MyOpcode(Opcodes.DUP));
            KLabel nullLabel=new KLabel();
            opcodeList.set(largestFound.startOpcode++, new MyOpcode(Opcodes.IFNULL, nullLabel));
            opcodeList.set(largestFound.startOpcode++, new MyOpcode(Opcodes.ARETURN));
            opcodeList.set(largestFound.startOpcode++, new MyOpcode(MyOpcode.LABEL, nullLabel));
            opcodeList.set(largestFound.startOpcode++, new MyOpcode(Opcodes.POP));
        }
        adapterMV.visitMaxs(0, 0);
        adapterMV.visitEnd();
        opcodeList.removeRange(largestFound.startOpcode, largestFound.endOpcode);
        return maxSizeChange;
    }
    /* Despite being a long method with a lot of loops called often, this method should still be
     * very fast due to constantly pruning the arrays. */
    private void considerSubmethod(KLabel endLabel) {
        int stackSize=stackTypes.size();
        if(waitTillStackClear) {
            if(stackSize==0)
                waitTillStackClear=false;
            else
                return;
        }
        if((returnSinceLastE && lastEStack<stackSize)
          ||(lastEStack<stackSize-1)) return;
        int totalSize=maxSizeSinceLastE;
        int lastSize=0;
        int bestEntranceI=-1;
        String otherReturn="()V";
        if(stackSize>0) {
            Integer lastType=stackTypes.get(stackTypes.size()-1);
            if((lastType==OBJECT)||(lastType==NOTYPE)) {}
            else if(lastType==INTEGER) otherReturn="()I";
            else if(lastType==DOUBLE) otherReturn="()D";
            else if(lastType==BOOLEAN) otherReturn="()Z";
            else otherReturn="()"+getStackTypeDesc(lastType);
        }
        boolean aReturn=returnSinceLastE;
        for(int i=markList.size()-1;i>=0;i--) {
            Mark m=markList.get(i);
            if((m.type==Mark.BRANCH)&&(m.label==endLabel)) continue;
            if(m.type!=Mark.ENTRANCE) break;
            if(m.stackSize<stackSize-1) break;
            if((aReturn||m.returnSinceLastE||(otherReturn=="()V")) && m.stackSize<stackSize) break;
            if(m.stackSize<=stackSize) bestEntranceI=i;
            totalSize+=lastSize;
            lastSize=m.maxSizeSinceLastE;
            aReturn|=m.returnSinceLastE;
            while(totalSize>maxSubSize) {
                int change=commitLargestMethod(m.location);
                if(change<0)
                    throw new RuntimeException("Cannot fit the code into small enough methods (commit)! "+totalSize);
                totalSize-=change;
            }
        }
        if((bestEntranceI==-1)||(totalSize<=4)) return;
        Mark m=markList.get(bestEntranceI);
        int startLocation=m.location;
        for(int j=markList.size()-1;j>bestEntranceI;j--) {
            Mark next=markList.get(j);
            if(next.type==Mark.ENTRANCE) {
                maxSizeSinceLastE+=next.maxSizeSinceLastE;
                returnSinceLastE|=next.returnSinceLastE;    //should be redundant
                markList.remove(j);
                //System.out.println("MarkOutB "+next.type+" "+next.location);
            }
        }
        int numPotential=potentialSSMs.size();
        skipNewSSM:
        {
            for(int j=potentialSSMs.size()-1;j>=0;j--) {
                PotentialSSM old=potentialSSMs.get(j);
                if(old.startOpcode>startLocation) {
                    potentialSSMs.remove(j);
                    continue;
                } else if(old.startOpcode==startLocation) {
                    old.endOpcode=opcodeList.size();
                    old.endLabel=endLabel;
                    old.maxSize=maxSizeSinceLastE;
                    old.otherReturn=otherReturn;
                    old.containsReturn=returnSinceLastE;
                    //System.out.println("PotentialR: "+startLocation+" "+opcodeList.size());
                    break skipNewSSM;
                } else {
                    break;
                }
            }
            potentialSSMs.add(new PotentialSSM(startLocation, opcodeList.size(), m.label, endLabel,
                maxSizeSinceLastE, returnSinceLastE, (m.stackSize==stackSize)?"()V":otherReturn));
        }
    }
    private void markBranchTo(KLabel L) {
        if(labelUsage.containsKey(L)) {
            Mark oldMark=labelUsage.get(L);
            if(oldMark.type==Mark.LABEL) {
                int indexOf=markList.lastIndexOf(oldMark);
                for(int i=indexOf;i<markList.size();) {
                    Mark next=markList.get(i);
                    if((next.type==Mark.ENTRANCE)&&(next.label!=L)) {
                        markList.remove(i);
                        //System.out.println("MarkOutC "+next.type+" "+next.location);
                        maxSizeSinceLastE+=next.maxSizeSinceLastE;
                        returnSinceLastE|=next.returnSinceLastE;
                    }
                    else
                        i++;
                }
                if((++L.currentCount==L.branchesToHere)&&(!L.isMainLabel)) {
                    labelUsage.remove(L);
                    markList.remove(indexOf);
                    //System.out.println("MarkOutD "+oldMark.type+" "+oldMark.location);
                }
            } else {
                markList.add(new Mark(Mark.BRANCH, stackTypes.size(), opcodeList.size(), 0, L, false));
                L.currentCount++;
            }
        } else {
            Mark newMark=new Mark(Mark.BRANCH, stackTypes.size(), opcodeList.size(), 0, L, false);
            markList.add(newMark);
            labelUsage.put(L, newMark);
        }
    }
    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        MyOpcode newOpcode=new MyOpcode(opcode, new String[]{owner, name, desc});
        handleStackChange(newOpcode);
        maxSizeSinceLastE+=getMaxSize(opcode);
        opcodeList.add(newOpcode);
        considerSubmethod(null);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        MyOpcode newOpcode=new MyOpcode(Opcodes.IINC, var, Integer.valueOf(increment));
        maxSizeSinceLastE+=getMaxSize(Opcodes.IINC);
        opcodeList.add(newOpcode);
    }

    @Override
    public void visitInsn(int opcode) {
        MyOpcode newOpcode=new MyOpcode(opcode);
        handleStackChange(newOpcode);
        maxSizeSinceLastE+=getMaxSize(opcode);
        opcodeList.add(newOpcode);
        considerSubmethod(null);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        MyOpcode newOpcode=new MyOpcode(opcode, operand);
        handleStackChange(newOpcode);
        maxSizeSinceLastE+=getMaxSize(opcode);
        opcodeList.add(newOpcode);
        considerSubmethod(null);
    }

    @Override
    public void visitJumpInsn(int opcode, org.objectweb.asm.Label label) {
        MyOpcode newOpcode=new MyOpcode(opcode, label);
        handleStackChange(newOpcode);
        maxSizeSinceLastE+=getMaxSize(opcode);
        opcodeList.add(newOpcode);
        markBranchTo((KLabel)label);
        considerSubmethod(null);
    }

    @Override
    public void visitLdcInsn(Object cst) {
        MyOpcode newOpcode=new MyOpcode(Opcodes.LDC, cst);
        handleStackChange(newOpcode);
        maxSizeSinceLastE+=getMaxSize(Opcodes.LDC);
        opcodeList.add(newOpcode);
        considerSubmethod(null);
    }

    @Override
    public void visitLookupSwitchInsn(org.objectweb.asm.Label dflt, int[] keys, org.objectweb.asm.Label[] labels) {
        throw new UnsupportedOperationException("Opcode not supported: "+Opcodes.LOOKUPSWITCH);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        MyOpcode newOpcode=new MyOpcode(opcode, new String[]{owner, name, desc});
        int stackUse=(opcode==Opcodes.INVOKESTATIC)?0:1;
        int fieldEnd=desc.lastIndexOf(')');
        for(String fieldString=desc.substring(1, fieldEnd);fieldString.length()>0;stackUse++)
        {
            while(fieldString.charAt(0)=='[')
                fieldString=fieldString.substring(1);
            if(fieldString.charAt(0)=='L')
            {
                int fieldStart=fieldString.indexOf(';');
                if(fieldStart<0)
                    throw new RuntimeException("Format error for Method signature: "+desc);
                else
                    fieldString=fieldString.substring(fieldStart+1);
            }
            else
                fieldString=fieldString.substring(1);
        }
        if(stackUse>0)
            stackDecrease(stackUse);
        else
            markEntrance();
        maxSizeSinceLastE+=getMaxSize(opcode);
        opcodeList.add(newOpcode);
        if(!desc.endsWith("V")) {
            if(desc.endsWith(";")) {
                stackTypes.add(getStackTypeInt(desc.substring(desc.indexOf(")")+1)));
            } else if(desc.endsWith("I")) {
                stackTypes.add(INTEGER);
            } else if(desc.endsWith("Z")) {
                stackTypes.add(BOOLEAN);
            } else if(desc.endsWith("D")) {
                stackTypes.add(DOUBLE);
            } else throw new UnsupportedOperationException("Unknown type for stack: "+desc);
        }
        considerSubmethod(null);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        throw new UnsupportedOperationException("Opcode not supported: "+Opcodes.MULTIANEWARRAY);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, org.objectweb.asm.Label dflt, org.objectweb.asm.Label[] labels) {
        MyOpcode newOpcode=new MyOpcode(Opcodes.TABLESWITCH, max, new Object[]{Integer.valueOf(min), dflt, labels});
        handleStackChange(newOpcode);
        maxSizeSinceLastE+=(max-min+5)*4;
        opcodeList.add(newOpcode);
        markBranchTo((KLabel)dflt);
        for(KLabel L : (KLabel[])labels)
            markBranchTo(L);
        considerSubmethod(null);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if(opcode!=Opcodes.ANEWARRAY)
            throw new UnsupportedOperationException("Opcode not supported: "+opcode);
        MyOpcode newOpcode;
        if(opcode==Opcodes.ANEWARRAY)
            newOpcode=new MyOpcode(opcode, "["+type+";");
        else
            newOpcode=new MyOpcode(opcode, type);
        handleStackChange(newOpcode);
        maxSizeSinceLastE+=getMaxSize(opcode);
        opcodeList.add(newOpcode);
        considerSubmethod(null);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        MyOpcode newOpcode=new MyOpcode(opcode, var);
        handleStackChange(newOpcode);
        if(opcode==Opcodes.ALOAD)
            maxSizeSinceLastE+=(var<4)?1:2;
        else
            maxSizeSinceLastE+=getMaxSize(opcode);
        opcodeList.add(newOpcode);
        considerSubmethod(null);
    }

    @Override
    public void visitLabel(org.objectweb.asm.Label L) {
        unusedBranchCheck:
        if(opcodeList.size()>0 && opcodeList.get(opcodeList.size()-1).otherData==L) {
            //Only pruning GOTOs for now
            if(opcodeList.get(opcodeList.size()-1).opcode!=Opcodes.GOTO) {
                break unusedBranchCheck;
            }
            //Undoing a goto:
            //Stack does not require a fix. Remove entrance mark. Leave branch mark for normal logic to remove.
            //Reduce maxsize. Remove opcode. There won't be a potential submethod to worry about.
            maxSizeSinceLastE-=5;
            Mark markToRemove=markList.get(markList.size()-2);
            if(markToRemove.type==Mark.ENTRANCE) {
                markList.remove(markList.size()-2);
                maxSizeSinceLastE+=markToRemove.maxSizeSinceLastE;
                returnSinceLastE|=markToRemove.returnSinceLastE;
            }
            opcodeList.remove(opcodeList.size()-1);
        }
        considerSubmethod((KLabel)L);
        if(labelUsage.containsKey(L)) {
            Mark oldMark=labelUsage.get(L);
            int change=stackTypes.size()-oldMark.stackSize;
            if(change!=0) {
                //if(stackTypes.size()!=0)
                //    throw new RuntimeException("Leftovers on the stack from something? "+stackTypes.size()+" "+opcodeList.size());
                if(change>0)
                    stackDecrease(change);
                else
                    stackIncrease(-change);
            }
            int indexOf=markList.lastIndexOf(oldMark);
            for(int i=indexOf;i<markList.size();) {
                Mark next=markList.get(i);
                if((next.type==Mark.BRANCH)&&(next.label==L)) {
                    markList.remove(i);
                    //System.out.println("MarkOutE "+next.type+" "+next.location);
                } else if(next.type==Mark.ENTRANCE) {
                    //System.out.println("MarkOutF "+next.type+" "+next.location);
                    markList.remove(i);
                    maxSizeSinceLastE+=next.maxSizeSinceLastE;
                    returnSinceLastE|=next.returnSinceLastE;
                }
                else
                    i++;
            }
            //if((((KLabel)L).branchesToHere==0)&&(!((KLabel)L).isMainLabel)) {
                //System.out.println("No branches to label! "+((KLabel)L).extra);
            //}
            if(((++((KLabel)L).currentCount)==(((KLabel)L).branchesToHere))&&(!((KLabel)L).isMainLabel)) {
                labelUsage.remove(L);
            } else {
                //System.out.println("KLabel found old branches "+((KLabel)L).currentCount+"/"+((KLabel)L).branchesToHere);
                Mark newMark=new Mark(Mark.LABEL, stackTypes.size(), opcodeList.size(), 0, (KLabel)L, false);
                markList.add(newMark);
                labelUsage.put((KLabel)L, newMark);
            }
        } else {
            Mark newMark=new Mark(Mark.LABEL, stackTypes.size(), opcodeList.size(), 0, (KLabel)L, false);
            markList.add(newMark);
            labelUsage.put((KLabel)L, newMark);
        }
        opcodeList.add(new MyOpcode(MyOpcode.LABEL, L));
    }

    @Override
    public void visitMaxs(int i, int j) {
        int totalSize=maxSizeSinceLastE;
        for(Mark m : markList) {
            totalSize+=m.maxSizeSinceLastE;
        }
        while(totalSize>maxSize) {
            int change=commitLargestMethod(0);
            if(change<0)
                throw new RuntimeException("Cannot fit the code into small enough methods (final crunch)! "+totalSize);
            //    break;
            totalSize-=change;
        }
        for(MyOpcode op : opcodeList) {
            visit(op);
        }
        super.visitMaxs(i, j);
        
        /*
        System.out.println("Leftover marks: "+markList.size());
        for(Mark m : markList) {
            System.out.print(m.type+" "+m.location);
            KLabel L=m.label;
            if(L!=null) {
                System.out.print(" KLabel "+L.currentCount+"/"+L.branchesToHere);
                if(L.isMainLabel)
                    System.out.print(" (Main)");
            }
            System.out.print("\r\n");
        }
        */
    }
}
