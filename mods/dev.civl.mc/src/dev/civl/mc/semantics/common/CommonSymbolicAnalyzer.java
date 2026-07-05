package dev.civl.mc.semantics.common;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.civl.abc.ast.node.IF.acsl.ExtendedQuantifiedExpressionNode.ExtendedQuantifier;
import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.log.IF.CIVLErrorLogger;
import dev.civl.mc.model.IF.CIVLFunction;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLProperty;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLTypeFactory;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.model.IF.ModelConfiguration;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.expression.AbstractFunctionCallExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression.BINARY_OPERATOR;
import dev.civl.mc.model.IF.expression.CastExpression;
import dev.civl.mc.model.IF.expression.ConditionalExpression;
import dev.civl.mc.model.IF.expression.DereferenceExpression;
import dev.civl.mc.model.IF.expression.DomainGuardExpression;
import dev.civl.mc.model.IF.expression.DotExpression;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.Expression.ExpressionKind;
import dev.civl.mc.model.IF.expression.ExtendedQuantifiedExpression;
import dev.civl.mc.model.IF.expression.FunctionCallExpression;
import dev.civl.mc.model.IF.expression.FunctionIdentifierExpression;
import dev.civl.mc.model.IF.expression.LHSExpression;
import dev.civl.mc.model.IF.expression.LHSExpression.LHSExpressionKind;
import dev.civl.mc.model.IF.expression.SubscriptExpression;
import dev.civl.mc.model.IF.expression.UnaryExpression;
import dev.civl.mc.model.IF.expression.VariableExpression;
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
import dev.civl.mc.model.IF.statement.UpdateStatement;
import dev.civl.mc.model.IF.type.CIVLArrayType;
import dev.civl.mc.model.IF.type.CIVLHeapType;
import dev.civl.mc.model.IF.type.CIVLMemType;
import dev.civl.mc.model.IF.type.CIVLStructOrUnionType;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.type.CIVLType.TypeKind;
import dev.civl.mc.model.IF.type.StructOrUnionField;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.semantics.IF.Evaluation;
import dev.civl.mc.semantics.IF.Evaluator;
import dev.civl.mc.semantics.IF.SymbolicAnalyzer;
import dev.civl.mc.state.IF.DynamicScope;
import dev.civl.mc.state.IF.MemoryUnit;
import dev.civl.mc.state.IF.ProcessState;
import dev.civl.mc.state.IF.StackEntry;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.StateFactory;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.mc.state.common.immutable.ImmutableDynamicScope;
import dev.civl.mc.util.IF.Pair;
import dev.civl.mc.util.IF.Triple;
import dev.civl.sarl.IF.Reasoner;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.ArrayElementReference;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NTReferenceExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.NumericSymbolicConstant;
import dev.civl.sarl.IF.expr.OffsetReference;
import dev.civl.sarl.IF.expr.ReferenceExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;
import dev.civl.sarl.IF.expr.TupleComponentReference;
import dev.civl.sarl.IF.expr.UnionMemberReference;
import dev.civl.sarl.IF.number.IntegerNumber;
import dev.civl.sarl.IF.object.IntObject;
import dev.civl.sarl.IF.object.SymbolicObject;
import dev.civl.sarl.IF.object.SymbolicObject.SymbolicObjectKind;
import dev.civl.sarl.IF.object.SymbolicSequence;
import dev.civl.sarl.IF.type.SymbolicArrayType;
import dev.civl.sarl.IF.type.SymbolicCompleteArrayType;
import dev.civl.sarl.IF.type.SymbolicTupleType;
import dev.civl.sarl.IF.type.SymbolicType;
import dev.civl.sarl.IF.type.SymbolicType.SymbolicTypeKind;
import dev.civl.sarl.IF.type.SymbolicUnionType;

public class CommonSymbolicAnalyzer implements SymbolicAnalyzer {

	/* *************************** Instance Fields ************************* */

	/**
	 * The size threshold that decides if a symbolic expression is printed in a full
	 * way or a compressed way.
	 */
	private static int PRINT_SIZE_THRESHOLD = 700;

	private final String SEF_START = "[";
	private final String SEF = ":=";
	private final String SEF_END = "]";

	private CIVLErrorLogger errorLogger;

	private ModelFactory modelFactory;

	private CIVLTypeFactory typeFactory;

	/**
	 * The pointer value is a triple <s,v,r> where s identifies the dynamic scope, v
	 * identifies the variable within that scope, and r identifies a point within
	 * that variable. The type of s is scopeType, which is just a tuple wrapping a
	 * single integer which is the dynamic scope ID number. The type of v is
	 * integer; it is the (static) variable ID number for the variable in its scope.
	 * The type of r is ReferenceExpression from SARL.
	 */
	private SymbolicTupleType pointerType;

	private SymbolicUtility symbolicUtil;

	private SymbolicUniverse universe;

	private NumericExpression zero;

	private IntObject oneObj;

	private CIVLHeapType heapType;

	private SymbolicType dynamicHeapType;

	private SymbolicTupleType procType;

	private SymbolicType scopeType;

	private SymbolicTupleType functionPointerType;
	private Evaluator evaluator;

	@SuppressWarnings("unused")
	private CIVLConfiguration config;

	/* ***************************** Constructors ************************** */

	public CommonSymbolicAnalyzer(CIVLConfiguration config, CIVLErrorLogger errorLogger, SymbolicUniverse universe,
			ModelFactory modelFactory, SymbolicUtility symbolicUtil) {
		this.universe = universe;
		this.modelFactory = modelFactory;
		this.typeFactory = modelFactory.typeFactory();
		this.symbolicUtil = symbolicUtil;
		pointerType = typeFactory.pointerSymbolicType();
		this.heapType = typeFactory.heapType();
		this.dynamicHeapType = typeFactory.heapSymbolicType();
		this.procType = this.typeFactory.processSymbolicType();
		this.scopeType = this.typeFactory.scopeSymbolicType();
		this.functionPointerType = typeFactory.functionPointerSymbolicType();
		this.oneObj = universe.intObject(1);
		// this.twoObj = (IntObject) universe.canonic(universe.intObject(2));
		zero = universe.integer(0);
		this.config = config;
		this.errorLogger = errorLogger;
	}

	/* ******************** Methods From SymbolicAnalyzer ****************** */

	@Override
	public SymbolicUniverse getUniverse() {
		return universe;
	}

	@Override
	public ReferenceExpression getLeafNodeReference(State state, SymbolicExpression pointer, CIVLSource source) {
		CIVLType objType;
		ReferenceExpression ref = symbolicUtil.getSymRef(pointer);

		objType = civlTypeOfObjByPointer(source, state, pointer);
		while (objType.isArrayType()) {
			ref = universe.arrayElementReference(ref, zero);
			objType = ((CIVLArrayType) objType).elementType();
		}
		return ref;
	}

	@Override
	public SymbolicExpression getSubArray(State state, int pid, SymbolicExpression array, NumericExpression startIndex,
			NumericExpression endIndex, CIVLSource source) throws UnsatisfiablePathConditionException {
		// if startIndex is zero and endIndex is length, return array
		// verify startIndex >=0 and endIndex<= Length
		// if startIndex==endIndex return emptyArray
		// else if startIndex and endIndex are concrete, create concrete array
		// else need array lambdas or subsequence operation: todo
		BooleanExpression pathCondition = state.getPathCondition(universe);
		Reasoner reasoner = universe.reasoner(pathCondition);
		NumericExpression length = universe.length(array);
		SymbolicArrayType arrayType = (SymbolicArrayType) array.type();
		SymbolicType elementType = arrayType.elementType();

		if (reasoner.isValid(universe.equals(zero, startIndex)) && reasoner.isValid(universe.equals(length, endIndex)))
			return array;
		else {
			BooleanExpression claim = universe.lessThanEquals(zero, startIndex);
			ResultType valid = reasoner.valid(claim).getResultType();

			if (valid != ResultType.YES) {
				state = errorLogger.logError(source, state, pid, this.stateInformation(state), claim, valid,
						CIVLProperty.OUT_OF_BOUNDS, "negative start index");
				pathCondition = state.getPathCondition(universe);
				reasoner = universe.reasoner(pathCondition);
			}
			claim = universe.lessThanEquals(endIndex, length);
			valid = reasoner.valid(claim).getResultType();
			if (valid != ResultType.YES) {
				state = errorLogger.logError(source, state, pid, stateInformation(state), claim, valid,
						CIVLProperty.OUT_OF_BOUNDS,
						"Index exceeds length of array: " + endIndex + "\nArray type: " + array.type());
				pathCondition = state.getPathCondition(universe);
				reasoner = universe.reasoner(pathCondition);
			}
			claim = universe.lessThanEquals(startIndex, endIndex);
			valid = reasoner.valid(claim).getResultType();
			if (valid != ResultType.YES) {
				state = errorLogger.logError(source, state, pid, this.stateInformation(state), claim, valid,
						CIVLProperty.OUT_OF_BOUNDS, "start index greater than end index");
				pathCondition = state.getPathCondition(universe);
				reasoner = universe.reasoner(pathCondition);
			}
			if (reasoner.isValid(universe.equals(startIndex, endIndex))) {
				return universe.emptyArray(elementType);
			} else {
				IntegerNumber concreteStart = (IntegerNumber) reasoner.extractNumber(startIndex);
				IntegerNumber concreteEnd = (IntegerNumber) reasoner.extractNumber(endIndex);

				if (concreteStart != null && concreteEnd != null) {
					int startInt = concreteStart.intValue();
					int endInt = concreteEnd.intValue();
					LinkedList<SymbolicExpression> valueList = new LinkedList<SymbolicExpression>();

					for (int i = startInt; i < endInt; i++)
						valueList.add(universe.arrayRead(array, universe.integer(i)));
					return universe.array(elementType, valueList);
				} else {
					NumericExpression subLength = universe.subtract(endIndex, startIndex);
					SymbolicCompleteArrayType subArrayType = universe.arrayType(elementType, subLength);
					NumericSymbolicConstant index = (NumericSymbolicConstant) universe
							.symbolicConstant(universe.stringObject("i"), universe.integerType());
					SymbolicExpression subArrayFunction = universe.lambda(index,
							universe.arrayRead(array, universe.add(startIndex, index)));

					return universe.arrayLambda(subArrayType, subArrayFunction);

				}
			}
		}
	}

	@Override
	public StringBuffer stateToString(State state) {
		return stateToString(state, -1, -1);
	}

	@Override
	public StringBuffer stateToString(State state, int lastSavedState, int sequenceId) {
		int numScopes = state.numDyscopes();
		int numProcs = state.numProcs();
		StringBuffer result = new StringBuffer();

		result.append("State ");
		if (lastSavedState != -1 && sequenceId != -1)
			result.append(lastSavedState + "." + sequenceId + " " + state);
		else if (lastSavedState != -1)
			result.append(lastSavedState + " " + state);
		else
			result.append(state.identifier());
		result.append("\n");
		for (int pid = 0; pid < numProcs; pid++) {
			ProcessState process = state.getProcessState(pid);
			if (process == null)
				continue;
			BooleanExpression[] ppc = process.getPartialPathConditions();
			if (ppc.length == 0)
				continue;
			result.append("| Process " + pid + " partial path conditions\n");
			for (int i = ppc.length - 1; i >= 0; i--) {
				result.append("| | entry " + i);
				result.append(this.pathconditionToString(null, state, "| | | ", ppc[i]));
				result.append("\n");
			}
		}
		result.append("| Path condition");
		result.append(this.pathconditionToString(null, state, "| | ", state.getPermanentPathCondition()));
		// result.append("| | "
		// + this.symbolicExpressionToString(null, state, null,
		// state.getPathCondition()));
		result.append("\n");
		result.append("| Dynamic scopes\n");
		for (int i = 0; i < numScopes; i++) {
			ImmutableDynamicScope dyscope = (ImmutableDynamicScope) state.getDyscope(i);

			if (dyscope == null)
				result.append("| | dyscope - (id=" + i + "): null\n");
			else
				result.append(dynamicScopeToString(state, dyscope, "" + i, "| | "));
		}
		result.append("| Process states\n");
		for (int pid = 0; pid < numProcs; pid++) {
			ProcessState process = state.getProcessState(pid);

			if (process == null)
				result.append("| | process - (id=" + pid + "): null\n");
			else
				result.append(process.toStringBuffer("| | "));
		}
		return result;
	}

