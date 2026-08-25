package dev.civl.mc.transform.common.contracts;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Entity.EntityKind;
import dev.civl.abc.ast.entity.IF.Variable;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ArrowNode;
import dev.civl.abc.ast.node.IF.expression.CastNode;
import dev.civl.abc.ast.node.IF.expression.DotNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode.Quantifier;
import dev.civl.abc.ast.node.IF.expression.RegularRangeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.type.IF.ArrayType;
import dev.civl.abc.ast.type.IF.PointerType;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.util.IF.Pair;
import dev.civl.mc.model.IF.CIVLSyntaxException;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;

/**
 * <p>
 * This class manages memory locations in a contract program. Memory locations
 * can be categorized as two kinds: in heap or on stack. Memory locations in
 * memory heap are those locations that being allocated by statements like
 * "$malloc"; Memory locations on stack are program variables (or parts of
 * variables).
 * </p>
 * 
 * <p>
 * In contract programs, memory locations in heap must be declared as valid
 * locations via ACSL valid expression. This class shall be used to cache
 * informations of this kind of memory locations. Memory locations on stack
 * should always refer to variable entities, thus no need to cache them.
 * </p>
 * 
 * <p>
 * For a memory location set term (see ACSL v1.10, sec 2.3.4) t, this class is
 * able to refer t to a {@link MemoryBlock}.
 * </p>
 * 
 * @author ziqingluo
 *
 */
public class MemoryLocationManager {

	/**
	 * Name counter for bound variables that substitutes integer sets
	 */
	private static int boundVariableNameCounter = 0;

	/**
	 * A reference to the {@link NodeFactory}
	 */
	private NodeFactory nodeFactory;

	/**
	 * A map from entities to pairs of pointer type <code>T*</code> and the number
	 * of objects of type <code>T</code>.
	 */
	private Map<Entity, List<Pair<Type, ExpressionNode>>> memoryLocationSet;

	public MemoryLocationManager(NodeFactory nodeFactory) {
		this.nodeFactory = nodeFactory;
		this.memoryLocationSet = new HashMap<>();
	}

	/**
	 * <p>
	 * Save a memory location set, which is represented as <code>p + l ... h</code>,
	 * where p is a pointer to T type identifier expression and <code>l .. h</code>
	 * is a regular range.
	 * </p>
	 * <p>
	 * The the memory location set is thus equivalent to a memory block of type:
	 * <code>T[h-l]</code>.
	 * </p>
	 * <p>
	 * The memory location set is saved with the correspondence to the pointer
	 * identifier p. It means that this is one of the memory blocks that are
	 * possibly pointed by p.
	 * </p>
	 */
	public void addMemoryLocationSet(ExpressionNode ptr, ExpressionNode count) {
		Pair<Type, ExpressionNode> value = new Pair<>(ptr.getType(), count);
		/*
		 * For the pointer type expression, currently only identifier expression or cast
		 * of identifier expression kinds are supported.
		 */
		if (ptr.expressionKind() == ExpressionKind.CAST)
			ptr = ((CastNode) ptr).getArgument();
		if (ptr.expressionKind() != ExpressionKind.IDENTIFIER_EXPRESSION)
			throw new CIVLUnimplementedFeatureException(
					"Transform \\valid expressions containing " + "pointer type expressions that are NOT identifiers",
					ptr.getSource());

		Variable entity = (Variable) ((IdentifierExpressionNode) ptr).getIdentifier().getEntity();
		List<Pair<Type, ExpressionNode>> memLocInfos = memoryLocationSet.get(entity);

		if (memLocInfos == null)
			memLocInfos = new LinkedList<>();
		memLocInfos.add(value);
		memoryLocationSet.put(entity, memLocInfos);
	}

	public Variable variableContainingMemoryLocationSet(ExpressionNode memoryLocationSet) {
		Entity entity = parseMemoryLocationSet(memoryLocationSet).left;

		// TODO: need points-to analysis:
		if (entity.getEntityKind() == EntityKind.VARIABLE)
			return (Variable) entity;
		throw new CIVLUnimplementedFeatureException(
				"Refresh memory location set : " + memoryLocationSet.prettyRepresentation());
	}

