/**
 * 
 */
package dev.civl.mc.state.common.immutable;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;

import dev.civl.mc.dynamic.IF.DynamicMemoryLocationSet;
import dev.civl.mc.model.IF.ModelConfiguration;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.state.IF.DynamicScope;
import dev.civl.mc.state.IF.ProcessState;
import dev.civl.mc.state.IF.State;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;

/**
 * Implementation of State based on the Immutable Pattern. This class is not
 * entirely immutable; it has certain fields such as {@link #stackPosition} and
 * {@link #depth} used by the depth first search algorithm which can be
 * modified. But it has an "observationally immutable core" consisting of the
 * path condition, dynamic scopes, and process states. While these can also
 * change in certain restricted ways, these changes are not visible to a user
 * going through the State interface.
 * 
 * The path condition and equals methods are based solely on the observationally
 * immutable core.
 * 
 * @author Stephen F. Siegel (siegel)
 * @author Timothy K. Zirkel (zirkel)
 * @author Tim McClory (tmcclory)
 * @author Ziqing Luo (ziqing)
 * @author Yihao Yan (yanyihao)
 * 
 */
public class ImmutableState implements State {

	/**
	 * A simple class implementing Iterable, backed by the array of process
	 * states. It is needed because this class must implement a method to return
	 * an Iterable over ProcessState. We have a field which is an array of
	 * ImmutableProcessState. This is the easiest way to get an Iterable of the
	 * right type. Only one needs to be created, so once it is created it is
	 * cached. (Due to Immutable Pattern.)
	 * 
	 * @author siegel
	 * 
	 */
	class ProcessStateIterable implements Iterable<ProcessState> {

		class ProcessStateIterator implements Iterator<ProcessState> {

			int pos = 0;

			@Override
			public boolean hasNext() {
				return pos < processStates.length;
			}

			@Override
			public ProcessState next() {
				ProcessState result = processStates[pos];

				pos++;
				return result;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}

		@Override
		public Iterator<ProcessState> iterator() {
			return new ProcessStateIterator();
		}
	}

	/* *************************** Static Fields *************************** */

	/**
	 * The number of instances of this class that have been created since the
	 * class was loaded.
	 */
	static long instanceCount = 0;

	/* ************************** Instance Fields ************************** */

	/**
	 * The path condition, a non-null boolean-valued symbolic expression.
	 * 
	 */
	protected BooleanExpression pathCondition;

	/**
	 * processes[i] contains the process of pid i. some entries may be null.
	 */
	protected ImmutableProcessState[] processStates;

	/**
	 * The dynamic scopes. The scope at position i is the dynamic scope with
	 * dyscopeId i. The scope at index 0 is always the system scope.
	 */
	protected ImmutableDynamicScope[] dyscopes;

	/**
	 * If the hashcode has been computed, it is cached here.
	 */
	private int hashCode = -1;

	/**
	 * Has the hashcode on this state already been computed?
	 */
	private boolean hashed = false;

	/**
	 * The absolutely unique ID number of this state, among all states ever
	 * created in this run of the JVM.
	 */
	private final long instanceId = instanceCount++;

	/**
	 * The iterable object over the process states, created once and cached here
	 * for future use.
	 */
	private Iterable<ProcessState> processStateIterable = null;

	/**
	 * The cached hash code of the array of process states.
	 */
	private int procHashCode = -1;

	/**
	 * Has the hash code of the process state array been computed and cached?
	 */
	private boolean procHashed = false;

	/**
	 * The cached hash code of the array of dynamic scopes.
	 */
	private int scopeHashCode = -1;

	/**
	 * Has the hash code of the dynamic scope array been computed and cached?
	 */
	private boolean scopeHashed = false;

	/**
	 * Cached reference to the simplified version of this state.
	 */
	ImmutableState simplifiedState = null;

	int[] collectibleCounts;

	/* *************************** Static Methods ************************** */

