package dev.civl.sarl.prove.smt;

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

import dev.civl.sarl.IF.SARLInternalException;
import dev.civl.sarl.IF.TheoremProverException;
import dev.civl.sarl.IF.config.ProverInfo.ProverKind;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.NumericSymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;
import dev.civl.sarl.IF.number.IntegerNumber;
import dev.civl.sarl.IF.number.Number;
import dev.civl.sarl.IF.number.RationalNumber;
import dev.civl.sarl.IF.object.BooleanObject;
import dev.civl.sarl.IF.object.CharObject;
import dev.civl.sarl.IF.object.IntObject;
import dev.civl.sarl.IF.object.NumberObject;
import dev.civl.sarl.IF.object.SymbolicObject;
import dev.civl.sarl.IF.object.SymbolicSequence;
import dev.civl.sarl.IF.type.SymbolicArrayType;
import dev.civl.sarl.IF.type.SymbolicCompleteArrayType;
import dev.civl.sarl.IF.type.SymbolicFunctionType;
import dev.civl.sarl.IF.type.SymbolicFunctionType.SpecialRelationKind;
import dev.civl.sarl.IF.type.SymbolicTupleType;
import dev.civl.sarl.IF.type.SymbolicType;
import dev.civl.sarl.IF.type.SymbolicType.SymbolicTypeKind;
import dev.civl.sarl.IF.type.SymbolicTypeSequence;
import dev.civl.sarl.IF.type.SymbolicUninterpretedType;
import dev.civl.sarl.IF.type.SymbolicUnionType;
import dev.civl.sarl.preuniverse.IF.PreUniverse;
import dev.civl.sarl.prove.IF.ProverFunctionInterpretation;
import dev.civl.sarl.util.FastList;
import dev.civl.sarl.util.Pair;

/**
 * <p>
 * Translates SARL {@link SymbolicExpression}s to SMT-LIB v.2. See
 * {@link https://smt-lib.org}.
 * </p>
 * 
 * Notes on SMT-LIB:
 * 
 * <pre>
   Basic commands:  (assert expr) and (check-sat)
   To check if predicate is valid under context:
     (assert context)
     (assert (not predicate))
     (check-sat)
   If result is "sat": answer is NO (not valid)
   If result is "unsat": answer is YES (valid)
   Otherwise, MAYBE
   
   Basic Types: Int Bool Real
   Constants: true false integers, decimals, fractions can be written (/ a b)
   Boolean operations: and or not ite =>
   Symbolic constants: (declare-const all1 (Array Int Int))
   Symbolic constants of functional type: (declare-fun f (Int Bool) Int)
   
   Tuples...
     (declare-datatype T1 ((mk-T1 (proj_0 Int) (proj_1 Real))))
     (declare-datatype T2 ((mk-T2 (proj_0 Real) (proj_1 Int)))) 
     (simplify (mk-T1 2 3.1))
     (simplify (proj_0 (mk-T1 2 3.1)))
     (simplify (= (mk-T1 2 3.1) (mk-T1 2 3.1)))
     update(t, i, new_val) is equivalent to mk_tuple(proj_0(t), ..., new_val, ..., proj_n(t))
     
   Parameterized algebraic datatype...
     (declare-datatype Lst (par (E) ((nil) (cons (car E) (cdr (Lst E))))))
     (simplify (cons 1 (as nil (Lst Int))))
     (simplify (car (cons 1 (as nil (Lst Int)))))
     
   Arrays:
     (declare-const a1 (Array Int Int))
     (select a1 10)
     (store a1 10 100)
  
   Constant arrays: this works for CVC5 and Z3 though I can't find it in the SMT-LIB
     documentation.   It appears the value (1, in this example) has to be a constant
     for CVC5.  Maybe it's better to not use this.  Instead use the standard select.
     
     (declare-const all1 (Array Int Int))
     (assert (= all1 ((as const (Array Int Int)) 1)))
     (simplify (select all1 10))
   
   Functions: there is declare-fun, define-fun, define-fun-rec, and define-funs-rec.
   
   Lambdas expressions: coming in SMT-LIB 2.7 as follows:
     (lambda ((x Real) (y Real))
       (ite (not (= y 0.0))
            (/ x y)
            0.0))
     Above is supported in Z3 but not yet CVC5 1.3.3.
  
   Array lambdas:
    No support. Just make an assertion that says the elements of the array are
    equal to the evaluation of the function.   This works in outermost scope.
   
   Union types: example: union of Int and Real:
     (declare-datatype U1 (
       (inject_1_0 (extract_1_0 Int))
       (inject_1_1 (extract_1_1 Real))))
     (simplify (inject_1_0 34))
     (simplify ((_ is inject_1_0) (inject_1_0 34)))
     (simplify (is-inject_1_0 (inject_1_0 34)))
   Note: the first form of the tester (_ is inject_1_0) is what is described in the
   SMT-LIB spec 2.6.  But the second one seems to work also in both Z3 and CVC5.
   
   Integer operations: See https://smt-lib.org/theories-Ints.shtml
     +, - (unary and binary), *, div, mod, ** (exponentiation), abs
     
   Casting Int to Real: (to_real x)
   
   First order quantifiers:
     (forall ((x Int)) (p x))
     (forall ((x Int) (y Real)) (blah x y))
     (exists ...)
     (let ((x t1) (y t2)) (blah x y)) ; note: parallel assignment
   
   Uninterpreted type t: a unique algebraic datatype with one int field.
   Symbolic expressions of type t with {@link SymbolicOperator#CONCRETE}
   operator will be translated using the constructor Cons_t as
   (Const_t key). Selector Select_key_t will never be used for now.

 * </pre>
 * 
 * @author Stephen F. Siegel
 */
public class SMTTranslator {

	/**
	 * When n is at or below this threshold, expressions of the form base^n will be
	 * translated using repeated multiplication: (let ((tmp base)) (* tmp tmp ...
	 * tmp)). TODO: move these somewhere else?
	 */
	public static final int EXP_THRESHOLD = 10;

	/**
	 * If the size of the context or a predicate exceeds this threshold, this
	 * translator is working in a compressed way.
	 */
	public static final int FULL_EXPR_SIZE_THRESHOLD = 100;

	/**
	 * If the size of a single symbolic expression exceeds this threshold, it will
	 * be translated into a binding and keep being used in a compressed way.
	 */
	public static final int SINGLE_EXPR_SIZE_THRESHOLD = 10;

	/**
	 * The length of bit-vector represents an integer. TODO: this constant should be
	 * obtained from somewhere else.
	 */
	private String BITLEN_INT = "32";

	/**
	 * The kind of SMT theorem prover this translator targets. E.g., Z3, CVC5, etc.
	 * There are some differences in the languages they accept.
	 */
	private ProverKind proverKind;

	/**
	 * The symbolic universe used to create and manipulate SARL symbolic
	 * expressions.
	 */
	private PreUniverse universe;

	/**
	 * The number of auxiliary SMT variables created. These are the variables that
	 * do not correspond to any SARL variable but are needed for some reason to
	 * translate an expression. Includes both ordinary and bound SMT variables.
	 */
	private int smtAuxVarCount;

	/**
	 * The number of auxiliary SARL variables created. These are used for integer
	 * index variables for expressing array equality.
	 */
	private int sarlAuxVarCount;

	/**
	 * Mapping of SARL symbolic expression to corresponding SMT expression. Used to
	 * cache the results of translation.
	 */
	private Map<SymbolicExpression, FastList<String>> expressionMap;

	/**
	 * Mapping of pairs (t1,t2) of SARL types to the uninterpreted function symbol
	 * which represents casting from t1 to t2. The function has type "function from
	 * translate(t1) to translate(t2)".
	 */
	private Map<Pair<SymbolicType, SymbolicType>, String> castMap;

	/**
	 * The set of names of SARL symbolic constants which have been translated to
	 * SMT.
	 */
	private Set<String> variableSet;

	/**
	 * Mapping of SARL symbolic type to corresponding SMT type. Used to cache
	 * results of translation.
	 */
	private Map<SymbolicType, FastList<String>> typeMap;

	/**
	 * A set of pairs of declared SMT function names and their declaration texts.
	 * Used to avoid duplicated function declarations. SMT allows functions of
	 * different types to share the same function name.
	 */
	protected Set<Pair<String, String>> functionSet;

	/**
	 * Has the "bigArray" type been defined?
	 */
	private boolean bigArrayDefined = false;

	/**
	 * Has the function which computes x^y, where x and y are integers, been
	 * defined?
	 */
	private boolean powIntIntDefined = false;

	/**
	 * Has the function which computes x^y, where x is real and y is an integer,
	 * been defined?
	 */
	private boolean powRealIntDefined = false;

	/**
	 * Has the function which computes x^y, where x and y are reals, been defined?
	 */
	private boolean powRealRealDefined = false;

	/**
	 * Has the function which computes the i-th root of a real number x been defined
	 * (where i is an integer)?
	 */
	private boolean rootDefined = false;

	/**
	 * The declarations section resulting from the translation. This contains all
	 * the declarations of symbols used in the resulting SMT input. Also contains
	 * side assertions that were added in the process of translation.
	 */
	private FastList<String> smtDeclarations;

	/**
	 * The SMT expression which is the result of translating the given symbolic
	 * expression.
	 */
	private FastList<String> smtTranslation;

	/**
	 * If the translated expression is a conjunction (a boolean {@link
	 * SymbolicOperator#AND} with more than one clause), this holds the translation
	 * of each individual clause, in order. It is <code>null</code> otherwise. This
	 * allows a caller (e.g., a theorem prover writing an SMT-LIB script) to emit
	 * one <code>(assert ...)</code> per clause rather than a single giant
	 * conjunction. Each entry is the raw translation of a clause, <em>without</em>
	 * the shared sub-expression <code>let</code>-bindings; use
	 * {@link #getConjunctTranslations()} to obtain assertable, binding-wrapped
	 * clauses.
	 */
	private List<FastList<String>> conjunctTranslations = null;

