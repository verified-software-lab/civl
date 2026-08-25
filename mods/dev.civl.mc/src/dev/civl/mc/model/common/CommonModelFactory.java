package dev.civl.mc.model.common;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.acsl.ExtendedQuantifiedExpressionNode.ExtendedQuantifier;
import dev.civl.abc.program.IF.Program;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.TokenFactory;
import dev.civl.mc.analysis.IF.CodeAnalyzer;
import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.model.IF.AbstractFunction;
import dev.civl.mc.model.IF.CIVLException;
import dev.civl.mc.model.IF.CIVLFunction;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLTypeFactory;
import dev.civl.mc.model.IF.Fragment;
import dev.civl.mc.model.IF.Identifier;
import dev.civl.mc.model.IF.LogicFunction;
import dev.civl.mc.model.IF.Model;
import dev.civl.mc.model.IF.ModelConfiguration;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.SystemFunction;
import dev.civl.mc.model.IF.contract.LoopContract;
import dev.civl.mc.model.IF.expression.AbstractFunctionCallExpression;
import dev.civl.mc.model.IF.expression.AddressOfExpression;
import dev.civl.mc.model.IF.expression.ArrayLambdaExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression.BINARY_OPERATOR;
import dev.civl.mc.model.IF.expression.BooleanLiteralExpression;
import dev.civl.mc.model.IF.expression.BoundVariableExpression;
import dev.civl.mc.model.IF.expression.CastExpression;
import dev.civl.mc.model.IF.expression.CharLiteralExpression;
import dev.civl.mc.model.IF.expression.CompoundLiteralExpression;
import dev.civl.mc.model.IF.expression.ConditionalExpression;
import dev.civl.mc.model.IF.expression.DereferenceExpression;
import dev.civl.mc.model.IF.expression.DomainGuardExpression;
import dev.civl.mc.model.IF.expression.DotExpression;
import dev.civl.mc.model.IF.expression.DynamicTypeOfExpression;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.ExtendedQuantifiedExpression;
import dev.civl.mc.model.IF.expression.FunctionCallExpression;
import dev.civl.mc.model.IF.expression.FunctionGuardExpression;
import dev.civl.mc.model.IF.expression.FunctionIdentifierExpression;
import dev.civl.mc.model.IF.expression.HereOrRootExpression;
import dev.civl.mc.model.IF.expression.InitialValueExpression;
import dev.civl.mc.model.IF.expression.IntegerLiteralExpression;
import dev.civl.mc.model.IF.expression.LHSExpression;
import dev.civl.mc.model.IF.expression.LHSExpression.LHSExpressionKind;
import dev.civl.mc.model.IF.expression.LambdaExpression;
import dev.civl.mc.model.IF.expression.MemoryUnitExpression;
import dev.civl.mc.model.IF.expression.Nothing;
import dev.civl.mc.model.IF.expression.ProcnullExpression;
import dev.civl.mc.model.IF.expression.QuantifiedExpression;
import dev.civl.mc.model.IF.expression.QuantifiedExpression.Quantifier;
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
import dev.civl.mc.model.IF.expression.UnaryExpression.UNARY_OPERATOR;
import dev.civl.mc.model.IF.expression.VariableExpression;
import dev.civl.mc.model.IF.expression.WildcardExpression;
import dev.civl.mc.model.IF.expression.reference.ArraySliceReference;
import dev.civl.mc.model.IF.expression.reference.ArraySliceReference.ArraySliceKind;
import dev.civl.mc.model.IF.expression.reference.MemoryUnitReference;
import dev.civl.mc.model.IF.expression.reference.SelfReference;
import dev.civl.mc.model.IF.expression.reference.StructOrUnionFieldReference;
import dev.civl.mc.model.IF.location.Location;
import dev.civl.mc.model.IF.statement.AssignStatement;
import dev.civl.mc.model.IF.statement.CallOrSpawnStatement;
import dev.civl.mc.model.IF.statement.CivlParForSpawnStatement;
import dev.civl.mc.model.IF.statement.DomainIteratorStatement;
import dev.civl.mc.model.IF.statement.MallocStatement;
import dev.civl.mc.model.IF.statement.NoopStatement;
import dev.civl.mc.model.IF.statement.ParallelAssignStatement;
import dev.civl.mc.model.IF.statement.Statement;
import dev.civl.mc.model.IF.type.CIVLArrayType;
import dev.civl.mc.model.IF.type.CIVLFunctionType;
import dev.civl.mc.model.IF.type.CIVLPointerType;
import dev.civl.mc.model.IF.type.CIVLPrimitiveType;
import dev.civl.mc.model.IF.type.CIVLPrimitiveType.PrimitiveTypeKind;
import dev.civl.mc.model.IF.type.CIVLSetType;
import dev.civl.mc.model.IF.type.CIVLStructOrUnionType;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.model.common.contract.CommonLoopContract;
import dev.civl.mc.model.common.expression.CommonAbstractFunctionCallExpression;
import dev.civl.mc.model.common.expression.CommonAddressOfExpression;
import dev.civl.mc.model.common.expression.CommonArrrayLambdaExpression;
import dev.civl.mc.model.common.expression.CommonBinaryExpression;
import dev.civl.mc.model.common.expression.CommonBooleanLiteralExpression;
import dev.civl.mc.model.common.expression.CommonBoundVariableExpression;
import dev.civl.mc.model.common.expression.CommonCastExpression;
import dev.civl.mc.model.common.expression.CommonCharLiteralExpression;
import dev.civl.mc.model.common.expression.CommonCompoundLiteralExpression;
import dev.civl.mc.model.common.expression.CommonConditionalExpression;
import dev.civl.mc.model.common.expression.CommonDereferenceExpression;
import dev.civl.mc.model.common.expression.CommonDomainGuardExpression;
import dev.civl.mc.model.common.expression.CommonDotExpression;
import dev.civl.mc.model.common.expression.CommonDynamicTypeOfExpression;
import dev.civl.mc.model.common.expression.CommonExtendedQuantifiedExpression;
import dev.civl.mc.model.common.expression.CommonFunctionCallExpression;
import dev.civl.mc.model.common.expression.CommonFunctionGuardExpression;
import dev.civl.mc.model.common.expression.CommonFunctionIdentifierExpression;
import dev.civl.mc.model.common.expression.CommonHereOrRootExpression;
import dev.civl.mc.model.common.expression.CommonInitialValueExpression;
import dev.civl.mc.model.common.expression.CommonIntegerLiteralExpression;
import dev.civl.mc.model.common.expression.CommonLambdaExpression;
import dev.civl.mc.model.common.expression.CommonMemoryUnitExpression;
import dev.civl.mc.model.common.expression.CommonNothing;
import dev.civl.mc.model.common.expression.CommonProcnullExpression;
import dev.civl.mc.model.common.expression.CommonQuantifiedExpression;
import dev.civl.mc.model.common.expression.CommonRealLiteralExpression;
import dev.civl.mc.model.common.expression.CommonRecDomainLiteralExpression;
import dev.civl.mc.model.common.expression.CommonRegularRangeExpression;
import dev.civl.mc.model.common.expression.CommonScopeofExpression;
import dev.civl.mc.model.common.expression.CommonSelfExpression;
import dev.civl.mc.model.common.expression.CommonSizeofExpression;
import dev.civl.mc.model.common.expression.CommonSizeofTypeExpression;
import dev.civl.mc.model.common.expression.CommonSubscriptExpression;
import dev.civl.mc.model.common.expression.CommonSystemGuardExpression;
import dev.civl.mc.model.common.expression.CommonUnaryExpression;
import dev.civl.mc.model.common.expression.CommonUndefinedProcessExpression;
import dev.civl.mc.model.common.expression.CommonVariableExpression;
import dev.civl.mc.model.common.expression.CommonWildcardExpression;
import dev.civl.mc.model.common.expression.reference.CommonArraySliceReference;
import dev.civl.mc.model.common.expression.reference.CommonSelfReference;
import dev.civl.mc.model.common.expression.reference.CommonStructOrUnionFieldReference;
import dev.civl.mc.model.common.location.CommonLocation;
import dev.civl.mc.model.common.statement.CommonAssignStatement;
import dev.civl.mc.model.common.statement.CommonAtomicLockAssignStatement;
import dev.civl.mc.model.common.statement.CommonCallStatement;
import dev.civl.mc.model.common.statement.CommonCivlForEnterStatement;
import dev.civl.mc.model.common.statement.CommonCivlParForSpawnStatement;
import dev.civl.mc.model.common.statement.CommonGotoBranchStatement;
import dev.civl.mc.model.common.statement.CommonIfElseBranchStatement;
import dev.civl.mc.model.common.statement.CommonLoopBranchStatement;
import dev.civl.mc.model.common.statement.CommonMallocStatement;
import dev.civl.mc.model.common.statement.CommonNoopStatement;
import dev.civl.mc.model.common.statement.CommonParallelAssignStatement;
import dev.civl.mc.model.common.statement.CommonReturnStatement;
import dev.civl.mc.model.common.statement.CommonSwitchBranchStatement;
import dev.civl.mc.model.common.variable.CommonVariable;
import dev.civl.mc.util.IF.Pair;
import dev.civl.mc.util.IF.Singleton;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.SymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;
import dev.civl.sarl.IF.number.IntegerNumber;
import dev.civl.sarl.IF.object.IntObject;
import dev.civl.sarl.IF.object.NumberObject;
import dev.civl.sarl.IF.object.StringObject;
import dev.civl.sarl.IF.object.SymbolicObject;
import dev.civl.sarl.IF.type.SymbolicType;

