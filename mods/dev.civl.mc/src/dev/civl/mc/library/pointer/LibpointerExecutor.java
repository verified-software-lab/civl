package dev.civl.mc.library.pointer;

import java.util.ArrayList;
import java.util.List;

import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.library.common.BaseLibraryExecutor;
import dev.civl.mc.model.IF.CIVLProperty;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.type.CIVLPointerType;
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
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.ReferenceExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;
import dev.civl.sarl.IF.type.SymbolicArrayType;
import dev.civl.sarl.IF.type.SymbolicType;
import dev.civl.sarl.IF.type.SymbolicType.SymbolicTypeKind;

public class LibpointerExecutor extends BaseLibraryExecutor implements LibraryExecutor {

	public LibpointerExecutor(String name, Executor primaryExecutor, ModelFactory modelFactory,
			SymbolicUtility symbolicUtil, SymbolicAnalyzer symbolicAnalyzer, CIVLConfiguration civlConfig,
			LibraryExecutorLoader libExecutorLoader, LibraryEvaluatorLoader libEvaluatorLoader) {
		super(name, primaryExecutor, modelFactory, symbolicUtil, symbolicAnalyzer, civlConfig, libExecutorLoader,
				libEvaluatorLoader);
	}

	@Override
	protected Evaluation executeValue(State state, int pid, String process, CIVLSource source, String functionName,
			Expression[] arguments, SymbolicExpression[] argumentValues) throws UnsatisfiablePathConditionException {
		Evaluation callEval = null;

		switch (functionName) {
		case "$apply":
			callEval = executeApply(state, pid, process, arguments, argumentValues, source);
			break;
		case "$contains":
			callEval = executeContains(state, pid, process, arguments, argumentValues, source);
			break;
		case "$copy":
			callEval = executeCopy(state, pid, process, arguments, argumentValues, source);
			break;
		case "$equals":
			callEval = executeEquals(state, pid, process, arguments, argumentValues, source);
			break;
		case "$assert_equals":
			callEval = executeAssertEquals(state, pid, process, arguments, argumentValues, source);
			break;
		case "$translate_ptr":
			callEval = executeTranslatePointer(state, pid, process, arguments, argumentValues, source);
			break;
		case "$leaf_node_ptrs":
			callEval = executeLeafNodePointers(state, pid, process, arguments, argumentValues, source);
			break;
		case "$is_identity_ref":
			callEval = executeIsIdentityRef(state, pid, process, arguments, argumentValues, source);
			break;
		case "$leaf_nodes_equal_to":
			callEval = execute_leaf_nodes_equal_to(state, pid, process, arguments, argumentValues, source);
			break;
		case "$has_leaf_node_equal_to":
			callEval = execute_has_leaf_node_equal_to(state, pid, process, arguments, argumentValues, source);
			break;
		case "$set_default":
			callEval = executeSetDefault(state, pid, process, arguments, argumentValues, source);
			break;
		case "$set_leaf_nodes":
			callEval = execute_set_leaf_nodes(state, pid, process, arguments, argumentValues, source);
			break;
		case "$is_derefable_pointer":
			callEval = execute_is_valid_pointer(state, pid, process, arguments, argumentValues, source);
			break;
		case "$pointer_add":
			callEval = executePointer_add(state, pid, process, arguments, argumentValues, source);
			break;
		default:
			throw new CIVLUnimplementedFeatureException("the function " + name + " of library pointer.cvh", source);
		}
		return callEval;
	}