	/**
	 * A map that maps {@link SymbolicExpression}s to temporary binding names so
	 * that they can be reused. The translation is then processed in a compressed
	 * way. If this map is instantiated, this translator is working in this
	 * compressed way.
	 */
	private Map<SymbolicExpression, FastList<String>> subExpressionsBindingNames = null;

	/**
	 * All binding translations. Eventually, these bindings will be added on the
	 * head of the translation as <code>(let (bindings) (translation))</code>
	 */
	private List<FastList<String>> subExpressionBindings = null;

	/**
	 * A stack of bound variables. A new entry will be pushed onto this stack once
	 * the translation enters a quantified expression, and a top entry is popped
	 * once the translation finishes a quantified expression.
	 */
	private Stack<SymbolicConstant> boundVariableStack = new Stack<>();

	/**
	 * The flag controls whether the translation should be done with a compressed
	 * form (if the size of the formula exceeds some threshold). By default it is
	 * on.
	 */
	private boolean enableCompression = true;

	// Constructors...

	public SMTTranslator(PreUniverse universe, ProverKind proverKind, SymbolicExpression theExpression,
			ProverFunctionInterpretation logicFunctions[]) {
		this.universe = universe;
		this.proverKind = proverKind;
		this.smtAuxVarCount = 0;
		this.sarlAuxVarCount = 0;
		this.expressionMap = new HashMap<>();
		this.castMap = new HashMap<>();
		this.variableSet = new HashSet<>();
		this.typeMap = new HashMap<>();
		this.functionSet = new HashSet<>();
		this.smtDeclarations = new FastList<>();
		if (theExpression.size() >= FULL_EXPR_SIZE_THRESHOLD) {
			this.subExpressionsBindingNames = new HashMap<>();
			this.subExpressionBindings = new LinkedList<>();
		}
		// translate logic functions:
		for (ProverFunctionInterpretation logicFunction : logicFunctions)
			translateLogicFunction(logicFunction);
		translateTopLevel(theExpression);
	}

	public SMTTranslator(SMTTranslator startingContext, SymbolicExpression theExpression) {
		this.universe = startingContext.universe;
		this.proverKind = startingContext.proverKind;
		this.smtAuxVarCount = startingContext.smtAuxVarCount;
		this.sarlAuxVarCount = startingContext.sarlAuxVarCount;
		this.castMap = new HashMap<>(startingContext.castMap);
		this.typeMap = new HashMap<>(startingContext.typeMap);
		this.functionSet = new HashSet<>(startingContext.functionSet);
		this.expressionMap = new HashMap<>(startingContext.expressionMap);
		this.variableSet = new HashSet<>(startingContext.variableSet);
		if (theExpression.size() >= FULL_EXPR_SIZE_THRESHOLD || startingContext.subExpressionBindings != null) {
			this.subExpressionsBindingNames = new HashMap<>();
			this.subExpressionBindings = new LinkedList<>();
			// add bindings from context to this translation since some binding
			// symbols created in context will be used again:
			if (startingContext.subExpressionBindings != null)
				this.subExpressionBindings.addAll(startingContext.subExpressionBindings);
		}
		this.bigArrayDefined = startingContext.bigArrayDefined;
		this.smtDeclarations = new FastList<>();
		this.smtTranslation = translate(theExpression);
	}

	/**
	 * Translates the top-level expression, and, if it is a conjunction (a boolean
	 * {@link SymbolicOperator#AND} with more than one clause), also records the
	 * translation of each individual clause in {@link #conjunctTranslations}. Each
	 * clause is translated exactly once; the combined translation
	 * (<code>(and c1 c2 ... cn)</code>) is assembled from the clause translations
	 * so that {@link #getTranslation()} continues to return the whole conjunction.
	 * Because all clauses are translated by this same translator instance, their
	 * declarations and shared sub-expression bindings accumulate correctly.
	 *
	 * @param theExpression the expression being translated (typically the context)
	 */
	private void translateTopLevel(SymbolicExpression theExpression) {
		if (theExpression.operator() == SymbolicOperator.AND && theExpression.numArguments() > 1) {
			int n = theExpression.numArguments();
			List<FastList<String>> clauses = new ArrayList<>(n);
			FastList<String> combined = new FastList<>("(", "and");

			for (int i = 0; i < n; i++) {
				FastList<String> clause = translate((SymbolicExpression) theExpression.argument(i));

				clauses.add(clause);
				combined.add(" ");
				combined.append(clause.clone());
			}
			combined.add(")");
			this.conjunctTranslations = clauses;
			this.smtTranslation = combined;
		} else {
			this.smtTranslation = translate(theExpression);
		}
	}

	// Private methods...

	private void requireBigArray() {
		if (!bigArrayDefined) {
			smtDeclarations.add(
					"(declare-datatype BigArray (par (T) ((mk-BigArray (bigArray-len Int) (bigArray-val (Array Int T))))))\n");
			bigArrayDefined = true;
		}
	}

	private String tupleTypeName(SymbolicTupleType tupleType) {
		return "Tuple-" + tupleType.name().getString();
	}

	private String tupleConstructor(SymbolicTupleType tupleType) {
		return "mk-" + tupleTypeName(tupleType);
	}

	private String tupleProjector(SymbolicTupleType tupleType, int index) {
		return "proj-" + tupleTypeName(tupleType) + "_" + index;
	}

	private String unionTypeName(SymbolicUnionType unionType) {
		return "Union-" + unionType.name().getString();
	}

	private String uninterpretedTypeName(SymbolicUninterpretedType uninterpretedType) {
		return "Unintpret-" + uninterpretedType.name().getString();
	}

	private String uninterpretedTypeConstructor(SymbolicUninterpretedType uninterpretedType) {
		return "Cons-" + uninterpretedType.name().getString();
	}

	/**
	 * Computes the name of the index-th selector function into a union type. This
	 * is the function that takes an element of the union and returns an element of
	 * the index-th member type.
	 * 
	 * @param unionType a union type
	 * @param index     integer in [0,n), where n is the number of member types of
	 *                  the union type
	 * @return the name of the index-th selector function
	 */
	private String unionSelector(SymbolicUnionType unionType, int index) {
		return unionTypeName(unionType) + "_extract_" + index;
	}

	/**
	 * Computes the name of the index-th constructor function for a union type. This
	 * is the function which takes as input an element of the index-th member type
	 * and returns an element of the union type.
	 * 
	 * @param unionType a union type
	 * @param index     an integer in [0,n), where n is the number of member types
	 *                  of the union type
	 * @return the name of the index-th constructor function
	 */
	private String unionConstructor(SymbolicUnionType unionType, int index) {
		return unionTypeName(unionType) + "_inject_" + index;
	}

	private String unionTester(SymbolicUnionType unionType, int index) {
		return "(_ is " + unionConstructor(unionType, index) + ")";

	}

	/**
	 * Returns a fresh identifier for an SMT variable. Increments
	 * {@link #smtAuxVarCount}. This does not declare the variable or have any other
	 * side-effects.
	 * 
	 * @return the name of the new variable
	 */
	protected String newSmtVarName() {
		return "_smt" + (smtAuxVarCount++);
	}

	/**
	 * Creates a new SMT (ordinary) variable of given type with unique name;
	 * increments {@link #smtAuxVarCount}. CANNOT be used for a function type.
	 * 
	 * @param type an SMT type; it is consumed, so cannot be used after invoking
	 *             this method
	 * @return the new SMT variable
	 */
	protected String newSmtAuxVar(FastList<String> type) {
		String name = newSmtVarName();
		smtDeclarations.addAll("(declare-const ", name);
		type.addFront(" ");
		smtDeclarations.append(type);
		smtDeclarations.add(")\n");
		smtAuxVarCount++;
		return name;
	}

	/**
	 * Returns a new SARL symbolic constant of integer type. Increments
	 * {@link #sarlAuxVarCount}.
	 * 
	 * @return new symbolic constant of integer type
	 */
	protected NumericSymbolicConstant newSarlAuxVar() {
		NumericSymbolicConstant result = (NumericSymbolicConstant) universe
				.symbolicConstant(universe.stringObject("_i" + sarlAuxVarCount), universe.integerType());

		sarlAuxVarCount++;
		return result;
	}

	/**
	 * Creates "big array" expression: an ordered pair consisting of an integer
	 * expression which is the length of the array, and an expression of array type
	 * which is the contents.
	 * 
	 * @param length SMT expression yielding length of array; it is consumed (so
	 *               cannot be used after invoking this method)
	 * @param value  SMT expression of type "array-of-T"; it is consumed (so cannot
	 *               be used after invoking this method)
	 * @return ordered pair (tuple), consisting of length and value
	 */
	private FastList<String> bigArray(FastList<String> length, FastList<String> value) {
		FastList<String> result = new FastList<String>();
		requireBigArray();
		result.addAll("(mk-BigArray ");
		result.append(length);
		result.add(" ");
		result.append(value);
		result.add(")");
		return result;
	}

