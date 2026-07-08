package dev.civl.mc.state.common.immutable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.dynamic.IF.DynamicMemoryLocationSet;
import dev.civl.mc.dynamic.IF.DynamicMemoryLocationSetFactory;
import dev.civl.mc.dynamic.IF.Dynamics;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.model.IF.CIVLException;
import dev.civl.mc.model.IF.CIVLException.Certainty;
import dev.civl.mc.model.IF.CIVLFunction;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLProperty;
import dev.civl.mc.model.IF.CIVLTypeFactory;
import dev.civl.mc.model.IF.Model;
import dev.civl.mc.model.IF.ModelConfiguration;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.location.Location;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.state.IF.CIVLHeapException;
import dev.civl.mc.state.IF.CIVLHeapException.HeapErrorKind;
import dev.civl.mc.state.IF.DynamicScope;
import dev.civl.mc.state.IF.MemoryUnitFactory;
import dev.civl.mc.state.IF.ProcessState;
import dev.civl.mc.state.IF.StackEntry;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.StateFactory;
import dev.civl.mc.util.IF.Pair;
import dev.civl.mc.util.IF.Singleton;
import dev.civl.sarl.IF.CanonicalRenamer;
import dev.civl.sarl.IF.Reasoner;
import dev.civl.sarl.IF.SARLException;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.UnaryOperator;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.ReferenceExpression;
import dev.civl.sarl.IF.expr.SymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;
import dev.civl.sarl.IF.number.IntegerNumber;
import dev.civl.sarl.IF.object.IntObject;
import dev.civl.sarl.IF.object.StringObject;
import dev.civl.sarl.IF.object.SymbolicObject;
import dev.civl.sarl.IF.object.SymbolicObject.SymbolicObjectKind;
import dev.civl.sarl.IF.object.SymbolicSequence;
import dev.civl.sarl.IF.type.SymbolicCompleteArrayType;
import dev.civl.sarl.IF.type.SymbolicType;

/**
 * An implementation of StateFactory based on the Immutable Pattern.
 * 
 * @author Timothy K. Zirkel (zirkel)
 * @author Timothy J. McClory (tmcclory)
 * @author Stephen F. Siegel (siegel)
 * 
 */
public class ImmutableStateFactory implements StateFactory {

	/* ************************** Instance Fields ************************** */
	/**
	 * A reference to a reasoner whose context is literal true.
	 */
	private Reasoner trueContextReasoner = null;

	/**
	 * The number of instances of states that have been created.
	 */
	private long initialNumStateInstances = ImmutableState.instanceCount;

	/**
	 * The model factory.
	 */
	protected ModelFactory modelFactory;

	private CIVLTypeFactory typeFactory;

	/**
	 * The map of canonic process states. The key and the corresponding value should
	 * be the same, in order to allow fast checking of existence and returning the
	 * value.
	 */
	private Map<ImmutableProcessState, ImmutableProcessState> processMap = new ConcurrentHashMap<>(100000);

	/**
	 * The map of canonic dyscopes. The key and the corresponding value should be
	 * the same, in order to allow fast checking of existence and returning the
	 * value.
	 */
	private Map<ImmutableDynamicScope, ImmutableDynamicScope> scopeMap = new ConcurrentHashMap<>(100000);

	protected final SymbolicExpression undefinedProcessValue;

	/**
	 * the CIVL configuration specified by the comamnd line
	 */
	private CIVLConfiguration config;

	/**
	 * The unique symbolic expression for the null process value, which has the
	 * integer value -2.
	 */
	private final SymbolicExpression nullProcessValue;

	/**
	 * The list of canonicalized symbolic expressions of process IDs, will be used
	 * in Executor, Evaluator and State factory to obtain symbolic process ID's.
	 */
	private SymbolicExpression[] processValues;

	/**
	 * The max number of processes which can be specified through command line.
	 */
	private int maxProcs;

	/**
	 * Amount by which to increase the list of cached scope values and process
	 * values when a new value is requested that is outside of the current range.
	 */
	private final static int CACHE_INCREMENT = 10;

	/**
	 * Value of the identifier for the next Dynamic Scope that is created during a
	 * stack push.
	 */
	private int nextDyscopeId = 0;

	/**
	 * The unique symbolic expression for the undefined scope value, which has the
	 * integer value -1.
	 */
	private SymbolicExpression undefinedScopeValue;

	/**
	 * The unique symbolic expression for the null scope value, which has the
	 * integer value -2.
	 */
	private SymbolicExpression nullScopeValue;

	/** A list of nulls of length CACHE_INCREMENT */
	private List<SymbolicExpression> nullList = new LinkedList<SymbolicExpression>();

	/**
	 * The size of {@link #smallScopeValues}.
	 */
	private final int SCOPE_VALUES_INIT_SIZE = 500;

	/**
	 * The array which caches the canonicalized symbolic expression of small scope
	 * IDs which are less than {@link #SCOPE_VALUES_INIT_SIZE}.
	 */
	private SymbolicExpression[] smallScopeValues = new SymbolicExpression[500];

	/**
	 * The list of canonicalized symbolic expressions of scope IDs, will be used in
	 * Executor, Evaluator and State factory to obtain symbolic scope ID's.
	 * 
	 */
	private List<SymbolicExpression> bigScopeValues = new ArrayList<SymbolicExpression>();

	/**
	 * Class used to wrap integer arrays so they can be used as keys in hash maps.
	 * This is used to map dyscope ID substitution maps to SARL substituters, in
	 * order to reuse substituters when the same substitution map comes up again and
	 * again. Since the substituters cache their results, this has the potential to
	 * increase performance.
	 * 
	 * @author siegel
	 *
	 */
	private class IntArray {
		private int[] contents;

		public IntArray(int[] contents) {
			this.contents = contents;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof IntArray) {
				return Arrays.equals(contents, ((IntArray) obj).contents);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(contents);
		}
	}

	private Map<IntArray, UnaryOperator<SymbolicExpression>> dyscopeSubMap = new ConcurrentHashMap<>();

	/**
	 * The symbolic universe, provided by SARL.
	 */
	protected SymbolicUniverse universe;

	protected SymbolicUtility symbolicUtil;

	private ImmutableMemoryUnitFactory memUnitFactory;

	private ReservedConstant isReservedSymbolicConstant;

	private List<Variable> inputVariables;

	protected Set<HeapErrorKind> emptyHeapErrorSet = new HashSet<>(0);

	protected Set<HeapErrorKind> fullHeapErrorSet = new HashSet<>();

	/**
	 * An operator that converts a scope value which is an instance of
	 * {@link SymbolicExpression} to a dyscope ID in the form of
	 * {@link IntegerNumber}.
	 */
	private Function<SymbolicExpression, IntegerNumber> scopeValueToDyscopeID = null;

	/**
	 * An operator that converts a dyscope ID in the form {@link IntegerNumber} to a
	 * scope value which is an instance of {@link SymbolicExpression}.
	 */
	private Function<Integer, SymbolicExpression> dyscopeIDToScopeValue = null;

	/**
	 * A reference to {@link DynamicMemoryLocationSetFactory} which produces new
	 * instances of {@link DynamicMemoryLocationSet}
	 */
	private DynamicMemoryLocationSetFactory memoryLocationSetFactory;

	/**
	 * Will we do process canonicalization? Yes, unless we are doing preemption
	 * bounded search, or we are checking for fair termination.
	 */
	private boolean collectProcs = true;

	/* **************************** Constructors *************************** */

	/**
	 * Factory to create all state objects.
	 */
	public ImmutableStateFactory(ModelFactory modelFactory, MemoryUnitFactory memFactory, CIVLConfiguration config) {
		this.modelFactory = modelFactory;
		this.inputVariables = modelFactory.inputVariables();
		this.typeFactory = modelFactory.typeFactory();
		this.universe = modelFactory.universe();
		this.memUnitFactory = (ImmutableMemoryUnitFactory) memFactory;
		this.undefinedProcessValue = modelFactory.undefinedValue(typeFactory.processSymbolicType());
		isReservedSymbolicConstant = new ReservedConstant();
		this.config = config;
		this.nullProcessValue = universe.tuple(typeFactory.processSymbolicType(),
				new Singleton<SymbolicExpression>(universe.integer(-2)));
		this.maxProcs = config.getMaxProcs();
		this.processValues = new SymbolicExpression[maxProcs];
		for (HeapErrorKind kind : HeapErrorKind.class.getEnumConstants())
			fullHeapErrorSet.add(kind);
		for (int i = 0; i < maxProcs; i++) {
			processValues[i] = universe.tuple(typeFactory.processSymbolicType(),
					new Singleton<SymbolicExpression>(universe.integer(i)));
		}
		this.scopeValueToDyscopeID = typeFactory.scopeType().scopeValueToIdentityOperator(universe);
		this.dyscopeIDToScopeValue = typeFactory.scopeType().scopeIdentityToValueOperator(universe);
		this.undefinedScopeValue = dyscopeIDToScopeValue.apply(ModelConfiguration.DYNAMIC_UNDEFINED_SCOPE);
		this.nullScopeValue = dyscopeIDToScopeValue.apply(ModelConfiguration.DYNAMIC_NULL_SCOPE);
		for (int i = 0; i < SCOPE_VALUES_INIT_SIZE; i++) {
			smallScopeValues[i] = dyscopeIDToScopeValue.apply(i);
		}
		for (int i = 0; i < CACHE_INCREMENT; i++)
			nullList.add(null);
		this.trueContextReasoner = universe.reasoner(universe.trueExpression());
		memoryLocationSetFactory = Dynamics.newDynamicMemoryLocationSetFactory(universe, typeFactory,
				this.nullScopeValue);
		this.collectProcs = config.preemptionBound() < 0
				&& !(config.isToggleableProperty(CIVLProperty.TERMINATION) && config.isFair());
	}

	/* ********************** Methods from StateFactory ******************** */

	@Override
	public ImmutableState addProcess(State state, CIVLFunction function, SymbolicExpression[] arguments, int callerPid,
			boolean selfDestructable) {
		ImmutableState theState = createNewProcess(state, selfDestructable);

		return pushCallStack2(theState, state.numProcs(), function, function.outerScope(), -1, arguments, callerPid);
	}