	/**
	 * This is a convenience method for constructing a new state from an old
	 * state where any number of the fields may have changed. Any or all of the
	 * three field arguments (processStates, dyscopes, pathCondition) may be
	 * null; a null entry indicates that the field has not changed, so the old
	 * value from the old state should be used in constructing the new state.
	 * 
	 * @param state
	 *            the old state
	 * @param processStates
	 *            new value for processStates field or null if the old value
	 *            should be re-used
	 * @param dyscopes
	 *            new value for the dyscopes field or null if the old value
	 *            should be re-used
	 * @param pathCondition
	 *            new value for the path condition or null if the old one should
	 *            be re-used
	 * @param collectibleCounts
	 *            the counts of collectible symbolic constants the name of which
	 *            have the prefix from
	 *            {@link ModelConfiguration#SYMBOL_PREFIXES}
	 * @return new ImmutableState with fields as specified
	 */
	static ImmutableState newState(ImmutableState state,
			ImmutableProcessState[] processStates,
			ImmutableDynamicScope[] dyscopes, BooleanExpression pathCondition) {
		ImmutableState result = new ImmutableState(
				processStates == null ? state.processStates : processStates,
				dyscopes == null ? state.dyscopes : dyscopes,
				pathCondition == null ? state.pathCondition : pathCondition);

		if (processStates == null && state.procHashed) {
			result.procHashed = true;
			result.procHashCode = state.procHashCode;
		}
		if (dyscopes == null && state.scopeHashed) {
			result.scopeHashed = true;
			result.scopeHashCode = state.scopeHashCode;
		}
		result.collectibleCounts = state.collectibleCounts;
		return result;
	}

	/* **************************** Constructors *************************** */

	/**
	 * Constructs new ImmutableState. The arrays are used as fields---the
	 * elements are not copied into a new array. All arguments must be non-null.
	 * Seen and onStack bits are set to false.
	 * 
	 * @param processStates
	 *            the array of process states, with the element in position i
	 *            being the state of process with PID i; entries may be null
	 * @param dyscopes
	 *            array of dynamic scopes, with element in position i being the
	 *            dynamic scope with dyscopeId i
	 * @param pathCondition
	 *            the path condition, a boolean-valued symbolic expression which
	 *            is assumed to hold in this state
	 */
	ImmutableState(ImmutableProcessState[] processStates,
			ImmutableDynamicScope[] dyscopes, BooleanExpression pathCondition) {
		assert processStates != null;
		assert dyscopes != null;
		assert pathCondition != null;
		this.processStates = processStates;
		this.dyscopes = dyscopes;
		this.pathCondition = pathCondition;
	}

	/* *************************** Private Methods ************************* */

	/**
	 * Implements the flyweight pattern for ImmutableDynamicScopes: if there
	 * already exists a dyscope which is equivalent to the given dyscope, return
	 * that one, otherwise, add the dyscope to the table and return it.
	 * 
	 * This method goes into the dyscope and replaces each individual symbolic
	 * expression with the canonic version of that symbolic expression.
	 * 
	 * @param dyscope
	 *            the dyscope to be flyweighted
	 * @param scopeMap
	 *            the map to use for flyweighting in which the key-value pairs
	 *            have the form (X,X) for all canonic objects X
	 * @return the unique representative of the dyscope's equivalence class
	 */
	private ImmutableDynamicScope canonic(ImmutableDynamicScope dyscope,
			Map<ImmutableDynamicScope, ImmutableDynamicScope> scopeMap,
			SymbolicUniverse universe) {
		ImmutableDynamicScope canonicScope = scopeMap.get(dyscope);

		if (canonicScope == null) {
			canonicScope = scopeMap.putIfAbsent(dyscope, dyscope);
			return canonicScope == null ? dyscope : canonicScope;
		}
		return canonicScope;
	}