/**
 * The factory to create all model components. Usually this is the only way
 * model components will be created.
 * 
 * @author Timothy K. Zirkel (zirkel)
 * @author Manchun Zheng (zmanchun)
 * 
 */
public class CommonModelFactory implements ModelFactory {

	/* ******************************* Types ******************************* */

	/**
	 * Kinds for temporal variables introduced when translating conditional
	 * expressions that requires temporal variable to store some intermediate data
	 * 
	 */
	public enum TempVariableKind {
		CONDITIONAL
	}

	/* *************************** Static Fields *************************** */

	private static final String DOM_SIZE_PREFIX = "_dom_size";

	private static final String PAR_PROC_PREFIX = "_par_procs";

	/* ************************** Instance Fields ************************** */

	private List<Variable> inputVariables = new LinkedList<>();

	private int domSizeVariableId = 0;

	private int parProcsVariableId = 0;

	private CommonCIVLTypeFactory typeFactory;

	/**
	 * The list of variables created for array literals used to initialize a pointer
	 * type variable. E.g., int* p=(int[]){2,3} will introduce an anonymous variable
	 * int[] __anon0 = {2,3}; int* p = &__anon0[0];
	 */
	private int anonymousVariableId = 0;

	private Fragment anonFragment;

	/**
	 * The unique variable $ATOMIC_LOCK_VAR in the root scope for process that the
	 * executing of $atomic blocks to have the highest priority for execution.
	 */
	private VariableExpression atomicLockVariableExpression;

	private Variable timeCountVariable;

	private Variable brokenTimeVariable;

	// private Scope currentScope;

	/** Keep a set of used identifiers for fly-weighting purposes. */
	private Map<String, Identifier> identifiers;

	/** Keep a unique number to identify locations. */
	private int locationID = 0;

	/**
	 * When translating a CallOrSpawnStatement that has some conditional expressions
	 * as its arguments, we need to update the call statement stack maintained in
	 * the model builder worker, because the function field of each call statement
	 * is only updated after the whole AST tree is traversed.
	 */
	ModelBuilderWorker modelBuilder;

	private List<CodeAnalyzer> codeAnalyzers;

	/** Keep a unique number to identify scopes. */
	private int scopeID = 0;

	/**
	 * The system source, used to create the identifier of the system function
	 * (_CIVL_System), and for elements introduced during translation but doesn't
	 * have real source, e.g., the atomic lock variable, etc.
	 */
	private CIVLSource systemSource = new SystemCIVLSource();

	/**
	 * The unique ABC token factory, used for obtaining source in the translation.
	 */
	private TokenFactory tokenFactory;

	/**
	 * The unique symbolic expression for the undefined process value, which has the
	 * integer value -1.
	 */
	private SymbolicExpression undefinedProcessValue;

	/**
	 * The unique symbolic expression for the null process value, which has the
	 * integer value -2.
	 */
	private SymbolicExpression nullProcessValue;

	/**
	 * The unique SARL symbolic universe used in the system.
	 */
	private SymbolicUniverse universe;

	/**
	 * The unique integer object of zero.
	 */
	private IntObject zeroObj;

	/**
	 * The system scope of the model, i.e., the root scope.
	 */
	private Scope systemScope;

	/**
	 * The system scope id of the system scope.
	 */
	private SymbolicExpression systemScopeId;

	/**
	 * The static constant scope of the model, which is used for array literal
	 * constants
	 */
	private Scope staticScope;

	// /**
	// * An instance of a <code>$wait</code> system function identifier
	// expression
	// */
	// private FunctionIdentifierExpression waitFuncPointer = null;

	private FunctionIdentifierExpression elaborateDomainFuncPointer = null;

	private SymbolicConstant hideConstant = null;

	/* **************************** Constructors *************************** */

	/**
	 * The factory to create all model components. Usually this is the only way
	 * model components will be created.
	 * 
	 * @param universe The symbolic universe
	 */
	public CommonModelFactory(SymbolicUniverse universe, CIVLConfiguration config) {
		this.typeFactory = new CommonCIVLTypeFactory(universe, config);
		this.universe = universe;
		this.identifiers = new HashMap<String, Identifier>();
		zeroObj = universe.intObject(0);
		undefinedProcessValue = universe.tuple(typeFactory.processSymbolicType,
				new Singleton<SymbolicExpression>(universe.integer(-1)));
		this.nullProcessValue = universe.tuple(typeFactory.processSymbolicType,
				new Singleton<SymbolicExpression>(universe.integer(-2)));
		this.anonFragment = new CommonFragment();
	}

	/* ********************** Methods from ModelFactory ******************** */

	/*
	 * ********************************************************************* CIVL
	 * Expressions
	 * *********************************************************************
	 */

	@Override
	public AbstractFunctionCallExpression abstractFunctionCallExpression(CIVLSource source, AbstractFunction function,
			List<Expression> arguments) {
		// Note: While the abstract function may be declared in e.g. the
		// outermost scope, since it has no value or state, it doesn't
		// contribute anything non-local to the expression scope.
		Scope expressionScope = joinScope(arguments);
		Scope lowestScope = getLowerScope(arguments);

		return new CommonAbstractFunctionCallExpression(source, expressionScope, lowestScope, function, arguments);
	}

	@Override
	public AddressOfExpression addressOfExpression(CIVLSource source, LHSExpression operand) {
		CIVLType expressionType = operand.getExpressionType();

		if (expressionType.isSetType()) {
			expressionType = ((CIVLSetType) expressionType).elementType();
			expressionType = typeFactory.pointerType(expressionType);
			expressionType = typeFactory.civlSetType(expressionType);
		} else
			expressionType = typeFactory.pointerType(expressionType);

		AddressOfExpression result = new CommonAddressOfExpression(source, expressionType, operand);

		if (operand.lhsExpressionKind() == LHSExpressionKind.DOT) {
			DotExpression dotExpr = (DotExpression) operand;
			Expression structExpr = dotExpr.structOrUnion();

			if (structExpr instanceof DereferenceExpression) {
				DereferenceExpression derefExpr = (DereferenceExpression) structExpr;
				Expression pointerExpr = derefExpr.pointer();

				if (pointerExpr instanceof CastExpression) {
					CastExpression castExpr = (CastExpression) pointerExpr;
					Expression expr = castExpr.getExpression();
					CIVLType castType = castExpr.getCastType();

					if ((expr instanceof IntegerLiteralExpression)
							&& ((IntegerLiteralExpression) expr).value().intValue() == 0 && castType.isPointerType()) {
						result.setFieldOffset(true);
						result.setFieldIndex(dotExpr.fieldIndex());
						result.setTypeForOffset(((CIVLPointerType) castType).baseType());
					}
				}
			}
		}
		return result;
	}

