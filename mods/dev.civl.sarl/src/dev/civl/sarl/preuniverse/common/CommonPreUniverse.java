package dev.civl.sarl.preuniverse.common;

import static dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator.APPLY;
import static dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator.ARRAY;
import static dev.civl.sarl.number.IF.Numbers.REAL_FACTORY;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.civl.sarl.IF.CanonicalRenamer;
import dev.civl.sarl.IF.Predicate;
import dev.civl.sarl.IF.SARLBoundException;
import dev.civl.sarl.IF.SARLConstants;
import dev.civl.sarl.IF.SARLException;
import dev.civl.sarl.IF.SARLInternalException;
import dev.civl.sarl.IF.UnaryOperator;
import dev.civl.sarl.IF.expr.ArrayElementReference;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NTReferenceExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.NumericSymbolicConstant;
import dev.civl.sarl.IF.expr.OffsetReference;
import dev.civl.sarl.IF.expr.ReferenceExpression;
import dev.civl.sarl.IF.expr.ReferenceExpression.ReferenceKind;
import dev.civl.sarl.IF.expr.SymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;
import dev.civl.sarl.IF.expr.TupleComponentReference;
import dev.civl.sarl.IF.expr.UnionMemberReference;
import dev.civl.sarl.IF.expr.valueSetReference.NTValueSetReference;
import dev.civl.sarl.IF.expr.valueSetReference.VSArrayElementReference;
import dev.civl.sarl.IF.expr.valueSetReference.VSArraySectionReference;
import dev.civl.sarl.IF.expr.valueSetReference.VSIdentityReference;
import dev.civl.sarl.IF.expr.valueSetReference.VSOffsetReference;
import dev.civl.sarl.IF.expr.valueSetReference.VSTupleComponentReference;
import dev.civl.sarl.IF.expr.valueSetReference.VSUnionMemberReference;
import dev.civl.sarl.IF.expr.valueSetReference.ValueSetReference;
import dev.civl.sarl.IF.number.IntegerNumber;
import dev.civl.sarl.IF.number.Number;
import dev.civl.sarl.IF.number.NumberFactory;
import dev.civl.sarl.IF.number.RationalNumber;
import dev.civl.sarl.IF.object.BooleanObject;
import dev.civl.sarl.IF.object.CharObject;
import dev.civl.sarl.IF.object.IntObject;
import dev.civl.sarl.IF.object.NumberObject;
import dev.civl.sarl.IF.object.StringObject;
import dev.civl.sarl.IF.object.SymbolicObject;
import dev.civl.sarl.IF.object.SymbolicObject.SymbolicObjectKind;
import dev.civl.sarl.IF.object.SymbolicSequence;
import dev.civl.sarl.IF.type.SymbolicArrayType;
import dev.civl.sarl.IF.type.SymbolicCompleteArrayType;
import dev.civl.sarl.IF.type.SymbolicFunctionType;
import dev.civl.sarl.IF.type.SymbolicFunctionType.SpecialRelationKind;
import dev.civl.sarl.IF.type.SymbolicIntegerType;
import dev.civl.sarl.IF.type.SymbolicMapType;
import dev.civl.sarl.IF.type.SymbolicRealType;
import dev.civl.sarl.IF.type.SymbolicSetType;
import dev.civl.sarl.IF.type.SymbolicTupleType;
import dev.civl.sarl.IF.type.SymbolicType;
import dev.civl.sarl.IF.type.SymbolicType.SymbolicTypeKind;
import dev.civl.sarl.IF.type.SymbolicTypeSequence;
import dev.civl.sarl.IF.type.SymbolicUninterpretedType;
import dev.civl.sarl.IF.type.SymbolicUnionType;
import dev.civl.sarl.expr.IF.BooleanExpressionFactory;
import dev.civl.sarl.expr.IF.ExpressionFactory;
import dev.civl.sarl.expr.IF.NumericExpressionFactory;
import dev.civl.sarl.expr.common.HomogeneousExpression;
import dev.civl.sarl.ideal.IF.Constant;
import dev.civl.sarl.object.IF.ObjectFactory;
import dev.civl.sarl.preuniverse.IF.FactorySystem;
import dev.civl.sarl.preuniverse.IF.PreUniverse;
import dev.civl.sarl.type.IF.SymbolicTypeFactory;
import dev.civl.sarl.util.Pair;
import dev.civl.sarl.util.SequenceFactory;

// TODO: add to CERTAINTY: PROBABLY (with bound on probability)
// need to count the number of events. this is change in CIVL

public class CommonPreUniverse implements PreUniverse {

	// Fields...

	/**
	 * an uninterpreted function representing reduction over a number of operands
	 * see also
	 * {@link #makeReduction(NumericExpression, SymbolicExpression, SymbolicExpression)}
	 */
	private String reductionName = "$rdc";

	/**
	 * The maximum value of unsigned char value in C
	 */
	private final static int MAX_VALUE_UNSIGNED_CHAR = 256;

	/**
	 * A sequence of array writes in which the index never exceeds this bound will
	 * be represented in a dense format, i.e., like a regular Java array.
	 */
	public final static int DENSE_ARRAY_MAX_SIZE = 100000;

	/**
	 * A forall or exists expression over an integer range will be expanded to a
	 * conjunction or disjunction as long as the the size of the range (high-low)
	 * does not exceed this bound.
	 */
	public final static int QUANTIFIER_EXPAND_BOUND = 1000;

	// TODO: Make this parameter something that can be easily configured
	private int INTEGER_BIT_BOUND = 32;

	/**
	 * The upper bound on the probability of error when deciding whether a
	 * polynomial is 0. Must be a rational number in [0,1). If 0, probabilistic
	 * techniques are not used. In general, this should be a very small positive
	 * number.
	 */
	private RationalNumber probabilisticBound;

	/**
	 * IntegerNumber versions of the corresponding static int fields.
	 */
	private IntegerNumber denseArrayMaxSize, quantifierExpandBound;

	/**
	 * Factory for producing general symbolic objects, canonicalizing them, etc.
	 */
	protected ObjectFactory objectFactory;

	/**
	 * Factory for producing symbolic types.
	 */
	protected SymbolicTypeFactory typeFactory;

	/**
	 * Factory for producing general symbolic expressions.
	 */
	protected ExpressionFactory expressionFactory;

	/**
	 * Factory for producing and manipulating boolean expressions.
	 */
	protected BooleanExpressionFactory booleanFactory;

	/**
	 * The factory for producing and manipulating concrete numbers (such as infinite
	 * precision integers and rationals).
	 */
	protected NumberFactory numberFactory;

	/**
	 * Factory for dealing with symbolic expressions of numeric (i.e., integer or
	 * real) type. Includes dealing with relational expressions less-than and
	 * less-than-or-equal-to.
	 */
	private NumericExpressionFactory numericFactory;

	private SequenceFactory<SymbolicExpression> exprSeqFactory;

	/**
	 * The comparator on all symbolic objects used by this universe to sort such
	 * objects.
	 */
	private Comparator<SymbolicObject> objectComparator;

	/**
	 * The object used to give quantified (bound) variables unique names.
	 */
	private BoundCleaner cleaner;

	/** The boolean type. */
	private SymbolicType booleanType;

	/** The ideal integer type. */
	private SymbolicIntegerType integerType;

	/** The ideal real type. */
	private SymbolicRealType realType;

	/**
	 * The "NULL" symbolic expression, which is not the Java null but is used to
	 * represent "no expression" in certain contexts where a Java null is not
	 * allowed or desirable. It is a symbolic expression with operator NULL, null
	 * type, and no arguments.
	 */
	private SymbolicExpression nullExpression;

	/**
	 * The boolean symbolic concrete values true and false as symbolic expressions.
	 */
	private BooleanExpression trueExpr, falseExpr;

	/**
	 * Symbolic constant used as bound variable in certain lambda expressions. It
	 * has integer type.
	 */
	private NumericSymbolicConstant arrayIndex;

	final private String MEM_ASSIGN_INCOMPLETE_ERROR_MSG = "Attempt to assign to a section of $mem which is not fully complete.";

	private int validCount = 0;

	private int proverValidCount = 0;

	/**
	 * The stream to which output such as theorem prover queries should be sent.
	 */
	private PrintStream out = System.out;

	/**
	 * Should SARL reasoner queries be printed?
	 */
	private boolean showQueries = false;

	/**
	 * Should the theorem prover queries to the underlying prover(s) be printed?
	 */
	private boolean showProverQueries = false;

	/**
	 * A short, human-readable description of what the caller is trying to
	 * accomplish with the next theorem-prover query. Printed alongside the query
	 * when {@link #showProverQueries} is true. May be <code>null</code>.
	 */
	private String queryExplanation = null;

	private SymbolicExpression[] emptyExprArray = new SymbolicExpression[0];

	private String errFileName = "ProverOutput.txt";

	/**
	 * An array used to store created bit vector type with a concrete length.
	 */
	private List<SymbolicConstant> int2bvConstants = Collections.synchronizedList(new ArrayList<SymbolicConstant>());

	/**
	 * Shall this universe use backwards substitution to solve for certain numeric
	 * expressions in terms of others when simplifying?
	 */
	private boolean useBackwardSubstitution = SARLConstants.useBackwardSubstitution;

	// Constructor...

	/**
	 * Constructs a new CommonSymbolicUniverse from the given system of factories.
	 * The probabilistic bound is given default value of 2^(-128).
	 * 
	 * @param system a factory system
	 */
	public CommonPreUniverse(FactorySystem system) {
		objectFactory = system.objectFactory();
		typeFactory = system.typeFactory();
		expressionFactory = system.expressionFactory();
		booleanFactory = system.booleanFactory();
		numericFactory = expressionFactory.numericFactory();
		numberFactory = numericFactory.numberFactory();
		objectComparator = objectFactory.comparator();
		booleanType = typeFactory.booleanType();
		integerType = typeFactory.integerType();
		realType = typeFactory.realType();
		trueExpr = booleanFactory.trueExpr();
		falseExpr = booleanFactory.falseExpr();
		denseArrayMaxSize = numberFactory.integer(DENSE_ARRAY_MAX_SIZE);
		quantifierExpandBound = numberFactory.integer(QUANTIFIER_EXPAND_BOUND);
		nullExpression = expressionFactory.nullExpression();
		cleaner = new BoundCleaner(this, objectFactory, typeFactory);
		arrayIndex = (NumericSymbolicConstant) symbolicConstant(stringObject("i"), integerType);
		exprSeqFactory = new SequenceFactory<SymbolicExpression>() {
			@Override
			protected SymbolicExpression[] newArray(int size) {
				return new SymbolicExpression[size];
			}

		};

		RationalNumber twoTo128 = numberFactory.power(numberFactory.rational(numberFactory.integer(2)), 128);

		probabilisticBound = numberFactory.divide(numberFactory.oneRational(), twoTo128);
	}

	// Helper methods...

	/**
	 * <p>
	 * Returns a new instance of {@link SARLException} with the given message. (A
	 * {@link SARLException} is a {@link RuntimeException}, so it is not required to
	 * declare when it is thrown.) It is provided here for convenience since it is
	 * used a lot and it is short to say <code>throw err(...)</code> than
	 * <code>throw new SARLException(...)</code>.
	 * </p>
	 * 
	 * <p>
	 * This type of exception is usually thrown when the user does something wrong,
	 * like provide bad parameter values to a method.
	 * </p>
	 * 
	 * @param message an error message
	 * @return a new instance of {@link SARLException} with that message.
	 */
	protected SARLException err(String message) {
		return new SARLException(message);
	}

	/**
	 * Returns a new instance of {@link SARLBoundException}. This exception is
	 * thrown when an index is out of bounds.
	 * 
	 * NOTE: currently this is not being used. Instead, a special OUT_OF_BOUNDS
	 * expression is returned for such references.
	 * 
	 * @param expr       the symbolic expression into which the index points
	 *                   (typically, an array)
	 * @param index      the offending index
	 * @param lowerBound the lowest value that an index should take
	 * @param upperBound the greatest value that an index should take
	 * @param location   the kind of operation that resulted in the exception, e.g.,
	 *                   "an array write operation" or "an array read operation"
	 * @return a new instance of {@link SARLBoundException} formed from the given
	 *         parameters
	 */
	protected SARLBoundException boundErr(SymbolicExpression expr, NumericExpression index,
			NumericExpression lowerBound, NumericExpression upperBound, String location) {
		return new SARLBoundException(expr, index, lowerBound, upperBound, location);
	}

	/**
	 * Returns an expression representing an out-of-bounds reference into an array.
	 * 
	 * @param array      the array
	 * @param index      the index which is out of bounds for that array
	 * @param lowerBound the lowest value that an index should take; currently not
	 *                   used
	 * @param upperBound the greatest value that an index should take; currently not
	 *                   used
	 * @param location   the kind of operation that resulted in the exception, e.g.,
	 *                   "an array write operation" or "an array read operation";
	 *                   currently not used
	 * @return expression of the form "OUT_OF_BOUNDS(array,index)" where
	 *         OUT_OF_BOUNDS is an uninterpreted function with return type the
	 *         element type of the array
	 */
	protected SymbolicExpression outOfBoundExpr(SymbolicExpression array, NumericExpression index,
			NumericExpression lowerBound, NumericExpression upperBound, String location) {
		SymbolicArrayType arrayType = (SymbolicArrayType) array.type();
		SymbolicFunctionType ft = functionType(Arrays.asList(arrayType, integerType), arrayType.elementType());
		SymbolicConstant f = symbolicConstant(stringObject("OUT_OF_BOUND"), ft);
		SymbolicExpression result = apply(f, Arrays.asList(array, index));

		return result;
	}

	/**
	 * Throws a new instance of SARLInternalException with the given message. This
	 * type of exception is thrown when something bad happens that shouldn't be
	 * possible. (It is the developers' fault, not the user's.) A message that this
	 * is an internal error and it should be reported to the developers is
	 * pre-pended to the given message.
	 * 
	 * Note SARLInterException extends SARLException extends RuntimeException.
	 * 
	 * @param message an explanation of the unexpected thing that happened
	 * @return new instance of SARLInternalExcpetion with that message
	 */
	protected SARLInternalException ierr(String message) {
		return new SARLInternalException(message);
	}

	protected SymbolicExpression expression(SymbolicOperator operator, SymbolicType type, SymbolicObject[] arguments) {
		return expressionFactory.expression(operator, type, arguments);
	}

	protected SymbolicExpression expression(SymbolicOperator operator, SymbolicType type, SymbolicObject arg0) {
		return expressionFactory.expression(operator, type, arg0);
	}

	protected SymbolicExpression expression(SymbolicOperator operator, SymbolicType type, SymbolicObject arg0,
			SymbolicObject arg1) {
		return expressionFactory.expression(operator, type, arg0, arg1);
	}

	protected SymbolicExpression expression(SymbolicOperator operator, SymbolicType type, SymbolicObject arg0,
			SymbolicObject arg1, SymbolicObject arg2) {
		return expressionFactory.expression(operator, type, arg0, arg1, arg2);
	}

	protected NumericExpression zero(SymbolicType type) {
		if (type.isInteger())
			return zeroInt();
		else if (type.isReal())
			return zeroReal();
		else
			throw ierr("Expected type int or real, not " + type);
	}

	private SymbolicConstant boundVar(int index, SymbolicType type) {
		return symbolicConstant(stringObject("x" + index), type);
	}

	/**
	 * Returns a symbolic constant of integer type for use in binding expressions
	 * (e.g., "forall int i...").
	 * 
	 * @param index unique ID to be used in name of the symbolic constant
	 * @return the symbolic constant
	 */
	private NumericSymbolicConstant intBoundVar(int index) {
		return numericFactory.symbolicConstant(stringObject("i" + index), integerType);
	}

	/**
	 * Returns a boolean expression which holds iff the two types are compatible,
	 * using nestingDepth to control the name of the next bound variable.
	 * 
	 * @param type0 a symbolic type
	 * @param type1 a symbolic type
	 * @return a boolean expression which holds iff the two types are compatible
	 */
	private BooleanExpression compatible(SymbolicType type0, SymbolicType type1, int nestingDepth) {
		// since the "equals" case should be by far the most frequent
		// case, we check it first...
		if (type0.equals(type1))
			return trueExpr;

		SymbolicTypeKind kind = type0.typeKind();

		if (kind != type1.typeKind())
			return falseExpr;
		switch (kind) {
		case BOOLEAN:
		case CHAR:
			// only one BOOLEAN type; only one CHAR type...
			throw ierr("Unreachable: types are not equal but both have kind " + kind);
		case INTEGER:
		case REAL:
			// types are not equal but have same kind. We do not consider
			// Herbrand real and real to be compatible, e.g.
			return falseExpr;
		case ARRAY: {
			SymbolicArrayType a0 = (SymbolicArrayType) type0;
			SymbolicArrayType a1 = (SymbolicArrayType) type1;
			BooleanExpression result = compatible(a0.elementType(), a1.elementType(), nestingDepth);

			if (a0.isComplete() && a1.isComplete())
				result = and(result, equals(((SymbolicCompleteArrayType) a0).extent(),
						((SymbolicCompleteArrayType) a1).extent(), nestingDepth));
			return result;
		}
		case FUNCTION:
			return and(
					compatibleTypeSequence(((SymbolicFunctionType) type0).inputTypes(),
							((SymbolicFunctionType) type1).inputTypes(), nestingDepth),
					compatible(((SymbolicFunctionType) type0).outputType(), ((SymbolicFunctionType) type1).outputType(),
							nestingDepth));
		case TUPLE: {
			SymbolicTupleType t0 = (SymbolicTupleType) type0;
			SymbolicTupleType t1 = (SymbolicTupleType) type1;

			if (!t0.name().equals(t1.name()))
				return falseExpr;
			return compatibleTypeSequence(t0.sequence(), t1.sequence(), nestingDepth);
		}
		case UNION: {
			SymbolicUnionType t0 = (SymbolicUnionType) type0;
			SymbolicUnionType t1 = (SymbolicUnionType) type1;

			if (!t0.name().equals(t1.name()))
				return falseExpr;
			return compatibleTypeSequence(t0.sequence(), t1.sequence(), nestingDepth);
		}
		case UNINTERPRETED: {
			SymbolicUninterpretedType t0 = (SymbolicUninterpretedType) type0;
			SymbolicUninterpretedType t1 = (SymbolicUninterpretedType) type1;

			return bool(t0.equals(t1));
		}
		default:
			throw ierr("unreachable");
		}
	}

	/**
	 * Returns a boolean expression which holds iff the two types are compatible.
	 * Two types are compatible if it is possible for them to have a value in
	 * common. For the most part, this is the same as saying they are the same type.
	 * The exception is that an incomplete array type and a complete array type with
	 * compatible element types are compatible.
	 * 
	 * @param type0 a type
	 * @param type1 a type
	 * @return a boolean expression which holds iff the two types are compatible
	 */
	@Override
	public BooleanExpression compatible(SymbolicType type0, SymbolicType type1) {
		return compatible(type0, type1, 0);
	}

	/**
	 * Are the two types definitely incompatible? If this method returns true, the
	 * types cannot be compatible (i.e., there cannot be any object belonging to
	 * both). If it returns false, the two types are probably compatible, but there
	 * is no guarantee.
	 * 
	 * @param type0 a type
	 * @param type1 a type
	 * @return true iff definitely not compatible
	 */
	protected boolean incompatible(SymbolicType type0, SymbolicType type1) {
		return compatible(type0, type1).isFalse();
	}

	private BooleanExpression equals(ReferenceExpression arg0, ReferenceExpression arg1, int quantifierDepth) {
		BooleanExpression result;
		ReferenceKind kind = arg0.referenceKind();

		if (kind != arg1.referenceKind())
			result = falseExpr;
		else if (arg0 instanceof NTReferenceExpression) {
			ReferenceExpression parent0 = ((NTReferenceExpression) arg0).getParent();
			ReferenceExpression parent1 = ((NTReferenceExpression) arg1).getParent();

			result = equals(parent0, parent1, quantifierDepth);
			if (result.isFalse())
				return result;
			switch (kind) {
			case ARRAY_ELEMENT: {
				ArrayElementReference ref0 = (ArrayElementReference) arg0;
				ArrayElementReference ref1 = (ArrayElementReference) arg1;

				result = and(result, equals(ref0.getIndex(), ref1.getIndex(), quantifierDepth));
				break;
			}
			case OFFSET: {
				OffsetReference ref0 = (OffsetReference) arg0;
				OffsetReference ref1 = (OffsetReference) arg1;

				result = and(result, equals(ref0.getOffset(), ref1.getOffset(), quantifierDepth));
				break;
			}
			case TUPLE_COMPONENT: {
				TupleComponentReference ref0 = (TupleComponentReference) arg0;
				TupleComponentReference ref1 = (TupleComponentReference) arg1;

				result = ref0.getIndex().equals(ref1.getIndex()) ? result : falseExpr;
				break;
			}
			case UNION_MEMBER: {
				UnionMemberReference ref0 = (UnionMemberReference) arg0;
				UnionMemberReference ref1 = (UnionMemberReference) arg1;

				result = ref0.getIndex().equals(ref1.getIndex()) ? result : falseExpr;
				break;
			}
			default:
				throw err("Unreachable because the only kinds of NTReferenceExpression " + "are as listed above.\n"
						+ "This is: " + kind);
			}
		} else {
			// either both are identity of both are null
			result = trueExpr;
		}
		return result;
	}