	/**
	 * Implements the flyweight pattern for ImmutableProcessState: if there
	 * already exists a process state which is equivalent to the given one,
	 * return that one, otherwise, add the process state to the table and return
	 * it.
	 * 
	 * @param processState
	 *            the process state to be flyweighted
	 * @param scopeMap
	 *            the map to use for flyweighting in which the key-value pairs
	 *            have the form (X,X) for all canonic objects X
	 * @return the unique representative of the process state's equivalence
	 *         class
	 */
	private ImmutableProcessState canonic(ImmutableProcessState processState,
			Map<ImmutableProcessState, ImmutableProcessState> processMap,
			SymbolicUniverse universe) {
		ImmutableProcessState canonicProcessState = processMap
				.get(processState);

		if (canonicProcessState == null) {
			processState.makeCanonic();
			canonicProcessState = processMap.putIfAbsent(processState,
					processState);
			return canonicProcessState == null
					? processState
					: canonicProcessState;
		}
		return canonicProcessState;
	}

	/**
	 * Prints a dyscope of a given id of this state to the given print stream.
	 * 
	 * @param out
	 *            The print stream to be used.
	 * @param dyscope
	 *            The dyscope to be printed.
	 * @param id
	 *            The id of the dyscope to be printed.
	 * @param prefix
	 *            The line prefix of the printing result.
	 */
	private void printImmutableDynamicScope(PrintStream out,
			ImmutableDynamicScope dyscope, String id, String prefix) {
		Scope lexicalScope = dyscope.lexicalScope();
		int numVars = lexicalScope.numVariables();
		BitSet reachers = dyscope.getReachers();
		int bitSetLength = reachers.length();
		boolean first = true;

		out.println(prefix + "dyscope d" + id + " (parent ID="
				+ dyscope.getParent() + ", static=" + lexicalScope.id() + ")");
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
			SymbolicExpression value = dyscope.getValue(i);

			out.print(prefix + "| | " + variable.name() + " = ");
			if (variable.type().isPointerType()) {
				out.println(this.pointerValueToString(value));
			} else
				out.println(value);
		}
		out.flush();
	}

	/**
	 * Obtains the string representation of a pointer value.
	 * 
	 * @param pointer
	 *            The pointer value whose string representation is to be
	 *            generated.
	 * @return The string representation of the given pointer value.s
	 */
	private String pointerValueToString(SymbolicExpression pointer) {
		StringBuffer result = new StringBuffer();

		if (pointer.operator() == SymbolicOperator.NULL)
			return pointer.toString();
		else {
			result.append('&');
			return result.toString();
		}
	}

	/* *********************** Package-private Methods ********************* */

	/**
	 * Makes this state canonic. Recursively makes the path condition, dynamic
	 * scopes, and process states canonic, then applies the flyweight pattern to
	 * this state.
	 * 
	 * @param canonicId
	 *            the canonic ID to assign to this state
	 * @param universe
	 *            the symbolic universe used to canonize symbolic expressions
	 * @param scopeMap
	 *            the map used to flyweight dynamic scopes
	 * @param processMap
	 *            the map used to flyweight process states
	 */
	void makeCanonic(SymbolicUniverse universe,
			Map<ImmutableDynamicScope, ImmutableDynamicScope> scopeMap,
			Map<ImmutableProcessState, ImmutableProcessState> processMap) {
		int numProcs = processStates.length;
		int numScopes = dyscopes.length;

		for (int i = 0; i < numProcs; i++) {
			ImmutableProcessState processState = processStates[i];

			if (processState != null && !processState.isCanonic())
				processStates[i] = canonic(processState, processMap, universe);
		}
		for (int i = 0; i < numScopes; i++) {
			ImmutableDynamicScope scope = dyscopes[i];

			if (!scope.isCanonic())
				dyscopes[i] = canonic(scope, scopeMap, universe);
		}
	}

	/**
	 * Creates a shallow copy of the process state array with one additional
	 * null entry, and returns it.
	 * 
	 * @return an array one longer than the process state array with entry i
	 *         containing process state i for all but the last entry, which is
	 *         null
	 */
	ImmutableProcessState[] copyAndExpandProcesses() {
		ImmutableProcessState[] newProcesses = new ImmutableProcessState[processStates.length
				+ 1];

		System.arraycopy(processStates, 0, newProcesses, 0,
				processStates.length);
		return newProcesses;
	}

