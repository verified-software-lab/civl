package dev.civl.mc.transform.common;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.StringLiteralNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.DeclarationListNode;
import dev.civl.abc.ast.node.IF.statement.ForLoopInitializerNode;
import dev.civl.abc.ast.node.IF.statement.ForLoopNode;
import dev.civl.abc.ast.node.IF.statement.LoopNode;
import dev.civl.abc.ast.node.IF.statement.LoopNode.LoopKind;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode.StatementKind;
import dev.civl.abc.front.IF.CivlcTokenConstant;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.CivlcToken.TokenVocabulary;
import dev.civl.abc.token.IF.Formation;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.StringLiteral;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.mc.config.IF.CIVLConfiguration;

public class LoopContractTransformerWorker extends BaseWorker {
	/**
	 * A reference to {@link NodeFactory}
	 */
	private NodeFactory nodeFactory;

	/* ************************ Static fields ****************************** */

	/* *** Function identifiers *** */
	private final static String ASSUME_PUSH = "$assume_push";

	private final static String ASSUME_POP = "$assume_pop";

	private final static String WRITE_SET_PUSH = "$write_set_push";

	private final static String WRITE_SET_POP = "$write_set_pop";

	private final static String MEM_EMPTY = "$mem_empty";

	private final static String MEM_CONTAINS = "$mem_contains";

	private final static String MEM_UNION = "$mem_union";

	private final static String MEM_UNION_WIDENING = "$mem_union_widening";

	private final static String MEM_UNARY_WIDENING = "$mem_unary_widening";

	private final static String MEM_HAVOC = "$mem_havoc";

	@SuppressWarnings("unused")
	private final CIVLConfiguration config;

	@Deprecated
	private final static String MEM_HAVOC_SIDECOND = "$mem_havoc_sidecond";

	/* *** Generated identifier prefixes: *** */
	/**
	 * Names for miscellaneous temporary variables that do not need to be passed
	 * among different methods.
	 */
	private final static String LOOP_TMP_VAR_PREFIX = "_loop_tmp";

	/* *** counters for generating unique identifiers *** */

	private int loopTmpCounter = 0;

	/* ************** primitives for error reporting **************** */

	/**
	 * The token used to construct a {@link StringLiteral} which is used to report
	 * assertion violation.
	 */
	private final CivlcToken loopInvariantsViolationMessageToken;

	/**
	 * The static string literal of loop invariants violation messages
	 */
	private static final String violationMessage = "\"loop invariants violation\"";

	/**
	 * The token used to construct a {@link StringLiteral} which is used to report
	 * frame condition violation.
	 */
	private final CivlcToken frameConditionViolationMessageToken;

	/**
	 * The static string literal of frame condition violation messages
	 */
	private static final String frameConditionViolationMessage = "\"loop assigns violation\"";

	/**
	 * The token used to construct a {@link StringLiteral} which is used to report
	 * assertion establish violation.
	 */
	private final CivlcToken loopInvariantsEstablishViolationMessageToken;

	/**
	 * The static string literal of loop invariants establish violation messages
	 */
	private static final String establishViolationMessage = "\"loop invariants establish violation\"";

	/* ******************** static methods ********************** */
	/**
	 * @param node An {@link ASTNode}.
	 * @return true iff the given {@link ASTNode} represents a function definition.
	 */
	static private boolean isFunctionDefinition(ASTNode node) {
		return node.nodeKind() == NodeKind.FUNCTION_DEFINITION;
	}

	/**
	 * @param node An {@link ASTNode}.
	 * @return true iff the given {@link ASTNode} represents a loop.
	 */
	static private boolean isLoopNode(ASTNode node) {
		if (node.nodeKind() == NodeKind.STATEMENT)
			return ((StatementNode) node).statementKind() == StatementKind.LOOP;
		return false;
	}

	/**
	 * @param node An {@link LoopNode}.
	 * @return true iff the given {@link LoopNode} represents a loop.
	 */
	static private boolean isContractedLoop(LoopNode loop) {
		return loop.loopContracts() != null && loop.loopContracts().numChildren() > 0;
	}

