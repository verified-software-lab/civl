package dev.civl.mc.transform.common.contracts;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ConstantNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.RegularRangeNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.type.ArrayTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode.TypeNodeKind;
import dev.civl.abc.ast.type.IF.PointerType;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.value.IF.Value;
import dev.civl.abc.ast.value.IF.ValueFactory.Answer;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;
import dev.civl.abc.util.IF.Pair;
import dev.civl.mc.model.IF.CIVLSyntaxException;
import dev.civl.mc.transform.SubstituteGuide;
import dev.civl.mc.transform.common.BaseWorker;
import dev.civl.mc.transform.common.contracts.FunctionContractBlock.ContractClause;
import dev.civl.mc.util.IF.Triple;

/**
 * <p>
 * This class generates {@link ClauseTransformGuide} for contract clauses such
 * as <code>requires</code> and <code>ensures</code> clauses. An instance of
 * {@link ClauseTransformGuide} corresponds to one contract clause.
 * </p>
 * <p>
 * This class only contains static method hence no runtime instance of this
 * class is needed.
 * </p>
 * 
 * @author ziqingluo
 *
 */
public class ClauseTransformGuideGenerator {

	/**
	 * a clause transformation guide mainly consists of the following informations:
	 * <ul>
	 * 
	 * <li>clause : the contract clause which specifies boolean expressions</li>
	 * <li>conditions (a.k.a assumptions): the assumption over the clause:
	 * <code>conditions IMPLIES expressions</code></li>
	 * <li>arrivends : a set of expressions representing processes that this clause
	 * depende on</li>
	 * <li>side conditions: a set of boolean expressions that must be proved
	 * (implied) by the contract</li>
	 * <li>prefix: a set of intermediate variables and statements that must come
	 * BEFORE the evaluation of the clause expression.</li>
	 * <li>suffix: a set of intermediate variables and statements that must come
	 * AFTER the evaluation of the clause expression.</li>
	 * <li>substitutions: a set of substitutions that will modify the clause
	 * expressions</li>
	 * </ul>
	 * 
	 * @author ziqingluo
	 *
	 */
	static class ClauseTransformGuide {
		final ContractClause clause;
		List<ExpressionNode> conditions;
		List<ExpressionNode> arrivends;
		List<ExpressionNode> sideConditions;
		List<BlockItemNode> prefix;
		Map<ExpressionNode, SubstituteGuide> substitutions;
		List<BlockItemNode> suffix;
		/**
		 * a cache that helps reduce the number of intermediate variables for handling
		 * MPI_datatypes and MPI_extents
		 */
		Map<String, String> mpiDatatypeToIntermediateName;
		/**
		 * a counter for assigning unique names for intermediate variables
		 */
		int nameCounter;

		ClauseTransformGuide(ContractClause clause, List<ExpressionNode> conditions, List<ExpressionNode> arrivends,
				Map<String, String> mpiDatatype2IntermediateName, int nameCounter) {
			prefix = new LinkedList<>();
			substitutions = new HashMap<>();
			suffix = new LinkedList<>();
			mpiDatatypeToIntermediateName = mpiDatatype2IntermediateName;
			this.nameCounter = nameCounter;
			this.clause = clause;
			this.conditions = conditions;
			this.arrivends = arrivends;
			this.sideConditions = new LinkedList<>();
		}
	}

	public static void transformAssume(ContractClause clause, ASTFactory astFactory, boolean isLocal,
			boolean useRankAsPID, ClauseTransformGuide out) throws SyntaxException {
		if (clause.specialReferences != null) {
			// TODO: need to find new way to translate \old, now that we got rid of
			// value_at...
			// transformAcslOldExpression(clause, astFactory, civlcPreState, isLocal,
			// useRankAsPID, out);
			transformAcslResult(clause, astFactory, out);
			transformAcslValid(clause, astFactory, true, out);
		}
	}

	public static void transformAssert(ContractClause clause, ASTFactory astFactory, boolean isLocal,
			boolean useRankAsPID, ClauseTransformGuide out) throws SyntaxException {
		if (clause.specialReferences != null) {
			// transformAcslOldExpression(clause, astFactory, civlcPreState, isLocal,
			// useRankAsPID, out);
			transformAcslResult(clause, astFactory, out);
			// no need to transform valid (and MPI_valid)
		}
	}

	/* *********** Methods transforming special expressions ********** */