	/**
	 * A binary expression. One of {+,-,*,\,<,<=,==,!=,&&,||,%}
	 * 
	 * @param operator The binary operator.
	 * @param left     The left operand.
	 * @param right    The right operand.
	 * @return The binary expression.
	 */
	@Override
	public BinaryExpression binaryExpression(CIVLSource source, BINARY_OPERATOR operator, Expression left,
			Expression right) {
		Scope expressionScope = join(left.expressionScope(), right.expressionScope());
		Scope lowestScope = getLower(left.lowestScope(), right.lowestScope());

		switch (operator) {
		case AND:
		case EQUAL:
		case LESS_THAN:
		case LESS_THAN_EQUAL:
		case NOT_EQUAL:
		case OR:
		case VALID:
			return new CommonBinaryExpression(source, expressionScope, lowestScope, typeFactory.booleanType, operator,
					left, right);
		case PLUS:
		case TIMES:
		case DIVIDE:
		case MINUS:
		case MODULO:
		case POINTER_ADD:
			// TODO: there misses many cases and put the common checking
			// code under default is unsafe
		default:
			CIVLType leftType = left.getExpressionType();
			CIVLType rightType = right.getExpressionType();
			CIVLType resultType = null;

			// If we are not doing pointer arithmetic or pointer
			// subtraction, types should be the same:
			if (leftType instanceof CIVLPointerType && rightType instanceof CIVLPrimitiveType) {
				assert ((CIVLPrimitiveType) rightType).primitiveTypeKind() == PrimitiveTypeKind.INT;
				// ((CommonBinaryExpression)
				// result).setExpressionType(leftType);
				resultType = leftType;
			} else if (leftType instanceof CIVLPointerType && rightType instanceof CIVLPrimitiveType) {
				assert ((CIVLPrimitiveType) rightType).primitiveTypeKind() == PrimitiveTypeKind.INT;
				// ((CommonBinaryExpression)
				// result).setExpressionType(leftType);
				resultType = leftType;
			} else if (leftType instanceof CIVLPointerType && rightType instanceof CIVLPointerType)
				// compatibility checking
				if (((CIVLPointerType) leftType).baseType().equals(((CIVLPointerType) rightType).baseType()))
					// ((CommonBinaryExpression) result)
					// .setExpressionType(integerType());
					resultType = typeFactory.integerType;
				else
					throw new CIVLException(leftType + " and " + rightType + " are not pointers to compatiable types",
							source);
			else if (leftType.isSetType() || rightType.isSetType()) {
				// the only BINARY_OPERATOR that is compatible with set
				// type operands is: '+', i.e. PLUS or POINTER_ADD:
				if (operator == BINARY_OPERATOR.PLUS || operator == BINARY_OPERATOR.POINTER_ADD)
					if (leftType.isSetType())
						resultType = deriveBinaryOperationSetType((CIVLSetType) leftType, rightType);
					else
						resultType = deriveBinaryOperationSetType((CIVLSetType) rightType, leftType);
			} else if (leftType.equals(rightType))
				resultType = leftType;
			if (resultType == null)
				throw new CIVLException("Incompatible types to +", source);
			return new CommonBinaryExpression(source, expressionScope, lowestScope, resultType, operator, left, right);
		}
	}

	/**
	 * Deriving types of binary operation expressions where at least one operands
	 * have {@link CIVLSetType}. Binary operations that involves set type are:
	 * <ul>
	 * <li>pointer(set) PLUS integer(set)</li>
	 * <li>numeric(set) PLUS numeric(set)</li>
	 * </ul>
	 * 
	 * @param argType0 the type of one operand of the binary operation which has
	 *                 been known having set type
	 * @param argType1 the type of the other operand of the binary operation
	 * @return the type of the binary operation
	 */
	private CIVLType deriveBinaryOperationSetType(CIVLSetType argType0, CIVLType argType1) {
		CIVLType type0 = argType0.elementType();
		CIVLType type1 = argType1;
		boolean type1IsSet = type1.isSetType();

		if (type1IsSet)
			type1 = ((CIVLSetType) type1).elementType();
		if (type0.isNumericType() && type1.isNumericType()) {
			assert type0.equals(type1);
			return typeFactory.civlSetType(type0);
		}
		if (type0.isNumericType()) {
			assert type1.isPointerType() && type0.isIntegerType();
			return typeFactory.civlSetType(type1);
		}
		if (type1.isNumericType()) {
			assert type0.isPointerType() && type1.isIntegerType();
			return typeFactory.civlSetType(type0);
		}
		return null;
	}

	@Override
	public Expression booleanExpression(Expression expression) throws ModelFactoryException {
		CIVLSource source = expression.getSource();

		if (!expression.getExpressionType().equals(typeFactory.booleanType)) {
			CIVLType exprType = expression.getExpressionType();

			if (exprType.equals(typeFactory.integerType)) {
				expression = binaryExpression(source, BINARY_OPERATOR.NOT_EQUAL, expression,
						integerLiteralExpression(source, BigInteger.ZERO));
			} else if (exprType.equals(typeFactory.realType)) {
				expression = binaryExpression(source, BINARY_OPERATOR.NOT_EQUAL, expression,
						realLiteralExpression(source, BigDecimal.ZERO));
			} else if (exprType.isPointerType()) {
				CIVLPointerType pointerType = (CIVLPointerType) expression.getExpressionType();

				expression = binaryExpression(source, BINARY_OPERATOR.NOT_EQUAL, expression,
						this.nullPointerExpression(pointerType, source));
			} else if (exprType.isCharType()) {
				expression = binaryExpression(source, BINARY_OPERATOR.NOT_EQUAL, expression,
						this.charLiteralExpression(source, (char) 0));
			} else {
				throw new ModelFactoryException("The expression " + expression + " isn't compatible with boolean type",
						expression.getSource());
			}
		}
		return expression;
	}

	@Override
	public Expression numericExpression(Expression expression) throws ModelFactoryException {
		CIVLType type = expression.getExpressionType();

		if (type.isNumericType())
			return expression;
		if (type.isBoolType())
			return this.castExpression(expression.getSource(), typeFactory.integerType(), expression);
		if (type.isCharType())
			return this.castExpression(expression.getSource(), typeFactory.integerType(), expression);
		throw new ModelFactoryException("The expression " + expression + " isn't compatible with numeric type",
				expression.getSource());
	}

	@Override
	public Expression arithmeticableExpression(Expression expression) throws ModelFactoryException {
		CIVLType type = expression.getExpressionType();

		if (type.isPointerType() || type.isArrayType())
			return expression;
		if (type.isSetType() && (((CIVLSetType) type).elementType().isNumericType()
				|| ((CIVLSetType) type).elementType().isPointerType()))
			return expression;
		try {
			return comparableExpression(expression);
		} catch (ModelFactoryException e) {
			throw new ModelFactoryException("The expression " + expression + " isn't compatible with arithmetic type",
					expression.getSource());
		}
	}

	@Override
	public Expression comparableExpression(Expression expression) throws ModelFactoryException {
		CIVLType type = expression.getExpressionType();

		if (type.isScopeType())
			return expression;
		try {
			return numericExpression(expression);
		} catch (ModelFactoryException e) {
			throw new ModelFactoryException("The expression " + expression + " is not comparable",
					expression.getSource());
		}
	}

	/**
	 * A boolean literal expression.
	 * 
	 * @param value True or false.
	 * @return The boolean literal expression.
	 */
	@Override
	public BooleanLiteralExpression booleanLiteralExpression(CIVLSource source, boolean value) {
		return new CommonBooleanLiteralExpression(source, typeFactory.booleanType, value);
	}

	@Override
	public BoundVariableExpression boundVariableExpression(CIVLSource source, Identifier name, CIVLType type) {
		return new CommonBoundVariableExpression(source, type, name);
	}

	/**
	 * The ternary conditional expression ("?" in C).
	 * 
	 * @param condition   The condition being evaluated in this conditional.
	 * @param trueBranch  The expression returned if the condition evaluates to
	 *                    true.
	 * @param falseBranch The expression returned if the condition evaluates to
	 *                    false.
	 * @return The conditional expression.
	 */
	@Override
	public ConditionalExpression conditionalExpression(CIVLSource source, Expression condition, Expression trueBranch,
			Expression falseBranch) {
		// Front-end ABC has already guaranteed that both branches have the
		// exact type in the perspective of pure C, where has no bool type.
		// CIVL has bool type. Given two expressions "1" and "!1", both have
		// integer type in C and ABC but the latter one has bool type in CIVL.
		// The following casts deal with such cases.
		if (trueBranch.getExpressionType().isBoolType() && !falseBranch.getExpressionType().isBoolType())
			falseBranch = castExpression(source, trueBranch.getExpressionType(), falseBranch);
		if (!trueBranch.getExpressionType().isBoolType() && falseBranch.getExpressionType().isBoolType())
			trueBranch = castExpression(source, falseBranch.getExpressionType(), trueBranch);
		return new CommonConditionalExpression(source, joinScope(Arrays.asList(condition, trueBranch, falseBranch)),
				getLowerScope(Arrays.asList(condition, trueBranch, falseBranch)), trueBranch.getExpressionType(),
				condition, trueBranch, falseBranch);
	}