	/**
	 * <p>
	 * Given a SARL expression of array type, this method computes the SMT
	 * representation of the length of that array. This is an SMT expression of
	 * integer type.
	 * </p>
	 * 
	 * <p>
	 * Convergence criterion: this method calls {@link #translate}, and
	 * {@link #translate} calls this method. In order for the recursion to
	 * terminate, the following protocol must be followed: {@link #translate} should
	 * never call this method on its entire argument; it should only call this
	 * method on a proper sub-tree of its argument.
	 * </p>
	 * 
	 * @param array a SARL expression of array type
	 * @return translation into SMT of length of that array
	 */
	private FastList<String> lengthOfArray(SymbolicExpression array) {
		SymbolicArrayType type = (SymbolicArrayType) array.type();

		// imagine translating "length(a)" for a symbolic constant a.
		// this calls lengthOfArray(a). This calls translate(a).
		// Since a is a symbolic constant, this yields an SMT symbolic
		// constant A. The result returned is "(A).0".

		if (type instanceof SymbolicCompleteArrayType)
			return translate(((SymbolicCompleteArrayType) type).extent());
		// there are three kinds of array expressions for which translation
		// results in a literal ordered pair [int,array]: SEQUENCE,
		// ARRAY_WRITE, DENSE_ARRAY_WRITE. A concrete (SEQUENCE) array
		// always has complete type.
		switch (array.operator()) {
		case ARRAY:
			throw new SARLInternalException("Unreachable");
		case ARRAY_WRITE:
		case DENSE_ARRAY_WRITE:
			return lengthOfArray((SymbolicExpression) array.argument(0));
		default:
			FastList<String> result = new FastList<>("(bigArray-len ");
			result.append(translate(array));
			result.add(")");
			return result;
		}
	}

	private FastList<String> pretranslateConcreteArray(SymbolicExpression array) {
		SymbolicCompleteArrayType arrayType = (SymbolicCompleteArrayType) array.type();
		SymbolicType elementType = arrayType.elementType();
		NumericExpression extentExpression = arrayType.extent();
		IntegerNumber extentNumber = (IntegerNumber) universe.extractNumber(extentExpression);
		int size = array.numArguments();
		FastList<String> smtArrayType = new FastList<>("(Array Int ");

		smtArrayType.append(translateType(elementType));
		smtArrayType.add(")");
		assert extentNumber != null && extentNumber.intValue() == size;

		FastList<String> result = new FastList<>(newSmtAuxVar(smtArrayType));

		for (int i = 0; i < size; i++) {
			result.addFront("(store ");
			result.addAll(" ", Integer.toString(i), " ");
			result.append(translate((SymbolicExpression) array.argument(i)));
			result.add(")");
		}
		return result;
	}

	private FastList<String> pretranslateArrayWrite(SymbolicExpression arrayWrite) {
		// syntax: (store a index value)
		SymbolicExpression arrayExpression = (SymbolicExpression) arrayWrite.argument(0);
		NumericExpression indexExpression = (NumericExpression) arrayWrite.argument(1);
		SymbolicExpression valueExpression = (SymbolicExpression) arrayWrite.argument(2);
		FastList<String> result = new FastList<>("(store ");

		result.append(valueOfArray(arrayExpression));
		result.add(" ");
		result.append(translate(indexExpression));
		result.add(" ");
		result.append(translate(valueExpression));
		result.add(")");
		return result;
	}

	private FastList<String> pretranslateDenseArrayWrite(SymbolicExpression denseArrayWrite) {
		// syntax: lots of nested stores
		SymbolicExpression arrayExpression = (SymbolicExpression) denseArrayWrite.argument(0);
		SymbolicSequence<?> elements = (SymbolicSequence<?>) denseArrayWrite.argument(1);
		int n = elements.size();
		FastList<String> result = valueOfArray(arrayExpression);

		for (int i = 0; i < n; i++) {
			SymbolicExpression element = elements.get(i);

			if (!element.isNull()) {
				result.addFront("(store ");
				result.addAll(" ", Integer.toString(i), " ");
				result.append(translate(element));
				result.add(")");
			}
		}
		return result;
	}

	/**
	 * Given a SARL expression of array type, this method computes the SMT
	 * representation of array type corresponding to that array. The result will be
	 * an SMT expression of type array-of-T, where T is the element type.
	 * 
	 * @param array symbolic expression of array type
	 * @return SMT representation of array expression
	 */
	private FastList<String> valueOfArray(SymbolicExpression array) {
		// the idea is to catch any expression which would be translated
		// as an explicit ordered pair [len,val] and return just the val.
		// for expressions that are not translated to an explicit
		// ordered pair, just apply bigArray-val to get the array value
		// component.
		switch (array.operator()) {
		case ARRAY:
			return pretranslateConcreteArray(array);
		case ARRAY_WRITE:
			return pretranslateArrayWrite(array);
		case DENSE_ARRAY_WRITE:
			return pretranslateDenseArrayWrite(array);
		default: {
			FastList<String> result = new FastList<>("(bigArray-val ");
			result.append(translate(array));
			result.add(")");
			return result;
		}
		}
	}

	/**
	 * Translates a concrete SARL array into SMT-LIB.
	 * 
	 * @param arrayType a SARL complete array type
	 * @param elements  a sequence of elements whose types are all the element type
	 *                  of the arrayType
	 * @return SMT-LIB translation of the concrete array
	 */
	private FastList<String> translateConcreteArray(SymbolicExpression array) {
		FastList<String> result = pretranslateConcreteArray(array);
		int size = array.numArguments();

		result = bigArray(new FastList<>(Integer.toString(size)), result);
		return result;
	}

	private FastList<String> translateConcreteTuple(SymbolicExpression tuple) {
		// syntax: (mk-T e0 e1 ...)
		FastList<String> result;
		SymbolicTupleType type = (SymbolicTupleType) tuple.type();
		int n = tuple.numArguments();

		// declare the tuple type if you haven't already
		translateType(type);
		result = new FastList<String>("(" + tupleConstructor(type));
		for (int i = 0; i < n; i++) {
			SymbolicExpression member = (SymbolicExpression) tuple.argument(i);
			result.add(" ");
			result.append(translate(member));
		}
		result.add(")");
		return result;
	}

	private String translateBigAsInt(BigInteger big) {
		return big.signum() < 0 ? "(- " + big.abs() + ")" : big.toString();
	}

	private String translateBigAsReal(BigInteger big) {
		return big.signum() < 0 ? "(- " + big.abs() + ".0)" : big.toString() + ".0";
	}

	private FastList<String> translateNumberObj(NumberObject obj) {
		Number num = obj.getNumber();
		String str;
		if (num instanceof IntegerNumber) {
			str = translateBigAsInt(((IntegerNumber) num).bigIntegerValue());
		} else {
			RationalNumber rat = (RationalNumber) num;
			BigInteger numerator = rat.numerator(), denominator = rat.denominator();

			if (denominator.equals(BigInteger.ONE))
				str = translateBigAsReal(numerator);
			else
				str = "(/ " + translateBigAsReal(numerator) + " " + translateBigAsReal(denominator) + ")";
		}
		return new FastList<>(str);
	}

	/**
	 * Translates any concrete SymbolicExpression with concrete type to equivalent
	 * SMT-LIB expression using the ExprManager.
	 * 
	 * @param expr any symbolic expression of kind CONCRETE
	 * @return the SMT-LIB equivalent expression
	 */
	private FastList<String> translateConcrete(SymbolicExpression expr) {
		SymbolicType type = expr.type();
		SymbolicTypeKind kind = type.typeKind();
		SymbolicObject object = expr.argument(0);
		FastList<String> result;

		switch (kind) {
		case BOOLEAN:
			result = new FastList<>(((BooleanObject) object).getBoolean() ? "true" : "false");
			break;
		case CHAR:
			result = new FastList<>(Integer.toString((int) ((CharObject) object).getChar()));
			break;
		case INTEGER:
		case REAL:
			return translateNumberObj((NumberObject) object);
		case UNINTERPRETED: {
			SymbolicUninterpretedType uintType = (SymbolicUninterpretedType) type;
			int key = uintType.soleSelector().apply(expr).getInt();

			translateType(uintType);
			result = new FastList<>("(", uninterpretedTypeConstructor(uintType), " ", String.valueOf(key), " )");
			break;
		}
		default:
			throw new SARLInternalException("Unknown concrete object: " + expr);
		}
		return result;

	}

	// need to return not only the function declaration, but in the case of special
	// binary relation types, also additional assertions.
	protected FastList<String> functionDeclaration(String name, SymbolicFunctionType functionType) {
		SpecialRelationKind relKind = functionType.specialRelationKind();
		FastList<String> result = new FastList<>("(declare-fun ", name, " (");
		boolean first = true;

		for (SymbolicType inputType : functionType.inputTypes()) {
			if (first)
				first = false;
			else
				result.add(" ");
			result.append(translateType(inputType));
		}
		result.add(") ");
		result.append(translateType(functionType.outputType()));
		result.add(")\n");
		Pair<String, String> key = new Pair<>(name, result.toString());
		if (functionSet.contains(key))
			return new FastList<>();
		functionSet.add(key);
		if (relKind != SpecialRelationKind.NONE) {
			SymbolicType type = functionType.inputTypes().getType(0);
			switch (relKind) {
			case PARTIAL_ORDER:
				result.append(partialOrderAssumption(name, type));
				break;
			case LINEAR_ORDER:
				result.append(linearOrderAssumption(name, type));
				break;
			case TREE_ORDER:
				result.append(treeOrderAssumption(name, type));
				break;
			case PIECEWISE_LINEAR_ORDER:
				result.append(piecewiseLinearOrderAssumption(name, type));
				break;
			case NONE:
			default:
				throw new RuntimeException("Unreachable");
			}
		}
		return result;
	}

	/**
	 * Produces:
	 * 
	 * <pre>
	  	(assert (forall ((x A)) (R x x)))
		(assert (forall ((x A) (y A)) (=> (and (R x y) (R y x)) (= x y))))
		(assert (forall ((x A) (y A) (z A)) (=> (and (R x y) (R y z)) (R x z))))
	 * </pre>
	 * 
	 * where R is name and A is translation of type.
	 */
	private FastList<String> partialOrderAssumption(String r, SymbolicType type) {
		String a = translateType(type).toString();
		String x = newSmtVarName(), y = newSmtVarName();
		FastList<String> result = new FastList<>(
				"(assert (forall ((" + x + " " + a + ")) (" + r + " " + x + " " + x + ")))\n");
		result.add("(assert (forall ((" + x + " " + a + ") (" + y + " " + a + ")) (=> (and (" + r + " " + x + " " + y
				+ ") (" + r + " " + y + " " + x + ")) (= " + x + " " + y + "))))\n");
		result.add("(assert (forall ((" + x + " " + a + ") (" + y + " " + a + ") (z " + a + ")) (=> (and (" + r + " "
				+ x + " " + y + ") (" + r + " " + y + " z)) (" + r + " " + x + " z))))\n");
		return result;
	}

