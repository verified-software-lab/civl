package dev.civl.mc.kripke.common;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dev.civl.gmc.GMCConfiguration;
import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.kripke.IF.Enabler;
import dev.civl.mc.kripke.IF.LibraryEnabler;
import dev.civl.mc.kripke.IF.LibraryEnablerLoader;
import dev.civl.mc.log.IF.CIVLErrorLogger;
import dev.civl.mc.model.IF.CIVLFunction;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.Model;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.SystemFunction;
import dev.civl.mc.model.IF.contract.FunctionContract;
import dev.civl.mc.model.IF.expression.BooleanLiteralExpression;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.Expression.ExpressionKind;
import dev.civl.mc.model.IF.location.Location;
import dev.civl.mc.model.IF.statement.CallOrSpawnStatement;
import dev.civl.mc.model.IF.statement.Statement;
import dev.civl.mc.model.IF.statement.Statement.StatementKind;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.semantics.IF.Evaluator;
import dev.civl.mc.semantics.IF.Executor;
import dev.civl.mc.semantics.IF.LibraryLoaderException;
import dev.civl.mc.semantics.IF.Semantics;
import dev.civl.mc.semantics.IF.SymbolicAnalyzer;
import dev.civl.mc.semantics.IF.Transition;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.StateFactory;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.mc.util.IF.SeqSet;
import dev.civl.mc.util.IF.Triple;
import dev.civl.sarl.IF.Reasoner;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.SymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.util.EmptySet;

/**
 * <p>
 * This is an implementation of {@link Enabler} that aims to be simple and clear
 * while still being reasonably efficient. It implements a partial order
 * reduction scheme based on the dependency and reachability relations. See
 * {@link StrongConnect} for details on the POR scheme.
 * </p>
 * 
 * <p>
 * This class provides the implementations of the methods specified in
 * {@link Enabler}, but most of the work is done in two other classes:
 * {@link SimpleEnablerWorker} and {@link StrongConnect}. To compute an ample
 * set, this class instantiates a new worker, which does most of the work. This
 * class itself provides some general-purpose utility methods that are used by
 * the worker and can be used by other classes.
 * </p>
 * 
 * <p>
 * The worker creates a new instance of {@link StrongConnect} to carry out
 * Tarjan's algorithm to compute the strongly connected components of a directed
 * graph. The nodes in the graph are the processes, and there is an edge p->q if
 * whenever p is included in an ample set, q must also be included.
 * </p>
 * 
 * @author siegel
 */
public class SimpleEnabler implements Enabler {

	/* *************************** Instance Fields ************************* */

	/**
	 * Turn on/off debugging option to print more information.
	 */
	protected boolean debugging = false;

	/**
	 * The output stream for printing debugging information.
	 */
	protected PrintStream debugOut = System.out;

	/**
	 * The unique evaluator used by the system for evaluating expressions.
	 */
	protected Evaluator evaluator;

	/**
	 * The executor used to execute statements.
	 */
	private Executor executor;

	/**
	 * The unique model factory used by the system.
	 */
	protected ModelFactory modelFactory;

	/**
	 * The option to enable/disable the printing of ample sets of each state.
	 */
	protected boolean showAmpleSet = false;

	/**
	 * Show the impact/reachable memory units?
	 */
	protected boolean showMemoryUnits = false;

	/**
	 * If negative, ignore, otherwise an upper bound on the number of live
	 * processes.
	 */
	protected int procBound;

	/**
	 * The unique symbolic universe used by the system.
	 */
	protected SymbolicUniverse universe;

	/**
	 * The symbolic expression for the boolean value false.
	 */
	protected BooleanExpression falseExpression;

	/**
	 * The symbolic expression for the boolean value true.
	 */
	protected BooleanExpression trueExpression;

	/**
	 * The library enabler loader.
	 */
	protected LibraryEnablerLoader libraryLoader;

	/**
	 * Show ample sets with the states?
	 */
	protected boolean showAmpleSetWtStates = false;

	/**
	 * The state factory that provides operations on states.
	 */
	protected StateFactory stateFactory;

	/**
	 * The error logger for reporting errors.
	 */
	protected CIVLErrorLogger errorLogger;

	/**
	 * The symbolic analyzer to be used.
	 */
	protected SymbolicAnalyzer symbolicAnalyzer;

