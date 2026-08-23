package dev.civl.mc.library.mem;

import java.util.LinkedList;
import java.util.List;

import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.dynamic.IF.DynamicMemoryLocationSet;
import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.library.common.BaseLibraryExecutor;
import dev.civl.mc.library.mem.MemoryLocationMap.MemLocMapEntry;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLProperty;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.type.CIVLMemType;
import dev.civl.mc.model.IF.type.CIVLMemType.MemoryLocationReference;
import dev.civl.mc.model.IF.type.CIVLType.TypeKind;
import dev.civl.mc.model.IF.variable.Variable;
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
import dev.civl.sarl.IF.UnaryOperator;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.valueSetReference.ValueSetReference;
import dev.civl.sarl.IF.type.SymbolicType;

public class LibmemExecutor extends BaseLibraryExecutor implements LibraryExecutor {

	/**
	 * A unary operator that collects the references in the "memValue", which are
	 * referring to non-alive objects:
	 */
	private UnaryOperator<SymbolicExpression> collector;

	public LibmemExecutor(String name, Executor primaryExecutor, ModelFactory modelFactory,
			SymbolicUtility symbolicUtil, SymbolicAnalyzer symbolicAnalyzer, CIVLConfiguration civlConfig,
			LibraryExecutorLoader libExecutorLoader, LibraryEvaluatorLoader libEvaluatorLoader) {
		super(name, primaryExecutor, modelFactory, symbolicUtil, symbolicAnalyzer, civlConfig, libExecutorLoader,
				libEvaluatorLoader);
		collector = typeFactory.civlMemType().memValueCollector(universe, stateFactory.nullScopeValue());
	}

	@Override
	protected Evaluation executeValue(State state, int pid, String process, CIVLSource source, String functionName,
			Expression[] arguments, SymbolicExpression[] argumentValues) throws UnsatisfiablePathConditionException {
		Evaluation callEval = null;
		boolean write = false, read = true;

		switch (functionName) {
		case "$write_set_push":
			callEval = executeReadWriteSetPush(state, pid, arguments, argumentValues, write, source);
			break;
		case "$write_set_pop":
			callEval = executeReadWriteSetPop(state, pid, arguments, argumentValues, write, source);
			break;
		case "$write_set_peek":
			callEval = executeReadWriteSetPeek(state, pid, arguments, argumentValues, write, source);
			break;
		case "$read_set_push":
			callEval = executeReadWriteSetPush(state, pid, arguments, argumentValues, read, source);
			break;
		case "$read_set_pop":
			callEval = executeReadWriteSetPop(state, pid, arguments, argumentValues, read, source);
			break;
		case "$read_set_peek":
			callEval = executeReadWriteSetPeek(state, pid, arguments, argumentValues, read, source);
			break;
		case "$mem_contains":
			callEval = executeMemContains(state, pid, arguments, argumentValues, source);
			break;
		case "$mem_diff":
			callEval = executeMemDiff(state, pid, arguments, argumentValues, source);
			break;
		case "$mem_union":
			callEval = executeMemUnion(state, pid, arguments, argumentValues, source);
			break;
		case "$mem_no_intersect":
			callEval = executeMemNoIntersect(state, pid, arguments, argumentValues, source);
			break;
		case "$mem_union_widening":
			callEval = executeMemUnionWidening(state, pid, arguments, argumentValues, source);
			break;
		case "$mem_havoc":
			callEval = executeMemHavoc(state, pid, arguments, argumentValues, source);
			break;
		case "$mem_unary_widening":
			callEval = executeMemUnaryWidening(state, pid, arguments, argumentValues, source);
			break;
		case "$mem_elim_widening":
			callEval = executeMemElimWidening(state, pid, arguments, argumentValues, source);
			break;
		case "$mem_protective_widening":
			callEval = executeMemProtectiveWidening(state, pid, arguments, argumentValues, source);
			break;
		case "$mem_empty":
			callEval = executeMemNew(state, pid, arguments, argumentValues, source);
			break;
		case "$mem_equals":
			callEval = executeMemEquals(state, pid, arguments, argumentValues, source);
			break;
		case "$mem_quick_equals":
			callEval = executeMemQuickEquals(state, pid, arguments, argumentValues, source);
			break;
		default:
			throw new CIVLInternalException("Unknown mem function: " + functionName, source);
		}
		return callEval;
	}

