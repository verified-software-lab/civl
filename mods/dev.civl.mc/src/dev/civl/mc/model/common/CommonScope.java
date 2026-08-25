/**
 * 
 */
package dev.civl.mc.model.common;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.civl.mc.model.IF.CIVLFunction;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.Identifier;
import dev.civl.mc.model.IF.Model;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.type.CIVLArrayType;
import dev.civl.mc.model.IF.type.CIVLBundleType;
import dev.civl.mc.model.IF.type.CIVLHeapType;
import dev.civl.mc.model.IF.type.CIVLPointerType;
import dev.civl.mc.model.IF.type.CIVLPrimitiveType;
import dev.civl.mc.model.IF.type.CIVLStructOrUnionType;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.type.CIVLType.TypeKind;
import dev.civl.mc.model.IF.type.StructOrUnionField;
import dev.civl.mc.model.IF.variable.Variable;

/**
 * A scope.
 * 
 * @author Timothy K. Zirkel (zirkel)
 * 
 */
public class CommonScope extends CommonSourceable implements Scope {

	/**
	 * The parent scope of this scope. Each scope has exactly one parent scope,
	 * except for the root scope which has no parent.
	 */
	private Scope parent;
	private Variable[] variables;
	private String[] functionNames;
	private Map<String, CIVLFunction> functions;
	private Set<Scope> children = new LinkedHashSet<Scope>();
	private Collection<Variable> procRefs = new HashSet<Variable>();
	private Collection<Variable> stateRefs = new HashSet<Variable>();
	private Collection<Variable> scopeRefs = new HashSet<Variable>();
	private Collection<Variable> pointerRefs = new HashSet<Variable>();
	private Collection<Variable> varsNeedSymbolicConstant = new LinkedHashSet<Variable>();
	private int id;
	private CIVLFunction function;

	/**
	 * A scope.
	 * 
	 * @param parent    The containing scope of this scope. Only null for the
	 *                  outermost scope of the designated "System" function.
	 * 
	 * @param variables The set of variables in this scope.
	 */
	public CommonScope(CIVLSource source, Scope parent, Set<Variable> variables, int id) {
		super(source);
		this.parent = parent;
		this.variables = new Variable[variables.size()];
		this.functions = new HashMap<>();
		this.functionNames = new String[0];
		for (Variable v : variables) {
			assert this.variables[v.vid()] == null;
			this.variables[v.vid()] = v;
			v.setScope(this);
			// checkProcRef(v);
			// checkPointer(v);
			// checkScopeRef(v);
		}
		this.id = id;
	}

	/**
	 * @return The containing scope of this scope.
	 */
	public Scope parent() {
		return parent;
	}

	@Override
	public Variable[] variables() {
		return variables;
	}

	/**
	 * @return The number of variables contained in this scope.
	 */
	public int numVariables() {
		return variables.length;
	}

	/**
	 * @return The id of this scope.
	 */
	public int id() {
		return id;
	}

	/**
	 * @return The scopes contained by this scope.
	 */
	public Set<Scope> children() {
		return children;
	}

	/**
	 * @param parent The containing scope of this scope.
	 */
	public void setParent(Scope parent) {
		this.parent = parent;
	}

	/**
	 * @param variables The set of variables contained in this scope.
	 */
	public void setVariables(Set<Variable> variables) {
		this.variables = new Variable[variables.size()];
		for (Variable v : variables) {
			assert this.variables[v.vid()] == null;
			this.variables[v.vid()] = v;
			// checkProcRef(v);
			// checkPointer(v);
			// checkScopeRef(v);
		}
	}

	/**
	 * @param children The scopes contained by this scope.
	 */
	public void setChildren(Set<Scope> children) {
		this.children = children;
	}

	/**
	 * @param A new scope contained by this scope.
	 */
	public void addChild(Scope child) {
		children.add(child);
	}

	@Override
	public CIVLFunction getFunction(String name) {
		return functions.get(name);
	}