	/**
	 * Creates a shallow copy of the dynamic scope array with one additional
	 * null entry, and returns it.
	 * 
	 * @return an array one longer than the dynamic scope array with entry i
	 *         containing dynamic scope i for all but the last entry, which is
	 *         null
	 */
	ImmutableDynamicScope[] copyAndExpandScopes() {
		ImmutableDynamicScope[] newScopes = new ImmutableDynamicScope[dyscopes.length
				+ 1];

		System.arraycopy(dyscopes, 0, newScopes, 0, dyscopes.length);
		return newScopes;
	}

	/**
	 * Returns a shallow copy of the process state array.
	 * 
	 * @return a shallow copy of the process state array
	 */
	ImmutableProcessState[] copyProcessStates() {
		ImmutableProcessState[] newProcesses = new ImmutableProcessState[processStates.length];

		System.arraycopy(processStates, 0, newProcesses, 0,
				processStates.length);
		return newProcesses;
	}

	/**
	 * Returns a shallow copy of the dynamic scope array.
	 * 
	 * @return a shallow copy of the dynamic scope array
	 */
	ImmutableDynamicScope[] copyScopes() {
		ImmutableDynamicScope[] newScopes = new ImmutableDynamicScope[dyscopes.length];

		System.arraycopy(dyscopes, 0, newScopes, 0, dyscopes.length);
		return newScopes;
	}

	/**
	 * Finds the dynamic scope containing the given variable. The search begins
	 * in the current dynamic scope of process pid (i.e., the dyscope at the top
	 * of that process' call stack). If the variable is not found there, it then
	 * moves to the parent dyscope, and so on. If the variable is not found
	 * after looking in the root dyscope, an exception is thrown.
	 * 
	 * @param pid
	 *            the PID of the process containing the variable
	 * @param variable
	 *            the static variable
	 * @return the "innermost" dynamic scope containing the variable
	 */
	ImmutableDynamicScope getScope(int pid, Variable variable) {
		ImmutableProcessState proc = this.getProcessState(pid);
		int numStackEntries = proc.stackSize();
		Scope variableScope = variable.scope();
		ImmutableDynamicScope scope;

		for (int i = 0; i < numStackEntries; i++) {
			int scopeId = proc.getStackEntry(i).scope();

			while (scopeId >= 0) {
				scope = getDyscope(scopeId);
				if (scope.lexicalScope() == variableScope)
					return scope;
				scopeId = getParentId(scopeId);
			}
		}
		throw new IllegalArgumentException(
				"Variable not in scope: " + variable);
	}

	/**
	 * Returns a new state equivalent to this one, except that the dyscopes
	 * field is replaced with the given parameter.
	 * 
	 * @param dyscopes
	 *            new value of dyscopes array
	 * @return new state with new dyscopes
	 */
	ImmutableState setScopes(ImmutableDynamicScope[] dyscopes) {
		ImmutableState result = new ImmutableState(processStates, dyscopes,
				pathCondition);

		if (procHashed) {
			result.procHashed = true;
			result.procHashCode = procHashCode;
		}
		result.collectibleCounts = this.collectibleCounts;
		return result;
	}

	/**
	 * Returns a new state equivalent to this one, except that the process state
	 * of PID index is replaced with the given process state.
	 * 
	 * Precondition: index == processState.pid()
	 * 
	 * @param processState
	 *            new value for process state of PID index
	 * @param index
	 *            PID of process
	 * @return new state with new process state
	 */
	ImmutableState setProcessState(int index,
			ImmutableProcessState processState) {
		int n = processStates.length;
		ImmutableProcessState[] newProcessStates = new ImmutableProcessState[n];
		ImmutableState result;

		System.arraycopy(processStates, 0, newProcessStates, 0, n);
		newProcessStates[index] = processState;
		result = new ImmutableState(newProcessStates, dyscopes, pathCondition);
		if (scopeHashed) {
			result.scopeHashed = true;
			result.scopeHashCode = scopeHashCode;
		}
		result.collectibleCounts = this.collectibleCounts;
		return result;
	}