	/**
	 * <p>
	 * Executing the system function:<code>$write_set_push()</code>. <br>
	 * <br>
	 *
	 * Push an empty write set onto write set stack associated with the calling
	 * process.
	 *
	 * </p>
	 *
	 * @param state          The current state.
	 * @param pid            The ID of the process that the function call belongs
	 *                       to.
	 * @param arguments      The static representation of the arguments of the
	 *                       function call.
	 * @param argumentValues The dynamic representation of the arguments of the
	 *                       function call.
	 * @param isRead         true iff execute read set push; false iff execute write
	 *                       set push.
	 * @param source         The {@link CIVLSource} associates to the function call.
	 * @return The new state after executing the function call.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeReadWriteSetPush(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, boolean isRead, CIVLSource source) {
		state = stateFactory.pushEmptyReadWrite(state, pid, isRead);
		return new Evaluation(state, null);
	}

	/**
	 * <p>
	 * Executing the system function:<code>$write_set_pop($mem * m)</code>. <br>
	 * <br>
	 *
	 * Pop a write set w out of the write set stack associated with the calling
	 * process. Assign write set w' to the object refered by the given reference m,
	 * where w' is a subset of w. <code>w - w'</code> is a set of unreachable memory
	 * locaiton references.
	 *
	 * </p>
	 *
	 * @param state          The current state.
	 * @param pid            The ID of the process that the function call belongs
	 *                       to.
	 * @param arguments      The static representation of the arguments of the
	 *                       function call.
	 * @param argumentValues The dynamic representation of the arguments of the
	 *                       function call.
	 * @param isRead         true iff pop read set; false iff pop write set.
	 * @param source         The {@link CIVLSource} associates to the function call.
	 * @return The new state after executing the function call.
	 * @throws UnsatisfiablePathConditionException
	 */
	private Evaluation executeReadWriteSetPop(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, boolean isRead, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression memValue;
		DynamicMemoryLocationSet rwSet = stateFactory.peekReadWriteSet(state, pid, isRead);

		if (rwSet == null) {
			String setKind = isRead ? "read" : "write";

			state = errorLogger.logError(source, state, pid, symbolicAnalyzer.stateInformation(state),
					universe.falseExpression(), ResultType.NO, CIVLProperty.OTHER,
					"Attempt to pop an empty " + setKind + " set stack");
		}
		state = stateFactory.popReadWriteSet(state, pid, isRead);
		memValue = rwSet.getMemValue();
		return new Evaluation(state, memValue);
	}

	private Evaluation executeReadWriteSetPeek(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, boolean isRead, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		SymbolicExpression memValue;
		DynamicMemoryLocationSet rwSet = stateFactory.peekReadWriteSet(state, pid, isRead);

		memValue = rwSet.getMemValue();
		return new Evaluation(state, memValue);
	}

	private Evaluation executeMemContains(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression mem0 = collector.apply(argumentValues[0]);
		SymbolicExpression mem1 = collector.apply(argumentValues[1]);
		MemoryLocationMap set0 = memValue2MemoryLocationSet(mem0);
		MemoryLocationMap set1 = memValue2MemoryLocationSet(mem1);
		BooleanExpression result = universe.trueExpression();

		// for each "sub" value set template, there must exist one in "super"
		// mem value that contains it, otherwise false...
		for (MemLocMapEntry entry : set1.entrySet()) {
			SymbolicExpression suuper;

			suuper = set0.get(entry.vid(), entry.heapID(), entry.mallocID(), entry.scopeValue());
			if (suuper == null) {
				result = universe.falseExpression();
				break;
			} else
				result = universe.and(result, universe.valueSetContains(suuper, entry.valueSetTemplate()));
		}
		return new Evaluation(state, result);
	}