	@Override
	public CastExpression castExpression(CIVLSource source, CIVLType type, Expression expression) {
		return new CommonCastExpression(source, expression.expressionScope(), type, expression);
	}

	@Override
	public DereferenceExpression dereferenceExpression(CIVLSource source, Expression pointer) {
		CIVLType pointerType = pointer.getExpressionType();
		CIVLType derefExprType;
		boolean isSetType = pointerType.isSetType();

		if (isSetType)
			pointerType = ((CIVLSetType) pointerType).elementType();
		assert pointerType.isPointerType();

		derefExprType = ((CIVLPointerType) pointerType).baseType();
		if (isSetType)
			derefExprType = typeFactory.civlSetType(derefExprType);
		// systemScope: indicates unknown scope
		return new CommonDereferenceExpression(source, this.systemScope, derefExprType, pointer);
	}

	@Override
	public DotExpression dotExpression(CIVLSource source, Expression struct, int fieldIndex) {
		CIVLType structType = struct.getExpressionType();
		CIVLType dotExprType;
		boolean isSetType = structType.isSetType();

		if (isSetType)
			structType = ((CIVLSetType) structType).elementType();
		assert structType.isStructType() || structType.isUnionType();

		dotExprType = ((CIVLStructOrUnionType) structType).getField(fieldIndex).type();
		if (isSetType)
			dotExprType = typeFactory.civlSetType(dotExprType);
		return new CommonDotExpression(source, struct, fieldIndex, dotExprType);
	}

	@Override
	public DynamicTypeOfExpression dynamicTypeOfExpression(CIVLSource source, CIVLType type) {
		return new CommonDynamicTypeOfExpression(source, typeFactory.dynamicType, type);
	}

	@Override
	public FunctionIdentifierExpression functionIdentifierExpression(CIVLSource source, CIVLFunction function) {
		FunctionIdentifierExpression expression = new CommonFunctionIdentifierExpression(source, function,
				typeFactory.pointerSymbolicType);

		return expression;
	}

	@Override
	public HereOrRootExpression hereOrRootExpression(CIVLSource source, boolean isRoot) {
		return new CommonHereOrRootExpression(source, typeFactory.scopeType, isRoot,
				isRoot ? this.systemScopeId : null);
	}

	@Override
	public InitialValueExpression initialValueExpression(CIVLSource source, Variable variable) {
		return new CommonInitialValueExpression(source, variable);
	}

	/**
	 * An integer literal expression.
	 * 
	 * @param value The (arbitrary precision) integer value.
	 * @return The integer literal expression.
	 */
	@Override
	public IntegerLiteralExpression integerLiteralExpression(CIVLSource source, BigInteger value) {
		return new CommonIntegerLiteralExpression(source, typeFactory.integerType, value);
	}

	@Override
	public Expression nullPointerExpression(CIVLPointerType pointerType, CIVLSource source) {
		Expression zero = integerLiteralExpression(source, BigInteger.ZERO);
		Expression result;

		result = castExpression(source, pointerType, zero);
		return result;
	}

	@Override
	public QuantifiedExpression quantifiedExpression(CIVLSource source, Quantifier quantifier,
			List<Pair<List<Variable>, Expression>> boundVariableList, Expression restriction, Expression expression) {
		return new CommonQuantifiedExpression(source, join(expression.expressionScope(), restriction.expressionScope()),
				typeFactory.booleanType, quantifier, boundVariableList, restriction, expression);
	}

	/**
	 * A real literal expression.
	 * 
	 * @param value The (arbitrary precision) real value.
	 * @return The real literal expression.
	 */
	@Override
	public RealLiteralExpression realLiteralExpression(CIVLSource source, BigDecimal value) {
		return new CommonRealLiteralExpression(source, typeFactory.realType, value);
	}

	@Override
	public ScopeofExpression scopeofExpression(CIVLSource source, LHSExpression argument) {
		return new CommonScopeofExpression(source, typeFactory.scopeType, argument);
	}

	/**
	 * A self expression. Used to referenced the current process.
	 * 
	 * @return A new self expression.
	 */
	@Override
	public SelfExpression selfExpression(CIVLSource source) {
		return new CommonSelfExpression(source, typeFactory.processType);
	}

	@Override
	public ProcnullExpression procnullExpression(CIVLSource source) {
		return new CommonProcnullExpression(source, typeFactory.processType, this.nullProcessValue);
	}

	@Override
	public SizeofExpression sizeofExpressionExpression(CIVLSource source, Expression argument) {
		return new CommonSizeofExpression(source, typeFactory.integerType, argument);
	}

	@Override
	public SizeofTypeExpression sizeofTypeExpression(CIVLSource source, CIVLType type) {
		Variable typeStateVariable = type.getStateVariable();
		Scope expressionScope = null;

		// If the type has a state variable, then the scope of the sizeof
		// expression is the scope of the state variable; otherwise, the scope
		// of the sizeof expression is NULL
		if (typeStateVariable != null) {
			expressionScope = typeStateVariable.scope();
		}
		return new CommonSizeofTypeExpression(source, expressionScope, typeFactory.integerType, type);
	}

	/**
	 * An expression for an array index operation. e.g. a[i]
	 * 
	 * @param array An expression evaluating to an array.
	 * @param index An expression evaluating to an integer.
	 * @return The array index expression.
	 */
	@Override
	public SubscriptExpression subscriptExpression(CIVLSource source, LHSExpression array, Expression index) {
		CIVLType arrayType = array.getExpressionType();
		CIVLType expressionType;
		boolean isArraySet = arrayType.isSetType();
		boolean isIndiceSet = index.getExpressionType().isSetType();

		if (isArraySet)
			arrayType = ((CIVLSetType) arrayType).elementType();
		if (arrayType instanceof CIVLArrayType)
			expressionType = ((CIVLArrayType) arrayType).elementType();
		else if (arrayType instanceof CIVLPointerType)
			expressionType = ((CIVLPointerType) arrayType).baseType();
		else
			throw new CIVLInternalException("Unable to set expression type for the subscript expression: ", source);

		if (isArraySet || isIndiceSet)
			expressionType = typeFactory.civlSetType(expressionType);
		return new CommonSubscriptExpression(source, join(array.expressionScope(), index.expressionScope()),
				getLower(array.lowestScope(), index.lowestScope()), expressionType, array, index);
	}

	@Override
	public FunctionCallExpression functionCallExpression(CallOrSpawnStatement callStatement) {
		return new CommonFunctionCallExpression(callStatement.getSource(), callStatement);
	}

	/**
	 * A unary expression. One of {-,!}.
	 * 
	 * @param operator The unary operator.
	 * @param operand  The expression to which the operator is applied.
	 * @return The unary expression.
	 */
	@Override
	public UnaryExpression unaryExpression(CIVLSource source, UNARY_OPERATOR operator, Expression operand) {
		switch (operator) {
		case NEGATIVE:
		case BIG_O:
			return new CommonUnaryExpression(source, operand.getExpressionType(), operator, operand);
		case NOT:
			assert operand.getExpressionType().isBoolType();
			return new CommonUnaryExpression(source, typeFactory.booleanType, operator, operand);
		case BIT_NOT:
			assert operand.getExpressionType().isIntegerType();
			return new CommonUnaryExpression(source, typeFactory.integerType, operator, operand);
		default:
			throw new CIVLInternalException("Unknown unary operator: " + operator, source);

		}
	}

	/**
	 * A variable expression.
	 * 
	 * @param variable The variable being referenced.
	 * @return The variable expression.
	 */
	@Override
	public VariableExpression variableExpression(CIVLSource source, Variable variable) {
		return new CommonVariableExpression(source, variable);
	}

	/*
	 * *********************************************************************
	 * Fragments and Statements
	 * *********************************************************************
	 */

	@Override
	public AssignStatement assignStatement(CIVLSource civlSource, Location source, LHSExpression lhs, Expression rhs,
			boolean isInitialization) {
		return new CommonAssignStatement(civlSource, join(lhs.expressionScope(), rhs.expressionScope()),
				getLower(lhs.lowestScope(), rhs.lowestScope()), source, this.trueExpression(civlSource), lhs, rhs,
				isInitialization);
	}

