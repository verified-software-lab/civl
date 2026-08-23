package dev.civl.mc.semantics.common;

import java.util.HashSet;
import java.util.Set;

import dev.civl.mc.dynamic.IF.SymbolicUtility;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLTypeFactory;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.model.IF.ModelConfiguration;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.expression.AddressOfExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression.BINARY_OPERATOR;
import dev.civl.mc.model.IF.expression.DereferenceExpression;
import dev.civl.mc.model.IF.expression.DotExpression;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.Expression.ExpressionKind;
import dev.civl.mc.model.IF.expression.LHSExpression;
import dev.civl.mc.model.IF.expression.LHSExpression.LHSExpressionKind;
import dev.civl.mc.model.IF.expression.MemoryUnitExpression;
import dev.civl.mc.model.IF.expression.SubscriptExpression;
import dev.civl.mc.model.IF.expression.VariableExpression;
import dev.civl.mc.model.IF.expression.reference.ArraySliceReference;
import dev.civl.mc.model.IF.expression.reference.ArraySliceReference.ArraySliceKind;
import dev.civl.mc.model.IF.expression.reference.MemoryUnitReference;
import dev.civl.mc.model.IF.expression.reference.MemoryUnitReference.MemoryUnitReferenceKind;
import dev.civl.mc.model.IF.expression.reference.StructOrUnionFieldReference;
import dev.civl.mc.model.IF.type.CIVLCompleteArrayType;
import dev.civl.mc.model.IF.type.CIVLStructOrUnionType;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.semantics.IF.Evaluation;
import dev.civl.mc.semantics.IF.Evaluator;
import dev.civl.mc.semantics.IF.MemoryUnitExpressionEvaluator;
import dev.civl.mc.state.IF.MemoryUnit;
import dev.civl.mc.state.IF.MemoryUnitFactory;
import dev.civl.mc.state.IF.MemoryUnitSet;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.StateFactory;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.mc.util.IF.Pair;
import dev.civl.sarl.IF.Reasoner;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.expr.ArrayElementReference;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.ReferenceExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.number.IntegerNumber;
import dev.civl.sarl.IF.type.SymbolicTupleType;
import dev.civl.sarl.IF.type.SymbolicType;

/**
 * This is responsible for evaluating memory unit expressions. (IN PROGRESS)
 * 
 * @author Manchun Zheng
 *
 */