	private Evaluation executeMemDiff(State state, int pid, Expression[] arguments, SymbolicExpression[] argumentValues,
			CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression mem0 = collector.apply(argumentValues[0]);
		SymbolicExpression mem1 = collector.apply(argumentValues[1]);
		MemoryLocationMap set0 = memValue2MemoryLocationSet(mem0);
		MemoryLocationMap set1 = memValue2MemoryLocationSet(mem1);
		CIVLMemType memType = typeFactory.civlMemType();

		List<SymbolicExpression[]> results = new LinkedList<>();

		for (MemLocMapEntry entry : set0.entrySet()) {
			SymbolicExpression vst;

			vst = set1.get(entry.vid(), entry.heapID(), entry.mallocID(), entry.scopeValue());
			vst = vst == null ? entry.valueSetTemplate() : universe.valueSetDiff(entry.valueSetTemplate(), vst);
			results.add(new SymbolicExpression[] { universe.integer(entry.vid()), universe.integer(entry.heapID()),
					universe.integer(entry.mallocID()), entry.scopeValue(), vst });
		}

		return new Evaluation(state, memType.memValueCreator(universe).apply(results));
	}

	private Evaluation executeMemUnion(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression mem0 = collector.apply(argumentValues[0]);
		SymbolicExpression mem1 = collector.apply(argumentValues[1]);
		MemoryLocationMap set0 = memValue2MemoryLocationSet(mem0);
		MemoryLocationMap set1 = memValue2MemoryLocationSet(mem1);
		CIVLMemType memType = typeFactory.civlMemType();

		for (MemLocMapEntry entry : set1.entrySet()) {
			SymbolicExpression vst;

			vst = set0.get(entry.vid(), entry.heapID(), entry.mallocID(), entry.scopeValue());
			vst = vst == null ? entry.valueSetTemplate() : universe.valueSetUnion(vst, entry.valueSetTemplate());
			set0.put(entry.vid(), entry.heapID(), entry.mallocID(), entry.scopeValue(), vst);
		}

		List<SymbolicExpression[]> results = new LinkedList<>();

		for (MemLocMapEntry entry : set0.entrySet())
			results.add(new SymbolicExpression[] { universe.integer(entry.vid()), universe.integer(entry.heapID()),
					universe.integer(entry.mallocID()), entry.scopeValue(), entry.valueSetTemplate() });
		return new Evaluation(state, memType.memValueCreator(universe).apply(results));
	}

	/**
	 * <p>
	 * Definition of the <code>
	 * _Bool $mem_no_intersect($mem m0, $mem m1, $mem *output0, $mem *output1)
	 * </code> system function.
	 * </p>
	 *
	 * <p>
	 * The system function tests if <code>m0</code> and <code>m1</code> have no
	 * intersection. If the returned boolean value is not true, the
	 * <code>output0</code> and <code>output1</code> will be assigned a pair of
	 * memory locations that intersect.
	 * </p>
	 */
	private Evaluation executeMemNoIntersect(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression mem0 = collector.apply(argumentValues[0]);
		SymbolicExpression mem1 = collector.apply(argumentValues[1]);
		SymbolicExpression out0 = argumentValues[2];
		SymbolicExpression out1 = argumentValues[3];
		MemoryLocationMap set0 = memValue2MemoryLocationSet(mem0);
		MemoryLocationMap set1 = memValue2MemoryLocationSet(mem1);
		Reasoner reasoner = null;

		for (MemLocMapEntry entry : set1.entrySet()) {
			SymbolicExpression vst;

			vst = set0.get(entry.vid(), entry.heapID(), entry.mallocID(), entry.scopeValue());
			if (vst == null)
				continue;

			// test for no intersection:
			BooleanExpression isNoIntersect = universe.valueSetNoIntersect(vst, entry.valueSetTemplate());

			if (isNoIntersect.isTrue())
				// no intersection:
				continue;
			else if (isNoIntersect.isFalse()) {
				return outputIntersectedMems(state, pid, entry.vid(), entry.heapID(), entry.mallocID(),
						entry.scopeValue(), vst, out0, entry.valueSetTemplate(), out1, isNoIntersect, source);
			} else {
				reasoner = reasoner == null ? universe.reasoner(state.getPathCondition(universe)) : reasoner;

				if (reasoner.isValid(isNoIntersect))
					// no intersection:
					continue;
				else
					return outputIntersectedMems(state, pid, entry.vid(), entry.heapID(), entry.mallocID(),
							entry.scopeValue(), vst, out0, entry.valueSetTemplate(), out1, isNoIntersect, source);
			}
		}
		// no intersection at all, return:
		return new Evaluation(state, universe.trueExpression());
	}

