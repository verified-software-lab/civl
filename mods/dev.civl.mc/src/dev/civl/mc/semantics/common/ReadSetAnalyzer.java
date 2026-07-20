package dev.civl.mc.semantics.common;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.model.IF.Identifier;
import dev.civl.mc.model.IF.ModelConfiguration;
import dev.civl.mc.model.IF.expression.AbstractFunctionCallExpression;
import dev.civl.mc.model.IF.expression.AddressOfExpression;
import dev.civl.mc.model.IF.expression.ArrayLambdaExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression;
import dev.civl.mc.model.IF.expression.CastExpression;
import dev.civl.mc.model.IF.expression.CompoundLiteralExpression;
import dev.civl.mc.model.IF.expression.ConditionalExpression;
import dev.civl.mc.model.IF.expression.DereferenceExpression;
import dev.civl.mc.model.IF.expression.DotExpression;
import dev.civl.mc.model.IF.expression.DynamicTypeOfExpression;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.Expression.ExpressionKind;
import dev.civl.mc.model.IF.expression.ExtendedQuantifiedExpression;
import dev.civl.mc.model.IF.expression.FunctionCallExpression;
import dev.civl.mc.model.IF.expression.FunctionGuardExpression;
import dev.civl.mc.model.IF.expression.InitialValueExpression;
import dev.civl.mc.model.IF.expression.LHSExpression;
import dev.civl.mc.model.IF.expression.LambdaExpression;
import dev.civl.mc.model.IF.expression.RecDomainLiteralExpression;
import dev.civl.mc.model.IF.expression.RegularRangeExpression;
import dev.civl.mc.model.IF.expression.ScopeofExpression;
import dev.civl.mc.model.IF.expression.SizeofExpression;
import dev.civl.mc.model.IF.expression.SizeofTypeExpression;
import dev.civl.mc.model.IF.expression.SubscriptExpression;
import dev.civl.mc.model.IF.expression.UnaryExpression;
import dev.civl.mc.model.IF.expression.VariableExpression;
import dev.civl.mc.model.IF.type.CIVLArrayType;
import dev.civl.mc.model.IF.type.CIVLCompleteArrayType;
import dev.civl.mc.model.IF.type.CIVLMemType;
import dev.civl.mc.model.IF.type.CIVLPointerType;
import dev.civl.mc.model.IF.type.CIVLStructOrUnionType;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.type.StructOrUnionField;
import dev.civl.mc.semantics.IF.Evaluation;
import dev.civl.mc.semantics.IF.Evaluator;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;

/**
 * <p>
 * This class analyzes an expression "e" with a state "s" and a process "p" and
 * returns a symbolic expression of
 * {@link CIVLMemType#dynamicType(SymbolicUniverse)}, which represents the
 * precise set of memory locations that will be read during the evaluation of
 * the expression.
 * </p>
 * 
 * <p>
 * The basic analysis idea: for an expression <code>e</code>, recursively
 * collecting all the variables that <code>e</code> involves.
 * 
 * During recursion, a sub-expression <code>e'</code> refers to a memory
 * location iff <code>e'</code> is a {@link LHSExpression}.
 * 
 * 
 * Following rules must be inductively applied to {@link LHSExpression} during
 * the recursion: let "ban(e)" denote that the memory location referred by a
 * sub-expression "e" MUST NOT be saved otherwise the analysis is not precise.
 * <ul>
 * <li><code>*p</code>: the memory location referred by <code>*p</code> shall be
 * saved.</li>
 * <li><code>a[i]</code>: "ban(a)"; the memory location referred by
 * <code>a[i]</code> shall be saved, if not "ban(a[i])"</li>
 * <li><code>s.t</code>: "ban(s)"; and the memory location referred by
 * <code>s.t</code> should be saved, if not "ban(s.t)".</li>
 * <li><code>id</code>: trivial</li>
 * </ul>
 * </p>
 * 
 * @author ziqing
 *
 */
public class ReadSetAnalyzer {
	/**
	 * a reference to the {@link Evaluator}
	 */
	private Evaluator evaluator;

	/**
	 * a reference to the {@link SymbolicUniverse}
	 */
	private SymbolicUniverse universe;

	/**
	 * the dynamic constant scope value
	 */
	private final SymbolicExpression constantDyScopeVal;

	/* constructor */
	ReadSetAnalyzer(Evaluator evaluator) {
		this.evaluator = evaluator;
		this.universe = evaluator.universe();
		constantDyScopeVal = evaluator.modelFactory().typeFactory().scopeType().scopeIdentityToValueOperator(universe)
				.apply(ModelConfiguration.DYNAMIC_CONSTANT_SCOPE);
	}

