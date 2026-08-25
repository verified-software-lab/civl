package dev.civl.mc.transform.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.entity.IF.Variable;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.NodePredicate;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.declaration.DeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.CastNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.ExpressionStatementNode;
import dev.civl.abc.ast.node.IF.statement.ReturnNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.PointerTypeNode;
import dev.civl.abc.ast.node.IF.type.StructureOrUnionTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode.TypeNodeKind;
import dev.civl.abc.ast.node.IF.type.TypedefNameNode;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.front.IF.CivlcTokenConstant;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.transform.IF.Pthread2CIVLTransformer;

//TODO: add arguments to pthread_exit();

/**
 * TODO list all the transformation (e.g., pthread_create, pthread_exit to
 * _pthread_exit) with explanation of how it works and why it is necessary
 * 
 * @author zmanchun
 *
 */
public class Pthread2CIVLWorker extends BaseWorker {

	// private final static String PTHREAD_MUTEX_LOCK="pthread_mutex_lock";

	final static String PTHREAD_MUTEX_TYPE = "pthread_mutex_t";

	private final static String PTHREAD_MUTEX_ATTR_TYPE = "pthread_mutexattr_t";

	final static String PTHREAD_MUTEX_INITIALIZER = "PTHREAD_MUTEX_INITIALIZER";

	private final static String PTHREAD_POOL_TYPE = "$pthread_pool_t";

	private final static String MAIN = "main";

	private final static String GENERATED_MAIN = "_gen_main";

	private boolean fixedMain = false;

	static final String PTHREAD_MUTEX_LOCK = "pthread_mutex_lock";

	static final String PTHREAD_MUTEX_LOCK_NEW = "$pthread_mutex_lock";

	static final String PTHREAD_COND_WAIT = "pthread_cond_wait";

	static final String PTHREAD_COND_WAIT_NEW = "$pthread_cond_wait";

	final static String PTHREAD_POOL_CREATE = "$pthread_pool_create";

	final static String PTHREAD_POOL_CREATE_MAIN = "$pthread_pool_create_main";

	final static String PTHREAD_GPOOL = "$pthread_gpool";

	private final static String PTHREAD_POOL = "_pthread_pool";

	// needs to go to MPI process scope
	final static String PTHREAD_CREATE = "pthread_create";

	private final static String PTHREAD_EXIT = "pthread_exit";

	final static String PTHREAD_EXIT_NEW = "$pthread_exit";

	private final static String PTHREAD_SELF = "pthread_self";

	final static String PTHREAD_SELF_NEW = "$pthread_self";

	// needs to go to MPI process scope
	final static String PTHREAD_EXIT_MAIN_NEW = "$pthread_exit_main";

	static final String PTHREAD_HEADER = "pthread.h";

	/* **************************** Instance Fields ************************* */

	private boolean exitMainDone = false;

	private String originalMain = MAIN;

	private BlockItemNode firstThreadFunctionNode = null;

	private BlockItemNode firstMutexVarNode = null;

	private Set<String> threadFunctionNames = new LinkedHashSet<>();

	private Set<FunctionDefinitionNode> nonThreadFunctionsWtSyncCalls = new HashSet<>();

	private Set<String> nonThreadFunctionNamesWtSyncCalls = new HashSet<>();

	private Set<String> newNonThreadFunctionNamesWtSyncCalls = new HashSet<>();

	private Set<String> currentNewNonThreadFunctionNamesWtSyncCalls = new HashSet<>();

	private List<String> syncCallFunctionNames = new ArrayList<>();

	private Set<VariableDeclarationNode> thread_local_variable_declarations = new LinkedHashSet<>();

	private Set<String> newNonThreadFunctionNamesWtThreadLocal = new HashSet<>();

	private Set<String> currentNewNonThreadFunctionNamesWtThreadLocal = new HashSet<>();

	private Set<String> nonThreadFunctionNamesWtThreadLocal = new HashSet<>();

	private Set<FunctionDefinitionNode> nonThreadFunctionsWtThreadLocal = new HashSet<>();

	private Set<VariableDeclarationNode> shared_variables = new HashSet<>();

	/* ****************************** Constructor ************************** */
	/**
	 * Creates a new instance of MPITransformer.
	 * 
	 * @param astFactory The ASTFactory that will be used to create new nodes.
	 */
	public Pthread2CIVLWorker(ASTFactory astFactory) {
		super(Pthread2CIVLTransformer.LONG_NAME, astFactory);
		this.identifierPrefix = "$pthreads_";
		// threadFunctionNames.add(e)
		newNonThreadFunctionNamesWtSyncCalls.add(PTHREAD_COND_WAIT);
		newNonThreadFunctionNamesWtSyncCalls.add(PTHREAD_MUTEX_LOCK);
		newNonThreadFunctionNamesWtSyncCalls.add(PTHREAD_SELF);
		nonThreadFunctionNamesWtSyncCalls.add(PTHREAD_COND_WAIT);
		nonThreadFunctionNamesWtSyncCalls.add(PTHREAD_MUTEX_LOCK);
		nonThreadFunctionNamesWtSyncCalls.add(PTHREAD_SELF);

	}