	/**
	 * <p>
	 * For two memory locations that (may) intersect, packing the two memory
	 * locations as two singleton $mem sets and assigning the two $mem to the two
	 * output pointers.
	 * </p>
	 *
	 * @param state             the current state
	 * @param pid               the PID of the process that calls the system
	 *                          function
	 * @param variableID        the variable ID of the variable where the two memory
	 *                          locations belong to
	 * @param heapID            the heap ID of the heap object where the two memory
	 *                          locations belong to (significant if variableID = 0)
	 * @param mallocID          the malloc ID of the heap object where the two
	 *                          memory locations belong to (significant if
	 *                          variableID = 0)
	 * @param scopeVal          the value of the dyscope where the two memory
	 *                          locations belongs to
	 * @param valueSetTemplate0 the value set template of one of the memory location
	 * @param outPtr0           the output pointer for one of the $mem value
	 * @param valueSetTemplate1 the value set template of the other memory location
	 * @param outPtr1           the output pointer for the other $mem value
	 * @param hasNoIntersection the boolean condition that is true iff there is no
	 *                          intersection between the two memory location
	 * @param source            the {@link CIVLSource} of the call to this system
	 *                          function
	 * @return the evaluation including the post-state of the call and the returned
	 *         value from the call
	 * @throws UnsatisfiablePathConditionException when error happens in the
	 *                                             assignments to output pointers.
	 */
	private Evaluation outputIntersectedMems(State state, int pid, int variableID, int heapID, int mallocID,
			SymbolicExpression scopeVal, SymbolicExpression valueSetTemplate0, SymbolicExpression outPtr0,
			SymbolicExpression valueSetTemplate1, SymbolicExpression outPtr1, BooleanExpression hasNoIntersection,
			CIVLSource source) throws UnsatisfiablePathConditionException {
		CIVLMemType memType = typeFactory.civlMemType();
		List<SymbolicExpression[]> components = new LinkedList<>();
		SymbolicExpression mem0, mem1;

		components.add(new SymbolicExpression[] { universe.integer(variableID), universe.integer(heapID),
				universe.integer(mallocID), scopeVal, valueSetTemplate0 });
		mem0 = memType.memValueCreator(universe).apply(components);
		components.clear();
		components.add(new SymbolicExpression[] { universe.integer(variableID), universe.integer(heapID),
				universe.integer(mallocID), scopeVal, valueSetTemplate1 });
		mem1 = memType.memValueCreator(universe).apply(components);

		state = primaryExecutor.assign(source, state, pid, outPtr0, mem0);
		state = primaryExecutor.assign(source, state, pid, outPtr1, mem1);
		return new Evaluation(state, hasNoIntersection);
	}

	private Evaluation executeMemEquals(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression mem0 = collector.apply(argumentValues[0]);
		SymbolicExpression mem1 = collector.apply(argumentValues[1]);
		MemoryLocationMap set0 = memValue2MemoryLocationSet(mem0);
		MemoryLocationMap set1 = memValue2MemoryLocationSet(mem1);
		BooleanExpression result = universe.equals(universe.integer(set0.size()), universe.integer(set1.size()));

		for (MemLocMapEntry entry : set0.entrySet()) {
			SymbolicExpression vst0 = set0.get(entry.vid(), entry.heapID(), entry.mallocID(), entry.scopeValue());
			SymbolicExpression vst1 = set1.get(entry.vid(), entry.heapID(), entry.mallocID(), entry.scopeValue());

			if (vst1 != null)
				result = universe.and(result, universe.equals(vst0, vst1));
			else {
				result = universe.falseExpression();
				break;
			}
		}
		return new Evaluation(state, result);
	}

	private Evaluation executeMemQuickEquals(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression mem0 = collector.apply(argumentValues[0]);
		SymbolicExpression mem1 = collector.apply(argumentValues[1]);
		MemoryLocationMap set0 = memValue2MemoryLocationSet(mem0);
		MemoryLocationMap set1 = memValue2MemoryLocationSet(mem1);
		BooleanExpression result = universe.equals(universe.integer(set0.size()), universe.integer(set1.size()));

		for (MemLocMapEntry entry : set0.entrySet()) {
			SymbolicExpression vst0 = set0.get(entry.vid(), entry.heapID(), entry.mallocID(), entry.scopeValue());
			SymbolicExpression vst1 = set1.get(entry.vid(), entry.heapID(), entry.mallocID(), entry.scopeValue());

			if (vst1 != null)
				result = universe.and(result, universe.quickEquals(vst0, vst1));
			else {
				result = universe.falseExpression();
				break;
			}
		}
		return new Evaluation(state, result);
	}

