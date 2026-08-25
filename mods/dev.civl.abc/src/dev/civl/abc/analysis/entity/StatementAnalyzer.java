package dev.civl.abc.analysis.entity;

import java.util.Iterator;
import java.util.Stack;

import dev.civl.abc.ast.conversion.IF.ConversionFactory;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.entity.IF.Label;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.PragmaNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.StaticAssertionNode;
import dev.civl.abc.ast.node.IF.acsl.ContractNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.TypedefDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.label.LabelNode;
import dev.civl.abc.ast.node.IF.label.OrdinaryLabelNode;
import dev.civl.abc.ast.node.IF.label.SwitchLabelNode;
import dev.civl.abc.ast.node.IF.omp.OmpExecutableNode;
import dev.civl.abc.ast.node.IF.omp.OmpExecutableNode.OmpExecutableKind;
import dev.civl.abc.ast.node.IF.omp.OmpForNode;
import dev.civl.abc.ast.node.IF.omp.OmpFunctionReductionNode;
import dev.civl.abc.ast.node.IF.omp.OmpNode;
import dev.civl.abc.ast.node.IF.omp.OmpNode.OmpNodeKind;
import dev.civl.abc.ast.node.IF.omp.OmpParallelNode;
import dev.civl.abc.ast.node.IF.omp.OmpReductionNode;
import dev.civl.abc.ast.node.IF.omp.OmpReductionNode.OmpReductionNodeKind;
import dev.civl.abc.ast.node.IF.omp.OmpWorksharingNode;
import dev.civl.abc.ast.node.IF.statement.AtomicNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.ChooseStatementNode;
import dev.civl.abc.ast.node.IF.statement.CivlForNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.DeclarationListNode;
import dev.civl.abc.ast.node.IF.statement.ExpressionStatementNode;
import dev.civl.abc.ast.node.IF.statement.ForLoopInitializerNode;
import dev.civl.abc.ast.node.IF.statement.ForLoopNode;
import dev.civl.abc.ast.node.IF.statement.IfNode;
import dev.civl.abc.ast.node.IF.statement.JumpNode;
import dev.civl.abc.ast.node.IF.statement.JumpNode.JumpKind;
import dev.civl.abc.ast.node.IF.statement.LabeledStatementNode;
import dev.civl.abc.ast.node.IF.statement.LoopNode;
import dev.civl.abc.ast.node.IF.statement.ReturnNode;
import dev.civl.abc.ast.node.IF.statement.RunNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode.StatementKind;
import dev.civl.abc.ast.node.IF.statement.SwitchNode;
import dev.civl.abc.ast.node.IF.statement.WhenNode;
import dev.civl.abc.ast.node.IF.type.EnumerationTypeNode;
import dev.civl.abc.ast.node.IF.type.StructureOrUnionTypeNode;
import dev.civl.abc.ast.type.IF.DomainType;
import dev.civl.abc.ast.type.IF.IntegerType;
import dev.civl.abc.ast.type.IF.ObjectType;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.ast.type.IF.TypeFactory;
import dev.civl.abc.ast.value.IF.Value;
import dev.civl.abc.config.IF.Configuration;
import dev.civl.abc.err.IF.ABCRuntimeException;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.UnsourcedException;

//import dev.civl.abc.ast.node.IF.statement.AssertNode;

public class StatementAnalyzer {

	// ***************************** Fields *******************************

	private EntityAnalyzer entityAnalyzer;

	private NodeFactory nodeFactory;

	private ExpressionAnalyzer expressionAnalyzer;

	private TypeFactory typeFactory;

	private ConversionFactory conversionFactory;

	private AcslContractAnalyzer acslAnalyzer;

	// ************************** Constructors ****************************

	StatementAnalyzer(EntityAnalyzer entityAnalyzer, ExpressionAnalyzer expressionAnalyzer,
			ConversionFactory conversionFactory, TypeFactory typeFactory, Configuration config) {
		this.entityAnalyzer = entityAnalyzer;
		this.nodeFactory = entityAnalyzer.nodeFactory;
		this.expressionAnalyzer = expressionAnalyzer;
		this.conversionFactory = conversionFactory;
		this.typeFactory = typeFactory;
		this.acslAnalyzer = new AcslContractAnalyzer(entityAnalyzer, conversionFactory);
	}

