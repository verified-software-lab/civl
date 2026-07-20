package dev.civl.mc.kripke.common;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import dev.civl.mc.config.IF.CIVLConstants.DeadlockKind;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.kripke.IF.Enabler;
import dev.civl.mc.model.IF.CIVLFunction;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLTypeFactory;
import dev.civl.mc.model.IF.ModelConfiguration;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.contract.DependsEvent;
import dev.civl.mc.model.IF.contract.FunctionBehavior;
import dev.civl.mc.model.IF.contract.FunctionContract;
import dev.civl.mc.model.IF.contract.MemoryEvent;
import dev.civl.mc.model.IF.contract.NamedFunctionBehavior;
import dev.civl.mc.model.IF.expression.AbstractFunctionCallExpression;
import dev.civl.mc.model.IF.expression.AddressOfExpression;
import dev.civl.mc.model.IF.expression.ArrayLambdaExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression.BINARY_OPERATOR;
import dev.civl.mc.model.IF.expression.CastExpression;
import dev.civl.mc.model.IF.expression.ConditionalExpression;
import dev.civl.mc.model.IF.expression.DereferenceExpression;
import dev.civl.mc.model.IF.expression.DomainGuardExpression;
import dev.civl.mc.model.IF.expression.DotExpression;
import dev.civl.mc.model.IF.expression.DynamicTypeOfExpression;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.Expression.ExpressionKind;
import dev.civl.mc.model.IF.expression.ExtendedQuantifiedExpression;
import dev.civl.mc.model.IF.expression.FunctionCallExpression;
import dev.civl.mc.model.IF.expression.FunctionGuardExpression;
import dev.civl.mc.model.IF.expression.LHSExpression;
import dev.civl.mc.model.IF.expression.LambdaExpression;
import dev.civl.mc.model.IF.expression.MemoryUnitExpression;
import dev.civl.mc.model.IF.expression.QuantifiedExpression;
import dev.civl.mc.model.IF.expression.RecDomainLiteralExpression;
import dev.civl.mc.model.IF.expression.RegularRangeExpression;
import dev.civl.mc.model.IF.expression.ScopeofExpression;
import dev.civl.mc.model.IF.expression.SizeofExpression;
import dev.civl.mc.model.IF.expression.SizeofTypeExpression;
import dev.civl.mc.model.IF.expression.SubscriptExpression;
import dev.civl.mc.model.IF.expression.SystemGuardExpression;
import dev.civl.mc.model.IF.expression.UnaryExpression;
import dev.civl.mc.model.IF.expression.VariableExpression;
import dev.civl.mc.model.IF.expression.reference.ArraySliceReference;
import dev.civl.mc.model.IF.expression.reference.MemoryUnitReference;
import dev.civl.mc.model.IF.location.Location;
import dev.civl.mc.model.IF.statement.AssignStatement;
import dev.civl.mc.model.IF.statement.AtomicLockAssignStatement;
import dev.civl.mc.model.IF.statement.CallOrSpawnStatement;
import dev.civl.mc.model.IF.statement.CivlParForSpawnStatement;
import dev.civl.mc.model.IF.statement.DomainIteratorStatement;
import dev.civl.mc.model.IF.statement.MallocStatement;
import dev.civl.mc.model.IF.statement.ParallelAssignStatement;
import dev.civl.mc.model.IF.statement.ReturnStatement;
import dev.civl.mc.model.IF.statement.Statement;
import dev.civl.mc.model.IF.statement.Statement.StatementKind;
import dev.civl.mc.model.IF.type.CIVLArrayType;
import dev.civl.mc.model.IF.type.CIVLCompleteArrayType;
import dev.civl.mc.model.IF.type.CIVLFunctionType;
import dev.civl.mc.model.IF.type.CIVLHeapType;
import dev.civl.mc.model.IF.type.CIVLPointerType;
import dev.civl.mc.model.IF.type.CIVLSetType;
import dev.civl.mc.model.IF.type.CIVLStructOrUnionType;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.type.StructOrUnionField;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.semantics.IF.Evaluator;
import dev.civl.mc.semantics.IF.Semantics;
import dev.civl.mc.semantics.IF.Transition;
import dev.civl.mc.state.IF.DynamicScope;
import dev.civl.mc.state.IF.ProcessState;
import dev.civl.mc.state.IF.StackEntry;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.StateFactory;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.mc.util.IF.Pair;
import dev.civl.mc.util.IF.SeqSet;
import dev.civl.sarl.IF.Reasoner;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.ArrayElementReference;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NTReferenceExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.ReferenceExpression;
import dev.civl.sarl.IF.expr.ReferenceExpression.ReferenceKind;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;
import dev.civl.sarl.IF.expr.TupleComponentReference;
import dev.civl.sarl.IF.number.IntegerNumber;
import dev.civl.sarl.IF.object.SymbolicObject;
import dev.civl.sarl.IF.object.SymbolicSequence;
import dev.civl.sarl.IF.type.SymbolicArrayType;
import dev.civl.sarl.IF.type.SymbolicCompleteArrayType;
import dev.civl.sarl.IF.type.SymbolicFunctionType;
import dev.civl.sarl.IF.type.SymbolicMapType;
import dev.civl.sarl.IF.type.SymbolicSetType;
import dev.civl.sarl.IF.type.SymbolicTupleType;
import dev.civl.sarl.IF.type.SymbolicType;
import dev.civl.sarl.IF.type.SymbolicTypeSequence;
import dev.civl.sarl.IF.type.SymbolicUnionType;

/**
 * <p>
 * A worker object created to compute an ample set for one given {@link State}.
 * This worker is created and owned by a {@link SimpleEnabler}. It maintains a
 * reference to the enabler to access constant fields and utility methods.
 * </p>
 * 
 * <p>
 * This class makes extensive use of {@link SeqSet}s to represent sets of
 * objects present in the state. A leaf in the SeqSet is an {@code int} array of
 * length 2, 3, or 4. The first two components are always a dynamic scope ID and
 * a static variable ID. These specify a unique variable instance in the state.
 * If the length is two, then the leaf represents the object which is that
 * entire variable. If the variable is a heap, then the third integer represents
 * a row in the heap table, which corresponds to a specific {@code $malloc}
 * statement. Hence a length-3 leaf represents all objects created in a specific
 * heap by a single {@code $malloc} statement. If the length is 4, the fourth
 * integer specifies one object created by that {@code $malloc} statement.
 * </p>
 * <p>
 * In the future, we might consider getting even more precise and specifying
 * sub-components of objects, such as array slices, or particular fields of
 * structures. But for now, only complete objects can be specified.
 * </p>
 * 
 * @author siegel
 */
public class SimpleEnablerWorker {

	// Constants ...

	// /**
	// * An integer which will be used to insert a pair (terminationCode, pid)
	// * into a SeqSet to represent an imaginary "termination" variable for a
	// * process. Every process p will have (terminationCode,p) in its
	// reachWrite
	// * set, as long as that process has not terminated. A process p that is at
	// a
	// * blocked wait statement in which the argument evaluates to q will have
	// * (terminationCode,q) in its depends set. This implies that any ample set
	// * including p must also include q. Otherwise, it would be possible for a
	// * statement (wait on q) dependent on a statement in the ample set (any
	// * enabled statement in p) to occur before anything in the ample set
	// occurs.
	// */
	// public final static int terminationCode = Integer.MAX_VALUE;

	/**
	 * This is the string used as prefix for symbolic constants that result from
	 * havoc. (E.g., "Y".)
	 */
	public final static String havocPrefix = ModelConfiguration.SYMBOL_PREFIXES[ModelConfiguration.HAVOC_PREFIX_INDEX];

	// Fields...

	/**
	 * The {@link Enabler} that created this worker. The {@code enabler} provides
	 * many constant resources that this worker can access. It would be inefficient
	 * to duplicate all those references in each worker, since so many workers will
	 * be created and destroyed constantly.
	 */
	SimpleEnabler enabler;

	/**
	 * The state at which we are computing an ample set.
	 */
	State theState;

	/**
	 * The number of processes in the state. Note some of the processes may have
	 * terminated or may be {@code null}.
	 */
	int nprocs;

	/**
	 * A reasoner with context the path condition of the state.
	 */
	Reasoner reasoner;

	/**
	 * The evaluator used to evaluate expressions.
	 */
	Evaluator evaluator;

	/**
	 * The factory used to create new states. This is needed when analyzing
	 * transitions that involve a function call.
	 */
	StateFactory stateFactory;

	/**
	 * A utility class for analyzing and manipulating symbolic expressions specific
	 * to how they are used in CIVL-C.
	 */
	SymbolicUtility symbolicUtil;

	/**
	 * Factory for creating and manipulating CIVL types.
	 */
	CIVLTypeFactory typeFactory;

	/**
	 * The value type (symbolic type) of a heap. A symbolic expression representing
	 * the current state of a heap will have this type.
	 */
	SymbolicType heapSymbolicType;

	/**
	 * The value type for a pointer. All symbolic expressions representing a pointer
	 * value will have this type.
	 */
	SymbolicType pointerSymbolicType;

	/**
	 * The symbolic universe used to create and manipulate symbolic expressions.
	 */
	SymbolicUniverse universe;

	/**
	 * The guard values of the statements emanating from the current locations of
	 * the processes. Value in position [i][j] is the result of evaluating the guard
	 * of statement j of process i. These are initially {@code null} but are filled
	 * in as values are requested.
	 */
	BooleanExpression[][] theGuards;

	/**
	 * The transitions enabled in each process. Ragged array of length nprocs.
	 */
	Transition[][] enabledTransitions;

	/**
	 * After the ample set is computed, this flag tells you whether the ample set is
	 * the full set of enabled transitions.
	 */
	boolean full = false;

	/**
	 * The set of transitions which form an ample set. The objective of this class
	 * is to compute this set.
	 */
	Transition[] ampleSet = null;

	// Constructor...

	/**
	 * Creates a new worker. Initializes all fields.
	 * 
	 * @param enabler the {@link SimpleEnabler} that is creating this worker
	 * @param state   the {@link State} that this worker has been created to analyze
	 */
	SimpleEnablerWorker(SimpleEnabler enabler, State state) {
		this.enabler = enabler;
		this.evaluator = enabler.evaluator;
		this.typeFactory = evaluator.modelFactory().typeFactory();
		this.stateFactory = evaluator.stateFactory();
		this.symbolicUtil = evaluator.symbolicUtility();
		this.heapSymbolicType = typeFactory.heapSymbolicType();
		this.pointerSymbolicType = typeFactory.pointerSymbolicType();
		this.universe = evaluator.universe();
		this.reasoner = universe.reasoner(state.getPathCondition(universe));
		this.nprocs = state.numProcs();
		this.theGuards = new BooleanExpression[nprocs][];
		this.enabledTransitions = new Transition[nprocs][];
		this.theState = state;
		this.nprocs = theState.numProcs();
	}