	/**
	 * Produces: everything in partial order, plus
	 * 
	 * <pre>
	  	(assert (forall ((x A) (y A)) (or (R x y) (R y x))))
	 * </pre>
	 * 
	 * where R is name and A is translation of type.
	 */
	private FastList<String> linearOrderAssumption(String r, SymbolicType type) {
		String a = translateType(type).toString();
		FastList<String> result = partialOrderAssumption(r, type);
		String x = newSmtVarName(), y = newSmtVarName();
		result.add("(assert (forall ((" + x + " " + a + ") (" + y + " " + a + ")) (or (" + r + " " + x + " " + y + ") ("
				+ r + " " + y + " " + x + "))))\n");
		return result;
	}

	/**
	 * Produces: everything in partial order, plus
	 * 
	 * <pre>
		(assert (forall ((x A) (y A) (z A)) (=> (and (R y x) (R z x)) (or (R y z) (R z y)))))
	 * 
	 * </pre>
	 * 
	 * where R is name and A is translation of type.
	 */
	private FastList<String> treeOrderAssumption(String r, SymbolicType type) {
		String a = translateType(type).toString();
		FastList<String> result = partialOrderAssumption(r, type);
		String x = newSmtVarName(), y = newSmtVarName(), z = newSmtVarName();
		result.add("(assert (forall ((" + x + " " + a + ") (" + y + " " + a + ") (" + z + " " + a + ")) (=> (and (" + r
				+ " " + y + " " + x + ") (" + r + " " + z + " " + x + ")) (or (" + r + " " + y + " " + z + ") (" + r
				+ " " + z + " " + y + ")))))\n");
		return result;
	}

	/**
	 * Produces: everything in tree order, plus
	 * 
	 * <pre>
		(assert (forall ((x A) (y A) (z A)) (=> (and (R x y) (R x z)) (or (R y z) (R z y)))))
	 * 
	 * </pre>
	 * 
	 * where R is name and A is translation of type.
	 */
	private FastList<String> piecewiseLinearOrderAssumption(String r, SymbolicType type) {
		String a = translateType(type).toString();
		FastList<String> result = treeOrderAssumption(r, type);
		String x = newSmtVarName(), y = newSmtVarName(), z = newSmtVarName();
		result.add("(assert (forall ((" + x + " " + a + ") (" + y + " " + a + ") (" + z + " " + a + ")) (=> (and (" + r
				+ " " + x + " " + y + ") (" + r + " " + x + " " + z + ")) (or (" + r + " " + y + " " + z + ") (" + r
				+ " " + z + " " + y + ")))))\n");
		return result;
	}

	/**
	 * Translates a symbolic constant. It returns simply the name of the symbolic
	 * constant (in the form of a <code>FastList</code> of strings). For an ordinary
	 * (i.e., not quantified) symbolic constant, this method also adds to
	 * {@link #smtDeclarations} a declaration of the symbolic constant.
	 * 
	 * @param symbolicConstant a SARL symbolic constant
	 * @param isBoundVariable  is this a bound variable?
	 * @return the name of the symbolic constant as a fast string list
	 */
	private FastList<String> translateSymbolicConstant(SymbolicConstant symbolicConstant, boolean isBoundVariable) {
		String name = symbolicConstant.name().getString();
		FastList<String> result = new FastList<>(name);
		SymbolicType symbolicType = symbolicConstant.type();

		if (symbolicType.typeKind() == SymbolicTypeKind.FUNCTION) {
			smtDeclarations.append(functionDeclaration(name, (SymbolicFunctionType) symbolicType));
		} else {
			if (!isBoundVariable && !variableSet.contains(name)) {
				FastList<String> smtType = translateType(symbolicType);

				smtDeclarations.addAll("(declare-const ", name, " ");
				smtDeclarations.append(smtType);
				smtDeclarations.add(")\n");
			}
		}
		this.variableSet.add(name);
		this.expressionMap.put(symbolicConstant, result);
		return result.clone();
	}

	/**
	 * There is no lambda expression in SMT-LIB v2.6, but you can use
	 * 
	 * <pre>
	 * (define-fun funcName ((x1 T1) (x2 T2) ...) T expr)
	 * </pre>
	 * 
	 * where T1, T2, ... are the input types, T is the output type, x1, x2, ..., are
	 * the formal parameters, and expr is the function body. This method creates a
	 * fresh function symbol, and adds it to {@link #smtDeclarations}. This only
	 * works in the outermost scope.
	 * 
	 * Note: lambda is supported in v2.7, but CVC5 1.3.3 still uses v2.6 standard.
	 * 
	 * @param lambdaExpression symbolic expression of kind
	 *                         {@link SymbolicOperator#LAMBDA}
	 * @return new function symbol representing the lambda
	 */
	private FastList<String> translateLambda(SymbolicExpression lambdaExpression) {
		int argsNum = lambdaExpression.numArguments();
		SymbolicFunctionType functionType = (SymbolicFunctionType) lambdaExpression.type();
		SymbolicExpression body = (SymbolicExpression) lambdaExpression.argument(argsNum - 1);
		String name = newSmtVarName();
		FastList<String> smtSymbolicConstants = new FastList<>();

		for (int i = 0; i < argsNum - 1; i++) {
			SymbolicConstant inputVar = (SymbolicConstant) lambdaExpression.argument(i);

			smtSymbolicConstants.add("(");
			smtSymbolicConstants.append(translateSymbolicConstant(inputVar, true));
			smtSymbolicConstants.add(" ");
			smtSymbolicConstants.append(translateType(inputVar.type()));
			smtSymbolicConstants.add(")");
			if (i != argsNum - 2) {
				smtSymbolicConstants.add(" ");
			}
		}
		FastList<String> smtOutputType = translateType(functionType.outputType());
		FastList<String> smtBody = translate(body);

		smtDeclarations.addAll("(define-fun ", name, "(");
		smtDeclarations.append(smtSymbolicConstants);
		smtDeclarations.add(") ");
		smtDeclarations.append(smtOutputType);
		smtDeclarations.add(" ");
		smtDeclarations.append(smtBody);
		smtDeclarations.add(")\n");
		return new FastList<>(name);
	}

	/**
	 * No array lambdas in SMT-LIB either. But at least in outermost scope, you can
	 * declare a fresh variable to have array type and assert each element is as
	 * specified. Hence this method has the side-effect of adding to the
	 * {@link #smtDeclarations}.
	 * 
	 * @param arrayLambda the SARL expression of kind ARRAY_LAMBDA.
	 * @return expression translated to SMT-LIB
	 */
	private FastList<String> translateArrayLambda(SymbolicExpression arrayLambda) {
		NumericExpression length = universe.length(arrayLambda);
		SymbolicArrayType arrayType = (SymbolicArrayType) arrayLambda.type();
		SymbolicType elementType = arrayType.elementType();
		SymbolicExpression function = (SymbolicExpression) arrayLambda.argument(0);
		FastList<String> smtLength = translate(length);
		FastList<String> smtArrayType = new FastList<>("(Array Int ");
		smtArrayType.append(translateType(elementType));
		smtArrayType.add(")");
		String smtArrayVar = newSmtAuxVar(smtArrayType);
		NumericSymbolicConstant index = newSarlAuxVar();
		FastList<String> smtIndex = translateSymbolicConstant(index, true);
		FastList<String> assertion = new FastList<>(
				"(assert (forall ((" + smtIndex + " Int)) (=> (and (<= 0 " + smtIndex + ") (< " + smtIndex + " ");
		assertion.append(smtLength.clone());
		assertion.addAll(")) (= (select " + smtArrayVar + " " + smtIndex + ") ");
		assertion.append(translate(universe.apply(function, Arrays.asList(index))));
		assertion.add("))))\n");
		smtDeclarations.append(assertion);
		return bigArray(smtLength, new FastList<>(smtArrayVar));
	}

	/**
	 * Translates an array-read expression a[i] into equivalent SMT expression.
	 * Syntax: <code>(select a index)</code>.
	 * 
	 * @param expr a SARL symbolic expression of form a[i]
	 * @return an equivalent SMT expression
	 */
	private FastList<String> translateArrayRead(SymbolicExpression expr) {
		SymbolicExpression arrayExpression = (SymbolicExpression) expr.argument(0);
		NumericExpression indexExpression = (NumericExpression) expr.argument(1);
		FastList<String> result = new FastList<>("(select ");

		result.append(valueOfArray(arrayExpression));
		result.add(" ");
		result.append(translate(indexExpression));
		result.add(")");
		return result;
	}

	/**
	 * Translates a tuple-read expression t.i into equivalent SMT expression.
	 * 
	 * Recall: TUPLE_READ: 2 arguments: arg0 is the tuple expression. arg1 is an
	 * IntObject giving the index in the tuple.
	 * 
	 * @param expr a SARL symbolic expression of kind
	 *             {@link SymbolicOperator#TUPLE_READ}
	 * @return an equivalent SMT expression
	 */
	private FastList<String> translateTupleRead(SymbolicExpression expr) {
		// we can assume the tuple type has already been declared
		SymbolicExpression tupleExpression = (SymbolicExpression) expr.argument(0);
		int index = ((IntObject) expr.argument(1)).getInt();
		FastList<String> result = new FastList<>("(", tupleProjector((SymbolicTupleType) tupleExpression.type(), index),
				" ");

		result.append(translate(tupleExpression));
		result.add(")");
		return result;
	}