	/**
	 * @param loop An {@link LoopNode}.
	 * @return true iff the given {@link LoopNode} represents a for-loop.
	 */
	static private boolean isForLoop(LoopNode loop) {
		return loop.getKind() == LoopKind.FOR;
	}

	/* ******************* Constructor ********************** */

	/**
	 * @return A unique identifier name for miscellaneous variables which are only
	 *         used by within one Java method.
	 */
	private String nextLoopTmpIdentifier() {
		return LOOP_TMP_VAR_PREFIX + loopTmpCounter++;
	}

	/* ******************* Constructor ********************** */

	public LoopContractTransformerWorker(String transformerName, ASTFactory astFactory, CIVLConfiguration config)
			throws SyntaxException {
		super(transformerName, astFactory);
		this.nodeFactory = astFactory.getNodeFactory();
		this.config = config;
		Formation feedBackformation = tokenFactory.newTransformFormation(transformerName, "violation report");

		frameConditionViolationMessageToken = tokenFactory.newCivlcToken(CivlcTokenConstant.STRING_LITERAL,
				frameConditionViolationMessage, feedBackformation, TokenVocabulary.DUMMY);
		loopInvariantsViolationMessageToken = tokenFactory.newCivlcToken(CivlcTokenConstant.STRING_LITERAL,
				violationMessage, feedBackformation, TokenVocabulary.DUMMY);
		loopInvariantsEstablishViolationMessageToken = tokenFactory.newCivlcToken(CivlcTokenConstant.STRING_LITERAL,
				establishViolationMessage, feedBackformation, TokenVocabulary.DUMMY);
	}

	/* **************** Main transformation logic methods ****************** */
	@Override
	protected AST transformCore(AST ast) throws SyntaxException {
		SequenceNode<BlockItemNode> root = ast.getRootNode();
		boolean hasContractedLoop = false;

		ast.release();
		for (BlockItemNode block : root) {
			if (isFunctionDefinition(block)) {
				FunctionDefinitionNode funcDefi = (FunctionDefinitionNode) block;

				if (transformLoopInFunction(funcDefi.getBody()))
					hasContractedLoop = true;
			}
		}

		if (!hasContractedLoop)
			return astFactory.newAST(root, ast.getSourceFiles(), ast.isWholeProgram());
		;

		ast = astFactory.newAST(root, ast.getSourceFiles(), ast.isWholeProgram());
		// ast.prettyPrint(System.out, true);
		return ast;
	}

	/**
	 * Given a function body, transform all contracted loops in it into CIVL-C
	 * codes.
	 * 
	 * @param body The root node of a sub-ASTree representing a function body
	 * @return true iff at least one loop in this function has been annotated
	 * @throws SyntaxException
	 */
	private boolean transformLoopInFunction(BlockItemNode body) throws SyntaxException {
		ASTNode node = body;
		LoopContractBlock annotatedLoop;
		ASTNode parent = body.parent();
		int bodyChildIdx = body.childIndex();
		boolean hasContractedLoop = false;

		// temporarily take off the body so that the DFS will only traverse the
		// body:
		body.remove();
		do {
			// transform nested function definitions:
			if (isFunctionDefinition(node)) {
				boolean innerHasContractedLoop = transformLoopInFunction(((FunctionDefinitionNode) node).getBody());

				if (innerHasContractedLoop)
					hasContractedLoop = true;
			}
			// transform loop:
			if (isLoopNode(node)) {
				LoopNode loop = (LoopNode) node;

				if (isContractedLoop(loop)) {
					// transform the loop
					annotatedLoop = new LoopContractBlock(loop);
					// skip the whole loop then continue. The loopNode will be
					// removed in transformLoopWorker method, so the Skip call
					// must happen before it.
					node = BaseWorker.nextDFSSkip(loop);
					transformLoopWorker(annotatedLoop);
					hasContractedLoop = true;
					continue;
				}
			}
			node = node.nextDFS();
		} while (node != null);
		parent.setChild(bodyChildIdx, body);
		return hasContractedLoop;
	}