	/**
	 * CIVL configuration object, which specifies values for all the
	 * command-line options.
	 */
	protected CIVLConfiguration config;

	/**
	 * Used by the state collation module which is an experimental feature used
	 * for checking collective properties.
	 */
	protected CollateExecutor collateExecutor;

	/**
	 * The system function named {@code $wait}, specified in civlc.cvh.
	 */
	protected CIVLFunction waitFunction;

	/**
	 * The system function named {@code $yield}, used by a process in an atomic
	 * block to release the lock temporarily and allow other processes to
	 * execute. This may be {@code null} if the model being analyzed does not
	 * use this function.
	 */
	protected CIVLFunction yieldFunction;

	/**
	 * The system function named {@code $assume} used to specify an assumption.
	 * This may be {@code null} if the model being analyzed does not use this
	 * function.
	 */
	protected CIVLFunction assumeFunction;

	/**
	 * The system function named {@code $comm_enqueue}, used to enqueue data
	 * onto a FIFO message queue, used for message-passing communication. This
	 * may be {@code null} if the model being analyzed does not use this
	 * function.
	 */
	protected CIVLFunction commEnqueueFunction;

	/**
	 * The variable used to control access to atomic sections. It is declared
	 * implicitly in every model. It has type $proc.
	 */
	protected Variable atomicLockVariable;

	/**
	 * The variable ID number of the atomic lock variable.
	 */
	protected int atomicLockVariableVid;

	/**
	 * The static scope ID number of the atomic lock variable.
	 */
	protected int atomicLockVariableScopeId;

	/**
	 * An unmodifiable empty set of transitions.
	 */
	private Collection<Transition> emptySet;

	/**
	 * Special symbolic constant of function type used to "hide" pointer values
	 * from the reachability analysis.
	 */
	protected SymbolicConstant hideFunction;

	/* ***************************** Constructor *************************** */

	/**
	 * Creates a new instance of Enabler, using the given arguments to
	 * initialize many of the instance fields.
	 * 
	 * @param stateFactory
	 *                             factory that will be used for creating new
	 *                             states or reading information from states
	 * 
	 * @param evaluator
	 *                             used for evaluating expressions
	 * @param executor
	 *                             used to execute {@link Statement}s
	 * @param symbolicAnalyzer
	 *                             utility serving as higher-level interface to
	 *                             SARL (symbolic execution engine)
	 * @param libLoader
	 *                             used to find the classes (enablers,
	 *                             executors) implementing libraries and load
	 *                             them
	 * @param errorLogger
	 *                             used to log errors (violations) as they are
	 *                             encountered
	 * @param civlConfig
	 *                             class providing all of the configuration
	 *                             parameters provided by the user at startup
	 * @param gmcConfig
	 *                             configuration parameters for the GMC (generic
	 *                             model checker), which are in addition to
	 *                             those of CIVL
	 */
	public SimpleEnabler(StateFactory stateFactory, Evaluator evaluator,
			Executor executor, SymbolicAnalyzer symbolicAnalyzer,
			LibraryEnablerLoader libLoader, CIVLErrorLogger errorLogger,
			CIVLConfiguration civlConfig, GMCConfiguration gmcConfig) {
		this.errorLogger = errorLogger;
		this.evaluator = evaluator;
		this.executor = executor;
		this.symbolicAnalyzer = symbolicAnalyzer;
		this.debugOut = civlConfig.out();
		this.debugging = civlConfig.debug();
		this.showAmpleSet = civlConfig.showAmpleSet()
				|| civlConfig.showAmpleSetWtStates();
		this.showAmpleSetWtStates = civlConfig.showAmpleSetWtStates();
		this.modelFactory = evaluator.modelFactory();
		this.universe = modelFactory.universe();
		this.falseExpression = universe.falseExpression();
		this.trueExpression = universe.trueExpression();
		this.libraryLoader = libLoader;
		this.stateFactory = stateFactory;
		this.showMemoryUnits = civlConfig.showMemoryUnits();
		this.procBound = civlConfig.getProcBound();
		this.config = civlConfig;
		this.collateExecutor = new CollateExecutor(this, this.executor,
				errorLogger, civlConfig, gmcConfig);

		Model model = modelFactory.model();

		// the following will be null iff the model does not use $yield:
		this.yieldFunction = model.function("$yield");
		// ditto:
		this.waitFunction = model.function("$wait");
		this.assumeFunction = model.function("$assume");
		this.commEnqueueFunction = model.function("$comm_enqueue");
		this.emptySet = new EmptySet<Transition>();
		this.atomicLockVariable = modelFactory.atomicLockVariableExpression()
				.variable();
		this.atomicLockVariableVid = atomicLockVariable.vid();
		this.atomicLockVariableScopeId = atomicLockVariable.scope().id();
		this.hideFunction = modelFactory.getHideConstant();

	}

