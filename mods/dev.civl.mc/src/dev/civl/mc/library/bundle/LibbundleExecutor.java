package dev.civl.mc.library.bundle;

import java.util.Arrays;

import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.library.common.BaseLibraryExecutor;
import dev.civl.mc.model.IF.CIVLProperty;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.semantics.IF.ArrayToolBox;
import dev.civl.mc.semantics.IF.ArrayToolBox.ArrayShape;
import dev.civl.mc.semantics.IF.ArrayToolBox.ArraySlice;
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
import dev.civl.sarl.IF.expr.SymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;
import dev.civl.sarl.IF.number.IntegerNumber;
import dev.civl.sarl.IF.number.Number;
import dev.civl.sarl.IF.object.SymbolicObject;
import dev.civl.sarl.IF.type.SymbolicArrayType;
import dev.civl.sarl.IF.type.SymbolicCompleteArrayType;
import dev.civl.sarl.IF.type.SymbolicFunctionType;
import dev.civl.sarl.IF.type.SymbolicType;
import dev.civl.sarl.IF.type.SymbolicType.SymbolicTypeKind;
import dev.civl.sarl.object.common.SimpleSequence;

/**
 * <p>
 * Specification for bundle operations:<br>
 * The specification of bundle pack/unpack is essentially the specification of
 * get/set data from input/output arguments. Since CIVL implements multiple
 * dimensional arrays as nested arrays, assigning a set of data to a multiple
 * dimensional array will possibly involve several parts of different sub-arrays
 * inside a nested array. So the following description will note some
 * explanation of general cases for this get/set input/output arguments problem
 * which is totally irrelevant to bundle pack/unpack.
 * </p>
 * 
 * 
 * $bundle $bundle_pack(void *ptr, int size):<br>
 * <p>
 * Putting the whole or part of the object pointed by the first argument into
 * returned a bundle object.<br>
 * the first argument "ptr" is a pointer to the object part of which is going to
 * be assigned to the returned bundle type object. The second argument specifies
 * the size of the object pointed by the first argument. Here size means the
 * size of the data type times the the number of the elements of such data type
 * which are consisted of the data object will be packed in bundle.<br>
 * Note: For general cases, if some input argument, which happens to be a
 * pointer, has a specified data type, it's unnecessary to give the size unless
 * the function is just expecting part of the object pointed.
 * </p>
 * 
 * void $bundle_unpack($bundle bundle, void *ptr):
 * <p>
 * Extracting the whole data from a given bundle and assigning it to another
 * object pointed by the second argument. The pre-condition is the receiving
 * object must be able to contain the whole data object.<br>
 * The first argument is the bundle object which will be extracted. The second
 * argument is a pointer to receiving object. The pre-condition mentioned above
 * is defined as: If the receiving object has a compatible data type of itself
 * or elements of it with the data itself or elements of the data inside the
 * bundle and the size of the object (sometime it's just part of the object
 * because of different positions pointed by the pointer) is greater than or
 * equal to data in bundle, it's able to contain the whole data object. <br>
 * Note: For general setting output arguments cases, this precondition should
 * also hold. The only thing different is the data in bundle here can be data
 * from anywhere(Obviously general cases are irrelevant with bundle stuff).<br>
 * </p>
 * 
 * 
 */