	// Private methods...

	/**
	 * Adds a dyscope ID - variable pair to the given {@link SeqSet} {@code result},
	 * with some exceptions. If {@code variable} is {@code null}, or is an input
	 * variable, or is the atomic lock variable, or is a heap, this method is a
	 * no-op. Otherwise the pair is added to {@code result}.
	 * 
	 * @param result   the {@code SeqSet} to which the pair will be added
	 * @param dyid     the ID number of a dynamic scope in {@link #theState}
	 * @param variable a {@code Variable} that resides in the static scope
	 *                 corresponding to the dynamic scope {@code dyid}
	 */
	private void addVariable(SeqSet result, int dyid, Variable variable) {
		if (variable == null || variable.isInput() || variable == enabler.atomicLockVariable
				|| variable.type().isHeapType())
			return;

		int vid = variable.vid();

		assert vid >= 0;
		result.add(dyid, vid);
	}

	/**
	 * Adds the variable in the specified process to a set. The dynamic scope of the
	 * variable is found by looking at the current state of process pid in
	 * {@link #theState}.
	 * 
	 * @param result   the set to which the variable will be added
	 * @param pid      the process in which the variable is referenced
	 * @param variable the static variable
	 */
	private void addVariableInProc(SeqSet result, int pid, Variable variable) {
		if (variable == null || variable == enabler.atomicLockVariable)
			return;

		int dyid = theState.getDyscopeID(pid, variable);

		if (dyid < 0 || dyid >= theState.numDyscopes())
			return;
		addVariable(result, dyid, variable);
	}

	/**
	 * Adds a variable instance to a set of objects. The set represents a set of
	 * objects that exists at a certain state. A variable instance is represented as
	 * a pair (d,v), where d is the dynamic scope ID and v is the variable.
	 * 
	 * <p>
	 * A variable can be instantiated multiple (or 0) times because it is
	 * instantiated each time control in some process enters the variable's scope.
	 * Because of recursion and multiple processes, many instances of the variable
	 * may exist in the given state. The particular instance is specified by the
	 * state, pid, and variable. In the given state, there is at most one "visible"
	 * instance of the variable from the specified process: the top frame of the
	 * process's call stack points to a dynamic scope. If the variable exists in
	 * that scope, that is the instance, else the parent dynamic scope is examined,
	 * and so on until the variable is either found or the root dynamic scope is
	 * reached. When a variable is used in an expression, this is how the instance
	 * of the variable is found.
	 * </p>
	 * 
	 * <p>
	 * The specified {@code state} should be {@link #theState} or a "super-state" of
	 * {@link #theState}. A super-state should contain all dyscopes of
	 * {@link #theState} but possibly additional dyscopes. The only way this is
	 * currently used is where the super-state is obtained by executing a call
	 * statement from {@link #theState}. This pushes a new frame onto the call stack
	 * of a process and creates one or more dyscopes to create the context for the
	 * execution of the called function.
	 * </p>
	 * 
	 * If {@code filter} is true, then a variable will not be added to the
	 * {@code result} unless its dyscope exists in {@code #theState}. Hence if
	 * {@code state} is obtained by executing a call, then formal parameters and
	 * local variables of the new dyscope(s) will not be added to {@code result};
	 * this method will be a no-op.
	 * </p>
	 * 
	 * <p>
	 * If the variable instance is already in the given set, this will be a no-op.
	 * Certain variables are ignored and will not be added. Currently these are: the
	 * atomic lock variable. Cases where dyscope ID < 0 are also ignored. E.g., if a
	 * function type has an array type in which the length n is also a formal
	 * parameter, then the function identifier will have a reference to n in its
	 * type. n does not exist until the function is called, so the dyscope ID of n
	 * is undefined, which is -2.
	 * </p>
	 * 
	 * @param result   the set to which the variable should be added
	 * @param state    the state in which this variable instance exists
	 * @param pid      the ID of the process which references this variable
	 * @param variable the (static) variable to search for
	 * 
	 * @param filter   do not add a variable instance to the result if the dyscope
	 *                 of that instance does not exist in {@link #theState}
	 */
	private void addVariableInProc(SeqSet result, State state, int pid, Variable variable, boolean filter) {
		if (variable == null || variable == enabler.atomicLockVariable)
			return;

		int dyid = state.getDyscopeID(pid, variable);

		if (dyid < 0 || (filter && dyid >= theState.numDyscopes()))
			return;
		addVariable(result, dyid, variable);
	}

	/**
	 * <p>
	 * Adds to {@code result} an over-approximation to the set of memory locations
	 * pointed to by a pointer value. The pointer value specifies a sub-object
	 * (which may the whole object) of an object that exists in a state. An object
	 * is either a variable instance or the object that is created by a single call
	 * to {@code malloc}. This method will find and add the entire object to the
	 * given set {@code result}.
	 * </p>
	 * 
	 * <p>
	 * If the object does not exist in {@link #theState}, this is a no-op. If the
	 * object is already in {@code result}, this is a no-op. If the pointer value is
	 * the NULL pointer or undefined, this is a no-op.
	 * </p>
	 * 
	 * @param result  the set to which the memory locations should be added
	 * @param state   the state in which the pointer is evaluated
	 * @param source  source information used for error-reporting; should be the
	 *                piece of source code that was evaluated to yield the pointer
	 *                value
	 * @param pointer a non-null pointer value
	 * @throws NoReductionException if the pointer is not concrete, and therefore no
	 *                              reasonable over-estimate of the pointed-to
	 *                              objects can be made
	 */
	private void addPointer(SeqSet result, State state, CIVLSource source, SymbolicExpression pointer)
			throws NoReductionException {
		assert pointer.type() == typeFactory.pointerSymbolicType();
		if (pointer == symbolicUtil.nullPointer() || pointer == symbolicUtil.undefinedPointer())
			return;
		if (pointer.operator() != SymbolicOperator.TUPLE)
			throw new NoReductionException();

		int dyscopeID = stateFactory.getDyscopeId(symbolicUtil.getScopeValue(pointer));

		// ignore new dyscopes in state but not in theState...
		if (dyscopeID < 0 || dyscopeID >= theState.numDyscopes())
			return;

		int variableID = symbolicUtil.getVariableId(source, pointer);

		assert (variableID >= 0); // can a variable have a negative ID?
		// note: every dyscope has a heap, which is variable ID 0

		SymbolicExpression object = state.getVariableValue(dyscopeID, variableID);

		if (object.type() != heapSymbolicType) {
			Scope scope = state.getDyscope(dyscopeID).lexicalScope();
			Variable var = scope.variable(variableID);

			addVariable(result, dyscopeID, var);
		} else { // ... of arrayElementRef of tupleComponentRef of IdentityRef
			ReferenceExpression ref = symbolicUtil.getSymRef(pointer);
			ReferenceExpression ref1 = ref, ref2 = null, ref3 = null;

			while (ref1 instanceof NTReferenceExpression) {
				ref3 = ref2;
				ref2 = ref1;
				ref1 = ((NTReferenceExpression) ref1).getParent();
			}
			assert ref1.referenceKind() == ReferenceKind.IDENTITY;
			assert ref2 instanceof TupleComponentReference;
			assert ref3 instanceof ArrayElementReference;

			int mallocIndex = ((TupleComponentReference) ref2).getIndex().getInt();
			NumericExpression objectIndex = ((ArrayElementReference) ref3).getIndex();
			IntegerNumber objectNumber = (IntegerNumber) universe.extractNumber(objectIndex);

			if (objectNumber == null)
				result.add(dyscopeID, variableID, mallocIndex);
			else
				result.add(dyscopeID, variableID, mallocIndex, objectNumber.intValue());
		}
	}

	/**
	 * <p>
	 * Does a symbolic type {@code type}, contain, as a sub-type, the pointer type
	 * {@link #pointerSymbolicType}?
	 * </p>
	 * 
	 * <p>
	 * Note: it would be better if there were a way to do this once, when the
	 * symbolic type is created. This may have to be implemented in SARL. Also: for
	 * now we are not using this method, instead relying on CIVL types to answer
	 * this question. But it would be more accurate to use the symbolic type.
	 * </p>
	 * 
	 * @param type any symbolic type
	 * @return {@code true} iff the pointer type occurs as a sub-type of
	 *         {@code type} (including {@code type} itself)
	 */
	@SuppressWarnings("unused")
	private boolean containsPointer(SymbolicType type) {
		if (type == pointerSymbolicType)
			return true;
		switch (type.typeKind()) {
		case ARRAY:
			return containsPointer(((SymbolicArrayType) type).baseType());
		case FUNCTION: {
			SymbolicFunctionType ftype = (SymbolicFunctionType) type;

			return containsPointer(ftype.outputType());
		}
		case MAP: {
			SymbolicMapType mtype = (SymbolicMapType) type;

			return containsPointer(mtype.keyType()) || containsPointer(mtype.valueType());
		}
		case SET:
			return containsPointer(((SymbolicSetType) type).elementType());
		case TUPLE:
			for (SymbolicType ftype : ((SymbolicTupleType) type).sequence())
				if (containsPointer(ftype))
					return true;
			return false;
		case UNION:
			for (SymbolicType ftype : ((SymbolicUnionType) type).sequence())
				if (containsPointer(ftype))
					return true;
			return false;
		case BOOLEAN:
		case CHAR:
		case INTEGER:
		case REAL:
		case UNINTERPRETED:
		}
		return false;
	}

	/**
	 * Determines whether an object has a static CIVL type which contains a
	 * reference type as a sub-type. By reference type, we mean either the pointer
	 * type or the {@code $mem} type.
	 * 
	 * @param state a {@link State} of the model
	 * @param obj   an integer sequence identifying an object in state {@code state}
	 * @return {@code true} iff the variable or heap-allocated object identified by
	 *         {@code obj} has a static type which contains the CIVL pointer or
	 *         {@code $mem} type as a sub-type
	 */
	private boolean containsPointerType(State state, int[] obj) {
		int len = obj.length, dyid = obj[0], vid = obj[1];
		DynamicScope ds = state.getDyscope(dyid);
		Variable var = ds.lexicalScope().variable(vid);
		CIVLType type = var.type();

		if (len == 2)
			return type.hasReferences();
		assert type.isHeapType();

		CIVLType elementType = ((CIVLHeapType) type).getMalloc(obj[2]).getStaticElementType();

		return elementType.hasReferences();
	}

