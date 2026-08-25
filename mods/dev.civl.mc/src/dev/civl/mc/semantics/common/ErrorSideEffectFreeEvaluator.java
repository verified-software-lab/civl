package dev.civl.mc.semantics.common;

import java.util.Arrays;

import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.log.IF.CIVLErrorLogger;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.expression.BinaryExpression;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.SubscriptExpression;
import dev.civl.mc.semantics.IF.Evaluation;
import dev.civl.mc.semantics.IF.Evaluator;
import dev.civl.mc.semantics.IF.LibraryEvaluatorLoader;
import dev.civl.mc.semantics.IF.LibraryExecutorLoader;
import dev.civl.mc.semantics.IF.SymbolicAnalyzer;
import dev.civl.mc.state.IF.MemoryUnitFactory;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.StateFactory;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.mc.util.IF.Pair;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.ReferenceExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.type.SymbolicFunctionType;
import dev.civl.sarl.IF.type.SymbolicType;

/**
 * An error side-effects free alternative of {@link CommonEvaluator}. This
 * evaluator will not report errors for error side-effects during the
 * evaluation.
 * 
 * @author ziqingluo
 *
 */
public class ErrorSideEffectFreeEvaluator extends CommonEvaluator implements Evaluator {

	/**
	 * The {@link Exception} that will be thrown only by
	 * {@link ErrorSideEffectFreeEvaluator} when an erroneous side effect happens.
	 * 
	 * @author ziqing
	 */
	static public class ErroneousSideEffectException extends UnsatisfiablePathConditionException {
		/**
		 * The {@link SymbolicExpression} that really causes the side effect error,
		 * which will be used to as a key for generating a unique undefined value
		 */
		public final SymbolicExpression keyValue;
		/**
		 * generated serial ID
		 */
		private static final long serialVersionUID = -1237052183722755533L;

		/**
		 * @param keyValue The {@link SymbolicExpression} that really causes the side
		 *                 effect error, which will be used to as a key for generating a
		 *                 unique undefined value
		 */
		public ErroneousSideEffectException(SymbolicExpression keyValue) {
			this.keyValue = keyValue;
		}
	}

	/**
	 * The name of an abstract function which will wrap a
	 * {@link ErroneousSideEffectException#keyValue}. A such function call
	 * represents a unique undefined value of some type.
	 */
	private static String SEError_ABSTRACT_FUNCTION_NAME = "SEError_undefined";

	public ErrorSideEffectFreeEvaluator(ModelFactory modelFactory, StateFactory stateFactory,
			LibraryEvaluatorLoader loader, LibraryExecutorLoader loaderExec, SymbolicUtility symbolicUtil,
			SymbolicAnalyzer symbolicAnalyzer, MemoryUnitFactory memUnitFactory, CIVLErrorLogger errorLogger,
			CIVLConfiguration config) {
		super(modelFactory, stateFactory, loader, loaderExec, symbolicUtil, symbolicAnalyzer, memUnitFactory,
				errorLogger, config);
	}

	@Override
	public Evaluation evaluate(State state, int pid, Expression expression) throws UnsatisfiablePathConditionException {
		try {
			return super.evaluate(state, pid, expression);
		} catch (ErroneousSideEffectException e) {
			SymbolicType exprType = expression.getExpressionType().getDynamicType(universe);
			SymbolicFunctionType funcType = universe.functionType(Arrays.asList(e.keyValue.type()), exprType);

			return new Evaluation(state,
					universe.apply(
							universe.symbolicConstant(universe.stringObject(SEError_ABSTRACT_FUNCTION_NAME), funcType),
							Arrays.asList(e.keyValue)));
		}
	}

	@Override
	public Evaluation dereference(CIVLSource source, State state, int pid, String process, SymbolicExpression pointer,
			boolean checkOutput, boolean strict) throws UnsatisfiablePathConditionException {
		boolean muteErrorSideEffects = true; // mute error side effects

		return dereferenceWorker(source, state, pid, process, pointer, checkOutput, false, strict,
				muteErrorSideEffects);
	}

	@Override
	protected Evaluation evaluateSubscript(State state, int pid, String process, SubscriptExpression expression)
			throws UnsatisfiablePathConditionException {
		return evaluateSubscriptWorker(state, pid, process, expression, true);
	}

	@Override
	protected Evaluation evaluateDivide(State state, int pid, BinaryExpression expression, NumericExpression numerator,
			NumericExpression denominator) throws UnsatisfiablePathConditionException {
		return evaluateDivideWorker(state, pid, expression, numerator, denominator, true);
	}

	@Override
	protected Evaluation evaluateModulo(State state, int pid, BinaryExpression expression, NumericExpression numerator,
			NumericExpression denominator) throws UnsatisfiablePathConditionException {
		return evaluateModuloWorker(state, pid, expression, numerator, denominator, true);
	}

	@Override
	public Pair<Evaluation, NumericExpression[]> arrayElementReferenceAdd(State state, int pid, SymbolicExpression ptr,
			NumericExpression offset, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression newPtr = symbolicUtil.makePointer(ptr,
				symbolicAnalyzer.getLeafNodeReference(state, ptr, source));

		return arrayElementReferenceAddWorker(state, pid, newPtr, offset, true, source);
	}

	@Override
	public Evaluation evaluatePointerAdd(State state, int pid, BinaryExpression expression, SymbolicExpression pointer,
			SymbolicExpression offset) throws UnsatisfiablePathConditionException {
		Pair<BooleanExpression, ResultType> checkPointer = symbolicAnalyzer.isDefinedPointer(state, pointer,
				expression.getSource());

		if (checkPointer.right != ResultType.YES)
			return new Evaluation(state, symbolicUtil.undefinedPointer());
		else {
			ReferenceExpression symRef = symbolicUtil.getSymRef(pointer);

			if (symRef.isArrayElementReference()) {
				return arrayElementReferenceAddWorker(state, pid, pointer, (NumericExpression) offset, true,
						expression.left().getSource()).left;
			} else if (symRef.isOffsetReference()) {
				return offsetReferenceAddition(state, pid, pointer, (NumericExpression) offset, true,
						expression.getSource());
			} else if (symRef.isIdentityReference()) {
				return identityReferenceAddition(state, pid, pointer, (NumericExpression) offset, true,
						expression.getSource());
			} else
				throw new CIVLUnimplementedFeatureException(
						"Pointer addition for anything other than array elements or variables", expression);
		}
	}
}