	@Override
	public String symbolicExpressionToString(CIVLSource source, State state, CIVLType type,
			SymbolicExpression symbolicExpression) {
		return this.symbolicExpressionToString(source, state, type, symbolicExpression, "", "| ").toString();
	}

	@Override
	public CIVLType civlTypeOfObjByPointer(CIVLSource soruce, State state, SymbolicExpression pointer) {
		ReferenceExpression reference = this.symbolicUtil.getSymRef(pointer);
		int dyscopeId = evaluator.stateFactory().getDyscopeId(symbolicUtil.getScopeValue(pointer));
		int vid = symbolicUtil.getVariableId(soruce, pointer);
		Scope lexicalScope;
		CIVLType varType;

		if (dyscopeId == ModelConfiguration.DYNAMIC_CONSTANT_SCOPE)
			lexicalScope = this.modelFactory.model().staticConstantScope();
		else
			lexicalScope = state.getDyscope(dyscopeId).lexicalScope();
		varType = lexicalScope.variable(vid).type();
		return typeOfObjByRef(varType, reference);
	}

	@Override
	public SymbolicType dynamicTypeOfObjByPointer(CIVLSource soruce, State state, SymbolicExpression pointer) {
		ReferenceExpression reference = symbolicUtil.getSymRef(pointer);
		int dyscopeId = evaluator.stateFactory().getDyscopeId(symbolicUtil.getScopeValue(pointer));
		int vid = symbolicUtil.getVariableId(soruce, pointer);
		SymbolicExpression varValue;

		if (dyscopeId == ModelConfiguration.DYNAMIC_CONSTANT_SCOPE) {
			varValue = modelFactory.model().staticConstantScope().variable(vid).constantValue();
		} else
			varValue = state.getDyscope(dyscopeId).getValue(vid);
		return universe.dereference(varValue, reference).type();
	}

	@Override
	public CIVLType getArrayBaseType(State state, CIVLSource source, SymbolicExpression arrayPtr) {
		CIVLType type = this.civlTypeOfObjByPointer(source, state, arrayPtr);

		while (type instanceof CIVLArrayType)
			type = ((CIVLArrayType) type).elementType();
		return type;
	}

	/* *************************** Private Methods ************************* */

	/**
	 * accumulates the operator opString to every operand in the following format
	 * opString = " " + opString + " ";
	 * 
	 * @param buffer   string buffer to which computed result should be appended
	 * @param opString the string representation of the operator, e.g. "+"
	 * @param operands collection of Symbolic Objects
	 */
	private void accumulate(CIVLSource source, State state, StringBuffer buffer, String opString,
			SymbolicSequence<?> operands) {
		boolean first = true;

		for (SymbolicExpression arg : operands) {
			if (first)
				first = false;
			else
				buffer.append(opString);
			buffer.append(symbolicExpressionToString(source, state, null, arg, true, "", ""));
		}
	}

	/**
	 * Place parentheses around the string buffer.
	 * 
	 * @param buffer a string buffer
	 */
	private void atomize(StringBuffer buffer) {
		buffer.insert(0, '(');
		buffer.append(')');
	}

	/**
	 * Prints a dyscope to a given print stream.
	 * 
	 * @param out     The print stream to be used for printing.
	 * @param state   The state that the dyscope belongs to.
	 * @param dyscope The dyscope to be printed.
	 * @param id      The ID of the dyscope.
	 * @param prefix  The prefix for printing.
	 */
	private StringBuffer dynamicScopeToString(State state, DynamicScope dyscope, String id, String prefix) {
		Scope lexicalScope = dyscope.lexicalScope();
		int numVars = lexicalScope.numVariables();
		// BitSet reachers = dyscope.getReachers();
		// int bitSetLength = reachers.length();
		// boolean first = true;
		StringBuffer result = new StringBuffer();
		String parentString;
		int parentId = dyscope.getParent();

		if (parentId < 0)
			parentString = "NULL";
		else
			parentString = "d" + dyscope.getParent();
		result.append(prefix + "dyscope d" + id + " (parent=" + parentString + ", static=" + lexicalScope.id() + ")\n");
		result.append(prefix + "| variables\n");
		for (int i = 0; i < numVars; i++) {
			Variable variable = lexicalScope.variable(i);
			SymbolicExpression value = dyscope.getValue(i);
			String varName = variable.name().name();

			if (varName.equals(ModelConfiguration.HEAP_VAR) && value.isNull()) {
				continue;
			} else if (varName.equals(ModelConfiguration.TIME_COUNT_VARIABLE)) {
				continue;
			} else if (varName.equals(ModelConfiguration.ATOMIC_LOCK_VARIABLE_INDEX)
					&& (value.isNull() || !modelFactory.isPocessIdDefined(modelFactory.getProcessId(value)))) {
				continue;
			}
			result.append(prefix + "| | " + variable.name());
			// if (variable.type().areSubtypesScalar())
			result.append(" = ");
			result.append(symbolicExpressionToString(variable.getSource(), state, variable.type(), value,
					prefix + "| | ", "| "));
			result.append("\n");
		}
		return result;
	}

	/**
	 * Obtains the string representation of a symbolic expression which is a
	 * pointer.
	 * 
	 * @param source  The source code element of the symbolic expression
	 * @param state   The state that the given symbolic expression belongs to
	 * @param pointer The symbolic expression that is to be evaluated
	 * @return the string representation of a symbolic expression which is a pointer
	 */
	private StringBuffer functionPointerValueToString(CIVLSource source, State state, SymbolicExpression pointer) {
		StringBuffer result = new StringBuffer();

		if (pointer.operator() == SymbolicOperator.NULL || pointer.operator() != SymbolicOperator.TUPLE)
			return result.append(pointer);
		else {
			int dyscopeId = evaluator.stateFactory().getDyscopeId(symbolicUtil.getScopeValue(pointer));

			if (dyscopeId < 0)
				result.append("UNDEFINED");
			else {
				DynamicScope dyScope = state.getDyscope(dyscopeId);
				NumericExpression funcIdExpr = (NumericExpression) universe.tupleRead(pointer, oneObj);
				int fid = symbolicUtil.extractInt(source, funcIdExpr);
				CIVLFunction function = dyScope.lexicalScope().getFunction(fid);

				result.append("&<");
				result.append("d" + dyscopeId);
				result.append(">");
				result.append(function.toString());
			}
			return result;
		}
	}

	/**
	 * Obtains the string representation of a reference to a heap object or part of
	 * a heap object.
	 * 
	 * @param source    The source code element of the reference expression.
	 * @param dyscopeId The dynamic scope ID that the heap belongs to.
	 * @param type      The static type of the expression being referenced.
	 * @param reference The reference expression, could be:
	 *                  <ol>
	 *                  <li>identity reference</li>
	 *                  <li>array element reference</li>
	 *                  <li>tuple component reference</li>
	 *                  <li>union member reference</li>
	 *                  </ol>
	 * @return the string representation of a reference to a heap object or part of
	 *         a heap object.
	 */
	private Triple<Integer, CIVLType, String> heapObjectReferenceToString(CIVLSource source, int dyscopeId,
			CIVLType type, ReferenceExpression reference) {
		StringBuffer result = new StringBuffer();

		if (reference.isIdentityReference()) {
			result.append("&<d");
			result.append(dyscopeId);
			result.append(">");
			result.append("heap.malloc");
			return new Triple<>(0, type, result.toString());
		} else if (reference.isArrayElementReference()) {
			ArrayElementReference arrayEleRef = (ArrayElementReference) reference;
			Triple<Integer, CIVLType, String> parentResult = heapObjectReferenceToString(source, dyscopeId, type,
					arrayEleRef.getParent());
			NumericExpression index = arrayEleRef.getIndex();

			switch (parentResult.first) {
			case 0:
				throw new CIVLInternalException("Unreachable", source);
			case 1:
				result.append(parentResult.third);
				result.append(index);
				result.append(']');
				return new Triple<>(2, parentResult.second, result.toString());
			case 2:
				result.append(parentResult.third);
				result.append('[');
				result.append(index);
				result.append(']');
				return new Triple<>(-1, parentResult.second, result.toString());
			default:
				CIVLType arrayEleType = ((CIVLArrayType) parentResult.second).elementType();

				result.append(parentResult.third);
				result.append('[');
				result.append(index);
				result.append(']');
				return new Triple<>(-1, arrayEleType, result.toString());
			}
		} else {
			ReferenceExpression parent;
			IntObject index;
			Triple<Integer, CIVLType, String> parentResult;

			if (reference.isTupleComponentReference()) {
				TupleComponentReference tupleCompRef = (TupleComponentReference) reference;

				parent = tupleCompRef.getParent();
				index = tupleCompRef.getIndex();
			} else {
				UnionMemberReference unionMemRef = (UnionMemberReference) reference;

				parent = unionMemRef.getParent();
				index = unionMemRef.getIndex();
			}
			parentResult = heapObjectReferenceToString(source, dyscopeId, type, parent);

			switch (parentResult.first) {
			case 0:
				CIVLHeapType heapType = (CIVLHeapType) parentResult.second;
				int indexId = index.getInt();
				CIVLType heapObjType = heapType.getMalloc(indexId).getStaticElementType();

				result.append(parentResult.third);
				result.append(index.getInt());
				result.append('[');
				return new Triple<>(1, heapObjType, result.toString());
			case 1:
			case 2:
				throw new CIVLInternalException("Unreachable", source);
			default:
				CIVLStructOrUnionType structOrUnionType = (CIVLStructOrUnionType) parentResult.second;
				StructOrUnionField field = structOrUnionType.getField(index.getInt());

				result.append(parentResult.third);
				result.append('.');
				result.append(field.name());
				return new Triple<>(-1, field.type(), result.toString());
			}
		}
	}

	/**
	 * Computes string representation of a binary operator expression that may take
	 * either one argument (a list of expressions) or two arguments.
	 * 
	 * @param buffer        string buffer to which computed result should be
	 *                      appended
	 * @param opString      the string representation of the operator, e.g. "+"
	 * @param atomizeArgs   should each argument be atomized (surrounded by parens
	 *                      if necessary)?
	 * @param atomizeResult should the final result be atomized?
	 */
	private void processFlexibleBinaryNew(CIVLSource source, State state, SymbolicExpression symbolicExpression,
			StringBuffer buffer, String opString, boolean atomizeArgs, boolean atomizeResult) {
		int numArgs = symbolicExpression.numArguments();
		boolean first = true;

		for (int i = 0; i < numArgs; i++) {
			SymbolicObject arg = symbolicExpression.argument(i);
			String argString;

			if (arg instanceof SymbolicExpression)
				argString = this.symbolicExpressionToString(source, state, null, (SymbolicExpression) arg, true, "", "")
						.toString();
			else
				argString = symbolicExpression.argument(i).toStringBuffer(atomizeArgs).toString();
			if (!first && (!opString.equals("+") || !argString.startsWith("-"))) {
				buffer.append(opString);
			}
			buffer.append(argString);
			if (first)
				first = false;
		}
		if (atomizeResult) {
			buffer.insert(0, '(');
			buffer.append(')');
		}
	}

	/**
	 * Computes string representation of a binary operator expression
	 * 
	 * @param buffer      string buffer to which computed result should be appended
	 * @param opString    the string representation of the operator, e.g. "+"
	 * @param arg0        object to be represented
	 * @param arg1        object to be represented
	 * @param atomizeArgs should each argument be atomized (surrounded by parens if
	 *                    necessary)?
	 */
	private void processBinary(StringBuffer buffer, String opString, SymbolicObject arg0, SymbolicObject arg1,
			boolean atomizeArgs) {
		buffer.append(arg0.toStringBuffer(atomizeArgs));
		buffer.append(opString);
		buffer.append(arg1.toStringBuffer(atomizeArgs));
	}