	/**
	 * Finds all symbolic expressions occurring within a symbolic type, adding them
	 * to {@code result}. These symbolic expressions are necessarily of integer
	 * type, occurring as length expressions in array types.
	 * 
	 * @param result the collection to which the symbolic expressions will be added
	 * @param type   the type to be searched for symbolic expressions
	 */
	private void getExpressionsInType(Collection<SymbolicExpression> result, SymbolicType type) {
		switch (type.typeKind()) {
		case ARRAY:
			getExpressionsInType(result, ((SymbolicArrayType) type).baseType());
			if (type instanceof SymbolicCompleteArrayType)
				result.add(((SymbolicCompleteArrayType) type).extent());
			break;
		case FUNCTION: {
			SymbolicFunctionType ftype = (SymbolicFunctionType) type;

			getExpressionsInType(result, ftype.outputType());
			for (SymbolicType itype : ftype.inputTypes())
				getExpressionsInType(result, itype);
			break;
		}
		case MAP: {
			SymbolicMapType mtype = (SymbolicMapType) type;

			getExpressionsInType(result, mtype.keyType());
			getExpressionsInType(result, mtype.valueType());
			break;
		}
		case SET:
			getExpressionsInType(result, ((SymbolicSetType) type).elementType());
			break;
		case TUPLE:
			for (SymbolicType ftype : ((SymbolicTupleType) type).sequence())
				getExpressionsInType(result, ftype);
			break;
		case UNION:
			for (SymbolicType ftype : ((SymbolicUnionType) type).sequence())
				getExpressionsInType(result, ftype);
			break;
		case BOOLEAN:
		case CHAR:
		case INTEGER:
		case REAL:
		case UNINTERPRETED:
			// no pointers, nothing to do
		}
	}

	/**
	 * Computes over-approximation of set of objects that could be pointed to by
	 * some component of the given value. Objects that are in the given
	 * {@code state} but not in {@link #theState} will be filtered out, i.e., will
	 * not be added to {@code result}.
	 * 
	 * @param result set to which the objects will be added
	 * @param state  the state in which the value exists
	 * @param source source code info for error reporting; should correspond to the
	 *               expression that evaluated to {@code value}
	 * @param value  the value to search for pointers
	 * @throws NoReductionException if a pointer found in {@code value} is not
	 *                              concrete
	 */
	private void getPointedObjects(SeqSet result, State state, CIVLSource source, SymbolicExpression value)
			throws NoReductionException {
		Stack<SymbolicExpression> worklist = new Stack<>();

		worklist.push(value);
		while (!worklist.isEmpty()) {
			SymbolicExpression expr = worklist.pop();
			SymbolicType type = expr.type();

			if (type == null) {// a NULL object has null type, ignore
			} else if (expr.operator() == SymbolicOperator.APPLY && expr.argument(0) == enabler.hideFunction) {
				// do nothing. This is a special abstract function used
				// to hide pointers from this reachability analysis
			} else if (type == pointerSymbolicType) {
				addPointer(result, state, source, expr); // filters
			} else if (expr.operator() == SymbolicOperator.SYMBOLIC_CONSTANT) {
				// do nothing. temporary hack. these are the "Y" symbolic
				// constants used to initialize an uninitialized variable, and
				// also used by havoc. we will assume for now they can't contain
				// a pointer to anything. Same for inputs "X" and uninitialized
				// heap cells "H".
				// if (((StringObject) expr.argument(0)).getString()
				// .startsWith(havocPrefix)) {
				// } else if (containsPointer(type))
				// throw new NoReductionException();
			} else {
				for (SymbolicObject obj : expr.getArguments()) {
					switch (obj.symbolicObjectKind()) {
					case EXPRESSION:
						worklist.push((SymbolicExpression) obj);
						break;
					case TYPE:
						getExpressionsInType(worklist, (SymbolicType) obj);
						break;
					case SEQUENCE:
						for (SymbolicExpression se : (SymbolicSequence<?>) obj)
							worklist.push(se);
						break;
					case TYPE_SEQUENCE:
						for (SymbolicType stype : (SymbolicTypeSequence) obj)
							getExpressionsInType(worklist, stype);
						break;
					case BOOLEAN:
					case CHAR:
					case INT:
					case NUMBER:
					case STRING:
						// no pointers, nothing to do
					}
				}
			}
		}
	}

	/**
	 * Gets the value of an object in the specified state. The object is specified
	 * by a sequence of integers. If the object is a regular variable, the sequence
	 * consists of two integers: the dynamic scope ID and the variable ID. If the
	 * object is heap-allocated, the sequence consists of 4 integers: ID of the
	 * dynamic scope containing the heap, the variable ID of the heap variable in
	 * that scope, the row ID (corresponding to the malloc statement) in the heap
	 * table, and the column ID (corresponding to a single malloc call) within that
	 * row.
	 * 
	 * @param state    the state in which the value of the object will be found
	 * @param objectID the sequence of integers specifying the object
	 * @return the value of the specified object in {@code state}
	 */
	private SymbolicExpression getValue(State state, int[] objectID) {
		// dyscope, var; or dyscope, var, field, objID
		int dyscopeID = objectID[0];
		int variableID = objectID[1];
		SymbolicExpression value = state.getVariableValue(dyscopeID, variableID);

		if (objectID.length == 2) {
			return value;
		} else {
			assert objectID.length == 4;

			int mallocID = objectID[2];
			int objID = objectID[3];
			SymbolicExpression row, result;

			assert value.type() == heapSymbolicType;
			if (value.operator() == SymbolicOperator.TUPLE)
				row = (SymbolicExpression) value.argument(mallocID);
			else
				row = universe.tupleRead(value, universe.intObject(mallocID));
			if (row.operator() == SymbolicOperator.ARRAY)
				result = (SymbolicExpression) row.argument(objID);
			else
				result = universe.arrayRead(row, universe.integer(objID));
			return result;
		}
	}

	/**
	 * <p>
	 * Computes the set of objects that can be reached in one or more steps (pointer
	 * dereferences) from a given set of objects. There is a binary relation -> on
	 * the set of objects in a state: o1->o2 if o1 contains a pointer into some part
	 * of o2. Given a set of objects (specified as a {@link SeqSet}), this method
	 * will compute the set of objects that are reachable from the given set by
	 * traversing one or more edges of this relation.
	 * </p>
	 * 
	 * <p>
	 * The starting points are the objects specified by {@code objectSet}. These
	 * refer to objects in state {@code state}. Any of these objects may or may not
	 * exist in {@link #theState}. However, an edge o1->o2 is in the graph only if
	 * o2 is in {@link #theState}. I.e., while an initial object o1 may not be in
	 * {@link #theState}, all subsequent objects in a path from o1 must be in
	 * {@link #theState}. In particular, all objects added to the result will be in
	 * {@link #theState}. In all current usage, the only initial objects not in
	 * {@link #theState} are formal parameters of a newly called function.
	 * </p>
	 * 
	 * <p>
	 * This method assumes that if an object's static type does not contain a
	 * pointer type, then the value of that object can never include a pointer
	 * value. This assumption is unsound if pointer values can be cast to
	 * non-pointer values, e.g., if a pointer is cast to a {@code double} and stored
	 * in a variable of type {@code double}. This is a known limitation.
	 * </p>
	 * 
	 * @param result    the result of the irreflexive transitive closure,
	 *                  intersected with the set of objects belonging to
	 *                  {@link #theState}
	 * 
	 * @param objectSet the starting points; this set will not be modified (in)
	 * @param state     the state
	 * @param source    source object for this operation
	 * @throws NoReductionException if no over-approximation of the result can be
	 *                              obtained
	 */
	private void closeIrreflexive(SeqSet result, SeqSet objectSet, State state, CIVLSource source)
			throws NoReductionException {
		LinkedList<int[]> workset = new LinkedList<>();
		// objectSet and workset can refer to objects in state-theState
		// but result will not.

		for (int[] leaf : objectSet.getLeaves()) {
			if (containsPointerType(state, leaf))
				workset.add(leaf);
		}
		while (!workset.isEmpty()) {
			int[] objId = workset.remove();
			SymbolicExpression value = getValue(state, objId);
			SeqSet pointedObjects = new SeqSet();

			// getPointedObjects filters out objects not in theState...
			getPointedObjects(pointedObjects, state, source, value);
			for (int[] pObj : pointedObjects.getLeaves())
				if (result.add(pObj) && containsPointerType(state, pObj))
					workset.add(pObj);
		}
	}

	/**
	 * Finds all objects of a state that can be reached in one or more steps from
	 * the given set of variables, under the binary relation -> on the set of
	 * objects, where o1->o2 if o1 contains a pointer into some part of o2. Ignores
	 * variables that should be ignored according to the rules laid out in
	 * {@link #addVariable(SeqSet, int, Variable)}.
	 * 
	 * @param state  the state which specifies the values of all objects
	 * @param pid    the ID of the process which is referencing the variables; used
	 *               together with {@code state} to determine the variable instances
	 *               and the values stored
	 * @param source a source info object used for reporting errors
	 * @param vars   the set of variables which form the starting point of the
	 *               search
	 * @return the set of reachable objects, represented as a {@link SeqSet}
	 */
	private SeqSet findReachableIrreflexive(State state, int pid, CIVLSource source, Set<Variable> vars)
			throws NoReductionException {
		SeqSet input = new SeqSet(), result = new SeqSet();
		// input may contain objects not in theState, but result will
		// only get objects in theState

		for (Variable var : vars)
			addVariableInProc(input, state, pid, var, false);
		closeIrreflexive(result, input, state, source);
		return result;
	}