	/**
	 * Returns a new state in which the process state array field has been
	 * replaced by the given array.
	 * 
	 * @param processStates
	 *            new value for process states field
	 * @return new immutable state with process states field as given
	 */
	ImmutableState setProcessStates(ImmutableProcessState[] processStates) {
		ImmutableState result = new ImmutableState(processStates, dyscopes,
				pathCondition);

		if (scopeHashed) {
			result.scopeHashed = true;
			result.scopeHashCode = scopeHashCode;
		}
		result.collectibleCounts = this.collectibleCounts;
		return result;
	}

	/**
	 * Updates the count of the collectible symbolic constant of the given index
	 * 
	 * @param index
	 *            the index of the count to be updated
	 * @param newCount
	 *            the new count of the given index
	 * @return the new state which is identical to this state except that the
	 *         collectible count of the given index is updated
	 */
	ImmutableState updateCollectibleCount(int index, int newCount) {
		int length = this.collectibleCounts.length;
		int[] newCollectibleCounts = new int[length];
		ImmutableState newState = newState(this, processStates, dyscopes,
				pathCondition);

		System.arraycopy(this.collectibleCounts, 0, newCollectibleCounts, 0,
				length);
		newCollectibleCounts[index] = newCount;
		newState.collectibleCounts = newCollectibleCounts;
		return newState;
	}

	/**
	 * <p>
	 * Set new path condition on this state, returns a new state who has the new
	 * path condition against this one.
	 * </p>
	 * 
	 * @param newPathCondition
	 *            A boolean-value symbolic expression.
	 * @return A new state who has the new path condition against this one.
	 */
	ImmutableState setPermanentPathCondition(
			BooleanExpression newPathCondition) {
		ImmutableState result = new ImmutableState(processStates, dyscopes,
				newPathCondition);

		if (scopeHashed) {
			result.scopeHashed = true;
			result.scopeHashCode = scopeHashCode;
		}
		if (procHashed) {
			result.procHashed = true;
			result.procHashCode = procHashCode;
		}
		result.collectibleCounts = this.collectibleCounts;
		return result;
	}

	/**
	 * Set the partial path condition stack of the given process to the new one.
	 * 
	 * @param pid
	 *            The PID of the process who owns the stack.
	 * @param newPpcStack
	 *            The new stack.
	 * @return A new state in which the corresponding process state has changed.
	 */
	ImmutableState setPartialPathConditionStack(int pid,
			BooleanExpression newPpcStack[]) {
		ImmutableProcessState process = processStates[pid];
		ImmutableProcessState newProcessStates[] = Arrays.copyOf(processStates,
				processStates.length);

		newProcessStates[pid] = process.setPartialPathConditions(newPpcStack);
		return newState(this, newProcessStates, dyscopes, pathCondition);
	}

	/**
	 * @param pid
	 *            The PID of the process who owns the stack.
	 * @return The partial path condition stack of the given process.
	 */
	BooleanExpression[] copyOfPartialPathConditionStack(int pid) {
		BooleanExpression ppcs[] = processStates[pid]
				.getPartialPathConditions();

		return Arrays.copyOf(ppcs, ppcs.length);
	}

	/**
	 * Set the write set stack of the given process to the new one.
	 * 
	 * @param pid
	 *            The PID of the process who owns the stack.
	 * @param newWriteSetStack
	 *            The new stack.
	 * @return A new state in which the corresponding process state has changed.
	 */
	ImmutableState setWriteSetStack(int pid,
			DynamicMemoryLocationSet newWriteSetStack[]) {
		ImmutableProcessState process = processStates[pid];
		ImmutableProcessState newProcessStates[] = Arrays.copyOf(processStates,
				processStates.length);

		newProcessStates[pid] = process.setWriteSets(newWriteSetStack);
		return newState(this, newProcessStates, dyscopes, pathCondition);
	}

