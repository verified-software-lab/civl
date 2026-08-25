package dev.civl.abc.transform.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.IF.ASTException;
import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Entity.EntityKind;
import dev.civl.abc.ast.entity.IF.Typedef;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.acsl.ContractNode;
import dev.civl.abc.ast.node.IF.compound.CompoundInitializerNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.InitializerNode;
import dev.civl.abc.ast.node.IF.declaration.TypedefDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.StringLiteralNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.ExpressionStatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode.StatementKind;
import dev.civl.abc.ast.node.IF.type.EnumerationTypeNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.StructureOrUnionTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode.TypeNodeKind;
import dev.civl.abc.ast.node.IF.type.TypedefNameNode;
import dev.civl.abc.ast.type.IF.Enumerator;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.StructureOrUnionType;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.config.IF.Configurations.Language;
import dev.civl.abc.err.IF.ABCException;
import dev.civl.abc.front.IF.CivlcTokenConstant;
import dev.civl.abc.front.c.parse.COmpParser;
import dev.civl.abc.front.c.parse.CParser;
import dev.civl.abc.front.c.preproc.CPreprocessor;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.Formation;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SourceFile;
import dev.civl.abc.token.IF.StringToken;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;
import dev.civl.abc.token.IF.CivlcToken.TokenVocabulary;
import dev.civl.abc.transform.IF.Combiner;
import dev.civl.abc.transform.IF.Transformer;

/**
 * Combine two ASTs to form a new AST to be used for comparison. The
 * CompareCombiner treats the first argument to combine() as the specification
 * program, and the second argument as the implementation.
 * 
 * @author zirkel
 * 
 */
public class CompareCombiner implements Combiner {

	private final static String MY_NAME = "CompareCombiner";

	private final static String ASSUME = "$assume";

	private final static String NEW_SPEC_MAIN = "$spec_main";

	private final static String NEW_IMPL_MAIN = "$impl_main";

	private final static String SYSTEM_SPEC = "$system_spec";

	private final static String SYSTEM_IMPL = "$system_impl";

	/**
	 * The node factory that creates new AST nodes.
	 */
	private NodeFactory nodeFactory;

	/**
	 * The AST factory that creates new AST's.
	 */
	private ASTFactory astFactory;

	/**
	 * The token factory that creates tokens.
	 */
	private TokenFactory tokenFactory;

	/**
	 * The source of the specification.
	 */
	private Source specSource;

	/**
	 * The source of the implementation.
	 */
	private Source implSource;

	private final static String ASSERT_EQUALS = "$assert_equals";