	/* *************************** Private Methods ************************* */

	private VariableDeclarationNode pthread_pool_declaration(boolean wtInitializer, boolean isMain) {
		TypeNode pthreadPoolType;
		List<ExpressionNode> pthreadPoolCreateArgs;
		ExpressionNode pthreadPoolCreate;
		String pthread_pool_create_function = PTHREAD_POOL_CREATE;

		if (isMain)
			pthread_pool_create_function = PTHREAD_POOL_CREATE_MAIN;
		pthreadPoolType = nodeFactory.newTypedefNameNode(nodeFactory.newIdentifierNode(
				this.newSource("$phtread_pool_t type", CivlcTokenConstant.IDENTIFIER), PTHREAD_POOL_TYPE));
		if (wtInitializer) {
			pthreadPoolCreateArgs = new ArrayList<>(2);
			pthreadPoolCreateArgs.add(this.hereNode());
			pthreadPoolCreateArgs.add(this.identifierExpression(PTHREAD_GPOOL));
			pthreadPoolCreate = nodeFactory.newFunctionCallNode(
					this.newSource("function call " + pthread_pool_create_function, CivlcTokenConstant.CALL),
					this.identifierExpression(pthread_pool_create_function), pthreadPoolCreateArgs);
			return this.variableDeclaration(PTHREAD_POOL, pthreadPoolType, pthreadPoolCreate);
		} else
			return this.variableDeclaration(PTHREAD_POOL, pthreadPoolType);
	}

	/**
	 * TODO javadocs
	 * 
	 * @param root
	 * @throws SyntaxException
	 */
	private void transformWorker(SequenceNode<BlockItemNode> root) throws SyntaxException {
		// getThreadFunctions(root);
		for (String threadFunction : this.threadFunctionNames) {
			this.newNonThreadFunctionNamesWtThreadLocal.remove(threadFunction);
			this.nonThreadFunctionNamesWtThreadLocal.remove(threadFunction);
		}
		for (BlockItemNode node : root) {
			if (node == null)
				continue;
			if (node instanceof FunctionDefinitionNode) {
				process_shared_variable_access((FunctionDefinitionNode) node);
				process_thread_functions((FunctionDefinitionNode) node);
				process_pthread_exits((FunctionDefinitionNode) node);
			}
		}
		while (!this.newNonThreadFunctionNamesWtSyncCalls.isEmpty()
				|| !this.newNonThreadFunctionNamesWtThreadLocal.isEmpty()) {
			// currentNewNonThreadFunctionsWtSyncCalls = new HashSet<>(
			// newNonThreadFunctionsWtSyncCalls);
			currentNewNonThreadFunctionNamesWtSyncCalls = new HashSet<>(newNonThreadFunctionNamesWtSyncCalls);
			// newNonThreadFunctionsWtSyncCalls.clear();
			newNonThreadFunctionNamesWtSyncCalls.clear();

			this.currentNewNonThreadFunctionNamesWtThreadLocal = new HashSet<>(
					this.newNonThreadFunctionNamesWtThreadLocal);
			this.newNonThreadFunctionNamesWtThreadLocal.clear();
			for (ASTNode node : root.children()) {
				if (node == null)
					continue;
				if (node instanceof FunctionDefinitionNode) {
					process_pthread_sync_calls_thread_locals((FunctionDefinitionNode) node);
				}
			}
		}
		process_nonThread_functions_wt_syncCalls();
		process_nonThread_functions_wt_thread_locals();
		if (this.syncCallFunctionNames.size() > 0)
			process_function_call_of_functionsWtSyncCalls(root);
	}

	private void process_shared_variable_access(ASTNode node) {
		if (node instanceof ExpressionStatementNode) {
			this.transformAccess2SharedVariables((ExpressionStatementNode) node);
		} else {
			for (ASTNode child : node.children()) {
				if (child != null)
					process_shared_variable_access(child);
			}
		}
	}

	private void process_function_call_of_functionsWtSyncCalls(ASTNode root) {
		for (ASTNode node : root.children()) {
			if (node == null)
				continue;
			if (node instanceof FunctionCallNode) {
				FunctionCallNode call = (FunctionCallNode) node;
				ExpressionNode function = call.getFunction();

				if (function instanceof IdentifierExpressionNode) {
					String funcName = ((IdentifierExpressionNode) function).getIdentifier().name();

					if (this.syncCallFunctionNames.contains(funcName)) {
						call.getArguments().addSequenceChild(this.identifierExpression(PTHREAD_POOL));
					}
				}
			}
			process_function_call_of_functionsWtSyncCalls(node);
		}
	}

