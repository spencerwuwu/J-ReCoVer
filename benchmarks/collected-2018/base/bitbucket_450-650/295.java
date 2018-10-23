// https://searchcode.com/api/result/126531297/

package org.deuce.transform.asm;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.deuce.objectweb.asm.AnnotationVisitor;
import org.deuce.objectweb.asm.ClassWriter;
import org.deuce.objectweb.asm.FieldVisitor;
import org.deuce.objectweb.asm.MethodVisitor;
import org.deuce.objectweb.asm.Opcodes;
import org.deuce.objectweb.asm.Type;
import org.deuce.objectweb.asm.commons.Method;
import org.deuce.transaction.Context;
import org.deuce.transform.Exclude;
import org.deuce.transform.Mobile;
import org.deuce.transform.asm.code.DefaultConstructor;
import org.deuce.transform.asm.code.GetterSetterCode;
import org.deuce.transform.asm.code.ProxyImpl;
import org.deuce.transform.asm.code.ProxyInterface;
import org.deuce.transform.asm.method.MethodTransformer;
import org.deuce.transform.asm.method.StaticMethodTransformer;
import org.deuce.transform.asm.storage.ClassDetails;
import org.deuce.transform.asm.storage.FieldDetails;
import org.deuce.transform.asm.storage.GetterSetterDetails;
import org.deuce.transform.asm.storage.MethodDetails;
import org.deuce.transform.asm.storage.Names;
import org.deuce.transform.asm.type.TypeCodeResolver;
import org.deuce.transform.asm.type.TypeCodeResolverFactory;
import org.deuce.transform.util.Util;

import edu.vt.rt.hyflow.core.instrumentation.ITypeInternalName;
import edu.vt.rt.hyflow.core.instrumentation.method.RemoteConstructorTransformer;

@Exclude
public class ClassTransformer extends ByteCodeVisitor implements FieldsHolder{

	final private static String ENUM_DESC = Type.getInternalName(Enum.class); 
	
	protected boolean exclude = false;
	protected boolean mobile = false;			//To verify the Mobile class
	private boolean visitclinit = false;
	final private LinkedList<Field> fields = new LinkedList<Field>();
	private String staticField = null;
	
	final static public String EXCLUDE_DESC = Type.getDescriptor(Exclude.class);
	final static public String MOBILE_DESC = Type.getDescriptor(Mobile.class);
	final static private String ANNOTATION_NAME = Type.getInternalName(Annotation.class);
	private boolean isInterface;
	private boolean isEnum;
	private MethodVisitor staticMethod;
	
	private final FieldsHolder fieldsHolder;

	private boolean proxyClass;
	private boolean proxyInterface;
	private boolean proxyImpl;
	private boolean callWriter;
	private ClassDetails cD;
	private ArrayList<GetterSetterDetails> FieldNames;
	private boolean defaultConstructor;

	public static ArrayList<Object> rmdtl;
	public static HashMap<Object, ArrayList<Object>> Details;

	public ClassTransformer( String className, ClassWriter classWriter){
		super(className, classWriter);
		this.fieldsHolder = this;
	}
		
	public ClassTransformer( String className, FieldsHolder fieldsHolder, HashMap<Object, ArrayList<Object>> data){
		super( className,data);
		this.fieldsHolder = fieldsHolder == null ? this : fieldsHolder;
		proxyClass = false;		//To add proxy class in original class
		proxyInterface = false;		//To create proxy Interface
		proxyImpl = false;			//To create proxy Implementation
		callWriter = false;			//To access class Writer
		defaultConstructor = false;	//Assume Default constructor is not added by user
		cD = new ClassDetails();
		Details = data;
		FieldNames = new ArrayList<GetterSetterDetails>();
	}