	/**
	 * <p>
	 * Computes the dependencies of a function call in the case where the function
	 * has a contract with a "depends_on" clause. An implemented or system function
	 * may have a function contract, and that contract may include one or more
	 * "depends_on" clauses. These clauses specify "access", "read" or "write"
	 * events, each of which has an argument of pointer type. These arguments may
	 * refer to the formal parameters of the function. The actual arguments of the
	 * function call are evaluated, the formal parameters are assigned the
	 * corresponding values, and then the depends_on expressions are evaluated to
	 * yield a set of pointer values. The objects pointed to by these pointer values
	 * are collected into the set returned.
	 * </p>
	 * 
	 * <p>
	 * The differences between "access", "read", and "write" are currently ignored.
	 * All three are treated the same.
	 * </p>
	 * 
	 * <p>
	 * Missing depends clauses: a missing depends clause is interpreted to mean
	 * nothing, i.e., the function could depend on anything. It is equivalent to
	 * specifying the universal set consisting of all memory locations for the
	 * depends clause.
	 * </p>
	 * 
	 * 
	 * <p>
	 * Behaviors: for a contract with multiple behaviors, the effective depends
	 * clause is the intersection of the depends sets of the enabled behaviors.
	 * Rationale: similar to the case of assigns clauses in ACSL, to say a statement
	 * S depends on X really means it is independent of all statements in the
	 * complement of X. If multiple behaviors are enabled, then all of the claims
	 * encoded by those behaviors should hold, i.e., S is independent of all
	 * statements in the union of the complements of the X_i, i.e., X depends on the
	 * the intersection of the X_i.
	 * </p>
	 * 
	 * TODO: need to support depends_on(a[0..n-1]) which is equivalent to
	 * depends_on(a[0], ..., a[n-1]). Where a is an array of pointers.
	 * a[0..n-1][0..1] where a is an array of array of pointers.
	 * 
	 * @param result    the set into which the dependent object of the call will be
	 *                  added
	 * @param state     the state from which the function is called
	 * @param pid       the ID of the process making the call
	 * @param statement the call statement
	 * @return <code>true</code> if the function has an enabled depends_on clause at
	 *         {@code state}, <code>false</code> otherwise. In the case of
	 *         {@code false} being returned, the <code>result</code> is not modified
	 * @throws UnsatisfiablePathConditionException if in the course of evaluating
	 *                                             some expression it is discovered
	 *                                             that the path condition of
	 *                                             {@code state} is unsatisfiable
	 * @throws NoReductionException                if the called function is a
	 *                                             system function or atomic
	 *                                             function, but no depends_on
	 *                                             clause is specified
	 */
	private boolean memFromContract(SeqSet result, State state, int pid, CallOrSpawnStatement statement)
			throws UnsatisfiablePathConditionException, NoReductionException {
		CIVLFunction function = enabler.getFunction(state, pid, statement);

		if (function.isPureFunction())
			return true; // no dependencies

		FunctionContract contract = function.functionContract();

		if (contract == null)
			return false;

		State newState = null; // after executing the call
		SeqSet otherSet = null, dependSet = null;

		/*
		 * otherSet contains ancillary references, such as the variables occurring in
		 * the assumes and depends_on clauses. For these, the union is taken over all
		 * behaviors. dependSet contains the actual objects pointed to by the depends_on
		 * clause. For these, the intersection is taken over all behaviors. The final
		 * result is the union of these two sets, but the result is only used if there
		 * is at least one enabled depends_on clause.
		 */
		if (contract.hasDependsClause()) { // process the default behavior
			FunctionBehavior behavior0 = contract.defaultBehavior();

			if (behavior0 == null) {
				// nothing known
			} else if (behavior0.dependsNoact()) { // depends_on \nothing
				return true;
			} else if (behavior0.numDependsEvents() == 0) {
				// nothing known
			} else { // there are some depends_on clauses
				dependSet = new SeqSet();
				otherSet = new SeqSet();
				newState = enabler.executeContract(state, pid, function, statement.arguments());
				for (DependsEvent event : behavior0.dependsEvents()) {
					if (event instanceof MemoryEvent) {
						MemoryEvent memEvent = (MemoryEvent) event;
						Set<Expression> memSet = memEvent.memoryUnits();

						for (Expression expr : memSet) {
							SymbolicExpression pointer = evaluator.evaluate(newState, pid, expr).value;

							assert pointer.type() == typeFactory.pointerSymbolicType();
							addPointer(dependSet, newState, expr.getSource(), pointer);
							findObjects(otherSet, newState, pid, expr);
						}
					}
				}
			}
		}
		for (NamedFunctionBehavior behavior : contract.namedBehaviors()) {
			Expression assumption = behavior.assumptions();

			if (assumption != null) {
				if (otherSet == null)
					otherSet = new SeqSet();
				if (newState == null)
					newState = enabler.executeContract(state, pid, function, statement.arguments());

				findObjects(otherSet, newState, pid, assumption);

				BooleanExpression assumptionValue = (BooleanExpression) evaluator.evaluate(newState, pid,
						assumption).value;

				if (reasoner.isValid(assumptionValue)) {
					if (behavior.dependsNoact()) { // depends_on \nothing
						if (dependSet == null)
							dependSet = new SeqSet();
						dependSet.clear();
					} else if (behavior.numDependsEvents() == 0) {
						// nothing known
					} else { // there are some depends_on clauses
						// new depend set will be intersection with old...
						SeqSet newDependSet = new SeqSet();

						if (newState == null)
							newState = enabler.executeContract(state, pid, function, statement.arguments());
						for (DependsEvent event : behavior.dependsEvents()) {
							if (event instanceof MemoryEvent) {
								MemoryEvent memEvent = (MemoryEvent) event;
								Set<Expression> memSet = memEvent.memoryUnits();

								for (Expression expr : memSet) {
									SymbolicExpression pointer = evaluator.evaluate(newState, pid, expr).value;
									SeqSet ptrSet = new SeqSet();

									assert pointer.type() == typeFactory.pointerSymbolicType();
									addPointer(ptrSet, newState, expr.getSource(), pointer);
									if (dependSet == null || dependSet.containsAll(ptrSet))
										newDependSet.addAll(ptrSet);
									findObjects(otherSet, newState, pid, expr);
								}
							}
						}
						dependSet = newDependSet;
					}
				}
			}
		} // end loop over named behaviors
		if (dependSet != null) {
			if (otherSet != null)
				result.addAll(otherSet);
			result.addAll(dependSet);
			return true;
		}
		return false;
	}

	/**
	 * Finds all objects referenced in a {@link MemoryUnitReference}, adding them to
	 * the specified {@code SeqSet}.
	 * 
	 * @param result the set to which the objects should be added
	 * @param state  the state to which the memory unit reference applies
	 * @param pid    the ID of the process containing the memory unit reference
	 * @param ref    the memory unit reference
	 * @throws UnsatisfiablePathConditionException if in the course of evaluating,
	 *                                             it is determined that the path
	 *                                             condition of {@code state} is
	 *                                             unsatisfiable
	 * @throws NoReductionException                if a non-concrete pointer is
	 *                                             encountered
	 */
	private void findObjects(SeqSet result, State state, int pid, MemoryUnitReference ref)
			throws UnsatisfiablePathConditionException, NoReductionException {
		if (ref == null)
			return;
		if (ref instanceof ArraySliceReference)
			findObjects(result, state, pid, ((ArraySliceReference) ref).index());
		findObjects(result, state, pid, ref.child());
	}

	/**
	 * Computes an over-approximation of the set of objects accessed (read or
	 * modified) by executing a statement from {@link #theState}.
	 *
	 * @param resultAll the set to which the computed set of all objects will be
	 *                  added (out variable)
	 * @param resultRO  the set to which the computed set of read-only objects will
	 *                  be added (out variable)
	 * @param pid       the ID of the process executing the statement
	 * @param statement the statement being executed
	 * @throws UnsatisfiablePathConditionException if in the course of this
	 *                                             computation it is discovered that
	 *                                             {@code state} has an
	 *                                             unsatisfiable path condition
	 * @throws NoReductionException                if a non-concrete pointer is
	 *                                             encountered
	 */
	private void computeMem(SeqSet resultAll, SeqSet resultWrite, int pid, Statement statement)
			throws UnsatisfiablePathConditionException, NoReductionException {
		StatementKind kind = statement.statementKind();

		switch (kind) {
		case ASSIGN: {
			if (statement instanceof AtomicLockAssignStatement) {
				AtomicLockAssignStatement as = (AtomicLockAssignStatement) statement;

				if (as.enterAtomic())
					computeMemAtomicBlock(resultAll, resultWrite, theState, pid, as);
			} else {
				AssignStatement as = (AssignStatement) statement;

				findAccessesLHS(resultAll, resultWrite, pid, as.getLhs());
				findObjects(resultAll, theState, pid, as.rhs());
			}
			break;
		}
		case CALL_OR_SPAWN: {
			CallOrSpawnStatement cs = (CallOrSpawnStatement) statement;

			if (cs.isSpawn() && enabler.config.getProcBound() > 0) {
				throw new NoReductionException();
			} else if (enabler.isYield(cs)) {
				if (stateFactory.processInAtomic(theState) != pid) {
					// second part of $yield: this proc re-obtains
					// atomic lock. For now, say depends on everything.
					// TODO: eventually do same thing we do for atomic-enter
					throw new NoReductionException();
				} // else: first part of $yield: no dependencies
			} else {
				findObjects(resultAll, theState, pid, cs.functionExpression());
				for (Expression arg : cs.arguments())
					findObjects(resultAll, theState, pid, arg);
				if (cs.lhs() != null) {
					findAccessesLHS(resultAll, resultWrite, pid, cs.lhs());
				}

				CIVLFunction function = enabler.getFunction(theState, pid, cs);

				if (function.isAtomicFunction() || function.isSystemFunction()) {
					SeqSet tmpSet = new SeqSet();

					if (memFromContract(tmpSet, theState, pid, cs)) {
						resultAll.addAll(tmpSet);
						resultWrite.addAll(tmpSet);
					} else if (function.startLocation() != null) {
						computeMemAtomicFunction(resultAll, resultWrite, theState, pid, cs.function(), cs.arguments());
					} else
						throw new NoReductionException();
				}
			}
			break;
		}
		case CIVL_PAR_FOR_ENTER: {
			CivlParForSpawnStatement ps = (CivlParForSpawnStatement) statement;

			findObjects(resultAll, theState, pid, ps.domain());
			findObjects(resultAll, theState, pid, ps.domSizeVar());
			findObjects(resultAll, theState, pid, ps.parProcsVar());
			break;
		}
		case DOMAIN_ITERATOR: {
			DomainIteratorStatement ds = (DomainIteratorStatement) statement;

			findObjects(resultAll, theState, pid, ds.domain());
			// don't think these are needed...
			// computeObjectsIn(result, pid, ds.getLiteralDomCounter());
			// computeObjectsIn(result, pid, ds.loopVariables());
			break;
		}
		case MALLOC: {
			MallocStatement ms = (MallocStatement) statement;

			findObjects(resultAll, theState, pid, ms.getScopeExpression());
			findObjects(resultAll, theState, pid, ms.getSizeExpression());
			findAccessesLHS(resultAll, resultWrite, pid, ms.getLHS());
			break;
		}
		case NOOP:
			break; // nothing to do
		case PARALLEL_ASSIGN: {
			ParallelAssignStatement ps = (ParallelAssignStatement) statement;

			for (Pair<LHSExpression, Expression> pair : ps.assignments()) {
				findObjects(resultAll, theState, pid, pair.left);
				findAccessesLHS(resultAll, resultWrite, pid, pair.left);
				findObjects(resultAll, theState, pid, pair.right);
			}
			break;
		}
		case RETURN: {
			ReturnStatement rs = (ReturnStatement) statement;

			findObjects(resultAll, theState, pid, rs.expression());
			break;
		}
		default:
			throw new CIVLInternalException("unknown statement kind", statement);
		}
	}