	/* ************************** Private Methods ************************ */

	/**
	 * <p>
	 * Computes the guard from the contract of the called function, in the case
	 * when the called function is not known statically.
	 * </p>
	 * 
	 * <p>
	 * For a call statement for which the function expression is not an
	 * identifier (the usual case), the called function is not known statically.
	 * In this case, the function guard (specified perhaps in the enabled_when
	 * clause of the function contract) was not built into the statement's guard
	 * expression, and must therefore be computed dynamically, at the given
	 * state.
	 * </p>
	 * 
	 * <p>
	 * If the given {@code statement} is not a call statement, {@code null} is
	 * returned. If the function expression of the call statement is an
	 * identifier (the normal case), then
	 * {@link CallOrSpawnStatement#function()} returns something non-null, and
	 * this method will return {@code null}. Otherwise, the function expression
	 * is evaluated at the given {@code state} to determine the called function
	 * f. If f has a contract and that contract specifies a guard, then that
	 * guard is returned, otherwise {@code null} is returned.
	 * </p>
	 * 
	 * @param state
	 *                      the state from which the call will take place
	 * @param pid
	 *                      the ID of the process in which the call takes place
	 * @param statement
	 *                      a call statement
	 * @return the guard expression, or {@code null} (if {@code statement} is
	 *         not a call or if the called function is statically known)
	 * @throws UnsatisfiablePathConditionException
	 *                                                 if in the course of
	 *                                                 evaluating the function
	 *                                                 expression it is
	 *                                                 determined that the path
	 *                                                 condition is not
	 *                                                 satisfiable
	 */
	private Expression getDynamicGuard(State state, int pid,
			CallOrSpawnStatement statement)
			throws UnsatisfiablePathConditionException {
		if (!statement.isCall())
			return null;

		CIVLFunction function = statement.function();

		if (function != null)
			return null;

		Expression functionExpr = statement.functionExpression();
		Triple<State, CIVLFunction, Integer> triple = evaluator
				.evaluateFunctionIdentifier(state, pid, functionExpr,
						functionExpr.getSource());

		function = triple.second;

		FunctionContract contract = function.functionContract();

		if (contract == null)
			return null;

		return contract.guard();
	}

	/* ************************* Protected Methods *********************** */

	/**
	 * Gets the function being called or spawned in a call or spawn statement.
	 * Usually this is obvious since the function expression is an identifier so
	 * the function is known statically. However the function expression can be
	 * any expression (think function pointers in C), and in general may only be
	 * known dynamically. This method will figure it out in either case.
	 * 
	 * @param state
	 *                      the state in which the function is called or
	 *                      spawned; needed in case the function expression is
	 *                      not statically known
	 * @param pid
	 *                      the ID of the process performing the call or spawn
	 * @param statement
	 *                      the call or spawn statement
	 * @return the function called or spawned
	 * @throws UnsatisfiablePathConditionException
	 *                                                 if in the course of
	 *                                                 evaluating the function
	 *                                                 expression it is
	 *                                                 determined that the path
	 *                                                 condition is not
	 *                                                 satisfiable
	 */
	protected CIVLFunction getFunction(State state, int pid,
			CallOrSpawnStatement statement)
			throws UnsatisfiablePathConditionException {
		CIVLFunction function = statement.function();

		if (function == null) {
			Expression functionExpr = statement.functionExpression();
			Triple<State, CIVLFunction, Integer> triple = evaluator
					.evaluateFunctionIdentifier(state, pid, functionExpr,
							functionExpr.getSource());

			function = triple.second;
		}
		return function;
	}