	@Override
	public AST combine(AST spec, AST impl) throws SyntaxException {
		SequenceNode<BlockItemNode> specRoot = spec.getRootNode();
		SequenceNode<BlockItemNode> implRoot = impl.getRootNode();
		SequenceNode<BlockItemNode> newRoot;
		List<BlockItemNode> inputVariablesAndAssumes;
		Map<String, VariableDeclarationNode> specOutputs;
		Map<String, VariableDeclarationNode> implOutputs;
		FunctionDefinitionNode specSystem;
		FunctionDefinitionNode implSystem;
		FunctionDefinitionNode newMain;
		List<BlockItemNode> newMainBodyNodes = new ArrayList<BlockItemNode>();
		List<BlockItemNode> nodes = new ArrayList<BlockItemNode>();
		Transformer nameTransformer;
		TypedefDeclarationNode specFileTypeDef = null, implFileTypeDef = null;
		FunctionDeclarationNode equalsFunc = null;
		Collection<SourceFile> sourceFiles0 = spec.getSourceFiles(),
				sourceFiles1 = impl.getSourceFiles(),
				allSourceFiles = new LinkedHashSet<>();

		allSourceFiles.addAll(sourceFiles0);
		allSourceFiles.addAll(sourceFiles1);
		astFactory = spec.getASTFactory();
		tokenFactory = astFactory.getTokenFactory();
		nodeFactory = astFactory.getNodeFactory();
		spec.release();
		impl.release();
		specFileTypeDef = this.getAndRemoveFileTypeNode(specRoot);
		implFileTypeDef = this.getAndRemoveFileTypeNode(implRoot);
		processVariableDeclarations(specRoot);
		processVariableDeclarations(implRoot);
		if (specFileTypeDef != null)
			nodes.add(specFileTypeDef);
		else if (implFileTypeDef != null)
			nodes.add(implFileTypeDef);
		equalsFunc = this.getAndRemoveEqualsFuncNode(specRoot);
		nodes.add(equalsFunc);
		spec = astFactory.newAST(specRoot, sourceFiles0, spec.isWholeProgram());
		impl = astFactory.newAST(implRoot, sourceFiles1, impl.isWholeProgram());

		specSource = this.getMainSource(specRoot);
		implSource = this.getMainSource(implRoot);
		inputVariablesAndAssumes = combineInputs(specRoot, implRoot);
		nodes.add(this.assertFunctionNode(specSource));
		nodes.add(definedFunctionNode(specSource));
		nodes.add(assertEquals(specSource));
		nodes.addAll(inputVariablesAndAssumes);
		specOutputs = getOutputs(specRoot);
		implOutputs = getOutputs(implRoot);
		checkOutputs(specOutputs, implOutputs);
		// systemFunctions = combineSystemFunctions(specRoot, implRoot);
		// nodes.addAll(systemFunctions.values());
		nameTransformer = new CommonNameTransformer(
				renameVariables(specOutputs.values(), "_spec"), astFactory);
		spec = nameTransformer.transform(spec);
		// TODO: Check consistency of assumptions
		spec.release();
		impl.release();

		List<BlockItemNode> dependencies = removeInputOutPutDependencies(
				implRoot);

		removeInputOutPutDependencies(specRoot);
		nodes.addAll(0, dependencies);
		specSystem = wrapperFunction(specSource, specRoot, SYSTEM_SPEC);
		implSystem = wrapperFunction(implSource, implRoot, SYSTEM_IMPL);
		for (VariableDeclarationNode v : specOutputs.values()) {
			v.getTypeNode().setOutputQualified(false);
			nodes.add(v.copy());
		}
		for (VariableDeclarationNode v : implOutputs.values()) {
			v.getTypeNode().setOutputQualified(false);
			nodes.add(v.copy());
		}
		nodes.add(specSystem);
		nodes.add(implSystem);
		newMainBodyNodes.add(nodeFactory.newVariableDeclarationNode(specSource,
				nodeFactory.newIdentifierNode(specSource, "_isEqual"),
				nodeFactory.newBasicTypeNode(specSource, BasicTypeKind.BOOL)));

		newMainBodyNodes.add(nodeFactory.newExpressionStatementNode(
				nodeFactory.newFunctionCallNode(specSource,
						nodeFactory.newIdentifierExpressionNode(specSource,
								nodeFactory.newIdentifierNode(specSource,
										SYSTEM_SPEC)),
						new ArrayList<ExpressionNode>())));
		newMainBodyNodes.add(nodeFactory.newExpressionStatementNode(
				nodeFactory.newFunctionCallNode(implSource,
						nodeFactory.newIdentifierExpressionNode(implSource,
								nodeFactory.newIdentifierNode(implSource,
										SYSTEM_IMPL)),
						new ArrayList<ExpressionNode>())));
		// TODO: spawn and join (but calls ok until joint assertions
		// implemented)
		newMainBodyNodes.addAll(outputAssertions(specOutputs, implOutputs));
		newMain = nodeFactory.newFunctionDefinitionNode(specSource,
				nodeFactory.newIdentifierNode(specSource, "main"),
				nodeFactory.newFunctionTypeNode(specSource,
						nodeFactory.newVoidTypeNode(specSource),
						nodeFactory.newSequenceNode(specSource, "Formals",
								new ArrayList<VariableDeclarationNode>()),
						false),
				nodeFactory.newSequenceNode(specSource, "Contract",
						new ArrayList<ContractNode>()),
				nodeFactory.newCompoundStatementNode(specSource,
						newMainBodyNodes));
		nodes.add(newMain);
		newRoot = nodeFactory.newSequenceNode(
				astFactory.getTokenFactory().join(specSource, implSource),
				"Composite System", nodes);
		AST result = astFactory.newAST(newRoot, allSourceFiles, true);
		return result;
	}

	private FunctionDeclarationNode assertFunctionNode(Source specSource) {
		IdentifierNode name = nodeFactory.newIdentifierNode(specSource,
				"$assert");

		FunctionTypeNode funcType = nodeFactory.newFunctionTypeNode(specSource,
				nodeFactory.newVoidTypeNode(specSource),
				nodeFactory.newSequenceNode(specSource, "Formals",
						Arrays.asList(nodeFactory.newVariableDeclarationNode(
								specSource,
								nodeFactory.newIdentifierNode(specSource,
										"expression"),
								nodeFactory.newBasicTypeNode(specSource,
										BasicTypeKind.BOOL))))

				, false);

		funcType.setVariableArgs(true);

		FunctionDeclarationNode function = nodeFactory
				.newFunctionDeclarationNode(specSource, name, funcType, null);

		function.setSystemFunctionSpecifier(true);
		return function;
	}

	/**
	 * Find all the type definitions, struct definitions, and union definitions
	 * that input and output depend on. Removed those definitions from the ast
	 * and return them.
	 * 
	 * @param root
	 *            The root of an AST.
	 * @return A list of ast nodes that input and output depend on.
	 */
	private List<BlockItemNode> removeInputOutPutDependencies(ASTNode root) {
		Map<ASTNode, Integer> selfDefinedTypesIndexMap = new HashMap<>();
		List<ASTNode> selfDefinedTypes = getSelfDefinedTypes(root,
				selfDefinedTypesIndexMap);
		int selfDefinedTypeSize = selfDefinedTypes.size();
		Map<String, VariableDeclarationNode> input = getInputs(root);
		Map<String, VariableDeclarationNode> output = getOutputs(root);
		BitSet record = new BitSet(selfDefinedTypeSize);

		for (VariableDeclarationNode var : output.values()) {
			findStructUnionEnumerationThatInputAndOutDependOn(
					selfDefinedTypesIndexMap, var, record);
		}
		for (VariableDeclarationNode var : input.values()) {
			findStructUnionEnumerationThatInputAndOutDependOn(
					selfDefinedTypesIndexMap, var, record);
		}

		List<BlockItemNode> dependencies = new LinkedList<>();

		for (int i = record.nextSetBit(0); i >= 0; i = record
				.nextSetBit(i + 1)) {
			ASTNode dependentNode = selfDefinedTypes.get(i);

			dependentNode.remove();
			dependencies.add((BlockItemNode) dependentNode);
			// operate on index i here
			if (i == selfDefinedTypeSize) {
				break; // or (i+1) would overflow
			}
		}
		return dependencies;
	}