	/**
	 * Compares two arguments to check compatibility first, then passes those
	 * arguments to a case/switch. Each case checks the equality of the two
	 * arguments based on the following types:
	 * <ul>
	 * <li>BOOLEAN: Tests 2 boolean values for equality</li>
	 * <li>CHAR: Tests 2 char values for equality. Checks whether both are concrete
	 * or not.</li>
	 * <li>INTEGER:</li>
	 * <li>REAL: Checks whether 2 real values are equal</li>
	 * <li>ARRAY: Checks whether 2 arrays are equal</li>
	 * <li>FUNCTION: Takes a sequence and checks the content and equality of its
	 * elements</li>
	 * <li>TUPLE: Checks whether 2 tuples are equal</li>
	 * <li>UNION: Scans 2 separate unions to check equality</li>
	 * </ul>
	 * 
	 * @param arg0            SymbolicType
	 * @param arg1            SymbolicType
	 * @param quantifierDepth int
	 * @return BooleanExpression
	 */
	private BooleanExpression equals(SymbolicExpression arg0, SymbolicExpression arg1, int quantifierDepth) {
		if (arg0.equals(arg1))
			return trueExpr;

		SymbolicType type = arg0.type();
		BooleanExpression result = compatible(type, arg1.type(), quantifierDepth);

		if (result.equals(falseExpr))
			return result;
		if (arg0 instanceof ReferenceExpression && arg1 instanceof ReferenceExpression)
			return equals((ReferenceExpression) arg0, (ReferenceExpression) arg1, quantifierDepth);
		switch (type.typeKind()) {
		case BOOLEAN:
			return equiv((BooleanExpression) arg0, (BooleanExpression) arg1);
		case CHAR: {
			SymbolicOperator op0 = arg0.operator();
			SymbolicOperator op1 = arg1.operator();

			if (op0 == SymbolicOperator.CONCRETE && op1 == SymbolicOperator.CONCRETE) {
				return bool(arg0.argument(0).equals(arg1.argument(0)));
			}
			return booleanFactory.booleanExpression(SymbolicOperator.EQUALS, arg0, arg1);
		}
		case INTEGER:
		case REAL:
			return numericFactory.equals((NumericExpression) arg0, (NumericExpression) arg1);
		case ARRAY: {
			NumericExpression length = length(arg0);

			if (!(type instanceof SymbolicCompleteArrayType) || !(arg1.type() instanceof SymbolicCompleteArrayType))
				result = and(result, equals(length, length(arg1), quantifierDepth));
			if (result.isFalse())
				return result;
			else {
				NumericSymbolicConstant index = intBoundVar(quantifierDepth);

				result = and(result, forallInt(index, zeroInt(), length,
						equals(arrayRead(arg0, index), arrayRead(arg1, index), quantifierDepth + 1)));
				return result;
			}
		}
		case FUNCTION: {
			SymbolicTypeSequence inputTypes = ((SymbolicFunctionType) type).inputTypes();
			int numInputs = inputTypes.numTypes();

			if (numInputs == 0) {
				result = and(result, booleanFactory.booleanExpression(SymbolicOperator.EQUALS, arg0, arg1));
			} else {
				SymbolicConstant[] boundVariables = new SymbolicConstant[numInputs];
				SymbolicSequence<?> sequence;
				BooleanExpression expr;

				for (int i = 0; i < numInputs; i++)
					boundVariables[i] = boundVar(quantifierDepth + i, inputTypes.getType(i));
				sequence = objectFactory.sequence(boundVariables);
				expr = equals(apply(arg0, sequence), apply(arg1, sequence), quantifierDepth + numInputs);
				for (int i = numInputs - 1; i >= 0; i--)
					expr = forall(boundVariables[i], expr);
				result = and(result, expr);
				return result;
			}

			return result;
		}
		case TUPLE: {
			int numComponents = ((SymbolicTupleType) type).sequence().numTypes();

			for (int i = 0; i < numComponents; i++) {
				IntObject index = intObject(i);

				result = and(result, equals(tupleRead(arg0, index), tupleRead(arg1, index), quantifierDepth));
			}
			return result;
		}
		case UNION: {
			SymbolicUnionType unionType = (SymbolicUnionType) type;

			if (arg0.operator() == SymbolicOperator.UNION_INJECT) {
				IntObject index = (IntObject) arg0.argument(0);
				SymbolicExpression value0 = (SymbolicExpression) arg0.argument(1);

				if (arg1.operator() == SymbolicOperator.UNION_INJECT)
					return index.equals(arg1.argument(0))
							? and(result, equals(value0, (SymbolicExpression) arg1.argument(1), quantifierDepth))
							: falseExpr;
				else
					return and(result,
							and(unionTest(index, arg1), equals(value0, unionExtract(index, arg1), quantifierDepth)));
			} else if (arg1.operator() == SymbolicOperator.UNION_INJECT) {
				IntObject index = (IntObject) arg1.argument(0);

				return and(result, and(unionTest(index, arg0),
						equals((SymbolicExpression) arg1.argument(1), unionExtract(index, arg0), quantifierDepth)));
			} else {
				int numTypes = unionType.sequence().numTypes();
				BooleanExpression expr = falseExpr;

				for (int i = 0; i < numTypes; i++) {
					IntObject index = intObject(i);
					BooleanExpression clause = result;

					clause = and(clause, unionTest(index, arg0));
					if (clause.isFalse())
						continue;
					clause = and(clause, unionTest(index, arg1));
					if (clause.isFalse())
						continue;
					clause = and(clause, equals(unionExtract(index, arg0), unionExtract(index, arg1), quantifierDepth));
					if (clause.isFalse())
						continue;
					expr = or(expr, clause);
				}
				return expr;
			}
		}
		case UNINTERPRETED:
			if (arg0.operator() == SymbolicOperator.CONCRETE && arg1.operator() == SymbolicOperator.CONCRETE) {
				SymbolicUninterpretedType uninterpretedType = (SymbolicUninterpretedType) type;
				IntObject key0 = uninterpretedType.soleSelector().apply(arg0);
				IntObject key1 = uninterpretedType.soleSelector().apply(arg1);

				return bool(key0.equals(key1));
			} else {
				return (BooleanExpression) expression(SymbolicOperator.EQUALS, booleanType(), arg0, arg1);
			}
		default:
			throw ierr("Unknown type: " + type);
		}
	}

	private BooleanExpression compatibleTypeSequence(SymbolicTypeSequence seq0, SymbolicTypeSequence seq1,
			int nestingDepth) {
		int size = seq0.numTypes();

		if (size != seq1.numTypes())
			return falseExpr;
		if (size == 0)
			return trueExpr;
		else {
			BooleanExpression result = compatible(seq0.getType(0), seq1.getType(0), nestingDepth);

			if (size > 1)
				for (int i = 1; i < size; i++)
					result = and(result, compatible(seq0.getType(i), seq1.getType(i), nestingDepth));
			return result;
		}
	}

	protected BooleanExpression forallIntConcrete(NumericSymbolicConstant index, IntegerNumber low, IntegerNumber high,
			BooleanExpression predicate) {
		BooleanExpression result = trueExpr;

		for (IntegerNumber i = low; numberFactory.compare(i, high) < 0; i = numberFactory.increment(i)) {
			SymbolicExpression iExpression = number(numberObject(i));
			BooleanExpression substitutedPredicate = (BooleanExpression) simpleSubstituter(index, iExpression)
					.apply(predicate);

			result = and(result, substitutedPredicate);
		}
		return result;
	}

	protected BooleanExpression existsIntConcrete(SymbolicConstant index, IntegerNumber low, IntegerNumber high,
			BooleanExpression predicate) {
		BooleanExpression result = falseExpr;

		for (IntegerNumber i = low; numberFactory.compare(i, high) < 0; i = numberFactory.increment(i)) {
			SymbolicExpression iExpression = number(numberObject(i));
			BooleanExpression substitutedPredicate = (BooleanExpression) simpleSubstituter(index, iExpression)
					.apply(predicate);

			result = or(result, substitutedPredicate);
		}
		return result;
	}

	// Public methods...

	public NumericExpressionFactory numericExpressionFactory() {
		return numericFactory;
	}

	// Public methods implementing SymbolicUniverse...

	@Override
	public boolean getShowQueries() {
		return showQueries;
	}

	@Override
	public void setShowQueries(boolean value) {
		this.showQueries = value;
	}

	@Override
	public boolean getShowProverQueries() {
		return showProverQueries;
	}

	@Override
	public void setShowProverQueries(boolean value) {
		this.showProverQueries = value;
	}

	@Override
	public String getQueryExplanation() {
		return queryExplanation;
	}

	@Override
	public void setQueryExplanation(String explanation) {
		this.queryExplanation = explanation;
	}

	@Override
	public PrintStream getOutputStream() {
		return out;
	}

	@Override
	public void setOutputStream(PrintStream out) {
		this.out = out;
	}

	/**
	 * For exists and forall, must provide an instance of
	 * SymbolicConstantExpressionIF as arg0. Cannot be applied to make concrete
	 * expressions or SymbolicConstantExpressionIF. There are separate methods for
	 * those.
	 * 
	 * TODO: It seems that this is expecting {@link SymbolicSequence}s for iterable
	 * arguments. Why?
	 */
	@Override
	public SymbolicExpression make(SymbolicOperator operator, SymbolicType type, SymbolicObject[] args) {
		switch (operator) {
		case ADD:
			return add(type, args);
		case AND:
			return and(args);
		case APPLY: // 2 args: function and sequence
			if (isSigmaCall((SymbolicExpression) args[0]))
				return makeSigma((SymbolicSequence<?>) args[1]);
			if (isPermutCall((SymbolicExpression) args[0]))
				return makePermut((SymbolicSequence<?>) args[1]);
			if (isReductionCall((SymbolicExpression) args[0])) {
				SymbolicSequence<?> reduceArgs = (SymbolicSequence<?>) args[1];

				return makeReduction((NumericExpression) reduceArgs.get(0), reduceArgs.get(1), reduceArgs.get(2));
			}
			return apply((SymbolicExpression) args[0], (SymbolicSequence<?>) args[1]);
		case ARRAY_LAMBDA:
			return arrayLambda((SymbolicCompleteArrayType) type, (SymbolicExpression) args[0]);
		case ARRAY_READ:
			return arrayRead((SymbolicExpression) args[0], (NumericExpression) args[1]);
		case ARRAY_WRITE:
			return arrayWrite((SymbolicExpression) args[0], (NumericExpression) args[1], (SymbolicExpression) args[2]);
		case BIT_AND:
			return bitand((NumericExpression) args[0], (NumericExpression) args[1]);
		case BIT_NOT:
			return bitnot((NumericExpression) args[0]);
		case BIT_OR:
			return bitor((NumericExpression) args[0], (NumericExpression) args[1]);
		case BIT_XOR:
			return bitxor((NumericExpression) args[0], (NumericExpression) args[1]);
		case BIT_SHIFT_LEFT:
			return bitshiftLeft((NumericExpression) args[0], (NumericExpression) args[1]);
		case BIT_SHIFT_RIGHT:
			return bitshiftRight((NumericExpression) args[0], (NumericExpression) args[1]);
		case CAST:
			return cast(type, (SymbolicExpression) args[0]);
		case CONCRETE:
			if (type.isNumeric())
				return numericFactory.number((NumberObject) args[0]);
			else
				return expression(SymbolicOperator.CONCRETE, type, args[0]);
		case COND:
			return cond((BooleanExpression) args[0], (SymbolicExpression) args[1], (SymbolicExpression) args[2]);
		case DENSE_ARRAY_WRITE:
			return denseArrayWrite((SymbolicExpression) args[0], (SymbolicSequence<?>) args[1]);
		case DENSE_TUPLE_WRITE:
			return denseTupleWrite((SymbolicExpression) args[0], (SymbolicSequence<?>) args[1]);
		case DERIV:
			return derivative((SymbolicExpression) args[0], (IntObject) args[1], (IntObject) args[2]);
		case DIFFERENTIABLE: {
			@SuppressWarnings("unchecked")
			Iterable<? extends NumericExpression> lowers = (Iterable<? extends NumericExpression>) args[2];
			@SuppressWarnings("unchecked")
			Iterable<? extends NumericExpression> uppers = (Iterable<? extends NumericExpression>) args[3];

			return differentiable((SymbolicExpression) args[0], (IntObject) args[1], lowers, uppers);
		}
		case DIVIDE:
			return divide((NumericExpression) args[0], (NumericExpression) args[1]);
		case EQUALS:
			return equals((SymbolicExpression) args[0], (SymbolicExpression) args[1]);
		case EXISTS:
			return exists((SymbolicConstant) args[0], (BooleanExpression) args[1]);
		case FORALL:
			return forall((SymbolicConstant) args[0], (BooleanExpression) args[1]);
		case INT_DIVIDE:
			return divide((NumericExpression) args[0], (NumericExpression) args[1]);
		case LAMBDA:
			return lambda((SymbolicConstant) args[0], (SymbolicExpression) args[1]);
		case LENGTH:
			return length((SymbolicExpression) args[0]);
		case LESS_THAN:
			return lessThan((NumericExpression) args[0], (NumericExpression) args[1]);
		case LESS_THAN_EQUALS:
			return lessThanEquals((NumericExpression) args[0], (NumericExpression) args[1]);
		case MODULO:
			return modulo((NumericExpression) args[0], (NumericExpression) args[1]);
		case MULTIPLY:
			return multiply(type, args);
		case NEGATIVE:
			return minus((NumericExpression) args[0]);
		case NEQ:
			return neq((SymbolicExpression) args[0], (SymbolicExpression) args[1]);
		case NOT:
			return not((BooleanExpression) args[0]);
		case OR:
			return or(args);
		case POWER: // exponent could be expression or int constant
			if (args[1] instanceof SymbolicExpression)
				return power((NumericExpression) args[0], (NumericExpression) args[1]);
			else
				return power((NumericExpression) args[0], (IntegerNumber) ((NumberObject) args[1]).getNumber());
		case TUPLE:
			return tuple((SymbolicTupleType) type, Arrays.asList(args));
		case ARRAY:
			return array(((SymbolicArrayType) type).elementType(), Arrays.asList(args));
		case SUBTRACT:
			return subtract((NumericExpression) args[0], (NumericExpression) args[1]);
		case SYMBOLIC_CONSTANT:
			return symbolicConstant((StringObject) args[0], type);
		case TUPLE_READ:
			return tupleRead((SymbolicExpression) args[0], (IntObject) args[1]);
		case TUPLE_WRITE:
			return tupleWrite((SymbolicExpression) args[0], (IntObject) args[1], (SymbolicExpression) args[2]);
		case UNION_EXTRACT:
			return unionExtract((IntObject) args[0], (SymbolicExpression) args[1]);
		case UNION_INJECT: {
			SymbolicExpression expression = (SymbolicExpression) args[1];
			SymbolicUnionType unionType = (SymbolicUnionType) type;

			return unionInject(unionType, (IntObject) args[0], expression);

		}
		case UNION_TEST: {
			SymbolicExpression expression = (SymbolicExpression) args[1];

			return unionTest((IntObject) args[0], expression);
		}
		default:
			throw ierr("Unknown expression kind: " + operator);
		}
	}

	@Override
	public NumberFactory numberFactory() {
		return numberFactory;
	}

	@Override
	public NumericExpression add(Iterable<? extends NumericExpression> args) {
		if (args == null)
			throw err("Argument args to method add was null");

		Iterator<? extends NumericExpression> iter = args.iterator();

		if (!iter.hasNext())
			throw err("Iterable argument to add was empty but should have at least one element");
		else {
			NumericExpression result = iter.next();

			while (iter.hasNext()) {
				NumericExpression next = iter.next();

				result = add(result, next);
			}
			return result;
		}
	}

	/**
	 * Adds a sequence of elements by applying binary addition (@link
	 * {@link #add(NumericExpression, NumericExpression)}) from left to right.
	 * 
	 * @param type the type of the arguments and result
	 * @param args array whose elements are all non-<code>null</code> instances of
	 *             {@link NumericExpression}
	 * @return the sum of the elements of <code>args</code>
	 */
	private NumericExpression add(SymbolicType type, SymbolicObject[] args) {
		int n = args.length;

		if (n == 0)
			return type.isInteger() ? numericFactory.zeroInt() : numericFactory.zeroReal();
		else {
			NumericExpression result = (NumericExpression) args[0];

			for (int i = 1; i < n; i++) {
				NumericExpression next = (NumericExpression) args[i];

				result = add(result, next);
			}
			return result;
		}
	}

	/**
	 * Cannot assume anything about the collection of arguments. Therefore just
	 * apply the binary and operator to them in order.
	 */
	@Override
	public BooleanExpression and(Iterable<? extends BooleanExpression> args) {
		BooleanExpression result = trueExpr;

		for (BooleanExpression arg : args)
			result = and(result, arg);
		return result;
	}

	private BooleanExpression and(SymbolicObject[] args) {
		int n = args.length;

		if (n == 0)
			return trueExpr;
		else {
			BooleanExpression result = (BooleanExpression) args[0];

			for (int i = 1; i < n; i++) {
				BooleanExpression next = (BooleanExpression) args[i];

				result = and(result, next);
			}
			return result;
		}
	}

