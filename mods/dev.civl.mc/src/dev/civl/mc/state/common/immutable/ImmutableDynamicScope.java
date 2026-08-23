package dev.civl.mc.state.common.immutable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Map;

import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.state.IF.DynamicScope;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.UnaryOperator;
import dev.civl.sarl.IF.expr.SymbolicExpression;

/**
 * An ImmutableDynamicScope represents the state of a single dynamic scope in
 * the state of a CIVL model. It is an instance of a static (lexical) scope. It
 * assigns to each variable in the static scope a value, which is a symbolic
 * expression of the appropriate type.
 * 
 * As the name suggests an ImmutableDynamicScope is immutable, at least in all
 * ways visible to the user.
 * 
 * Instances of this class may be "flyweighted" (see the Flyweight Pattern). The
 * methods {@link #makeCanonic(SymbolicUniverse)} and {@link #isCanonic()}
 * support this pattern.
 * 
 * @author Timothy K. Zirkel (zirkel)
 * @author Timothy J. McClory (tmcclory)
 * @author Stephen F. Siegel (siegel)
 * 
 */
public class ImmutableDynamicScope implements DynamicScope {

	/* *************************** Static Fields *************************** */

	/**
	 * If true, print debugging information.
	 */
	private final static boolean debug = false;

	/* ************************** Instance Fields ************************** */

	/**
	 * Is this instance the canonic representative of its equivalence class
	 * under the "equals" method?
	 */
	boolean canonic = false;

	/**
	 * If the hash code has been computed, it is cached here.
	 */
	private int hashCode = -1;

	/**
	 * Has the hash code been computed?
	 */
	private boolean hashed = false;

	/**
	 * Non-null static scope to which this dynamic scope is associated.
	 */
	private Scope lexicalScope;

	/**
	 * The dyscope ID of the parent of this dyscope in the dynamic scope tree,
	 * or -1 if this is the root (and therefore has no parent).
	 */
	private int parent;

	/**
	 * // * The identifier of the parent of this dynamic scope in the dynamic
	 * scope // * tree, or -1 if this is the root (and therefore has no parent).
	 * //
	 */
	// private int parentIdentifier;

	/**
	 * Sets of PIDs of processes that can "reach" this dynamic scope.
	 */
	private BitSet reachers;

	/**
	 * Values associated to the variables declared in the lexicalScope. This is
	 * a non-null array of symbolic expressions. The symbolic expression in
	 * position i is the value associated to variable i; it is non-null, but may
	 * be the symbolic expression NULL.
	 */
	private SymbolicExpression[] variableValues;

	/**
	 * This identifier is not part of the state. It is never renamed, helping to
	 * identify a specific dynamic scope when scopes get collected.
	 */
	private int identifier;

	/* **************************** Constructors *************************** */

	/**
	 * Constructs a new immutable dynamic scope with the given fields. No data
	 * is cloned---the given parameters become the fields of the new instance.
	 * 
	 * @param lexicalScope
	 *            the static scope of which this dynamic scope is an instance
	 * @param parent
	 *            the dyscope ID of the parent of this dynamic scope in the
	 *            dyscope tree
	 * @param parentIdentifier
	 *            the identifier of the parent scope
	 * @param variableValues
	 *            the array of values associated to the variables declared in
	 *            the static scope
	 * @param reachers
	 *            the set of PIDs of processes that can reach this dyscope
	 * @param identifier
	 *            the identifier of this dyscope
	 */
	ImmutableDynamicScope(int identifier, Scope lexicalScope, int parent, // int
															// parentIdentifier,
			SymbolicExpression[] variableValues, BitSet reachers) {
		assert variableValues != null
				&& variableValues.length == lexicalScope.numVariables();
		this.lexicalScope = lexicalScope;
		this.parent = parent;
		this.variableValues = variableValues;
		this.reachers = reachers;
		this.identifier = identifier;
		// this.parentIdentifier = parentIdentifier;
	}

	/* ********************** Package-private Methods ********************** */

	/**
	 * Returns a new immutable dynamic scope which is equivalent to this one,
	 * except that the parent field is replaced by the given value.
	 * 
	 * @param parent
	 *            new value for the parent field
	 * @param parentIdentifer
	 *            the identifier of the new parent
	 * @return new instance same as original but with new parent value
	 */
	ImmutableDynamicScope setParent(int parent) { // ,int parentIdentifer) {
		return parent == this.parent
				? this
				: new ImmutableDynamicScope(identifier,lexicalScope, parent,
						variableValues, reachers);
	}

