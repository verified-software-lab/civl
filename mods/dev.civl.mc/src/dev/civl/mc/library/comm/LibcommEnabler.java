package dev.civl.mc.library.comm;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.config.IF.CIVLConstants.DeadlockKind;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.kripke.IF.Enabler;
import dev.civl.mc.kripke.IF.LibraryEnabler;
import dev.civl.mc.kripke.IF.LibraryEnablerLoader;
import dev.civl.mc.library.common.BaseLibraryEnabler;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.model.IF.Identifier;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.SystemFunction;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.FunctionIdentifierExpression;
import dev.civl.mc.model.IF.expression.LHSExpression;
import dev.civl.mc.model.IF.location.Location;
import dev.civl.mc.model.IF.statement.CallOrSpawnStatement;
import dev.civl.mc.model.IF.statement.Statement;
import dev.civl.mc.model.IF.statement.Statement.StatementKind;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.semantics.IF.Evaluation;
import dev.civl.mc.semantics.IF.Evaluator;
import dev.civl.mc.semantics.IF.LibraryEvaluatorLoader;
import dev.civl.mc.semantics.IF.LibraryLoaderException;
import dev.civl.mc.semantics.IF.Semantics;
import dev.civl.mc.semantics.IF.SymbolicAnalyzer;
import dev.civl.mc.semantics.IF.Transition;
import dev.civl.mc.state.IF.MemoryUnitSet;
import dev.civl.mc.state.IF.ProcessState;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.sarl.IF.Reasoner;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.number.IntegerNumber;

