package dev.civl.abc.analysis.common;

import java.util.Iterator;

import dev.civl.abc.analysis.IF.Analyzer;
import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.entity.IF.EntityFactory;
import dev.civl.abc.ast.entity.IF.Scope;
import dev.civl.abc.ast.entity.IF.Scope.ScopeKind;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.acsl.ContractNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ArrayLambdaNode;
import dev.civl.abc.ast.node.IF.expression.LambdaNode;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.label.OrdinaryLabelNode;
import dev.civl.abc.ast.node.IF.statement.CivlForNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.IfNode;
import dev.civl.abc.ast.node.IF.statement.LoopNode;
import dev.civl.abc.ast.node.IF.statement.SwitchNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.token.IF.SyntaxException;

/**
 * Given an AST, determines and sets scope of every node.
 * 
 * Scope structure of a function definition:
 * 
 * <pre>
 * { ...current parent scope...
 *   declNode - formals (incl. return type, identifier)
 *   { function scope:
 *     labels
 *     { block scope: formals
 *       { block scope: contract if contract!=null }
 *       body (will be block scope because is compound stmt)
 *     }
 *   }
 *   ...current parent scope...
 * }
 * </pre>
 * 
 * 
 * Scope structure for function declaration:
 * 
 * <pre>
 * { ...current parent scope...
 *   decl - formals (including return type, identifier)
 *   { function prototype scope: formals
 *     { block scope: contract }
 *   }
 *   ...current parent scope...
 * }
 * </pre>
 * 
 * Scope structure of a parameterized scope decl node used any place other than
 * the situations above (i.e., typedefs):
 * 
 * <pre>
 * { ...current parent scope...
 *   decl - scopeList - baseDecl
 *   identifier
 *   { block scope: 
 *     scopeList
 *     baseDecl - identifier
 *   }
 *   ...current parent scope...
 * }
 * </pre>
 * 
 * 
 * @author siegel
 * 
 */
public class ScopeAnalyzer implements Analyzer {

	public static void setScopes(AST ast, EntityFactory scopeFactory) throws SyntaxException {
		(new ScopeAnalyzer(scopeFactory)).analyze(ast);
	}

	private EntityFactory scopeFactory;

	private int currentId = 0;

	public ScopeAnalyzer(EntityFactory scopeFactory) {
		this.scopeFactory = scopeFactory;
	}

	private void processFunctionDefinitionNode(FunctionDefinitionNode funcNode, Scope parentScope)
			throws SyntaxException {
		// children: identifier, type, contract (optional), body
		String name = funcNode.getName();
		FunctionTypeNode typeNode = (FunctionTypeNode) funcNode.getTypeNode();
		SequenceNode<ContractNode> contract = funcNode.getContract();
		CompoundStatementNode body = funcNode.getBody();
		SequenceNode<VariableDeclarationNode> paramsNode = typeNode.getParameters();
		Scope newFunctionScope = scopeFactory.newScope(ScopeKind.FUNCTION, parentScope, funcNode);
		Scope parameterScope = scopeFactory.newScope(ScopeKind.BLOCK, newFunctionScope, paramsNode);

		if (name == null)
			throw new SyntaxException("Encountered a function definition with no name", funcNode.getSource());
		if (!parentScope.addFunctionName(name)) {
			throw new SyntaxException("Second definition of function " + name + " in same scope", funcNode.getSource());
		}
		if (paramsNode != null)
			processRecursive(paramsNode, parameterScope, null);
		if (contract != null) {
			Scope contractScope = scopeFactory.newScope(ScopeKind.CONTRACT, parameterScope, contract);

			processRecursive(contract, contractScope, null);
		}
		processNode(body, parameterScope, newFunctionScope);
		processRecursive(funcNode, parentScope, null);
	}

	private void processFunctionDeclarationNode(FunctionDeclarationNode funcNode, Scope parentScope)
			throws SyntaxException {
		// children: ident, type, contract.
		TypeNode typeNode = funcNode.getTypeNode();

		if (typeNode instanceof FunctionTypeNode) {
			FunctionTypeNode functionTypeNode = (FunctionTypeNode) typeNode;

			SequenceNode<ContractNode> contract = funcNode.getContract();
			SequenceNode<VariableDeclarationNode> paramsNode = functionTypeNode.getParameters();
			Scope parameterScope = scopeFactory.newScope(ScopeKind.BLOCK, parentScope, paramsNode);

			if (paramsNode != null)
				processRecursive(paramsNode, parameterScope, null);
			if (contract != null) {
				Scope contractScope = scopeFactory.newScope(ScopeKind.CONTRACT, parameterScope, contract);

				processRecursive(contract, contractScope, null);
			}
		}
		processRecursive(funcNode, parentScope, null);
	}