	/**
	 * Transform a contracted loop including nested ones into pure CIVL-C codes.
	 * 
	 * @param loop
	 * @throws SyntaxException
	 */
	private void transformLoopWorker(LoopContractBlock loop) throws SyntaxException {
		// transform inner loops
		transformLoopInFunction(loop.getLoopNode().getBody());

		// duplicateLoop.loopContracts().remove();
		List<BlockItemNode> LISEComponents = new LinkedList<>();
		ASTNode loopParent = loop.getLoopNode().parent();
		int childIdx = loop.getLoopNode().childIndex();

		if (!loop.getLoopAssignSet().isEmpty()) {
			// transforms loop body with [loop-assigns]:
			LISEComponents.add(toNDBranch(loop, inductionStepWithAssigns(loop)));
		} else {
			// transforms loop body by inferring [loop-assigns] automatically:
			String writeSetName = nextLoopTmpIdentifier();
			LISEComponents.add(memTypeVariableDeclaration(writeSetName));
			LISEComponents.addAll(inductionStepInferringAssigns(loop, writeSetName));
			LISEComponents.addAll(createConclusion(loop, writeSetName));
		}

		Source source = loop.getLoopNode().getSource();
		BlockItemNode LISEBlock = nodeFactory.newCompoundStatementNode(source, LISEComponents);

		loopParent.setChild(childIdx, LISEBlock);
	}

	/* **************** Loop transformation helper methods ****************** */
	/**
	 * <p>
	 * Transform the loop body to the form that proves the induction step w.r.t the
	 * given invariants through computing the write set until a fixed-point reached.
	 * </p>
	 * 
	 * <code>
	 *   $mem ws = $mem_empty();
	 *   $assert(loop-inv); // base case establishment
	 *   while ($choose_int(1)) {
	 *     $mem_havoc(ws);
	 *     $assume_push([loop-inv] && [loop-cond]);
	 *     $write_push();
	 *     [loop-body]
	 *     $assert([loop-inv]); // invariant preserves
	 *     
	 *     $mem tmp = $write_pop();
	 *     
	 *     ws = $mem_union_widening(ws, tmp);
	 *     $assume_pop();
	 *   }
	 * </code>
	 */
	private List<BlockItemNode> inductionStepInferringAssigns(LoopContractBlock loop, String collectedWriteSetName)
			throws SyntaxException {
		List<BlockItemNode> result = new LinkedList<>();

		result.addAll(getForLoopInitializers(loop));
		result.add(createAssertion(loop.getLoopInvariants(nodeFactory), 0));
		result.add(createAssignment(identifierExpression(collectedWriteSetName), createMemEmptyCall()));

		List<BlockItemNode> whileLoopBody = new LinkedList<>();
		ExpressionNode inv = loop.getLoopInvariants(nodeFactory);
		ExpressionNode loopCond = loop.getLoopNode().getCondition();
		Source invAndCondSource = tokenFactory.join(inv.getSource(), loopCond.getSource());
		ExpressionNode invAndCond = nodeFactory.newOperatorNode(invAndCondSource, Operator.LAND, inv.copy(),
				loopCond.copy());

		whileLoopBody.addAll(createMemHavoc(collectedWriteSetName));
		whileLoopBody.add(createAssumptionPush(invAndCond));
		whileLoopBody.add(createWriteSetPush());
		whileLoopBody.add(wrapLoopBody(loop));
		// assert (inv);
		whileLoopBody.add(createAssertion(inv.copy(), 2));

		String tmpWsName = nextLoopTmpIdentifier();

		whileLoopBody.add(memTypeVariableDeclaration(tmpWsName));
		// pop write set
		whileLoopBody.add(createWriteSetPop(tmpWsName));
		whileLoopBody.add(createMemUnionWidening(collectedWriteSetName, tmpWsName, collectedWriteSetName));
		whileLoopBody.add(createAssumptionPop());

		ExpressionNode choiceAsCond = createNDBinaryChoice();
		Source whileLoopSource = joinSource(whileLoopBody);

		result.add(nodeFactory.newWhileLoopNode(whileLoopSource, choiceAsCond,
				nodeFactory.newCompoundStatementNode(whileLoopSource, whileLoopBody), null));
		return result;
	}