	/**
	 * Construct the function declaration of the system function $equals.
	 * 
	 * @param specSource
	 * @return
	 */
	private FunctionDeclarationNode assertEquals(Source specSource) {
		try {
			AST pointerLibAST = astFactory.getASTofLibrary(
					new File(CPreprocessor.ABC_INCLUDE_PATH, "pointer.cvh"),
					Language.C);
			SequenceNode<BlockItemNode> root = pointerLibAST.getRootNode();

			pointerLibAST.release();
			for (BlockItemNode item : root) {
				if (item instanceof FunctionDeclarationNode) {
					FunctionDeclarationNode function = (FunctionDeclarationNode) item;

					if (function.getName().equals(ASSERT_EQUALS)) {
						function.remove();
						return function;
					}
				}
			}
		} catch (ABCException e) {
		}
		return null;
	}

	/**
	 * Construct the function declaration of the system function $defined.
	 * 
	 * @param specSource
	 * @return
	 */
	private FunctionDeclarationNode definedFunctionNode(Source specSource) {
		IdentifierNode name = nodeFactory.newIdentifierNode(specSource,
				"$defined");
		FunctionTypeNode funcType = nodeFactory.newFunctionTypeNode(specSource,
				nodeFactory.newBasicTypeNode(specSource, BasicTypeKind.BOOL),
				nodeFactory.newSequenceNode(specSource, "Formals",
						Arrays.asList(nodeFactory.newVariableDeclarationNode(
								specSource,
								nodeFactory.newIdentifierNode(specSource,
										"obj"),
								nodeFactory.newPointerTypeNode(specSource,
										nodeFactory.newVoidTypeNode(
												specSource))))),
				false);
		FunctionDeclarationNode function = nodeFactory
				.newFunctionDeclarationNode(specSource, name, funcType, null);

		function.setSystemFunctionSpecifier(true);
		return function;
	}

	/**
	 * Finds the $file type declaration node from an AST, returns it and remove
	 * it from the AST if it is found. $file type declaration node is required
	 * by the output variable CIVL_filesystem (array-of-$file type) (if IO
	 * transformer has been applied). So we need to move it to the final file
	 * scope.
	 * 
	 * @param root
	 *            The root node of the AST
	 * @return the $file type declaration node, or null if it absent from the
	 *         AST.
	 */
	private TypedefDeclarationNode getAndRemoveFileTypeNode(
			SequenceNode<BlockItemNode> root) {
		int numChildren = root.numChildren();

		for (int i = 0; i < numChildren; i++) {
			BlockItemNode def = root.getSequenceChild(i);

			if (def != null && def instanceof TypedefDeclarationNode) {
				TypedefDeclarationNode typeDefNode = (TypedefDeclarationNode) def;
				String sourceFile = typeDefNode.getSource().getFirstToken()
						.getSourceFile().getName();

				if (sourceFile.equals("stdio.cvl")
						&& typeDefNode.getName().equals("$file")) {
					typeDefNode.parent().removeChild(typeDefNode.childIndex());
					return typeDefNode;
				}
			}
		}
		return null;
	}

	/**
	 * Finds the $equals function declaration node from an AST, returns it and
	 * remove it from the AST if it is found.
	 * 
	 * @param root
	 *            The root node of the AST
	 * @return the $equals function declaration node, or null if it absent from
	 *         the AST.
	 */
	private FunctionDeclarationNode getAndRemoveEqualsFuncNode(
			SequenceNode<BlockItemNode> root) {
		int numChildren = root.numChildren();

		for (int i = 0; i < numChildren; i++) {
			BlockItemNode def = root.getSequenceChild(i);

			if (def != null && def instanceof FunctionDeclarationNode) {
				FunctionDeclarationNode funcDec = (FunctionDeclarationNode) def;
				String sourceFile = funcDec.getSource().getFirstToken()
						.getSourceFile().getName();

				if (sourceFile.equals("pointer.cvh")
						&& funcDec.getName().equals("$equals")) {
					funcDec.parent().removeChild(funcDec.childIndex());
					return funcDec;
				}
			}
		}
		return null;
	}