	/* the sole package interface */

	/**
	 * Analyze an expression with respect to a state and a process, returns a set of
	 * mem values, each of which represents a memory location subset of the precise
	 * memory location set that is read during an expression evaluation.
	 * 
	 * @param expr  an {@link Expression}
	 * @param state a {@link State}
	 * @param pid   the PID of a process
	 * @return the set of subsets of the precise memory location set that is read
	 *         during evaluation
	 * @throws UnsatisfiablePathConditionException
	 */
	Set<SymbolicExpression> analyze(Expression expr, State state, int pid) {
		try {
			return analyzeMemWorker(expr, state, pid);
		} catch (UnsatisfiablePathConditionException e) {
			// if analysis runs into an UnsatisfiablePathConditionException, so
			// does the actual evaluation, hence the exception can be ignored
			// here:
			return new TreeSet<>(universe.comparator());
		}
	}

	/**
	 * Analyze an {@link LHSExpression} as if it is in an address-of expression
	 * <code>&e</code>. Note that reading a <code>&e</code> does not involve reading
	 * the value of <code>e</code>.
	 */
	Set<SymbolicExpression> analyzeAsAddressof(State state, int pid, LHSExpression addressofArgument) {
		Set<SymbolicExpression> readSets;

		switch (addressofArgument.lhsExpressionKind()) {
		case DEREFERENCE: {
			// reading `&*p` -> reading `p`:
			DereferenceExpression derefExpr = (DereferenceExpression) addressofArgument;
			readSets = analyze(derefExpr.pointer(), state, pid);
			break;
		}
		case DOT: {
			// reading `&s.f` -> reading `&s`:
			DotExpression dotExpr = (DotExpression) addressofArgument;

			if (dotExpr instanceof LHSExpression)
				readSets = analyzeAsAddressof(state, pid, (LHSExpression) dotExpr.structOrUnion());
			else
				readSets = analyze(dotExpr.structOrUnion(), state, pid);
			break;
		}
		case SUBSCRIPT: {
			// reading `&a[i]` -> reading `&a` and `i`:
			SubscriptExpression subsExpr = (SubscriptExpression) addressofArgument;

			readSets = analyzeAsAddressof(state, pid, subsExpr.array());
			readSets.addAll(analyze(subsExpr.index(), state, pid));
			break;
		}
		case VARIABLE:
			// reading `&var` -> reads nothing
			readSets = new TreeSet<>(universe.comparator());
			break;
		default:
			throw new CIVLInternalException("unknown LHS expression kind: " + addressofArgument.lhsExpressionKind(),
					addressofArgument.getSource());
		}
		return readSets;
	}