	/**
	 * Computes string representation of a binary operator expression that may take
	 * either one argument (a list of expressions) or two arguments.
	 * 
	 * @param buffer        string buffer to which computed result should be
	 *                      appended
	 * @param opString      the string representation of the operator, e.g. "+"
	 * @param atomizeArgs   should each argument be atomized (surrounded by parens
	 *                      if necessary)?
	 * @param atomizeResult should the final result be atomized?
	 */
	private void processFlexibleBinary(CIVLSource source, State state, SymbolicExpression symbolicExpression,
			StringBuffer buffer, String opString, boolean atomizeArgs, boolean atomizeResult) {
		int numArgs = symbolicExpression.numArguments();

		if (numArgs == 1)
			accumulate(source, state, buffer, opString, (SymbolicSequence<?>) symbolicExpression.argument(0));
		else
			processBinary(buffer, opString, symbolicExpression.argument(0), symbolicExpression.argument(1), true);
		if (atomizeResult) {
			buffer.insert(0, '(');
			buffer.append(')');
		}
	}

	/**
	 * Obtains the string representation from a reference expression.
	 * 
	 * @param source    The source code element of the reference expression.
	 * @param type      The type of the expression being referenced.
	 * @param reference The reference expression whose string representation is to
	 *                  be obtained.
	 * @return The type of the remaining part, and the string representation of the
	 *         given reference expression.
	 */
	private Pair<CIVLType, String> referenceToString(CIVLSource source, CIVLType type, ReferenceExpression reference) {
		StringBuffer result = new StringBuffer();

		if (reference.isIdentityReference())
			return new Pair<>(type, result.toString());
		if (reference.isArrayElementReference()) {
			ArrayElementReference arrayEleRef = (ArrayElementReference) reference;
			Pair<CIVLType, String> parentResult = this.referenceToString(source, type, arrayEleRef.getParent());
			String parent = parentResult.right;
			CIVLType arrayEleType = ((CIVLArrayType) parentResult.left).elementType();
			NumericExpression index = arrayEleRef.getIndex();

			result.append(parent);
			result.append('[');
			result.append(index);
			result.append(']');
			return new Pair<>(arrayEleType, result.toString());
		} else if (reference.isTupleComponentReference()) {
			TupleComponentReference tupleComponentRef = (TupleComponentReference) reference;
			IntObject index = tupleComponentRef.getIndex();
			Pair<CIVLType, String> parentResult = this.referenceToString(source, type, tupleComponentRef.getParent());
			String parent = parentResult.right;

			if (!parentResult.left.isStructType())
				return parentResult;

			CIVLStructOrUnionType structOrUnionType = (CIVLStructOrUnionType) parentResult.left;
			StructOrUnionField field = structOrUnionType.getField(index.getInt());

			result.append(parent);
			result.append('.');
			result.append(field.name());
			return new Pair<CIVLType, String>(field.type(), result.toString());
		} else if (reference.isUnionMemberReference()) {
			UnionMemberReference unionMemberRef = (UnionMemberReference) reference;
			IntObject index = unionMemberRef.getIndex();
			Pair<CIVLType, String> parentResult = this.referenceToString(source, type, unionMemberRef.getParent());
			String parent = parentResult.right;

			if (!parentResult.left.isStructType())
				return parentResult;

			CIVLStructOrUnionType structOrUnionType = (CIVLStructOrUnionType) parentResult.left;
			StructOrUnionField field = structOrUnionType.getField(index.getInt());

			result.append(parent);
			result.append('.');
			result.append(field.name());
			return new Pair<CIVLType, String>(field.type(), result.toString());
		} else if (reference.isOffsetReference()) {
			OffsetReference offsetRef = (OffsetReference) reference;
			NumericExpression offset = offsetRef.getOffset();
			Pair<CIVLType, String> parentResult = this.referenceToString(source, type, offsetRef.getParent());
			String parent = parentResult.right;

			result.append(parent);
			result.append('+');
			result.append(offset.atomString());
			return new Pair<CIVLType, String>(parentResult.left, result.toString());
		} else {
			throw new CIVLInternalException("Unreachable", source);
		}
	}

	/**
	 * Obtains the string representation of a symbolic expression which is a
	 * pointer.
	 * 
	 * @param source  The source code element of the symbolic expression
	 * @param state   The state that the given symbolic expression belongs to
	 * @param pointer The symbolic expression that is to be evaluated
	 * @return the string representation of a symbolic expression which is a pointer
	 */
	private StringBuffer pointerValueToString(CIVLSource source, State state, SymbolicExpression pointer) {
		StringBuffer result = new StringBuffer();

		if (pointer.operator() == SymbolicOperator.NULL || pointer.operator() != SymbolicOperator.TUPLE)
			result.append(pointer);
		else {
			SymbolicTupleType pointerType = (SymbolicTupleType) pointer.type();

			if (!pointerType.equals(this.pointerType)) {
				return this.symbolicExpressionToString(source, state, null, pointer, "", "");
			}
			return this.variableReferenceToString(state, source, false,
					evaluator.stateFactory().getDyscopeId(symbolicUtil.getScopeValue(pointer)),
					symbolicUtil.getVariableId(source, pointer), symbolicUtil.getSymRef(pointer));
		}
		return result;
	}

	private StringBuffer variableReferenceToString(State state, CIVLSource source, boolean isMu, int dyscopeId, int vid,
			ReferenceExpression reference) {
		StringBuffer result = new StringBuffer();

		if (dyscopeId == ModelConfiguration.NULL_POINTER_DYSCOPE && vid == ModelConfiguration.NULL_POINTER_VID) {
			result.append("(void*)0");
		} else if (dyscopeId == ModelConfiguration.DYNAMIC_CONSTANT_SCOPE) {
			result.append(this.stringLiteralToString(source,
					this.modelFactory.model().staticConstantScope().variable(vid).constantValue()));
		} else if (dyscopeId < 0)
			result.append("UNDEFINED");
		else {
			DynamicScope dyscope = state.getDyscope(dyscopeId);
			Variable variable = dyscope.lexicalScope().variable(vid);

			if (variable.type().equals(this.heapType)) {
				result.append(heapObjectReferenceToString(source, dyscopeId, this.heapType, reference).third);
			} else {
				if (variable.name().name().startsWith(ModelConfiguration.ANONYMOUS_VARIABLE_PREFIX)) {
					// this is an array literal
					SymbolicExpression objectValue = state.getVariableValue(dyscopeId, vid);
					CIVLArrayType arrayType = (CIVLArrayType) variable.type();

					if (arrayType.elementType().isCharType()) {
						result.append(stringLiteralToString(source, objectValue));
					} else {
						result.append(symbolicExpressionToString(source, state, arrayType, objectValue, false, "", ""));
					}
				} else {
					if (!isMu)
						result.append('&');
					result.append("<");
					result.append("d" + dyscopeId);
					result.append('>');
					result.append(variable.name());
					result.append(referenceToString(source, variable.type(), reference).right);
				}
			}
		}
		return result;
	}

	private StringBuffer stringLiteralToString(CIVLSource source, SymbolicExpression array) {
		StringBuffer result = new StringBuffer();

		result.append("\"");
		result.append(this.symbolicUtil.charArrayToString(source, array, 0, true));
		result.append("\"");
		return result;
	}

	private StringBuffer symbolicExpressionToString(CIVLSource source, State state, CIVLType type,
			SymbolicExpression symbolicExpression, String prefix, String separate) {
		return this.symbolicExpressionToString(source, state, type, symbolicExpression, false, prefix, separate);
	}