	@Override
	public Expression trueExpression(CIVLSource civlSource) {
		return this.booleanLiteralExpression(civlSource, true);
	}

	// @Override
	// public Fragment assumeFragment(CIVLSource civlSource, Location source,
	// Expression expression) {
	// return new CommonFragment(new CommonAssumeStatement(civlSource, source,
	// this.trueExpression(civlSource), expression));
	// }
	//
	// @Override
	// public Fragment assertFragment(CIVLSource civlSource, Location source,
	// Expression condition, Expression[] explanation) {
	// Scope statementScope = condition.expressionScope();
	// Scope lowestScope = condition.lowestScope();
	// Scope lowestScopeExplanation = getLower(explanation);
	//
	// if (explanation != null)
	// for (Expression arg : explanation) {
	// statementScope = join(statementScope, arg.expressionScope());
	// }
	// return new CommonFragment(
	// new CommonAssertStatement(civlSource, statementScope, getLower(
	// lowestScope, lowestScopeExplanation), source, this
	// .trueExpression(civlSource), condition, explanation));
	// }

	@Override
	public Fragment atomicFragment(Fragment fragment, Location start, Location end) {
		Statement enterAtomic = new CommonAtomicLockAssignStatement(start.getSource(), this.systemScope,
				this.systemScope, start, this.trueExpression(start.getSource()), true,
				this.atomicLockVariableExpression, this.selfExpression(systemSource));
		Statement leaveAtomic = new CommonAtomicLockAssignStatement(end.getSource(), this.systemScope, this.systemScope,
				end, this.trueExpression(end.getSource()), false, this.atomicLockVariableExpression,
				new CommonUndefinedProcessExpression(systemSource, typeFactory.processType,
						this.undefinedProcessValue));
		Fragment startFragment = new CommonFragment(enterAtomic);
		Fragment endFragment = new CommonFragment(leaveAtomic);
		Fragment result;
		Expression startGuard = null;

		start.setEnterAtomic();
		if (fragment.startLocation().getNumOutgoing() == 1) {
			Statement firstStmtOfBody = fragment.startLocation().getSoleOutgoing();

			startGuard = firstStmtOfBody.guard();
			firstStmtOfBody.setGuard(this.trueExpression(startGuard.getSource()));
		} else {
			for (Statement statement : fragment.startLocation().outgoing()) {
				if (startGuard == null)
					startGuard = statement.guard();
				else {
					startGuard = this.binaryExpression(startGuard.getSource(), BINARY_OPERATOR.OR, startGuard,
							statement.guard());
				}
			}
		}
		if (startGuard != null)
			enterAtomic.setGuard(startGuard);
		end.setLeaveAtomic();
		result = startFragment.combineWith(fragment);
		result = result.combineWith(endFragment);
		return result;
	}

	@Override
	public Statement atomicEnter(Location loc) {
		CIVLSource source = loc.getSource();

		return new CommonAtomicLockAssignStatement(source, systemScope, systemScope, loc, trueExpression(source), true,
				atomicLockVariableExpression, selfExpression(systemSource));
	}

	@Override
	public Statement atomicExit(Location loc) {
		CIVLSource source = loc.getSource();

		return new CommonAtomicLockAssignStatement(source, systemScope, systemScope, loc, trueExpression(source), false,
				atomicLockVariableExpression, selfExpression(systemSource));
	}

	@Override
	public CallOrSpawnStatement callOrSpawnStatement(CIVLSource civlSource, Location source, boolean isCall,
			Expression function, List<Expression> arguments, Expression guard, boolean isInitialization) {
		Scope statementScope = null;

		for (Expression arg : arguments) {
			statementScope = join(statementScope, arg.expressionScope());
		}
		return new CommonCallStatement(civlSource, statementScope, getLowerScope(arguments), source,
				guard != null ? guard : this.trueExpression(civlSource), isCall, null, function, arguments,
				isInitialization);
	}

	@Override
	public NoopStatement gotoBranchStatement(CIVLSource civlSource, Location source, String label) {
		return new CommonGotoBranchStatement(civlSource, source, this.trueExpression(civlSource), label);
	}

	@Override
	public NoopStatement ifElseBranchStatement(CIVLSource civlSource, Location source, Expression guard,
			boolean isTrue) {
		return new CommonIfElseBranchStatement(civlSource, source,
				guard != null ? guard : this.trueExpression(civlSource), isTrue);
	}

	@Override
	public NoopStatement loopBranchStatement(CIVLSource civlSource, Location source, Expression guard, boolean isTrue,
			LoopContract loopContract) {
		if (loopContract != null)
			loopContract.setLocation(source);
		return new CommonLoopBranchStatement(civlSource, source,
				guard != null ? guard : this.trueExpression(civlSource), isTrue, loopContract);
	}

	@Override
	public MallocStatement mallocStatement(CIVLSource civlSource, Location source, LHSExpression lhs,
			CIVLType staticElementType, Expression scopeExpression, Expression sizeExpression, int mallocId,
			Expression guard) {
		// SymbolicType dynamicElementType = staticElementType
		// .getDynamicType(universe);
		// SymbolicArrayType dynamicObjectType = (SymbolicArrayType) universe
		// .canonic(universe.arrayType(dynamicElementType));
		// SymbolicExpression undefinedObject =
		// undefinedValue(dynamicObjectType);

		return new CommonMallocStatement(civlSource, null,
				getLowerScope(Arrays.asList(lhs, scopeExpression, sizeExpression)), source,
				guard != null ? guard : this.trueExpression(civlSource), mallocId, scopeExpression, staticElementType,
				null, null, sizeExpression, lhs);
	}

	@Override
	public NoopStatement noopStatement(CIVLSource civlSource, Location source, Expression expression) {
		return new CommonNoopStatement(civlSource, source, this.trueExpression(civlSource), expression);
	}

	/**
	 * A noop statement.
	 * 
	 * @param source The source location for this noop statement.
	 * @return A new noop statement.
	 */
	@Override
	public NoopStatement noopStatementWtGuard(CIVLSource civlSource, Location source, Expression guard) {
		return new CommonNoopStatement(civlSource, source, guard != null ? guard : this.trueExpression(civlSource),
				null);
	}

	@Override
	public NoopStatement switchBranchStatement(CIVLSource civlSource, Location source, Expression guard,
			Expression label) {
		return new CommonSwitchBranchStatement(civlSource, source,
				guard != null ? guard : this.trueExpression(civlSource), label);
	}

	@Override
	public NoopStatement switchBranchStatement(CIVLSource civlSource, Location source, Expression guard) {
		return new CommonSwitchBranchStatement(civlSource, source,
				guard != null ? guard : this.trueExpression(civlSource));
	}

	@Override
	public Fragment returnFragment(CIVLSource civlSource, Location source, Expression expression,
			CIVLFunction function) {
		return new CommonFragment(
				new CommonReturnStatement(civlSource, source, this.trueExpression(civlSource), expression, function));
	}

	/*
	 * ********************************************************************* CIVL
	 * Source *********************************************************************
	 */

	@Override
	public CIVLSource sourceOf(ASTNode node) {
		return sourceOf(node.getSource());
	}

	@Override
	public CIVLSource sourceOf(Source abcSource) {
		return new ABC_CIVLSource(abcSource);
	}

	@Override
	public CIVLSource sourceOfBeginning(ASTNode node) {
		return sourceOfToken(node.getSource().getFirstToken());
	}

	@Override
	public CIVLSource sourceOfEnd(ASTNode node) {
		return sourceOfToken(node.getSource().getLastToken());
	}

	@Override
	public CIVLSource sourceOfSpan(ASTNode node1, ASTNode node2) {
		return sourceOfSpan(node1.getSource(), node2.getSource());
	}

	@Override
	public CIVLSource sourceOfSpan(CIVLSource source1, CIVLSource source2) {
		return sourceOfSpan(((ABC_CIVLSource) source1).getABCSource(), ((ABC_CIVLSource) source2).getABCSource());
	}

	@Override
	public CIVLSource sourceOfSpan(Source abcSource1, Source abcSource2) {
		return sourceOf(tokenFactory.join(abcSource1, abcSource2));
	}

	@Override
	public CIVLSource sourceOfToken(CivlcToken token) {
		return sourceOf(tokenFactory.newSource(token));
	}