	@Override
	public void visit(final int version, final int access, final String name,
			final String signature, final String superName, final String[] interfaces) {
		
		boolean mobileClass = false;
		ArrayList<Object> classNames = Details.get(Names.MobileClassNames);
		if(classNames != null){
			for(Object className: classNames){
				String Name = (String) className;
				if(name.equals(Name)){
					mobileClass = true;
					break;
				}
			}
		}
		
		fieldsHolder.visit(superName);
		isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
		isEnum = ENUM_DESC.equals(superName);
		
		for(String inter : interfaces){
			if( inter.equals(ANNOTATION_NAME)){
				exclude = true;
				break;
			}
		}
		cD.update(name);
		
		//To Update Interface details, Adding all details at VisitEnd
		if(name.contains("$HY$_I")){
			proxyInterface = true;
			String[] iF = {"java/rmi/Remote","java/io/Serializable"};
			super.visit(version, access, name, null, "java/lang/Object",iF);
			return;
		}
		
		//To update implementation details, Adding all details at VisitEnd
		if(name.contains("$HY$_Proxy")){
			proxyImpl = true;
			String proxyIName = cD.pkg + Names.ProxyInterfacePrefix + cD.name.substring(Names.ProxyClassPrefix.length());
			String[] iF = {proxyIName};
			super.visit(version, access, name, null, "java/rmi/server/UnicastRemoteObject",iF);
			return;
		}
		
		if(mobileClass){
			//Update the Super Name and Interfaces
			String superName1 = Names.AbstractLoggableClass;
			
			String[] interfaces1 = new String[interfaces.length +1];
			System.arraycopy(interfaces, 0, interfaces1, 0, interfaces.length);
			interfaces1[interfaces.length] = Names.HyflowInterface;
			
			super.visit(version, access, name, signature, superName1, interfaces1);
			return;
		}
		
		super.visit(version,access,name, signature, superName, interfaces);
	}

	/**
	 * Checks if the class is marked as {@link Exclude @Exclude}
	 */
	@Override
	public AnnotationVisitor visitAnnotation( String desc, boolean visible) {
		exclude = exclude ? exclude : EXCLUDE_DESC.equals(desc);
		
		//Check for Mobile Class
		mobile = mobile? mobile: MOBILE_DESC.equals(desc);
		
		return super.visitAnnotation(desc, visible);
	}