	/**
	 * <p>
	 * The general analysis method for collecting the precise memory location set
	 * that is read during the expression evaluation.
	 * </p>
	 *
	 * @param expr  the expression that is analyzed
	 * @param state the state
	 * @param pid   the PID of the process
	 * @return the set of subsets of the precisely analyzed memory location set
	 * @throws UnsatisfiablePathConditionException
	 */
	private Set<SymbolicExpression> analyzeMemWorker(Expression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		ExpressionKind kind = expr.expressionKind();
		Set<SymbolicExpression> result = new TreeSet<>(universe.comparator());

		switch (kind) {
		case ABSTRACT_FUNCTION_CALL:
			result.addAll(analyzeAbstractFuncCall((AbstractFunctionCallExpression) expr, state, pid));
			break;
		case ADDRESS_OF:
			result.addAll(analyzeAddressOf((AddressOfExpression) expr, state, pid));
			break;
		case ARRAY_LAMBDA:
			result.addAll(analyzeArrayLambda((ArrayLambdaExpression) expr, state, pid));
			break;
		case BINARY:
			result.addAll(analyzeBinary((BinaryExpression) expr, state, pid));
			break;
		case CAST:
			result.addAll(analyzeCast((CastExpression) expr, state, pid));
			break;
		case COND:
			result.addAll(analyzeCond((ConditionalExpression) expr, state, pid));
			break;
		case DEREFERENCE:
			result.addAll(analyzeDeref((DereferenceExpression) expr, state, pid));
			break;
		case DOT:
			result.addAll(analyzeDot((DotExpression) expr, state, pid));
			break;
		case DYNAMIC_TYPE_OF:
			result.addAll(analyzeDyTypeOf((DynamicTypeOfExpression) expr, state, pid));
			break;
		case EXTENDED_QUANTIFIER:
			result.addAll(analyzeExtQuantifier((ExtendedQuantifiedExpression) expr, state, pid));
			break;
		case FUNCTION_GUARD:
			result.addAll(analyzeFuncGuard((FunctionGuardExpression) expr, state, pid));
			break;
		case FUNC_CALL:
			result.addAll(analyzeFuncCall((FunctionCallExpression) expr, state, pid));
			break;
		case INITIAL_VALUE:
			result.addAll(analyzeInitVal((InitialValueExpression) expr, state, pid));
			break;
		case LAMBDA:
			result.addAll(analyzeLambda((LambdaExpression) expr, state, pid));
			break;
		case REC_DOMAIN_LITERAL:
			result.addAll(analyzeRecDomLit((RecDomainLiteralExpression) expr, state, pid));
			break;
		case REGULAR_RANGE:
			result.addAll(analyzeRange((RegularRangeExpression) expr, state, pid));
			break;
		case SCOPEOF:
			result.addAll(analyzeScopeof((ScopeofExpression) expr, state, pid));
			break;
		case SIZEOF_EXPRESSION:
			result.addAll(analyzeSizeof((SizeofExpression) expr, state, pid));
			break;
		case SIZEOF_TYPE:
			result.addAll(analyzeSizeofType((SizeofTypeExpression) expr, state, pid));
			break;
		case SUBSCRIPT:
			result.addAll(analyzeSubscript((SubscriptExpression) expr, state, pid));
			break;
		case UNARY:
			result.addAll(analyzeUnaryExpression((UnaryExpression) expr, state, pid));
			break;
		case VARIABLE:
			result.addAll(analyzeVariable((VariableExpression) expr, state, pid));
			break;
		case COMPOUND_LITERAL:
			result.addAll(analyzeCompoundLiteral((CompoundLiteralExpression) expr, state, pid));
			break;
		/* Ignor-able kinds section */
		case BOOLEAN_LITERAL:
		case BOUND_VARIABLE:
		case CHAR_LITERAL:
		case FUNCTION_IDENTIFIER:
		case HERE_OR_ROOT:
		case INTEGER_LITERAL:
		case NOTHING:
		case NULL_LITERAL:
		case PROC_NULL:
		case QUANTIFIER:
		case REAL_LITERAL:
		case RESULT:
		case SELF:
		case STRING_LITERAL:
		case UNDEFINED_PROC:
		case WILDCARD:
		case SYSTEM_GUARD:
		case DOMAIN_GUARD:// TODO: what is this ?
			break;
		/* shall not happen section */
		case MEMORY_UNIT:
			throw new CIVLUnimplementedFeatureException(
					"dynamic analysis of read set during evaluation of " + "expression of " + kind + " kind");
		default:
			throw new CIVLInternalException("unknown expression kind " + kind, expr.getSource());
		}
		return result;
	}

	/**
	 * Analyze expressions in types
	 *
	 */
	private Set<SymbolicExpression> analyzeType(CIVLType type, State state, int pid, Set<Identifier> seenStructOrUnions)
			throws UnsatisfiablePathConditionException {
		Set<SymbolicExpression> result = new TreeSet<>(universe.comparator());

		switch (type.typeKind()) {
		case ARRAY:
		case COMPLETE_ARRAY: {
			CIVLArrayType arrType = (CIVLArrayType) type;

			if (arrType.isComplete())
				result.addAll(analyze(((CIVLCompleteArrayType) arrType).extent(), state, pid));
			result.addAll(analyzeType(arrType.elementType(), state, pid, seenStructOrUnions));
			break;
		}
		case POINTER: {
			CIVLPointerType ptrType = (CIVLPointerType) type;

			result.addAll(analyzeType(ptrType.baseType(), state, pid, seenStructOrUnions));
			break;
		}
		case STRUCT_OR_UNION: {
			CIVLStructOrUnionType structOrUnionType = (CIVLStructOrUnionType) type;

			if (seenStructOrUnions.contains(structOrUnionType.name()))
				return result;
			seenStructOrUnions.add(structOrUnionType.name());

			if (seenStructOrUnions.contains(structOrUnionType.name()))
				return result;
			seenStructOrUnions.add(structOrUnionType.name());
			for (StructOrUnionField field : structOrUnionType.fields())
				result.addAll(analyzeType(field.type(), state, pid, seenStructOrUnions));
			break;
		}
		case BUNDLE:
		case DOMAIN:
		case ENUM:
		case PRIMITIVE:
		case FUNCTION:
		case HEAP:
		case MEM:
		case SET:
			break;
		default:
			throw new CIVLUnimplementedFeatureException(
					"dynamic analysis of read set during evaluation of " + "expression in " + type);

		}
		return result;
	}