	/**
	 * <pre>
	 * updates the leaf nodes of a status variable to the default value 0
	 * 
	 * void $set_default(void *status);
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
	private Evaluation executeSetDefault(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		CIVLType objectTypeByPointer = symbolicAnalyzer.civlTypeOfObjByPointer(arguments[0].getSource(), state,
				argumentValues[0]);
		SymbolicExpression value;

		// TODO assert objectTypeByPointer.isScalarType()
		if (objectTypeByPointer.isBoolType())
			value = this.falseValue;
		else if (objectTypeByPointer.isIntegerType())
			value = this.zero;
		else if (objectTypeByPointer.isRealType())
			value = universe.rational(0);
		else if (objectTypeByPointer.isCharType())
			value = universe.character((char) 0);
		else if (objectTypeByPointer.isPointerType())
			value = symbolicUtil.nullPointer();
		else
			throw new CIVLUnimplementedFeatureException(
					"Argument of " + objectTypeByPointer + " type for $set_default()", source);
		state = this.primaryExecutor.assign(source, state, pid, argumentValues[0], value);
		return new Evaluation(state, null);
	}

	/**
	 * <pre>
	 * applies the operation op on obj1 and obj2 and stores the result 
	 * void $apply(void *obj1, $operation op, void *obj2, void *result);
	 * </pre>
	 * 
	 * TODO: this method should be specified with some pre-conditions, currently I
	 * assume "*obj1" or "obj2" points to a object that has a basic type which can
	 * be applied an CIVL operation.
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
	private Evaluation executeApply(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		Pair<Evaluation, SymbolicExpression> writtenRet;
		SymbolicExpression objs[], result;
		NumericExpression operandCount;
		CIVLOperator civlOperator;
		Evaluation eval;
		int opCode;

		opCode = this.symbolicUtil.extractInt(arguments[1].getSource(), (NumericExpression) argumentValues[1]);
		civlOperator = translateOperator(opCode);
		// size of operands:
		operandCount = operandCounts(civlOperator);
		objs = new SymbolicExpression[2];
		eval = getDataFrom(state, pid, process, arguments[0], argumentValues[0], operandCount, false, false, source);
		state = eval.state;
		objs[0] = eval.value;
		eval = getDataFrom(state, pid, process, arguments[2], argumentValues[2], operandCount, false, false, source);
		state = eval.state;
		objs[1] = eval.value;
		if (civlConfig.isPropertyToggled(CIVLProperty.POINTER) && !objs[0].type().equals(objs[1].type())) {
			errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
					CIVLProperty.POINTER,
					"Arguments of the $apply system function have different types: \n" + arguments[0] + " points to a "
							+ objs[0].type() + " object\n" + arguments[2] + " points to a " + objs[1].type()
							+ " object\n");
			throw new UnsatisfiablePathConditionException();
		}
		SymbolicType operandType = objs[0].type();
		SymbolicType elementType = operandType.typeKind() == SymbolicTypeKind.ARRAY
				? ((SymbolicArrayType) operandType).baseType()
				: operandType;

		result = applyCIVLOperation(state, pid, process, objs, civlOperator, one, elementType, source);
		writtenRet = setDataFrom(state, pid, process, arguments[3], argumentValues[3], operandCount, result, false,
				source);
		eval = writtenRet.left;
		state = primaryExecutor.assign(source, eval.state, pid, writtenRet.right, eval.value);
		eval.state = state;
		eval.value = null;
		return eval;
	}

	private Evaluation execute_is_valid_pointer(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression result = this.symbolicAnalyzer.isDerefablePointer(state, argumentValues[0], source).left;

		return new Evaluation(state, result);
	}

	/**
	 *
	 * returns true iff at least one leaf nodes of the given object equal to the
	 * given value
	 * 
	 * _Bool $has_leaf_node_equal_to(void *obj, int value);
	 * 
	 * @throws UnsatisfiablePathConditionException
	 */