	/**
	 * <p>
	 * the induction proof code for a loop body.
	 * </p>
	 * 
	 * <p>
	 * The translation is : <code>
	 *   $assert(loop-inv); // base case establishment
	 *   
	 *   $mem ws = $mem_union([loop-assigns]);
	 *   
	 *   $mem_havoc(ws);
	 *   $assume([loop-inv] && [loop-cond]);
	 *   $write_set_push();
	 *   [loop-body];
	 *   
	 *   $mem tmp = $write_set_pop();
	 *   
	 *   $assert($mem_contains(ws, tmp));
	 *   $assert([loop-inv]);
	 *   while(true); // to an endless loop so that this state space path ends
	 * </code>
	 * </p>
	 * 
	 * @throws SyntaxException
	 */
	private List<BlockItemNode> inductionStepWithAssigns(LoopContractBlock loop) throws SyntaxException {
		assert !loop.getLoopAssignSet().isEmpty();

		List<BlockItemNode> results = new LinkedList<>();
		ExpressionNode inv = loop.getLoopInvariants(nodeFactory);
		ExpressionNode loopCond = loop.getLoopNode().getCondition();
		Source invAndCondSource = tokenFactory.join(inv.getSource(), loopCond.getSource());
		ExpressionNode invAndCond = nodeFactory.newOperatorNode(invAndCondSource, Operator.LAND, inv.copy(),
				loopCond.copy());
		String writeSetName = nextLoopTmpIdentifier();

		results.addAll(getForLoopInitializers(loop));
		results.add(createAssertion(loop.getLoopInvariants(nodeFactory), 0));
		// declaring a $mem variable for holding the set of modified memory
		// locations:
		results.add(memTypeVariableDeclaration(writeSetName));
		// havoc the change set either specified in loop assigns:
		results.addAll(unionLoopAssigns(loop.getLoopAssignSet(), writeSetName));
		results.addAll(createMemHavoc(writeSetName));
		// assume([loop-inv] && [loop-cond]) ...
		results.add(assumeNode(invAndCond));
		// write set push ...
		results.add(createWriteSetPush());

		// original body ...
		results.add(wrapLoopBody(loop));
		// assert (inv);
		results.add(createAssertion(inv.copy(), 2));

		String tmpWsName = nextLoopTmpIdentifier();

		results.add(memTypeVariableDeclaration(tmpWsName));
		// pop write set:
		results.add(createWriteSetPop(tmpWsName));
		// check frame-condition:
		results.addAll(checkFrameCondition(tmpWsName, writeSetName));

		// add endless loop to terminate the search path:
		results.addAll(endlessWhileLoop());
		return results;
	}

	/**
	 * Wraps the original loop body (and the incrementor if it is a for-loop) in a
	 * pair of curly-braces.
	 */
	private BlockItemNode wrapLoopBody(LoopContractBlock loop) {
		// original body ...
		List<BlockItemNode> bodyAndIncrementor = new LinkedList<>();
		StatementNode originalBody = loop.getLoopNode().getBody();

		bodyAndIncrementor.add(originalBody);
		originalBody.remove();
		// for-loop incrementer ...
		bodyAndIncrementor.addAll(getForLoopIncrementors(loop));
		// wrap the original body with a pair of curly braces so that loop-body
		// local variables won't be saved in the write set:
		return nodeFactory.newCompoundStatementNode(originalBody.getSource(), bodyAndIncrementor);
	}

