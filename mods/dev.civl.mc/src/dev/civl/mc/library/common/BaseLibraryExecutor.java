package dev.civl.mc.library.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.model.IF.CIVLProperty;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.LHSExpression;
import dev.civl.mc.model.IF.location.Location;
import dev.civl.mc.model.IF.statement.CallOrSpawnStatement;
import dev.civl.mc.semantics.IF.*;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.StateFactory;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.mc.util.IF.Pair;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.ArrayElementReference;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NTReferenceExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.TupleComponentReference;
import dev.civl.sarl.IF.number.IntegerNumber;

/**
 * This class provides the common data and operations of library executors.
 * 
 * @author Manchun Zheng (zmanchun)
 * 
 */
public abstract class BaseLibraryExecutor extends LibraryComponent implements LibraryExecutor {

	/* ************************** Instance Fields ************************** */

	/**
	 * The primary executor of the system.
	 */
	protected Executor primaryExecutor;

	/**
	 * The state factory for state-related computation.
	 */
	protected StateFactory stateFactory;

	/**
	 * The set of characters that are used to construct a number in a format string.
	 */
	protected Set<Character> numbers;

	protected LibraryExecutorLoader libExecutorLoader;

	/**
	 * A reference to a {@link SymbolicUniverse}.
	 */
	protected SymbolicUniverse universe;

	/* **************************** Constructors *************************** */

	/**
	 * Creates a new instance of a library executor.
	 * 
	 * @param primaryExecutor  The executor for normal CIVL execution.
	 * @param output           The output stream to be used in the enabler.
	 * @param enablePrintf     If printing is enabled for the printf function.
	 * @param modelFactory     The model factory of the system.
	 * @param symbolicUtil     The symbolic utility used in the system.
	 * @param symbolicAnalyzer The symbolic analyzer used in the system.
	 */
	public BaseLibraryExecutor(String name, Executor primaryExecutor, ModelFactory modelFactory,
			SymbolicUtility symbolicUtil, SymbolicAnalyzer symbolicAnalyzer, CIVLConfiguration civlConfig,
			LibraryExecutorLoader libExecutorLoader, LibraryEvaluatorLoader libEvaluatorLoader) {
		super(name, primaryExecutor.evaluator().universe(), symbolicUtil, symbolicAnalyzer, civlConfig,
				libEvaluatorLoader, modelFactory, primaryExecutor.errorLogger(), primaryExecutor.evaluator());
		this.primaryExecutor = primaryExecutor;
		this.stateFactory = evaluator.stateFactory();
		this.errorLogger = primaryExecutor.errorLogger();
		this.libExecutorLoader = libExecutorLoader;
		this.universe = evaluator.universe();
		numbers = new HashSet<Character>(10);
		for (int i = 0; i < 10; i++) {
			numbers.add(Character.forDigit(i, 10));
		}
	}

	/* ************************* Protected Methods ************************* */

	/**
	 * Executes the function call "$free(*void)": removes from the heap the object
	 * referred to by the given pointer.
	 * 
	 * @param state          The current state.
	 * @param pid            The ID of the process that the function call belongs
	 *                       to.
	 * @param arguments      The static representation of the arguments of the
	 *                       function call.
	 * @param argumentValues The dynamic representation of the arguments of the
	 *                       function call.
	 * @param source         The source code element to be used for error report.
	 * @return The new state after executing the function call.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation executeFree(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression firstElementPointer = argumentValues[0];
		Pair<BooleanExpression, ResultType> checkPointer = symbolicAnalyzer.isDefinedPointer(state, firstElementPointer,
				source);

		if (civlConfig.isPropertyToggled(CIVLProperty.MEMORY_MANAGE) && checkPointer.right != ResultType.YES) {
			state = this.errorLogger.logError(source, state, pid, symbolicAnalyzer.stateInformation(state),
					checkPointer.left, checkPointer.right, CIVLProperty.MEMORY_MANAGE,
					"attempt to deallocate memory space through an undefined pointer");
			// dont report unsatisfiable path condition exception
		} else if (this.symbolicUtil.isNullPointer(firstElementPointer)) {
			// does nothing for null pointer.
		} else if (civlConfig.isPropertyToggled(CIVLProperty.MEMORY_MANAGE)
				&& (!this.symbolicUtil.isPointerToHeap(firstElementPointer)
						|| !this.symbolicUtil.isMallocPointer(source, firstElementPointer))) {
			this.errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
					CIVLProperty.MEMORY_MANAGE,
					"the argument of free "
							+ symbolicAnalyzer.symbolicExpressionToString(source, state,
									arguments[0].getExpressionType(), firstElementPointer)
							+ " is not a pointer returned by a memory " + "management method");
		} else {
			Evaluation eval;
			SymbolicExpression heapObject = null;
			Pair<BooleanExpression, ResultType> checkDerefable = symbolicAnalyzer.isDerefablePointer(state,
					firstElementPointer, source);

			if (checkDerefable.right == ResultType.YES) {
				eval = evaluator.dereference(source, state, pid, process, firstElementPointer, false, true);
				heapObject = eval.value;
				state = eval.state;
			}
			if (civlConfig.isPropertyToggled(CIVLProperty.MEMORY_MANAGE) && heapObject != null && heapObject.isNull()) {
				// the heap object has been deallocated
				this.errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
						CIVLProperty.MEMORY_MANAGE,
						"attempt to deallocate an object that has been deallocated previously");
			} else {
				Pair<Integer, Integer> indexes;
				boolean saveWrite = state.isMonitoringWrites(pid);

				indexes = getMallocIndex(firstElementPointer);
				if (saveWrite) {
					SymbolicExpression pointer2memoryBlk = symbolicUtil.parentPointer(firstElementPointer);

					eval = evaluator.memEvaluator().pointer2memValue(state, pid, pointer2memoryBlk, source);
					state = eval.state;
					state = stateFactory.addReadWriteRecords(state, pid, eval.value, false);
				}
				state = stateFactory.deallocate(state, firstElementPointer,
						symbolicUtil.getScopeValue(firstElementPointer), indexes.left, indexes.right);
			}
		}
		return new Evaluation(state, null);
	}

	/**
	 * A helper function for reporting runtime assertion failure. The helper
	 * function is aiming to be re-used by both execution implementations of
	 * $assert() and $assert_equal();
	 * 
	 * @author ziqing luo
	 * @param state          The current state
	 * @param pid            The PID of the process
	 * @param process        The string identifier of the process
	 * @param resultType     The {@link ResultType} of the failure assertion
	 * @param arguments      The expressions of the arguments
	 * @param argumentValues The symbolic expressions of the arguments
	 * @param source         The CIVL source of the assertion statement
	 * @param claim          The boolean expression of the value of the assertion
	 *                       claim
	 * @param msgOffset      the start index in arguments list of the assertion
	 *                       failure messages.
	 * @return the new state after reporting the assertion failure
	 * @throws UnsatisfiablePathConditionException
	 */
	protected State reportAssertionFailure(State state, int pid, String process, ResultType resultType, String message,
			Expression[] arguments, SymbolicExpression[] argumentValues, CIVLSource source, BooleanExpression claim,
			int msgOffset) throws UnsatisfiablePathConditionException {
		assert resultType != ResultType.YES;
		if (arguments.length > msgOffset) {
			Expression[] pArguments = Arrays.copyOfRange(arguments, msgOffset, arguments.length);
			SymbolicExpression[] pArgumentValues = Arrays.copyOfRange(argumentValues, msgOffset, argumentValues.length);

			state = this.primaryExecutor.execute_printf(source, state, pid, process, pArguments, pArgumentValues,
					false).state;
			civlConfig.out().println();
		}
		state = errorLogger.logError(source, state, pid, this.symbolicAnalyzer.stateInformation(state), claim,
				resultType, CIVLProperty.ASSERTION_VIOLATION, message);
		return state;
	}