	/**
	 * Returns a new immutable dynamic scope which is equivalent to this one,
	 * except that the reachers field is replaced by the given value.
	 * 
	 * @param reachers
	 *            new value for the reachers field
	 * @return new instance same as original but with new reachers value
	 */
	ImmutableDynamicScope setReachers(BitSet reachers) {
		return new ImmutableDynamicScope(identifier,lexicalScope, parent, variableValues,
				reachers);
	}

	/**
	 * How many processes can reach this dynamic scope? A process p can reach a
	 * dynamic scope d iff there is a path starting from a dynamic scope which
	 * is referenced in a frame on p's call stack to d, following the "parent"
	 * edges in the scope tree.
	 * 
	 * @return the number of processes which can reach this dynamic scope
	 */
	int numberOfReachers() {
		return reachers.cardinality();
	}

	/**
	 * Is this dynamic scope reachable by the process with the given PID?
	 * 
	 * @param pid
	 *            the PID of the process to be checked
	 * @return true iff this dynamic scope is reachable from the process with
	 *         pid PID
	 */
	boolean reachableByProcess(int pid) {
		return reachers.get(pid);
	}

	/**
	 * Returns a new immutable dynamic scope which is equivalent to this one,
	 * except that the variableValues field is replaced by the given value.
	 * 
	 * @param newVariableValues
	 *            new value for the variableValues field
	 * @return new instance same as original but with new variableValues value
	 */
	ImmutableDynamicScope setVariableValues(
			SymbolicExpression[] newVariableValues) {
		return new ImmutableDynamicScope(identifier,lexicalScope, parent,
				newVariableValues, reachers);
	}

	/**
	 * Returns the number of variables in this dynamic scope; this is the same
	 * as the number in the associated static scope; this is just provided for
	 * convenience.
	 * 
	 * @return number of variables in this scope
	 */
	int numberOfVariables() {
		return variableValues.length;
	}

	/**
	 * Returns a copy of the variable values.
	 * 
	 * @return a copy of the variable values
	 */
	SymbolicExpression[] copyValues() {
		SymbolicExpression[] newValues = new SymbolicExpression[variableValues.length];

		System.arraycopy(variableValues, 0, newValues, 0,
				variableValues.length);
		return newValues;
	}

	/**
	 * Is this instance the unique representative of its equivalence class?
	 * 
	 * @return true iff this is the canonic representative of its equivalence
	 *         class
	 */
	boolean isCanonic() {
		return canonic;
	}

	/**
	 * Prints detailed representation of this dynamic scope.
	 * 
	 * @param out
	 *            print stream to which output should be sent
	 * @param id
	 *            a name to use for this dynamic scope in the print out (e.g.,
	 *            an ID number)
	 * @param prefix
	 *            a string to prepend to each line of output
	 */
	void print(PrintStream out, String id, String prefix) {
		int numVars = lexicalScope.numVariables();
		int bitSetLength = reachers.length();
		boolean first = true;

		out.println(prefix + "dyscope d" + id + "(parent=d" + parent
				+ ", static=" + lexicalScope.id() + ")");
		out.print(prefix + "| reachers = {");
		for (int j = 0; j < bitSetLength; j++) {
			if (reachers.get(j)) {
				if (first)
					first = false;
				else
					out.print(",");
				out.print(j);
			}
		}
		out.println("}");
		out.println(prefix + "| variables");
		for (int i = 0; i < numVars; i++) {
			Variable variable = lexicalScope.getVariable(i);
			SymbolicExpression value = variableValues[i];

			out.print(prefix + "| | " + variable.name() + " = ");
			if (debug)
				out.println(value.toStringBufferLong());
			else
				out.println(value);
		}
		out.flush();
	}

	/**
	 * Update the current dyscope using the given dyscope map, including
	 * updating the parent dyscope ID, and any variable that contains a scope
	 * value.
	 * 
	 * @param scopeSubMap
	 *            The map of new dyscope ID's: from old dyscope ID to new
	 *            dyscope ID. A value of -1 means the key dyscope has been
	 *            removed.
	 * @param universe
	 *            The symbolic universe to be used to perform the update on
	 *            variable values.
	 * @param newParentId
	 *            The new parent ID of this dyscope.
	 * @param newParentIdentifier
	 *            The identifier of the new parent.
	 * @return A new instance of dyscope with its parent changed and variable
	 *         values changed according to the given dyscope map.
	 */
	ImmutableDynamicScope updateDyscopeIds(
			UnaryOperator<SymbolicExpression> substituter,
			SymbolicUniverse universe, int newParentId) {
		Collection<Variable> scopeVariableIter = lexicalScope
				.variablesWithScoperefs();
		SymbolicExpression[] newValues = null;

		for (Variable variable : scopeVariableIter) {
			int vid = variable.vid();
			SymbolicExpression oldValue = variableValues[vid];

			if (oldValue != null && !oldValue.isNull()) {
				SymbolicExpression newValue = substituter.apply(oldValue);

				if (oldValue != newValue) {
					if (newValues == null)
						newValues = copyValues();
					newValues[vid] = newValue;
				}
			}
		}
		return newValues == null
				? setParent(newParentId)
				: new ImmutableDynamicScope(identifier, lexicalScope, newParentId,
						newValues, reachers);
	}