	@Override
	public void addFunction(CIVLFunction function) {
		String[] oldFunctionNames = this.functionNames;

		this.functions.put(function.name().name(), function);
		this.functionNames = new String[oldFunctionNames.length + 1];
		for (int i = 0; i < oldFunctionNames.length; i++)
			functionNames[i] = oldFunctionNames[i];
		assert function.fid() == oldFunctionNames.length;
		functionNames[oldFunctionNames.length] = function.name().name();
	}

	/**
	 * A new variable in this scope.
	 */
	public void addVariable(Variable variable) {
		Variable[] oldVariables = variables;

		variables = new Variable[oldVariables.length + 1];
		for (int i = 0; i < oldVariables.length; i++) {
			variables[i] = oldVariables[i];
		}
		assert variable.vid() == oldVariables.length;
		variables[oldVariables.length] = variable;
		// checkProcRef(variable);
		// checkScopeRef(variable);
		// checkPointer(variable);
		variable.setScope(this);
	}

	public Variable seekVariable(Identifier name) {
		for (Variable v : variables) {
			if (v.name().equals(name)) {
				return v;
			}
		}
		if (parent != null) {
			return parent.seekVariable(name);
		}
		return null;
	}

	@Override
	public boolean containsVariable(String name) {
		for (Variable v : variables) {
			if (v.name().name().equals(name)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the variable at the specified array index.
	 * 
	 * @param vid The index of the variable. Should be in the range
	 *            [0,numVariable()-1].
	 * @return The variable at the index.
	 */
	public Variable getVariable(int vid) {
		return variables[vid];
	}

	/**
	 * @param function The function containing this scope.
	 */
	public void setFunction(CIVLFunction function) {
		this.function = function;
	}

	/**
	 * @return The function containing this scope.
	 */
	public CIVLFunction function() {
		return function;
	}

	/**
	 * @return The identifier of the function containing this scope.
	 */
	public Identifier functionName() {
		return function.name();
	}

	/**
	 * A variables has a "procRefType" if it is of type Process, if it is an array
	 * with element of procRefType, or if it is a struct with fields of procRefType.
	 * 
	 * @return A collection of the variables in this scope with a procRefType.
	 */
	public Collection<Variable> variablesWithProcrefs() {
		return procRefs;
	}

	@Override
	public Collection<Variable> variablesWithStaterefs() {
		return stateRefs;
	}

	/**
	 * A variables has a "scopeRefType" if it is of type Scope, if it is an array
	 * with element of scopeRefType, if it is a struct with fields of scopeRefType,
	 * or if it contains a pointer.
	 * 
	 * @return A collection of the variables in this scope with a scopeRefType.
	 */
	public Collection<Variable> variablesWithScoperefs() {
		return scopeRefs;
	}

	/**
	 * A variable contains a pointer type if it is of type PointerType, if it is an
	 * array with elements containing pointer type, or if it is a struct with fields
	 * containing pointer type.
	 * 
	 * @return A collection of the variables in this scope containing pointer types.
	 */
	public Collection<Variable> variablesWithPointers() {
		return pointerRefs;
	}

	/**
	 * Checks if a variables is a procRefType. If it is, it gets added to procRefs.
	 * 
	 * @param v The variable being checked.
	 */
	private void checkProcRef(Variable variable) {
		boolean procRefType = containsProcType(variable.type());

		if (procRefType) {
			procRefs.add(variable);
		}
	}

	/**
	 * Checks if a variables is a scopeRefType. If it is, it gets added to
	 * scopeRefs.
	 * 
	 * @param v The variable being checked.
	 */
	private void checkScopeRef(Variable variable) {
		CIVLType type = variable.type();
		boolean scopeRefType = containsScopeType(type);

		if (scopeRefType) {
			scopeRefs.add(variable);
		} else if (containsPointerType(type)) {
			scopeRefs.add(variable);
		}
	}

	/**
	 * Checks if a variables is a pointer. If it is, it gets added to pointers.
	 * 
	 * @param v The variable being checked.
	 */
	private void checkPointer(Variable variable) {
		boolean pointerType = containsPointerType(variable.type());

		if (pointerType) {
			pointerRefs.add(variable);
			variable.setPointerRef(true);
		}
	}

	/**
	 * Checks if a variables is of primitive type. If it is NOT, it gets added to
	 * non-primitive variables.
	 * 
	 * @param variable The variable being checked.
	 */
	private void needsSymbolicConstant(Variable variable) {
		boolean needsSymbolicConstant = this.needsSymbolicConstantWorker(variable);

		if (needsSymbolicConstant)
			this.varsNeedSymbolicConstant.add(variable);
	}

	private boolean needsSymbolicConstantWorker(Variable variable) {
		if (variable.vid() == 0 || variable.isInput() || variable.isBound())
			return true;
		else {
			CIVLType type = variable.type();

			if (type instanceof CIVLPrimitiveType || type.isPointerType() || type.isDomainType())
				return false;
		}
		return true;
	}

	private boolean containsPointerType(CIVLType type) {
		boolean containsPointerType = false;

		if (type instanceof CIVLPointerType) {
			containsPointerType = true;
		} else if (type instanceof CIVLArrayType) {
			containsPointerType = containsPointerType(((CIVLArrayType) type).elementType());
		} else if (type instanceof CIVLStructOrUnionType) {
			for (StructOrUnionField f : ((CIVLStructOrUnionType) type).fields()) {
				boolean fieldContainsPointer = containsPointerType(f.type());

				containsPointerType = containsPointerType || fieldContainsPointer;
			}
		} else if (type.isHeapType()) {
			for (int i = 0; i < ((CIVLHeapType) type).getNumMallocs(); i++) {
				boolean elementContainsPointer = containsPointerType(
						((CIVLHeapType) type).getMalloc(i).getStaticElementType());

				containsPointerType = containsPointerType || elementContainsPointer;
			}
		} else if (type.isBundleType()) {
			List<CIVLType> types = ((CIVLBundleType) type).types();

			for (CIVLType elementType : types) {
				boolean elementContainsPointer = containsPointerType(elementType);

				containsPointerType = containsPointerType || elementContainsPointer;
			}
		} else if (type.typeKind() == TypeKind.MEM)
			// MEM type is a set of pointers hence contains scope refs:
			containsPointerType = true;
		return containsPointerType;
	}

	private boolean containsScopeType(CIVLType type) {
		boolean containsScopeType = false;

		if (type.isScopeType() || type.isFunction() || type.isPointerType()) {
			containsScopeType = true;
		} else if (type.isArrayType()) {
			containsScopeType = containsScopeType(((CIVLArrayType) type).elementType());
		} else if (type.isStructType()) {
			for (StructOrUnionField f : ((CIVLStructOrUnionType) type).fields()) {
				boolean fieldContainsScope = containsScopeType(f.type());

				containsScopeType = containsScopeType || fieldContainsScope;
			}
		} else if (type.isHeapType()) {
			for (int i = 0; i < ((CIVLHeapType) type).getNumMallocs(); i++) {
				boolean elementContainsScope = containsScopeType(
						((CIVLHeapType) type).getMalloc(i).getStaticElementType());

				containsScopeType = containsScopeType || elementContainsScope;
			}
		} else if (type.isBundleType()) {
			List<CIVLType> types = ((CIVLBundleType) type).types();

			for (CIVLType elementType : types) {
				boolean elementContainsScope = containsScopeType(elementType);

				containsScopeType = containsScopeType || elementContainsScope;
			}
		}
		return containsScopeType;
	}

	private boolean containsProcType(CIVLType type) {
		boolean containsProcType = false;

		if (type.isProcessType()) {
			containsProcType = true;
		} else if (type.isArrayType()) {
			containsProcType = containsProcType(((CIVLArrayType) type).elementType());
		} else if (type.isStructType()) {
			for (StructOrUnionField f : ((CIVLStructOrUnionType) type).fields()) {
				boolean fieldContainsProc = containsProcType(f.type());

				containsProcType = containsProcType || fieldContainsProc;
			}
		} else if (type.isHeapType()) {
			for (int i = 0; i < ((CIVLHeapType) type).getNumMallocs(); i++) {
				boolean elementContainsProc = containsProcType(
						((CIVLHeapType) type).getMalloc(i).getStaticElementType());

				containsProcType = containsProcType || elementContainsProc;
			}
		} else if (type.isBundleType()) {
			List<CIVLType> types = ((CIVLBundleType) type).types();

			for (CIVLType elementType : types) {
				boolean elementContainsProc = containsProcType(elementType);

				containsProcType = containsProcType || elementContainsProc;
			}
		}
		return containsProcType;
	}

	@Override
	public String toString() {
		String result = "Variables : {";

		for (int i = 0; i < variables.length; i++) {
			if (i != 0) {
				result += ", ";
			}
			result += variables[i].toString();
		}
		result += "}";
		return result;
	}

	@Override
	public void print(String prefix, PrintStream out, boolean isDebug) {
		String parentID = "";

		if (parent == null) {
			parentID += "null";
		} else {
			parentID += parent.id();
		}
		out.println(prefix + "scope " + id + " (parent: " + parentID + ")");
		for (Variable v : variables) {
			if (isDebug) {
				out.println(prefix + "| " + v + " (purely local)");
			} else {
				out.println(prefix + "| " + v);
			}
		}
		for (Scope child : children) {
			if (child.function().equals(function)) {
				child.print(prefix + "| ", out, isDebug);
			}
		}
		out.flush();
	}

	@Override
	public CIVLFunction seekFunction(Identifier name) {
		String functionName = name.name();

		if (this.functions.containsKey(functionName))
			return functions.get(functionName);
		if (this.parent != null)
			return this.parent.seekFunction(name);
		return null;
	}

	@Override
	public CIVLFunction seekFunction(String name) {
		if (this.functions.containsKey(name))
			return functions.get(name);
		if (this.parent != null)
			return this.parent.seekFunction(name);
		return null;
	}

	public int getVid(Variable staticVariable) {
		int result = -1;

		for (int i = 0; i < variables.length; i++) {
			if (variables[i].equals(staticVariable)) {
				result = i;
				break;
			}
		}
		return result;
	}

	@Override
	public Model model() {
		return function.model();
	}

	@Override
	public boolean isDescendantOf(Scope anc) {
		Scope parent = this.parent;

		while (parent != null) {
			if (parent.id() == anc.id())
				return true;
			parent = parent.parent();
		}
		return false;
	}

	@Override
	public Variable getVariable(String name) {
		for (Variable v : variables) {
			if (v.name().name().equals(name)) {
				return v;
			}
		}
		return null;
	}

	@Override
	public void complete() {
		for (Variable v : variables) {
			this.checkPointer(v);
			this.checkScopeRef(v);
			this.checkProcRef(v);
			this.needsSymbolicConstant(v);
		}
		if (children != null)
			for (Scope child : children)
				child.complete();
	}

	@Override
	public Collection<Variable> varsNeedSymbolicConstant() {
		return this.varsNeedSymbolicConstant;
	}

	@Override
	public boolean hasVariable() {
		return variables != null && variables.length > 0;
	}

	@Override
	public boolean hasVariableWtPointer() {
		return this.pointerRefs != null && this.pointerRefs.size() > 0;
	}

	@Override
	public int numFunctions() {
		return this.functionNames.length;
	}

	@Override
	public CIVLFunction getFunction(int fid) {
		return this.functions.get(functionNames[fid]);
	}

	@Override
	public Variable getMatch(Variable variable) {
		for (Variable v : variables) {
			if (v.name().name().equals(variable.name().name())) {
				return v;
			}
		}
		return null;
	}
}