	/**
	 * 
	 * @return a list of {@link MemoryBlock}s that are associated with the given
	 *         <code>memset</code> expression.
	 * @throws SyntaxException
	 */
	public List<MemoryBlock> getMemoryLocationSize(ExpressionNode memset) throws SyntaxException {
		Pair<Entity, ExpressionNode> entity_identifier = parseMemoryLocationSet(memset);
		Entity entity = entity_identifier.left;
		List<Pair<Type, ExpressionNode>> typeSignatures = memoryLocationSet.get(entity);

		if (typeSignatures == null) {
			if (entity.getEntityKind() == EntityKind.VARIABLE
					&& ((Variable) entity).getType().kind() != TypeKind.POINTER) {
				// the variable is not a pointer:
				Variable var = (Variable) entity;
				Source source = memset.getSource();
				ExpressionNode addr = nodeFactory.newOperatorNode(source, Operator.ADDRESSOF, entity_identifier.right);

				return Arrays.asList(new MemoryBlock(addr, var.getType(), null));
			} else if (memset.expressionKind() == ExpressionKind.OPERATOR) {
				OperatorNode opNode = (OperatorNode) memset;
				// the memory set expression has the form : var[low ..
				// high],
				// where var can only have an array of T or pointer to T
				// type
				// and T is a non-pointer type:
				MemoryBlock result = null;

				if (opNode.getOperator() == Operator.SUBSCRIPT)
					result = getMemoryBlockSizeFromSubscript(memset.getSource(), opNode);
				if (opNode.getOperator() == Operator.DEREFERENCE)
					result = getMemoryBlockSizeFromDereference(memset.getSource(), opNode);
				if (result != null)
					return Arrays.asList(result);
			}
			throw new CIVLUnimplementedFeatureException(
					"statically parse the memory locations expressed: " + memset.prettyRepresentation());
		}

		List<MemoryBlock> results = new LinkedList<>();

		for (Pair<Type, ExpressionNode> typeSignature : typeSignatures) {
			Type type = typeSignature.left;
			ExpressionNode count = typeSignature.right;

			assert type.kind() == TypeKind.POINTER;
			// TODO: filed should associate to a struct object entity
			if (entity.getEntityKind() == EntityKind.FIELD)
				results.add(new MemoryBlock(entity_identifier.right, ((PointerType) type).referencedType(), count));
			else if (entity.getEntityKind() == EntityKind.VARIABLE)
				results.add(new MemoryBlock(entity_identifier.right, ((PointerType) type).referencedType(), count));
			else
				throw new CIVLSyntaxException(
						"Fail to recognize memory location set expression " + memset.prettyRepresentation());
		}
		return results;
	}

	/**
	 * Analyze the memory location set expression which is an operation of
	 * SUBSCRIPT.
	 * 
	 * @return A {@link MemoryBlock} if this method successfully figure out an
	 *         over-approximation of the memory locations that includes the ones
	 *         represented by the given expression. Otherwise, null.
	 */
	private MemoryBlock getMemoryBlockSizeFromSubscript(Source source, OperatorNode subscriptMemset)
			throws SyntaxException {
		ExpressionNode array = subscriptMemset.getArgument(0);
		ExpressionNode index = subscriptMemset.getArgument(1);
		ExpressionNode count = nodeFactory.newIntegerConstantNode(source, "1");
		Type baseType;

		if (array.getType().kind() == TypeKind.ARRAY)
			baseType = ((ArrayType) array.getType()).getElementType();
		else
			baseType = ((PointerType) array.getType()).referencedType();
		if (baseType.kind() != TypeKind.POINTER && index.expressionKind() == ExpressionKind.REGULAR_RANGE) {
			RegularRangeNode range = (RegularRangeNode) index;
			ExpressionNode diff = nodeFactory.newOperatorNode(source, Operator.MINUS, range.getHigh().copy(),
					range.getLow().copy());

			count = nodeFactory.newOperatorNode(source, Operator.PLUS, count, diff);
			return new MemoryBlock(array, baseType, count);
		}
		return null;
	}

	/**
	 * Analyze the memory location set expression which is an operation of
	 * DEREFERENCE.
	 * 
	 * @return A {@link MemoryBlock} if this method successfully figure out an
	 *         over-approximation of the memory locations that includes the ones
	 *         represented by the given expression. Otherwise, null.
	 */
	private MemoryBlock getMemoryBlockSizeFromDereference(Source source, OperatorNode derefNode)
			throws SyntaxException {
		ExpressionNode pointer = derefNode.getArgument(0);
		List<MemorySetBoundVariableSubstitution> anyRange = new LinkedList<>();

		replaceRangeKindOffsetsWithBoundVars(pointer, anyRange);
		if (!anyRange.isEmpty())
			return null;
		return new MemoryBlock(pointer, derefNode.getType(), null);
	}

