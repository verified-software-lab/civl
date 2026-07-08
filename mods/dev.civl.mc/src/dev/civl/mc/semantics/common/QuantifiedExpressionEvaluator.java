package dev.civl.mc.semantics.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import dev.civl.abc.ast.node.IF.acsl.ExtendedQuantifiedExpressionNode.ExtendedQuantifier;
import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.log.IF.CIVLErrorLogger;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLSyntaxException;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.expression.ArrayLambdaExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression.BINARY_OPERATOR;
import dev.civl.mc.model.IF.expression.BoundVariableExpression;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.Expression.ExpressionKind;
import dev.civl.mc.model.IF.expression.ExtendedQuantifiedExpression;
import dev.civl.mc.model.IF.expression.LambdaExpression;
import dev.civl.mc.model.IF.expression.QuantifiedExpression;
import dev.civl.mc.model.IF.expression.RegularRangeExpression;
import dev.civl.mc.model.IF.type.CIVLCompleteArrayType;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.semantics.IF.Evaluation;
import dev.civl.mc.semantics.IF.LibraryEvaluatorLoader;
import dev.civl.mc.semantics.IF.LibraryExecutorLoader;
import dev.civl.mc.semantics.IF.SymbolicAnalyzer;
import dev.civl.mc.semantics.IF.TypeEvaluation;
import dev.civl.mc.state.IF.MemoryUnitFactory;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.StateFactory;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.mc.util.IF.Pair;
import dev.civl.sarl.IF.Reasoner;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.NumericSymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.number.IntegerNumber;
import dev.civl.sarl.IF.number.Number;
import dev.civl.sarl.IF.type.SymbolicCompleteArrayType;
import dev.civl.sarl.IF.type.SymbolicFunctionType;
import dev.civl.sarl.IF.type.SymbolicType;

/**
 * <p>
 * This class is a stateful evaluator for a single expression. All stateful
 * informations kept by this evaluator are only live within the evaluation of
 * the expression period. (Thus, for LibarayEvaluator or LibraryExecutors, their
 * primary evaluator should refer to a instance of this class during such a
 * stateful evaluation period).
 * </p>
 * 
 * <p>
 * This is a stateful evaluator for different kinds of quantified expressions:
 * <ul>
 * <li>Ordinary quantified expression: FORALL or EXISTS</li>
 * <li>Extended quantified expression (fold expression)</li>
 * <li>Lambda expression</li>
 * <li>Array lambda expression</li>
 * <li>Uniform expression</li>
 * <li>Big-O expression</li>
 * </ul>
 * <strong>No array-out-of bound error will be thrown by CIVL during the
 * evaluation of quantified expressions</strong>
 * </p>
 * 
 * The reason of why the above expressions need a stateful evaluation is: All of
 * this expressions are allowed to contain bounded or free variables and
 * restrictions. They can affect the evaluation state on their sub-expressions.
 * 
 * @author ziqing (Ready for review)
 *
 */