	/**
	 * $exit terminates the calling process.
	 * 
	 * @param state The current state.
	 * @param pid   The process ID of the process to be terminated.
	 * @return The state resulting from removing the specified process.
	 */
	protected Evaluation executeExit(State state, int pid) {
		int atomicPID = stateFactory.processInAtomic(state);

		if (atomicPID == pid) {
			state = stateFactory.releaseAtomicLock(state);
		}
		return new Evaluation(stateFactory.terminateProcess(state, pid), null);
	}

	@Override
	public Evaluation execute(State state, int pid, CallOrSpawnStatement call, String functionName)
			throws UnsatisfiablePathConditionException {
		Evaluation eval;
		LHSExpression lhs = call.lhs();
		Location target = call.target();
		Expression[] arguments;
		SymbolicExpression[] argumentValues;
		int numArgs;
		String process = state.getProcessState(pid).name();

		numArgs = call.arguments().size();
		arguments = new Expression[numArgs];
		argumentValues = new SymbolicExpression[numArgs];
		for (int i = 0; i < numArgs; i++) {
			arguments[i] = call.arguments().get(i);
			eval = evaluator.evaluate(state, pid, arguments[i]);
			argumentValues[i] = eval.value;
			state = eval.state;
		}
		eval = this.executeValue(state, pid, process, call.getSource(), functionName, arguments, argumentValues);
		state = eval.state;
		if (lhs != null && eval.value != null)
			state = this.primaryExecutor.assign(state, pid, process, lhs, eval.value, call.isInitializer());
		if (target != null && !state.getProcessState(pid).hasEmptyStack())
			state = this.stateFactory.setLocation(state, pid, target);
		eval.state = state;
		return eval;
	}

	@Override
	public void setEvaluator(Evaluator primaryEvaluator) {
		super.evaluator = primaryEvaluator;
	}

	abstract protected Evaluation executeValue(State state, int pid, String process, CIVLSource source,
			String functionName, Expression[] arguments, SymbolicExpression[] argumentValues)
			throws UnsatisfiablePathConditionException;

	/* ************************** Private Methods ************************** */

	/**
	 * Obtains the field ID in the heap type via a heap-object pointer.
	 * 
	 * @param pointer The heap-object pointer.
	 * @return The field ID in the heap type of the heap-object that the given
	 *         pointer refers to.
	 */
	private Pair<Integer, Integer> getMallocIndex(SymbolicExpression pointer) {
		// ref points to element 0 of an array:
		NTReferenceExpression ref = (NTReferenceExpression) symbolicUtil.getSymRef(pointer);
		// objectPointer points to array:
		ArrayElementReference objectPointer = (ArrayElementReference) ref.getParent();
		int mallocIndex = ((IntegerNumber) universe.extractNumber(objectPointer.getIndex())).intValue();
		// fieldPointer points to the field:
		TupleComponentReference fieldPointer = (TupleComponentReference) objectPointer.getParent();
		int mallocId = fieldPointer.getIndex().getInt();

		return new Pair<>(mallocId, mallocIndex);
	}
}