	/**
	 * 
	 * @return the loop initializers of the <code>loop</code>, if it is a for-loop;
	 *         empty list otherwise.
	 */
	private List<BlockItemNode> getForLoopInitializers(LoopContractBlock loop) {
		List<BlockItemNode> result = new LinkedList<>();

		// loop initializer:
		if (isForLoop(loop.getLoopNode())) {
			ForLoopNode forLoop = (ForLoopNode) loop.getLoopNode();
			ForLoopInitializerNode initializer = forLoop.getInitializer();

			if (initializer != null) {
				if (initializer instanceof ExpressionNode)
					result.add(nodeFactory.newExpressionStatementNode((ExpressionNode) initializer.copy()));
				else {
					DeclarationListNode declList = (DeclarationListNode) initializer;

					for (VariableDeclarationNode decl : declList) {
						result.add(decl.copy());
					}
				}
			}
		}
		return result;
	}

	/**
	 * @returns copy of loop incrementors of the <code>loop</code> if it is a
	 *          for-loop; empty list otherwise.
	 */
	private List<BlockItemNode> getForLoopIncrementors(LoopContractBlock loop) {
		List<BlockItemNode> result = new LinkedList<>();

		if (isForLoop(loop.getLoopNode())) {
			ForLoopNode forLoop = (ForLoopNode) loop.getLoopNode();

			if (forLoop.getIncrementer() != null)
				result.add(nodeFactory.newExpressionStatementNode(forLoop.getIncrementer().copy()));
		}
		return result;
	}

	/**
	 * The non-deterministic branch: one branch goes to the endless while loop
	 * completing the induction step and the other branch goes to the conclusion,
	 * i.e., havoc write set nicely, assume invariants hold, etc.
	 * 
	 * @throws SyntaxException
	 */
	private BlockItemNode toNDBranch(LoopContractBlock loop, List<BlockItemNode> inductionStep) throws SyntaxException {
		ExpressionNode bnd = createNDBinaryChoice();
		StatementNode inductionBranch, concludeBranch;
		Source inductionStepSource = joinSource(inductionStep);

		inductionBranch = nodeFactory.newCompoundStatementNode(inductionStepSource, inductionStep);
		// build conclusion branch ...
		List<BlockItemNode> concludeBranchComponents = new LinkedList<>();
		concludeBranchComponents.addAll(getForLoopInitializers(loop));
		concludeBranchComponents.addAll(createConclusion(loop, null));
		concludeBranch = nodeFactory.newCompoundStatementNode(joinSource(concludeBranchComponents),
				concludeBranchComponents);

		Source source = joinSource(Arrays.asList(bnd, inductionBranch, concludeBranch));

		return nodeFactory.newIfNode(source, bnd, inductionBranch, concludeBranch);
	}

	/**
	 * <p>
	 * Depending on whether <code>writeSetName</code> is <code>null</code>, the
	 * returned code is slightly different.
	 * </p>
	 * 
	 * <p>
	 * If<code>writeSetName</code> is <code>null</code>,<code>loop</code> must have
	 * non-empty loop-assigns. Returning <code>
	 * 
	 *  $mem_havoc([loop-assigns]);
	 *  $assume([loop-inv] && ![loop-cond]);
	 *  
	 * </code>
	 * </p>
	 * 
	 * <p>
	 * If<code>writeSetName</code> is <code>non-null</code>, returning <code>
	 * 
	 *  $mem_havoc(writeSetName);
	 *  $assume([loop-inv] && ![loop-cond]);
	 *  
	 * </code>
	 * </p>
	 */
	private List<BlockItemNode> createConclusion(LoopContractBlock loop, String writeSetName) {
		List<BlockItemNode> result = new LinkedList<>();

		if (writeSetName == null) {
			assert !loop.getLoopAssignSet().isEmpty();
			String tmpWsName = nextLoopTmpIdentifier();

			result.add(memTypeVariableDeclaration(tmpWsName));
			result.addAll(unionLoopAssigns(loop.getLoopAssignSet(), tmpWsName));
			result.addAll(createMemHavoc(tmpWsName));
		}

		// assuming (loop-inv && !loop-cond && sidecond) holds:
		ExpressionNode loopInv = loop.getLoopInvariants(nodeFactory);
		ExpressionNode loopCond = loop.getLoopNode().getCondition();
		ExpressionNode notLoopCond = nodeFactory.newOperatorNode(loopCond.getSource(), Operator.NOT, loopCond.copy());
		Source finalAssumeSource = joinSource(Arrays.asList(loopInv, notLoopCond));
		ExpressionNode assumption = nodeFactory.newOperatorNode(finalAssumeSource, Operator.LAND, loopInv.copy(),
				notLoopCond);

		result.add(assumeNode(assumption));
		return result;
	}

