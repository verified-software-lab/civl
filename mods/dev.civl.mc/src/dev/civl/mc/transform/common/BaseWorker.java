package dev.civl.mc.transform.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Entity.EntityKind;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.NodePredicate;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.declaration.EnumeratorDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FieldDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.TypedefDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.CastNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.IntegerConstantNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.RegularRangeNode;
import dev.civl.abc.ast.node.IF.expression.SizeableNode;
import dev.civl.abc.ast.node.IF.expression.StringLiteralNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.ExpressionStatementNode;
import dev.civl.abc.ast.node.IF.statement.ForLoopInitializerNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode.StatementKind;
import dev.civl.abc.ast.node.IF.statement.WhenNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.type.IF.ArrayType;
import dev.civl.abc.ast.type.IF.EnumerationType;
import dev.civl.abc.ast.type.IF.PointerType;
import dev.civl.abc.ast.type.IF.QualifiedObjectType;
import dev.civl.abc.ast.type.IF.StandardBasicType;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.StructureOrUnionType;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.ast.value.IF.IntegerValue;
import dev.civl.abc.config.IF.Configurations.Language;
import dev.civl.abc.err.IF.ABCException;
import dev.civl.abc.front.IF.CivlcTokenConstant;
import dev.civl.abc.front.c.parse.COmpParser;
import dev.civl.abc.front.c.parse.CParser;
import dev.civl.abc.main.ABCExecutor;
import dev.civl.abc.main.TranslationTask;
import dev.civl.abc.main.TranslationTask.TranslationStage;
import dev.civl.abc.main.UnitTask;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.CivlcToken.TokenVocabulary;
import dev.civl.abc.token.IF.Formation;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SourceFile;
import dev.civl.abc.token.IF.StringToken;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;
import dev.civl.abc.token.IF.TransformFormation;
import dev.civl.abc.transform.IF.Transformer;
import dev.civl.mc.config.IF.CIVLConstants;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLSyntaxException;
import dev.civl.mc.transform.IF.GeneralTransformer;

/**
 * Object used to perform one transformation task. It is instantiated to carry
 * out one invocation of {@link CIVLBaseTransformer#transform(AST)}.
 * 
 * @author siegel
 */
public abstract class BaseWorker {
	protected final static Map<String, String> EMPTY_MACRO_MAP = new HashMap<>(0);
	final static String GEN_MAIN = GeneralTransformer.PREFIX + "main";
	final static String MAIN = "main";
	public final static String ASSUME_PUSH = "$assume_push";
	public final static String ASSUME_POP = "$assume_pop";
	public final static String ASSUME = "$assume";
	public final static String ASSERT = "$assert";
	public final static String CHOOSE_INT = "$choose_int";
	public final static String ELABORATE_LOOP_VAR = "_elab_i";
	public final static String HAVOC = "$havoc";
	public final static String DEREFRABLE = "$is_derefable";
	public final static String MEMCPY = "memcpy";
	final static String EXTENT_MPI_DATATYPE = "$mpi_extentof";

	protected String identifierPrefix;

	/**
	 * The number of new identifiers created by this transformer worker.
	 */
	protected int newIdentifierCounter = 0;

	/**
	 * The name of this transformer, e.g., "OMPtoCIVLTransformer". To be used in
	 * output such as error messages.
	 */
	protected String transformerName;

	/**
	 * The AST factory used by this transformer for all its AST needs.
	 */
	protected ASTFactory astFactory;

	/**
	 * The node factory used by this transformer; same as the node factory
	 * associated to the {@link #astFactory}.
	 */
	protected NodeFactory nodeFactory;

	/**
	 * The token factory used by this transformer; same as the token factory used by
	 * the {@link #astFactory}.
	 */
	protected TokenFactory tokenFactory;

	/* ****************************** Constructor ************************** */

	protected BaseWorker(String transformerName, ASTFactory astFactory) {
		this.transformerName = transformerName;
		this.astFactory = astFactory;
		this.nodeFactory = astFactory.getNodeFactory();
		this.tokenFactory = astFactory.getTokenFactory();
	}

	/* ************************** Protected Methods ************************ */

	protected boolean isUnsignedIntegerType(Type type) {
		if (type instanceof StandardBasicType) {
			StandardBasicType basicType = (StandardBasicType) type;

			return (basicType.getBasicTypeKind() == BasicTypeKind.UNSIGNED);
		} else if (type instanceof QualifiedObjectType) {
			return this.isUnsignedIntegerType(((QualifiedObjectType) type).getBaseType());
		}
		return false;
	}

	/**
	 * Transforms the AST. This is the method that will be invoked to implement
	 * {@link Transformer#transform(AST)}.
	 * 
	 * @param ast the given AST to transform
	 * @return the transformed AST, which may or may not == the given one
	 * @throws SyntaxException if some statically-detectable error is discovered in
	 *                         the process of transformation
	 */
	public AST transform(AST ast) throws SyntaxException {
		AST newAST = transformCore(ast);
		completeSources(newAST.getRootNode());
		return newAST;
	}

	protected abstract AST transformCore(AST ast) throws SyntaxException;

	/**
	 * Elaborate an expression by inserting an empty for-loop bounded by the given
	 * expression: <code>for(int i = 0; i < expression; i++);
	 * </code>
	 * 
	 * @param expression The expression will be elaborated
	 * @return A for-loop statement node
	 * @throws SyntaxException An unexpected exception happens during creation of a
	 *                         zero constant node.
	 */
	protected StatementNode elaborateExpression(ExpressionNode expression) throws SyntaxException {
		Source source = newSource("elaborateExpression", CivlcTokenConstant.FOR);
		VariableDeclarationNode forLoopVarDecl = nodeFactory.newVariableDeclarationNode(source,
				identifier(ELABORATE_LOOP_VAR), nodeFactory.newBasicTypeNode(source, BasicTypeKind.INT),
				nodeFactory.newIntegerConstantNode(source, "0"));
		ForLoopInitializerNode forLoopInitializer = nodeFactory.newForLoopInitializerNode(source,
				Arrays.asList(forLoopVarDecl));
		ExpressionNode forLoopCondition = nodeFactory.newOperatorNode(source, Operator.LT,
				Arrays.asList(identifierExpression(ELABORATE_LOOP_VAR), expression.copy()));
		ExpressionNode forLoopIncrementor = nodeFactory.newOperatorNode(source, Operator.POSTINCREMENT,
				Arrays.asList(identifierExpression(ELABORATE_LOOP_VAR)));

		return nodeFactory.newForLoopNode(source, forLoopInitializer, forLoopCondition, forLoopIncrementor,
				nodeFactory.newNullStatementNode(source), null);
	}