	private static void transformAcslResult(ContractClause clause, ASTFactory astFactory, ClauseTransformGuide out) {
		NodeFactory nf = astFactory.getNodeFactory();

		for (ExpressionNode expr : clause.specialReferences.acslResults) {
			ExpressionNode resultVar = identifierExpression(nf, MPIContractUtilities.ACSL_RESULT_VAR, expr.getSource());

			out.substitutions.put(expr, new CommonASTNodeSubstituteGuide(resultVar, expr));
		}
	}

	private static void transformAcslValid(ContractClause clause, ASTFactory astFactory, boolean isAssume,
			ClauseTransformGuide out) throws SyntaxException {
		assert isAssume;

		NodeFactory nf = astFactory.getNodeFactory();

		for (ExpressionNode expr : clause.specialReferences.acslValidExpressions) {
			OperatorNode valid = (OperatorNode) expr;
			Triple<ExpressionNode, ExpressionNode, ExpressionNode> pointer_offset_extent = processAcslValidWorker(nf,
					valid);
			ExpressionNode pointer = pointer_offset_extent.first;
			ExpressionNode offset = pointer_offset_extent.second;
			ExpressionNode extent = pointer_offset_extent.third;
			ExpressionNode subst;
			TypeNode elementType = getPointerReferredTypeNode(nf, pointer);

			if (offset != null)
				if (!offset.isConstantExpression() || ((ConstantNode) offset).getConstantValue().isZero() != Answer.YES)
					pointer = nf.newOperatorNode(pointer.getSource(), Operator.PLUS, pointer.copy(), offset);
			subst = createAllocation(astFactory, clause, pointer, elementType, extent, expr.getSource(), out);
			out.substitutions.put(expr, new CommonASTNodeSubstituteGuide(subst, expr));
		}
	}

	/* ********************** Public Utils *********************** */
	public static Pair<ExpressionNode, ExpressionNode> processAcslValid(NodeFactory nf, OperatorNode valid)
			throws SyntaxException {
		Triple<ExpressionNode, ExpressionNode, ExpressionNode> pointer_offset_extent = processAcslValidWorker(nf,
				valid);
		ExpressionNode pointer = pointer_offset_extent.first;
		ExpressionNode offset = pointer_offset_extent.second;

		if (offset != null)
			if (!offset.isConstantExpression() || ((ConstantNode) offset).getConstantValue().isZero() != Answer.YES)
				pointer = nf.newOperatorNode(pointer.getSource(), Operator.PLUS, pointer.copy(), offset);
		return new Pair<>(pointer, pointer_offset_extent.third);
	}

	/* ********************** Private Utils *********************** */
	private static ExpressionNode identifierExpression(NodeFactory nf, String name, Source source) {
		return nf.newIdentifierExpressionNode(source, nf.newIdentifierNode(source, name));
	}

	private static Triple<ExpressionNode, ExpressionNode, ExpressionNode> decomposeRange(NodeFactory nf,
			RegularRangeNode range) throws SyntaxException {
		ExpressionNode low = range.getLow().copy();
		ExpressionNode high = range.getHigh().copy();
		Value constantVal = nf.getConstantValue(low);
		ExpressionNode count = constantVal.isZero() != Answer.YES
				? nf.newOperatorNode(range.getSource(), Operator.MINUS, high, low)
				: high;

		count = nf.newOperatorNode(low.getSource(), Operator.PLUS, count,
				nf.newIntegerConstantNode(range.getSource(), "1"));
		return new Triple<>(low, high, count);
	}

	private static TypeNode getPointerReferredTypeNode(NodeFactory nf, ExpressionNode pointer) throws SyntaxException {
		Type referredType = ((PointerType) pointer.getType()).referencedType();

		return BaseWorker.typeNode(pointer.getSource(), referredType, nf);
	}