	/**
	 * Is the given expression the boolean expression "true"?
	 * 
	 * @param expr
	 *                 a non-null CIVL {@link Expression}
	 * @return {@code true} iff {@code expr} is the expression "true"
	 */
	protected boolean isTrue(Expression expr) {
		return expr.expressionKind() == ExpressionKind.BOOLEAN_LITERAL
				&& ((BooleanLiteralExpression) expr).value();
	}

	/**
	 * Is the given {@link Statement} the {@code $yield} statement?
	 * 
	 * @param stmt
	 *                 a (non-null) {@link Statement}
	 * @return {@code true} iff {@code stmt} is the {@code $yield statement}
	 */
	protected boolean isYield(Statement stmt) {
		if (yieldFunction == null)
			return false;
		return stmt.statementKind() == StatementKind.CALL_OR_SPAWN
				&& ((CallOrSpawnStatement) stmt).isCall()
				&& ((CallOrSpawnStatement) stmt).function() == yieldFunction;
	}

	/**
	 * Is the given {@link Statement} a {@code $wait} statement?
	 * 
	 * @param stmt
	 *                 a (non-null) {@link Statement}
	 * @return {@code true} iff {@code stmt} is a {@code $wait statement}
	 */
	public boolean isWait(Statement stmt) {
		if (waitFunction == null)
			return false;
		return stmt.statementKind() == StatementKind.CALL_OR_SPAWN
				&& ((CallOrSpawnStatement) stmt).isCall()
				&& ((CallOrSpawnStatement) stmt).function() == waitFunction;
	}

	/**
	 * Is the given {@link Statement} a call of a system function? This method
	 * will produce the correct answer even if the call is through a function
	 * pointer or other complex function expression.
	 * 
	 * @param state
	 *                  the state from which the statement is executed
	 * @param pid
	 *                  the ID of the process executing {@code stmt}
	 * @param stmt
	 *                  the statement being executed
	 * @return {@code true} iff {@code stmt} is a call of a system function
	 * @throws UnsatisfiablePathConditionException
	 *                                                 if in the course of
	 *                                                 evaluating the function
	 *                                                 expression it is
	 *                                                 discovered that the path
	 *                                                 condition of
	 *                                                 {@code state} is
	 *                                                 unsatisfiable
	 */
	protected boolean isSystemCall(State state, int pid, Statement stmt)
			throws UnsatisfiablePathConditionException {
		if (stmt.statementKind() == StatementKind.CALL_OR_SPAWN) {
			CallOrSpawnStatement call = (CallOrSpawnStatement) stmt;

			if (call.isCall()) {
				CIVLFunction function = getFunction(state, pid, call);

				if (function.isSystemFunction())
					return true;
			}
		}
		return false;
	}

	/**
	 * Is the given statement a "send" operation, i.e., a call of function
	 * {@code $comm_enqueue}?
	 * 
	 * @param state
	 *                  the state from which the statement is executed
	 * @param pid
	 *                  the ID of the process executing the statement
	 * @param stmt
	 *                  any non-null CIVL {@link Statement}
	 * @return {@code true} iff {@code stmt} is a call of function
	 *         {@code $comm_enqueue}
	 * @throws UnsatisfiablePathConditionException
	 *                                                 if it is determined that
	 *                                                 the path condition of
	 *                                                 {@code state} is
	 *                                                 unsatisfiable
	 */
	protected boolean isSend(State state, int pid, Statement stmt)
			throws UnsatisfiablePathConditionException {
		if (commEnqueueFunction == null)
			return false;
		if (stmt.statementKind() == StatementKind.CALL_OR_SPAWN) {
			CallOrSpawnStatement call = (CallOrSpawnStatement) stmt;

			if (call.isCall()) {
				CIVLFunction function = getFunction(state, pid, call);

				if (function == commEnqueueFunction)
					return true;
			}
		}
		return false;
	}

	/**
	 * Is the given {@link Statement} an invocation of the {@code $assume}
	 * statement?
	 * 
	 * @param stmt
	 *                 a (non-null) {@link Statement}
	 * @return {@code true} iff {@code stmt} is an invocation of the
	 *         {@code $assume statement}
	 */
	protected boolean isAssume(Statement stmt) {
		if (assumeFunction == null)
			return false;
		return stmt.statementKind() == StatementKind.CALL_OR_SPAWN
				&& ((CallOrSpawnStatement) stmt).isCall()
				&& ((CallOrSpawnStatement) stmt).function() == assumeFunction;
	}

