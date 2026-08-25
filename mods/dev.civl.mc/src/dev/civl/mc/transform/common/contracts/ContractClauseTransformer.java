package dev.civl.mc.transform.common.contracts;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;
import dev.civl.mc.transform.SubstituteGuide;
import dev.civl.mc.transform.common.BaseWorker;
import dev.civl.mc.transform.common.contracts.ClauseTransformGuideGenerator.ClauseTransformGuide;
import dev.civl.mc.transform.common.contracts.FunctionContractBlock.ConditionalClauses;
import dev.civl.mc.transform.common.contracts.FunctionContractBlock.ContractClause;

class ContractClauseTransformer {

	public static final String ContractClauseTransformerName = "Contract-clause";

	/**
	 * A reference to an instance of {@link NodeFactory}
	 */
	private NodeFactory nodeFactory;

	/**
	 * A reference to an instance of {@link ASTFactory}
	 */
	private ASTFactory astFactory;

	/**
	 * <p>
	 * This class consists of the final transformed ASTNodes which are divided into
	 * two groups: ASTNodes before the "function" and ASTNodes after the "function".
	 * </p>
	 * 
	 * <p>
	 * Here "function" refers to two kinds of code:
	 * <ol>
	 * <li>The target function call in the driver of the target function</li>
	 * <li>The transformation of "assigns" clauses of the callee functions since
	 * these are the parts "simulate" the side-effect modification of the
	 * function.</li>
	 * </ol>
	 * </p>
	 * 
	 * @author ziqingluo
	 */
	class TransformedPair {
		List<BlockItemNode> before;
		List<BlockItemNode> after;

		TransformedPair(List<BlockItemNode> before, List<BlockItemNode> after) {
			this.before = before;
			this.after = after;
		}
	}

	ContractClauseTransformer(ASTFactory astFactory, MemoryLocationManager memoryLocationManager) {
		this.astFactory = astFactory;
		this.nodeFactory = astFactory.getNodeFactory();
		// this.memoryLocationManager = memoryLocationManager;
	}

	/**
	 * <p>
	 * This methods analyzes the given {@link FunctionContractBlock}, creates
	 * {@link ClauseTransformGuide}s for requirements and ensurances. A
	 * {@link ClauseTransformGuide} is associated with one ACSL clauses such as a
	 * <code>requires</code> or a <code>ensures</code> clause.
	 * </p>
	 * 
	 * <p>
	 * The analysis process does not modify the ASTree hence this is suppose to
	 * happen before the release of the tree. During the analysis, new nodes will be
	 * generated in {@link ClauseTransformGuide}s which encodes the information of
	 * how to transform (modify) the program with those generated nodes.
	 * </p>
	 * 
	 * @param block                 The {@link FunctionContractBlock} which is
	 *                              either the sequential contract block or one of
	 *                              the MPI collective blocks
	 * @param isCallee              true iff the given contracts belong to a callee
	 *                              function
	 * @param isPurelyLocalFunction true iff the given contracts is sequential
	 *                              contract and it belongs to a function has no MPI
	 *                              collective contract
	 * @param requiresGuides        output. {@link ClauseTransformGuide}s for
	 *                              <code>requires</code> clauses
	 * @param ensuresGuides         output. {@link ClauseTransformGuide}s for
	 *                              <code>ensures</code> clauses
	 * @throws SyntaxException
	 */
	void analysisContractBlock(FunctionContractBlock block, boolean isCallee, boolean isPurelyLocalFunction,
			List<ClauseTransformGuide> requiresGuides, List<ClauseTransformGuide> ensuresGuides)
			throws SyntaxException {
		int nameCounter = 0;
		Map<String, String> mpiDatatype2intermediateName = new HashMap<>();

		for (ConditionalClauses condClause : block.getConditionalClauses()) {
			ContractClause requires = condClause.getRequires();
			ContractClause ensures = condClause.getEnsures();
			ClauseTransformGuide reqTransGuide = new ClauseTransformGuide(requires, condClause.getConditions(),
					condClause.getWaitsfors(), mpiDatatype2intermediateName, nameCounter);
			boolean isLocal = true;

			if (isCallee)
				ClauseTransformGuideGenerator.transformAssert(requires, astFactory, isLocal, !isPurelyLocalFunction,
						reqTransGuide);
			else
				ClauseTransformGuideGenerator.transformAssume(requires, astFactory, isLocal, !isPurelyLocalFunction,
						reqTransGuide);

			nameCounter = reqTransGuide.nameCounter; // update name counter
			ClauseTransformGuide ensTransGuide = new ClauseTransformGuide(ensures, condClause.getConditions(),
					condClause.getWaitsfors(), mpiDatatype2intermediateName, nameCounter);

			if (isCallee)
				ClauseTransformGuideGenerator.transformAssume(ensures, astFactory, isLocal, !isPurelyLocalFunction,
						ensTransGuide);
			else
				ClauseTransformGuideGenerator.transformAssert(ensures, astFactory, isLocal, !isPurelyLocalFunction,
						ensTransGuide);
			nameCounter = ensTransGuide.nameCounter; // update name counter
			requiresGuides.add(reqTransGuide);
			ensuresGuides.add(ensTransGuide);
		}
	}