public class QuantifiedExpressionEvaluator
		extends
			ErrorSideEffectFreeEvaluator {

	/**
	 * A stack of map used to store bound variables during evaluation of
	 * (possibly nested) quantified expressions. LinkedList is used instead of
	 * Stack because of its more intuitive iteration order.
	 */
	private LinkedList<Map<String, SymbolicConstant>> boundVariableStack = new LinkedList<>();

	/**
	 * This object represents a stack of restrictions specified by
	 * {@link ExtendedQuantifiedExpression}s. One extended quantified expression
	 * specifies a restriction on the free variable of its lambda expression via
	 * defining the bounds on it. For example
	 * <code> \sum(0, 10, \lambda i; i+1); </code> The restriction on free
	 * variable i is <code>0&lt= i &lt=10</code>.
	 */
	private Stack<SymbolicExpression> extendedQuantifiedRestrictionsStack = new Stack<>();

	/**
	 * A Java function interface. An instance of this interface can be assigned
	 * by a Java method which has two {@link BooleanExpression}s as arguments
	 * and returns a {@link BooleanExpression}.
	 * 
	 * @author ziqing
	 *
	 */
	@FunctionalInterface
	private static interface LogicalOperation {
		BooleanExpression operation(BooleanExpression op0,
				BooleanExpression op1);
	}

	/**
	 * A Java function interface. An instance of this interface can be assigned
	 * by a Java method which has a {@link SymbolicConstant} argument and a
	 * {@link BooleanExpression} argument, returns a {@link BooleanExpression}.
	 * 
	 * @author ziqing
	 *
	 */
	@FunctionalInterface
	private static interface ApplyConstantOperation {
		BooleanExpression operation(SymbolicConstant boundVar,
				BooleanExpression pred);
	}

	@Override
	public Evaluation evaluate(State state, int pid, Expression expression)
			throws UnsatisfiablePathConditionException {
		if (expression.expressionKind() == ExpressionKind.BOUND_VARIABLE)
			return evaluateBoundVariable(state, pid,
					(BoundVariableExpression) expression);
		else
			return super.evaluate(state, pid, expression);
	}

	/**
	 * Constructor, parameters are similar to
	 * {@link CommonEvaluator#CommonEvaluator(ModelFactory, StateFactory, LibraryEvaluatorLoader, LibraryExecutorLoader, SymbolicUtility, SymbolicAnalyzer, MemoryUnitFactory, CIVLErrorLogger, CIVLConfiguration)}
	 */
	QuantifiedExpressionEvaluator(ModelFactory modelFactory,
			StateFactory stateFactory, LibraryEvaluatorLoader loader,
			LibraryExecutorLoader loaderExec, SymbolicUtility symbolicUtil,
			SymbolicAnalyzer symbolicAnalyzer, MemoryUnitFactory memUnitFactory,
			CIVLErrorLogger errorLogger, CIVLConfiguration config) {
		super(modelFactory, stateFactory, loader, loaderExec, symbolicUtil,
				symbolicAnalyzer, memUnitFactory, errorLogger, config);
	}

	/**
	 * <p>
	 * Evaluate an {@link ArrayLambdaExpression}
	 * </p>
	 * 
	 * @param state
	 *            The state where the evaluation happens
	 * @param pid
	 *            The PID of the process who invokes the evaluation
	 * @param arrayLambda
	 *            The expression that will be evaluated
	 * @return The evaluation result
	 * @throws UnsatisfiablePathConditionException
	 */
	@Override
	protected Evaluation evaluateArrayLambda(State state, int pid,
			ArrayLambdaExpression arrayLambda)
			throws UnsatisfiablePathConditionException {
		List<Pair<List<Variable>, Expression>> boundVariableList = arrayLambda
				.boundVariableList();
		CIVLCompleteArrayType exprType = arrayLambda.getExpressionType();
		NumericSymbolicConstant[] boundVariables;
		TypeEvaluation typeEval = getDynamicType(state, pid, exprType,
				arrayLambda.getSource(), false);
		SymbolicCompleteArrayType arrayType = (SymbolicCompleteArrayType) typeEval.type;
		Evaluation eval;
		int numBoundVars = 0;

		boundVariables = new NumericSymbolicConstant[exprType.dimension()];
		state = typeEval.state;
		boundVariableStack.push(new TreeMap<>());
		for (Pair<List<Variable>, Expression> boundVariableSubList : boundVariableList) {
			if (boundVariableSubList.right != null)
				throw new CIVLUnimplementedFeatureException(
						"declaring bound variables within a specific domain in array lambdas",
						arrayLambda.getSource());
			for (Variable variable : boundVariableSubList.left) {
				NumericSymbolicConstant boundVariable;

				assert variable.type().isIntegerType();
				boundVariable = (NumericSymbolicConstant) universe
						.symbolicConstant(variable.name().stringObject(),
								variable.type().getDynamicType(universe));
				boundVariables[numBoundVars++] = boundVariable;
				boundVariableStack.peek().put(boundVariable.name().getString(),
						boundVariable);
			}
		}
		assert exprType.dimension() == numBoundVars;
		if (arrayLambda.restriction() != null) {
			eval = evaluate(state, pid, arrayLambda.restriction());
			if (!eval.value.isTrue())
				throw new CIVLUnimplementedFeatureException(
						"non-trivial restriction expression in array lambdas",
						arrayLambda.getSource());
		}
		eval = new Evaluation(state, arrayLambda(state, pid, boundVariables, 0,
				arrayType, arrayLambda.expression()));
		boundVariableStack.pop();
		return eval;
	}

	/**
	 * Evaluates a bound variable expression.
	 * 
	 * @param state
	 *            The state where the evaluation happens.
	 * @param pid
	 *            The PID of the process who invokes the evaluation.
	 * @param expression
	 *            The bound variable expression to be evaluated.
	 * @return A possibly new state resulted from side effects during the
	 *         evaluation and the value of the bound variable expression.
	 * @throws UnsatisfiablePathConditionException
	 */
	Evaluation evaluateBoundVariable(State state, int pid,
			BoundVariableExpression expression) {
		SymbolicConstant value = null;
		String name = expression.name().name();

		for (Map<String, SymbolicConstant> boundVariableSet : boundVariableStack) {
			value = boundVariableSet.get(name);
			if (value != null)
				break;
		}
		if (value == null)
			throw new CIVLInternalException(
					"unreachable: unknown bound variable",
					expression.getSource());
		if (value.isNumeric()) {
			Reasoner reasoner = universe
					.reasoner(state.getPathCondition(universe));
			Number number = reasoner.extractNumber((NumericExpression) value);

			if (number != null)
				return new Evaluation(state, universe.number(number));
		}
		return new Evaluation(state, value);
	}

	@Override
	protected Evaluation evaluateQuantifiedExpression(State state, int pid,
			QuantifiedExpression expression)
			throws UnsatisfiablePathConditionException {
		List<Pair<List<Variable>, Expression>> boundVariableList = expression
				.boundVariableList();
		Evaluation eval;
		int numBoundVars = expression.numBoundVariables();
		SymbolicConstant[] boundVariables = new SymbolicConstant[numBoundVars];
		BooleanExpression restriction = processBoundVariableList(state, pid,
				boundVariableList, boundVariables, expression.getSource());

		eval = evaluate(state, pid, expression.restriction());
		state = eval.state;
		restriction = universe.and(restriction, (BooleanExpression) eval.value);

		// Temporarily add restriction into path condition:
		State newState = stateFactory.addToPathcondition(state, pid,
				restriction);
		BooleanExpression predicate;

		try {
			predicate = (BooleanExpression) evaluate(newState, pid,
					expression.expression()).value;
		} catch (UnsatisfiablePathConditionException e) {
			// since the restriction is pushed into the context for evaluating
			// the predicate, if an unsatisfiable exception was caught, which
			// means the "restriction && context" is unsatisfiable hence this
			// expression evaluates to either true (forall) or false (exists).
			switch (expression.quantifier()) {
				case EXISTS :
					return new Evaluation(state, universe.falseExpression());
				case FORALL :
					return new Evaluation(state, universe.trueExpression());
				default :
					throw new CIVLInternalException(
							"Unknown quantifier: " + expression.quantifier(),
							expression.getSource());
			}
		}
		// function references:
		// Either "restriction AND predicate" or "restriction IMPLIES
		// predicate" ?
		LogicalOperation restirctionCombiner;
		// Either "exists" or "forall" ?
		ApplyConstantOperation quantifiedExpression;

		switch (expression.quantifier()) {
			case EXISTS :
				restirctionCombiner = universe::and;
				quantifiedExpression = universe::exists;
				break;
			case FORALL :
				restirctionCombiner = universe::implies;
				quantifiedExpression = universe::forall;
				break;
			default :
				throw new CIVLInternalException(
						"Unknown quantifier: " + expression.quantifier(),
						expression.getSource());
		}
		predicate = restirctionCombiner.operation(restriction, predicate);
		for (SymbolicConstant complexBoundVar : boundVariables)
			predicate = quantifiedExpression.operation(complexBoundVar,
					predicate);
		eval = new Evaluation(state, predicate);
		boundVariableStack.pop();
		return eval;
	}

	/**
	 * Evaluate an {@link ExtendedQuantifiedExpression}
	 * 
	 * @throws UnsatisfiablePathConditionException
	 */
	@Override
	protected Evaluation evaluateExtendedQuantifiedExpression(State state,
			int pid, ExtendedQuantifiedExpression extQuant)
			throws UnsatisfiablePathConditionException {
		Evaluation eval;
		Expression function = extQuant.function();
		NumericExpression low, high;
		ExtendedQuantifier quant = extQuant.extendedQuantifier();
		CIVLSource source = extQuant.getSource();

		eval = evaluate(state, pid, extQuant.lower());
		state = eval.state;
		low = (NumericExpression) eval.value;
		eval = evaluate(state, pid, extQuant.higher());
		high = (NumericExpression) eval.value;
		state = eval.state;

		NumericSymbolicConstant idx = (NumericSymbolicConstant) universe
				.symbolicConstant(universe.stringObject("i"),
						universe.integerType());
		BooleanExpression restriction = universe.lessThanEquals(low, idx);
		Number lowNum, highNum;
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		TypeEvaluation typeEval = this.getDynamicType(state, pid,
				extQuant.getExpressionType(), source, false);

		restriction = universe.and(restriction,
				universe.lessThanEquals(idx, high));
		// To deal with nested extended-quantified expressions:
		// Push a lambda function into the stack. During the evaluation of the
		// extended-quantified expression, applying the top stack entry to a
		// free variable in a lambda expression will return a boolean-value
		// restriction for the free variable:
		extendedQuantifiedRestrictionsStack
				.push(universe.lambda(idx, restriction));
		lowNum = reasoner.extractNumber(low);
		highNum = reasoner.extractNumber(high);
		eval = evaluate(typeEval.state, pid, function);
		// Using different helper methods for concrete and non-concrete cases:
		if (lowNum != null && highNum != null) {
			int lowInt, highInt;

			lowInt = ((IntegerNumber) lowNum).intValue();
			highInt = ((IntegerNumber) highNum).intValue();
			eval.value = computeConcreteFoldExpression(lowInt, highInt,
					eval.value, quant, typeEval.type, source);
		} else
			eval = computeNonconcreteFoldExpression(eval.state, pid, reasoner,
					low, high, eval.value, quant, source);
		extendedQuantifiedRestrictionsStack.pop();
		return eval;
	}

	/**
	 * 
	 * <p>
	 * Evaluate {@link ExtendedQuantifiedExpression} e(i,j,f):
	 * <code>f(i) op f(i+1) op ... op f(j)</code> where i &lt= j and op stands
	 * for an {@link ExtendedQuantifier}.
	 * 
	 * This method requires both i and j have non-concrete values.
	 * 
	 * If j > i can be proved, an induction step will be added to the path
	 * condition: <code>e(i,j,f) == e(i,j-1,f) + f(j)</code>
	 * </p>
	 * 
	 * @param state
	 *            The current state when this method is called
	 * @param reasoner
	 *            A reference to a {@link Reasoner}
	 * @param low
	 *            The lower bound of the parameter.
	 * @param high
	 *            The higher bound of the parameter.
	 * @param lambda
	 *            The lambda expression which maps the parameter to an
	 *            expression
	 * @param quant
	 *            The {@link ExtendedQuantifier} which is a kind of a binary
	 *            operator
	 * @param source
	 *            The {@link CIVLSource} related to this method call
	 * @return
	 */
	private Evaluation computeNonconcreteFoldExpression(State state, int pid,
			Reasoner reasoner, NumericExpression low, NumericExpression high,
			SymbolicExpression lambda, ExtendedQuantifier quant,
			CIVLSource source) {
		NumericExpression result;

		if (reasoner.isValid(universe.lessThan(high, low))) {
			result = ((SymbolicFunctionType) lambda.type()).outputType()
					.isInteger() ? universe.zeroInt() : universe.zeroReal();
			return new Evaluation(state, result);
		}
		switch (quant) {
			case SUM :
				// Higher bound in SARL's sigma is exclusive:
				result = universe.sigma(low,
						universe.add(high, universe.oneInt()), lambda);
				break;
			default :
				throw new CIVLUnimplementedFeatureException(
						"evaluating non-concrete extended quantification "
								+ quant,
						source);
		}
		return new Evaluation(state, result);
	}

	/**
	 * <p>
	 * Evaluate {@link ExtendedQuantifiedExpression} :
	 * <code>f(i) op f(i+1) op ... op f(j)</code> where i &lt= j and op stands
	 * for an {@link ExtendedQuantifier}.
	 * 
	 * This method requires both i and j have concrete values.
	 * </p>
	 * 
	 * @param low
	 *            The lower bound of the parameter.
	 * @param high
	 *            The higher bound of the parameter.
	 * @param lambda
	 *            The lambda expression which maps the parameter to an
	 *            expression
	 * @param quant
	 *            The {@link ExtendedQuantifier} which is a kind of a binary
	 *            operator
	 * @param expressionType
	 *            The expression type of this
	 *            {@link ExtendedQuantifiedExpression}.
	 * @param source
	 *            The {@link CIVLSource} related to this method call
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private NumericExpression computeConcreteFoldExpression(int low, int high,
			SymbolicExpression lambda, ExtendedQuantifier quant,
			SymbolicType expressionType, CIVLSource source) {
		if (high < low)
			return expressionType.isInteger()
					? universe.zeroInt()
					: universe.zeroReal();

		SymbolicExpression applied = universe.apply(lambda,
				Arrays.asList(universe.integer(low)));

		if (!(applied instanceof NumericExpression))
			throw new CIVLSyntaxException(
					"the body of $sum must have a numeric type "
							+ "(int, float, etc.), but the body produced "
							+ "a value of type " + applied.type(),
					source);
		NumericExpression result = (NumericExpression) applied;

		for (int i = low + 1; i <= high; i++) {
			NumericExpression index = universe.integer(i);
			NumericExpression current;
			SymbolicExpression currentVal = universe.apply(lambda,
					Arrays.asList(index));

			if (!(currentVal instanceof NumericExpression))
				throw new CIVLSyntaxException(
						"the body of $sum must have a numeric type "
								+ "(int, float, etc.), but the body produced "
								+ "a value of type " + currentVal.type(),
						source);
			current = (NumericExpression) currentVal;
			switch (quant) {
				case SUM :
					result = universe.add(result, current);
					break;
				case PROD :
					result = universe.multiply(result, current);
					break;
				default :
					throw new CIVLUnimplementedFeatureException(
							"evaluating concrete extended quantification "
									+ quant,
							source);
			}
		}
		return result;
	}

	@Override
	protected Evaluation evaluateLambda(State state, int pid,
			LambdaExpression lambda)
			throws UnsatisfiablePathConditionException {
		Variable freeVariable = lambda.freeVariable();
		Evaluation eval = null;
		TypeEvaluation typeEval;
		SymbolicType varType;
		NumericSymbolicConstant freeVariableValue;

		typeEval = getDynamicType(state, pid, freeVariable.type(),
				freeVariable.getSource(), false);
		state = typeEval.state;
		varType = typeEval.type;
		boundVariableStack.push(new TreeMap<>());
		freeVariableValue = (NumericSymbolicConstant) universe
				.symbolicConstant(freeVariable.name().stringObject(), varType);
		boundVariableStack.peek().put(freeVariableValue.name().getString(),
				freeVariableValue);

		State oldState = state;

		if (!extendedQuantifiedRestrictionsStack.isEmpty()) {
			SymbolicExpression restrictFunction = extendedQuantifiedRestrictionsStack
					.peek();
			BooleanExpression restriction;

			restriction = (BooleanExpression) universe.apply(restrictFunction,
					Arrays.asList(freeVariableValue));
			assert restriction.type()
					.typeKind() == SymbolicType.SymbolicTypeKind.BOOLEAN;
			state = stateFactory.addToPathcondition(state, pid, restriction);
		}
		eval = evaluate(state, pid, lambda.lambdaFunction());
		eval.state = oldState;
		eval.value = universe.lambda(freeVariableValue, eval.value);
		boundVariableStack.pop();
		return eval;
	}

	@Override
	protected Evaluation evaluateValid(State state, int pid, Expression pointer,
			Expression offsets, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		Evaluation eval;

		if (offsets.getExpressionType().isIntegerType()) {
			Expression singlePointer = modelFactory.binaryExpression(source,
					BINARY_OPERATOR.PLUS, pointer, offsets);

			eval = evaluate(state, pid, singlePointer);
			eval.value = symbolicAnalyzer.isDerefablePointer(state,
					eval.value, source).left;
			return eval;
		} else {
			// for \valid(p + range), it is evaluated as
			// forall int i. i in range -> dereferable(p + i)
			RegularRangeExpression range = (RegularRangeExpression) offsets;
			// step is always one ...
			NumericExpression lowVal, highVal;
			NumericSymbolicConstant offset = (NumericSymbolicConstant) universe
					.symbolicConstant(
							universe.stringObject(BOUNDED_OFFSET_IDENTIFIER),
							universe.integerType());

			eval = evaluate(state, pid, range);
			state = eval.state;
			lowVal = symbolicUtil.getLowOfRegularRange(eval.value);
			highVal = symbolicUtil.getHighOfRegularRange(eval.value);

			BooleanExpression offsetBounds = universe.and(
					universe.lessThanEquals(lowVal, offset),
					universe.lessThanEquals(offset, highVal));
			Expression boundVar = modelFactory.boundVariableExpression(
					offsets.getSource(),
					modelFactory.identifier(offsets.getSource(),
							BOUNDED_OFFSET_IDENTIFIER),
					typeFactory.integerType());
			Expression eachPointer = modelFactory.binaryExpression(source,
					BINARY_OPERATOR.POINTER_ADD, pointer, boundVar);

			state = stateFactory.pushAssumption(state, pid, offsetBounds);
			boundVariableStack.push(new TreeMap<>());
			boundVariableStack.peek().put(BOUNDED_OFFSET_IDENTIFIER, offset);
			eval = evaluate(state, pid, eachPointer);
			eval.value = symbolicAnalyzer.isDerefablePointer(state,
					eval.value, source).left;
			boundVariableStack.pop();
			eval.state = stateFactory.popAssumption(eval.state, pid);
			eval.value = universe.forallInt(offset, lowVal, highVal,
					(BooleanExpression) eval.value);
			return eval;
		}
	}

	/* ********************** Private helper methods ************************ */
	/**
	 * Evaluate a list of bound variables to a set of symbolic constants. For
	 * bound variables bounded by domains, their constraints will be returned as
	 * a boolean expression.
	 * 
	 * @param state
	 *            The current state
	 * @param pid
	 *            The PID of the calling process.
	 * @param boundVariableList
	 *            A list of bound variable groups. Each bound variable group
	 *            shares a domain constraint or has no domain constraint.
	 * @param boundVariables
	 *            Output argument. An array eventually will contain symbolic
	 *            constants which are values of bound variables.
	 * @param source
	 *            CIVLSource associates to those bounded variables.
	 * @return a constraint on some variables which are bounded by domains.
	 * @throws UnsatisfiablePathConditionException
	 */
	private BooleanExpression processBoundVariableList(State state, int pid,
			List<Pair<List<Variable>, Expression>> boundVariableList,
			SymbolicConstant[] boundVariables, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		BooleanExpression restriction = universe.trueExpression();
		Evaluation eval;
		int index = 0;

		this.boundVariableStack.push(new HashMap<>());
		for (Pair<List<Variable>, Expression> boundVariableSubList : boundVariableList) {
			List<Variable> boundVariableDecls = boundVariableSubList.left;
			Expression domain = boundVariableSubList.right;
			SymbolicConstant boundValue;

			if (domain != null && boundVariableDecls.size() != 1)
				throw new CIVLUnimplementedFeatureException(
						"declaring bound variables within a specific domain in quantified expressions",
						source);
			if (domain != null) {
				// range
				Variable boundVar = boundVariableDecls.get(0);
				SymbolicExpression range;
				NumericExpression lower, upper;

				boundValue = universe.symbolicConstant(
						boundVar.name().stringObject(),
						boundVar.type().getDynamicType(universe));
				eval = this.evaluate(state, pid, domain);
				// TODO assert domain has dimension one
				boundVariables[index++] = boundValue;
				this.boundVariableStack.peek()
						.put(boundValue.name().getString(), boundValue);
				state = eval.state;
				range = eval.value;
				lower = this.symbolicUtil.getLowOfRegularRange(range);
				upper = this.symbolicUtil.getHighOfRegularRange(range);
				restriction = universe.and(restriction, universe.and(
						this.universe.lessThanEquals(lower,
								(NumericExpression) boundValue),
						this.universe.lessThanEquals(
								(NumericExpression) boundValue, upper)));
			} else {
				for (Variable boundVar : boundVariableDecls) {
					boundValue = universe.symbolicConstant(
							boundVar.name().stringObject(),
							boundVar.type().getDynamicType(universe));
					boundVariables[index++] = boundValue;
					this.boundVariableStack.peek()
							.put(boundValue.name().getString(), boundValue);
				}
			}
		}
		return restriction;
	}
}