	/**
	 * @return <code>while(1);</code>
	 */
	private List<BlockItemNode> endlessWhileLoop() throws SyntaxException {
		Source source = this.newSource("while(1);", CivlcTokenConstant.WHILE);
		Source trueSource = this.newSource("1", CivlcTokenConstant.CONST);

		return Arrays.asList(nodeFactory.newWhileLoopNode(source, nodeFactory.newIntegerConstantNode(trueSource, "1"),
				nodeFactory.newNullStatementNode(source), null));
	}

	/* *********************** Utility methods ****************************** */
	@SuppressWarnings("unused")
	private boolean isStandardForLoop(LoopNode loop) {
		return loop instanceof ForLoopNode && ((ForLoopNode) loop).isStandard();
	}

	/**
	 * $mem_havoc(m);
	 */
	private List<BlockItemNode> createMemHavoc(String varName) {
		List<BlockItemNode> results = new LinkedList<>();
		String funcName;
		List<ExpressionNode> args;
		ExpressionNode call, m = identifierExpression(varName);

		funcName = MEM_HAVOC;
		args = Arrays.asList(m);
		call = nodeFactory.newFunctionCallNode(m.getSource(), identifierExpression(funcName), args);
		results.add(nodeFactory.newExpressionStatementNode(call));
		return results;
	}

	private BlockItemNode memTypeVariableDeclaration(String name) {
		Source source = newSource("$mem " + name, CivlcTokenConstant.DECLARATION);

		return nodeFactory.newVariableDeclarationNode(source, identifier(name), nodeFactory.newMemTypeNode(source));
	}

	private StatementNode createAssignment(ExpressionNode lhs, ExpressionNode rhs) {
		Source source = newSource(lhs.prettyRepresentation() + " = " + rhs.prettyRepresentation(),
				CivlcTokenConstant.ASSIGN);

		return nodeFactory.newExpressionStatementNode(nodeFactory.newOperatorNode(source, Operator.ASSIGN, lhs, rhs));
	}

	/**
	 * <code>
	 * $assert($mem_contains(loopAssignsUnionName, write_set));
	 * </code>
	 * 
	 * @throws SyntaxException
	 * 
	 */
	private List<BlockItemNode> checkFrameCondition(String writeSetName, String loopAssignsUnionName)
			throws SyntaxException {
		Source mcSource = newSource(MEM_CONTAINS, CivlcTokenConstant.CALL);
		List<BlockItemNode> results = new LinkedList<>();
		// assert
		ExpressionNode call = nodeFactory.newFunctionCallNode(mcSource, identifierExpression(MEM_CONTAINS),
				Arrays.asList(identifierExpression(loopAssignsUnionName), identifierExpression(writeSetName)));

		results.add(createAssertion(call, 1));
		return results;
	}