	/**
	 * Translates an array-write (or array update) SARL symbolic expression to
	 * equivalent SMT expression.
	 * 
	 * @param expr a SARL array expression of kind
	 *             {@link SymbolicOperator#ARRAY_WRITE}
	 * @return the result of translating to SMT
	 */
	private FastList<String> translateArrayWrite(SymbolicExpression expr) {
		FastList<String> result = pretranslateArrayWrite(expr);

		result = bigArray(lengthOfArray(expr), result);
		return result;
	}

	/**
	 * <p>
	 * Translates a tuple-write (or tuple update) SARL symbolic expression to
	 * equivalent SMT expression.
	 * </p>
	 * 
	 * <p>
	 * Recall: TUPLE_WRITE: 3 arguments: arg0 is the original tuple expression, arg1
	 * is an IntObject giving the index, arg2 is the new value to write into the
	 * tuple.
	 * </p>
	 * 
	 * <pre>
	 * update(t, i, new_val) is equivalent to
	 * (mk-TupleName (proj_0 t) ... new_val ... (proj_n t))
	 * </pre>
	 * 
	 * @param expr a SARL expression of kind {@link SymbolicOperator#TUPLE_WRITE}
	 * @return the result of translating to SMT
	 */
	private FastList<String> translateTupleWrite(SymbolicExpression expr) {
		SymbolicExpression tupleExpression = (SymbolicExpression) expr.argument(0);
		SymbolicTupleType tupleType = (SymbolicTupleType) tupleExpression.type();
		FastList<String> t = translate(tupleExpression);
		int index = ((IntObject) expr.argument(1)).getInt();
		SymbolicExpression valueExpression = (SymbolicExpression) expr.argument(2);
		int tupleLength = ((SymbolicTupleType) expr.type()).sequence().numTypes();
		FastList<String> result = new FastList<>(tupleConstructor(tupleType));

		for (int i = 0; i < tupleLength; i++) {
			result.add(" ");
			if (i == index) {
				result.append(translate(valueExpression));
			} else {
				result.addAll("(", tupleProjector(tupleType, i), " ");
				result.append(t.clone());
				result.add(")");
			}
		}
		result.add(")");
		return result;
	}

	/**
	 * Translates a multiple array-write (or array update) SARL symbolic expression
	 * to equivalent SMT expression.
	 * 
	 * @param expr a SARL expression of kind
	 *             {@link SymbolicOperator#DENSE_ARRAY_WRITE}
	 * @return the result of translating expr to SMT
	 */
	private FastList<String> translateDenseArrayWrite(SymbolicExpression expr) {
		FastList<String> result = pretranslateDenseArrayWrite(expr);

		result = bigArray(lengthOfArray(expr), result);
		return result;
	}

	/**
	 * <p>
	 * Translates a multiple tuple-write (or tuple update) SARL symbolic expression
	 * to equivalent SMT expression.
	 * </p>
	 * 
	 * <p>
	 * Syntax: <code>(mk-T e0 e1 ...)</code>, where <code>e</code>i is
	 * <code>(proj_i tup)</code> if there is no i-th element in the sequence or the
	 * i-th element in the sequence is NULL, otherwise the i-th element of the
	 * sequence.
	 * </p>
	 * 
	 * @param expr a SARL expression of kind
	 *             {@link SymbolicOperator#DENSE_TUPLE_WRITE}
	 * @return result of translating to SMT
	 */
	private FastList<String> translateDenseTupleWrite(SymbolicExpression expr) {
		SymbolicExpression tupleExpression = (SymbolicExpression) expr.argument(0);
		SymbolicTupleType tupleType = (SymbolicTupleType) tupleExpression.type();
		SymbolicSequence<?> values = (SymbolicSequence<?>) expr.argument(1);
		int numValues = values.size();
		FastList<String> origin = translate(tupleExpression);

		if (numValues == 0) {
			return origin;
		}

		FastList<String> result = new FastList<>("(", tupleConstructor(tupleType));

		result.append(translate(tupleExpression));
		for (int i = 0; i < numValues; i++) {
			SymbolicExpression value = values.get(i);

			result.add(" ");
			if (!value.isNull()) {
				result.append(translate(value));
			} else {
				result.addAll("(", tupleProjector(tupleType, i), " ");
				result.append(origin.clone());
				result.add(")");
			}
		}

		int tupleLength = tupleType.sequence().numTypes();

		for (int i = numValues; i < tupleLength; i++) {
			result.addAll("(", tupleProjector(tupleType, i), " ");
			result.append(origin.clone());
			result.add(")");
		}
		result.add(")");
		return result;
	}

	/**
	 * Translates SymbolicExpressions of the type "exists" and "forall" into the SMT
	 * equivalent.
	 * 
	 * @param expr a SARL "exists" or "forall" expression
	 * @return result of translating to SMT
	 */
	private FastList<String> translateQuantifier(SymbolicExpression expr) {
		// syntax: (forall ((x T)) expr)
		SymbolicOperator kind = expr.operator();
		SymbolicConstant boundVariable = (SymbolicConstant) expr.argument(0);
		BooleanExpression predicate = (BooleanExpression) expr.argument(1);
		FastList<String> result = new FastList<String>("(");

		boundVariableStack.push(boundVariable);
		switch (kind) {
		case FORALL:
			result.add("forall");
			break;
		case EXISTS:
			result.add("exists");
			break;
		default:
			throw new SARLInternalException("unreachable");
		}
		result.add(" ((");
		result.append(translateSymbolicConstant(boundVariable, true));
		result.add(" ");
		result.append(translateType(boundVariable.type()));
		result.add(")) ");
		result.append(translate(predicate));
		result.add(")");
		boundVariableStack.pop();
		return result;
	}

	/**
	 * Given two SARL symbolic expressions of compatible type, this returns the
	 * SMT-LIB translation of the assertion that the two expressions are equal.
	 * Special handling is needed for arrays, to basically say:
	 * 
	 * <pre>
	 * lengths are equal and forall i: 0<=i<length -> expr1[i]=expr2[i].
	 * </pre>
	 * 
	 * @param expr1 a SARL symbolic expression
	 * @param expr2 a SARL symbolic expression of type compatible with that of
	 *              <code>expr1</code>
	 * @return result of translating into SMT the assertion "expr1=expr2"
	 */
	private FastList<String> processEquality(SymbolicExpression expr1, SymbolicExpression expr2) {
		FastList<String> result;

		if (expr1.type().typeKind() == SymbolicTypeKind.ARRAY) {
			// lengths are equal and forall i (0<=i<length).a[i]=b[i]. Syntax:
			// (and (= len1 len2)
			// (forall ((i Int)) (=> (and (<= 0 i) (< i len1)) <rec-call>)))
			FastList<String> extent1 = lengthOfArray(expr1);
			NumericSymbolicConstant index = newSarlAuxVar();
			String indexString = index.name().getString();
			SymbolicExpression read1 = universe.arrayRead(expr1, index);
			SymbolicExpression read2 = universe.arrayRead(expr2, index);

			result = new FastList<>("(and (= ");
			result.append(extent1.clone());
			result.add(" ");
			result.append(lengthOfArray(expr2));
			result.addAll(") (forall ((", indexString, " Int)) (=> (and (<= 0 ", indexString, ") (< ", indexString,
					" ");
			result.append(extent1);
			result.add(")) ");
			result.append(processEquality(read1, read2));
			result.add(")))");
		} else {
			result = new FastList<>("(= ");
			result.append(translate(expr1));
			result.add(" ");
			result.append(translate(expr2));
			result.add(")");
		}
		return result;
	}

	/**
	 * Translates a SymbolicExpression that represents a = b into the SMT
	 * equivalent.
	 * 
	 * @param expr SARL symbolic expression with kind
	 *             {@link SymbolicOperator.EQUALS}
	 * @return the equivalent SMT
	 */
	private FastList<String> translateEquality(SymbolicExpression expr) {
		SymbolicExpression leftExpression = (SymbolicExpression) expr.argument(0);
		SymbolicExpression rightExpression = (SymbolicExpression) expr.argument(1);
		FastList<String> result = processEquality(leftExpression, rightExpression);

		return result;
	}

	/**
	 * <p>
	 * Translates a union-extract expression. The result has the form
	 * 
	 * <pre>
	 * (UT_extract_i y)
	 * </pre>
	 * 
	 * where <code>UT</code> is the name of the union type, <code>y</code> is the
	 * argument belonging to the union type, and <code>i</code> is the index
	 * argument.
	 * </p>
	 * 
	 * <p>
	 * UNION_EXTRACT: 2 arguments: arg0 is an IntObject giving the index of a member
	 * type of a union type; arg1 is a symbolic expression whose type is the union
	 * type. The resulting expression has type the specified member type. This
	 * essentially pulls the expression out of the union and casts it to the member
	 * type. If arg1 does not belong to the member type (as determined by a
	 * UNION_TEST expression), the value of this expression is undefined.
	 * </p>
	 * 
	 * <p>
	 * Note that the union type will be declared as follows:
	 * 
	 * <pre>
	 * (declare-datatype UT (
	 *     (UT-inject_0 (UT-extract_0 T0))
	 *     (UT-inject_1 (UT-extract_1 T1))
	 *     ...
	 *   ))
	 * </pre>
	 * 
	 * Usage:
	 * 
	 * <pre>
	 *   (UT-inject_i x)
	 *   (UT-extract_i y)
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param expr a "union extract" expression, kind
	 *             {@link SymbolicOperator#UNION_EXTRACT}
	 * @return result of translating to SMT
	 */
	private FastList<String> translateUnionExtract(SymbolicExpression expr) {
		int index = ((IntObject) expr.argument(0)).getInt();
		SymbolicExpression arg = (SymbolicExpression) expr.argument(1);
		SymbolicUnionType unionType = (SymbolicUnionType) arg.type();
		FastList<String> result = new FastList<>("(", unionSelector(unionType, index), " ");

		translateType(unionType); // create the decls if they aren't there
		result.append(translate(arg));
		result.add(")");
		return result;
	}