	private void process_nonThread_functions_wt_syncCalls() {
		Iterator<FunctionDefinitionNode> iterator = this.nonThreadFunctionsWtSyncCalls.iterator();

		while (iterator.hasNext()) {
			FunctionDefinitionNode function = iterator.next();
			// int numDeclarations = function.getEntity().getNumDeclarations();

			for (DeclarationNode declaration : function.getEntity().getDeclarations()) {
				FunctionDeclarationNode functionDecl = (FunctionDeclarationNode) declaration;
				TypeNode type = functionDecl.getTypeNode();
				VariableDeclarationNode pthread_pool_param = this.pthread_pool_declaration(false, false);
				FunctionTypeNode funcType;

				if (type.kind() == TypeNodeKind.TYPEDEF_NAME) {
					TypedefNameNode typedef = (TypedefNameNode) type;
					Function funcEntity = (Function) typedef.getName().getEntity();

					funcType = funcEntity.getDefinition().getTypeNode();
				} else
					funcType = (FunctionTypeNode) type;

				if (hasVoidParameter(funcType))
					funcType.getParameters().setSequenceChild(0, pthread_pool_param);
				else
					funcType.getParameters().addSequenceChild(pthread_pool_param);

			}
			// function.getTypeNode()
			// .getParameters()
			// .addSequenceChild(
			// this.variableDeclaration(PTHREAD_POOL, nodeFactory
			// .newTypedefNameNode(nodeFactory
			// .newIdentifierNode(this.newSource(
			// "$phtread_pool_t type",
			// CivlcTokenConstant.IDENTIFIER),
			// PTHREAD_POOL_TYPE), null)));
			// FunctionTypeNode funcType = function.getTypeNode();
			// VariableDeclarationNode pthread_pool_param = this
			// .pthread_pool_declaration(false);
			//
			// if (hasVoidParameter(funcType))
			// funcType.getParameters()
			// .setSequenceChild(0, pthread_pool_param);
			// else
			// funcType.getParameters().addSequenceChild(pthread_pool_param);
		}
	}

	private boolean hasVoidParameter(FunctionTypeNode type) {
		if (type.getParameters().numChildren() == 1) {
			TypeNode parameterType = type.getParameters().getSequenceChild(0).getTypeNode();

			if (parameterType.kind() == TypeNodeKind.VOID) {
				return true;
			}
		}
		return false;
	}

	private void process_nonThread_functions_wt_thread_locals() {
		Iterator<FunctionDefinitionNode> iterator = this.nonThreadFunctionsWtThreadLocal.iterator();

		while (iterator.hasNext()) {
			FunctionDefinitionNode function = iterator.next();
			// function.getTypeNode()
			// .getParameters()
			// .addSequenceChild(
			// this.variableDeclaration(PTHREAD_POOL, nodeFactory
			// .newTypedefNameNode(nodeFactory
			// .newIdentifierNode(this.newSource(
			// "$phtread_pool_t type",
			// CivlcTokenConstant.IDENTIFIER),
			// PTHREAD_POOL_TYPE), null)));
			FunctionTypeNode funcType = function.getTypeNode();

			if (this.threadFunctionNames.contains(function.getName()))
				continue;
			for (VariableDeclarationNode threadLocalVar : this.thread_local_variable_declarations) {
				VariableDeclarationNode newParameter = threadLocalVar.copy();

				newParameter.setThreadLocalStorage(false);
				if (newParameter.getInitializer() != null)
					newParameter.setInitializer(null);
				funcType.getParameters().addSequenceChild(newParameter);
			}
		}
	}

	/**
	 * modify the function definition to take an extra argument: $pthread_pool_t
	 * pthread_pool
	 * 
	 * @param funcDef
	 */
	@SuppressWarnings("unused")
	private void process_sync_call_thread_local_function(FunctionDefinitionNode funcDef) {
		FunctionTypeNode funcType = funcDef.getTypeNode();
		VariableDeclarationNode pthread_pool_param = this.pthread_pool_declaration(false, false);

		funcType.getParameters().addSequenceChild(pthread_pool_param);
		// if (!this.funcList.contains(funcDef.getName()))
		// syncCallFunctionNames.add(funcDef.getName());
	}

	private void process_pthread_sync_calls_thread_locals(FunctionDefinitionNode node) {
		process_pthread_sync_calls_thread_locals(node, node);
	}

	private void process_pthread_sync_calls_thread_locals(FunctionDefinitionNode funcDef, ASTNode node) {
		for (ASTNode child : node.children()) {
			if (child == null)
				continue;
			if (child instanceof FunctionCallNode) {
				process_pthread_sync_call_and_thread_local_work(funcDef, (FunctionCallNode) child);
			}
			process_pthread_sync_calls_thread_locals(funcDef, child);
		}
	}