	/**
	 * <p>
	 * Computes the new state resulting from executing a function call, for the
	 * purposes of evaluating contract clauses. The new frame pushed onto the
	 * call stack will have static scope the contract scope of the function.
	 * Since this method only requires that the called function have a contract,
	 * it is appropriate for system functions. The body of the called function,
	 * if it exists, is not used.
	 * </p>
	 * 
	 * <p>
	 * Note: For functions with a variable number of arguments (which are
	 * necessarily system functions), the extra arguments are ignored. Since
	 * these extra arguments cannot appear in function contracts, which is the
	 * whole purpose of this method, they can be safely ignored.
	 * </p>
	 * 
	 * @param state
	 *                      the original state
	 * @param pid
	 *                      the ID of the process performing the call
	 * @param function
	 *                      the function being called
	 * @param arguments
	 *                      the actual argument expressions in the call
	 * @return the new state immediately after the call
	 * @throws UnsatisfiablePathConditionException
	 *                                                 if in the course of
	 *                                                 evaluating the arguments
	 *                                                 it is discovered that the
	 *                                                 path condition is
	 *                                                 unsatisfiable
	 */
	protected State executeContract(State state, int pid, CIVLFunction function,
			List<Expression> arguments)
			throws UnsatisfiablePathConditionException {
		int numArgs = arguments.size();
		int numParams = function.functionType().parameterTypes().length;

		// functions with variable number of arguments can have more
		// arguments than parameters. Ignore the extra arguments.
		assert numArgs >= numParams;

		SymbolicExpression[] argumentValues = new SymbolicExpression[numParams];
		Iterator<Expression> argIter = arguments.iterator();

		for (int i = 0; i < numParams; i++)
			argumentValues[i] = evaluator.evaluate(state, pid,
					argIter.next()).value;
		return stateFactory.pushContract(state, pid, function, argumentValues);
	}

	/**
	 * <p>
	 * Computes the new state resulting from executing a function call of a
	 * function with a body. This is for the purposes of analyzing the state the
	 * model will be in immediately after that call. The new location will be
	 * the start location of the function. This works correctly for atomic
	 * functions as well as non-atomic functions.
	 * </p>
	 * 
	 * <p>
	 * Note: For functions with a variable number of arguments (which are
	 * necessarily system functions), the extra arguments are ignored. Since
	 * these extra arguments cannot appear in function contracts, which is the
	 * whole purpose of this method, they can be safely ignored.
	 * </p>
	 * 
	 * @param state
	 *                      the original state
	 * @param pid
	 *                      the ID of the process performing the call
	 * @param function
	 *                      the function being called
	 * @param arguments
	 *                      the actual argument expressions in the call
	 * @return the new state immediately after the call (i.e., just after the
	 *         new frame is pushed onto the call stack and control enters the
	 *         called function)
	 * @throws UnsatisfiablePathConditionException
	 *                                                 if in the course of
	 *                                                 evaluating the arguments
	 *                                                 it is discovered that the
	 *                                                 path condition is
	 *                                                 unsatisfiable
	 */
	protected State executeCall(State state, int pid, CIVLFunction function,
			List<Expression> arguments)
			throws UnsatisfiablePathConditionException {
		int numArgs = arguments.size();
		int numParams = function.functionType().parameterTypes().length;

		assert numArgs >= numParams;

		SymbolicExpression[] argumentValues = new SymbolicExpression[numParams];
		Iterator<Expression> argIter = arguments.iterator();

		for (int i = 0; i < numParams; i++)
			argumentValues[i] = evaluator.evaluate(state, pid,
					argIter.next()).value;

		State result = stateFactory.pushCallStack(state, pid, function,
				argumentValues);

		if (function.isAtomicFunction())
			result = stateFactory.enterAtomic(result, pid);
		return result;
	}