	/**
	 * <p>
	 * Translates a union-inject expression. The result has the form
	 * 
	 * <pre>
	 * (UT-inject_i x)
	 * </pre>
	 * 
	 * where <code>UT</code> is the name of the union type, <code>x</code> is the
	 * argument belonging to the member type, and <code>i</code> is the index
	 * argument.
	 * </p>
	 * 
	 * <p>
	 * UNION_INJECT: injects an element of a member type into a union type that
	 * includes that member type. 2 arguments: arg0 is an IntObject giving the index
	 * of the member type of the union type; arg1 is a symbolic expression whose
	 * type is the member type. The union type itself is the type of the
	 * UNION_INJECT expression.
	 * </p>
	 * 
	 * @param expr a "union inject" expression
	 * @return the SMT translation of that expression
	 */
	private FastList<String> translateUnionInject(SymbolicExpression expr) {
		int index = ((IntObject) expr.argument(0)).getInt();
		SymbolicExpression arg = (SymbolicExpression) expr.argument(1);
		SymbolicUnionType unionType = (SymbolicUnionType) expr.type();
		FastList<String> result = new FastList<>("(", unionConstructor(unionType, index), " ");

		translateType(unionType); // create the decls if they aren't there
		result.append(translate(arg));
		result.add(")");
		return result;
	}

	/**
	 * <p>
	 * Translates a union-test expression. The result has the form
	 * 
	 * <pre>
	 * ( (_ is UT-inject_i) y)
	 * </pre>
	 * 
	 * where <code>UT</code> is the name of the union type, <code>y</code> is the
	 * argument belonging to the union type, and <code>i</code> is the index
	 * argument.
	 * </p>
	 * 
	 * <p>
	 * UNION_TEST: 2 arguments: arg0 is an IntObject giving the index of a member
	 * type of the union type; arg1 is a symbolic expression whose type is the union
	 * type. This is a boolean-valued expression whose value is true iff arg1
	 * belongs to the specified member type of the union type.
	 * </p>
	 * 
	 * @param expr a "union test" expression
	 * @return the SMT translation of that expression
	 */
	private FastList<String> translateUnionTest(SymbolicExpression expr) {
		int index = ((IntObject) expr.argument(0)).getInt();
		SymbolicExpression arg = (SymbolicExpression) expr.argument(1);
		SymbolicUnionType unionType = (SymbolicUnionType) arg.type();
		FastList<String> result = new FastList<>("(", unionTester(unionType, index), " ");

		translateType(unionType); // create the decls if they aren't there
		result.append(translate(arg));
		result.add(")");
		return result;
	}

	private FastList<String> translateCast(SymbolicExpression expression) {
		SymbolicExpression argument = (SymbolicExpression) expression.argument(0);
		SymbolicType originalType = argument.type();
		SymbolicType newType = expression.type();

		if (originalType.equals(newType))
			return translate(argument);
		if (originalType.isInteger() && newType.isReal()) {
			FastList<String> result = new FastList<>("(to_real ");
			result.append(translate(argument));
			result.add(")");
			return result;
		}

		Pair<SymbolicType, SymbolicType> key = new Pair<>(originalType, newType);
		String castFunction = castMap.get(key);

		if (castFunction == null) {
			castFunction = "cast" + castMap.size();
			smtDeclarations.append(
					functionDeclaration(castFunction, universe.functionType(Arrays.asList(originalType), newType)));
			castMap.put(key, castFunction);
		}
		FastList<String> result = new FastList<>("(", castFunction, " ");
		result.append(translate(argument));
		result.add(")");
		return result;
	}

	private FastList<String> translateFloor(SymbolicExpression expression) {
		FastList<String> result = new FastList<String>("(to_int ");
		SymbolicSequence<?> arguments = (SymbolicSequence<?>) expression.argument(1);
		SymbolicExpression arg = arguments.getFirst();
		result.append(translate(arg));
		result.add(")");
		return result;
	}

	private FastList<String> translateApply(SymbolicExpression expression) {
		if (universe.isFloor(expression))
			return translateFloor(expression);

		SymbolicExpression function = (SymbolicExpression) expression.argument(0);
		SymbolicSequence<?> arguments = (SymbolicSequence<?>) expression.argument(1);
		FastList<String> result = new FastList<String>("(");

		result.append(translate(function));
		for (SymbolicExpression arg : arguments) {
			result.add(" ");
			result.append(translate(arg));
		}
		result.add(")");
		return result;
	}

	private FastList<String> translateNegative(SymbolicExpression expression) {
		FastList<String> result = new FastList<>("(- ");

		result.append(translate((SymbolicExpression) expression.argument(0)));
		result.add(")");
		return result;
	}

	private FastList<String> translateNEQ(SymbolicExpression expression) {
		FastList<String> result = new FastList<>("(not ");

		result.append(processEquality((SymbolicExpression) expression.argument(0),
				(SymbolicExpression) expression.argument(1)));
		result.add(")");
		return result;
	}

	private FastList<String> translateNot(SymbolicExpression expression) {
		FastList<String> result = new FastList<>("(not ");

		result.append(translate((SymbolicExpression) expression.argument(0)));
		result.add(")");
		return result;
	}

	private String powIntInt() {
		String name = "_powIntInt";
		if (!powIntIntDefined) {
			smtDeclarations.add("(define-fun-rec " + name + " ((x Int) (y Int)) Int\n" + "  (ite (<= y 0) 1 (* x ("
					+ name + " x (- y 1)))))\n");
			powIntIntDefined = true;
		}
		return name;
	}

	private String powRealInt() {
		String name = "_powRealInt";
		if (!powRealIntDefined) {
			smtDeclarations.add("(define-fun-rec _powRealIntAux ((x Real) (y Int)) Real\n"
					+ "  (ite (<= y 0) 1.0 (* x (_powRealIntAux x (- y 1)))))\n" + "(define-fun " + name
					+ " ((x Real) (y Int)) Real\n" + "  (ite (>= y 0)\n" + "       (_powRealIntAux x y)\n"
					+ "       (/ 1.0 (_powRealIntAux x (- y)))))\n");
			powRealIntDefined = true;
		}
		return name;
	}

	private String root() {
		String name = "_root";
		if (!rootDefined) {
			smtDeclarations.add("(declare-fun " + name + " (Int Real) Real)\n");
			smtDeclarations.add("(assert (forall ((n Int) (x Real))\n"
					+ "  (=> (and (>= n 2) (= (mod n 2) 0) (>= x 0.0))\n" + "      (and (= (" + powRealInt() + " ("
					+ name + " n x) n) x)\n" + "           (>= (" + name + " n x) 0.0)))))\n");
			smtDeclarations.add("(assert (forall ((n Int) (x Real))\n" + "  (=> (and (>= n 1) (= (mod n 2) 1))\n"
					+ "      (= (" + powRealInt() + " (" + name + " n x) n) x))))\n");
			rootDefined = true;
		}
		return name;
	}

	private String powRealReal() {
		String name = "_powRealReal";
		if (!powRealRealDefined) {
			smtDeclarations.add("(declare-fun " + name + " (Real Real) Real)\n");
			// TODO: add some axioms about this function...
			powRealRealDefined = true;
		}
		return name;
	}

	protected FastList<String> translatePowerAsCarrot(FastList<String> base, FastList<String> exp) {
		FastList<String> result = new FastList<>("(^ ");
		result.append(base);
		result.add(" ");
		result.append(exp);
		result.add(")");
		return result;
	}

	/**
	 * An exponent is either a SymbolicExpression (of integer or real type) or a
	 * {@link NumberObject} wrapping a concrete positive int.
	 * 
	 * @param exp exponent from a POWER expression
	 * @return translation to SMT
	 */
	protected FastList<String> translateExponent(SymbolicObject obj) {
		return obj instanceof NumberObject ? translateNumberObj((NumberObject) obj)
				: translate((SymbolicExpression) obj);
	}

	protected Number extractConcreteNumber(SymbolicObject obj) {
		Number num = null;
		if (obj instanceof NumberObject) {
			num = ((NumberObject) obj).getNumber();
		} else if (obj instanceof NumericExpression) {
			num = universe.extractNumber((NumericExpression) obj);
		}
		return num;
	}

	protected BigInteger extractConcreteInt(Number num) {
		if (num == null)
			return null;
		if (num instanceof IntegerNumber)
			return ((IntegerNumber) num).bigIntegerValue();
		else {
			RationalNumber rat = (RationalNumber) num;
			return rat.denominator().equals(BigInteger.ONE) ? rat.numerator() : null;
		}
	}

	protected FastList<String> translatePowerSmall(SymbolicExpression base, BigInteger expBig) {
		// base^1 = base:
		if (expBig.equals(BigInteger.ONE))
			return translate(base);
		// small concrete positive integer exponent:
		if (expBig.signum() == 1 && expBig.compareTo(BigInteger.valueOf(EXP_THRESHOLD)) <= 0) {
			int expInt = expBig.intValue();
			String tmpVar = newSmtVarName();
			FastList<String> result = new FastList<>("(let ((" + tmpVar + " ");
			result.append(translate(base));
			result.add(")) (*");
			for (int i = 0; i < expInt; i++)
				result.add(" " + tmpVar);
			result.add("))");
			return result;
		}
		return null;
	}