	private static ExpressionNode createAllocation(ASTFactory af, ContractClause clause, ExpressionNode pointer,
			TypeNode elementType, ExpressionNode numElements, Source source, ClauseTransformGuide out)
			throws SyntaxException {
		NodeFactory nf = af.getNodeFactory();
		TypeNode arrayType = nf.newArrayTypeNode(source, elementType.copy(), numElements.copy());
		String allocationName = MPIContractUtilities.nextAllocationName(out.nameCounter++);
		IdentifierNode allocationIdentifierNode;

		pointer = ContractTransformerWorker.decast(pointer);
		allocationIdentifierNode = nf.newIdentifierNode(pointer.getSource(), allocationName);

		VariableDeclarationNode artificialVariable = nf.newVariableDeclarationNode(source, allocationIdentifierNode,
				arrayType);
		// assign allocated object to pointer;
		ExpressionNode assign = nf.newOperatorNode(source, Operator.ASSIGN,
				Arrays.asList(pointer.copy(), nf.newIdentifierExpressionNode(source, allocationIdentifierNode.copy())));
		ExpressionNode extentGTzero = arrayExtentsGTZero(nf, artificialVariable.getTypeNode(), source);

		// For allocation, array objects need assumptions for valid extents;
		// variables as memory objects must be inserted in some place where
		// is visible to all contracts...
		// TODO: use assume push might be better here:
		out.prefix.add(createAssumption(nf, extentGTzero));
		out.prefix.add(artificialVariable);
		out.sideConditions.add(extentGTzero);

		ExpressionNode conditions = conjunct(af, out.conditions);

		if (conditions != null)
			out.prefix.add(nf.newIfNode(source, conditions, nf.newExpressionStatementNode(assign)));
		else
			out.prefix.add(nf.newExpressionStatementNode(assign));
		return nf.newBooleanConstantNode(source, true);
	}

	private static ExpressionNode arrayExtentsGTZero(NodeFactory nf, TypeNode type, Source source)
			throws SyntaxException {
		if (type.kind() != TypeNodeKind.ARRAY)
			return null;

		ArrayTypeNode arrayType = (ArrayTypeNode) type;
		ExpressionNode extentsGTZero = arrayExtentsGTZero(nf, arrayType.getElementType(), source);
		ExpressionNode myExtentGTZero = nf.newOperatorNode(source, Operator.GT, arrayType.getExtent().copy(),
				nf.newIntegerConstantNode(source, "0"));

		if (extentsGTZero == null)
			extentsGTZero = myExtentGTZero;
		else
			extentsGTZero = nf.newOperatorNode(source, Operator.LAND, extentsGTZero, myExtentGTZero);
		return extentsGTZero;
	}

	private static StatementNode createAssumption(NodeFactory nf, ExpressionNode pred) {
		ExpressionNode assumeIdentifier = identifierExpression(nf, BaseWorker.ASSUME, pred.getSource());
		FunctionCallNode assumeCall = nf.newFunctionCallNode(pred.getSource(), assumeIdentifier,
				Arrays.asList(pred.copy()));

		return nf.newExpressionStatementNode(assumeCall);
	}

	private static Triple<ExpressionNode, ExpressionNode, ExpressionNode> processAcslValidWorker(NodeFactory nf,
			OperatorNode valid) throws SyntaxException {
		ExpressionNode arg = valid.getArgument(0);
		ExpressionNode pointer, extent, offset = null;

		// Check if the argument of valid is in a limited form:
		if (arg.expressionKind() == ExpressionKind.OPERATOR) {
			OperatorNode opNode = (OperatorNode) arg;
			ExpressionNode range;

			if (opNode.getOperator() != Operator.PLUS)
				throw new CIVLSyntaxException(
						"CIVL requires the argument of \\valid " + "expression to be a limited form:\n"
								+ "ptr (+ range)?\n" + "range can be either an integer-expression\n "
								+ "or has the form \"integer-expression .. integer-expression\"",
						opNode.getSource());
			pointer = opNode.getArgument(0);
			range = opNode.getArgument(1).copy();
			if (range.expressionKind() == ExpressionKind.REGULAR_RANGE) {
				Triple<ExpressionNode, ExpressionNode, ExpressionNode> tri = decomposeRange(nf,
						(RegularRangeNode) range);

				offset = tri.first; // low
				extent = tri.third; // high - low + 1
			} else
				extent = nf.newIntegerConstantNode(range.getSource(), "1");
		} else {
			pointer = arg;
			extent = nf.newIntegerConstantNode(valid.getSource(), "1");
		}
		return new Triple<>(pointer, offset, extent);
	}

	private static ExpressionNode conjunct(ASTFactory af, List<ExpressionNode> exprs) {
		Iterator<ExpressionNode> iter = exprs.iterator();
		ExpressionNode result = null;
		Source source = null;
		TokenFactory tf = af.getTokenFactory();
		NodeFactory nf = af.getNodeFactory();

		while (iter.hasNext()) {
			ExpressionNode expr = iter.next();

			source = source != null ? tf.join(source, expr.getSource()) : expr.getSource();
			result = result != null ? nf.newOperatorNode(source, Operator.LAND, expr.copy(), result) : expr.copy();
		}
		return result;
	}
}