	/**
	 * <p>
	 * <b>Summary :</b> Take the input variable declaration nodes and related
	 * assumption call nodes from the specification and the implementation into
	 * a list. The input variables of the specification should be a subset of
	 * those of the implementation. Here related assumption call nodes are nodes
	 * representing assumptions whose predicate involves at least one seen input
	 * variables.
	 * </p>
	 * 
	 * 
	 * @param specRoot
	 *            The root node of the specification AST.
	 * @param implRoot
	 *            The root node of the implementation AST.
	 * @return The combined list of input variable declaration nodes and
	 *         assumption call nodes
	 */
	private List<BlockItemNode> combineInputs(ASTNode specRoot,
			ASTNode implRoot) {
		List<BlockItemNode> inputsAndAssumes = new LinkedList<BlockItemNode>();
		Set<String> seenVariableIdentifiers = new HashSet<>();
		boolean existsAssumeDecl = false;

		existsAssumeDecl = combineInputsWorker(implRoot, inputsAndAssumes,
				seenVariableIdentifiers, existsAssumeDecl);
		combineInputsWorker(specRoot, inputsAndAssumes, seenVariableIdentifiers,
				existsAssumeDecl);
		return inputsAndAssumes;
	}

	/**
	 * <p>
	 * <b>Summary: </b> A helper method for the
	 * {@link #combineInputs(ASTNode, ASTNode)}. This method takes the result
	 * collection "inputsAndAssumes" and a seen variable set, continue to add
	 * input variable declaration nodes and related assumption nodes in "root"
	 * into the collection.
	 * </p>
	 * 
	 * @param root
	 *            The root node of a given tree which is searched for input
	 *            variable declaration and assume nodes
	 * @param inputsAndAssumes
	 *            The result collection contains variable declaration and assume
	 *            nodes
	 * @param seenVariableIdentifiers
	 *            A seen variable identifier set, used to determine if a assume
	 *            node is a related assume node
	 * 
	 * @param existsAssumeDecl
	 *            Informs this method if it is necessary to insert an
	 *            <code>$assume</code> declaration when reaches a related assume
	 *            node.
	 */
	private boolean combineInputsWorker(ASTNode root,
			List<BlockItemNode> inputsAndAssumes,
			Set<String> seenVariableIdentifiers, boolean existsAssumeDecl) {
		boolean ret = existsAssumeDecl;

		for (int i = 0; i < root.numChildren(); i++) {
			ASTNode child = root.child(i);

			if (child instanceof BlockItemNode) {
				BlockItemNode castedChild = (BlockItemNode) child;

				if (isRelatedAssumptionNode(castedChild,
						seenVariableIdentifiers)) {
					if (!existsAssumeDecl) {
						inputsAndAssumes.add(assumeFunctionDeclaration(
								castedChild.getSource()));
						ret = true;
					}
					inputsAndAssumes.add(castedChild.copy());
					continue;
				}
			}
			if (child != null
					&& child.nodeKind() == NodeKind.VARIABLE_DECLARATION) {
				VariableDeclarationNode var = (VariableDeclarationNode) child;

				if (var.getTypeNode().isInputQualified()) {
					if (seenVariableIdentifiers.add(var.getName()))
						inputsAndAssumes.add(var.copy());
				}
			}
		}
		return ret;
	}

	/**
	 * Return the input variables of the AST
	 * 
	 * @param root
	 *            The root node of the AST
	 * @return the input variables of the AST, where key is the name of the
	 *         variable.
	 */
	private Map<String, VariableDeclarationNode> getInputs(ASTNode root) {
		Map<String, VariableDeclarationNode> inputs = new LinkedHashMap<String, VariableDeclarationNode>();
		int childNum = root.numChildren();

		// put all input variables into the map.
		for (int i = 0; i < childNum; i++) {
			ASTNode child = root.child(i);

			if (child != null
					&& child.nodeKind() == NodeKind.VARIABLE_DECLARATION) {
				VariableDeclarationNode var = (VariableDeclarationNode) child;

				if (var.getTypeNode().isInputQualified()) {
					inputs.put(var.getName(), var);
				}
			}
		}
		return inputs;
	}

	/**
	 * Returns the output variables of the AST.
	 * 
	 * @param root
	 *            The root node of the AST.
	 * @return the output variables of the AST, where key is name of variable.
	 */
	private Map<String, VariableDeclarationNode> getOutputs(ASTNode root) {
		Map<String, VariableDeclarationNode> outputs = new LinkedHashMap<String, VariableDeclarationNode>();

		// Put all output variables into the map.
		for (int i = 0; i < root.numChildren(); i++) {
			ASTNode child = root.child(i);

			if (child != null
					&& child.nodeKind() == NodeKind.VARIABLE_DECLARATION) {
				VariableDeclarationNode var = (VariableDeclarationNode) child;

				if (var.getTypeNode().isOutputQualified()) {
					outputs.put(var.getName(), var);
				}
			}
		}
		return outputs;
	}

	/**
	 * Get the {@link TypedefDeclarationNode}s, struct definition(
	 * {@link TypeNode}) and union definition ({@link TypeNode}).
	 * 
	 * @param root
	 *            The root node of specification and implementation.
	 * @return a list of {@link TypedefDeclarationNode}.
	 */
	private List<ASTNode> getSelfDefinedTypes(ASTNode root,
			Map<ASTNode, Integer> typesMap) {
		List<ASTNode> selfDefinedTypes = new ArrayList<>();
		int numChildren = root.numChildren();
		int index = 0;

		for (int i = 0; i < numChildren; i++) {
			ASTNode child = root.child(i);

			if (child != null) {
				NodeKind nodeKind = child.nodeKind();

				if (nodeKind == NodeKind.TYPEDEF) {
					selfDefinedTypes.add(child);
					typesMap.put(child, index++);
				} else if (nodeKind == NodeKind.TYPE) {
					TypeNode typeNode = (TypeNode) child;

					if (typeNode instanceof StructureOrUnionTypeNode
							|| typeNode instanceof EnumerationTypeNode) {
						selfDefinedTypes.add(child);
						typesMap.put(child, index++);
					}
				}
			}
		}
		return selfDefinedTypes;
	}