	protected FastList<String> translatePowerGeneric(SymbolicExpression base, SymbolicObject exponent) {
		Number expNum = extractConcreteNumber(exponent);
		BigInteger expBig = extractConcreteInt(expNum);
		FastList<String> result = new FastList<>("(");
		if (expBig != null || ((SymbolicExpression) exponent).type().isInteger()) {
			// exponent has integer type: use powIntInt or powRealInt
			if (base.type().isInteger()) {
				result.add(powIntInt());
			} else {
				result.add(powRealInt());
			}
			result.append(translate(base));
			result.add(" ");
			result.append(translateExponent(exponent));
		} else if (expNum != null) { // exponent is a concrete rational
			assert base.type().isReal();
			RationalNumber rat = (RationalNumber) expNum;
			BigInteger numerator = rat.numerator(), denominator = rat.denominator();
			result.add(root() + " " + denominator + " ");
			result.append(translate(base));
			if (!numerator.equals(BigInteger.ONE)) {
				FastList<String> result2 = new FastList<>("(" + powRealInt() + " (");
				result2.append(result);
				result2.add(") " + numerator.toString());
				result = result2;
			}
		} else { // exponent is real but not a concrete rational
			assert base.type().isReal();
			result.add(powRealReal() + " ");
			result.append(translate(base));
			result.add(" ");
			result.append(translateExponent(exponent));
		}
		result.add(")");
		return result;
	}

	/**
	 * Power operation: x to the y-th power. This is a generic implementation using
	 * only standard SMT-LIB features.
	 */
	protected FastList<String> translatePower(SymbolicExpression expression) {
		SymbolicExpression base = (SymbolicExpression) expression.argument(0);
		SymbolicObject exponent = expression.argument(1);
		BigInteger expBig = extractConcreteInt(extractConcreteNumber(exponent));
		if (expBig != null) {
			FastList<String> result = translatePowerSmall(base, expBig);
			if (result != null)
				return result;
		}
		return translatePowerGeneric(base, exponent);

	}

	private FastList<String> translateCond(SymbolicExpression expression) {
		// syntax: (ite b x y)
		FastList<String> result = new FastList<>("(ite ");
		result.append(translate((SymbolicExpression) expression.argument(0)));
		result.add(" ");
		result.append(translate((SymbolicExpression) expression.argument(1)));
		result.add(" ");
		result.append(translate((SymbolicExpression) expression.argument(2)));
		result.add(")");
		return result;
	}

	/**
	 * This is used to translate an expression that involves applying a binary
	 * operator to a list of arguments. The argument list can be empty. Examples
	 * include +, *, AND, OR. In each case, there is a default value, which is the
	 * identify for the operation (e.g., 0, 1, true, false).
	 */
	private FastList<String> translateKeySet(String operator, String defaultValue, SymbolicExpression expression) {
		int size = expression.numArguments();

		if (size == 0) {
			return new FastList<>(defaultValue);
		} else if (size == 1) {
			return translate((SymbolicExpression) expression.argument(0));
		} else {
			FastList<String> result = new FastList<>("(", operator);

			for (int i = 0; i < size; i++) {
				SymbolicExpression term = (SymbolicExpression) expression.argument(i);

				result.add(" ");
				result.append(translate(term));
			}
			result.add(")");
			return result;
		}
	}

	private FastList<String> translateBinary(String operator, SymbolicExpression arg0, SymbolicExpression arg1) {
		FastList<String> result = new FastList<>("(", operator, " ");

		result.append(translate(arg0));
		result.addAll(" ");
		result.append(translate(arg1));
		result.add(")");
		return result;
	}

	/**
	 * Translates a SARL symbolic expression to SMT.
	 * 
	 * @param expression a non-null SymbolicExpression
	 * @return translation to SMT as a fast list of strings
	 */
	private FastList<String> translateWork(SymbolicExpression expression) throws TheoremProverException {
		SymbolicOperator operator = expression.operator();
		FastList<String> result;

		switch (operator) {
		case ADD:
			result = translateKeySet("+", "0", expression);
			break;
		case AND:
			result = translateKeySet("and", "true", expression);
			break;
		case APPLY:
			result = translateApply(expression);
			break;
		case ARRAY:
			result = translateConcreteArray(expression);
			break;
		case ARRAY_LAMBDA:
			result = translateArrayLambda(expression);
			break;
		case ARRAY_READ:
			result = translateArrayRead(expression);
			break;
		case ARRAY_WRITE:
			result = translateArrayWrite(expression);
			break;
		case BIT_AND:
			result = translateBitBinary("bvand", (SymbolicExpression) expression.argument(0),
					(SymbolicExpression) expression.argument(1));
			break;
		case BIT_NOT:
			result = translateBitUnary("bvnot", (SymbolicExpression) expression.argument(0));
			break;
		case BIT_OR:
			result = translateBitBinary("bvor", (SymbolicExpression) expression.argument(0),
					(SymbolicExpression) expression.argument(1));
			break;
		case BIT_XOR:
			result = translateBitBinary("bvxor", (SymbolicExpression) expression.argument(0),
					(SymbolicExpression) expression.argument(1));
			break;
		case BIT_SHIFT_LEFT:
			result = translateBitBinary("bvshl", (SymbolicExpression) expression.argument(0),
					(SymbolicExpression) expression.argument(1));
			break;
		case BIT_SHIFT_RIGHT:
			result = translateBitBinary("bvlshr", (SymbolicExpression) expression.argument(0),
					(SymbolicExpression) expression.argument(1));
			break;
		case CAST:
			result = translateCast(expression);
			break;
		case CONCRETE:
			result = translateConcrete(expression);
			break;
		case COND:
			result = translateCond(expression);
			break;
		case DENSE_ARRAY_WRITE:
			result = translateDenseArrayWrite(expression);
			break;
		case DENSE_TUPLE_WRITE:
			result = translateDenseTupleWrite(expression);
			break;
		case DIVIDE: // real division
			result = translateBinary("/", (SymbolicExpression) expression.argument(0),
					(SymbolicExpression) expression.argument(1));
			break;
		case EQUALS:
			result = translateEquality(expression);
			break;
		case EXISTS:
		case FORALL:
			result = translateQuantifier(expression);
			break;
		case INT_DIVIDE:
			result = translateBinary("div", (SymbolicExpression) expression.argument(0),
					(SymbolicExpression) expression.argument(1));
			break;
		case LENGTH:
			result = lengthOfArray((SymbolicExpression) expression.argument(0));
			break;
		case LESS_THAN:
			result = translateBinary("<", (SymbolicExpression) expression.argument(0),
					(SymbolicExpression) expression.argument(1));
			break;
		case LESS_THAN_EQUALS:
			result = translateBinary("<=", (SymbolicExpression) expression.argument(0),
					(SymbolicExpression) expression.argument(1));
			break;
		case MODULO:
			result = translateBinary("mod", (SymbolicExpression) expression.argument(0),
					(SymbolicExpression) expression.argument(1));
			break;
		case MULTIPLY:
			result = translateKeySet("*", "1", expression);
			break;
		case NEGATIVE:
			result = translateNegative(expression);
			break;
		case NEQ:
			result = translateNEQ(expression);
			break;
		case NOT:
			result = translateNot(expression);
			break;
		case OR:
			result = translateKeySet("or", "false", expression);
			break;
		case POWER:
			result = translatePower(expression);
			break;
		case SUBTRACT:
			result = translateBinary("-", (SymbolicExpression) expression.argument(0),
					(SymbolicExpression) expression.argument(1));
			break;
		case SYMBOLIC_CONSTANT:
			result = translateSymbolicConstant((SymbolicConstant) expression, false);
			break;
		case TUPLE:
			result = translateConcreteTuple(expression);
			break;
		case TUPLE_READ:
			result = translateTupleRead(expression);
			break;
		case TUPLE_WRITE:
			result = translateTupleWrite(expression);
			break;
		case UNION_EXTRACT:
			result = translateUnionExtract(expression);
			break;
		case UNION_INJECT:
			result = translateUnionInject(expression);
			break;
		case UNION_TEST:
			result = translateUnionTest(expression);
			break;
		case LAMBDA:
			result = translateLambda(expression);
			break;
		case NULL:
			result = null;
			break;
		case DERIV:
		case DIFFERENTIABLE: {
			// just create fresh symbolic constants
			FastList<String> smtType = translateType(expression.type());
			String name = newSmtAuxVar(smtType.clone());
			result = new FastList<String>(name);
			break;
		}
		default:
			throw new SARLInternalException("unreachable: unknown operator: " + operator);
		}
		return result;
	}

	private FastList<String> translateBitUnary(String operator, SymbolicExpression arg0) {
		FastList<String> result = new FastList<>("((_ bv2int " + BITLEN_INT + ") (", operator);

		result.add(" ((_ int2bv " + BITLEN_INT + ") ");
		result.append(translate(arg0));
		result.add(")))");
		return result;
	}

	private FastList<String> translateBitBinary(String operator, SymbolicExpression arg0, SymbolicExpression arg1) {
		FastList<String> result = new FastList<>("((_ bv2int " + BITLEN_INT + ") (", operator);

		result.add(" ((_ int2bv " + BITLEN_INT + ") ");
		result.append(translate(arg0));
		result.add(") ((_ int2bv " + BITLEN_INT + ") ");
		result.append(translate(arg1));
		result.add(")))");
		return result;
	}