	/**
	 * <p>
	 * Transform sequential function contracts to a {@link TransformedPair}. This
	 * process will actually modify the AST hence it must happen after the release
	 * of the AST.
	 * </p>
	 * 
	 * @param requiresGuides a list of {@link ClauseTransformGuide} for
	 *                       <code>requires</code> clauses
	 * @param ensuresGuides  a list of {@link ClauseTransformGuide} for
	 *                       <code>ensures</code> clauses
	 * @param localBlock     The {@link FunctionContractBlock} which contains (but
	 *                       not only contains) the <code>requires</code> and
	 *                       <code>ensures</code> clauses, which are associated with
	 *                       the given guides.
	 * @param isCallee       true iff the function, to which the contract belongs,
	 *                       is not the target function
	 * @return a {@link TransformedPair} which is the result of the transformation
	 *         of the given contract block
	 * @throws SyntaxException
	 */
	TransformedPair transformLocalBlock(List<ClauseTransformGuide> requiresGuides,
			List<ClauseTransformGuide> ensuresGuides, FunctionContractBlock localBlock, boolean isCallee)
			throws SyntaxException {
		List<BlockItemNode> reqTranslations = new LinkedList<>();
		List<BlockItemNode> ensTranslations = new LinkedList<>();
		List<BlockItemNode> assignsTranslations = new LinkedList<>();

		// transform requires:
		for (ClauseTransformGuide reqGuide : requiresGuides)
			reqTranslations.addAll(reqGuide.prefix);
		for (ClauseTransformGuide reqGuide : requiresGuides) {
			substitute(reqGuide);
			reqTranslations.addAll(createConditionalAssumeOrAssert(!isCallee, reqGuide.conditions,
					reqGuide.clause.getClauseExpressions()));
		}
		for (ClauseTransformGuide reqGuide : requiresGuides)
			reqTranslations.addAll(reqGuide.suffix);

		// transform ensures:
		for (ClauseTransformGuide ensGuide : ensuresGuides)
			ensTranslations.addAll(ensGuide.prefix);
		for (ClauseTransformGuide ensGuide : ensuresGuides) {
			substitute(ensGuide);
			ensTranslations.addAll(createConditionalAssumeOrAssert(isCallee, ensGuide.conditions,
					ensGuide.clause.getClauseExpressions()));
		}
		for (ClauseTransformGuide ensGuide : ensuresGuides)
			ensTranslations.addAll(ensGuide.suffix);

		if (isCallee) {
			// Transformation of "assigns" ...
			for (ConditionalClauses condClause : localBlock.getConditionalClauses())
				assignsTranslations.addAll(transformAssignsClause(!isCallee, condClause));
		}
		// TODO: check assigns for target (!isCallee)
		reqTranslations.addAll(assignsTranslations);
		return new TransformedPair(reqTranslations, ensTranslations);
	}

	/**
	 * Create assertions to check side conditions
	 */
	Collection<BlockItemNode> checkSideConditions(List<ClauseTransformGuide> guides) {
		List<ExpressionNode> sideConditions = new LinkedList<>();

		for (ClauseTransformGuide guide : guides) {
			ExpressionNode sideCondition = conjunct(guide.sideConditions);
			ExpressionNode assumptions = conjunct(guide.conditions);

			if (sideCondition == null)
				continue;
			if (assumptions != null)
				sideCondition = nodeFactory.newOperatorNode(sideCondition.getSource(), Operator.IMPLIES, assumptions,
						sideCondition);
			sideConditions.add(sideCondition);
		}
		if (!sideConditions.isEmpty())
			return Arrays.asList(createAssertion(conjunct(sideConditions)));
		else
			return Arrays.asList();
	}

