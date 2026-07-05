package dev.civl.mc.library.pthread;

import java.util.Arrays;

import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.library.common.BaseLibraryExecutor;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.model.IF.ModelConfiguration;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.type.CIVLArrayType;
import dev.civl.mc.model.IF.type.CIVLStructOrUnionType;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.semantics.IF.Evaluation;
import dev.civl.mc.semantics.IF.Executor;
import dev.civl.mc.semantics.IF.LibraryEvaluatorLoader;
import dev.civl.mc.semantics.IF.LibraryExecutor;
import dev.civl.mc.semantics.IF.LibraryExecutorLoader;
import dev.civl.mc.semantics.IF.SymbolicAnalyzer;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.type.SymbolicArrayType;
import dev.civl.sarl.IF.type.SymbolicTupleType;

public class LibpthreadExecutor extends BaseLibraryExecutor
		implements
			LibraryExecutor {

	private CIVLType gpoolType;
	private CIVLType poolType;
	private SymbolicTupleType poolSymbolicType;
	private CIVLArrayType pthreadArrayType;
	@SuppressWarnings("unused")
	private SymbolicArrayType pthreadArraySymbolicType;

	public LibpthreadExecutor(String name, Executor primaryExecutor,
			ModelFactory modelFactory, SymbolicUtility symbolicUtil,
			SymbolicAnalyzer symbolicAnalyzer, CIVLConfiguration civlConfig,
			LibraryExecutorLoader libExecutorLoader,
			LibraryEvaluatorLoader libEvaluatorLoader) {
		super(name, primaryExecutor, modelFactory, symbolicUtil,
				symbolicAnalyzer, civlConfig, libExecutorLoader,
				libEvaluatorLoader);
		this.gpoolType = this.typeFactory
				.systemType(ModelConfiguration.PTHREAD_GPOOL);
		this.poolType = this.typeFactory
				.systemType(ModelConfiguration.PTHREAD_POOL);
		this.poolSymbolicType = (SymbolicTupleType) this.poolType
				.getDynamicType(universe);
		pthreadArrayType = (CIVLArrayType) ((CIVLStructOrUnionType) this.gpoolType)
				.getField(0).type();
		pthreadArraySymbolicType = (SymbolicArrayType) pthreadArrayType
				.getDynamicType(universe);
	}

	@Override
	protected Evaluation executeValue(State state, int pid, String process,
			CIVLSource source, String functionName, Expression[] arguments,
			SymbolicExpression[] argumentValues)
			throws UnsatisfiablePathConditionException {
		Evaluation callEval = null;

		switch (functionName) {
			case "$pthread_gpool_thread" :
				callEval = execute_pthread_gpool_thread(state, pid, process,
						arguments, argumentValues, source);
				break;
			case "$pthread_gpool_size" :
				callEval = execute_pthread_gpool_size(state, pid, process,
						arguments, argumentValues, source);
				break;
			case "$pthread_pool_create" :
				callEval = execute_pthread_pool_create(state, pid, process,
						arguments, argumentValues, source);
				break;
			case "$pthread_gpool_add" :
				callEval = execute_pthread_gpool_add(state, pid, process,
						arguments, argumentValues, source);
				break;
			case "$pthread_gpool_join" :
				callEval = execute_pthread_gpool_join(state, pid, process,
						arguments, argumentValues, source);
				break;
			// case "$pthread_pool_exit":
			// state = execute_pthread_pool_exit(state, pid, process, arguments,
			// argumentValues, source);
			// break;
			case "$pthread_pool_get_terminated" :
				callEval = execute_pthread_pool_get_terminated(state, pid,
						process, arguments, argumentValues, source);
				break;
			case "$pthread_pool_get_id" :
				callEval = this.execute_pthread_pool_get_id(state, pid, process,
						arguments, argumentValues, source);
				break;
			case "_add_thread" :
				callEval = execute_add_thread(state, pid, process, arguments,
						argumentValues, source);
				break;
			case "$pthread_pool_terminates" :
				callEval = execute_pthread_pool_terminates(state, pid, process,
						arguments, argumentValues, source);
				break;
			case "$pthread_pool_is_terminated" :
				callEval = execute_pthread_pool_is_terminated(state, pid,
						process, arguments, argumentValues, source);
				break;
			case "$pthread_pool_thread" :
				callEval = execute_pthread_pool_thread(state, pid, process,
						arguments, argumentValues, source);
				break;
			case "$pthread_exit" :
				callEval = execute_pthread_exit(state, pid, process, arguments,
						argumentValues, source);
				break;
			default :
				throw new CIVLUnimplementedFeatureException(
						"execution of function " + functionName
								+ " in pthread library",
						source);
		}
		return callEval;
	}

	/**
	 * <pre>
	 * void $pthread_exit(void *value_ptr, $pthread_pool_t $pthread_pool){
	 *   $pthread_pool_terminates($pthread_pool, value_ptr); 
	 *   $free($pthread_pool);
	 *   $exit(); 
	 * }
	 * </pre>
	 * 
	 * @param state
	 * @param pid
	 * @param process
	 * @param arguments
	 * @param argumentValues
	 * @param source
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation execute_pthread_exit(State state, int pid,
			String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		Expression[] terminatesExprs = new Expression[2];
		SymbolicExpression[] terminatesExprValues = new SymbolicExpression[2];

		terminatesExprs[0] = arguments[1];
		terminatesExprs[1] = arguments[0];
		terminatesExprValues[0] = argumentValues[1];
		terminatesExprValues[1] = argumentValues[0];
		state = this.execute_pthread_pool_terminates(state, pid, process,
				terminatesExprs, terminatesExprValues, source).state;
		state = this.executeFree(state, pid, process, terminatesExprs,
				terminatesExprValues, source).state;
		return this.executeExit(state, pid);
	}

	private Evaluation execute_pthread_gpool_join(State state, int pid,
			String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression gpool = argumentValues[0];
		Evaluation eval;
		SymbolicExpression gpoolObj, threads;
		int numThreads;

		eval = this.evaluator.dereference(source, state, pid, process, gpool,
				false, true);
		gpoolObj = eval.value;
		state = eval.state;
		threads = this.universe.tupleRead(gpoolObj, zeroObject);
		numThreads = this.symbolicUtil.extractInt(source,
				universe.length(threads));
		for (int i = 0; i < numThreads; i++) {
			SymbolicExpression threadObj = universe.arrayRead(threads,
					universe.integer(i));
			SymbolicExpression pidValue;
			int pidInt;

			pidValue = universe.tupleRead(threadObj, this.zeroObject);
			pidInt = modelFactory.getProcessId(pidValue);
			if (pidInt != pid && !modelFactory.isProcessIdNull(pidInt)
					&& modelFactory.isPocessIdDefined(pidInt))
				state = stateFactory.removeProcess(state, pidInt);
		}
		return this.executeFree(state, pid, process, arguments, argumentValues,
				source);
	}

	private Evaluation execute_pthread_pool_thread(State state, int pid,
			String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression pool = argumentValues[0], poolObj;
		Evaluation eval;
		SymbolicExpression threadPointer;

		eval = this.evaluator.dereference(source, state, pid, process, pool,
				false, true);
		poolObj = eval.value;
		state = eval.state;
		threadPointer = universe.tupleRead(poolObj, this.twoObject);
		eval = this.evaluator.dereference(source, state, pid, process,
				threadPointer, false, true);
		state = eval.state;
		return new Evaluation(eval.state, eval.value);
	}

	/**
	 * _Bool $pthread_pool_is_terminated($pthread_pool_t pool, $proc pid);
	 * 
	 * @param state
	 * @param pid
	 * @param process
	 * @param lhs
	 * @param arguments
	 * @param argumentValues
	 * @param source
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation execute_pthread_pool_is_terminated(State state, int pid,
			String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression pool = argumentValues[0], proc = argumentValues[1];
		SymbolicExpression poolObject, gpool, gpoolObject, threads;
		NumericExpression numThreads;
		int numThreads_int;
		Evaluation eval;
		SymbolicExpression result = trueValue;

		if (modelFactory.isProcNull(proc)) {
			result = universe.falseExpression();
		} else {
			eval = evaluator.dereference(source, state, pid, process, pool,
					false, true);
			poolObject = eval.value;
			state = eval.state;
			gpool = universe.tupleRead(poolObject, zeroObject);
			eval = evaluator.dereference(source, state, pid, process, gpool,
					false, true);
			state = eval.state;
			gpoolObject = eval.value;
			threads = universe.tupleRead(gpoolObject, zeroObject);
			numThreads = universe.length(threads);
			numThreads_int = symbolicUtil.extractInt(source, numThreads);
			for (int i = 0; i < numThreads_int; i++) {
				SymbolicExpression threadPointer = universe.arrayRead(threads,
						universe.integer(i));
				SymbolicExpression threadObj, threadId;

				eval = evaluator.dereference(source, state, pid, process,
						threadPointer, false, true);
				threadObj = eval.value;
				state = eval.state;
				threadId = universe.tupleRead(threadObj, zeroObject);
				if (universe.equals(threadId, proc).isTrue()) {
					result = universe.tupleRead(threadObj, twoObject);
					break;
				}
			}
		}
		return new Evaluation(state, result);
	}

	private Evaluation execute_pthread_gpool_thread(State state, int pid,
			String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression gpool = argumentValues[0];
		NumericExpression index = (NumericExpression) argumentValues[1];
		SymbolicExpression gpoolObject, threadPointer, threadObj, result;
		Evaluation eval;

		eval = evaluator.dereference(source, state, pid, process, gpool, false,
				true);
		gpoolObject = eval.value;
		state = eval.state;
		threadPointer = universe
				.arrayRead(universe.tupleRead(gpoolObject, zeroObject), index);
		if (symbolicAnalyzer.isDerefablePointer(state,
				threadPointer, source).right != ResultType.YES)
			result = modelFactory.nullProcessValue();
		else {
			eval = this.evaluator.dereference(source, state, pid, process,
					threadPointer, false, true);
			threadObj = eval.value;
			state = eval.state;
			result = universe.tupleRead(threadObj, zeroObject);
		}
		return new Evaluation(state, result);
	}

	/**
	 * int $pthread_gpool_size($pthread_gpool_t gpool);
	 * 
	 * @param state
	 * @param pid
	 * @param process
	 * @param lhs
	 * @param arguments
	 * @param argumentValues
	 * @param source
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation execute_pthread_gpool_size(State state, int pid,
			String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression gpool = argumentValues[0];
		SymbolicExpression gpoolObject, result;
		Evaluation eval;

		if (symbolicAnalyzer.isDerefablePointer(state,
				gpool, source).right == ResultType.YES) {
			eval = evaluator.dereference(source, state, pid, process, gpool,
					false, true);
			gpoolObject = eval.value;
			state = eval.state;
			result = universe
					.length(universe.tupleRead(gpoolObject, zeroObject));
		} else
			result = zero;
		return new Evaluation(state, result);
	}

	/**
	 * void $pthread_pool_terminates($pthread_pool_t pool);
	 * 
	 * 
	 * @param state
	 * @param pid
	 * @param process
	 * @param arguments
	 * @param argumentValues
	 * @param source
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation execute_pthread_pool_terminates(State state, int pid,
			String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression pool = argumentValues[0], poolObj;
		Evaluation eval;
		SymbolicExpression threadPointer;
		SymbolicExpression threadTermPointer, threadExitValuePointer;

		eval = evaluator.dereference(source, state, pid, process, pool, false,
				true);
		poolObj = eval.value;
		state = eval.state;
		threadPointer = universe.tupleRead(poolObj, this.twoObject);
		if (this.symbolicAnalyzer.isDerefablePointer(state,
				threadPointer, source).right == ResultType.YES) {
			threadTermPointer = this.symbolicUtil.makePointer(threadPointer,
					universe.tupleComponentReference(
							symbolicUtil.getSymRef(threadPointer),
							this.twoObject));
			state = this.primaryExecutor.assign(source, state, pid,
					threadTermPointer, trueValue);
			if (!argumentValues[1].type().isInteger()) {
				threadExitValuePointer = this.symbolicUtil.makePointer(
						threadPointer,
						universe.tupleComponentReference(
								symbolicUtil.getSymRef(threadPointer),
								this.threeObject));
				state = this.primaryExecutor.assign(source, state, pid,
						threadExitValuePointer, argumentValues[1]);
			}
		}
		return new Evaluation(state, null);
	}

	private Evaluation execute_pthread_pool_get_id(State state, int pid,
			String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression threadPointer = universe.tupleRead(argumentValues[0],
				this.twoObject);
		Evaluation eval = evaluator.dereference(source, state, pid, process,
				threadPointer, false, true);
		SymbolicExpression thread = eval.value;

		state = eval.state;
		return new Evaluation(state,
				universe.tupleRead(thread, this.twoObject));
	}

	private Evaluation execute_pthread_pool_get_terminated(State state, int pid,
			String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression threadPointer = universe.tupleRead(argumentValues[0],
				this.twoObject);
		Evaluation eval = evaluator.dereference(source, state, pid, process,
				threadPointer, false, true);
		SymbolicExpression thread = eval.value;

		state = eval.state;
		return new Evaluation(state,
				universe.tupleRead(thread, this.threeObject));
	}

	/**
	 * void $pthread_gpool_add($pthread_gpool gpool, pthread_t * thread);
	 * 
	 * @param state
	 * @param pid
	 * @param process
	 * @param arguments
	 * @param argumentValues
	 * @param source
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation execute_pthread_gpool_add(State state, int pid,
			String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression gpool = argumentValues[0],
				threadPointer = argumentValues[1], threadObj;
		Evaluation eval;
		SymbolicExpression gpoolObj, threads;

		eval = evaluator.dereference(source, state, pid, process, gpool, false,
				true);
		gpoolObj = eval.value;
		state = eval.state;
		eval = evaluator.dereference(source, state, pid, process, threadPointer,
				false, true);
		state = eval.state;
		threadObj = eval.value;
		threads = universe.tupleRead(gpoolObj, this.zeroObject);
		threads = universe.append(threads, threadObj);
		gpoolObj = universe.tupleWrite(gpoolObj, zeroObject, threads);
		state = this.primaryExecutor.assign(source, state, pid, gpool,
				gpoolObj);
		return new Evaluation(state, null);
	}

	/**
	 * $pthread_pool $pthread_pool_create ($scope scope, $pthread_gpool gpool);
	 * 
	 * 
	 * struct _pthread_pool_t{ $pthread_gpool gpool; $proc tid; pthread_t *
	 * thread; };
	 * 
	 * @param state
	 * @param pid
	 * @param process
	 * @param lhs
	 * @param arguments
	 * @param argumentValues
	 * @param source
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation execute_pthread_pool_create(State state, int pid,
			String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression scopeValue = argumentValues[0],
				gpool = argumentValues[1];
		SymbolicExpression tid = stateFactory.processValue(pid);
		SymbolicExpression gpoolObject;
		Evaluation eval;
		SymbolicExpression threadPointer, pool;

		eval = evaluator.dereference(source, state, pid, process, gpool, false,
				true);
		gpoolObject = eval.value;
		state = eval.state;
		eval = this.findThreadFromPool(source, state, pid, process, gpoolObject,
				pid);
		state = eval.state;
		threadPointer = eval.value;
		pool = universe.tuple(this.poolSymbolicType,
				Arrays.asList(gpool, tid, threadPointer));
		return this.primaryExecutor.malloc(source, state, pid, process,
				arguments[0], scopeValue, this.poolType, pool);
	}

	/**
	 * 
	 * @param source
	 * @param state
	 * @param process
	 * @param gpool
	 * @param tid
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	Evaluation findThreadFromPool(CIVLSource source, State state, int pid,
			String process, SymbolicExpression gpool, int tid)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression threads = universe.tupleRead(gpool, zeroObject);
		NumericExpression nthreads = universe.length(threads);
		int nthreads_int = this.symbolicUtil.extractInt(source, nthreads);
		Evaluation eval;

		for (int i = 0; i < nthreads_int; i++) {
			SymbolicExpression threadPointer = universe.arrayRead(threads,
					universe.integer(i));
			SymbolicExpression thread, threadId;
			int threadId_int;

			eval = evaluator.dereference(source, state, pid, process,
					threadPointer, false, true);
			thread = eval.value;
			state = eval.state;
			threadId = universe.tupleRead(thread, zeroObject);

			threadId_int = modelFactory.getProcessId(threadId);
			if (threadId_int == tid)
				return new Evaluation(state, threadPointer);
		}
		return new Evaluation(state, symbolicUtil.nullPointer());
	}

	private Evaluation execute_add_thread(State state, int pid, String process,
			Expression[] arguments, SymbolicExpression[] argumentValues,
			CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression poolPointer = argumentValues[0];
		SymbolicExpression threadPointer = argumentValues[1];
		CIVLSource poolPointerSource = arguments[0].getSource();
		SymbolicExpression pool;
		Evaluation eval = evaluator.dereference(poolPointerSource, state, pid,
				process, poolPointer, false, true);
		NumericExpression len;
		SymbolicExpression threads;

		pool = eval.value;
		state = eval.state;
		len = (NumericExpression) universe.tupleRead(pool, oneObject);
		threads = universe.tupleRead(pool, zeroObject);
		threads = universe.append(threads, threadPointer);
		len = universe.add(len, one);
		pool = universe.tupleWrite(pool, zeroObject, threads);
		pool = universe.tupleWrite(pool, oneObject, len);
		state = primaryExecutor.assign(poolPointerSource, state, pid,
				poolPointer, pool);
		return new Evaluation(state, null);
	}

}