public class LibcommEnabler extends BaseLibraryEnabler
		implements
			LibraryEnabler {

	/* **************************** Constructors *************************** */

	public LibcommEnabler(String name, Enabler primaryEnabler,
			Evaluator evaluator, ModelFactory modelFactory,
			SymbolicUtility symbolicUtil, SymbolicAnalyzer symbolicAnalyzer,
			CIVLConfiguration civlConfig, LibraryEnablerLoader libEnablerLoader,
			LibraryEvaluatorLoader libEvaluatorLoader) {
		super(name, primaryEnabler, evaluator, modelFactory, symbolicUtil,
				symbolicAnalyzer, civlConfig, libEnablerLoader,
				libEvaluatorLoader);
	}

	/* ********************* Methods from LibraryEnabler ******************* */

	@Override
	public BitSet ampleSet(State state, int pid, CallOrSpawnStatement statement,
			MemoryUnitSet[] setsReachableRead,
			MemoryUnitSet[] setsReachableWrite)
			throws UnsatisfiablePathConditionException {
		Identifier name;
		CallOrSpawnStatement call;

		if (!(statement instanceof CallOrSpawnStatement)) {
			throw new CIVLInternalException("Unsupported statement for civlc",
					statement);
		}
		call = (CallOrSpawnStatement) statement;
		name = call.function().name();
		switch (name.name()) {
			case "$comm_enqueue" :
			case "$comm_dequeue" :
				return ampleSetWork(state, pid, call, setsReachableRead,
						setsReachableWrite);
			default :
				return super.ampleSet(state, pid, statement, setsReachableRead,
						setsReachableWrite);
		}
	}

	@Override
	public List<Transition> enabledTransitions(State state,
			CallOrSpawnStatement call, BooleanExpression clause, int pid)
			throws UnsatisfiablePathConditionException {
		String functionName = call.function().name().name();

		switch (functionName) {
			case "$comm_dequeue" :
				return this.enabledCommDequeueTransitions(state, call, clause,
						pid);
			default :
				return super.enabledTransitions(state, call, clause, pid);
		}
	}

	/* *************************** Private Methods ************************* */

	/**
	 * Computes the ample set process ID's from a system function call.
	 * 
	 * @param state
	 *            The current state.
	 * @param pid
	 *            The ID of the process that the system function call belongs
	 *            to.
	 * @param call
	 *            The system function call statement.
	 * @param reachableMemUnitsMap
	 *            The map of reachable memory units of all active processes.
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 * @throws LibraryLoaderException
	 */
	private BitSet ampleSetWork(State state, int pid, CallOrSpawnStatement call,
			MemoryUnitSet[] setsReachableRead,
			MemoryUnitSet[] setsReachableWrite)
			throws UnsatisfiablePathConditionException {
		int numArgs;
		numArgs = call.arguments().size();
		Expression[] arguments;
		SymbolicExpression[] argumentValues;
		String function = call.function().name().name();
		CIVLSource source = call.getSource();
		BitSet ampleSet = new BitSet();
		String process = state.getProcessState(pid).name();

		arguments = new Expression[numArgs];
		argumentValues = new SymbolicExpression[numArgs];
		for (int i = 0; i < numArgs; i++) {
			Evaluation eval = null;

			arguments[i] = call.arguments().get(i);
			try {
				eval = evaluator.evaluate(state, pid, arguments[i]);
			} catch (UnsatisfiablePathConditionException e) {
				return new BitSet();
			}
			argumentValues[i] = eval.value;
			state = eval.state;
		}

		switch (function) {
			case "$comm_dequeue" :
				NumericExpression argSrc = (NumericExpression) argumentValues[1];
				Reasoner reasoner = universe
						.reasoner(state.getPathCondition(universe));

				if (reasoner.isValid(universe.lessThanEquals(zero, argSrc))) {
					return this.computeAmpleSetByHandleObject(state, pid,
							arguments[0], argumentValues[0], setsReachableRead,
							setsReachableWrite);
				} else {
					for (int otherPid : procIdsInComm(state, pid, process,
							arguments, argumentValues))
						ampleSet.set(otherPid);
				}
				return ampleSet;
			case "$comm_enqueue" :
				// Because we don't know if other processes will call an wild
				// card receive(dequeue), we have to put all processes into
				// ample process set.
				ampleSet = this.computeAmpleSetByHandleObject(state, pid,
						arguments[0], argumentValues[0], setsReachableRead,
						setsReachableWrite);

				if (civlConfig.checkDeadlockKind()
						.equals(DeadlockKind.POTENTIAL)) {
					BooleanExpression hasMatchedDequeue;

					hasMatchedDequeue = this.hasMatchedDequeue(state, pid,
							process, call, false);
					if (hasMatchedDequeue.isFalse()) {
						for (int otherPid : this.procIdsInComm(state, pid,
								process, arguments, argumentValues))
							ampleSet.set(otherPid);
					}
				}
				return ampleSet;
			default :
				throw new CIVLInternalException("Unreachable" + function,
						source);
		}
	}

	/**
	 * Emanating one or multiple transitions from the given state which is at
	 * the location: $comm_dequeue().
	 * 
	 * @param state
	 *            The current state.
	 * @param call
	 *            The function call statement, upon which the set of enabled
	 *            transitions will be computed.
	 * @param pathCondition
	 *            The current path condition.
	 * @param pid
	 *            The ID of the process that the function call belongs to.
	 * @param assignAtomicLock
	 *            The assignment statement for the atomic lock variable, should
	 *            be null except that the process is going to re-obtain the
	 *            atomic lock variable.
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private List<Transition> enabledCommDequeueTransitions(State state,
			CallOrSpawnStatement call, BooleanExpression clause, int pid)
			throws UnsatisfiablePathConditionException {
		List<Expression> arguments = call.arguments();
		List<Transition> localTransitions = new LinkedList<>();
		Evaluation eval;
		String process = "p" + pid;
		Reasoner reasoner = universe.reasoner(
				universe.and(state.getPathCondition(universe), clause));
		IntegerNumber argSourceNumber; // numeric object of the value of source
		int intSource, intDest;
		// set of all available sources
		List<NumericExpression> possibleSources;
		Expression commHandleExpr, sourceExpr, tagExpr;
		SymbolicExpression gcommHandle, gcomm, comm, commHandle, dest;
		// Set of transition statements
		List<Statement> callWorkers = new LinkedList<>();
		boolean isWildcardSrc = false;
		NumericExpression tag;
		boolean isWildcardTag = false;

		// evaluate the second argument: source
		eval = evaluator.evaluate(
				stateFactory.addToPathcondition(state, pid, clause), pid,
				arguments.get(1));
		state = eval.state;
		argSourceNumber = (IntegerNumber) reasoner
				.extractNumber((NumericExpression) eval.value);
		if (argSourceNumber == null)
			throw new CIVLUnimplementedFeatureException(
					"CIVL doesn't support using non-concrete source of messages \n",
					arguments.get(1).getSource());
		else
			intSource = argSourceNumber.intValue();
		// clause: source >= 0
		// If and only if "source < 0" is true, the "comm_dequeue()" becomes
		// non-deterministic.
		if (intSource == -1)
			isWildcardSrc = true;
		// Initialize arguments
		possibleSources = new LinkedList<>();
		commHandleExpr = arguments.get(0);
		sourceExpr = arguments.get(1);
		tagExpr = arguments.get(2);
		commHandle = evaluator.evaluate(state, pid, commHandleExpr).value;
		comm = evaluator.dereference(commHandleExpr.getSource(), state, pid,
				process, commHandle, false, true).value;
		dest = this.universe.tupleRead(comm, zeroObject);
		gcommHandle = this.universe.tupleRead(comm, oneObject);
		gcomm = evaluator.dereference(commHandleExpr.getSource(), state, pid,
				process, gcommHandle, false, true).value;
		assert (dest instanceof NumericExpression) : "Argument of destination of $comm_dequeue() should be a numeric type.\n";
		intDest = ((IntegerNumber) reasoner
				.extractNumber((NumericExpression) dest)).intValue();
		eval = evaluator.evaluate(state, pid, tagExpr);
		state = eval.state;
		tag = (NumericExpression) eval.value;
		if (reasoner.isValid(universe.equals(tag, universe.integer(-2))))
			isWildcardTag = true;
		if (isWildcardSrc) {
			LibcommEvaluator libevaluator;

			try {
				libevaluator = (LibcommEvaluator) this.libEvaluatorLoader
						.getLibraryEvaluator(this.name, evaluator,
								this.modelFactory, symbolicUtil,
								symbolicAnalyzer);
				possibleSources = libevaluator.getAllPossibleSources(eval.state,
						reasoner, gcomm, intSource, intDest, tag, isWildcardTag,
						call.getSource());
			} catch (LibraryLoaderException e) {
				throw new CIVLInternalException(
						"LibraryLoader exception happens when loading library comm evaluator.\n",
						call.getSource());
			}
			callWorkers = (List<Statement>) this.dequeueStatementGenerator(
					sourceExpr, tagExpr, possibleSources, call.getSource(),
					call.function().parameters(), arguments,
					call.function().returnType(), call.statementScope(),
					call.guard(), call.target(), call.lhs(),
					call.isInitializer());
			for (int j = 0; j < callWorkers.size(); j++)
				localTransitions.add(Semantics.newTransition(pid, clause,
						callWorkers.get(j)));
		} else
			localTransitions.add(Semantics.newTransition(pid, clause, call));
		return localTransitions;
	}

	/**
	 * Generates a list of "comm_dequeue()" statements whose second argument
	 * "source" is always non-wild card and has a exact concrete value, which is
	 * decided by the passed in list of numbers of all possible sources. This
	 * method also helps building the statement expression from a bunch of
	 * parameters and configurations.
	 * 
	 * @param sourceExpr
	 *            The expression of source argument of the function statement
	 * @param tagExpr
	 *            The expression of tag argument of the function statement
	 * @param possibleSources
	 *            The list contains all possible values of the source argument.
	 * @param civlsource
	 *            CIVL source of the function statement
	 * @param parameters
	 *            A list of {@link Variable}s of parameters of the function
	 * @param arguments
	 *            A list of {@link Expression}s of parameters of the function
	 * @param returnType
	 *            The CIVL type of the function return type
	 * @param containingScope
	 *            The containing scope of the function in this statement.
	 * @param callGuard
	 *            The guard of this statement
	 * @param callTarget
	 *            The target location of the function statement
	 * @param lhs
	 *            The left hand side expression of this function statement
	 * @param assignAtomicLock
	 *            The assignment statement for the atomic lock variable, should
	 *            be null except that the process is going to re-obtain the
	 *            atomic lock variable.
	 * @param initializeLHS
	 *            a boolean value indicating if the returned value of this call
	 *            statement will initialize a left-hand side expression
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private Iterable<Statement> dequeueStatementGenerator(Expression sourceExpr,
			Expression tagExpr, List<NumericExpression> possibleSources,
			CIVLSource civlsource, List<Variable> parameters,
			List<Expression> arguments, CIVLType returnType,
			Scope containingScope, Expression callGuard, Location callTarget,
			LHSExpression lhs, boolean initializeLHS)
			throws UnsatisfiablePathConditionException {
		CallOrSpawnStatement callWorker;
		List<Statement> transitionStatements = new LinkedList<>();
		List<Expression> newArgs;
		SystemFunction dequeueWorkFunction;
		FunctionIdentifierExpression dequeueWorkPointer;
		Location newLocation = null;
		String dequeueWork = "$comm_dequeue_work";
		List<Variable> parametersCopy = new LinkedList<>();
		Scope parameterScopeCopy = this.modelFactory.scope(civlsource,
				containingScope, new ArrayList<>(0), null);

		// copy new instances of parameters:
		for (Variable var : parameters) {
			Variable parameter = modelFactory.variable(var.getSource(),
					var.type(), var.name(), var.vid());

			parametersCopy.add(parameter);
			parameterScopeCopy.addVariable(parameter);
		}
		dequeueWorkFunction = modelFactory.systemFunction(civlsource,
				modelFactory.identifier(civlsource, dequeueWork),
				parameterScopeCopy, parametersCopy, returnType, containingScope,
				this.name);
		parameterScopeCopy.setFunction(dequeueWorkFunction);
		dequeueWorkPointer = modelFactory
				.functionIdentifierExpression(civlsource, dequeueWorkFunction);
		newArgs = new LinkedList<Expression>(arguments);
		// dummy location:
		newLocation = modelFactory.location(civlsource, containingScope);
		for (NumericExpression newSource : possibleSources) {
			int int_newSource;

			try {
				int_newSource = ((IntegerNumber) universe
						.extractNumber(newSource)).intValue();
			} catch (ClassCastException e) {
				throw new CIVLInternalException(
						"Unexpected exception when casting Number object of a value of a message source to IntegerNumber object.\n",
						civlsource);
			}
			newArgs = new LinkedList<Expression>(arguments);
			newArgs.set(1,
					modelFactory.integerLiteralExpression(
							arguments.get(1).getSource(),
							BigInteger.valueOf(int_newSource)));
			callWorker = modelFactory.callOrSpawnStatement(civlsource,
					newLocation, true, dequeueWorkPointer, newArgs, callGuard,
					initializeLHS);
			callWorker.setTargetTemp(callTarget);
			// callWorker.setFunction(dequeueWorkPointer);
			callWorker.setLhs(lhs);
			callWorker.source().removeOutgoing(callWorker);
			transitionStatements.add(callWorker);
		}
		return transitionStatements;
	}

	/**
	 * Return a {@link BooleanExpression} as a result of weather the current
	 * <code>comm_enqueue</code> call has a matched <code>comm_dequeue</code>
	 * statement.
	 * 
	 * @param state
	 *            The current state
	 * @param pid
	 *            The PID of the process
	 * @param process
	 *            The String identifier of the process
	 * @param enqueue_call
	 *            The <code>comm_enqueue</code> {@link CallOrSpawnStatement}
	 * @param wildcardCounts
	 *            A flag indicates that if a wild-card <code>comm_dequeue</code>
	 *            is a matched one
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	public BooleanExpression hasMatchedDequeue(State state, int pid,
			String process, CallOrSpawnStatement enqueue_call,
			boolean wildcardCounts) throws UnsatisfiablePathConditionException {
		LibcommEvaluator libevaluator;
		Expression commHandleExpr = enqueue_call.arguments().get(0);
		Expression msgExpr = enqueue_call.arguments().get(1);
		SymbolicExpression comm, gcomm, place, dest, tag, msg;
		SymbolicExpression procArray, candidateProc;
		Evaluation eval;
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		int candidateProcId;

		eval = evaluator.evaluate(state, pid, enqueue_call.guard());
		state = eval.state;
		// False -> False <=> True
		// No enqueue statement enabled -> no matched dequeue <=> True
		if (eval.value.isFalse())
			return trueValue;
		try {
			libevaluator = (LibcommEvaluator) this.libEvaluatorLoader
					.getLibraryEvaluator(this.name, evaluator, modelFactory,
							symbolicUtil, symbolicAnalyzer);
		} catch (LibraryLoaderException e) {
			throw new CIVLInternalException(
					"LibcommEnabler loads LibcommEvaluator failed",
					(CIVLSource) null);
		}

		SymbolicExpression commHandle;
		BooleanExpression commHandleIsNull;

		eval = evaluator.evaluate(state, pid, commHandleExpr);
		commHandle = eval.value;
		commHandleIsNull = universe.equals(commHandle,
				symbolicUtil.nullPointer());
		if (commHandleIsNull.isTrue())
			return universe.falseExpression();
		assert commHandleIsNull
				.isFalse() : "expecting concrete boolean value for "
						+ commHandleIsNull;
		eval = evaluator.dereference(commHandleExpr.getSource(), eval.state,
				pid, process, commHandle, false, true);
		comm = eval.value;
		eval = libevaluator.getGcommByComm(eval.state, pid, process, eval.value,
				commHandleExpr.getSource());
		gcomm = eval.value;
		eval = evaluator.evaluate(state, pid, msgExpr);
		msg = eval.value;
		state = eval.state;
		procArray = universe.tupleRead(gcomm, oneObject);
		dest = universe.tupleRead(msg, oneObject);
		place = universe.tupleRead(msg, zeroObject);
		tag = universe.tupleRead(msg, twoObject);
		candidateProc = libevaluator.readProcArray(state, pid, process,
				procArray, (NumericExpression) dest, enqueue_call.getSource());
		candidateProcId = modelFactory.getProcessId(candidateProc);
		if (candidateProcId < 0)
			return falseValue;

		ProcessState procState = state.getProcessState(candidateProcId);
		Iterable<Statement> procOutgoings;
		Iterator<Statement> iter;
		BooleanExpression result = falseValue;

		if (!procState.hasEmptyStack()) {
			procOutgoings = procState.peekStack().location().outgoing();
			iter = procOutgoings.iterator();
			while (iter.hasNext()) {
				Statement procCall = iter.next();

				if (procCall.statementKind()
						.equals(StatementKind.CALL_OR_SPAWN)) {
					BooleanExpression hasMatched = this
							.isMatchedDequeueStatement(state,
									candidateProcId, procState.getPid(),
									libevaluator, reasoner,
									(CallOrSpawnStatement) procCall, place,
									tag, comm, wildcardCounts);
					if (hasMatched.isTrue())
						return hasMatched;
					else
						result = universe.or(result, hasMatched);
				}

			}
		}
		return result;
	}

	/**
	 * A helper function for
	 * {@link #hasMatchedDequeue(State, int, String, CallOrSpawnStatement, boolean)}
	 * . Given a <code>comm_dequeue</code> statement and the expected message
	 * source, message tag and the communicator handle, returns the true value
	 * of {@link BooleaneExpression} if the <code>comm_dequeue</code> is matched
	 * with those given parameters.
	 * 
	 * @param state
	 *            The current state
	 * @param pid
	 *            The PID of the process
	 * @param identifier
	 *            The identifier of the process
	 * @param libevaluator
	 *            A instance of {@link LibcommEvaluator}
	 * @param reasoner
	 *            A instance of {@link Reasoner}
	 * @param call
	 *            The testing <code>comm_dequeue</code> statement
	 * @param expectedSource
	 *            The message source from the sender (which is a enqueue
	 *            statement)
	 * @param expectedTag
	 *            the message tag from the sender (which is a enqueue statement)
	 * @param expectedComm
	 *            the handle of the communicator of the sender (which is a
	 *            enqueue statement)
	 * @param wildcardCounts
	 *            A flag indicates that if a wild-card <code>comm_dequeue</code>
	 *            is a matched one
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private BooleanExpression isMatchedDequeueStatement(State state, int pid,
			int identifier, LibcommEvaluator libevaluator, Reasoner reasoner,
			CallOrSpawnStatement call, SymbolicExpression expectedSource,
			SymbolicExpression expectedTag, SymbolicExpression expectedComm,
			boolean wildcardCounts) throws UnsatisfiablePathConditionException {
		String dequeueName = "$comm_dequeue";
		String process = "p" + identifier + " (id = " + pid + ")";
		BooleanExpression claim1, claim2, claim3;

		if (call.function().name().name().equals(dequeueName)) {
			Expression commHandleExpr = call.arguments().get(0);
			Expression sourceExpr = call.arguments().get(1);
			Expression tagExpr = call.arguments().get(2);
			Evaluation eval;
			SymbolicExpression enqueueGcommHandle, myGcommHandle, mySource,
					myTag;
			// the GcommHandles of enqueue and dequeue should be same

			eval = libevaluator.getCommByCommHandleExpr(state, pid, process,
					commHandleExpr);
			state = eval.state;
			myGcommHandle = universe.tupleRead(eval.value, oneObject);
			enqueueGcommHandle = universe.tupleRead(expectedComm, oneObject);
			eval = evaluator.evaluate(state, pid, sourceExpr);
			state = eval.state;
			mySource = eval.value;
			eval = evaluator.evaluate(state, pid, tagExpr);
			state = eval.state;
			myTag = eval.value;
			claim1 = universe.equals(mySource, expectedSource);
			if (wildcardCounts)
				claim1 = universe.or(claim1,
						universe.equals(mySource, universe.integer(-1)));
			claim2 = universe.equals(myTag, expectedTag);
			claim2 = universe.or(claim2,
					universe.equals(myTag, universe.integer(-2)));
			claim3 = universe.equals(myGcommHandle, enqueueGcommHandle);
			// TODO change to andTo
			return universe.and(universe.and(claim1, claim2), claim3);
		}
		return falseValue;
	}

	// TODO: what if process x still not add itself into gcomm then the value of
	// it in procArray in gcomm is -1?
	private Set<Integer> procIdsInComm(State state, int pid, String process,
			Expression arguments[], SymbolicExpression argumentValues[])
			throws UnsatisfiablePathConditionException {
		Set<Integer> procs = new HashSet<>();

		for (int procid = 0; procid < state.numProcs(); procid++)
			procs.add(procid);
		return procs;
	}
}