	/**
	 * Creates a new static filed for each existing field.
	 * The field will be statically initialized to hold the field address.   
	 */
	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature,
			Object value) {

		FieldVisitor fieldVisitor = super.visitField(access, name, desc, signature, value);
		if( exclude)
			return fieldVisitor;
		
		// Define as constant
		int fieldAccess = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
		
		//Return if it proxy Implementation or proxyInterface
		if(proxyImpl||proxyInterface)
			return fieldVisitor;
		
		if(!proxyClass && mobile){  		//Add code for class proxy field and its address
			//Add Proxy Class
			fieldsHolder.addField(Opcodes.ACC_PUBLIC, "$HY$_proxy", Type.getObjectType(cD.pkg+"$HY$_I"+cD.name).getDescriptor(), null);
			
			//Add its Proxy Address Field
			fieldsHolder.addField(fieldAccess,Util.getAddressField("$HY$_proxy"),Type.LONG_TYPE.getDescriptor(), null);
			
			Field field = new Field("$HY$_proxy",Util.getAddressField("$HY$_proxy"));
			fields.add(field);		//To initialized the address value
			proxyClass = true;		//Proxy Class details added
		}
		
		String addressFieldName = Util.getAddressField( name);
		
		final boolean include = (access & Opcodes.ACC_FINAL) == 0 && !name.startsWith(ITypeInternalName.$HY$_IMMUTABLE);
		final boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
		
		if(mobile){
			if( include){ // include field if not final 
				Field field = new Field(name, addressFieldName);
				fields.add( field);
				FieldNames.add(new GetterSetterDetails(new FieldDetails(access, name, desc, signature, value),false,false));
				if(isStatic)
					staticField = name;
				fieldsHolder.addField( fieldAccess, addressFieldName, Type.LONG_TYPE.getDescriptor(), null);
				
				//Add code for proxy field
				fieldsHolder.addField(Opcodes.ACC_PUBLIC, "$HY$_"+name, Type.getDescriptor(Object.class), null);
				
				//Add code for proxy address field
				fieldsHolder.addField(fieldAccess, Util.getAddressField("$HY$_"+name), Type.LONG_TYPE.getDescriptor(), null);
				Field field1 = new Field("$HY$_"+name, Util.getAddressField("$HY$_"+name));
				fields.add(field1);			//To initialize later
			}else{
				// If this field is final mark with a negative address.
				fieldsHolder.addField( fieldAccess, addressFieldName, Type.LONG_TYPE.getDescriptor(), -1L);
			}			
		}
		
		return fieldVisitor;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature,
			String[] exceptions) {
		//TODO: Use Hash Map,Reduce checking time; Check for don't touch Method
		boolean donotTouch = false;
		ArrayList<Object> donotTouchMethodes = Details.get(className + Names.DonotTouchMethodDetails);
		if(donotTouchMethodes != null){
			for(Object md: donotTouchMethodes){
				MethodDetails mD = (MethodDetails) md;
				if(mD.name.equals(name) && mD.desc.equals(desc)){
					donotTouch = true;
					break;
				}
			}
		}
		
		//Remove old constructor in case of Proxy Implementation Class
		if(proxyImpl && name.equals("<init>") && !callWriter){
			return null;		
		}
		
		//Check if it is a default Constructor
		if(name.equals("<init>") && desc.equals("()V"))		
				defaultConstructor = true;

		MethodVisitor originalMethod =  super.visitMethod(access, name, desc, signature, exceptions);
				
		if( exclude || donotTouch)			//If excluded Class or Immutable Method No Transformation
			return originalMethod;
		
		if(callWriter)
			return originalMethod;
		
		final boolean isNative = (access & Opcodes.ACC_NATIVE) != 0;
		if(isNative){
			createNativeMethod(access, name, desc, signature, exceptions);
			return originalMethod;
		}
		
		if( name.equals("<clinit>") && !mobile) {
			return originalMethod;			
		}else if(name.equals("<clinit>") && mobile){		
			staticMethod = originalMethod;
			visitclinit = true;

			if( isInterface){
				return originalMethod;
			}

			int fieldAccess = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
			fieldsHolder.addField( fieldAccess, StaticMethodTransformer.CLASS_BASE,
					Type.getDescriptor(Object.class), null);
			
			MethodVisitor staticMethodVisitor = fieldsHolder.getStaticMethodVisitor();
			return createStaticMethodTransformer( originalMethod, staticMethodVisitor);
		}
		
		if( name.equals("<init>") && !mobile){
			return originalMethod;
		}	
		else if( name.equals("<init>") && mobile){			//To add additional Registration block
			int argSize = Type.getArgumentsAndReturnSizes(desc);
			return new RemoteConstructorTransformer(originalMethod, argSize);
		}
		
		//TODO: It is not required, remove it: Code to check the getter and setter methods exist		
		for(GetterSetterDetails fnm: FieldNames){
			if(name.equals("set"+getUpperCaseName(fnm.fD.VarName)))
				fnm.sttrXst = true;
			if(name.equals("get"+getUpperCaseName(fnm.fD.VarName)))
				fnm.gttrXst=true;				
		}
		
		//Add context variable for copy method arguments
		Method newMethod = createNewMethod(name, desc);

		MethodVisitor copyMethod =  super.visitMethod(access | Opcodes.ACC_SYNTHETIC, name, newMethod.getDescriptor(),
				signature, exceptions);

		return new MethodTransformer( originalMethod, copyMethod, className,
				access, name, desc, newMethod, fieldsHolder);
	}

	private String getUpperCaseName(String varName) {
		String first = varName.substring(0,1).toUpperCase();
		String second = varName.substring(1);
		return first + second;
	}

	/**
	 * Build a dummy method that delegates the call to the native method
	 */
	private void createNativeMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		Method newMethod = createNewMethod(name, desc);
		final int newAccess = access & ~Opcodes.ACC_NATIVE;
		MethodVisitor copyMethod =  super.visitMethod(newAccess | Opcodes.ACC_SYNTHETIC, name, newMethod.getDescriptor(),
				signature, exceptions);
		copyMethod.visitCode();
		
		// load the arguments before calling the original method
		final boolean isStatic = (access & ~Opcodes.ACC_STATIC) != 0;
		int place = 0; // place on the stack
		if(!isStatic){
			copyMethod.visitVarInsn(Opcodes.ALOAD, 0); // load this
			place = 1;
		}
		
		Type[] argumentTypes = newMethod.getArgumentTypes();
		for(int i=0 ; i<(argumentTypes.length-1) ; ++i){
			Type type = argumentTypes[i];
			copyMethod.visitVarInsn(type.getOpcode(Opcodes.ILOAD), place);
			place += type.getSize();
		}
		
		// call the original method
		copyMethod.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL, className, name, desc);
		TypeCodeResolver returnReolver = TypeCodeResolverFactory.getReolver(newMethod.getReturnType());
		if( returnReolver == null) {
			copyMethod.visitInsn( Opcodes.RETURN); // return;
		}else {
			copyMethod.visitInsn(returnReolver.returnCode());
		}
		copyMethod.visitMaxs(1, 1);
		copyMethod.visitEnd();
	}

	@Override
	public void visitEnd() {
		//Didn't see any static method till now, so creates one.
		if(!exclude){
			super.visitAnnotation(EXCLUDE_DESC, false);
			if( !visitclinit && fields.size() > 0) { // creates a new <clinit> in case we didn't see one already. 

				//TODO avoid creating new static method in case of external fields holder
				visitclinit = true;
				MethodVisitor method = visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
				method.visitCode();
				method.visitInsn(Opcodes.RETURN);
				method.visitMaxs(100, 100); // TODO set the right value
				method.visitEnd();

			}
			if(isEnum){ // Build a dummy ordinal() method
				MethodVisitor ordinalMethod = 
					super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "ordinal", "(Lorg/deuce/transaction/Context;)I", null, null);
				ordinalMethod.visitCode();
				ordinalMethod.visitVarInsn(Opcodes.ALOAD, 0);
				ordinalMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "ordinal", "()I");
				ordinalMethod.visitInsn(Opcodes.IRETURN);
				ordinalMethod.visitMaxs(1, 2);
				ordinalMethod.visitEnd();
			}
		}
		
		//Add Getter and setters
		for (GetterSetterDetails gSD : FieldNames) {
			if (gSD.gttrXst == false){
				MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, "get"+gSD.fD.VarName, Type.getMethodDescriptor(Type.getType(gSD.fD.VarDesc), new Type[]{}),
					null, null);
				GetterSetterCode.addGttrCode(gSD, mv,className); // Add super.visitMethod
			}
			if (gSD.sttrXst == false){
				MethodVisitor mv1 = super.visitMethod(Opcodes.ACC_PUBLIC, "set"+gSD.fD.VarName, Type.getMethodDescriptor(Type.VOID_TYPE, new Type[]{Type.getType(gSD.fD.VarDesc)}),
						null, null);
				GetterSetterCode.addSttrCode(gSD, mv1,className);					
			}
		}
		
		//Set Call writer True so that visitMethod can return original method
		callWriter = true;
		
		
		if(proxyInterface || proxyImpl){
			String key = getKey(cD, proxyInterface, proxyImpl);
			rmdtl = rmd.get(key + Names.RemoteMethodDetails);
		}
		if(proxyInterface == true ){		//Add Proxy Interface Code
			ProxyInterface.addCode(rmdtl, this);
		}else if(proxyImpl == true){				//Add Proxy Class Code		
			ProxyImpl.addCode(rmdtl, this, cD);
		}else if(!defaultConstructor){
			DefaultConstructor.addCode(this);
		}
		
		super.visitEnd();
		fieldsHolder.close();
	}

	private String getKey(ClassDetails cD2, boolean proxyInterface2,
			boolean proxyImpl2) {
		if(proxyInterface2){
			String name = cD.name.substring(6);
			return cD2.pkg + name; 
		}
		if(proxyImpl2){
			String name = cD.name.substring(11);
			return cD2.pkg + name; 
		}
		return null;
	}

	private StaticMethodTransformer createStaticMethodTransformer(MethodVisitor originalMethod, MethodVisitor staticMethod){
		return new StaticMethodTransformer( originalMethod, staticMethod, fields, staticField,
				className, fieldsHolder.getFieldsHolderName(className));
	}
	
	public static Method createNewMethod(String name, String desc) {
		Method method = new Method( name, desc);
		Type[] arguments = method.getArgumentTypes();

		Type[] newArguments = new Type[ arguments.length + 1];
		System.arraycopy( arguments, 0, newArguments, 0, arguments.length);
		newArguments[newArguments.length - 1] = Context.CONTEXT_TYPE; // add as a constant

		return new Method( name, method.getReturnType(), newArguments);
	}
	
	@Override
	public void addField(int fieldAccess, String addressFieldName, String desc, Object value){
		super.visitField( fieldAccess, addressFieldName, desc, null, value);
	}
	
	@Override
	public void close(){
	}
	
	@Override
	public MethodVisitor getStaticMethodVisitor(){
		return staticMethod;
	}
	
	@Override
	public String getFieldsHolderName(String owner){
		return owner;
	}

	@Override
	public void visit(String superName) {
		//nothing to do
	}
}