	private Evaluation execute_has_leaf_node_equal_to(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		CIVLType objectType = symbolicAnalyzer.civlTypeOfObjByPointer(arguments[1].getSource(), state,
				argumentValues[0]);
		List<ReferenceExpression> leafs = this.evaluator.leafNodeReferencesOfType(arguments[0].getSource(), state, pid,
				objectType);
		List<SymbolicExpression> leafPointers = new ArrayList<>();
		SymbolicExpression objectPointer = argumentValues[0];
		Evaluation eval;
		SymbolicExpression result = falseValue;

		for (ReferenceExpression ref : leafs)
			leafPointers.add(this.symbolicUtil.setSymRef(objectPointer, ref));
		for (SymbolicExpression leafPtr : leafPointers) {
			eval = evaluator.dereference(source, state, pid, process, leafPtr, false, true);
			state = eval.state;
			if (universe.equals(eval.value, argumentValues[1]).isTrue()) {
				result = trueValue;
				break;
			}
		}
		return new Evaluation(state, result);
	}

	/**
	 * _Bool $leaf_nodes_equal_to(void *obj, int value);
	 * 
	 * @throws UnsatisfiablePathConditionException
	 */

	private Evaluation execute_leaf_nodes_equal_to(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		CIVLType objectType = symbolicAnalyzer.civlTypeOfObjByPointer(arguments[1].getSource(), state,
				argumentValues[0]);
		List<ReferenceExpression> leafs = evaluator.leafNodeReferencesOfType(arguments[0].getSource(), state, pid,
				objectType);
		List<SymbolicExpression> leafPointers = new ArrayList<>();
		SymbolicExpression objectPointer = argumentValues[0];
		Evaluation eval;
		SymbolicExpression result = trueValue;

		for (ReferenceExpression ref : leafs)
			leafPointers.add(this.symbolicUtil.setSymRef(objectPointer, ref));
		for (SymbolicExpression leafPtr : leafPointers) {
			eval = evaluator.dereference(source, state, pid, process, leafPtr, false, true);
			state = eval.state;
			if (universe.equals(eval.value, argumentValues[1]).isFalse()) {
				result = falseValue;
				break;
			}
		}
		return new Evaluation(state, result);
	}