	/**
	 * transforms pthread_mutex_lock(mutex) to pthread_mutex_lock(mutex,
	 * _pthread_pool) and similar for pthread_cond_wait();
	 * 
	 * @param node
	 */
	private void process_pthread_sync_call_and_thread_local_work(FunctionDefinitionNode funcDef,
			FunctionCallNode node) {
		ExpressionNode function = node.getFunction();
		boolean hasSyncCall = false, hasThreadLocalCall = false;

		if (function instanceof IdentifierExpressionNode) {
			String calleeName = ((IdentifierExpressionNode) function).getIdentifier().name();

			if (this.currentNewNonThreadFunctionNamesWtSyncCalls.contains(calleeName)) {
				hasSyncCall = true;
			} else if (this.currentNewNonThreadFunctionNamesWtThreadLocal.contains(calleeName)) {
				hasThreadLocalCall = true;
			}
			if (calleeName.equals(PTHREAD_MUTEX_LOCK) || calleeName.equals(PTHREAD_COND_WAIT)
					|| calleeName.equals(PTHREAD_SELF)) {
				((IdentifierExpressionNode) function).getIdentifier().setName("$" + calleeName);
			}
			if (hasSyncCall) {
				String funcName = funcDef.getName();

				node.getArguments().addSequenceChild(this.identifierExpression(PTHREAD_POOL));
				if (!this.threadFunctionNames.contains(funcName)
						&& !this.nonThreadFunctionNamesWtSyncCalls.contains(funcName)) {
					nonThreadFunctionsWtSyncCalls.add(funcDef);
					nonThreadFunctionNamesWtSyncCalls.add(funcName);
					// this.newNonThreadFunctionsWtSyncCalls.add(funcDef);
					this.newNonThreadFunctionNamesWtSyncCalls.add(funcName);
				}
			}
			if (hasThreadLocalCall) {
				String funcName = funcDef.getName();

				for (VariableDeclarationNode threadLocalVar : this.thread_local_variable_declarations) {
					node.getArguments().addSequenceChild(this.identifierExpression(threadLocalVar.getName()));
				}
				if (!this.threadFunctionNames.contains(funcName)
						&& !this.nonThreadFunctionNamesWtThreadLocal.contains(funcName)) {
					this.nonThreadFunctionNamesWtThreadLocal.add(funcName);
					this.newNonThreadFunctionNamesWtThreadLocal.add(funcName);
					this.nonThreadFunctionsWtThreadLocal.add(funcDef);
				}
			}
		}
	}

	/**
	 * Insert pthread pool creation at the beginning of pthread functions
	 * 
	 * @param node
	 */
	private void process_thread_functions(FunctionDefinitionNode node) {
		String name = node.getName();

		if (this.threadFunctionNames.contains(name) || name.equals(this.originalMain)) {
			CompoundStatementNode body = node.getBody();
			List<BlockItemNode> newBodyNodes = new LinkedList<>();
			VariableDeclarationNode pthreadPoolVar = this.pthread_pool_declaration(true,
					name.equals(this.originalMain));

			body.remove();
			newBodyNodes.add(pthreadPoolVar);
			for (VariableDeclarationNode threadLocalVar : this.thread_local_variable_declarations) {
				newBodyNodes.add(threadLocalVar.copy());
			}
			newBodyNodes.add(body);
			node.setBody(this.nodeFactory.newCompoundStatementNode(body.getSource(), newBodyNodes));
		}
	}

	private void transformAccess2SharedVariables(ExpressionStatementNode expressionStatement) {
		int index = expressionStatement.childIndex();
		ASTNode parent = expressionStatement.parent();

		ExpressionNode expression = expressionStatement.getExpression();

		if (expression instanceof OperatorNode) {
			OperatorNode operatorNode = (OperatorNode) expression;

			if (operatorNode.getOperator() == Operator.ASSIGN) {
				ExpressionNode rhs = operatorNode.getArgument(1);
				ExpressionNode lhs = operatorNode.getArgument(0);
				List<BlockItemNode> newItems = null;// =
													// transform_access_to_shared_varaibles(rhs);

				if (!(lhs instanceof IdentifierExpressionNode)) {
					newItems = breakAtomicity(lhs, rhs);
				}
				// List<BlockItemNode> newItems =
				// transform_access_to_shared_varaibles(rhs);
				//
				// if (newItems.size() <= 0) {
				// ExpressionNode lhs = operatorNode.getArgument(0);
				//
				// if (!(lhs instanceof IdentifierExpressionNode)) {
				// newItems = breakAtomicity(lhs, rhs);
				// }
				// }
				if (newItems != null && newItems.size() > 0) {
					expressionStatement.remove();
					newItems.add(expressionStatement);
					parent.setChild(index,
							this.nodeFactory.newCompoundStatementNode(expressionStatement.getSource(), newItems));
				}
			}
		}
	}

	private List<BlockItemNode> breakAtomicity(ExpressionNode lhs, ExpressionNode rhs) {
		List<BlockItemNode> result = new LinkedList<>();

		if (rhs.equiv(lhs)) {
			String tmpName = this.newUniqueIdentifier("tmp");
			VariableDeclarationNode tmpVariable = this.variableDeclaration(tmpName, this.typeNode(rhs.getType()),
					rhs.copy());

			rhs.parent().setChild(rhs.childIndex(), this.identifierExpression(tmpName));
			result.add(tmpVariable);
		} else {
			for (ASTNode child : rhs.children()) {
				if (child != null && (child instanceof ExpressionNode)) {
					List<BlockItemNode> subResult = breakAtomicity(lhs, (ExpressionNode) child);

					result.addAll(subResult);
				}
			}
		}
		return result;
	}