	/**
	 * Computes the guard for a specific statement of a specific process at a
	 * given state. Several factors go into the computation of the guard, and
	 * this method handles them all correctly. There can be an explicit guard in
	 * the model (using a {@code $when} statement). There are implicit guards on
	 * certain system functions. Examples include: A {@code $yield} statement is
	 * guarded by the guard of the location immediately following the
	 * {@code $yield}. Entrance into an {@code $atomic} block is guarded by the
	 * guard of the first location of the block. A {@code $spawn} statement has
	 * an implicit guard if there is a bound on the number of processes.
	 * 
	 * @param state
	 *                     the state
	 * @param reasoner
	 *                     a {@link Reasoner} based on the path condition of
	 *                     {@code state}
	 * @param pid
	 *                     ID of the process about to execute
	 * @param stmt
	 *                     the {@code Statement} emanating from the source
	 *                     location of process {@code pid} at state
	 *                     {@code state}
	 * @return the symbolic expression of boolean type which evaluates to
	 *         {@code true} iff the statement can execute
	 * @throws UnsatisfiablePathConditionException
	 *                                                 if it is determined that
	 *                                                 the path condition of
	 *                                                 {@code state} is
	 *                                                 unsatisfiable
	 */
	protected BooleanExpression computeGuard(State state, Reasoner reasoner,
			int pid, Statement stmt)
			throws UnsatisfiablePathConditionException {
		int atomicPid = stateFactory.processInAtomic(state);

		if (atomicPid >= 0 && atomicPid != pid)
			return falseExpression; // another process has the atomic lock

		Expression expr = stmt.guard();
		BooleanExpression result = isTrue(expr)
				? trueExpression
				: (BooleanExpression) evaluator.evaluate(state, pid,
						expr).value;

		if (isYield(stmt)) {
			if (atomicPid < 0) {
				// this process can return from $yield and re-take the lock
				// only if the next statement is enabled
				Location loc2 = stmt.target();
				// evaluate all guards from loc2, take disjunction
				BooleanExpression guard2 = falseExpression;

				for (Statement stmt2 : loc2.outgoing())
					guard2 = universe.or(guard2,
							computeGuard(state, reasoner, pid, stmt2));
				result = universe.and(result, guard2);
			}
		} else if (stmt.statementKind() == StatementKind.CALL_OR_SPAWN) {
			CallOrSpawnStatement call = (CallOrSpawnStatement) stmt;

			if (procBound > 0 && call.isSpawn()
					&& state.numLiveProcs() >= procBound)
				return falseExpression;

			// if the called function is statically known and is
			// a system function, the guard is already baked into the
			// $when of the call statement.
			// If the called function is not statically known (because
			// the call happens through a function pointer), see if it
			// is a system function and if so, get the guard...
			// Note: for now, we are ignoring contract guards on
			// non-system atomic functions; these can instead be
			// expressed as $when statements at the beginning of the
			// function body

			Expression dynamicGuard = getDynamicGuard(state, pid, call);
			CIVLFunction function = getFunction(state, pid, call);

			if (dynamicGuard != null) {
				if (!isTrue(dynamicGuard)) {
					State newState = executeContract(state, pid, function,
							call.arguments());
					BooleanExpression dynamicGuardValue = (BooleanExpression) evaluator
							.evaluate(newState, pid, dynamicGuard).value;

					result = universe.and(result, dynamicGuardValue);
				}
			} else if (function.isAtomicFunction()
					&& !function.isSystemFunction() && atomicPid < 0) {
				// function is atomic, has definition, and we don't have
				// the atomic lock. Need to examine start location of
				// function to determine guard...
				State newState = executeCall(state, pid, function,
						call.arguments());
				Location loc2 = function.startLocation();
				BooleanExpression guard2 = falseExpression;

				for (Statement stmt2 : loc2.outgoing())
					guard2 = universe.or(guard2,
							computeGuard(newState, reasoner, pid, stmt2));
				result = universe.and(result, guard2);
			}
		}
		if (result == trueExpression || result == falseExpression)
			return result;
		// for now you have to check if pc is unsat because the
		// deadlock predicates do not and will report spurious errors.
		// fix that and then we can get rid of this:
		if (universe.getShowProverQueries())
			universe.setQueryExplanation("checking guard satisfiability for statement at "
					+ stmt.getSource().getLocation());
		if (reasoner.unsat(result).getResultType() == ResultType.YES)
			return falseExpression;
		return result;
	}

