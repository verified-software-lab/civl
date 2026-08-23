package dev.civl.mc.semantics.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.log.IF.CIVLErrorLogger;
import dev.civl.mc.model.IF.AbstractFunction;
import dev.civl.mc.model.IF.CIVLFunction;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLProperty;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLSyntaxException;
import dev.civl.mc.model.IF.CIVLTypeFactory;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.model.IF.LogicFunction;
import dev.civl.mc.model.IF.ModelConfiguration;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.SystemFunction;
import dev.civl.mc.model.IF.expression.AbstractFunctionCallExpression;
import dev.civl.mc.model.IF.expression.AddressOfExpression;
import dev.civl.mc.model.IF.expression.ArrayLambdaExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression.BINARY_OPERATOR;
import dev.civl.mc.model.IF.expression.BooleanLiteralExpression;
import dev.civl.mc.model.IF.expression.CastExpression;
import dev.civl.mc.model.IF.expression.CharLiteralExpression;
import dev.civl.mc.model.IF.expression.CompoundLiteralExpression;
import dev.civl.mc.model.IF.expression.CompoundLiteralExpression.CIVLCompoundLiteralObject;
import dev.civl.mc.model.IF.expression.CompoundLiteralExpression.CIVLLiteralObject;
import dev.civl.mc.model.IF.expression.CompoundLiteralExpression.CIVLScalarLiteralObject;
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
import dev.civl.mc.model.IF.expression.FunctionIdentifierExpression;
import dev.civl.mc.model.IF.expression.HereOrRootExpression;
import dev.civl.mc.model.IF.expression.InitialValueExpression;
import dev.civl.mc.model.IF.expression.IntegerLiteralExpression;
import dev.civl.mc.model.IF.expression.LHSExpression;
import dev.civl.mc.model.IF.expression.LambdaExpression;
import dev.civl.mc.model.IF.expression.ProcnullExpression;
import dev.civl.mc.model.IF.expression.QuantifiedExpression;
import dev.civl.mc.model.IF.expression.RealLiteralExpression;
import dev.civl.mc.model.IF.expression.RecDomainLiteralExpression;
import dev.civl.mc.model.IF.expression.RegularRangeExpression;
import dev.civl.mc.model.IF.expression.ScopeofExpression;
import dev.civl.mc.model.IF.expression.SelfExpression;
import dev.civl.mc.model.IF.expression.SizeofExpression;
import dev.civl.mc.model.IF.expression.SizeofTypeExpression;
import dev.civl.mc.model.IF.expression.SubscriptExpression;
import dev.civl.mc.model.IF.expression.SystemGuardExpression;
import dev.civl.mc.model.IF.expression.UnaryExpression;
import dev.civl.mc.model.IF.expression.VariableExpression;
import dev.civl.mc.model.IF.type.CIVLArrayType;
import dev.civl.mc.model.IF.type.CIVLCompleteArrayType;
import dev.civl.mc.model.IF.type.CIVLCompleteDomainType;
import dev.civl.mc.model.IF.type.CIVLDomainType;
import dev.civl.mc.model.IF.type.CIVLEnumType;
import dev.civl.mc.model.IF.type.CIVLPointerType;
import dev.civl.mc.model.IF.type.CIVLPrimitiveType;
import dev.civl.mc.model.IF.type.CIVLPrimitiveType.PrimitiveTypeKind;
import dev.civl.mc.model.IF.type.CIVLStructOrUnionType;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.type.CIVLType.TypeKind;
import dev.civl.mc.model.IF.type.StructOrUnionField;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.model.common.ABC_CIVLSource;
import dev.civl.mc.semantics.IF.ArrayToolBox;
import dev.civl.mc.semantics.IF.Evaluation;
import dev.civl.mc.semantics.IF.Evaluator;
import dev.civl.mc.semantics.IF.LibraryEvaluator;
import dev.civl.mc.semantics.IF.LibraryEvaluatorLoader;
import dev.civl.mc.semantics.IF.LibraryExecutorLoader;
import dev.civl.mc.semantics.IF.LibraryLoaderException;
import dev.civl.mc.semantics.IF.MemoryUnitExpressionEvaluator;
import dev.civl.mc.semantics.IF.SymbolicAnalyzer;
import dev.civl.mc.semantics.IF.TypeEvaluation;
import dev.civl.mc.semantics.common.CIVLDereferenceOperator.DereferencedResult;
import dev.civl.mc.semantics.common.ErrorSideEffectFreeEvaluator.ErroneousSideEffectException;
import dev.civl.mc.state.IF.MemoryUnitFactory;
import dev.civl.mc.state.IF.ProcessState;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.StateFactory;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.mc.util.IF.Pair;
import dev.civl.mc.util.IF.Singleton;
import dev.civl.mc.util.IF.Triple;
import dev.civl.sarl.IF.Reasoner;
import dev.civl.sarl.IF.SARLException;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.ArrayElementReference;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NTReferenceExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.NumericSymbolicConstant;
import dev.civl.sarl.IF.expr.OffsetReference;
import dev.civl.sarl.IF.expr.ReferenceExpression;
import dev.civl.sarl.IF.expr.SymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;
import dev.civl.sarl.IF.expr.TupleComponentReference;
import dev.civl.sarl.IF.number.IntegerNumber;
import dev.civl.sarl.IF.number.Number;
import dev.civl.sarl.IF.number.NumberFactory;
import dev.civl.sarl.IF.object.IntObject;
import dev.civl.sarl.IF.object.StringObject;
import dev.civl.sarl.IF.object.SymbolicObject;
import dev.civl.sarl.IF.object.SymbolicSequence;
import dev.civl.sarl.IF.type.SymbolicArrayType;
import dev.civl.sarl.IF.type.SymbolicCompleteArrayType;
import dev.civl.sarl.IF.type.SymbolicFunctionType;
import dev.civl.sarl.IF.type.SymbolicTupleType;
import dev.civl.sarl.IF.type.SymbolicType;
import dev.civl.sarl.IF.type.SymbolicType.SymbolicTypeKind;
import dev.civl.sarl.IF.type.SymbolicUnionType;
import dev.civl.sarl.expr.cnf.BooleanPrimitive;
import dev.civl.sarl.number.IF.Numbers;
import dev.civl.sarl.prove.IF.ProverFunctionInterpretation;

/**
 * An evaluator is used to evaluate expressions.
 * 
 * @author Timothy K. Zirkel (zirkel)
 * @author Manchun Zheng (zmanchun)
 * @author Ziqing Luo (ziqing)
 * 
 */
public class CommonEvaluator implements Evaluator {

	private static int INTEGER_BIT_LENGTH = 32;
	public static String POINTER_TO_INT_FUNCTION = "_pointer2Int";
	public static String INT_TO_POINTER_FUNCTION = "_int2Pointer";
	public static String CHAR_TO_INT_FUNCTION = "_char2int";
	public static String INT_TO_CHAR_FUNCTION = "_int2char";

	/**
	 * A bounded offset identifier identifies the bound variable in the quantified
	 * expression which represents an offset in a pointer representation:
	 * <code>base-address + _offset</code>
	 */
	protected static String BOUNDED_OFFSET_IDENTIFIER = "_offset";

	/* *************************** Instance Fields ************************* */
	/**
	 * A reference to {@link CIVLDereferenceOperator}, which dereferences a variable
	 * value according to a {@link ReferenceExpression}. The dereference operation
	 * succeeds if and only if the pointer is dereferable.
	 */
	CIVLDereferenceOperator derefOperator;

	protected LibraryExecutorLoader libExeLoader;

	/**
	 * An evaluator for evaluating expressions that may contains terms of set types:
	 */
	protected MemEvaluator memExpressionEvaluator = null;

	protected MemoryUnitFactory memUnitFactory;

	protected CIVLConfiguration civlConfig;

	/**
	 * The library evaluator loader. This is used to location and obtain the
	 * appropriate library evaluators when library-defined expressions need to be
	 * evaluated. These are primarily guards of system functions.
	 */
	protected LibraryEvaluatorLoader libLoader;

	/**
	 * An uninterpreted function used to evaluate "BigO" of an expression. It takes
	 * as input one expression of real type and return a real type,
	 * <code>real $O(real x)</code>.
	 */
	private SymbolicExpression bigOFunction;

	/**
	 * The identity reference expression. A symbolic reference expression can be
	 * viewed as a function which takes a symbolic expression x (of the appropriate
	 * type) and returns a sub-expression of x. The identify reference, viewed this
	 * way, corresponds to the identify function: given x it returns x.
	 */
	private ReferenceExpression identityReference;

	/**
	 * The unique model factory used to construct the CIVL model elements that this
	 * evaluator will encounter.
	 */
	protected ModelFactory modelFactory;

	/**
	 * The symbolic expression representing "NULL" expression, which is non-null (as
	 * a Java object) but represents the absence of some value. It is used in CIVL
	 * to represent the undefined value: it is the value assigned to variables
	 * before they are initialized. Note that this is the only symbolic expression
	 * that does not have a type.
	 */
	private SymbolicExpression nullExpression;

	/**
	 * The unique real number factory used in the system.
	 */
	private NumberFactory numberFactory = Numbers.REAL_FACTORY;

	/**
	 * The symbolic expression 1 of integer type. (Note that this is distinct from
	 * the 1 of real type.)
	 */
	protected NumericExpression one;

	/**
	 * The pointer value is a triple <s,v,r> where s identifies the dynamic scope, v
	 * identifies the variable within that scope, and r identifies a point within
	 * that variable. The type of s is scopeType, which is just a tuple wrapping a
	 * single integer which is the dynamic scope ID number. The type of v is
	 * integer; it is the (static) variable ID number for the variable in its scope.
	 * The type of r is ReferenceExpression from SARL.
	 */
	protected SymbolicTupleType pointerType;

	/**
	 * The function pointer value is a pair <s,i> where s identifies the dynamic
	 * scope, i is the function id in the scope. The type of s is scopeType, which
	 * is just a tuple wrapping a single integer which is the dynamic scope ID
	 * number. The type of i is integer. Function pointer type need to be different
	 * from pointer type, because there is analysis particularly for pointers, like
	 * heap object reachability, reachable memory units, etc.
	 */
	private SymbolicTupleType functionPointerType;

	/**
	 * 
	 */
	// private boolean enableShortCircuitLogicEval;

	/**
	 * The unique state factory used in the system.
	 */
	protected StateFactory stateFactory;

	/**
	 * The unique symbolic universe used in the system.
	 */
	protected SymbolicUniverse universe;

	/**
	 * The symbolic int object of 0,1 and 2
	 */
	final private IntObject zeroObj, oneObj, twoObj;

	/**
	 * The symbolic numeric expression of 0 of integer type.
	 */
	protected NumericExpression zero;

	/**
	 * The symbolic numeric expression of 0 of real type.
	 */
	private NumericExpression zeroR;

	/** The SARL character type. */
	private SymbolicType charType;

	/**
	 * The SARL character 0, i.e., '\0' or '\u0000', used as the "null character
	 * constant" in C.
	 */
	private SymbolicExpression nullCharExpr;

	/**
	 * The symbolic utility for manipulations of symbolic expressions.
	 */
	protected SymbolicUtility symbolicUtil;

	/**
	 * The error logger to report errors.
	 */
	protected CIVLErrorLogger errorLogger;

	/**
	 * The symbolic numeric expression that has the value of either zero or one.
	 */
	// private NumericExpression zeroOrOne;

	/**
	 * The symbolic analyzer to be used.
	 */
	protected SymbolicAnalyzer symbolicAnalyzer;

	protected MemoryUnitExpressionEvaluator memUnitEvaluator;

	protected CIVLTypeFactory typeFactory;

	// private SymbolicConstant pointer2IntFunc;
	//
	// private SymbolicConstant int2PointerFunc;

	/**
	 * A bit-vector type which representing a boolean array with a concrete length
	 * corresponding to the bit-length of an integer define by the environment. (The
	 * default length is 32);
	 */
	@SuppressWarnings("unused")
	private SymbolicCompleteArrayType bitVectorType;

	protected FunctionCallExecutor functionCallExecutor;

	private SymbolicExpression offsetFunction;

	/* *************** Creating sub-class instances *************** */
	@Override
	public ReadSetCollectEvaluator newReadSetCollectEvaluator() {
		return new ReadSetCollectEvaluator(this);
	}

	/* ***************************** Constructors ************************** */

	/**
	 * Create a new instance of evaluator for evaluating expressions.
	 * 
	 * @param modelFactory     The model factory of the system.
	 * @param stateFactory     The state factory of the system.
	 * @param loader           The loader for library evaluators.
	 * @param symbolicUtil     The symbolic utility.
	 * @param symbolicAnalyzer The symbolic analyzer used in the system.
	 * @param errorLogger      The error logger for logging errors.
	 */
	public CommonEvaluator(ModelFactory modelFactory, StateFactory stateFactory, LibraryEvaluatorLoader loader,
			LibraryExecutorLoader loaderExec, SymbolicUtility symbolicUtil, SymbolicAnalyzer symbolicAnalyzer,
			MemoryUnitFactory memUnitFactory, CIVLErrorLogger errorLogger, CIVLConfiguration config) {
		this.libExeLoader = loaderExec;
		this.memUnitFactory = memUnitFactory;
		this.libLoader = loader;
		this.errorLogger = errorLogger;
		this.symbolicUtil = symbolicUtil;
		this.symbolicAnalyzer = symbolicAnalyzer;
		((CommonSymbolicAnalyzer) symbolicAnalyzer).setEvaluator(this);
		this.modelFactory = modelFactory;
		this.typeFactory = modelFactory.typeFactory();
		this.stateFactory = stateFactory;
		this.universe = stateFactory.symbolicUniverse();
		this.memUnitEvaluator = new CommonMemoryUnitEvaluator(symbolicUtil, this, memUnitFactory, universe);
		this.derefOperator = new CIVLDereferenceOperator(universe);
		pointerType = typeFactory.pointerSymbolicType();
		functionPointerType = typeFactory.functionPointerSymbolicType();
		zeroObj = universe.intObject(0);
		oneObj = universe.intObject(1);
		twoObj = universe.intObject(2);
		identityReference = universe.identityReference();
		zero = universe.integer(0);
		zeroR = universe.zeroReal();
		one = universe.integer(1);
		nullExpression = universe.nullExpression();
		bigOFunction = universe.symbolicConstant(universe.stringObject("BIG_O"),
				universe.functionType(new Singleton<SymbolicType>(universe.realType()), universe.realType()));
		offsetFunction = universe.symbolicConstant(universe.stringObject("OFFSET"), universe.functionType(
				Arrays.asList(symbolicUtil.dynamicType(), universe.integerType()), universe.integerType()));
		charType = universe.characterType();
		nullCharExpr = universe.character('\u0000');
		// pointer2IntFunc = universe.symbolicConstant(universe
		// .stringObject(POINTER_TO_INT_FUNCTION), universe.functionType(
		// Arrays.asList(this.pointerType), this.universe.integerType()));
		// int2PointerFunc = universe.symbolicConstant(universe
		// .stringObject(INT_TO_POINTER_FUNCTION), universe.functionType(
		// Arrays.asList(this.universe.integerType()), this.pointerType));
		this.civlConfig = config;
		// this.zeroOrOne = (NumericExpression) universe.symbolicConstant(
		// universe.stringObject("ZeroOrOne"), universe.integerType());
		this.bitVectorType = universe.bitVectorType(INTEGER_BIT_LENGTH);
		this.functionCallExecutor = new FunctionCallExecutor(modelFactory, stateFactory, loaderExec, this,
				symbolicAnalyzer, errorLogger, civlConfig);
	}

	/* ************************** Private Methods ************************** */