	/**
	 * Find struct, union, and enumeration that an input or output depends on.
	 * 
	 * @param typeMap
	 *            A map that maps a struct, union or enumeration into an index.
	 * @param typeNode
	 */
	private void findStructUnionEnumerationThatInputAndOutDependOn(
			Map<ASTNode, Integer> typeMap, ASTNode astNode, BitSet record) {
		if (astNode == null)
			return;

		if (astNode.nodeKind() == NodeKind.TYPE) {
			TypeNode typeNode = (TypeNode) astNode;
			TypeNodeKind typeNodeKind = typeNode.kind();

			switch (typeNodeKind) {
				case ENUMERATION : {
					EnumerationTypeNode enumerationTypeNode = (EnumerationTypeNode) typeNode;
					Entity enumerationTypeNodeEntity = enumerationTypeNode
							.getEntity();

					if (enumerationTypeNodeEntity
							.getEntityKind() == EntityKind.ENUMERATION) {
						Enumerator enumerator = (Enumerator) enumerationTypeNodeEntity;
						ASTNode enumeratorDefinition = enumerator
								.getDefinition();
						Integer index = typeMap.get(enumeratorDefinition);

						if (index != null)
							record.set(index);
					}
					return;
				}
				case STRUCTURE_OR_UNION : {
					StructureOrUnionTypeNode structureOrUnionTypeNode = (StructureOrUnionTypeNode) typeNode;
					Entity structureOrUnionTypeNodeEntity = structureOrUnionTypeNode
							.getEntity();

					if (structureOrUnionTypeNodeEntity
							.getEntityKind() == EntityKind.STRUCTURE_OR_UNION) {
						StructureOrUnionType structureOrUnionType = (StructureOrUnionType) structureOrUnionTypeNodeEntity;
						ASTNode structureOrUnionDefinition = structureOrUnionType
								.getDefinition();
						Integer index = typeMap.get(structureOrUnionDefinition);

						if (index != null)
							record.set(index);
					}
					return;
				}
				case TYPEDEF_NAME : {
					TypedefNameNode typedefNameNode = (TypedefNameNode) typeNode;
					Entity typedefNameNodeEntity = typedefNameNode.getName()
							.getEntity();

					if (typedefNameNodeEntity
							.getEntityKind() == EntityKind.TYPEDEF) {
						Typedef typedef = (Typedef) typedefNameNodeEntity;
						ASTNode typedefDefinition = typedef.getDefinition();
						Integer index = typeMap.get(typedefDefinition);

						if (index != null)
							record.set(index);
					}
					return;
				}
				default :
			}
		}
		for (ASTNode child : astNode.children()) {
			findStructUnionEnumerationThatInputAndOutDependOn(typeMap, child,
					record);
		}
	}

	/**
	 * Checks if the output variables of the specification and the
	 * implementation satisfy the precondition that all the output variables of
	 * the specification should also be the output variables of the
	 * implementation. The output variable CIVL_filesystem which is introduced
	 * by IO transformer is an exception, i.e., it is fine for the specification
	 * to contain CIVL_filesystem while the implementation doesn't.
	 * 
	 * @param specOutputs
	 *            The output variables of the
	 * @param implOutputs
	 */
	private void checkOutputs(Map<String, VariableDeclarationNode> specOutputs,
			Map<String, VariableDeclarationNode> implOutputs) {
		for (String name : specOutputs.keySet()) {
			// allow CIVL_output_filesystem to exist in only one program
			// TODO better solution for IO transformer
			if (name.equals("CIVL_output_filesystem"))
				continue;
			if (!implOutputs.containsKey(name)) {
				throw new ASTException(
						"No implementation output variable matching specification outputs variable "
								+ name + ".");
			}
		}
	}