	/**
	 * <p>
	 * Computes an over-approximation of the set of pre-existing objects accessed by
	 * executing an atomic statement. (The atomic statement may allocate and access
	 * new memory, but these are not included in this computation.)
	 * </p>
	 * 
	 * <p>
	 * Current implementation: all objects reachable from variables that occur
	 * within the atomic region (which includes functions called within the atomic
	 * block, functions called by those functions, etc.). Here, "atomic region"
	 * includes a "begin local ... end local" section of code, but by definition of
	 * local, such code depends on nothing.
	 * </p>
	 * 
	 * @param resultAll   set of objects which could be accessed (out)
	 * @param resultWrite set of objects which could be accessed by writes (out)
	 * @param state       the state from which the atomic statement is executed
	 * @param pid         process ID for the process executing the atomic statement
	 * @param as          the {@link Statement} that marks the entrance to the
	 *                    atomic statement by obtaining the atomic lock
	 * @throws NoReductionException if no upper bound on the set of objects can be
	 *                              found
	 */
	private void computeMemAtomicBlock(SeqSet resultAll, SeqSet resultWrite, State state, int pid,
			AtomicLockAssignStatement as) throws NoReductionException {
		if (as.source().isEntryOfLocalBlock())
			// begin_local ... end_local: depends on nothing
			return;

		Set<Variable> vars = as.getVariables();

		if (vars == null)
			throw new NoReductionException();

		Set<Variable> varsWrite = as.source().writableVariables();

		// all variables occurring in the atomic section are accessible:
		for (Variable var : vars)
			addVariableInProc(resultAll, pid, var);
		// only writable variables occurring in the atomic block are writable
		// by this transition:
		varsWrite.retainAll(vars);
		for (Variable var : varsWrite)
			addVariableInProc(resultWrite, pid, var);
		// anything that can be reached in one or more steps by pointer
		// dereference from any variable is potentially writable...

		SeqSet tmpSet = findReachableIrreflexive(state, pid, as.source().getSource(), vars);

		resultAll.addAll(tmpSet);
		resultWrite.addAll(tmpSet);
	}

	/**
	 * Computes over-approximation of the objects accessed by a call to an atomic,
	 * defined (non-system) function.
	 * 
	 * @param resultAll   objects that could be accessed (out)
	 * @param resultWrite objects that could be accessed by a write (out)
	 * @param state       state from which the call is made (in)
	 * @param pid         ID of the process making the call (in)
	 * @param function    the atomic function being called
	 * @param arguments   the arguments in the call expression
	 * @throws NoReductionException                if no good approximation to the
	 *                                             resulting sets can be obtained
	 * @throws UnsatisfiablePathConditionException if it is determined that the path
	 *                                             condition of {@code state} is
	 *                                             unsatisfiable
	 */
	private void computeMemAtomicFunction(SeqSet resultAll, SeqSet resultWrite, State state, int pid,
			CIVLFunction function, List<Expression> arguments)
			throws NoReductionException, UnsatisfiablePathConditionException {
		assert function.isAtomicFunction() && function.startLocation() != null;

		Set<Variable> vars = function.getAccessesAtomicFunction();

		if (vars == null)
			throw new NoReductionException();

		State newState = enabler.executeCall(state, pid, function, arguments);
		Location start = function.startLocation();
		Set<Variable> varsWrite = start.writableVariables();

		// all variables occurring in the atomic section are accessible:
		for (Variable var : vars)
			addVariableInProc(resultAll, pid, var);
		// only writable variables occurring in the atomic block are writable
		// by this transition:
		varsWrite.retainAll(vars);
		for (Variable var : varsWrite)
			addVariableInProc(resultWrite, pid, var);

		// The starting point for the irreflexive reachability search...
		Set<Variable> vars2 = new HashSet<Variable>(vars);

		vars2.addAll(function.parameters());
		// anything that can be reached in one or more steps by pointer
		// dereference from any variable is potentially writable...
		SeqSet tmpSet = findReachableIrreflexive(newState, pid, start.getSource(), vars2);

		resultAll.addAll(tmpSet);
		resultWrite.addAll(tmpSet);
	}

	/**
	 * Analyzes the object accesses associated to the left-hand side of an
	 * assignment {@code lhs = ...}. If {@code lhs} is a variable {@code x}, then
	 * {@code x} is accessed as a write. If {@code lhs} is a dereference expression
	 * {@code *p}, where {@code p} is an expression, then all accessed arising from
	 * the evaluation of {@code p} occur, and in addition a write access to the
	 * object pointed to by the pointer value resulting from evaluating {@code p}
	 * occurs. And so on.
	 * 
	 * @param resultAll   the set of objects accessed (out)
	 * @param resultWrite the set of objects accessed by writing (out)
	 * @param pid         the ID of the process performing the assignment
	 * @param lhs         the left hand side expression of the assignment
	 * @throws NoReductionException                if no good approximation can be
	 *                                             obtained
	 * @throws UnsatisfiablePathConditionException if it is determined that the path
	 *                                             condition of {@code state} is
	 *                                             unsatisfiable
	 */
	private void findAccessesLHS(SeqSet resultAll, SeqSet resultWrite, int pid, LHSExpression lhs)
			throws NoReductionException, UnsatisfiablePathConditionException {
		switch (lhs.lhsExpressionKind()) {
		case DEREFERENCE: { // *p = e;
			// evaluate p and find the object o into which it points,
			// add o to resultWrite
			Expression pointerArg = ((DereferenceExpression) lhs).pointer();
			SymbolicExpression pointerVal = coarsePointerEval(theState, pid, pointerArg);
			CIVLSource source = pointerArg.getSource();

			findObjects(resultAll, theState, pid, pointerArg);
			addPointer(resultAll, theState, source, pointerVal);
			addPointer(resultWrite, theState, source, pointerVal);
			break;
		}
		case DOT: { // s.f = ...;
			LHSExpression struct = (LHSExpression) ((DotExpression) lhs).structOrUnion();

			findAccessesLHS(resultAll, resultWrite, pid, struct);
			break;
		}
		case SUBSCRIPT: { // a[i] = ...;
			SubscriptExpression sub = (SubscriptExpression) lhs;
			LHSExpression array = sub.array();
			Expression index = sub.index();

			findAccessesLHS(resultAll, resultWrite, pid, array);
			findObjects(resultAll, theState, pid, index);
			break;
		}
		case VARIABLE: {
			Variable var = ((VariableExpression) lhs).variable();

			addVariableInProc(resultAll, pid, var);
			addVariableInProc(resultWrite, pid, var);
			break;
		}
		default:
			throw new CIVLInternalException("unreachable", lhs);
		}
	}

	/**
	 * Computes an over-approximation of the set of objects accessed by evaluating
	 * an expression of the form {@code &lhs}. If {@code lhs} is a variable, no
	 * objects are accessed: the variable is neither read nor written. If
	 * {@code lhs} has the form {@code *p} the result is the set of objects accessed
	 * in the course of evaluating {@code p}, but not the objected pointed to by the
	 * result of that evaluation, as that object is neither read nor modified. And
	 * so on.
	 * 
	 * @param result the set to which the memory locations should be added
	 * @param state  the state in which the expression {@code arg} occurs
	 * @param pid    process ID for the process evaluating the expression
	 * @param arg    the argument to the address-of operator
	 * @throws UnsatisfiablePathConditionException if it is discovered that the path
	 *                                             condition of {@code state} is
	 *                                             unsatisfiable
	 * @throws NoReductionException                if no good over-approximation can
	 *                                             be found
	 */
	private void findObjectsLHS(SeqSet result, State state, int pid, LHSExpression arg)
			throws UnsatisfiablePathConditionException, NoReductionException {
		switch (arg.lhsExpressionKind()) {
		case DEREFERENCE:
			// evaluating &*e accesses the memory locations accessed when
			// evaluating e
			findObjects(result, state, pid, ((DereferenceExpression) arg).pointer());
			break;
		case DOT: {
			// evaluating &e.f accesses the memory locations accessed when
			// evaluating &e
			LHSExpression struct = (LHSExpression) ((DotExpression) arg).structOrUnion();

			findObjectsLHS(result, state, pid, struct);
			break;
		}
		case SUBSCRIPT: {
			// evaluating &e[f] accesses the memory locations accessed
			// when evaluating &e and f
			SubscriptExpression sub = (SubscriptExpression) arg;
			LHSExpression array = sub.array();
			Expression index = sub.index();

			findObjectsLHS(result, state, pid, array);
			findObjects(result, state, pid, index);
			break;
		}
		case VARIABLE: // evaluating &x does not access any memory location
			break;
		default:
			throw new CIVLInternalException("Unknown kind of LExpression", arg);
		}
	}