	/**
	 * <p>
	 * Obtains the string representation of a symbolic expression, making pointers
	 * represented in a user-friendly way.
	 * </p>
	 * If a pointer is pointing to
	 * <ul>
	 * <li>
	 * 
	 * <pre>
	 * a variable: & variable &lt;dyscope name>;
	 * e.g., int a = 9; int * p = &a;
	 * then the representation of p would be &a&lt;d0> assuming that the name of the dynamic scope of a is d0.
	 * </pre>
	 * 
	 * </li>
	 * <li>
	 * 
	 * <pre>
	 * an element of an array: &array<dyscope name>[index];
	 * e.g., int a[5]; int *p = &a[1];
	 * then the representation of p would be &a&lt;d0>[1] assuming that the name of the dynamic scope of a is d0.
	 * </pre>
	 * 
	 * </li>
	 * <li>
	 * 
	 * <pre>
	 * a field of a struct: &struct&lt;dyscope name>.field;
	 * e.g., typedef struct {int x; int y;} A; A s; int*p = &s.y;
	 * then the representation of p would be &a&lt;d0>.y assuming that the name of the dynamic scope of a is d0.
	 * </pre>
	 * 
	 * </li>
	 * <li>
	 * 
	 * <pre>
	 * a heap cell: heapObject&lt;dyscope name, malloc ID, number of malloc call>.
	 * </pre>
	 * 
	 * </li>
	 * </ul>
	 * 
	 * @param source             The source code element of the symbolic expression.
	 * @param state              The state where the given symbolic expression is
	 *                           evaluated from.
	 * @param symbolicExpression The symbolic expression whose string representation
	 *                           is to be obtained.
	 * @param atomize            True iff this is an atomic symbolic expression.
	 * @return The string representation of the given symbolic expression
	 * @throws UnsatisfiablePathConditionException
	 */
	// @SuppressWarnings("unchecked")
	private StringBuffer symbolicExpressionToString(CIVLSource source, State state, CIVLType civlType,
			SymbolicExpression symbolicExpression, boolean atomize, String prefix, String separator) {
		StringBuffer result = new StringBuffer();
		SymbolicType type = symbolicExpression.type();

		if (symbolicExpression.size() >= PRINT_SIZE_THRESHOLD && !type.equals(this.dynamicHeapType)) {
			symbolicExpression.printCompressedTree(prefix, result);

			// insert the first new line character and deletes the last
			// character if it is a new line:
			result.insert(0, "\n");
			if (result.charAt(result.length() - 1) == '\n')
				result.delete(result.length() - 1, result.length());
			return result;
		}

		SymbolicType charType = typeFactory.charType().getDynamicType(universe);

		if (type == null)
			result.append("NULL");
		else if (type.equals(this.pointerType)) {
			// pointer
			return pointerValueToString(source, state, symbolicExpression);
		} else if (type.equals(this.functionPointerType)) {
			// function pointer
			return functionPointerValueToString(source, state, symbolicExpression);
		} else if (type.equals(this.dynamicHeapType)) {
			// heap
			return heapValueToString(source, state, symbolicExpression, prefix, separator);
		} else if (symbolicExpression.operator() == SymbolicOperator.ARRAY && type instanceof SymbolicArrayType
				&& ((SymbolicArrayType) type).elementType().equals(charType)) {
			// string literal
			result.append("\"");
			result.append(this.symbolicUtil.charArrayToString(source, symbolicExpression, 0, true));
			result.append("\"");
		} else if (type.equals(procType)) {
			// $proc's
			if (symbolicExpression.operator() != SymbolicOperator.TUPLE)
				result.append(symbolicExpression);
			else {
				int pid = modelFactory.getProcessId(symbolicExpression);

				if (!modelFactory.isPocessIdDefined(pid)) {
					result.append("UNDEFINED");
				} else {
					if (pid < 0)
						result.append("$proc_null");
					else {
						ProcessState procState = state.getProcessState(pid);

						if (procState == null)
							result.append("UNDEFINED");
						else
							result.append(procState.name());
					}
				}
			}
		} else if (type.equals(scopeType)) {
			// $scope's
			if (symbolicExpression.operator() != SymbolicOperator.TUPLE)
				result.append(symbolicExpression);
			else {
				StateFactory stateFactory = evaluator.stateFactory();
				int scopeId = stateFactory.getDyscopeId(symbolicExpression);

				if (!stateFactory.isScopeIdDefined(scopeId))
					result.append("UNDEFINED");
				else
					result.append("d" + scopeId);
			}
		} else if (symbolicExpression.type() == typeFactory.dynamicMemType())
			result.append(prettyMemValue(state, symbolicExpression, source));
		else {
			SymbolicOperator operator = symbolicExpression.operator();

			// The structure of a symbolic expression has changed.
			// Symbolic collections are no longer used as arguments.
			// This code must be updated accordingly.
			if (operator == SymbolicOperator.TUPLE) {
				if (type.toString().equals("$domain")) {
					SymbolicExpression dimension = (SymbolicExpression) symbolicExpression.argument(0);
					String unionKind = symbolicExpression.argument(1).toStringBuffer(false).toString();
					SymbolicExpression value = (SymbolicExpression) symbolicExpression.argument(2);

					if (unionKind.equals("0")) {
						result.append("($domain(");
						result.append(dimension.toStringBuffer(false));
						result.append("))");
					}
					result.append(this.symbolicExpressionToString(source, state, null, value, false, "", ""));
				} else if (type.toString().equals("$regular_range")) {
					int numArgs = symbolicExpression.numArguments();

					result.append("(");
					for (int i = 0; i < numArgs; i++) {
						if (i == 1)
							result.append("..");
						else if (i == 2) {
							result.append("#");
						}
						result.append(this.symbolicExpressionToString(source, state, null,
								(SymbolicExpression) symbolicExpression.argument(i), prefix, separator));
					}
					result.append(")");
				} else {
					result.append(symbolicTupleOrArrayToString(source, state, symbolicExpression, civlType, separator,
							prefix));
				}
				return result;
			} else if (operator == SymbolicOperator.ARRAY) {
				result.append(
						symbolicTupleOrArrayToString(source, state, symbolicExpression, civlType, separator, prefix));
				return result;
			} else if (operator == SymbolicOperator.CONCRETE) {
				SymbolicTypeKind tk = type.typeKind();

				// if (showType
				// && (type instanceof SymbolicArrayType || type instanceof
				// SymbolicTupleType)) {
				// result.append('(');
				// result.append(type.toStringBuffer(false));
				// result.append(')');
				// }
				if (tk == SymbolicTypeKind.CHAR) {
					result.append("'");
					result.append(symbolicExpression.argument(0).toStringBuffer(false));
					result.append("'");
				} else {
					if (symbolicExpression.type().equals(symbolicUtil.dynamicType())) {
						result.append(typeFactory.getStaticTypeOfDynamicType(symbolicExpression).toString());
					} else {
						SymbolicObjectKind objectKind = symbolicExpression.argument(0).symbolicObjectKind();

						if (objectKind == SymbolicObjectKind.SEQUENCE) {
							SymbolicSequence<? extends SymbolicExpression> sequence = (SymbolicSequence<? extends SymbolicExpression>) symbolicExpression
									.argument(0);
							result.append(
									symbolicSequenceToString(source, state, sequence, civlType, separator, prefix));
						} else {
							result.append(symbolicExpression.argument(0).toStringBuffer(true));
						}
						if (type.isHerbrand())
							result.append('h');
					}
				}
				return result;
			} else {
				// if (showType
				// && (type instanceof SymbolicArrayType || type instanceof
				// SymbolicTupleType)) {
				// // if (tk == SymbolicTypeKind.TUPLE)
				// // result.append(type.toStringBuffer(false));
				// // else {
				// result.append('(');
				// result.append(type.toStringBuffer(false));
				// result.append(')');
				// // }
				// }
				switch (operator) {
				case ADD:
					processFlexibleBinaryNew(source, state, symbolicExpression, result, "+", false, atomize);
					break;
				case AND:
					processFlexibleBinaryNew(source, state, symbolicExpression, result, "&&", true, atomize);
					break;
				case APPLY: {
					String function = symbolicExpression.argument(0).toStringBuffer(true).toString();

					result.append(function);
					result.append("(");
					accumulate(source, state, result, ",", (SymbolicSequence<?>) symbolicExpression.argument(1));
					result.append(")");
					break;
				}
				case ARRAY_LAMBDA:
					if (type != null) {
						result.append("(");
						result.append(type);
						result.append(") ");
					}
					result.append(symbolicExpressionToString(source, state, civlType,
							(SymbolicExpression) symbolicExpression.argument(0), true, prefix, separator));
					break;
				case ARRAY_READ: {
					result.append(symbolicExpression.argument(0).toStringBuffer(true));
					result.append("[");
					result.append(symbolicExpression.argument(1).toStringBuffer(false));
					result.append("]");
					break;
				}
				case ARRAY_WRITE: {
					boolean needNewLine = // !civlType.areSubtypesScalar()
							!separator.isEmpty();
					String padding = "\n" + prefix + separator;
					String newPrefix = needNewLine ? prefix + separator : prefix;

					if (symbolicExpression.argument(0) instanceof SymbolicExpression) {
						result.append("(");
						result.append(this.symbolicExpressionToString(source, state, civlType,
								(SymbolicExpression) symbolicExpression.argument(0), false, prefix, separator));
						result.append(")");
					} else
						result.append(symbolicExpression.argument(0).toStringBuffer(true));
					result.append("{");
					if (needNewLine)
						result.append(padding);
					result.append("[");
					result.append(symbolicExpression.argument(1).toStringBuffer(false));
					result.append("]=");
					result.append(this.symbolicExpressionToString(source, state,
							civlType != null ? ((CIVLArrayType) civlType).elementType() : null,
							(SymbolicExpression) symbolicExpression.argument(2), true, newPrefix, separator));
					result.append("}");
					break;
				}
				case CAST:
					result.append('(');
					result.append(type.toStringBuffer(false));
					result.append(')');
					result.append(this.symbolicExpressionToString(source, state, null,
							(SymbolicExpression) symbolicExpression.argument(0), true, "", ""));
					break;
				case COND:
					result.append(this.symbolicExpressionToString(source, state, this.typeFactory.booleanType(),
							(SymbolicExpression) symbolicExpression.argument(0), true, "", ""));
					result.append(" ? ");
					result.append(this.symbolicExpressionToString(source, state, civlType,
							(SymbolicExpression) symbolicExpression.argument(1), true, "", ""));
					result.append(" : ");
					result.append(this.symbolicExpressionToString(source, state, civlType,
							(SymbolicExpression) symbolicExpression.argument(2), true, "", ""));
					if (atomize)
						atomize(result);
					break;
				case DENSE_ARRAY_WRITE: {
					int count = 0;
					boolean first = true;
					boolean needNewLine = !separator.isEmpty() && civlType != null ? !civlType.areSubtypesScalar()
							: false;
					String padding = "\n" + prefix + separator;
					String newPrefix = needNewLine ? prefix + separator : prefix;

					if (symbolicExpression.argument(0) instanceof SymbolicExpression) {
						result.append("(");
						result.append(this.symbolicExpressionToString(source, state, civlType,
								(SymbolicExpression) symbolicExpression.argument(0), atomize, prefix, separator));
						result.append(")");
					} else
						result.append(symbolicExpression.argument(0).toStringBuffer(true));
					result.append("{");
					for (SymbolicExpression value : (SymbolicSequence<?>) symbolicExpression.argument(1)) {
						if (!value.isNull()) {
							if (first)
								first = false;
							else
								result.append(", ");
							if (needNewLine)
								result.append(padding);
							result.append("[" + count + "]" + "=");
							result.append(symbolicExpressionToString(source, state, this.subType(civlType, count).right,
									value, false, newPrefix, separator));
							// result.append(value.toStringBuffer(false));
						}
						count++;
					}
					result.append("}");
					break;
				}
				case DENSE_TUPLE_WRITE: {
					boolean first = true;
					int eleIndex = 0;
					boolean allSubtypesScalar = civlType.areSubtypesScalar();
					boolean needNewLine = !separator.isEmpty() && !allSubtypesScalar;
					String padding = "\n" + prefix + separator;
					String newPrefix = needNewLine ? prefix + separator : prefix;
					SymbolicSequence<?> elements = (SymbolicSequence<?>) symbolicExpression.argument(1);
					boolean needBrackets = allSubtypesScalar || elements.size() == 0;

					result.append(symbolicExpression.argument(0).toStringBuffer(true));
					if (needBrackets)
						result.append("{");
					for (SymbolicExpression value : elements) {
						if (!value.isNull()) {
							Pair<String, CIVLType> eleNameAndType = this.subType(civlType, eleIndex);

							if (first)
								first = false;
							else
								result.append(", ");
							if (needNewLine)
								result.append(padding);
							result.append("." + eleNameAndType.left + "=");
							result.append(symbolicExpressionToString(source, state, eleNameAndType.right, value, false,
									newPrefix, separator));
						}
						eleIndex++;
					}
					if (needBrackets)
						result.append("}");
					break;
				}
				case DIVIDE:
					result.append(symbolicExpression.argument(0).toStringBuffer(true));
					result.append("/");
					result.append(symbolicExpression.argument(1).toStringBuffer(true));
					if (atomize)
						atomize(result);
					break;
				case EQUALS:
					processFlexibleBinary(source, state, symbolicExpression, result, "==", true, atomize);
					break; // if (arguments[0] instanceof
							// SymbolicExpression)
				// result.append(this.symbolicExpressionToString(source,
				// state, null, (SymbolicExpression) arguments[0]));
				// else
				// result.append(arguments[0].toStringBuffer(false));
				// result.append("==");
				// if (arguments[1] instanceof SymbolicExpression)
				// result.append(this.symbolicExpressionToString(source,
				// state, null, (SymbolicExpression) arguments[1]));
				// else
				// result.append(arguments[1].toStringBuffer(false));
				// if (atomize)
				// atomize(result);
				// return result.toString();
				case EXISTS:
					result.append("exists ");
					result.append(symbolicExpression.argument(0).toStringBuffer(false));
					result.append(" : ");
					result.append(((SymbolicExpression) symbolicExpression.argument(0)).type().toStringBuffer(false));
					result.append(" . ");
					result.append(symbolicExpression.argument(1).toStringBuffer(true));
					if (atomize)
						atomize(result);
					break;
				case FORALL:
					result.append("forall ");
					result.append(symbolicExpression.argument(0).toStringBuffer(false));
					result.append(" : ");
					result.append(((SymbolicExpression) symbolicExpression.argument(0)).type().toStringBuffer(false));
					result.append(" . ");
					result.append(symbolicExpression.argument(1).toStringBuffer(true));
					if (atomize)
						atomize(result);
					break;
				case INT_DIVIDE: {
					result.append(symbolicExpression.argument(0).toStringBuffer(true));
					// result.append("\u00F7");
					result.append("/");
					result.append(symbolicExpression.argument(1).toStringBuffer(true));
					if (atomize)
						atomize(result);
					break;
				}
				case LAMBDA:
					result.append("$lambda ");
					result.append(symbolicExpression.argument(0).toStringBuffer(false));
					result.append(": ");
					result.append(((SymbolicExpression) symbolicExpression.argument(0)).type().toStringBuffer(false));
					result.append(". ");
					result.append(this.symbolicExpressionToString(source, state, null,
							(SymbolicExpression) symbolicExpression.argument(1), true, prefix, separator));
					if (atomize)
						atomize(result);
					break;
				case LENGTH:
					result.append("length(");
					result.append(this.symbolicExpressionToString(source, state, null,
							(SymbolicExpression) symbolicExpression.argument(0), "", ""));
					result.append(")");
					break;
				case LESS_THAN:
					processFlexibleBinaryNew(source, state, symbolicExpression, result, "<", true, atomize);
					break;
				case LESS_THAN_EQUALS:
					processFlexibleBinaryNew(source, state, symbolicExpression, result, "<=", true, atomize);
					break;
				case MODULO:
					processFlexibleBinary(source, state, symbolicExpression, result, "%", true, atomize);
					break;
				case MULTIPLY:
					processFlexibleBinaryNew(source, state, symbolicExpression, result, "*", true, atomize);
					break;
				case NEGATIVE:
					result.append("-");
					result.append(this.symbolicExpressionToString(source, state, null,
							(SymbolicExpression) symbolicExpression.argument(0), "", ""));
					atomize(result);
					break;
				case NEQ:
					result.append(this.symbolicExpressionToString(source, state, null,
							(SymbolicExpression) symbolicExpression.argument(0), "", ""));
					result.append("!=");
					result.append(this.symbolicExpressionToString(source, state, null,
							(SymbolicExpression) symbolicExpression.argument(1), true, "", ""));
					if (atomize)
						atomize(result);
					break;
				case NOT:
					result.append("!");
					result.append(this.symbolicExpressionToString(source, state, null,
							(SymbolicExpression) symbolicExpression.argument(0), true, "", ""));
					if (atomize)
						atomize(result);
					break;
				case NULL:
					result.append("NULL");
					break;
				case OR:
					processFlexibleBinaryNew(source, state, symbolicExpression, result, "||", false, atomize);
					break;
				case POWER:
					processFlexibleBinary(source, state, symbolicExpression, result, "^", false, atomize);
					break;
				case SUBTRACT:
					processFlexibleBinary(source, state, symbolicExpression, result, "-", false, atomize);
					break;
				case SYMBOLIC_CONSTANT:
					result.append(symbolicExpression.argument(0).toStringBuffer(true));
					break;
				case TUPLE_READ:
					result.append(symbolicExpression.argument(0).toStringBuffer(true));
					result.append(".");
					result.append(symbolicExpression.argument(1).toStringBuffer(false));
					if (atomize)
						atomize(result);
					break;
				case TUPLE_WRITE: {
					boolean needNewLine = !separator.isEmpty() && !civlType.areSubtypesScalar();
					String padding = "\n" + prefix + separator;
					String newPrefix = needNewLine ? prefix + separator : prefix;
					int fieldIndex = ((IntObject) symbolicExpression.argument(1)).getInt();
					StructOrUnionField field = ((CIVLStructOrUnionType) civlType).getField(fieldIndex);

					result.append(symbolicExpression.argument(0).toStringBuffer(true));
					result.append("{");
					if (needNewLine)
						result.append(padding);
					result.append(".");
					result.append(field.name().name());
					result.append(":=");
					result.append(this.symbolicExpressionToString(source, state, field.type(), symbolicExpression,
							newPrefix, separator));
					result.append("}");
					break;
				}
				case UNION_EXTRACT:
					result.append("extract(");
					result.append(symbolicExpression.argument(0).toStringBuffer(false));
					result.append(",");
					result.append(symbolicExpression.argument(1).toStringBuffer(false));
					result.append(")");
					break;
				case UNION_INJECT: {
					int fieldIndex = ((IntObject) symbolicExpression.argument(0)).getInt();
					CIVLType fieldType = null;
					if (civlType != null && civlType instanceof CIVLStructOrUnionType) {
						CIVLStructOrUnionType unionType = (CIVLStructOrUnionType) civlType;
						fieldType = unionType.getField(fieldIndex).type();
					}

					result.append(this.symbolicExpressionToString(source, state, fieldType,
							(SymbolicExpression) symbolicExpression.argument(1), false, prefix, separator));
					break;
				}
				case UNION_TEST:
					result.append("test(");
					result.append(symbolicExpression.argument(0).toStringBuffer(false));
					result.append(",");
					result.append(symbolicExpression.argument(1).toStringBuffer(false));
					result.append(")");
					break;
				default:
					result.append(symbolicExpression.toStringBufferLong());
				}
			}
		}
		return result;
	}