public class CommonMemoryUnitEvaluator
		implements
			MemoryUnitExpressionEvaluator {

	private ModelFactory modelFactory;

	private CIVLTypeFactory typeFactory;

	/**
	 * The symbolic utility to be used.
	 */
	private SymbolicUtility symbolicUtil;

	/**
	 * The evaluator to be used for evaluating parameters of memory unit
	 * expressions, e.g, index of an array.
	 */
	private CommonEvaluator evaluator;

	/**
	 * The symbolic universe to be used.
	 */
	private SymbolicUniverse universe;

	private MemoryUnitFactory muFactory;

	private StateFactory stateFactory;

	private SymbolicTupleType pointerType;

	@SuppressWarnings("unused")
	private SymbolicType rangeType;

	public CommonMemoryUnitEvaluator(SymbolicUtility symbolicUtil,
			Evaluator evaluator, MemoryUnitFactory muFactory,
			SymbolicUniverse universe) {
		this.symbolicUtil = symbolicUtil;
		this.evaluator = (CommonEvaluator) evaluator;
		this.universe = universe;
		this.muFactory = muFactory;
		this.modelFactory = evaluator.modelFactory();
		this.typeFactory = this.modelFactory.typeFactory();
		this.pointerType = typeFactory.pointerSymbolicType();
		this.rangeType = typeFactory.rangeType().getDynamicType(universe);
		this.stateFactory = evaluator.stateFactory();
	}

	/**
	 * Evaluates a memory unit expression.
	 * 
	 * @param state
	 * @param pid
	 * @param memUnit
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	public MemoryUnitSet evaluates(State state, int pid,
			MemoryUnitExpression memUnit, MemoryUnitSet muSet)
			throws UnsatisfiablePathConditionException {
		MemoryUnitSet result = muSet;
		int scopeID = memUnit.scopeId();
		int dyscopeID = state.getDyscope(pid, scopeID);
		Set<ReferenceExpression> referenceValues;

		if (dyscopeID < 0)
			return result;
		referenceValues = this.evaluatesMemoryUnitReference(memUnit.getSource(),
				state, pid, memUnit.objectType(), memUnit.reference(),
				null).right;
		for (ReferenceExpression reference : referenceValues) {
			MemoryUnit newMemUnit = muFactory.newMemoryUnit(dyscopeID,
					memUnit.variableId(), reference);

			muFactory.add(result, newMemUnit);
			if (memUnit.deref()) {
				SymbolicExpression pointer = state.getVariableValue(dyscopeID,
						memUnit.variableId());

				if (!pointer.isNull() && pointer.type()
						.equals(this.typeFactory.pointerSymbolicType()))
					muFactory.add(result, this.muFactory.newMemoryUnit(
							stateFactory.getDyscopeId(
									symbolicUtil.getScopeValue(pointer)),
							this.symbolicUtil.getVariableId(null, pointer),
							symbolicUtil.getSymRef(pointer)));
			}
			// result.add(pointer);
			// this.findPointersInExpression(
			// this.symbolicUtil.makePointer(dyscopeID,
			// memUnit.variableId(), reference), result,
			// state, state.getProcessState(pid).name());
			// result.add(symbolicUtil.makePointer(dyscopeID,
			// memUnit.variableId(), reference));
		}
		return result;

	}

	/**
	 * Evaluates reference of memory unit expressions.
	 * 
	 * @param state
	 * @param pid
	 * @param reference
	 * @param parent
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	private Pair<State, Set<ReferenceExpression>> evaluatesMemoryUnitReference(
			CIVLSource source, State state, int pid, CIVLType objType,
			MemoryUnitReference reference, Set<ReferenceExpression> parents)
			throws UnsatisfiablePathConditionException {
		MemoryUnitReferenceKind refKind = reference.memoryUnitKind();
		MemoryUnitReference child = reference.child();
		Set<ReferenceExpression> myRefValues = new HashSet<>();
		CIVLType myObjType = objType;
		// ReferenceExpression myRefValue = null;

		switch (refKind) {
			case SELF :
				myRefValues.add(universe.identityReference());
				break;
			case ARRAY_SLICE :// TODO to be finished
			{
				ArraySliceReference arraySlice = (ArraySliceReference) reference;
				ArraySliceKind sliceKind = arraySlice.sliceKind();
				Expression indexExpression = arraySlice.index();
				Evaluation eval = null;

				assert parents != null && parents.size() > 0;
				if (indexExpression != null) {
					eval = evaluator.evaluate(state, pid, indexExpression);
					state = eval.state;
				}
				switch (sliceKind) {
					case ELEMENT :
						for (ReferenceExpression parent : parents)
							myRefValues.add(universe.arrayElementReference(
									parent, (NumericExpression) eval.value));
						break;
					case WILDCARD : {
						CIVLCompleteArrayType arrayType = (CIVLCompleteArrayType) objType;
						Expression extent = arrayType.extent();
						int extentInt;
						Reasoner reasoner = universe
								.reasoner(state.getPathCondition(universe));
						IntegerNumber length_number;

						eval = evaluator.evaluate(state, pid, extent);
						state = eval.state;
						length_number = (IntegerNumber) reasoner
								.extractNumber((NumericExpression) eval.value);
						extentInt = length_number.intValue();
						for (int i = 0; i < extentInt; i++)
							for (ReferenceExpression parent : parents)
								myRefValues.add(universe.arrayElementReference(
										parent, universe.integer(i)));
						break;
					}
					case REG_RANGE :
						// TODO to be finished
						break;
					default :
						throw new CIVLUnimplementedFeatureException(
								"evaluating array slice reference of "
										+ sliceKind + " kind",
								source);
				}

				break;
			}
			case STRUCT_OR_UNION_FIELD : {
				StructOrUnionFieldReference fieldRef = (StructOrUnionFieldReference) reference;
				int fieldIndex = fieldRef.fieldIndex();

				assert parents != null && parents.size() > 0;
				myObjType = ((CIVLStructOrUnionType) objType)
						.getField(fieldIndex).type();
				for (ReferenceExpression parent : parents)
					myRefValues.add(universe.tupleComponentReference(parent,
							universe.intObject(fieldRef.fieldIndex())));
				break;
			}
			default :
				throw new CIVLUnimplementedFeatureException(
						"evaluating memory unit reference of " + refKind
								+ " kind",
						source);
		}
		assert myRefValues.size() > 0;
		if (child != null)
			return evaluatesMemoryUnitReference(source, state, pid, myObjType,
					child, myRefValues);
		else {
			// result.addAll(myRefValues);
			return new Pair<>(state, myRefValues);
		}
	}

	@Override
	public MemoryUnitSet evaluateMemoryUnit(State state,
			Pair<Scope, SymbolicExpression[]> parameterScope, int pid,
			Expression muExpr) throws UnsatisfiablePathConditionException {
		ExpressionKind exprKind = muExpr.expressionKind();
		MemoryUnitSet result = this.muFactory.newMemoryUnitSet();
		Evaluation eval;

		// DEREFERENCE, DOT, SUBSCRIPT, VARIABLE
		switch (exprKind) {
			case BINARY : {
				BinaryExpression binary = (BinaryExpression) muExpr;

				if (binary.operator() != BINARY_OPERATOR.POINTER_ADD) {
					throw new CIVLInternalException(
							"invalid expression for memory units",
							muExpr.getSource());
				}

				MemoryUnitSet leftMus = this.evaluateMemoryUnit(state,
						parameterScope, pid, binary.left());
				SymbolicType offsetType;

				eval = this.evaluator.evaluate(state, pid, binary.right());
				offsetType = eval.value.type();
				state = eval.state;

				NumericExpression offset = (NumericExpression) eval.value;

				if (!offsetType.isInteger()
						&& !offsetType.equals(this.typeFactory.rangeType()
								.getDynamicType(universe))) {
					throw new CIVLInternalException("invalid pointer addition: "
							+ "the right hand side operand should be either of integer or range type",
							muExpr.getSource());
				}
				for (MemoryUnit mu : leftMus) {
					ReferenceExpression reference = mu.reference();

					if (!reference.isArrayElementReference())
						throw new CIVLInternalException(
								"invalid pointer addition", muExpr.getSource());

					ArrayElementReference arrayEle = (ArrayElementReference) reference;

					arrayEle = universe.arrayElementReference(
							arrayEle.getParent(),
							universe.add(arrayEle.getIndex(), offset));
					result.add(mu.setReference(arrayEle));
				}
				break;
			}
			case DOT : {
				DotExpression dotExpr = (DotExpression) muExpr;
				Expression structOrUnion = dotExpr.structOrUnion();
				MemoryUnitSet suMus = this.evaluateMemoryUnit(state,
						parameterScope, pid, structOrUnion);
				int index = dotExpr.fieldIndex();

				for (MemoryUnit mu : suMus) {
					result.add(this.muFactory.extendReference(mu,
							this.universe.tupleComponentReference(
									mu.reference(),
									universe.intObject(index))));
				}
				break;
			}
			case ADDRESS_OF : {
				return lhs2MemoryUnit(state, parameterScope, pid,
						((AddressOfExpression) muExpr).operand());
			}
			case VARIABLE : {
				Variable variable = ((VariableExpression) muExpr).variable();

				SymbolicExpression value;

				if (parameterScope.left == variable.scope()) {
					value = parameterScope.right[variable.vid()];
				} else
					value = state.valueOf(pid, variable);
				if (!value.isNull())
					if (isPointer(value)) {
						int dyscopeID = stateFactory.getDyscopeId(
								symbolicUtil.getScopeValue(value)),
								vid = this.symbolicUtil.getVariableId(null,
										value);
						Variable object = state.getDyscope(dyscopeID)
								.lexicalScope().getVariable(vid);

						if (!(object.isInput() || object.isConst()))
							result.add(this.pointer2MemoryUnit(value));
					}
				break;
			}
			default :
				throw new CIVLUnimplementedFeatureException(
						"invalid kind of memory unit expression: " + exprKind);
		}
		return result;
	}

	private MemoryUnitSet lhs2MemoryUnit(State state,
			Pair<Scope, SymbolicExpression[]> parameterScope, int pid,
			LHSExpression lhs) throws UnsatisfiablePathConditionException {
		MemoryUnitSet result = this.muFactory.newMemoryUnitSet();
		LHSExpressionKind kind = lhs.lhsExpressionKind();
		Evaluation eval;

		switch (kind) {
			case DEREFERENCE : {
				return this.evaluateMemoryUnit(state, parameterScope, pid,
						((DereferenceExpression) lhs).pointer());
			}
			case DOT : {
				DotExpression dotExpression = (DotExpression) lhs;
				int index = dotExpression.fieldIndex();
				boolean isStruct = dotExpression.getExpressionType()
						.isStructType();

				if (dotExpression.structOrUnion() instanceof LHSExpression) {
					MemoryUnitSet subResult = this.lhs2MemoryUnit(state,
							parameterScope, pid,
							(LHSExpression) dotExpression.structOrUnion());

					for (MemoryUnit mu : subResult) {
						if (isStruct)
							result.add(mu.setReference(universe
									.tupleComponentReference(mu.reference(),
											universe.intObject(index))));
						else
							result.add(mu.setReference(universe
									.unionMemberReference(mu.reference(),
											universe.intObject(index))));
					}
				}
				break;
			}
			case SUBSCRIPT : {
				SubscriptExpression subscript = (SubscriptExpression) lhs;
				MemoryUnitSet subResult = this.lhs2MemoryUnit(state,
						parameterScope, pid, subscript.array());
				NumericExpression index;

				eval = this.evaluator.evaluate(state, pid, subscript.index());
				index = (NumericExpression) eval.value;
				for (MemoryUnit mu : subResult) {
					result.add(mu.setReference(universe
							.arrayElementReference(mu.reference(), index)));
				}
				break;
			}
			case VARIABLE : {
				Variable variable = ((VariableExpression) lhs).variable();
				int dyscopeId = state.getDyscope(pid, variable.scope().id());

				result.add(muFactory.newMemoryUnit(dyscopeId, variable.vid(),
						universe.identityReference()));
				break;
			}
		}
		return result;
	}

	private boolean isPointer(SymbolicExpression value) {
		return value.type().equals(this.pointerType)
				&& stateFactory.getDyscopeId(symbolicUtil.getScopeValue(
						value)) != ModelConfiguration.DYNAMIC_CONSTANT_SCOPE;
	}

	private MemoryUnit pointer2MemoryUnit(SymbolicExpression pointer) {
		return this.muFactory.newMemoryUnit(
				stateFactory.getDyscopeId(symbolicUtil.getScopeValue(pointer)),
				symbolicUtil.getVariableId(null, pointer),
				this.symbolicUtil.getSymRef(pointer));
	}

}