	/**
	 * <p>
	 * Given a pointer <code>p</code>, and a set of memory location set expressions
	 * <code>E</code> of size <code>m</code>, returns a boolean expression: <code>
	 * p == e<sub>0</sub> || p == e<sub>1</sub> || ... || p == e<sub>m-1</sub>,
	 * where e<sub>i</sub> is an element of E.
	 * </code>.
	 * </p>
	 * 
	 * <p>
	 * For an element <code>e<sub>i</sub></code> in <code>E</code>, if
	 * <code>e<sub>i</sub></code> contains range expressions
	 * <code>r<sub>0</sub>, r<sub>1</sub>, ...</code> the predicate will be
	 * translated as an EXISTS formula: <code>
	 * EXISTS int : i<sub>0</sub>, i<sub>1</sub>, ... ; 
	 *              i<sub>0</sub> in r<sub>0</sub> && i<sub>1</sub> in r<sub>1</sub> && ...&&
	 *              e[i<sub>0</sub>/r<sub>0</sub>, ...] == p;
	 * </code>
	 * </p>
	 * 
	 * @param pointer
	 * @param memoryLocationSets
	 * @param source
	 * @return
	 */
	public ExpressionNode pointerBelongsToMemoryLocationSet(ExpressionNode pointer,
			List<ExpressionNode> memoryLocationSets, Source source) {
		TypeNode intTypeNode = nodeFactory.newBasicTypeNode(source, BasicTypeKind.INT);
		ExpressionNode predicate = null;

		for (ExpressionNode memoryLocationSet : memoryLocationSets) {
			ExpressionNode memLocSetCopy = memoryLocationSet.copy();
			List<MemorySetBoundVariableSubstitution> substitutions = new LinkedList<>();
			ExpressionNode memSetPointer = nodeFactory.newOperatorNode(source, Operator.ADDRESSOF, memLocSetCopy);
			ExpressionNode subPred_ptrEquals = nodeFactory.newOperatorNode(source, Operator.EQUALS, pointer.copy(),
					memSetPointer);

			replaceRangeKindOffsetsWithBoundVars(memLocSetCopy, substitutions);
			if (!substitutions.isEmpty()) {
				/*
				 * For the memeory location set expression that contains range expressions :
				 */
				List<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableDeclarationList;
				ExpressionNode restriction = substitutionRange(substitutions);

				boundVariableDeclarationList = new LinkedList<>();
				for (MemorySetBoundVariableSubstitution subst : substitutions) {
					Source rangeSource = subst.integerSet.getSource();
					IdentifierExpressionNode boundVarExpr = subst.boundVariable.copy();
					VariableDeclarationNode boundVarDecl = nodeFactory.newVariableDeclarationNode(rangeSource,
							boundVarExpr.getIdentifier().copy(), intTypeNode.copy());

					boundVariableDeclarationList.add(nodeFactory.newPairNode(
							rangeSource, nodeFactory.newSequenceNode(rangeSource,
									"lvalue-set in assigns transformation:bound vars", Arrays.asList(boundVarDecl)),
							null));
				}
				subPred_ptrEquals = nodeFactory.newQuantifiedExpressionNode(
						source, Quantifier.EXISTS, nodeFactory.newSequenceNode(source,
								"lvalue-set in assigns transformation", boundVariableDeclarationList),
						restriction, subPred_ptrEquals);
			}
			predicate = predicate == null ? subPred_ptrEquals
					: nodeFactory.newOperatorNode(source, Operator.LOR, predicate, subPred_ptrEquals);
		}
		return predicate;
	}