	private StringBuffer symbolicTupleOrArrayToString(CIVLSource source, State state, SymbolicExpression tuppleOrArray,
			CIVLType civlType, String separator, String prefix) {
		StringBuffer result = new StringBuffer();
		// int elementIndex = 0;
		boolean allSubtypesScalar = civlType != null ? civlType.areSubtypesScalar() : false;
		boolean needNewLine = !separator.isEmpty() && !allSubtypesScalar;
		String padding = "\n" + prefix + separator;
		String newPrefix = needNewLine ? prefix + separator : prefix;
		int numArgs = tuppleOrArray.numArguments();
		boolean needBrackets = allSubtypesScalar || numArgs == 0;
		boolean isArray = civlType != null ? civlType.isArrayType() : false;

		if (needBrackets)
			result.append("{");
		for (int i = 0; i < numArgs; i++) {
			Pair<String, CIVLType> elementNameAndType = this.subType(civlType, i);
			CIVLType eleType = elementNameAndType.right;
			boolean subtypesOfEleScalar = eleType != null ? eleType.areSubtypesScalar() : false;
			boolean eleEmpty = false;
			SymbolicExpression symbolicElement = (SymbolicExpression) tuppleOrArray.argument(i);

			eleEmpty = symbolicElement.numArguments() == 0;
			if (i != 0 && !needNewLine)
				result.append(", ");
			if (needNewLine)
				result.append(padding);
			if (elementNameAndType.left != null)
				result.append("." + elementNameAndType.left);
			else if (isArray)
				result.append("[" + i + "]");
			if (subtypesOfEleScalar || eleEmpty)
				result.append("=");
			else if (eleType != null)
				result.append(": " + eleType);
			result.append(symbolicExpressionToString(source, state, elementNameAndType.right, symbolicElement, false,
					newPrefix, separator));
		}
		if (needBrackets)
			result.append("}");
		return result;
	}

	private StringBuffer symbolicSequenceToString(CIVLSource source, State state,
			SymbolicSequence<? extends SymbolicExpression> symbolicCollection, CIVLType civlType, String separator,
			String prefix) {
		StringBuffer result = new StringBuffer();
		int elementIndex = 0;
		boolean allSubtypesScalar = civlType != null ? civlType.areSubtypesScalar() : false;
		boolean needNewLine = !separator.isEmpty() && !allSubtypesScalar;
		String padding = "\n" + prefix + separator;
		String newPrefix = needNewLine ? prefix + separator : prefix;
		boolean needBrackets = allSubtypesScalar || symbolicCollection.size() == 0;
		boolean isArray = civlType != null ? civlType.isArrayType() : false;

		if (needBrackets)
			result.append("{");
		for (SymbolicExpression symbolicElement : symbolicCollection) {
			Pair<String, CIVLType> elementNameAndType = this.subType(civlType, elementIndex);
			CIVLType eleType = elementNameAndType.right;
			boolean subtypesOfEleScalar = eleType != null ? eleType.areSubtypesScalar() : false;
			boolean eleEmpty = false;

			if (symbolicElement.argument(0) instanceof SymbolicSequence) {
				SymbolicSequence<? extends SymbolicExpression> symbolicEleCollection = (SymbolicSequence<? extends SymbolicExpression>) symbolicElement
						.argument(0);

				eleEmpty = symbolicEleCollection.size() == 0;
			}
			if (elementIndex != 0 && !needNewLine)
				result.append(", ");
			if (needNewLine)
				result.append(padding);
			if (elementNameAndType.left != null)
				result.append("." + elementNameAndType.left);
			else if (isArray)
				result.append("[" + elementIndex + "]");
			if (subtypesOfEleScalar || eleEmpty)
				result.append("=");
			else if (eleType != null)
				result.append(": " + eleType);
			elementIndex++;
			result.append(symbolicExpressionToString(source, state, elementNameAndType.right, symbolicElement, false,
					newPrefix, separator));
		}
		if (needBrackets)
			result.append("}");
		return result;
	}

	/**
	 * Returns the i-th sub-type (and its name if it is a struct or union field) of
	 * the given type. If the given type is an array type, then return the element
	 * type; if the given type is a struct or union type, then returns the i-th
	 * field type. Returns null for other types.
	 * 
	 * @param type
	 * @param i
	 * @return
	 */
	private Pair<String, CIVLType> subType(CIVLType type, int i) {
		if (type != null)
			if (type instanceof CIVLArrayType) {
				CIVLArrayType arrayType = (CIVLArrayType) type;

				return new Pair<>(null, arrayType.elementType());
			} else if (type instanceof CIVLStructOrUnionType) {
				CIVLStructOrUnionType structOrUnionType = (CIVLStructOrUnionType) type;

				if (structOrUnionType.numFields() > 0) {
					StructOrUnionField field = structOrUnionType.getField(i);

					return new Pair<>(field.name().name(), field.type());
				}
			}
		return new Pair<>(null, null);
	}

	/**
	 * Constructs the pretty presentation of a heap.
	 * 
	 * For example:
	 * 
	 * <pre>
	 * | | | | __heap = 
	 * | | | | | objects of malloc 0 by f0:14.2-53 "p1d = (double *) ... )":
	 * | | | | | | 0: H0[0:=0]
	 * | | | | | objects of malloc 1 by f0:15.2-56 "p2d = (double ** ... )":
	 * | | | | | | 0: H1[0:=&<d0>heap<3,0>[0]]
	 * | | | | | objects of malloc 2 by f0:16.2-58 "p3d = (double ** ... )":
	 * | | | | | | 0: H2[0:=&<d0>heap<4,0>[0]]
	 * | | | | | objects of malloc 3 by f0:19.4-58 "p2d[i] = (double ... )":
	 * | | | | | | 0: H3[0:=0, 1:=1, 2:=2]
	 * | | | | | objects of malloc 4 by f0:20.4-61 "p3d[i] = (double ... )":
	 * | | | | | | 0: H4[0:=&<d0>heap<5,0>[0], 1:=&<d0>heap<5,1>[0], 2:=&<d0>heap<5,2>[0]]
	 * | | | | | objects of malloc 5 by f0:23.6-63 "p3d[i][j] = ... )":
	 * | | | | | | 0: H5[0:=0, 1:=1, 2:=2, 3:=3, 4:=4, 5:=5, 6:=6, 7:=7, 8:=8, 9:=9]
	 * | | | | | | 1: H6[0:=10, 1:=11, 2:=12, 3:=13, 4:=14, 5:=15, 6:=16, 7:=17, 8:=18, 9:=19]
	 * | | | | | | 2: H7[0:=20, 1:=21, 2:=22, 3:=23, 4:=24, 5:=25, 6:=26, 7:=27]
	 * </pre>
	 * 
	 * @param source    The source code element for error report.
	 * @param state     The current state.
	 * @param heapValue The value of the heap to be printed.
	 * @param prefix    The prefix of the heap value in printing.
	 * @param separate  The separate string for sub-components of the heap.
	 * @return The pretty presentation of a heap for printing.
	 */
	private StringBuffer heapValueToString(CIVLSource source, State state, SymbolicExpression heapValue, String prefix,
			String separate) {
		StringBuffer result = new StringBuffer();
		int numFields = typeFactory.heapType().getNumMallocs();
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		String fieldPrefix = prefix + separate;
		String objectPrefix = fieldPrefix + separate;

		if (heapValue.isNull()) {
			result.append("NULL");
		}
		for (int i = 0; i < numFields; i++) {
			SymbolicExpression heapField = universe.tupleRead(heapValue, universe.intObject(i));
			NumericExpression fieldLength = universe.length(heapField);
			int length;
			CIVLSource mallocSource;
			CIVLType fieldTypeElement = heapType.getMalloc(i).getStaticElementType();

			if (fieldLength.isZero())
				continue;
			result.append("\n");
			result.append(fieldPrefix);

			result.append("objects of malloc ");
			result.append(i);
			mallocSource = this.heapType.getMalloc(i).getSource();
			if (mallocSource != null) {
				result.append(" at ");
				result.append(mallocSource.getSummary(false));
			}
			// result.append(":");
			length = ((IntegerNumber) reasoner.extractNumber(fieldLength)).intValue();
			for (int j = 0; j < length; j++) {
				SymbolicExpression heapObject = universe.arrayRead(heapField, universe.integer(j));
				IntegerNumber heapObjLenNumber = ((IntegerNumber) reasoner.extractNumber(universe.length(heapObject)));
				CIVLType heapObjType = null;

				if (heapObjLenNumber != null) {
					int heapObjSize = ((IntegerNumber) reasoner.extractNumber(universe.length(heapObject))).intValue();

					heapObjType = this.typeFactory.completeArrayType(fieldTypeElement,
							this.modelFactory.integerLiteralExpression(mallocSource, BigInteger.valueOf(heapObjSize)));
				} else
					heapObjType = this.typeFactory.incompleteArrayType(fieldTypeElement);
				result.append("\n");
				result.append(objectPrefix);
				result.append(j);
				result.append(": ");
				result.append(heapObjType);
				result.append(this.symbolicExpressionToString(source, state, heapObjType, heapObject, false,
						objectPrefix, separate));
			}
		}
		if (result.length() == 0)
			result.append("EMPTYP");
		return result;
	}