	private BooleanExpression or(SymbolicObject[] args) {
		int n = args.length;

		if (n == 0)
			return falseExpr;
		else {
			BooleanExpression result = (BooleanExpression) args[0];

			for (int i = 1; i < n; i++) {
				BooleanExpression next = (BooleanExpression) args[i];

				result = or(result, next);
			}
			return result;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Assumes the given arguments are in CNF form and produces the conjunction of
	 * the two.
	 * </p>
	 * 
	 * <p>
	 * 
	 * <pre>
	 * CNF form: true | false | AND set | e
	 * </pre>
	 * 
	 * where set is a set of boolean expressions which are not true, false, or AND
	 * expressions and set has cardinality at least 2. e is any boolean expression
	 * not a true, false, or AND expression. Strategy: eliminate the true and false
	 * cases in the obvious way. Then
	 * 
	 * <pre>
	 * AND s1, AND s2 -> AND union(s1,s2)
	 * AND s1, e -> AND add(s1, e)
	 * AND e1, e2-> if e1.equals(e2) then e1 else AND {e1,e2}
	 * </pre>
	 * </p>
	 */
	@Override
	public BooleanExpression and(BooleanExpression arg0, BooleanExpression arg1) {
		return booleanFactory.and(arg0, arg1);
	}

	@Override
	public SymbolicType pureType(SymbolicType type) {
		return typeFactory.pureType(type);
	}

	@Override
	public SymbolicType booleanType() {
		return booleanType;
	}

	@Override
	public SymbolicIntegerType integerType() {
		return integerType;
	}

	@Override
	public SymbolicIntegerType herbrandIntegerType() {
		return typeFactory.herbrandIntegerType();
	}

	@Override
	public SymbolicRealType realType() {
		return realType;
	}

	@Override
	public SymbolicUninterpretedType symbolicUninterpretedType(String name) {
		return typeFactory.uninterpretedType(stringObject(name));
	}

	@Override
	public SymbolicIntegerType boundedIntegerType(NumericExpression min, NumericExpression max, boolean cyclic) {
		return typeFactory.boundedIntegerType(min, max, cyclic);
	}

	@Override
	public SymbolicRealType herbrandRealType() {
		return typeFactory.herbrandRealType();
	}

	@Override
	public SymbolicType characterType() {
		return typeFactory.characterType();
	}

	@Override
	public SymbolicCompleteArrayType arrayType(SymbolicType elementType, NumericExpression extent) {
		return typeFactory.arrayType(elementType, extent);
	}

	@Override
	public SymbolicArrayType arrayType(SymbolicType elementType) {
		return typeFactory.arrayType(elementType);
	}

	@Override
	public Pair<Integer, SymbolicType> arrayDimensionAndBaseType(SymbolicArrayType arrayType) {
		SymbolicType elementType;
		int dimension = 0;

		elementType = arrayType;
		do {
			elementType = ((SymbolicArrayType) elementType).elementType();
			dimension++;
		} while (elementType.typeKind().equals(SymbolicTypeKind.ARRAY));
		return new Pair<>(dimension, elementType);
	}

	public SymbolicTypeSequence typeSequence(SymbolicType[] types) {
		return typeFactory.sequence(types);
	}

	public SymbolicTypeSequence typeSequence(Iterable<? extends SymbolicType> types) {
		return typeFactory.sequence(types);
	}

	public SymbolicTupleType tupleType(StringObject name, SymbolicTypeSequence fieldTypes) {
		return typeFactory.tupleType(name, fieldTypes);
	}

	@Override
	public SymbolicTupleType tupleType(StringObject name, Iterable<? extends SymbolicType> types) {
		return tupleType(name, typeSequence(types));
	}

	public SymbolicFunctionType functionType(SymbolicTypeSequence inputTypes, SymbolicType outputType) {
		return typeFactory.functionType(inputTypes, outputType);
	}

	@Override
	public SymbolicFunctionType functionType(Iterable<? extends SymbolicType> inputTypes, SymbolicType outputType) {
		return typeFactory.functionType(typeSequence(inputTypes), outputType);
	}

	@Override
	public SymbolicFunctionType functionType(Iterable<? extends SymbolicType> inputTypes, SymbolicType outputType,
			SpecialRelationKind relKind) {
		return typeFactory.functionType(typeSequence(inputTypes), outputType, relKind);
	}

	public SymbolicUnionType unionType(StringObject name, SymbolicTypeSequence memberTypes) {
		return typeFactory.unionType(name, memberTypes);
	}

	@Override
	public SymbolicUnionType unionType(StringObject name, Iterable<? extends SymbolicType> memberTypes) {
		return typeFactory.unionType(name, typeSequence(memberTypes));
	}

	@Override
	public int numObjects() {
		return objectFactory.numObjects();
	}

	@Override
	public SymbolicObject objectWithId(int index) {
		return objectFactory.objectWithId(index);
	}

	@Override
	public BooleanObject booleanObject(boolean value) {
		return objectFactory.booleanObject(value);
	}

	@Override
	public CharObject charObject(char value) {
		return objectFactory.charObject(value);
	}

	@Override
	public IntObject intObject(int value) {
		return objectFactory.intObject(value);
	}

	@Override
	public NumberObject numberObject(Number value) {
		return objectFactory.numberObject(value);
	}

	@Override
	public StringObject stringObject(String string) {
		return objectFactory.stringObject(string);
	}

	@Override
	public SymbolicConstant symbolicConstant(StringObject name, SymbolicType type) {
		if (type.isNumeric())
			return numericFactory.symbolicConstant(name, type);
		if (type.isBoolean())
			return booleanFactory.booleanSymbolicConstant(name);
		if (type.typeKind() == SymbolicTypeKind.FUNCTION)
			if (ReservedFunctions.isNameReserved(name.getString())) {
				throw new SARLException("Function name " + name + " is reserved. Consider a different name.");
			}
		return expressionFactory.symbolicConstant(name, type);
	}

	@Override
	public SymbolicExpression nullExpression() {
		return nullExpression;
	}

	@Override
	public NumericExpression number(NumberObject numberObject) {
		return numericFactory.number(numberObject);
	}

	@Override
	public NumericExpression integer(int value) {
		return number(numberObject(numberFactory.integer(value)));
	}

	@Override
	public NumericExpression rational(double value) {
		return number(numberObject(numberFactory.rational(Double.toString(value))));
	}

	@Override
	public NumericExpression rational(int numerator, int denominator) {
		return number(numberObject(numberFactory.divide(numberFactory.rational(numberFactory.integer(numerator)),
				numberFactory.rational(numberFactory.integer(denominator)))));
	}

	@Override
	public NumericExpression zeroInt() {
		return numericFactory.zeroInt();
	}

	@Override
	public NumericExpression zeroReal() {
		return numericFactory.zeroReal();
	}

	@Override
	public NumericExpression oneInt() {
		return numericFactory.oneInt();
	}

	@Override
	public NumericExpression oneReal() {
		return numericFactory.oneReal();
	}

	@Override
	public SymbolicExpression character(char theChar) {
		CharObject charObject = (CharObject) charObject(theChar);

		return expression(SymbolicOperator.CONCRETE, typeFactory.characterType(), charObject);
	}

	@Override
	public Character extractCharacter(SymbolicExpression expression) {
		if (expression.type().typeKind() == SymbolicTypeKind.CHAR && expression.operator() == SymbolicOperator.CONCRETE)
			return ((CharObject) expression.argument(0)).getChar();
		return null;
	}

	@Override
	public SymbolicExpression stringExpression(String theString) {
		List<SymbolicExpression> charExprList = new LinkedList<SymbolicExpression>();
		int numChars = theString.length();

		for (int i = 0; i < numChars; i++)
			charExprList.add(character(theString.charAt(i)));
		return array(typeFactory.characterType(), charExprList);
	}

	private void checkSameType(SymbolicExpression arg0, SymbolicExpression arg1, String message) {
		if (!arg0.type().equals(arg1.type()))
			throw err(message + ".\narg0: " + arg0 + "\narg0 type: " + arg0.type() + "\narg1: " + arg1 + "\narg1 type: "
					+ arg1.type());
	}

	@Override
	public NumericExpression add(NumericExpression arg0, NumericExpression arg1) {
		checkSameType(arg0, arg1, "Arguments to add had different types");
		return numericFactory.add(arg0, arg1);
	}

	@Override
	public NumericExpression subtract(NumericExpression arg0, NumericExpression arg1) {
		checkSameType(arg0, arg1, "Arguments to subtract had different types");
		return numericFactory.subtract(arg0, arg1);
	}

	@Override
	public NumericExpression multiply(NumericExpression arg0, NumericExpression arg1) {
		checkSameType(arg0, arg1, "Arguments to multiply had different types");
		return numericFactory.multiply(arg0, arg1);
	}

	/**
	 * Multiplies a sequence of elements by applying binary multiplication (@link
	 * {@link #multiply(NumericExpression, NumericExpression)}) from left to right.
	 * 
	 * @param type the type of the arguments and the result
	 * @param args array whose elements are all non-<code>null</code> instances of
	 *             {@link NumericExpression}
	 * @return the product of the elements of <code>args</code>
	 */
	private NumericExpression multiply(SymbolicType type, SymbolicObject[] args) {
		int n = args.length;

		if (n == 0)
			return type.isInteger() ? numericFactory.oneInt() : numericFactory.oneReal();
		else {
			NumericExpression result = (NumericExpression) args[0];

			for (int i = 1; i < n; i++) {
				NumericExpression next = (NumericExpression) args[i];

				result = multiply(result, next);
			}
			return result;
		}
	}

	@Override
	public NumericExpression multiply(Iterable<? extends NumericExpression> args) {
		Iterator<? extends NumericExpression> iter = args.iterator();

		if (!iter.hasNext())
			throw err("Iterable argument to multiply was empty but should have" + " at least one element");
		else {
			NumericExpression result = iter.next();

			while (iter.hasNext())
				result = multiply(result, iter.next());
			return result;
		}
	}

	@Override
	public NumericExpression divide(NumericExpression arg0, NumericExpression arg1) throws ArithmeticException {
		checkSameType(arg0, arg1, "Arguments to divide had different types");
		return numericFactory.divide(arg0, arg1);
	}

	@Override
	public NumericExpression modulo(NumericExpression arg0, NumericExpression arg1) throws ArithmeticException {
		if (!arg0.type().isInteger())
			throw err("Argument arg0 to modulo did not have integer type.\n" + "\narg0: " + arg0 + "\narg0 type: "
					+ arg0.type());
		if (!arg1.type().isInteger())
			throw err("Argument arg1 to modulo did not have integer type.\n" + "\narg0: " + arg1 + "\narg0 type: "
					+ arg1.type());
		return numericFactory.modulo(arg0, arg1);
	}

	@Override
	public NumericExpression minus(NumericExpression arg) {
		return numericFactory.minus(arg);
	}

	@Override
	public NumericExpression power(NumericExpression base, IntegerNumber exponent) {
		if (exponent.signum() < 0)
			throw err("Argument exponent to method power was negative." + "\nexponent: " + exponent);
		return numericFactory.power(base, numberObject(exponent));
	}

	@Override
	public NumericExpression power(NumericExpression base, int exponent) {
		return power(base, numberFactory.integer(exponent));
	}

	@Override
	public NumericExpression power(NumericExpression base, NumericExpression exponent) {
		return numericFactory.power(base, exponent);
	}

	@Override
	public Number extractNumber(NumericExpression expression) {
		if (expression.operator() == SymbolicOperator.CONCRETE) {
			SymbolicObject object = expression.argument(0);

			if (object.symbolicObjectKind() == SymbolicObjectKind.NUMBER)
				return ((NumberObject) object).getNumber();
		}
		return null;
	}

	@Override
	public BooleanExpression bool(BooleanObject object) {
		return booleanFactory.symbolic(object);
	}

	@Override
	public BooleanExpression bool(boolean value) {
		return booleanFactory.symbolic(value);
	}

	/**
	 * Assume both args are in CNF normal form:
	 * 
	 * arg: true | false | AND set1 | OR set2 | e
	 * 
	 * Strategy: get rid of true false cases as usual. Then:
	 * 
	 * <pre>
	 * or(AND set, X) = and(s in set) or(s,X)
	 * or(X, AND set) = and(s in set) or(X,s)
	 * or(OR set0, OR set1) = OR(union(set0, set1))
	 * or(OR set, e) = OR(add(set, e))
	 * or(e, OR set) = OR(add(set, e))
	 * or(e1, e2) = OR(set(e1,e2))
	 * </pre>
	 * 
	 * where X is an AND, OR or e expression; set0 and set1 are sets of e
	 * expressions.
	 */
	@Override
	public BooleanExpression or(BooleanExpression arg0, BooleanExpression arg1) {
		return booleanFactory.or(arg0, arg1);
	}

	/**
	 * Assume nothing about the list of args.
	 */
	@Override
	public BooleanExpression or(Iterable<? extends BooleanExpression> args) {
		return booleanFactory.or(args);
	}

	/**
	 * <pre>
	 * expr       : AND set<or> | or
	 * or         : OR set<basic> | basic
	 * basic      : literal | quantifier | relational
	 * literal    : booleanPrimitive | ! booleanPrimitive
	 * quantifier : q[symbolicConstant].expr
	 * q          : forall | exists
	 * relational : 0<e | 0=e | 0<=e | 0!=e
	 * </pre>
	 * 
	 * Note: a booleanPrimitive is any boolean expression that doesn't fall into one
	 * of the other categories above.
	 * 
	 * <pre>
	 * not(AND set) => or(s in set) not(s)
	 * not(or set) => and(s in set) not(s)
	 * not(!e) => e
	 * not(forall x.e) => exists x.not(e)
	 * not(exists x.e) => forall x.not(e)
	 * not(0<e) => 0<=-e
	 * not(0=e) => 0!=e
	 * not(0!=e) => 0=e
	 * not(0<=e) => 0<-e
	 * not(booleanPrimitive) = !booleanPrimitive
	 * </pre>
	 */
	@Override
	public BooleanExpression not(BooleanExpression arg) {
		// SymbolicOperator operator = arg.operator();
		//
		// switch (operator) {
		// case LESS_THAN:
		// return numericFactory.notLessThan(
		// (NumericExpression) arg.argument(0),
		// (NumericExpression) arg.argument(1));
		// case LESS_THAN_EQUALS:
		// return numericFactory.notLessThanEquals(
		// (NumericExpression) arg.argument(0),
		// (NumericExpression) arg.argument(1));
		// default:
		return booleanFactory.not(arg);
		// }
	}

	@Override
	public BooleanExpression implies(BooleanExpression arg0, BooleanExpression arg1) {
		return or(not(arg0), arg1);
	}

	@Override
	public BooleanExpression equiv(BooleanExpression arg0, BooleanExpression arg1) {
		BooleanExpression result = implies(arg0, arg1);

		if (result.isFalse())
			return result;
		return and(result, implies(arg1, arg0));
	}

	@Override
	public BooleanExpression forallInt(NumericSymbolicConstant index, NumericExpression low, NumericExpression high,
			BooleanExpression predicate) {
		IntegerNumber lowNumber = (IntegerNumber) extractNumber(low);

		if (lowNumber != null) {
			IntegerNumber highNumber = (IntegerNumber) extractNumber(high);

			if (highNumber != null && numberFactory.compare(numberFactory.subtract(highNumber, lowNumber),
					quantifierExpandBound) <= 0) {
				return forallIntConcrete(index, lowNumber, highNumber, predicate);
			}
		}
		return forall(index, implies(and(lessThanEquals(low, index), lessThan(index, high)), predicate));
	}

	@Override
	public BooleanExpression existsInt(NumericSymbolicConstant index, NumericExpression low, NumericExpression high,
			BooleanExpression predicate) {
		IntegerNumber lowNumber = (IntegerNumber) extractNumber(low);

		if (lowNumber != null) {
			IntegerNumber highNumber = (IntegerNumber) extractNumber(high);

			if (highNumber != null && numberFactory.compare(numberFactory.subtract(highNumber, lowNumber),
					quantifierExpandBound) <= 0) {
				return existsIntConcrete(index, lowNumber, highNumber, predicate);
			}
		}
		return exists(index, implies(and(lessThanEquals(low, index), lessThan(index, high)), predicate));
	}

	@Override
	public BooleanExpression lessThan(NumericExpression arg0, NumericExpression arg1) {

		return numericFactory.lessThan(arg0, arg1);
	}

	@Override
	public BooleanExpression lessThanEquals(NumericExpression arg0, NumericExpression arg1) {
		return numericFactory.lessThanEquals(arg0, arg1);
	}

	@Override
	public BooleanExpression equals(SymbolicExpression arg0, SymbolicExpression arg1) {
		if (arg0.isNumeric() && arg1.isNumeric())
			return numericFactory.equals((NumericExpression) arg0, (NumericExpression) arg1);
		return equals(arg0, arg1, 0);
	}

	@Override
	public BooleanExpression quickEquals(SymbolicExpression arg0, SymbolicExpression arg1) {
		return arg0.equals(arg1) ? trueExpr : falseExpr;
	}

	@Override
	public BooleanExpression neq(SymbolicExpression arg0, SymbolicExpression arg1) {
		if (arg0.isNumeric())
			return numericFactory.neq((NumericExpression) arg0, (NumericExpression) arg1);
		return not(equals(arg0, arg1));
	}

	@Override
	public BooleanExpression divides(NumericExpression a, NumericExpression b) {
		return equals(modulo(b, a), zeroInt());
	}

	private <T extends SymbolicExpression> SymbolicSequence<T> sequence(Iterable<T> elements) {
		if (elements instanceof SymbolicSequence<?>)
			return (SymbolicSequence<T>) elements;
		return objectFactory.sequence(elements);
	}

	@Override
	public BooleanExpression removeQuantifiers(BooleanExpression arg) {
		List<BooleanExpression> quantFreeClauses = new ArrayList<>(arg.getClauses().length);
		for (BooleanExpression clause : arg.getClauses()) {
			if (!clause.containsQuantifier()) {
				quantFreeClauses.add(clause);
			}
		}
		return and(quantFreeClauses);
	}

	/**
	 * We are assuming that each type has a nonempty domain.
	 * 
	 * <pre>
	 * forall x.true => true
	 * forall x.false => false
	 * forall x.(p && q) => (forall x.p) && (forall x.q)
	 * </pre>
	 */
	@Override
	public BooleanExpression forall(SymbolicConstant boundVariable, BooleanExpression predicate) {
		if (predicate == trueExpr)
			return trueExpr;
		if (predicate == falseExpr)
			return falseExpr;

		BooleanExpression result;

		if (predicate.operator() == SymbolicOperator.AND) {
			result = trueExpr;
			for (SymbolicObject clause : predicate.getArguments())
				result = and(result, forall(boundVariable, (BooleanExpression) clause));
		} else {
			result = booleanFactory.forall(boundVariable, predicate);

			ForallStructure structure = getForallStructure(result);

			if (structure != null) {
				IntegerNumber lo = (IntegerNumber) extractNumber(structure.lowerBound);

				if (lo != null) {
					IntegerNumber hi = (IntegerNumber) extractNumber(structure.upperBound);

					if (hi != null)
						result = forallIntConcrete(structure.boundVariable, lo, numberFactory.increment(hi),
								structure.body);
				}
			}
		}
		return result;
	}

	@Override
	public BooleanExpression exists(SymbolicConstant boundVariable, BooleanExpression predicate) {
		return booleanFactory.exists(boundVariable, predicate);
	}

	@Override
	public Boolean extractBoolean(BooleanExpression expression) {
		if (expression == trueExpr)
			return true;
		if (expression == falseExpr)
			return false;
		return null;
	}

	@Override
	public SymbolicExpression lambda(SymbolicConstant boundVariable, SymbolicExpression expression) {
		return expression(SymbolicOperator.LAMBDA,
				functionType(typeFactory.singletonSequence(boundVariable.type()), expression.type()), boundVariable,
				expression);
	}

	@Override
	public SymbolicExpression apply(SymbolicExpression function,
			Iterable<? extends SymbolicExpression> argumentSequence) {
		SymbolicOperator op0 = function.operator();
		SymbolicExpression result;

		if (op0 == SymbolicOperator.LAMBDA) {
			Iterator<? extends SymbolicExpression> iter = argumentSequence.iterator();
			SymbolicExpression arg;

			if (!iter.hasNext())
				throw err("Argument argumentSequence to method apply is empty"
						+ " but since function is a lambda expression it should" + " have at least one element");
			arg = iter.next();
			assert !iter.hasNext();
			if (iter.hasNext())
				throw err("Argument argumentSequence to method apply has more than one element"
						+ " but since function is a lambda expression it should" + " have exactly one element");
			// function.argument(0): bound symbolic constant : dummy variable
			// function.argument(1): symbolic expression: body of function
			result = simpleSubstituter((SymbolicConstant) function.argument(0), arg)
					.apply((SymbolicExpression) function.argument(1));
		} else {
			// TODO check the argument types...
			result = expression(SymbolicOperator.APPLY, ((SymbolicFunctionType) function.type()).outputType(), function,
					sequence(argumentSequence));
		}
		return result;
	}

	@Override
	public SymbolicExpression unionInject(SymbolicUnionType unionType, IntObject memberIndex,
			SymbolicExpression object) {
		SymbolicType objectType = object.type();
		int indexInt = memberIndex.getInt();
		int numMembers = unionType.sequence().numTypes();
		SymbolicType memberType;

		if (indexInt < 0 || indexInt >= numMembers)
			throw err("Argument memberIndex to unionInject is out of range.\n" + "unionType: " + unionType + "\nSaw: "
					+ indexInt + "\nExpected: integer in range [0," + (numMembers - 1) + "]");
		memberType = unionType.sequence().getType(indexInt);
		if (incompatible(memberType, objectType))
			throw err("Argument object of unionInject has the wrong type.\n"
					+ "Its type should agree with the type of member " + memberIndex + " of the union type.\n"
					+ "Expected: " + memberType + "\n.Saw: " + objectType + ": " + object);
		// inject_i(extract_i(x))=x...
		if (object.operator() == SymbolicOperator.UNION_EXTRACT
				&& unionType.equals(((SymbolicExpression) object.argument(1)).type())
				&& memberIndex.equals(object.argument(0)))
			return (SymbolicExpression) object.argument(1);
		return expression(SymbolicOperator.UNION_INJECT, unionType, memberIndex, object);
	}

	@Override
	public BooleanExpression unionTest(IntObject memberIndex, SymbolicExpression object) {
		if (object.operator() == SymbolicOperator.UNION_INJECT)
			return object.argument(0).equals(memberIndex) ? trueExpr : falseExpr;
		return booleanFactory.booleanExpression(SymbolicOperator.UNION_TEST, memberIndex, object);
	}

	@Override
	public SymbolicExpression unionExtract(IntObject memberIndex, SymbolicExpression object) {
		if (object.operator() == SymbolicOperator.UNION_INJECT && memberIndex.equals(object.argument(0)))
			return (SymbolicExpression) object.argument(1);
		return expression(SymbolicOperator.UNION_EXTRACT,
				((SymbolicUnionType) object.type()).sequence().getType(memberIndex.getInt()), memberIndex, object);
	}

	@Override
	public SymbolicExpression array(SymbolicType elementType, SymbolicExpression elements[]) {
		return expression(ARRAY, arrayType(elementType, integer(elements.length)), elements);
	}

	@Override
	public SymbolicExpression array(SymbolicType elementType, Iterable<? extends SymbolicObject> elements) {
		int count = 0;

		if (elementType == null)
			throw err("Argument elementType to method array was null");
		if (elements == null)
			throw err("Argument elements to method array was null");
		for (SymbolicObject object : elements) {
			if (!(object instanceof SymbolicExpression))
				throw err("Element " + count + " of elements: expected a SymbolicExpression, saw: " + object);

			SymbolicExpression element = (SymbolicExpression) object;

			if (element.isNull())
				throw err("Element " + count + " of array elements argument has illegal value:\n" + element);
			if (incompatible(elementType, element.type()))
				throw err("Element " + count + " of array elements argument had incompatible type:\n" + "Expected: "
						+ elementType + "\nSaw: " + element.type());
			count++;
		}

		SymbolicExpression[] elementArray = new SymbolicExpression[count];

		count = 0;
		for (SymbolicObject element : elements) {
			elementArray[count] = (SymbolicExpression) element;
			count++;
		}
		return array(elementType, elementArray);
	}

	@Override
	public SymbolicExpression append(SymbolicExpression concreteArray, SymbolicExpression element) {
		SymbolicType type = concreteArray.type();

		if (type.typeKind() != SymbolicTypeKind.ARRAY)
			throw err("argument concreteArray not array type:\n" + concreteArray);
		if (concreteArray.operator() != ARRAY) {
			throw err("append invoked on non-concrete array:\n" + concreteArray);
		} else {
			HomogeneousExpression<?> hArray = (HomogeneousExpression<?>) concreteArray;
			SymbolicExpression[] elements = (SymbolicExpression[]) hArray.arguments();
			SymbolicType elementType = ((SymbolicArrayType) type).elementType();

			if (element == null || element.isNull())
				throw err("Element to append has illegal value:\n" + element);
			if (incompatible(elementType, element.type()))
				throw err("Element to append has incompatible type:\n" + "Expected: " + elementType + "\nSaw: "
						+ element.type());
			elements = exprSeqFactory.add(elements, element);
			return array(elementType, elements);
		}
	}

	@Override
	public SymbolicExpression removeElementAt(SymbolicExpression concreteArray, int index) {
		SymbolicType type = concreteArray.type();

		if (type.typeKind() != SymbolicTypeKind.ARRAY)
			throw err("argument concreteArray not array type:\n" + concreteArray);
		if (concreteArray.operator() != ARRAY) {
			throw err("argument concreteArray is not concrete:\n" + concreteArray);
		} else {
			SymbolicType elementType = ((SymbolicArrayType) type).elementType();
			HomogeneousExpression<?> hArray = (HomogeneousExpression<?>) concreteArray;
			SymbolicExpression[] elements = (SymbolicExpression[]) hArray.arguments();
			int length = elements.length;

			if (index < 0 || index >= length)
				return outOfBoundExpr(concreteArray, integer(index), zeroInt(), integer(length),
						"a remove element operation");
			elements = exprSeqFactory.remove(elements, index);
			return array(elementType, elements);
		}
	}

	@Override
	public SymbolicExpression emptyArray(SymbolicType elementType) {
		return array(elementType, emptyExprArray);
	}

	@Override
	public SymbolicExpression constantArray(SymbolicType elementType, NumericExpression length,
			SymbolicExpression value) {
		SymbolicCompleteArrayType arrayType = arrayType(elementType, length);
		IntegerNumber lengthNumber = (IntegerNumber) extractNumber(length);
		SymbolicExpression result;

		if (lengthNumber == null) {
			result = arrayLambda(arrayType, lambda(arrayIndex, value));
		} else {
			int lengthInt = lengthNumber.intValue();
			SymbolicExpression[] elements = new SymbolicExpression[lengthInt];

			Arrays.fill(elements, value);
			result = array(elementType, elements);
		}
		return result;
	}

	@Override
	public NumericExpression length(SymbolicExpression array) {
		if (array == null)
			throw err("Argument array to method length was null");
		if (!(array.type() instanceof SymbolicArrayType))
			throw err("Argument array to method length does not have array type." + "\narray: " + array + "\ntype: "
					+ array.type());
		else {
			SymbolicArrayType type = (SymbolicArrayType) array.type();

			if (type.isComplete())
				return (NumericExpression) ((SymbolicCompleteArrayType) type).extent();
			else
				return numericFactory.expression(SymbolicOperator.LENGTH, integerType, array);
		}
	}

	@Override
	public SymbolicExpression arrayRead(SymbolicExpression array, NumericExpression index) {
		if (array == null)
			throw err("Argument array to method arrayRead is null.");
		if (index == null)
			throw err("Argument index to method arrayRead is null.");
		if (!(array.type() instanceof SymbolicArrayType))
			throw err("Argument array to method arrayRead does not have array type." + "\narray: " + array + "\ntype: "
					+ array.type());

		SymbolicOperator op = array.operator();

		if (op == SymbolicOperator.ARRAY_LAMBDA)
			return apply((SymbolicExpression) array.argument(0), Arrays.asList(index));

		SymbolicArrayType arrayType = (SymbolicArrayType) array.type();
		IntegerNumber indexNumber = (IntegerNumber) extractNumber(index);

		if (indexNumber != null) {
			if (indexNumber.signum() < 0)
				return outOfBoundExpr(array, index, zeroInt(), null, "an array read operation");
			if (arrayType.isComplete()) {
				IntegerNumber lengthNumber = (IntegerNumber) extractNumber(
						((SymbolicCompleteArrayType) arrayType).extent());

				if (lengthNumber != null && numberFactory.compare(indexNumber, lengthNumber) >= 0)
					return outOfBoundExpr(array, index, zeroInt(), number(lengthNumber), "an array read operation");
			}
			if (op == ARRAY)
				return (SymbolicExpression) array.argument(indexNumber.intValue());
			else if (op == SymbolicOperator.DENSE_ARRAY_WRITE) {
				SymbolicExpression origin = (SymbolicExpression) array.argument(0);

				if (numberFactory.compare(indexNumber, denseArrayMaxSize) < 0) {
					int indexInt = indexNumber.intValue();
					SymbolicSequence<?> values = (SymbolicSequence<?>) array.argument(1);
					int size = values.size();

					if (indexInt < size) {
						SymbolicExpression value = values.get(indexInt);

						if (!value.isNull())
							return value;
					}
				}
				// either indexNumber too big or entry is null
				return arrayRead(origin, index);
			}
		} // end if (indexNumber != null)

		SymbolicExpression result = null;

		if (op == SymbolicOperator.ARRAY_WRITE) {
			NumericExpression writtenIndex = (NumericExpression) array.argument(1);
			SymbolicExpression value = (SymbolicExpression) array.argument(2);

			if (writtenIndex.equals(index))
				return value;
			if (SARLConstants.arrayReadCondSimplify) {
				result = cond(equals(index, writtenIndex), value,
						arrayRead((SymbolicExpression) array.argument(0), index));
			} else {
				result = expression(SymbolicOperator.ARRAY_READ, ((SymbolicArrayType) array.type()).elementType(),
						array, index);
			}
		} else if (op == SymbolicOperator.COND) {
			BooleanExpression cond = (BooleanExpression) array.argument(0);
			SymbolicExpression truBrch = (SymbolicExpression) array.argument(1);
			SymbolicExpression flsBrch = (SymbolicExpression) array.argument(2);

			truBrch = arrayRead(truBrch, index);
			flsBrch = arrayRead(flsBrch, index);
			return cond(cond, truBrch, flsBrch);
		} else {
			result = expression(SymbolicOperator.ARRAY_READ, ((SymbolicArrayType) array.type()).elementType(), array,
					index);
		}
		return result;
	}

	private SymbolicExpression arrayWrite_noCheck(SymbolicExpression array, SymbolicArrayType arrayType,
			NumericExpression index, SymbolicExpression value) {
		IntegerNumber indexNumber = (IntegerNumber) extractNumber(index);
		IntegerNumber lengthNumber = null; // length of array type if complete

		if (indexNumber != null) {
			int indexInt = indexNumber.intValue();
			SymbolicOperator op = array.operator();

			if (indexNumber.signum() < 0)
				return outOfBoundExpr(array, index, zeroInt(), null, "an array write operation");
			if (arrayType.isComplete()) {
				lengthNumber = (IntegerNumber) extractNumber(((SymbolicCompleteArrayType) arrayType).extent());
				if (lengthNumber != null && numberFactory.compare(indexNumber, lengthNumber) >= 0)
					return outOfBoundExpr(array, index, zeroInt(), number(lengthNumber), "an array write operation");
			}
			if (op == ARRAY) {
				HomogeneousExpression<?> hArray = (HomogeneousExpression<?>) array;
				SymbolicExpression[] sequence = (SymbolicExpression[]) hArray.arguments();
				SymbolicExpression[] newSequence = exprSeqFactory.set(sequence, indexInt, value);

				return expression(op, arrayType, newSequence);
			}
			if (indexInt < DENSE_ARRAY_MAX_SIZE) {
				SymbolicSequence<SymbolicExpression> sequence;
				SymbolicExpression origin;

				if (op == SymbolicOperator.DENSE_ARRAY_WRITE) {
					@SuppressWarnings("unchecked")
					SymbolicSequence<SymbolicExpression> arg1 = (SymbolicSequence<SymbolicExpression>) array
							.argument(1);

					sequence = arg1;
					origin = (SymbolicExpression) array.argument(0);
				} else {
					origin = array;
					sequence = objectFactory.emptySequence();
				}
				sequence = sequence.setExtend(indexInt, value, nullExpression);
				// if the length of the sequence is the extent of the array type
				// AND the sequence has no null values, you can forget the
				// origin and make a concrete array value, since every cell
				// has been over-written...
				if (lengthNumber != null && sequence.getNumNull() == 0 && lengthNumber.intValue() == sequence.size()) {
					int n = sequence.size();
					SymbolicExpression[] newArray = new SymbolicExpression[n];

					for (int i = 0; i < n; i++)
						newArray[i] = sequence.get(i);
					return expression(ARRAY, arrayType, newArray);
				}
				return expression(SymbolicOperator.DENSE_ARRAY_WRITE, arrayType, origin, sequence);
			}
		}
		return expression(SymbolicOperator.ARRAY_WRITE, arrayType, array, index, value);
	}

	@Override
	public SymbolicExpression arrayWrite(SymbolicExpression array, NumericExpression index, SymbolicExpression value) {
		if (array == null)
			throw err("Argument array to method arrayWrite is null.");
		if (index == null)
			throw err("Argument index to method arrayWrite is null.");
		if (value == null)
			throw err("Argument value to method arrayWrite is null.");
		if (!(array.type() instanceof SymbolicArrayType))
			throw err("Argument array to method arrayWrite does not have array type." + "\narray: " + array + "\ntype: "
					+ array.type());
		if (!index.type().isInteger())
			throw err("Argument index to method arrayWrite does not have integer type." + "\nindex: " + index
					+ "\ntype: " + index.type());
		if (value.isNull())
			throw err("Argument value to method arrayWrite is NULL.");
		else {
			SymbolicArrayType arrayType = (SymbolicArrayType) array.type();

			if (incompatible(arrayType.elementType(), value.type()))
				throw err("Argument value to method arrayWrite has incompatible type." + "\nvalue: " + value
						+ "\ntype: " + value.type() + "\nExpected: " + arrayType.elementType());
			// If array has ARRAY_WRITE operator and the written index is the
			// same as the one going to be written, it's safe to over-write it:
			if (array.operator() == SymbolicOperator.ARRAY_WRITE) {
				NumericExpression prevIdx = (NumericExpression) array.argument(1);

				if (prevIdx.equals(index))
					return arrayWrite_noCheck((SymbolicExpression) array.argument(0), arrayType, index, value);
			}
			return arrayWrite_noCheck(array, arrayType, index, value);
		}
	}

	/**
	 * Returns an iterable object equivalent to given one except that any "null"
	 * values are replaced by the SymbolicExpression NULL. Also, trailing
	 * nulls/NULLs are removed.
	 * 
	 * @param values any iterable of symbolic expressions, which may contain null
	 *               values
	 * @return an iterable object with nulls replaced with NULLs
	 */
	private <T extends SymbolicExpression> Iterable<? extends SymbolicExpression> replaceNulls(Iterable<T> values) {
		int count = 0;
		int lastNonNullIndex = -1;

		for (T value : values) {
			if (value == null) { // element in position count is null
				LinkedList<SymbolicExpression> list = new LinkedList<SymbolicExpression>();
				Iterator<T> iter = values.iterator();

				for (int i = 0; i < count; i++)
					list.add(iter.next());
				list.add(nullExpression);
				iter.next();
				count++;
				while (iter.hasNext()) {
					T element = iter.next();

					list.add(element == null ? nullExpression : element);
					if (element != null && !element.isNull())
						lastNonNullIndex = count;
					count++;
				}
				// count is size of list, lastNonNullIndex is index of
				// last non-null element
				if (lastNonNullIndex < count - 1) {
					// remove elements lastNonNullIndex+1,...,count-1
					list.subList(lastNonNullIndex + 1, count).clear();
				}
				return list;
			}
			if (!value.isNull())
				lastNonNullIndex = count;
			count++;
		}
		if (lastNonNullIndex < count - 1) {
			LinkedList<SymbolicExpression> list = new LinkedList<SymbolicExpression>();
			Iterator<T> iter = values.iterator();

			for (int i = 0; i <= lastNonNullIndex; i++)
				list.add(iter.next());
			return list;
		}
		return values;
	}

	@Override
	public SymbolicExpression denseArrayWrite(SymbolicExpression array, Iterable<? extends SymbolicExpression> values) {
		SymbolicType theArraysType = array.type();

		if (!(theArraysType instanceof SymbolicArrayType))
			throw new SARLException("Argument 0 of denseArrayWrite must have array type but had type " + theArraysType);
		else {
			SymbolicArrayType arrayType = (SymbolicArrayType) theArraysType;
			SymbolicType elementType = arrayType.elementType();
			int count = 0;
			int numNulls = 0;

			values = replaceNulls(values);
			for (SymbolicExpression value : values) {
				if (value.isNull())
					numNulls++;
				else if (incompatible(elementType, value.type()))
					throw err("Element " + count + " of values argument to denseArrayWrite has incompatible type.\n"
							+ "Expected: " + elementType + "\nSaw: " + value.type());
				count++;
			}
			if (numNulls == 0 && arrayType.isComplete()) {
				IntegerNumber lengthNumber = (IntegerNumber) extractNumber(
						((SymbolicCompleteArrayType) arrayType).extent());

				if (lengthNumber != null && count == lengthNumber.intValue()) {
					SymbolicExpression[] elements = new SymbolicExpression[count];

					count = 0;
					for (SymbolicExpression value : values) {
						elements[count] = value;
						count++;
					}
					return array(elementType, elements);
				}
			}
			return expression(SymbolicOperator.DENSE_ARRAY_WRITE, arrayType, array, sequence(values));
		}
	}

	public SymbolicExpression denseTupleWrite(SymbolicExpression tuple, Iterable<? extends SymbolicExpression> values) {
		int count = 0;

		for (SymbolicExpression value : values) {
			if (value != null && !value.isNull()) {
				tuple = tupleWrite(tuple, intObject(count), value);
			}
			count++;
		}
		return tuple;
	}

	@Override
	public SymbolicExpression arrayLambda(SymbolicCompleteArrayType arrayType, SymbolicExpression function) {
		if (arrayType == null)
			throw err("Argument arrayType to method arrayLambda was null");
		if (function == null)
			throw err("Argument function to method arrayLambda was null");
		if (function.operator() != SymbolicOperator.LAMBDA)
			throw err("Function must be LAMBDA type");

		if (function.type().typeKind() != SymbolicTypeKind.FUNCTION)
			throw err("function must have a function type, not " + function.type());

		SymbolicFunctionType functionType = (SymbolicFunctionType) function.type();
		SymbolicTypeSequence inputSeq = functionType.inputTypes();
		int numInputs = inputSeq.numTypes();

		if (numInputs != 1)
			throw err("function in array lambda must take one input, not " + numInputs + ": " + functionType);

		SymbolicType inputType = inputSeq.getType(0);

		if (inputType.typeKind() != SymbolicTypeKind.INTEGER)
			throw err("input type of array lambda function must be integer, not " + inputType + ": " + functionType);

		SymbolicType outputType = functionType.outputType();

		if (compatible(outputType, arrayType.elementType()).isFalse()) {
			throw err("Return type of array lambda function is incompatible with element type:\n" + "element type: "
					+ arrayType.elementType() + "\n" + "lambda function type: " + functionType + "\n"
					+ "lambda function output type: " + outputType + "\n");
		}

		NumericExpression lengthExpression = arrayType.extent();

		// if elementExpr has form lambda (i) e[i], and the type of e
		// equals arrayType, then return e.
		if (function.operator() == SymbolicOperator.LAMBDA) {
			SymbolicExpression body = (SymbolicExpression) function.argument(1);

			if (body.operator() == SymbolicOperator.ARRAY_READ) {
				SymbolicConstant boundVar = (SymbolicConstant) function.argument(0);

				if (body.argument(1) == boundVar) {
					SymbolicExpression e = (SymbolicExpression) body.argument(0);

					if (e.type() == arrayType)
						return e;
				}
			}
		}

		Number lengthNumber = this.extractNumber(lengthExpression);

		if (lengthNumber != null) {
			int length = ((IntegerNumber) lengthNumber).intValue();

			if (length < DENSE_ARRAY_MAX_SIZE) {
				SymbolicExpression[] elements = new SymbolicExpression[length];
				// TODO: this is assuming function is a lambda expression.
				// Fix me. If it's not a lambda expression, you can just call
				// method apply.
				SymbolicConstant boundVar = (SymbolicConstant) function.argument(0);
				SymbolicExpression elementExpr = (SymbolicExpression) function.argument(1);

				for (int i = 0; i < length; i++) {
					elements[i] = simpleSubstituter(boundVar, integer(i)).apply(elementExpr);
				}
				return array(arrayType.elementType(), elements);
			}
		}
		return expression(SymbolicOperator.ARRAY_LAMBDA, arrayType, function);
	}

	@Override
	public SymbolicExpression tuple(SymbolicTupleType type, SymbolicExpression[] components) {
		return expression(SymbolicOperator.TUPLE, type, components);
	}

	@Override
	public SymbolicExpression tuple(SymbolicTupleType type, Iterable<? extends SymbolicObject> components) {
		SymbolicTypeSequence fieldTypes = type.sequence();
		int m = fieldTypes.numTypes();
		SymbolicExpression[] componentArray = new SymbolicExpression[m];
		int i = 0;

		for (SymbolicObject arg : components) {
			if (i >= m)
				throw err("In method tuple, number of components exceeded " + m + ", the number expected by tuple type "
						+ type);

			if (!(arg instanceof SymbolicExpression))
				throw err("Component " + i + " in method tuple: " + "expected a SymbolicExpression, saw: " + arg);

			SymbolicExpression component = (SymbolicExpression) arg;
			SymbolicType fieldType = fieldTypes.getType(i);
			SymbolicType componentType = component.type();

			if (incompatible(fieldType, componentType))
				throw err("Element " + i + " of components argument to method tuple has incompatible type.\n"
						+ "\nExpected: " + fieldType + "\nSaw: " + componentType);
			componentArray[i] = component;
			i++;
		}
		if (i != m)
			throw err("In method tuple, tuple type has exactly" + m + " components but sequence has length " + i);
		return tuple(type, componentArray);
	}

	@Override
	public SymbolicExpression tupleRead(SymbolicExpression tuple, IntObject index) {
		SymbolicType type = tuple.type();
		SymbolicOperator op = tuple.operator();
		int indexInt = index.getInt();

		if (type == null || type.typeKind() != SymbolicTypeKind.TUPLE)
			throw new SARLException("Argument tuple to tupleRead does not have tuple type:\n" + tuple);
		if (op == SymbolicOperator.TUPLE)
			return (SymbolicExpression) tuple.argument(indexInt);
		if (op == SymbolicOperator.DENSE_TUPLE_WRITE) {
			SymbolicExpression value = ((SymbolicSequence<?>) tuple.argument(1)).get(indexInt);

			if (!value.isNull())
				return value;
			return tupleRead((SymbolicExpression) tuple.argument(0), index);

		}
		return expression(SymbolicOperator.TUPLE_READ, ((SymbolicTupleType) tuple.type()).sequence().getType(indexInt),
				tuple, index);
	}

	@Override
	public SymbolicExpression tupleWrite(SymbolicExpression tuple, IntObject index, SymbolicExpression value) {
		SymbolicOperator op = tuple.operator();
		int indexInt = index.getInt();
		SymbolicTupleType tupleType = (SymbolicTupleType) tuple.type();
		SymbolicType fieldType = tupleType.sequence().getType(indexInt);
		SymbolicType valueType = value.type();

		if (incompatible(fieldType, valueType))
			throw err("Argument value to tupleWrite has incompatible type." + "\nExpected: " + fieldType + "\nSaw: "
					+ valueType);
		if (op == SymbolicOperator.TUPLE) {
			SymbolicExpression oldValue = (SymbolicExpression) tuple.argument(indexInt);

			if (value == oldValue)
				return tuple;

			SymbolicExpression[] components = (SymbolicExpression[]) ((HomogeneousExpression<?>) tuple).arguments();
			SymbolicExpression[] newComponents = exprSeqFactory.set(components, indexInt, value);

			return tuple(tupleType, newComponents);
		} else if (op == SymbolicOperator.DENSE_TUPLE_WRITE) {
			@SuppressWarnings("unchecked")
			SymbolicSequence<SymbolicExpression> sequence = (SymbolicSequence<SymbolicExpression>) tuple.argument(1);
			SymbolicExpression oldValue = sequence.get(indexInt);

			if (value == oldValue)
				return tuple;
			sequence = sequence.set(indexInt, value);
			for (SymbolicExpression x : sequence) {
				if (x == null || x.isNull())
					return expression(SymbolicOperator.DENSE_TUPLE_WRITE, tupleType, tuple.argument(0), sequence);
			}

			int n = sequence.size();
			SymbolicExpression[] componentArray = new SymbolicExpression[n];

			for (int i = 0; i < n; i++)
				componentArray[i] = sequence.get(i);
			return tuple(tupleType, componentArray);
		} else {
			int numComponents = tupleType.sequence().numTypes();
			SymbolicExpression[] elementsArray = new SymbolicExpression[numComponents];
			SymbolicSequence<SymbolicExpression> sequence;

			for (int i = 0; i < numComponents; i++) {
				elementsArray[i] = nullExpression;
			}
			elementsArray[indexInt] = value;
			sequence = objectFactory.sequence(elementsArray);
			if (numComponents <= 1)
				return tuple(tupleType, elementsArray);
			else
				return expression(SymbolicOperator.DENSE_TUPLE_WRITE, tupleType, tuple, sequence);
		}
	}

	@Override
	public SymbolicExpression cast(SymbolicType newType, SymbolicExpression expression) {
		SymbolicType oldType = expression.type();

		if (oldType.equals(newType))
			return expression;
		if (oldType.isInteger() && newType.isChar()) {
			IntegerNumber intNum = (IntegerNumber) extractNumber((NumericExpression) expression);

			if (intNum != null) {
				int intVal = intNum.intValue();

				// Truncate the valid bits for casting from int to char
				// Limit the valid int range from 0 to (MAX_VALUE_UNSIGNED_CHAR
				// - 1)
				intVal %= MAX_VALUE_UNSIGNED_CHAR;
				intVal += MAX_VALUE_UNSIGNED_CHAR;
				intVal %= MAX_VALUE_UNSIGNED_CHAR;
				return character((char) (intVal));
			} else
				return expression(SymbolicOperator.CAST, newType, expression);
		} else if (oldType.isChar() && newType.isInteger()) {
			Character c = extractCharacter(expression);

			if (c != null) // Concrete
				return integer(c.charValue());
			else // Symbolic
				return expression(SymbolicOperator.CAST, newType, expression);
		}
		if (oldType.isNumeric() && newType.isNumeric()) {
			return numericFactory.cast((NumericExpression) expression, newType);
		}
		throw err("Cannot cast from type " + oldType + " to type " + newType + ": " + expression);
	}

	@Override
	public SymbolicExpression cond(BooleanExpression predicate, SymbolicExpression trueValue,
			SymbolicExpression falseValue) {
		if (predicate.isTrue())
			return trueValue;
		if (predicate.isFalse())
			return falseValue;
		assert !incompatible(trueValue.type(), falseValue.type());
		if (trueValue.equals(falseValue))
			return trueValue;
		return expression(SymbolicOperator.COND, trueValue.type(), predicate, trueValue, falseValue);
	}

	@Override
	public Comparator<SymbolicObject> comparator() {
		return objectComparator;
	}

	@Override
	public NumericExpression integer(long value) {
		return number(numberFactory.integer(value));
	}

	@Override
	public NumericExpression integer(BigInteger value) {
		return number(numberFactory.integer(value));
	}

	@Override
	public NumericExpression rational(int value) {
		return number(numberFactory.rational(numberFactory.integer(value)));
	}

	@Override
	public NumericExpression rational(long value) {
		return number(numberFactory.rational(numberFactory.integer(value)));
	}

	@Override
	public NumericExpression rational(BigInteger value) {
		return number(numberFactory.rational(numberFactory.integer(value)));
	}

	@Override
	public NumericExpression rational(float value) {
		return number(numberFactory.rational(Float.toString(value)));
	}

	@Override
	public NumericExpression rational(long numerator, long denominator) {
		return rational(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator));
	}

	@Override
	public NumericExpression rational(BigInteger numerator, BigInteger denominator) {
		return number(numberFactory.rational(numerator, denominator));
	}

	@Override
	public NumericExpression number(Number number) {
		return number(numberObject(number));
	}

	@Override
	public BooleanExpression trueExpression() {
		return trueExpr;
	}

	@Override
	public BooleanExpression falseExpression() {
		return falseExpr;
	}

	@Override
	public int numValidCalls() {
		return validCount;
	}

	@Override
	public int numProverValidCalls() {
		return proverValidCount;
	}

	@Override
	public void incrementValidCount() {
		validCount++;
	}

	@Override
	public void incrementProverValidCount() {
		proverValidCount++;
	}

	@Override
	public SymbolicType referenceType() {
		return expressionFactory.referenceType();
	}

	@Override
	public ReferenceExpression nullReference() {
		return expressionFactory.nullReference();
	}

	@Override
	public SymbolicExpression dereference(SymbolicExpression value, ReferenceExpression reference) {
		if (value == null)
			throw new SARLException("dereference given null value");
		if (reference == null)
			throw new SARLException("dereference given null reference");
		switch (reference.referenceKind()) {
		case NULL:
			throw new SARLException("Cannot dereference the null reference expression:\n" + value + "\n" + reference);
		case IDENTITY:
			return value;
		case ARRAY_ELEMENT: {
			ArrayElementReference ref = (ArrayElementReference) reference;

			return arrayRead(dereference(value, ref.getParent()), ref.getIndex());
		}
		case TUPLE_COMPONENT: {
			TupleComponentReference ref = (TupleComponentReference) reference;

			return tupleRead(dereference(value, ref.getParent()), ref.getIndex());
		}
		case UNION_MEMBER: {
			UnionMemberReference ref = (UnionMemberReference) reference;

			return this.unionExtract(ref.getIndex(), dereference(value, ref.getParent()));
		}
		case OFFSET: {
			OffsetReference ref = (OffsetReference) reference;
			NumericExpression index = ref.getOffset();
			IntegerNumber indexNumber = (IntegerNumber) extractNumber(index);

			if (indexNumber == null || !indexNumber.isZero())
				throw new SARLException(
						"Cannot dereference an offset reference with non-zero offset:\n" + reference + "\n" + value);
			return dereference(value, ref.getParent());
		}
		default:
			throw new SARLInternalException("Unknown reference kind: " + reference);
		}
	}

	@Override
	public SymbolicType referencedType(SymbolicType type, ReferenceExpression reference) {
		if (reference == null)
			throw new SARLException("referencedType given null reference");
		if (type == null)
			throw new SARLException("referencedType given null type");
		switch (reference.referenceKind()) {
		case NULL:
			throw new SARLException(
					"Cannot compute referencedType of the null reference expression:\n" + type + "\n" + reference);
		case IDENTITY:
			return type;
		case ARRAY_ELEMENT: {
			ArrayElementReference ref = (ArrayElementReference) reference;
			SymbolicType parentType = referencedType(type, ref.getParent());

			if (parentType instanceof SymbolicArrayType)
				return ((SymbolicArrayType) parentType).elementType();
			else
				throw new SARLException("Incompatible type and reference:\n" + type + "\n" + reference);
		}
		case TUPLE_COMPONENT: {
			TupleComponentReference ref = (TupleComponentReference) reference;
			SymbolicType parentType = referencedType(type, ref.getParent());

			if (parentType instanceof SymbolicTupleType)
				return ((SymbolicTupleType) parentType).sequence().getType(ref.getIndex().getInt());
			else
				throw new SARLException("Incompatible type and reference:\n" + type + "\n" + reference);
		}
		case UNION_MEMBER: {
			UnionMemberReference ref = (UnionMemberReference) reference;
			SymbolicType parentType = referencedType(type, ref.getParent());

			if (parentType instanceof SymbolicUnionType)
				return ((SymbolicUnionType) parentType).sequence().getType(ref.getIndex().getInt());
			else
				throw new SARLException("Incompatible type and reference:\n" + type + "\n" + reference);
		}
		case OFFSET: {
			OffsetReference ref = (OffsetReference) reference;
			SymbolicType parentType = referencedType(type, ref.getParent());

			return parentType;
		}
		default:
			throw new SARLInternalException("Unknown reference kind: " + reference);// unreachable
		}
	}

	@Override
	public ReferenceExpression identityReference() {
		return expressionFactory.identityReference();
	}

	@Override
	public ArrayElementReference arrayElementReference(ReferenceExpression arrayReference, NumericExpression index) {
		return expressionFactory.arrayElementReference(arrayReference, index);
	}

	@Override
	public TupleComponentReference tupleComponentReference(ReferenceExpression tupleReference, IntObject fieldIndex) {
		return expressionFactory.tupleComponentReference(tupleReference, fieldIndex);
	}

	@Override
	public UnionMemberReference unionMemberReference(ReferenceExpression unionReference, IntObject memberIndex) {
		return expressionFactory.unionMemberReference(unionReference, memberIndex);
	}

	@Override
	public OffsetReference offsetReference(ReferenceExpression reference, NumericExpression offset) {
		return expressionFactory.offsetReference(reference, offset);
	}

	@Override
	public SymbolicExpression assign(SymbolicExpression value, ReferenceExpression reference,
			SymbolicExpression subValue) {
		ReferenceKind kind;

		if (reference == null)
			throw new SARLException("assign given null reference");
		if (subValue == null)
			throw new SARLException("assign given null subValue");
		kind = reference.referenceKind();
		if (kind == ReferenceKind.IDENTITY)
			return subValue;
		if (value == null)
			throw new SARLException("assign given null value");
		switch (kind) {
		case NULL:
			throw new SARLException("Cannot assign using the null reference expression:\n" + value + "\n" + reference
					+ "\n" + subValue);
		case ARRAY_ELEMENT: {
			ArrayElementReference ref = (ArrayElementReference) reference;
			ReferenceExpression arrayReference = ref.getParent();
			SymbolicExpression array = dereference(value, arrayReference);
			SymbolicExpression newArray = arrayWrite(array, ref.getIndex(), subValue);

			return assign(value, arrayReference, newArray);
		}
		case TUPLE_COMPONENT: {
			TupleComponentReference ref = (TupleComponentReference) reference;
			ReferenceExpression tupleReference = ref.getParent();
			SymbolicExpression tuple = dereference(value, tupleReference);
			SymbolicExpression newTuple = tupleWrite(tuple, ref.getIndex(), subValue);

			return assign(value, tupleReference, newTuple);
		}
		case UNION_MEMBER: {
			UnionMemberReference ref = (UnionMemberReference) reference;
			ReferenceExpression unionReference = ref.getParent();
			SymbolicExpression unionValue = dereference(value, unionReference);
			SymbolicUnionType unionType = (SymbolicUnionType) unionValue.type();
			SymbolicExpression newUnionValue = unionInject(unionType, ref.getIndex(), subValue);

			return assign(value, unionReference, newUnionValue);
		}
		case OFFSET: {
			OffsetReference ref = (OffsetReference) reference;
			NumericExpression index = ref.getOffset();
			IntegerNumber indexNumber = (IntegerNumber) extractNumber(index);

			if (indexNumber == null || !indexNumber.isZero()) // first case
																// unreachable
				throw new SARLException(
						"Cannot assign via an offset reference with non-zero offset:\n" + reference + "\n" + value);
			return assign(value, ref.getParent(), subValue);
		}
		default: // unreachable
			throw new SARLInternalException("Unknown reference kind: " + reference);
		}
	}

	@Override
	public SymbolicExpression cleanBoundVariables(SymbolicExpression expr) {
		return cleaner.apply(expr);
	}

	@Override
	public UnaryOperator<SymbolicExpression> newMinimalBoundCleaner() {
		return new BoundCleaner2(this, objectFactory, typeFactory);
	}

	@Override
	public UnaryOperator<SymbolicExpression> cloneBoundCleaner(UnaryOperator<SymbolicExpression> boundCleaner) {
		return ((BoundCleaner2) boundCleaner).clone();
	}

	@Override
	public SymbolicSetType setType(SymbolicType elementType) {
		return typeFactory.setType(elementType);
	}

	@Override
	public SymbolicMapType mapType(SymbolicType keyType, SymbolicType valueType) {
		return typeFactory.mapType(keyType, valueType);
	}

	@Override
	public SymbolicTupleType entryType(SymbolicMapType mapType) {
		return typeFactory.entryType(mapType);
	}

	@Override
	public SymbolicExpression insertElementAt(SymbolicExpression concreteArray, int index, SymbolicExpression value) {
		SymbolicType type = concreteArray.type();

		if (type.typeKind() != SymbolicTypeKind.ARRAY)
			throw err("argument concreteArray not array type:\n" + concreteArray);
		if (concreteArray.operator() != ARRAY) {
			throw err("argument concreteArray is not concrete:\n" + concreteArray);

		} else {
			SymbolicType elementType = ((SymbolicArrayType) type).elementType();
			HomogeneousExpression<?> hArray = (HomogeneousExpression<?>) concreteArray;
			SymbolicExpression[] elements = (SymbolicExpression[]) hArray.arguments();
			int length = elements.length;

			if (index < 0 || index > length)
				throw err("Index out of range:\narray: " + concreteArray + "\nlength: " + length + "\nindex: " + index);
			if (incompatible(elementType, value.type()))
				throw err("Argument value to method insertElementAt has incompatible type." + "\nvalue: " + value
						+ "\ntype: " + value.type() + "\nExpected: " + elementType);
			elements = exprSeqFactory.insert(elements, index, value);
			return array(elementType, elements);
		}
	}

	@Override
	public BooleanExpression isSubsetOf(SymbolicExpression set1, SymbolicExpression set2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SymbolicExpression setAdd(SymbolicExpression set, SymbolicExpression value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SymbolicExpression setRemove(SymbolicExpression set, SymbolicExpression value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SymbolicExpression setUnion(SymbolicExpression set1, SymbolicExpression set2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SymbolicExpression setIntersection(SymbolicExpression set1, SymbolicExpression set2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SymbolicExpression setDifference(SymbolicExpression set1, SymbolicExpression set2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NumericExpression cardinality(SymbolicExpression set) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SymbolicExpression emptyMap(SymbolicMapType mapType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SymbolicExpression put(SymbolicExpression map, SymbolicExpression key, SymbolicExpression value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SymbolicExpression get(SymbolicExpression map, SymbolicExpression key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SymbolicExpression removeEntryWithKey(SymbolicExpression map, SymbolicExpression key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SymbolicExpression keySet(SymbolicExpression map) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NumericExpression mapSize(SymbolicExpression map) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SymbolicExpression entrySet(SymbolicExpression map) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<SymbolicConstant> getFreeSymbolicConstants(SymbolicExpression expr) {
		return expr.getFreeVars();
	}

	public UnaryOperator<SymbolicExpression> mapSubstituter(Map<SymbolicExpression, SymbolicExpression> map) {
		return mapSubstituter(e -> map.get(e));
	}

	@Override
	public UnaryOperator<SymbolicExpression> constantSubstituter(Map<SymbolicConstant, SymbolicExpression> map) {
		return mapSubstituter(e -> e instanceof SymbolicConstant ? map.get(e) : null);
	}

	@Override
	public UnaryOperator<SymbolicExpression> mapSubstituter(UnaryOperator<SymbolicExpression> operator) {
		return new MapSubstituter(this, objectFactory, typeFactory, operator);
	}

	@Override
	public UnaryOperator<SymbolicExpression> nameSubstituter(Map<StringObject, StringObject> nameMap) {
		return new NameSubstituter(this, objectFactory, typeFactory, nameMap);
	}

	@Override
	public UnaryOperator<SymbolicExpression> simpleSubstituter(SymbolicConstant var, SymbolicExpression value) {
		return new SimpleSubstituter(this, objectFactory, typeFactory, var, value);
	}

	@Override
	public CanonicalRenamer canonicalRenamer(String root, Predicate<SymbolicConstant> ignore) {
		return new CommonCanonicalRenamer(this, typeFactory, objectFactory, root, ignore);
	}

	@Override
	public CanonicalRenamer canonicalRenamer(String root) {
		return new CommonCanonicalRenamer(this, typeFactory, objectFactory, root, null);
	}

	@Override
	public ObjectFactory objectFactory() {
		return objectFactory;
	}

	@Override
	public SymbolicTypeFactory typeFactory() {
		return typeFactory;
	}

	/**
	 * Returns the result of bit-and operation for two given bit vectors, those two
	 * vectors are in the form of {@link SymbolicExpression}.
	 * 
	 * @param left  a non-<code>null</code> {@link SymbolicExpression} representing
	 *              an array of booleans. The length of this array should be
	 *              concrete.
	 * @param right a non-<code>null</code> {@link SymbolicExpression} representing
	 *              an array of booleans with the same type of the left array. And
	 *              the length of this array should be concrete.
	 * @return a {@link SymbolicExpression} representing the result array.
	 */
	private SymbolicExpression bitand(SymbolicExpression left, SymbolicExpression right) {
		if (left == null)
			throw err("Argument left to method bitand is null.");
		if (right == null)
			throw err("Argument right to method bitand is null.");
		if (!(left.type() instanceof SymbolicCompleteArrayType))
			throw err("Argument left to method bitand does not have array type." + "\narray: " + left + "\ntype: "
					+ left.type());
		if (!(right.type() instanceof SymbolicCompleteArrayType))
			throw err("Argument right to method bitand does not have array type." + "\narray: " + right + "\ntype: "
					+ right.type());
		SymbolicCompleteArrayType leftArrayType = (SymbolicCompleteArrayType) left.type();
		SymbolicCompleteArrayType rightArrayType = (SymbolicCompleteArrayType) right.type();

		if (!leftArrayType.equals(rightArrayType)) {
			throw err("Argument left and right to method bitand does not have the same array type." + "\nleft array: "
					+ left + "\nleft type: " + left.type() + "\nright array: " + right + "\nright type: "
					+ right.type());
		}
		if (!leftArrayType.elementType().isBoolean()) {
			throw err("Elements of left or right to method bitand does not have the boolean type." + "\nelement type: "
					+ leftArrayType.typeKind());
		}

		IntegerNumber lengthNumber = (IntegerNumber) extractNumber(leftArrayType.extent());

		assert lengthNumber != null;

		int size = lengthNumber.intValue();
		BooleanExpression[] resultArray = new BooleanExpression[size];

		if (size < 0) {
			throw err("Argument left and right to method bitand could not have a length less than 0." + "\nleft array: "
					+ left + "\nleft array length: " + extractNumber(leftArrayType.extent()) + "\nright array: " + right
					+ "\nleft array length: " + extractNumber(rightArrayType.extent()));
		}
		for (int i = 0; i < size; i++) {
			NumericExpression index = integer(i);
			BooleanExpression leftElement = (BooleanExpression) arrayRead(left, index);

			if (leftElement.isFalse()) {
				resultArray[i] = booleanFactory.falseExpr();
				continue;
			}

			BooleanExpression rightElement = (BooleanExpression) arrayRead(right, index);

			resultArray[i] = booleanFactory.and(leftElement, rightElement);
		}
		return array(leftArrayType.elementType(), resultArray);
	}

	@Override
	public SymbolicExpression integer2Bitvector(NumericExpression integer, SymbolicCompleteArrayType bitVectorType) {
		assert integer != null;
		assert integer.type() instanceof SymbolicIntegerType;

		IntegerNumber lenNum = (IntegerNumber) extractNumber(bitVectorType.extent());
		int intVal = -1;
		int mask = 1;
		int length = lenNum.intValue();
		BooleanExpression[] resultArray = new BooleanExpression[length];
		SymbolicType elementType = ((SymbolicCompleteArrayType) bitVectorType).elementType();
		IntegerNumber intNum = (IntegerNumber) extractNumber(integer);

		if (intNum != null) {
			// Input integer is concrete
			intVal = intNum.intValue();
			// Greater index for lower bit
			for (int i = length - 1; i >= 0; i--) {
				resultArray[i] = bool((intVal & mask) != 0);
				mask = mask << 1;
			}
			return array(elementType, resultArray);
		} else {
			// Input integer is symbolic
			SymbolicConstant int2bvConstant = null;

			if (length > int2bvConstants.size()) {
				for (int i = int2bvConstants.size() - 1; i < length; i++) {
					int2bvConstants.add(null);
				}
			}
			int2bvConstant = int2bvConstants.get(length);
			if (int2bvConstant == null) {
				// Create new bvType, if not found.
				SymbolicFunctionType int2bvFunctionType = functionType(Arrays.asList(integerType), bitVectorType);
				String bvName = "int2bv_" + length;

				int2bvConstant = symbolicConstant(stringObject(bvName), int2bvFunctionType);
				int2bvConstants.add(int2bvConstant); /* Record */
				int2bvConstants.set(length, int2bvConstant);
			}
			// If input is x and length is 32, it will return 'int2bv_32(x)'
			return apply(int2bvConstant, Arrays.asList(integer));
		}
	}

	@Override
	public NumericExpression bitvector2Integer(SymbolicExpression bitvector) {
		assert bitvector != null;

		SymbolicCompleteArrayType boolArrayType = (SymbolicCompleteArrayType) bitvector.type();

		assert boolArrayType instanceof SymbolicCompleteArrayType;
		assert boolArrayType.elementType().isBoolean();

		NumericExpression lengthExpr = boolArrayType.extent();
		NumericExpression result = zeroInt();
		NumericExpression j = integer(1);
		NumericExpression intTwo = integer(2);
		BooleanExpression boolArrayElement = null;
		int len = ((IntegerNumber) extractNumber(lengthExpr)).intValue();

		for (int i = len - 1; i >= 0; i--) {
			boolArrayElement = (BooleanExpression) arrayRead(bitvector, integer(i));
			if (boolArrayElement.operator().equals(SymbolicOperator.CONCRETE)) {
				result = boolArrayElement.isTrue() ? add(result, j) : result;
			} else {
				NumericExpression tempExpr = (NumericExpression) cond(boolArrayElement, j, zeroInt());

				result = add(tempExpr, result);
			}
			j = multiply(j, intTwo);
		}
		return result;
	}

	@Override
	public SymbolicCompleteArrayType bitVectorType(int length) {
		return arrayType(booleanType, integer(length));
	}

	@Override
	public void setErrFile(String errFile) {
		this.errFileName = errFile;
	}

	@Override
	public String getErrFile() {
		return errFileName;
	}

	/**
	 * Returns the result of bit-or operation for two given bit vectors, those two
	 * vectors are in the form of {@link SymbolicExpression}.
	 * 
	 * @param left  a non-<code>null</code> {@link SymbolicExpression} representing
	 *              an array of booleans. The length of this array should be
	 *              concrete.
	 * @param right a non-<code>null</code> {@link SymbolicExpression} representing
	 *              an array of booleans with the same type of the left array. And
	 *              the length of this array should be concrete.
	 * @return a {@link SymbolicExpression} representing the result array.
	 */
	private SymbolicExpression bitor(SymbolicExpression left, SymbolicExpression right) {
		if (left == null)
			throw err("Argument left to method bitor is null.");
		if (right == null)
			throw err("Argument right to method bitor is null.");
		if (!(left.type() instanceof SymbolicCompleteArrayType))
			throw err("Argument left to method bitor does not have array type." + "\narray: " + left + "\ntype: "
					+ left.type());
		if (!(right.type() instanceof SymbolicCompleteArrayType))
			throw err("Argument right to method bitor does not have array type." + "\narray: " + right + "\ntype: "
					+ right.type());
		SymbolicCompleteArrayType leftArrayType = (SymbolicCompleteArrayType) left.type();
		SymbolicCompleteArrayType rightArrayType = (SymbolicCompleteArrayType) right.type();

		if (!leftArrayType.equals(rightArrayType)) {
			throw err("Argument left and right to method bitor does not have the same array type." + "\nleft array: "
					+ left + "\nleft type: " + left.type() + "\nright array: " + right + "\nright type: "
					+ right.type());
		}
		if (!leftArrayType.elementType().isBoolean()) {
			throw err("Elements of left or right to method bitor does not have the boolean type." + "\nelement type: "
					+ leftArrayType.typeKind());
		}

		IntegerNumber lengthNumber = (IntegerNumber) extractNumber(leftArrayType.extent());

		assert lengthNumber != null;

		int size = lengthNumber.intValue();
		BooleanExpression[] resultArray = new BooleanExpression[size];

		if (size < 0) {
			throw err("Argument left and right to method bitor could not have a length less than 0." + "\nleft array: "
					+ left + "\nleft array length: " + extractNumber(leftArrayType.extent()) + "\nright array: " + right
					+ "\nleft array length: " + extractNumber(rightArrayType.extent()));
		}
		for (int i = 0; i < size; i++) {
			NumericExpression index = integer(i);
			BooleanExpression leftElement = (BooleanExpression) arrayRead(left, index);

			if (leftElement.isTrue()) {
				resultArray[i] = booleanFactory.trueExpr();
				continue;
			}

			BooleanExpression rightElement = (BooleanExpression) arrayRead(right, index);

			resultArray[i] = booleanFactory.or(leftElement, rightElement);
		}
		return array(leftArrayType.elementType(), resultArray);
	}

	/**
	 * Returns the result of bit-xor operation for two given bit vectors, those two
	 * vectors are in the form of {@link SymbolicExpression}.
	 * 
	 * @param left  a non-<code>null</code> {@link SymbolicExpression} representing
	 *              an array of booleans. The length of this array should be
	 *              concrete.
	 * @param right a non-<code>null</code> {@link SymbolicExpression} representing
	 *              an array of booleans with the same type of the left array. And
	 *              the length of this array should be concrete.
	 * @return a {@link SymbolicExpression} representing the result array.
	 */
	private SymbolicExpression bitxor(SymbolicExpression left, SymbolicExpression right) {
		if (left == null)
			throw err("Argument left to method bitxor is null.");
		if (right == null)
			throw err("Argument right to method bitxor is null.");
		if (!(left.type() instanceof SymbolicCompleteArrayType))
			throw err("Argument left to method bitxor does not have array type." + "\narray: " + left + "\ntype: "
					+ left.type());
		if (!(right.type() instanceof SymbolicCompleteArrayType))
			throw err("Argument right to method bitxor does not have array type." + "\narray: " + right + "\ntype: "
					+ right.type());
		SymbolicCompleteArrayType leftArrayType = (SymbolicCompleteArrayType) left.type();
		SymbolicCompleteArrayType rightArrayType = (SymbolicCompleteArrayType) right.type();

		if (!leftArrayType.equals(rightArrayType)) {
			throw err("Argument left and right to method bitxor does not have the same array type." + "\nleft array: "
					+ left + "\nleft type: " + left.type() + "\nright array: " + right + "\nright type: "
					+ right.type());
		}
		if (!leftArrayType.elementType().isBoolean()) {
			throw err("Elements of left or right to method bitxor does not have the boolean type." + "\nelement type: "
					+ leftArrayType.typeKind());
		}

		IntegerNumber lengthNumber = (IntegerNumber) extractNumber(leftArrayType.extent());

		assert lengthNumber != null;

		int size = lengthNumber.intValue();
		BooleanExpression[] resultArray = new BooleanExpression[size];

		if (size < 0) {
			throw err("Argument left and right to method bitxor could not have a length less than 0." + "\nleft array: "
					+ left + "\nleft array length: " + extractNumber(leftArrayType.extent()) + "\nright array: " + right
					+ "\nleft array length: " + extractNumber(rightArrayType.extent()));
		}
		for (int i = 0; i < size; i++) {
			NumericExpression index = integer(i);
			BooleanExpression leftElement = (BooleanExpression) arrayRead(left, index);
			BooleanExpression rightElement = (BooleanExpression) arrayRead(right, index);
			BooleanExpression resultElement1 = booleanFactory.and(booleanFactory.not(leftElement), rightElement);
			BooleanExpression resultElement2 = booleanFactory.and(booleanFactory.not(rightElement), leftElement);

			resultArray[i] = booleanFactory.or(resultElement1, resultElement2);
		}
		return array(leftArrayType.elementType(), resultArray);
	}

	/**
	 * Returns the result of bit-not operation for the given bit vector, the bit
	 * vectors is in the form of {@link SymbolicExpression}.
	 * 
	 * @param expression a non-<code>null</code> {@link SymbolicExpression}
	 *                   representing an array of booleans. The length of this array
	 *                   should be concrete.
	 * @return a {@link SymbolicExpression} representing the result array.
	 */
	private SymbolicExpression bitnot(SymbolicExpression expression) {
		if (expression == null)
			throw err("Argument expression to method bitnot is null.");
		if (!(expression.type() instanceof SymbolicCompleteArrayType))
			throw err("Argument expression to method bitnot does not have array type." + "\n array: " + expression
					+ "\ntype: " + expression.type());
		SymbolicCompleteArrayType exprArrayType = (SymbolicCompleteArrayType) expression.type();

		if (!exprArrayType.elementType().isBoolean()) {
			throw err("Elements of expression to method bitnot does not have the boolean type." + "\n element type: "
					+ exprArrayType.typeKind());
		}

		IntegerNumber lengthNumber = (IntegerNumber) extractNumber(exprArrayType.extent());

		assert lengthNumber != null;

		int size = lengthNumber.intValue();
		BooleanExpression[] resultArray = new BooleanExpression[size];

		if (size < 0) {
			throw err("Argument expression to method bitnot could not have a length less than 0." + "\n array: "
					+ expression + "\n array length: " + extractNumber(exprArrayType.extent()));
		}
		for (int i = 0; i < size; i++) {
			NumericExpression index = integer(i);
			BooleanExpression exprElement = (BooleanExpression) arrayRead(expression, index);
			BooleanExpression resultElement = booleanFactory.not(exprElement);

			resultArray[i] = resultElement;
		}
		return array(exprArrayType.elementType(), resultArray);
	}

	@Override
	public NumericExpression bitand(NumericExpression left, NumericExpression right) {
		assert left != null && right != null;

		SymbolicCompleteArrayType bitVectorType = bitVectorType(INTEGER_BIT_BOUND);
		SymbolicObject leftObj = left.argument(0);
		SymbolicObject rightObj = right.argument(0);
		boolean isLeftConcrete = leftObj instanceof NumberObject;
		boolean isRightConcrete = rightObj instanceof NumberObject;

		if (isLeftConcrete && isRightConcrete) {
			SymbolicExpression leftBitVector = integer2Bitvector(left, bitVectorType);
			SymbolicExpression rightBitVector = integer2Bitvector(right, bitVectorType);
			SymbolicExpression resBitVector = bitand(leftBitVector, rightBitVector);

			return bitvector2Integer(resBitVector);
		} else if (isLeftConcrete) {
			NumericExpression limit = power(integer(2), integer(INTEGER_BIT_BOUND));

			if (((NumberObject) leftObj).isZero())
				return numericFactory.zeroInt();
			else if (subtract(limit, left).isOne())
				return right;
		} else if (isRightConcrete) {
			NumericExpression limit = power(integer(2), integer(INTEGER_BIT_BOUND));

			if (((NumberObject) rightObj).isZero())
				return numericFactory.zeroInt();
			else if (subtract(limit, right).isOne())
				return left;
		}
		return numericFactory.expression(SymbolicOperator.BIT_AND, integerType, left, right);
	}

	@Override
	public NumericExpression bitor(NumericExpression left, NumericExpression right) {
		assert left != null && right != null;

		SymbolicCompleteArrayType bitVectorType = bitVectorType(INTEGER_BIT_BOUND);
		SymbolicObject leftObj = left.argument(0);
		SymbolicObject rightObj = right.argument(0);
		boolean isLeftConcrete = leftObj instanceof NumberObject;
		boolean isRightConcrete = rightObj instanceof NumberObject;

		if (isLeftConcrete && isRightConcrete) {
			SymbolicExpression leftBitVector = integer2Bitvector(left, bitVectorType);
			SymbolicExpression rightBitVector = integer2Bitvector(right, bitVectorType);
			SymbolicExpression resBitVector = bitor(leftBitVector, rightBitVector);

			return bitvector2Integer(resBitVector);
		} else if (isLeftConcrete) {
			NumericExpression limit = power(integer(2), integer(INTEGER_BIT_BOUND));

			if (((NumberObject) leftObj).isZero())
				return right;
			else if (subtract(limit, left).isOne())
				return left;
		} else if (isRightConcrete) {
			NumericExpression limit = power(integer(2), integer(INTEGER_BIT_BOUND));

			if (((NumberObject) rightObj).isZero())
				return left;
			else if (subtract(limit, right).isOne())
				return right;
		}
		return numericFactory.expression(SymbolicOperator.BIT_OR, integerType, left, right);
	}

	@Override
	public NumericExpression bitxor(NumericExpression left, NumericExpression right) {
		assert left != null && right != null;

		SymbolicCompleteArrayType bitVectorType = bitVectorType(INTEGER_BIT_BOUND);
		SymbolicObject leftObj = left.argument(0);
		SymbolicObject rightObj = right.argument(0);
		boolean isLeftConcrete = leftObj instanceof NumberObject;
		boolean isRightConcrete = rightObj instanceof NumberObject;

		if (isLeftConcrete && isRightConcrete) {
			SymbolicExpression leftBitVector = integer2Bitvector(left, bitVectorType);
			SymbolicExpression rightBitVector = integer2Bitvector(right, bitVectorType);
			SymbolicExpression resBitVector = bitxor(leftBitVector, rightBitVector);

			return bitvector2Integer(resBitVector);
		} else if (isLeftConcrete) {
			NumericExpression limit = power(integer(2), integer(INTEGER_BIT_BOUND));

			if (((NumberObject) leftObj).isZero())
				return right;
			else if (subtract(limit, left).isOne())
				return bitnot(right);
		} else if (isRightConcrete) {
			NumericExpression limit = power(integer(2), integer(INTEGER_BIT_BOUND));

			if (((NumberObject) rightObj).isZero())
				return left;
			else if (subtract(limit, right).isOne())
				return bitnot(left);
		}
		return numericFactory.expression(SymbolicOperator.BIT_XOR, integerType, left, right);
	}

	@Override
	public NumericExpression bitnot(NumericExpression expression) {
		assert expression != null;

		SymbolicCompleteArrayType bitVectorType = bitVectorType(INTEGER_BIT_BOUND);
		SymbolicObject exprObj = expression.argument(0);
		boolean isConcrete = exprObj instanceof NumberObject;

		if (expression.operator().equals(SymbolicOperator.BIT_NOT)) {
			return (NumericExpression) expression.argument(0);
		} else if (expression.operator().equals(SymbolicOperator.BIT_AND)) {
			return numericFactory.expression(SymbolicOperator.BIT_OR, integerType,
					bitnot((NumericExpression) expression.argument(0)),
					bitnot((NumericExpression) expression.argument(1)));
		} else if (expression.operator().equals(SymbolicOperator.BIT_OR)) {
			return numericFactory.expression(SymbolicOperator.BIT_AND, integerType,
					bitnot((NumericExpression) expression.argument(0)),
					bitnot((NumericExpression) expression.argument(1)));
		}
		if (isConcrete) {
			SymbolicExpression exprBitVector = integer2Bitvector(expression, bitVectorType);
			SymbolicExpression resBitVector = bitnot(exprBitVector);

			return bitvector2Integer(resBitVector);
		} else
			return numericFactory.expression(SymbolicOperator.BIT_NOT, integerType, expression);
	}

	@Override
	public void printCompressed(SymbolicExpression expr, PrintStream out) {
		new CompressedPrinter(this, out, expr).print();
	}

	@Override
	public void printCompressedTree(String prefix, SymbolicExpression expr, PrintStream out) {
		StringBuffer sbuf = new StringBuffer();

		expr.printCompressedTree(prefix, sbuf);
		out.print(sbuf.toString());
	}

	@Override
	public NumericExpression sigma(NumericExpression low, NumericExpression high, SymbolicExpression function) {
		NumericExpression sum;

		if (!low.type().isInteger() || !high.type().isInteger())
			throw new SARLException("low and high of Sigma expression must have integer type");
		if (function.type().typeKind() != SymbolicTypeKind.FUNCTION)
			throw new SARLException("Addends of Sigma expression must have function type");
		if (function.operator() != SymbolicOperator.LAMBDA)
			throw new SARLException("The third argument must be a lambda expression");

		SymbolicFunctionType functionType = (SymbolicFunctionType) function.type();

		if (functionType.inputTypes().numTypes() != 1)
			throw new SARLException("The type of the addend of Sigma expression must be a function type and "
					+ "have exact one input type");
		if (!functionType.outputType().isNumeric())
			throw new SARLException("The type of the addend of Sigma expression must be a function type and "
					+ "have a numeric output type");
		// Simplification case : low and high are both constants:
		if ((low instanceof Constant) && ((high instanceof Constant))) {
			IntegerNumber lowNum = (IntegerNumber) ((Constant) low).value().getNumber();
			IntegerNumber highNum = (IntegerNumber) ((Constant) high).value().getNumber();
			NumberFactory numberFactory = numberFactory();

			sum = zeroInt();
			for (IntegerNumber i = lowNum; numberFactory.compare(i, highNum) < 0; i = numberFactory.increment(i)) {
				NumberObject input = numberObject(i);
				NumericExpression addend = (NumericExpression) apply(function,
						Arrays.asList(numericExpressionFactory().number(input)));

				sum = add(sum, addend);
			}
			return sum;
		}
		// General case: return an uninterpreted function:
		SymbolicExpression result = ReservedFunctions.sigma(this, expressionFactory, functionType);

		return (NumericExpression) apply(result, Arrays.asList(low, high, function));
	}

	/*
	 * TODO: this function is experimental. The general representation of reduction
	 * operation is not perfect.
	 */
	@Override
	public SymbolicExpression reduction(SymbolicExpression[] operands, NumericExpression count, SymbolicExpression op,
			List<BooleanExpression> compatibleConditionOutput) {
		List<SymbolicExpression> allOperands = new LinkedList<>();
		BooleanExpression compatible = trueExpr;
		NumericSymbolicConstant idx = (NumericSymbolicConstant) symbolicConstant(stringObject("i"), integerType);

		/*
		 * Given two 1d array of objects "A0" and "A1", each of which has "count"
		 * elements, the element-wise reduction of the given operator "op" over these
		 * arrays is represented as
		 *
		 * ARRAY_LAMBDA int i. $rdc(count, op, {A0[i], A1[i]});
		 *
		 * The array lambda has the same type as A0 or A1. The $rdc function groups all
		 * the parameter that matters for the reduction result. The order of the
		 * elements in the array {A0[i], A1[i]} is always in canonicalized order. In
		 * other words, {A0[i], A1[i]} is a bag instead of an array.
		 *
		 * The return type of the $rdc function is the same type as A0[i] or A1[i].
		 */

		/*
		 * There are several points about the "inperfection" about this representation:
		 * 1. $rdc groups all the parameters that are matters for the reduction but the
		 * meaning of $rdc itself becomes confusing.
		 *
		 * 2. The two arrays A0 and A1 can be as general as byte arrays. While the
		 * elements, participated in element-wise reduction, are not necessarily bytes.
		 * They can be integers (by given different count value). However, since the
		 * return type of $rdc is same as the element type of A0 or A1, if given byte
		 * array A0 (or A1) while the reduction is performed on integers (e.g. every
		 * four bytes), the $rdc function is incompatible with another $rdc function
		 * that is applied to integer arrays instead of byte arrays.
		 */
		for (SymbolicExpression operand : operands) {
			// pre-condition: operand must be an 1-d array:
			Pair<List<SymbolicExpression>, BooleanExpression> ppOpreandResult = reductionPreproc(
					arrayRead(operand, idx), count, op);

			for (SymbolicExpression ppOperand : ppOpreandResult.left)
				allOperands.add(ppOperand);
			compatible = and(compatible, ppOpreandResult.right);
		}

		SymbolicType arrType = operands[0].type();

		assert arrType.typeKind() == SymbolicTypeKind.ARRAY;

		SymbolicType eleType = ((SymbolicArrayType) arrType).elementType();
		SymbolicExpression array = array(eleType, allOperands);
		SymbolicExpression reduced = makeReduction(count, op, array);

		compatibleConditionOutput.add(compatible);
		return arrayLambda((SymbolicCompleteArrayType) arrType, lambda(idx, reduced));
	}

	/**
	 * <p>
	 * If the given operand has such a form <code>$rdc(c, p, a)</code>, this method
	 * returns a list of elements in "a" and the compatible condition "c == count &&
	 * p == operator".
	 * </p>
	 */
	private Pair<List<SymbolicExpression>, BooleanExpression> reductionPreproc(SymbolicExpression operand,
			NumericExpression count, SymbolicExpression operator) {
		List<SymbolicExpression> result = new LinkedList<>();

		if (operand.operator() != APPLY) {
			result.add(operand);
			return new Pair<>(result, trueExpr);
		}

		SymbolicConstant func = (SymbolicConstant) operand.argument(0);
		StringObject name = (StringObject) func.argument(0);

		if (!reductionName.equals(name.getString())) {
			result.add(operand);
			return new Pair<>(result, trueExpr);
		}

		// reduction(count, op, array):
		SymbolicSequence<?> args = (SymbolicSequence<?>) operand.argument(1);
		NumericExpression thisCount = (NumericExpression) args.get(0);
		SymbolicExpression thisOp = args.get(1);
		SymbolicExpression thisArray = args.get(2);
		BooleanExpression compatible = and(equals(thisCount, count), equals(thisOp, operator));

		for (SymbolicObject ele : thisArray.getArguments()) {
			result.add((SymbolicExpression) ele);
		}
		return new Pair<>(result, compatible);
	}

	/**
	 * <p>
	 * returns true iff the given symbolic constant represents the
	 * {@link #reductionName} function
	 * </p>
	 *
	 * @param function a symbolic constant
	 * @return true iff the given symbolic constant represents the reduction
	 *         function
	 */
	private boolean isReductionCall(SymbolicExpression function) {
		SymbolicConstant symConst = (SymbolicConstant) function;

		return reductionName.equals(symConst.name().getString());
	}

	/**
	 * <p>
	 * Make a $rdc reduction function call <code>$rdc(count, op, array)</code>,
	 * which represents the reduction result of a group of elements.
	 *
	 * The returned type of the function is the element type of the array.
	 * </p>
	 *
	 * @param count the number of elements for element-wise reduction
	 * @param op    the reduction operator
	 * @param array the array of elements that will be reduced
	 * @return the uninterpreted function call <code>$rdc(count, op, array)</code>
	 */
	private SymbolicExpression makeReduction(NumericExpression count, SymbolicExpression op, SymbolicExpression array) {
		SymbolicExpression[] elements = new SymbolicExpression[array.numArguments()];
		int i = 0;

		for (SymbolicObject ele : array.getArguments())
			elements[i++] = (SymbolicExpression) ele;

		if (i == 1)
			return elements[0];
		Arrays.sort(elements, comparator());
		SymbolicType eleType = ((SymbolicArrayType) array.type()).elementType();
		SymbolicExpression newArray = array(eleType, Arrays.asList(elements));
		SymbolicType funcType = functionType(Arrays.asList(integerType, op.type(), arrayType(eleType)), eleType);
		SymbolicConstant func = symbolicConstant(stringObject(reductionName), funcType);

		return apply(func, Arrays.asList(count, op, newArray));
	}

	@Override
	public boolean isSigmaCall(SymbolicExpression expr) {
		return ReservedFunctions.isSigmaCall(expr);
	}

	/**
	 * A helper method for
	 * {@link #make(SymbolicOperator, SymbolicType, SymbolicObject[])}.
	 * 
	 * <p>
	 * Make a sigma expression by giving a {@link SymbolicSequence} of arguments of
	 * the sigma uninterpreted function
	 * </p>
	 * 
	 * @param args {@link SymbolicSequence} of arguments of the uninterpreted
	 *             function "Sigma".
	 * @return
	 */
	private SymbolicExpression makeSigma(SymbolicSequence<?> args) {
		NumericExpression low = (NumericExpression) args.get(0);
		NumericExpression high = (NumericExpression) args.get(1);
		SymbolicExpression sigmaFunction = args.get(2);

		return sigma(low, high, sigmaFunction);
	}

	/**
	 * A helper method for
	 * {@link #make(SymbolicOperator, SymbolicType, SymbolicObject[])} to make
	 * {@link #permut(SymbolicExpression, SymbolicExpression, NumericExpression, NumericExpression)}
	 */
	private SymbolicExpression makePermut(SymbolicSequence<?> args) {
		SymbolicExpression array_a = args.get(0);
		SymbolicExpression array_b = args.get(1);
		NumericExpression low = (NumericExpression) args.get(2);
		NumericExpression high = (NumericExpression) args.get(3);

		return permut(array_a, array_b, low, high);
	}

	@Override
	public void printExprTree(SymbolicExpression expr, PrintStream out) {
		printExprTreeWorker("", out, expr);
	}

	private void printExprTreeWorker(String prefix, PrintStream out, SymbolicObject expr) {
		switch (expr.symbolicObjectKind()) {
		case EXPRESSION: {
			SymbolicExpression symExpr = (SymbolicExpression) expr;

			prefix += " ";
			if (symExpr.operator() == SymbolicOperator.CONCRETE
					|| symExpr.operator() == SymbolicOperator.SYMBOLIC_CONSTANT)
				out.println(prefix + symExpr);
			else {
				out.print(prefix);
				out.println(symExpr.operator());
				for (SymbolicObject arg : symExpr.getArguments())
					printExprTreeWorker(prefix + "|", out, arg);
			}
		}
			break;
		case SEQUENCE: {
			SymbolicSequence<?> symSeq = (SymbolicSequence<?>) expr;

			out.println(prefix + " SEQ");
			for (int i = 0; i < symSeq.size(); i++) {
				SymbolicObject seq = symSeq.get(i);

				printExprTreeWorker(prefix + " |", out, seq);
			}
			break;
		}
		case INT:
		case CHAR:
		case BOOLEAN:
		case STRING:
		case NUMBER:
			out.println(prefix + " " + expr);
			break;
		case TYPE:
		case TYPE_SEQUENCE:
		default:
			out.println("Unkownn Symbolic Object: " + expr.symbolicObjectKind());
		}
	}

	@Override
	public NumericExpression bitshiftLeft(NumericExpression left, NumericExpression right) {
		assert left != null && right != null;

		SymbolicCompleteArrayType bitVectorType = bitVectorType(INTEGER_BIT_BOUND);
		SymbolicObject leftObj = left.argument(0);
		SymbolicObject rightObj = right.argument(0);
		boolean isLeftConcrete = leftObj instanceof NumberObject;
		boolean isRightConcrete = rightObj instanceof NumberObject;

		if (isLeftConcrete && isRightConcrete) {
			SymbolicExpression leftBitVector = integer2Bitvector(left, bitVectorType);
			SymbolicExpression rightShiftNumBits = right;
			SymbolicExpression resBitVector = bitshiftLeft(leftBitVector, rightShiftNumBits);

			return bitvector2Integer(resBitVector);
		}
		return numericFactory.expression(SymbolicOperator.BIT_SHIFT_LEFT, integerType, left, right);
	}

	private SymbolicExpression bitshiftLeft(SymbolicExpression left, SymbolicExpression right) {
		if (left == null)
			throw err("Argument left to method bitShiftLeft is null.");
		if (right == null)
			throw err("Argument right to method bitShiftLeft is null.");
		if (!(left.type() instanceof SymbolicCompleteArrayType))
			throw err("Argument left to method bitShiftLeft does not have array type." + "\narray: " + left + "\ntype: "
					+ left.type());
		if (!(right instanceof NumericExpression))
			throw err("Argument right to method bitShiftLeft should be an unsigned integer." + "\narray: " + right
					+ "\ntype: " + right.type());

		SymbolicCompleteArrayType leftArrayType = (SymbolicCompleteArrayType) left.type();

		if (!leftArrayType.elementType().isBoolean()) {
			throw err("Elements of left to method bitShiftLeft does not have the boolean type." + "\nelement type: "
					+ leftArrayType.typeKind());
		}

		IntegerNumber lengthNumber = (IntegerNumber) extractNumber(leftArrayType.extent());
		IntegerNumber shiftNumberOfBits = (IntegerNumber) extractNumber((NumericExpression) right);

		assert lengthNumber != null;
		assert shiftNumberOfBits != null;

		int size = lengthNumber.intValue();
		int shiftSize = shiftNumberOfBits.intValue();
		BooleanExpression[] resultArray = new BooleanExpression[size];

		if (size < 0) {
			throw err("Argument left to method bitShiftLeft could not have a length less than 0." + "\nleft array: "
					+ left + "\nleft array length: " + size);
		}
		if (shiftSize < 0) {
			throw err("Argument right to method bitShiftLeft could not have a value less than 0." + "\nleft array: "
					+ left + "\nleft array length: " + shiftSize);
		} else if (shiftSize >= INTEGER_BIT_BOUND) {
			// Return 0
			for (int i = 0; i < size; i++)
				resultArray[i] = booleanFactory.falseExpr();
		} else {
			for (int i = 0; i < size - shiftSize; i++) {
				int shift_i = i + shiftSize;
				NumericExpression shift_index = integer(shift_i);
				resultArray[i] = (BooleanExpression) arrayRead(left, shift_index);
			}
			for (int i = size - shiftSize; i < size; i++)
				resultArray[i] = booleanFactory.falseExpr();
		}
		return array(leftArrayType.elementType(), resultArray);
	}

	@Override
	public NumericExpression bitshiftRight(NumericExpression left, NumericExpression right) {
		assert left != null && right != null;

		SymbolicCompleteArrayType bitVectorType = bitVectorType(INTEGER_BIT_BOUND);
		SymbolicObject leftObj = left.argument(0);
		SymbolicObject rightObj = right.argument(0);
		boolean isLeftConcrete = leftObj instanceof NumberObject;
		boolean isRightConcrete = rightObj instanceof NumberObject;

		if (isLeftConcrete && isRightConcrete) {
			SymbolicExpression leftBitVector = integer2Bitvector(left, bitVectorType);
			SymbolicExpression rightShiftNumBits = right;
			SymbolicExpression resBitVector = bitshiftRight(leftBitVector, rightShiftNumBits);

			return bitvector2Integer(resBitVector);
		}
		return numericFactory.expression(SymbolicOperator.BIT_SHIFT_RIGHT, integerType, left, right);
	}

	private SymbolicExpression bitshiftRight(SymbolicExpression left, SymbolicExpression right) {
		if (left == null)
			throw err("Argument left to method bitshiftRight is null.");
		if (right == null)
			throw err("Argument right to method bitshiftRight is null.");
		if (!(left.type() instanceof SymbolicCompleteArrayType))
			throw err("Argument left to method bitshiftRight does not have array type." + "\narray: " + left
					+ "\ntype: " + left.type());
		if (!(right instanceof NumericExpression))
			throw err("Argument right to method bitshiftRight should be an unsigned integer." + "\narray: " + right
					+ "\ntype: " + right.type());

		SymbolicCompleteArrayType leftArrayType = (SymbolicCompleteArrayType) left.type();

		if (!leftArrayType.elementType().isBoolean()) {
			throw err("Elements of left to method bitshiftRight does not have the boolean type." + "\nelement type: "
					+ leftArrayType.typeKind());
		}

		IntegerNumber lengthNumber = (IntegerNumber) extractNumber(leftArrayType.extent());
		IntegerNumber shiftNumberOfBits = (IntegerNumber) extractNumber((NumericExpression) right);

		assert lengthNumber != null;
		assert shiftNumberOfBits != null;

		int size = lengthNumber.intValue();
		int shiftSize = shiftNumberOfBits.intValue();
		BooleanExpression[] resultArray = new BooleanExpression[size];

		if (size < 0) {
			throw err("Argument left to method bitshiftRight could not have a length less than 0." + "\nleft array: "
					+ left + "\nleft array length: " + size);
		}
		if (shiftSize < 0) {
			throw err("Argument right to method bitshiftRight could not have a value less than 0." + "\nleft array: "
					+ left + "\nleft array length: " + shiftSize);
		} else if (shiftSize >= INTEGER_BIT_BOUND) {
			// Return 0
			for (int i = 0; i < size; i++)
				resultArray[i] = booleanFactory.falseExpr();
		} else {
			for (int i = 0; i < shiftSize; i++)
				resultArray[i] = booleanFactory.falseExpr();
			for (int i = shiftSize; i < size; i++) {
				int shift_i = i - shiftSize;
				NumericExpression shift_index = integer(shift_i);
				resultArray[i] = (BooleanExpression) arrayRead(left, shift_index);
			}
		}
		return array(leftArrayType.elementType(), resultArray);
	}

	@Override
	public SymbolicExpression fullySubstitute(Map<SymbolicExpression, SymbolicExpression> substituteMap,
			SymbolicExpression expression) {
		UnaryOperator<SymbolicExpression> substituter = mapSubstituter(substituteMap);
		SymbolicExpression transformedExpression = expression;
		SymbolicExpression prevTransformedExpression = expression;

		do {
			prevTransformedExpression = transformedExpression;
			transformedExpression = substituter.apply(transformedExpression);
		} while (transformedExpression != prevTransformedExpression);
		return transformedExpression;
	}

	@Override
	public int getIntegerLengthBound() {
		return this.INTEGER_BIT_BOUND;
	}

	@Override
	public boolean setIntegerLengthBound(int bound) {
		boolean result = bound >= this.INTEGER_BIT_BOUND;

		this.INTEGER_BIT_BOUND = bound;
		return result;
	}

	/**
	 * Checks that symbolic expression is a function from R^n to R for some positive
	 * integer n.
	 * 
	 * @param function symbolic expression which has type function from R^n to R for
	 *                 some positive integer n
	 * @return the dimension of the input space, n
	 * @throws SARLException if <code>function</code>'s type is not function from
	 *                       R^n to R for some positive integer n
	 */
	private int checkRealFunction(SymbolicExpression function) {
		SymbolicType theType = function.type();

		if (!(theType instanceof SymbolicFunctionType))
			throw err("Argument function should have a function type, not " + theType);

		SymbolicFunctionType functionType = (SymbolicFunctionType) theType;
		SymbolicType outputType = functionType.outputType();

		if (!outputType.isReal())
			throw err("Function should return real, not " + outputType);

		SymbolicTypeSequence inputTypes = functionType.inputTypes();
		int numInputs = inputTypes.numTypes();

		if (numInputs <= 0)
			throw err("Function must accept at least one real input, not " + numInputs);
		for (int i = 0; i < numInputs; i++) {
			SymbolicType inputType = inputTypes.getType(i);

			if (!inputType.isReal())
				throw err("Expected function from R^n, but input type " + i + " is " + inputType);
		}
		return numInputs;
	}

	private void checkRealSequence(SymbolicSequence<? extends NumericExpression> seq, int length) {
		if (length != seq.size())
			throw err("Expected sequence of length " + length + " but got " + seq.size());
		for (int i = 0; i < length; i++) {
			SymbolicType type = seq.get(i).type();

			if (!type.isReal())
				throw err("Expected real type, but element " + i + " of sequence has type " + type);
		}
	}

	@Override
	public SymbolicExpression derivative(SymbolicExpression function, IntObject index, IntObject degree) {
		checkRealFunction(function);
		return expression(SymbolicOperator.DERIV, function.type(), function, index, degree);
	}

	@Override
	public BooleanExpression differentiable(SymbolicExpression function, IntObject degree,
			Iterable<? extends NumericExpression> lowerBounds, Iterable<? extends NumericExpression> upperBounds) {
		int n = checkRealFunction(function);
		SymbolicSequence<? extends NumericExpression> lowerSeq = objectFactory.sequence(lowerBounds),
				upperSeq = objectFactory.sequence(upperBounds);

		checkRealSequence(lowerSeq, n);
		checkRealSequence(upperSeq, n);
		if (degree.isNegative())
			throw err("Differentiable degree must be nonnegative but was " + degree);
		return (BooleanExpression) expression(SymbolicOperator.DIFFERENTIABLE, booleanType,
				new SymbolicObject[] { function, degree, lowerSeq, upperSeq });
	}

	/**
	 * Decomposes an expression into a sum of terms, adding those terms to a list.
	 * 
	 * @param expr        an expression
	 * @param accumulator list to which to add the terms
	 */
	private void getSummandsWork(NumericExpression expr, List<NumericExpression> accumulator) {
		SymbolicOperator op = expr.operator();

		if (op == SymbolicOperator.ADD) {
			@SuppressWarnings("unchecked")
			Iterable<NumericExpression> args = (Iterable<NumericExpression>) expr.getArguments();

			for (NumericExpression arg : args)
				getSummandsWork(arg, accumulator);
		} else if (op == SymbolicOperator.SUBTRACT) {
			getSummandsWork((NumericExpression) expr.argument(0), accumulator);

			List<NumericExpression> temp = new LinkedList<>();

			getSummandsWork((NumericExpression) expr.argument(1), temp);
			for (NumericExpression x : temp)
				accumulator.add(minus(x));
		} else {
			accumulator.add(expr);
		}
	}

	@Override
	public NumericExpression[] getSummands(NumericExpression expr) {
		List<NumericExpression> list = new LinkedList<NumericExpression>();

		getSummandsWork(expr, list);

		NumericExpression[] result = new NumericExpression[list.size()];

		list.toArray(result);
		return result;
	}

	private class InequalitySolution {
		boolean isUpper;
		NumericExpression bound;
	}

	/**
	 * Given a boolean expression and integer variable <code>v</code>, if that
	 * expression is an inequality of the form
	 * 
	 * <pre>
	 * [+/-]v [+/-]e [<=,>=] 0,
	 * </pre>
	 * 
	 * where <code>e</code> is an integer expression that does not involve
	 * <code>v</code>, this method will "solve" for <code>v</code> to return either
	 * an upper or lower bound on <code>v</code>. That bound will be a numeric
	 * expression, either <code>e</code> or <code>-e</code>.
	 * 
	 * @param var        variable to "solve" for
	 * @param inequality any non-<code>null</code> boolean expression, usually an
	 *                   inequality
	 * @return the solution, which specifies the non-strict bound and whether it is
	 *         upper or lower, or <code>null</code> if <code>inequality</code> is
	 *         not an inequality or is not of the correct form.
	 */
	private InequalitySolution solveIntegerInequality(NumericSymbolicConstant var, BooleanExpression inequality) {
		SymbolicOperator op = inequality.operator();

		if (op != SymbolicOperator.LESS_THAN_EQUALS)
			return null;

		NumericExpression arg0 = (NumericExpression) inequality.argument(0);
		NumericExpression arg1 = (NumericExpression) inequality.argument(1);
		NumericExpression expr;
		boolean isUpper;

		if (arg0.isZero()) { // 0 <= arg1 = v+e
			expr = arg1;
			isUpper = false;
		} else if (arg1.isZero()) { // v+e = arg0 <= 0
			expr = arg0;
			isUpper = true;
		} else { // 0 <= arg1 - arg0
			expr = subtract(arg1, arg0);
			isUpper = false;
		}

		NumericExpression[] terms = getSummands(expr);
		int numTerms = terms.length;
		NumericExpression negVar = minus(var);
		int varIndex = -1;
		boolean negateBound = true;

		// looking for v or -v ...
		for (int i = 0; i < numTerms; i++) {
			NumericExpression term = terms[i];

			if (term.equals(var)) {
				varIndex = i;
				break;
			} else if (term.equals(negVar)) { // 0<=-v+e, -v+e<=0
				varIndex = i;
				negateBound = !negateBound;
				isUpper = !isUpper;
			}
		}
		if (varIndex < 0)
			return null;

		NumericExpression bound = zeroInt();

		for (int i = 0; i < numTerms; i++) {
			if (i != varIndex) {
				NumericExpression term = terms[i];

				if (getFreeSymbolicConstants(term).contains(var))
					return null;
				bound = add(bound, term);
			}
		}
		if (negateBound)
			bound = minus(bound);

		InequalitySolution result = new InequalitySolution();

		result.bound = bound;
		result.isUpper = isUpper;
		return result;
	}

	class ClauseAnalysis {
		NumericExpression lower;
		NumericExpression upper;
		BooleanExpression remain;
	}

	/**
	 * Given a boolean expression, interpreted as an disjunction
	 * <code>p1||p2|| ... ||pn</code> of clauses, and integer variable
	 * <code>var</code>, this method searches for a clause which gives an upper
	 * bound on <code>var</code> and a clause which gives a lower bound on
	 * <code>var</code>. Those two clauses are separated out and the disjunction of
	 * the remaining clauses form the <code>remain</code> field of the object
	 * returned.
	 * 
	 * @param var      the variable of integer type
	 * @param disjunct the boolean expression
	 * @return the analysis object specifying the lower and upper bounds found and
	 *         the disjunction of the remaining clauses, or <code>null</code> if a
	 *         lower bound or upper bound clause was not found
	 */
	private ClauseAnalysis analyzeClause(NumericSymbolicConstant var, BooleanExpression disjunct) {
		if (disjunct.operator() != SymbolicOperator.OR)
			return null;

		int numClauses = disjunct.numArguments();

		if (numClauses < 2)
			return null;

		@SuppressWarnings("unchecked")
		Iterable<? extends BooleanExpression> clauses = (Iterable<? extends BooleanExpression>) disjunct.getArguments();
		NumericExpression upper = null, lower = null;
		List<BooleanExpression> remainingClauses = new LinkedList<>();

		for (BooleanExpression clause : clauses) {
			if (upper == null || lower == null) {
				InequalitySolution solution = solveIntegerInequality(var, clause);

				if (solution != null) {
					if (solution.isUpper) {
						if (upper == null) {
							upper = solution.bound;
							continue;
						}
					} else {
						if (lower == null) {
							lower = solution.bound;
							continue;
						}
					}
				}
			}
			remainingClauses.add(clause);
		}
		if (upper == null || lower == null)
			return null;

		ClauseAnalysis result = new ClauseAnalysis();

		result.lower = lower;
		result.upper = upper;
		result.remain = or(remainingClauses);
		return result;
	}

	@Override
	public ForallStructure getForallStructure(BooleanExpression forallExpr) {
		if (forallExpr.operator() != SymbolicOperator.FORALL)
			return null;
		if (!((SymbolicExpression) forallExpr.argument(0)).type().isInteger())
			return null;

		NumericSymbolicConstant var = (NumericSymbolicConstant) forallExpr.argument(0);
		BooleanExpression[] disjuncts = ((BooleanExpression) forallExpr.argument(1)).getClauses();
		int numDisjuncts = disjuncts.length;

		if (numDisjuncts == 0)
			return null;

		ClauseAnalysis analysis0 = analyzeClause(var, disjuncts[0]);

		if (analysis0 == null)
			return null;

		List<BooleanExpression> newClauses = new LinkedList<>();

		newClauses.add(analysis0.remain);
		for (int i = 1; i < numDisjuncts; i++) {
			BooleanExpression disjunct = disjuncts[i];
			ClauseAnalysis analysis = analyzeClause(var, disjunct);

			if (analysis == null)
				return null;
			if (!analysis0.lower.equals(analysis.lower))
				return null;
			if (!analysis0.upper.equals(analysis.upper))
				return null;
			newClauses.add(analysis.remain);
		}

		ForallStructure result = new ForallStructure();
		NumericExpression one = oneInt();

		result.boundVariable = var;
		result.lowerBound = add(analysis0.upper, one);
		result.upperBound = subtract(analysis0.lower, one);
		result.body = and(newClauses);
		return result;
	}

	@Override
	public NumericExpression[] expand(NumericExpression expr) {
		return numericFactory.expand(expr);
	}

	@Override
	public RationalNumber getProbabilisticBound() {
		return this.probabilisticBound;
	}

	@Override
	public void setProbabilisticBound(RationalNumber epsilon) {
		if (epsilon.signum() < 0 || REAL_FACTORY.oneRational().numericalCompareTo(epsilon) <= 0)
			throw new SARLException("Probabilitic bound must be in [0,1), not " + epsilon);
		this.probabilisticBound = epsilon;
	}

	@Override
	public NumericExpression floor(NumericExpression expr) {
		return numericFactory.floor(expr);
	}

	@Override
	public boolean isFloor(SymbolicExpression expr) {
		return numericFactory.isFloor(expr);
	}

	@Override
	public NumericExpression ceil(NumericExpression expr) {
		return numericFactory.ceil(expr);
	}

	@Override
	public NumericExpression roundToZero(NumericExpression expr) {
		return numericFactory.roundToZero(expr);
	}

	@Override
	public boolean getUseBackwardSubstitution() {
		return useBackwardSubstitution;
	}

	@Override
	public void setUseBackwardSubstitution(boolean value) {
		this.useBackwardSubstitution = value;
	}

	@Override
	public SymbolicExpression concreteValueOfUninterpretedType(SymbolicUninterpretedType type, IntObject key) {
		return expression(SymbolicOperator.CONCRETE, type, key);
	}

	@Override
	public BooleanExpression permut(SymbolicExpression array_a, SymbolicExpression array_b, NumericExpression low,
			NumericExpression high) {
		if (array_a.type().typeKind() != SymbolicTypeKind.ARRAY)
			throw new SARLException("First argument " + array_a + " to permut predicate has no array type.");
		if (array_b.type().typeKind() != SymbolicTypeKind.ARRAY)
			throw new SARLException("Second argument " + array_b + " to permut predicate has no array type.");

		SymbolicArrayType typea = (SymbolicArrayType) array_a.type();
		SymbolicArrayType typeb = (SymbolicArrayType) array_b.type();

		if (!typea.elementType().equals(typeb.elementType()))
			throw new SARLException("First two arguments of permut predicate: " + array_a + ", " + array_b
					+ " have inconsistent element types: " + typea.elementType() + ", " + typeb.elementType() + ".");

		SymbolicExpression result = ReservedFunctions.permutation(this, expressionFactory, typea.elementType());

		return (BooleanExpression) apply(result, Arrays.asList(array_a, array_b, low, high));
	}

	@Override
	public boolean isPermutCall(SymbolicExpression expr) {
		return ReservedFunctions.isPermutCall(expr);
	}

	@Override
	public SymbolicExpression valueSetTemplate(SymbolicType valueType, ValueSetReference[] vsRefs) {
		return expressionFactory.valueSetTemplate(valueType, vsRefs);
	}

	@Override
	public SymbolicType valueSetTemplateType() {
		return expressionFactory.valueSetTemplateType();
	}

	@Override
	public Iterable<ValueSetReference> valueSetReferences(SymbolicExpression valueSetTemplate) {
		if (!expressionFactory.isValueSetTemplateType(valueSetTemplate.type()))
			throw new SARLException(
					"the argument to method 'valueSetReferences' must have value " + "set template type");

		SymbolicExpression arr = (SymbolicExpression) valueSetTemplate.argument(1);

		assert arr.operator() == ARRAY;

		@SuppressWarnings("unchecked")
		Iterable<ValueSetReference> args = (Iterable<ValueSetReference>) arr.getArguments();

		return (Iterable<ValueSetReference>) args;
	}

	@Override
	public SymbolicType valueType(SymbolicExpression valueSetTemplate) {
		if (!expressionFactory.isValueSetTemplateType(valueSetTemplate.type()))
			throw new SARLException("the argument to method 'valueType' must have value set " + "template type");
		return getValueTypeOfValueSetTemplate(valueSetTemplate);
	}

	@Override
	public BooleanExpression valueSetContains(SymbolicExpression vst0, SymbolicExpression vst1) {
		checkValueSetOperandsCompatiable(vst0, vst1, "contains");

		SymbolicType valueType = getValueTypeOfValueSetTemplate(vst0);
		SymbolicExpression refArray0 = tupleRead(vst0, intObject(1)), refArray1 = tupleRead(vst1, intObject(1));

		return expressionFactory.valueSetContains(valueType, refArray0, refArray1);
	}

	@Override
	public BooleanExpression valueSetNoIntersect(SymbolicExpression vst0, SymbolicExpression vst1) {
		checkValueSetOperandsCompatiable(vst0, vst1, "no_intersect");

		SymbolicType valueType = getValueTypeOfValueSetTemplate(vst0);
		SymbolicExpression refArray0 = tupleRead(vst0, intObject(1)), refArray1 = tupleRead(vst1, intObject(1));

		/*
		 * conjunct over i : 0 .. length(refArray0)-1 conjunct over i : 0 ..
		 * length(refArray0)-1 valueSetReferenceNoIntersect(refArray0[i],
		 * refArray[1][j])
		 */
		assert refArray0.operator() == ARRAY;
		assert refArray1.operator() == ARRAY;

		BooleanExpression result = trueExpr;

		for (SymbolicObject ref0 : refArray0.getArguments())
			for (SymbolicObject ref1 : refArray1.getArguments()) {
				BooleanExpression tmp = expressionFactory.valueSetRefereceNoIntersect(valueType,
						(ValueSetReference) ref0, (ValueSetReference) ref1);

				result = and(result, tmp);
			}

		return result;
	}

	@Override
	public SymbolicExpression valueSetDiff(SymbolicExpression vst0, SymbolicExpression vst1) {
		checkValueSetOperandsCompatiable(vst0, vst1, "diff");

		SymbolicType valueType = getValueTypeOfValueSetTemplate(vst0);
		SymbolicExpression refArray0 = tupleRead(vst0, intObject(1)), refArray1 = tupleRead(vst1, intObject(1));
		return expressionFactory.valueSetDiff(valueType, refArray0, refArray1);
	}

	@Override
	public SymbolicExpression valueSetUnion(SymbolicExpression vst0, SymbolicExpression vst1) {
		checkValueSetOperandsCompatiable(vst0, vst1, "union");

		SymbolicType valueType = getValueTypeOfValueSetTemplate(vst0);
		SymbolicExpression refArray0 = tupleRead(vst0, intObject(1)), refArray1 = tupleRead(vst1, intObject(1));
		ValueSetReference[] union = new ValueSetReference[refArray0.numArguments() + refArray1.numArguments()];
		int i = 0;

		for (SymbolicObject e : refArray0.getArguments())
			union[i++] = (ValueSetReference) e;
		for (SymbolicObject e : refArray1.getArguments())
			union[i++] = (ValueSetReference) e;
		return valueSetTemplate(valueType, (ValueSetReference[]) union);
	}

	@Override
	public Pair<SymbolicExpression, Integer> valueSetHavoc(SymbolicExpression value,
			SymbolicExpression valueSetTemplate, String prefix, int startCount) {
		SymbolicType rootType = getValueTypeOfValueSetTemplate(valueSetTemplate);
		SymbolicExpression result = value;
		int[] count = { startCount };

		for (SymbolicObject ref : tupleRead(valueSetTemplate, intObject(1)).getArguments()) {
			LinkedList<ValueSetReference> refStack = new LinkedList<>();
			ValueSetReference vsRef = (ValueSetReference) ref;

			while (!vsRef.isIdentityReference()) {
				refStack.push(vsRef);
				vsRef = ((NTValueSetReference) vsRef).getParent();
			}
			if (refStack.isEmpty())
				return new Pair<>(freshMemHavocSymbolicConstant(rootType, prefix, count), count[0]);
			result = valueSetHavocWorker(result, refStack, prefix, count);
		}
		return new Pair<>(result, count[0]);
	}

	/*
	 * Defers creating a fresh symbolic constant to havoc with until we either reach
	 * a leaf ValueSetReference or we reach an array section (because at this point,
	 * we have a set of memory locations and can no longer defer)
	 */
	private SymbolicExpression valueSetHavocWorker(SymbolicExpression value, LinkedList<ValueSetReference> vsRefStack,
			String prefix, int[] count) {
		ValueSetReference vsRef = vsRefStack.pop();

		switch (vsRef.valueSetReferenceKind()) {
		case ARRAY_ELEMENT: {
			NumericExpression index = ((VSArrayElementReference) vsRef).getIndex();
			SymbolicArrayType arrayType = (SymbolicArrayType) value.type();
			SymbolicExpression newElement = vsRefStack.isEmpty()
					? freshMemHavocSymbolicConstant(arrayType.elementType(), prefix, count)
					: valueSetHavocWorker(arrayRead(value, index), vsRefStack, prefix, count);
			return arrayWrite(value, index, newElement);
		}
		case ARRAY_SECTION: {
			vsRefStack.push(vsRef);
			return valueSetAssignsWorker(value, new LinkedList<>(vsRefStack),
					freshMemHavocSymbolicConstant(value.type(), prefix, count));
		}
		case TUPLE_COMPONENT: {
			IntObject idx = ((VSTupleComponentReference) vsRef).getIndex();
			SymbolicTupleType tupleType = (SymbolicTupleType) value.type();
			SymbolicExpression newElement = vsRefStack.isEmpty()
					? freshMemHavocSymbolicConstant(tupleType.sequence().getType(idx.getInt()), prefix, count)
					: valueSetHavocWorker(tupleRead(value, idx), vsRefStack, prefix, count);
			return tupleWrite(value, idx, newElement);
		}
		case UNION_MEMBER:
			IntObject idx = ((VSUnionMemberReference) vsRef).getIndex();
			SymbolicUnionType unionType = (SymbolicUnionType) value.type();
			SymbolicExpression newElement = vsRefStack.isEmpty()
					? freshMemHavocSymbolicConstant(unionType.sequence().getType(idx.getInt()), prefix, count)
					: valueSetHavocWorker(unionExtract(idx, value), vsRefStack, prefix, count);
			return unionInject(unionType, idx, newElement);
		case OFFSET:
			throw new SARLException(
					"unsupported value set reference kind " + vsRef.valueSetReferenceKind() + " for value set assign");
		case IDENTITY:
		default:
			throw new SARLException("unreachable");
		}
	}

	private SymbolicConstant freshMemHavocSymbolicConstant(SymbolicType valueType, String prefix, int[] count) {
		return symbolicConstant(stringObject(prefix + count[0]++), valueType);
	}

	@Override
	public SymbolicExpression valueSetAssigns(SymbolicExpression oldValue, SymbolicExpression valueSetTemplate,
			SymbolicExpression newValue) {
		if (!oldValue.type().equals(newValue.type()))
			throw new SARLException(
					"the oldValue: " + oldValue + " has a different type from the newValue: " + newValue);
		if (!expressionFactory.isValueSetTemplateType(valueSetTemplate.type()))
			throw new SARLException(
					"the given value set template: " + valueSetTemplate + " does not have a value set template type");

		SymbolicExpression result = oldValue;
		for (SymbolicObject ref : tupleRead(valueSetTemplate, intObject(1)).getArguments()) {
			LinkedList<ValueSetReference> refStack = new LinkedList<>();
			ValueSetReference vsRef = (ValueSetReference) ref;

			while (!vsRef.isIdentityReference()) {
				refStack.push(vsRef);
				vsRef = ((NTValueSetReference) vsRef).getParent();
			}
			result = valueSetAssignsWorker(result, refStack, newValue);
		}
		return result;
	}

	/**
	 * Recursive worker method for
	 * {@link #valueSetAssigns(SymbolicExpression, SymbolicExpression, SymbolicExpression)}
	 */
	private SymbolicExpression valueSetAssignsWorker(SymbolicExpression oldValue,
			LinkedList<ValueSetReference> vsRefStack, SymbolicExpression newValue) {
		if (vsRefStack.isEmpty())
			return newValue;

		ValueSetReference vsRef = vsRefStack.pop();

		switch (vsRef.valueSetReferenceKind()) {
		case ARRAY_ELEMENT: {
			NumericExpression index = ((VSArrayElementReference) vsRef).getIndex();
			if (!((SymbolicArrayType) oldValue.type()).isComplete()) {
				throw new SARLException(MEM_ASSIGN_INCOMPLETE_ERROR_MSG);
			}
			SymbolicExpression newElement = valueSetAssignsWorker(arrayRead(oldValue, index), vsRefStack,
					arrayRead(newValue, index));

			return arrayWrite(oldValue, index, newElement);
		}
		case ARRAY_SECTION: {
			VSArraySectionReference ref = (VSArraySectionReference) vsRef;
			NumericExpression lower = ref.lowerBound();
			NumericExpression upper = ref.upperBound();
			NumericExpression step = ref.step();
			SymbolicArrayType arrayType = (SymbolicArrayType) oldValue.type();
			if (!arrayType.isComplete())
				throw new SARLException(MEM_ASSIGN_INCOMPLETE_ERROR_MSG);
			SymbolicCompleteArrayType completeArrayType = (SymbolicCompleteArrayType) arrayType;

			boolean sectionIsWholeArray = false;

			// 1. optimization case: the section is the whole array and
			// there is
			// no sub-array
			if (lower.isZero() && upper.equals(completeArrayType.extent()) && step.isOne()) {
				sectionIsWholeArray = true;
				if (vsRefStack.isEmpty())
					return newValue;
			}
			// 2. optimization case: if the section is concrete
			IntegerNumber lowerNum, upperNum, stepNum;

			lowerNum = (IntegerNumber) extractNumber(lower);
			upperNum = (IntegerNumber) extractNumber(upper);
			stepNum = (IntegerNumber) extractNumber(step);
			if (lowerNum != null && upperNum != null && stepNum != null) {
				int upperInt = upperNum.intValue(), stepInt = stepNum.intValue();

				for (int i = lowerNum.intValue(); i < upperInt; i += stepInt) {
					NumericExpression idx = integer(i);

					oldValue = arrayWrite(oldValue, idx, valueSetAssignsWorker(arrayRead(oldValue, idx),
							new LinkedList<>(vsRefStack), arrayRead(newValue, idx)));
				}
				return oldValue;
			}
			// 3. general case: array lambda
			int bvSuffix = 0;
			NumericSymbolicConstant bv;
			Set<SymbolicConstant> scSet = oldValue.getFreeVars();
			BooleanExpression inRange;
			SymbolicExpression lambda;
			// create a local BoundCleaner since bound variables don't need
			// to
			// be globally different:
			BoundCleaner cleaner = new BoundCleaner(this, objectFactory, typeFactory);
			SymbolicExpression newSection = newValue;

			oldValue = cleaner.apply(oldValue);
			newSection = cleaner.apply(newSection);
			scSet.addAll(newSection.getFreeVars());
			do {
				bv = (NumericSymbolicConstant) symbolicConstant(stringObject("i" + bvSuffix++), integerType());
			} while (scSet.contains(bv));
			inRange = and(lessThanEquals(lower, bv), lessThan(bv, upper));
			inRange = and(inRange, equals(modulo(subtract(bv, lower), step), zeroInt()));

			SymbolicExpression newElementValue, oldElementValue;

			newElementValue = arrayRead(newSection, bv);
			oldElementValue = arrayRead(oldValue, bv);
			newSection = valueSetAssignsWorker(oldElementValue, vsRefStack, newElementValue);

			if (newSection == newElementValue && sectionIsWholeArray)
				/*
				 * "newPart == newElementValue" means that the WHOLE generic element
				 * "newSection[i]" is ALL referred by the rest of the VSReferences. Then in such
				 * a case, if "sectionIsWholeArray" is true, the whole array is assigned by the
				 * "newValue".
				 */
				return newValue;
			lambda = cond(inRange, newSection, arrayRead(oldValue, bv));
			lambda = lambda(bv, lambda);
			return arrayLambda(completeArrayType, lambda);
		}
		case TUPLE_COMPONENT: {
			IntObject idx = ((VSTupleComponentReference) vsRef).getIndex();

			newValue = valueSetAssignsWorker(tupleRead(oldValue, idx), vsRefStack, tupleRead(newValue, idx));
			return tupleWrite(oldValue, idx, newValue);
		}
		case UNION_MEMBER:
			IntObject idx = ((VSUnionMemberReference) vsRef).getIndex();

			newValue = valueSetAssignsWorker(unionExtract(idx, oldValue), vsRefStack, unionExtract(idx, newValue));
			return unionInject((SymbolicUnionType) oldValue.type(), idx, newValue);
		case OFFSET:
			throw new SARLException(
					"unsupported value set reference kind " + vsRef.valueSetReferenceKind() + " for value set assign");
		case IDENTITY:
		default:
			throw new SARLException("unreachable");
		}
	}

	@Override
	public VSIdentityReference vsIdentityReference() {
		return expressionFactory.vsIdentityReference();
	}

	@Override
	public VSArrayElementReference vsArrayElementReference(ValueSetReference parent, NumericExpression index) {
		return expressionFactory.vsArrayElementReference(parent, index);
	}

	@Override
	public VSArraySectionReference vsArraySectionReference(ValueSetReference parent, NumericExpression lower,
			NumericExpression upper) {
		return expressionFactory.vsArraySectionReference(parent, lower, upper, oneInt());
	}

	@Override
	public VSArraySectionReference vsArraySectionReference(ValueSetReference parent, NumericExpression lower,
			NumericExpression upper, NumericExpression step) {
		return expressionFactory.vsArraySectionReference(parent, lower, upper, step);
	}

	@Override
	public VSTupleComponentReference vsTupleComponentReference(ValueSetReference parent, IntObject fieldIndex) {
		return expressionFactory.vsTupleComponentReference(parent, fieldIndex);
	}

	@Override
	public VSUnionMemberReference vsUnionMemberReference(ValueSetReference parent, IntObject memberIndex) {
		return expressionFactory.vsUnionMemberReference(parent, memberIndex);
	}

	@Override
	public VSOffsetReference vsOffsetReference(ValueSetReference parent, NumericExpression offset) {
		return expressionFactory.vsOffsetReference(parent, offset);
	}

	/**
	 * <p>
	 * Given a value set template, returns the symbolic value type that is
	 * associated with the value set template.
	 * </p>
	 * 
	 * @param vst a value set template expression
	 * @return a symbolic type that is associated with the given "vst"
	 */
	protected SymbolicType getValueTypeOfValueSetTemplate(SymbolicExpression vst) {
		SymbolicFunctionType funcType = (SymbolicFunctionType) ((SymbolicExpression) ((SymbolicExpression) vst
				.argument(0)).argument(0)).type();

		return funcType.inputTypes().getType(0);
	}

	/**
	 * <p>
	 * Given two operands of value set template type, check if they are compatible
	 * for value set template operations.
	 * </p>
	 *
	 * <p>
	 * </p>
	 * 
	 * @param vst0 a value set template expression
	 * @param vst1 a value set template expression
	 * @param op   a string which is a pretty representation of the operation that
	 *             will be used for error reporting
	 */
	private void checkValueSetOperandsCompatiable(SymbolicExpression vst0, SymbolicExpression vst1, String op) {
		SymbolicType type0 = vst0.type();
		SymbolicType type1 = vst1.type();

		if (!expressionFactory.isValueSetTemplateType(type0))
			throw new SARLException(
					"Operand " + vst0 + " of value set " + op + " operation does not have value set template type.");
		if (!expressionFactory.isValueSetTemplateType(type1))
			throw new SARLException(
					"Operand " + vst1 + " of value set " + op + " operation does not have value set template type.");

		SymbolicType valueType0 = getValueTypeOfValueSetTemplate(vst0);
		SymbolicType valueType1 = getValueTypeOfValueSetTemplate(vst1);

		if (!valueType0.equals(valueType1))
			throw new SARLException("Value set templates of types: " + type0 + " and " + type1
					+ " are not compatiable for operation: " + op);
	}

	@Override
	public SymbolicType valueSetReferenceType() {
		return expressionFactory.valueSetReferenceType();
	}
}