public class LibbundleExecutor extends BaseLibraryExecutor
		implements
			LibraryExecutor {
	/**
	 * The reference to {@link ArrayToolBox}
	 */
	private ArrayToolBox arrayToolBox;

	/**
	 * <p>
	 * This is the name of a abstract function :
	 * <code>$bundle_subarray(array, indices, count)</code> which represents
	 * consecutive part of "count" elements of the "array", starting from the
	 * "indices".
	 * </p>
	 * 
	 * <p>
	 * The purpose of this abstract function is to relieve the $bundle_pack
	 * function from getting data of non-concrete size from an array of
	 * non-concrete sizes, since the data will later be unpack by $bundle_unpack
	 * and what shape it eventually will be is unknown to $bundle_pack.
	 * </p>
	 */
	private static String BUNDLE_SUBARRAY_FUNCTION_NAME = "$bundle_slice";

	/* **************************** Constructors *************************** */

	public LibbundleExecutor(String name, Executor primaryExecutor,
			ModelFactory modelFactory, SymbolicUtility symbolicUtil,
			SymbolicAnalyzer symbolicAnalyzer, CIVLConfiguration civlConfig,
			LibraryExecutorLoader libExecutorLoader,
			LibraryEvaluatorLoader libEvaluatorLoader) {
		super(name, primaryExecutor, modelFactory, symbolicUtil,
				symbolicAnalyzer, civlConfig, libExecutorLoader,
				libEvaluatorLoader);
		arrayToolBox = evaluator.newArrayToolBox(universe);
	}

	/*
	 * ******************** Methods from BaseLibraryExecutor *******************
	 */

	@Override
	protected Evaluation executeValue(State state, int pid, String process,
			CIVLSource source, String functionName, Expression[] arguments,
			SymbolicExpression[] argumentValues)
			throws UnsatisfiablePathConditionException {
		Evaluation callEval = null;

		switch (functionName) {
			case "$bundle_pack" :
				callEval = executeBundlePack(state, pid, process, arguments,
						argumentValues, source);
				break;
			case "$bundle_size" :
				callEval = executeBundleSize(state, pid, process, arguments,
						argumentValues, source);
				break;
			case "$bundle_unpack" :
				callEval = executeBundleUnpack(state, pid, process, arguments,
						argumentValues, source);
				break;
		}
		return callEval;
	}

	/* ************************** Private Methods ************************** */

	/**
	 * Returns the size of a bundle.
	 * 
	 * @param state
	 *            The current state.
	 * @param pid
	 *            The ID of the process that the function call belongs to.
	 * @param lhs
	 *            The left hand side expression of the call, which is to be
	 *            assigned with the returned value of the function call. If NULL
	 *            then no assignment happens.
	 * @param arguments
	 *            The static representation of the arguments of the function
	 *            call.
	 * @param argumentValues
	 *            The dynamic representation of the arguments of the function
	 *            call.
	 * @param civlSource
	 *            The source code element to be used for error report.
	 * @return The new state after executing the function call.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeBundleSize(State state, int pid, String process,
			Expression[] arguments, SymbolicExpression[] argumentValues,
			CIVLSource civlSource) throws UnsatisfiablePathConditionException {
		SymbolicObject arrayObject;
		SymbolicExpression array;

		assert arguments.length == 1;
		assert argumentValues[0].operator() == SymbolicOperator.UNION_INJECT;
		arrayObject = argumentValues[0].argument(1);
		assert arrayObject instanceof SymbolicExpression;
		array = (SymbolicExpression) arrayObject;
		return new Evaluation(state,
				typeFactory.sizeofDynamicType(array.type()));
	}

	/**
	 * Creates a bundle from the memory region specified by ptr and size,
	 * copying the data into the new bundle:
	 * 
	 * <code>$bundle $bundle_pack(void *ptr, int size);</code>
	 * 
	 * Copies the data out of the bundle into the region specified:
	 * 
	 * <code>void $bundle_unpack($bundle bundle, void *ptr, int size);</code>
	 * 
	 * Pre-Condition : The size of the object pointed by the given address
	 * should larger than or equal to the other parameter "size".<br>
	 * 
	 * @param state
	 *            The current state.
	 * @param pid
	 *            The ID of the process that the function call belongs to.
	 * @param process
	 *            The information of the process.
	 * @param bundleType
	 *            The bundle type of the model.
	 * @param lhs
	 *            The left hand side expression of the call, which is to be
	 *            assigned with the returned value of the function call. If NULL
	 *            then no assignment happens.
	 * @param arguments
	 *            The static representation of the arguments of the function
	 *            call.
	 * @param argumentValues
	 *            The dynamic representation of the arguments of the function
	 *            call.
	 * @param source
	 *            The source code element to be used for error report.
	 * @return The new state after executing the function call.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeBundlePack(State state, int pid, String process,
			Expression[] arguments, SymbolicExpression[] argumentValues,
			CIVLSource source) throws UnsatisfiablePathConditionException {
		CIVLSource ptrSource = arguments[0].getSource();
		SymbolicExpression pointer = argumentValues[0], rootPointer, rootArray;
		NumericExpression size = (NumericExpression) argumentValues[1];
		Evaluation eval;

		rootPointer = symbolicUtil.arrayRootPtr(pointer);
		eval = evaluator.dereference(ptrSource, state, pid, process,
				rootPointer, true, true);
		state = eval.state;
		rootArray = eval.value;

		// error checking
		NumericExpression availableSize, baseSize;
		BooleanExpression inBound, baseDivides;
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		ResultType resultType;
		// rootArrayShape is null if data is not an array:
		ArrayShape rootArrayShape = null;
		// startIndices is null if data is not an array:
		NumericExpression[] startIndices = null;

		if (rootArray.type().typeKind() == SymbolicTypeKind.ARRAY) {
			rootArrayShape = arrayToolBox
					.newArrayShape((SymbolicArrayType) rootArray.type());
			startIndices = symbolicUtil.extractArrayIndicesFrom(pointer);
			availableSize = bytewiseDataSize(rootArrayShape, startIndices);
			baseSize = typeFactory.sizeofDynamicType(rootArrayShape.baseType);
		} else
			baseSize = availableSize = typeFactory
					.sizeofDynamicType(rootArray.type());
		inBound = universe.lessThanEquals(size, availableSize);
		baseDivides = universe.divides(baseSize, availableSize);
		resultType = reasoner.valid(universe.and(baseDivides, inBound))
				.getResultType();
		if (resultType != ResultType.YES
				&& civlConfig.isPropertyToggled(CIVLProperty.OUT_OF_BOUNDS))
			eval.state = reportBundlePackError(state, pid, pointer,
					availableSize, size, universe.and(baseDivides, inBound),
					resultType, source);
		eval.value = pack(reasoner, rootArray, rootArrayShape, startIndices,
				size, source);
		// Packing bundle:
		eval.value = universe.unionInject(
				typeFactory.bundleType().getDynamicType(universe),
				universe.intObject(typeFactory.bundleType()
						.getIndexOf(universe.pureType(eval.value.type()))),
				eval.value);
		return eval;
	}

	/**
	 * pack the data, assuming no error can happen. If the data is a single
	 * object, pack the object, otherwise, pack the data as an array slice whose
	 * base type is a non-array type.
	 */
	private SymbolicExpression pack(Reasoner reasoner,
			SymbolicExpression rootMemoryArray, ArrayShape rootMemoryArrayShape,
			NumericExpression startIndices[], NumericExpression size,
			CIVLSource source) {
		assert rootMemoryArrayShape == null || startIndices != null;

		if (rootMemoryArrayShape == null)
			return rootMemoryArray;

		NumericExpression baseSize = typeFactory
				.sizeofDynamicType(rootMemoryArrayShape.baseType);
		NumericExpression count = universe.divide(size, baseSize);
		Number concreteCount = reasoner.extractNumber(count);

		if (concreteCount != null) {
			startIndices = zeroFill(startIndices,
					rootMemoryArrayShape.dimensions);
			return arrayToolBox.arraySliceRead(rootMemoryArray, startIndices,
					((IntegerNumber) concreteCount).intValue());
		}

		NumericExpression[] startTrimedIndices = zeroTrim(reasoner,
				startIndices);

		startIndices = zeroFill(startIndices, rootMemoryArrayShape.dimensions);
		if (startTrimedIndices.length == 0) // if all indices are zeroes
			if (reasoner.isValid(
					universe.equals(count, rootMemoryArrayShape.arraySize)))
				packAsSliceFunction(rootMemoryArray, startIndices, count,
						universe.arrayType(rootMemoryArrayShape.baseType,
								count),
						source);

		ArraySlice slice = arrayToolBox.newArraySlice(rootMemoryArray,
				startIndices, count, rootMemoryArrayShape.baseType);
		ArraySlice narrowedSlice = arraySliceNarrower(reasoner, slice,
				rootMemoryArrayShape);
		ArrayShape narrowerArrayShape = narrowedSlice == slice
				? rootMemoryArrayShape
				: arrayToolBox.newArrayShape(
						(SymbolicArrayType) narrowedSlice.array.type());

		return packAsSliceFunction(narrowedSlice.array,
				zeroFill(narrowedSlice.startIndices,
						narrowerArrayShape.dimensions),
				narrowedSlice.count,
				universe.arrayType(narrowedSlice.baseType, narrowedSlice.count),
				source);
	}

	/**
	 * <p>
	 * read an array slice, which is represented by an array <code>a</code>, a
	 * group of starting indices
	 * <code>{i<sub>0</sub>, i<sub>1</sub>, ..., i<sub>n-1</sub>}</code> and a
	 * count <code>c</code> and <code>c</code> is non-concrete.
	 * </p>
	 * 
	 * <p>
	 * In this case, the slice will be wrapped by an uninterpreted function
	 * {@link #BUNDLE_SUBARRAY_FUNCTION_NAME}
	 * </p>
	 * 
	 * @throws UnsatisfiablePathConditionException
	 */
	private SymbolicExpression packAsSliceFunction(SymbolicExpression array,
			NumericExpression indices[], NumericExpression count,
			SymbolicType sliceType, CIVLSource source) {
		/*
		 * The idea is not deal with non-concrete array reading at bundle
		 * packing time since no one knows what the shape of the data (array)
		 * would be at unpack time. Just wrapping all necessary information with
		 * a unique protocol to bundle_unpack.
		 */
		SymbolicExpression indicesArray = universe.array(universe.integerType(),
				Arrays.asList(indices));
		SymbolicType outputType = sliceType;
		SymbolicFunctionType funcType;

		funcType = universe.functionType(Arrays.asList(array.type(),
				indicesArray.type(), universe.integerType()), outputType);

		SymbolicExpression symConst = universe.symbolicConstant(
				universe.stringObject(BUNDLE_SUBARRAY_FUNCTION_NAME), funcType);

		return universe.apply(symConst,
				Arrays.asList(array, indicesArray, count));
	}

	/**
	 * Copies the data out of the bundle into the region specified:
	 * 
	 * void $bundle_unpack($bundle bundle, void *ptr); <br>
	 * 
	 * Pre-Condition : The data in bundle is in the form of a flattened one
	 * dimensional array.<br>
	 * 
	 * @see{executeBunldePack :post-condition.<br>
	 * 
	 * 
	 * @author Ziqing Luo
	 * 
	 * @param state
	 *            The current state.
	 * @param pid
	 *            The ID of the process that the function call belongs to.
	 * @param arguments
	 *            The static representation of the arguments of the function
	 *            call.
	 * @param argumentValues
	 *            The dynamic representation of the arguments of the function
	 *            call.
	 * @param source
	 *            The source code element to be used for error report.
	 * @return The new state after executing the function call.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeBundleUnpack(State state, int pid, String process,
			Expression[] arguments, SymbolicExpression[] argumentValues,
			CIVLSource source) throws UnsatisfiablePathConditionException {
		// do error checking, then call "unpack" ...
		CIVLSource ptrSource = arguments[1].getSource();
		SymbolicExpression bundle = argumentValues[0];
		SymbolicExpression pointer = argumentValues[1];
		SymbolicExpression bundleData = (SymbolicExpression) bundle.argument(1);
		SymbolicExpression rootPointer = symbolicUtil.arrayRootPtr(pointer);
		Evaluation eval = evaluator.dereference(ptrSource, state, pid, process,
				rootPointer, false, false);
		SymbolicExpression wrtArray = eval.value;

		NumericExpression availableSize, sizeofBundleData;
		SymbolicType wrtArrayBase_t, bundleDataBase_t;
		ArrayShape wrtArrayShape = null, dataArrayShape = null;
		Reasoner reasoner = universe.reasoner(state.getPathCondition(universe));
		NumericExpression wrtStartIndices[] = symbolicUtil
				.extractArrayIndicesFrom(pointer);
		SymbolicType wrtArrayType = !wrtArray.isNull()
				? wrtArray.type()
				: symbolicAnalyzer
						.civlTypeOfObjByPointer(ptrSource, state, rootPointer)
						.getDynamicType(universe);

		// compute available size ...
		if (wrtArrayType.typeKind() == SymbolicTypeKind.ARRAY) {
			wrtArrayShape = arrayToolBox
					.newArrayShape((SymbolicArrayType) wrtArrayType);
			wrtArrayBase_t = wrtArrayShape.baseType;
			availableSize = bytewiseDataSize(wrtArrayShape, wrtStartIndices);
		} else {
			availableSize = typeFactory.sizeofDynamicType(wrtArrayType);
			wrtArrayBase_t = wrtArrayType;
		}
		// compute bundle data size ...
		if (bundleData.type().typeKind() == SymbolicTypeKind.ARRAY) {
			NumericExpression zero[] = {universe.zeroInt()};

			dataArrayShape = arrayToolBox
					.newArrayShape((SymbolicArrayType) bundleData.type());
			sizeofBundleData = bytewiseDataSize(dataArrayShape, zero);
			bundleDataBase_t = dataArrayShape.baseType;
		} else {
			bundleDataBase_t = bundleData.type();
			sizeofBundleData = typeFactory.sizeofDynamicType(bundleDataBase_t);
		}

		ResultType errorCheckingResult = ResultType.NO;
		BooleanExpression inBoundClaim = universe
				.lessThanEquals(sizeofBundleData, availableSize);

		if (bundleDataBase_t.equals(wrtArrayBase_t)) {
			if (universe.getShowProverQueries())
				universe.setQueryExplanation("checking $bundle_unpack size in bounds at " + source.getLocation());
			errorCheckingResult = reasoner.valid(inBoundClaim).getResultType();
			if (errorCheckingResult != ResultType.NO) {
				wrtArray = unpack(reasoner, bundleData, dataArrayShape,
						wrtArray, wrtArrayShape, wrtStartIndices);
				state = primaryExecutor.assign(source, state, pid, rootPointer,
						wrtArray);
			}
		}
		if (errorCheckingResult != ResultType.YES
				&& civlConfig.isPropertyToggled(CIVLProperty.OUT_OF_BOUNDS))
			state = reportBundleUnpackError(state, pid, pointer, bundleData,
					typeFactory.bundleType(), sizeofBundleData, availableSize,
					inBoundClaim, errorCheckingResult, source);
		return new Evaluation(state, universe.nullExpression());
	}

	/**
	 * Unpack the bundle and write it to the receiving array. Assuming no error
	 * can happen.
	 * 
	 * @param reasoner
	 *            a {@link Reasoner} based on current context
	 * @param bundleData
	 *            the data in bundle
	 * @param bundleSliceShape
	 *            the shape of the slice in bundle, generated from the slice
	 *            type (i.e. the type of bundleData). null iff the bundle data
	 *            does not have an array type
	 * @param memoryArr
	 *            the array that will be written
	 * @param memoryArrShape
	 *            the {@link ArrayShape} of the array that will be written. null
	 *            iff the receiving object does not have an array type
	 * @param memoryArrStartIdx
	 *            the start indices referring to the position of writing
	 * @return the updated receiving array
	 */
	private SymbolicExpression unpack(Reasoner reasoner,
			SymbolicExpression bundleData, ArrayShape bundleSliceShape,
			SymbolicExpression memoryArr, ArrayShape memoryArrShape,
			NumericExpression memoryArrStartIdx[]) {
		if (bundleSliceShape == null || memoryArrShape == null) {
			// data is a single object
			if (bundleSliceShape == null && memoryArrShape == null)
				return bundleData;
			else if (memoryArrShape != null) {
				memoryArrStartIdx = zeroFill(memoryArrStartIdx,
						memoryArrShape.dimensions);
				return arrayToolBox.mdArrayWrite(memoryArr, memoryArrStartIdx,
						bundleData);
			} else {
				ArraySlice bundleSlice = extractBundleData(bundleData);

				return universe.arrayRead(
						arrayToolBox.extractArraySlice(bundleSlice), zero);
			}
		}
		// base types are all non-array types :
		assert bundleSliceShape.baseType.typeKind() != SymbolicTypeKind.ARRAY;
		assert memoryArrShape.baseType.typeKind() != SymbolicTypeKind.ARRAY;

		ArraySlice bundleSlice = extractBundleData(bundleData);

		SymbolicExpression quick_result = unpack_bundleSliceFitInMemorySubArray(
				reasoner, bundleSlice, bundleSliceShape, memoryArr,
				memoryArrShape, zeroTrim(reasoner, memoryArrStartIdx));

		if (quick_result != null)
			return quick_result;
		memoryArrStartIdx = zeroFill(memoryArrStartIdx,
				memoryArrShape.dimensions);

		// the exact memory slice that will be written :
		ArraySlice memorySlice = arrayToolBox.newArraySlice(memoryArr,
				memoryArrStartIdx, bundleSliceShape.arraySize,
				memoryArrShape.baseType);
		ArraySlice narrowedMemorySlice = arraySliceNarrower(reasoner,
				memorySlice, memoryArrShape);
		// indices for writing the memory slice to memory array :
		NumericExpression memorySliceIndices[] = null;

		if (memorySlice != narrowedMemorySlice) {
			ArrayShape memorySliceArrayShape = arrayToolBox.newArrayShape(
					(SymbolicArrayType) narrowedMemorySlice.array.type());

			memorySliceIndices = new NumericExpression[memoryArrShape.dimensions
					- memorySliceArrayShape.dimensions];
			if (memorySliceIndices.length <= memoryArrStartIdx.length)
				System.arraycopy(memoryArrStartIdx, 0, memorySliceIndices, 0,
						memorySliceIndices.length);
			else
				Arrays.fill(memorySliceIndices, zero);
			memoryArrShape = memorySliceArrayShape;
		}

		// find out the largest common base type of the bundle slice and the
		// memory slice:
		Pair<ArraySlice[], ArrayShape[]> commonBaseSlicesAndShapes = commonBaseType(
				reasoner, bundleSlice,
				arrayToolBox.newArrayShape(
						(SymbolicArrayType) bundleSlice.array.type()),
				narrowedMemorySlice, memoryArrShape);

		bundleSlice = commonBaseSlicesAndShapes.left[0];
		memorySlice = commonBaseSlicesAndShapes.left[1];
		memoryArrShape = commonBaseSlicesAndShapes.right[1];

		SymbolicExpression updatedMemroySliceValue = arrayToolBox
				.arraySliceWrite(bundleSlice, memorySlice.array, memoryArrShape,
						zeroFill(memorySlice.startIndices,
								memoryArrShape.dimensions));

		// write updated memory slice back to memory array:
		if (memorySliceIndices != null)
			return arrayToolBox.mdArrayWrite(memoryArr, memorySliceIndices,
					updatedMemroySliceValue);
		else
			return updatedMemroySliceValue;
	}

	/**
	 * given an array slice: array <code>a</code>, indices
	 * <code>{i<sub>0</sub>, i<sub>1</sub>...}</code>, number of base type
	 * elements <code>c</code>. Returns the same slice but the array
	 * <code>a</code> may change to its sub-array <code>a'</code>, and indices
	 * may have corresponding changes.
	 * 
	 * @param reasoner
	 *            a {@link Reasoner} based on the current context
	 * @param slice
	 *            an array slice
	 * @param sliceArrayShape
	 *            the shape of the array where the slice is carved out
	 * @return an {@link ArraySlice} represent the same slice that is
	 *         represented by the given inputs
	 */
	private ArraySlice arraySliceNarrower(Reasoner reasoner, ArraySlice slice,
			ArrayShape sliceArrayShape) {
		int dims = sliceArrayShape.dimensions;

		if (dims <= 1)
			return slice;

		NumericExpression size;
		NumericExpression sliceIndices[] = zeroFill(slice.startIndices, dims);
		NumericExpression narrowingIndices[];
		SymbolicExpression narrowedArray;

		for (int i = 1; i < dims; i++) {
			size = universe.multiply(sliceArrayShape.subArraySizes[i], universe
					.subtract(sliceArrayShape.extents[i], sliceIndices[i]));
			if (!reasoner.isValid(universe.lessThanEquals(slice.count, size))) {
				if (i == 1)
					return slice;

				narrowingIndices = Arrays.copyOfRange(sliceIndices, 0, i - 1);
				narrowedArray = arrayToolBox.mdArrayRead(slice.array,
						narrowingIndices);
				return arrayToolBox.newArraySlice(narrowedArray,
						Arrays.copyOfRange(sliceIndices, i - 1, dims),
						slice.count, slice.baseType);
			}
		}
		narrowingIndices = Arrays.copyOfRange(sliceIndices, 0, dims - 1);
		narrowedArray = arrayToolBox.mdArrayRead(slice.array, narrowingIndices);
		return arrayToolBox.newArraySlice(narrowedArray,
				Arrays.copyOfRange(sliceIndices, dims - 1, dims), slice.count,
				slice.baseType);
	}

	/**
	 * Find the common base type of two array shapes. TODO: so far it requires
	 * that the given two array shapes are not sub-array of each other. Such
	 * restriction should be lifted.
	 */
	private Pair<ArraySlice[], ArrayShape[]> commonBaseType(Reasoner reasoner,
			ArraySlice s0, ArrayShape s0ArrayShape, ArraySlice s1,
			ArrayShape s1ArrayShape) {
		assert s0.baseType.typeKind() != SymbolicTypeKind.ARRAY;
		assert s1.baseType.typeKind() != SymbolicTypeKind.ARRAY;
		assert reasoner.isValid(universe.equals(s0.count, s1.count));

		NumericExpression count = s0.count, newCount;
		NumericExpression[] trimedS0Indices = zeroTrim(reasoner,
				s0.startIndices);
		NumericExpression[] trimedS1Indices = zeroTrim(reasoner,
				s1.startIndices);
		int s0BaseMaxDims = s0ArrayShape.dimensions - trimedS0Indices.length;
		int s1BaseMaxDims = s1ArrayShape.dimensions - trimedS1Indices.length;
		int maxDims = s0BaseMaxDims > s1BaseMaxDims
				? s1BaseMaxDims
				: s0BaseMaxDims;
		int s0Dims = s0ArrayShape.dimensions;
		int s1Dims = s1ArrayShape.dimensions;
		ArraySlice resultSlices[] = {s0, s1};
		ArrayShape resultShapes[] = {s0ArrayShape, s1ArrayShape};

		if (maxDims < 1)
			return new Pair<>(resultSlices, resultShapes);

		for (int i = 0; i < maxDims; i++) {
			BooleanExpression divides = universe
					.divides(s0ArrayShape.extents[s0Dims - 1 - i], count);
			BooleanExpression equals = universe.equals(
					s0ArrayShape.extents[s0Dims - 1 - i],
					s1ArrayShape.extents[s1Dims - 1 - i]);

			if (!reasoner.isValid(universe.and(divides, equals))) {
				if (i != 0) {
					newCount = universe.divide(count,
							s0ArrayShape.subArraySizes[s0Dims - 1 - i]);
					resultShapes[0] = arrayToolBox.switchBaseType(s0ArrayShape,
							i);
					resultShapes[1] = arrayToolBox.switchBaseType(s1ArrayShape,
							i);
					resultSlices[0] = arrayToolBox.newArraySlice(s0.array,
							zeroFill(trimedS0Indices, s0Dims - i), newCount,
							resultShapes[0].baseType);
					resultSlices[1] = arrayToolBox.newArraySlice(s1.array,
							zeroFill(trimedS1Indices, s1Dims - i), newCount,
							resultShapes[1].baseType);
				}
				return new Pair<>(resultSlices, resultShapes);
			}
		}
		// maxDims should never be greater than or equal to array dimensions,
		// otherwise, count can only be one, the control will not reach here
		newCount = universe.divide(count,
				s0ArrayShape.subArraySizes[s0Dims - 1 - maxDims]);
		resultShapes[0] = arrayToolBox.switchBaseType(s0ArrayShape, maxDims);
		resultShapes[1] = arrayToolBox.switchBaseType(s1ArrayShape, maxDims);
		resultSlices[0] = arrayToolBox.newArraySlice(s0.array,
				zeroFill(trimedS0Indices, s0Dims - maxDims), newCount,
				resultShapes[0].baseType);
		resultSlices[1] = arrayToolBox.newArraySlice(s1.array,
				zeroFill(trimedS1Indices, s1Dims - maxDims), newCount,
				resultShapes[1].baseType);
		return new Pair<>(resultSlices, resultShapes);
	}

	/**
	 * Deal with two special cases:
	 * 
	 * <p>
	 * If the given bundle slice type is a sub-type of the written array,
	 * directly written the slice to the referred position.
	 * </p>
	 * 
	 * <p>
	 * If the given bundle slice type is physically equivalent to a sub-type of
	 * the written array, directly written the reshaped slice to the referred
	 * position.
	 * </p>
	 * 
	 * <p>
	 * returns the bundle unpack result if the given slice and the written array
	 * falls into the above cases, otherwise null.
	 * </p>
	 * 
	 * @param reasoner
	 *            a reasoner based on the current context
	 * @param bundleSlice
	 *            the bundle data in {@link ArraySlice} form
	 * @param bundleSliceShape
	 *            the {@link ArrayShape} based on the
	 *            {@link ArraySlice#sliceType} of the bundle data
	 * @param memoryArray
	 *            the written array
	 * @param memoryArrayShape
	 *            the {@link ArrayShape} of the written array
	 * @param memoryStartTrimedIndices
	 *            the starting indices for writing the array without zero suffix
	 * @return the bundle unpack result if the given slice and the written array
	 *         falls into the above cases, otherwise null.
	 */
	private SymbolicExpression unpack_bundleSliceFitInMemorySubArray(
			Reasoner reasoner, ArraySlice bundleSlice,
			ArrayShape bundleSliceShape, SymbolicExpression memoryArray,
			ArrayShape memoryArrayShape,
			NumericExpression memoryStartTrimedIndices[]) {
		int memorySubarrayDimsMax = memoryArrayShape.dimensions
				- memoryStartTrimedIndices.length;

		if (memorySubarrayDimsMax >= bundleSliceShape.dimensions)
			if (isSubArray(reasoner, bundleSliceShape, memoryArrayShape)) {
				// case 1
				// adjust the written array indices s.t. they must refer to the
				// element who has the same type as the data array type ...
				NumericExpression[] wrtIndices = zeroFill(
						memoryStartTrimedIndices, memoryArrayShape.dimensions
								- bundleSliceShape.dimensions);
				SymbolicExpression sliceValue = arrayToolBox
						.extractArraySlice(bundleSlice);

				return wrtIndices.length > 0
						? arrayToolBox.mdArrayWrite(memoryArray, wrtIndices,
								sliceValue)
						: sliceValue;
			} else {
				// case 2
				// any physically equivalent sub-array ?
				ArrayShape wrtSubArrayShape;

				do {
					wrtSubArrayShape = arrayToolBox.subArrayShape(
							memoryArrayShape, memorySubarrayDimsMax--);
					if (reasoner.isValid(
							arrayToolBox.areArrayShapesPhysicallyEquivalent(
									bundleSliceShape, wrtSubArrayShape))) {
						SymbolicExpression reshapedSliceValue = arrayToolBox
								.arrayReshape(arrayToolBox.extractArraySlice(
										bundleSlice), wrtSubArrayShape);
						NumericExpression[] wrtIndices = zeroFill(
								memoryStartTrimedIndices,
								memoryArrayShape.dimensions
										- wrtSubArrayShape.dimensions);

						return wrtIndices.length > 0
								? arrayToolBox.mdArrayWrite(memoryArray,
										wrtIndices, reshapedSliceValue)
								: reshapedSliceValue;
					}
				} while (memorySubarrayDimsMax >= bundleSliceShape.dimensions);
			}
		return null;
	}

	/**
	 * Extract bundle data to an {@link ArraySlice}
	 */
	private ArraySlice extractBundleData(SymbolicExpression bundleData) {
		if (isBundleSubarrayFunction(bundleData)) {
			@SuppressWarnings("unchecked")
			SimpleSequence<SymbolicExpression> args = (SimpleSequence<SymbolicExpression>) bundleData
					.argument(1);
			SymbolicExpression array = args.get(0);
			SymbolicExpression indicesArray = args.get(1);
			NumericExpression count = (NumericExpression) args.get(2);
			NumericExpression readStartIndices[] = new NumericExpression[((IntegerNumber) universe
					.extractNumber(universe.length(indicesArray))).intValue()];
			SymbolicType sliceBaseType = ((SymbolicArrayType) bundleData.type())
					.elementType();

			for (int i = 0; i < readStartIndices.length; i++)
				readStartIndices[i] = (NumericExpression) universe
						.arrayRead(indicesArray, universe.integer(i));
			return arrayToolBox.newArraySlice(array, readStartIndices, count,
					sliceBaseType);
		} else {
			SymbolicCompleteArrayType dataArrayType = (SymbolicCompleteArrayType) bundleData
					.type();
			NumericExpression indices[] = {universe.zeroInt()};

			return arrayToolBox.newArraySlice(bundleData, indices,
					dataArrayType.extent(), dataArrayType.elementType());
		}
	}

	/**
	 * return byte-wise size of data in an array of the given array shape that
	 * starting from the given indices. e.g. given <code>int a[N][M]</code> and
	 * indices <code>{1,1}</code>, the data size is
	 * <code>sizeof_int * ( M * N - (M + 1))</code>
	 * 
	 * @param arrayShape
	 * @param startIndices
	 * @return
	 */
	private NumericExpression bytewiseDataSize(ArrayShape arrayShape,
			NumericExpression[] startIndices) {
		NumericExpression base = typeFactory
				.sizeofDynamicType(arrayShape.baseType);
		NumericExpression total = universe.multiply(Arrays.asList(base,
				arrayShape.extents[0], arrayShape.subArraySizes[0]));

		NumericExpression unavailable = universe.zeroInt();

		for (int i = 0; i < startIndices.length; i++)
			unavailable = universe.add(unavailable, universe
					.multiply(startIndices[i], arrayShape.subArraySizes[i]));
		unavailable = universe.multiply(unavailable, base);
		return universe.subtract(total, unavailable);
	}

	/**
	 * @param expr
	 * @return true iff the bundle data is encoded as a special abstract
	 *         function {@link #BUNDLE_SUBARRAY_FUNCTION_NAME}
	 * 
	 */
	private static boolean isBundleSubarrayFunction(SymbolicExpression expr) {
		if (expr.operator() == SymbolicOperator.APPLY) {
			SymbolicConstant funcIdent = (SymbolicConstant) expr.argument(0);

			return funcIdent.name().getString()
					.equals(BUNDLE_SUBARRAY_FUNCTION_NAME);
		}
		return false;
	}

	/**
	 * @param reasoner
	 * @param s0
	 * @param s1
	 * @return true iff s0 is a sub-array of s1
	 */
	private boolean isSubArray(Reasoner reasoner, ArrayShape s0,
			ArrayShape s1) {
		if (s0.baseType.equals(s1.baseType) && s0.dimensions <= s1.dimensions) {
			int d0 = s0.dimensions;
			int d1 = s1.dimensions;
			BooleanExpression extentsEquals = universe.trueExpression();

			for (int i = 0; i < d0; i++)
				extentsEquals = universe.and(extentsEquals, universe.equals(
						s0.extents[d0 - i - 1], s1.extents[d1 - i - 1]));
			return reasoner.isValid(extentsEquals);
		}
		return false;
	}

	/**
	 * Shrink the array via removing all integral zero suffixes
	 * 
	 * @param reasoner
	 * @param arr
	 * @return a new array if the given array is shrunk, otherwise the given
	 *         array
	 */
	private NumericExpression[] zeroTrim(Reasoner reasoner,
			NumericExpression arr[]) {
		int i;
		for (i = arr.length - 1; i >= 0; i--)
			if (!reasoner.isValid(universe.equals(zero, arr[i])))
				break;
		return Arrays.copyOfRange(arr, 0, i + 1);
	}

	/**
	 * Extends the length of the given array to "len", sets extended cells to
	 * SARL integral zero. No op if the length of the array is greater than or
	 * equal to "len"
	 * 
	 * @param arr
	 * @param len
	 * @return a new array if the array is extended, otherwise the given array
	 */
	private NumericExpression[] zeroFill(NumericExpression arr[], int len) {
		if (arr.length >= len)
			return arr;

		NumericExpression[] result = new NumericExpression[len];

		System.arraycopy(arr, 0, result, 0, arr.length);
		Arrays.fill(result, arr.length, len, zero);
		return result;
	}

	private State reportBundleUnpackError(State state, int pid,
			SymbolicExpression pointer, SymbolicExpression bundleData,
			CIVLType bundleDataType, NumericExpression dataSize,
			NumericExpression memSize, BooleanExpression claim,
			ResultType resultType, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		StringBuffer message = new StringBuffer();
		CIVLType voidPointerType = typeFactory
				.pointerType(typeFactory.voidType());

		message.append(
				"Cannot unpack the bundle data into the pointed memory region.\n");
		message.append(
				"Bundle data: " + symbolicAnalyzer.symbolicExpressionToString(
						source, state, bundleDataType, bundleData) + "\n");
		message.append(
				"Bundle data size: "
						+ symbolicAnalyzer.symbolicExpressionToString(source,
								state, typeFactory.integerType(), dataSize)
						+ "\n");
		message.append(
				"Pointer :" + symbolicAnalyzer.symbolicExpressionToString(
						source, state, voidPointerType, pointer) + "\n");
		message.append("Memory region size: "
				+ symbolicAnalyzer.symbolicExpressionToString(source, state,
						typeFactory.integerType(), memSize));
		state = errorLogger.logError(source, state, pid,
				symbolicAnalyzer.stateInformation(state), claim, resultType,
				CIVLProperty.OUT_OF_BOUNDS, message.toString());
		state = stateFactory.addToPathcondition(state, pid, claim);
		return state;
	}

	private State reportBundlePackError(State state, int pid,
			SymbolicExpression pointer, NumericExpression memSize,
			NumericExpression specifiedSize, BooleanExpression claim,
			ResultType resultType, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		StringBuffer message = new StringBuffer();
		CIVLType voidPointerType = typeFactory
				.pointerType(typeFactory.voidType());

		message.append(
				"Cannot pack data of the specified size from the pointed memory region into a bundle .\n");
		message.append(
				"Specified size: "
						+ symbolicAnalyzer.symbolicExpressionToString(source,
								state, typeFactory.integerType(), specifiedSize)
						+ "\n");
		message.append(
				"Pointer :" + symbolicAnalyzer.symbolicExpressionToString(
						source, state, voidPointerType, pointer) + "\n");
		message.append("Memory region size: "
				+ symbolicAnalyzer.symbolicExpressionToString(source, state,
						typeFactory.integerType(), memSize));
		state = errorLogger.logError(source, state, pid,
				symbolicAnalyzer.stateInformation(state), claim, resultType,
				CIVLProperty.OUT_OF_BOUNDS, message.toString());
		state = stateFactory.addToPathcondition(state, pid, claim);
		return state;
	}
}