	/*
	 * ********************************************************************* Atomic
	 * Lock Variable
	 * *********************************************************************
	 */

	@Override
	public VariableExpression atomicLockVariableExpression() {
		return this.atomicLockVariableExpression;
	}

	@Override
	public Variable timeCountVariable() {
		return this.timeCountVariable;
	}

	@Override
	public Variable brokenTimeVariable() {
		return this.brokenTimeVariable;
	}

	/*
	 * ********************************************************************* Other
	 * helper methods
	 * *********************************************************************
	 */

	@Override
	public void computeImpactScopeOfLocation(Location location) {
		if (location.enterAtomic()) {
			Stack<Integer> atomicFlags = new Stack<Integer>();
			Set<Integer> checkedLocations = new HashSet<Integer>();
			Scope impactScope = null;
			Stack<Location> workings = new Stack<Location>();

			workings.add(location);
			// DFS searching for reachable statements inside the $atomic/$atom
			// block
			while (!workings.isEmpty()) {
				Location currentLocation = workings.pop();

				checkedLocations.add(currentLocation.id());
				if (location.enterAtomic() && currentLocation.enterAtomic())
					atomicFlags.push(1);
				if (location.enterAtomic() && currentLocation.leaveAtomic())
					atomicFlags.pop();
				if (atomicFlags.isEmpty()) {
					if (location.enterAtomic()) {
						if (!currentLocation.enterAtomic())
							atomicFlags.push(1);
					}
					continue;
				}
				for (int i = 0; i < currentLocation.getNumOutgoing(); i++) {
					Statement s = currentLocation.getOutgoing(i);

					if (s instanceof CallOrSpawnStatement) {
						if (((CallOrSpawnStatement) s).isCall()) {
							// calling a function is considered as impact
							// scope because we don't keep record of the
							// total impact scope of a function
							location.setImpactScopeOfAtomicOrAtomBlock(systemScope);
							return;
						}
					}
					if (impactScope == null)
						impactScope = s.statementScope();
					else
						impactScope = join(impactScope, s.statementScope());
					if (impactScope != null && impactScope.id() == systemScope.id()) {
						location.setImpactScopeOfAtomicOrAtomBlock(impactScope);
						return;
					}
					if (s.target() != null) {
						if (!checkedLocations.contains(s.target().id())) {
							workings.push(s.target());

						}
					}
				}

			}
			location.setImpactScopeOfAtomicOrAtomBlock(impactScope);
			return;
		}
	}

	@Override
	public SymbolicExpression nullProcessValue() {
		return this.nullProcessValue;
	}

	@Override
	public boolean isTrue(Expression expression) {
		return expression instanceof BooleanLiteralExpression && ((BooleanLiteralExpression) expression).value();
	}

	@Override
	public SymbolicUniverse universe() {
		return universe;
	}

	@Override
	public AbstractFunction abstractFunction(CIVLSource source, Identifier name, Scope parameterScope,
			List<Variable> parameters, CIVLType returnType, Scope containingScope, String attribute) {
		return new CommonAbstractFunction(source, name, parameterScope, parameters, returnType, containingScope,
				containingScope.numFunctions(), attribute);
	}

	@Override
	public CIVLFunction function(CIVLSource source, boolean isAtomic, Identifier name, Scope parameterScope,
			List<Variable> parameters, CIVLType returnType, Scope containingScope, Location startLocation) {
		for (Variable v : parameterScope.variables()) {
			if (v.type().isArrayType()) {
				throw new CIVLInternalException("Parameter of array type.", v);
			}
		}
		return new CommonFunction(source, isAtomic, name, parameterScope, parameters, returnType, containingScope,
				containingScope != null ? containingScope.numFunctions() : -1, startLocation);
	}

	@Override
	public Identifier identifier(CIVLSource source, String name) {
		Identifier result = identifiers.get(name);

		if (result == null) {
			StringObject stringObject = universe.stringObject(name);

			result = new CommonIdentifier(source, stringObject);
			identifiers.put(name, result);
		}
		return result;
	}

	@Override
	public Location location(CIVLSource source, Scope scope) {
		return new CommonLocation(source, scope, locationID++);
	}

	@Override
	public Model model(CIVLSource civlSource, CIVLFunction system, Program program) {
		return new CommonModel(civlSource, this, system, program);
	}

	@Override
	public Scope scope(CIVLSource source, Scope parent, List<Variable> variables, CIVLFunction function) {
		Scope newScope;
		Set<Variable> myVariables = new HashSet<Variable>();

		if (scopeID != ModelConfiguration.STATIC_CONSTANT_SCOPE) {
			Variable heapVariable;

			heapVariable = this.variable(source, modelBuilder.heapType,
					this.identifier(source, ModelConfiguration.HEAP_VAR), 0);
			myVariables.add(heapVariable);
		}
		myVariables.addAll(variables);
		newScope = new CommonScope(source, parent, myVariables, scopeID++);
		if (newScope.id() == ModelConfiguration.STATIC_ROOT_SCOPE) {
			this.createAtomicLockVariable(newScope);
			createTimeVariables(newScope);
		}
		if (parent != null) {
			parent.addChild(newScope);
		}
		newScope.setFunction(function);
		return newScope;
	}

	@Override
	public void setTokenFactory(TokenFactory tokens) {
		this.tokenFactory = tokens;
	}

	@Override
	public SystemFunction systemFunction(CIVLSource source, Identifier name, Scope parameterScope,
			List<Variable> parameters, CIVLType returnType, Scope containingScope, String libraryName) {
		boolean needsEnabler = false;

		switch (libraryName) {
		case "bundle":
		case "civlc":
		case "comm":
		case "domain":
		case "mpi":
		case "pointer":
		case "pthread":
		case "scope":
		case "seq":
		case "stdio":
		case "stdlib":
		case "string":
		case "time":
			needsEnabler = true;
			break;
		default:
		}
		return new CommonSystemFunction(source, name, parameterScope, parameters, returnType, containingScope,
				containingScope.numFunctions(), (Location) null, libraryName, needsEnabler);
	}

	@Override
	public CIVLSource systemSource() {
		return systemSource;
	}

	@Override
	public Variable variable(CIVLSource source, CIVLType type, Identifier name, int vid) {
		return variableWork(source, type, name, vid, false);
	}

	@Override
	public Variable variableAsParameter(CIVLSource source, CIVLType type, Identifier name, int vid) {
		return variableWork(source, type, name, vid, true);
	}

	private Variable variableWork(CIVLSource source, CIVLType type, Identifier name, int vid, boolean isParameter) {
		Variable variable = new CommonVariable(source, type, name, vid, isParameter);

		return variable;
	}

	@Override
	public int getProcessId(SymbolicExpression processValue) {
		return extractIntField(processValue, zeroObj);
	}

	@Override
	public void setScopes(Scope scope) {
		this.systemScope = scope;
		this.staticScope = scope.parent();
		// TODO : why model factory has to deal with scope values ? -z
		this.systemScopeId = typeFactory.scopeType.scopeIdentityToValueOperator(universe).apply(scope.id());
	}

	/* *************************** Private Methods ************************* */
	// TODO: why model factory has to deal with undefined values ?! Semantics
	// package is where this should be in. -z
	@Override
	public SymbolicExpression undefinedValue(SymbolicType type) {
		if (type.equals(typeFactory.processSymbolicType))
			return this.undefinedProcessValue;
		else if (type.equals(typeFactory.scopeSymbolicType))
			return typeFactory.scopeType.scopeIdentityToValueOperator(universe)
					.apply(ModelConfiguration.DYNAMIC_UNDEFINED_SCOPE);
		else {
			SymbolicExpression result = universe.symbolicConstant(universe.stringObject("UNDEFINED"), type);

			return result;
		}
	}

	@Override
	public CompoundLiteralExpression compoundLiteralExpression(CIVLSource source, Scope exprScope, CIVLType type,
			boolean isStringLiteral) {
		return new CommonCompoundLiteralExpression(source, exprScope, exprScope, type, isStringLiteral);
	}

	@Override
	public CharLiteralExpression charLiteralExpression(CIVLSource sourceOf, char value) {
		return new CommonCharLiteralExpression(sourceOf, typeFactory.charType, value);
	}

