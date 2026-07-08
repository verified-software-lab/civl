package dev.civl.mc.library.comm;

import java.util.Arrays;
import java.util.LinkedList;

import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.library.common.BaseLibraryExecutor;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLProperty;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.Model;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.type.CIVLBundleType;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.semantics.IF.Evaluation;
import dev.civl.mc.semantics.IF.Executor;
import dev.civl.mc.semantics.IF.LibraryEvaluatorLoader;
import dev.civl.mc.semantics.IF.LibraryExecutor;
import dev.civl.mc.semantics.IF.LibraryExecutorLoader;
import dev.civl.mc.semantics.IF.SymbolicAnalyzer;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.mc.util.IF.Pair;
import dev.civl.sarl.IF.Reasoner;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.NumericSymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.number.IntegerNumber;
import dev.civl.sarl.IF.number.Number;
import dev.civl.sarl.IF.object.StringObject;
import dev.civl.sarl.IF.type.SymbolicTupleType;
import dev.civl.sarl.IF.type.SymbolicType;

public class LibcommExecutor extends BaseLibraryExecutor
		implements
			LibraryExecutor {
	private LibcommEvaluator libEvaluator;
	/* **************************** Constructors *************************** */

	public LibcommExecutor(String name, Executor primaryExecutor,
			ModelFactory modelFactory, SymbolicUtility symbolicUtil,
			SymbolicAnalyzer symbolicAnalyzer, CIVLConfiguration civlConfig,
			LibraryExecutorLoader libExecutorLoader,
			LibraryEvaluatorLoader libEvaluatorLoader) {
		super(name, primaryExecutor, modelFactory, symbolicUtil,
				symbolicAnalyzer, civlConfig, libExecutorLoader,
				libEvaluatorLoader);
		this.libEvaluator = new LibcommEvaluator(name, evaluator, modelFactory,
				symbolicUtil, symbolicAnalyzer, civlConfig, libEvaluatorLoader);
	}

	/* ******************** Methods from LibraryExecutor ******************* */

	@Override
	protected Evaluation executeValue(State state, int pid, String process,
			CIVLSource source, String functionName, Expression[] arguments,
			SymbolicExpression[] argumentValues)
			throws UnsatisfiablePathConditionException {
		Evaluation callEval = null;

		switch (functionName) {
			// case "$comm_create":
			// callEval = this.executeCommCreate(state, pid, process, arguments,
			// argumentValues);
			// break;
			case "$comm_defined" :
				callEval = this.executeGcommOrCommDefined(state, pid, process,
						arguments, argumentValues);
				break;
			case "$comm_dequeue" :
				callEval = executeCommDequeue(state, pid, process, arguments,
						argumentValues);
				break;
			case "$comm_dequeue_work" :
				callEval = executeCommDequeue(state, pid, process, arguments,
						argumentValues);
				break;
			case "$comm_empty_in" :
				callEval = executeCommEmpty(state, pid, process, arguments,
						argumentValues, true);
				break;
			case "$comm_empty_out" :
				callEval = executeCommEmpty(state, pid, process, arguments,
						argumentValues, false);
				break;
			case "$comm_enqueue" :
			case "$comm_ienqueue" :
				callEval = executeCommEnqueue(state, pid, process, arguments,
						argumentValues);
				break;
			case "$comm_seek" :
				callEval = this.executeCommSeek(state, pid, process, arguments,
						argumentValues);
				break;
			case "$comm_probe" :
				callEval = this.executeCommProbe(state, pid, process, source,
						arguments, argumentValues);
				break;
			case "$comm_size" :
				callEval = this.executeCommSize(state, pid, process, arguments,
						argumentValues);
				break;
			// case "$gcomm_dup":
			// callEval = this.executeGcommDup(state, pid, process,
			// arguments,
			// argumentValues, source);
			// break;
			// case "$comm_destroy":
			// callEval = executeFree(state, pid, process, arguments,
			// argumentValues, source);
			// break;
			// case "$gcomm_destroy":
			// callEval = this.executeGcommDestroy(state, pid, process,
			// arguments,
			// argumentValues, source);
			// break;
			// case "$gcomm_create":
			// callEval = executeGcommCreate(state, pid, process, arguments,
			// argumentValues, source);
			// break;
			case "$gcomm_defined" :
				callEval = this.executeGcommOrCommDefined(state, pid, process,
						arguments, argumentValues);
				break;
		}
		return callEval;
	}

	/* ************************** Private Methods ************************** */

	// /**
	// * Creates a new local communicator object and returns a handle to it. The
	// * new communicator will be affiliated with the specified global
	// * communicator. This local communicator handle will be used as an
	// argument
	// * in most message-passing functions. The place must be in [0,size-1] and
	// * specifies the place in the global communication universe that will be
	// * occupied by the local communicator. The local communicator handle may
	// be
	// * used by more than one process, but all of those processes will be
	// viewed
	// * as occupying the same place. Only one call to $comm_create may occur
	// for
	// * each gcomm-place pair. The new object will be allocated in the given
	// * scope.
	// *
	// * @param state
	// * The current state.
	// * @param pid
	// * The ID of the process that the function call belongs to.
	// * @param lhs
	// * The left hand side expression of the call, which is to be
	// * assigned with the returned value of the function call. If NULL
	// * then no assignment happens.
	// * @param arguments
	// * The static representation of the arguments of the function
	// * call.
	// * @param argumentValues
	// * The dynamic representation of the arguments of the function
	// * call.
	// * @param source
	// * The source code element to be used for error report.
	// * @return The new state after executing the function call.
	// * @throws UnsatisfiablePathConditionException
	// */
	// private Evaluation executeCommCreate(State state, int pid, String
	// process,
	// Expression[] arguments, SymbolicExpression[] argumentValues)
	// throws UnsatisfiablePathConditionException {
	// SymbolicExpression scope = argumentValues[0];
	// Expression scopeExpression = arguments[0];
	// SymbolicExpression gcommHandle = argumentValues[1];
	// SymbolicExpression place = argumentValues[2];
	// SymbolicExpression gcomm;
	// SymbolicExpression comm;
	// SymbolicExpression procArray, initArray;
	// SymbolicExpression myProc;
	// LinkedList<SymbolicExpression> commComponents = new
	// LinkedList<SymbolicExpression>();
	// CIVLSource civlsource = arguments[0].getSource();
	// CIVLType commType = typeFactory
	// .systemType(ModelConfiguration.COMM_TYPE);
	// Evaluation eval;
	//
	// eval = this.evaluator.dereference(civlsource, state, process,
	// arguments[1], gcommHandle, false);
	// state = eval.state;
	// gcomm = eval.value;
	// procArray = universe.tupleRead(gcomm, oneObject);
	// initArray = universe.tupleRead(gcomm, twoObject);
	// myProc = modelFactory.processValue(pid);
	// // TODO report an error if the place has already been taken by other
	// // processes.
	// // assert universe.arrayRead(initArray, (NumericExpression)
	// // place).equals(
	// // falseValue);
	// // TODO report an error if the place exceeds the size of the
	// // communicator
	// procArray = universe.arrayWrite(procArray, (NumericExpression) place,
	// myProc);
	// initArray = universe.arrayWrite(initArray, (NumericExpression) place,
	// trueValue);
	// gcomm = universe.tupleWrite(gcomm, oneObject, procArray);
	// gcomm = universe.tupleWrite(gcomm, twoObject, initArray);
	// state = this.primaryExecutor.assign(civlsource, state, process,
	// gcommHandle, gcomm);
	// // builds comm
	// commComponents.add(place);
	// commComponents.add(gcommHandle);
	// comm = universe.tuple(
	// (SymbolicTupleType) commType.getDynamicType(universe),
	// commComponents);
	// return this.primaryExecutor.malloc(civlsource, state, pid, process,
	// scopeExpression, scope, commType, comm);
	// }

	/**
	 * Checks if a $gcomm or $comm object is defined, i.e., it doesn't point to
	 * the heap of an invalid scope, implementing the function
	 * $gcomm_defined($gcomm gcomm) / $comm_defined($comm comm).
	 * 
	 * @param state
	 *            The state where the checking happens.
	 * @param pid
	 *            The ID of the process that this computation belongs to.
	 * @param lhs
	 *            The left hand side expression of this function call.
	 * @param arguments
	 *            The static arguments of the function call.
	 * @param argumentValues
	 *            The symbolic values of the arguments of the function call
	 * @return The new state after executing the function call.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeGcommOrCommDefined(State state, int pid,
			String process, Expression[] arguments,
			SymbolicExpression[] argumentValues)
			throws UnsatisfiablePathConditionException {
		Pair<BooleanExpression, ResultType> result = symbolicAnalyzer
				.isDerefablePointer(state, argumentValues[0], arguments[0].getSource());

		if (result.right != ResultType.YES)
			state = this.errorLogger.logError(arguments[0].getSource(), state,
					pid, this.symbolicAnalyzer.stateInformation(state),
					result.left, result.right, CIVLProperty.DEREFERENCE,
					"attempt to access a memory location that can't be dereferenced: "
							+ symbolicAnalyzer.symbolicExpressionToString(
									arguments[0].getSource(), state,
									arguments[0].getExpressionType(),
									argumentValues[0]));
		return new Evaluation(state, result.left);
	}

	/**
	 * Finds the first matching message, removes it from the communicator, and
	 * returns the message
	 * 
	 * @param state
	 *            The current state.
	 * @param pid
	 *            The ID of the process that the function call belongs to.
	 * @param lhs
	 *            The left hand side expression of the call, which is to be
	 *            assigned with the returned value of the function call. If NULL
	 *            then no assignment happens.
	 * @param arguments
	 *            The static representation of the arguments of the function
	 *            call.
	 * @param argumentValues
	 *            The dynamic representation of the arguments of the function
	 *            call.
	 * @param source
	 *            The source code element to be used for error report.
	 * @return The new state after executing the function call.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeCommDequeue(State state, int pid, String process,
			Expression[] arguments, SymbolicExpression[] argumentValues)
			throws UnsatisfiablePathConditionException {
		CIVLSource civlsource = state.getProcessState(pid).getLocation()
				.getSource();
		SymbolicExpression message = null;
		SymbolicExpression commHandle = argumentValues[0];
		SymbolicExpression comm;
		SymbolicExpression gcommHandle;
		SymbolicExpression gcomm;
		NumericExpression source = (NumericExpression) argumentValues[1];
		NumericExpression tag = (NumericExpression) argumentValues[2];
		NumericExpression dest;
		SymbolicExpression buf;
		Evaluation eval;
		Pair<SymbolicExpression, SymbolicExpression> msg_buf;
		BooleanExpression commHandleIsNull = universe.equals(commHandle,
				this.symbolicUtil.nullPointer());

		if (commHandleIsNull.isTrue())
			return new Evaluation(state, null);
		assert commHandleIsNull
				.isFalse() : "expecting concrete boolean value for "
						+ commHandleIsNull;
		eval = evaluator.dereference(civlsource, state, pid, process,
				commHandle, false, true);
		state = eval.state;
		comm = eval.value;
		gcommHandle = universe.tupleRead(comm, oneObject);
		eval = evaluator.dereference(civlsource, state, pid, process,
				gcommHandle, false, true);
		state = eval.state;
		gcomm = eval.value;
		buf = universe.tupleRead(gcomm, threeObject);
		dest = (NumericExpression) universe.tupleRead(comm, zeroObject);
		msg_buf = getMsgOutofChannel(state, pid, process, buf, source, dest,
				tag, civlsource);
		message = msg_buf.left;
		buf = msg_buf.right;
		gcomm = universe.tupleWrite(gcomm, threeObject, buf);
		state = this.primaryExecutor.assign(civlsource, state, pid, gcommHandle,
				gcomm);
		return new Evaluation(state, message);
	}

	/**
	 * Adds the message to the appropriate message queue in the communication
	 * universe specified by the comm. The source of the message must equal the
	 * place of the comm.
	 * 
	 * @param state
	 *            The current state.
	 * @param pid
	 *            The ID of the process that the function call belongs to.
	 * @param arguments
	 *            The static representation of the arguments of the function
	 *            call.
	 * @param argumentValues
	 *            The dynamic representation of the arguments of the function
	 *            call.
	 * @param source
	 *            The source code element to be used for error report.
	 * @return The new state after executing the function call.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeCommEnqueue(State state, int pid, String process,
			Expression[] arguments, SymbolicExpression[] argumentValues)
			throws UnsatisfiablePathConditionException {
		CIVLSource civlsource = arguments[0].getSource();
		SymbolicExpression commHandle = argumentValues[0];
		SymbolicExpression newMessage = argumentValues[1];
		SymbolicExpression comm;
		SymbolicExpression gcommHandle;
		SymbolicExpression gcomm;
		SymbolicExpression buf;
		Evaluation eval;
		BooleanExpression commHandleIsNull = universe.equals(commHandle,
				this.symbolicUtil.nullPointer());

		if (commHandleIsNull.isTrue())
			return new Evaluation(state, null);
		assert commHandleIsNull
				.isFalse() : "expecting concrete boolean value for "
						+ commHandleIsNull;
		eval = evaluator.dereference(civlsource, state, pid, process,
				commHandle, false, true);
		state = eval.state;
		comm = eval.value;
		gcommHandle = universe.tupleRead(comm, oneObject);
		eval = evaluator.dereference(civlsource, state, pid, process,
				gcommHandle, false, true);
		state = eval.state;
		gcomm = eval.value;
		buf = universe.tupleRead(gcomm, threeObject);
		buf = putMsgInChannel(buf, newMessage, civlsource);
		// TODO checks if source is equal to the place of comm.
		gcomm = universe.tupleWrite(gcomm, threeObject, buf);
		state = this.primaryExecutor.assign(civlsource, state, pid, gcommHandle,
				gcomm);
		return new Evaluation(state, null);
	}

	/**
	 * Returns true iff a matching message exists in the communication universe
	 * specified by the comm. A message matches the arguments if the destination
	 * of the message is the place of the comm, and the sources and tags match.
	 * 
	 * @param state
	 *            The current state.
	 * @param pid
	 *            The ID of the process that the function call belongs to.
	 * @param lhs
	 *            The left hand side expression of the call, which is to be
	 *            assigned with the returned value of the function call. If NULL
	 *            then no assignment happens.
	 * @param arguments
	 *            The static representation of the arguments of the function
	 *            call.
	 * @param argumentValues
	 *            The dynamic representation of the arguments of the function
	 *            call.
	 * @param source
	 *            The source code element to be used for error report.
	 * @return The new state after executing the function call.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeCommProbe(State state, int pid, String process,
			CIVLSource civlsource, Expression[] arguments,
			SymbolicExpression[] argumentValues)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression commHandle = argumentValues[0], comm, gcomm,
				gcommHandle;
		NumericExpression source = (NumericExpression) argumentValues[1];
		NumericExpression tag = (NumericExpression) argumentValues[2];
		NumericExpression dest;
		int srcInt, destInt;
		boolean isFind = false, isWildcardTag = false;
		Evaluation eval;
		Reasoner reasoner;
		Number number;

		eval = evaluator.dereference(civlsource, state, pid, process,
				commHandle, false, true);
		state = eval.state;
		comm = eval.value;
		gcommHandle = universe.tupleRead(comm, oneObject);
		eval = evaluator.dereference(civlsource, state, pid, process,
				gcommHandle, false, true);
		state = eval.state;
		gcomm = eval.value;
		dest = (NumericExpression) universe.tupleRead(comm, zeroObject);
		reasoner = universe.reasoner(state.getPathCondition(universe));
		number = reasoner.extractNumber(source);
		if (number != null)
			srcInt = ((IntegerNumber) number).intValue();
		else
			throw new CIVLInternalException(
					"The second argument of $comm_probe must be concrete ",
					arguments[1].getSource());
		number = reasoner.extractNumber(dest);
		if (number != null)
			destInt = ((IntegerNumber) number).intValue();
		else
			throw new CIVLInternalException(
					"The place of a process in $comm must be concrete",
					arguments[2].getSource());
		if (reasoner.isValid(universe.equals(tag, universe.integer(-2))))
			isWildcardTag = true;
		isFind = !(libEvaluator.getAllPossibleSources(state, reasoner, gcomm,
				srcInt, destInt, tag, isWildcardTag, civlsource).isEmpty());
		return new Evaluation(state, universe.bool(isFind));
	}

	/**
	 * Finds the first matching message and returns it without modifying the
	 * communication universe. If no matching message exists, returns a message
	 * with source, dest, and tag all negative.
	 * 
	 * @param state
	 *            The current state.
	 * @param pid
	 *            The ID of the process that the function call belongs to.
	 * @param lhs
	 *            The left hand side expression of the call, which is to be
	 *            assigned with the returned value of the function call. If NULL
	 *            then no assignment happens.
	 * @param arguments
	 *            The static representation of the arguments of the function
	 *            call.
	 * @param argumentValues
	 *            The dynamic representation of the arguments of the function
	 *            call.
	 * @param source
	 *            The source code element to be used for error report.
	 * @return The new state after executing the function call.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeCommSeek(State state, int pid, String process,
			Expression[] arguments, SymbolicExpression[] argumentValues)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression commHandle = argumentValues[0];
		SymbolicExpression comm;
		SymbolicExpression gcommHandle;
		SymbolicExpression gcomm;
		NumericExpression source = (NumericExpression) argumentValues[1];
		NumericExpression dest;
		SymbolicExpression tag = argumentValues[2];
		SymbolicExpression message, messages, queue, queueLength;
		CIVLSource civlsource = state.getProcessState(pid).getLocation()
				.getSource();
		Evaluation eval;
		int msgIdx = -1;

		eval = evaluator.dereference(civlsource, state, pid, process,
				commHandle, false, true);
		state = eval.state;
		comm = eval.value;
		gcommHandle = universe.tupleRead(comm, oneObject);
		eval = evaluator.dereference(civlsource, state, pid, process,
				gcommHandle, false, true);
		state = eval.state;
		gcomm = eval.value;
		dest = (NumericExpression) universe.tupleRead(comm, zeroObject);
		queue = universe.arrayRead(universe.arrayRead(
				universe.tupleRead(gcomm, threeObject), source), dest);
		queueLength = universe.tupleRead(queue, zeroObject);
		messages = universe.tupleRead(queue, oneObject);
		msgIdx = this.getMatchedMsgIdx(state, pid, process, messages,
				queueLength, tag, civlsource);
		if (msgIdx == -1)
			message = this.getEmptyMessage(state);
		else
			message = universe.arrayRead(messages, universe.integer(msgIdx));
		return new Evaluation(state, message);
	}

	/**
	 * Returns the size (number of places) in the global communicator associated
	 * to the given comm.
	 * 
	 * @param state
	 *            The current state.
	 * @param pid
	 *            The ID of the process that the function call belongs to.
	 * @param lhs
	 *            The left hand side expression of the call, which is to be
	 *            assigned with the returned value of the function call. If NULL
	 *            then no assignment happens.
	 * @param arguments
	 *            The static representation of the arguments of the function
	 *            call.
	 * @param argumentValues
	 *            The dynamic representation of the arguments of the function
	 *            call.
	 * @param source
	 *            The source code element to be used for error report.
	 * @return The new state after executing the function call.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeCommSize(State state, int pid, String process,
			Expression[] arguments, SymbolicExpression[] argumentValues)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression commHandle = argumentValues[0];
		SymbolicExpression comm;
		SymbolicExpression gcommHandle;
		SymbolicExpression gcomm;
		SymbolicExpression nprocs;
		CIVLSource civlsource = arguments[0].getSource();
		Evaluation eval;

		eval = evaluator.dereference(civlsource, state, pid, process,
				commHandle, false, true);
		state = eval.state;
		comm = eval.value;
		gcommHandle = universe.tupleRead(comm, oneObject);
		eval = evaluator.dereference(civlsource, state, pid, process,
				gcommHandle, false, true);
		state = eval.state;
		gcomm = eval.value;
		nprocs = universe.tupleRead(gcomm, zeroObject);
		return new Evaluation(state, nprocs);
	}

	// /**
	// * Creates a new global communicator object and returns a handle to it.
	// The
	// * global communicator will have size communication places. The global
	// * communicator defines a communication "universe" and encompasses message
	// * buffers and all other components of the state associated to
	// * message-passing. The new object will be allocated in the given scope.
	// *
	// * @param state
	// * The current state.
	// * @param pid
	// * The ID of the process that the function call belongs to.
	// * @param lhs
	// * The left hand side expression of the call, which is to be
	// * assigned with the returned value of the function call. If NULL
	// * then no assignment happens.
	// * @param arguments
	// * The static representation of the arguments of the function
	// * call.
	// * @param argumentValues
	// * The dynamic representation of the arguments of the function
	// * call.
	// * @param source
	// * The source code element to be used for error report.
	// * @return The new state after executing the function call.
	// * @throws UnsatisfiablePathConditionException
	// */
	// private Evaluation executeGcommCreate(State state, int pid, String
	// process,
	// Expression[] arguments, SymbolicExpression[] argumentValues,
	// CIVLSource source) throws UnsatisfiablePathConditionException {
	// SymbolicExpression gcomm;
	// NumericExpression nprocs = (NumericExpression) argumentValues[1];
	// SymbolicExpression scope = argumentValues[0];
	// Expression scopeExpression = arguments[0];
	// SymbolicExpression procNegOne;
	// SymbolicExpression procArray;
	// SymbolicExpression initArray;
	// SymbolicExpression buf;
	// CIVLType queueType = model.queueType();
	// CIVLType gcommType = typeFactory
	// .systemType(ModelConfiguration.GCOMM_TYPE);
	// SymbolicType dynamicQueueType = queueType.getDynamicType(universe);
	// SymbolicType procType = typeFactory.processSymbolicType();
	// BooleanExpression context = state.getPathCondition();
	//
	// procNegOne = modelFactory.processValue(-1);
	// assert dynamicQueueType instanceof SymbolicTupleType;
	// procArray = symbolicUtil
	// .newArray(context, procType, nprocs, procNegOne);
	// initArray = symbolicUtil.newArray(context, universe.booleanType(),
	// nprocs, falseValue);
	// buf = newGcommBuffer(universe, model, symbolicUtil, context, nprocs);
	// gcomm = universe.tuple(
	// (SymbolicTupleType) gcommType.getDynamicType(universe),
	// Arrays.asList(nprocs, procArray, initArray, buf));
	// return primaryExecutor.malloc(source, state, pid, process,
	// scopeExpression, scope, gcommType, gcomm);
	// }

	/**
	 * Helper function for creating an empty buffer
	 * 
	 * @param universe
	 *            The Symbolic Universe
	 * @param model
	 *            The CIVL model of the program
	 * @param symbolicUtil
	 *            The SymbolicUtility
	 * @param context
	 *            The path condition of the current state
	 * @param nprocs
	 *            The NumericExpression of the number of processes
	 * @return
	 */
	public static SymbolicExpression newGcommBuffer(SymbolicUniverse universe,
			Model model, SymbolicUtility symbolicUtil,
			BooleanExpression context, NumericExpression nprocs) {
		SymbolicExpression queueLength = universe.integer(0);
		CIVLType messageType = model.mesageType();
		CIVLType queueType = model.queueType();
		SymbolicType dynamicQueueType = queueType.getDynamicType(universe);
		SymbolicType dynamicMessageType = messageType.getDynamicType(universe);
		SymbolicExpression emptyMessages = universe.array(dynamicMessageType,
				new LinkedList<SymbolicExpression>());
		SymbolicExpression emptyQueue = universe.tuple(
				(SymbolicTupleType) dynamicQueueType,
				Arrays.asList(queueLength, emptyMessages));
		SymbolicExpression bufRow = symbolicUtil.newArray(context,
				emptyQueue.type(), nprocs, emptyQueue);

		return symbolicUtil.newArray(context, bufRow.type(), nprocs, bufRow);
	}

	/**
	 * Frees the gcomm object and gives a CIVL sequence ($seq) of remaining
	 * messages if there are any of them through the output argument.Returns the
	 * number of remaining messages.
	 * 
	 * <code>Prototype: int $gcomm_destroy($gcomm, void * ptr)</code>
	 * 
	 * @param state
	 *            the current state
	 * @param pid
	 *            the PID of the process
	 * @param process
	 *            the identifier of the process
	 * @param lhs
	 *            the left hand side expression
	 * @param arguments
	 *            expressions of the arguments
	 * @param argumentValues
	 *            symbolic expressions of the arguments
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	@SuppressWarnings("unused")
	private Evaluation executeGcommDestroy(State state, int pid, String process,
			Expression[] arguments, SymbolicExpression[] argumentValues,
			CIVLSource source) throws UnsatisfiablePathConditionException {
		Expression nprocExpr;
		Expression gcommHandleExpr = arguments[0];
		Expression gcommExpr;
		SymbolicExpression gcommHandle;
		SymbolicExpression junkMsgPtr = argumentValues[1];
		SymbolicExpression gcomm;
		NumericExpression nprocs;
		SymbolicExpression buf;
		SymbolicExpression queues;
		int nprocs_int;
		Evaluation eval;
		// Collective all remaining messages
		LinkedList<SymbolicExpression> remainMsgs = new LinkedList<>();

		gcommHandle = argumentValues[0];
		eval = evaluator.dereference(arguments[0].getSource(), state, pid,
				process, gcommHandle, false, true);
		state = eval.state;
		gcomm = eval.value;
		nprocs = (NumericExpression) universe.tupleRead(gcomm, zeroObject);
		gcommExpr = modelFactory.dereferenceExpression(
				gcommHandleExpr.getSource(), gcommHandleExpr);
		nprocExpr = modelFactory.dotExpression(gcommExpr.getSource(), gcommExpr,
				0);
		nprocs_int = symbolicUtil.extractInt(nprocExpr.getSource(), nprocs);
		buf = universe.tupleRead(gcomm, threeObject);
		for (int i = 0; i < nprocs_int; i++) {
			Reasoner reasoner = universe
					.reasoner(state.getPathCondition(universe));

			queues = universe.arrayRead(buf, universe.integer(i));
			for (int j = 0; j < nprocs_int; j++) {
				SymbolicExpression queue;
				NumericExpression queueLength;
				IntegerNumber concQLength;

				queue = universe.arrayRead(queues, universe.integer(j));
				queueLength = (NumericExpression) universe.tupleRead(queue,
						zeroObject);
				concQLength = ((IntegerNumber) reasoner
						.extractNumber(queueLength));
				if (concQLength == null) {
					throw new CIVLInternalException(
							"The length of a message queue in a CIVL-C communicator is not concrete",
							source);
				} else {
					SymbolicExpression msgArray = universe.tupleRead(queue,
							oneObject);

					for (int k = 0; k < concQLength.intValue(); k++)
						remainMsgs.add(universe.arrayRead(msgArray,
								universe.integer(k)));
				}
			}
		}
		// If there is any remaining messages, assign to the given pointer (a
		// pointer to a civl-c $seq)
		if (!remainMsgs.isEmpty() && !junkMsgPtr.isNull()) {
			SymbolicType msgType = model.mesageType().getDynamicType(universe);
			SymbolicExpression junkMsgArray = universe.array(msgType,
					remainMsgs);

			state = primaryExecutor.assign(arguments[1].getSource(), state, pid,
					junkMsgPtr, junkMsgArray);
		}
		// Return the number of remaining messages (junk messages):
		state = this.executeFree(state, pid, process, arguments, argumentValues,
				source).state;
		return new Evaluation(state, universe.integer(remainMsgs.size()));
	}

	/**
	 * Read matched message index from the given messages array. Here "matched
	 * message" means if the given tag is wild card tag, then read the first
	 * message inside the messages array, otherwise the tag should be a specific
	 * tag then read the first message has the same tag inside the messages
	 * array.
	 * 
	 * Other cases like failure of finding a matched message or the tag is
	 * neither a wild card tag nor a valid specific tag will be an execution
	 * exception.
	 * 
	 * @param state
	 *            The current state which emanating the transition being
	 *            executed right now
	 * @param pid
	 *            The pid of the process
	 * @param messagesArray
	 *            The given messages array
	 * @param tag
	 *            The given message tag
	 * @param civlsource
	 * @return The index of a matched message in the given array
	 * @throws UnsatisfiablePathConditionException
	 */
	private int getMatchedMsgIdx(State state, int pid, String process,
			SymbolicExpression messagesArray, SymbolicExpression queueLength,
			SymbolicExpression tag, CIVLSource civlsource)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression message = null;
		NumericExpression numericQueueLength = (NumericExpression) queueLength;
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		BooleanExpression isAnyTag = universe.equals(universe.integer(-2), tag);
		BooleanExpression isSpecTag = universe.lessThanEquals(zero,
				(NumericExpression) tag);
		int msgIndex = -1;

		// specific tag
		if (reasoner.isValid(isSpecTag)) {
			NumericExpression iter = zero;
			BooleanExpression iterQueueLengthClaim = universe.lessThan(iter,
					(NumericExpression) queueLength);

			while (reasoner.isValid(iterQueueLengthClaim)) {
				BooleanExpression isTagMatched;

				message = universe.arrayRead(messagesArray, iter);
				isTagMatched = universe.equals(
						universe.tupleRead(message, universe.intObject(2)),
						tag);
				if (reasoner.isValid(isTagMatched)) {
					msgIndex = symbolicUtil.extractInt(null, iter);
					break;
				}
				iter = universe.add(iter, one);
				iterQueueLengthClaim = universe.lessThan(iter,
						numericQueueLength);
			}
		}
		// wild card tag
		else if (reasoner.isValid(isAnyTag)) {
			BooleanExpression queueGTzeroClaim = universe.lessThan(zero,
					numericQueueLength);

			if (reasoner.isValid(queueGTzeroClaim))
				msgIndex = 0;

		}
		// Exception
		else if (civlConfig.isPropertyToggled(CIVLProperty.COMMUNICATION)) {
			// tag != -2 && tag < 0
			errorLogger.logSimpleError(civlsource, state, pid, process,
					symbolicAnalyzer.stateToString(state),
					CIVLProperty.COMMUNICATION, "Illegal message tag:" + tag);
			throw new UnsatisfiablePathConditionException();
		}
		return msgIndex;
	}

	/**
	 * Make up an empty message. An empty message has only one canonical form.
	 * 
	 * @param state
	 *            The current state.
	 * @return
	 */
	private SymbolicExpression getEmptyMessage(State state) {
		SymbolicExpression message;
		CIVLType messageType = model.mesageType();
		CIVLBundleType bundleType = typeFactory.bundleType();
		LinkedList<SymbolicExpression> emptyMessageComponents = new LinkedList<SymbolicExpression>();
		StringObject name;
		SymbolicExpression bundle;

		name = universe.stringObject("X_s" + -1 + "v" + -1);
		bundle = universe.symbolicConstant(name,
				bundleType.getDynamicType(universe));
		emptyMessageComponents.add(universe.integer(-1));
		emptyMessageComponents.add(universe.integer(-1));
		emptyMessageComponents.add(universe.integer(-1));
		emptyMessageComponents.add(bundle);
		emptyMessageComponents.add(universe.integer(-1));
		message = this.universe.tuple(
				(SymbolicTupleType) messageType.getDynamicType(universe),
				emptyMessageComponents);
		return message;
	}

	/**
	 * Public helper function of inserting a message into the destination
	 * position of a given message buffer. The function may be called by other
	 * library (e.g. MPI Library).
	 * 
	 * @param messageBuffer
	 *            The message buffer which is a 2-d array of message queues.
	 * @param source
	 *            The place where the message comes from
	 * @param dest
	 *            The destination where the message goes to
	 * @param message
	 *            The message delivered
	 * @param civlsource
	 *            The CIVLSource of this action
	 * @return
	 */
	public SymbolicExpression putMsgInChannel(SymbolicExpression messageBuffer,
			SymbolicExpression message, CIVLSource civlsource) {
		SymbolicExpression buf = messageBuffer;
		SymbolicExpression bufRow, queue, messages;
		NumericExpression queueLength, source, dest;
		int int_queueLength;

		source = (NumericExpression) universe.tupleRead(message, zeroObject);
		dest = (NumericExpression) universe.tupleRead(message, oneObject);
		bufRow = universe.arrayRead(buf, (NumericExpression) source);
		queue = universe.arrayRead(bufRow, (NumericExpression) dest);
		queueLength = (NumericExpression) universe.tupleRead(queue, zeroObject);
		messages = universe.tupleRead(queue, oneObject);
		messages = universe.append(messages, message);
		int_queueLength = symbolicUtil.extractInt(civlsource, queueLength);
		int_queueLength++;
		queueLength = universe.integer(int_queueLength);
		queue = universe.tupleWrite(queue, zeroObject, queueLength);
		queue = universe.tupleWrite(queue, oneObject, messages);
		bufRow = universe.arrayWrite(bufRow, (NumericExpression) dest, queue);
		buf = universe.arrayWrite(buf, (NumericExpression) source, bufRow);
		return buf;
	}

	/**
	 * Public helper method for getting a message out of the given message
	 * buffer. Pre-condition: The message must already exist in the channel.
	 * 
	 * @param state
	 *            The current state
	 * @param pid
	 *            The PID of the process
	 * @param process
	 *            The String identifier of the process
	 * @param messageBuffer
	 *            The message buffer
	 * @param source
	 *            The source of the message
	 * @param dest
	 *            The destination of the message
	 * @param tag
	 *            The message tag
	 * @param civlsource
	 * @return The received message and the updated message buffer.
	 * @throws UnsatisfiablePathConditionException
	 */
	public Pair<SymbolicExpression, SymbolicExpression> getMsgOutofChannel(
			State state, int pid, String process,
			SymbolicExpression messageBuffer, NumericExpression source,
			NumericExpression dest, NumericExpression tag,
			CIVLSource civlsource) throws UnsatisfiablePathConditionException {
		SymbolicExpression bufRow, queue, messages;
		SymbolicExpression message, buf;
		NumericExpression queueLength;
		int msgIdx;

		buf = messageBuffer;
		bufRow = universe.arrayRead(messageBuffer, source);
		queue = universe.arrayRead(bufRow, dest);
		queueLength = (NumericExpression) universe.tupleRead(queue, zeroObject);
		messages = universe.tupleRead(queue, oneObject);
		msgIdx = this.getMatchedMsgIdx(state, pid, process, messages,
				queueLength, tag, civlsource);
		if (msgIdx == -1
				&& civlConfig.isPropertyToggled(CIVLProperty.COMMUNICATION)) {
			state = errorLogger.logError(civlsource, state, pid,
					symbolicAnalyzer.stateInformation(state),
					universe.trueExpression(), ResultType.NO,
					CIVLProperty.COMMUNICATION,
					"There is no matched message [source:" + source
							+ ", destination:" + dest + ", tag:" + tag
							+ " ] in the message buffer.");
		}
		message = universe.arrayRead(messages, universe.integer(msgIdx));
		messages = universe.removeElementAt(messages, msgIdx);
		queueLength = universe.subtract(queueLength, one);
		queue = universe.tupleWrite(queue, zeroObject, queueLength);
		queue = universe.tupleWrite(queue, oneObject, messages);
		bufRow = universe.arrayWrite(bufRow, dest, queue);
		buf = universe.arrayWrite(buf, source, bufRow);
		return new Pair<>(message, buf);
	}

	/**
	 * This method represents executions of two system functions:
	 * <ul>
	 * <li>$comm_empty_in:</li> A state pure function which returns a predicate
	 * that all message channels TO the calling process are empty.
	 * <li>$comm_empty_out:</li> A state pure function which returns a predicate
	 * that all message channels FROM the calling process are empty.
	 * </ul>
	 * 
	 * @param state
	 *            The program state when calling either of the two functions
	 * @param pid
	 *            The PID of the calling process
	 * @param process
	 *            The String identifier of the calling process
	 * @param arguments
	 *            An array of {@link Expression}s which represents function
	 *            parameters
	 * @param values
	 *            An array of {@link SymbolicExpression}s which represents
	 *            values of function parameters
	 * @param isCommEmptyIn
	 *            True iff the execution is for $comm_empty_in, false iff for
	 *            $comm_empty_out
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeCommEmpty(State state, int pid, String process,
			Expression[] arguments, SymbolicExpression values[],
			boolean isCommEmptyIn) throws UnsatisfiablePathConditionException {
		SymbolicExpression commHandle = values[0];
		SymbolicExpression gcommHandle, messageBuffer, queue;
		NumericExpression place, nprocs;
		Evaluation eval;

		eval = evaluator.dereference(arguments[0].getSource(), state, pid,
				process, commHandle, false, true);
		state = eval.state;
		gcommHandle = universe.tupleRead(eval.value, oneObject);
		place = (NumericExpression) universe.tupleRead(eval.value, zeroObject);
		eval = evaluator.dereference(arguments[0].getSource(), state, pid,
				process, gcommHandle, false, true);
		state = eval.state;
		messageBuffer = universe.tupleRead(eval.value, threeObject);
		nprocs = (NumericExpression) universe.tupleRead(eval.value, zeroObject);

		BooleanExpression pred;
		NumericSymbolicConstant i = (NumericSymbolicConstant) universe
				.symbolicConstant(universe.stringObject("i"),
						universe.integerType());
		NumericExpression queueLength;

		if (isCommEmptyIn)
			queue = universe.arrayRead(universe.arrayRead(messageBuffer, i),
					place);
		else
			queue = universe.arrayRead(universe.arrayRead(messageBuffer, place),
					i);
		queueLength = (NumericExpression) universe.tupleRead(queue, zeroObject);
		pred = universe.forallInt(i, zero, nprocs,
				universe.equals(queueLength, zero));
		eval.value = pred;
		return eval;
	}
}