	/**
	 * Does the root node contains a _main function definition in its children?
	 * 
	 * @return
	 */
	protected boolean has_gen_mainFunction(SequenceNode<BlockItemNode> root) {
		for (BlockItemNode child : root) {
			if (child == null)
				continue;
			if (child instanceof FunctionDefinitionNode) {
				if (((FunctionDefinitionNode) child).getName().equals(GEN_MAIN))
					return true;
			}
		}
		return false;
	}

	/**
	 * Identifies adjacent nodes that are redundant and reduce them to exactly one
	 * node.
	 * 
	 * @param root
	 * @param nodePredicate The predicate that if an AST node holds then it will be
	 *                      considered as the target one.
	 */
	protected void reduceDuplicateNode(ASTNode root, NodePredicate nodePredicate) {
		int lastIndex = -1;
		int numChildren = root.numChildren();
		// boolean changed = false;

		for (int i = 0; i < numChildren; i++) {
			ASTNode child = root.child(i);

			if (child == null)
				continue;
			reduceDuplicateNode(child, nodePredicate);
			child = root.child(i);
			if (nodePredicate.holds(child)) {
				if (lastIndex >= 0 && lastIndex == i - 1) {
					// this node is identical to the previous node, then remove
					// last node
					root.removeChild(lastIndex);
					// changed = true;
				} else {
					ASTNode previousNonNullChild = this.nonNullChildBefore(root, i);

					if (previousNonNullChild != null && (previousNonNullChild instanceof CompoundStatementNode)) {
						CompoundStatementNode previousCompound = (CompoundStatementNode) previousNonNullChild;
						ASTNode lastChildOfPrevious = this.getVeryLastItemNodeOfCompoundStatement(previousCompound);

						if (lastChildOfPrevious != null && nodePredicate.holds(lastChildOfPrevious)) {
							lastChildOfPrevious.remove();
						}
					}
				}
				// update last index
				lastIndex = i;
			}
		}
		this.normalizeCompoundStatementNodes(root);
		// if (root.parent() != null) {
		// root.parent().setChild(root.childIndex(),
		// this.normalizeCompoundStatementNodes(root));
		// }
		// if (changed && root.parent() != null) {
		// if (root instanceof CompoundStatementNode) {
		// CompoundStatementNode compoundNode = (CompoundStatementNode) root;
		// List<BlockItemNode> newChildren = new LinkedList<>();
		// int rootIndex = root.childIndex();
		//
		// for (BlockItemNode child : compoundNode) {
		// if (child != null) {
		// child.remove();
		// newChildren.add(child);
		// }
		// }
		// if (newChildren.size() == 1
		// && root.parent() instanceof CompoundStatementNode)
		// root.parent().setChild(rootIndex, newChildren.get(0));
		// else
		// root.parent().setChild(
		// rootIndex,
		// nodeFactory.newCompoundStatementNode(
		// root.getSource(), newChildren));
		// }
		// }
	}

	protected ASTNode nonNullChildBefore(ASTNode node, int index) {
		int numChildren = node.numChildren();

		for (int i = index - 1; i < numChildren && i > 0; i--) {
			ASTNode child = node.child(i);

			if (child != null)
				return child;
		}
		return null;
	}

	protected int getIntValue(IntegerConstantNode constant) {
		return constant.getConstantValue().getIntegerValue().intValue();
	}

	protected ASTNode getVeryLastItemNodeOfCompoundStatement(CompoundStatementNode compound) {
		int numChildren = compound.numChildren();

		for (int i = numChildren - 1; i >= 0; i--) {
			BlockItemNode child = compound.getSequenceChild(i);

			if (child != null) {
				if (child instanceof CompoundStatementNode)
					return this.getVeryLastItemNodeOfCompoundStatement((CompoundStatementNode) child);
				else
					return child;
			}
		}
		return null;
	}

	protected ExpressionNode nullPointer() throws SyntaxException {
		return nodeFactory.newCastNode(newSource("nullPointer", 0),
				this.typeNode(nodeFactory.typeFactory().pointerType(nodeFactory.typeFactory().voidType())),
				this.integerConstant(0));
	}

	/**
	 * For a compound statement node, removes any child node that is null or an
	 * empty compound statement node.
	 * 
	 * @param node
	 * @return
	 */
	protected ASTNode normalizeCompoundStatementNodes(ASTNode node) {
		int numChildren = node.numChildren();
		ASTNode newNode = node;

		for (int i = 0; i < numChildren; i++) {
			ASTNode child = node.child(i);

			if (child == null)
				continue;
			normalizeCompoundStatementNodes(child);
		}
		if (node instanceof CompoundStatementNode) {
			List<BlockItemNode> items = new LinkedList<>();
			CompoundStatementNode compound = (CompoundStatementNode) node;

			for (BlockItemNode item : compound) {
				if (item != null && (!(item instanceof CompoundStatementNode)
						|| !this.isEmptyCompoundStatementNode((CompoundStatementNode) item))) {
					item.remove();
					items.add(item);
				}
			}
			newNode = nodeFactory.newCompoundStatementNode(node.getSource(), items);
			if (node.parent() != null)
				node.parent().setChild(node.childIndex(), newNode);
		}
		return newNode;
	}

	protected boolean isEmptyCompoundStatementNode(CompoundStatementNode compound) {
		if (compound.numChildren() == 0)
			return true;
		for (BlockItemNode child : compound) {
			if (child == null)
				continue;
			if (child instanceof CompoundStatementNode) {
				if (isEmptyCompoundStatementNode((CompoundStatementNode) child))
					continue;
			}
			return false;
		}
		return true;
	}

	protected boolean is_callee_name_equals(FunctionCallNode call, String name) {
		ExpressionNode function = call.getFunction();

		if (function instanceof IdentifierExpressionNode) {
			String callee = ((IdentifierExpressionNode) function).getIdentifier().name();

			if (callee.equals(name))
				return true;
		}
		return false;
	}