	/* *********** Induction on different expression kinds **************/

	private Set<SymbolicExpression> analyzeVariable(VariableExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		Evaluation eval = evaluator.reference(state, pid, expr);
		Set<SymbolicExpression> result = new TreeSet<>(universe.comparator());

		if (!isPointsToConstantScope(eval.value)) {
			eval = evaluator.memEvaluator().pointer2memValue(state, pid, eval.value, expr.getSource());
			result.add(eval.value);
		}
		return result;
	}

	private Set<SymbolicExpression> analyzeSubscript(SubscriptExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		Set<SymbolicExpression> result = analyzeMemWorker(expr.index(), state, pid);
		Evaluation eval = evaluator.reference(state, pid, expr);

		if (!isPointsToConstantScope(eval.value)) {
			eval = evaluator.memEvaluator().pointer2memValue(state, pid, eval.value, expr.getSource());
			result.add(eval.value);
		}
		// reading `a[i]` -> reading `a[i]`, `i` and `&a`
		result.addAll(analyzeAsAddressof(state, pid, expr.array()));
		return result;
	}

	private Set<SymbolicExpression> analyzeSizeofType(SizeofTypeExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		return analyzeType(expr.getTypeArgument(), state, pid, new HashSet<>());
	}

	private Set<SymbolicExpression> analyzeSizeof(SizeofExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		return analyzeMemWorker(expr.getArgument(), state, pid);
	}

	private Set<SymbolicExpression> analyzeScopeof(ScopeofExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		return analyzeMemWorker(expr.argument(), state, pid);
	}

	private Set<SymbolicExpression> analyzeRange(RegularRangeExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		Set<SymbolicExpression> result = analyzeMemWorker(expr.getLow(), state, pid);

		result.addAll(analyzeMemWorker(expr.getHigh(), state, pid));
		if (expr.getStep() != null)
			result.addAll(analyzeMemWorker(expr.getStep(), state, pid));
		return result;
	}

	private Set<SymbolicExpression> analyzeRecDomLit(RecDomainLiteralExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		int dims = expr.dimension();
		Set<SymbolicExpression> result = new TreeSet<>(universe.comparator());

		for (int i = 0; i < dims; i++)
			result.addAll(analyzeMemWorker(expr.rangeAt(i), state, pid));
		return result;
	}

	private Set<SymbolicExpression> analyzeInitVal(InitialValueExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		return analyzeType(expr.getExpressionType(), state, pid, new HashSet<>());
	}

	private Set<SymbolicExpression> analyzeLambda(LambdaExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		return analyzeMemWorker(expr.lambdaFunction(), state, pid);
	}

	private Set<SymbolicExpression> analyzeFuncCall(FunctionCallExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		Set<SymbolicExpression> result = new TreeSet<>(universe.comparator());
		boolean isSystem = expr.callStatement().function().isSystemFunction();

		for (Expression arg : expr.callStatement().arguments()) {
			result.addAll(analyzeMemWorker(arg, state, pid));

			if (isSystem) {
				// Over-approximate for pointer arguments in case of system
				// functions: we assume the whole variable pointed by the
				// pointer will be read.
				CIVLType argTy = arg.getExpressionType();

				if (argTy.isPointerType() || argTy.isArrayType()) {
					Evaluation eval = evaluator.evaluate(state, pid, arg);
					SymbolicUtility symUtil = evaluator.symbolicUtility();

					if (!symUtil.isConcretePointer(eval.value))
						continue; // lets ignore non-concrete pointers
					result.add(symUtil.setSymRef(eval.value, universe.identityReference()));
				}
			}
		}
		// TODO: process function reads clauses
		return result;
	}

	private Set<SymbolicExpression> analyzeFuncGuard(FunctionGuardExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		Set<SymbolicExpression> result = analyzeMemWorker(expr.functionExpression(), state, pid);

		for (Expression arg : expr.arguments())
			result.addAll(analyzeMemWorker(arg, state, pid));
		return result;
	}

	private Set<SymbolicExpression> analyzeExtQuantifier(ExtendedQuantifiedExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		Set<SymbolicExpression> result = analyzeMemWorker(expr.lower(), state, pid);

		result.addAll(analyzeMemWorker(expr.higher(), state, pid));
		result.addAll(analyzeMemWorker(expr.function(), state, pid));
		return result;
	}