	/**
	 * TODO javadocs
	 * 
	 * @param function
	 * @param threadList
	 * @throws SyntaxException
	 */
	private void process_pthread_exits(FunctionDefinitionNode function) throws SyntaxException {
		String name = function.getName();
		TypeNode returnType = function.getTypeNode().getReturnType();
		boolean isMain = false;

		if (name.equals(GENERATED_MAIN) || (!this.fixedMain && name.equals(MAIN))) {
			isMain = true;
			this.fixedMain = true;
		}
		if (isMain) {
			ExpressionNode ZERO = this.integerConstant(0);
			if (!hasReturn(function)) {
				// this.identifierExpression(this
				// .newSource(PTHREAD_POOL,
				// CivlcTokenConstant.IDENTIFIER),
				// PTHREAD_POOL)
				if (returnType.getType().kind() == TypeKind.VOID)
					function.getBody().addSequenceChild(nodeFactory
							.newReturnNode(this.newSource("return statement", CivlcTokenConstant.RETURN), null));
				else
					function.getBody().addSequenceChild(nodeFactory
							.newReturnNode(this.newSource("return statement", CivlcTokenConstant.RETURN), ZERO));
			}
			process_pthread_exit(function, true);
			// return;
		}
		if ((this.isVoidOrVoidPointerType(returnType)) && this.threadFunctionNames.contains(name)) {
			String pthread_exit_name = isMain ? PTHREAD_EXIT_MAIN_NEW : PTHREAD_EXIT_NEW;

			if (!isMain && function.getTypeNode().getParameters().numChildren() == 0) {
				function.getTypeNode()
						.setParameters(nodeFactory.newSequenceNode(
								this.newSource("parameter declaration of " + function.getName(),
										CivlcTokenConstant.DECLARATION_LIST),
								"parameters",
								Arrays.asList(this.variableDeclaration("arg", nodeFactory.newPointerTypeNode(
										this.newSource("type void *", CivlcTokenConstant.TYPE), this.voidType())))));
			}
			ExpressionNode nullNode = nodeFactory.newCastNode(
					this.newSource("cast expression", CivlcTokenConstant.CAST),
					nodeFactory.newPointerTypeNode(this.newSource("type void *", CivlcTokenConstant.TYPE),
							this.voidType()),
					this.integerConstant(0));
			FunctionCallNode newPthreadExit = nodeFactory.newFunctionCallNode(
					this.newSource("function call " + pthread_exit_name, CivlcTokenConstant.CALL),
					this.identifierExpression(pthread_exit_name), Arrays.asList(nullNode, this.identifierExpression(
							this.newSource(PTHREAD_POOL, CivlcTokenConstant.IDENTIFIER), PTHREAD_POOL)));
			StatementNode pthreadExit = nodeFactory.newExpressionStatementNode(newPthreadExit);
			function.getBody().addSequenceChild(pthreadExit);
			process_pthread_exit(function, isMain);
			fix_duplicated_pthread_exits(function, isMain);
		}
	}

	private void fix_duplicated_pthread_exits(FunctionDefinitionNode function, boolean isMain) {
		// function.prettyPrint(System.out);
		this.reduceDuplicateNode(function, new NodePredicate() {
			@Override
			public boolean holds(ASTNode node) {
				return isFunctionCallStatementNodeOf(node, PTHREAD_EXIT_NEW);
			}

		});

		// CompoundStatementNode bodyNode = function.getBody();
		// BlockItemNode newBody = fix_duplicated_pthread_exits_worker(bodyNode,
		// isMain);
		//
		// if (newBody instanceof CompoundStatementNode)
		// function.setBody((CompoundStatementNode) newBody);
		// else
		// function.setBody(nodeFactory.newCompoundStatementNode(
		// bodyNode.getSource(), Arrays.asList(newBody)));
	}

	@SuppressWarnings("unused")
	private BlockItemNode fix_duplicated_pthread_exits_worker(CompoundStatementNode block, boolean isMain) {
		String pthread_exit_name = isMain ? PTHREAD_EXIT_MAIN_NEW : PTHREAD_EXIT_NEW;
		int lastIndex = -1;
		List<BlockItemNode> newItems = new LinkedList<>();

		for (int i = 0; i < block.numChildren(); i++) {
			BlockItemNode child = block.getSequenceChild(i);

			if (child == null)
				continue;
			if (child instanceof CompoundStatementNode) {
				BlockItemNode newChild = fix_duplicated_pthread_exits_worker((CompoundStatementNode) child, isMain);

				block.setChild(i, newChild);
				child = newChild;
			}
			if (isFunctionCallStatementNodeOf(child, pthread_exit_name)) {
				if (lastIndex >= 0)
					block.removeChild(i);
				else
					lastIndex = i;
			}
		}
		for (BlockItemNode item : block) {
			if (item == null)
				continue;
			item.remove();
			newItems.add(item);
		}
		if (newItems.size() > 1)
			return nodeFactory.newCompoundStatementNode(block.getSource(), newItems);
		else if (newItems.size() == 1)
			return newItems.get(0);
		else
			return null;
	}