	/**
	 * <code>
	 *    writeSet = $mem_union(loopAssigns<sub>0</sub>, 
	 *                          loopAssigns<sub>1</sub>, 
	 *                          ...,
	 *                          loopAssigns<sub>n-1</sub>);
	 * </code>
	 */
	private List<BlockItemNode> unionLoopAssigns(List<ExpressionNode> loopAssigns, String writeSetName) {
		Source muSource = newSource(MEM_UNION, CivlcTokenConstant.CALL);
		Source meSource = newSource(MEM_EMPTY, CivlcTokenConstant.CALL);
		List<BlockItemNode> results = new LinkedList<>();

		// init to empty
		results.add(createAssignment(identifierExpression(writeSetName),
				nodeFactory.newFunctionCallNode(meSource, identifierExpression(MEM_EMPTY), Arrays.asList())));
		// executes union:
		for (ExpressionNode loopAssignsArg : loopAssigns) {
			ExpressionNode addrOf = nodeFactory.newOperatorNode(loopAssignsArg.getSource(), Operator.ADDRESSOF,
					loopAssignsArg.copy());
			ExpressionNode unionCall = nodeFactory.newFunctionCallNode(muSource, identifierExpression(MEM_UNION),
					Arrays.asList(identifierExpression(writeSetName), addrOf));

			results.add(createAssignment(identifierExpression(writeSetName), unionCall));
		}
		return results;
	}

	/**
	 * <code>$mem_unary_widening(memVarName)</code>
	 */
	@SuppressWarnings("unused")
	private BlockItemNode createMemWidening(String lhs, String operand) {
		Source source = newSource(MEM_UNARY_WIDENING, CivlcTokenConstant.CALL);
		return createAssignment(identifierExpression(lhs),
				functionCall(source, MEM_UNARY_WIDENING, Arrays.asList(this.identifierExpression(operand))));
	}

	/**
	 * <code>lhs = $mem_unary_widening($mem_union(operand1, operand2))</code>
	 */
	private BlockItemNode createMemUnionWidening(String lhs, String operand1, String operand2) {
		Source wideningsource = newSource(MEM_UNARY_WIDENING, CivlcTokenConstant.CALL);
		Source unionSource = newSource(MEM_UNION, CivlcTokenConstant.CALL);
		ExpressionNode unionNode = functionCall(unionSource, MEM_UNION,
				Arrays.asList(identifierExpression(operand1), identifierExpression(operand2)));
		ExpressionNode wideningNode = functionCall(wideningsource, MEM_UNARY_WIDENING, Arrays.asList(unionNode));

		return createAssignment(identifierExpression(lhs), wideningNode);
	}

	@SuppressWarnings("unused")
	private BlockItemNode createMemUnionWidening2(String lhs, String operand1, String operand2) {
		Source unionSource = newSource(MEM_UNION_WIDENING, CivlcTokenConstant.CALL);
		ExpressionNode unionNode = functionCall(unionSource, MEM_UNION_WIDENING,
				Arrays.asList(identifierExpression(operand1), identifierExpression(operand2)));

		return createAssignment(identifierExpression(lhs), unionNode);
	}

	/**
	 * <code>$mem_empty()</code> expression
	 */
	private ExpressionNode createMemEmptyCall() {
		Source source = newSource(MEM_EMPTY, CivlcTokenConstant.CALL);
		ExpressionNode callNode = functionCall(source, MEM_EMPTY, Arrays.asList());

		return callNode;
	}

	/**
	 * <code>lhs = $mem_havoc_sidecond(writeSet)</code>
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private BlockItemNode createMemHavocSidecond(String lhs, String writeSetName) {
		Source source = newSource(MEM_HAVOC_SIDECOND, CivlcTokenConstant.CALL);
		ExpressionNode callNode = functionCall(source, MEM_HAVOC_SIDECOND,
				Arrays.asList(identifierExpression(writeSetName)));

		return createAssignment(identifierExpression(lhs), callNode);
	}

	/**
	 * Creates an assertion function call with an argument "predicate".
	 * 
	 * @param predicate The {@link ExpressionNode} which represents a predicate. It
	 *                  is the only argument of an assertion call.
	 * @param kind      an integer indicating the kind of error message, there are 3
	 *                  options: 0: establishment violation; 1: frame-condition
	 *                  violation; 2: loop invariant preservation violation
	 * @return A created assert call statement node;
	 * @throws SyntaxException
	 */
	private StatementNode createAssertion(ExpressionNode predicate, int kind) throws SyntaxException {
		ExpressionNode assertIdentifier = identifierExpression(BaseWorker.ASSERT);
		Source source = newSource("$assert", CivlcTokenConstant.CALL);
		String errMsg = kind == 0 ? establishViolationMessage
				: kind == 1 ? frameConditionViolationMessage : violationMessage;
		CivlcToken errMsgToken = kind == 0 ? loopInvariantsEstablishViolationMessageToken
				: kind == 1 ? frameConditionViolationMessageToken : loopInvariantsViolationMessageToken;

		StringLiteralNode messageNode = nodeFactory.newStringLiteralNode(source, errMsg,
				astFactory.getTokenFactory().newStringToken(errMsgToken).getStringLiteral());
		FunctionCallNode assertCall = nodeFactory.newFunctionCallNode(source, assertIdentifier,
				Arrays.asList(predicate.copy(), messageNode));

		return nodeFactory.newExpressionStatementNode(assertCall);
	}