	/**
	 * rename all main function declaration to _main, and all function call to main
	 * to _main.
	 * 
	 * @param root
	 */
	protected void transformMainFunction(SequenceNode<BlockItemNode> root) {
		for (BlockItemNode child : root) {
			if (child == null)
				continue;
			if (child instanceof FunctionDeclarationNode) {
				FunctionDeclarationNode funcDecl = (FunctionDeclarationNode) child;

				if (funcDecl.getName().equals(MAIN)) {
					funcDecl.getIdentifier().setName(GEN_MAIN);
				}
			}
			transformMainCall(child);
		}
	}

	protected void createNewMainFunction(SequenceNode<BlockItemNode> root) {
		String srcMethod = "createNewMainFunction";
		List<BlockItemNode> blockItems = new LinkedList<>();
		blockItems.add(nodeStmtCall(srcMethod, GEN_MAIN));

		FunctionDefinitionNode newMainFunction = nodeDefnFunction(srcMethod, MAIN, nodeTypeInt(srcMethod),
				new LinkedList<VariableDeclarationNode>(), blockItems);

		root.addSequenceChild(newMainFunction);
	}

	/**
	 * rename all calls to main to _main.
	 * 
	 * @param node
	 */
	private void transformMainCall(ASTNode node) {
		if (node instanceof FunctionCallNode) {
			FunctionCallNode call = (FunctionCallNode) node;
			ExpressionNode function = call.getFunction();

			if (function instanceof IdentifierExpressionNode) {
				IdentifierNode functionID = ((IdentifierExpressionNode) function).getIdentifier();

				if (functionID.name().equals(MAIN))
					functionID.setName(GEN_MAIN);
			}
		}
		for (ASTNode child : node.children()) {
			if (child == null)
				continue;
			this.transformMainCall(child);
		}
	}

	protected FunctionDeclarationNode assumeFunctionDeclaration(Source source) {
		IdentifierNode name = nodeFactory.newIdentifierNode(source, "$assume");
		FunctionTypeNode funcType = nodeFactory.newFunctionTypeNode(source, nodeFactory.newVoidTypeNode(source),
				nodeFactory.newSequenceNode(source, "Formals",
						Arrays.asList(nodeFactory.newVariableDeclarationNode(source,
								nodeFactory.newIdentifierNode(source, "expression"),
								nodeFactory.newBasicTypeNode(source, BasicTypeKind.BOOL))))

				, false);
		FunctionDeclarationNode function = nodeFactory.newFunctionDeclarationNode(source, name, funcType, null);

		function.setSystemFunctionSpecifier(true);
		function.setSystemLibrary("civlc");
		return function;
	}

	protected FunctionCallNode functionCall(Source source, String name, List<ExpressionNode> arguments) {
		return nodeFactory.newFunctionCallNode(source, this.identifierExpression(source, name), arguments);
	}

	/**
	 * Produces a unique identifier ending with the given name.
	 * 
	 * @param name The ending of the new unique identifier.
	 * @return a unique identifier ending with the given name.
	 */
	protected String newUniqueIdentifier(String name) {
		return identifierPrefix + this.newIdentifierCounter++ + "_" + name;
	}

	protected StatementNode assumeNode(ExpressionNode expression) {
		return nodeFactory.newExpressionStatementNode(
				this.functionCall(this.newSource("assumeNode", CivlcTokenConstant.EXPRESSION_STATEMENT), ASSUME,
						Arrays.asList(expression)));
	}

	/**
	 * Produces a unique identifier.
	 * 
	 * @return a unique identifier.
	 */
	protected String newUniqueIdentifierPrefix() {
		return identifierPrefix + this.newIdentifierCounter++;
	}

	/**
	 * parses a certain CIVL library (which resides in the folder text/include) into
	 * an AST.
	 * 
	 * @param file the file of the library, e.g., civlc.cvh, civlc-omp.cvh, etc.
	 * @return the AST of the given library.
	 * @throws ABCException
	 */
	protected AST parseSystemLibrary(File file, Map<String, String> macros) {
		UnitTask task = new UnitTask(new File[] { file });

		task.setLanguage(Language.C);
		task.setMacros(macros);

		TranslationTask translation = new TranslationTask(new UnitTask[] { task });

		translation.setStage(TranslationStage.ANALYZE_ASTS);

		ABCExecutor executor = new ABCExecutor(translation);

		try {
			executor.execute();
		} catch (ABCException e) {
			throw new CIVLSyntaxException(
					"unable to parse system library " + file + " while applying " + this.transformerName + ":\n" + e);
		}
		return executor.getAST(0);
	}

	/**
	 * Creates a new {@link Source} object to associate to AST nodes that are
	 * invented by this transformer worker.
	 * 
	 * @param method    any string which identifies the specific part of this
	 *                  transformer responsible for creating the new content;
	 *                  typically, the name of the method that created the new
	 *                  context. This will appear in error message to help isolate
	 *                  the source of the new content.
	 * @param tokenType the integer code for the type of the token used to represent
	 *                  the source; use one of the constants in {@link CParser} or
	 *                  {@link COmpParser}, for example, such as
	 *                  {@link CParser#IDENTIFIER}.
	 * @return the new source object
	 */
	protected Source newSource(String method, int tokenType) {
		Formation formation = tokenFactory.newTransformFormation(transformerName, method);
		// "inserted text" is just something temporary for now, and it will be
		// fixed when complete source is done in the transformer
		CivlcToken token = tokenFactory.newCivlcToken(tokenType, "inserted text", formation, TokenVocabulary.DUMMY);
		Source source = tokenFactory.newSource(token);

		return source;
	}