	/**
	 * <p>
	 * Transforms "assigns" clauses under some behavior. The assigns clauses will be
	 * checked hold if they belong to the main verifying function; otherwise they
	 * will be transformed to a set of statements that havocs the memory locations
	 * specified by the assigns clauses.
	 * </p>
	 * 
	 * @param condClauses   {@link ConditionalClauses} containing "assigns" clauses
	 * @param isPurelyLocal true iff the transforming "assigns" clauses belongs to
	 *                      local contract block
	 * @param isTarget      true iff the function who owns the transforming
	 *                      "assigns" clauses is the main verifying function;
	 * @return the transformed result
	 * @throws SyntaxException
	 */
	private List<BlockItemNode> transformAssignsClause(boolean isTarget, ConditionalClauses condClauses)
			throws SyntaxException {
		if (condClauses.getAssignsArgs().isEmpty())
			return Arrays.asList();

		List<BlockItemNode> refreshments = new LinkedList<>();
		Source source = null;

		for (ExpressionNode memoryLocationSet : condClauses.getAssignsArgs()) {
			refreshments.addAll(refreshMemoryLocationSetExpression(memoryLocationSet));

			if (source == null)
				source = memoryLocationSet.getSource();
		}

		ExpressionNode condition = conjunct(condClauses.getConditions());

		if (condition != null) {
			StatementNode compoundStmt = nodeFactory.newCompoundStatementNode(source, refreshments);

			refreshments.clear();
			refreshments.add(nodeFactory.newIfNode(condition.getSource(), condition.copy(), compoundStmt));
		}
		return refreshments;
	}

	/*
	 * ************************************************************************
	 * Package artificial node creating helper methods:
	 **************************************************************************/

	/**
	 * <p>
	 * <b>Summary: </b> Creates an assertion function call with an argument
	 * "predicate".
	 * </p>
	 * 
	 * @param predicate The {@link ExpressionNode} which represents a predicate. It
	 *                  is the only argument of an assertion call.
	 * @return A created assert call statement node;
	 */
	private StatementNode createAssertion(ExpressionNode predicate) {
		ExpressionNode assertIdentifier = nodeFactory.newIdentifierExpressionNode(predicate.getSource(),
				nodeFactory.newIdentifierNode(predicate.getSource(), BaseWorker.ASSERT));
		FunctionCallNode assumeCall = nodeFactory.newFunctionCallNode(predicate.getSource(), assertIdentifier,
				Arrays.asList(predicate));
		return nodeFactory.newExpressionStatementNode(assumeCall);
	}

	/**
	 * <p>
	 * <b>Summary: </b> Creates an assumption function call with an argument
	 * "predicate".
	 * </p>
	 * 
	 * @param predicate The {@link ExpressionNode} which represents a predicate. It
	 *                  is the only argument of an assumption call.
	 * @return A created assumption call statement node;
	 */
	private StatementNode createAssumption(ExpressionNode predicate) {
		ExpressionNode assumeIdentifier = identifierExpressionNode(predicate.getSource(), BaseWorker.ASSUME);
		FunctionCallNode assumeCall = nodeFactory.newFunctionCallNode(predicate.getSource(), assumeIdentifier,
				Arrays.asList(predicate));
		return nodeFactory.newExpressionStatementNode(assumeCall);
	}

	private IdentifierExpressionNode identifierExpressionNode(Source source, String name) {
		return nodeFactory.newIdentifierExpressionNode(source, nodeFactory.newIdentifierNode(source, name));
	}

	/*
	 * *************************************************************************
	 * Methods process ASSIGNS clauses
	 **************************************************************************/
	/**
	 * <p>
	 * IF the expression is an MPI_REGION expression, transform it to
	 * <code>$mpi_assigns</code> function, which is defined in "civl-mpi.cvh".
	 * </p>
	 * 
	 * <p>
	 * Otherwise, transform it to $mem_havoc function, which is defined in "mem.cvh"
	 * </p>
	 * 
	 * @param expression    An expression represents a memory location set
	 * @param isPurelyLocal if the given expression belongs to a local contract
	 *                      block
	 * @return A {@link BlockItemNode} which consists of statements that will assign
	 *         fresh new symbolic constants to the given expression
	 * @throws SyntaxException When the given expression is not a valid memory
	 *                         location set expression.
	 */
	private List<BlockItemNode> refreshMemoryLocationSetExpression(ExpressionNode expression) throws SyntaxException {
		return refreshACSLMemoryLocationSetExpression(expression);
	}

