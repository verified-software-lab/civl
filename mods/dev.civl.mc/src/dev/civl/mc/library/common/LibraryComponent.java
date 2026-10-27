package dev.civl.mc.library.common;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.log.IF.CIVLErrorLogger;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLProperty;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLTypeFactory;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.model.IF.Model;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.semantics.IF.Evaluation;
import dev.civl.mc.semantics.IF.Evaluator;
import dev.civl.mc.semantics.IF.LibraryEvaluatorLoader;
import dev.civl.mc.semantics.IF.SymbolicAnalyzer;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.mc.util.IF.Pair;
import dev.civl.sarl.IF.Reasoner;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.ArrayElementReference;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.NumericSymbolicConstant;
import dev.civl.sarl.IF.expr.ReferenceExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;
import dev.civl.sarl.IF.number.IntegerNumber;
import dev.civl.sarl.IF.number.Number;
import dev.civl.sarl.IF.object.IntObject;
import dev.civl.sarl.IF.object.SymbolicObject;
import dev.civl.sarl.IF.type.SymbolicArrayType;
import dev.civl.sarl.IF.type.SymbolicCompleteArrayType;
import dev.civl.sarl.IF.type.SymbolicType;
import dev.civl.sarl.IF.type.SymbolicType.SymbolicTypeKind;

/**
 * The LibraryComponent class provides the common data and operations of library
 * evaluator, enabler, and executor.
 * 
 * @author Manchun Zheng (zmanchun)
 * 
 */
public abstract class LibraryComponent {

	/**
	 * A helper class for storaging measurements of an array object, including
	 * dimensions, extent for each dimension and slice size for each sub-array with
	 * lower dimension.
	 * 
	 * There are three fields can be read from an instance of this class:
	 * <ol>
	 * <li>dimensions: number of dimensions in the corresponding array</li>
	 * <li>extents: extents of dimensions in the corresponding array, extents are
	 * saved in a "Java array" which have the same order as order of declaring the
	 * corresponding array.</li>
	 * <li>sliceSizes: sizes of sub-array slices for each lower dimension, sizes are
	 * saved in a "java array" which have the order from largest to the
	 * smallest.</li>
	 * <li>baseType: the base type of the corresponding array, base type must not be
	 * an array type.</li>
	 * </ol>
	 * 
	 * @author ziqing
	 *
	 */
	public class ArrayMeasurement {
		final int dimensions;
		final NumericExpression extents[];
		final NumericExpression sliceSizes[];
		final SymbolicType baseType;

		ArrayMeasurement(SymbolicArrayType arrayType) {
			dimensions = arrayType.dimensions();
			extents = new NumericExpression[dimensions];
			sliceSizes = new NumericExpression[dimensions];
			// Get extents:
			for (int i = 0; i < dimensions; i++) {
				assert arrayType.isComplete();
				SymbolicCompleteArrayType completeType = (SymbolicCompleteArrayType) arrayType;

				extents[i] = completeType.extent();
				if (i < dimensions - 1)
					arrayType = (SymbolicArrayType) arrayType.elementType();
			}
			baseType = arrayType.elementType();
			// Compute slice sizes:
			NumericExpression sliceSize = one;

			for (int i = dimensions; --i >= 0;) {
				sliceSizes[i] = sliceSize;
				sliceSize = universe.multiply(sliceSize, extents[i]);
			}
		}
	}

	// The order of these operations should be consistent with civlc.cvh
	// file.
	/**
	 * Operators:
	 * 
	 * <pre>
	 *   _NO_OP,  // no operation
	 *   _MAX,    // maxinum
	 *   _MIN,    // minimun
	 *   _SUM,    // sum
	 *   _PROD,   // product
	 *   _LAND,   // logical and
	 *   _BAND,   // bit-wise and
	 *   _LOR,    // logical or
	 *   _BOR,    // bit-wise or
	 *   _LXOR,   // logical exclusive or
	 *   _BXOR,   // bit-wise exclusive or
	 *   _MINLOC, // min value and location
	 *   _MAXLOC, // max value and location
	 *   _REPLACE // replace ? TODO: Find definition for this operation
	 * </pre>
	 */
	protected enum CIVLOperator {
		CIVL_NO_OP, // no operation
		CIVL_MAX, // maxinum
		CIVL_MIN, // minimun
		CIVL_SUM, // sum
		CIVL_PROD, // product
		CIVL_LAND, // logical and
		CIVL_BAND, // bit-wise and
		CIVL_LOR, // logical or
		CIVL_BOR, // bit-wise or
		CIVL_LXOR, // logical exclusive or
		CIVL_BXOR, // bit-wise exclusive or
		CIVL_MINLOC, // min value and location
		CIVL_MAXLOC, // max value and location
		CIVL_REPLACE, // replace ? TODO: Find definition for this operation
		CIVL_EQ, // equal to
		CIVL_NEQ, // NOT equal to
	}

	/**
	 * The symbolic expression of one.
	 */
	protected NumericExpression one;

	/**
	 * The symbolic object of integer one.
	 */
	protected IntObject oneObject;

	/**
	 * The symbolic expression of three.
	 */
	protected NumericExpression three;

	/**
	 * The symbolic object of integer three.
	 */
	protected IntObject threeObject;

	/**
	 * The symbolic expression of two.
	 */
	protected NumericExpression two;

	/**
	 * The symbolic object of integer two.
	 */
	protected IntObject twoObject;

	/**
	 * The symbolic expression of zero.
	 */
	protected NumericExpression zero;

	/**
	 * The symbolic object of integer zero.
	 */
	protected IntObject zeroObject;

	/**
	 * The symbolic universe for symbolic computations.
	 */
	protected SymbolicUniverse universe;

	/**
	 * The symbolic universe for symbolic computations.
	 */
	protected SymbolicUtility symbolicUtil;

	/**
	 * The symbolic analyzer for operations on symbolic expressions and states.
	 */
	protected SymbolicAnalyzer symbolicAnalyzer;

	protected String name;

	protected BooleanExpression trueValue;
	protected BooleanExpression falseValue;

	protected LibraryEvaluatorLoader libEvaluatorLoader;

	/**
	 * The model factory of the system.
	 */
	protected ModelFactory modelFactory;

	/**
	 * The static model of the program.
	 */
	protected Model model;

	protected CIVLTypeFactory typeFactory;

	/**
	 * The CIVL configuration object
	 */
	protected CIVLConfiguration civlConfig;

	protected CIVLErrorLogger errorLogger;

	protected Evaluator evaluator;