	/**
	 * Evaluates a pointer expression to get some pointer into the object pointed
	 * to. If the expression contains bound variables this might make it impossible
	 * to evaluate and the no-reduction exception is thrown.
	 * 
	 * @param state a state
	 * @param pid   ID of process evaluating the pointer expression
	 * @param expr  an expression of pointer type
	 * @return pointer to the object pointed to, though not necessarily the exact
	 *         location within that object
	 * @throws NoReductionException if it is not possible to evaluate the pointer
	 *                              expression
	 */
	private SymbolicExpression coarsePointerEval(State state, int pid, Expression expr) throws NoReductionException {
		ExpressionKind kind = expr.expressionKind();

		if (kind == ExpressionKind.BINARY) {
			BinaryExpression be = (BinaryExpression) expr;
			BINARY_OPERATOR op = be.operator();

			if (op == BINARY_OPERATOR.POINTER_ADD) {
				Expression arg0 = be.left();

				return arg0.getExpressionType().isPointerType() ? coarsePointerEval(state, pid, arg0)
						: coarsePointerEval(state, pid, be.right());
			} else if (op == BINARY_OPERATOR.POINTER_SUBTRACT)
				return coarsePointerEval(state, pid, be.left());
		}
		try {
			return evaluator.evaluate(state, pid, expr).value;
		} catch (Exception e) {
			throw new NoReductionException();
		}
	}

	/**
	 * Computes an over-approximation to the set of objects accessed when evaluating
	 * an expression. Only objects existing in {@code #theState} are kept.
	 * 
	 * @param result the (non-null) set to which the memory locations referenced in
	 *               {@code expr} will be added
	 * @param state  the super-state in which the evaluation occurs
	 * @param pid    the process ID number for the process that is evaluating
	 *               {@code expr}
	 * @param expr   the expression being evaluated. may be {@code null}, in which
	 *               case this is a no-op
	 * @throws UnsatisfiablePathConditionException if in the course of evaluating
	 *                                             {@code expr} it is discovered
	 *                                             that the path condition of the
	 *                                             current state is not satisfiable
	 * @throws NoReductionException                if no good over-approximation can
	 *                                             be found
	 */
	private void findObjects(SeqSet result, State state, int pid, Expression expr)
			throws UnsatisfiablePathConditionException, NoReductionException {
		if (expr == null)
			return;
		findObjects(result, state, pid, expr.getExpressionType());
		switch (expr.expressionKind()) {
		case ABSTRACT_FUNCTION_CALL:
			for (Expression arg : ((AbstractFunctionCallExpression) expr).arguments())
				findObjects(result, state, pid, arg);
			break;
		case ADDRESS_OF:
			findObjectsLHS(result, state, pid, ((AddressOfExpression) expr).operand());
			break;
		case ARRAY_LAMBDA: {// (int[n])$lambda (int i,j,... | e) f
			ArrayLambdaExpression ale = (ArrayLambdaExpression) expr;

			for (Pair<List<Variable>, Expression> pair : ale.boundVariableList())
				findObjects(result, state, pid, pair.right);
			findObjects(result, state, pid, ale.restriction());
			findObjects(result, state, pid, ale.expression());
			break;
		}
		case BINARY:
			findObjects(result, state, pid, ((BinaryExpression) expr).left());
			findObjects(result, state, pid, ((BinaryExpression) expr).right());
			break;
		case BOOLEAN_LITERAL: // nothing
			break;
		case BOUND_VARIABLE: // nothing
			break;
		case CAST:
			findObjects(result, state, pid, ((CastExpression) expr).getExpression());
			break;
		case CHAR_LITERAL: // nothing
			break;
		case COND: {
			ConditionalExpression cond = (ConditionalExpression) expr;

			findObjects(result, state, pid, cond.getCondition());
			findObjects(result, state, pid, cond.getTrueBranch());
			findObjects(result, state, pid, cond.getFalseBranch());
			break;
		}
		case DEREFERENCE: {
			Expression pointerArg = ((DereferenceExpression) expr).pointer();
			SymbolicExpression pointerVal = coarsePointerEval(state, pid, pointerArg);

			findObjects(result, state, pid, pointerArg);
			addPointer(result, state, pointerArg.getSource(), pointerVal);
			break;
		}
		case DOMAIN_GUARD: {
			DomainGuardExpression dge = (DomainGuardExpression) expr;
			int n = dge.dimension();

			findObjects(result, state, pid, dge.domain());
			for (int i = 0; i < n; i++)
				addVariableInProc(result, state, pid, dge.variableAt(i), true);
			addVariableInProc(result, state, pid, dge.getLiteralDomCounter(), true);
			break;
		}
		case DOT:
			findObjects(result, state, pid, ((DotExpression) expr).structOrUnion());
			break;
		case DYNAMIC_TYPE_OF:
			findObjects(result, state, pid, ((DynamicTypeOfExpression) expr).getType());
			break;
		case EXTENDED_QUANTIFIER: {
			ExtendedQuantifiedExpression eqf = (ExtendedQuantifiedExpression) expr;

			findObjects(result, state, pid, eqf.function());
			findObjects(result, state, pid, eqf.lower());
			findObjects(result, state, pid, eqf.higher());
			break;
		}
		case FUNCTION_GUARD: {
			FunctionGuardExpression fge = (FunctionGuardExpression) expr;

			findObjects(result, state, pid, fge.functionExpression());
			for (Expression arg : fge.arguments())
				findObjects(result, state, pid, arg);
			break;
		}
		case FUNCTION_IDENTIFIER: // nothing
			break;
		case FUNC_CALL: {// these are atomic, pure functions
			CallOrSpawnStatement call = ((FunctionCallExpression) expr).callStatement();

			findObjects(result, state, pid, call.functionExpression());
			for (Expression arg : call.arguments())
				findObjects(result, state, pid, arg);
			assert call.lhs() == null;
			break;
		}
		case HERE_OR_ROOT: // nothing
			break;
		case INITIAL_VALUE: // nothing - abstract initial value
			break;
		case INTEGER_LITERAL: // nothing
			break;
		case LAMBDA:
			findObjects(result, state, pid, ((LambdaExpression) expr).lambdaFunction());
			break;
		case MEMORY_UNIT:
			findObjects(result, state, pid, ((MemoryUnitExpression) expr).reference());
			break;
		case NOTHING: // nothing
			break;
		case NULL_LITERAL: // nothing
			break;
		case PROC_NULL: // nothing
			break;
		case QUANTIFIER: {
			QuantifiedExpression qe = (QuantifiedExpression) expr;

			for (Pair<List<Variable>, Expression> pair : qe.boundVariableList())
				findObjects(result, state, pid, pair.right);
			findObjects(result, state, pid, qe.expression());
			findObjects(result, state, pid, qe.restriction());
			break;
		}
		case REAL_LITERAL: // nothing
			break;
		case REC_DOMAIN_LITERAL: {
			RecDomainLiteralExpression rdl = (RecDomainLiteralExpression) expr;
			int n = rdl.dimension();

			for (int i = 0; i < n; i++)
				findObjects(result, state, pid, rdl.rangeAt(i));
			break;
		}
		case REGULAR_RANGE: {
			RegularRangeExpression rr = (RegularRangeExpression) expr;

			findObjects(result, state, pid, rr.getLow());
			findObjects(result, state, pid, rr.getHigh());
			findObjects(result, state, pid, rr.getStep());
			break;
		}
		case RESULT: // nothing
			break;
		case SCOPEOF:
			findObjectsLHS(result, state, pid, ((ScopeofExpression) expr).argument());
			break;
		case SELF: // nothing
			break;
		case SIZEOF_EXPRESSION:
			findObjects(result, state, pid, ((SizeofExpression) expr).getArgument().getExpressionType());
			// this is what the evaluator does
			break;
		case SIZEOF_TYPE:
			findObjects(result, state, pid, ((SizeofTypeExpression) expr).getTypeArgument());
			break;
		case STRING_LITERAL: // nothing
			break;
		case COMPOUND_LITERAL:
			// nothing. these have constant values only (see Evaluator)
			break;
		case SUBSCRIPT: {
			SubscriptExpression se = (SubscriptExpression) expr;

			findObjects(result, state, pid, se.array());
			findObjects(result, state, pid, se.index());
			break;
		}
		case SYSTEM_GUARD:
			for (Expression arg : ((SystemGuardExpression) expr).arguments())
				findObjects(result, state, pid, arg);
			break;
		case UNARY:
			findObjects(result, state, pid, ((UnaryExpression) expr).operand());
			break;
		case UNDEFINED_PROC: // nothing
			break;
		case VARIABLE:
			addVariableInProc(result, state, pid, ((VariableExpression) expr).variable(), true);
			break;
		case WILDCARD: // nothing to do
			break;
		default:
			break;
		}
	}

	/**
	 * Computes an over-approximation to the set of objects referenced in a CIVL
	 * type. These object references would occur in array length expressions.
	 * Example: in the type {@code int[n]} the object {@code n} is referenced.
	 * 
	 * @param result the set to which the objects shall be added
	 * @param state  the state in which this type is evaluated
	 * @param pid    the ID of the process performing the evaluation
	 * @param type   the CIVL type
	 * @throws UnsatisfiablePathConditionException if it is discovered that the path
	 *                                             condition of {@code state} is
	 *                                             unsatisfiable
	 * @throws NoReductionException                if no good over-approximation can
	 *                                             be found
	 */
	private void findObjects(SeqSet result, State state, int pid, CIVLType type)
			throws UnsatisfiablePathConditionException, NoReductionException {
		findObjectsHelper(result, state, pid, type, new HashSet<CIVLType>());
	}

	/**
	 * Auxiliary function used by
	 * {@link #findObjects(SeqSet, State, int, CIVLType)}. This is a recursive
	 * function that keeps track of the set of seen types.
	 * 
	 * @param result the set to which the objects shall be added
	 * @param state  the state in which this type is evaluated
	 * @param pid    the ID of the process performing the evaluation
	 * @param type   the CIVL type
	 * @param seen   the set of types already encountered in this invocation of
	 *               {@link #findObjects(SeqSet, State, int, CIVLType)}.
	 * @throws UnsatisfiablePathConditionException if it is discovered that the path
	 *                                             condition of {@code state} is
	 *                                             unsatisfiable
	 * @throws NoReductionException                if no good over-approximation can
	 *                                             be found
	 */
	private void findObjectsHelper(SeqSet result, State state, int pid, CIVLType type, Set<CIVLType> seen)
			throws UnsatisfiablePathConditionException, NoReductionException {
		if (!seen.add(type))
			return;
		switch (type.typeKind()) {
		case ARRAY:
			findObjectsHelper(result, state, pid, ((CIVLArrayType) type).elementType(), seen);
			break;
		case COMPLETE_ARRAY: {
			CIVLCompleteArrayType atype = (CIVLCompleteArrayType) type;

			findObjectsHelper(result, state, pid, atype.elementType(), seen);
			findObjects(result, state, pid, atype.extent());
			break;
		}
		case FUNCTION: {
			CIVLFunctionType ftype = (CIVLFunctionType) type;

			for (CIVLType ptype : ftype.parameterTypes())
				findObjectsHelper(result, state, pid, ptype, seen);
			findObjectsHelper(result, state, pid, ftype.returnType(), seen);
			break;
		}
		case POINTER:
			findObjectsHelper(result, state, pid, ((CIVLPointerType) type).baseType(), seen);
			break;
		case SET:
			findObjectsHelper(result, state, pid, ((CIVLSetType) type).elementType(), seen);
			break;
		case STRUCT_OR_UNION: {
			CIVLStructOrUnionType sutype = (CIVLStructOrUnionType) type;

			if (sutype.isComplete())
				for (StructOrUnionField field : sutype.fields())
					findObjectsHelper(result, state, pid, field.type(), seen);
			break;
		}
		case BUNDLE:
		case DOMAIN:
		case ENUM:
		case HEAP:
		case MEM:
		case PRIMITIVE:
		default:
			break;
		}
	}