	/**
	 * Set the read set stack of the given process to the new one.
	 * 
	 * @param pid
	 *            The PID of the process who owns the stack.
	 * @param newReadSetStack
	 *            The new stack.
	 * @return A new state in which the corresponding process state has changed.
	 */
	ImmutableState setReadSetStack(int pid,
			DynamicMemoryLocationSet newReadSetStack[]) {
		ImmutableProcessState process = processStates[pid];
		ImmutableProcessState newProcessStates[] = Arrays.copyOf(processStates,
				processStates.length);

		newProcessStates[pid] = process.setReadSets(newReadSetStack);
		return newState(this, newProcessStates, dyscopes, pathCondition);
	}

	/* ************************ Methods from State ************************* */

	@Override
	public int getParentId(int scopeId) {
		return getDyscope(scopeId).getParent();
	}

	@Override
	public BooleanExpression getPathCondition(SymbolicUniverse universe) {
		BooleanExpression pc = pathCondition;

		for (ImmutableProcessState procState : processStates) {
			if (procState == null)
				continue;

			BooleanExpression ppcs[] = procState.getPartialPathConditions();

			for (int i = 0; i < ppcs.length; i++)
				pc = universe.and(pc, ppcs[i]);
		}
		return pc;
	}
	
	@Override
	public BooleanExpression getPermanentPathCondition() {
		return pathCondition;
	}

	@Override
	public ImmutableProcessState getProcessState(int pid) {
		return processStates[pid];
	}

	@Override
	public synchronized Iterable<ProcessState> getProcessStates() {
		if (processStateIterable == null) {
			processStateIterable = new ProcessStateIterable();
		}
		return processStateIterable;
	}

	@Override
	public ImmutableDynamicScope getDyscope(int id) {
		return dyscopes[id];
	}

	@Override
	public int getDyscopeID(int pid, Variable variable) {
		Scope variableScope = variable.scope();

		if (variableScope.id() == ModelConfiguration.STATIC_CONSTANT_SCOPE) {
			return ModelConfiguration.DYNAMIC_CONSTANT_SCOPE;
		}

		int scopeId = getProcessState(pid).getDyscopeId();
		DynamicScope scope;

		while (scopeId >= 0) {
			scope = getDyscope(scopeId);
			if (scope.lexicalScope() == variableScope)
				return scopeId;
			scopeId = getParentId(scopeId);
		}
		return ModelConfiguration.DYNAMIC_NULL_SCOPE;
	}

	@Override
	public SymbolicExpression getVariableValue(int scopeId, int variableId) {
		DynamicScope scope = getDyscope(scopeId);

		return scope.getValue(variableId);
	}

	@Override
	public int getDyscope(int pid, Scope scope) {
		return this.getDyscope(pid, scope.id());
	}

	@Override
	public int getDyscope(int pid, int scopeID) {
		if (scopeID == ModelConfiguration.STATIC_CONSTANT_SCOPE)
			return ModelConfiguration.DYNAMIC_CONSTANT_SCOPE;

		ImmutableProcessState proc = getProcessState(pid);
		int stackSize = proc.stackSize();
		int stackIndex = 0;
		int dyScopeId = proc.getStackEntry(stackIndex).scope();
		DynamicScope dyScope = this.getDyscope(dyScopeId);

		while (dyScope.lexicalScope().id() != scopeID) {
			dyScopeId = this.getParentId(dyScopeId);
			if (dyScopeId < 0) {
				stackIndex++;
				if (stackIndex >= stackSize)
					return ModelConfiguration.DYNAMIC_NULL_SCOPE;
				dyScopeId = proc.getStackEntry(stackIndex).scope();
			}
			dyScope = this.getDyscope(dyScopeId);
		}
		return dyScopeId;
	}

	@Override
	public String identifier() {
		return "(id=" + instanceId + ")";
	}

	@Override
	public int numberOfReachers(int sid) {
		return getDyscope(sid).numberOfReachers();
	}

	@Override
	public int numProcs() {
		return processStates.length;
	}

	@Override
	public int numLiveProcs() {
		int count = 0;

		for (ProcessState procState : processStates)
			if (procState != null)
				count++;
		return count;
	}