	/**
	 * Given an AST, remove input and output variables and create a new function
	 * wrapping the remaining file scope items.
	 * 
	 * @param root
	 *            The root node of the AST being wrapped in a new function.
	 * @param name
	 *            The name of the new function.
	 * @return A function definition node with the appropriate name, void return
	 *         type, and no parameters.
	 */
	private FunctionDefinitionNode wrapperFunction(Source source, ASTNode root,
			String name) {
		FunctionTypeNode functionType = nodeFactory
				.newFunctionTypeNode(source,
						nodeFactory.newBasicTypeNode(source, BasicTypeKind.INT),
						nodeFactory.newSequenceNode(source, "Formals",
								new ArrayList<VariableDeclarationNode>()),
						false);
		List<BlockItemNode> items = new ArrayList<BlockItemNode>();
		FunctionDefinitionNode oldMain = null;
		CompoundStatementNode body;
		FunctionDefinitionNode result;

		for (int i = 0; i < root.numChildren(); i++) {
			ASTNode child = root.child(i);

			if (child == null)
				continue;
			if (child.nodeKind() == NodeKind.VARIABLE_DECLARATION) {
				VariableDeclarationNode var = (VariableDeclarationNode) child;

				if (!var.getTypeNode().isInputQualified()
						&& !var.getTypeNode().isOutputQualified()) {
					items.add(var.copy());
				}
			} else if (child.nodeKind() == NodeKind.FUNCTION_DECLARATION) {
				FunctionDeclarationNode function = (FunctionDeclarationNode) child;

				items.add(function.copy());
			} else if (child.nodeKind() == NodeKind.FUNCTION_DEFINITION) {
				FunctionDefinitionNode function = (FunctionDefinitionNode) child;
				if (function.getName() != null
						&& function.getName().equals("main")) {
					// convert main function into a function with another name.
					items.add(oldMain = (name.equals(SYSTEM_SPEC)
							? constructNewMainFunction(function, NEW_SPEC_MAIN)
							: constructNewMainFunction(function,
									NEW_IMPL_MAIN)));
				} else {
					items.add(function.copy());
				}
			} else {
				assert child instanceof BlockItemNode;
				items.add((BlockItemNode) child.copy());
			}
		}
		// spec and impl should both have main functions.
		assert oldMain != null;
		items.add(nodeFactory.newExpressionStatementNode(
				constructFunctionCallNode(oldMain)));
		body = nodeFactory.newCompoundStatementNode(source, items);
		result = nodeFactory
				.newFunctionDefinitionNode(source,
						nodeFactory.newIdentifierNode(root.getSource(), name),
						functionType, nodeFactory.newSequenceNode(source,
								"Contract", new ArrayList<ContractNode>()),
						body);
		return result;
	}

	/**
	 * Construct a function call node from a function definition node.
	 * 
	 * @param functionDefinition
	 * 
	 * @return The corresponding function definition node.
	 */
	private FunctionCallNode constructFunctionCallNode(
			FunctionDefinitionNode functionDefinition) {
		String functionName = functionDefinition.getName();
		Source source = this.newSource(functionName + "()",
				CivlcTokenConstant.CALL);
		ExpressionNode functionExpression = nodeFactory
				.newIdentifierExpressionNode(
						functionDefinition.getIdentifier().getSource(),
						functionDefinition.getIdentifier().copy());

		return nodeFactory.newFunctionCallNode(source, functionExpression,
				new LinkedList<>());
	}

	/**
	 * Convert original main function into a function with another name.
	 * 
	 * @param mainFunction
	 *            The original main function.
	 * @param name
	 *            {@link #SYSTEM_IMPL} or {@link #SYSTEM_SPEC}
	 * @return the converted main function.
	 */
	private FunctionDefinitionNode constructNewMainFunction(
			FunctionDefinitionNode mainFunction, String name) {
		Source newSource = mainFunction.getSource();
		CompoundStatementNode newBody = mainFunction.getBody();
		FunctionTypeNode newFunctionType = mainFunction.getTypeNode();
		IdentifierNode newIdentifier = nodeFactory.newIdentifierNode(
				mainFunction.getIdentifier().getSource(), name);

		newBody.remove();
		newFunctionType.remove();

		return nodeFactory.newFunctionDefinitionNode(newSource, newIdentifier,
				newFunctionType, null, newBody);
	}

	/**
	 * Generate the set of assertions comparing output variables. We assume that
	 * checkOutputs has been called, so the sets are of equal cardinality and
	 * the names correspond. Furthermore, we assume that name substitutions have
	 * happened, but that the String key values in the argument maps are the old
	 * names.
	 * 
	 * @param specOutputs
	 *            Mapping from original output variable name to the
	 *            corresponding variable declaration from the spec program.
	 * @param implOutputs
	 *            Mapping from original output variable name to the
	 *            corresponding variable declaration from the impl program.
	 * @return A list of assertion statements checking equivalence of
	 *         corresponding output variables.
	 * @throws SyntaxException
	 */
	private List<StatementNode> outputAssertions(
			Map<String, VariableDeclarationNode> specOutputs,
			Map<String, VariableDeclarationNode> implOutputs)
			throws SyntaxException {
		List<StatementNode> result = new ArrayList<StatementNode>();
		// TODO: do something better for source
		// ExpressionNode equalFunction = factory.newIdentifierExpressionNode(
		// specSource, factory.newIdentifierNode(specSource, "$equals"));
		ExpressionNode assertEqualFunction = nodeFactory
				.newIdentifierExpressionNode(specSource, nodeFactory
						.newIdentifierNode(specSource, ASSERT_EQUALS));

		for (String outputName : specOutputs.keySet()) {
			Source source = specOutputs.get(outputName).getSource();
			List<ExpressionNode> args = new ArrayList<ExpressionNode>();
			FunctionCallNode assertEqualCall;
			VariableDeclarationNode specOutput = specOutputs.get(outputName);
			VariableDeclarationNode implOutput = implOutputs.get(outputName);
			String message;

			// don't compare outputs if only one program has output
			// CIVL_output_system
			// TODO better solution from IO transformer
			if (outputName.equals("CIVL_output_filesystem")
					&& (specOutput == null || implOutput == null))
				continue;
			message = "\"Specification and implementation have"
					+ " different values for the output " + outputName + "!\"";
			args.add(nodeFactory.newOperatorNode(specOutput.getSource(),
					Operator.ADDRESSOF,
					Arrays.asList((ExpressionNode) nodeFactory
							.newIdentifierExpressionNode(specOutput.getSource(),
									specOutput.getIdentifier().copy()))));
			args.add(nodeFactory.newOperatorNode(implOutput.getSource(),
					Operator.ADDRESSOF,
					Arrays.asList((ExpressionNode) nodeFactory
							.newIdentifierExpressionNode(implOutput.getSource(),
									implOutput.getIdentifier().copy()))));
			args.add(this.createStringLiteral(message));
			assertEqualCall = nodeFactory.newFunctionCallNode(source,
					assertEqualFunction.copy(), args);
			result.add(nodeFactory.newExpressionStatementNode(assertEqualCall));
		}
		return result;
	}