	@Override
	public State addProcess(State state, CIVLFunction function, int functionParentDyscope,
			SymbolicExpression[] arguments, int callerPid, boolean selfDestructable) {
		ImmutableState theState = createNewProcess(state, selfDestructable);

		return pushCallStack2(theState, state.numProcs(), function, function.outerScope(), functionParentDyscope,
				arguments, callerPid);
	}

	@Override
	public ImmutableState canonic(State state, boolean collectProcesses, boolean collectScopes, boolean collectHeaps,
			boolean collectSymbolicConstants, boolean simplify, Set<HeapErrorKind> toBeIgnored)
			throws CIVLHeapException {
		return canonicWork(state, collectProcesses, collectScopes, collectHeaps, collectSymbolicConstants, simplify,
				toBeIgnored);
	}

	/**
	 * <p>
	 * In this implementation of canonic: process states are collected, heaps are
	 * collected, dynamic scopes are collected, the flyweight representative is
	 * taken, simplify is called if that option is selected, then the flyweight
	 * representative is taken again.
	 * </p>
	 * 
	 * 
	 * @param state            The state that will be canonicalized
	 * @param collectProcesses true to collect process states in the state during
	 *                         canonicalization.
	 * @param collectScopes    true to collect dynamic scopes in the state during
	 *                         canonicalization.
	 * @param collectHeaps     true to collect memory heaps in the state during
	 *                         canonicalization.
	 * @param toBeIgnored      A set of {@link HeapErrorKind}s which will be
	 *                         supressed during heap collection.
	 * @param isReferredState
	 *                         <p>
	 *                         True if and only if the given state is a referred
	 *                         state, i.e. it is referred by a variable in current
	 *                         main state (currently it is always a collate state).
	 *                         For referred state, their simplification and symbolic
	 *                         constant collection must be carried out along with
	 *                         their main state. Otherwise there will be
	 *                         inconsistency in between them (referred and main
	 *                         states).
	 *                         </p>
	 *                         <p>
	 *                         Here main state means the state where has variables
	 *                         referring this referred state.
	 *                         </p>
	 * @return
	 * @throws CIVLHeapException
	 */
	public ImmutableState canonicWork(State state, boolean collectProcesses, boolean collectScopes,
			boolean collectHeaps, boolean collectSymbolicConstants, boolean simplify, Set<HeapErrorKind> toBeIgnored)
			throws CIVLHeapException {
		ImmutableState theState = (ImmutableState) state;

		// performance experiment: seems to make no difference
		// theState = flyweight(theState);
		if (collectProcesses)
			theState = collectProcesses(theState);
		if (collectScopes)
			theState = collectScopes(theState, toBeIgnored);
		if (collectHeaps)
			theState = collectHeaps(theState, toBeIgnored);
		// theState = collectSymbolicConstants(theState, collectHeaps);
		if (simplify)
			theState = simplify(theState);
		if (collectSymbolicConstants)
			theState = collectHavocVariables(theState);
		theState.makeCanonic(universe, scopeMap, processMap);
		return theState;
	}

	@Override
	public ImmutableState collectHeaps(State state, Set<HeapErrorKind> toBeIgnored) throws CIVLHeapException {
		ImmutableState theState = (ImmutableState) state;

		// only collect heaps when necessary.
		if (!this.hasNonEmptyHeaps(theState))
			return theState;
		else {
			Set<SymbolicExpression> reachable = this.reachableHeapObjectsOfState(theState);
			int numDyscopes = theState.numDyscopes();
			int numHeapFields = typeFactory.heapType().getNumMallocs();
			Map<SymbolicExpression, SymbolicExpression> oldToNewHeapMemUnits = new HashMap<>();
			ImmutableDynamicScope[] newScopes = new ImmutableDynamicScope[numDyscopes];
			ReferenceExpression[] fieldRefs = new ReferenceExpression[numHeapFields];

			for (int mallocId = 0; mallocId < numHeapFields; mallocId++) {
				fieldRefs[mallocId] = universe.tupleComponentReference(universe.identityReference(),
						universe.intObject(mallocId));
			}
			for (int dyscopeId = 0; dyscopeId < numDyscopes; dyscopeId++) {
				DynamicScope dyscope = theState.getDyscope(dyscopeId);
				SymbolicExpression heap = dyscope.getValue(0);

				if (heap.isNull())
					continue;
				else {
					SymbolicExpression newHeap = heap;
					SymbolicExpression heapPointer = this.symbolicUtil.makePointer(dyscopeId, 0,
							universe.identityReference());

					for (int mallocId = 0; mallocId < numHeapFields; mallocId++) {
						SymbolicExpression heapField = universe.tupleRead(heap, universe.intObject(mallocId));
						int length = this.symbolicUtil.extractInt(null, (NumericExpression) universe.length(heapField));
						Map<Integer, Integer> oldID2NewID = new HashMap<>();
						int numRemoved = 0;
						SymbolicExpression newHeapField = heapField;
						boolean hasNew = false;

						for (int objectId = 0; objectId < length; objectId++) {
							ReferenceExpression objectRef = universe.arrayElementReference(fieldRefs[mallocId],
									universe.integer(objectId));
							SymbolicExpression objectPtr = this.symbolicUtil.setSymRef(heapPointer, objectRef);

							if (!reachable.contains(objectPtr)) {
								SymbolicExpression heapObj = universe.arrayRead(heapField, universe.integer(objectId));

								if (config.isPropertyToggled(CIVLProperty.MEMORY_LEAK)
										&& !symbolicUtil.isInvalidHeapObject(heapObj)
										&& !toBeIgnored.contains(HeapErrorKind.UNREACHABLE)) {
									throw new CIVLHeapException(CIVLProperty.MEMORY_LEAK, Certainty.CONCRETE, theState,
											"d" + dyscopeId, dyscopeId, heap, mallocId, objectId,
											HeapErrorKind.UNREACHABLE, dyscope.lexicalScope().getSource());
								}
								// remove unreachable heap object
								// updates references
								for (int nextId = objectId + 1; nextId < length; nextId++) {
									if (oldID2NewID.containsKey(nextId))
										oldID2NewID.put(nextId, oldID2NewID.get(nextId) - 1);
									else
										oldID2NewID.put(nextId, nextId - 1);
								}
								// remove object
								hasNew = true;
								newHeapField = universe.removeElementAt(newHeapField, objectId - numRemoved);
								numRemoved++;
							}
						}
						if (oldID2NewID.size() > 0)
							addOldToNewHeapMemUnits(oldID2NewID, heapPointer, fieldRefs[mallocId],
									oldToNewHeapMemUnits);
						if (hasNew)
							newHeap = universe.tupleWrite(newHeap, universe.intObject(mallocId), newHeapField);
					}
					if (symbolicUtil.isEmptyHeap(newHeap))
						newHeap = universe.nullExpression();
					theState = this.setVariable(theState, 0, dyscopeId, newHeap);
				}
			}
			computeOldToNewHeapPointers(theState, oldToNewHeapMemUnits, oldToNewHeapMemUnits);
			for (int i = 0; i < numDyscopes; i++)
				newScopes[i] = theState.getDyscope(i).updateHeapPointers(oldToNewHeapMemUnits, universe);
			// update heap pointers in write set and partial path conditions:
			theState = applyToProcessStates(theState, universe.mapSubstituter(oldToNewHeapMemUnits));
			theState = theState.setScopes(newScopes);
			return theState;
		}
	}

	/**
	 * Apply an {@link UnaryOperator} to symbolic expressions in partial path
	 * conditions and write sets in {@link ProcessState}s of the given state.
	 * 
	 * @param state         The state where heap pointers are collected.
	 * @param substituteMap A unary operator which will be applied to partial path
	 *                      condition stacks and write set stacks of processes in
	 *                      the given state.
	 * @return A new state in which heap pointers in process states are collected.
	 */
	private ImmutableState applyToProcessStates(ImmutableState state, UnaryOperator<SymbolicExpression> substituter) {
		ImmutableProcessState[] newProcs = state.copyProcessStates();
		ImmutableProcessState newProcesses[] = new ImmutableProcessState[state.numProcs()];
		boolean procChanged = false;

		for (int i = 0; i < newProcs.length; i++) {
			if (state.getProcessState(i) == null) {
				newProcesses[i] = null;
				continue;
			} else
				newProcesses[i] = state.getProcessState(i);

			ImmutableProcessState newProcState = newProcesses[i].apply(substituter);

			if (newProcState != newProcesses[i]) {
				procChanged = true;
				newProcesses[i] = newProcState;
			}
		}
		if (procChanged)
			return state.setProcessStates(newProcesses);
		else
			return state;
	}

	@Override
	public ImmutableState collectScopes(State state, Set<HeapErrorKind> toBeIgnored) throws CIVLHeapException {
		return collectScopesWorker(state, toBeIgnored, null);
	}