	private CIVLType typeOfObjByRef(CIVLType type, ReferenceExpression ref) {
		if (ref.isIdentityReference())
			return type;
		else if (ref.isArrayElementReference()) {
			ArrayElementReference arrayEleRef = (ArrayElementReference) ref;
			CIVLType parentType = typeOfObjByRef(type, arrayEleRef.getParent());

			if (parentType.isDomainType())
				return typeFactory.rangeType();
			return ((CIVLArrayType) parentType).elementType();
		} else {
			int index;
			CIVLType parentType;
			ReferenceExpression parent;

			if (ref.isTupleComponentReference()) {
				TupleComponentReference tupleCompRef = (TupleComponentReference) ref;

				index = tupleCompRef.getIndex().getInt();
				parent = tupleCompRef.getParent();
			} else {
				// UnionMemberReference
				UnionMemberReference unionMemRef = (UnionMemberReference) ref;

				index = unionMemRef.getIndex().getInt();
				parent = unionMemRef.getParent();
			}
			parentType = typeOfObjByRef(type, parent);
			if (parentType.isHeapType()) {
				MallocStatement malloc = ((CIVLHeapType) parentType).getMalloc(index);
				CIVLType elementType = malloc.getStaticElementType();
				Expression sizeExpr = malloc.getSizeExpression();
				Expression sizeofType = modelFactory.sizeofTypeExpression(malloc.getSource(), elementType);
				Expression numHeapObjects = modelFactory.binaryExpression(malloc.getSource(), BINARY_OPERATOR.DIVIDE,
						sizeExpr, sizeofType);
				CIVLArrayType heapTupleType = typeFactory.completeArrayType(elementType, numHeapObjects);

				heapTupleType = typeFactory.incompleteArrayType(heapTupleType);
				return heapTupleType;
			}
			return ((CIVLStructOrUnionType) parentType).getField(index).type();
		}
	}

	@Override
	public Pair<State, String> expressionEvaluation(State state, int pid, Expression expression, boolean resultOnly)
			throws UnsatisfiablePathConditionException {
		return this.expressionEvaluationWorker(state, pid, expression, resultOnly, true);
	}

	private Pair<State, String> expressionEvaluation(State state, int pid, Expression expression)
			throws UnsatisfiablePathConditionException {
		return this.expressionEvaluation(state, pid, expression, false);
	}

	private Pair<State, String> expressionEvaluationFinalResult(State state, int pid, Expression expression)
			throws UnsatisfiablePathConditionException {
		return this.expressionEvaluationWorker(state, pid, expression, true, false);
	}

	private StringBuffer evaluateLHSExpression(State state, int pid, LHSExpression lhs)
			throws UnsatisfiablePathConditionException {
		LHSExpressionKind kind = lhs.lhsExpressionKind();
		StringBuffer result = new StringBuffer();

		switch (kind) {
		case DEREFERENCE: {
			result.append("*(");
			result.append(this.expressionEvaluation(state, pid, ((DereferenceExpression) lhs).pointer()).right);
			result.append(")");
			break;
		}
		case DOT: {
			DotExpression dot = (DotExpression) lhs;
			Expression structOrUnion = dot.structOrUnion();

			result.append("(");
			if (structOrUnion instanceof LHSExpression)
				result.append(this.evaluateLHSExpression(state, pid, (LHSExpression) structOrUnion));
			else
				result.append(this.expressionEvaluation(state, pid, structOrUnion).right);
			result.append(").");
			assert structOrUnion.getExpressionType() instanceof CIVLStructOrUnionType;
			result.append(((CIVLStructOrUnionType) structOrUnion.getExpressionType()).getField(dot.fieldIndex()).name()
					.name());
			break;
		}
		case SUBSCRIPT: {
			SubscriptExpression subscript = (SubscriptExpression) lhs;

			result.append(this.evaluateLHSExpression(state, pid, subscript.array()));
			result.append("[");
			result.append(this.expressionEvaluationFinalResult(state, pid, subscript.index()).right);
			result.append("]");
			break;
		}
		case VARIABLE:
			result.append(((VariableExpression) lhs).variable().name().name());
			break;
		default:
			throw new CIVLUnimplementedFeatureException("evaluating left-hand-side expression of " + kind + " kind",
					lhs.getSource());
		}
		return result;
	}

	// TODO: why this is called evaluation ?
	@Override
	public StringBuffer statementEvaluation(State state, State postState, int pid, Statement statement)
			throws UnsatisfiablePathConditionException {
		StatementKind kind = statement.statementKind();
		StringBuffer result = new StringBuffer();
		Pair<State, String> tmp;

		switch (kind) {
		case ASSIGN: {
			if (statement instanceof AtomicLockAssignStatement) {
				AtomicLockAssignStatement atomicLockStmt = (AtomicLockAssignStatement) statement;
				String process = state.getProcessState(pid).name();
				int previousAtomicCount = state.getProcessState(pid).atomicCount();

				if (atomicLockStmt.enterAtomic()) {
					result.append("ENTER_ATOMIC [");
					if (previousAtomicCount < 1)
						result.append(ModelConfiguration.ATOMIC_LOCK_VARIABLE_INDEX + ":=" + process + ", ");
					result.append(process + ".atomicCount:=" + (previousAtomicCount + 1));
					result.append("]");
				} else {
					result.append("LEAVE_ATOMIC [");

					if (previousAtomicCount == 1)
						result.append(ModelConfiguration.ATOMIC_LOCK_VARIABLE_INDEX + ":=$proc_null, ");
					result.append(process + ".atomicCount:=0");
					result.append("]");
				}
			} else {
				AssignStatement assign = (AssignStatement) statement;
				LHSExpression lhs = assign.getLhs();
				Expression rhs = assign.rhs();
				StringBuffer lhsString = this.evaluateLHSExpression(state, pid, lhs);
				String rhsString = this.expressionEvaluation(state, pid, rhs).right.toString();
				String newRhsString = this.expressionEvaluationFinalResult(state, pid, rhs).right;

				result.append(lhsString);
				result.append("=");
				result.append(rhsString);
				if (!rhsString.equals(newRhsString)) {
					result.append(" ");
					result.append(SEF_START);
					result.append(lhsString);
					result.append(SEF);
					result.append(newRhsString);
					result.append(SEF_END);
				}
			}
			break;
		}
		case PARALLEL_ASSIGN: {
			ParallelAssignStatement paraAssign = (ParallelAssignStatement) statement;
			List<Pair<LHSExpression, Expression>> assigns = paraAssign.assignments();
			boolean isFirst = true;

			for (Pair<LHSExpression, Expression> assign : assigns) {
				LHSExpression lhs = assign.left;
				Expression rhs = assign.right;
				StringBuffer lhsString = this.evaluateLHSExpression(state, pid, lhs);
				String rhsString = this.expressionEvaluation(state, pid, rhs).right.toString();
				String newRhsString = this.expressionEvaluationFinalResult(state, pid, rhs).right;

				if (isFirst)
					isFirst = false;
				else
					result.append("; ");
				result.append(lhsString);
				result.append("=");
				result.append(rhsString);
				if (!rhsString.equals(newRhsString)) {
					result.append(" ");
					result.append(SEF_START);
					result.append(lhsString);
					result.append(SEF);
					result.append(newRhsString);
					result.append(SEF_END);
				}
			}
			break;
		}
		case CALL_OR_SPAWN: {
			CallOrSpawnStatement callOrSpawn = (CallOrSpawnStatement) statement;
			CIVLFunction function = callOrSpawn.function();
			List<Expression> args = callOrSpawn.arguments();
			int numArgs = args.size();
			LHSExpression lhs = callOrSpawn.lhs();
			StringBuffer lhsString = null;

			if (lhs != null) {
				lhsString = this.evaluateLHSExpression(state, pid, lhs);
				result.append(lhsString);
				result.append("=");
			}
			if (callOrSpawn.isSpawn())
				result.append("$spawn ");
			if (function == null) {
				function = this.evaluator.evaluateFunctionIdentifier(state, pid, callOrSpawn.functionExpression(),
						callOrSpawn.getSource()).second;
				assert function != null;
			}
			result.append(function.name().name());
			result.append("(");
			for (int i = 0; i < numArgs; i++) {
				Expression arg = args.get(i);

				if (i != 0)
					result.append(", ");
				tmp = this.expressionEvaluation(state, pid, arg);
				result.append(tmp.right);
			}
			result.append(")");
			if (lhs != null && (callOrSpawn.isSpawn() || callOrSpawn.isSystemCall())) {
				String newLhsValue = this.expressionEvaluationFinalResult(postState, pid, lhs).right;

				if (newLhsValue != null) {
					result.append(" ");
					result.append(SEF_START);
					result.append(lhsString);
					result.append(SEF);
					result.append(this.expressionEvaluationFinalResult(postState, pid, lhs).right);
					result.append(SEF_END);
				}
			}
			break;
		}
		case DOMAIN_ITERATOR: {
			DomainIteratorStatement civlForEnter = (DomainIteratorStatement) statement;
			List<Variable> loopVars = civlForEnter.loopVariables();
			int dim = loopVars.size();

			result.append("NEXT of (");
			for (int i = 0; i < dim; i++) {
				Variable loopVar = loopVars.get(i);

				if (i != 0)
					result.append(", ");
				result.append(this.symbolicExpressionToString(loopVar.getSource(), state, loopVar.type(),
						state.valueOf(pid, loopVar), "", ""));
			}
			result.append(") in ");
			tmp = this.expressionEvaluation(state, pid, civlForEnter.domain());
			result.append(tmp.right);
			result.append(" [");
			for (int i = 0; i < dim; i++) {
				Variable loopVar = loopVars.get(i);

				if (i != 0)
					result.append(", ");
				result.append(loopVar.name().name());
				result.append(":=");
				result.append(this.symbolicExpressionToString(loopVar.getSource(), postState, loopVar.type(),
						postState.valueOf(pid, loopVar), "", ""));
			}
			result.append("]");
			break;
		}
		case CIVL_PAR_FOR_ENTER: {
			// $parfor(i0, i1, i2: dom) $spawn function(i0, i1, i2);
			CivlParForSpawnStatement parForEnter = (CivlParForSpawnStatement) statement;
			StringBuffer arguments = new StringBuffer();

			for (int i = 0; i < parForEnter.dimension(); i++) {
				if (i != 0)
					arguments.append(",");
				arguments.append("i");
				arguments.append(i);
			}
			result.append("$parfor(");
			result.append(arguments);
			result.append(": ");
			result.append(this.expressionEvaluation(state, pid, parForEnter.domain()).right);
			result.append(")");
			result.append(" $spawn ");
			result.append(parForEnter.parProcFunction().name().name());
			result.append("(");
			result.append(arguments);
			result.append(")");
			break;
		}
		case MALLOC: {
			MallocStatement malloc = (MallocStatement) statement;
			LHSExpression lhs = malloc.getLHS();
			StringBuffer lhsString = null;
			String newLhsString;

			if (lhs != null) {
				lhsString = this.evaluateLHSExpression(state, pid, lhs);
				result.append(lhsString);
				result.append("=");
			}
			result.append("(");
			result.append(malloc.getStaticElementType());
			result.append("*)");
			result.append("$malloc(");
			result.append(this.expressionEvaluation(state, pid, malloc.getScopeExpression()).right);
			result.append(", ");
			result.append(this.expressionEvaluation(state, pid, malloc.getSizeExpression()).right);
			result.append(") ");
			newLhsString = this.expressionEvaluationFinalResult(postState, pid, lhs).right;
			if (newLhsString != null) {
				result.append(SEF_START);
				result.append(lhsString);
				result.append(SEF);
				result.append(newLhsString);
				result.append(SEF_END);
			}
			break;
		}
		case NOOP: {
			Expression guard = statement.guard();

			result.append(statement.toString());
			result.append(" (guard: ");
			result.append(this.expressionEvaluation(state, pid, guard, false).right);
			result.append(")");
			break;
		}
		case RETURN: {
			// return expression (assigning to...)
			// ProcessState procState=state.getProcessState(pid);
			CIVLFunction function = state.getProcessState(pid).peekStack().location().function();
			// String functionName;
			Expression expression = ((ReturnStatement) statement).expression();
			StackEntry callerStack = state.getProcessState(pid).peekSecondLastStack();
			CallOrSpawnStatement caller = null;

			if (callerStack != null) {
				Statement stmt = callerStack.location().getSoleOutgoing();
				if (stmt.statementKind() == StatementKind.CALL_OR_SPAWN)
					caller = (CallOrSpawnStatement) callerStack.location().getSoleOutgoing();
			}
			assert function != null;
			result.append(function.name().name());
			result.append("(...) return");
			if (expression != null) {
				result.append(" ");
				result.append(this.expressionEvaluation(state, pid, expression).right);
				if (caller != null) {
					LHSExpression lhs = caller.lhs();

					if (lhs != null) {
						result.append(" ");
						result.append(SEF_START);
						result.append(this.evaluateLHSExpression(state, pid, lhs));
						result.append(SEF);
						result.append(this.expressionEvaluationFinalResult(state, pid, expression).right);
						result.append(SEF_END);
					}
				}
			}
			break;
		}
		case UPDATE: {
			UpdateStatement update = (UpdateStatement) statement;

			result.append("$update (");
			result.append(this.expressionEvaluation(state, pid, update.collator()).right);
			result.append(") ");
			result.append(this.statementEvaluation(state, postState, pid, update.call()));
			break;
		}
		default:
			throw new CIVLUnimplementedFeatureException("pretty-printing statement of " + kind + " kind",
					statement.getSource());
		}
		return result;

	}