	/**
	 * Create a mapping from Entity to String where the entities are variables
	 * and the strings are those variable names with a suffix added.
	 * 
	 * @param variables
	 *            A set of variable declarations.
	 * @param suffix
	 *            The suffix to be added to each variable name.
	 * @return The mapping from entities to their new names.
	 */
	private Map<Entity, String> renameVariables(
			Collection<VariableDeclarationNode> variables, String suffix) {
		Map<Entity, String> result = new LinkedHashMap<Entity, String>();

		for (VariableDeclarationNode var : variables) {
			result.put(var.getEntity(), var.getName() + suffix);
		}
		return result;
	}

	private Source getMainSource(ASTNode node) {
		if (node.nodeKind() == NodeKind.FUNCTION_DEFINITION) {
			FunctionDefinitionNode functionNode = (FunctionDefinitionNode) node;
			IdentifierNode functionName = (IdentifierNode) functionNode
					.child(0);

			if (functionName.name().equals("main")) {
				return node.getSource();
			}
		}
		for (ASTNode child : node.children()) {
			if (child == null)
				continue;
			else {
				Source childResult = getMainSource(child);

				if (childResult != null)
					return childResult;
			}
		}
		return null;
	}

	private StringLiteralNode createStringLiteral(String text)
			throws SyntaxException {
		TokenFactory tokenFactory = astFactory.getTokenFactory();
		Formation formation = tokenFactory.newTransformFormation(MY_NAME,
				"stringLiteral");
		CivlcToken ctoke = tokenFactory.newCivlcToken(
				CivlcTokenConstant.STRING_LITERAL, text, formation,
				TokenVocabulary.DUMMY);
		StringToken stringToken = tokenFactory.newStringToken(ctoke);

		return nodeFactory.newStringLiteralNode(tokenFactory.newSource(ctoke),
				text, stringToken.getStringLiteral());
	}

	/**
	 * <p>
	 * Process the declaration of variables. Since each translation unit will
	 * become a function, declaration in the global/file scope will become
	 * declaration in a function scope. While re-declaration in file scope is
	 * allowed (N1570 6.9.2), re-declaration is not allowed in function scope.
	 * </p>
	 * 
	 * <p>
	 * <ul>
	 * <li>Remove <code>extern</code> decorator for all declarations</li>
	 * <li>For array declarations of identifier var_arr:
	 * <ul>
	 * <li>if there exists an declaration with explicit initializer of var_arr,
	 * this declaration will be moved to the location where var_arr is first
	 * declared, and also the rest declaration of var_arr will be removed.</li>
	 * <li>if there doesn't exist an declaration of var_arr with explicit
	 * initializer, the first declaration will be kept, the rest will be
	 * removed.</li>
	 * </ul>
	 * </li>
	 * <li>For declarations of a variable var, the first declaration will be
	 * kept. The rest declaration without definition will be removed, the one
	 * with definition will converted into an assignment.</li>
	 * </ul>
	 * </p>
	 * 
	 * @param root
	 *            The root node of this translation unit.
	 */
	private void processVariableDeclarations(SequenceNode<BlockItemNode> root) {
		Map<String, Integer> identifierCache = new HashMap<>();
		int numChildern = root.numChildren();

		for (int i = 0; i < numChildern; i++) {
			BlockItemNode item = (BlockItemNode) root.child(i);

			if (item == null)
				continue;
			if (item instanceof VariableDeclarationNode) {
				VariableDeclarationNode variableDecl = (VariableDeclarationNode) item;
				String identifier = variableDecl.getName();

				if (variableDecl.hasExternStorage())
					variableDecl.setExternStorage(false);
				if (variableDecl.getEntity().getDefinition()
						.equals(variableDecl)) {
					// definition
					if (identifierCache.containsKey(identifier)) {
						if (variableDecl.getTypeNode().getType()
								.kind() == TypeKind.ARRAY) {
							int index = identifierCache.get(identifier);

							variableDecl.remove();
							root.setChild(index, variableDecl);
						} else
							// change to a assignment
							root.setChild(i,
									convertVariableDefinitionToVaribleAssignment(
											variableDecl));
					} else
						identifierCache.put(identifier, i);
				} else {
					// just declaration
					if (identifierCache.containsKey(identifier))
						variableDecl.remove();
					else
						identifierCache.put(identifier, i);
				}
			}
		}
	}