	/**
	 * The worker method which is called directly by
	 * {@link #collectScopes(State, Set)}. This method has one more output parameter
	 * that {@link #collectScopes(State, Set)} doesn't need.
	 * 
	 * @param state         the state whose scopes will be collected
	 * @param toBeIgnored   the set of {@link HeapErrorKind}s that will be ignored
	 *                      during collection
	 * @param old2NewOutput OUTPUT parameter. A map from old scope IDs to new scope
	 *                      IDs. <b>pre-condition:</b>
	 *                      <code>old2NewOutput.length == state.numDyscopes()</code>
	 * 
	 * @return
	 * @throws CIVLHeapException
	 */
	private ImmutableState collectScopesWorker(State state, Set<HeapErrorKind> toBeIgnored, int old2NewOutput[])
			throws CIVLHeapException {
		ImmutableState theState = (ImmutableState) state;
		int oldNumScopes = theState.numDyscopes();
		int[] oldToNew = numberScopes(theState);
		boolean change = false;
		int newNumScopes = 0;

		for (int i = 0; i < oldNumScopes; i++) {
			int id = oldToNew[i];

			if (id >= 0)
				newNumScopes++;
			if (!change && id != i)
				change = true;
			if (id < 0 && config.isPropertyToggled(CIVLProperty.MEMORY_LEAK)
					&& !toBeIgnored.contains(HeapErrorKind.NONEMPTY)) {
				ImmutableDynamicScope scopeToBeRemoved = theState.getDyscope(i);
				Variable heapVariable = scopeToBeRemoved.lexicalScope().variable(ModelConfiguration.HEAP_VAR);
				SymbolicExpression heapValue = scopeToBeRemoved.getValue(heapVariable.vid());

				if (!(heapValue.isNull() || symbolicUtil.isEmptyHeap(heapValue))) {
					throw new CIVLHeapException(CIVLProperty.MEMORY_LEAK, Certainty.CONCRETE, state, "d" + i, i,
							heapValue, HeapErrorKind.NONEMPTY, heapVariable.getSource());
				}
			}
		}
		if (change) {
			UnaryOperator<SymbolicExpression> substituter = getDyscopeSubstituter(oldToNew);

			ImmutableDynamicScope[] newScopes = new ImmutableDynamicScope[newNumScopes];
			int numProcs = theState.numProcs();
			ImmutableProcessState[] newProcesses = new ImmutableProcessState[numProcs];
			BooleanExpression newPathCondition = (BooleanExpression) substituter
					.apply(theState.getPermanentPathCondition());

			for (int i = 0; i < oldNumScopes; i++) {
				int newId = oldToNew[i];

				if (newId >= 0) {
					ImmutableDynamicScope oldScope = theState.getDyscope(i);
					int oldParent = oldScope.getParent();
					// int oldParentIdentifier = oldScope.identifier();

					newScopes[newId] = oldScope.updateDyscopeIds(substituter, universe,
							oldParent < 0 ? oldParent : oldToNew[oldParent]);
				}
			}
			for (int pid = 0; pid < numProcs; pid++) {
				newProcesses[pid] = theState.getProcessState(pid);
				if (newProcesses[pid] != null)
					newProcesses[pid] = newProcesses[pid].updateDyscopes(oldToNew, substituter);
			}
			theState = ImmutableState.newState(theState, newProcesses, newScopes, newPathCondition);
		}
		if (theState.numDyscopes() == 1 && !toBeIgnored.contains(HeapErrorKind.NONEMPTY)
				&& theState.getProcessState(0).hasEmptyStack()) {
			// checks the memory leak for the final state
			DynamicScope dyscope = state.getDyscope(0);
			SymbolicExpression heap = dyscope.getValue(0);

			if (config.isPropertyToggled(CIVLProperty.MEMORY_LEAK) && !symbolicUtil.isEmptyHeap(heap))
				throw new CIVLHeapException(CIVLProperty.MEMORY_LEAK, Certainty.CONCRETE, state, "d0", 0, heap,
						HeapErrorKind.NONEMPTY, dyscope.lexicalScope().getSource());

		}
		if (old2NewOutput != null)
			System.arraycopy(oldToNew, 0, old2NewOutput, 0, oldToNew.length);
		return theState;
	}

	// @Override
	public State getAtomicLock(State state, int pid) {
		Variable atomicVar = modelFactory.atomicLockVariableExpression().variable();

		// assert state.getVariableValue(0, atomicVar.vid())
		return this.setVariable(state, atomicVar.vid(), 0, processValue(pid));
	}

	@Override
	public long getNumStateInstances() {
		return ImmutableState.instanceCount - initialNumStateInstances;
	}

	@Override
	public ImmutableState initialState(Model model) throws CIVLHeapException {
		// HashMap<Integer, Map<SymbolicExpression, Boolean>> reachableMUs = new
		// HashMap<Integer, Map<SymbolicExpression, Boolean>>();
		// HashMap<Integer, Map<SymbolicExpression, Boolean>> reachableMUwtPtr =
		// new HashMap<Integer, Map<SymbolicExpression, Boolean>>();
		ImmutableState state;
		CIVLFunction function = model.rootFunction();
		int numArgs = function.parameters().size();
		SymbolicExpression[] arguments = new SymbolicExpression[numArgs];
		Variable atomicVar = modelFactory.atomicLockVariableExpression().variable();
		Variable timeCountVar = modelFactory.timeCountVariable();

		// reachableMUs.put(0, new HashMap<SymbolicExpression, Boolean>());
		state = new ImmutableState(new ImmutableProcessState[0], new ImmutableDynamicScope[0],
				universe.trueExpression());
		state.collectibleCounts = new int[ModelConfiguration.SYMBOL_PREFIXES.length];
		for (int i = 0; i < ModelConfiguration.SYMBOL_PREFIXES.length; i++) {
			state.collectibleCounts[i] = 0;
		}
		// system function doesn't have any argument, because the General
		// transformer has translated away all parameters of the main function.
		state = addProcess(state, function, arguments, -1, false);
		state = this.setVariable(state, atomicVar.vid(), 0, undefinedProcessValue);
		if (timeCountVar != null)
			state = this.setVariable(state, timeCountVar.vid(), 0, universe.zeroInt());
		// state = this.computeReachableMemUnits(state, 0);
		state = canonic(state, false, false, false, false, false, emptyHeapErrorSet);
		return state;
	}

	@Override
	public boolean isDescendantOf(State state, int ancestor, int descendant) {
		if (ancestor == descendant) {
			return false;
		} else {
			int parent = state.getParentId(descendant);

			while (parent >= 0) {
				if (ancestor == parent)
					return true;
				parent = state.getParentId(parent);
			}
		}
		return false;
	}

	@Override
	public boolean lockedByAtomic(State state) {
		SymbolicExpression symbolicAtomicPid = state.getVariableValue(0,
				modelFactory.atomicLockVariableExpression().variable().vid());
		int atomicPid = modelFactory.getProcessId(symbolicAtomicPid);

		return atomicPid >= 0;
	}

	@Override
	// TODO: improve the performance: keep track of depth of dyscopes
	public int lowestCommonAncestor(State state, int one, int another) {
		if (one == another) {
			return one;
		} else {
			int parent = one;

			while (parent >= 0) {
				if (parent == another || this.isDescendantOf(state, parent, another))
					return parent;
				parent = state.getParentId(parent);
			}
		}
		return state.rootDyscopeID();
	}

	@Override
	public ImmutableState popCallStack(State state, int pid) {
		ImmutableState theState = (ImmutableState) state;
		ImmutableProcessState process = theState.getProcessState(pid);
		ImmutableProcessState[] processArray = theState.copyProcessStates();
		ImmutableDynamicScope[] newScopes = theState.copyScopes();
		processArray[pid] = process.pop();
		setReachablesForProc(newScopes, processArray[pid]);
		theState = ImmutableState.newState(theState, processArray, newScopes, null);
		return theState;
	}

	@Override
	public int processInAtomic(State state) {
		// TODO use a field for vid
		SymbolicExpression symbolicAtomicPid = state.getVariableValue(0,
				modelFactory.atomicLockVariableExpression().variable().vid());

		return modelFactory.getProcessId(symbolicAtomicPid);
	}

	@Override
	public ImmutableState pushCallStack(State state, int pid, CIVLFunction function, SymbolicExpression[] arguments) {
		return pushCallStack2((ImmutableState) state, pid, function, function.outerScope(), -1, arguments, pid);
	}

	@Override
	public State pushCallStack(State state, int pid, CIVLFunction function, int functionParentDyscope,
			SymbolicExpression[] arguments) {
		return pushCallStack2((ImmutableState) state, pid, function, function.outerScope(), functionParentDyscope,
				arguments, pid);
	}

	@Override
	public State pushContract(State state, int pid, CIVLFunction function, SymbolicExpression[] arguments) {
		return pushCallStack2((ImmutableState) state, pid, function, function.functionContract().scope(), -1, arguments,
				pid);
	}

	@Override
	public ImmutableState collectProcesses(State state) {
		ImmutableState theState = (ImmutableState) state;

		if (!collectProcs)
			return theState;

		int numProcs = theState.numProcs();
		boolean change = false;
		int counter = 0;

		while (counter < numProcs) {
			if (theState.getProcessState(counter) == null) {
				change = true;
				break;
			}
			counter++;
		}
		if (change) {
			int newNumProcs = counter;
			int[] oldToNewPidMap = new int[numProcs];
			ImmutableProcessState[] newProcesses;
			ImmutableDynamicScope[] newScopes;
			// Map<Integer, Map<SymbolicExpression, Boolean>>
			// reachableMUsWtPointer, reachableMUsWoPointer;

			for (int i = 0; i < counter; i++)
				oldToNewPidMap[i] = i;
			oldToNewPidMap[counter] = -1;
			for (int i = counter + 1; i < numProcs; i++) {
				if (theState.getProcessState(i) == null) {
					oldToNewPidMap[i] = -1;
				} else {
					oldToNewPidMap[i] = newNumProcs;
					newNumProcs++;
				}
			}
			newProcesses = new ImmutableProcessState[newNumProcs];
			for (int i = 0; i < numProcs; i++) {
				int newPid = oldToNewPidMap[i];

				if (newPid >= 0)
					newProcesses[newPid] = theState.getProcessState(i).setPid(newPid);
			}
			// newReachableMemUnitsMap =
			// updateProcessReferencesInReachableMemoryUnitsMap(
			// theState, oldToNewPidMap);
			// reachableMUsWtPointer = this.updatePIDsForReachableMUs(
			// oldToNewPidMap, theState, true);
			// reachableMUsWoPointer = this.updatePIDsForReachableMUs(
			// oldToNewPidMap, theState, false);
			newScopes = updateProcessReferencesInScopes(theState, oldToNewPidMap);
			theState = ImmutableState.newState(theState, newProcesses, newScopes, null);
		}
		return theState;
	}

	@Override
	public State terminateProcess(State state, int pid) {
		ImmutableState theState = (ImmutableState) state;
		ImmutableProcessState emptyProcessState = new ImmutableProcessState(pid, false);

		return theState.setProcessState(pid, emptyProcessState);
	}

	@Override
	public ImmutableState removeProcess(State state, int pid) {
		ImmutableState theState = (ImmutableState) state;

		theState = theState.setProcessState(pid, null);
		return theState;
	}

	@Override
	public State releaseAtomicLock(State state) {
		Variable atomicVar = modelFactory.atomicLockVariableExpression().variable();

		return this.setVariable(state, atomicVar.vid(), 0, processValue(-1));
	}