	private Evaluation executeMemUnionWidening(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression mem0 = collector.apply(argumentValues[0]);
		SymbolicExpression mem1 = collector.apply(argumentValues[1]);
		MemoryLocationMap set0 = memValue2MemoryLocationSet(mem0);
		MemoryLocationMap set1 = memValue2MemoryLocationSet(mem1);
		CIVLMemType memType = typeFactory.civlMemType();

		// for each "sub" value set template, there must exist one in "super"
		// mem value that contains it, otherwise false...
		for (MemLocMapEntry entry : set1.entrySet()) {
			SymbolicExpression vst;

			vst = set0.get(entry.vid(), entry.heapID(), entry.mallocID(), entry.scopeValue());
			vst = vst == null ? entry.valueSetTemplate() : universe.valueSetUnion(vst, entry.valueSetTemplate());
			set0.put(entry.vid(), entry.heapID(), entry.mallocID(), entry.scopeValue(), vst);
		}

		List<SymbolicExpression[]> results = new LinkedList<>();
		for (MemLocMapEntry entry : set0.entrySet())
			results.add(new SymbolicExpression[] { universe.integer(entry.vid()), universe.integer(entry.heapID()),
					universe.integer(entry.mallocID()), entry.scopeValue(),
					universe.valueSetWidening(state.getPathCondition(universe), entry.valueSetTemplate()) });
		return new Evaluation(state, memType.memValueCreator(universe).apply(results));
	}

	private Evaluation executeMemProtectiveWidening(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression m = collector.apply(argumentValues[0]);
		SymbolicExpression p = collector.apply(argumentValues[1]);
		MemoryLocationMap mMap = memValue2MemoryLocationSet(m);
		MemoryLocationMap pMap = memValue2MemoryLocationSet(p);
		CIVLMemType memType = typeFactory.civlMemType();

		List<SymbolicExpression[]> results = new LinkedList<>();
		for (MemLocMapEntry entry : mMap.entrySet()) {
			SymbolicExpression pEntry = pMap.get(entry.vid(), entry.heapID(), entry.mallocID(), entry.scopeValue());
			if (pEntry == null) {
				pEntry = universe.valueSetTemplate(universe.valueType(entry.valueSetTemplate()),
						new ValueSetReference[0]);
			}

			SymbolicExpression widenedResult = universe.valueSetProtectiveWidening(state.getPathCondition(universe),
					entry.valueSetTemplate(), pEntry);

			results.add(new SymbolicExpression[] { universe.integer(entry.vid()), universe.integer(entry.heapID()),
					universe.integer(entry.mallocID()), entry.scopeValue(), widenedResult });
		}
		return new Evaluation(state, memType.memValueCreator(universe).apply(results));
	}

	private Evaluation executeMemElimWidening(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression mem = collector.apply(argumentValues[0]);
		SymbolicExpression elimExpr = argumentValues[1], lower = argumentValues[2], upper = argumentValues[3];
		MemoryLocationMap set = memValue2MemoryLocationSet(mem);
		List<SymbolicExpression[]> results = new LinkedList<>();

		for (MemLocMapEntry entry : set.entrySet()) {
			results.add(new SymbolicExpression[] { universe.integer(entry.vid()), universe.integer(entry.heapID()),
					universe.integer(entry.mallocID()), entry.scopeValue(), universe.valueSetElimWidening(
							state.getPathCondition(universe), entry.valueSetTemplate(), elimExpr, lower, upper) });
		}
		return new Evaluation(state, typeFactory.civlMemType().memValueCreator(universe).apply(results));
	}