	/**
	 * Dereferences a pointer. Logs error when the dereference fails, like when the
	 * pointer is null.
	 * 
	 * @param source               Source code information for error report.
	 * @param state                The state where the dereference happens.
	 * @param process              The process name for error report.
	 * @param referredType         The {@link CIVLType} of the dereference
	 *                             operation.
	 * @param pointer              The pointer to be dereferenced.
	 * @param checkOutput          Is reading of output variable to be checked?
	 * @param analysisOnly         Is this called from pointer reachability
	 *                             analysis?
	 * @param strict               Should the method report undefined value error
	 *                             when the value pointed by the pointer is
	 *                             undefined ? Currently, this filed is false only
	 *                             when executing $copy function.
	 * @param muteErrorSideEffects Should this method mute error side-effects ? i.e.
	 *                             Dereferencing a pointer with error side-effects
	 *                             <strong> results an undefined value of the same
	 *                             type as the dereference expression </strong> iff
	 *                             this parameter set to true. Otherwise, an error
	 *                             will be reported and
	 *                             UnsatisfiablePathConditionException will be
	 *                             thrown.
	 * @return A possibly new state and the value of memory space pointed by the
	 *         pointer.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation dereferenceWorker(CIVLSource source, State state, int pid, String process,
			SymbolicExpression pointer, boolean checkOutput, boolean analysisOnly, boolean strict,
			boolean muteErrorSideEffects) throws UnsatisfiablePathConditionException {
		SymbolicExpression deref;

		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		pointer = reasoner.simplify(pointer);

		// C11 6.5.3.2: If an invalid value has been assigned to the
		// pointer, the behavior of the unary * operator is undefined.
		//
		// footnote: Among the invalid values for dereferencing a pointer by
		// the unary * operator are a null pointer, an address
		// inappropriately aligned for the type of object pointed to, and
		// the address of an object after the end of its lifetime.
		dereferenceWorkerErrorChecking(state, pid, process, pointer, muteErrorSideEffects, source);

		ReferenceExpression symRef = symbolicUtil.getSymRef(pointer);
		SymbolicExpression variableValue;
		int vid = symbolicUtil.getVariableId(source, pointer);
		int sid = stateFactory.getDyscopeId(symbolicUtil.getScopeValue(pointer));
		Variable variable;

		// Get the variable value:
		if (sid == ModelConfiguration.DYNAMIC_CONSTANT_SCOPE) {
			variable = modelFactory.model().staticConstantScope().getVariable(vid);
			variableValue = variable.constantValue();
		} else {
			variable = state.getDyscope(sid).lexicalScope().getVariable(vid);
			if (!analysisOnly && checkOutput && civlConfig.isPropertyToggled(CIVLProperty.OUTPUT_READ))
				if (variable.isOutput()) {
					errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
							CIVLProperty.OUTPUT_READ, "Attempt to read output variable " + variable.name().name());
					throw new UnsatisfiablePathConditionException();
				}
			variableValue = state.getDyscope(sid).getValue(vid);
		}
		// Check if variable is uninitialized :
		if (variableValue.isNull())
			if (!strict && symRef.isIdentityReference())
				return new Evaluation(state, variableValue);
			else if (civlConfig.isPropertyToggled(CIVLProperty.UNDEFINED_VALUE) && !muteErrorSideEffects) {
				errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
						CIVLProperty.UNDEFINED_VALUE,
						"Attempt to dereference a pointer that refers " + "to an object with undefined value");
				throw new UnsatisfiablePathConditionException();
			} else
				return new Evaluation(state, universe.dereference(variableValue, symRef));
		// Dereference the variable value according to the
		// ReferenceExpression:
		if (muteErrorSideEffects) {
			deref = universe.dereference(variableValue, symRef);
			return new Evaluation(state, deref);
		} else {
			DereferencedResult derefResult = derefOperator.dereference(variableValue, symRef);

			if (universe.reasoner(state.getPathCondition(universe)).isValid(derefResult.validCondition)) {
				deref = derefResult.value;
				if (vid != 0 || !deref.containsSubobject(ModelConfiguration.getInvalidName(universe)))
					return new Evaluation(state, deref);
			}
		}

		CIVLType variableType = variable.type();
		String variableValueToString = symbolicAnalyzer.symbolicExpressionToString(source, state, variableType,
				variableValue);

		errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
				CIVLProperty.DEREFERENCE,
				"Illegal pointer dereference:\n" + "Pointer : " + pointer + " \nReferred variable : "
						+ variableValueToString + " \nReferred variable type : " + variableType);
		throw new UnsatisfiablePathConditionException();
	}

	/**
	 * <p>
	 * Error checking for dereference operations. This method checks if the given
	 * pointer
	 * <li>has NOT a pointer type</li>
	 * <li>is NOT a concrete pointer</li>
	 * <li>is a NULL pointer</li>
	 * <li>points to a NOT exist dynamic scope</li> <br>
	 * If any the above cases happens, either an error will be logged or an
	 * undefined value will be returned.
	 * </p>
	 * 
	 * @param state                The current state
	 * @param process              The String identifier of the process
	 * @param pointer              The pointer to be dereferenced.
	 * @param muteErrorSideEffects Should this method mute error side-effects ? i.e.
	 *                             Dereferencing a pointer with error side-effects
	 *                             <strong> results an undefined value of the same
	 *                             type as the dereference expression </strong> iff
	 *                             this parameter set to true. Otherwise, an error
	 *                             will be reported and
	 *                             UnsatisfiablePathConditionException will be
	 *                             thrown.
	 * @param source               The {@link CIVLSource} associates with the
	 *                             dereference operation.
	 * @throws UnsatisfiablePathConditionException When muteErrorSideEffects is
	 *                                             false and the pointer falls into
	 *                                             one of the error cases.
	 */
	private void dereferenceWorkerErrorChecking(State state, int pid, String process, SymbolicExpression pointer,
			boolean muteErrorSideEffects, CIVLSource source) throws UnsatisfiablePathConditionException {
		boolean throwPCException = false;

		if (pointer == symbolicUtil.undefinedPointer() || !pointer.type().equals(pointerType)) {
			if (civlConfig.isPropertyToggled(CIVLProperty.UNDEFINED_VALUE) && !muteErrorSideEffects)
				errorLogger.logSimpleError(source, state, pid, process, this.symbolicAnalyzer.stateInformation(state),
						CIVLProperty.UNDEFINED_VALUE, "attempt to deference an invalid pointer");
			throwPCException = true;
		} else if (pointer.operator() != SymbolicOperator.TUPLE) {
			if (civlConfig.isPropertyToggled(CIVLProperty.UNDEFINED_VALUE) && !muteErrorSideEffects)
				errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
						CIVLProperty.UNDEFINED_VALUE,
						"attempt to dereference a pointer that is not concrete.\n" + "pointer value: " + pointer);
			throwPCException = true;
		} else if (symbolicUtil.isNullPointer(pointer)) {
			if (!muteErrorSideEffects)
				// null pointer dereference
				errorLogger.logSimpleError(source, state, pid, process, this.symbolicAnalyzer.stateInformation(state),
						CIVLProperty.DEREFERENCE, "attempt to dereference a null pointer");
			throwPCException = true;
		} else {
			int sid = stateFactory.getDyscopeId(symbolicUtil.getScopeValue(pointer));

			if (sid == ModelConfiguration.DYNAMIC_NULL_SCOPE) {
				if (!muteErrorSideEffects)
					errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
							CIVLProperty.DEREFERENCE,
							"Attempt to dereference pointer into scope" + " which has been removed from state: \n "
									+ symbolicAnalyzer.symbolicExpressionToString(source, state, null, pointer));
				throwPCException = true;
			}
		}
		if (throwPCException && muteErrorSideEffects)
			throw new ErroneousSideEffectException(pointer);
		if (throwPCException)
			throw new UnsatisfiablePathConditionException();
	}

	/**
	 * Evaluates the dynamic type of a given CIVL type at a certain state. When the
	 * CIVL type has some state, e.g., an array type with a variable as the extent,
	 * the type needs to be evaluated.
	 * 
	 * @param state         The current state.
	 * @param pid           The ID of the process where the computation happens.
	 * @param type          The CIVL type to be evaluated for the dynamic type.
	 * @param source        The source code element for error report.
	 * @param isDeclaration The flag denoting if the type is part of a
	 *                      variable/function declaration.
	 * @return The dynamic type of the given type.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation dynamicTypeOf(State state, int pid, CIVLType type, CIVLSource source, boolean isDeclaration)
			throws UnsatisfiablePathConditionException {
		TypeEvaluation typeEval = getDynamicType(state, pid, type, source, isDeclaration);
		SymbolicExpression expr = typeFactory.expressionOfType(type, typeEval.type);

		return new Evaluation(typeEval.state, expr);
	}

	/**
	 * Evaluates an abstract function call.
	 * 
	 * @param state      The current state.
	 * @param pid        The ID of the process that the expression belongs to.
	 * @param expression The abstract function call expression to be evaluated.
	 * @return The value of the expression and the new state.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateAbstractFunctionCall(State state, int pid, AbstractFunctionCallExpression expression)
			throws UnsatisfiablePathConditionException {
		AbstractFunction function = expression.function();
		SymbolicType returnType = function.returnType().getDynamicType(universe);
		List<SymbolicType> argumentTypes = new ArrayList<SymbolicType>();
		List<SymbolicExpression> arguments = new ArrayList<SymbolicExpression>();
		SymbolicType functionType;
		SymbolicExpression functionExpression;
		SymbolicExpression functionApplication;
		Evaluation result;
		String functionName = function.name().name();

		for (Variable param : function.parameters()) {
			argumentTypes.add(param.type().getDynamicType(universe));
		}
		for (Expression arg : expression.arguments()) {
			Evaluation eval = evaluate(state, pid, arg);
			arguments.add(eval.value);
		}
		// process atrribute
		switch (function.getAttribute()) {
		case PARTIAL_ORDER:
			functionType = universe.functionType(argumentTypes, returnType,
					SymbolicFunctionType.SpecialRelationKind.PARTIAL_ORDER);
			break;
		case TREE_ORDER:
			functionType = universe.functionType(argumentTypes, returnType,
					SymbolicFunctionType.SpecialRelationKind.TREE_ORDER);
			break;
		case LINEAR_ORDER:
			functionType = universe.functionType(argumentTypes, returnType,
					SymbolicFunctionType.SpecialRelationKind.LINEAR_ORDER);
			break;
		case PIECEWISE_LINEAR_ORDER:
			functionType = universe.functionType(argumentTypes, returnType,
					SymbolicFunctionType.SpecialRelationKind.PIECEWISE_LINEAR_ORDER);
			break;
		default:
			functionType = universe.functionType(argumentTypes, returnType);
		}

		StringObject funcName = ModelConfiguration.getAbstractFunctionName(universe, functionName);

		functionExpression = universe.symbolicConstant(funcName, functionType);
		functionApplication = universe.apply(functionExpression, arguments);
		result = new Evaluation(state, functionApplication);
		return result;
	}

	/**
	 * Evaluates an address-of expression <code>&e</code>.
	 * 
	 * @param state      the pre-state
	 * @param pid        PID of the process performing the evaluation
	 * @param expression the address-of expression
	 * @return the symbolic expression of pointer type resulting from evaluating the
	 *         address of the argument and a new state (if there is side-effect,
	 *         otherwise just return the original state)
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateAddressOf(State state, int pid, AddressOfExpression expression)
			throws UnsatisfiablePathConditionException {
		if (expression.isFieldOffset()) {
			CIVLType structType = expression.getTypeForOffset();
			SymbolicExpression typeValue = typeFactory.expressionOfType(structType,
					structType.getDynamicType(universe));
			SymbolicExpression value = universe.apply(offsetFunction,
					Arrays.asList(typeValue, universe.integer(expression.getFieldIndex())));
			return new Evaluation(state, value);
		} else
			return reference(state, pid, expression.operand());
	}

	/**
	 * Evaluates a short-circuit "and" expression <code>p && q</code>.
	 * 
	 * @param state      the pre-state
	 * @param pid        PID of the process evaluating this expression
	 * @param expression the and expression
	 * @return the result of applying the AND operator to the two arguments together
	 *         with the post-state whose path condition may contain the side-effects
	 *         resulting from evaluation
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateAnd(State state, int pid, BinaryExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.left());
		BooleanExpression leftValue = (BooleanExpression) eval.value;

		// if (enableShortCircuitLogicEval) {
		if (leftValue.isTrue())
			return evaluate(eval.state, pid, expression.right());
		if (leftValue.isFalse()) {
			// false && x = false;
			eval.value = universe.falseExpression();
			return eval;
		}
		// } else {
		// System.out.println("here");// for debugging
		// }
		eval = evaluate(eval.state, pid, expression.right());
		eval.value = universe.and(leftValue, (BooleanExpression) eval.value);
		return eval;
	}

	/**
	 * Evaluate a conditional expression (ternary expression): condition ?
	 * trueBranch : falseBranch.
	 * 
	 * @param state                  The pre-state.
	 * @param pid                    PID of the process evaluating this expression
	 * @param conditionalExpression. The conditional expression to be evaluated.
	 * @return The evaluation result of the conditional expression.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateConditionalExpression(State state, int pid,
			ConditionalExpression conditionalExpression) throws UnsatisfiablePathConditionException {
		Evaluation eva = evaluate(state, pid, conditionalExpression.getCondition());
		BooleanExpression conEval = (BooleanExpression) eva.value;
		SymbolicExpression trueBranch, falseBranch;

		if (conEval.isTrue())
			return evaluate(eva.state, pid, conditionalExpression.getTrueBranch());
		if (conEval.isFalse())
			return evaluate(eva.state, pid, conditionalExpression.getFalseBranch());
		eva = evaluate(eva.state, pid, conditionalExpression.getTrueBranch());
		trueBranch = eva.value;
		eva = evaluate(eva.state, pid, conditionalExpression.getFalseBranch());
		falseBranch = eva.value;
		eva.value = universe.cond(conEval, trueBranch, falseBranch);
		return eva;
	}

	/**
	 * Evaluates a binary expression. Either this throws an unsatisfiable path
	 * condition exception or it returns a non-null state and a non-null value if
	 * there is no error during the evaluation.
	 * 
	 * @param state      The state of the program.
	 * @param pid        The PID of the currently executing process.
	 * @param process    The name of the process for error report.
	 * @param expression The binary expression.
	 * @return A symbolic expression for the binary operation and a new state if
	 *         there is side-effect.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateBinary(State state, int pid, String process, BinaryExpression expression)
			throws UnsatisfiablePathConditionException {
		BINARY_OPERATOR operator = expression.operator();

		switch (operator) {
		case AND:
			return evaluateAnd(state, pid, expression);
		case OR:
			return evaluateOr(state, pid, expression);
		// TODO code review
		case IMPLIES:
			return evaluateImplies(state, pid, expression);
		case BIT_AND:
			return evaluateBitand(state, pid, expression);
		case BIT_OR:
			return evaluateBitor(state, pid, expression);
		case BIT_XOR:
			return evaluateBitxor(state, pid, expression);
		case SHIFTLEFT:
			return evaluateShiftleft(state, pid, expression);
		case SHIFTRIGHT:
			return evaluateShiftright(state, pid, expression);
		case DIVIDE:
		case LESS_THAN:
		case LESS_THAN_EQUAL:
		case MINUS:
		case MODULO:
		case PLUS:
		case POINTER_ADD:
		case POINTER_SUBTRACT:
		case TIMES:
			// numeric expression like +,-,*,/,%,etc
			if (expression.left().getExpressionType() != null
					&& expression.left().getExpressionType().equals(typeFactory.scopeType())) {
				return evaluateScopeOperations(state, pid, expression);
			} else {
				return evaluateNumericOperations(state, pid, process, expression);
			}
		case NOT_EQUAL:
		case EQUAL:
			return evaluateNumericOperations(state, pid, process, expression);
		case VALID:
			return evaluateValid(state, pid, expression.left(), expression.right(), expression.getSource());
		default:
			throw new CIVLUnimplementedFeatureException("Evaluating binary operator of " + operator + " kind");
		}
	}

	/**
	 * Evaluates a bit and expression.
	 * 
	 * @param state      The state where the evaluation happens.
	 * @param pid        The PID of the process that triggers the evaluation.
	 * @param expression The bit and expression to be evaluated.
	 * @return A possibly new state resulted from side effects during the evaluation
	 *         and the value of the bit and expression.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateBitand(State state, int pid, BinaryExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.left());
		SymbolicExpression left = eval.value, right, result;

		eval = evaluate(eval.state, pid, expression.right());
		right = eval.value;
		state = eval.state;
		result = universe.bitand((NumericExpression) left, (NumericExpression) right);
		return new Evaluation(state, result);
	}

	/**
	 * Evaluates a bit complement expression.
	 * 
	 * @param state      The state where the evaluation happens.
	 * @param pid        The PID of the process that triggers the evaluation.
	 * @param expression The bit complement expression to be evaluated.
	 * @return A possibly new state resulted from side effects during the evaluation
	 *         and the value of the bit complement expression.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation evaluateBitcomplement(State state, int pid, UnaryExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.operand());
		SymbolicExpression operand = eval.value, result;

		state = eval.state;
		result = universe.bitnot((NumericExpression) operand);
		return new Evaluation(state, result);
	}

	/**
	 * Evaluates a bit or expression.
	 * 
	 * @param state      The state where the evaluation happens.
	 * @param pid        The PID of the process that triggers the evaluation.
	 * @param expression The bit or expression to be evaluated.
	 * @return A possibly new state resulted from side effects during the evaluation
	 *         and the value of the bit or expression.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateBitor(State state, int pid, BinaryExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.left());
		SymbolicExpression left = eval.value, right, result;

		eval = evaluate(eval.state, pid, expression.right());
		right = eval.value;
		state = eval.state;
		result = universe.bitor((NumericExpression) left, (NumericExpression) right);
		return new Evaluation(state, result);
	}

	/**
	 * Evaluates a bit xor expression.
	 * 
	 * @param state      The state where the evaluation happens.
	 * @param pid        The PID of the process that triggers the evaluation.
	 * @param expression The bit xor expression to be evaluated.
	 * @return A possibly new state resulted from side effects during the evaluation
	 *         and the value of the bit xor expression.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateBitxor(State state, int pid, BinaryExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.left());
		SymbolicExpression left = eval.value, right, result;

		eval = evaluate(eval.state, pid, expression.right());
		right = eval.value;
		state = eval.state;
		result = universe.bitxor((NumericExpression) left, (NumericExpression) right);
		return new Evaluation(state, result);
	}

	/**
	 * Evaluate a boolean literal expression.
	 * 
	 * @param state      The state of the program.
	 * @param pid        The pid of the currently executing process.
	 * @param expression The boolean literal expression.
	 * @return The symbolic representation of the boolean literal expression and the
	 *         new state if there is side effect during the evaluation.
	 */
	protected Evaluation evaluateBooleanLiteral(State state, int pid, BooleanLiteralExpression expression) {
		return new Evaluation(state, universe.bool(expression.value()));
	}

	/**
	 * Evaluates a cast expression.
	 * 
	 * @param state      The state of the program.
	 * @param pid        The pid of the currently executing process.
	 * @param process    The process name for error report.
	 * @param expression The cast expression.
	 * @return A possibly new state resulted from side effects during the evaluation
	 *         and the value of the cast expression.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateCast(State state, int pid, String process, CastExpression expression)
			throws UnsatisfiablePathConditionException {
		CIVLType cast2type = expression.getCastType();

		if (cast2type.isSetType()) {
			assert cast2type.typeKind() == TypeKind.MEM;
			return castPointerSet2Mem(state, pid, expression.getExpression());
		}
		return this.evaluateCastWorker(state, pid, process, expression.getCastType(), expression.getExpression());
	}

	/**
	 * Evaluates expression of the form: <code>($mem)expr</code>.
	 */
	private Evaluation castPointerSet2Mem(State state, int pid, Expression pointerSet)
			throws UnsatisfiablePathConditionException {
		if (this.memExpressionEvaluator == null)
			this.memExpressionEvaluator = new MemEvaluator(modelFactory, stateFactory, libLoader, libExeLoader,
					symbolicUtil, symbolicAnalyzer, memUnitFactory, errorLogger, civlConfig);

		return memExpressionEvaluator.evaluateMemCastingExpression(state, pid, pointerSet);
	}

	/**
	 * Given a struct type, this method determines if the given type equals the
	 * struct type, or equals the type of the first (index 0) field of the struct
	 * type, or if that first field is itself a struct type and the given type is
	 * the first field of that type, or ....
	 * 
	 * @param structType a non-null struct type
	 * @param type       any non-null type
	 * @return if <code>type</code> is found by repeating the first field operation
	 *         starting with <code>structType</code> some k>=0 times, this returns
	 *         k, else returns -1
	 */
	private int firstFieldLevel(CIVLStructOrUnionType structType, CIVLType type) {
		int count = 0;
		CIVLType memberType = structType;
		while (true) {
			if (memberType.equals(type))
				return count;
			if (!memberType.isStructType())
				return -1;
			memberType = ((CIVLStructOrUnionType) memberType).getField(0).type();
			count++;
		}
	}

	/**
	 * Given a pointer value {@code ptr} of static type {@code oldType}, and a new
	 * pointer type {@code newType}, where one of these types is pointer to struct
	 * and the other is the result of 0 or more applications of "type of first field
	 * of struct", this method attempts to cast the pointer value to the new type.
	 * 
	 * @param ptr     original pointer value
	 * @param oldType CIVL static type of pointer value
	 * @param newType new CIVL static type
	 * @return new value of new type, or {@code null} if the types are not related
	 *         by the first-field relation or for some reason the cast is not
	 *         possible
	 */
	private SymbolicExpression castStructPointer(SymbolicExpression ptr, CIVLPointerType oldType,
			CIVLPointerType newType) {
		if (oldType.equals(newType))
			return ptr;
		CIVLType oldBaseType = oldType.baseType(), newBaseType = newType.baseType();
		ReferenceExpression ref = symbolicUtil.getSymRef(ptr);
		if (oldBaseType.isStructType()) {
			int level = firstFieldLevel((CIVLStructOrUnionType) oldBaseType, newBaseType);
			if (level >= 0) {
				// the cast navigates down into the struct
				for (int i = 0; i < level; i++) {
					// convert ref to struct to ref to field 0 of struct
					ref = universe.tupleComponentReference(ref, zeroObj);
				}
				return symbolicUtil.setSymRef(ptr, ref);
			}
		}
		if (newBaseType.isStructType()) {
			int level = firstFieldLevel((CIVLStructOrUnionType) newBaseType, oldBaseType);
			if (level >= 0) {
				// the cast navigates up to the parent struct
				for (int i = 0; i < level; i++) {
					// if this cast is not possible there is nothing you can do.
					// without a concrete pointer, how can you get the parent?
					if (!(ref instanceof TupleComponentReference))
						return null;
					ref = ((TupleComponentReference) ref).getParent();
				}
				return symbolicUtil.setSymRef(ptr, ref);
			}
		}
		return null;
	}

	@Override
	public Evaluation evaluateCastWorker(State state, int pid, String process, CIVLType castType, Expression arg)
			throws UnsatisfiablePathConditionException {
		CIVLType argType = arg.getExpressionType();
		Evaluation eval = evaluate(state, pid, arg);
		SymbolicExpression value = eval.value;
		TypeEvaluation typeEval = getDynamicType(eval.state, pid, castType, arg.getSource(), false);
		SymbolicType endType = typeEval.type;

		state = typeEval.state;
		if (argType.isDomainType() && castType.isDomainType()) {
			return new Evaluation(state, value);
		} else if (argType.isBoolType() && castType.isIntegerType()) {
			eval.value = value.isTrue() ? one
					: value.isFalse() ? zero : universe.cond((BooleanExpression) value, one, zero);
			return eval;
		} else if (argType.isIntegerType() && castType.isBoolType()) {
			if (value.type().isBoolean())
				eval.value = value;
			else
				eval.value = universe.not(universe.equals(value, zero));
			return eval;
		} else if (argType.isIntegerType() && castType.isPointerType()) {
			SymbolicType type = value.type();

			assert type.isInteger();
			eval.value = (new Int2PointerCaster(universe, symbolicUtil, this.pointerType))
					.apply(state.getPathCondition(universe), value, castType);
			return eval;
		} else if (argType.isPointerType() && castType.isIntegerType()) {
			SymbolicType type = value.type();

			assert type == pointerType;
			eval.value = (new Pointer2IntCaster(universe, symbolicUtil, this.pointerType))
					.apply(state.getPathCondition(universe), value, castType);
			return eval;
		} else if (argType.isPointerType() && castType.isPointerType()) {
			// first try struct pointer conversion...
			SymbolicExpression newValue = castStructPointer(value, (CIVLPointerType) argType,
					(CIVLPointerType) castType);
			if (newValue != null) { // success
				eval.value = newValue;
				return eval;
			}
			// try some trivial pointer conversions...
			CIVLType argBaseType = ((CIVLPointerType) argType).baseType(),
					castBaseType = ((CIVLPointerType) castType).baseType();
			if (castBaseType.isFunction()) {
				return eval;
			} else if (!castBaseType.isCharType() && !argBaseType.isCharType() && !castBaseType.isVoidType()
					&& !argBaseType.isVoidType() && !argBaseType.equals(castBaseType)) {
				// eval.value.type()
				throw new CIVLUnimplementedFeatureException(
						"type conversion from pointer-to-" + argBaseType + " to pointer-to-" + castBaseType,
						arg.getSource());
			}
			return eval;
		} else if (argType.isRealType() && castType.isIntegerType()) {
			eval.value = realToIntegerCastWorker(state, pid, (NumericExpression) value);
			return eval;
		} else if (castType.typeKind() == TypeKind.MEM) {
			eval.value = new Pointer2MemCaster(universe).apply(universe.trueExpression(), value, castType);
			return eval;
		}
		try {
			eval.value = universe.cast(endType, eval.value);
		} catch (SARLException e) {
			if (civlConfig.isPropertyToggled(CIVLProperty.INVALID_CAST)) {
				errorLogger.logSimpleError(arg.getSource(), state, pid, process,
						this.symbolicAnalyzer.stateInformation(state), CIVLProperty.INVALID_CAST,
						"SARL could not cast: " + e);
				throw new UnsatisfiablePathConditionException();
			}
		}
		return eval;
	}

	/**
	 * Evaluate the binary expression: valid( pointer, offsets) which expresses that
	 * (pointer + offsets) represents a set of dereferable pointers.
	 * 
	 * @param state   The current state
	 * @param pid     The PID of the calling process
	 * @param pointer The expression representing the pointer (base address).
	 * @param offsets A set of integers which represents a set of offsets.
	 * @return The evaluation of the expression.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateValid(State state, int pid, Expression pointer, Expression offsets, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		return new QuantifiedExpressionEvaluator(modelFactory, stateFactory, libLoader, libExeLoader, symbolicUtil,
				symbolicAnalyzer, memUnitFactory, errorLogger, civlConfig)
				.evaluateValid(state, pid, pointer, offsets, source);
	}

	/**
	 * Cast a real numeric expression e to an integral expression. If e has a
	 * concrete value, the result is {@link SymbolicUniverse#roundToZero(e)}, else,
	 * {@link SymbolicUniverse#cast(integerType, e)}.
	 * 
	 * @param state     The current state
	 * @param pid       The PID of the calling process
	 * @param realValue a numeric expression of real type
	 * @return a numeric expression of integer type casted from the 'realValue'.
	 */
	private NumericExpression realToIntegerCastWorker(State state, int pid, NumericExpression realValue) {
		Number number = universe.extractNumber(realValue);

		if (number == null) {
			Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));

			number = reasoner.extractNumber(realValue);
		}
		if (number != null)
			return universe.roundToZero(universe.number(number));
		else
			return (NumericExpression) universe.cast(universe.integerType(), realValue);
	}

	/**
	 * Evaluates a char literal expression.
	 * 
	 * @param state      The state of the program.
	 * @param pid        The pid of the currently executing process.
	 * @param expression The char literal expression.
	 * @return A possibly new state resulted from side effects during the evaluation
	 *         and the value of the char literal expression.
	 */
	protected Evaluation evaluateCharLiteral(State state, int pid, CharLiteralExpression expression) {
		return new Evaluation(state, universe.character(expression.value()));
	}

	/**
	 * Evaluates a dereference expression <code>*e</code>.
	 * 
	 * @param state      the pre-state
	 * @param pid        PID of the process performing the evaluation
	 * @param process    The process name for error report.
	 * @param expression the dereference expression
	 * @return the evaluation with the properly updated state and the value after
	 *         the dereference.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateDereference(State state, int pid, String process, DereferenceExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.pointer());

		return dereference(expression.pointer().getSource(), eval.state, pid, process, eval.value, true, true);
	}

	/**
	 * Evaluates a domain guard expression, the value of which is true iff there is
	 * a subsequent element of of the current one in the domain object. See also
	 * {@link DomainGuardExpression}.
	 * 
	 * @param state       The current state
	 * @param pid         The PID of the process
	 * @param domainGuard The domain guard expression to be evaluated, which
	 *                    contains the information of the current domain element and
	 *                    the domain object.
	 * @return the evaluation with the properly updated state and the value of the
	 *         domain guard expression.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateDomainGuard(State state, int pid, DomainGuardExpression domainGuard)
			throws UnsatisfiablePathConditionException {
		Expression domain = domainGuard.domain();
		int dimension = domainGuard.dimension();
		// Collection for storing given domain element.
		List<SymbolicExpression> domElement = new LinkedList<>();
		SymbolicExpression domainValue;
		// The value of the domain union.
		SymbolicExpression domainUnion;
		Evaluation eval = this.evaluate(state, pid, domain);
		// Result, if there is a subsequence.
		boolean hasNext = false;
		// Flag indicating the given domain element only contains NULLs.
		boolean isAllNull = true;

		state = eval.state;
		domainValue = eval.value;
		domainUnion = universe.tupleRead(domainValue, twoObj);
		// Evaluating the value of the given element.
		for (int i = 0; i < dimension; i++) {
			SymbolicExpression varValue = state.valueOf(pid, domainGuard.variableAt(i));

			domElement.add(varValue);
			if (!varValue.isNull())
				isAllNull = false;
		}
		// If the domain object is a rectangular domain
		if (symbolicUtil.isRectangularDomain(domainValue)) {
			SymbolicExpression recDom = universe.unionExtract(zeroObj, domainUnion);

			if (isAllNull)
				hasNext = !symbolicUtil.isEmptyDomain(domainValue, dimension, domain.getSource());
			else
				hasNext = symbolicUtil.recDomainHasNext(recDom, dimension, domElement);
			eval.state = state;
			// TODO:rectangular domain always has concrete ranges so that the
			// result is always concrete ?
			eval.value = universe.bool(hasNext);
		} else if (symbolicUtil.isLiteralDomain(domainValue)) {
			Variable literalDomCounterVar;

			// TODO: is there a domain that contains none elements ?
			if (isAllNull)
				hasNext = !symbolicUtil.isEmptyDomain(domainValue, dimension, domain.getSource());
			else {
				NumericExpression literalCounter;
				NumericExpression domainSize = symbolicUtil.getDomainSize(domainValue);
				int counter, size;

				// Compare the literal domain counter and the size of the
				// domain.
				literalDomCounterVar = domainGuard.getLiteralDomCounter();
				literalCounter = (NumericExpression) state.valueOf(pid, literalDomCounterVar);
				counter = ((IntegerNumber) universe.extractNumber(literalCounter)).intValue();
				size = ((IntegerNumber) universe.extractNumber(domainSize)).intValue();
				hasNext = counter < size;
			}
		} else
			throw new CIVLInternalException("A domain object is neither a rectangular domain nor a literal domain",
					domainGuard.getSource());
		eval.state = state;
		eval.value = universe.bool(hasNext);
		return eval;
	}

	/**
	 * Evaluates the value of a rectangular domain literal expression.
	 * 
	 * @param state     The current state
	 * @param pid       The PID of the process
	 * @param recDomain The expression of the rectangular domain
	 * @return The evaluation with the properly updated state and the value of the
	 *         rectangular domain literal expression.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateRecDomainLiteral(State state, int pid, RecDomainLiteralExpression recDomain)
			throws UnsatisfiablePathConditionException {
		int dim = recDomain.dimension();
		List<SymbolicExpression> ranges = new ArrayList<>();
		Evaluation eval;
		SymbolicExpression domainV;
		SymbolicType domainType;
		// For a rectangular domain, its range types are all the same.
		// because rectangular domain is an array of ranges
		SymbolicType rangeType;
		List<SymbolicExpression> domValueComponents = new LinkedList<>();
		// ranges should be in form of an array inside a domain.
		SymbolicExpression rangesArray;
		CIVLDomainType civlDomType;
		CIVLType civlRangeType;

		for (int i = 0; i < dim; i++) {
			eval = evaluate(state, pid, recDomain.rangeAt(i));
			state = eval.state;
			ranges.add(eval.value);
		}
		rangeType = ranges.get(0).type();
		civlRangeType = typeFactory.rangeType();
		civlDomType = typeFactory.domainType(civlRangeType);
		domainType = civlDomType.getDynamicType(universe);
		assert domainType instanceof SymbolicTupleType : "Dynamic $domain type is not a tuple type";
		assert rangeType instanceof SymbolicTupleType : "Dynamic $range type is not a tuple type";
		// Adding components
		domValueComponents.add(universe.integer(dim));
		// Union field index which indicates it's a rectangular domain.
		domValueComponents.add(zero);
		rangesArray = universe.array(rangeType, ranges);
		domValueComponents
				.add(universe.unionInject(civlDomType.getDynamicSubTypesUnion(universe), zeroObj, rangesArray));
		// The cast is guaranteed
		// TODO: when is the appropriate time to call universe.canonic() ?
		domainV = universe.tuple((SymbolicTupleType) domainType, domValueComponents);
		return new Evaluation(state, domainV);
	}

	/**
	 * Evaluates a "dot" expression used to navigate to a field in a record,
	 * <code>e.f</code>.
	 * 
	 * @param state      The state of the model
	 * @param pid        The PID of the process evaluating this expression
	 * @param expression The dot expression to evaluated
	 * @return The evaluation which contains the result of evaluating the expression
	 *         together with the post-state which may incorporate side-effects
	 *         resulting from the evaluation
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateDot(State state, int pid, String process, DotExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.structOrUnion());
		SymbolicExpression structValue = eval.value;
		int fieldIndex = expression.fieldIndex();

		if (expression.isStruct()) {
			eval.value = universe.tupleRead(structValue, universe.intObject(fieldIndex));
		} else {
			Expression unionExpr = expression.structOrUnion();
			CIVLStructOrUnionType unionType = (CIVLStructOrUnionType) unionExpr.getExpressionType();
			StructOrUnionField field = unionType.getField(fieldIndex);
			BooleanExpression test = universe.unionTest(universe.intObject(fieldIndex), structValue);

			if (civlConfig.isPropertyToggled(CIVLProperty.UNION) && test.isFalse()) {
				String msg = "Attempt to access an invalid member of union object of type " + unionType + ":\n";

				msg += "Union object: " + structValue + "\n";
				msg += "Attempt to access via field " + fieldIndex + " (" + field + ")";
				errorLogger.logSimpleError(expression.getSource(), eval.state, pid, process,
						this.symbolicAnalyzer.stateInformation(state), CIVLProperty.UNION, msg);
				throw new UnsatisfiablePathConditionException();
			}
			eval.value = universe.unionExtract(universe.intObject(fieldIndex), structValue);
		}
		return eval;
	}

	/**
	 * Evaluates a dynamic type of expression. TODO what's this for?
	 * 
	 * @param state
	 * @param pid
	 * @param expression
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateDynamicTypeOf(State state, int pid, DynamicTypeOfExpression expression)
			throws UnsatisfiablePathConditionException {
		return dynamicTypeOf(state, pid, expression.getType(), expression.getSource(), true);
	}

	/**
	 * Evaluates a function guard expression. When the function is a system
	 * function, the evaluation inquires the corresponding library for its
	 * evaluation; otherwise, the result is always the true value.
	 * 
	 * @param state
	 * @param pid
	 * @param process
	 * @param expression
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateFunctionGuard(State state, int pid, String process, FunctionGuardExpression expression)
			throws UnsatisfiablePathConditionException {
		Triple<State, CIVLFunction, Integer> eval = this.evaluateFunctionIdentifier(state, pid,
				expression.functionExpression(), expression.getSource());
		CIVLFunction function;

		state = eval.first;
		function = eval.second;
		if (function == null) {
			errorLogger.logSimpleError(expression.getSource(), state, pid, process,
					symbolicAnalyzer.stateInformation(state), CIVLProperty.OTHER, "function body can't be found");
			throw new UnsatisfiablePathConditionException();
		}
		if (function.isSystemFunction()) {
			SystemFunction systemFunction = (SystemFunction) function;

			return this.evaluateGuardofSystemFunction(expression.getSource(), state, pid, systemFunction.getLibrary(),
					systemFunction, expression.arguments());
		}
		return new Evaluation(state, universe.trueExpression());
	}

	protected Evaluation evaluateFunctionIdentifierExpression(State state, int pid,
			FunctionIdentifierExpression expression) {
		Scope scope = expression.scope();
		SymbolicExpression dyScopeId = stateFactory.scopeValue(state.getDyscope(pid, scope));
		SymbolicExpression functionPointer = universe.tuple(this.functionPointerType,
				Arrays.asList(dyScopeId, universe.integer(expression.function().fid())));

		return new Evaluation(state, functionPointer);
	}

	protected Evaluation evaluateHereOrRootScope(State state, int pid, HereOrRootExpression expression) {
		int dyScopeID = expression.isRoot() ? state.rootDyscopeID() : state.getProcessState(pid).getDyscopeId();

		return new Evaluation(state, stateFactory.scopeValue(dyScopeID));
	}

	/**
	 * Evaluates a short-circuit "implies" expression "p=?q".
	 * 
	 * @param state      the pre-state
	 * @param pid        PID of the process evaluating this expression
	 * @param expression the and expression
	 * @return the result of applying the IMPLIES operator to the two arguments
	 *         together with the post-state whose path condition may contain the
	 *         side-effects resulting from evaluation
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateImplies(State state, int pid, BinaryExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.left());
		BooleanExpression p = (BooleanExpression) eval.value;

		// If p is false, the implication will evaluate to true.
		if (p.isFalse()) {
			eval.value = universe.trueExpression();
			return eval;
		} else {
			eval = evaluate(eval.state, pid, expression.right());
			eval.value = universe.implies(p, (BooleanExpression) eval.value);
			return eval;
		}
	}

	/**
	 * <p>
	 * <b><Summary: </b>
	 * 
	 * Evaluates an {@link InitialValueExpression}. An
	 * {@link InitialValueExpression} is an initial value that will be assigned to
	 * an uninitialized variable.
	 * </p>
	 * 
	 * @param state      The current state
	 * @param pid        The PID of the current process
	 * @param expression The {@link InitialValueExpression} that will be evaluated
	 * @return The {@link Evaluation} of the {@link InitialValueExpression}
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateInitialValue(State state, int pid, InitialValueExpression expression)
			throws UnsatisfiablePathConditionException {
		Variable variable = expression.variable();
		CIVLType type = variable.type();
		TypeEvaluation typeEval = getDynamicType(state, pid, type, expression.getSource(), false);
		int sid = typeEval.state.getDyscopeID(pid, variable);

		return computeInitialValue(typeEval.state, pid, variable, typeEval.type, sid);
	}

	/**
	 * Computes the symbolic initial value of a variable.
	 * 
	 * @param state       The state where the computation happens.
	 * @param pid         The PID of the process that triggers the computation.
	 * @param variable    The variable to be evaluated.
	 * @param dynamicType The symbolic type of the variable.
	 * @param dyscopeId   The dynamic scope ID of the current state.
	 * @return The symbolic initial value of the given variable
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation computeInitialValue(State state, int pid, Variable variable, SymbolicType dynamicType,
			int dyscopeId) throws UnsatisfiablePathConditionException {
		CIVLType type = variable.type();
		SymbolicExpression result;

		if (!variable.isInput() && variable.isStatic()) {
			// if (!variable.isInput()) {
			return initialValueOfType(state, pid, type);
		} else if (!variable.isInput() && !variable.isBound()
				&& (type instanceof CIVLPrimitiveType || type.isPointerType() || type.isDomainType())) {
			result = nullExpression;
		} else {// the case of an input variable or compound type
			String name;
			StringObject nameObj;

			// TODO temporarily doing this for contract verification, ultimately
			// this should be fixed and the scope id checking should be an
			// assertion instead
			if (variable.isInput() && variable.scope().id() == ModelConfiguration.STATIC_ROOT_SCOPE) {
				// if (variable.isInput()){
				// assert (variable.scope().id() ==
				// ModelConfiguration.STATIC_ROOT_SCOPE);
				name = "X_" + variable.name().name();
				nameObj = universe.stringObject(name);

				result = universe.symbolicConstant(nameObj, dynamicType);
			} else {
				Pair<State, SymbolicConstant> freshSymbol = this.stateFactory.getFreshSymbol(state,
						ModelConfiguration.HAVOC_PREFIX_INDEX, dynamicType);

				state = freshSymbol.left;
				result = freshSymbol.right;
			}
		}
		return new Evaluation(state, result);
	}

	/**
	 * Evaluates an integer literal expression.
	 * 
	 * @param state      The state of the program.
	 * @param pid        The pid of the currently executing process.
	 * @param expression The integer literal expression.
	 * @return The symbolic representation of the integer literal expression.
	 */
	protected Evaluation evaluateIntegerLiteral(State state, int pid, IntegerLiteralExpression expression) {
		return new Evaluation(state, universe.integer(expression.value().intValue()));
	}

	protected Evaluation evaluateNumericOperations(State state, int pid, String process, BinaryExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = this.evaluate(state, pid, expression.left());
		SymbolicExpression left = eval.value;
		SymbolicExpression right;

		eval = evaluate(eval.state, pid, expression.right());
		right = eval.value;
		switch (expression.operator()) {
		case PLUS:
			eval.value = evaluatePlus(left, right);
			break;
		case MINUS:
			eval.value = universe.subtract((NumericExpression) left, (NumericExpression) right);
			break;
		case TIMES:
			eval.value = universe.multiply((NumericExpression) left, (NumericExpression) right);
			break;
		case DIVIDE:
			return evaluateDivide(eval.state, pid, expression, (NumericExpression) left, (NumericExpression) right);
		case LESS_THAN:
			eval.value = universe.lessThan((NumericExpression) left, (NumericExpression) right);
			break;
		case LESS_THAN_EQUAL:
			eval.value = universe.lessThanEquals((NumericExpression) left, (NumericExpression) right);
			break;
		// equal and not_equal operators support scope, process, and pointer
		// types. If the value of those types is undefined (e.g., process
		// -1,
		// scope -1, pointer<-1, ..., ...>), an error should be reported.
		case EQUAL: {
			SymbolicType leftType = left.type(), rightType = right.type();

			this.isValueDefined(eval.state, pid, process, expression.left(), left);
			this.isValueDefined(eval.state, pid, process, expression.right(), right);
			if (leftType.isBoolean() && rightType.isInteger())
				left = universe.cast(rightType, left);
			else if (leftType.isInteger() && rightType.isBoolean())
				right = universe.cast(leftType, right);
			eval.value = universe.equals(left, right);
			break;
		}
		case NOT_EQUAL: {
			SymbolicType leftType = left.type(), rightType = right.type();

			this.isValueDefined(eval.state, pid, process, expression.left(), left);
			this.isValueDefined(eval.state, pid, process, expression.right(), right);
			if (leftType.isBoolean() && rightType.isInteger())
				left = universe.cast(rightType, left);
			else if (leftType.isInteger() && rightType.isBoolean())
				right = universe.cast(leftType, right);

			eval.value = universe.neq(left, right);
			break;
		}
		case MODULO:
			eval = evaluateModulo(eval.state, pid, expression, (NumericExpression) left, (NumericExpression) right);
			break;
		case POINTER_ADD:
			eval = evaluatePointerAdd(eval.state, pid, expression, left, right);
			break;
		case POINTER_SUBTRACT: {
			if (right.isNumeric())
				eval = this.evaluatePointerAdd(state, pid, expression, left, universe.minus((NumericExpression) right));
			else
				eval = pointerSubtraction(eval.state, pid, process, expression, left, right);
			break;

		}
		case IMPLIES:
		case AND:
		case OR:
			throw new CIVLInternalException("unreachable", expression);
		default:
			throw new CIVLUnimplementedFeatureException("Evaluating numeric operator " + expression.operator(),
					expression);
		}
		return eval;
	}

	/**
	 * <p>
	 * Evaluates a modulo operation.
	 * </p>
	 * 
	 * @param state       The current state
	 * @param pid         The PID of the calling process
	 * @param expression  A {@link BinaryExpression} which represents a division
	 *                    operation.
	 * @param numerator   The value of the numerator
	 * @param denominator The value of the denominator
	 * @return The evaluation of this operation.
	 * @throws UnsatisfiablePathConditionException When the denominator equals to
	 *                                             zero.
	 */
	protected Evaluation evaluateModulo(State state, int pid, BinaryExpression expression, NumericExpression numerator,
			NumericExpression denominator) throws UnsatisfiablePathConditionException {
		return evaluateModuloWorker(state, pid, expression, numerator, denominator, false);
	}

	/**
	 * <p>
	 * Worker method for evaluating a modulo operation.
	 * </p>
	 * 
	 * @param state                The current state
	 * @param pid                  The PID of the calling process
	 * @param expression           A {@link BinaryExpression} which represents a
	 *                             division operation.
	 * @param numerator            The value of the numerator
	 * @param denominator          The value of the denominator
	 * @param muteErrorSideEffects Division by zero error will NOT be reported iff
	 *                             this parameter is set to true or
	 *                             civlConfig.checkDivisionByZero() is false.
	 * @return The evaluation of this operation.
	 * @throws UnsatisfiablePathConditionException When the denominator equals to
	 *                                             zero.
	 */
	protected Evaluation evaluateModuloWorker(State state, int pid, BinaryExpression expression,
			NumericExpression numerator, NumericExpression denominator, boolean muteErrorSideEffects)
			throws UnsatisfiablePathConditionException {
		BooleanExpression assumption = state.getPathCondition(universe);
		SymbolicExpression result = null;

		if (!muteErrorSideEffects && civlConfig.isPropertyToggled(CIVLProperty.DIVISION_BY_ZERO)) {
			BooleanExpression claim = universe.neq(zeroOf(expression.getSource(), expression.getExpressionType()),
					denominator);
			ResultType resultType = universe.reasoner(assumption).valid(claim).getResultType();

			if (resultType != ResultType.YES)
				state = errorLogger.logError(expression.right().getSource(), state, pid,
						symbolicAnalyzer.stateInformation(state), claim, resultType, CIVLProperty.DIVISION_BY_ZERO,
						"Modulus denominator is zero");
		}
		try {
			result = universe.modulo(numerator, denominator);
		} catch (ArithmeticException e) {
			System.err.println("Warning: ignoring a modulus with zero divisor");
			throw new UnsatisfiablePathConditionException();
		}
		return new Evaluation(state, result);
	}

	/**
	 * <p>
	 * Evaluates a division operation.
	 * </p>
	 * 
	 * @param state       The current state
	 * @param pid         The PID of the calling process
	 * @param expression  A {@link BinaryExpression} which represents a division
	 *                    operation.
	 * @param numerator   The value of the numerator
	 * @param denominator The value of the denominator
	 * @return The evaluation of this operation.
	 * @throws UnsatisfiablePathConditionException When the denominator equals to
	 *                                             zero.
	 */
	protected Evaluation evaluateDivide(State state, int pid, BinaryExpression expression, NumericExpression numerator,
			NumericExpression denominator) throws UnsatisfiablePathConditionException {
		return this.evaluateDivideWorker(state, pid, expression, numerator, denominator, false);
	}

	/**
	 * <p>
	 * Worker method for evaluating a division operation.
	 * </p>
	 * 
	 * @param state                The current state
	 * @param pid                  The PID of the calling process
	 * @param expression           A {@link BinaryExpression} which represents a
	 *                             division operation.
	 * @param numerator            The value of the numerator
	 * @param denominator          The value of the denominator
	 * @param muteErrorSideEffects Division by zero error will NOT be reported iff
	 *                             this parameter is set to true
	 * @return The evaluation of this operation.
	 * @throws UnsatisfiablePathConditionException When the denominator equals to
	 *                                             zero.
	 */
	protected Evaluation evaluateDivideWorker(State state, int pid, BinaryExpression expression,
			NumericExpression numerator, NumericExpression denominator, boolean muteErrorSideEffects)
			throws UnsatisfiablePathConditionException {
		BooleanExpression assumption = state.getPathCondition(universe);
		SymbolicExpression result = null;

		if (civlConfig.isPropertyToggled(CIVLProperty.DIVISION_BY_ZERO) && !muteErrorSideEffects) {
			SymbolicExpression zero = zeroOf(expression.getSource(), expression.getExpressionType());
			BooleanExpression claim = universe.neq(zero, denominator);
			ResultType resultType = universe.reasoner(assumption).valid(claim).getResultType();

			if (resultType != ResultType.YES) {
				Expression divisor = expression.right();

				state = errorLogger.logError(expression.right().getSource(), state, pid,
						this.symbolicAnalyzer.stateInformation(state), claim, resultType, CIVLProperty.DIVISION_BY_ZERO,
						"division by zero where divisor: " + expression.right() + "="
								+ this.symbolicAnalyzer.symbolicExpressionToString(divisor.getSource(), state,
										divisor.getExpressionType(), denominator));
			}
		}
		try {
			result = universe.divide((NumericExpression) numerator, denominator);
		} catch (ArithmeticException e) {
			System.err.println("Warning: ignoring a division by zero");
			throw new UnsatisfiablePathConditionException();
		}
		return new Evaluation(state, result);
	}

	/**
	 * Evaluates a short-circuit "or" expression "p||q".
	 * 
	 * @param state      the pre-state
	 * @param pid        PID of the process evaluating this expression
	 * @param expression the OR expression
	 * @return the result of applying the OR operator to the two arguments together
	 *         with the post-state whose path condition may contain the side-effects
	 *         resulting from evaluation
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateOr(State state, int pid, BinaryExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.left());
		BooleanExpression p = (BooleanExpression) eval.value;

		// if (enableShortCircuitLogicEval) {
		if (p.isTrue()) {
			eval.value = universe.trueExpression();
			return eval;
		}
		if (p.isFalse())
			return evaluate(eval.state, pid, expression.right());
		// } else {
		// System.out.println("here"); // for debugging
		// }
		eval = evaluate(eval.state, pid, expression.right());
		eval.value = universe.or(p, (BooleanExpression) eval.value);
		return eval;
	}

	protected SymbolicExpression lambda(State state, int pid, NumericSymbolicConstant[] boundVariables, int boundIndex,
			SymbolicFunctionType arrayType, Expression body) throws UnsatisfiablePathConditionException {
		NumericSymbolicConstant index = boundVariables[boundIndex];
		SymbolicExpression eleValue;
		Evaluation eval;

		if (boundIndex == boundVariables.length - 1) {
			eval = this.evaluate(state, pid, body);
			eleValue = eval.value;
			state = eval.state;
		} else {
			eleValue = lambda(state, pid, boundVariables, boundIndex + 1, arrayType, body);
		}
		return universe.lambda(index, eleValue);
	}

	/**
	 * Creates an array lambda symbolic expression recursively (If it is a
	 * multi-dimensional array, the created one will be a nested array lambda
	 * expression).
	 * 
	 * 
	 * @param state          The state where the array lambda expression body will
	 *                       evaluate
	 * @param pid            The PID of the process who invokes the creation of
	 *                       array lambda expressions.
	 * @param boundVariables An array of bound variables specified by the array
	 *                       lambda expression
	 * @param boundIndex     The index of the bound variable in the array which
	 *                       belongs to the current sub-array-lambda expression.
	 * @param arrayType      The symbolic type of a array lambda expression, which
	 *                       must be a complete array type
	 * @param body           The lambda expression body of the array lambda
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	protected SymbolicExpression arrayLambda(State state, int pid, NumericSymbolicConstant[] boundVariables,
			int boundIndex, SymbolicCompleteArrayType arrayType, Expression body)
			throws UnsatisfiablePathConditionException {
		NumericSymbolicConstant index = boundVariables[boundIndex];
		SymbolicExpression eleValue;
		SymbolicExpression arrayEleFunction;
		Evaluation eval;
		BooleanExpression arrayIndexingConstraint = universe.and(universe.lessThanEquals(this.zero, index),
				universe.lessThan(index, arrayType.extent()));

		state = stateFactory.addToPathcondition(state, pid, arrayIndexingConstraint);
		if (boundIndex == boundVariables.length - 1) {
			eval = this.evaluate(state, pid, body);
			eleValue = eval.value;
			state = eval.state;
		} else {
			eleValue = arrayLambda(state, pid, boundVariables, boundIndex + 1,
					(SymbolicCompleteArrayType) arrayType.elementType(), body);
		}
		arrayEleFunction = universe.lambda(index, eleValue);
		return universe.arrayLambda(arrayType, arrayEleFunction);
	}

	//
	// // TODO break into small functions
	// protected Evaluation evaluateQuantifiedExpression(State state, int pid,
	// QuantifiedExpression expression)
	// throws UnsatisfiablePathConditionException {
	// Evaluation result;
	// Evaluation quantifiedExpression;
	// BooleanExpression context;
	// Reasoner reasoner;
	// BooleanExpression simplifiedExpression;
	// SymbolicConstant boundVariable = universe.symbolicConstant(expression
	// .boundVariableName().stringObject(), expression
	// .boundVariableType().getDynamicType(universe));
	// State stateWithRestriction;
	//
	// boundVariables.push(boundVariable);
	// if (expression.isRange()) {
	// Evaluation range = evaluate(state, pid, expression.restriction());
	// // Evaluation lower = evaluate(state, pid, expression.lower());
	// // Evaluation upper = evaluate(state, pid, expression.upper());
	// BooleanExpression rangeRestriction;
	// NumericExpression lower, upper, upperBound;
	// ResultType isRestrictionInValid;
	//
	// assert expression.restriction().getExpressionType()
	// .equals(this.typeFactory.rangeType());
	// lower = this.symbolicUtil.getLowOfRegularRange(range.value);
	// upper = this.symbolicUtil.getHighOfRegularRange(range.value);
	// if (!this.symbolicUtil.getStepOfRegularRange(range.value).isOne()) {
	// errorLogger
	// .logSimpleError(
	// expression.getSource(),
	// state,
	// state.getProcessState(pid).name(),
	// this.symbolicAnalyzer.stateInformation(state),
	// CIVLProperty.OTHER,
	// "the range expression of bound variables in "
	// + "quantified expression only allows one as the step");
	// throw new UnsatisfiablePathConditionException();
	// }
	// // assert lower.value instanceof NumericExpression;
	// // assert upper.value instanceof NumericExpression;
	// upperBound = universe.add(one, upper);
	// rangeRestriction = universe.and(universe.lessThanEquals(lower,
	// (NumericExpression) boundVariable), universe
	// .lessThanEquals((NumericExpression) boundVariable, upper));
	// reasoner = universe.reasoner(state.getPathCondition());
	// isRestrictionInValid = reasoner.valid(
	// universe.not(rangeRestriction)).getResultType();
	// if (isRestrictionInValid == ResultType.YES) {
	// // invalid range restriction
	// switch (expression.quantifier()) {
	// case EXISTS:
	// result = new Evaluation(state, universe.falseExpression());
	// break;
	// default:// FORALL UNIFORM
	// result = new Evaluation(state, universe.trueExpression());
	// }
	// } else {
	// // TODO change to andTo
	// stateWithRestriction = state.setPathCondition(universe.and(
	// (BooleanExpression) rangeRestriction,
	// state.getPathCondition()));
	// quantifiedExpression = evaluate(stateWithRestriction, pid,
	// expression.expression());
	// switch (expression.quantifier()) {
	// case EXISTS:
	// result = new Evaluation(state, universe.existsInt(
	// (NumericSymbolicConstant) boundVariable, lower,
	// upperBound,
	// (BooleanExpression) quantifiedExpression.value));
	// break;
	// case FORALL:
	// result = new Evaluation(state, universe.forallInt(
	// (NumericSymbolicConstant) boundVariable, lower,
	// upperBound,
	// (BooleanExpression) quantifiedExpression.value));
	// break;
	// case UNIFORM:
	// result = new Evaluation(state, universe.forallInt(
	// (NumericSymbolicConstant) boundVariable, lower,
	// upperBound,
	// (BooleanExpression) quantifiedExpression.value));
	// break;
	// default:
	// throw new CIVLException("Unknown quantifier ",
	// expression.getSource());
	// }
	// }
	// } else {
	// Evaluation restriction = evaluate(state, pid,
	// expression.restriction());
	// Interval interval = null;
	// NumericExpression lower = null, upper = null;
	// ResultType isRestrictionInValid;
	//
	// reasoner = universe.reasoner(state.getPathCondition());
	// isRestrictionInValid = reasoner.valid(
	// universe.not((BooleanExpression) restriction.value))
	// .getResultType();
	// if (isRestrictionInValid == ResultType.YES) {
	// // invalid range restriction
	// switch (expression.quantifier()) {
	// case EXISTS:
	// result = new Evaluation(state, universe.falseExpression());
	// break;
	// default:// FORALL UNIFORM
	// result = new Evaluation(state, universe.trueExpression());
	// }
	// } else {
	// stateWithRestriction = state.setPathCondition(universe.and(
	// (BooleanExpression) restriction.value,
	// state.getPathCondition()));
	// quantifiedExpression = evaluate(stateWithRestriction, pid,
	// expression.expression());
	// // By definition, the restriction should be boolean valued.
	// assert restriction.value instanceof BooleanExpression;
	// context = universe.and(state.getPathCondition(),
	// (BooleanExpression) restriction.value);
	// reasoner = universe.reasoner(context);
	// simplifiedExpression = (BooleanExpression) reasoner
	// .simplify(quantifiedExpression.value);
	// interval = reasoner.assumptionAsInterval(boundVariable);
	// if (interval != null) {
	// lower = universe.number(interval.lower());
	// upper = universe.add(universe.number(interval.upper()),
	// this.one);
	// }
	// switch (expression.quantifier()) {
	// case EXISTS:
	// if (interval != null)
	// result = new Evaluation(
	// state,
	// universe.existsInt(
	// (NumericSymbolicConstant) boundVariable,
	// lower,
	// upper,
	// (BooleanExpression) simplifiedExpression));
	// else
	// result = new Evaluation(state, universe.exists(
	// boundVariable, universe.and(
	// (BooleanExpression) restriction.value,
	// simplifiedExpression)));
	// break;
	// case FORALL:
	// if (interval != null)
	// result = new Evaluation(
	// state,
	// universe.forallInt(
	// (NumericSymbolicConstant) boundVariable,
	// lower,
	// upper,
	// (BooleanExpression) simplifiedExpression));
	// else
	// result = new Evaluation(state, universe.forall(
	// boundVariable, universe.implies(
	// (BooleanExpression) restriction.value,
	// simplifiedExpression)));
	// break;
	// case UNIFORM:
	// if (interval != null)
	// result = new Evaluation(
	// state,
	// universe.forallInt(
	// (NumericSymbolicConstant) boundVariable,
	// lower,
	// upper,
	// (BooleanExpression) simplifiedExpression));
	// else
	// result = new Evaluation(state, universe.forall(
	// boundVariable, universe.implies(
	// (BooleanExpression) restriction.value,
	// simplifiedExpression)));
	// break;
	// default:
	// throw new CIVLException("Unknown quantifier ",
	// expression.getSource());
	// }
	// }
	//
	// }
	// boundVariables.pop();
	// return result;
	// }

	protected Evaluation evaluateRegularRange(State state, int pid, RegularRangeExpression range)
			throws UnsatisfiablePathConditionException {
		SymbolicTupleType type = (SymbolicTupleType) range.getExpressionType().getDynamicType(universe);
		Evaluation eval = this.evaluate(state, pid, range.getLow());
		SymbolicExpression low, high, step, rangeValue;
		BooleanExpression claim;
		boolean negativeStep = false;
		ResultType validity;
		String process = state.getProcessState(pid).name() + "(id = " + pid + ")";

		low = eval.value;
		state = eval.state;
		eval = evaluate(state, pid, range.getHigh());
		high = eval.value;
		state = eval.state;
		eval = evaluate(state, pid, range.getStep());
		step = eval.value;
		state = eval.state;
		claim = universe.equals(this.zero, step);
		validity = universe.reasoner(state.getPathCondition(universe)).valid(claim).getResultType();
		if (validity == ResultType.YES) {
			errorLogger.logSimpleError(range.getSource(), state, pid, process, symbolicAnalyzer.stateInformation(state),
					CIVLProperty.OTHER, "a regular range expression requires a non-zero step");
			throw new UnsatisfiablePathConditionException();
		}
		claim = universe.lessThan(this.zero, (NumericExpression) step);
		validity = universe.reasoner(state.getPathCondition(universe)).valid(claim).getResultType();
		if (validity == ResultType.NO)
			negativeStep = true;
		if (negativeStep) {
			SymbolicExpression tmp = low;

			low = high;
			high = tmp;
		}
		rangeValue = universe.tuple(type, Arrays.asList(low, high, step));
		return new Evaluation(state, rangeValue);
	}

	protected Evaluation evaluateScopeOperations(State state, int pid, BinaryExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.left());
		int left = stateFactory.getDyscopeId(eval.value);
		int right;
		boolean result;

		state = eval.state;
		eval = evaluate(state, pid, expression.right());
		state = eval.state;
		right = stateFactory.getDyscopeId(eval.value);
		switch (expression.operator()) {
		case PLUS:
			int lowestCommonAncestor = stateFactory.lowestCommonAncestor(state, left, right);

			eval.value = stateFactory.scopeValue(lowestCommonAncestor);
			break;
		case LESS_THAN:
			result = stateFactory.isDescendantOf(state, right, left);
			eval.value = universe.bool(result);
			break;
		case LESS_THAN_EQUAL:
			result = (left == right) ? true : stateFactory.isDescendantOf(state, right, left);
			eval.value = universe.bool(result);
			break;
		case EQUAL:
			eval.value = universe.bool(left == right);
			break;
		case NOT_EQUAL:
			eval.value = universe.bool(left != right);
			break;
		default:
			throw new CIVLUnimplementedFeatureException("evaluting scope operator " + expression.operator(),
					expression.getSource());
		}
		return eval;
	}

	protected Evaluation evaluateSizeofExpressionExpression(State state, int pid, SizeofExpression expression)
			throws UnsatisfiablePathConditionException {
		return evaluateSizeofType(expression.getSource(), state, pid, expression.getArgument().getExpressionType());
	}

	protected Evaluation evaluateSizeofTypeExpression(State state, int pid, SizeofTypeExpression expression)
			throws UnsatisfiablePathConditionException {
		return evaluateSizeofType(expression.getSource(), state, pid, expression.getTypeArgument());
	}

	/**
	 * <p>
	 * Evaluating a subscript expression.
	 * </p>
	 * 
	 * @param state      The state of the program.
	 * @param pid        The pid of the currently executing process.
	 * @param expression The array index expression.
	 * @return A symbolic expression for an array read.
	 */
	protected Evaluation evaluateSubscript(State state, int pid, String process, SubscriptExpression expression)
			throws UnsatisfiablePathConditionException {
		return this.evaluateSubscriptWorker(state, pid, process, expression, false);
	}

	/**
	 * <p>
	 * Worker method for evaluating a subscript expression.
	 * </p>
	 * 
	 * @param state               The state of the program.
	 * @param pid                 The pid of the currently executing process.
	 * @param expression          The array subscript expression.
	 * @param muteErrorSideEffect Array index out-of bound error will NOT be
	 *                            reported iff this parameter sets to true.
	 * @return A symbolic expression for an array read.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateSubscriptWorker(State state, int pid, String process, SubscriptExpression expression,
			boolean muteErrorSideEffect) throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.array());
		SymbolicExpression array = eval.value;
		SymbolicArrayType arrayType = (SymbolicArrayType) array.type();
		NumericExpression index;

		eval = evaluate(state, pid, expression.index());
		index = (NumericExpression) eval.value;
		if (!muteErrorSideEffect && civlConfig.isPropertyToggled(CIVLProperty.OUT_OF_BOUNDS))
			eval.state = checkArrayIndexInBound(eval.state, pid, expression, arrayType, array, index, false);
		eval.value = universe.arrayRead(array, index);
		return eval;
	}

	protected State checkArrayIndexInBound(State state, int pid, SubscriptExpression expression,
			SymbolicArrayType arrayType, SymbolicExpression array, NumericExpression index, boolean addressOnly)
			throws UnsatisfiablePathConditionException {
		CIVLSource arraySource = expression.array().getSource();
		CIVLSource indexSource = expression.index().getSource();

		NumericExpression length = universe.length(array);
		BooleanExpression assumption = state.getPathCondition(universe);
		BooleanExpression claim, notNegative;
		ResultType resultType;
		Reasoner reasoner = universe.reasoner(assumption);

		notNegative = universe.lessThanEquals(zero, index);
		if (addressOnly)
			claim = universe.or(universe.equals(zero, index),
					universe.and(notNegative, universe.lessThanEquals(index, length)));
		else
			claim = universe.and(notNegative, universe.lessThan(index, length));
		resultType = reasoner.valid(claim).getResultType();
		if (resultType != ResultType.YES) {
			StringBuilder sb = new StringBuilder();

			if (!reasoner.isValid(notNegative))
				sb.append("\nPossible negative array index:");
			else
				sb.append("\nOut of bounds array index:");
			sb.append("\nArray expression: ");
			sb.append(((ABC_CIVLSource) arraySource).getABCSource().getFirstToken().getText());
			sb.append("\nArray type: ");
			sb.append(arrayType);
			sb.append("\nIndex exprssion: ");
			sb.append(((ABC_CIVLSource) indexSource).getABCSource().getFirstToken().getText());
			sb.append("\nIndex value: ");
			sb.append(index);
			sb.append("\n");
			state = errorLogger.logError(indexSource, state, pid, symbolicAnalyzer.stateInformation(state), claim,
					resultType, CIVLProperty.OUT_OF_BOUNDS, sb.toString());
		}
		return state;
	}

	protected Evaluation evaluateSelf(State state, int pid, SelfExpression expression) {
		return new Evaluation(state, stateFactory.processValue(pid));
	}

	protected Evaluation evaluateProcnull(State state, int pid, ProcnullExpression expression) {
		return new Evaluation(state, modelFactory.nullProcessValue());
	}

	/**
	 * Evaluate a real literal expression.
	 * 
	 * @param state      The state of the program.
	 * @param pid        The pid of the currently executing process.
	 * @param expression The real literal expression.
	 * @return The symbolic representation of the real literal expression.
	 */
	protected Evaluation evaluateRealLiteral(State state, int pid, RealLiteralExpression expression) {
		return new Evaluation(state,
				universe.number(universe.numberObject(numberFactory.rational(expression.value().toPlainString()))));
	}

	protected Evaluation evaluateScopeofExpression(State state, int pid, String process, ScopeofExpression expression)
			throws UnsatisfiablePathConditionException {
		LHSExpression argument = expression.argument();

		return evaluateScopeofExpressionWorker(state, pid, process, argument);
	}

	private Evaluation evaluateScopeofExpressionWorker(State state, int pid, String process, LHSExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval;

		switch (expression.lhsExpressionKind()) {
		case DEREFERENCE:
			Expression pointer = ((DereferenceExpression) expression).pointer();

			eval = evaluate(state, pid, pointer);
			int sid = stateFactory.getDyscopeId(symbolicUtil.getScopeValue(eval.value));
			state = eval.state;
			if (sid < 0) {
				errorLogger.logSimpleError(pointer.getSource(), state, pid, process,
						symbolicAnalyzer.stateInformation(state), CIVLProperty.DEREFERENCE,
						"Attempt to dereference pointer into scope which has been removed from state");
				throw new UnsatisfiablePathConditionException();
			}
			return new Evaluation(state, stateFactory.scopeValue(sid));
		case DOT:
			return evaluateScopeofExpressionWorker(state, pid, process,
					(LHSExpression) (((DotExpression) expression).structOrUnion()));
		case SUBSCRIPT:
			return evaluateScopeofExpressionWorker(state, pid, process,
					(LHSExpression) (((SubscriptExpression) expression).array()));

		case VARIABLE:// VARIABLE
			int scopeId = state.getDyscopeID(pid, ((VariableExpression) expression).variable());

			return new Evaluation(state, stateFactory.scopeValue(scopeId));
		default:
			throw new CIVLUnimplementedFeatureException(
					"scope of expression with operand of " + expression.lhsExpressionKind() + " kind");
		}
	}

	protected Evaluation evaluateShiftleft(State state, int pid, BinaryExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.left());
		SymbolicExpression left = eval.value, right, result;

		eval = evaluate(eval.state, pid, expression.right());
		right = eval.value;
		state = eval.state;

		// TODO: this shouldn't happen. The model should enforce
		// that bit-shift operands have type int, int.

		if (left instanceof BooleanPrimitive)
			left = left.isTrue() ? universe.integer(1) : universe.integer(0);
		result = universe.bitshiftLeft((NumericExpression) left, (NumericExpression) right);
		return new Evaluation(state, result);
	}

	protected Evaluation evaluateShiftright(State state, int pid, BinaryExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.left());
		SymbolicExpression left = eval.value, right, result;

		eval = evaluate(eval.state, pid, expression.right());
		right = eval.value;
		state = eval.state;
		if (left instanceof BooleanPrimitive)
			left = left.isTrue() ? universe.integer(1) : universe.integer(0);
		result = universe.bitshiftRight((NumericExpression) left, (NumericExpression) right);
		return new Evaluation(state, result);
	}

	/**
	 * Evaluates a compound literal expression. The evaluation will go through the
	 * {@link CIVLCompoundLiteralObject} of the expression.
	 * 
	 * @param state      the current program state
	 * @param pid        the ID the running process
	 * @param expression the compound literal expression
	 * @return the {@link Evaluation} result
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateCompoundLiteral(State state, int pid, CompoundLiteralExpression expression)
			throws UnsatisfiablePathConditionException {
		// At this point, we know this expression has no constant value:
		return evaluateLiteralObject(state, pid, expression.getLiteralObject(), expression.getSource());
	}

	/**
	 * Evaluate a {@link CIVLLiteralObject} for a compound literal expression
	 * 
	 * @param state  the current state
	 * @param pid    the ID of the running process
	 * @param obj    the evaluating literal object
	 * @param source {@link CIVLSource} of the compound literal expression
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation evaluateLiteralObject(State state, int pid, CIVLLiteralObject obj, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		if (obj instanceof CIVLScalarLiteralObject) {
			CIVLScalarLiteralObject scalar = (CIVLScalarLiteralObject) obj;
			Expression scalarExpr = scalar.getExpression();

			if (scalarExpr != null)
				return evaluate(state, pid, scalarExpr);
			return initialValueOfType(state, pid, scalar.type());
		}

		CIVLCompoundLiteralObject compound = (CIVLCompoundLiteralObject) obj;
		List<SymbolicExpression> subObjVals = new ArrayList<>(compound.size());
		SymbolicType compoundDyTy;
		CIVLType compoundTy = compound.type();

		assert compoundTy.isArrayType() || compoundTy.isStructType() || compoundTy.isUnionType();

		TypeEvaluation teval = getDynamicType(state, pid, compoundTy, source, false);

		state = teval.state;
		compoundDyTy = teval.type;
		for (CIVLLiteralObject subObj : compound) {
			Evaluation eval = evaluateLiteralObject(state, pid, subObj, source);

			state = eval.state;
			subObjVals.add(eval.value);
		}

		SymbolicExpression resultVal;

		switch (compoundDyTy.typeKind()) {
		case ARRAY: {
			Evaluation eval = initialValueOfType(state, pid, compoundTy);

			state = eval.state;
			resultVal = universe.denseArrayWrite(eval.value, subObjVals);
			break;
		}
		case TUPLE:
			resultVal = universe.tuple((SymbolicTupleType) compoundDyTy, subObjVals);
			break;
		case UNION: {
			int memberIdx = subObjVals.size() - 1;

			assert (memberIdx >= 0);
			resultVal = universe.unionInject((SymbolicUnionType) compoundDyTy, universe.intObject(memberIdx),
					subObjVals.get(subObjVals.size() - 1));
			break;
		}
		default:
			throw new CIVLInternalException("Unexpected compound literal object type kind " + compoundDyTy.typeKind(),
					source);
		}
		return new Evaluation(state, resultVal);
	}

	/**
	 * Evaluate a unary expression.
	 * 
	 * @param state      The state of the program.
	 * @param pid        The pid of the currently executing process.
	 * @param expression The unary expression.
	 * @return The symbolic representation of the unary expression.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateUnary(State state, int pid, UnaryExpression expression)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, expression.operand());

		switch (expression.operator()) {
		case NEGATIVE:
			eval.value = universe.minus((NumericExpression) eval.value);
			break;
		case NOT:
			eval.value = universe.not((BooleanExpression) eval.value);
			break;
		case BIG_O:
			eval.value = universe.apply(bigOFunction, new Singleton<SymbolicExpression>(eval.value));
			break;
		case BIT_NOT:
			return evaluateBitcomplement(state, pid, expression);
		default:
			throw new CIVLUnimplementedFeatureException("evaluating unary operator " + expression.operator(),
					expression);
		}
		return eval;
	}

	/**
	 * Evaluate a variable expression.
	 * 
	 * @param state      The state of the program.
	 * @param pid        The pid of the currently executing process.
	 * @param expression The variable expression.
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Evaluation evaluateVariable(State state, int pid, String process, VariableExpression expression,
			boolean checkUndefinedValue) throws UnsatisfiablePathConditionException {
		if (expression.variable().isOutput() && civlConfig.isPropertyToggled(CIVLProperty.OUTPUT_READ)) {
			errorLogger.logSimpleError(expression.getSource(), state, pid, process,
					this.symbolicAnalyzer.stateInformation(state), CIVLProperty.OUTPUT_READ,
					"attempt to read the output variable " + expression.variable().name());
			throw new UnsatisfiablePathConditionException();
		} else {
			SymbolicExpression value = state.valueOf(pid, expression.variable());

			if (checkUndefinedValue && civlConfig.isPropertyToggled(CIVLProperty.UNDEFINED_VALUE) && value.isNull()) {
				errorLogger.logSimpleError(expression.getSource(), state, pid, process,
						this.symbolicAnalyzer.stateInformation(state), CIVLProperty.UNDEFINED_VALUE,
						"attempt to read uninitialized variable " + expression);
				throw new UnsatisfiablePathConditionException();
			}
			return new Evaluation(state, value);
		}
	}

	/**
	 * evaluate a the guard of a system function at a certain state with a list of
	 * arguments
	 * 
	 * @param source    the source information for error report
	 * @param state     The state where the computation happens.
	 * @param pid       The ID of the process that wants to evaluate the guard.
	 * @param library   the library that the system function belongs to
	 * @param function  the system function
	 * @param arguments the list of arguments
	 * @return The result of the evaluation, including the state and the symbolic
	 *         expression of the value.
	 * @throws UnsatisfiablePathConditionException if there is no contract
	 *                                             specifying the guard and the
	 *                                             library evaluator is missing
	 */
	protected Evaluation evaluateGuardofSystemFunction(CIVLSource source, State state, int pid, String library,
			CIVLFunction function, List<Expression> arguments) throws UnsatisfiablePathConditionException {
		if (function.functionContract() != null) {
			Expression guard = function.functionContract().guard();

			if (guard != null) {
				if (guard.constantValue() != null)
					return new Evaluation(state, guard.constantValue());

				int numArgs = arguments.size();
				SymbolicExpression[] argumentValues = new SymbolicExpression[numArgs];
				Evaluation eval;

				for (int i = 0; i < numArgs; i++) {
					Expression arg = arguments.get(i);

					eval = this.evaluate(state, pid, arg);
					state = eval.state;
					argumentValues[i] = eval.value;
				}
				// state = stateFactory.pushCallStack(state, pid, function,
				// state.getProcessState(pid).getDyscopeId(),
				// argumentValues);
				state = stateFactory.pushContract(state, pid, function, argumentValues);
				return this.evaluate(state, pid, guard);
			}
		}
		return getSystemGuard(source, state, pid, library, function.name().name(), arguments);
	}

	@Override
	public TypeEvaluation getDynamicType(State state, int pid, CIVLType type, CIVLSource source, boolean isDefinition)
			throws UnsatisfiablePathConditionException {
		TypeEvaluation result;

		if (type.getStateVariable() != null && !isDefinition) {
			SymbolicExpression value = state.valueOf(pid, type.getStateVariable());

			result = new TypeEvaluation(state, typeFactory.getType(value));
		} else if (type instanceof CIVLArrayType) {
			CIVLArrayType arrayType = (CIVLArrayType) type;
			TypeEvaluation elementTypeEval = getDynamicType(state, pid, arrayType.elementType(), source, false);

			if (arrayType.isComplete()) {
				Evaluation lengthEval = evaluate(elementTypeEval.state, pid,
						((CIVLCompleteArrayType) arrayType).extent());
				NumericExpression length = (NumericExpression) lengthEval.value;

				result = new TypeEvaluation(lengthEval.state, universe.arrayType(elementTypeEval.type, length));
			} else {
				result = new TypeEvaluation(elementTypeEval.state, universe.arrayType(elementTypeEval.type));
			}
		} else if (type instanceof CIVLStructOrUnionType) {
			CIVLStructOrUnionType structType = (CIVLStructOrUnionType) type;
			int numFields = structType.numFields();
			LinkedList<SymbolicType> componentTypes = new LinkedList<SymbolicType>();
			SymbolicType symbolicType;

			for (int i = 0; i < numFields; i++) {
				StructOrUnionField field = structType.getField(i);
				TypeEvaluation componentEval = getDynamicType(state, pid, field.type(), source, false);
				state = componentEval.state;
				componentTypes.add(componentEval.type);
			}
			if (structType.isStructType())
				symbolicType = universe.tupleType(structType.name().stringObject(), componentTypes);
			else
				symbolicType = universe.unionType(structType.name().stringObject(), componentTypes);
			result = new TypeEvaluation(state, symbolicType);
		} else {
			result = new TypeEvaluation(state, type.getDynamicType(universe));
		}
		return result;
	}

	protected Evaluation getSystemGuard(CIVLSource source, State state, int pid, String library, String function,
			List<Expression> arguments) throws UnsatisfiablePathConditionException {
		try {
			LibraryEvaluator libEvaluator = this.libLoader.getLibraryEvaluator(library, this, this.modelFactory,
					symbolicUtil, symbolicAnalyzer);
			Expression[] args = new Expression[arguments.size()];

			arguments.toArray(args);
			return libEvaluator.evaluateGuard(source, state, pid, function, args);
		} catch (LibraryLoaderException exception) {
			String process = state.getProcessState(pid).name() + "(id=" + pid + ")";
			StringBuffer message = new StringBuffer();
			int numArgs = arguments.size();
			SymbolicExpression[] argumentValues = new SymbolicExpression[numArgs];

			for (int i = 0; i < numArgs; i++) {
				Evaluation eval = this.evaluate(state, pid, arguments.get(i));

				state = eval.state;
				argumentValues[i] = eval.value;
			}
			message.append("unable to load the library evaluator for the library ");
			message.append(library);
			message.append(" for the function ");
			message.append(function);
			this.errorLogger.logSimpleError(source, state, pid, process, this.symbolicAnalyzer.stateInformation(state),
					CIVLProperty.LIBRARY, message.toString());
			return new Evaluation(state,
					this.symbolicUtil.getAbstractGuardOfFunctionCall(library, function, argumentValues));
		}
	}

	// TODO:
	// right now can't have a struct (for example) with a field
	// which is a universe.nullExpression().
	// Instead: use a symbolic constant UNDEF of each type.
	// For a scalar: initialize to UNDEF of the scalar type.
	// For an incomplete array: length 0 concrete
	// For a complete array: length n all of the undefined value.
	// For a struct: concrete tuple, each initial value of the field type
	// For a union: choose type 0, undef
	@Override
	public Evaluation initialValueOfType(State state, int pid, CIVLType type)
			throws UnsatisfiablePathConditionException {
		TypeKind kind = type.typeKind();
		Evaluation eval = null;

		switch (kind) {
		case ARRAY: {
			CIVLArrayType arrayType = (CIVLArrayType) type;
			CIVLType elementType = arrayType.elementType();

			// TODO: I think this is wrong, how can a incomplete array
			// has a default value of an array of length 0 type ?!
			eval = new Evaluation(state, universe.emptyArray(elementType.getDynamicType(universe)));
			break;
		}
		case COMPLETE_ARRAY: {
			CIVLCompleteArrayType arrayType = (CIVLCompleteArrayType) type;
			CIVLType elementType = arrayType.elementType();
			SymbolicExpression elementValue;
			NumericExpression extent;
			TypeEvaluation teval;

			eval = initialValueOfType(state, pid, elementType);
			state = eval.state;
			elementValue = eval.value;
			eval = this.evaluate(state, pid, arrayType.extent());
			state = eval.state;
			extent = (NumericExpression) eval.value;
			// using "evaluator.getDynamicType" so that extent info won't be
			// lost:
			teval = getDynamicType(state, pid, elementType, null, false);
			state = teval.state;
			eval.value = symbolicUtil.newArray(state.getPathCondition(universe), teval.type, extent, elementValue);
			break;
		}
		case BUNDLE:
			eval = new Evaluation(state, universe.nullExpression());
			break;
		case DOMAIN: {
			CIVLDomainType domainType = (CIVLDomainType) type;
			SymbolicExpression initDomainValue;
			int dim;
			SymbolicType integerType = universe.integerType();
			TypeEvaluation teval = getDynamicType(state, pid, domainType, modelFactory.systemSource(), false);

			state = teval.state;

			SymbolicTupleType tupleType = (SymbolicTupleType) teval.type;
			SymbolicUnionType unionComponentType = (SymbolicUnionType) tupleType.sequence().getType(2);
			List<SymbolicExpression> tupleComponents = new LinkedList<>();
			SymbolicExpression unionComponentVal = universe.unionInject(unionComponentType, oneObj,
					universe.emptyArray(universe.arrayType(integerType)));

			tupleComponents.add(one);
			tupleComponents.add(one);
			tupleComponents.add(unionComponentVal);
			if (domainType.isComplete()) {
				CIVLCompleteDomainType compDomainType = (CIVLCompleteDomainType) domainType;

				dim = compDomainType.getDimension();
				tupleComponents.set(0, universe.integer(dim));

			}
			initDomainValue = universe.tuple(tupleType, tupleComponents);
			eval = new Evaluation(state, initDomainValue);
			break;
		}
		case ENUM: {
			CIVLEnumType enumType = (CIVLEnumType) type;

			eval = new Evaluation(state, universe.integer(enumType.firstValue()));
			break;
		}
		case POINTER:
			eval = new Evaluation(state, symbolicUtil.nullPointer());
			break;
		case PRIMITIVE: {
			CIVLPrimitiveType primitiveType = (CIVLPrimitiveType) type;

			eval = new Evaluation(state, primitiveTypeInitialValue(primitiveType));
			break;
		}
		case MEM: {
			SymbolicExpression empty = typeFactory.civlMemType().memValueCreator(universe).apply(new LinkedList<>());

			return new Evaluation(state, empty);
		}
		default:// STRUCT_OR_UNION{ // TODO: don't make this the default!
		{
			CIVLStructOrUnionType strOrUnion = (CIVLStructOrUnionType) type;

			if (strOrUnion.isUnionType()) {
				eval = this.initialValueOfType(state, pid, strOrUnion.getField(0).type());
				eval.value = universe.unionInject((SymbolicUnionType) strOrUnion.getDynamicType(universe), this.zeroObj,
						eval.value);
			} else {
				int size = strOrUnion.numFields();
				List<SymbolicExpression> components = new ArrayList<>(size);
				// TODO: how do I know at this pointer whether the last
				// argument of "getDynamicType" is true or false ? it makes
				// no sense.
				TypeEvaluation teval = getDynamicType(state, pid, strOrUnion, null, false);

				state = teval.state;
				for (int i = 0; i < size; i++) {
					eval = this.initialValueOfType(state, pid, strOrUnion.getField(i).type());
					state = eval.state;
					components.add(eval.value);
				}
				eval = new Evaluation(state, universe.tuple((SymbolicTupleType) teval.type, components));
			}
		}
		}
		return eval;
	}

	private SymbolicExpression primitiveTypeInitialValue(CIVLPrimitiveType type) {
		switch (type.primitiveTypeKind()) {
		case BOOL:
			return universe.bool(false);
		case DYNAMIC:
			return null;
		case INT:
			return universe.integer(0);
		case PROCESS:
			return universe.tuple((SymbolicTupleType) type.getDynamicType(universe),
					new Singleton<SymbolicExpression>(universe.integer(-2)));
		case SCOPE:
			return stateFactory.undefinedScopeValue();
		case REAL:
			return universe.rational(0);
		case CHAR:
			return universe.character('\0');
		default:
		}
		return null;
	}

	/**
	 * Checks if a value of type scope, process, or pointer type is defined. If the
	 * value of those types is undefined (e.g., process -1, scope -1, pointer<-1,
	 * ..., ...>), an error should be reported.
	 * 
	 * @param state           The state where the checking happens.
	 * @param expression      The static representation of the value.
	 * @param expressionValue The symbolic value to be checked if it is defined.
	 * @throws UnsatisfiablePathConditionException
	 */
	private void isValueDefined(State state, int pid, String process, Expression expression,
			SymbolicExpression expressionValue) throws UnsatisfiablePathConditionException {
		CIVLSource source = expression.getSource();
		CIVLType expressionType = expression.getExpressionType();

		if (expressionType.equals(typeFactory.scopeType())) {
			if (civlConfig.isPropertyToggled(CIVLProperty.UNDEFINED_VALUE)
					&& expressionValue.equals(modelFactory.undefinedValue(typeFactory.scopeSymbolicType()))) {
				errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
						CIVLProperty.UNDEFINED_VALUE, "Attempt to evaluate an invalid scope reference");
				throw new UnsatisfiablePathConditionException();
			}
		} else if (expressionType.equals(typeFactory.processType())) {
			if (civlConfig.isPropertyToggled(CIVLProperty.UNDEFINED_VALUE)
					&& expressionValue.equals(modelFactory.undefinedValue(typeFactory.processSymbolicType()))) {
				errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
						CIVLProperty.UNDEFINED_VALUE, "Attempt to evaluate an invalid process reference");
				throw new UnsatisfiablePathConditionException();
			}
		} else if (expressionValue.type().equals(this.pointerType)) {
			if (this.symbolicUtil.isNullPointer(expressionValue))
				return;
			if (this.symbolicUtil.applyReverseFunction(INT_TO_POINTER_FUNCTION, expressionValue) != null)
				return;
			if (expressionValue.operator() != SymbolicOperator.TUPLE)
				return;
			// try {
			int scopeID = stateFactory.getDyscopeId(symbolicUtil.getScopeValue(expressionValue));

			if (scopeID < 0) {
				StringBuffer message = new StringBuffer();

				message.append("Attempt to evaluate a pointer refererring to memory of an invalid scope:\n");
				message.append("pointer expression: " + expression.toString() + "\n");
				message.append("value: " + expressionValue);
				errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
						CIVLProperty.MEMORY_LEAK, message.toString());
				throw new UnsatisfiablePathConditionException();
			}
			// } catch (Exception e) {
			// errorLogger.logSimpleError(source, state, process,
			// symbolicAnalyzer.stateInformation(state),
			// CIVLProperty.UNDEFINED_VALUE,
			// "Attempt to use undefined pointer");
			// throw new UnsatisfiablePathConditionException();
			// }
		}
	}

	/**
	 * zero
	 * 
	 * @param source
	 * @param type
	 * @return
	 */
	protected NumericExpression zeroOf(CIVLSource source, CIVLType type) {
		if (type instanceof CIVLPrimitiveType) {
			if (((CIVLPrimitiveType) type).primitiveTypeKind() == PrimitiveTypeKind.INT)
				return zero;
			if (((CIVLPrimitiveType) type).primitiveTypeKind() == PrimitiveTypeKind.REAL)
				return zeroR;
		}
		throw new CIVLInternalException("Expected integer or real type, not " + type, source);
	}

	/*
	 * ********************* Pointer addition helpers ************************
	 */
	/**
	 * <p>
	 * This is a helper function for
	 * {@link Evaluator#evaluatePointerAdd(State, int, String, BinaryExpression, SymbolicExpression, NumericExpression)}
	 * . <br>
	 * 
	 * For the given pointer who has an {@link ArrayElementReference}, this method
	 * performs the pointer addition operation by adding the given offset to the
	 * ArrayElementReference and possibly other ancestor-ArrayElementReferences.
	 * </p>
	 * 
	 * @param state                The current state
	 * @param process              The String identifier of the process
	 * @param pointer              The {@link SymbolicExpression} of the original
	 *                             pointer before addition
	 * @param offset               The {@link NumericExpression} of the offset will
	 *                             be added on the pointer
	 * @param checkOutput          Dereferencing operation has to check if the
	 *                             object pointed by input pointer is an output
	 *                             variable if this flag is set TRUE
	 * @param muteErrorSideEffects This method will NOT report error side-effects
	 *                             iff this parameter set to true.
	 * @param source               {@link CIVLSource} of the pointer addition
	 *                             statement
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 * 
	 * @author ziqing
	 */
	protected Pair<Evaluation, NumericExpression[]> arrayElementReferenceAddWorker(State state, int pid,
			SymbolicExpression pointer, NumericExpression offset, boolean muteErrorSideEffects, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression arrayPtr;
		NumericExpression extent, index;
		BooleanExpression zeroOffset, inBound;
		Evaluation eval;
		int scopeId = stateFactory.getDyscopeId(symbolicUtil.getScopeValue(pointer));
		int vid = symbolicUtil.getVariableId(source, pointer);
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		ReferenceExpression ref = symbolicUtil.getSymRef(pointer);
		String process = state.getProcessState(pid).name();

		zeroOffset = universe.equals(offset, zero);
		if (offset.isZero() || reasoner.isValid(zeroOffset))
			return new Pair<>(new Evaluation(state, pointer), null);
		assert ref.isArrayElementReference();
		arrayPtr = symbolicUtil.parentPointer(pointer);
		index = ((ArrayElementReference) ref).getIndex();

		SymbolicType dyType = symbolicAnalyzer.dynamicTypeOfObjByPointer(source, state, arrayPtr);
		SymbolicArrayType dyArrayType;
		ReferenceExpression newRef;

		assert dyType != null && dyType.typeKind() == SymbolicTypeKind.ARRAY;
		dyArrayType = (SymbolicArrayType) dyType;
		if (dyArrayType.isComplete()) {
			NumericExpression totalOffset = universe.add(index, offset);
			ResultType resultType = null;

			extent = ((SymbolicCompleteArrayType) dyArrayType).extent();
			// In bound condition: greater than or equal to the lower bound &&
			// lower than the upper bound:
			inBound = universe.and(universe.lessThanEquals(zero, totalOffset), universe.lessThan(totalOffset, extent));
			// Conditions of recomputing array indices:
			// If the parent pointer points to an element of an array object as
			// well and it can be proved that the result of the pointer addition
			// will point to other sub-arrays.
			if (!symbolicUtil.isPointer2MemoryBlock(arrayPtr)
					&& symbolicUtil.getSymRef(arrayPtr).isArrayElementReference()) {
				resultType = reasoner.valid(inBound).getResultType();
				if (resultType != ResultType.YES)
					return recomputeArrayIndices(state, pid, vid, scopeId, pointer, offset, reasoner,
							muteErrorSideEffects, source);
			} else if (!muteErrorSideEffects && civlConfig.isPropertyToggled(CIVLProperty.OUT_OF_BOUNDS)) {
				// Valid pointer addition condition: inBound || point-to the end
				// of the array:
				resultType = reasoner.valid(universe.or(inBound, universe.equals(totalOffset, extent))).getResultType();
				if (resultType != ResultType.YES) {
					StringBuffer message = printedPointerAdditionErrorMessage(state, pid, process, pointer, extent,
							index, offset, source);

					state = errorLogger.logError(source, state, pid, symbolicAnalyzer.stateInformation(state),
							zeroOffset, resultType, CIVLProperty.OUT_OF_BOUNDS, message.toString());
				}
			}
		} else if (civlConfig.isPropertyToggled(CIVLProperty.POINTER) && !muteErrorSideEffects)
			errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
					CIVLProperty.POINTER, "Pointer addition on incomplete array");
		// For the cases that either the pointer addition results within the
		// same sub-array or error-side effects are muted, directly making a new
		// pointer by adding the given offset to the reference expression of the
		// given pointer:
		newRef = universe.arrayElementReference(symbolicUtil.getSymRef(arrayPtr), universe.add(index, offset));
		eval = new Evaluation(state, symbolicUtil.makePointer(scopeId, vid, newRef));
		return new Pair<>(eval, null);
	}

	/**
	 * Print a "pointer-addition error message" : <code>
	 *  Pointer addition [pretty printing of pointer] + [offset] results in an index out of bound error. 
	 *  Object: Variable [a] (or An allocated memory region)
	 *  Object type :[type-of the Object]
	 *  Pointer value: [pretty printing of pointer]
	 *  Offset value: [offset]
	 *  Violated constraint: 0 &lt [index] + [offset] &lt= [extent]
	 * </code>
	 */
	private StringBuffer printedPointerAdditionErrorMessage(State state, int pid, String process,
			SymbolicExpression pointer, NumericExpression extent, NumericExpression index, NumericExpression offset,
			CIVLSource source) {
		String objStr;
		String prettyPointer = symbolicAnalyzer.symbolicExpressionToString(source, state,
				typeFactory.pointerType(symbolicAnalyzer.civlTypeOfObjByPointer(source, state, pointer)), pointer);
		int sid = stateFactory.getDyscopeId(symbolicUtil.getScopeValue(pointer));
		SymbolicType objType;

		// different pretty form for heap object and variable :
		if (symbolicUtil.isPointerToHeap(pointer)) {
			objStr = "An allocated memory region";
			objType = symbolicAnalyzer.dynamicTypeOfObjByPointer(source, state,
					symbolicUtil.getPointer2MemoryBlock(pointer));
		} else {
			int vid = symbolicUtil.getVariableId(source, pointer);
			Variable variable = state.getDyscope(sid).lexicalScope().getVariable(vid);

			objStr = "Variable " + variable.name();
			objType = state.getVariableValue(sid, vid).type();
		}

		StringBuffer message = new StringBuffer();

		message.append(
				"Pointer addition " + prettyPointer + " + " + offset + " results in an index out of bound error. \n");
		message.append("Object: " + objStr + "\n");
		message.append("Object type :" + objType + "\n");
		message.append("Pointer value: " + prettyPointer + "\n");
		message.append("Offset value: " + offset + "\n");
		message.append("Violated constraint: 0 < " + index + " + " + offset + " <= " + extent);
		return message;
	}

	/**
	 * <p>
	 * This is a Helper function for
	 * {@link Evaluator#evaluatePointerAdd(State, int, String, BinaryExpression, SymbolicExpression, NumericExpression)}
	 * . <br>
	 * 
	 * For the given pointer who has an {@link OffsetReference}, this method
	 * performs the pointer addition operation by adding the given offset to the
	 * OffsetReference.
	 * </p>
	 * 
	 * @param state                The current state
	 * @param pid                  The PID of the calling process
	 * @param pointer              The pointer operand in pointer-addition
	 * @param offset               The numeric operand in pointer-addition
	 * @param muteErrorSideEffects This method will NOT report error side-effects
	 *                             iff this parameter set to true.
	 * @param source               The {@link CIVLSource} associates to the pointer
	 *                             addition.
	 * @return The evaluation of the pointer addition operation
	 * @throws UnsatisfiablePathConditionException Iff error side-effects unmutes
	 *                                             and offset is neither one or
	 *                                             zero.
	 */
	protected Evaluation offsetReferenceAddition(State state, int pid, SymbolicExpression pointer,
			NumericExpression offset, boolean muteErrorSideEffects, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		OffsetReference offsetRef = (OffsetReference) symbolicUtil.getSymRef(pointer);
		NumericExpression oldOffset = offsetRef.getOffset();
		NumericExpression newOffset = universe.add(oldOffset, offset);
		Evaluation eval;

		if (!muteErrorSideEffects && civlConfig.isPropertyToggled(CIVLProperty.OUT_OF_BOUNDS)) {
			BooleanExpression claim = universe.and(universe.lessThanEquals(zero, newOffset),
					universe.lessThanEquals(newOffset, one));
			BooleanExpression assumption = state.getPathCondition(universe);
			ResultType resultType = universe.reasoner(assumption).valid(claim).getResultType();

			if (resultType != ResultType.YES) {
				state = errorLogger.logError(source, state, pid, symbolicAnalyzer.stateInformation(state), claim,
						resultType, CIVLProperty.OUT_OF_BOUNDS,
						"Pointer addition results in out of bounds.\nobject pointer:" + pointer + "\n" + "offset = "
								+ offset);
			}
		}
		eval = new Evaluation(state,
				symbolicUtil.setSymRef(pointer, universe.offsetReference(offsetRef.getParent(), newOffset)));
		return eval;
	}

	/**
	 * *
	 * <p>
	 * This is a Helper function for
	 * {@link Evaluator#evaluatePointerAdd(State, int, String, BinaryExpression, SymbolicExpression, NumericExpression)}
	 * . <br>
	 * 
	 * For the given pointer who has an {@link IdentifyReference}, this method
	 * performs the pointer addition operation by adding the given offset to the
	 * IdentifyReference.
	 * </p>
	 * 
	 * @param state                The current state
	 * @param pid                  The PID of the calling process
	 * @param pointer              The pointer operand in pointer-addition
	 * @param offset               The numeric operand in pointer-addition
	 * @param muteErrorSideEffects This method will NOT report error side-effects
	 *                             iff this parameter set to true.
	 * @param source               The {@link CIVLSource} associates to the pointer
	 *                             addition.
	 * @return The evaluation of the pointer addition operation
	 * @throws UnsatisfiablePathConditionException Iff error side-effects unmutes
	 *                                             and offset is neither one or
	 *                                             zero.
	 */
	protected Evaluation identityReferenceAddition(State state, int pid, SymbolicExpression pointer,
			NumericExpression offset, boolean muteErrorSideEffects, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		BooleanExpression claim;
		BooleanExpression assumption;
		ResultType resultType;
		ReferenceExpression symRef = symbolicUtil.getSymRef(pointer);

		claim = universe.equals(zero, offset);
		assumption = state.getPathCondition(universe);
		resultType = universe.reasoner(assumption).valid(claim).getResultType();
		if (resultType.equals(ResultType.YES))
			return new Evaluation(state, pointer);
		claim = universe.equals(one, offset);
		assumption = state.getPathCondition(universe);
		resultType = universe.reasoner(assumption).valid(claim).getResultType();
		if (resultType.equals(ResultType.YES))
			return new Evaluation(state, symbolicUtil.makePointer(pointer, universe.offsetReference(symRef, one)));
		else {
			if (!muteErrorSideEffects && civlConfig.isPropertyToggled(CIVLProperty.OUT_OF_BOUNDS))
				state = errorLogger.logError(source, state, pid, symbolicAnalyzer.stateInformation(state), claim,
						resultType, CIVLProperty.OUT_OF_BOUNDS,
						"Pointer addition results in out of bounds.\nobject pointer:" + pointer + "\noffset = "
								+ offset);
			// recovered, invalid pointer cannot be dereferenced, but
			// execution is not suppose to stop here:
			return new Evaluation(state, symbolicUtil.makePointer(pointer, universe.offsetReference(symRef, offset)));
		}
	}

	/* ********************** Methods from Evaluator *********************** */

	@Override
	public Evaluation evaluate(State state, int pid, Expression expression, boolean checkUndefinedValue)
			throws UnsatisfiablePathConditionException {
		ExpressionKind kind = expression.expressionKind();
		Evaluation result;
		String process = state.getProcessState(pid).name();

		if (expression.hasConstantValue())
			return new Evaluation(state, expression.constantValue());
		switch (kind) {
		case ABSTRACT_FUNCTION_CALL:
			result = evaluateAbstractFunctionCall(state, pid, (AbstractFunctionCallExpression) expression);
			break;
		case ADDRESS_OF:
			result = evaluateAddressOf(state, pid, (AddressOfExpression) expression);
			break;
		case ARRAY_LAMBDA: {
			result = evaluateArrayLambda(state, pid, (ArrayLambdaExpression) expression);
			break;
		}
		case BINARY:
			result = evaluateBinary(state, pid, process, (BinaryExpression) expression);
			break;
		case BOOLEAN_LITERAL:
			result = evaluateBooleanLiteral(state, pid, (BooleanLiteralExpression) expression);
			break;
		case CAST:
			result = evaluateCast(state, pid, process, (CastExpression) expression);
			break;
		case CHAR_LITERAL:
			result = evaluateCharLiteral(state, pid, (CharLiteralExpression) expression);
			break;
		case COND:
			result = evaluateConditionalExpression(state, pid, (ConditionalExpression) expression);
			break;
		case DEREFERENCE:
			result = evaluateDereference(state, pid, process, (DereferenceExpression) expression);
			break;
		case DOMAIN_GUARD:
			result = evaluateDomainGuard(state, pid, (DomainGuardExpression) expression);
			break;
		case REC_DOMAIN_LITERAL:
			result = evaluateRecDomainLiteral(state, pid, (RecDomainLiteralExpression) expression);
			break;
		case DOT:
			result = evaluateDot(state, pid, process, (DotExpression) expression);
			break;
		case DYNAMIC_TYPE_OF:
			result = evaluateDynamicTypeOf(state, pid, (DynamicTypeOfExpression) expression);
			break;
		case FUNCTION_IDENTIFIER:
			result = evaluateFunctionIdentifierExpression(state, pid, (FunctionIdentifierExpression) expression);
			break;
		case FUNCTION_GUARD:
			result = evaluateFunctionGuard(state, pid, process, (FunctionGuardExpression) expression);
			break;
		case HERE_OR_ROOT:
			result = evaluateHereOrRootScope(state, pid, (HereOrRootExpression) expression);
			break;
		case INITIAL_VALUE:
			result = evaluateInitialValue(state, pid, (InitialValueExpression) expression);
			break;
		case INTEGER_LITERAL:
			result = evaluateIntegerLiteral(state, pid, (IntegerLiteralExpression) expression);
			break;
		case LAMBDA:
			result = evaluateLambda(state, pid, (LambdaExpression) expression);
			break;
		case REAL_LITERAL:
			result = evaluateRealLiteral(state, pid, (RealLiteralExpression) expression);
			break;
		case REGULAR_RANGE:
			result = evaluateRegularRange(state, pid, (RegularRangeExpression) expression);
			break;
		case SCOPEOF:
			result = evaluateScopeofExpression(state, pid, process, (ScopeofExpression) expression);
			break;
		case SELF:
			result = evaluateSelf(state, pid, (SelfExpression) expression);
			break;
		case PROC_NULL:
			result = this.evaluateProcnull(state, pid, (ProcnullExpression) expression);
			break;
		case SIZEOF_TYPE:
			result = evaluateSizeofTypeExpression(state, pid, (SizeofTypeExpression) expression);
			break;
		case SIZEOF_EXPRESSION:
			result = evaluateSizeofExpressionExpression(state, pid, (SizeofExpression) expression);
			break;
		case COMPOUND_LITERAL:
			result = evaluateCompoundLiteral(state, pid, (CompoundLiteralExpression) expression);
			break;
		case SUBSCRIPT:
			result = evaluateSubscript(state, pid, process, (SubscriptExpression) expression);
			break;
		case SYSTEM_GUARD: {
			SystemGuardExpression systemGuard = (SystemGuardExpression) expression;
			CIVLFunction function = systemGuard.function();

			if (function.functionContract() != null) {
				Expression guard = function.functionContract().guard();

				if (guard != null)
					return evaluateGuardofSystemFunction(systemGuard.getSource(), state, pid, systemGuard.library(),
							function, systemGuard.arguments());
			}
			result = getSystemGuard(expression.getSource(), state, pid, systemGuard.library(),
					systemGuard.function().name().name(), systemGuard.arguments());
			break;
		}
		case UNARY:
			result = evaluateUnary(state, pid, (UnaryExpression) expression);
			break;
		case UNDEFINED_PROC:
			result = new Evaluation(state, modelFactory.undefinedValue(typeFactory.processSymbolicType()));
			break;
		case VARIABLE:
			result = evaluateVariable(state, pid, process, (VariableExpression) expression, checkUndefinedValue);
			break;
		case QUANTIFIER: {
			result = evaluateQuantifiedExpression(state, pid, (QuantifiedExpression) expression);
			break;
		}
		case FUNC_CALL:
			result = evaluateFunctionCallExpression(state, pid, (FunctionCallExpression) expression);
			break;
		case EXTENDED_QUANTIFIER:
			result = evaluateExtendedQuantifiedExpression(state, pid, (ExtendedQuantifiedExpression) expression);
			break;
		case MEMORY_UNIT:
		case NULL_LITERAL:
		case STRING_LITERAL:
			throw new CIVLSyntaxException("Illegal use of " + kind + " expression: ", expression.getSource());
		default:
			throw new CIVLInternalException("unreachable: " + kind, expression);
		}
		return result;
	}

	protected Evaluation evaluateQuantifiedExpression(State state, int pid, QuantifiedExpression expression)
			throws UnsatisfiablePathConditionException {
		return new QuantifiedExpressionEvaluator(modelFactory, stateFactory, libLoader, libExeLoader, symbolicUtil,
				symbolicAnalyzer, memUnitFactory, errorLogger, civlConfig)
				.evaluateQuantifiedExpression(state, pid, (QuantifiedExpression) expression);
	}

	protected Evaluation evaluateArrayLambda(State state, int pid, ArrayLambdaExpression arrayLambda)
			throws UnsatisfiablePathConditionException {
		return new QuantifiedExpressionEvaluator(modelFactory, stateFactory, libLoader, libExeLoader, symbolicUtil,
				symbolicAnalyzer, memUnitFactory, errorLogger, civlConfig).evaluateArrayLambda(state, pid, arrayLambda);
	}

	protected Evaluation evaluateLambda(State state, int pid, LambdaExpression arrayLambda)
			throws UnsatisfiablePathConditionException {
		return new QuantifiedExpressionEvaluator(modelFactory, stateFactory, libLoader, libExeLoader, symbolicUtil,
				symbolicAnalyzer, memUnitFactory, errorLogger, civlConfig).evaluateLambda(state, pid, arrayLambda);
	}

	@SuppressWarnings("unchecked")
	protected boolean containsSymbolicConstant(SymbolicExpression expr, SymbolicConstant symbol) {
		if (expr instanceof SymbolicConstant)
			return expr.equals(symbol);

		int numArgs = expr.numArguments();

		for (int i = 0; i < numArgs; i++) {
			SymbolicObject arg = expr.argument(i);

			if (arg instanceof SymbolicExpression) {
				if (containsSymbolicConstant((SymbolicExpression) arg, symbol))
					return true;
			} else if (arg instanceof SymbolicSequence) {
				SymbolicSequence<SymbolicExpression> sequence = (SymbolicSequence<SymbolicExpression>) arg;
				int numEles = sequence.size();

				for (int j = 0; j < numEles; j++) {
					SymbolicExpression ele = sequence.get(j);

					if (containsSymbolicConstant(ele, symbol))
						return true;
				}
			}
		}
		return false;
	}

	protected Evaluation evaluateExtendedQuantifiedExpression(State state, int pid,
			ExtendedQuantifiedExpression extQuant) throws UnsatisfiablePathConditionException {
		return new QuantifiedExpressionEvaluator(modelFactory, stateFactory, libLoader, libExeLoader, symbolicUtil,
				symbolicAnalyzer, memUnitFactory, errorLogger, civlConfig)
				.evaluateExtendedQuantifiedExpression(state, pid, extQuant);
	}

	protected Evaluation evaluateFunctionCallExpression(State state, int pid, FunctionCallExpression expression)
			throws UnsatisfiablePathConditionException {
		FunctionCallExpression funcCallExpr = (FunctionCallExpression) expression;
		Expression funcExpr = ((FunctionCallExpression) expression).callStatement().functionExpression();

		if (funcExpr.expressionKind() == ExpressionKind.FUNCTION_IDENTIFIER) {
			FunctionIdentifierExpression funcId = (FunctionIdentifierExpression) funcExpr;

			if (funcId.function().isLogic())
				return evaluateLogicFunctionCall(state, pid, funcCallExpr);
		}
		return this.functionCallExecutor.evaluateAtomicPureFunction(state, pid, expression.callStatement());
	}

	/**
	 * <p>
	 * Evaluate logic function calls to symbolic expressions with APPLY operators,
	 * which is applications of function-type symbolic constants with actual
	 * parameters.
	 * </p>
	 * 
	 * <p>
	 * Logic function definitions are state-independent, hence pointer-type actual
	 * parameters will be converted to arrays which containing the element pointed
	 * by those pointers.
	 * </p>
	 * 
	 * @throws UnsatisfiablePathConditionException if any error side-effect happens
	 *                                             during evaluation.
	 */
	private Evaluation evaluateLogicFunctionCall(State state, int pid, FunctionCallExpression logicCall)
			throws UnsatisfiablePathConditionException {
		List<SymbolicExpression> argumentValues = new LinkedList<>();
		LogicFunction logicFunction = (LogicFunction) logicCall.callStatement().function();
		SymbolicType symbolicPointerType = modelFactory.typeFactory().pointerSymbolicType();
		Evaluation eval;

		for (Expression actualArg : logicCall.callStatement().arguments()) {
			eval = evaluate(state, pid, actualArg);
			if (eval.value.type().equals(symbolicPointerType))
				eval = evaluatePointerTypeLogicFunctionArgument(state, pid, actualArg);
			assert state == eval.state : "Logic function call argument has side-effects.";
			argumentValues.add(eval.value);
		}
		// check if the predicate is a reserved predicate:
		if (logicFunction.isReservedFunction()) {
			SymbolicExpression[] argValArray = new SymbolicExpression[argumentValues.size()];

			argumentValues.toArray(argValArray);
			return new Evaluation(state, ReservedLogicFunctionCallEvaluator.applyReservedFunction(this, logicFunction,
					argValArray, logicCall.getSource()));
		}
		// else, it is a user-defined logic function:
		ProverFunctionInterpretation logicFuncInterpret = logicFunction.getConstantValue();
		SymbolicExpression predCallValue;
		SymbolicFunctionType type;

		if (logicFuncInterpret == null) {
			// logic function without definition must be the case that there is
			// something wrong during interpreting the logic function
			// definition:
			SymbolicType retType = logicFunction.returnType().getDynamicType(universe);
			List<SymbolicType> argType = new LinkedList<>();

			for (CIVLType formalType : logicFunction.functionType().parameterTypes())
				argType.add(formalType.getDynamicType(universe));
			type = universe.functionType(argType, retType);
		} else
			type = (SymbolicFunctionType) logicFuncInterpret.function.type();
		predCallValue = universe.symbolicConstant(universe.stringObject(logicFunction.name().name()), type);
		return new Evaluation(state, universe.apply(predCallValue, argumentValues));
	}

	/**
	 * This method evaluates pointer-type expressions with a special semantics:
	 * <p>
	 * <li>If the pointer points to an element in an array, the evaluation is the
	 * array. Note that here the "array" refers to a physical array instead of a
	 * logical array, which means the evaluation array is never an element of some
	 * other array.</li>
	 * <li>Otherwise, the evaluation is the dereference of the pointer.</li>
	 * </p>
	 */
	private Evaluation evaluatePointerTypeLogicFunctionArgument(State state, int pid, Expression pointer)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluate(state, pid, pointer);
		ReferenceExpression ref = symbolicUtil.getSymRef(eval.value);

		while (ref.isArrayElementReference())
			ref = ((NTReferenceExpression) ref).getParent();
		eval.value = symbolicUtil.makePointer(eval.value, ref);
		return dereference(pointer.getSource(), state, pid, state.getProcessState(pid).name(), eval.value, false, true);
	}

	@Override
	public Evaluation evaluateSizeofType(CIVLSource source, State state, int pid, CIVLType type)
			throws UnsatisfiablePathConditionException {
		Evaluation eval;

		if (type instanceof CIVLPrimitiveType) {
			NumericExpression value = ((CIVLPrimitiveType) type).getSizeof();
			BooleanExpression facts = ((CIVLPrimitiveType) type).getFacts();
			BooleanExpression pc = state.getPathCondition(universe);

			facts = universe.and(facts, pc);
			state = stateFactory.addToPathcondition(state, pid, facts);
			eval = new Evaluation(state, value);
		} else if (type instanceof CIVLCompleteArrayType) {
			NumericExpression extentValue;

			eval = evaluate(state, pid, ((CIVLCompleteArrayType) type).extent());
			extentValue = (NumericExpression) eval.value;
			eval = evaluateSizeofType(source, eval.state, pid, ((CIVLArrayType) type).elementType());
			eval.value = universe.multiply(extentValue, (NumericExpression) eval.value);
		} else if (type instanceof CIVLArrayType) {
			throw new CIVLInternalException("sizeof applied to incomplete array type", source);
		} else {
			NumericExpression sizeof;
			TypeEvaluation teval = getDynamicType(state, pid, type, source, false);

			state = teval.state;
			sizeof = typeFactory.sizeofDynamicType(teval.type);
			eval = new Evaluation(state, sizeof);
			eval.state = stateFactory.addToPathcondition(eval.state, pid, typeFactory.sizeofNonPrimitiveTypesFact());
		}
		return eval;
	}

	@Override
	public Triple<State, CIVLFunction, Integer> evaluateFunctionIdentifier(State state, int pid,
			Expression functionIdentifier, CIVLSource source) throws UnsatisfiablePathConditionException {
		CIVLFunction function;
		Evaluation eval = this.evaluate(state, pid, functionIdentifier);
		int scopeId = stateFactory.getDyscopeId(symbolicUtil.getScopeValue(eval.value));
		int fid = symbolicUtil.getVariableId(source, eval.value);
		// String funcName = "";
		Scope containingScope;

		if (scopeId < 0) {
			ProcessState procState = state.getProcessState(pid);

			errorLogger.logSimpleError(source, state, pid, procState.name() + "(id=" + pid + ")",
					symbolicAnalyzer.stateInformation(state), CIVLProperty.MEMORY_LEAK,
					"invalid function pointer: " + functionIdentifier);
			throw new UnsatisfiablePathConditionException();
		}
		state = eval.state;
		containingScope = state.getDyscope(scopeId).lexicalScope();
		function = containingScope.getFunction(fid);
		return new Triple<>(state, function, scopeId);
	}

	@Override
	public CIVLErrorLogger errorLogger() {
		return this.errorLogger;
	}

	@Override
	public Evaluation dereference(CIVLSource source, State state, int pid, String process, SymbolicExpression pointer,
			boolean checkOutput, boolean strict) throws UnsatisfiablePathConditionException {
		boolean muteErrorSideEffects = false; // report error side effects

		return dereferenceWorker(source, state, pid, process, pointer, checkOutput, false, strict,
				muteErrorSideEffects);
	}

	/**
	 * * Only three types (represented differently in CIVL) of the symbolic
	 * expression "charPointer" is acceptable:<br>
	 * A pointer to char <br>
	 * A pointer to an element of array of char. (e.g. char a[N][M]; a[x] or
	 * &a[x][0] are acceptable. Address of an element of array of char or pointer to
	 * an array of char are same as this situation.)<br>
	 * A complete char array
	 * 
	 * @param source      The CIVL source of the pointer to char expression
	 * @param state       The current state
	 * @param process     The identifier of the process
	 * @param charPointer The pointer to char
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	@Override
	public Triple<State, StringBuffer, Boolean> getString(CIVLSource source, State state, int pid, String process,
			Expression charPointerExpr, SymbolicExpression charPointer) throws UnsatisfiablePathConditionException {
		SymbolicExpression originalArray = null;
		SymbolicOperator operator = charPointer.operator();
		int int_arrayIndex = -1;
		StringBuffer result = new StringBuffer();

		if (operator == SymbolicOperator.ARRAY) {// string literal
			originalArray = charPointer;
			int_arrayIndex = 0;
		} else if (operator == SymbolicOperator.TUPLE) {
			ReferenceExpression ref = null;
			Evaluation eval;

			ref = symbolicUtil.getSymRef(charPointer);
			if (ref instanceof ArrayElementReference) {
				SymbolicExpression pointerCharArray = symbolicUtil.parentPointer(charPointer);
				SymbolicExpression charArray;

				eval = dereference(source, state, pid, process, pointerCharArray, false, true);
				state = eval.state;
				charArray = eval.value;
				originalArray = charArray;
				int_arrayIndex = symbolicUtil.extractInt(source, ((ArrayElementReference) ref).getIndex());

			} else {
				eval = dereference(source, state, pid, process, charPointer, false, true);
				state = eval.state;
				// A single character is not acceptable.
				if (eval.value.numArguments() <= 1) {
					this.errorLogger.logSimpleError(source, state, pid, process,
							this.symbolicAnalyzer.stateInformation(state), CIVLProperty.OTHER,
							"Try to obtain a string from a sequence of char has length" + " less than or equal to one");
					throw new UnsatisfiablePathConditionException();
				} else {
					originalArray = eval.value;
					int_arrayIndex = 0;
				}
			}
		} else
			throw new CIVLUnimplementedFeatureException("access on a non-concrete string", source);
		if (originalArray.operator() != SymbolicOperator.ARRAY)
			result.append(symbolicAnalyzer.symbolicExpressionToString(source, state, null, originalArray));
		else
			result = symbolicUtil.charArrayToString(source, originalArray, int_arrayIndex, false);
		return new Triple<>(state, result, true);
		// if (charPointer.operator() == SymbolicOperator.ARRAY) {
		// // SymbolicSequence<?> originalArray = null;
		// StringBuffer result = new StringBuffer();
		// ReferenceExpression ref = null;
		// Evaluation eval;
		//
		// if (charPointer.type() instanceof SymbolicCompleteArrayType) {
		// originalArray = charPointer;
		// int_arrayIndex = 0;
		// } else {
		// ref = symbolicUtil.getSymRef(charPointer);
		// if (ref instanceof ArrayElementReference) {
		// SymbolicExpression pointerCharArray = symbolicUtil
		// .parentPointer(source, charPointer);
		// SymbolicExpression charArray;
		//
		// eval = dereference(source, state, process, null,
		// pointerCharArray, false);
		// state = eval.state;
		// charArray = eval.value;
		// originalArray = charArray;
		// int_arrayIndex = symbolicUtil.extractInt(source,
		// ((ArrayElementReference) ref).getIndex());
		//
		// } else {
		// eval = dereference(source, state, process, charPointerExpr,
		// charPointer, false);
		// state = eval.state;
		// // A single character is not acceptable.
		// if (eval.value.numArguments() <= 1) {
		// this.errorLogger.logSimpleError(source, state, process,
		// this.symbolicAnalyzer.stateInformation(state),
		// CIVLProperty.OTHER,
		// "Try to obtain a string from a sequence of char has length"
		// + " less than or equal to one");
		// throw new UnsatisfiablePathConditionException();
		// } else {
		// originalArray = eval.value;
		// int_arrayIndex = 0;
		// }
		// }
		// }
		// result = symbolicUtil.charArrayToString(source, originalArray,
		// int_arrayIndex, false);
		// return new Triple<>(state, result, true);
		// } else
		// throw new CIVLUnimplementedFeatureException(
		// "access on a non-concrete string", source);
	}

	@Override
	public Evaluation getStringExpression(State state, int pid, String process, CIVLSource source,
			SymbolicExpression charPointer) throws UnsatisfiablePathConditionException {
		BooleanExpression pc = state.getPathCondition(universe);
		Reasoner reasoner = universe.reasoner(pc);
		ReferenceExpression symRef = symbolicUtil.getSymRef(charPointer);

		if (symRef.isArrayElementReference()) {
			ArrayElementReference arrayEltRef = (ArrayElementReference) symRef;
			SymbolicExpression arrayReference = symbolicUtil.parentPointer(charPointer);
			NumericExpression indexExpr = arrayEltRef.getIndex();
			Evaluation eval = dereference(source, state, pid, process, arrayReference, false, true);
			int index;

			if (indexExpr.isZero())
				index = 0;
			else {
				IntegerNumber indexNum = (IntegerNumber) reasoner.extractNumber(indexExpr);

				if (indexNum == null)
					throw new CIVLUnimplementedFeatureException(
							"access an element of an array of char with a non-concrete index", source);
				index = indexNum.intValue();
			}
			if (index == 0)
				return eval;
			else if (index > 0) {
				SymbolicExpression arrayValue = eval.value;
				SymbolicArrayType arrayType = (SymbolicArrayType) arrayValue.type();
				LinkedList<SymbolicExpression> charExprList = new LinkedList<>();
				int length;

				if (arrayType.isComplete()) {
					NumericExpression extent = ((SymbolicCompleteArrayType) arrayType).extent();
					IntegerNumber extentNum = (IntegerNumber) reasoner.extractNumber(extent);

					if (extentNum == null)
						throw new CIVLUnimplementedFeatureException("pointer into string of non-concrete length",
								source);
					length = extentNum.intValue();
				} else
					throw new CIVLUnimplementedFeatureException("pointer into string of unknown length", source);
				for (int i = index; i < length; i++) {
					SymbolicExpression charExpr = universe.arrayRead(arrayValue, universe.integer(i));

					charExprList.add(charExpr);
					// if you wanted to get heavy-weight, call the prover to see
					// if charExpr equals the null character instead of this:
					if (nullCharExpr.equals(charExpr))
						break;
				}
				eval.value = universe.array(charType, charExprList);
				return eval;
			} else
				throw new CIVLInternalException("negative pointer index: " + index, source);
		}
		throw new CIVLUnimplementedFeatureException("pointer to char is not into an array of char", source);
	}

	@Override
	public ModelFactory modelFactory() {
		return modelFactory;
	}

	@Override
	public Evaluation evaluatePointerAdd(State state, int pid, BinaryExpression expression, SymbolicExpression pointer,
			SymbolicExpression offset) throws UnsatisfiablePathConditionException {
		Pair<BooleanExpression, ResultType> checkPointer = symbolicAnalyzer.isDefinedPointer(state, pointer,
				expression.getSource());

		if (checkPointer.right != ResultType.YES) {
			errorLogger.logError(expression.getSource(), state, pid, symbolicAnalyzer.stateInformation(state),
					checkPointer.left, checkPointer.right, CIVLProperty.DEREFERENCE,
					"Attempt to perform pointer addition upon an undefined pointer");
			throw new UnsatisfiablePathConditionException();
		} else {
			ReferenceExpression symRef = symbolicUtil.getSymRef(pointer);

			if (symRef.isArrayElementReference()) {
				return arrayElementReferenceAddWorker(state, pid, pointer, (NumericExpression) offset, false,
						expression.left().getSource()).left;
			} else if (symRef.isOffsetReference()) {
				return offsetReferenceAddition(state, pid, pointer, (NumericExpression) offset, false,
						expression.getSource());
			} else if (symRef.isIdentityReference()) {
				return identityReferenceAddition(state, pid, pointer, (NumericExpression) offset, false,
						expression.getSource());
			} else
				throw new CIVLUnimplementedFeatureException(
						"Pointer addition for anything other than array elements or variables", expression);
		}
	}

	@Override
	public Evaluation pointerSubtraction(State state, int pid, String process, BinaryExpression expression,
			SymbolicExpression leftPtr, SymbolicExpression rightPtr) throws UnsatisfiablePathConditionException {
		int leftVid, leftSid, rightVid, rightSid;
		SymbolicExpression arrayPtr;
		NumericExpression leftPos = zero, rightPos = zero;
		NumericExpression[] leftPtrIndices, rightPtrIndices;
		NumericExpression[] arraySliceSizes;

		// ModelFactory already checked type compatibility, so here we just
		// check if these two pointers are pointing to same object and array
		// element reference pointers.
		leftVid = symbolicUtil.getVariableId(expression.left().getSource(), leftPtr);
		leftSid = stateFactory.getDyscopeId(symbolicUtil.getScopeValue(leftPtr));
		rightVid = symbolicUtil.getVariableId(expression.right().getSource(), rightPtr);
		rightSid = stateFactory.getDyscopeId(symbolicUtil.getScopeValue(rightPtr));

		if (rightSid == -1 && rightVid == -1) {
			// offset subtraction
			return new Evaluation(state,
					this.symbolicAnalyzer.pointerArithmetics(expression.getSource(), state, true, leftPtr, rightPtr));
		} else {
			// Check if the two point to the same object
			if (civlConfig.isPropertyToggled(CIVLProperty.POINTER) && (rightVid != leftVid) || (rightSid != leftSid)) {
				state = errorLogger.logError(expression.getSource(), state, pid,
						symbolicAnalyzer.stateInformation(state), null, ResultType.NO, CIVLProperty.POINTER,
						"Operands of pointer subtraction don't point to the " + "same obejct");
				throw new UnsatisfiablePathConditionException();
			}
			// Check if two pointers are array element reference pointers. Based
			// on
			// C11 Standard 6.5.6, entry 9: When two pointers are subtracted,
			// both
			// shall point to elements of the same array object, or one past the
			// last element of the array object; the result is the difference of
			// the
			// subscripts of the two array elements.
			// Thus, any pointer which is not an array element reference is
			// invalid
			// for pointer subtraction.
			if (civlConfig.isPropertyToggled(CIVLProperty.POINTER)
					&& !(symbolicUtil.getSymRef(leftPtr).isArrayElementReference()
							&& symbolicUtil.getSymRef(rightPtr).isArrayElementReference()))
				state = errorLogger.logError(expression.getSource(), state, pid,
						symbolicAnalyzer.stateInformation(state), null, ResultType.NO, CIVLProperty.POINTER,
						"Not both of the operands of pointer subtraction points to an array element");
			// Get the pointer to the whole array
			arrayPtr = symbolicUtil.arrayRootPtr(leftPtr);
			leftPtrIndices = symbolicUtil.extractArrayIndicesFrom(leftPtr);
			rightPtrIndices = symbolicUtil.extractArrayIndicesFrom(rightPtr);
			// If the two pointers are pointing to heap, check if they are
			// pointing to the same memory block:
			if (civlConfig.isPropertyToggled(CIVLProperty.POINTER) && leftVid == 0
					&& !symbolicUtil.arePoint2SameMemoryBlock(leftPtr, rightPtr)) {
				state = errorLogger.logError(expression.getSource(), state, pid,
						symbolicAnalyzer.stateInformation(state), null, ResultType.NO, CIVLProperty.POINTER,
						"Operands of pointer subtraction point to different heap obejcts");
			}
			// Get array by dereferencing array pointer
			SymbolicType arrayType = symbolicAnalyzer.dynamicTypeOfObjByPointer(expression.getSource(), state,
					arrayPtr);

			assert arrayType != null && arrayType.typeKind() == SymbolicTypeKind.ARRAY
					&& arrayType instanceof SymbolicCompleteArrayType;
			arraySliceSizes = symbolicUtil
					.arraySlicesSizes(symbolicUtil.arrayDimensionExtents((SymbolicCompleteArrayType) arrayType));
			for (int i = leftPtrIndices.length, j = arraySliceSizes.length - 1; --i >= 0; j--) {
				NumericExpression leftIdx, rightIdx;
				NumericExpression sliceSizes = arraySliceSizes[j];

				leftIdx = leftPtrIndices[i];
				rightIdx = rightPtrIndices[i];
				leftPos = universe.add(leftPos, universe.multiply(leftIdx, sliceSizes));
				rightPos = universe.add(rightPos, universe.multiply(rightIdx, sliceSizes));
			}
			return new Evaluation(state, universe.subtract(leftPos, rightPos));
		}
	}

	@Override
	public Evaluation reference(State state, int pid, LHSExpression operand)
			throws UnsatisfiablePathConditionException {
		Evaluation result;

		if (operand instanceof VariableExpression) {
			Variable variable = ((VariableExpression) operand).variable();
			int sid = state.getDyscopeID(pid, variable);
			int vid = variable.vid();

			result = new Evaluation(state, symbolicUtil.makePointer(sid, vid, identityReference));
		} else if (operand instanceof SubscriptExpression) {
			LHSExpression arrayExpr = ((SubscriptExpression) operand).array();
			Evaluation refEval = reference(state, pid, arrayExpr);
			SymbolicExpression arrayPointer = refEval.value;
			ReferenceExpression oldSymRef = symbolicUtil.getSymRef(arrayPointer);
			NumericExpression index;
			ReferenceExpression newSymRef;
			SymbolicExpression array;
			SymbolicArrayType arrayType;

			result = evaluate(refEval.state, pid, ((SubscriptExpression) operand).index());
			index = (NumericExpression) result.value;
			result = dereference(operand.getSource(), state, pid, state.getProcessState(pid).name(), arrayPointer,
					false, true);
			array = result.value;
			arrayType = (SymbolicArrayType) array.type();
			if (array.type() == null)
				arrayType = (SymbolicArrayType) arrayExpr.getExpressionType().getDynamicType(universe);
			if (!operand.isErrorFree() && civlConfig.isPropertyToggled(CIVLProperty.OUT_OF_BOUNDS))
				result.state = this.checkArrayIndexInBound(state, pid, (SubscriptExpression) operand, arrayType, array,
						index, true);
			newSymRef = universe.arrayElementReference(oldSymRef, index);
			result.value = symbolicUtil.setSymRef(arrayPointer, newSymRef);
		} else if (operand instanceof DereferenceExpression) {
			// &(*p) = p, so just evaluate the pointer and return.
			result = evaluate(state, pid, ((DereferenceExpression) operand).pointer());
		} else if (operand instanceof DotExpression) {
			DotExpression dot = (DotExpression) operand;
			int index = dot.fieldIndex();

			Evaluation eval = reference(state, pid, (LHSExpression) dot.structOrUnion());
			SymbolicExpression structPointer = eval.value;
			ReferenceExpression oldSymRef = symbolicUtil.getSymRef(structPointer);
			ReferenceExpression newSymRef;

			if (dot.isStruct()) {
				newSymRef = universe.tupleComponentReference(oldSymRef, universe.intObject(index));
			} else {
				newSymRef = universe.unionMemberReference(oldSymRef, universe.intObject(index));
			}
			eval.value = symbolicUtil.setSymRef(structPointer, newSymRef);
			result = eval;
		} else
			throw new CIVLInternalException("Unknown kind of LHSExpression", operand);
		return result;
	}

	public StateFactory stateFactory() {
		return stateFactory;
	}

	@Override
	public SymbolicUtility symbolicUtility() {
		return symbolicUtil;
	}

	public SymbolicUniverse universe() {
		return universe;
	}

	@Override
	public Pair<Evaluation, NumericExpression[]> arrayElementReferenceAdd(State state, int pid, SymbolicExpression ptr,
			NumericExpression offset, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression newPtr = symbolicUtil.makePointer(ptr,
				symbolicAnalyzer.getLeafNodeReference(state, ptr, source));

		return arrayElementReferenceAddWorker(state, pid, newPtr, offset, false, source);
	}

	@Override
	public List<ReferenceExpression> leafNodeReferencesOfType(CIVLSource source, State state, int pid, CIVLType type)
			throws UnsatisfiablePathConditionException {
		return this.leafNodeReferencesOfType(source, state, pid, type, universe.identityReference());
	}

	private List<ReferenceExpression> leafNodeReferencesOfType(CIVLSource source, State state, int pid, CIVLType type,
			ReferenceExpression parent) throws UnsatisfiablePathConditionException {
		List<ReferenceExpression> result = new ArrayList<>();
		TypeKind typeKind = type.typeKind();

		switch (typeKind) {
		case ARRAY:
			throw new CIVLUnimplementedFeatureException("sub-references of incomplete arrays", source);

		case COMPLETE_ARRAY: {
			CIVLCompleteArrayType arrayType = (CIVLCompleteArrayType) type;
			Expression extent = arrayType.extent();
			Evaluation eval = this.evaluate(state, pid, extent);
			NumericExpression extentValue = (NumericExpression) eval.value;
			CIVLType eleType = arrayType.elementType();

			state = eval.state;

			Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
			IntegerNumber length_number = (IntegerNumber) reasoner.extractNumber(extentValue);

			if (length_number != null) {
				int length_int = length_number.intValue();

				for (int i = 0; i < length_int; i++) {
					ArrayElementReference arrayEle = universe.arrayElementReference(parent, universe.integer(i));

					result.addAll(this.leafNodeReferencesOfType(source, state, pid, eleType, arrayEle));
				}
			} else
				throw new CIVLUnimplementedFeatureException("sub-references of arrays with non-concrete extent",
						source);
			break;
		}
		case DOMAIN:
		case ENUM:
		case POINTER:
		case BUNDLE:
		case PRIMITIVE:
			result.add(parent);
			break;
		case STRUCT_OR_UNION: {
			CIVLStructOrUnionType structOrUnion = (CIVLStructOrUnionType) type;
			int numFields = structOrUnion.numFields();

			if (structOrUnion.isUnionType())
				throw new CIVLUnimplementedFeatureException("sub-references of union type", source);
			for (int i = 0; i < numFields; i++) {
				CIVLType filedType = structOrUnion.getField(i).type();
				TupleComponentReference tupleComp = universe.tupleComponentReference(parent, universe.intObject(i));

				result.addAll(this.leafNodeReferencesOfType(source, state, pid, filedType, tupleComp));
			}
			break;
		}
		default:
			throw new CIVLUnimplementedFeatureException("sub-references of " + typeKind, source);
		}
		return result;
	}

	@Override
	public Pair<State, SymbolicArrayType> evaluateCIVLArrayType(State state, int pid, CIVLArrayType type)
			throws UnsatisfiablePathConditionException {
		Pair<State, SymbolicArrayType> ret_pair;
		Evaluation eval;
		NumericExpression extent;

		if (!type.isComplete()) {
			// since type is CIVLArrayType, following cast should be safe.
			ret_pair = new Pair<>(state, (SymbolicArrayType) type.getDynamicType(universe));
			return ret_pair;
		}
		// if type is complete array type, get extent.
		eval = this.evaluate(state, pid, ((CIVLCompleteArrayType) type).extent());
		extent = (NumericExpression) eval.value;
		if (!type.elementType().isArrayType()) {
			SymbolicArrayType ret_type = universe.arrayType(type.elementType().getDynamicType(universe), extent);

			state = eval.state;
			ret_pair = new Pair<>(state, ret_type);
			return ret_pair;
		} else {
			SymbolicArrayType ret_type;

			// This branch comes from
			// "if element type of 'type' has an array type", so following cast
			// is safe.
			ret_pair = this.evaluateCIVLArrayType(state, pid, (CIVLArrayType) type.elementType());
			ret_type = universe.arrayType(ret_pair.right, extent);
			ret_pair.right = ret_type;
			return ret_pair;
		}
	}

	@Override
	public MemoryUnitExpressionEvaluator memoryUnitEvaluator() {
		return memUnitEvaluator;
	}

	/**
	 * <p>
	 * <strong>pre-condition:</strong>
	 * <ol>
	 * <li>The given pointer must point to an array element of a complete array
	 * object.</li>
	 * </ol>
	 * </p>
	 * 
	 * <p>
	 * Updates index informations of {@link ArrayElementReference}s in the given
	 * pointer, results in a new pointer which is the result of the pointer
	 * arithmetic operation. The given pointer can be represented as the address of
	 * the first element in the array plus an offset which depends on the position
	 * of the element: <code>
	 * Array being pointed: a[e<sub>0</sub>][e<sub>..</sub>][e<sub>n-1</sub>], 
	 * where e<sub>i</sub> represents the extent of the i-th dimension.
	 * 
	 * Pointer p = &a[0][..][0] + 
	 *             \sum(0, n-2, \lambda int j;
	 *                                 idx<sub>j</sub> * \product(j+1, n-1, \lambda int i; e<sub>i</sub>)
	 *                 ) + idx<sub>n-1</sub>;
	 * where we borrows the fold notation of ACSL and idx<sub>i</sub> is the index of element in i-th dimension.
	 * 
	 * Lets use a symbol X = \sum(0, n-2, \lambda int j;
	 *                                 idx<sub>j</sub> * \product(j+1, n-1, \lambda int i; e<sub>i</sub>)
	 *                           ) + idx<sub>n-1</sub>;
	 * 
	 * For the pointer arithmetic: p + offset = &a[0][..][0] + (X + offset);, it eventually points to 
	 * an element (<strong>the element will not necessarily exist</strong>) with indices:
	 * 
	 * idx<sub>i</sub> = (  (X + offset) - \sum(0, i-1, \lambda int j; 
	 *                                            idx<sub>j</sub> * \product(j+1, n-1, \lambda int k; e<sub>k</sub>)
	 *                                         )
	 *                   ) / \product(i+1, n-1, \lambda int k; e<sub>k</sub>)
	 * 
	 *  </code>
	 * </p>
	 * 
	 * @param state      The current state
	 * @param process    The String identifier of the process
	 * @param pointedVid The variable id of the pointed array object
	 * @param pointedSid The variable id of the pointed array object
	 * @param pointer    The pointer points to the array object
	 * @param offset     The offset added to the pointer
	 * @param reasoner   An instance reference of a {@link Reasoner} object
	 * @param source     The CIVLSource of the statement
	 * @return A pair of an evaluation, which is the evaluation of the array
	 *         element-reference addition, and an array of sizes of each dimensional
	 *         slice of the referred array object, the order of sizes of the
	 *         dimensional slices is from larger ones to smaller ones.
	 * @throws UnsatisfiablePathConditionException
	 */
	protected Pair<Evaluation, NumericExpression[]> recomputeArrayIndices(State state, int pid, int pointedVid,
			int pointedSid, SymbolicExpression pointer, NumericExpression offset, Reasoner reasoner,
			boolean muteErrorSideEffects, CIVLSource source) throws UnsatisfiablePathConditionException {
		NumericExpression totalOffset = zero;
		NumericExpression sliceSize = one;
		SymbolicExpression arrayRootPtr;
		NumericExpression[] indices, arrayExtents, sliceSizes, oldIndices;
		ReferenceExpression newRef;
		SymbolicType dyType;
		SymbolicCompleteArrayType dyarrayType;
		Evaluation eval;
		int dim;

		arrayRootPtr = symbolicUtil.arrayRootPtr(pointer);
		dyType = symbolicAnalyzer.dynamicTypeOfObjByPointer(source, state, arrayRootPtr);
		assert dyType != null && dyType instanceof SymbolicArrayType && ((SymbolicArrayType) dyType).isComplete();
		dyarrayType = (SymbolicCompleteArrayType) dyType;
		arrayExtents = symbolicUtil.arrayDimensionExtents(dyarrayType);
		sliceSizes = symbolicUtil.arraySlicesSizes(arrayExtents);
		dim = dyarrayType.dimensions();
		oldIndices = symbolicUtil.extractArrayIndicesFrom(pointer);
		assert oldIndices.length <= dim && sliceSizes.length == dim;
		// computes total offset from the first element
		for (int i = 0; i < oldIndices.length; i++) {
			NumericExpression oldIndex;

			oldIndex = oldIndices[i];
			sliceSize = sliceSizes[i];
			totalOffset = universe.add(totalOffset, universe.multiply(oldIndex, sliceSize));
		}
		totalOffset = universe.add(totalOffset, offset);

		// Computes new indexes
		Pair<State, NumericExpression[]> state_newIndices = recomputeArrayIndicesWorker(state, pid, dim, totalOffset,
				sliceSizes, arrayExtents, muteErrorSideEffects, source);

		indices = state_newIndices.right;
		newRef = symbolicUtil.makeArrayElementReference(symbolicUtil.getSymRef(arrayRootPtr), indices);
		eval = new Evaluation(state_newIndices.left, symbolicUtil.makePointer(pointedSid, pointedVid, newRef));
		return new Pair<>(eval, sliceSizes);
	}

	/**
	 * <p>
	 * The worker method for
	 * {@link #recomputeArrayIndices(State, int, int, int, SymbolicExpression, NumericExpression, Reasoner, boolean, CIVLSource)}
	 * . This method returns an updated state and an array of new indices.
	 * 
	 * Note that the compute of new indices is driven by pointer addition, so that
	 * they may point to the end of an array (different from array subscript which
	 * will not allow one to do that).
	 * </p>
	 * 
	 * @param state                The current state
	 * @param pid                  The PID of the calling process
	 * @param dimensions           The number of dimensions of the array object
	 *                             whose element was referred by the old indices.
	 * @param totalOffset          The total offset from the base address of the
	 *                             referred array object to the element, which will
	 *                             be referred by the new indices.
	 * @param arraySliceSizes      The size of each dimensional slice of the
	 *                             referred array object.
	 * @param arrayExtents         The extent of each dimension of the referred
	 *                             array object.
	 * @param muteErrorSideEffects This method will not report out-of-bound error
	 *                             iff this parameter set to true.
	 * @param source               The {@link CIVLSource} associates to the pointer
	 *                             addition.
	 * @return A pair of an updated state and an array of new indices, the order of
	 *         the indices is same as the order of subscript.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Pair<State, NumericExpression[]> recomputeArrayIndicesWorker(State state, int pid, int dimensions,
			NumericExpression totalOffset, NumericExpression[] arraySliceSizes, NumericExpression[] arrayExtents,
			boolean muteErrorSideEffects, CIVLSource source) throws UnsatisfiablePathConditionException {
		NumericExpression[] results = new NumericExpression[dimensions];
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		ResultType resultType;
		BooleanExpression checkClaim;

		if (!muteErrorSideEffects) {
			NumericExpression arraySize = universe.multiply(arraySliceSizes[0], arrayExtents[0]);

			// totalOffset <= arraySize, e.g. T a[100], a + 100 is valid:
			checkClaim = universe.lessThanEquals(totalOffset, arraySize);
			checkClaim = universe.and(universe.lessThanEquals(zero, totalOffset), checkClaim);
			resultType = reasoner.valid(checkClaim).getResultType();
			if (resultType != ResultType.YES && civlConfig.isPropertyToggled(CIVLProperty.OUT_OF_BOUNDS))
				state = errorLogger.logError(source, state, pid, symbolicAnalyzer.stateInformation(state), checkClaim,
						resultType, CIVLProperty.OUT_OF_BOUNDS,
						"Pointer addition out-of-bound.\n" + "Base address + " + totalOffset);
		}
		for (int i = 0; i < dimensions; i++) {
			Pair<NumericExpression, NumericExpression> newIndex_remainder;
			NumericExpression sliceSize = arraySliceSizes[i];
			NumericExpression newIndex;

			newIndex_remainder = symbolicUtil.arithmeticIntDivide(totalOffset, sliceSize);
			newIndex = newIndex_remainder.left;
			totalOffset = newIndex_remainder.right;
			if (i == dimensions - 1) { // no need check for the last index
				results[i] = newIndex;
				break;
			}
			// if new_index == extent, new_index--, offset += sliceSize:
			checkClaim = universe.equals(newIndex, arrayExtents[i]);
			resultType = reasoner.valid(checkClaim).getResultType();
			if (resultType == ResultType.YES) {
				newIndex = universe.subtract(newIndex, one);
				totalOffset = universe.add(totalOffset, arraySliceSizes[i]);
			} else if (resultType != ResultType.NO) {
				// Encoding conditional expressions for the unknown condition:
				// "remainder == 0 ?"
				newIndex = (NumericExpression) universe.cond(checkClaim, universe.subtract(newIndex, one), newIndex);
				totalOffset = (NumericExpression) universe.cond(checkClaim,
						universe.add(totalOffset, arraySliceSizes[i]), totalOffset);
			}
			results[i] = newIndex;
		}
		return new Pair<>(state, results);
	}

	/**
	 * Evaluates the result of an PLUS operation with two numeric operands
	 */
	protected SymbolicExpression evaluatePlus(SymbolicExpression left, SymbolicExpression right) {
		return universe.add((NumericExpression) left, (NumericExpression) right);
	}

	@Override
	public SymbolicAnalyzer symbolicAnalyzer() {
		return this.symbolicAnalyzer;
	}

	@Override
	public Evaluation evaluate(State state, int pid, Expression expression) throws UnsatisfiablePathConditionException {
		return this.evaluate(state, pid, expression, true);
	}

	@Override
	public Evaluation havoc(State state, SymbolicType type) {
		Pair<State, SymbolicConstant> freshSymbol = this.stateFactory.getFreshSymbol(state,
				ModelConfiguration.HAVOC_PREFIX_INDEX, type);

		return new Evaluation(freshSymbol.left, freshSymbol.right);
	}

	@Override
	public void setConfiguration(CIVLConfiguration config) {
		this.civlConfig = config;
	}

	@Override
	public ArrayToolBox newArrayToolBox(SymbolicUniverse universe) {
		return new SimpleArrayToolBox(universe);
	}

	@Override
	public MemEvaluator memEvaluator() {
		if (memExpressionEvaluator == null)
			memExpressionEvaluator = new MemEvaluator(modelFactory, stateFactory, libLoader, libExeLoader, symbolicUtil,
					symbolicAnalyzer, memUnitFactory, errorLogger, civlConfig);
		return memExpressionEvaluator;
	}
}