	/**
	 * Procedure:
	 * 
	 * <ol>
	 * <li>get the current dynamic scope ds0 of the process. Let ss0 be the static
	 * scope associated to ds0.</li>
	 * <li>Let ss1 be the static scope of the new location to move to.</li>
	 * <li>Compute the join (youngest common ancestor) of ss0 and ss1. Also save the
	 * sequence of static scopes from join to ss1.</li>
	 * <li>Iterate UP over dynamic scopes from ds0 up (using parent field) to the
	 * first dynamic scope whose static scope is join.</li>
	 * <li>Iterate DOWN from join to ss1, creating NEW dynamic scopes along the
	 * way.</li>
	 * <li>Set the frame pointer to the new dynamic scope corresponding to ss1, and
	 * set the location to the given location.</li>
	 * <li>Remove all unreachable scopes.</li>
	 * </ol>
	 * 
	 * @param state
	 * @param pid
	 * @param location
	 * @return
	 */
	@Override
	// TODO UPDATE reachable mem units
	public ImmutableState setLocation(State state, int pid, Location location, boolean accessChanged) {
		ImmutableState theState = (ImmutableState) state;
		ImmutableProcessState[] processArray = theState.copyProcessStates();
		int dynamicScopeId = theState.getProcessState(pid).getDyscopeId();
		ImmutableDynamicScope dynamicScope = theState.getDyscope(dynamicScopeId);
		// int dynamicScopeIdentifier = dynamicScope.identifier();
		boolean stayInScope = location.isSleep();

		if (!location.isSleep()) {
			stayInScope = location.scope() == dynamicScope.lexicalScope();
		}
		if (stayInScope) {// remains in the same dyscope
			processArray[pid] = theState.getProcessState(pid).replaceTop(stackEntry(location, dynamicScopeId));
			theState = theState.setProcessStates(processArray);
			// if (accessChanged)
			// theState = updateReachableMemUnitsAccess(theState, pid);
			return theState;
		} else {// a different dyscope is encountered
			Scope[] joinSequence = joinSequence(dynamicScope.lexicalScope(), location.scope());
			Scope join = joinSequence[0];
			Set<Integer> dyscopeIDsequence = new HashSet<>();

			// iterate UP...
			while (dynamicScope.lexicalScope() != join) {
				dyscopeIDsequence.add(dynamicScopeId);
				dynamicScopeId = theState.getParentId(dynamicScopeId);
				if (dynamicScopeId < 0)
					throw new RuntimeException("State is inconsistent");
				dynamicScope = theState.getDyscope(dynamicScopeId);
				// dynamicScopeIdentifier = dynamicScope.identifier();
			}
			if (joinSequence.length == 1) {
				// Map<Integer, Map<SymbolicExpression, Boolean>>
				// reachableMUwoPtr, reachableMUwtPtr;

				// the previous scope(s) just disappear
				processArray[pid] = theState.getProcessState(pid).replaceTop(stackEntry(location, dynamicScopeId));
				// reachableMUwoPtr = this.setReachableMemUnits(theState, pid,
				// this.removeReachableMUwoPtrFromDyscopes(
				// dyscopeIDsequence, theState, pid), false);
				// reachableMUwtPtr = this.setReachableMemUnits(theState, pid,
				// this.computeReachableMUofProc(theState, pid, true),
				// true);
				theState = ImmutableState.newState(theState, processArray, null, null);
			} else {
				// iterate DOWN, adding new dynamic scopes...
				int oldNumScopes = theState.numDyscopes();
				int newNumScopes = oldNumScopes + joinSequence.length - 1;
				int index = 0;
				ImmutableDynamicScope[] newScopes = new ImmutableDynamicScope[newNumScopes];
				int[] newDyscopes = new int[joinSequence.length - 1];

				for (; index < oldNumScopes; index++)
					newScopes[index] = theState.getDyscope(index);
				for (int i = 1; i < joinSequence.length; i++) {
					// only this process can reach the new dyscope
					BitSet reachers = new BitSet(processArray.length);

					reachers.set(pid);
					newScopes[index] = initialDynamicScope(joinSequence[i], dynamicScopeId, index, reachers);
					dynamicScopeId = index;
					newDyscopes[i - 1] = dynamicScopeId;
					index++;
				}
				processArray[pid] = processArray[pid].replaceTop(stackEntry(location, dynamicScopeId));
				setReachablesForProc(newScopes, processArray[pid]);
				theState = ImmutableState.newState(theState, processArray, newScopes, null);
				// theState = addReachableMemUnitsFromDyscope(newDyscopes,
				// newScopes, theState, pid);
			}
			// if (accessChanged)
			// theState = updateReachableMemUnitsAccess(theState, pid);
			return theState;
		}
	}

	@Override
	public State setProcessState(State state, ProcessState p) {
		ImmutableState theState = (ImmutableState) state;
		ImmutableState newState;
		ImmutableProcessState[] newProcesses;
		int pid = p.getPid();

		newProcesses = theState.copyProcessStates();
		newProcesses[pid] = (ImmutableProcessState) p;
		theState = theState.setProcessStates(newProcesses);
		newState = new ImmutableState(newProcesses, theState.copyScopes(), theState.getPermanentPathCondition());
		newState.collectibleCounts = theState.collectibleCounts;
		return newState;
	}

	@Override
	public ImmutableState setVariable(State state, int vid, int scopeId, SymbolicExpression value) {
		ImmutableState theState = (ImmutableState) state;
		ImmutableDynamicScope oldScope = (ImmutableDynamicScope) theState.getDyscope(scopeId);
		ImmutableDynamicScope[] newScopes = theState.copyScopes();
		SymbolicExpression[] newValues = oldScope.copyValues();
		ImmutableDynamicScope newScope;

		newValues[vid] = value;
		newScope = new ImmutableDynamicScope(oldScope.identifier(), oldScope.lexicalScope(), oldScope.getParent(),
				newValues, oldScope.getReachers());
		newScopes[scopeId] = newScope;
		theState = theState.setScopes(newScopes);
		return theState;
	}

	@Override
	public ImmutableState setVariable(State state, Variable variable, int pid, SymbolicExpression value) {
		int scopeId = state.getDyscopeID(pid, variable);

		return setVariable(state, variable.vid(), scopeId, value);
	}

	@Override
	public ImmutableState simplify(State state) {
		return simplify(state, -1, null);
	}

	@Override
	public ImmutableState simplify(State state, int pid) {
		return simplify(state, pid, null);
	}

	@Override
	public ImmutableState simplify(State state, Set<SymbolicConstant> aggressiveSet) {
		return simplify(state, -1, aggressiveSet);
	}

	@Override
	public ImmutableState simplify(State state, int pid, Set<SymbolicConstant> aggressiveSet) {
		ImmutableState theState = (ImmutableState) state;
		return simplifyWork(theState, true, pid, aggressiveSet);
	}

	/**
	 * Simplify the given state.
	 * 
	 * @param state the state that will gets simplified
	 * @return
	 */
	private ImmutableState simplifyWork(State state, boolean reducePathCondition, int simplifyingPid,
			Set<SymbolicConstant> aggressiveSet) {
		ImmutableState theState = (ImmutableState) state;

		if (theState.simplifiedState != null)
			return theState.simplifiedState;

		ImmutableProcessState[] procStates = theState.copyProcessStates();

		List<BooleanExpression> conditionStack;
		if (simplifyingPid >= 0) {
			BooleanExpression[] ppc = procStates[simplifyingPid].getPartialPathConditions();
			conditionStack = new ArrayList<>(ppc.length + 1);
			conditionStack.add(state.getPermanentPathCondition());
			conditionStack.addAll(Arrays.asList(ppc));
		} else {
			conditionStack = new ArrayList<>(2);
			conditionStack.add(state.getPermanentPathCondition());

			BooleanExpression conjppc = universe.trueExpression();
			for (int i = 0; i < procStates.length; i++) {
				if (procStates[i] == null)
					continue;

				conjppc = universe.and(conjppc, universe.and(Arrays.asList(procStates[i].getPartialPathConditions())));
			}
			if (!conjppc.isTrue())
				conditionStack.add(conjppc);
		}

		Reasoner reasoner = universe.reasoner(conditionStack);
		// reasoner.aggressivelySimplifyTopContext(aggressiveSet);

		if (nsat(reasoner.getReducedCollapsedContext())) {
			return theState.setPermanentPathCondition(universe.falseExpression());
		}

		UnaryOperator<SymbolicExpression> substituter = universe
				.constantSubstituter(reasoner.constantSubstitutionMap());
		boolean processChanged = false;

		for (int i = 0; i < procStates.length; i++) {
			if (procStates[i] == null)
				continue;

			boolean procStateChanged = false;
			BooleanExpression[] newPartialPathConds = procStates[i].getPartialPathConditions().clone();
			int ppcLen = newPartialPathConds.length;

			for (int j = 0; j < ppcLen; j++) {
				BooleanExpression oldPartialPathCond = newPartialPathConds[j];
				newPartialPathConds[j] = i == simplifyingPid
						? (reducePathCondition ? reasoner.getReducedContext(j + 1) : reasoner.getFullContext(j + 1))
						: (BooleanExpression) substituter.apply(newPartialPathConds[j]);

				if (!oldPartialPathCond.equals(newPartialPathConds[j]))
					procStateChanged = true;
			}

			SimplifyOperator simplifier = new SimplifyOperator(reasoner, aggressiveSet);
			ImmutableProcessState tmp = procStateChanged ? procStates[i].setPartialPathConditions(newPartialPathConds)
					: procStates[i];
			tmp = tmp.apply(simplifier, false);

			if (tmp != procStates[i])
				processChanged = true;
			procStates[i] = tmp;
		}

		int numScopes = theState.numDyscopes();
		ImmutableDynamicScope[] newDynamicScopes = null;

		for (int i = 0; i < numScopes; i++) {
			ImmutableDynamicScope oldScope = theState.getDyscope(i);
			int numVars = oldScope.numberOfVariables();
			SymbolicExpression[] newVariableValues = null;

			for (int j = 0; j < numVars; j++) {
				SymbolicExpression oldValue = oldScope.getValue(j);
				SymbolicExpression newValue = reasoner.simplify(oldValue, aggressiveSet);

				if (oldValue != newValue && newVariableValues == null) {
					newVariableValues = new SymbolicExpression[numVars];
					for (int j2 = 0; j2 < j; j2++)
						newVariableValues[j2] = oldScope.getValue(j2);
				}
				if (newVariableValues != null)
					newVariableValues[j] = newValue;
			}
			if (newVariableValues != null && newDynamicScopes == null) {
				newDynamicScopes = new ImmutableDynamicScope[numScopes];
				for (int i2 = 0; i2 < i; i2++)
					newDynamicScopes[i2] = theState.getDyscope(i2);
			}
			if (newDynamicScopes != null)
				newDynamicScopes[i] = newVariableValues != null ? oldScope.setVariableValues(newVariableValues)
						: oldScope;
		}

		if (newDynamicScopes != null || processChanged) {
			theState = ImmutableState.newState(theState, procStates, newDynamicScopes,
					reducePathCondition ? reasoner.getReducedContext(0) : reasoner.getFullContext(0));
			theState.simplifiedState = theState;
		}

		return theState;
	}