	// ************************* Private Methods **************************

	private SyntaxException error(String message, ASTNode node) {
		return entityAnalyzer.error(message, node);
	}

	private SyntaxException error(UnsourcedException e, ASTNode node) {
		return entityAnalyzer.error(e, node);
	}

	private void processExpression(ExpressionNode expression) throws SyntaxException {
		if (expression != null)
			expressionAnalyzer.processExpression(expression);
	}

	private void processIf(IfNode node) throws SyntaxException {
		processExpression(node.getCondition());
		processStatement(node.getTrueBranch());
		if (node.getFalseBranch() != null)
			processStatement(node.getFalseBranch());
	}

	private SwitchNode enclosingSwitch(SwitchLabelNode labelNode) {
		for (ASTNode node = labelNode.parent(); node != null; node = node.parent()) {
			if (node instanceof SwitchNode)
				return (SwitchNode) node;
		}
		return null;
	}

	private ASTNode enclosingSwitchOrChoose(SwitchLabelNode labelNode) {
		for (ASTNode node = labelNode.parent(); node != null; node = node.parent()) {
			if (node instanceof SwitchNode || node instanceof ChooseStatementNode)
				return node;
		}
		return null;
	}

	private void processLabeledStatement(LabeledStatementNode node) throws SyntaxException {
		LabelNode labelNode = node.getLabel();
		StatementNode statementNode = node.getStatement();
		Function function = entityAnalyzer.enclosingFunction(node);

		if (function == null)
			throw error("Label occurs outside of function", node);
		labelNode.setStatement(statementNode);
		if (labelNode instanceof OrdinaryLabelNode)
			processOrdinaryLabel((OrdinaryLabelNode) labelNode, function);
		else if (labelNode instanceof SwitchLabelNode)
			processSwitchLabel(node, (SwitchLabelNode) labelNode, function);
		else
			throw new RuntimeException("unreachable");
		processStatement(statementNode);

	}

	private void processOrdinaryLabel(OrdinaryLabelNode node, Function function) throws SyntaxException {
		Label label = entityAnalyzer.entityFactory.newLabel(node);

		node.setFunction(function);
		node.setEntity(label);
		node.getIdentifier().setEntity(label);
		try {
			function.getScope().add(label);
		} catch (UnsourcedException e) {
			throw error(e, node);
		}
	}

	private void processSwitchLabel(LabeledStatementNode labeledStatement, SwitchLabelNode switchLabel,
			Function function) throws SyntaxException {

		if (switchLabel.isDefault()) {
			ASTNode enclosing = enclosingSwitchOrChoose(switchLabel);

			if (enclosing instanceof ChooseStatementNode) {
				ChooseStatementNode choose = (ChooseStatementNode) enclosing;
				LabeledStatementNode oldDefault = choose.getDefaultCase();

				if (oldDefault != null)
					throw error("Two default cases in choose statement.  First was at " + oldDefault.getSource(),
							switchLabel);
				choose.setDefaultCase(labeledStatement);
				return;
			}
		}

		SwitchNode switchNode = enclosingSwitch(switchLabel);

		if (switchNode == null)
			throw error("Switch label occurs outside of any switch statement", switchLabel);
		if (switchLabel.isDefault()) {
			LabeledStatementNode oldDefault = switchNode.getDefaultCase();

			if (oldDefault != null)
				throw error("Two default cases in switch statement.  First was at " + oldDefault.getSource(),
						switchLabel);
			switchNode.setDefaultCase(labeledStatement);
		} else {
			ExpressionNode caseExpression = switchLabel.getExpression();
			Iterator<LabeledStatementNode> cases = switchNode.getCases();
			Value constant;

			expressionAnalyzer.processExpression(caseExpression);
			if (!caseExpression.isConstantExpression()) {
				throw error("Case expression not constant", caseExpression);
			}
			constant = nodeFactory.getConstantValue(caseExpression);
			while (cases.hasNext()) {
				SwitchLabelNode labelNode = (SwitchLabelNode) cases.next().getLabel();
				ExpressionNode oldExpression = labelNode.getExpression();
				Value oldConstant = nodeFactory.getConstantValue(oldExpression);

				if (constant.equals(oldConstant))
					throw error("Case constant appears twice: first time was at " + oldExpression, caseExpression);
			}
			switchNode.addCase(labeledStatement);
		}
	}