	/* *************************** private methods ***************************/
	/**
	 * <p>
	 * Given a memory location set expression which consist of one base address and
	 * sets of integral offsets. This method substitutes (side-effects) all the
	 * offset sets, which is a kind of a regular range, with bound variables.
	 * </p>
	 * 
	 * @param memset        the memory location set expression
	 * @param substitutions output argument, a list of
	 *                      {@link MemorySetBoundVariableSubstitution}s. A
	 *                      MemorySetBoundVariableSubstitution consists the original
	 *                      integer set expression and the substituted bound
	 *                      variable.
	 */
	private void replaceRangeKindOffsetsWithBoundVars(ExpressionNode memset,
			List<MemorySetBoundVariableSubstitution> substitutions) {
		replaceRangeKindOffsetsWithBoundVarsWorker(memset, substitutions);
	}

	/**
	 * @param substitutions a list of {@link MemorySetBoundVariableSubstitution}s
	 * @return A boolean type expression states that for each bound variable and the
	 *         substituted integer set, the bound variable belongs to the integer
	 *         set.
	 */
	private ExpressionNode substitutionRange(List<MemorySetBoundVariableSubstitution> substitutions) {
		assert !substitutions.isEmpty();

		ExpressionNode result = null;
		ExpressionNode subResult;

		for (MemorySetBoundVariableSubstitution subst : substitutions) {
			ExpressionNode intSet = subst.integerSet.copy();
			ExpressionNode boundVar = subst.boundVariable.copy();

			if (intSet.expressionKind() == ExpressionKind.REGULAR_RANGE) {
				RegularRangeNode range = (RegularRangeNode) intSet;
				ExpressionNode low = range.getLow().copy();
				ExpressionNode high = range.getHigh().copy();

				low = nodeFactory.newOperatorNode(intSet.getSource(), Operator.LTE, low, boundVar);
				high = nodeFactory.newOperatorNode(intSet.getSource(), Operator.LTE, boundVar.copy(), high);
				subResult = nodeFactory.newOperatorNode(intSet.getSource(), Operator.LAND, low, high);
			} else
				subResult = nodeFactory.newOperatorNode(intSet.getSource(), Operator.EQUALS, intSet, boundVar);
			result = result == null ? subResult
					: nodeFactory.newOperatorNode(intSet.getSource(), Operator.LAND, subResult, result);
		}
		return result;
	}

	private Pair<Entity, ExpressionNode> parseMemoryLocationSet(ExpressionNode expr) {
		ExpressionKind kind = expr.expressionKind();
		ExpressionNode subExpr = null;
		Entity entity = null;

		// switch on the operations that can form a set term ...
		switch (kind) {
		case ARROW:
			ArrowNode arrow = (ArrowNode) expr;
			entity = arrow.getFieldName().getEntity();
			subExpr = arrow.getStructurePointer();
			break;
		case CAST:
			CastNode cast = (CastNode) expr;
			subExpr = cast.getArgument();
			break;
		case DOT:
			DotNode dot = (DotNode) expr;
			entity = dot.getFieldName().getEntity();
			subExpr = ((DotNode) expr).getStructure();
			break;
		case OPERATOR:
			OperatorNode opNode = (OperatorNode) expr;
			Operator op = opNode.getOperator();

			switch (op) {
			case DEREFERENCE:
				subExpr = opNode.getArgument(0);
				break;
			case PLUS:
				subExpr = opNode.getArgument(0);
				if (subExpr instanceof RegularRangeNode)
					subExpr = opNode.getArgument(1);
				break;
			case SUBSCRIPT:
				subExpr = opNode.getArgument(0);
				break;
			default:
			}
			break;
		case IDENTIFIER_EXPRESSION:
			entity = ((IdentifierExpressionNode) expr).getIdentifier().getEntity();
			assert entity != null && entity.getEntityKind() == EntityKind.VARIABLE;
			break;
		default:
			throw new CIVLUnimplementedFeatureException(
					"Recognize memory location set expression of kind: " + expr.expressionKind());
		}
		if (entity == null) {
			assert subExpr != null;
			return parseMemoryLocationSet(subExpr);
		}
		return new Pair<>(entity, expr.copy());
	}