	/**
	 * Gets the result of evaluating a guard for a statement. This method handles
	 * the caching of the results. It uses
	 * {@link SimpleEnabler#computeGuard(State, Reasoner, int, int)} to compute the
	 * guard the first time. The guard is evaluated at state {@link #theState}.
	 * 
	 * @param pid the process ID
	 * @param sid the statement ID, i.e., the index in the list of outgoing
	 *            statements from the current location of the process
	 * @return evaluated guard
	 * @throws UnsatisfiablePathConditionException if in the course of evaluating it
	 *                                             is discovered that the path
	 *                                             condition of {@link #theState} is
	 *                                             unsatisfiable
	 */
	private BooleanExpression getGuardValue(int pid, int sid) throws UnsatisfiablePathConditionException {
		if (theGuards[pid] == null) {
			int numOutgoing = theState.getProcessState(pid).getLocation().getNumOutgoing();

			theGuards[pid] = new BooleanExpression[numOutgoing];
			return theGuards[pid][sid] = enabler.computeGuard(theState, reasoner, pid, sid);
		} else {
			BooleanExpression evaluatedGuard = theGuards[pid][sid];

			if (evaluatedGuard == null) {
				evaluatedGuard = enabler.computeGuard(theState, reasoner, pid, sid);
				theGuards[pid][sid] = evaluatedGuard;
			}
			return evaluatedGuard;
		}
	}

	/**
	 * <p>
	 * Computes the set of transitions enabled at {@link #theState} by a given
	 * statement.
	 * </p>
	 * 
	 * <p>
	 * Precondition: it is not the case that some other process owns the atomic lock
	 * </p>
	 * 
	 * @param result   the list to which the enabled transitions will be added
	 * @param pid      the ID of the process executing the statement
	 * @param location the current location of the process at state
	 *                 {@link #theState} (this could be determined from
	 *                 {@link #theState} but is an argument for efficiency)
	 * @param stmtID   the ID number of the outgoing statement from {@code location}
	 * @throws UnsatisfiablePathConditionException if it is determined that the path
	 *                                             condition of {@link #theState} is
	 *                                             unsatisfiable
	 */
	private void computeEnabledFromStatement(List<Transition> result, int pid, Location location, int stmtID)
			throws UnsatisfiablePathConditionException {
		Statement stmt = location.getOutgoing(stmtID);
		BooleanExpression guardValue = getGuardValue(pid, stmtID);

		if (guardValue.isFalse())
			return;
		// second half of $yield: re-obtaining lock...
		if (enabler.isYield(stmt) && !stateFactory.lockedByAtomic(theState)) {
			result.add(Semantics.newTransition(pid, guardValue, stmt, false));
		} else if (enabler.isSystemCall(theState, pid, stmt)) {
			result.addAll(
					enabler.enabledTransitionsOfSystemCall(theState, pid, guardValue, (CallOrSpawnStatement) stmt));
		} else {
			boolean simplify = enabler.isAssume(stmt);
			boolean noop = stmt.statementKind() == StatementKind.NOOP;
			Transition trans = noop ? Semantics.newNoopTransition(pid, guardValue, stmt, simplify)
					: Semantics.newTransition(pid, guardValue, stmt, simplify);

			result.add(trans);
		}
	}

	/**
	 * <p>
	 * Computes the set of transitions enabled at {@link #theState} in the specified
	 * process.
	 * </p>
	 * 
	 * <p>
	 * Precondition: the atomic lock is not held by another process at
	 * {@link #theState}. Hence the atomic lock may be free, or it may be held by
	 * the specified process.
	 * </p>
	 * 
	 * @param result the list to which the enabled transitions will be added
	 * @param pid    ID of the process
	 * @throws UnsatisfiablePathConditionException if it is determined that the path
	 *                                             condition of {@link #theState} is
	 *                                             unsatisfiable
	 */
	private void computeEnabledInProcess(List<Transition> result, int pid) throws UnsatisfiablePathConditionException {
		ProcessState ps = theState.getProcessState(pid);

		if (ps == null)
			return;

		Location location = ps.getLocation();

		if (location == null)
			return;

		int numStatements = location.getNumOutgoing();

		for (int i = 0; i < numStatements; i++)
			computeEnabledFromStatement(result, pid, location, i);
	}

	/**
	 * <p>
	 * Computes an over-approximation to the set of objects associated to a
	 * process's current location at state {@link #theState}. These are: (1) any
	 * object that could be read or modified by a currently enabled statement, and
	 * (2) all objects that are read by any guard of an (enabled or disabled)
	 * statement emanating from that location. Additionally, computes set of
	 * processes on which the given process depends, due to waiting.
	 * </p>
	 * 
	 * <p>
	 * For a system function call: if the system function does not specify a
	 * depends_on contract clause, nothing is assumed about the call, i.e., it could
	 * depend on everything. Example: $wait. I think $wait should depends_on
	 * nothing. Nothing can disable it and it commutes with everything.
	 * </p>
	 * 
	 * @param pid         the ID of the process (in)
	 * @param depend      the set of objects of the process's depend set (out)
	 * @param dependWrite the set of objects of the depend set that may be modified
	 *                    (out)
	 * @return set of PIDs of processes on which this process is waiting with
	 *         blocked wait statements, or {@code null} if there are no such waitees
	 * @throws UnsatisfiablePathConditionException if it is determined that the path
	 *                                             condition of {@link #theState} is
	 *                                             unsatisfiable
	 */
	Set<Integer> computeDepends(int pid, SeqSet depend, SeqSet dependWrite) throws UnsatisfiablePathConditionException {
		Location location = theState.getProcessState(pid).getLocation();
		int numOutgoing = location.getNumOutgoing();
		Set<Integer> result = null;

		try {
			for (int i = 0; i < numOutgoing; i++) {
				Statement statement = location.getOutgoing(i);
				Expression guard = statement.guard();
				BooleanExpression guardValue = getGuardValue(pid, i);

				findObjects(depend, theState, pid, guard);
				if (reasoner.unsat(guardValue).getResultType() == ResultType.YES) {
					if (enabler.isWait(statement)) {
						// this is a blocked wait, get the "waitee"...
						Expression arg = ((CallOrSpawnStatement) statement).arguments().get(0);
						SymbolicExpression val = enabler.evaluator.evaluate(theState, pid, arg).value;
						int pidValue = enabler.modelFactory.getProcessId(val);

						if (result == null)
							result = new HashSet<>(2);
						result.add(pidValue);
					}
				} else {
					computeMem(depend, dependWrite, pid, statement);
				}
			}
		} catch (NoReductionException e) {
			depend.makeFull();
			dependWrite.makeFull();
		}
		return result;
	}

	/**
	 * Computes an over-approximation of all objects reachable from a process in
	 * {@link #theState}. Consider the directed graph in which the nodes are the
	 * objects which exist at {@link #theState} and there is an edge from u to v if
	 * u contains a pointer which points to some part of v. The initial nodes are
	 * all variable instances in the dyscopes reachable from the process's call
	 * stack. The reachable dyscopes are those referenced by the call stack, the
	 * parents of those dyscopes, the parents of those, etc.
	 * 
	 * <p>
	 * Heaps are treated specially since a heap is technically a single variable,
	 * but is considered to represent a set of independent objects.
	 * </p>
	 * 
	 * <p>
	 * If no good over-approximation can be found, the universal set (containing all
	 * objects) is returned.
	 * </p>
	 * 
	 * @param pid        the ID of the process
	 * @param reach      out variable: set to which all reachable objects will be
	 *                   added
	 * @param reachWrite out variable: set to which all reachable objects which are
	 *                   possibly modified will be added
	 * @return the set of reachable objects represented as a {@link SeqSet}
	 */
	void computeReach(int pid, SeqSet reach, SeqSet reachWrite) {
		ProcessState ps = theState.getProcessState(pid);
		Set<Integer> dyscopeIDs = new HashSet<>();
		Set<Variable> writeableVars = new HashSet<>();

		if (ps == null || ps.hasEmptyStack())
			return;
		for (StackEntry se : ps.getStackEntries()) {
			int dyscopeID = se.scope();
			Location loc = se.location();

			if (loc != null)
				writeableVars.addAll(loc.writableVariables());
			while (dyscopeID != -1 && dyscopeIDs.add(dyscopeID))
				dyscopeID = theState.getParentId(dyscopeID);
		}
		for (int dyscopeID : dyscopeIDs) {
			DynamicScope ds = theState.getDyscope(dyscopeID);
			Scope scope = ds.lexicalScope();

			for (Variable var : scope.variables()) {
				addVariable(reach, dyscopeID, var);
				if (writeableVars.contains(var))
					addVariable(reachWrite, dyscopeID, var);
			}
		}
		try {
			SeqSet closure = new SeqSet();

			// any object that can be reached by one or more pointer derefs
			// is a writable reachable object...
			closeIrreflexive(closure, reach, theState, ps.getLocation().getSource());
			reach.addAll(closure);
			reachWrite.addAll(closure);
		} catch (NoReductionException e) {
			reach.add(); // makes it the universal set
			reachWrite.add(); // ditto
		}
	}

