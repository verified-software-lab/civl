/**
 * 
 */
package dev.civl.mc.model.IF;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Set;

import dev.civl.mc.model.IF.variable.Variable;

/**
 * <p>
 * A scope. A scope contains the variables exclusive to this scope and
 * references to any subscopes.
 * </p>
 * 
 * <p>
 * Maintainer: Stephen Siegel (siegel)
 * </p>
 * 
 * @author Timothy K. Zirkel (zirkel)
 * 
 */
public interface Scope extends Sourceable {

	/**
	 * @return The model to which this scope belongs.
	 */
	Model model();

	/**
	 * @return The id of this scope. This id is unique within the model.
	 */
	int id();

	/**
	 * @return The function containing this scope.
	 */
	CIVLFunction function();

	/**
	 * @return The identifier of the function containing this scope.
	 */
	Identifier functionName();

	/**
	 * Sets the containing function.
	 * 
	 * @param function the function containing this scope.
	 */
	void setFunction(CIVLFunction function);

	/**
	 * @return The containing scope of this scope. If this is the top-most scope,
	 *         returns null.
	 */
	Scope parent();

	/**
	 * @param parent The containing scope of this scope.
	 */
	void setParent(Scope parent);

	/**
	 * Return true iff this scope is a descendant of (i.e., contained within) the
	 * given scope {@code anc}.
	 * 
	 * @param anc a non-null scope
	 * @return {@code true} iff this scope is a descendant of {@code anc}
	 */
	boolean isDescendantOf(Scope anc);

	/**
	 * @return The scopes contained by this scope.
	 */
	Set<Scope> children();

	/**
	 * @param children The scopes contained by this scope.
	 */
	void setChildren(Set<Scope> children);

	/**
	 * @param A new scope contained by this scope.
	 */
	void addChild(Scope child);

	/**
	 * <p>
	 * <b>Important notice: </b> Never ever modify the variable!
	 * </p>
	 * 
	 * @return The set of variables contained in this scope. The iterator over the
	 *         returned set will iterate in variable ID order.
	 */
	Variable[] variables();

	/**
	 * @return The number of variables contained in this scope.
	 */
	int numVariables();

	/**
	 * Does this scope contain at least one variable? (Not counting containing
	 * scopes.)
	 * 
	 * @return {@code true} iff this scope contains at least one variable
	 */
	boolean hasVariable();

	/**
	 * Gets the index of the given variable in the sequence of variables belonging
	 * to this scope. This finds the first variable in the sequence that "equals"
	 * (using the equals method) the given variable, and returns its index. If there
	 * is no such variable in the sequence, returns -1.
	 * 
	 * @param staticVariable a non-null variable
	 * @return index of {@code staticVariable} in the variable sequence of this
	 *         scope, or -1
	 */
	int getVid(Variable staticVariable);

	boolean hasVariableWtPointer();

	/**
	 * @param variables The set of variables contained in this scope.
	 */
	void setVariables(Set<Variable> variables);

	/**
	 * A new variable in this scope.
	 */
	void addVariable(Variable variable);

	/**
	 * If a variable with the same name as the given variable is already in in this
	 * scope, returns that variable. Otherwise return {@code null}.
	 * 
	 * @param variable variable with name to look for
	 * @return variable in this scope with same name or {@code null}
	 */
	Variable getMatch(Variable variable);

	/**
	 * Says whether this scope contains a variable with the given name. Does not
	 * look in containing scopes.
	 * 
	 * @param name a non-null string, the name of the variable to look for
	 * @return {@code true} iff this scope contains a variable with the given name
	 */
	boolean containsVariable(String name);

	/**
	 * Finds the variable in this scope with the given name. Does not look in
	 * containing scopes.
	 * 
	 * @param name a non-null string, the name of the variable to look for
	 * @return the variable with the given name or {@code null} if no variable with
	 *         than name exists in this scope
	 */
	Variable getVariable(String name);

	/**
	 * Get the variable associated with an identifier. If this scope does not
	 * contain such a variable, parent scopes will be recursively checked.
	 * 
	 * @param name The identifier for the variable.
	 * @return The model representation of the variable in this scope hierarchy, or
	 *         null if not found.
	 */
	Variable seekVariable(Identifier name);

	/**
	 * Get the variable at the specified array index.
	 * 
	 * @param vid The index of the variable. Should be in the range
	 *            [0,numVariable()-1].
	 * @return The variable at the index.
	 */
	Variable getVariable(int vid);

	/**
	 * A variables has a "procRefType" if it is of type Process, if it is an array
	 * with element of procRefType, or if it is a struct with fields of procRefType.
	 * 
	 * @return A collection of the variables in this scope with a procRefType.
	 */
	Collection<Variable> variablesWithProcrefs();

	/**
	 * A variables has a "$state" type, if it is of type $state, if it is an array
	 * with element of type $state, or if it is a struct with fields of type $state.
	 * 
	 * @return A collection of the variables in this scope with a type $state.
	 */
	Collection<Variable> variablesWithStaterefs();

	/**
	 * A variables has a "scopeRefType" if it is of type Scope, if it is an array
	 * with element of scopeRefType, if it is a struct with fields of scopeRefType,
	 * or if it contains a pointer.
	 * 
	 * @return A collection of the variables in this scope with a scopeRefType.
	 */
	Collection<Variable> variablesWithScoperefs();

	/**
	 * A variable contains a pointer type if it is of type PointerType, if it is an
	 * array with elements containing pointer type, or if it is a struct with fields
	 * containing pointer type.
	 * 
	 * @return A collection of the variables in this scope containing pointer types.
	 */
	Collection<Variable> variablesWithPointers();

	/**
	 * A variable whose type is not a primitive type.
	 * 
	 * @return A collection of the variables in this scope containing pointer types.
	 */
	Collection<Variable> varsNeedSymbolicConstant();

	/**
	 * @return The number of functions contained in this scope.
	 */
	int numFunctions();

	/**
	 * Adds a function to this scope.
	 * 
	 * @param function a non-null function to add
	 */
	void addFunction(CIVLFunction function);
	
	CIVLFunction getFunction(String name);

	/**
	 * Searches for a function with the given name in this scope and then ancestor
	 * scopes. Same as {@code getFunction(name.name())}.
	 * 
	 * @param name non-null identifier containing name to search for
	 * @return the first function found with given name (starting in this scope and
	 *         then moving up the ancestor path), or {@code null} if there is no
	 *         such function
	 */
	CIVLFunction seekFunction(Identifier name);

	/**
	 * Searches for a function with the given name in this scope and then ancestor
	 * scopes.
	 * 
	 * @param name non-null string, the name to search for
	 * @return the first function found with given name (starting in this scope and
	 *         then moving up the ancestor path), or {@code null} if there is no
	 *         such function
	 */
	CIVLFunction seekFunction(String name);

	CIVLFunction getFunction(int fid);

	void complete();

	/**
	 * Print the scope and all children.
	 * 
	 * @param prefix  String prefix to print on each line
	 * @param out     The PrintStream to use for printing.
	 * @param isDebug True iff the debugging option is enabled, when more
	 *                information will be printed, such as if a variable is purely
	 *                local
	 */
	void print(String prefix, PrintStream out, boolean isDebug);

}