	/**
	 * This method should be called after the transformer has completed its
	 * transformation; it finds all source objects (in node and the descendants of
	 * node) that were created by this transformer and adds more information to
	 * them. The new information includes the pretty-print textual representation of
	 * the content of that node (and its descendants), and the precise point in
	 * original actual source code where the new content was inserted.
	 * 
	 * @param node a node in the AST being transformed; typically, the root node
	 */
	protected void completeSources(ASTNode node) {
		ASTNode postNode = nextRealNode(node);
		ASTNode preNode = null;

		for (; node != null; node = node.nextDFS()) {
			if (node == postNode) {
				preNode = postNode;
				postNode = nextRealNode(preNode);
				continue;
			}
			Source source = node.getSource();

			if (source != null) {
				CivlcToken firstToken = source.getFirstToken();

				if (firstToken != null) {
					Formation formation = firstToken.getFormation();

					if (formation instanceof TransformFormation) {
						TransformFormation tf = (TransformFormation) formation;

						if (transformerName.equals(tf.getLastFile().getName())) {
							CivlcToken preToken = preNode == null ? null : preNode.getSource().getLastToken();
							CivlcToken postToken = postNode == null ? null : postNode.getSource().getFirstToken();
							String text = node.prettyRepresentation(35).toString();

							tf.setPreToken(preToken);
							tf.setPostToken(postToken);
							firstToken.setText(text);
						}
					}
				}
			}
		}
	}

	/**
	 * Creates an identifier node with a given name. The source information of the
	 * new node is automatically constructed using the method
	 * {@link #newSource(String, int)}.
	 * 
	 * @param name The name of the identifier.
	 * @return the new identifier node.
	 */
	protected IdentifierNode identifier(String name) {
		return nodeFactory.newIdentifierNode(this.newSource("identifier " + name, CivlcTokenConstant.IDENTIFIER), name);
	}

	/**
	 * Creates an identifier expression node with a given name. The source
	 * information of the new node is automatically constructed using the method
	 * {@link #newSource(String, int)}.
	 * 
	 * @param name The name of the identifier.
	 * @return the new identifier expression node.
	 */
	protected ExpressionNode identifierExpression(String name) {
		Source source = this.newSource("identifierExpression " + name, CivlcTokenConstant.IDENTIFIER);

		return nodeFactory.newIdentifierExpressionNode(source, nodeFactory.newIdentifierNode(source, name));
	}

	/**
	 * Creates an identifier expression node with a given name.
	 * 
	 * @param source The source information of the identifier.
	 * @param name   The name of the identifier.
	 * @return the new identifier expression node.
	 */
	protected ExpressionNode identifierExpression(Source source, String name) {
		return nodeFactory.newIdentifierExpressionNode(source, nodeFactory.newIdentifierNode(source, name));
	}

	/**
	 * Creates a variable declaration node with a given name of the specified type.
	 * The sources are created automatically through the method
	 * {@link #newSource(String, int)}.
	 * 
	 * @param name The name of the variable
	 * @param type The type of the variable
	 * @return the new variable declaration node
	 */
	protected VariableDeclarationNode variableDeclaration(String name, TypeNode type) {
		return nodeFactory.newVariableDeclarationNode(
				this.newSource("variableDeclaration " + name, CivlcTokenConstant.DECLARATION), this.identifier(name),
				type);
	}

	/**
	 * Creates a variable declaration node with a given name of the specified type
	 * and initializer. The sources are created automatically through the method
	 * {@link #newSource(String, int)}.
	 * 
	 * @param name The name of the variable
	 * @param type The type of the variable
	 * @param init The initializer of the variable
	 * @return the new variable declaration node.
	 */
	protected VariableDeclarationNode variableDeclaration(String name, TypeNode type, ExpressionNode init) {
		// String text = type.prettyRepresentation() + " " + name;
		// if (init != null)
		// text = text + " = " + init.prettyRepresentation();
		return nodeFactory.newVariableDeclarationNode(
				this.newSource("variableDeclaration " + name, CivlcTokenConstant.DECLARATION), this.identifier(name),
				type, init);
	}

	/**
	 * Creates a constant node of <code>$here</code>, the source of which is
	 * generated automatically using {@link #newSource(String, int)}.
	 * 
	 * @return the new here node.
	 */
	protected ExpressionNode hereNode() {
		return nodeFactory.newHereNode(this.newSource("hereNode", CivlcTokenConstant.HERE));
	}

	/**
	 * Creates a type node of void type, the source of which is generated
	 * automatically using {@link #newSource(String, int)}.
	 * 
	 * @return the new void type node.
	 */
	protected TypeNode voidType() {
		return nodeFactory.newVoidTypeNode(this.newSource("voidType", CivlcTokenConstant.VOID));
	}

	protected boolean isVoidType(Type type) {
		return type.kind() == TypeKind.VOID;
	}

	/**
	 * Creates a type node of a certain basic type kind, the source of which is
	 * generated automatically using {@link #newSource(String, int)}.
	 * 
	 * @param kind the specified basic type kind
	 * @return the new basic type node.
	 */
	protected TypeNode basicType(BasicTypeKind kind) {
		String name = "";

		switch (kind) {
		case BOOL:
			name = "_Bool";
			break;
		case CHAR:
			name = "char";
			break;
		case DOUBLE:
		case DOUBLE_COMPLEX:
			name = "double";
			break;
		case FLOAT:
		case FLOAT_COMPLEX:
			name = "float";
			break;
		case INT:
			name = "int";
			break;
		case LONG:
			name = "long";
			break;
		case LONG_DOUBLE:
			name = "long double";
			break;
		case LONG_DOUBLE_COMPLEX:
			name = "long double";
			break;
		case LONG_LONG:
			name = "long long";
			break;
		case REAL:
			name = "real";
			break;
		case SHORT:
			name = "short";
			break;
		case SIGNED_CHAR:
			name = "signed char";
			break;
		case UNSIGNED:
			name = "unsigned";
			break;
		case UNSIGNED_CHAR:
			name = "unsigned char";
			break;
		case UNSIGNED_LONG:
			name = "unsigned long";
			break;
		case UNSIGNED_LONG_LONG:
			name = "unsigned long long";
			break;
		case UNSIGNED_SHORT:
			name = "unsigned short";
		default:
		}
		return this.nodeFactory.newBasicTypeNode(this.newSource("type " + name, CivlcTokenConstant.TYPE), kind);
	}

	/**
	 * Creates a type node of a given type, the source of which is generated
	 * automatically using {@link #newSource(String, int)}.
	 * 
	 * @param type the specified type
	 * @return the new type node.
	 */
	protected TypeNode typeNode(Type type) {
		Source source = this.newSource("typeNode " + type, CivlcTokenConstant.TYPE);

		return this.typeNode(source, type);
	}