	private void processJump(JumpNode statement) throws SyntaxException {
		switch (statement.getKind()) {
		case RETURN: {
			ExpressionNode expression = ((ReturnNode) statement).getExpression();
			Function function = entityAnalyzer.enclosingFunction(statement);
			ObjectType returnType = function.getType().getReturnType();
			boolean returnTypeIsVoid = returnType.kind() == TypeKind.VOID;

			if (expression == null) {
				if (!returnTypeIsVoid)
					throw error("Missing expression in return statement", statement);
			} else {
				if (returnTypeIsVoid)
					throw error("Argument for return in function returning void", statement);
				if (expression != null)
					processExpression(expression);
				try {
					expressionAnalyzer.processAssignment(returnType, expression);
				} catch (UnsourcedException e) {
					throw error(e, expression);
				}
			}
		}
		case GOTO: // taken care of later in processGotos
		case BREAK: // nothing to do
		case CONTINUE: // nothing to do
			break;
		default:
			throw new RuntimeException("Unreachable");
		}
	}

	private void processLoop(LoopNode loopNode) throws SyntaxException {
		SequenceNode<ContractNode> loopContracts = loopNode.loopContracts();

		switch (loopNode.getKind()) {
		case WHILE:
			processExpression(loopNode.getCondition());
			processStatement(loopNode.getBody());
			// processExpression(loopNode.getInvariant());
			break;
		case DO_WHILE:
			processStatement(loopNode.getBody());
			processExpression(loopNode.getCondition());
			// processExpression(loopNode.getInvariant());
			break;
		case FOR: {
			ForLoopNode forNode = (ForLoopNode) loopNode;
			ForLoopInitializerNode initializer = forNode.getInitializer();

			if (initializer == null) {
			} else if (initializer instanceof ExpressionNode) {
				processExpression((ExpressionNode) initializer);
			} else if (initializer instanceof DeclarationListNode) {
				DeclarationListNode declarationList = (DeclarationListNode) initializer;

				for (VariableDeclarationNode child : declarationList) {
					if (child == null)
						continue;
					entityAnalyzer.declarationAnalyzer.processVariableDeclaration(child);
				}
			} else
				throw error("Unknown kind of initializer clause in for loop", initializer);
			processExpression(loopNode.getCondition());
			processExpression(forNode.getIncrementer());
			processStatement(loopNode.getBody());
			// processExpression(loopNode.getInvariant());
			break;
		}
		default:
			throw new RuntimeException("Unreachable");
		}
		if (loopContracts != null) {
			acslAnalyzer.processLoopContractNodes(loopContracts);
		}
	}

	private void processCivlFor(CivlForNode node) throws SyntaxException {
		ExpressionNode domainNode = node.getDomain();
		int numVars = 0;
		Type domainNodeType;
		DomainType domainType;
		int domainDimension;

		for (VariableDeclarationNode child : node.getVariables()) {
			Type type;

			entityAnalyzer.declarationAnalyzer.processVariableDeclaration(child);
			if (child.getInitializer() != null)
				throw error("Loop variable " + numVars + " in $for/$parfor statement has initializer", child);
			type = child.getTypeNode().getType();
			if (!(type instanceof IntegerType))
				throw error("Loop variable " + numVars + " in $for/$parfor has non-integer type: " + type,
						child.getTypeNode());
			numVars++;
		}
		expressionAnalyzer.processExpression(domainNode);
		domainNodeType = domainNode.getConvertedType();
		if (domainNodeType.equals(typeFactory.rangeType())) {
			domainNode.addConversion(conversionFactory.regularRangeToDomainConversion((ObjectType) domainNodeType,
					typeFactory.domainType(1)));
			domainNodeType = domainNode.getConvertedType();
		} else if (!(domainNodeType instanceof DomainType))
			throw error("Domain expression in $for/$parfor does not have $domain type", domainNode);
		domainType = (DomainType) domainNodeType;
		if (!domainType.hasDimension())
			throw error("Use of incomplete domain type in $for/$parfor", domainNode);
		domainDimension = domainType.getDimension();
		if (domainDimension != numVars)
			throw error("Dimension of domain (" + domainDimension + ") " + "does not equal number of loop variables ("
					+ numVars + ")", domainNode);
		processStatement(node.getBody());
		if (node.loopContracts() != null)
			acslAnalyzer.processLoopContractNodes(node.loopContracts());
	}