	@Override
	public Variable newAnonymousVariableForArrayLiteral(CIVLSource sourceOf, Scope scope, CIVLArrayType type) {
		String name = ModelConfiguration.ANONYMOUS_VARIABLE_PREFIX + this.anonymousVariableId++;
		Variable variable = this.variable(sourceOf, type, this.identifier(sourceOf, name), scope.numVariables());

		variable.setConst(true);
		scope.addVariable(variable);
		return variable;
	}

	@Override
	public Variable newAnonymousVariableForConstantArrayLiteral(CIVLSource sourceOf, CIVLArrayType type,
			SymbolicExpression value) {
		String name = ModelConfiguration.ANONYMOUS_VARIABLE_PREFIX + this.anonymousVariableId++;
		Variable variable = this.variable(sourceOf, type, this.identifier(sourceOf, name),
				this.staticScope.numVariables());

		variable.setConst(true);
		variable.setConstantValue(value);
		this.staticScope.addVariable(variable);
		return variable;
	}

	@Override
	public Variable newAnonymousVariable(CIVLSource sourceOf, Scope scope, CIVLType type) {
		String name = ModelConfiguration.ANONYMOUS_VARIABLE_PREFIX + this.anonymousVariableId++;
		Variable variable = this.variable(sourceOf, type, this.identifier(sourceOf, name), scope.numVariables());

		scope.addVariable(variable);
		return variable;
	}

	// @Override
	// public Scope currentScope() {
	// return this.currentScope;
	// }

	@Override
	public Fragment anonFragment() {
		return this.anonFragment;
	}

	@Override
	public void clearAnonFragment() {
		this.anonFragment = new CommonFragment();
	}

	@Override
	public void addAnonStatement(Statement statement) {
		this.anonFragment.addNewStatement(statement);
	}

	@Override
	public Expression systemGuardExpression(CallOrSpawnStatement call) {
		if (call.function() != null && call.function().functionContract() != null) {
			Expression guard = call.function().functionContract().guard();

			if (guard != null && guard instanceof BooleanLiteralExpression)
				return guard;
		}

		SystemGuardExpression systemGuard = new CommonSystemGuardExpression(call.getSource(), call.statementScope(),
				((SystemFunction) call.function()).getLibrary(), call.function(), call.arguments(),
				typeFactory.booleanType);

		if (this.isTrue(call.guard()))
			return systemGuard;
		return this.binaryExpression(call.guard().getSource(), BINARY_OPERATOR.AND, call.guard(), systemGuard);
	}

	@Override
	public Expression functionGuardExpression(CIVLSource source, Expression function, List<Expression> arguments) {
		FunctionGuardExpression functionGuard = new CommonFunctionGuardExpression(source, function, arguments,
				typeFactory.booleanType);

		return functionGuard;
	}

	@Override
	public Model model() {
		return this.modelBuilder.getModel();
	}

	@Override
	public boolean isPocessIdDefined(int pid) {
		if (pid == -1)
			return false;
		return true;
	}

	@Override
	public boolean isProcNull(SymbolicExpression procValue) {
		int pid = extractIntField(procValue, zeroObj);

		return this.isProcessIdNull(pid);
	}

	@Override
	public Fragment civlForEnterFragment(CIVLSource source, Location src, Expression dom, List<Variable> variables,
			Variable counter) {
		DomainIteratorStatement statement = new CommonCivlForEnterStatement(source, src, this.trueExpression(source),
				dom, variables, counter);

		return new CommonFragment(statement);
	}

	@Override
	public RegularRangeExpression regularRangeExpression(CIVLSource source, Expression low, Expression high,
			Expression step) {
		return new CommonRegularRangeExpression(source, joinScope(Arrays.asList(low, high, step)),
				getLowerScope(Arrays.asList(low, high, step)), typeFactory.rangeType, low, high, step);
	}

	@Override
	public RecDomainLiteralExpression recDomainLiteralExpression(CIVLSource source, List<Expression> ranges,
			CIVLType type) {
		return new CommonRecDomainLiteralExpression(source, getLowerScope(ranges), ranges, type);
	}

	@Override
	public DomainGuardExpression domainGuard(CIVLSource source, List<Variable> vars, Variable counter,
			Expression domain) {
		return new CommonDomainGuardExpression(source, typeFactory.booleanType, domain, vars, counter);
	}

	@Override
	public VariableExpression domSizeVariable(CIVLSource source, Scope scope) {
		Variable variable = this.variable(source, typeFactory.integerType,
				this.identifier(source, DOM_SIZE_PREFIX + this.domSizeVariableId++), scope.numVariables());

		scope.addVariable(variable);
		return this.variableExpression(source, variable);
	}

	@Override
	public Identifier getLiteralDomCounterIdentifier(CIVLSource source, int count) {
		return identifier(source, "__LiteralDomain_counter" + count + "__");
	}

	@Override
	public VariableExpression parProcsVariable(CIVLSource source, CIVLType type, Scope scope) {
		Variable variable = this.variable(source, type,
				this.identifier(source, PAR_PROC_PREFIX + this.parProcsVariableId++), scope.numVariables());

		scope.addVariable(variable);
		return this.variableExpression(source, variable);
	}

	@Override
	public CivlParForSpawnStatement civlParForEnterStatement(CIVLSource source, Location location, Expression domain,
			VariableExpression domSize, VariableExpression procsVar, CIVLFunction parProcFunc) {
		return new CommonCivlParForSpawnStatement(source, location, this.trueExpression(source), domain, domSize,
				procsVar, parProcFunc);
	}

	@Override
	public SymbolicConstant getHideConstant() {
		if (hideConstant == null) {
			hideConstant = universe.symbolicConstant(universe.stringObject("AF_$hide"), universe
					.functionType(Arrays.asList(typeFactory.pointerSymbolicType()), typeFactory.pointerSymbolicType()));
		}
		return hideConstant;
	}

	@Override
	public boolean isProcessIdNull(int pid) {
		if (pid == -2)
			return true;
		return false;
	}

	@Override
	public ArraySliceReference arraySliceReference(ArraySliceKind sliceKind, Expression index) {
		return new CommonArraySliceReference(sliceKind, index);
	}

	@Override
	public SelfReference selfReference() {
		return new CommonSelfReference();
	}

	@Override
	public StructOrUnionFieldReference structFieldReference(int fieldIndex) {
		return new CommonStructOrUnionFieldReference(fieldIndex);
	}

	@Override
	public MemoryUnitExpression memoryUnitExpression(CIVLSource source, Variable variable, CIVLType objType,
			MemoryUnitReference reference, boolean writable, boolean hasPinterRef) {
		return new CommonMemoryUnitExpression(source, variable, objType, reference, writable, hasPinterRef);
	}

	@Override
	public CIVLTypeFactory typeFactory() {
		return this.typeFactory;
	}

	/* *************************** Private Methods ************************* */

	/**
	 * An atomic lock variable is used to keep track of the process that executes an
	 * $atomic block which prevents interleaving with other processes. This variable
	 * is maintained as a global variable
	 * {@link ComonModelFactory#ATOMIC_LOCK_VARIABLE_INDEX} of <code>$proc</code>
	 * type in the root scope in the CIVL model (always with index 0).
	 * 
	 * @param scope The scope of the atomic lock variable, and should always be the
	 *              root scope.
	 */
	private void createAtomicLockVariable(Scope scope) {
		// Since the atomic lock variable is not declared explicitly in the CIVL
		// model specification, the system source will be used here.
		Variable variable = this.variable(this.systemSource, typeFactory.processType,
				this.identifier(this.systemSource, ModelConfiguration.ATOMIC_LOCK_VARIABLE_INDEX),
				scope.numVariables());

		this.atomicLockVariableExpression = this.variableExpression(this.systemSource, variable);
		scope.addVariable(variable);
	}

	private void createTimeVariables(Scope scope) {
		// Since the atomic lock variable is not declared explicitly in the CIVL
		// model specification, the system source will be used here.
		if (modelBuilder.hasNextTimeCountCall) {
			timeCountVariable = this.variable(this.systemSource, typeFactory.integerType,
					this.identifier(this.systemSource, ModelConfiguration.TIME_COUNT_VARIABLE), scope.numVariables());
			timeCountVariable.setStatic(true);
			scope.addVariable(timeCountVariable);
		}
		if (modelBuilder.timeLibIncluded) {
			brokenTimeVariable = this.variable(this.systemSource, typeFactory.integerType,
					this.identifier(systemSource, ModelConfiguration.BROKEN_TIME_VARIABLE), scope.numVariables());
			scope.addVariable(brokenTimeVariable);
		}
	}