	private boolean isFunctionCallStatementNodeOf(ASTNode node, String function) {
		if (node instanceof ExpressionStatementNode) {
			ExpressionNode expression = ((ExpressionStatementNode) node).getExpression();

			if (expression instanceof FunctionCallNode) {
				ExpressionNode functionNode = ((FunctionCallNode) expression).getFunction();

				if (functionNode instanceof IdentifierExpressionNode) {
					return ((IdentifierExpressionNode) functionNode).getIdentifier().name().equals(function);
				}
			}
		}
		return false;
	}

	/**
	 * In main(), translate pthread_exit(arg) to pthread_exit(arg, true); in other
	 * function, translate pthread_exit(arg) to pthread_exit(arg, false).
	 * 
	 * @param function
	 * @throws SyntaxException
	 */
	private void process_pthread_exit(FunctionDefinitionNode function, boolean isMain) throws SyntaxException {
		process_pthread_exit_worker(function, isMain);
	}

	/**
	 * TODO javadoc
	 * 
	 * @param node
	 * @param isMain
	 * @throws SyntaxException
	 */
	private void process_pthread_exit_worker(ASTNode node, boolean isMain) throws SyntaxException {
		for (ASTNode child : node.children()) {
			if (child == null)
				continue;
			if (child instanceof FunctionCallNode) {
				FunctionCallNode funcCall = (FunctionCallNode) child;
				ExpressionNode funcName = funcCall.getFunction();

				if (funcName instanceof IdentifierExpressionNode) {
					IdentifierExpressionNode name = (IdentifierExpressionNode) funcName;
					String nameString = name.getIdentifier().name();

					if (nameString.equals(PTHREAD_EXIT)) {
						// ExpressionNode isMainArg =
						// this.booleanConstant(isMain);
						ExpressionNode oldArg = funcCall.getArgument(0);
						SequenceNode<ExpressionNode> newArgs;

						if (!isMain) {
							name.getIdentifier().setName(PTHREAD_EXIT_NEW);
							oldArg.parent().removeChild(oldArg.childIndex());
							newArgs = nodeFactory.newSequenceNode(
									this.newSource(
											"actual parameter list of " + nameString, CivlcTokenConstant.ARGUMENT_LIST),
									"Actual parameters",
									Arrays.asList(oldArg,
											this.identifierExpression(
													this.newSource(PTHREAD_POOL, CivlcTokenConstant.IDENTIFIER),
													PTHREAD_POOL)));
							funcCall.setArguments(newArgs);
						} else if (!exitMainDone) {
							this.exitMainDone = true;
							name.getIdentifier().setName(PTHREAD_EXIT_MAIN_NEW);
							oldArg.parent().removeChild(oldArg.childIndex());
							newArgs = nodeFactory.newSequenceNode(
									this.newSource(
											"actual parameter list of " + nameString, CivlcTokenConstant.ARGUMENT_LIST),
									"Actual parameters",
									Arrays.asList(oldArg,
											this.identifierExpression(
													this.newSource(PTHREAD_POOL, CivlcTokenConstant.IDENTIFIER),
													PTHREAD_POOL)));
							funcCall.setArguments(newArgs);
						}
					}
				}
			} else if (child instanceof ReturnNode && (!isMain || !exitMainDone)) {
				// ExpressionNode isMainArg = this.booleanConstant(isMain);
				String pthread_exit_name = isMain ? PTHREAD_EXIT_MAIN_NEW : PTHREAD_EXIT_NEW;
				ExpressionNode returnExpr = ((ReturnNode) child).getExpression();
				ExpressionNode newExpr = returnExpr == null ? this.nullPointerNode() : returnExpr.copy();

				if (isMain)
					exitMainDone = true;
				FunctionCallNode newPthreadExit = nodeFactory.newFunctionCallNode(
						this.newSource("function call of " + pthread_exit_name, CivlcTokenConstant.CALL),
						this.identifierExpression(pthread_exit_name), Arrays.asList(newExpr, this.identifierExpression(
								this.newSource(PTHREAD_POOL, CivlcTokenConstant.IDENTIFIER), PTHREAD_POOL)));
				StatementNode pthreadExit = nodeFactory.newExpressionStatementNode(newPthreadExit);

				child.parent().setChild(child.childIndex(), pthreadExit);
			} else {
				process_pthread_exit_worker(child, isMain);
			}
		}
	}