	/**
	 * Construct an assignment expression statement from variable declaration
	 * node.
	 * 
	 * @param variableDecl
	 *            The given declaration node.
	 * @return the converted assignment statement node.
	 */
	private ExpressionStatementNode convertVariableDefinitionToVaribleAssignment(
			VariableDeclarationNode variableDecl) {
		Source variableDeclSource = variableDecl.getSource();
		IdentifierNode identifierNode = variableDecl.getIdentifier();
		Source identifierSource = identifierNode.getSource();
		InitializerNode initializerNode = variableDecl.getInitializer();
		TypeNode typeNode = variableDecl.getTypeNode();

		identifierNode.remove();
		initializerNode.remove();
		typeNode.remove();

		IdentifierExpressionNode identifierExpression = nodeFactory
				.newIdentifierExpressionNode(identifierSource, identifierNode);
		assert initializerNode != null;
		// TODO for InitializerNode, if it is not just scalar
		// (stuct union array).

		ExpressionNode righHandSide = initializerNode instanceof CompoundInitializerNode
				? nodeFactory.newCompoundLiteralNode(
						initializerNode.getSource(), typeNode,
						(CompoundInitializerNode) initializerNode)
				: (ExpressionNode) initializerNode;

		if (initializerNode instanceof CompoundInitializerNode) {
			return nodeFactory.newExpressionStatementNode(nodeFactory
					.newOperatorNode(variableDeclSource, Operator.ASSIGN,
							identifierExpression, righHandSide));
		} else {
			return nodeFactory.newExpressionStatementNode(nodeFactory
					.newOperatorNode(variableDeclSource, Operator.ASSIGN,
							identifierExpression, righHandSide));
		}
	}

	/**
	 * <p>
	 * <b>Summary: </b> Returns true if and only if the given
	 * {@link BlockItemNode} node is an assumption statement and the assumed
	 * expression involves at least one of the identifiers.
	 * </p>
	 * 
	 * @param node
	 *            The {@link BlockItemNode} node
	 * @param identifiers
	 *            A set of {@link String} identifiers.
	 * @return
	 */
	private boolean isRelatedAssumptionNode(BlockItemNode node,
			Set<String> identifiers) {
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

		String funcName = ((IdentifierExpressionNode) function).getIdentifier()
				.name();
		if (funcName.equals(ASSUME)) {
			ExpressionNode arg = ((FunctionCallNode) expr).getArgument(0);
			ASTNode next = arg;

			while (next != null) {
				if (next instanceof IdentifierExpressionNode) {
					String nameInArg = ((IdentifierExpressionNode) next)
							.getIdentifier().name();

					return identifiers.contains(nameInArg);
				}
				next = next.nextDFS();
			}
		}
		return false;
	}

	/**
	 * <p>
	 * <b>Summary: </b> Create an <code>$assume</code> function declaration node
	 * </p>
	 * 
	 * @param source
	 *            The {@link Source} attached with the created node.
	 * @return A new node which represents an <code>$assume</code> function
	 *         declaration
	 */
	private FunctionDeclarationNode assumeFunctionDeclaration(Source source) {
		IdentifierNode name = nodeFactory.newIdentifierNode(source, "$assume");
		FunctionTypeNode funcType = nodeFactory.newFunctionTypeNode(source,
				nodeFactory.newVoidTypeNode(source),
				nodeFactory.newSequenceNode(source, "Formals",
						Arrays.asList(
								nodeFactory.newVariableDeclarationNode(source,
										nodeFactory.newIdentifierNode(source,
												"expression"),
										nodeFactory.newBasicTypeNode(source,
												BasicTypeKind.BOOL))))

				, false);
		FunctionDeclarationNode function = nodeFactory
				.newFunctionDeclarationNode(source, name, funcType, null);

		function.setSystemFunctionSpecifier(true);
		return function;
	}

	/**
	 * Creates a new {@link Source} object to associate to AST nodes that are
	 * invented by this compare combiner.
	 * 
	 * @param method
	 *            any string which identifies the specific part of this combiner
	 *            responsible for creating the new content; typically, the name
	 *            of the method that created the new context. This will appear
	 *            in error message to help isolate the source of the new
	 *            content.
	 * @param tokenType
	 *            the integer code for the type of the token used to represent
	 *            the source; use one of the constants in {@link CParser} or
	 *            {@link COmpParser}, for example, such as
	 *            {@link CParser#IDENTIFIER}.
	 * @return the new source object
	 */
	protected Source newSource(String method, int tokenType) {
		Formation formation = tokenFactory
				.newTransformFormation("compare combiner", method);
		CivlcToken token = tokenFactory.newCivlcToken(tokenType,
				"inserted text", formation, TokenVocabulary.DUMMY);
		Source source = tokenFactory.newSource(token);

		return source;
	}
}