	private void replaceRangeKindOffsetsWithBoundVarsWorker(ExpressionNode memset,
			List<MemorySetBoundVariableSubstitution> substitutions) {
		ExpressionKind kind = memset.expressionKind();
		ExpressionNode subExpr = null;

		// switch on the operations that can form a set term ...
		switch (kind) {
		case ARROW:
			ArrowNode arrow = ((ArrowNode) memset);

			subExpr = arrow.getStructurePointer();
			break;
		case CAST:
			CastNode cast = ((CastNode) memset);

			subExpr = cast.getArgument();
			break;
		case DOT:
			DotNode dot = ((DotNode) memset);

			subExpr = dot.getStructure();
			break;
		case OPERATOR:
			OperatorNode opNode = (OperatorNode) memset;
			Operator op = opNode.getOperator();

			switch (op) {
			case DEREFERENCE:
				subExpr = opNode.getArgument(0);
				break;
			case PLUS:
				replacePLUSExpression(opNode, substitutions);
				return;
			case SUBSCRIPT:
				replaceSUBSCRIPTExpression(opNode, substitutions);
				return;
			default:
			}
			break;
		case IDENTIFIER_EXPRESSION:
			return;
		default:
			throw new CIVLUnimplementedFeatureException(
					"Deal with memory location set expression of kind: " + memset.expressionKind());
		}
		assert subExpr != null;
		replaceRangeKindOffsetsWithBoundVarsWorker(subExpr, substitutions);
	}

	private void replacePLUSExpression(OperatorNode plus, List<MemorySetBoundVariableSubstitution> substitutions) {
		ExpressionNode arg0 = plus.getArgument(1);
		ExpressionNode arg1 = plus.getArgument(0);

		if (arg0.expressionKind() == ExpressionKind.REGULAR_RANGE) {
			IdentifierExpressionNode boundVar = newBoundVariable(plus.getSource());
			int childIdx = arg0.childIndex();

			arg0.remove();
			plus.setChild(childIdx, boundVar);
			substitutions.add(new MemorySetBoundVariableSubstitution(arg0, boundVar));
		} else
			replaceRangeKindOffsetsWithBoundVarsWorker(arg0, substitutions);
		if (arg1.expressionKind() == ExpressionKind.REGULAR_RANGE) {
			IdentifierExpressionNode boundVar = newBoundVariable(plus.getSource());
			int childIdx = arg1.childIndex();

			arg1.remove();
			plus.setChild(childIdx, boundVar);
			substitutions.add(new MemorySetBoundVariableSubstitution(arg1, boundVar));
		} else
			replaceRangeKindOffsetsWithBoundVarsWorker(arg1, substitutions);
	}

	private void replaceSUBSCRIPTExpression(OperatorNode subscript,
			List<MemorySetBoundVariableSubstitution> substitutions) {
		ExpressionNode subExpr = subscript.getArgument(0);
		ExpressionNode index = subscript.getArgument(1);
		IdentifierExpressionNode boundVar = newBoundVariable(subscript.getSource());

		replaceRangeKindOffsetsWithBoundVarsWorker(subExpr, substitutions);
		substitutions.add(new MemorySetBoundVariableSubstitution(index, boundVar));

		int childIdx = index.childIndex();
		index.remove();
		subscript.setChild(childIdx, boundVar);
	}

	private IdentifierExpressionNode newBoundVariable(Source source) {
		return nodeFactory.newIdentifierExpressionNode(source, nodeFactory.newIdentifierNode(source,
				MPIContractUtilities.BOUND_VAR_PREFIX + boundVariableNameCounter++));
	}

	/* ******************* sub-classes ********************/
	/**
	 * <p>
	 * This class represents a memory block which can be either a (part of) variable
	 * or a (part of) memory block in heap. To identify such a memory block, one
	 * must provide a base address and a type signature : a pair of object type and
	 * number of objects.
	 * </p>
	 * 
	 * @author ziqingluo
	 *
	 */
	public class MemoryBlock {
		public final ExpressionNode baseAddress;
		public final Type type;
		/**
		 * null if the memory location is a single object
		 */
		public final ExpressionNode count; // count

		MemoryBlock(ExpressionNode baseAddress, Type type, ExpressionNode size) {
			this.baseAddress = baseAddress;
			this.type = type;
			this.count = size;
		}
	}

	/**
	 * This class represents one substitution from an "integer set" to a unique
	 * bound variable for memory location set expressions.
	 * 
	 * @author ziqing
	 */
	class MemorySetBoundVariableSubstitution {
		final ExpressionNode integerSet;
		final IdentifierExpressionNode boundVariable;

		MemorySetBoundVariableSubstitution(ExpressionNode integerSet, IdentifierExpressionNode boundVariable) {
			this.integerSet = integerSet;
			this.boundVariable = boundVariable;
		}
	}
}