	@Override
	public int numDyscopes() {
		return dyscopes.length;
	}

	@Override
	public void print(PrintStream out) {
		int numScopes = numDyscopes();
		int numProcs = numProcs();

		out.print("State " + identifier());
		out.println();
		out.println("| Path condition");
		out.println("| | " + pathCondition);
		out.println("| Dynamic scopes");
		for (int i = 0; i < numScopes; i++) {
			ImmutableDynamicScope dyscope = (ImmutableDynamicScope) dyscopes[i];

			if (dyscope == null)
				out.println("| | dyscope - (id=" + i + "): null");
			else
				this.printImmutableDynamicScope(out, dyscope, "" + i, "| | ");
		}
		out.println("| Process states");
		for (int i = 0; i < numProcs; i++) {
			ProcessState process = processStates[i];

			if (process == null)
				out.println("| | process - (id=" + i + "): null");
			else
				process.print(out, "| | ");
		}
		out.flush();
	}

	@Override
	public boolean reachableByProcess(int sid, int pid) {
		return getDyscope(sid).reachableByProcess(pid);
	}

	@Override
	public int rootDyscopeID() {
		return 0;
	}

	@Override
	public SymbolicExpression valueOf(int pid, Variable variable) {
		DynamicScope scope = getScope(pid, variable);
		int variableID = scope.lexicalScope().getVid(variable);

		return scope.getValue(variableID);
	}

	/* ************************ Methods from Object ************************ */

	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (object instanceof ImmutableState) {
			ImmutableState that = (ImmutableState) object;

			if (that.instanceId == this.instanceId)
				return true;
			if (hashed && that.hashed && hashCode != that.hashCode)
				return false;
			if (!pathCondition.equals(that.pathCondition))
				return false;
			if (procHashed && that.procHashed
					&& procHashCode != that.procHashCode)
				return false;
			if (scopeHashed && that.scopeHashed
					&& scopeHashCode != that.scopeHashCode)
				return false;
			if (!Arrays.equals(processStates, that.processStates))
				return false;
			if (!Arrays.equals(dyscopes, that.dyscopes))
				return false;
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (!hashed) {
			if (!procHashed) {
				procHashCode = Arrays.hashCode(processStates);
				procHashed = true;
			}
			if (!scopeHashed) {
				scopeHashCode = Arrays.hashCode(dyscopes);
				scopeHashed = true;
			}
			hashCode = pathCondition.hashCode() ^ procHashCode ^ scopeHashCode;
			hashed = true;
		}
		return hashCode;
	}

	@Override
	public String toString() {
		return identifier();
	}

	@Override
	public StringBuffer callStackToString() {
		StringBuffer result = new StringBuffer();
		int numProcs = this.numProcs();

		result.append("\nCall stacks:\n");
		for (int i = 0; i < numProcs; i++) {
			ProcessState process = processStates[i];

			// result.append("\n");
			if (process != null)
				result.append(process.toSBrieftringBuffer());
		}
		return result;
	}

	@Override
	public SymbolicExpression[] getOutputValues(String[] outputNames) {
		DynamicScope rootDyscope = this.dyscopes[0];
		Scope rootScope = rootDyscope.lexicalScope();
		int numOutputs = outputNames.length;
		SymbolicExpression[] outputValues = new SymbolicExpression[numOutputs];

		for (int i = 0; i < numOutputs; i++) {
			Variable outputVariable = rootScope.getVariable(outputNames[i]);

			outputValues[i] = rootDyscope.getValue(outputVariable.vid());
		}
		return outputValues;
	}

	@Override
	public boolean isFinalState() {
		return processStates.length == 1 && processStates[0].hasEmptyStack();
	}

	@Override
	public boolean isMonitoringWrites(int pid) {
		return processStates[pid].getWriteSets(false).length > 0;
	}

	@Override
	public boolean isMonitoringReads(int pid) {
		return processStates[pid].getReadSets(false).length > 0;
	}
}