	private Evaluation executeMemHavoc(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression memValue = collector.apply(argumentValues[0]);
		Iterable<MemoryLocationReference> memRefs = typeFactory.civlMemType().memValueIterator().apply(memValue);
		Evaluation eval = new Evaluation(state, universe.nullExpression());

		for (MemoryLocationReference memRef : memRefs) {
			if (civlConfig.isPropertyToggled(CIVLProperty.OUT_OF_BOUNDS)) {
				SymbolicExpression vst = memRef.valueSetTemplate();
				SymbolicType valueType = universe.valueType(vst);

				for (ValueSetReference vsRef : universe.valueSetReferences(vst))
					eval.state = evaluator.memEvaluator().checkValueSetReferenceOutOfBound(eval.state, pid, valueType,
							vsRef, source);
			}
			eval = havoc(eval.state, pid, memRef, source);
		}
		return eval;
	}

	/*
	 * Description: apply a "unary widening" operator to each memory location in the
	 * "m". The result of the operation to a memory location 'a' will be the memory
	 * location of a program variable or a memory heap object that contains 'a'.
	 *
	 * $atomic_f $system $mem $mem_unary_widening($mem m);
	 */
	private Evaluation executeMemUnaryWidening(State state, int pid, Expression[] arguments,
			SymbolicExpression[] argumentValues, CIVLSource source) throws UnsatisfiablePathConditionException {
		SymbolicExpression memValue = collector.apply(argumentValues[0]);
		Iterable<MemoryLocationReference> memRefs = typeFactory.civlMemType().memValueIterator().apply(memValue);
		List<SymbolicExpression[]> components = new LinkedList<>();

		for (MemoryLocationReference memRef : memRefs) {
			SymbolicExpression vid, heapId, mallocId;
			SymbolicType rootValueType = getRootValue(memRef, state, null, pid).type();
			SymbolicExpression rootTemplate;

			if (rootValueType == null) {
				Variable var = state.getDyscope(stateFactory.getDyscopeId(memRef.scopeValue())).lexicalScope()
						.getVariable(memRef.vid());

				assert var.type().typeKind() == TypeKind.PRIMITIVE;
				rootValueType = var.type().getDynamicType(universe);
			}
			// Since it is an identity reference, we do not have to worry about
			// referencing sequence elements:
			rootTemplate = universe.valueSetTemplate(rootValueType,
					new ValueSetReference[] { universe.vsIdentityReference() });
			vid = universe.integer(memRef.vid());
			heapId = universe.integer(memRef.heapID());
			mallocId = universe.integer(memRef.mallocID());
			components.add(new SymbolicExpression[] { vid, heapId, mallocId, memRef.scopeValue(), rootTemplate });
		}

		SymbolicExpression result = typeFactory.civlMemType().memValueCreator(universe).apply(components);

		return new Evaluation(state, result);
	}

	/**
	 * <p>
	 * Havoc memory locations that are referred by "memRef".
	 * </p>
	 *
	 * @param state  the state where the havoc operation will happen
	 * @param pid    the PID of the running process
	 * @param memRef a {@link MemoryLocationReference}
	 * @param source the CIVLSource that is related to this operation
	 * @return the {@link Evaluation} after havoc
	 * @throws UnsatisfiablePathConditionException
	 */
	/*
	 * private Evaluation havoc(State state, int pid, MemoryLocationReference
	 * memRef, CIVLSource source) throws UnsatisfiablePathConditionException { int
	 * sid = stateFactory.getDyscopeId(memRef.scopeValue()); SymbolicExpression
	 * oldValue = getRootValue(memRef, state, null, pid); SymbolicExpression
	 * rootPointer = getRootPointer(memRef); SymbolicType oldValueType =
	 * oldValue.type(); Evaluation eval;
	 * 
	 * // If the referred variable was uninitialized and has a // primitive type,
	 * its value may be NULL hence type cannot be // obtained from its value. But
	 * primitive types have simple dynamic // types. if (oldValueType == null) {
	 * Variable var = state.getDyscope(sid).lexicalScope() .variable(memRef.vid());
	 * 
	 * assert var.type().typeKind() == TypeKind.PRIMITIVE; oldValueType =
	 * var.type().getDynamicType(universe); } eval = evaluator.havoc(state,
	 * oldValueType); eval.state = primaryExecutor.assign2(source, eval.state, pid,
	 * rootPointer, eval.value, memRef.valueSetTemplate()); eval.value =
	 * universe.nullExpression(); return eval; }
	 */