	/**
	 * <code>
	 * $mem_havoc(m); 
	 * </code>
	 */
	private List<BlockItemNode> refreshACSLMemoryLocationSetExpression(ExpressionNode expression) {
		Source source = expression.getSource();
		ExpressionNode memHavocFuncIdent = identifierExpressionNode(source, MPIContractUtilities.MEM_HAVOC);
		ExpressionNode addrof = nodeFactory.newOperatorNode(source, Operator.ADDRESSOF, expression.copy());
		ExpressionNode tmp = nodeFactory.newFunctionCallNode(source, memHavocFuncIdent, Arrays.asList(addrof));

		List<BlockItemNode> results = new LinkedList<>();

		results.add(nodeFactory.newExpressionStatementNode(tmp));
		return results;
	}

	/**
	 * Substitutes sub-expressions in clause expressions with transformed
	 * expressions.
	 * 
	 * @param guide a instance of {@link ClauseTransformGuide} which encodes the
	 *              substitution map and clause expressions that will be
	 *              substituted.
	 */
	private void substitute(ClauseTransformGuide guide) {
		Map<ASTNode, ASTNode> substituted = new HashMap<>();

		for (ExpressionNode clause : guide.clause.getClauseExpressions())
			if (clause != null)
				visitAndSubstitute(clause, guide.substitutions, substituted);
	}

	/**
	 * Bottom-up substitution for the given expression with the map
	 * 
	 * @param expression the expression that will be substituted
	 * @param subMap     a substitution map from the old node references in the
	 *                   substituting expression to the
	 *                   {@link ASTNodeSubstituteGuide}s
	 * @param subHistory the history of substituted sub-expressions, which is needed
	 *                   to solve the problem that both a parent <code>A</code> and
	 *                   its child <code>B</code> in a subtree <code>A -> B</code>
	 *                   will be substituted. Since it's bottom-up, <code>B</code>
	 *                   is firstly substituted, which results in
	 *                   <code>A->B'</code>. Later, <code>A</code> is substituted to
	 *                   <code>A'</code> then the children of <code>A'</code> must
	 *                   be updated as well otherwise <code>B'</code> is lost.
	 */
	private void visitAndSubstitute(ExpressionNode expression, Map<ExpressionNode, SubstituteGuide> subMap,
			Map<ASTNode, ASTNode> subHistory) {
		int nchildren = expression.numChildren();

		for (int i = 0; i < nchildren; i++) {
			ASTNode child = expression.child(i);

			if (child == null || child.nodeKind() != NodeKind.EXPRESSION)
				continue;
			visitAndSubstitute((ExpressionNode) child, subMap, subHistory);
		}

		SubstituteGuide subNode = subMap.get(expression);

		if (subNode != null) {
			ASTNode substed = subNode.subsitute(nodeFactory);

			subHistory.put(expression, substed);
			nchildren = substed.numChildren();
			for (int i = 0; i < nchildren; i++) {
				ASTNode child = substed.child(i);
				ASTNode update = subHistory.get(child);

				if (update != null) {
					update.remove();
					child.remove();
					substed.setChild(i, update);
				}
			}
		}
	}

	private ExpressionNode conjunct(List<ExpressionNode> exprs) {
		Iterator<ExpressionNode> iter = exprs.iterator();
		ExpressionNode result = null;
		Source source = null;
		TokenFactory tf = astFactory.getTokenFactory();

		while (iter.hasNext()) {
			ExpressionNode expr = iter.next();

			source = source != null ? tf.join(source, expr.getSource()) : expr.getSource();
			result = result != null ? nodeFactory.newOperatorNode(source, Operator.LAND, expr.copy(), result)
					: expr.copy();
		}
		return result;
	}

	private List<BlockItemNode> createConditionalAssumeOrAssert(boolean isAssume, List<ExpressionNode> conditions,
			List<ExpressionNode> expressions) {
		ExpressionNode pred = conjunct(expressions);

		if (pred == null)
			return Arrays.asList();

		ExpressionNode cond = conjunct(conditions);
		StatementNode assumeOrAssert;

		if (isAssume)
			assumeOrAssert = createAssumption(pred);
		else
			assumeOrAssert = createAssertion(pred);
		if (cond != null)
			return Arrays.asList(nodeFactory.newIfNode(cond.getSource(), cond, assumeOrAssert));
		else
			return Arrays.asList(assumeOrAssert);
	}
}