	protected TypeNode typeNode(Source source, Type type) {
		try {
			return typeNode(source, type, nodeFactory);
		} catch (SyntaxException e) {
			throw new CIVLInternalException("Unexpected syntax exception in type: " + type, source);
		}
	}

	/**
	 * Creates a type node of a given type, with the given source.
	 * 
	 * @param source The source of the type node
	 * @param type   the specified type
	 * @return the new type node
	 * @throws SyntaxException
	 */
	public static TypeNode typeNode(Source source, Type type, NodeFactory nodeFactory) throws SyntaxException {

		switch (type.kind()) {
		case VOID:
			return nodeFactory.newVoidTypeNode(source);
		case BASIC:
			return nodeFactory.newBasicTypeNode(source, ((StandardBasicType) type).getBasicTypeKind());
		case OTHER_INTEGER:
			return nodeFactory.newBasicTypeNode(source, BasicTypeKind.INT);
		case ARRAY:
			ExpressionNode extent;
			ArrayType arrayType = (ArrayType) type;

			if (arrayType.hasKnownConstantSize()) {
				IntegerValue constExtent = arrayType.getConstantSize();

				extent = nodeFactory.newIntegerConstantNode(source, constExtent.toString());
			} else
				extent = arrayType.getVariableSize().copy();
			return nodeFactory.newArrayTypeNode(source,
					typeNode(source, ((ArrayType) type).getElementType(), nodeFactory), extent);
		case POINTER:
			return nodeFactory.newPointerTypeNode(source,
					typeNode(source, ((PointerType) type).referencedType(), nodeFactory));
		case QUALIFIED:
			return typeNode(source, ((QualifiedObjectType) type).getBaseType(), nodeFactory);
		case STRUCTURE_OR_UNION: {
			StructureOrUnionType structOrUnionType = (StructureOrUnionType) type;

			return nodeFactory.newStructOrUnionTypeNode(source, structOrUnionType.isStruct(),
					nodeFactory.newIdentifierNode(source, structOrUnionType.getTag()), null);
		}
		case ENUMERATION: {
			EnumerationType enumType = (EnumerationType) type;

			return nodeFactory.newTypedefNameNode(nodeFactory.newIdentifierNode(source, enumType.getTag()));
		}
		case SCOPE:
			return nodeFactory.newScopeTypeNode(source);
		default:
		}
		return null;
	}

	/**
	 * Creates a boolean constant node (either <code>$true</code> or
	 * <code>$false</code>), the source of which is generated automatically using
	 * {@link #newSource(String, int)}.
	 * 
	 * @param value The value of the boolean constant
	 * @return the new boolean constant node
	 */
	protected ExpressionNode booleanConstant(boolean value) {
		String method = value ? "booleanConstant $true" : "booleanConstant $false";
		int tokenType = value ? 1 : 0;

		return nodeFactory.newBooleanConstantNode(this.newSource(method, tokenType), value);
	}

	/**
	 * Creates an integer constant node of the specified value, the source of which
	 * is generated automatically using {@link #newSource(String, int)}.
	 * 
	 * @param value The value of the integer constant
	 * @return the new integer constant node
	 */
	protected ExpressionNode integerConstant(int value) throws SyntaxException {
		return nodeFactory.newIntegerConstantNode(
				this.newSource("integerConstant " + value, CivlcTokenConstant.INTEGER_CONSTANT),
				Integer.toString(value));
	}

	/** @return {@link CompoundStatementNode} */
	protected CompoundStatementNode nodeBlock(String srcMethod, BlockItemNode... blockItems) {
		return nodeBlock(srcMethod, Arrays.asList(blockItems));
	}

	/** @return {@link CompoundStatementNode} */
	protected CompoundStatementNode nodeBlock(String srcMethod, List<BlockItemNode> blockItems) {
		return nodeFactory.newCompoundStatementNode(newSource(srcMethod, CivlcTokenConstant.COMPOUND_STATEMENT),
				blockItems);
	}

	protected VariableDeclarationNode nodeDeclVarInt(String srcMethod, String varName) {
		return nodeDeclVar(srcMethod, varName, nodeTypeInt(srcMethod));
	}

	/** @return {@link VariableDeclarationNode} with type but no init */
	protected VariableDeclarationNode nodeDeclVar(String srcMethod, String varName, TypeNode type) {
		return nodeFactory.newVariableDeclarationNode(newSource(srcMethod, CivlcTokenConstant.DECLARATION),
				nodeIdent(srcMethod, varName), type);
	}

	/** @return {@link VariableDeclarationNode} with type and init */
	protected VariableDeclarationNode nodeDeclVarInit(String srcMethod, String varName, TypeNode type,
			ExpressionNode init) {
		return nodeFactory.newVariableDeclarationNode(newSource(srcMethod, CivlcTokenConstant.DECLARATION),
				nodeIdent(srcMethod, varName), type, init);
	}

	/** @return {@link FunctionCallNode} */
	protected FunctionCallNode nodeExprCall(String srcMethod, String funcName, ExpressionNode... argExprs) {
		return nodeFactory.newFunctionCallNode(newSource(srcMethod, CivlcTokenConstant.CALL),
				nodeExprId(srcMethod, funcName), Arrays.asList(argExprs));
	}

	/** @return {@link ExpressionNode}: <code>(type) expr</code> */
	protected CastNode nodeExprCast(String srcMethod, TypeNode type, ExpressionNode expr) {
		return nodeFactory.newCastNode(newSource(srcMethod, CivlcTokenConstant.CAST), type, expr);
	}

	protected ExpressionNode nodeExprNullPointer(String srcMethod) {
		TypeNode voidType = voidType();
		return nodeExprCast(srcMethod, nodeFactory.newPointerTypeNode(voidType.getSource(), voidType),
				nodeExprInt(srcMethod, 0));
	}

	/** @return {@link ExpressionNode}: <code>$here</code> */
	protected ExpressionNode nodeExprHere(String srcMethod) {
		return nodeFactory.newHereNode(newSource(srcMethod, CivlcTokenConstant.HERE));
	}

	/** @return {@link IdentifierExpressionNode} */
	protected IdentifierExpressionNode nodeExprId(String srcMethod, String idName) {
		IdentifierNode ident = nodeIdent(srcMethod, idName);

		return nodeFactory.newIdentifierExpressionNode(ident.getSource(), ident);
	}