	private Pair<State, String> expressionEvaluationWorker(State state, int pid, Expression expression,
			boolean resultOnly, boolean isTopLevel) throws UnsatisfiablePathConditionException {
		ExpressionKind kind = expression.expressionKind();
		StringBuilder result = new StringBuilder();
		Pair<State, String> temp;
		CIVLType exprType = expression.getExpressionType();

		if (resultOnly && !isTopLevel) {
			Evaluation eval;

			// if (expression.expressionKind() == ExpressionKind.ORIGINAL)
			// result.append(expression);
			// else {
			try {
				eval = this.evaluator.evaluate(state, pid, expression);
			} catch (Exception ex) {
				return new Pair<>(state, (String) null);
			}
			state = eval.state;
			result.append(this.symbolicExpressionToString(expression.getSource(), state, exprType, eval.value,
					!isTopLevel, "", ""));
			// }
		} else {
			if (expression.getExpressionType().typeKind() == TypeKind.MEM) {
				Evaluation eval = evaluator.evaluate(state, pid, expression);

				return new Pair<>(eval.state, prettyMemValue(eval.state, eval.value, expression.getSource()));
			}
			switch (kind) {
			case ABSTRACT_FUNCTION_CALL: {
				AbstractFunctionCallExpression abstractFuncCall = (AbstractFunctionCallExpression) expression;
				int i = 0;
				result.append(abstractFuncCall.function().name().name());
				result.append("(");
				for (Expression argument : abstractFuncCall.arguments()) {
					if (i != 0)
						result.append(", ");
					i++;
					temp = expressionEvaluationWorker(state, pid, argument, resultOnly, false);
					result.append(temp.right);
					state = temp.left;
				}
				result.append(")");
				break;
			}
			case BINARY: {
				BinaryExpression binary = (BinaryExpression) expression;

				if (!isTopLevel)
					result.append(" (");
				temp = this.expressionEvaluationWorker(state, pid, binary.left(), true, false);
				state = temp.left;
				if (temp.right == null)
					temp = this.expressionEvaluationWorker(state, pid, binary.left(), false, false);
				result.append(temp.right);
				result.append(binary.operatorToString());
				result.append(binary.right());
				if (!isTopLevel)
					result.append(")");
				break;
			}
			case CAST: {
				CastExpression cast = (CastExpression) expression;

				result.append("(");
				result.append(cast.getCastType().toString());
				result.append(")");
				temp = expressionEvaluationWorker(state, pid, cast.getExpression(), resultOnly, false);
				state = temp.left;
				result.append(temp.right);
				break;
			}
			case COND: {
				ConditionalExpression condExpr = (ConditionalExpression) expression;

				temp = expressionEvaluationWorker(state, pid, condExpr.getCondition(), resultOnly, false);
				state = temp.left;
				result.append(temp.right);
				temp = expressionEvaluationWorker(state, pid, condExpr.getTrueBranch(), resultOnly, false);
				state = temp.left;
				result.append(temp.right);
				temp = expressionEvaluationWorker(state, pid, condExpr.getFalseBranch(), resultOnly, false);
				state = temp.left;
				result.append(temp.right);
				break;
			}
			case DEREFERENCE: {
				DereferenceExpression dereference = (DereferenceExpression) expression;

				result.append("*");
				temp = this.expressionEvaluationWorker(state, pid, dereference.pointer(), resultOnly, false);
				state = temp.left;
				result.append(temp.right);
				break;
			}
			case DOMAIN_GUARD: {
				DomainGuardExpression domGuard = (DomainGuardExpression) expression;
				int dim = domGuard.dimension();

				temp = this.expressionEvaluationWorker(state, pid, domGuard.domain(), resultOnly, false);
				state = temp.left;
				result.append(temp.right);
				result.append(" has next for (");
				for (int i = 0; i < dim; i++) {
					Variable var = domGuard.variableAt(i);

					if (i != 0)
						result.append(", ");
					result.append(this.symbolicExpressionToString(var.getSource(), state, var.type(),
							state.getVariableValue(state.getDyscope(pid, var.scope()), var.vid())));
				}
				result.append(")");
				break;
			}
			case EXTENDED_QUANTIFIER: {
				ExtendedQuantifiedExpression extQuant = (ExtendedQuantifiedExpression) expression;
				ExtendedQuantifier quantifier = extQuant.extendedQuantifier();
				Expression function = extQuant.function();

				result.append(quantifier);
				result.append("(");
				temp = this.expressionEvaluationWorker(state, pid, extQuant.lower(), resultOnly, false);
				result.append(temp.right);
				state = temp.left;
				result.append(", ");
				temp = this.expressionEvaluationWorker(state, pid, extQuant.higher(), resultOnly, false);
				result.append(temp.right);
				state = temp.left;
				result.append(", ");
				if (function.expressionKind() == ExpressionKind.LAMBDA) {
					temp = this.expressionEvaluationWorker(state, pid, extQuant.function(), resultOnly, false);
					result.append(temp.right);
					state = temp.left;
				} else
					result.append(function);
				result.append(")");
				break;
			}
			case FUNCTION_IDENTIFIER: {
				FunctionIdentifierExpression functionID = (FunctionIdentifierExpression) expression;
				Triple<State, CIVLFunction, Integer> functionResult = this.evaluator.evaluateFunctionIdentifier(state,
						pid, functionID, expression.getSource());

				state = functionResult.first;
				result.append(functionResult.second.name().name());
				break;
			}
			case QUANTIFIER: {
				result.append(expression.toString());
				break;
			}
			case UNARY: {
				UnaryExpression unary = (UnaryExpression) expression;

				result.append(unary.operatorToString());
				temp = this.expressionEvaluationWorker(state, pid, unary.operand(), resultOnly, false);
				state = temp.left;
				result.append(temp.right);
				break;
			}
			case INITIAL_VALUE: {
				result.append(expression.toString());
				break;
			}
			case FUNC_CALL: {
				CallOrSpawnStatement call = ((FunctionCallExpression) expression).callStatement();

				result.append(this.statementEvaluation(state, null, pid, call));
				break;
			}
			case VARIABLE: {
				SymbolicExpression val = state.valueOf(pid, ((VariableExpression) expression).variable());
				// not report any error during printing
				result.append(symbolicExpressionToString(expression.getSource(), state, exprType, val, "", ""));
				break;
			}
			case ADDRESS_OF:
			case BOOLEAN_LITERAL:
			case CHAR_LITERAL:
			case DOT:
			case DYNAMIC_TYPE_OF:
			case HERE_OR_ROOT:
			case INTEGER_LITERAL:
			case MEMORY_UNIT:
			case NULL_LITERAL:
			case REAL_LITERAL:
			case REGULAR_RANGE:
			case SIZEOF_TYPE:
			case SIZEOF_EXPRESSION:
			case STRING_LITERAL:
			case COMPOUND_LITERAL:
			case SUBSCRIPT:
			case LAMBDA:
			case ARRAY_LAMBDA:
			case REC_DOMAIN_LITERAL: {
				Evaluation eval = this.evaluator.evaluate(state, pid, expression);

				state = eval.state;
				result.append(
						this.symbolicExpressionToString(expression.getSource(), state, exprType, eval.value, "", ""));
				break;
			}
			case BOUND_VARIABLE:
			case DERIVATIVE:
			case FUNCTION_GUARD:
			case RESULT:
			case SCOPEOF:
			case SELF:
			case SYSTEM_GUARD:
			case UNDEFINED_PROC:
			case PROC_NULL:
			case STATE_NULL:
				result.append(expression.toString());
				break;
			default:
				throw new CIVLUnimplementedFeatureException(
						"printing the evaluation of expression of " + kind + " kind", expression.getSource());
			}
		}
		return new Pair<>(state, result.toString());
	}