	/**
	 * Computes the guard for a specific statement of a specific process at a
	 * given state. The only difference between this method and method
	 * {@code #computeGuard(State, Reasoner, int, Statement)} is that this
	 * method accept the ID number of the {@link Statement} rather than the
	 * {@link Statement} itself. Use whichever is more convenient.
	 * 
	 * @param state
	 *                     the state
	 * @param reasoner
	 *                     a {@link Reasoner} based on the path condition of
	 *                     {@code state}
	 * @param pid
	 *                     ID of the process about to execute
	 * @param sid
	 *                     the ID number of a {@link Statement} emanating from
	 *                     the source location of process {@code pid} at state
	 *                     {@code state}; from each location, the outgoing
	 *                     statements are numbered from 0
	 * @return the symbolic expression of boolean type which evaluates to
	 *         {@code true} iff the statement can execute
	 * @throws UnsatisfiablePathConditionException
	 *                                                 if it is determined that
	 *                                                 the path condition of
	 *                                                 {@code state} is
	 *                                                 unsatisfiable
	 */
	protected BooleanExpression computeGuard(State state, Reasoner reasoner,
			int pid, int sid) throws UnsatisfiablePathConditionException {
		Location location = state.getProcessState(pid).getLocation();

		Statement stmt = location.getOutgoing(sid);
		return computeGuard(state, reasoner, pid, stmt);
	}

	/**
	 * Finds the {@link LibraryEnabler} for the specified library. A library
	 * enabler is used to determine when system functions defined in that
	 * library are enabled. The library is specified by its name (a
	 * {@link String}) and the enabler is a class with a name of the form
	 * "LibXEnabler".
	 * 
	 * @param civlSource
	 *                       source object for reporting errors
	 * @param library
	 *                       the name of the library, which is how libraries are
	 *                       uniquely identified
	 * @return the library's enabler, or {@code null} if that class cannot be
	 *         found
	 */
	protected LibraryEnabler libraryEnabler(CIVLSource civlSource,
			String library) {
		try {
			return this.libraryLoader.getLibraryEnabler(library, this,
					evaluator, evaluator.modelFactory(),
					evaluator.symbolicUtility(), this.symbolicAnalyzer);
		} catch (LibraryLoaderException e) {
			return null;
		}
	}

	/**
	 * Computes the set of enabled transitions of a call of a system function.
	 * The set is obtained using the function's library's
	 * {@link LibraryEnabler}.
	 * 
	 * @param state
	 *                       the {@link State} from which the system call will
	 *                       take place
	 * @param pid
	 *                       the ID of the process making the call
	 * @param guardValue
	 *                       the value of the guard expression of the call
	 *                       statement, in state {@link #theState}
	 * @param call
	 *                       the call statement
	 * @return the list of transitions enabled by this system call
	 * @throws UnsatisfiablePathConditionException
	 *                                                 if in the process of
	 *                                                 evaluating the function
	 *                                                 expression it is
	 *                                                 determined that the path
	 *                                                 condition of
	 *                                                 {@link #theState} is
	 *                                                 unsatisfiable
	 */
	protected List<Transition> enabledTransitionsOfSystemCall(State state,
			int pid, BooleanExpression guardValue, CallOrSpawnStatement call)
			throws UnsatisfiablePathConditionException {
		SystemFunction sf = (SystemFunction) getFunction(state, pid, call);
		LibraryEnabler lib = libraryEnabler(call.getSource(), sf.getLibrary());

		if (lib != null) {
			return lib.enabledTransitions(state, call, guardValue, pid);
		} else {
			return Arrays
					.asList(Semantics.newTransition(pid, guardValue, call));
		}
	}