	/** @return {@link IntegerConstantNode} */
	protected ExpressionNode nodeExprInt(String srcMethod, int val) {
		return nodeFactory.newIntConstantNode(newSource(srcMethod, CivlcTokenConstant.INTEGER_CONSTANT), val);
	}

	/** @return {@link RegularRangeNode} */
	protected ExpressionNode nodeExprRange(String srcMethod, ExpressionNode lo, ExpressionNode hi,
			ExpressionNode step) {
		if (step == null)
			return nodeFactory.newRegularRangeNode(newSource(srcMethod, CivlcTokenConstant.EXPR), lo, hi);
		else
			return nodeFactory.newRegularRangeNode(newSource(srcMethod, CivlcTokenConstant.EXPR), lo, hi, step);
	}

	protected ExpressionNode nodeExprDot(String srcMethod, ExpressionNode structExpr, String fieldName) {
		return nodeFactory.newDotNode(newSource(srcMethod, CivlcTokenConstant.DOT), structExpr,
				nodeIdent(srcMethod, fieldName));
	}

	protected ExpressionNode nodeExprArrow(String srcMethod, ExpressionNode structExpr, String fieldName) {
		return nodeFactory.newArrowNode(newSource(srcMethod, CivlcTokenConstant.ARROW), structExpr,
				nodeIdent(srcMethod, fieldName));
	}

	protected ExpressionNode nodeExprOp(String srcMethod, Operator op, ExpressionNode... argExprs) {
		return nodeFactory.newOperatorNode(newSource(srcMethod, CivlcTokenConstant.OTHER), op, Arrays.asList(argExprs));
	}

	protected ExpressionNode nodeExprSizeof(String srcMethod, SizeableNode sizeable) {
		return nodeFactory.newSizeofNode(newSource(srcMethod, CivlcTokenConstant.SIZEOF), sizeable);
	}

	protected FunctionDefinitionNode nodeDefnFunction(String srcMethod, String functionName, TypeNode returnType,
			List<VariableDeclarationNode> parameters, List<BlockItemNode> body) {
		return nodeFactory.newFunctionDefinitionNode(newSource(srcMethod, CivlcTokenConstant.FUNCTION_DEFINITION),
				nodeIdent(srcMethod, functionName),
				nodeFactory.newFunctionTypeNode(newSource(srcMethod, CivlcTokenConstant.FUNCTION_DEFINITION),
						returnType,
						nodeFactory.newSequenceNode(newSource(srcMethod, CivlcTokenConstant.PARAMETER_LIST),
								functionName, parameters),
						false),
				null, nodeFactory.newCompoundStatementNode(newSource(srcMethod, CivlcTokenConstant.COMPOUND_STATEMENT),
						body));
	}

	/** @return {@link IdentifierNode} */
	protected IdentifierNode nodeIdent(String srcMethod, String idName) {
		return nodeFactory.newIdentifierNode(newSource(srcMethod, CivlcTokenConstant.IDENTIFIER), idName);
	}

	/** @return {@link StatementNode} for a function call */
	protected ExpressionStatementNode nodeStmtCall(String srcMethod, String funcName, ExpressionNode... argExprs) {
		return nodeFactory.newExpressionStatementNode(nodeExprCall(srcMethod, funcName, argExprs));
	}

	protected WhenNode nodeStmtWhen(String srcMethod, ExpressionNode guard) {
		return nodeStmtWhen(srcMethod, guard,
				nodeFactory.newNullStatementNode(newSource(srcMethod, CivlcTokenConstant.STATEMENT)));
	}

	protected WhenNode nodeStmtWhen(String srcMethod, ExpressionNode guard, StatementNode body) {
		return nodeFactory.newWhenNode(newSource(srcMethod, CivlcTokenConstant.WHEN), guard, body);
	}

	protected StatementNode nodeStmtAssign(String srcMethod, ExpressionNode lhs, ExpressionNode rhs) {
		return nodeFactory.newExpressionStatementNode(nodeFactory
				.newOperatorNode(newSource(srcMethod, CivlcTokenConstant.ASSIGN), Operator.ASSIGN, lhs, rhs));
	}

	protected BlockItemNode nodeBreak(String srcMethod) {
		return nodeFactory.newBreakNode(newSource(srcMethod, CivlcTokenConstant.BREAK));
	}

	/** @return CIVL <code>$domain(dim)</code> type node: */
	protected TypeNode nodeTypeDom(String srcMethod, int dim) {
		if (dim > 0) // $domain(dim)
			return nodeFactory.newDomainTypeNode(newSource(srcMethod, CivlcTokenConstant.DOMAIN),
					nodeExprInt(srcMethod, dim));
		else // $domain
			return nodeFactory.newDomainTypeNode(newSource(srcMethod, CivlcTokenConstant.DOMAIN));
	}

	protected TypeNode nodeTypeScope(String srcMethod) {
		return nodeFactory.newScopeTypeNode(newSource(srcMethod, CivlcTokenConstant.TYPE));
	}

	/** @return <code>int</code> {@link TypeNode} */
	protected TypeNode nodeTypeInt(String srcMethod) {
		return nodeFactory.newBasicTypeNode(newSource(srcMethod, CivlcTokenConstant.INT), BasicTypeKind.INT);
	}

	protected TypeNode nodeTypeBasic(String srcMethod, BasicTypeKind typeKind) {
		return nodeFactory.newBasicTypeNode(newSource(srcMethod, CivlcTokenConstant.TYPE), typeKind);
	}

	/** @return named_type_def {@link TypeNode} w/ given <code>name</code> */
	protected TypeNode nodeTypeNamed(String srcMethod, String name) {
		return nodeFactory.newTypedefNameNode(nodeIdent(srcMethod, name));
	}

	/** @return CIVL <code>$range</code> {@link TypeNode} */
	protected TypeNode nodeTypeRange(String srcMethod) {
		return nodeFactory.newRangeTypeNode(newSource(srcMethod, CivlcTokenConstant.RANGE));
	}

	protected TypeNode nodeTypePointer(String srcMethod, TypeNode referencedType) {
		return nodeFactory.newPointerTypeNode(newSource(srcMethod, CivlcTokenConstant.POINTER), referencedType);
	}