	void setEvaluator(Evaluator evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public StringBuffer stateInformation(State state) {
		StringBuffer result = new StringBuffer();

		result.append(this.inputVariablesToStringBuffer(state));
		result.append("\nContext:");
		result.append(this.pathconditionToString(null, state, "  ", state.getPathCondition(universe)));
		result.append(state.callStackToString());
		return result;
	}

	@Override
	public StringBuffer inputVariablesToStringBuffer(State state) {
		Map<Variable, SymbolicExpression> inputVariableValues = evaluator.stateFactory().inputVariableValueMap(state);
		StringBuffer result = new StringBuffer("");

		if (!inputVariableValues.isEmpty())
			result.append("\nInput:");
		for (Map.Entry<Variable, SymbolicExpression> entry : inputVariableValues.entrySet()) {
			result.append("\n  ");
			result.append(entry.getKey().name().name());
			result.append("=");
			result.append(this.symbolicExpressionToString(entry.getKey().getSource(), state, entry.getKey().type(),
					entry.getValue(), "", ""));
		}
		return result;
	}

	@Override
	public Evaluator evaluator() {
		return this.evaluator;
	}

	/**
	 * C11, 6.2.6.1 paragraph 4:
	 * 
	 * Values stored in non-bit-field objects of any other object type consist of n
	 * × CHAR_BIT bits, where n is the size of an object of that type, in bytes. The
	 * value may be copied into an object of type unsigned char [ n ] (e.g., by
	 * memcpy); the resulting set of bytes is called the object representation of
	 * the value.
	 * 
	 * 6.3.2.3 Pointers p7:
	 * 
	 * ... When a pointer to an object is converted to a pointer to a character
	 * type, the result points to the lowest addressed byte of the object.
	 * Successive increments of the result, up to the size of the object, yield
	 * pointers to the remaining bytes of the object.
	 * 
	 * @param source
	 * @param state
	 * @param isSubtract
	 * @param pointer
	 * @param offset
	 * @return
	 */
	@Override
	public SymbolicExpression pointerArithmetics(CIVLSource source, State state, boolean isSubtract,
			SymbolicExpression pointer, SymbolicExpression offset) {
		SymbolicExpression result = null;

		if (isSubtract) {
			assert evaluator.stateFactory().getDyscopeId(symbolicUtil.getScopeValue(offset)) == -1;
			assert this.symbolicUtil.getVariableId(source, offset) == -1;

			ReferenceExpression offsetRef = symbolicUtil.getSymRef(offset);
			return symbolicUtil.setSymRef(pointer, this.subtract(symbolicUtil.getSymRef(pointer), offsetRef));
		}
		return result;
	}

	/**
	 * left-right
	 * 
	 * for example, if left is TupleComponenent(<Ref 1>, 1), and right is
	 * TupleComponent(<Ref 1>, 1), then result is <Ref 1>.
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	private ReferenceExpression subtract(ReferenceExpression left, ReferenceExpression right) {
		if (universe.equals(left, right).isTrue())
			return universe.identityReference();
		return null;
	}

	@Override
	public Pair<BooleanExpression, ResultType> isDerefablePointer(State state, SymbolicExpression pointer,
			CIVLSource source) {
		if (this.symbolicUtil.isNullPointer(pointer) || pointer.isNull())
			return new Pair<>(universe.falseExpression(), ResultType.NO);

		int dyscope = evaluator.stateFactory().getDyscopeId(symbolicUtil.getScopeValue(pointer));
		int vid = symbolicUtil.getVariableId(null, pointer);
		SymbolicExpression value;

		if (dyscope == ModelConfiguration.DYNAMIC_CONSTANT_SCOPE) {
			value = modelFactory.model().staticConstantScope().variable(vid).constantValue();
		} else if (dyscope < 0)
			return new Pair<>(universe.falseExpression(), ResultType.NO);
		else
			value = state.getVariableValue(dyscope, vid);
		if (value == null)
			return new Pair<>(universe.falseExpression(), ResultType.NO);
		return this.checkReference(true, universe.reasoner(state.getPathCondition(universe)),
				symbolicUtil.getSymRef(pointer), value, state, source);
	}

	/**
	 * Is the given reference applicable to the specified symbolic expression?
	 * 
	 * @param ref   The reference expression to be checked.
	 * @param value The symbolic expression specified.
	 * @return True iff the given reference is applicable to the specified symbolic
	 *         expression
	 */
	private Triple<SymbolicExpression, BooleanExpression, ResultType> isValidRefOfValue(Reasoner reasoner,
			boolean derefable, ReferenceExpression ref, SymbolicExpression value, State state, CIVLSource source) {
		BooleanExpression predicate = universe.falseExpression();

		if (ref.isIdentityReference())
			return new Triple<>(value, universe.trueExpression(), ResultType.YES);
		else {
			ReferenceExpression parent = ((NTReferenceExpression) ref).getParent();
			Triple<SymbolicExpression, BooleanExpression, ResultType> parentTest = isValidRefOfValue(reasoner,
					derefable, parent, value, state, source);
			SymbolicExpression targetValue;

			if (parentTest.third != ResultType.YES)
				return parentTest;
			targetValue = parentTest.first;
			if (targetValue == null)
				return parentTest;
			if (ref.isArrayElementReference()) {
				ArrayElementReference arrayEleRef = (ArrayElementReference) ref;

				if (targetValue.type() instanceof SymbolicArrayType) {
					NumericExpression index = arrayEleRef.getIndex();
					NumericExpression length = universe.length(targetValue);

					BooleanExpression claim = derefable ? universe.lessThan(index, length)
							: universe.lessThanEquals(index, length);

					if (universe.getShowProverQueries()) {
						String arrayStr = state != null
								? symbolicExpressionToString(source, state, null, targetValue)
								: targetValue.toString();

						// the array value can be large; keep the annotation readable
						if (arrayStr.length() > 120)
							arrayStr = arrayStr.substring(0, 120) + "...";

						String locationStr = source != null ? " at " + source.getLocation() : "";

						universe.setQueryExplanation("checking array index within bounds while validating a pointer"
								+ " reference: accessing index " + index + " of array " + arrayStr + " with length "
								+ length + locationStr);
					}

					ResultType result = reasoner.valid(claim).getResultType();

					claim = reasoner.simplify(claim);
					if (result == ResultType.YES) {
						if (!derefable
								&& reasoner.valid(universe.equals(length, index)).getResultType() != ResultType.NO) {
							return new Triple<>(null, claim, result);
						} else {
							return new Triple<>(universe.arrayRead(targetValue, index), claim, result);
						}
					} else if (result == ResultType.MAYBE)
						return new Triple<>(null, claim, result);
					predicate = claim;
				}
			} else if (ref.isTupleComponentReference()) {
				TupleComponentReference tupleCompRef = (TupleComponentReference) ref;

				if (targetValue.type() instanceof SymbolicTupleType) {
					IntObject index = tupleCompRef.getIndex();
					int length = ((SymbolicTupleType) targetValue.type()).sequence().numTypes();

					if (index.getInt() < length)
						return new Triple<>(universe.tupleRead(targetValue, index), universe.trueExpression(),
								ResultType.YES);
				}
			} else if (ref instanceof UnionMemberReference) {
				UnionMemberReference unionMemRef = (UnionMemberReference) ref;
				IntObject index = unionMemRef.getIndex();

				if (targetValue.type() instanceof SymbolicUnionType) {
					int length = ((SymbolicUnionType) targetValue.type()).sequence().numTypes();

					if (index.getInt() < length)
						return new Triple<>(universe.unionExtract(index, targetValue), universe.trueExpression(),
								ResultType.YES);
				}
			} else {
				// offset reference
				return new Triple<>(null, universe.trueExpression(), ResultType.YES);
			}
		}
		return new Triple<>(null, predicate, ResultType.NO);
	}

	/**
	 * 
	 * @param derefable true iff to check derefableness; otherwise, only check
	 *                  definedness.
	 * @param reasoner
	 * @param reference
	 * @param object
	 * @return
	 */
	private Pair<BooleanExpression, ResultType> checkReference(boolean derefable, Reasoner reasoner,
			ReferenceExpression reference, SymbolicExpression object, State state, CIVLSource source) {
		Triple<SymbolicExpression, BooleanExpression, ResultType> result = isValidRefOfValue(reasoner, derefable,
				reference, object, state, source);

		return new Pair<>(result.second, result.third);
	}

	@Override
	public StringBuffer pathconditionToString(CIVLSource source, State state, String prefix, BooleanExpression pc) {
		BooleanExpression[] clauses = this.symbolicUtil.getConjunctiveClauses(pc);
		StringBuffer result = new StringBuffer();
		int length = clauses.length;

		for (int i = 0; i < length; i++) {
			result.append("\n");
			result.append(prefix);
			result.append(this.symbolicExpressionToString(source, state, typeFactory.booleanType(), clauses[i]));
		}
		return result;
	}

	@Override
	public Pair<BooleanExpression, ResultType> isDefinedPointer(State state, SymbolicExpression pointer,
			CIVLSource civlSource) {
		if (this.symbolicUtil.isNullPointer(pointer))
			return new Pair<>(universe.trueExpression(), ResultType.YES);
		if (pointer.isNull())
			return new Pair<>(universe.falseExpression(), ResultType.NO);
		if (!SymbolicAnalyzer.isConcretePointer(pointer))
			throw new CIVLUnimplementedFeatureException(
					"\nAbility to deterine whether a non-concrete pointer is defined." + "\npointer value: "
							+ pointer.toString(),
					civlSource);

		int dyscope = evaluator.stateFactory().getDyscopeId(symbolicUtil.getScopeValue(pointer));

		if (dyscope == ModelConfiguration.DYNAMIC_CONSTANT_SCOPE)
			return new Pair<>(universe.trueExpression(), ResultType.YES);
		if (dyscope < 0)
			return new Pair<>(universe.falseExpression(), ResultType.NO);

		int vid = symbolicUtil.getVariableId(null, pointer);
		SymbolicExpression value = state.getVariableValue(dyscope, vid);

		if (value == null)
			return new Pair<>(universe.falseExpression(), ResultType.NO);
		return this.checkReference(false, universe.reasoner(state.getPathCondition(universe)),
				symbolicUtil.getSymRef(pointer), value, state, civlSource);
	}

	@Override
	public StringBuffer memoryUnitToString(State state, MemoryUnit mu) {
		return this.variableReferenceToString(state, null, true, mu.dyscopeID(), mu.varID(), mu.reference());
	}

	/**
	 * Pretty print of value of $mem type. See
	 * {@link CIVLMemType#getDynamicType(SymbolicUniverse)} for definition of
	 * dynamic $mem type.
	 */
	private String prettyMemValue(State state, SymbolicExpression memValue, CIVLSource source) {
		return MemEvaluator.prettyPrintMemValue(this.typeFactory, this.universe, state, memValue, source);
	}

	@Override
	public boolean areDynamicTypesCompatiableForAssign(SymbolicType lhsType, SymbolicType rhsType) {
		switch (lhsType.typeKind()) {

		case BOOLEAN:
		case CHAR:
		case INTEGER:
		case REAL:
		case UNINTERPRETED:
			return lhsType == rhsType;
		case ARRAY:
			SymbolicArrayType rhsArrType, lhsArrType = (SymbolicArrayType) lhsType;

			if (rhsType.typeKind() != SymbolicTypeKind.ARRAY)
				return false;
			rhsArrType = (SymbolicArrayType) rhsType;
			if (lhsArrType.isComplete()) {
				if (!rhsArrType.isComplete())
					return false;
				if (!((SymbolicCompleteArrayType) lhsArrType).extent()
						.equals(((SymbolicCompleteArrayType) rhsArrType).extent()))
					return false;
				return areDynamicTypesCompatiableForAssign(lhsArrType.elementType(), rhsArrType.elementType());
			} else
				return areDynamicTypesCompatiableForAssign(lhsArrType.elementType(), rhsArrType.elementType());
		case TUPLE: {
			SymbolicTupleType lhsTupleType = (SymbolicTupleType) lhsType, rhsTupleType;

			if (rhsType.typeKind() != SymbolicTypeKind.TUPLE)
				return false;
			rhsTupleType = (SymbolicTupleType) rhsType;
			if (lhsTupleType.sequence().numTypes() != rhsTupleType.sequence().numTypes())
				return false;
			else {
				int numTypes = lhsTupleType.sequence().numTypes();

				for (int i = 0; i < numTypes; i++)
					if (!areDynamicTypesCompatiableForAssign(lhsTupleType.sequence().getType(i),
							rhsTupleType.sequence().getType(i)))
						return false;
				return true;
			}
		}
		case UNION: {
			SymbolicUnionType lhsUnionType = (SymbolicUnionType) lhsType, rhsUnionType;

			if (rhsType.typeKind() != SymbolicTypeKind.UNION)
				return false;
			rhsUnionType = (SymbolicUnionType) rhsType;
			if (lhsUnionType.sequence().numTypes() != rhsUnionType.sequence().numTypes())
				return false;
			else {
				int numTypes = lhsUnionType.sequence().numTypes();

				for (int i = 0; i < numTypes; i++)
					if (!areDynamicTypesCompatiableForAssign(lhsUnionType.sequence().getType(i),
							rhsUnionType.sequence().getType(i)))
						return false;
				return true;
			}
		}
		// either not supported yet or not possible as a lhs expression
		case FUNCTION:
			return false;
		case MAP:
		case SET:
		default:
			throw new CIVLUnimplementedFeatureException("Left-hand side expression has type: " + lhsType);
		}
	}
}