	/* *************************** Private Methods ************************* */

	/**
	 * Gets a Java concrete int from a symbolic expression or throws exception.
	 * 
	 * @param source
	 * 
	 * @param expression a numeric expression expected to hold concrete int value
	 * @return the concrete int
	 * @throws ClassCastException if a concrete integer value cannot be extracted
	 */
	private int extractInt(NumericExpression expression) {
		assert expression.operator() == SymbolicOperator.CONCRETE;
		SymbolicObject object = expression.argument(0);

		return ((IntegerNumber) ((NumberObject) object).getNumber()).intValue();
	}

	/**
	 * Gets a concrete Java int from the field of a symbolic expression of tuple
	 * type or throws exception.
	 * 
	 * @param source
	 * 
	 * @param tuple      symbolic expression of tuple type
	 * @param fieldIndex index of a field in that tuple
	 * @return the concrete int value of that field
	 */
	private int extractIntField(SymbolicExpression tuple, IntObject fieldIndex) {
		NumericExpression field = (NumericExpression) universe.tupleRead(tuple, fieldIndex);

		return extractInt(field);
	}

	/**
	 * @param s0 A scope. May be null.
	 * @param s1 A scope. May be null.
	 * @return The scope that is the join, or least common ancestor in the scope
	 *         tree, of s0 and s1. Null if both are null. If exactly one of s0 and
	 *         s1 are null, returns the non-null scope.
	 */
	private Scope join(Scope s0, Scope s1) {
		Set<Scope> s0Ancestors = new HashSet<Scope>();
		Scope s0Ancestor = s0;
		Scope s1Ancestor = s1;

		if (s0 == null) {
			return s1;
		} else if (s1 == null) {
			return s0;
		}
		s0Ancestors.add(s0Ancestor);
		while (s0Ancestor.parent() != null) {
			s0Ancestor = s0Ancestor.parent();
			s0Ancestors.add(s0Ancestor);
		}
		while (true) {
			if (s0Ancestors.contains(s1Ancestor)) {
				return s1Ancestor;
			}
			s1Ancestor = s1Ancestor.parent();
		}
	}

	// TODO: move this to modelFactory:
	public Scope leastCommonAncestor(Scope scope0, Scope scope1) {
		Scope ancestor;

		if (scope0.isDescendantOf(scope1))
			return scope1;
		else if (scope1.isDescendantOf(scope0))
			return scope0;
		ancestor = scope1.parent();
		while (ancestor != null) {
			if (scope0.isDescendantOf(ancestor))
				return ancestor;
			else
				ancestor = ancestor.parent();
		}
		return ancestor;
	}

	/**
	 * Returns the lower scope. Precondition: one of the scope must be the ancestor
	 * of the other if they are not the same.
	 * 
	 * @param s0
	 * @param s1
	 * @return
	 */
	private Scope getLower(Scope s0, Scope s1) {
		if (s0 == null)
			return s1;
		if (s1 == null)
			return s0;
		if (s0.id() != s1.id()) {
			Scope sParent0 = s0, sParent1 = s1;

			while (sParent0.id() > 0 && sParent1.id() > 0) {
				sParent0 = sParent0.parent();
				sParent1 = sParent1.parent();
			}
			if (sParent0.id() == 0)
				return s1;
			return s0;
		}
		return s0;
	}

	private Scope getLowerScope(List<Expression> expressions) {
		Scope scope = null;

		for (Expression expression : expressions) {
			if (expression != null)
				scope = getLower(scope, expression.lowestScope());
		}
		return scope;
	}

	/**
	 * Calculate the join scope (the most local common scope) of a list of
	 * expressions.
	 * 
	 * @param expressions The list of expressions whose join scope is to be
	 *                    computed.
	 * @return The join scope of the list of expressions.
	 */
	private Scope joinScope(List<Expression> expressions) {
		Scope scope = null;

		for (Expression expression : expressions) {
			scope = join(scope, expression.expressionScope());
		}
		return scope;
	}

	@Override
	public List<CodeAnalyzer> codeAnalyzers() {
		return this.codeAnalyzers;
	}

	@Override
	public void setCodeAnalyzers(List<CodeAnalyzer> analyzers) {
		this.codeAnalyzers = analyzers;
	}

	@Override
	public List<Variable> inputVariables() {
		return this.inputVariables;
	}

	@Override
	public void addInputVariable(Variable variable) {
		this.inputVariables.add(variable);
	}

	@Override
	public FunctionIdentifierExpression elaborateDomainPointer() {
		if (this.elaborateDomainFuncPointer == null) {
			List<Variable> parameters = new ArrayList<>(2);
			CIVLFunction function;
			Scope paraScope;

			parameters.add(this.variable(systemSource, typeFactory.domainType(typeFactory.rangeType()),
					this.identifier(systemSource, "domain"), 1));
			paraScope = this.scope(systemSource, systemScope, parameters, null);
			function = this.systemFunction(systemSource, this.identifier(systemSource, "$elaborate_domain"), paraScope,
					parameters, typeFactory.voidType, systemScope, "civlc");
			paraScope.setFunction(function);
			this.elaborateDomainFuncPointer = this.functionIdentifierExpression(systemSource, function);
		}
		return this.elaborateDomainFuncPointer;
	}

	@Override
	public NoopStatement noopStatementTemporary(CIVLSource civlSource, Location source) {
		return new CommonNoopStatement(civlSource, source, this.trueExpression(civlSource), true);
	}

	@Override
	public NoopStatement noopStatementForVariableDeclaration(CIVLSource civlSource, Location source) {
		return new CommonNoopStatement(civlSource, source, this.trueExpression(civlSource), false, true);
	}

	@Override
	public WildcardExpression wildcardExpression(CIVLSource source, CIVLType type) {
		return new CommonWildcardExpression(source, type);
	}

	@Override
	public Nothing nothing(CIVLSource source) {
		return new CommonNothing(source);
	}

	@Override
	public LoopContract loopContract(CIVLSource civlSource, Location loopLocation, List<Expression> loopInvariants,
			List<LHSExpression> loopAssigns, List<Expression> loopVariants) {
		return new CommonLoopContract(civlSource, loopLocation, loopInvariants, loopAssigns, loopVariants);
	}

	@Override
	public ArrayLambdaExpression arrayLambdaExpression(CIVLSource source, CIVLArrayType arrayType,
			List<Pair<List<Variable>, Expression>> boundVariableList, Expression restriction, Expression expression) {
		return new CommonArrrayLambdaExpression(source,
				join(expression.expressionScope(), restriction.expressionScope()), arrayType, boundVariableList,
				restriction, expression);
	}

	@Override
	public Scope staticConstantScope() {
		return this.staticScope;
	}

	@Override
	public ParallelAssignStatement parallelAssignStatement(CIVLSource source,
			List<Pair<LHSExpression, Expression>> assignPairs) {
		return new CommonParallelAssignStatement(source, null, this.trueExpression(source), assignPairs);
	}

	@Override
	public LambdaExpression lambdaExpression(CIVLSource source, CIVLFunctionType functionType, Variable freeVariable,
			Expression expression) {
		return new CommonLambdaExpression(source, functionType, freeVariable, expression);
	}

	@Override
	public ExtendedQuantifiedExpression extendedQuantifiedExpression(CIVLSource source, CIVLType type,
			ExtendedQuantifier quant, Expression lo, Expression hi, Expression function) {
		return new CommonExtendedQuantifiedExpression(source, type, quant, lo, hi, function);
	}

	@Override
	public CIVLFunction nondetFunction(CIVLSource source, Identifier name, CIVLType returnType, Scope containingScope) {
		return new CommonNondetFunction(source, name, returnType, containingScope,
				containingScope != null ? containingScope.numFunctions() : -1);
	}

	@Override
	public LogicFunction logicFunction(CIVLSource source, Identifier name, Scope parameterScope,
			List<Variable> parameters, CIVLType outputType, int[] pointerToHeap, Scope containingScope,
			Expression definition) {
		LogicFunction logicFunction = new CommonLogicFunction(source, name, parameterScope, parameters, outputType,
				pointerToHeap, containingScope, containingScope != null ? containingScope.numFunctions() : -1,
				definition, location(source, parameterScope));
		// add logic function to model:
		modelBuilder.seenLogicFunctions.add(logicFunction);
		logicFunction.setLogic(true);
		return logicFunction;
	}
}