	protected FastList<String> translateType(SymbolicType type) {
		FastList<String> result = typeMap.get(type);

		if (result != null)
			return result.clone();

		SymbolicTypeKind kind = type.typeKind();

		switch (kind) {
		case BOOLEAN:
			result = new FastList<>("Bool");
			break;
		case INTEGER:
		case CHAR:
			result = new FastList<>("Int");
			break;
		case REAL:
			result = new FastList<>("Real");
			break;
		case ARRAY: {
			SymbolicArrayType arrayType = (SymbolicArrayType) type;
			requireBigArray();
			result = new FastList<>("(BigArray ");
			result.append(translateType(arrayType.elementType()));
			result.add(")");
			break;
		}
		case TUPLE: {
			SymbolicTupleType tupleType = (SymbolicTupleType) type;
			SymbolicTypeSequence sequence = tupleType.sequence();
			int numTypes = sequence.numTypes();
			String typeName = tupleTypeName(tupleType);

			// before doing anything translate the member types,
			// because these could modify smtDeclarations.
			for (SymbolicType memberType : sequence)
				translateType(memberType);

			smtDeclarations.addAll("(declare-datatype ", typeName, " ((", tupleConstructor(tupleType));

			for (int i = 0; i < numTypes; i++) {
				SymbolicType memberType = sequence.getType(i);

				smtDeclarations.addAll(" (", tupleProjector(tupleType, i), " ");
				smtDeclarations.append(translateType(memberType));
				smtDeclarations.add(")");
			}
			smtDeclarations.add(")))\n");
			result = new FastList<>(typeName);
			break;
		}
		case FUNCTION: {
			throw new TheoremProverException("SMT-LIB does not have a function type");
		}
		case UNION: {
			/**
			 * <pre>
			 * 			 (declare-datatype UT (
			 * 			     (UT-inject_0 (UT-extract_0 T0))
			 * 			     (UT-inject_1 (UT-extract_1 T1))
			 * 			     ...
			 * 			  ))
			 * </pre>
			 */
			SymbolicUnionType unionType = (SymbolicUnionType) type;
			String typeName = unionTypeName(unionType);
			SymbolicTypeSequence sequence = unionType.sequence();
			int n = sequence.numTypes();

			// before doing anything translate the member types, because these could modify
			// smtDeclarations.
			for (SymbolicType memberType : sequence)
				translateType(memberType);

			smtDeclarations.addAll("(declare-datatype ", typeName, " (");
			for (int i = 0; i < n; i++) {
				SymbolicType memberType = sequence.getType(i);

				smtDeclarations.addAll("\n    (", unionConstructor(unionType, i), " (", unionSelector(unionType, i),
						" ");
				smtDeclarations.append(translateType(memberType));
				smtDeclarations.add("))");
			}
			smtDeclarations.add("\n))\n");
			result = new FastList<>(typeName);
			break;
		}
		case UNINTERPRETED: {
			SymbolicUninterpretedType uninterpretedType = (SymbolicUninterpretedType) type;
			String typeName = uninterpretedTypeName(uninterpretedType);
			String consName = uninterpretedTypeConstructor(uninterpretedType);
			String typeDef = "((" + consName + " (Selector-" + uninterpretedType.name() + " Int)))";

			smtDeclarations.addAll("(declare-datatype ", typeName, typeDef, ")\n");
			result = new FastList<>(typeName);
			break;
		}
		default:
			throw new SARLInternalException("Unknown SARL type: " + type);
		}
		typeMap.put(type, result);
		return result.clone();
	}

	protected FastList<String> translate(SymbolicExpression expression) throws TheoremProverException {
		FastList<String> result = expressionMap.get(expression);

		if (result == null) {
			result = translateWork(expression);
			expressionMap.put(expression, result);
			if (useCompressedName(expression)) {
				// in compressed translation mode:
				result = translateExpression2binding(expression, result);
			}
		} else if (useCompressedName(expression)) {
			// expression has been translated but has no alias (when the context
			// translator is reused for translating predicate):
			result = subExpressionsBindingNames.get(expression);
			if (result == null)
				result = translateExpression2binding(expression, expressionMap.get(expression));
		}
		return result.clone();
	}

	private boolean useCompressedName(SymbolicExpression expression) {
		return subExpressionsBindingNames != null && expression.size() > SINGLE_EXPR_SIZE_THRESHOLD
				&& boundVariableStack.isEmpty() && enableCompression;
	}

	/**
	 * For a translated sub-expression, creating an alias for it. The aliasing is
	 * implemented using <code>(let binding term)</code>.
	 */
	private FastList<String> translateExpression2binding(SymbolicExpression expression, FastList<String> translation) {
		// in compressed translation mode:
		FastList<String> tmpVarName = new FastList<>(newSmtVarName());
		FastList<String> binding = letTempVarRepresentExpression(tmpVarName.clone(), translation.clone());
		subExpressionBindings.add(binding);
		subExpressionsBindingNames.put(expression, tmpVarName.clone());
		return tmpVarName;
	}

	/**
	 * Add a alias binding for the sub-expression: <code>(symbol term)</code>
	 */
	private FastList<String> letTempVarRepresentExpression(FastList<String> var, FastList<String> subExpr) {
		FastList<String> result = new FastList<String>();

		result.add("(");
		result.append(var);
		result.add(" ");
		result.append(subExpr);
		result.add(") ");
		return result;
	}

	// Exported methods...

	/**
	 * Returns the result of translating the symbolic expression specified at
	 * construction into the SMT-LIB language. The result is returned as a
	 * {@link FastList}. The elements of that list are Strings, which, concatenated,
	 * yield the translation result. In most cases you never want to convert the
	 * result to a single string. Rather, you should iterate over this list,
	 * printing each element to the appropriate output stream.
	 * 
	 * @return result of translation of the specified symbolic expression
	 */
	public FastList<String> getTranslation() {
		if (subExpressionBindings != null)
			return wrapWithBindings(smtTranslation.clone());
		return smtTranslation;
	}

	/**
	 * Returns the translation split into its top-level conjunctive clauses: one
	 * {@link FastList} per clause of the translated expression, each ready to be
	 * asserted on its own (i.e., with the shared sub-expression
	 * <code>let</code>-bindings wrapped around it). If the translated expression is
	 * not a conjunction, the returned list contains a single element equal to
	 * {@link #getTranslation()}.
	 *
	 * <p>
	 * This lets a caller emit one <code>(assert clause)</code> per clause of a path
	 * condition instead of a single <code>(assert (and ...))</code>, which is
	 * convenient for, e.g., attaching <code>:pattern</code> triggers to quantified
	 * clauses.
	 * </p>
	 *
	 * @return a non-empty list of assertable clause translations
	 */
	public List<FastList<String>> getConjunctTranslations() {
		List<FastList<String>> result = new ArrayList<>();

		if (conjunctTranslations != null) {
			for (FastList<String> clause : conjunctTranslations)
				result.add(wrapWithBindings(clause.clone()));
		} else {
			result.add(getTranslation());
		}
		return result;
	}

	/**
	 * Wraps the given translated body with this translator's shared sub-expression
	 * <code>let</code>-bindings, if any, producing a self-contained assertable
	 * term: <code>(let (b1) (let (b2) ... body ...))</code>. If there are no
	 * bindings, the body is returned unchanged.
	 *
	 * @param body a translated term; ownership is transferred to this method (it
	 *             may be consumed/appended), so pass a clone if you need to keep it
	 * @return the body wrapped with the shared bindings
	 */
	private FastList<String> wrapWithBindings(FastList<String> body) {
		if (subExpressionBindings == null)
			return body;

		FastList<String> result = new FastList<>();
		FastList<String> suffixes = new FastList<>();

		for (FastList<String> binding : subExpressionBindings) {
			result.add("(let (");
			result.append(binding.clone());
			result.add(") ");
			suffixes.add(")");
		}
		result.add(" ");
		result.append(body);
		result.append(suffixes);
		return result;
	}

	/**
	 * Returns the text of the declarations of the SMT-LIB symbols that occur in the
	 * translated expression. Typically, the declarations are submitted to the
	 * prover first, followed by a query or assertion of the translated expression.
	 * 
	 * @return the declarations of the SMT-LIB symbols
	 */
	public FastList<String> getDeclarations() {
		return smtDeclarations;
	}

	/**
	 * The translation of logic function definitions is similar to
	 * {@link #translateLambda(SymbolicExpression)} but the translated functions are
	 * named after their corresponding logic functions.
	 * 
	 * @param logicFunction a {@link LogicFunction} that will be translated into the
	 *                      SMT function with body
	 */
	private void translateLogicFunction(ProverFunctionInterpretation logicFunction) {
		int argsNum = logicFunction.parameters.length;
		List<SymbolicType> inputTypes = new LinkedList<>();

		for (int i = 0; i < argsNum; i++)
			inputTypes.add(logicFunction.parameters[i].type());

		SymbolicFunctionType functionType = universe.functionType(inputTypes, logicFunction.definition.type());
		SymbolicExpression body = logicFunction.definition;

		String name = logicFunction.identifier;
		FastList<String> smtSymbolicConstants = new FastList<>();

		for (int i = 0; i < argsNum; i++) {
			SymbolicConstant inputVar = logicFunction.parameters[i];

			smtSymbolicConstants.add("(");
			smtSymbolicConstants.append(translateSymbolicConstant(inputVar, true));
			smtSymbolicConstants.add(" ");
			smtSymbolicConstants.append(translateType(inputVar.type()));
			smtSymbolicConstants.add(")");
		}
		FastList<String> smtOutputType = translateType(functionType.outputType());
		boolean oldCompressionOption = this.enableCompression;

		// no compression should be performed for the function body:
		enableCompression = false;

		FastList<String> smtBody = translate(body);

		enableCompression = oldCompressionOption;
		smtDeclarations.addAll("(define-fun ", name, "(");
		smtDeclarations.append(smtSymbolicConstants);
		smtDeclarations.add(") ");
		smtDeclarations.append(smtOutputType);
		smtDeclarations.add(" ");
		smtDeclarations.append(smtBody);
		smtDeclarations.add(")\n");
		// add result into expression map so that translating calls to this
		// logic function will not create declarations again:
		this.expressionMap.put(logicFunction.function, new FastList<>(name));
	}
}