	@Override
	public SymbolicUniverse symbolicUniverse() {
		return universe;
	}

	@Override
	public Pair<State, SymbolicExpression> malloc(State state, int dyscopeId, int mallocId,
			SymbolicExpression heapObject) {
		DynamicScope dyscope = state.getDyscope(dyscopeId);
		IntObject indexObj = universe.intObject(mallocId);
		SymbolicExpression heapValue = dyscope.getValue(0);
		SymbolicExpression heapField;
		SymbolicExpression heapAtomicObjectPtr;
		ReferenceExpression symRef;
		NumericExpression heapLength;

		if (heapValue.isNull())
			heapValue = typeFactory.heapType().getInitialValue();
		heapField = universe.tupleRead(heapValue, indexObj);
		heapLength = universe.length(heapField);
		heapField = universe.append(heapField, heapObject);
		heapValue = universe.tupleWrite(heapValue, indexObj, heapField);
		state = setVariable(state, 0, dyscopeId, heapValue);
		symRef = universe.identityReference();
		symRef = universe.tupleComponentReference(symRef, indexObj);
		symRef = universe.arrayElementReference(symRef, heapLength);
		symRef = universe.arrayElementReference(symRef, universe.zeroInt());
		heapAtomicObjectPtr = symbolicUtil.makePointer(dyscopeId, 0, symRef);
		return new Pair<>(state, heapAtomicObjectPtr);
	}

	@Override
	public Pair<State, SymbolicExpression> malloc(State state, int pid, int dyscopeId, int mallocId,
			SymbolicType elementType, NumericExpression elementCount) {
		DynamicScope dyscope = state.getDyscope(dyscopeId);
		SymbolicExpression heapValue = dyscope.getValue(0).isNull() ? typeFactory.heapType().getInitialValue()
				: dyscope.getValue(0);
		IntObject index = universe.intObject(mallocId);
		SymbolicExpression heapField = universe.tupleRead(heapValue, index);
		int length = ((IntegerNumber) universe.extractNumber(universe.length(heapField))).intValue();
		StringObject heapObjectName = universe
				.stringObject("Hp" + pid + "s" + dyscopeId + "f" + mallocId + "o" + length);
		SymbolicType heapObjectType = universe.arrayType(elementType, elementCount);
		SymbolicExpression heapObject = universe.symbolicConstant(heapObjectName, heapObjectType);

		return this.malloc(state, dyscopeId, mallocId, heapObject);
	}

	@Override
	public State deallocate(State state, SymbolicExpression heapObjectPointer, SymbolicExpression scopeOfPointer,
			int mallocId, int index) {
		int dyscopeId = getDyscopeId(scopeOfPointer);
		SymbolicExpression heapValue = state.getDyscope(dyscopeId).getValue(0);
		IntObject mallocIndex = universe.intObject(mallocId);
		SymbolicExpression heapField = universe.tupleRead(heapValue, mallocIndex);
		ImmutableState theState = (ImmutableState) state;
		SymbolicExpression oldVal = universe.arrayRead(heapField, universe.integer(index));
		SymbolicType oldValType = oldVal.type();

		assert oldValType instanceof SymbolicCompleteArrayType;
		heapField = universe.arrayWrite(heapField, universe.integer(index), symbolicUtil.invalidHeapObject(oldValType));
		heapValue = universe.tupleWrite(heapValue, mallocIndex, heapField);
		theState = this.setVariable(theState, 0, dyscopeId, heapValue);
		return theState;
	}

	/* *************************** Private Methods ************************* */

	/**
	 * Adds a new initial process state to the given state.
	 * 
	 * @param state            The old state.
	 * @param selfDestructable If the created process is self-destructable
	 * @return A new instance of state with only the process states changed.
	 */
	protected ImmutableState createNewProcess(State state, boolean selfDestructable) {
		ImmutableState theState = (ImmutableState) state;
		int numProcs = theState.numProcs();
		ImmutableProcessState[] newProcesses;

		newProcesses = theState.copyAndExpandProcesses();
		newProcesses[numProcs] = new ImmutableProcessState(numProcs, selfDestructable);
		theState = theState.setProcessStates(newProcesses);
		return theState;
	}

	/**
	 * Creates a dyscope in its initial state.
	 * 
	 * @param lexicalScope The lexical scope corresponding to this dyscope.
	 * @param parent       The parent of this dyscope. -1 only for the topmost
	 *                     dyscope.
	 * @return A new dynamic scope.
	 */
	private ImmutableDynamicScope initialDynamicScope(Scope lexicalScope, int parent, int dynamicScopeId,
			BitSet reachers) {
		int ident = nextDyscopeId;
		nextDyscopeId++;
		return new ImmutableDynamicScope(ident, lexicalScope, parent, initialValues(lexicalScope), reachers);
	}

	/**
	 * Creates the initial value of a given lexical scope.
	 * 
	 * @param lexicalScope The lexical scope whose variables are to be initialized.
	 * @return An array of initial values of variables of the given lexical scope.
	 */
	protected SymbolicExpression[] initialValues(Scope lexicalScope) {
		// TODO: special handling for input variables in root scope?
		SymbolicExpression[] values = new SymbolicExpression[lexicalScope.numVariables()];

		for (int i = 0; i < values.length; i++) {
			values[i] = universe.nullExpression();
		}
		return values;
	}

	/**
	 * Given two static scopes, this method computes a non-empty sequence of scopes
	 * with the following properties:
	 * <ul>
	 * <li>The first (0-th) element of the sequence is the join of scope1 and
	 * scope2.</li>
	 * <li>The last element is scope2.</li>
	 * <li>For each i (0<=i<length-1), the i-th element is the parent of the
	 * (i+1)-th element.</li>
	 * </ul>
	 * 
	 * @param scope1 a static scope
	 * @param scope2 a static scope
	 * @return join sequence as described above
	 * 
	 * @exception IllegalArgumentException if the scopes do not have a common
	 *                                     ancestor
	 */
	private Scope[] joinSequence(Scope scope1, Scope scope2) {
		if (scope1 == scope2)
			return new Scope[] { scope2 };
		for (Scope scope1a = scope1; scope1a != null; scope1a = scope1a.parent())
			for (Scope scope2a = scope2; scope2a != null; scope2a = scope2a.parent())
				if (scope1a.equals(scope2a)) {
					Scope join = scope2a;
					int length = 1;
					Scope[] result;
					Scope s;

					for (s = scope2; s != join; s = s.parent())
						length++;
					result = new Scope[length];
					s = scope2;
					for (int i = length - 1; i >= 0; i--) {
						result[i] = s;
						s = s.parent();
					}
					return result;
				}
		throw new IllegalArgumentException("No common scope:\n" + scope1 + "\n" + scope2);
	}

	/**
	 * Numbers the reachable dynamic scopes in a state in a canonical way. Scopes
	 * are numbered from 0 up, in the order in which they are encountered by
	 * iterating over the processes by increasing ID, iterating over the process'
	 * call stack frames from index 0 up, iterating over the parent scopes from the
	 * scope referenced by the frame.
	 * 
	 * Unreachable scopes are assigned the number -1.
	 * 
	 * Returns an array which of length numScopes in which the element at position i
	 * is the new ID number for the scope whose old ID number is i. Does not modify
	 * anything.
	 * 
	 * @param state a state
	 * @return an array mapping old scope IDs to new.
	 */
	private int[] numberScopes(ImmutableState state) {
		int numScopes = state.numDyscopes();
		int numProcs = state.numProcs();
		int[] oldToNew = new int[numScopes];
		int nextScopeId = 1;

		// the root dyscope is forced to be 0
		oldToNew[0] = 0;
		for (int i = 1; i < numScopes; i++)
			oldToNew[i] = ModelConfiguration.DYNAMIC_NULL_SCOPE;
		for (int pid = 0; pid < numProcs; pid++) {
			ImmutableProcessState process = state.getProcessState(pid);
			int stackSize;

			if (process == null)
				continue;
			stackSize = process.stackSize();
			// start at bottom of stack so system scope in proc 0
			// is reached first
			for (int i = stackSize - 1; i >= 0; i--) {
				int dynamicScopeId = process.getStackEntry(i).scope();

				while (oldToNew[dynamicScopeId] < 0) {
					oldToNew[dynamicScopeId] = nextScopeId;
					nextScopeId++;
					dynamicScopeId = state.getParentId(dynamicScopeId);
					if (dynamicScopeId < 0)
						break;
				}
			}
		}
		return oldToNew;
	}

	/**
	 * Checks if a given claim is not satisfiable.
	 * 
	 * @param claim The given claim.
	 * @return True iff the given claim is evaluated to be false.
	 */
	private boolean nsat(BooleanExpression claim) {
		if (universe.getShowProverQueries())
			universe.setQueryExplanation("checking path-condition satisfiability (state feasibility)");
		return trueContextReasoner.unsat(claim).getResultType() == ResultType.YES;
	}

	/**
	 * Creates a map of process value's according to PID map from old PID to new
	 * PID.
	 * 
	 * @param oldToNewPidMap The map of old PID to new PID, i.e, oldToNewPidMap[old
	 *                       PID] = new PID.
	 * @return The map of process value's from old process value to new process
	 *         value.
	 */
	private Map<SymbolicExpression, SymbolicExpression> procSubMap(int[] oldToNewPidMap) {
		int size = oldToNewPidMap.length;
		Map<SymbolicExpression, SymbolicExpression> result = new HashMap<SymbolicExpression, SymbolicExpression>(size);

		for (int i = 0; i < size; i++) {
			SymbolicExpression oldVal = processValue(i);
			SymbolicExpression newVal = processValue(oldToNewPidMap[i]);

			result.put(oldVal, newVal);
		}
		return result;
	}