	/**
	 * Creates an assume_push function call with an argument "predicate".
	 * 
	 * @param predicate The {@link ExpressionNode} which represents a predicate. It
	 *                  is the only argument of an assumption call.
	 * @return A created assumption call statement node;
	 */
	private StatementNode createAssumptionPush(ExpressionNode predicate) {
		ExpressionNode assumeIdentifier = identifierExpression(ASSUME_PUSH);
		FunctionCallNode assumeCall = nodeFactory.newFunctionCallNode(predicate.getSource(), assumeIdentifier,
				Arrays.asList(predicate.copy()));

		return nodeFactory.newExpressionStatementNode(assumeCall);
	}

	/**
	 * Creates an assume_pop function call.
	 * 
	 * @return A created assumption call statement node;
	 */
	private StatementNode createAssumptionPop() {
		Source source = newSource(ASSUME_POP, CivlcTokenConstant.CALL);
		ExpressionNode assumeIdentifier = identifierExpression(ASSUME_POP);

		FunctionCallNode assumeCall = nodeFactory.newFunctionCallNode(source, assumeIdentifier, Arrays.asList());
		return nodeFactory.newExpressionStatementNode(assumeCall);
	}

	/**
	 * 
	 * @return a <code>$write_set_push()</code> statement node;
	 */
	private StatementNode createWriteSetPush() {
		Source source = this.newSource("$write_set_push", CivlcTokenConstant.CALL);
		ExpressionNode wsPushIdentifier = identifierExpression(WRITE_SET_PUSH);
		FunctionCallNode wsPushCall = nodeFactory.newFunctionCallNode(source, wsPushIdentifier, Arrays.asList());
		return nodeFactory.newExpressionStatementNode(wsPushCall);
	}

	/**
	 * <code>
	 * lhs = $write_set_pop();
	 * </code>
	 */
	private BlockItemNode createWriteSetPop(String lhs) {
		Source writeSetPop = newSource(WRITE_SET_POP, CivlcTokenConstant.CALL);

		return createAssignment(identifierExpression(lhs),
				nodeFactory.newFunctionCallNode(writeSetPop, identifierExpression(WRITE_SET_POP), Arrays.asList()));
	}

	/**
	 * @return <code>$choose_int(2)</code> call.
	 * @throws SyntaxException
	 */
	private ExpressionNode createNDBinaryChoice() throws SyntaxException {
		Source source = newSource("$choose_int(2)", CivlcTokenConstant.CALL);
		return nodeFactory.newFunctionCallNode(source, identifierExpression(CHOOSE_INT),
				Arrays.asList(nodeFactory.newIntegerConstantNode(source, "2")));
	}

	/**
	 * combines {@link Source}s of a list of iterable of AST nodes.
	 */
	private Source joinSource(Iterable<? extends ASTNode> nodes) {
		Source result = null;

		for (ASTNode node : nodes) {
			result = result == null ? node.getSource() : tokenFactory.join(result, node.getSource());
		}
		if (result == null)
			result = newSource("", CivlcTokenConstant.ABSENT);
		return result;
	}
}