	/**
	 * 
	 * updates the leaf nodes of the given objects to with the given integer value
	 * 
	 * void $set_leaf_nodes(void *obj, int value);
	 * 
	 * @throws UnsatisfiablePathConditionException
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
	private Evaluation execute_set_leaf_nodes(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		CIVLType objectType = symbolicAnalyzer.civlTypeOfObjByPointer(arguments[1].getSource(), state,
				argumentValues[0]);
		List<ReferenceExpression> leafs = this.evaluator.leafNodeReferencesOfType(arguments[0].getSource(), state, pid,
				objectType);
		List<SymbolicExpression> leafPointers = new ArrayList<>();
		SymbolicExpression objectPointer = argumentValues[0];

		for (ReferenceExpression ref : leafs)
			leafPointers.add(this.symbolicUtil.setSymRef(objectPointer, ref));
		for (SymbolicExpression leafPtr : leafPointers)
			state = this.primaryExecutor.assign(source, state, pid, leafPtr, argumentValues[1]);
		return new Evaluation(state, null);
	}

	/**
	 * _Bool $is_identity_ref(void *obj);
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
	private Evaluation executeIsIdentityRef(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression result = falseValue, objetPointer = argumentValues[0];

		if (!symbolicUtil.isPointerToHeap(objetPointer)) {
			if (symbolicUtil.getSymRef(objetPointer).isIdentityReference())
				result = trueValue;
		}
		return new Evaluation(state, result);
	}

	/**
	 * Copies the references to the leaf nodes of obj to the given array
	 * <p>
	 * obj:pointer to type T' whose leaf node types are all type T <br>
	 * array: pointer to array of pointer to type T
	 * 
	 * void $leaf_node_ptrs(void *array, void *obj);
	 * 
	 * incomplete array type not supporte at this point
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
	private Evaluation executeLeafNodePointers(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		// TODO check null or invalid pointers.
		CIVLType objectType = symbolicAnalyzer.civlTypeOfObjByPointer(arguments[1].getSource(), state,
				argumentValues[1]);
		List<ReferenceExpression> leafs = this.evaluator.leafNodeReferencesOfType(arguments[1].getSource(), state, pid,
				objectType);
		List<SymbolicExpression> leafPointers = new ArrayList<>();
		SymbolicExpression objectPointer = argumentValues[1];
		SymbolicExpression result;

		for (ReferenceExpression ref : leafs) {
			leafPointers.add(this.symbolicUtil.setSymRef(objectPointer, ref));
		}
		result = universe.array(typeFactory.pointerSymbolicType(), leafPointers);
		state = this.primaryExecutor.assign(source, state, pid, argumentValues[0], result);
		return new Evaluation(state, null);
	}

	private Evaluation executeContains(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression first = argumentValues[0], second = argumentValues[1], result;
		Pair<BooleanExpression, ResultType> checkFirst = symbolicAnalyzer.isDerefablePointer(state, first, source),
				checkRight = symbolicAnalyzer.isDerefablePointer(state, second, source);

		if (checkFirst.right != ResultType.YES || checkRight.right != ResultType.YES)
			result = falseValue;
		else
			result = symbolicUtil.contains(first, second);
		return new Evaluation(state, result);
	}

	private Evaluation executeCopy(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression left = argumentValues[0];
		SymbolicExpression right = argumentValues[1];
		Evaluation eval;
		CIVLSource sourceLeft = arguments[0].getSource();
		CIVLSource sourceRight = arguments[1].getSource();

		if ((symbolicUtil.isNullPointer(left) || symbolicUtil.isNullPointer(right))) {
			StringBuffer msg = new StringBuffer();

			msg.append("the arguments of $copy() must both be non-null pointers.\n");
			msg.append("first argument:\n");
			msg.append("    ");
			msg.append(arguments[0]);
			msg.append("    ");
			msg.append(symbolicAnalyzer.expressionEvaluation(state, pid, arguments[0], false).right);
			msg.append("\n    ");
			msg.append(symbolicAnalyzer.symbolicExpressionToString(sourceLeft, state, arguments[0].getExpressionType(),
					left));
			msg.append("\nsecond argument:\n");
			msg.append("    ");
			msg.append(arguments[1]);
			msg.append("    ");
			msg.append(symbolicAnalyzer.expressionEvaluation(state, pid, arguments[1], false).right);
			msg.append("\n    ");
			msg.append(symbolicAnalyzer.symbolicExpressionToString(sourceRight, state, arguments[1].getExpressionType(),
					right));
			this.errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
					CIVLProperty.DEREFERENCE, msg.toString());
			throw new UnsatisfiablePathConditionException();
		} else {
			SymbolicExpression rightValue;
			CIVLType objTypeLeft = symbolicAnalyzer.civlTypeOfObjByPointer(sourceLeft, state, left);
			CIVLType objTypeRight = symbolicAnalyzer.civlTypeOfObjByPointer(sourceRight, state, right);
			SymbolicType dynObjTypeLeft = objTypeLeft.getDynamicType(universe);
			SymbolicType dynObjTypeRight = objTypeRight.getDynamicType(universe);
			if (!dynObjTypeLeft.equals(dynObjTypeRight)) {
				StringBuffer msg = new StringBuffer();

				msg.append(
						"the objects pointed to by the two given pointers of $copy() " + "must have the same type.\n");
				msg.append("first argument:\n");
				msg.append("    ");
				msg.append(arguments[0]);
				msg.append("\n    ");
				msg.append(symbolicAnalyzer.expressionEvaluation(state, pid, arguments[0], false).right);
				msg.append("\n    type of the object: ");
				msg.append(objTypeLeft);
				msg.append("\nsecond argument:\n");
				msg.append("    ");
				msg.append(arguments[1]);
				msg.append("\n    ");
				msg.append(symbolicAnalyzer.expressionEvaluation(state, pid, arguments[1], false).right);
				msg.append("\n    type of the object: ");
				msg.append(objTypeRight);
				this.errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
						CIVLProperty.DEREFERENCE, msg.toString());
				throw new UnsatisfiablePathConditionException();
			}
			eval = evaluator.dereference(sourceRight, state, pid, process, right, false, false);
			state = eval.state;
			rightValue = eval.value;
			state = primaryExecutor.assign(source, state, pid, left, rightValue);
		}
		return new Evaluation(state, null);
	}

	/**
	 * are the object pointed to equal?
	 * 
	 * _Bool $equals(void *x, void *y);
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
	private Evaluation executeEquals(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression first, second, rhs;
		Evaluation eval = evaluator.dereference(arguments[0].getSource(), state, pid, process, argumentValues[0], false,
				true);
		int invalidArg = -1;

		state = eval.state;
		first = eval.value;
		eval = evaluator.dereference(arguments[1].getSource(), state, pid, process, argumentValues[1], false, true);
		state = eval.state;
		second = eval.value;
		if (!symbolicUtil.isInitialized(first))
			invalidArg = 0;
		else if (!symbolicUtil.isInitialized(second))
			invalidArg = 1;
		if (invalidArg != -1) {
			SymbolicExpression invalidValue = invalidArg == 0 ? first : second;

			if (civlConfig.isPropertyToggled(CIVLProperty.UNDEFINED_VALUE)) {
				this.errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
						CIVLProperty.UNDEFINED_VALUE,
						"the object that " + arguments[invalidArg] + " points to is undefined, which has the value "
								+ symbolicAnalyzer.symbolicExpressionToString(arguments[invalidArg].getSource(), state,
										null, invalidValue));
			}
			// recovery:
			rhs = this.falseValue;
		} else
			rhs = universe.equals(first, second);
		return new Evaluation(state, rhs);
	}

	/**
	 * Executing an assertion that objects pointed by two pointers are equal. The
	 * statement will have such a form:<br>
	 * <code>void $assert_equals(void *x, void *y, ...);</code>
	 * 
	 * @param state          The current state
	 * @param pid            The PID of the process
	 * @param process        The identifier string of the process
	 * @param arguments      Expressions of arguments
	 * @param argumentValues Symbolic expressions of arguments
	 * @param source         CIVL source of the statement
	 * @return the new state after executing the statement
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeAssertEquals(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression firstPtr, secondPtr;
		SymbolicExpression first, second;
		BooleanExpression claim;
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		ResultType resultType;
		Evaluation eval;
		boolean firstPtrDefined, secPtrDefined, firstInit, secondInit;

		firstPtrDefined = secPtrDefined = true;
		firstInit = secondInit = true;
		firstPtr = argumentValues[0];
		secondPtr = argumentValues[1];
		if (firstPtr.operator() != SymbolicOperator.TUPLE)
			firstPtrDefined = false;
		if (secondPtr.operator() != SymbolicOperator.TUPLE)
			secPtrDefined = false;
		if ((!firstPtrDefined || !secPtrDefined)) {
			String msg = new String();

			if (!firstPtrDefined)
				msg += symbolicAnalyzer.symbolicExpressionToString(arguments[0].getSource(), state,
						arguments[0].getExpressionType(), firstPtr);
			if (!secPtrDefined) {
				msg += (!firstPtrDefined) ? ", " : "";
				msg += symbolicAnalyzer.symbolicExpressionToString(arguments[1].getSource(), state,
						arguments[1].getExpressionType(), secondPtr);
			}
			errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
					CIVLProperty.DEREFERENCE, "Attempt to dereference a invalid pointer:" + msg);
			return new Evaluation(state, null);
		}
		eval = evaluator.dereference(arguments[0].getSource(), state, pid, process, argumentValues[0], false, true);
		state = eval.state;
		first = eval.value;
		eval = evaluator.dereference(arguments[1].getSource(), state, pid, process, argumentValues[1], false, true);
		state = eval.state;
		second = eval.value;
		if (!symbolicUtil.isInitialized(first))
			firstInit = false;
		if (!symbolicUtil.isInitialized(second))
			secondInit = false;
		if (!firstInit || !secondInit) {
			String ptrMsg = new String();
			String objMsg = new String();

			if (!firstInit) {
				ptrMsg += symbolicAnalyzer.symbolicExpressionToString(arguments[0].getSource(), state, null, firstPtr);
				objMsg += symbolicAnalyzer.symbolicExpressionToString(arguments[0].getSource(), state, null, first);
			}
			if (!secondInit) {
				String comma = (!firstInit) ? ", " : "";

				ptrMsg += comma;
				objMsg += comma;
				ptrMsg += symbolicAnalyzer.symbolicExpressionToString(arguments[1].getSource(), state, null, secondPtr);
				objMsg += symbolicAnalyzer.symbolicExpressionToString(arguments[1].getSource(), state, null, second);
			}
			this.errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
					CIVLProperty.UNDEFINED_VALUE,
					"the object that " + ptrMsg + " points to is undefined, which has the value " + objMsg);
			return new Evaluation(state, null);
		}
		claim = universe.equals(first, second);
		if (universe.getShowProverQueries())
			universe.setQueryExplanation("checking pointer equality at " + source.getLocation());
		resultType = reasoner.valid(claim).getResultType();
		if (resultType != ResultType.YES && civlConfig.isPropertyToggled(CIVLProperty.ASSERTION_VIOLATION)) {
			StringBuilder message = new StringBuilder();
			String firstArg, secondArg;

			message.append("Context: ");
			message.append(reasoner.getReducedCollapsedContext());
			message.append("\nAssertion voilated: ");
			message.append("$equals(" + arguments[0] + ", " + arguments[1] + ")");
			message.append("\nEvaluation: \n          ");
			firstArg = this.symbolicAnalyzer.symbolicExpressionToString(arguments[0].getSource(), state,
					arguments[0].getExpressionType(), argumentValues[0]);
			message.append(arguments[0].toString() + "=" + firstArg);
			message.append("\n          ");
			secondArg = this.symbolicAnalyzer.symbolicExpressionToString(arguments[1].getSource(), state,
					arguments[1].getExpressionType(), argumentValues[1]);
			message.append(arguments[1].toString() + "=" + secondArg);
			message.append("\nResult: \n          ");
			message.append(firstArg.substring(1) + "="
					+ this.symbolicAnalyzer.symbolicExpressionToString(arguments[0].getSource(), state,
							((CIVLPointerType) arguments[0].getExpressionType()).baseType(), first));
			message.append("\n          ");
			message.append(secondArg.substring(1) + "="
					+ this.symbolicAnalyzer.symbolicExpressionToString(arguments[1].getSource(), state,
							((CIVLPointerType) arguments[1].getExpressionType()).baseType(), second));
			state = this.reportAssertionFailure(state, pid, process, resultType, message.toString(), arguments,
					argumentValues, source, claim, 2);
		}
		return new Evaluation(state, null);
	}

	/**
	 * Translates a pointer into one object to a pointer into a different object
	 * with similar structure.
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
	private Evaluation executeTranslatePointer(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression pointer = argumentValues[0];
		SymbolicExpression objPtr = argumentValues[1];
		SymbolicExpression result;

		if (symbolicUtil.isNullPointer(pointer) || symbolicUtil.isNullPointer(objPtr)) {
			result = symbolicUtil.nullPointer();
		} else {
			ReferenceExpression reference = this.symbolicUtil.referenceOfPointer(pointer);
			SymbolicExpression newPointer = symbolicUtil.extendPointer(objPtr, reference);
			CIVLSource objSource = arguments[1].getSource();
			int dyscopeId = stateFactory.getDyscopeId(symbolicUtil.getScopeValue(newPointer));
			int vid = symbolicUtil.getVariableId(objSource, newPointer);
			SymbolicExpression objValue = state.getVariableValue(dyscopeId, vid);

			reference = (ReferenceExpression) symbolicUtil.getSymRef(newPointer);
			if (!symbolicUtil.isValidRefOf(reference, objValue.type())) {
				this.errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
						CIVLProperty.OTHER,
						"the second argument of $translate_ptr() "
								+ symbolicAnalyzer.symbolicExpressionToString(objSource, state, null, objPtr)
								+ " doesn't have a compatible type hierarchy as the first argument " + symbolicAnalyzer
										.symbolicExpressionToString(arguments[0].getSource(), state, null, pointer));
				throw new UnsatisfiablePathConditionException();
			}
			result = newPointer;
		}
		return new Evaluation(state, result);
	}

	/**
	 * Executes the
	 * <code>void * $pointer_add(const void *ptr, int offset, int type_size);</code>
	 * system function. The resulting pointer should point to the byte that is
	 * {@code offset*type_size} to the right of {@code ptr}, i.e.,
	 * {@code (char*)ptr + (offset*typesize)}.
	 * 
	 * The current implementation is very limited. It requires that ptr points to
	 * some element in a (possibly multidimensional) array. I.e. an element of type
	 * T in an array of type T[n1]...[nk].
	 * 
	 * @param state          The current state
	 * @param pid            The PID of the process
	 * @param process        The string identifier of the process
	 * @param arguments      {@link Expression} of arguments
	 * @param argumentValues {@link SymbolicExpression} of arguments
	 * @param source         CIVL source of the statement
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executePointer_add(State state, int pid, String process, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression ptr = argumentValues[0];
		NumericExpression offset = (NumericExpression) argumentValues[1];
		NumericExpression type_size = (NumericExpression) argumentValues[2];
		// the following is the offset in bytes, i.e., offset*typesize:
		NumericExpression offsetBytes = universe.multiply(offset, type_size);

		if (!ptr.operator().equals(SymbolicOperator.TUPLE)) {
			errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
					CIVLProperty.DEREFERENCE, "$pointer_add() doesn't accept an invalid pointer:"
							+ symbolicAnalyzer.symbolicExpressionToString(source, state, null, ptr));
			throw new UnsatisfiablePathConditionException();
		}
		// the following gets the "primitive" type T and its size:
		CIVLType primitiveTypePointedStatic = symbolicAnalyzer.getArrayBaseType(state, arguments[0].getSource(), ptr);
		SymbolicType primitiveTypePointed = primitiveTypePointedStatic.getDynamicType(universe);
		NumericExpression ptr_primType_size = typeFactory.sizeofDynamicType(primitiveTypePointed);

		// check that offsetBytes is divisible by sizeof(T):
		BooleanExpression claim = universe.divides(ptr_primType_size, offsetBytes);
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		if (universe.getShowProverQueries())
			universe.setQueryExplanation("checking $pointer_add offset alignment at " + source.getLocation());
		ResultType resultType = reasoner.valid(claim).getResultType();
		if (civlConfig.isPropertyToggled(CIVLProperty.POINTER) && !resultType.equals(ResultType.YES)) {
			state = this.errorLogger.logError(source, state, pid, this.symbolicAnalyzer.stateInformation(state), claim,
					resultType, CIVLProperty.POINTER,
					"the primitive type of the object pointed by input pointer:" + primitiveTypePointed + " must be"
							+ " consistent with the size of the" + " primitive type specified at the third argument: "
							+ type_size);
		}
		// get the offset measured in units of T:
		NumericExpression offsetPrim = universe.divide(offsetBytes, ptr_primType_size);
		// compute the pointer to the new element of the array:
		return evaluator.arrayElementReferenceAdd(state, pid, ptr, offsetPrim, source).left;
	}
}