	// ************************* Exported Methods **************************

	void processStatement(StatementNode statement) throws SyntaxException {
		StatementKind kind = statement.statementKind();

		switch (kind) {
		case COMPOUND:
			processCompoundStatement((CompoundStatementNode) statement);
			break;
		case EXPRESSION:
			processExpression(((ExpressionStatementNode) statement).getExpression());
			break;
		case IF:
			processIf((IfNode) statement);
			break;
		case JUMP:
			processJump((JumpNode) statement);
			break;
		case LABELED:
			processLabeledStatement((LabeledStatementNode) statement);
			break;
		case LOOP:
			processLoop((LoopNode) statement);
			break;
		case SWITCH:
			processExpression(((SwitchNode) statement).getCondition());
			processStatement(((SwitchNode) statement).getBody());
			break;
		case PRAGMA:
			entityAnalyzer.processPragma((PragmaNode) statement);
			break;
		case RUN:
			processRunStatement((RunNode) statement);
			break;
		case OMP:
			processOmpNode((OmpNode) statement);
			break;
		case NULL:
			break;
		case WHEN: {
			ExpressionNode guard = ((WhenNode) statement).getGuard();
			Type guardType;

			processExpression(guard);
			if (!guard.isSideEffectFree(false))
				throw this.error("the guard of a $when statement is not allowed to have side effects.", guard);
			guardType = guard.getConvertedType();
			// check guardType can be converted to a boolean...
			if (!guardType.isScalar())
				throw error("Guard has non-scalar type " + guardType, guard);
			processStatement(((WhenNode) statement).getBody());
			break;
		}
		case CHOOSE: {
			ChooseStatementNode chooseStatement = (ChooseStatementNode) statement;

			for (StatementNode child : chooseStatement)
				processStatement(child);
			break;
		}
		case ATOMIC:
			processStatement(((AtomicNode) statement).getBody());
			break;
		case CIVL_FOR:
			processCivlFor((CivlForNode) statement);
			break;
		default:
			throw error("Unknown kind of statement", statement);
		}
	}

	private void processOmpNode(OmpNode ompNode) throws SyntaxException {
		OmpNodeKind ompKind = ompNode.ompNodeKind();

		switch (ompKind) {
		case EXECUTABLE:
			processOmpExecutableNode((OmpExecutableNode) ompNode);
			break;
		case DECLARATIVE:
		default:

		}
	}

	/**
	 * Process the body statements of a $run statement. Apply checking for no return
	 * node is allowed inside the body of the $run statement.
	 * 
	 * @param runNode The {@link RunNode}
	 * @throws SyntaxException
	 */
	private void processRunStatement(RunNode runNode) throws SyntaxException {
		processStatement(runNode.getStatement());

		StatementNode body = runNode.getStatement();
		ASTNode next = body;
		Stack<ASTNode> children = new Stack<ASTNode>();

		children.push(next);
		while (!children.isEmpty()) {
			ASTNode item = children.pop();

			if (item != null) {
				if (item.nodeKind() == NodeKind.STATEMENT)
					if (((StatementNode) item).statementKind() == StatementKind.JUMP)
						if (((JumpNode) item).getKind() == JumpKind.RETURN)
							throw error("No return statement is allowed to be inside a $run statement block.", item);
				for (ASTNode child : item.children())
					children.push(child);
			}
		}
	}