	/**
	 * <p>
	 * Creates a single {@link Transition} representing the execution of a
	 * {@link Statement}.
	 * </p>
	 * 
	 * <p>
	 * If the statement is a call of a system function, the library implementing
	 * the system function should return a single {@link Transition} for the
	 * execution of that statement; if this is not the case, this method returns
	 * {@code null}.
	 * </p>
	 * 
	 * @param state
	 *                       the state from which the statement is executed
	 * @param pid
	 *                       the ID of the process executing the statement
	 * @param guardValue
	 *                       the value of the guard for the statement at state
	 *                       {@code state}, as obtained from
	 *                       {@link #computeGuard(State, Reasoner, int, Statement)}
	 * @param stmt
	 *                       the statement
	 * @return a transition wrapping the statement, guard, and pid, or
	 *         {@code null} if the statement is a system function call and the
	 *         implementing library returns n transitions, where n!=1
	 * @throws UnsatisfiablePathConditionException
	 *                                                 if it is discovered that
	 *                                                 the path condition of
	 *                                                 {@code state} is
	 *                                                 unsatisfiable
	 */
	protected Transition singleTransitionFromStatement(State state, int pid,
			BooleanExpression guardValue, Statement stmt)
			throws UnsatisfiablePathConditionException {
		if (guardValue.isFalse())
			return null;
		if (isSystemCall(state, pid, stmt)) {
			List<Transition> list = enabledTransitionsOfSystemCall(state, pid,
					guardValue, (CallOrSpawnStatement) stmt);

			if (list.size() != 1)
				return null;
			return list.get(0);
		} else {
			boolean simplify = isAssume(stmt);
			Transition trans = Semantics.newTransition(pid, guardValue, stmt,
					simplify);

			return trans;
		}
	}

	/* ************************** Public Methods ************************ */
	public Set<Integer> computeDepends(State state, int pid, SeqSet depend,
			SeqSet dependWrite) throws UnsatisfiablePathConditionException {
		SimpleEnablerWorker worker = new SimpleEnablerWorker(this, state);
		return worker.computeDepends(pid, depend, dependWrite);
	}
	
	public void computeReach(State state, int pid, SeqSet reach, SeqSet reachWrite) {
		SimpleEnablerWorker worker = new SimpleEnablerWorker(this, state);
		worker.computeReach(pid, reach, reachWrite);
	}
	
	public Collection<Transition> enabledTransitionsInProcess(State state, int pid) {
		int atomicProc = stateFactory.processInAtomic(state);
		if (atomicProc < 0 || atomicProc == pid) {
			try {
				SimpleEnablerWorker worker = new SimpleEnablerWorker(this,
						state);
				return Arrays.asList(worker.enabledTransitionsInProcess(pid));
			} catch (UnsatisfiablePathConditionException e) {}
		}
		return new LinkedList<>();
	}

	@Override
	public Collection<Transition> ampleSet(State source) {
		if (source.getPathCondition(universe).isFalse())
			return emptySet;
		if (source.numLiveProcs() <= 1)
			return fullSet(source);

		SimpleEnablerWorker worker = new SimpleEnablerWorker(this, source);
		int pid = stateFactory.processInAtomic(source);

		try {
			Transition[] result;

			if (pid >= 0)
				result = worker.enabledTransitionsInProcess(pid);
			else {
				worker.computeAmpleSet();
				result = worker.ampleSet();
			}
			return Arrays.asList(result);
		} catch (UnsatisfiablePathConditionException e) {
			return emptySet;
		}
	}

	@Override
	public Collection<Transition> fullSet(State state) {
		if (state.getPathCondition(universe).isFalse())
			return emptySet;

		SimpleEnablerWorker worker = new SimpleEnablerWorker(this, state);
		int pid = stateFactory.processInAtomic(state);

		try {
			if (pid >= 0)
				return Arrays.asList(worker.enabledTransitionsInProcess(pid));
			else {
				List<Transition> result = new LinkedList<>();
				int nprocs = state.numProcs();

				for (int i = 0; i < nprocs; i++)
					result.addAll(Arrays
							.asList(worker.enabledTransitionsInProcess(i)));
				return result;
			}
		} catch (UnsatisfiablePathConditionException e) {
			return emptySet;
		}
	}

	@Override
	public void setDebugging(boolean value) {
		debugging = value;
	}

	@Override
	public boolean debugging() {
		return debugging;
	}

	@Override
	public void setDebugOut(PrintStream out) {
		debugOut = out;
	}

	@Override
	public PrintStream getDebugOut() {
		return debugOut;
	}

	@Override
	public BooleanExpression getGuard(Statement statement, int pid,
			State state) {
		try {
			return computeGuard(state,
					universe.reasoner(state.getPathCondition(universe)), pid,
					statement);
		} catch (UnsatisfiablePathConditionException e) {
			return falseExpression;
		}
	}

	@Override
	public boolean inAtomic(State state) {
		return stateFactory.lockedByAtomic(state);
	}

}