	ImmutableDynamicScope updateHeapPointers(
			Map<SymbolicExpression, SymbolicExpression> oldToNewExpression,
			SymbolicUniverse universe) {
		Collection<Variable> pointerVariableIter = lexicalScope
				.variablesWithPointers();
		SymbolicExpression[] newValues = null;

		// update pointers
		if (oldToNewExpression.size() > 0) {
			UnaryOperator<SymbolicExpression> substituter = universe
					.mapSubstituter(oldToNewExpression);

			for (Variable variable : pointerVariableIter) {
				int vid = variable.vid();
				SymbolicExpression oldValue = variableValues[vid];

				if (oldValue != null && !oldValue.isNull()) {
					SymbolicExpression newValue = substituter.apply(oldValue);

					if (oldValue != newValue) {
						if (newValues == null)
							newValues = copyValues();
						newValues[vid] = newValue;
					}
				}
			}
		}
		return newValues == null
				? this
				: new ImmutableDynamicScope(identifier, lexicalScope, this.parent,
						newValues, reachers);
	}

	ImmutableDynamicScope updateSymbolicConstants(
			UnaryOperator<SymbolicExpression> substituter) {
		SymbolicExpression[] newValues = null;

		for (Variable variable : this.lexicalScope.variables()) {
			int vid = variable.vid();
			SymbolicExpression oldValue = variableValues[vid];

			if (oldValue != null && !oldValue.isNull()) {
				SymbolicExpression newValue = substituter.apply(oldValue);

				if (oldValue != newValue) {
					if (newValues == null)
						newValues = copyValues();
					newValues[vid] = newValue;
				}
			}
		}
		return newValues == null
				? this
				: new ImmutableDynamicScope(identifier, lexicalScope, this.parent,
						newValues, reachers);
	}
	
	/* ************************* Methods from Object *********************** */

	/**
	 * Determines if the given object is equal to this one. Returns true iff:
	 * (1) obj is an instance of ImmutableDynamicScope, (2) the lexical scopes
	 * are equal, (3) the parent fields are equal, (4) the variableValues arrays
	 * have the same length and corresponding values are equal, and (5) the
	 * reachers sets are equal.
	 * 
	 * This implementation is optimized by taking advantage of immutability and
	 * flyweighting.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof ImmutableDynamicScope) {
			ImmutableDynamicScope that = (ImmutableDynamicScope) obj;

			if (canonic && that.canonic)
				return false;
			if (hashed && that.hashed && hashCode != that.hashCode)
				return false;
			return lexicalScope == that.lexicalScope && parent == that.parent
					&& Arrays.equals(variableValues, that.variableValues)
					&& reachers.equals(that.reachers);
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (!hashed) {
			hashCode = lexicalScope.hashCode() ^ (1017 * parent)
					^ Arrays.hashCode(variableValues) ^ reachers.hashCode();
			hashed = true;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return "[static=" + lexicalScope.id() + ", parent=d" + parent + "]";
	}

	/* ********************** Methods from DynamicScope ******************** */

	@Override
	public int getParent() {
		return parent;
	}

	@Override
	public BitSet getReachers() {
		return reachers;
	}

	@Override
	public SymbolicExpression getValue(int vid) {
		return variableValues[vid];
	}

	@Override
	public Iterable<SymbolicExpression> getValues() {
		return Arrays.asList(variableValues);
	}

	public int identifier() {
		return identifier;
	}

	@Override
	public Scope lexicalScope() {
		return lexicalScope;
	}

	@Override
	public void print(PrintStream out, String prefix) {
		print(out, "", prefix);
	}

	@Override
	public ImmutableDynamicScope setValue(int vid, SymbolicExpression value) {
		int n = numberOfVariables();
		SymbolicExpression[] newVariableValues = new SymbolicExpression[n];

		System.arraycopy(variableValues, 0, newVariableValues, 0, n);
		newVariableValues[vid] = value;
		return new ImmutableDynamicScope(identifier, lexicalScope, parent,
				newVariableValues, reachers);
	}

	@Override
	public int numberOfValues() {
		return this.variableValues.length;
	}

	// @Override
	// public String name() {
	// return "d" + identifier;
	// }

	/* ************************ Other Public Methods *********************** */
	// none
}