	/**
	 * Creates a new instance of a library.
	 * 
	 * @param universe         The symbolic universe to be used.
	 * @param symbolicUtil     The symbolic utility to be used.
	 * @param symbolicAnalyzer The symbolic analyzer to be used.
	 */
	protected LibraryComponent(String name, SymbolicUniverse universe, SymbolicUtility symbolicUtil,
			SymbolicAnalyzer symbolicAnalyzer, CIVLConfiguration civlConfig, LibraryEvaluatorLoader libEvaluatorLoader,
			ModelFactory modelFactory, CIVLErrorLogger errLogger, Evaluator evaluator) {
		this.name = name;
		this.universe = universe;
		this.zero = universe.zeroInt();
		this.one = universe.oneInt();
		this.two = universe.integer(2);
		this.three = universe.integer(3);
		this.zeroObject = universe.intObject(0);
		this.oneObject = universe.intObject(1);
		this.twoObject = universe.intObject(2);
		this.threeObject = universe.intObject(3);
		this.symbolicUtil = symbolicUtil;
		this.symbolicAnalyzer = symbolicAnalyzer;
		this.trueValue = universe.trueExpression();
		this.falseValue = universe.falseExpression();
		this.libEvaluatorLoader = libEvaluatorLoader;
		this.modelFactory = modelFactory;
		this.model = modelFactory.model();
		this.typeFactory = modelFactory.typeFactory();
		this.civlConfig = civlConfig;
		this.errorLogger = errLogger;
		this.evaluator = evaluator;
	}

	/**
	 * Returns the name associated to this library executor or enabler, for example,
	 * "stdio".
	 * 
	 * @return
	 */
	public String name() {
		return this.name;
	}

	/**
	 * <p>
	 * <b>Summary: </b> Apply a CIVL operation on a pair of operands.
	 * </p>
	 * 
	 * @param state       The current state
	 * @param pid         The identifier of the current process
	 * @param process     The String identifier of the current process
	 * @param operands    An array of two operands:
	 *                    <code>{operand0, operand1}</code>
	 * @param CIVLOp      A integer code represents a CIVL operation
	 * @param count       The number of pairs of operands.
	 * @param elementType The {@link SymbolicType} of elements of one operand
	 * @param countStep   The number of elements in one operand
	 * @param civlsource  The {@link CIVLSource} of this operation
	 * @return A {@link SymbolicExpression} results after the operation.
	 * @throws UnsatisfiablePathConditionException when types of operands are
	 *                                             invalid for operations.
	 */
	protected SymbolicExpression applyCIVLOperation(State state, int pid, String process, SymbolicExpression operands[],
			CIVLOperator CIVLOp, NumericExpression count, SymbolicType elementType, CIVLSource civlsource)
			throws UnsatisfiablePathConditionException {
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		Number concCount = reasoner.extractNumber(count);
		SymbolicExpression operand0 = operands[0];
		SymbolicExpression operand1 = operands[1];
		int countStep = 1;

		if (CIVLOp == CIVLOperator.CIVL_MINLOC || CIVLOp == CIVLOperator.CIVL_MAXLOC)
			countStep = 2;

		if (concCount == null) {
			SymbolicExpression[] singleOperand0 = new SymbolicExpression[countStep];
			SymbolicExpression[] singleOperand1 = new SymbolicExpression[countStep];
			SymbolicExpression[] result = new SymbolicExpression[countStep];
			NumericExpression totalUnits = universe.multiply(count, universe.integer(countStep));
			NumericSymbolicConstant identifier = (NumericSymbolicConstant) universe
					.symbolicConstant(universe.stringObject("j"), universe.integerType());

			for (int w = 0; w < countStep; w++) {
				singleOperand0[w] = universe.arrayRead(operand0, universe.add(identifier, universe.integer(w)));
				singleOperand1[w] = universe.arrayRead(operand1, universe.add(identifier, universe.integer(w)));
			}
			result = singleApplyCIVLOperation(state, pid, process, singleOperand0, singleOperand1, CIVLOp, countStep,
					civlsource);
			if (countStep == 1) {
				// optimization
				return universe.arrayLambda(universe.arrayType(elementType, totalUnits),
						universe.lambda(identifier, result[0]));
			} else {
				// When an operand contains more than one basic elements (e.g.
				// MINLOC or MAXLOC), the return the value will be constructed
				// as follows:
				// For the result of an single operation:
				// R := {a[j], b[x(j)], c[y(j)]...}, where j is a symbolic
				// constant and x(j) ,y(j) is a (integer->integer) function on
				// j.
				//
				// For the whole array w which consists of "count" Rs, given a
				// valid index i, it should return w[i].
				// w[i] := R[i - i%step]; Where R[k] := is the k-th element of R
				// with a substitution on j with k.

				// j % countStep:
				NumericExpression identOffset = universe.modulo(identifier, universe.integer(countStep));
				// j - j % countStep:
				NumericExpression identBase = universe.subtract(identifier, identOffset);
				SymbolicExpression function;

				// For R := {a[j], b[x(j)], c[y(j)]...}, giving a index k, R' is
				// computed by update all elements in R with a substitution on j
				// with k. Note here k := i - i % countStep, i is an arbitrary
				// valid index on the whole array w:
				for (int i = 0; i < countStep; i++)
					result[i] = universe.apply(universe.lambda(identifier, result[i]), Arrays.asList(identBase));
				// make R' an array:
				function = universe.array(elementType, result);
				function = universe.lambda(identifier, universe.arrayRead(function, identOffset));
				return universe.arrayLambda(universe.arrayType(elementType, totalUnits), function);
			}
		} else {
			int countInt = ((IntegerNumber) concCount).intValue();

			if (countInt <= 0)
				return universe.emptyArray(elementType);

			int totalUnits = countInt * countStep;
			SymbolicExpression[] singleOperand0 = new SymbolicExpression[countStep];
			SymbolicExpression[] singleOperand1 = new SymbolicExpression[countStep];
			SymbolicExpression[] result = new SymbolicExpression[totalUnits];
			SymbolicExpression[] singleResult;

			for (int i = 0; i < totalUnits; i = i + countStep) {
				for (int w = 0; w < countStep; w++) {
					singleOperand0[w] = universe.arrayRead(operand0, universe.integer(i + w));
					singleOperand1[w] = universe.arrayRead(operand1, universe.integer(i + w));
				}

				singleResult = singleApplyCIVLOperation(state, pid, process, singleOperand0, singleOperand1, CIVLOp,
						countStep, civlsource);
				System.arraycopy(singleResult, 0, result, i, countStep);
			}
			return universe.array(elementType, result);
		}
	}