	private Evaluation havoc(State state, int pid, MemoryLocationReference memRef, CIVLSource source)
			throws UnsatisfiablePathConditionException {
		int sid = stateFactory.getDyscopeId(memRef.scopeValue());
		SymbolicExpression oldValue = getRootValue(memRef, state, null, pid);
		SymbolicExpression rootPointer = getRootPointer(memRef);
		SymbolicType oldValueType = oldValue.type();

		// If the referred variable was uninitialized and has a
		// primitive type, its value may be NULL hence type cannot be
		// obtained from its value. But primitive types have simple dynamic
		// types.
		if (oldValueType == null) {
			Variable var = state.getDyscope(sid).lexicalScope().getVariable(memRef.vid());

			assert var.type().typeKind() == TypeKind.PRIMITIVE;
			oldValueType = var.type().getDynamicType(universe);
		}
		SymbolicExpression vst = memRef.valueSetTemplate();
		Pair<State, SymbolicExpression> havocResult = stateFactory.valueSetHavoc(state, oldValue, vst);

		state = havocResult.left;
		Evaluation eval = new Evaluation(
				primaryExecutor.assign2(source, state, pid, rootPointer, havocResult.right, vst),
				universe.nullExpression());
		return eval;
	}

	private Evaluation executeMemNew(State state, int pid, Expression[] arguments, SymbolicExpression[] argumentValues,
			CIVLSource source) throws UnsatisfiablePathConditionException {
		return new Evaluation(state, memEmpty());
	}

	private SymbolicExpression memEmpty() {
		CIVLMemType memType = typeFactory.civlMemType();
		return memType.memValueCreator(universe).apply(new LinkedList<>());
	}

	/**
	 * Create a {@link MemoryLocationMap} for memory location references in the
	 * given "memValue"
	 */
	private MemoryLocationMap memValue2MemoryLocationSet(SymbolicExpression memValue) {
		MemoryLocationMap set = new MemoryLocationMap();
		CIVLMemType memType = typeFactory.civlMemType();

		for (CIVLMemType.MemoryLocationReference memLocRef : memType.memValueIterator().apply(memValue))
			set.put(memLocRef.vid(), memLocRef.heapID(), memLocRef.mallocID(), memLocRef.scopeValue(),
					memLocRef.valueSetTemplate());
		return set;
	}

	/**
	 * @param memRef                a {@link MemoryLocationReference}
	 * @param state                 a state where all memory locations referred by
	 *                              the "memRef" are alive
	 * @param scopeValueSubstituter a scope value substituter which can change the
	 *                              scope value in "memRef" to the corresponding
	 *                              scope value in the given "state"
	 * @param pid                   the PID of the running process
	 *
	 * @return the value in the given state of the variable or the memory heap
	 *         object that contains all the memory locations referred by the given
	 *         "memRef"
	 */
	private SymbolicExpression getRootValue(MemoryLocationReference memRef, State state,
			UnaryOperator<SymbolicExpression> scopeValueSubstituter, int pid) {
		SymbolicExpression scopeVal = memRef.scopeValue();

		if (scopeValueSubstituter != null)
			scopeVal = scopeValueSubstituter.apply(scopeVal);

		int sid = stateFactory.getDyscopeId(scopeVal);
		int vid = memRef.vid();
		SymbolicExpression rootValue = state.getVariableValue(sid, vid);

		if (vid == 0) {
			rootValue = universe.tupleRead(rootValue, universe.intObject(memRef.heapID()));
			rootValue = universe.arrayRead(rootValue, universe.integer(memRef.mallocID()));
		}
		return rootValue;
	}

	/**
	 * @param memRef a {@link MemoryLocationReference}
	 * @return the pointer to the variable or the memory heap object that contains
	 *         the memory locations referred by the given "memRef"
	 */
	private SymbolicExpression getRootPointer(MemoryLocationReference memRef) {
		SymbolicExpression scopeVal = memRef.scopeValue();
		int vid = memRef.vid(), sid = stateFactory.getDyscopeId(scopeVal);

		if (vid == 0)
			// TODO: here the code couples with the definition of the heap
			// type, better there is better way to hide heap structure.
			return symbolicUtil.makePointer(sid, memRef.vid(), universe.arrayElementReference(
					universe.tupleComponentReference(universe.identityReference(), universe.intObject(memRef.heapID())),
					universe.integer(memRef.mallocID())));
		else
			return symbolicUtil.makePointer(sid, memRef.vid(), universe.identityReference());
	}
}