	private Set<SymbolicExpression> analyzeDyTypeOf(DynamicTypeOfExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		return analyzeType(expr.getType(), state, pid, new HashSet<>());
	}

	private Set<SymbolicExpression> analyzeDot(DotExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		Set<SymbolicExpression> result;
		Evaluation eval;

		if (expr.structOrUnion() instanceof LHSExpression)
			result = analyzeAsAddressof(state, pid, (LHSExpression) expr.structOrUnion());
		else
			result = analyze(expr.structOrUnion(), state, pid);
		eval = evaluator.reference(state, pid, expr);
		if (!isPointsToConstantScope(eval.value)) {
			eval = evaluator.memEvaluator().pointer2memValue(state, pid, eval.value, expr.getSource());
			result.add(eval.value);
		}
		return result;
	}

	private Set<SymbolicExpression> analyzeDeref(DereferenceExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		Set<SymbolicExpression> result = analyzeMemWorker(expr.pointer(), state, pid);
		Evaluation eval = evaluator.reference(state, pid, expr);

		if (isPointsToConstantScope(eval.value))
			return result;
		eval = evaluator.memEvaluator().pointer2memValue(state, pid, eval.value, expr.getSource());
		result.add(eval.value);
		return result;
	}

	private Set<SymbolicExpression> analyzeCond(ConditionalExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		Evaluation eva = evaluator.evaluate(state, pid, expr.getCondition());
		BooleanExpression conEval = (BooleanExpression) eva.value;
		Set<SymbolicExpression> result = analyzeMemWorker(expr.getCondition(), state, pid);

		if (!conEval.isFalse())
			result.addAll(analyzeMemWorker(expr.getTrueBranch(), state, pid));
		if (!conEval.isTrue())
			result.addAll(analyzeMemWorker(expr.getFalseBranch(), state, pid));
		return result;
	}

	private Set<SymbolicExpression> analyzeCast(CastExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		CIVLType type = expr.getCastType();
		Set<SymbolicExpression> result = analyzeType(type, state, pid, new HashSet<>());

		result.addAll(analyzeMemWorker(expr.getExpression(), state, pid));
		return result;
	}

	private Set<SymbolicExpression> analyzeArrayLambda(ArrayLambdaExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		CIVLCompleteArrayType arrType = expr.getExpressionType();
		Set<SymbolicExpression> result = analyzeType(arrType, state, pid, new HashSet<>());

		result.addAll(analyzeMemWorker(expr.expression(), state, pid));
		return result;
	}

	private Set<SymbolicExpression> analyzeAddressOf(AddressOfExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		return analyzeAsAddressof(state, pid, expr.operand());
	}

	private Set<SymbolicExpression> analyzeAbstractFuncCall(AbstractFunctionCallExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		Set<SymbolicExpression> result = new TreeSet<>(universe.comparator());
		for (Expression arg : expr.arguments())
			result.addAll(analyzeMemWorker(arg, state, pid));
		return result;
	}

	private Set<SymbolicExpression> analyzeBinary(BinaryExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		Set<SymbolicExpression> result = analyzeMemWorker(expr.left(), state, pid);

		result.addAll(analyzeMemWorker(expr.right(), state, pid));
		return result;
	}

	private Set<SymbolicExpression> analyzeUnaryExpression(UnaryExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		return this.analyzeMemWorker(expr.operand(), state, pid);
	}

	private Set<SymbolicExpression> analyzeCompoundLiteral(CompoundLiteralExpression expr, State state, int pid)
			throws UnsatisfiablePathConditionException {
		Set<SymbolicExpression> result = new HashSet<>();

		if (!expr.hasConstantValue())
			for (Expression obj : expr.getLiteralObject().subExpressions())
				result.addAll(analyzeMemWorker(obj, state, pid));
		return result;
	}

	/**
	 * It's kind confusing that why DYNAMIC_CONSTANT_SCOPE is -1. To make sure the
	 * mem value contains no negative scope value, here has to ignore such
	 * reference. But anyway, we probably do not care about pointers (mem values) to
	 * those in constant scopes.
	 */
	private boolean isPointsToConstantScope(SymbolicExpression pointer) {
		SymbolicExpression scopeVal = evaluator.symbolicUtility().getScopeValue(pointer);

		return scopeVal.equals(constantDyScopeVal);
	}
}