	/**
	 * Completing an operation (which is included in CIVLOperation enumerator).
	 * 
	 * @author Ziqing Luo
	 * @param arg0 The new data got from the bundle
	 * @param arg1 The data has already been received previously
	 * @param op   The CIVL Operation
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private SymbolicExpression[] singleApplyCIVLOperation(State state, int pid, String process,
			SymbolicExpression op0[], SymbolicExpression op1[], CIVLOperator op, int numElementsInOperand,
			CIVLSource civlsource) throws UnsatisfiablePathConditionException {
		BooleanExpression claim;
		SymbolicExpression[] result = new SymbolicExpression[numElementsInOperand];

		/*
		 * For MAX and MIN operation, if CIVL cannot figure out a concrete result, make
		 * a abstract function for it.
		 */
		try {
			switch (op) {
			// TODO: consider using heuristic to switch to abstract
			// functions if these expressions get too big (max,min):
			case CIVL_MAX:
				claim = universe.lessThan((NumericExpression) op1[0], (NumericExpression) op0[0]);
				result[0] = universe.cond(claim, op0[0], op1[0]);
				break;
			case CIVL_MIN:
				claim = universe.lessThan((NumericExpression) op0[0], (NumericExpression) op1[0]);
				result[0] = universe.cond(claim, op0[0], op1[0]);
				break;
			case CIVL_SUM:
				result[0] = universe.add((NumericExpression) op0[0], (NumericExpression) op1[0]);
				break;
			case CIVL_PROD:
				result[0] = universe.multiply((NumericExpression) op0[0], (NumericExpression) op1[0]);
				break;
			case CIVL_LAND:
				BooleanExpression aop0 = op0[0].isZero() ? universe.falseExpression() : universe.trueExpression();
				BooleanExpression aop1 = op1[0].isZero() ? universe.falseExpression() : universe.trueExpression();

				result[0] = universe.and(aop0, aop1);
				break;
			case CIVL_LOR:
				BooleanExpression oop0 = op0[0].isZero() ? universe.falseExpression() : universe.trueExpression();
				BooleanExpression oop1 = op1[0].isZero() ? universe.falseExpression() : universe.trueExpression();

				result[0] = universe.or(oop0, oop1);
				break;
			case CIVL_LXOR:
				BooleanExpression notNewData = universe.not((BooleanExpression) op0[0]);
				BooleanExpression notPrevData = universe.not((BooleanExpression) op1[0]);

				result[0] = universe.or(universe.and(notNewData, (BooleanExpression) op0[0]),
						universe.and((BooleanExpression) op1[0], notPrevData));
				break;
			case CIVL_MINLOC:
				return applyMINOrMAXLOC(state, process, op0, op1, true, civlsource);
			case CIVL_MAXLOC:
				return applyMINOrMAXLOC(state, process, op0, op1, false, civlsource);
			case CIVL_REPLACE:
			case CIVL_BAND:
				result[0] = universe.bitand((NumericExpression) op0[0], (NumericExpression) op1[0]);
				break;
			case CIVL_BOR:
				result[0] = universe.bitor((NumericExpression) op0[0], (NumericExpression) op1[0]);
				break;
			case CIVL_BXOR:
				result[0] = universe.bitxor((NumericExpression) op0[0], (NumericExpression) op1[0]);
				break;
			case CIVL_EQ:
				result[0] = universe.equals((NumericExpression) op0[0], (NumericExpression) op1[0]);
				break;
			case CIVL_NEQ:
				result[0] = universe.neq((NumericExpression) op0[0], (NumericExpression) op1[0]);
				break;
			default:
				throw new CIVLUnimplementedFeatureException("CIVLOperation: " + op.name());
			}
			return result;
		} catch (ClassCastException e) {
			errorLogger.logSimpleError(civlsource, state, pid, process, symbolicAnalyzer.stateToString(state),
					CIVLProperty.OTHER, "Invalid operands type for CIVL Operation: " + op.name());
			throw new UnsatisfiablePathConditionException();
		}
	}

	/**
	 * Returns the count of objects in one operand in a CIVL operation. e.g. MINLOC
	 * or MAXLOC needs 2 objects for 1 operand
	 * 
	 * @param op The {@link CIVLOperator}
	 * @return
	 */
	protected NumericExpression operandCounts(CIVLOperator civlOp) {
		if (civlOp == CIVLOperator.CIVL_MAXLOC || civlOp == CIVLOperator.CIVL_MINLOC)
			return two;
		else
			return one;
	}

	protected CIVLOperator translateOperator(int op) {
		return CIVLOperator.values()[op];
	}

	/**
	 * <p>
	 * <b>Summary: </b> Apply MIN_LOC or MAX_LOC operation on two operands.
	 * </p>
	 * 
	 * @param state      The current state
	 * @param process    The String identifier of the process
	 * @param operands   An array of all operands:
	 *                   <code>{loc0, idx0, loc1, idx1}</code>
	 * @param isMin      A flag, true for MIN_LOC operation, false for MAX_LOC
	 *                   operation.
	 * @param civlsource {@link CIVLSource} of the operation
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private SymbolicExpression[] applyMINOrMAXLOC(State state, String process, SymbolicExpression[] operand0,
			SymbolicExpression[] operand1, boolean isMin, CIVLSource civlsource)
			throws UnsatisfiablePathConditionException {
		NumericExpression locations[] = { (NumericExpression) operand0[0], (NumericExpression) operand1[0] };
		NumericExpression indices[] = { (NumericExpression) operand0[1], (NumericExpression) operand1[1] };

		assert (operand0.length == 2) && (operand1.length == 2);
		return isMin ? applyMinLocOperation(locations, indices) : applyMaxLocOperation(locations, indices);
	}

	/**
	 * <p>
	 * <b>Summary: </b> Apply a MINLOC operation on two operands: [loc0, idx0] and
	 * [loc1, idx1]
	 * </p>
	 * 
	 * @param locations Location array which consists of a "location0" and a
	 *                  "location1"
	 * @param indices   Index array which consists of a "index0" and a "index1"
	 * @return loc0 \lt loc1 ? [loc0, idx0] : loc0 != loc1 ? [loc1, idx1] : idx0 \lt
	 *         idx1 ? [loc0, idx0]: [loc1, idx1]
	 */
	private SymbolicExpression[] applyMinLocOperation(NumericExpression locations[], NumericExpression indices[]) {
		BooleanExpression loc0LTloc1 = universe.lessThan(locations[0], locations[1]);
		BooleanExpression loc0NEQloc1 = universe.not(universe.equals(locations[0], locations[1]));
		BooleanExpression idx0LTidx1 = universe.lessThan(indices[0], indices[1]);
		SymbolicExpression locResult, idxResult;

		// optimize:
		if (loc0LTloc1.isTrue() && loc0NEQloc1.isTrue()) {
			SymbolicExpression[] result = { locations[0], indices[0] };

			return result;
		} else {
			locResult = universe.cond(loc0LTloc1, locations[0], locations[1]);
			idxResult = universe.cond(loc0LTloc1, indices[0],
					universe.cond(loc0NEQloc1, indices[1], universe.cond(idx0LTidx1, indices[0], indices[1])));

			SymbolicExpression[] result = { locResult, idxResult };

			return result;
		}
	}

	/**
	 * <p>
	 * <b>Summary: </b> Apply a MAXLOC operation on two operands: [loc0, idx0] and
	 * [loc1, idx1]
	 * </p>
	 * 
	 * 
	 * @param locations Location array which consists of a "location0" and a
	 *                  "location1"
	 * @param indices   Index array which consists of a "index0" and a "index1"
	 * @return loc0 \gt loc1 ? [loc0, idx0] : loc0 != loc1 ? [loc1, idx1] : idx0 \lt
	 *         idx1 ? [loc0, idx0]: [loc1, idx1]
	 * @return
	 */
	private SymbolicExpression[] applyMaxLocOperation(NumericExpression locations[], NumericExpression indices[]) {
		BooleanExpression loc0GTloc1 = universe.lessThan(locations[1], locations[0]);
		BooleanExpression loc0NEQloc1 = universe.not(universe.equals(locations[0], locations[1]));
		BooleanExpression idx0LTidx1 = universe.lessThan(indices[0], indices[1]);
		SymbolicExpression locResult, idxResult;

		// optimize:
		if (loc0GTloc1.isTrue() && loc0NEQloc1.isTrue()) {
			SymbolicExpression[] result = { locations[0], indices[0] };

			return result;
		} else {
			locResult = universe.cond(loc0GTloc1, locations[0], locations[1]);
			idxResult = universe.cond(loc0GTloc1, indices[0],
					universe.cond(loc0NEQloc1, indices[1], universe.cond(idx0LTidx1, indices[0], indices[1])));

			SymbolicExpression[] result = { locResult, idxResult };

			return result;

		}
	}

	protected Pair<State, SymbolicExpression[]> evaluateArguments(State state, int pid, Expression[] arguments)
			throws UnsatisfiablePathConditionException {
		int numArgs = arguments.length;
		SymbolicExpression[] argumentValues = new SymbolicExpression[numArgs];

		for (int i = 0; i < numArgs; i++) {
			Evaluation eval = null;

			eval = evaluator.evaluate(state, pid, arguments[i]);
			argumentValues[i] = eval.value;
			state = eval.state;
		}
		return new Pair<>(state, argumentValues);

	}

	/**
	 * Pre-conditions:
	 * <ol>
	 * <li>"pointer" is a valid pointer</li>
	 * <li>"count" greater than zero</li>
	 * <li>"dataArray" is an one dimensional array</li>
	 * <li>"pointer" must points to a compatible type with the "dataArray"</li>
	 * </ol>
	 * post_condition:
	 * <ol>
	 * <li>Return a sequence of data with length "count" from the pointed object
	 * starting from the pointed position</li>
	 * </ol>
	 * Setting a sequence of data starting from a pointer
	 * 
	 * @param state       The current state
	 * @param process     The information of the process
	 * @param pointer     The pointer to the start position
	 * @param count       The number of cells in the array of data
	 * @param dataArray   The sequence of data is going to be set
	 * @param checkOutput Flag for check output variable
	 * @param source      CIVL source of the statement
	 * @return A pair of evaluation and pointer.The data in form of an array which
	 *         can be assigned to the returned pointer.
	 * @throws UnsatisfiablePathConditionException
	 */
	public Pair<Evaluation, SymbolicExpression> setDataFrom(State state, int pid, String process, Expression ptrExpr,
			SymbolicExpression pointer, NumericExpression count, SymbolicExpression dataArray, boolean checkOutput,
			CIVLSource source) throws UnsatisfiablePathConditionException {
		NumericExpression[] arraySlicesSizes;
		NumericExpression startPos;
		NumericExpression dataSeqLength = universe.length(dataArray);
		SymbolicExpression startPtr, endPtr;
		Evaluation eval;
		Pair<Evaluation, NumericExpression[]> eval_and_slices;
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		ReferenceExpression symref;
		BooleanExpression claim;
		ResultType resultType;
		int dim;

		// If data length > count, report an error:
		claim = universe.lessThan(dataSeqLength, count);
		if (universe.getShowProverQueries())
			universe.setQueryExplanation("checking data length does not exceed count in setDataFrom at "
					+ source.getLocation());
		resultType = reasoner.valid(claim).getResultType();
		if (resultType.equals(ResultType.YES) && civlConfig.isPropertyToggled(CIVLProperty.OUT_OF_BOUNDS))
			reportOutOfBoundError(state, pid, claim, resultType, pointer, dataSeqLength, count, source);
		// If the type of the object is exact same as the dataArray, then do a
		// directly assignment:
		SymbolicType objType = symbolicAnalyzer.dynamicTypeOfObjByPointer(source, state, pointer);

		if (dataArray.type().equals(objType))
			return new Pair<>(new Evaluation(state, dataArray), pointer);

		// "pointer" may not point to a memory base type object yet
		symref = symbolicAnalyzer.getLeafNodeReference(state, pointer, source);

		// If count is one:
		if (universe.getShowProverQueries())
			universe.setQueryExplanation("determining whether copy count is one in setDataFrom at "
					+ source.getLocation());
		if (reasoner.isValid(universe.equals(count, one))) {
			SymbolicExpression data = universe.arrayRead(dataArray, zero);

			return new Pair<>(new Evaluation(state, data), symbolicUtil.makePointer(pointer, symref));
		}
		// Else, count greater than one:
		if (!symref.isArrayElementReference()) {
			if (reasoner.isValid(universe.equals(count, one)))
				return new Pair<>(new Evaluation(state, universe.arrayRead(dataArray, zero)),
						symbolicUtil.makePointer(pointer, symref));
			if (civlConfig.isPropertyToggled(CIVLProperty.OUT_OF_BOUNDS)) {
				// report error:
				CIVLType integerType;

				integerType = typeFactory.integerType();
				errorLogger.logSimpleError(source, state, pid, process, symbolicAnalyzer.stateInformation(state),
						CIVLProperty.OUT_OF_BOUNDS,
						"$bundle_unpack out of bound: \nPointer: "
								+ symbolicAnalyzer.symbolicExpressionToString(source, state,
										ptrExpr.getExpressionType(), pointer)
								+ "\nSize: "
								+ symbolicAnalyzer.symbolicExpressionToString(source, state, integerType, count)
								+ "\n");
			}
		}
		eval_and_slices = evaluator.arrayElementReferenceAdd(state, pid, pointer, count, source);
		eval = eval_and_slices.left;
		endPtr = eval.value;
		state = eval.state;
		arraySlicesSizes = eval_and_slices.right;
		// If the pointer addition happens to be done within the minimal array
		// slice, the "arraySlicesSizes" is null and we don't really need it.
		// Otherwise, we can reuse it.
		if (arraySlicesSizes == null) {
			arraySlicesSizes = new NumericExpression[1];
			arraySlicesSizes[0] = one;
		}
		dim = arraySlicesSizes.length;
		startPtr = symbolicUtil.makePointer(pointer, symref);
		startPos = zero;
		if (symref.isArrayElementReference()) {
			NumericExpression[] startIndices = symbolicUtil.extractArrayIndicesFrom(startPtr);
			int numIndices = startIndices.length;

			assert numIndices >= dim;
			// Loop until startPtr and endPtr point to the common array slice:
			for (int i = 1; !startPtr.equals(endPtr); i++) {
				startPtr = symbolicUtil.parentPointer(startPtr);
				endPtr = symbolicUtil.parentPointer(endPtr);
				startPos = universe.add(startPos,
						universe.multiply(startIndices[numIndices - i], arraySlicesSizes[dim - i]));
			}
		}
		eval = evaluator.dereference(source, state, pid, process, startPtr, false, true);
		state = eval.state;
		if (eval.value.type().typeKind().equals(SymbolicTypeKind.ARRAY)) {
			eval = setDataBetween(state, pid, eval.value, arraySlicesSizes, startPos, count, pointer, dataArray,
					source);
		} else if (civlConfig.isPropertyToggled(CIVLProperty.UNDEFINED_VALUE)) {
			reportOutOfBoundError(state, pid, null, null, startPtr, one, count, source);
		}
		return new Pair<>(eval, startPtr);
	}

	/**
	 * Pre-condition:
	 * <ol>
	 * <li>"pointer" is valid</li>
	 * <li>"count" >= 0</li>
	 * </ol>
	 * post_condition:
	 * <ol>
	 * <li>Return a sequence of data with length "count" from the pointed object
	 * starting from the pointed position</li>
	 * </ol>
	 * Get a sequence of data starting from a pointer.
	 * 
	 * @param state       The current state
	 * @param process     The information of the process
	 * @param pointer     The pointer to the start position of a sequence of data
	 * @param count       The number of cells in the array of data
	 * @param checkOutput Flag for check output variable
	 * @param source      CIVL source of the statement
	 * @return Evaluation contains the sequence of data which is in form of a one
	 *         dimensional array.
	 * @throws UnsatisfiablePathConditionException
	 */
	public Evaluation getDataFrom(State state, int pid, String process, Expression pointerExpr,
			SymbolicExpression pointer, NumericExpression count, boolean toBase, boolean checkOutput, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		ReferenceExpression symref;
		Evaluation eval;
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));

		// If "count" == 1:
		if (reasoner.isValid(universe.equals(count, one))) {
			eval = evaluator.dereference(source, state, pid, process, pointer, true, true);
			if (civlConfig.isPropertyToggled(CIVLProperty.UNDEFINED_VALUE) && eval.value.isNull())
				reportUndefinedValueError(state, pid, symbolicUtil.getSymRef(pointer).isIdentityReference(),
						pointerExpr);
			eval.value = universe.array(eval.value.type(), Arrays.asList(eval.value));
			eval.value = arrayFlatten(state, pid, eval.value,
					new ArrayMeasurement((SymbolicArrayType) eval.value.type()), source);
			return eval;
		}
		// Else "count" > 1:
		SymbolicExpression rootPointer, rootArray;
		List<NumericExpression> indicesList = new LinkedList<>();
		NumericExpression indices[];
		boolean isHeap = symbolicUtil.isPointerToHeap(pointer);

		symref = symbolicUtil.getSymRef(pointer);
		while (symref.isArrayElementReference()) {
			indicesList.add(((ArrayElementReference) symref).getIndex());
			symref = ((ArrayElementReference) symref).getParent();
			// Special handling for memory heap:
			if (isHeap)
				break;
		}
		rootPointer = symbolicUtil.makePointer(pointer, symref);
		eval = evaluator.dereference(source, state, pid, process, rootPointer, false, true);
		state = eval.state;
		rootArray = eval.value;
		if (civlConfig.isPropertyToggled(CIVLProperty.UNDEFINED_VALUE) && rootArray.isNull())
			reportUndefinedValueError(state, pid, symbolicUtil.getSymRef(pointer).isIdentityReference(), pointerExpr);
		indices = new NumericExpression[indicesList.size()];
		indicesList.toArray(indices);
		// reverse so that the order satisfies the requirements of the
		// arraySliceRead method:
		for (int i = (indices.length / 2) - 1; i >= 0; i--) {
			NumericExpression tmp = indices[i];
			indices[i] = indices[indices.length - i - 1];
			indices[indices.length - i - 1] = tmp;
		}
		eval.value = arraySliceRead(state, pid, rootArray, indices, count, source);
		return eval;
	}

	/**
	 * <p>
	 * Report an attempt to read undefined (or uninitialized) object error
	 * </p>
	 * 
	 * @param state      The current state when calling this method
	 * @param pid        The PID of the process who calls this method
	 * @param isVariable If the object is an variable
	 * @param expression The expression associates to this error. Error reporting
	 *                   will be on the {@link CIVLSource} of this expression.
	 * @throws UnsatisfiablePathConditionException always.
	 */
	public void reportUndefinedValueError(State state, int pid, boolean isVariable, Expression expression)
			throws UnsatisfiablePathConditionException {
		String kind = "undefined";
		String process = state.getProcessState(pid).name();

		if (isVariable)
			kind = "uninitialized";
		errorLogger.logSimpleError(expression.getSource(), state, pid, process,
				symbolicAnalyzer.stateInformation(state), CIVLProperty.UNDEFINED_VALUE,
				"Attempt to read an object with " + kind + " value");
	}

	/**
	 * <p>
	 * <b>Pre-condition</b> <br>
	 * 1. The dimensions of the array must be greater than or equal to
	 * "indices.length"; <br>
	 * 2. Values in "indices" array are ordered from left to right as same as
	 * subscript order. (e.g. A[a][b][c] ==> {a,b,c})
	 * </p>
	 * 
	 * <p>
	 * Given a group of indices I, an array a and a number c. Read c consequent
	 * elements start from a[I]. If size of I less than the dimension of a, it will
	 * be supplemented with zeros. e.g. Given an array a[4][5][6] and a ordered set
	 * of indices {2, 1}. The indices locate the element [2][1][0] in array a.
	 * </p>
	 * 
	 * @param state     The current state when calling this method
	 * @param pid       The PID of the current process
	 * @param arrayType The {@link CIVLType} of the array
	 * @param array     The array that will be read
	 * @param indices   The start indices of the element in the array.
	 * @param count     The number of elements will be read
	 * @param source    The {@link CIVLSource} associates to this method call.
	 * @return A sequence of elements (A one dimensional array)
	 * @throws UnsatisfiablePathConditionException When array out of bound happens.
	 */
	public SymbolicExpression arraySliceRead(State state, int pid, SymbolicExpression array,
			NumericExpression indices[], NumericExpression count, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		NumericExpression pos = zero, step = one;
		SymbolicExpression flattenArray;
		ArrayMeasurement arrayMeasure = new ArrayMeasurement((SymbolicArrayType) array.type());
		NumericExpression sliceSizes[] = arrayMeasure.sliceSizes;
		int i;

		// Attempt to avoid array flatten for multi-dimensional arrays.
		// If the size of the smallest (1 dimentional, if single cell is NOT
		// counted as an array slice) array slice is greater than or equal to
		// the index (or 'offset') of the smallest array slice plus the count,
		// then directly return a sub-array of that slice ...
		if (arrayMeasure.dimensions > 1) {
			// The size of the smallest array slice:
			NumericExpression minSliceSize = sliceSizes[sliceSizes.length - 2];
			// The index of the smallest array slice:
			NumericExpression index = indices.length == arrayMeasure.dimensions ? indices[indices.length - 1] : zero;
			BooleanExpression indexPlusCountLteSize = universe.lessThanEquals(universe.add(index, count), minSliceSize);
			Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));

			if (reasoner.isValid(indexPlusCountLteSize)) {
				for (i = 0; i < arrayMeasure.dimensions - 1; i++)
					array = i < indices.length ? universe.arrayRead(array, indices[i])
							: universe.arrayRead(array, zero);
				return symbolicAnalyzer.getSubArray(state, pid, array, index, universe.add(index, count), source);
			}
		}
		flattenArray = arrayFlatten(state, pid, array, arrayMeasure, source);
		for (i = 0; i < indices.length; i++)
			pos = universe.add(pos, universe.multiply(indices[i], sliceSizes[i]));
		// valid subscript: d < indices.length <= dimension && sliceSizes.length
		// == dimension
		step = i > 0 ? sliceSizes[i - 1] : sliceSizes[0];
		return symbolicAnalyzer.getSubArray(state, pid, flattenArray, pos,
				universe.add(pos, universe.multiply(count, step)), source);
	}

	/**
	 * <p>
	 * <b>Pre-condition:</b> <br>
	 * length(targetArray) >= length(dataArray) + index;<br>
	 * elementTypeOf(targetArray) == elementTypeOf(dataArray)
	 * </p>
	 * <p>
	 * Writes the sequence of elements in "dataArray" to the "targetArray" from the
	 * "index" of the "targetArray". This operation only will be done within one
	 * dimension, i.e. both "dataArray" and "targetArray" represent a sequence of
	 * elements, no matter the type of the elements is a scalar type, an array type
	 * or a complex structure.
	 * 
	 * For example, writes b[2][3] into a[3][3] start from index 0, the results will
	 * be:
	 * 
	 * a[3][3] = {b[0][0], b[0][1], b[0][2], b[1][0], b[1][1], b[1][2], a[2][0],
	 * a[2][1], a[2][2]}
	 * 
	 * </p>
	 * 
	 * @param state       The current state when this method is called
	 * @param pid         The PID of the process
	 * @param targetArray The target array that will be written
	 * @param dataArray   The sequence of data that will be insert into the
	 *                    targetArray
	 * @param index       The start index of this write operation
	 * @param source      The {@link CIVLSource} related with this method call
	 * @return
	 */
	public SymbolicExpression arraySliceWrite1d(State state, int pid, SymbolicExpression targetArray,
			SymbolicExpression dataArray, NumericExpression index, CIVLSource source) {
		NumericExpression dataLength = universe.length(dataArray);
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		Number concreteDataLength = reasoner.extractNumber(dataLength);

		// If the data array has a non-concrete length, use array lambda:
		if (concreteDataLength == null) {
			NumericSymbolicConstant symConst = (NumericSymbolicConstant) universe
					.symbolicConstant(universe.stringObject("i"), universe.integerType());
			BooleanExpression hiCond = universe.lessThan(symConst, universe.add(index, dataLength));
			BooleanExpression loCond = universe.lessThanEquals(index, symConst);
			SymbolicExpression elementLambda;
			SymbolicCompleteArrayType targetArrayType = (SymbolicCompleteArrayType) targetArray.type();

			elementLambda = universe.lambda(symConst, universe.cond(universe.and(hiCond, loCond),
					universe.arrayRead(dataArray, symConst), universe.arrayRead(targetArray, symConst)));
			return universe.arrayLambda(targetArrayType, elementLambda);
		} else {
			int intDataLength = ((IntegerNumber) concreteDataLength).intValue();

			for (int i = 0; i < intDataLength; i++) {
				NumericExpression I = universe.integer(i);
				NumericExpression IplusIndex = universe.add(I, index);

				targetArray = universe.arrayWrite(targetArray, IplusIndex, universe.arrayRead(dataArray, I));
			}
			return targetArray;
		}
	}

	/**
	 * Cast an array to another array. The two arrays before and after casting must
	 * be able to hold same number of non-array elements.<br>
	 * e.g. For arrays <code>int a[2][2]; int b[4]; int c[5]</code>, a and b can be
	 * casted into each other but both of them can not be casted to c.
	 * 
	 * @author Ziqing Luo
	 * @param state               The current state
	 * @param pid                 The PID of the calling process
	 * @param oldArray            The array before casting
	 * @param oldArrayMeasurement The {@link ArrayMeasurement} of the oldArray
	 * @param targetType          The target type that the oldArray will be casted
	 *                            to
	 * @param source              The CIVL source of the oldArray or the pointer to
	 *                            OldArray
	 * @return casted array
	 * @throws UnsatisfiablePathConditionException
	 */
	public SymbolicExpression arrayCasting(State state, int pid, SymbolicExpression oldArray,
			ArrayMeasurement oldArrayMeasurement, SymbolicCompleteArrayType targetType, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		int[] targetExtentNumbers;
		int dim;

		// Straightforward Optimizations:
		if (oldArray.type().equals(targetType))
			return oldArray;
		// Optimization: if oldArray is a symbolic constant, just change type:
		if (oldArray.operator() == SymbolicOperator.SYMBOLIC_CONSTANT) {
			SymbolicObject[] args = { oldArray.argument(1) };

			return universe.make(SymbolicOperator.SYMBOLIC_CONSTANT, targetType, args);
		}

		ArrayMeasurement targetArrayMeasurement = new ArrayMeasurement(targetType);

		dim = targetArrayMeasurement.dimensions;
		targetExtentNumbers = new int[dim];
		for (int d = 0; d < dim; ++d) {
			IntegerNumber extent = (IntegerNumber) reasoner.extractNumber(targetArrayMeasurement.extents[d]);

			if (extent == null)
				throw new CIVLUnimplementedFeatureException("Transform symbolic array " + oldArray + " of type "
						+ oldArray.type() + "to another type " + targetType);
			targetExtentNumbers[d] = extent.intValue();
		}

		SymbolicExpression flattenArray = arrayFlatten(state, pid, oldArray, oldArrayMeasurement, source);

		return flattenToMultiDimensionalArray(targetExtentNumbers, oldArrayMeasurement.baseType, flattenArray);
	}

	/**
	 * Transform an flatten array a to a multiple dimensional array b. The extent of
	 * a equals to the product of the extents of b.
	 * 
	 * @param extents   An array of extents {e<sub>0</sub>, e<sub>1</sub>, ..., e
	 *                  <sub>n-1</sub>} for a multi-dimensional array
	 *                  <code>T b[e<sub>0</sub>][e<sub>1</sub>][..][e<sub>n-1</sub>]</code>
	 * @param baseType  The base type T of the multi-dimensional array
	 *                  <code>T b[e<sub>0</sub>][e<sub>1</sub>][..][e<sub>n-1</sub>]</code>
	 * @param flatArray The flatten array T a[e<sub>0</sub> * e<sub>1</sub> * ... *
	 *                  e <sub>n-1</sub>];
	 * @return The multi-dimensional array b.
	 */
	SymbolicExpression flattenToMultiDimensionalArray(int extents[], SymbolicType baseType,
			SymbolicExpression flatArray) {
		return this.flattenToMultiDimensionalArrayWorker(extents, 0, baseType, flatArray, 0).left;
	}

	/**
	 * The recursive worker method of
	 * {@link #flattenToMultiDimensionalArray(int[], SymbolicType, SymbolicExpression)}
	 * .
	 * 
	 * @param extents         An array of extents {e<sub>0</sub>, e<sub>1</sub>,
	 *                        ..., e <sub>n-1</sub>} for a multi-dimensional array
	 *                        <code>T b[e<sub>0</sub>][e<sub>1</sub>][..][e<sub>n-1</sub>]</code>
	 * @param dim             current dimension. This method recursively creates
	 *                        elements for each dimension, this parameter represents
	 *                        the current dimension of this method execution.
	 * @param baseType        The base type T of the multi-dimensional array
	 *                        <code>T b[e<sub>0</sub>][e<sub>1</sub>][..][e<sub>n-1</sub>]</code>
	 * @param flatArray       The flatten array T a[e<sub>0</sub> * e<sub>1</sub> *
	 *                        ... * e <sub>n-1</sub>];
	 * @param flatArrayOffset Each recursive execution creates an element from a
	 *                        segment on the flatten array. This flatArrayOffset is
	 *                        the start index of the segment.
	 * @return A sub-array and the number of base elements in this sub-array.
	 */
	private Pair<SymbolicExpression, Integer> flattenToMultiDimensionalArrayWorker(int extents[], int dim,
			SymbolicType baseType, SymbolicExpression flatArray, int flatArrayOffset) {
		List<SymbolicExpression> components = new LinkedList<>();
		SymbolicExpression result;

		if (dim < extents.length - 1) {
			// If this is not the base case...
			int step = 1;

			for (int i = 0; i < extents[dim]; i++) {
				Pair<SymbolicExpression, Integer> subResult = flattenToMultiDimensionalArrayWorker(extents, dim + 1,
						baseType, flatArray, flatArrayOffset);

				step = subResult.right;
				flatArrayOffset += step;
				components.add(subResult.left);
			}
			// components shall never be empty since the extent of an array
			// shall never be zero:
			return new Pair<>(universe.array(components.get(0).type(), components), extents[dim] * step);
		} else {
			// base case:
			for (int i = 0; i < extents[dim]; i++)
				components.add(universe.arrayRead(flatArray, universe.integer(flatArrayOffset++)));
			result = universe.array(baseType, components);
			return new Pair<>(result, extents[dim]);
		}
	}

	/**
	 * <p>
	 * <b>Pre-condition:</b>'array' must be a complete array object. <br>
	 * 'arrayMeasurement' is an {@link ArrayMeasurement} object associates to the
	 * 'array'.
	 * </p>
	 * <p>
	 * Flatten an array to a one-dimensional array whose elements must have
	 * non-array type.
	 * </p>
	 * 
	 * @param state            The current state when this method is called
	 * @param pid              The PID of the calling process.
	 * @param array            The complete array object
	 * @param arrayMeasurement The {@link ArrayMeasurement} of the array.
	 * @param civlsource       The {@link CIVLSource} associates to this method.
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	public SymbolicExpression arrayFlatten(State state, int pid, SymbolicExpression array,
			ArrayMeasurement arrayMeasurement, CIVLSource civlsource) throws UnsatisfiablePathConditionException {
		Queue<SymbolicExpression> subTreeQueue = new LinkedList<>();
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		SymbolicType baseType = arrayMeasurement.baseType;
		NumericExpression extents[] = arrayMeasurement.extents;
		NumericExpression sliceSizes[] = arrayMeasurement.sliceSizes;
		int dimensions = arrayMeasurement.dimensions;

		subTreeQueue.add(array);
		// If any extent of the array is non-concrete, use lambdaFlatten:
		for (int d = 0; d < dimensions; d++)
			if (reasoner.extractNumber(extents[d]) == null)
				return arrayLambdaFlatten2(state, array, sliceSizes, extents, civlsource);
		// If the totoal size of the array is concrete:
		for (int d = 0; d < dimensions; d++) {
			int prevExtent = subTreeQueue.size();
			NumericExpression extent;
			Number concExtent;
			int intExtent;

			extent = extents[d];
			concExtent = reasoner.extractNumber(extent);
			// TODO: it's possible that an array 'a[1/N][N]' will be falttened
			// as an array having concrete extents, but it should be really rare
			// case so just throw a internal error here for now. It it realy
			// happens, change the code here.
			if (concExtent == null)
				throw new CIVLInternalException("Unexpected exception during flatten an array of concrete extents.",
						civlsource);
			intExtent = ((IntegerNumber) concExtent).intValue();
			for (int i = 0; i < prevExtent; i++) {
				array = subTreeQueue.poll();
				for (int j = 0; j < intExtent; j++)
					subTreeQueue.add(universe.arrayRead(array, universe.integer(j)));
			}
		}
		return universe.array(baseType, subTreeQueue);
	}

	/**
	 * Pre-condition:
	 * <ol>
	 * <li>"pointer" points to the start position</li>
	 * <li>"count" > 0</li>
	 * <li>"count" >= length(dataSequence)</li>
	 * <li>"array" has {@link SymbolicCompleteArrayType}</li>
	 * <li>"dataSequence" is an one dimensional array</li>
	 * </ol>
	 * Post-condition:
	 * <ol>
	 * <li>left side of the pair: Return the new value of the pointed object after
	 * assigning the given data sequence from pointed position</li>
	 * <li>right side of the pair: Return the pointer which can be assigned with the
	 * new value</li>
	 * </ol>
	 * Setting a sequence of data between two array element references. Returns the
	 * settled new array and the pointer to that array.
	 * 
	 * Pre-condition: start pointer and end pointer should point to the same object.
	 * 
	 * @param state            The current state
	 * @param pid              The PID of the calling process
	 * @param startPtr         The pointer to the start position
	 * @param endPtr           The pointer to the end position
	 * @param dataSequence     The sequence of data which is going to be set
	 * @param arraySlicesSizes The capacity information of the array pointed by the
	 *                         startPtr or endPtr(These two pointers point to the
	 *                         same object).<br>
	 *                         Note: Here capacity information of an array means
	 *                         that for one cell in each dimension of an array how
	 *                         many non-array elements it can hold. e.g. For array
	 *                         <code>int a[2][2];</code>, the one cell in deepest
	 *                         dimension can only hold one element while one cell in
	 *                         the second deepest dimension can hold 2 elements.
	 *                         Here we use 0 marking (which is key in the given map)
	 *                         the deepest dimension and 1 marking the second
	 *                         deepest dimension and so forth.
	 * @param source           The CIVL source of the start pointer.
	 * @return the settled new array and the pointer to that array.
	 * @throws UnsatisfiablePathConditionException
	 * @author Ziqing Luo
	 */
	private Evaluation setDataBetween(State state, int pid, SymbolicExpression array,
			NumericExpression[] arraySlicesSizes, NumericExpression startPos, NumericExpression count,
			SymbolicExpression pointer, SymbolicExpression dataSequence, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression flattenArray;
		NumericExpression dataSize;
		NumericExpression i;
		BooleanExpression claim;
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));

		dataSize = universe.length(dataSequence);
		// Direct assignment conditions:
		// 1. start position is zero.
		// 2. Interval between pointers equals to data size.
		// 3. The least common array capacity equals to data size.
		if (reasoner.isValid(universe.equals(startPos, zero)) && arraySlicesSizes.length == 1) {
			NumericExpression arraySize = universe.length(array);

			claim = universe.and(universe.equals(dataSize, count), universe.equals(dataSize, arraySize));
			if (reasoner.isValid(claim))
				return new Evaluation(state, dataSequence);
		} // TODO: what if the length of dataSize is non-concrete and cannot be
			// decided by reasoner?
		flattenArray = arrayFlatten(state, pid, array, new ArrayMeasurement((SymbolicArrayType) array.type()), source);
		i = startPos;

		Number dataSizeConcrete = reasoner.extractNumber(dataSize);

		if (dataSizeConcrete == null) {
			// TODO: only if flattenArray has dimension 1:
			NumericSymbolicConstant idx = (NumericSymbolicConstant) universe
					.symbolicConstant(universe.stringObject("i"), universe.integerType());
			BooleanExpression condition = universe.and(universe.lessThanEquals(startPos, idx),
					universe.lessThan(idx, universe.add(startPos, dataSize)));
			SymbolicExpression function = universe.cond(condition,
					universe.arrayRead(dataSequence, universe.subtract(idx, startPos)),
					universe.arrayRead(flattenArray, idx));

			flattenArray = universe.arrayLambda((SymbolicCompleteArrayType) flattenArray.type(),
					universe.lambda(idx, function));
			return new Evaluation(state, flattenArray);
			// throw new CIVLInternalException(
			// "Array write with a non-concrete length", source);
		}
		int dataSizeInt = ((IntegerNumber) dataSizeConcrete).intValue();
		for (int j = 0; j < dataSizeInt; j++) {
			SymbolicExpression elementInDataArray = null;
			NumericExpression jVal = universe.integer(j);

			elementInDataArray = universe.arrayRead(dataSequence, jVal);
			flattenArray = universe.arrayWrite(flattenArray, i, elementInDataArray);
			i = universe.add(i, one);
		}
		flattenArray = arrayCasting(state, pid, flattenArray,
				new ArrayMeasurement((SymbolicArrayType) flattenArray.type()), (SymbolicCompleteArrayType) array.type(),
				source);
		return new Evaluation(state, flattenArray);
	}

	/**
	 * <p>
	 * Pre-condition: array has a complete array type. <br>
	 * dimension(array) == arraySliceSizes.length
	 * </p>
	 * <p>
	 * Given a complete array a[N0][N1]..[Nn] (n >= 0), flatten a to a
	 * one-dimensional array a'[N0 * N1 * .. * Nn]. The value of a' will be <code>
	 * let f(i, j) := Ni * Ni+1 * .. * Nj (j > i);
	 * 
	 * lambda int i : a[i/f(1,n)][i%f(1,n)/f(2,n)][i%f(1,n)%f(2,n)/f(3,n)]...[i%f(1,n)%..%f(n-1,n)]
	 * </code> <br>
	 * This method is used to flatten a multiple dimensional array with non-concrete
	 * size into an one-dimensional array.
	 * </p>
	 * 
	 * @param state           The current state when this method is called
	 * @param array           The array that will be flattened.
	 * @param arraySliceSizes An sequence of size of slices of the parameter 'array'
	 * @param arrayExtents    An sequence of extents of the parameter 'array'
	 * @param civlsource      The {@link CIVLSource} corresponding to this method
	 *                        call
	 * @return A flattened array
	 */
	private SymbolicExpression arrayLambdaFlatten2(State state, SymbolicExpression array,
			NumericExpression[] arraySliceSizes, NumericExpression[] arrayExtents, CIVLSource civlsource) {
		SymbolicCompleteArrayType arrayType = (SymbolicCompleteArrayType) array.type();
		int dim = arrayType.dimensions();
		int newDim;
		// pre-process: preprocess an array
		// a[1][1][...][1][n][...][m] to a'[n][...][m]:
		for (int i = 0; i < dim - 1; i++)
			if (arrayExtents[i].isOne()) {
				array = universe.arrayRead(array, zero);
			} else
				break;
		// this cast is guaranteed to be correct since the loop above loops from
		// 0 to dim - 1...
		arrayType = (SymbolicCompleteArrayType) array.type();
		newDim = arrayType.dimensions();
		if (newDim == 1)
			return array;
		if (newDim < dim) {
			arraySliceSizes = Arrays.copyOfRange(arraySliceSizes, dim - newDim, dim);
			dim = newDim;
		}
		// end of pre-process

		NumericSymbolicConstant symConst = (NumericSymbolicConstant) universe
				.symbolicConstant(universe.stringObject("i"), universe.integerType());
		NumericExpression extent = arrayType.extent();
		NumericExpression index = symConst;
		SymbolicExpression arrayReadFunc = array;

		for (int d = 0; d < dim; d++) {
			arrayReadFunc = universe.arrayRead(arrayReadFunc, universe.divide(index, arraySliceSizes[d]));
			index = universe.modulo(index, arraySliceSizes[d]);
		}
		arrayType = universe.arrayType(arrayReadFunc.type(), universe.multiply(arraySliceSizes[0], extent));
		return universe.arrayLambda(arrayType, universe.lambda(symConst, arrayReadFunc));
	}

	/**
	 * Helper function of report an out of bound error.
	 * 
	 * @param state       The current state
	 * @param process     The string identifier of the process
	 * @param claim       The {@link BooleanExpression} of the predicate (optional,
	 *                    can be null)
	 * @param resultType  The {@link ResultType} of reasoning the predicate
	 *                    (optional, can be null)
	 * @param pointer     The pointer to the array
	 * @param arrayLength The length of the array
	 * @param offset      The offset of the element from the position pointed by
	 *                    pointer
	 * @param source
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private void reportOutOfBoundError(State state, int pid, BooleanExpression claim, ResultType resultType,
			SymbolicExpression pointer, NumericExpression arrayLength, NumericExpression offset, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		String message = "Out of bound error may happen when access on an array element.\n" + "Pointer:"
				+ symbolicAnalyzer.symbolicExpressionToString(source, state, null, pointer) + "\n" + "Offset:"
				+ symbolicAnalyzer.symbolicExpressionToString(source, state, null, offset) + "\n" + "Array length:"
				+ symbolicAnalyzer.symbolicExpressionToString(source, state, null, arrayLength);

		if (claim != null && resultType != null)
			state = errorLogger.logError(source, state, pid, symbolicAnalyzer.stateInformation(state), claim,
					resultType, CIVLProperty.OUT_OF_BOUNDS, message);
		else
			errorLogger.logSimpleError(source, state, pid, state.getProcessState(pid).name(),
					symbolicAnalyzer.stateInformation(state), CIVLProperty.OUT_OF_BOUNDS, message);
		throw new UnsatisfiablePathConditionException();
	}
}