	private ExpressionNode nullPointerNode() throws SyntaxException {
		return nodeFactory.newCastNode(this.newSource("cast expression", CivlcTokenConstant.CAST),
				nodeFactory.newPointerTypeNode(this.newSource("type void *", CivlcTokenConstant.TYPE), this.voidType()),
				this.integerConstant(0));
	}

	private void setOrignalMainFunction(SequenceNode<BlockItemNode> root) {
		for (BlockItemNode item : root) {
			if (item != null && item instanceof FunctionDeclarationNode) {
				FunctionDeclarationNode function = (FunctionDeclarationNode) item;
				String name = function.getName();

				if (name.equals(GENERATED_MAIN)) {
					this.originalMain = GENERATED_MAIN;
					return;
				}
			}
		}
	}

	private void movePthreadMutexInitializer(SequenceNode<BlockItemNode> root) {
		for (BlockItemNode item : root) {
			if (item == null)
				continue;

			String fileName = item.getSource().getFirstToken().getSourceFile().getName();

			if (fileName.equals("pthread.h") || fileName.equals("pthread.cvl"))
				continue;
			if (item instanceof FunctionDeclarationNode) {
				FunctionDeclarationNode function = (FunctionDeclarationNode) item;

				if (function.getName().equals("main"))
					break;
			} else if (item instanceof VariableDeclarationNode) {
				VariableDeclarationNode mutexInit = (VariableDeclarationNode) item;
				TypeNode type = mutexInit.getTypeNode();

				if (type instanceof TypedefNameNode) {
					TypedefNameNode typedefNode = (TypedefNameNode) type;

					if (typedefNode.getName().name().equals(PTHREAD_MUTEX_TYPE)) {
						firstMutexVarNode = item;
						break;
					}
				}
			}
		}
		if (firstMutexVarNode != null) {
			VariableDeclarationNode mutexInit = null;
			StructureOrUnionTypeNode mutexType = null, mutexAttrType = null;
			int achieved = 0;

			for (BlockItemNode item : root) {
				if (item == null)
					continue;
				if (!item.getSource().getFirstToken().getSourceFile().getName().equals("pthread.cvl"))
					continue;
				if (item instanceof VariableDeclarationNode) {
					VariableDeclarationNode variable = (VariableDeclarationNode) item;
					String name = variable.getName();

					if (name.equals(PTHREAD_MUTEX_INITIALIZER)) {
						mutexInit = variable;
						achieved++;
					}
				} else if (item instanceof StructureOrUnionTypeNode) {
					StructureOrUnionTypeNode structType = (StructureOrUnionTypeNode) item;

					if (structType.getName().equals(PTHREAD_MUTEX_TYPE)) {
						mutexType = structType;
						achieved++;
					} else if (structType.getName().equals(PTHREAD_MUTEX_ATTR_TYPE)) {
						mutexAttrType = structType;
						achieved++;
					}
				}
				if (achieved == 3)
					break;
			}
			if (mutexInit != null) {
				mutexAttrType.remove();
				root.insertChildren(this.firstMutexVarNode.childIndex(), Arrays.asList(mutexAttrType));
				mutexType.remove();
				root.insertChildren(this.firstMutexVarNode.childIndex(), Arrays.asList(mutexType));
				mutexInit.remove();
				root.insertChildren(this.firstMutexVarNode.childIndex(), Arrays.asList(mutexInit));
			}
		}
	}

	private void movePthreadGpoolDeclaration(SequenceNode<BlockItemNode> root) {
		for (BlockItemNode item : root) {
			if (item != null && item instanceof FunctionDeclarationNode) {
				FunctionDeclarationNode function = (FunctionDeclarationNode) item;
				String name = function.getName();

				if (this.threadFunctionNames.contains(name)) {
					this.firstThreadFunctionNode = item;
					break;
				}
			}
		}
		if (firstThreadFunctionNode != null) {
			VariableDeclarationNode gpool = null;

			for (BlockItemNode item : root) {
				if (item != null && item instanceof VariableDeclarationNode) {
					VariableDeclarationNode variable = (VariableDeclarationNode) item;
					String name = variable.getName();

					if (name.equals(PTHREAD_GPOOL)) {
						gpool = variable;
						break;
					}
				}
			}
			if (gpool != null) {
				gpool.remove();
				root.insertChildren(this.firstThreadFunctionNode.childIndex(), Arrays.asList(gpool));
			}
		}
	}