	/**
	 * Performs scope analysis on a node and all its decendants, but back tracks if
	 * it encounters a node that already has a non-null scope. I.e., if a node has a
	 * non-null scope, it and all of its descendants are left alone.
	 * 
	 * @param node          an AST node
	 * @param parentScope   the current scope we are in when the given node is
	 *                      reached; may be null if node is the root node, i.e., the
	 *                      first node in the AST
	 * @param functionScope the function scope for the current function we are "in"
	 *                      when we reach this node; this is used only for
	 *                      LabelNodes as these must go into the first containing
	 *                      function scope; may be null if the node and all its
	 *                      descendants could not possibly have a label
	 * @throws SyntaxException if AST is malformed in some way
	 */
	public void processNode(ASTNode node, Scope parentScope, Scope functionScope) throws SyntaxException {

		if (node.getScope() != null)
			return;
		if (parentScope == null) {
			parentScope = scopeFactory.newScope(ScopeKind.FILE, null, node);
		} else if (node instanceof QuantifiedExpressionNode || node instanceof ArrayLambdaNode
				|| node instanceof LambdaNode) {
			parentScope = scopeFactory.newScope(ScopeKind.BLOCK, parentScope, node);
		} else if (node instanceof FunctionDefinitionNode) {
			processFunctionDefinitionNode((FunctionDefinitionNode) node, parentScope);
			return;
		} else if (node instanceof CompoundStatementNode) {
			parentScope = scopeFactory.newScope(ScopeKind.BLOCK, parentScope, node);
		} else if (node instanceof SwitchNode) {
			ASTNode body = ((SwitchNode) node).getBody();
			Scope bodyScope;

			parentScope = scopeFactory.newScope(ScopeKind.BLOCK, parentScope, node);
			bodyScope = scopeFactory.newScope(ScopeKind.BLOCK, parentScope, body);
			processRecursive(body, bodyScope, functionScope);
		} else if (node instanceof IfNode) {
			ASTNode trueBranch = ((IfNode) node).getTrueBranch();
			ASTNode falseBranch = ((IfNode) node).getFalseBranch();
			Scope trueBranchScope;

			parentScope = scopeFactory.newScope(ScopeKind.BLOCK, parentScope, node);
			trueBranchScope = scopeFactory.newScope(ScopeKind.BLOCK, parentScope, trueBranch);
			processRecursive(trueBranch, trueBranchScope, functionScope);
			if (falseBranch != null) {
				Scope falseBranchScope = scopeFactory.newScope(ScopeKind.BLOCK, parentScope, falseBranch);

				processRecursive(falseBranch, falseBranchScope, functionScope);
			}
		} else if (node instanceof LoopNode) {
			LoopNode loopNode = (LoopNode) node;
			ASTNode body = loopNode.getBody();
			ASTNode loopContracts = loopNode.loopContracts();
			Scope bodyScope;

			parentScope = scopeFactory.newScope(ScopeKind.BLOCK, parentScope, node);
			bodyScope = scopeFactory.newScope(ScopeKind.BLOCK, parentScope, body);

			Iterable<ASTNode> children = loopNode.children();

			loopNode.setScope(parentScope);
			for (ASTNode child : children)
				if (child != null && child != body && child != loopContracts)
					processRecursive(child, parentScope, functionScope);

			// Process body and contracts (if exists) specially:
			processRecursive(body, bodyScope, functionScope);
			if (loopContracts != null) {
				Scope conditionScope = scopeFactory.newScope(ScopeKind.CONTRACT, loopNode.getCondition().getScope(),
						loopContracts);

				processRecursive(loopContracts, conditionScope, functionScope);
			}
			return;
		} else if (node instanceof CivlForNode) {
			ASTNode body = ((CivlForNode) node).getBody();
			Scope bodyScope;

			parentScope = scopeFactory.newScope(ScopeKind.BLOCK, parentScope, node);
			bodyScope = scopeFactory.newScope(ScopeKind.BLOCK, parentScope, body);
			processRecursive(body, bodyScope, functionScope);
		} else if (node instanceof FunctionDeclarationNode) {
			processFunctionDeclarationNode((FunctionDeclarationNode) node, parentScope);
			return;
		} else if (node instanceof FunctionTypeNode) {
			// a function type node may occur outside of a function
			// declaration/definition, e.g., as a type name...
			ASTNode parameters = ((FunctionTypeNode) node).getParameters();

			if (parameters != null && parameters.getScope() == null) {
				Scope prototypeScope = scopeFactory.newScope(ScopeKind.FUNCTION_PROTOTYPE, parentScope, parameters);

				processRecursive(parameters, prototypeScope, functionScope);
			}
		} else if (node instanceof OrdinaryLabelNode) {
			parentScope = functionScope;
		}
		processRecursive(node, parentScope, functionScope);
	}

	private void setIds(Scope scope) {
		if (scope.getId() >= 0) {
			return;
		} else {
			Iterator<Scope> children = scope.getChildrenScopes();

			scope.setId(currentId);
			currentId++;
			while (children.hasNext())
				setIds(children.next());
		}
	}

	/**
	 * Assigns the given scope to the given node and then invokes method
	 * {@link #processNode} to all the children.
	 * 
	 * @param node          an ASTNode which does not yet have a Scope
	 * @param scope         the scope that will be assigned to the given node
	 * @param functionScope the function scope we are currently in
	 * @throws SyntaxException if problem in AST
	 */
	private void processRecursive(ASTNode node, Scope scope, Scope functionScope) throws SyntaxException {
		Iterable<ASTNode> children = node.children();

		assert scope != null;
		node.setScope(scope);
		for (ASTNode child : children) {
			if (child != null)
				processNode(child, scope, functionScope);
		}
	}

	private void clearNode(ASTNode node) {
		if (node != null) {
			Iterable<ASTNode> children = node.children();

			node.setScope(null);
			for (ASTNode child : children)
				clearNode(child);
		}
	}

	// Exported methods...

	@Override
	public void clear(AST unit) {
		clearNode(unit.getRootNode());
	}

	@Override
	public void analyze(AST unit) throws SyntaxException {
		ASTNode root = unit.getRootNode();

		processNode(root, null, null);
		setIds(root.getScope());
	}

}