	/**
	 * General method for pushing a frame onto a call stack, whether or not the call
	 * stack is for a new process (and therefore empty).
	 * 
	 * @param state     the initial state
	 * @param pid       the PID of the process whose stack is to be modified; this
	 *                  stack may be empty
	 * @param function  the called function that will be pushed onto the stack
	 * @param newScope  the static scope that will be used for the new dynamic scope
	 *                  that will be associated to the new frame. This is usually
	 *                  either the outer scope of the function, or the contract
	 *                  scope
	 * @param cid       The dyscope ID of the parent of the new function; If the
	 *                  caller has no knowledge about what is suppose to be the
	 *                  correct parent scope, caller can pass "-1" for this
	 *                  argument. This method will attempt to use the dyscope of the
	 *                  static parent scope of the function definition as the parent
	 *                  dyscope. If this is not the case you want, don't pass '-1'
	 *                  here.
	 * @param arguments the arguments to the function
	 * @param callerPid the PID of the process that is creating the new frame. For
	 *                  an ordinary function call, this will be the same as pid. For
	 *                  a "spawn" command, callerPid will be different from pid and
	 *                  process pid will be new and have an empty stack. Exception:
	 *                  if callerPid is -1 then the new dynamic scope will have no
	 *                  parent; this is used for pushing the original system
	 *                  function, which has no caller
	 * @return new stack with new frame on call stack of process pid
	 */
	protected ImmutableState pushCallStack2(ImmutableState state, int pid, CIVLFunction function, Scope newScope,
			int cid, SymbolicExpression[] arguments, int callerPid) {
		Scope containingScope = newScope.parent();
		// function.containingScope();

		if (cid < 0 && callerPid >= 0) {
			ProcessState caller = state.getProcessState(callerPid);

			for (cid = caller.getDyscopeId(); cid >= 0
					&& containingScope != state.getDyscope(cid).lexicalScope(); cid = state.getParentId(cid))
				;
			assert cid >= 0;
		}

		ImmutableDynamicScope[] newScopes = state.copyAndExpandScopes();
		int sid = state.numDyscopes();
		// Scope funcScope = function.outerScope();
		SymbolicExpression[] values = initialValues(newScope);
		ImmutableProcessState[] newProcesses = state.copyProcessStates();
		BitSet bitSet = new BitSet(newProcesses.length);

		// For now, ignoring extra arguments in call to variadic
		// functions. TODO: actually implement variadic functions.
		int nparam = function.parameters().size(), narg = arguments.length;
		assert (narg >= nparam); // this should always hold

		// first value is always heap, which will be null initially
		for (int i = 0; i < nparam; i++)
			if (arguments[i] != null)
				values[i + 1] = arguments[i];
		if (narg > nparam && !config.isQuiet()) {
			System.err.println("Warning: ignoring extra arguments in call to " + function.name().name());
		}
		bitSet.set(pid);
		newScopes[sid] = new ImmutableDynamicScope(nextDyscopeId, newScope, cid, values, bitSet);
		nextDyscopeId++;
		for (int id = cid; id >= 0;) {
			ImmutableDynamicScope scope = newScopes[id];

			bitSet = scope.getReachers();
			if (bitSet.get(pid))
				break;
			bitSet = (BitSet) bitSet.clone();
			bitSet.set(pid);
			newScopes[id] = scope.setReachers(bitSet);
			id = scope.getParent();
		}
		newProcesses[pid] = state.getProcessState(pid).push(stackEntry(null, sid));
		state = ImmutableState.newState(state, newProcesses, newScopes, null);
		if (!function.isSystemFunction() && newScope == function.outerScope())
			state = setLocation(state, pid, function.startLocation());
		return state;
	}

	/**
	 * Creates a map of scope value's according to the given dyscope map from old
	 * dyscope ID to new dyscope ID.
	 * 
	 * @param oldToNewSidMap The map of old dyscope ID to new dyscoep ID, i.e,
	 *                       oldToNewSidMap[old dyscope ID] = new dyscope ID.
	 * @return The map of scope value's from old scope value to new scope value.
	 */
	private Map<SymbolicExpression, SymbolicExpression> scopeSubMap(int[] oldToNewSidMap) {
		int size = oldToNewSidMap.length;
		Map<SymbolicExpression, SymbolicExpression> result = new HashMap<SymbolicExpression, SymbolicExpression>(size);

		for (int i = 0; i < size; i++) {
			SymbolicExpression oldVal = scopeValue(i);
			SymbolicExpression newVal = scopeValue(oldToNewSidMap[i]);

			result.put(oldVal, newVal);
		}
		return result;
	}

	/**
	 * Given an array of dynamic scopes and a process state, computes the actual
	 * dynamic scopes reachable from that process and modifies the array as
	 * necessary by replacing a dynamic scope with a scope that is equivalent except
	 * for the corrected bit set.
	 * 
	 * @param dynamicScopes an array of dynamic scopes, to be modified
	 * @param process       a process state
	 */
	private void setReachablesForProc(ImmutableDynamicScope[] dynamicScopes, ImmutableProcessState process) {
		int stackSize = process.stackSize();
		int numScopes = dynamicScopes.length;
		boolean reached[] = new boolean[numScopes];
		int pid = process.getPid();

		for (int i = 0; i < stackSize; i++) {
			StackEntry frame = process.getStackEntry(i);
			int id = frame.scope();

			while (id >= 0) {
				if (reached[id])
					break;
				reached[id] = true;
				id = dynamicScopes[id].getParent();
			}
		}
		for (int j = 0; j < numScopes; j++) {
			ImmutableDynamicScope scope = dynamicScopes[j];
			BitSet bitSet = scope.getReachers();

			if (bitSet.get(pid) != reached[j]) {
				BitSet newBitSet = (BitSet) bitSet.clone();

				newBitSet.flip(pid);
				dynamicScopes[j] = dynamicScopes[j].setReachers(newBitSet);
			}
		}
	}

	/**
	 * Create a new call stack entry.
	 * 
	 * @param location  The location to go to after returning from this call.
	 * @param dyscopeId The dynamic scope the process is in before the call.
	 */
	protected ImmutableStackEntry stackEntry(Location location, int dyscopeId) {
		return new ImmutableStackEntry(location, dyscopeId);

	}

	/**
	 * Given a BitSet indexed by process IDs, and a map of old PIDs to new PIDs,
	 * returns a BitSet equivalent to original but indexed using the new PIDs.
	 * 
	 * If no changes are made, the original BitSet (oldBitSet) is returned.
	 * 
	 * @param oldBitSet
	 * @param oldToNewPidMap array of length state.numProcs in which element at
	 *                       index i is the new PID of the process whose old PID is
	 *                       i. A negative value indicates that the process of (old)
	 *                       PID i is to be removed.
	 * @return
	 */
	private BitSet updateBitSet(BitSet oldBitSet, int[] oldToNewPidMap) {
		BitSet newBitSet = null;
		int length = oldBitSet.length();

		for (int i = 0; i < length; i++) {
			boolean flag = oldBitSet.get(i);

			if (flag) {
				int newIndex = oldToNewPidMap[i];

				if (newIndex >= 0) {
					if (newBitSet == null)
						newBitSet = new BitSet(length);
					newBitSet.set(newIndex);
				}
			}
		}
		if (newBitSet == null)
			return oldBitSet;
		return newBitSet;
	}

	/**
	 * Searches the dynamic scopes in the given state for any process reference
	 * value, and returns a new array of scopes equivalent to the old except that
	 * those process reference values have been replaced with new specified values.
	 * Used for garbage collection and canonicalization of PIDs.
	 * 
	 * Also updates the reachable BitSet in each DynamicScope: create a new BitSet
	 * called newReachable. iterate over all entries in old BitSet (reachable). If
	 * old entry is position i is true, set oldToNewPidMap[i] to true in
	 * newReachable (assuming oldToNewPidMap[i]>=0).
	 * 
	 * The method returns null if no changes were made.
	 * 
	 * @param state          a state
	 * @param oldToNewPidMap array of length state.numProcs in which element at
	 *                       index i is the new PID of the process whose old PID is
	 *                       i. A negative value indicates that the process of (old)
	 *                       PID i is to be removed.
	 * @return new dynamic scopes or null
	 */
	private ImmutableDynamicScope[] updateProcessReferencesInScopes(State state, int[] oldToNewPidMap) {
		Map<SymbolicExpression, SymbolicExpression> procSubMap = procSubMap(oldToNewPidMap);
		UnaryOperator<SymbolicExpression> substituter = universe.mapSubstituter(procSubMap);
		ImmutableDynamicScope[] newScopes = null;
		int numScopes = state.numDyscopes();

		for (int i = 0; i < numScopes; i++) {
			ImmutableDynamicScope dynamicScope = (ImmutableDynamicScope) state.getDyscope(i);
			Scope staticScope = dynamicScope.lexicalScope();
			Collection<Variable> procrefVariableIter = staticScope.variablesWithProcrefs();
			SymbolicExpression[] newValues = null;
			BitSet oldBitSet = dynamicScope.getReachers();
			BitSet newBitSet = updateBitSet(oldBitSet, oldToNewPidMap);

			for (Variable variable : procrefVariableIter) {
				int vid = variable.vid();
				SymbolicExpression oldValue = dynamicScope.getValue(vid);
				SymbolicExpression newValue = substituter.apply(oldValue);

				if (oldValue != newValue) {
					if (newValues == null)
						newValues = dynamicScope.copyValues();
					newValues[vid] = newValue;
				}
			}
			if (newValues != null || newBitSet != oldBitSet) {
				if (newScopes == null) {
					newScopes = new ImmutableDynamicScope[numScopes];
					for (int j = 0; j < i; j++)
						newScopes[j] = (ImmutableDynamicScope) state.getDyscope(j);
				}
				if (newValues == null)
					newScopes[i] = dynamicScope.setReachers(newBitSet);
				else
					newScopes[i] = new ImmutableDynamicScope(dynamicScope.identifier(), staticScope,
							dynamicScope.getParent(), newValues, newBitSet);
			} else if (newScopes != null) {
				newScopes[i] = dynamicScope;
			}
		}
		return newScopes;
	}