	protected TypeNode nodeTypeArray(String srcMethod, TypeNode elementType, ExpressionNode extentExpr) {
		return nodeFactory.newArrayTypeNode(newSource(srcMethod, CivlcTokenConstant.ARRAY_SUFFIX), elementType,
				extentExpr);
	}

	protected TypeNode nodeTypeFromExpr(String srcMethod, ExpressionNode expr) {
		return typeNode(newSource(srcMethod, CivlcTokenConstant.TYPE), expr.getConvertedType());
	}

	protected TypedefDeclarationNode nodeTypeDefStruct(String srcMethod, String structName,
			List<FieldDeclarationNode> fieldNodes) {
		return nodeFactory.newTypedefDeclarationNode(newSource(srcMethod, CivlcTokenConstant.TYPEDEF),
				nodeIdent(srcMethod, structName),
				nodeFactory.newStructOrUnionTypeNode(newSource(srcMethod, CivlcTokenConstant.STRUCT_DECLARATION), true,
						nodeIdent(srcMethod, structName),
						nodeFactory.newSequenceNode(newSource(srcMethod, CivlcTokenConstant.SEQUENCE),
								"Field sequence of " + structName, fieldNodes)));
	}

	protected FunctionDeclarationNode nodeDeclFunction(String srcMethod, String functionName, TypeNode returnType,
			List<VariableDeclarationNode> varDecls) {
		FunctionTypeNode functionTypeNode = nodeFactory.newFunctionTypeNode(
				newSource(srcMethod, CivlcTokenConstant.DECLARATION), returnType,
				nodeFactory.newSequenceNode(newSource(srcMethod, CivlcTokenConstant.SEQUENCE),
						functionName + " formal parameters", varDecls),
				false);
		return nodeFactory.newFunctionDeclarationNode(functionTypeNode.getSource(), nodeIdent(srcMethod, functionName),
				functionTypeNode, null);
	}

	protected FieldDeclarationNode nodeDeclField(String srcMethod, String fieldName, TypeNode fieldType) {
		IdentifierNode ident = nodeIdent(srcMethod, fieldName);
		return nodeFactory.newFieldDeclarationNode(tokenFactory.join(fieldType.getSource(), ident.getSource()), ident,
				fieldType);
	}

	protected EnumeratorDeclarationNode nodeDeclEnumerator(String srcMethod, String enumValueName) {
		return nodeFactory.newEnumeratorDeclarationNode(newSource(srcMethod, CivlcTokenConstant.ENUMERATOR),
				nodeIdent(srcMethod, enumValueName), null);
	}

	/**
	 * Combines two ASTs into one, assuming that there are no name conflicts.
	 * 
	 * @param first  the first AST
	 * @param second the second AST
	 * @return
	 * @throws SyntaxException
	 */
	protected AST combineASTs(AST first, AST second) throws SyntaxException {
		SequenceNode<BlockItemNode> rootNode, firstRoot = first.getRootNode(), secondRoot = second.getRootNode();
		List<BlockItemNode> allNodes = new ArrayList<>();
		List<SourceFile> sourceFiles = new ArrayList<>();
		boolean isWholeProgram = first.isWholeProgram() || second.isWholeProgram();

		sourceFiles.addAll(first.getSourceFiles());
		sourceFiles.addAll(second.getSourceFiles());
		first.release();
		for (BlockItemNode child : firstRoot) {
			if (child != null) {
				child.remove();
				allNodes.add(child);
			}
		}
		second.release();
		for (BlockItemNode child : secondRoot) {
			if (child != null) {
				child.remove();
				allNodes.add(child);
			}
		}
		rootNode = this.nodeFactory.newSequenceNode(secondRoot.getSource(), "Translation Unit", allNodes);
		return this.astFactory.newAST(rootNode, sourceFiles, isWholeProgram);
	}

	/**
	 * insert a block item node to a compound statement node at the given index.
	 * 
	 * @param compoundNode
	 * @param node
	 * @return
	 */
	protected CompoundStatementNode insertToCompoundStatement(CompoundStatementNode compoundNode, BlockItemNode node,
			int index) {
		int numChildren = compoundNode.numChildren();
		List<BlockItemNode> nodeList = new ArrayList<>(numChildren + 1);

		for (int i = 0; i < numChildren; i++) {
			BlockItemNode child = compoundNode.getSequenceChild(i);

			if (i == index)
				nodeList.add(node);
			nodeList.add(child);
			compoundNode.removeChild(i);
		}
		if (index >= numChildren)
			nodeList.add(node);
		return nodeFactory.newCompoundStatementNode(compoundNode.getSource(), nodeList);
	}

	protected void releaseNodes(List<? extends ASTNode> nodes) {
		for (ASTNode node : nodes) {
			if (node == null)
				continue;
			node.remove();
		}
	}

	/* *************************** Private Methods ************************* */

	/**
	 * Determines whether the given node is a leaf node, i.e., a node with no
	 * non-null children.
	 * 
	 * @param node a non-null AST node
	 * @return true iff node is a leaf node
	 */
	private boolean isLeaf(ASTNode node) {
		for (ASTNode child : node.children()) {
			if (child != null)
				return false;
		}
		return true;
	}

	/**
	 * Finds the next node u after the given node, in DFS order, which satisfies (1)
	 * u is a leaf node, and (2) u contains "actual" source (i.e., not source
	 * generated by a transformer).
	 * 
	 * @param node any AST node
	 * @return next leaf node whose first token is actual source, or null if there
	 *         is none
	 */
	private ASTNode nextRealNode(ASTNode node) {
		while (true) {
			node = node.nextDFS();
			if (node == null)
				break;
			if (isLeaf(node)) {
				Source source = node.getSource();

				if (source != null) {
					CivlcToken token = source.getFirstToken();

					if (token != null) {
						Formation formation = token.getFormation();

						if (!(formation instanceof TransformFormation))
							break;
					}
				}
			}
		}
		return node;
	}

