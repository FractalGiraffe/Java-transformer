package comp0012.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Stack;
import java.util.HashMap;
import java.lang.Number;
import java.lang.Integer;
import java.lang.Float;
import java.lang.Double;
import java.lang.Long;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.util.InstructionFinder;
import org.apache.bcel.generic.*;



public class ConstantFolder {
	ClassParser parser = null;
	JavaClass original = null;
	JavaClass optimized = null;
	Stack<Number> constantStack = null;
	HashMap<Integer, Number> localVars = null;

	public ConstantFolder(String classFilePath) {
		try{
			System.out.println(classFilePath);
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();

		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	private void optimize() {
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();

		// Implement your optimization here

		Method[] methods = cgen.getMethods();
		for (Method m : methods) {
			optimizeMethod(cgen, cpgen, m);
		}
        
		this.optimized = cgen.getJavaClass();
	}

	private void optimizeMethod(ClassGen cgen, ConstantPoolGen cpgen, Method method) {
		Code methodCode = method.getCode();
		constantStack = new Stack<Number>();
		localVars = new HashMap<Integer, Number>();
		InstructionList instList = new InstructionList(methodCode.getCode());
		MethodGen methodGen = new MethodGen(method.getAccessFlags(), method.getReturnType(), method.getArgumentTypes(), null, method.getName(), cgen.getClassName(), instList, cpgen);

		System.out.println("\n");
		System.out.println("Optimising method: " + method.getName());
		System.out.println("\n");

		for (InstructionHandle handle : instList.getInstructionHandles()) {
			boolean isConst = (handle.getInstruction() instanceof LDC) || (handle.getInstruction() instanceof LDC2_W)
					|| (handle.getInstruction() instanceof SIPUSH) || (handle.getInstruction() instanceof BIPUSH)
					|| (handle.getInstruction() instanceof ICONST) || (handle.getInstruction()) instanceof FCONST || (handle.getInstruction() instanceof LCONST) || (handle.getInstruction() instanceof DCONST);
			boolean isArithmeticExpr = (handle.getInstruction() instanceof ArithmeticInstruction);
			boolean isStore = (handle.getInstruction() instanceof StoreInstruction);
			boolean isLoad = (handle.getInstruction() instanceof LoadInstruction);
			boolean isConv = (handle.getInstruction() instanceof I2D);

			System.out.println(handle + "\tStack contents:" + constantStack);

			if (isConst) {
				Number value = getValue(handle, cpgen);
				constantStack.push(value);

				System.out.println(value + " pushed to stack");

			} else if (isArithmeticExpr) {
				evalExpr(handle);
				Number topOfStack = constantStack.peek();
				removeConstantDecs(handle, instList, 2);

				if (topOfStack instanceof Double) {
					instList.insert(handle, new LDC2_W(cpgen.addDouble((Double) topOfStack)));
				} else if (topOfStack instanceof Long) {
					instList.insert(handle, new LDC2_W(cpgen.addLong((Long) topOfStack)));
				} else if (topOfStack instanceof Integer) {
					instList.insert(handle, new LDC(cpgen.addInteger((Integer) topOfStack)));
				} else if (topOfStack instanceof Float) {
					instList.insert(handle, new LDC(cpgen.addFloat((Float) topOfStack)));
				}

				safeDelete(handle, instList);
			} else if (isConv) {
				safeDelete(handle, instList);
			} else if (isStore) {
				Number value = constantStack.pop();
				int ref = (((StoreInstruction) handle.getInstruction()).getIndex());
				localVars.put(ref, value);
			} else if (isLoad) {
				if (!(handle.getInstruction() instanceof ALOAD)) {
					int ref = ((LoadInstruction) handle.getInstruction()).getIndex();
					Number topOfStack = localVars.get(ref);
					constantStack.push(topOfStack);
					System.out.println(topOfStack + " pushed to stack");
				}
			}
		}

		System.out.println("\nResult:");
		for (InstructionHandle handle : instList.getInstructionHandles()) {
			System.out.println(handle.toString());
		}

		instList.setPositions(true);
		methodGen.setMaxStack();
		methodGen.setMaxLocals();
		Method newMethod = methodGen.getMethod();
		cgen.replaceMethod(method, newMethod);
	}

	private void evalExpr(InstructionHandle handle) {
		Number x = constantStack.pop();
		Number y = constantStack.pop();

		if (handle.getInstruction() instanceof FMUL) {
			constantStack.push(y.floatValue() * x.floatValue());
		} else if (handle.getInstruction() instanceof DMUL) {
			constantStack.push(y.doubleValue() * x.doubleValue());
		} else if (handle.getInstruction() instanceof ISUB) {
			constantStack.push(y.intValue() - x.intValue());
		} else if (handle.getInstruction() instanceof LSUB) {
			constantStack.push(y.longValue() - x.longValue());
		} else if (handle.getInstruction() instanceof FSUB) {
			constantStack.push(y.floatValue() - x.floatValue());
		} else if (handle.getInstruction() instanceof DSUB) {
			constantStack.push(y.doubleValue() - x.doubleValue());
		} else if (handle.getInstruction() instanceof IDIV) {
			constantStack.push(y.intValue() / x.intValue());
		} else if (handle.getInstruction() instanceof LDIV) {
			constantStack.push(y.longValue() / x.longValue());
		} else if (handle.getInstruction() instanceof FDIV) {
			constantStack.push(y.floatValue() / x.floatValue());
		} else if (handle.getInstruction() instanceof DDIV) {
			constantStack.push(y.doubleValue() / x.doubleValue());
		} else if (handle.getInstruction() instanceof IADD) {
			constantStack.push(y.intValue() + x.intValue());
		} else if (handle.getInstruction() instanceof LADD) {
			constantStack.push(y.longValue() + x.longValue());
		} else if (handle.getInstruction() instanceof FADD) {
			constantStack.push(y.floatValue() + x.floatValue());
		} else if (handle.getInstruction() instanceof DADD) {
			constantStack.push(y.doubleValue() + x.doubleValue());
		} else if (handle.getInstruction() instanceof IMUL) {
			constantStack.push(y.intValue() * x.intValue());
		} else if (handle.getInstruction() instanceof LMUL) {
			constantStack.push(y.longValue() * x.longValue());
		}
	}

	private Number getValue(InstructionHandle handle, ConstantPoolGen cpgen) {
		Instruction instruction = handle.getInstruction();
		if ((instruction instanceof LDC)) {
			return (Number) (((LDC) instruction).getValue(cpgen));
		} else if (instruction instanceof LDC2_W) {
		   	return (((LDC2_W) instruction).getValue(cpgen));
		} else if (instruction instanceof ICONST) {
			return (((ICONST) handle.getInstruction()).getValue());
		} else if (instruction instanceof FCONST) {
			return (((FCONST) handle.getInstruction()).getValue());
		} else if (instruction instanceof DCONST) {
			return (((DCONST) handle.getInstruction()).getValue());
		} else if (instruction instanceof LCONST) {
			return (((LCONST) handle.getInstruction()).getValue());
		} else if (instruction instanceof BIPUSH) {
			return (((BIPUSH) handle.getInstruction()).getValue());
		} else if (instruction instanceof SIPUSH) {
			return (((SIPUSH) handle.getInstruction()).getValue());
		}
		return null;
	}

	private void safeDelete(InstructionHandle handle, InstructionList instList) {
		try {
			instList.delete(handle);
		} catch (TargetLostException e) {
			InstructionHandle[] targets = e.getTargets();
			for (InstructionHandle target : targets) {
				InstructionTargeter[] targeters = target.getTargeters();
				for (InstructionTargeter targeter : targeters) {
					targeter.updateTarget(target, null);
				}
			}
		}
	}

	private void removeConstantDecs(InstructionHandle handle, InstructionList instList, int target) {
		int count = 0;
		InstructionHandle prevHandle = handle.getPrev();
		while (count < target) {
			if (prevHandle != null) {
				if ((prevHandle.getInstruction() instanceof LDC) || (prevHandle.getInstruction() instanceof LDC2_W)
				|| (prevHandle.getInstruction() instanceof BIPUSH) || (prevHandle.getInstruction() instanceof SIPUSH)
				|| (prevHandle.getInstruction() instanceof ICONST) || (prevHandle.getInstruction() instanceof LCONST)
				|| (prevHandle.getInstruction() instanceof FCONST) || (prevHandle.getInstruction() instanceof DCONST)
				|| (prevHandle.getInstruction() instanceof LoadInstruction)) {
					if (prevHandle.getPrev() == null) {
						safeDelete(prevHandle, instList);
						break;
					} else {
						prevHandle = prevHandle.getPrev();
						safeDelete(prevHandle.getNext(), instList);
						count++;
					}
				} else {
					prevHandle = prevHandle.getPrev();
				}
			} else {
				break;
			}
		}
	}
	
	public void write(String optimisedFilePath) {
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}