	// TODO: what is this function trying to do for pthread_create? What kind of
	// transformation and why?
	/**
	 * find out all functions used for pthread_create, i.e., all functions that will
	 * be executed in a thread.
	 * 
	 * @param node
	 */
	private void getThreadFunctions(ASTNode node) {
		for (ASTNode child : node.children()) {
			if (child == null)
				continue;
			if (child instanceof FunctionCallNode) {
				FunctionCallNode funcCall = (FunctionCallNode) child;
				ExpressionNode funcName = funcCall.getFunction();
				if (funcName instanceof IdentifierExpressionNode) {
					IdentifierExpressionNode named = (IdentifierExpressionNode) funcName;
					String nameString = named.getIdentifier().name();
					if (nameString.equals(PTHREAD_CREATE)) {
						ExpressionNode arg = funcCall.getArgument(2);

						if (arg instanceof OperatorNode) {
							OperatorNode argOp = (OperatorNode) arg;
							IdentifierExpressionNode threadName = (IdentifierExpressionNode) argOp.getArgument(0);

							threadFunctionNames.add(threadName.getIdentifier().name());
						} else if (arg instanceof IdentifierExpressionNode) {
							IdentifierExpressionNode threadName = (IdentifierExpressionNode) funcCall.getArgument(2);

							threadFunctionNames.add(threadName.getIdentifier().name());
						} else if (arg instanceof CastNode) {
							IdentifierExpressionNode threadName = (IdentifierExpressionNode) ((CastNode) arg)
									.getArgument();

							threadFunctionNames.add(threadName.getIdentifier().name());
						} else {
							throw new CIVLUnimplementedFeatureException(
									"unimplemented handling of Pthread transformer for expression of "
											+ arg.expressionKind()
											+ " kind as the function pointer argument of pthread_create()");
						}
					}
				}
			} else {
				getThreadFunctions(child);
			}
		}
	}

	@SuppressWarnings("unused")
	private ReturnNode returnNull() throws SyntaxException {
		ExpressionNode nullNode = nodeFactory.newCastNode(this.newSource("cast expression", CivlcTokenConstant.CAST),
				nodeFactory.newPointerTypeNode(this.newSource("type void *", CivlcTokenConstant.TYPE), this.voidType()),
				this.integerConstant(0));

		return nodeFactory.newReturnNode(this.newSource("return statement", CivlcTokenConstant.RETURN), nullNode);
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	private boolean isVoidOrVoidPointerType(TypeNode type) {
		if (type.kind() == TypeNodeKind.POINTER) {
			PointerTypeNode pointer = (PointerTypeNode) type;

			if (pointer.referencedType().kind() == TypeNodeKind.VOID)
				return true;
		}
		return (type.kind() == TypeNodeKind.VOID);
	}

	/*
	 * 
	 */

	private boolean hasReturn(ASTNode node) {
		if (node instanceof ReturnNode) {
			return true;
		} else {
			for (ASTNode child : node.children()) {
				if (child != null) {
					if (hasReturn(child)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private void check_thread_local_accesses(SequenceNode<BlockItemNode> root) {
		for (BlockItemNode item : root) {
			if (item == null)
				continue;
			if (item instanceof VariableDeclarationNode) {
				VariableDeclarationNode varDecl = (VariableDeclarationNode) item;

				if (varDecl.hasThreadLocalStorage())
					this.thread_local_variable_declarations.add(varDecl);
				else {
					if (!varDecl.getTypeNode().isInputQualified())
						this.shared_variables.add(varDecl.getEntity().getDefinition());
				}
			} else if (item instanceof FunctionDefinitionNode) {
				if (thread_local_variable_declarations.size() > 0) {
					FunctionDefinitionNode function = (FunctionDefinitionNode) item;

					if (!function.getName().equals("main") && !function.getName().equals("_gen_main")
							&& has_reference_to_thread_local_variables(function.getBody())) {
						this.newNonThreadFunctionNamesWtThreadLocal.add(function.getName());
						this.nonThreadFunctionsWtThreadLocal.add(function);
						this.nonThreadFunctionNamesWtThreadLocal.add(function.getName());
					}
				}
			}
		}
	}

	private boolean has_reference_to_thread_local_variables(ASTNode node) {
		if (node instanceof IdentifierExpressionNode) {
			IdentifierExpressionNode identifier = (IdentifierExpressionNode) node;
			Entity entity = identifier.getIdentifier().getEntity();

			if (entity instanceof Variable) {
				Variable variable = (Variable) entity;

				if (this.thread_local_variable_declarations.contains(variable.getDefinition())) {
					return true;
				}
			}
		} else {
			for (ASTNode child : node.children()) {
				if (child == null)
					continue;
				if (this.has_reference_to_thread_local_variables(child))
					return true;
			}
		}
		return false;
	}

	/* ********************* Methods From BaseTransformer ****************** */

	@Override
	protected AST transformCore(AST ast) throws SyntaxException {
		if (!this.hasHeader(ast, PTHREAD_HEADER))
			return ast;

		SequenceNode<BlockItemNode> root = ast.getRootNode();

		assert this.astFactory == ast.getASTFactory();
		assert this.nodeFactory == astFactory.getNodeFactory();
		setOrignalMainFunction(root);
		this.threadFunctionNames.add(this.originalMain);
		check_thread_local_accesses(root);
		this.getThreadFunctions(root);
		ast.release();
		movePthreadGpoolDeclaration(root);
		movePthreadMutexInitializer(root);
		transformWorker(root);
		AST result = astFactory.newAST(root, ast.getSourceFiles(), ast.isWholeProgram());
		// result.prettyPrint(System.out, false);
		return result;
	}
}