	/**
	 * <p>
	 * <b>Summary: </b> Returns true if and only if the given {@link BlockItemNode}
	 * node is an assumption statement and the assumed expression involves at least
	 * one of the identifiers.
	 * </p>
	 * 
	 * @param node        The {@link BlockItemNode} node
	 * @param identifiers A set of {@link String} identifiers.
	 * @return
	 */
	protected boolean isRelatedAssumptionNode(BlockItemNode node, List<String> identifiers) {
		StatementNode stmt;
		ExpressionNode expr, function;

		if (node.nodeKind() != NodeKind.STATEMENT)
			return false;
		stmt = (StatementNode) node;
		if (stmt.statementKind() != StatementKind.EXPRESSION)
			return false;
		expr = ((ExpressionStatementNode) stmt).getExpression();
		if (expr.expressionKind() != ExpressionKind.FUNCTION_CALL)
			return false;
		function = ((FunctionCallNode) expr).getFunction();
		// TODO: not deal with calling $assume with function pointers
		if (function.expressionKind() != ExpressionKind.IDENTIFIER_EXPRESSION)
			return false;

		String funcName = ((IdentifierExpressionNode) function).getIdentifier().name();
		if (funcName.equals(ASSUME)) {
			ExpressionNode arg = ((FunctionCallNode) expr).getArgument(0);
			ASTNode next = arg;

			while (next != null) {
				if (next instanceof IdentifierExpressionNode) {
					String nameInArg = ((IdentifierExpressionNode) next).getIdentifier().name();

					for (String identifier : identifiers)
						if (identifier.equals(nameInArg))
							return true;
				}
				next = next.nextDFS();
			}
		}
		return false;
	}

	protected boolean hasHeader(AST ast, String header) {
		for (SourceFile sourceFile : ast.getSourceFiles()) {
			File file = sourceFile.getFile();
			String name = sourceFile.getName();

			if (file.getPath().startsWith(CIVLConstants.ROOT_RESOURCE_PATH_STR) && name.equals(header))
				return true;
		}
		return false;
	}

	protected StringLiteralNode stringLiteral(String string) throws SyntaxException {

		string = "\"" + string + "\"";

		TokenFactory tokenFactory = astFactory.getTokenFactory();
		Formation formation = tokenFactory.newTransformFormation(this.transformerName, "stringLiteral");
		CivlcToken ctoke = tokenFactory.newCivlcToken(CivlcTokenConstant.STRING_LITERAL, string, formation,
				TokenVocabulary.DUMMY);
		StringToken stringToken = tokenFactory.newStringToken(ctoke);

		return nodeFactory.newStringLiteralNode(tokenFactory.newSource(ctoke), string, stringToken.getStringLiteral());
	}

	protected boolean refersInputVariable(ASTNode node) {
		if (node instanceof IdentifierNode) {
			Entity entity = ((IdentifierNode) node).getEntity();

			if (entity.getEntityKind() == EntityKind.VARIABLE) {
				dev.civl.abc.ast.entity.IF.Variable variable = (dev.civl.abc.ast.entity.IF.Variable) entity;

				return ((VariableDeclarationNode) variable.getFirstDeclaration()).getTypeNode().isInputQualified();
			}
		} else {
			for (ASTNode child : node.children()) {
				if (child == null)
					continue;
				if (refersInputVariable(child))
					return true;
			}
		}
		return false;
	}

	/**
	 * returns a boolean expression for checking errors of an expression like
	 * index-out-of-bound, division-by-zero, etc. for example, if expr is a[i]/b[k]
	 * then returns 0<=i && i<N && 0<=k && k<M && b[k]!=0 where N, M is the extent
	 * of the array a and b, respectively.
	 * 
	 * @param expr the target expression
	 * @return a boolean expression for checking errors of the given expression
	 * @throws SyntaxException
	 */
	protected ExpressionNode condition4ErrorChecking(ExpressionNode expr) throws SyntaxException {
		ExpressionNode result = null;
		ExpressionKind kind = expr.expressionKind();
		ExpressionNode condition = null;

		for (ASTNode child : expr.children()) {
			if (child != null && child instanceof ExpressionNode) {
				ExpressionNode subCondition = this.condition4ErrorChecking((ExpressionNode) child);

				if (subCondition != null) {
					if (result == null)
						result = subCondition;
					else
						result = this.nodeFactory.newOperatorNode(subCondition.getSource(), Operator.LAND,
								Arrays.asList(result, subCondition));
				}
			}
		}
		if (kind == ExpressionKind.OPERATOR) {
			OperatorNode operator = (OperatorNode) expr;
			Operator op = operator.getOperator();

			if (op == Operator.MOD || op == Operator.DIV || op == Operator.DIVEQ || op == Operator.MODEQ) {
				condition = this.nodeFactory.newOperatorNode(expr.getSource(), Operator.NEQ,
						Arrays.asList(operator.getArgument(1).copy(), this.integerConstant(0)));
			} else if (op == Operator.SUBSCRIPT) {
				ExpressionNode array = operator.getArgument(0), index = operator.getArgument(1);
				Type type = array.getConvertedType();

				if (type instanceof ArrayType) {
					ArrayType arrayType = (ArrayType) type;
					ExpressionNode extent = arrayType.getVariableSize();

					if (extent != null) {
						condition = this.nodeFactory.newOperatorNode(expr.getSource(), Operator.LAND,
								Arrays.asList(
										nodeFactory.newOperatorNode(expr.getSource(), Operator.LEQ,
												Arrays.asList(this.integerConstant(0), index.copy())),
										nodeFactory.newOperatorNode(expr.getSource(), Operator.LEQ,
												Arrays.asList(index.copy(), extent.copy()))));
					}
				}
			} else if (op == Operator.DEREFERENCE) {
				condition = this.functionCall(operator.getArgument(0).getSource(), DEREFRABLE,
						Arrays.asList(operator.getArgument(0).copy()));
			}
		}
		if (condition != null) {
			if (result == null)
				result = condition;
			else
				result = this.nodeFactory.newOperatorNode(condition.getSource(), Operator.LAND,
						Arrays.asList(result, condition));
		}
		return result;
	}

	/**
	 * Returns the next node (in DFS order) by skipping the whole sub-tree of the
	 * given node.
	 * 
	 * @param node
	 * @return
	 */
	static ASTNode nextDFSSkip(ASTNode node) {
		ASTNode parent = node.parent();
		int childIdx = node.childIndex();

		if (parent == null)
			return null;
		if (parent.numChildren() == childIdx + 1)
			return nextDFSSkip(parent);
		return parent.child(childIdx + 1);
	}
}