	private Set<SymbolicExpression> reachableHeapObjectsOfState(State state) {
		Set<SymbolicExpression> reachable = new LinkedHashSet<>();
		int numDyscopes = state.numDyscopes();

		for (int i = 0; i < numDyscopes; i++) {
			DynamicScope dyscope = state.getDyscope(i);
			int numVars = dyscope.numberOfValues();

			for (int vid = 1; vid < numVars; vid++) {
				SymbolicExpression value = dyscope.getValue(vid);

				reachableHeapObjectsOfValue(state, value, reachable);
			}
		}
		return reachable;
	}

	private void reachableHeapObjectsOfValue(State state, SymbolicExpression value, Set<SymbolicExpression> reachable) {
		if (value.isNull())
			return;

		if (value.operator() == SymbolicOperator.TUPLE && this.isPointer(value)) {
			if (symbolicUtil.isPointerToHeap(value)) {
				// Widen our pointer to include the entire heap memory unit
				value = this.symbolicUtil.heapMemUnit(value);

				// If we already analyzed this heap memory unit then we are done
				if (!reachable.add(value))
					return;
			}

			SymbolicExpression scopeVal = symbolicUtil.getScopeValue(value);
			int dyscopeId = scopeValueToDyscopeID.apply(scopeVal).intValue();

			if (dyscopeId >= 0) {
				int vid = this.symbolicUtil.getVariableId(null, value);
				ReferenceExpression reference = this.symbolicUtil.getSymRef(value);
				SymbolicExpression varValue = state.getVariableValue(dyscopeId, vid);
				SymbolicExpression objectValue;

				try {
					objectValue = this.universe.dereference(varValue, reference);
				} catch (SARLException e) {
					return;
				}
				reachableHeapObjectsOfValue(state, objectValue, reachable);
			}
		} else {
			int numArgs = value.numArguments();

			for (int i = 0; i < numArgs; i++) {
				SymbolicObject arg = value.argument(i);
				SymbolicObjectKind kind = arg.symbolicObjectKind();

				switch (kind) {
				case BOOLEAN:
				case INT:
				case NUMBER:
				case STRING:
				case CHAR:
				case TYPE:
				case TYPE_SEQUENCE:
					break;
				case EXPRESSION:
					reachableHeapObjectsOfValue(state, (SymbolicExpression) arg, reachable);
					break;
				case SEQUENCE: {
					Iterator<? extends SymbolicExpression> iter = ((SymbolicSequence<?>) arg).iterator();

					while (iter.hasNext()) {
						SymbolicExpression expr = iter.next();

						reachableHeapObjectsOfValue(state, expr, reachable);
					}
				}
				}
			}
		}
	}

	private boolean isPointer(SymbolicExpression value) {
		if (value.type().equals(typeFactory.pointerSymbolicType()))
			return true;
		return false;
	}

	private boolean hasNonEmptyHeaps(State state) {
		int numDyscopes = state.numDyscopes();

		for (int dyscopeId = 0; dyscopeId < numDyscopes; dyscopeId++) {
			DynamicScope dyscope = state.getDyscope(dyscopeId);
			SymbolicExpression heap = dyscope.getValue(0);

			if (!heap.isNull())
				return true;
		}
		return false;
	}

	private void computeOldToNewHeapPointers(State state, Map<SymbolicExpression, SymbolicExpression> heapMemUnitsMap,
			Map<SymbolicExpression, SymbolicExpression> oldToNewExpressions) {
		if (heapMemUnitsMap.size() < 1)
			return;
		else {
			int numDyscopes = state.numDyscopes();

			for (int dyscopeID = 0; dyscopeID < numDyscopes; dyscopeID++) {
				DynamicScope dyscope = state.getDyscope(dyscopeID);
				int numVars = dyscope.numberOfValues();

				for (int vid = 0; vid < numVars; vid++) {
					computeNewHeapPointer(dyscope.getValue(vid), heapMemUnitsMap, oldToNewExpressions);
				}
			}
		}
	}

	// /**
	// * renames all collectible symbolic constants. Note: this method should
	// only
	// * be called when necessary.
	// *
	// * @param state
	// * @return
	// */
	// private ImmutableState updateAllSymbols(ImmutableState state) {
	//
	// }

	@SuppressWarnings("incomplete-switch")
	private void computeNewHeapPointer(SymbolicExpression value,
			Map<SymbolicExpression, SymbolicExpression> heapMemUnitsMap,
			Map<SymbolicExpression, SymbolicExpression> oldToNewHeapPointers) {
		if (value.isNull())
			return;
		else if (!this.isPointer(value)) {
			int numArgs = value.numArguments();

			for (int i = 0; i < numArgs; i++) {
				SymbolicObject arg = value.argument(i);
				SymbolicObjectKind kind = arg.symbolicObjectKind();

				switch (kind) {
				case BOOLEAN:
				case INT:
				case NUMBER:
				case STRING:
				case CHAR:
				case TYPE:
				case TYPE_SEQUENCE:
					break;
				default:
					switch (kind) {
					case EXPRESSION:
						computeNewHeapPointer((SymbolicExpression) arg, heapMemUnitsMap, oldToNewHeapPointers);
						break;
					case SEQUENCE: {
						Iterator<? extends SymbolicExpression> iter = ((SymbolicSequence<?>) arg).iterator();

						while (iter.hasNext()) {
							SymbolicExpression expr = iter.next();

							computeNewHeapPointer(expr, heapMemUnitsMap, oldToNewHeapPointers);
						}
					}
					}
				}
			}
		} else if (symbolicUtil.isPointerToHeap(value)) {
			SymbolicExpression heapObjPtr = this.symbolicUtil.heapMemUnit(value);
			SymbolicExpression newHeapObjPtr = heapMemUnitsMap.get(heapObjPtr);

			if (newHeapObjPtr != null && !oldToNewHeapPointers.containsKey(value)) {
				if (newHeapObjPtr.isNull())
					oldToNewHeapPointers.put(value, newHeapObjPtr);
				else {
					ReferenceExpression ref = symbolicUtil.referenceToHeapMemUnit(value);
					SymbolicExpression newPointer = symbolicUtil.extendPointer(newHeapObjPtr, ref);

					oldToNewHeapPointers.put(value, newPointer);
				}
			}
		}
	}

	private void addOldToNewHeapMemUnits(Map<Integer, Integer> oldID2NewID, SymbolicExpression heapPointer,
			ReferenceExpression fieldRef, Map<SymbolicExpression, SymbolicExpression> oldToNewMap) {
		for (Map.Entry<Integer, Integer> entry : oldID2NewID.entrySet()) {
			ReferenceExpression oldRef = universe.arrayElementReference(fieldRef, universe.integer(entry.getKey()));
			SymbolicExpression oldPtr = this.symbolicUtil.setSymRef(heapPointer, oldRef);
			ReferenceExpression newRef = universe.arrayElementReference(fieldRef, universe.integer(entry.getValue()));
			SymbolicExpression newPtr = this.symbolicUtil.setSymRef(heapPointer, newRef);

			oldToNewMap.put(oldPtr, newPtr);
		}
	}

	/**
	 * Rename all symbolic constants of the state. Trying to use the new interface
	 * (canonicRenamer) provided by SARL.
	 * 
	 * @param state
	 * @return
	 * @throws CIVLHeapException
	 */
	private ImmutableState collectHavocVariables(State state) throws CIVLHeapException {
		ImmutableState theState = (ImmutableState) state;

		if (theState.collectibleCounts[ModelConfiguration.HAVOC_PREFIX_INDEX] < 1)
			return theState;

		int numDyscopes = theState.numDyscopes();
		CanonicalRenamer canonicRenamer = universe.canonicalRenamer(
				ModelConfiguration.SYMBOL_PREFIXES[ModelConfiguration.HAVOC_PREFIX_INDEX],
				this.isReservedSymbolicConstant);
		ImmutableDynamicScope[] newScopes = new ImmutableDynamicScope[numDyscopes];
		boolean change = false;

		for (int dyscopeId = 0; dyscopeId < numDyscopes; dyscopeId++) {
			ImmutableDynamicScope oldScope = theState.getDyscope(dyscopeId);
			ImmutableDynamicScope newScope = oldScope.updateSymbolicConstants(canonicRenamer);

			change = change || newScope != oldScope;
			newScopes[dyscopeId] = newScope;
		}
		if (!change)
			newScopes = null;

		BooleanExpression oldPathCondition = theState.getPermanentPathCondition();
		BooleanExpression newPathCondition = (BooleanExpression) canonicRenamer.apply(oldPathCondition);

		if (oldPathCondition == newPathCondition)
			newPathCondition = null;
		else
			change = true;

		ImmutableState tmpState = applyToProcessStates(theState, canonicRenamer);

		if (tmpState != theState) {
			theState = tmpState;
			change = true;
		}
		if (change) {
			theState = ImmutableState.newState(theState, null, newScopes, newPathCondition);
			theState = theState.updateCollectibleCount(ModelConfiguration.HAVOC_PREFIX_INDEX,
					canonicRenamer.getNumNewNames());
		}
		return theState;
	}

	@Override
	public ImmutableState setLocation(State state, int pid, Location location) {
		return this.setLocation(state, pid, location, false);
	}

	@Override
	public MemoryUnitFactory memUnitFactory() {
		return this.memUnitFactory;
	}

	@Override
	public Map<Variable, SymbolicExpression> inputVariableValueMap(State state) {
		Map<Variable, SymbolicExpression> result = new LinkedHashMap<>();

		// If the root process has no stack entry, return a empty map:
		if (state.getProcessState(0).stackSize() > 0) {
			// If the parameter is a merged state, the dynamic scope id of the
			// root
			// lexical scope may not be 0:
			int rootDysid = state.getDyscope(0, ModelConfiguration.STATIC_ROOT_SCOPE);

			for (Variable variable : this.inputVariables) {
				assert variable.scope().id() == ModelConfiguration.STATIC_ROOT_SCOPE;
				result.put(variable, state.getVariableValue(rootDysid, variable.vid()));
			}
		}
		return result;
	}