	private void processOmpExecutableNode(OmpExecutableNode statement) throws SyntaxException {
		OmpExecutableKind kind = statement.ompExecutableKind();
		SequenceNode<OmpReductionNode> reductionList = (SequenceNode<OmpReductionNode>) statement.reductionList();

		for (int i = 0; i <= 5; i++) {
			@SuppressWarnings("unchecked")
			SequenceNode<ExpressionNode> list = (SequenceNode<ExpressionNode>) statement.child(i);

			if (list != null) {
				int count = list.numChildren();

				for (int j = 0; j < count; j++) {
					this.expressionAnalyzer.processExpression(list.getSequenceChild(j));
				}
			}
		}
		if (reductionList != null) {
			int count = reductionList.numChildren();

			for (int j = 0; j < count; j++) {
				this.processOmpReductionNode(reductionList.getSequenceChild(j));
			}
		}
		switch (kind) {
		case PARALLEL:
			OmpParallelNode parallel = (OmpParallelNode) statement;

			if (parallel.ifClause() != null)
				processExpression(parallel.ifClause());
			if (parallel.numThreads() != null)
				processExpression(parallel.numThreads());
			break;
		case WORKSHARING:
			OmpWorksharingNode workshare = (OmpWorksharingNode) statement;

			switch (workshare.ompWorkshareNodeKind()) {
			case FOR:
				OmpForNode forNode = (OmpForNode) statement;
				SequenceNode<FunctionCallNode> assertions = forNode.assertions();
				FunctionCallNode invariant = forNode.invariant();
				ExpressionNode chunkSize = forNode.chunkSize();

				if (assertions != null) {
					for (FunctionCallNode node : assertions)
						processExpression(node);
				}
				if (invariant != null)
					processExpression(invariant);
				if (chunkSize != null)
					processExpression(chunkSize);
				break;
			default:
			}
			break;
		case SIMD:
			// TODO: there are more of SIMD clauses under construction
		case SYNCHRONIZATION:
			break;
		default:
			throw new ABCRuntimeException("unknown OpenMP executable node kind to StatementAnalyzer: " + kind
					+ "\nStatement: " + statement.prettyRepresentation());
		}
		if (statement.statementNode() != null)
			processStatement(statement.statementNode());
	}

	private void processOmpReductionNode(OmpReductionNode reduction) throws SyntaxException {
		OmpReductionNodeKind kind = reduction.ompReductionOperatorNodeKind();
		SequenceNode<IdentifierExpressionNode> list = reduction.variables();
		int count = list.numChildren();

		if (kind == OmpReductionNodeKind.FUNCTION) {
			this.expressionAnalyzer.processExpression(((OmpFunctionReductionNode) reduction).function());
		}
		for (int i = 0; i < count; i++) {
			this.expressionAnalyzer.processExpression(list.getSequenceChild(i));
		}
	}

	/**
	 * <ul>
	 * <li>StatementNode</li> (includes PragmaNode)
	 * <li>StructureOrUnionTypeNode</li>
	 * <li>EnumerationTypeNode</li>
	 * <li>StaticAssertionNode</li>
	 * <li>VariableDeclarationNode</li>
	 * <li>FunctionDeclarationNode</li> (but not a FunctionDefinitionNode)
	 * <li>TypedefDeclarationNode</li>
	 * </ul>
	 * 
	 * @param node
	 * @throws SyntaxException
	 */
	void processCompoundStatement(CompoundStatementNode node) throws SyntaxException {
		for (BlockItemNode item : node) {
			if (item == null)
				continue;
			if (item instanceof StatementNode)
				processStatement((StatementNode) item);
			else if (item instanceof StructureOrUnionTypeNode)
				entityAnalyzer.typeAnalyzer.processStructureOrUnionType((StructureOrUnionTypeNode) item);
			else if (item instanceof EnumerationTypeNode)
				entityAnalyzer.typeAnalyzer.processEnumerationType((EnumerationTypeNode) item);
			else if (item instanceof StaticAssertionNode)
				entityAnalyzer.processStaticAssertion((StaticAssertionNode) item);
			else if (item instanceof VariableDeclarationNode)
				entityAnalyzer.declarationAnalyzer.processVariableDeclaration((VariableDeclarationNode) item);
			else if (item instanceof FunctionDeclarationNode)
				entityAnalyzer.declarationAnalyzer.processFunctionDeclaration((FunctionDeclarationNode) item);
			else if (item instanceof TypedefDeclarationNode)
				entityAnalyzer.declarationAnalyzer.processTypedefDeclaration((TypedefDeclarationNode) item);
			else
				throw error("Unknown kind of block item", item);
		}
	}
}