	/**
	 * Prints a {@code SeqSet} representing a set of objects in a human readable
	 * form. The set represents a set of variable instances or heap objects in the
	 * current state {@link #theState}.
	 * 
	 * @param out the stream to which the output should be printed
	 * @param ss  the set representing a set of objects
	 */
	protected void printObjSet(PrintStream out, SeqSet ss) {
		boolean first = true;

		for (int[] vec : ss.getLeaves()) {
			if (first)
				first = false;
			else
				out.print(", ");
			if (vec.length == 0)
				out.print("all");
			else {
				// dyscope, variable, mallocIdx(optional), objIdx(optional)
				int dyid = vec[0], vid = vec[1];
				DynamicScope dyscope = theState.getDyscope(dyid);
				Scope scope = dyscope.lexicalScope();
				Variable var = scope.variable(vid);

				out.print(var.name().name() + "#" + dyid);
				if (vec.length > 2) {
					out.print("." + vec[2]);
					if (vec.length > 3)
						out.print("[" + vec[3] + "]");
				}
			}
		}
	}

	/**
	 * Does the specified process satisfy the simple invisibility criterion for the
	 * current deadlock predicate at state {@link #theState}?
	 * 
	 * <p>
	 * This means: assuming the current state s does not satisfy the "bad" property
	 * p, on any execution starting from s in which the executing processes do not
	 * access the dependencies of process {@code pid}, p will not hold. Example:
	 * 
	 * <pre>
	 * $input int X;
	 * p0 : { int x = X; $when x>0 ; }
	 * p1 : { 1; } // 1 is a no-op with guard true
	 * </pre>
	 * </p>
	 * 
	 * <p>
	 * Absolute deadlock is the bad property which holds when the enabling predicate
	 * (the disjunction of guards of statements from current locations) is not
	 * valid, i.e., there exists an assignment of values to symbolic constants such
	 * that the resulting concrete state is deadlocked.
	 * </p>
	 * 
	 * <p>
	 * In the state s where p0 is at the $when and p1 is at the no-op, absolute
	 * deadlock is false, as the enabled predicate is true, because of p1. However
	 * the simple invisibility criterion does not hold for pid=0. That is because
	 * after process 1 executes the no-op, absolute deadlock holds, as X>0 is not
	 * valid. Hence it would be wrong to choose {p0} as an ample set for s.
	 * </p>
	 * 
	 * <p>
	 * A sufficient condition for the criterion to hold is that the enabling
	 * predicate of the single process pid is valid. By the assumption, further
	 * execution by other processes can only weaken the enabling predicate of pid,
	 * so it must remain valid. That is because the other processes cannot affect
	 * any variable occurring in a guard of an enabled transition in pid. They may
	 * affect variables which enable currently disabled transitions in pid, but that
	 * can only weaken the enabling predicate.
	 * </p>
	 * 
	 * <p>
	 * For potential deadlock: the situation is similar except send transitions
	 * should be considered possibly blocking, so the disjunction of guards of all
	 * transitions departing from pid's location other than send transitions must be
	 * valid. A "send" is actually an "enqueue" operation in the comm library.
	 * </p>
	 * 
	 * <p>
	 * $wait: $wait is a system function which should be declared as "depends_on"
	 * nothing. It really is independent of any transition from another process. Its
	 * guard is "terminated(p)", where p is the $proc that is the argument to $wait.
	 * If that guard evaluates to true, it will remain true. So there is no need for
	 * any special treatment for $wait.
	 * </p>
	 * 
	 * <p>
	 * $spawn is normally always enabled, except for a process-bounded search
	 * (proc_bound > 0). For such a search, $spawn should never be considered
	 * independent. That is because a $spawn can disable another $spawn. Since this
	 * method should only be invoked for a set of transitions that are independent
	 * (of transitions in other processes), this case also requires no special
	 * handling.
	 * </p>
	 * 
	 * @param pid process ID
	 * @return {@code true} if it is possible the process has a visible enabled
	 *         transition
	 * @throws UnsatisfiablePathConditionException if it is determined that the path
	 *                                             condition of {@link #theState} is
	 *                                             unsatisfiable
	 */
	protected boolean allInvisible(int pid) throws UnsatisfiablePathConditionException {
		// optimization: handle the common cases first.
		// TODO: perform this computation statically

		DeadlockKind kind = enabler.config.checkDeadlockKind();

		if (kind == DeadlockKind.NONE)
			return true;

		Location location = theState.getProcessState(pid).getLocation();

		if (location == null)
			return true; // process pid has terminated

		if (location.isBinaryBranching() || location.isSwitchOrChooseWithDefault())
			return true;

		int numOutgoing = location.getNumOutgoing();

		if (numOutgoing == 0)
			return true;

		if (numOutgoing == 1) {
			Statement stmt = location.getOutgoing(0);

			if (enabler.isTrue(stmt.guard())) {
				if (kind == DeadlockKind.ABSOLUTE || !enabler.isSend(theState, pid, stmt))
					return true;
			}
		}

		BooleanExpression enabled = universe.falseExpression();

		if (kind == DeadlockKind.ABSOLUTE)
			for (int i = 0; i < numOutgoing; i++)
				enabled = universe.or(enabled, getGuardValue(pid, i));
		else
			for (int i = 0; i < numOutgoing; i++)
				if (!enabler.isSend(theState, pid, location.getOutgoing(i))) {
					enabled = universe.or(enabled, getGuardValue(pid, i));
				}
		return reasoner.isValid(enabled);
	}

	/**
	 * Determines whether the specified process is at a location (at state
	 * {@link #theState}) from which it could enter an atomic block for which
	 * termination is not guaranteed.
	 * 
	 * @param pid the ID of the process
	 * @return {@code} true if process {@code PID} is at a location from which it
	 *         could enter a possibly non-terminating atomic block
	 * 
	 * @see Location#isEntryOfUnsafeAtomic()
	 */
	protected boolean unsafeAtomic(int pid) {
		ProcessState ps = theState.getProcessState(pid);

		if (ps == null)
			return false;

		Location location = ps.getLocation();

		if (location == null)
			return false;
		return location.isEntryOfUnsafeAtomic();
	}

	/**
	 * Returns the set of transitions enabled in the specified process at state
	 * {@link #theState}. If the set has been previously computed, it is returned
	 * immediately from a cache, otherwise it is computed and cached. This is the
	 * method clients should use to get the set of transitions enabled in a process.
	 * 
	 * @param pid the ID of the process
	 * @return the set of enabled transitions, represented as an array
	 * @throws UnsatisfiablePathConditionException if it is determined that the path
	 *                                             condition of {@link #theState} is
	 *                                             unsatisfiable
	 */
	protected Transition[] enabledTransitionsInProcess(int pid) throws UnsatisfiablePathConditionException {
		Transition[] result = enabledTransitions[pid];

		if (result == null) {
			List<Transition> list = new LinkedList<>();

			computeEnabledInProcess(list, pid);
			enabledTransitions[pid] = result = list.toArray(new Transition[list.size()]);
		}
		return result;
	}

	/**
	 * Auxiliary function used by {@link #computeAmpleSet()} to print out ample set
	 * information to {@link Enabler#debugOut}.
	 * 
	 * @param sc        the instance of {@link StrongConnect} already used to find
	 *                  an ample set (or fail to find one)
	 * @param amplePids the list of process IDs of the ample set, or {@code null} if
	 *                  no ample set was found and therefore the full set should be
	 *                  used
	 * @throws UnsatisfiablePathConditionException should not be thrown
	 */
	private void printAmpleInfo(StrongConnect sc, LinkedList<Integer> amplePids)
			throws UnsatisfiablePathConditionException {
		boolean first = true;

		if (amplePids == null) {
			amplePids = new LinkedList<Integer>();
			for (int i = 0; i < nprocs; i++)
				amplePids.add(i);
		}
		enabler.debugOut.print("\nample processes at state " + theState + ":\t");
		for (int i : amplePids) {
			if (first)
				first = false;
			else
				enabler.debugOut.print(", ");
			enabler.debugOut.print("p" + i + "(" + enabledTransitionsInProcess(i).length + ")");
		}
		enabler.debugOut.println();
		sc.printData(enabler.debugOut);
		if (!enabler.debugging && enabler.showAmpleSetWtStates)
			enabler.debugOut.print(theState.callStackToString());
	}

	/**
	 * Computes an ample set for state {@link #theState}. This may be the full set
	 * (consisting of all enabled transitions). This method may set {@link #full} to
	 * {@code true}, indicating that the full set was used.
	 * 
	 * @throws UnsatisfiablePathConditionException if it is discovered that the path
	 *                                             condition of {@link #theState} is
	 *                                             unsatisfiable
	 */
	protected void computeAmpleSet() throws UnsatisfiablePathConditionException {
		StrongConnect sc = new StrongConnect(this);
		LinkedList<Integer> amplePids = sc.findAmple();
		int size = 0, c = 0;
		int numProcs = 0;

		if (amplePids == null) {
			full = true;
			for (int i = 0; i < nprocs; i++) {
				int ntrans = enabledTransitionsInProcess(i).length;

				if (ntrans > 0) {
					size += ntrans;
					numProcs++;
				}
			}
			ampleSet = new Transition[size];
			for (int i = 0; i < nprocs; i++)
				for (Transition tran : enabledTransitions[i]) {
					ampleSet[c++] = tran;
				}
		} else {
			full = false;
			for (int i : amplePids) {
				int ntrans = enabledTransitionsInProcess(i).length;

				if (ntrans > 0) {
					size += ntrans;
					numProcs++;
				}
			}
			ampleSet = new Transition[size];
			for (int i : amplePids)
				for (Transition tran : enabledTransitions[i]) {
					ampleSet[c++] = tran;
				}
		}
		if (numProcs > 1 && (enabler.debugging || enabler.showAmpleSet))
			printAmpleInfo(sc, amplePids);
	}

	/**
	 * Returns the ample set for {@link #theState}. This method should be called
	 * only after {@link #computeAmpleSet()} has been called.
	 * 
	 * @return the ample set
	 */
	protected Transition[] ampleSet() {
		return ampleSet;
	}

	/**
	 * Returns the full bit. This method should be called only after
	 * {@link #computeAmpleSet()} has been called. If this method returns
	 * {@code true}, then the ample set consists of all enabled transitions at
	 * {@link #theState}.
	 * 
	 * @return the full bit
	 */
	protected boolean isFull() {
		return full;
	}
}

/**
 * An exception indicating that no good approximation of a set of dependencies
 * or reachable objects can be determined.
 * 
 * @author siegel
 */
class NoReductionException extends Exception {
	private static final long serialVersionUID = 1L;
}