	@Override
	public State enterAtomic(State state, int pid) {
		ProcessState procState = state.getProcessState(pid);
		int atomicCount = procState.atomicCount();

		if (atomicCount == 0)
			state = getAtomicLock(state, pid);
		return this.setProcessState(state, procState.incrementAtomicCount());
	}

	@Override
	public State leaveAtomic(State state, int pid) {
		ProcessState procState = state.getProcessState(pid);
		int atomicCount = procState.atomicCount();

		if (atomicCount == 1)
			state = releaseAtomicLock(state);
		return this.setProcessState(state, procState.decrementAtomicCount());
	}

	@Override
	public Pair<State, SymbolicConstant> getFreshSymbol(State state, int index, SymbolicType type) {
		ImmutableState immutableState = (ImmutableState) state;
		int count = immutableState.collectibleCounts[index];
		SymbolicConstant newSymbol = universe
				.symbolicConstant(universe.stringObject(ModelConfiguration.SYMBOL_PREFIXES[index] + count), type);
		State newState = immutableState.updateCollectibleCount(index, count + 1);

		return new Pair<>(newState, newSymbol);
	}

	@Override
	public Pair<State, SymbolicExpression> valueSetHavoc(State state, SymbolicExpression value,
			SymbolicExpression valueSetTemplate) {
		ImmutableState immutableState = (ImmutableState) state;
		int index = ModelConfiguration.HAVOC_PREFIX_INDEX;
		int count = immutableState.collectibleCounts[index];
		String prefix = ModelConfiguration.SYMBOL_PREFIXES[index];
		dev.civl.sarl.util.Pair<SymbolicExpression, Integer> result = universe.valueSetHavoc(value, valueSetTemplate,
				prefix, count);
		State newState = immutableState.updateCollectibleCount(index, result.right);

		return new Pair<>(newState, result.left);
	}

	@Override
	public State emptyState(int nprocs) {
		ImmutableProcessState processes[] = new ImmutableProcessState[nprocs];
		ImmutableDynamicScope dyscopes[] = new ImmutableDynamicScope[0];
		ImmutableState result;

		for (int i = 0; i < nprocs; i++)
			processes[i] = new ImmutableProcessState(i, false);
		result = new ImmutableState(processes, dyscopes, universe.trueExpression());
		result.collectibleCounts = new int[ModelConfiguration.SYMBOL_PREFIXES.length];
		return result;
	}

	@Override
	public void setConfiguration(CIVLConfiguration config) {
		this.config = config;
	}

	@Override
	public ImmutableState addToPathcondition(State state, int pid, BooleanExpression clause) {
		ImmutableState imuState = (ImmutableState) state;
		BooleanExpression partialPathConditions[] = imuState.copyOfPartialPathConditionStack(pid);
		int head = partialPathConditions.length - 1;

		if (head >= 0) {
			partialPathConditions[head] = universe.and(partialPathConditions[head], clause);
			return imuState.setPartialPathConditionStack(pid, partialPathConditions);
		}
		BooleanExpression newPathCondition = universe.and(imuState.getPermanentPathCondition(), clause);

		return imuState.setPermanentPathCondition(newPathCondition);
	}

	@Override
	public SymbolicExpression processValue(int pid) {
		if (pid == -2)
			return this.nullProcessValue;
		if (pid < 0)
			return undefinedProcessValue;
		if (pid < maxProcs) {
			return processValues[pid];
		} else {
			String errorMessage = "pid is " + pid + " which is greater the upper bound " + maxProcs
					+ ". So you need to specify a larger maxProcs(-maxProcs=num) through command line";

			throw new CIVLException(errorMessage, null);
		}
	}

	@Override
	public ImmutableState addReadWriteRecords(State state, int pid, SymbolicExpression memValue, boolean isRead) {
		ImmutableState imuState = ((ImmutableState) state);

		if (isRead) {
			DynamicMemoryLocationSet newRsStack[] = imuState.getProcessState(pid).getReadSets(true);
			int head = newRsStack.length - 1;

			newRsStack[head] = memoryLocationSetFactory.addReference(newRsStack[head], memValue);
			return imuState.setReadSetStack(pid, newRsStack);
		} else {
			DynamicMemoryLocationSet newWsStack[] = imuState.getProcessState(pid).getWriteSets(true);
			int head = newWsStack.length - 1;

			newWsStack[head] = memoryLocationSetFactory.addReference(newWsStack[head], memValue);
			return imuState.setWriteSetStack(pid, newWsStack);
		}
	}

	@Override
	public DynamicMemoryLocationSet peekReadWriteSet(State state, int pid, boolean isRead) {
		ImmutableState imuState = ((ImmutableState) state);
		DynamicMemoryLocationSet[] stack;

		if (isRead)
			stack = imuState.getProcessState(pid).getReadSets(false);
		else
			stack = imuState.getProcessState(pid).getWriteSets(false);
		if (stack.length > 0) {
			int head = stack.length - 1;

			return stack[head];
		} else
			return null;
	}

	@Override
	public State pushEmptyReadWrite(State state, int pid, boolean isRead) {
		DynamicMemoryLocationSet newEmptySet = memoryLocationSetFactory.empty();
		ImmutableState imuState = ((ImmutableState) state);

		if (isRead) {
			DynamicMemoryLocationSet[] rsStack = imuState.getProcessState(pid).getReadSets(true);
			DynamicMemoryLocationSet[] newRsStack = Arrays.copyOf(rsStack, rsStack.length + 1);
			int head = rsStack.length;

			newRsStack[head] = newEmptySet;
			return imuState.setReadSetStack(pid, newRsStack);
		} else {
			DynamicMemoryLocationSet[] wsStack = imuState.getProcessState(pid).getWriteSets(true);
			DynamicMemoryLocationSet[] newWsStack = Arrays.copyOf(wsStack, wsStack.length + 1);
			int head = wsStack.length;

			newWsStack[head] = newEmptySet;
			return imuState.setWriteSetStack(pid, newWsStack);
		}
	}

	@Override
	public State popReadWriteSet(State state, int pid, boolean isRead) {
		ImmutableState imuState = ((ImmutableState) state);

		if (isRead) {
			DynamicMemoryLocationSet[] rsStack = imuState.getProcessState(pid).getReadSets(true);

			if (rsStack.length == 0)
				throw new CIVLInternalException("Attempt to pop an empty read set stack",
						imuState.getProcessState(pid).getLocation());
			DynamicMemoryLocationSet[] newRsStack = Arrays.copyOf(rsStack, rsStack.length - 1);

			return imuState.setReadSetStack(pid, newRsStack);
		} else {
			DynamicMemoryLocationSet[] wsStack = imuState.getProcessState(pid).getWriteSets(true);

			if (wsStack.length == 0)
				throw new CIVLInternalException("Attempt to pop an empty write set stack",
						imuState.getProcessState(pid).getLocation());
			DynamicMemoryLocationSet[] newWsStack = Arrays.copyOf(wsStack, wsStack.length - 1);

			return imuState.setWriteSetStack(pid, newWsStack);
		}
	}

	@Override
	public State pushAssumption(State state, int pid, BooleanExpression assumption) {
		ImmutableState imuState = ((ImmutableState) state);
		BooleanExpression[] ppcStack = imuState.copyOfPartialPathConditionStack(pid);
		BooleanExpression[] ppcNewStack = Arrays.copyOf(ppcStack, ppcStack.length + 1);
		int head = ppcStack.length;

		ppcNewStack[head] = assumption;
		return imuState.setPartialPathConditionStack(pid, ppcNewStack);
	}

	@Override
	public State popAssumption(State state, int pid) {
		ImmutableState imuState = ((ImmutableState) state);
		BooleanExpression[] ppcStack = imuState.copyOfPartialPathConditionStack(pid);
		BooleanExpression[] ppcNewStack = Arrays.copyOf(ppcStack, ppcStack.length - 1);

		return imuState.setPartialPathConditionStack(pid, ppcNewStack);
	}

	@Override
	public SymbolicExpression scopeValue(int sid) {
		SymbolicExpression result;

		if (sid == ModelConfiguration.DYNAMIC_NULL_SCOPE)
			return this.nullScopeValue;
		if (sid == ModelConfiguration.DYNAMIC_UNDEFINED_SCOPE)
			return this.undefinedScopeValue;
		if (sid < SCOPE_VALUES_INIT_SIZE)
			return smallScopeValues[sid];

		// key := sid - INIT_SIZE;
		// value := scope_value_of(sid);
		int key = sid - SCOPE_VALUES_INIT_SIZE;

		while (key >= bigScopeValues.size())
			bigScopeValues.addAll(nullList);
		result = bigScopeValues.get(key);
		if (result == null) {
			result = dyscopeIDToScopeValue.apply(sid);
			bigScopeValues.set(key, result);
		}
		return result;
	}

	@Override
	public void setSymbolicUtility(SymbolicUtility symbolicUtility) {
		this.symbolicUtil = symbolicUtility;
	}

	@Override
	public boolean isScopeIdDefined(int sid) {
		return ModelConfiguration.DYNAMIC_UNDEFINED_SCOPE == sid;
	}

	@Override
	public int getDyscopeId(SymbolicExpression scopeValue) {
		return scopeValueToDyscopeID.apply(scopeValue).intValue();
	}

	@Override
	public SymbolicExpression undefinedScopeValue() {
		return this.undefinedScopeValue;
	}

	@Override
	public SymbolicExpression nullScopeValue() {
		return this.nullScopeValue;
	}

	/**
	 * Given an array mapping old dyscope ids to new dyscope ids, returns a
	 * substituter which performs this mapping on symbolic expressions.
	 * 
	 * This method performs caching.
	 * 
	 * @param oldToNew An array in which oldToNew[i] == j means that the dyscope
	 *                 with id "i" should now have id "j"
	 * @return The substituter that performs this remapping of dyscope ids to
	 *         symbolic expressions
	 */
	private UnaryOperator<SymbolicExpression> getDyscopeSubstituter(int[] oldToNew) {
		IntArray key = new IntArray(oldToNew);
		UnaryOperator<SymbolicExpression> substituter = dyscopeSubMap.get(key);

		if (substituter == null) {
			substituter = universe.mapSubstituter(scopeSubMap(oldToNew));
			dyscopeSubMap.putIfAbsent(key, substituter);
		}
		return substituter;
	}
}
