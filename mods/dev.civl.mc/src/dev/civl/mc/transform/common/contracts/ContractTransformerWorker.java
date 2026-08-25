package dev.civl.mc.transform.common.contracts;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.CastNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.front.IF.CivlcTokenConstant;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.config.IF.CIVLConstants;
import dev.civl.mc.model.IF.CIVLSyntaxException;
import dev.civl.mc.transform.IF.ContractTransformer;
import dev.civl.mc.transform.common.BaseWorker;
import dev.civl.mc.transform.common.contracts.ClauseTransformGuideGenerator.ClauseTransformGuide;
import dev.civl.mc.transform.common.contracts.ContractClauseTransformer.TransformedPair;
import dev.civl.mc.transform.common.contracts.ContractTransformerWorker.SourceFileWithContractedFunctions.ContractedFunction;

/**
 * This transformer serves for CIVL Contracts mode.
 * 
 * @author ziqingluo
 *
 */
public class ContractTransformerWorker extends BaseWorker {

	/**
	 * Naming suffix for a generated function that contains the original body of a
	 * verifying function:
	 */
	static private final String originalSuffix = "_$origin";

	/**
	 * Naming suffix for a generated function that drives the verification of a
	 * verifying function:
	 */
	static private final String driverSuffix = "_$driver";

	/**
	 * The name of main function:
	 */
	private final static String MAIN_NAME = "main";

	/**
	 * A string source for a return statement:
	 */
	private final static String RETURN_RESULT = "return $result;";

	/**
	 * Set of all global variables in source files:
	 */
	private Set<VariableDeclarationNode> globalVarDecls = new HashSet<>();

	/**
	 * Names of all driver functions for all verifying functions:
	 */
	private Set<String> allDriverNames = new HashSet<>();

	/* ********************* Private class fields: ********************** */
	/**
	 * The target function that will be verified independently. Other functions will
	 * be not verified. For other functions that been annotated with contracts, the
	 * transformer will remove their bodies, since only their contracts are used.
	 */
	private final String targetFunctionName;

	public ContractTransformerWorker(ASTFactory astFactory, String targetFunctionName, CIVLConfiguration civlConfig) {
		super(ContractTransformer.LONG_NAME, astFactory);
		identifierPrefix = MPIContractUtilities.CIVL_CONTRACT_PREFIX;
		this.targetFunctionName = targetFunctionName;
	}

	/* ************************* Protected methods: ************************** */
	@Override
	protected AST transformCore(AST ast) throws SyntaxException {
		SequenceNode<BlockItemNode> root = ast.getRootNode();
		List<BlockItemNode> externalList = new LinkedList<>();
		SequenceNode<BlockItemNode> newRootNode;
		List<BlockItemNode> sourceFiles = new LinkedList<>();
		List<BlockItemNode> globalVarHavocs;
		AST newAst;
		int count;

		findMPIAndNodesofSourceFiles(root, sourceFiles);
		globalVarHavocs = havocForGlobalVariables(sourceFiles);

		// extracted function declarations and other source file nodes:
		SourceFileWithContractedFunctions contractedFuncsInSrc;
		// transformed contracted functions:
		List<BlockItemNode> transformedContractedFuncs;
		List<FunctionContractTransformGuide> callees = new LinkedList<>();
		List<FunctionContractTransformGuide> targets;

		// extract function definitions and declarations from source files:
		contractedFuncsInSrc = extractContractedFunctionsFromSourceFileNodes(sourceFiles);
		targets = analysisContractedFunctions(contractedFuncsInSrc, callees);
		ast.release();
		transformedContractedFuncs = processContractedFunctions(targets, callees);
		// takes off the rest in the source files:
		sourceFiles.clear();
		for (BlockItemNode otherInSrc : contractedFuncsInSrc.others)
			if (otherInSrc != null) {
				sourceFiles.add(otherInSrc);
				otherInSrc.remove();
			}
		// inserting the rests in the AST to the new tree:
		count = root.numChildren();
		for (int i = 0; i < count; i++) {
			BlockItemNode child = root.getSequenceChild(i);

			if (child != null) {
				root.removeChild(i);
				externalList.add(child);
			}
		}
		// adds the rest in the source files to the new tree:
		externalList.addAll(sourceFiles);
		// adding transformed functions:
		externalList.addAll(transformedContractedFuncs);
		// $havoc for all global variables:
		externalList.addAll(globalVarHavocs);
		externalList.add(mainFunction());
		newRootNode = nodeFactory.newSequenceNode(newSource("TranslationUnit", CivlcTokenConstant.TRANSLATION_UNIT),
				"TranslationUnit", externalList);
		newAst = astFactory.newAST(newRootNode, ast.getSourceFiles(), ast.isWholeProgram());
		// newAst.prettyPrint(System.out, false);
		return newAst;
	}

	/* ******************* Package private methods: ******************** */
	/**
	 * @param type   a {@link Type} instance
	 * @param source {@link Source} will associate to the returned node
	 * @return A {@link TypeNode} of the given type.
	 */
	TypeNode typeNode(Type type, Source source) {
		return super.typeNode(source, type);
	}

	/* ******************* Primary transforming methods: ******************** */
	/**
	 * <p>
	 * <b>Summary: </b> Create a new main function which enables all driver
	 * functions. If the MPI library is included, wrap the call to driver with a
	 * pair of <code>MPI_Init and MPI_Finalize</code>.
	 * 
	 * @param targetFunc The target function. The driver of the target function will
	 *                   be called in the created main function.
	 * @return The created main function definition node
	 * @throws SyntaxException
	 */
	private FunctionDefinitionNode mainFunction() throws SyntaxException {
		List<BlockItemNode> items = new LinkedList<BlockItemNode>();
		List<StatementNode> callDrivers = new LinkedList<>();
		Source combinedSource = null;

		// To enable multiple driver functions without letting their assumptions
		// interfere with each other, non-deterministic choice is used. i.e.
		// x = choose_int(#drivers);
		// if (x == 0) call driver1;
		// if (x == 1) call driver2;

		// the name of the variable taking result of $choose_int:
		String ndVerifyChoicerName = "CIVL_choose";
		int driverCounter = 0;

		for (String driverName : allDriverNames) {
			// creating calls to drivers with a branch condition for
			// non-determinism:
			Source source = newSource(targetFunctionName + "(...);", CivlcTokenConstant.CALL);
			StatementNode callDriver = nodeFactory.newExpressionStatementNode(
					nodeFactory.newFunctionCallNode(source, identifierExpression(driverName), Arrays.asList()));
			ExpressionNode choiceCond;

			choiceCond = nodeFactory.newOperatorNode(source, Operator.EQUALS,
					Arrays.asList(identifierExpression(ndVerifyChoicerName),
							nodeFactory.newIntConstantNode(source, driverCounter++)));
			callDriver = nodeFactory.newIfNode(source, choiceCond, callDriver);
			callDrivers.add(callDriver);
			combinedSource = combinedSource == null ? source
					: astFactory.getTokenFactory().join(source, combinedSource);
		}
		// build the body of the generated main function:

		// choose_int call:
		ExpressionNode ndVerifyChoicesCall = functionCall(combinedSource, CHOOSE_INT,
				Arrays.asList(nodeFactory.newIntConstantNode(combinedSource, callDrivers.size())));
		StatementNode ndVerifyChoices = nodeFactory
				.newExpressionStatementNode(nodeFactory.newOperatorNode(combinedSource, Operator.ASSIGN,
						Arrays.asList(identifierExpression(ndVerifyChoicerName), ndVerifyChoicesCall)));
		BlockItemNode ndChoicerDecl = nodeFactory.newVariableDeclarationNode(combinedSource,
				identifier(ndVerifyChoicerName), nodeFactory.newBasicTypeNode(combinedSource, BasicTypeKind.INT));

		items.add(ndChoicerDecl);
		items.add(ndVerifyChoices);
		items.addAll(callDrivers);

		CompoundStatementNode mainBody = nodeFactory
				.newCompoundStatementNode(newSource("main body", CivlcTokenConstant.COMPOUND_STATEMENT), items);
		SequenceNode<VariableDeclarationNode> mainFormals = nodeFactory.newSequenceNode(
				this.newSource("formal parameter of the declaration of the main function",
						CivlcTokenConstant.DECLARATION_LIST),
				"FormalParameterDeclarations", new ArrayList<VariableDeclarationNode>());
		FunctionTypeNode mainType = nodeFactory.newFunctionTypeNode(
				this.newSource("type of the main function", CivlcTokenConstant.TYPE), this.basicType(BasicTypeKind.INT),
				mainFormals, true);

		return nodeFactory.newFunctionDefinitionNode(
				this.newSource("definition of the main function", CivlcTokenConstant.FUNCTION_DEFINITION),
				this.identifier(MAIN_NAME), mainType, null, mainBody);
	}

	/**
	 * Classify ASTNodes in source files to 3 groups: target functions T and their
	 * contracts, callee functions C and their contracts and others. Note that T and
	 * C may have overlap.
	 * 
	 * @return an instance of {@link SourceFileWithContractedFunctions} which is the
	 *         result of the classification
	 * @throws SyntaxException
	 */
	private SourceFileWithContractedFunctions extractContractedFunctionsFromSourceFileNodes(
			List<BlockItemNode> sourceFileNodes) throws SyntaxException {
		List<FunctionDefinitionNode> targets = new LinkedList<>();
		List<FunctionDeclarationNode> callees = new LinkedList<>();
		List<BlockItemNode> others = new LinkedList<>();
		boolean verifyAll = targetFunctionName.equals(CIVLConstants.CONTRACT_CHECK_ALL);

		for (BlockItemNode child : sourceFileNodes) {
			if (child.nodeKind() == NodeKind.FUNCTION_DECLARATION || child.nodeKind() == NodeKind.FUNCTION_DEFINITION) {
				FunctionDeclarationNode funcDecl = (FunctionDeclarationNode) child;

				// If the function declaration has definition, test if it is
				// the target function:
				if (funcDecl.isDefinition()) {
					boolean isTarget = funcDecl.getName().equals(targetFunctionName);

					if (verifyAll || isTarget)
						if (funcDecl.getContract() != null)
							targets.add((FunctionDefinitionNode) funcDecl);
						else if (!verifyAll)
							throw new CIVLSyntaxException("No contracts specified for the target function");
				}
				// If a function f declaration is contracted, replace its body
				// with abstraction based on its contract, and creates a mirror
				// function f_origin which contains its original body.
				if (funcDecl.getContract() != null) {
					TypeNode funcDeclTypeNode = funcDecl.getTypeNode();

					if (funcDeclTypeNode.kind() == TypeNode.TypeNodeKind.FUNCTION)
						callees.add(funcDecl);
				}
			} else
				others.add(child);
		}
		if (targets.isEmpty() && !verifyAll)
			throw new CIVLSyntaxException("Target function: " + this.targetFunctionName + " not exist!");
		if (targets.isEmpty() && verifyAll)
			throw new CIVLSyntaxException("No function will be verified because no function definition has a contract");
		return new SourceFileWithContractedFunctions(targets, callees, others);
	}

	/**
	 * Transform all contracted functions with the given guides
	 * 
	 * @throws SyntaxException
	 */
	private List<BlockItemNode> processContractedFunctions(List<FunctionContractTransformGuide> targets,
			List<FunctionContractTransformGuide> callees) throws SyntaxException {
		List<BlockItemNode> results = new LinkedList<>();
		FunctionDefinitionNode driver;

		// transform callees:
		for (FunctionContractTransformGuide callee : callees) {
			FunctionDefinitionNode defn = callee.function.getEntity().getDefinition();

			if (defn != null)
				defn.remove();
			// special handling for main function: remove it from the ASTS
			// main is never be called:
			if (defn.getName().equals(MAIN_NAME))
				continue;
			// replace body with abstraction based on contracts:
			results.add(
					transformCalleeFunction(callee.function, (FunctionTypeNode) callee.function.getTypeNode(), callee));
			callee.function.remove();
		}
		// transform targets:
		for (FunctionContractTransformGuide target : targets) {
			// add driver function for verification:
			driver = transformTargetFunction((FunctionDefinitionNode) target.function, target);

			// add a mirror function which contains its original body:
			FunctionDefinitionNode defn = (FunctionDefinitionNode) target.function;
			FunctionTypeNode funcType = defn.getTypeNode();
			CompoundStatementNode defnBody = defn.getBody();

			defn.remove();
			funcType.remove();
			defnBody.remove();
			defn = nodeFactory.newFunctionDefinitionNode(defn.getSource(),
					identifier(target.getFunctionNameForOriginalBody()), funcType, null, defnBody);
			results.add(defn);
			results.add(driver);
			allDriverNames.add(driver.getName());

		}
		return results;
	}

	/**
	 * <p>
	 * Analyzes the contracted functions and their contracts in source files and
	 * generates transform guides for those contracts.
	 * </p>
	 * 
	 * @param contractedFuncsInSrc an instance of
	 *                             {@link SourceFileWithContractedFunctions}
	 * @param callees              output, a list of
	 *                             {@link FunctionContractTransformGuide}s for all
	 *                             contracted callee functions
	 * @return the {@link FunctionContractTransformGuide} for the contracted target
	 *         function
	 * @throws SyntaxException
	 */
	private List<FunctionContractTransformGuide> analysisContractedFunctions(
			SourceFileWithContractedFunctions contractedFuncsInSrc, List<FunctionContractTransformGuide> callees)
			throws SyntaxException {
		// analyze callees:
		for (ContractedFunction callee : contractedFuncsInSrc.callees) {
			MemoryLocationManager memoryLocationManager = new MemoryLocationManager(nodeFactory);
			ContractClauseTransformer clauseTransformer = new ContractClauseTransformer(astFactory,
					memoryLocationManager);
			boolean purelyLocal = callee.contracts.size() == 1;
			FunctionContractTransformGuide info = new FunctionContractTransformGuide(callee.function,
					memoryLocationManager);

			for (FunctionContractBlock block : callee.contracts) {
				List<ClauseTransformGuide> requiresTuples = new LinkedList<>();
				List<ClauseTransformGuide> ensuresTuples = new LinkedList<>();

				clauseTransformer.analysisContractBlock(block, true, purelyLocal, requiresTuples, ensuresTuples);
				info.addGuide(block, requiresTuples, ensuresTuples);
			}
			callees.add(info);
		}
		// analyze target:
		MemoryLocationManager memoryLocationManager = new MemoryLocationManager(nodeFactory);
		ContractClauseTransformer clauseTransformer = new ContractClauseTransformer(astFactory, memoryLocationManager);
		List<ContractedFunction> targets = contractedFuncsInSrc.targets;
		List<FunctionContractTransformGuide> targetInfos = new LinkedList<>();

		for (ContractedFunction target : targets) {
			boolean purelyLocal = target.contracts.size() == 1;
			FunctionContractTransformGuide targetInfo = new FunctionContractTransformGuide(target.function,
					memoryLocationManager);

			for (FunctionContractBlock block : target.contracts) {
				List<ClauseTransformGuide> requiresTuples = new LinkedList<>();
				List<ClauseTransformGuide> ensuresTuples = new LinkedList<>();

				clauseTransformer.analysisContractBlock(block, false, purelyLocal, requiresTuples, ensuresTuples);
				targetInfo.addGuide(block, requiresTuples, ensuresTuples);
			}
			targetInfos.add(targetInfo);
		}
		return targetInfos;
	}

	/**
	 * <p>
	 * Transform a non-target contracted function into a deductive executable form.
	 * </p>
	 * 
	 * <p>
	 * The body of a non-target contracted function f will be added or replaced its
	 * definition with: <code>
	 * f () {
	 *   assert ( seq-requires );
	 *   cp = snapshot();
	 *   $run $when($collate_arrived(cp, args .. )) $with(cp) 
	 *      if (assumes-cond)
	 *         assert ( col-requires );
	 *         
	 *   int $result;
	 *   
	 *   $havoc(&$result);
	 *   assume( seq-ensures);
	 *   if (assume-cond)
	 *      $wit(cp) assume(non-sync-col-ensures);
	 *   $run {  
	 *     if (assume-cond)
	 *        $when($collate_arrived(cp, args .. )) $with(cp)
	 *           assume(sync-col-ensures);
	 *   }
	 * }
	 * </code>
	 * </p>
	 * 
	 * @param funcDecl The {@link FunctionDeclarationNode} of the transformed
	 *                 function. It's original body will be removed.
	 * @return
	 * @throws SyntaxException
	 */
	private FunctionDefinitionNode transformCalleeFunction(FunctionDeclarationNode funcDecl,
			FunctionTypeNode funcTypeNode, FunctionContractTransformGuide guide) throws SyntaxException {
		CompoundStatementNode body;
		Source contractSource = funcDecl.getContract().getSource();
		;
		ContractClauseTransformer clauseTransformer = new ContractClauseTransformer(astFactory,
				guide.memoryLocationManager);
		/*
		 * Requirements (TODO: including assigns) of callees will be transformed to
		 * assertions
		 */
		List<BlockItemNode> transformedRequirements = new LinkedList<>();
		/* Ensurances of callees will be transformed to assumptions */
		List<BlockItemNode> transformedEnsurances = new LinkedList<>();
		List<ClauseTransformGuide> reqGuides4SideCond = new LinkedList<>();
		List<ClauseTransformGuide> ensGuides4SideCond = new LinkedList<>();

		if (guide.localBlock != null) {
			TransformedPair localPair = clauseTransformer.transformLocalBlock(guide.localREGuides.requiresGuides,
					guide.localREGuides.ensuresGuides, guide.localBlock, true);

			reqGuides4SideCond.addAll(guide.localREGuides.requiresGuides);
			ensGuides4SideCond.addAll(guide.localREGuides.ensuresGuides);
			transformedRequirements.addAll(localPair.before);
			transformedEnsurances.addAll(localPair.after);
		}
		/* check side conditions */
		transformedRequirements.addAll(clauseTransformer.checkSideConditions(reqGuides4SideCond));
		transformedEnsurances.addAll(clauseTransformer.checkSideConditions(ensGuides4SideCond));

		List<BlockItemNode> bodyItems = new LinkedList<>();
		boolean returnVoid = false;

		bodyItems.addAll(transformedRequirements);
		returnVoid = isVoidType(funcTypeNode.getReturnType().getType());
		if (!returnVoid) {
			bodyItems.add(nodeFactory.newVariableDeclarationNode(contractSource,
					identifier(MPIContractUtilities.ACSL_RESULT_VAR), funcTypeNode.getReturnType().copy()));
			bodyItems.add(nodeFactory.newExpressionStatementNode(
					createHavocCall(identifierExpression(MPIContractUtilities.ACSL_RESULT_VAR), nodeFactory)));
		}
		bodyItems.addAll(transformedEnsurances);
		if (!returnVoid)
			bodyItems.add(nodeFactory.newReturnNode(newSource(RETURN_RESULT, CivlcTokenConstant.RETURN),
					identifierExpression(MPIContractUtilities.ACSL_RESULT_VAR)));
		body = nodeFactory.newCompoundStatementNode(funcDecl.getSource(), bodyItems);
		return nodeFactory.newFunctionDefinitionNode(funcDecl.getSource(), funcDecl.getIdentifier().copy(),
				funcTypeNode.copy(), null, body);
	}

	/**
	 * <p>
	 * <b>Summary:</b> Wraps the target function with a harness function. The
	 * harness is created based on the contracts of the target function.
	 * </p>
	 * <p>
	 * <b>Details:</b> The contracted function will be transformed into the
	 * following pattern:
	 * <ul>
	 * <b> driver( ... ) { </b>
	 * <li>1 localContractStatements;</li>
	 * <li>2 $mpi_comm_size and $mpi_comm_rank decl;</li>
	 * <li>3 MPI_Comm_size(comm, &$mpi_comm_size) && MPI_Comm_rank( ... );</li>
	 * <li>4 take-snapshot;</li>
	 * <li>5 collectiveContractStatements</li>
	 * <li>6 enters</li>
	 * <li>7 $result declaration && call target function</li>
	 * <li>8 entered check</li>
	 * <li>9 localContractStatements;</li>
	 * <li>10 take-snapshot;</li>
	 * <li>11 collectiveContractStatements</li> <b>}</b>
	 * </p>
	 * 
	 * @param funcDefi The definition of the target function
	 * @return A new driver function for the target function.
	 * @throws SyntaxException
	 */
	private FunctionDefinitionNode transformTargetFunction(FunctionDefinitionNode funcDefi,
			FunctionContractTransformGuide guide) throws SyntaxException {
		CompoundStatementNode body;
		String driverName = guide.getDriverNameForVerification();
		Source contractSource = funcDefi.getContract().getSource();
		Source driverSource = newSource(driverName, CivlcTokenConstant.FUNCTION_DEFINITION);
		ContractClauseTransformer clauseTransformer = new ContractClauseTransformer(astFactory,
				guide.memoryLocationManager);

		List<BlockItemNode> requirements = new LinkedList<>();
		List<BlockItemNode> ensurances = new LinkedList<>();
		List<ClauseTransformGuide> reqGuides4SideCond = new LinkedList<>();
		List<ClauseTransformGuide> ensGuides4SideCond = new LinkedList<>();

		if (guide.localBlock != null) {
			TransformedPair localPair = clauseTransformer.transformLocalBlock(guide.localREGuides.requiresGuides,
					guide.localREGuides.ensuresGuides, guide.localBlock, false);

			reqGuides4SideCond.addAll(guide.localREGuides.requiresGuides);
			ensGuides4SideCond.addAll(guide.localREGuides.ensuresGuides);
			requirements.addAll(localPair.before);
			ensurances.addAll(localPair.after);
		}
		/* check side conditions */
		requirements.addAll(clauseTransformer.checkSideConditions(reqGuides4SideCond));
		ensurances.addAll(clauseTransformer.checkSideConditions(ensGuides4SideCond));

		List<BlockItemNode> driverComponents = new LinkedList<>();
		ExpressionNode targetCall;
		ExpressionNode originalBodyIdentifier = identifierExpression(guide.getFunctionNameForOriginalBody());
		FunctionTypeNode funcTypeNode = funcDefi.getTypeNode();
		List<ExpressionNode> funcParamIdentfiers = new LinkedList<>();

		for (VariableDeclarationNode param : funcTypeNode.getParameters())
			funcParamIdentfiers.add(identifierExpression(param.getIdentifier().name()));
		targetCall = nodeFactory.newFunctionCallNode(driverSource, originalBodyIdentifier, funcParamIdentfiers);

		// Create variable declarations which are actual parameters of the
		// target function:
		driverComponents.addAll(createVariableDeclsAndInitsForDriver(funcTypeNode));
		driverComponents.addAll(requirements);
		if (!isVoidType(funcTypeNode.getReturnType().getType()))
			driverComponents.add(nodeFactory.newVariableDeclarationNode(contractSource,
					identifier(MPIContractUtilities.ACSL_RESULT_VAR), funcDefi.getTypeNode().getReturnType().copy(),
					targetCall));
		else
			driverComponents.add(nodeFactory.newExpressionStatementNode(targetCall));
		driverComponents.addAll(ensurances);
		body = nodeFactory.newCompoundStatementNode(driverSource, driverComponents);
		funcTypeNode = nodeFactory.newFunctionTypeNode(funcTypeNode.getSource(), funcTypeNode.getReturnType().copy(),
				nodeFactory.newSequenceNode(funcTypeNode.getParameters().getSource(), "contract_driver_parameters",
						Arrays.asList()),
				funcTypeNode.hasIdentifierList());
		return nodeFactory.newFunctionDefinitionNode(driverSource, identifier(driverName), funcTypeNode.copy(), null,
				body);
	}

	/*
	 * ************************* Utility methods ****************************
	 */

	/**
	 * Return a list of {@link BlockItemNode}s which comes from source code in
	 * "source file"s. Here a "source file" is a file with ".c, .cvl, .h" or ".cvh"
	 * suffix and NOT under the system include path.
	 * 
	 * @param root         the root node of an AST
	 * @param srcFileNodes OUTPUT. all the block item nodes in the given AST that
	 *                     come from "source files"
	 * @return true iff there is at least one node comes from "mpi.h"
	 */
	private boolean findMPIAndNodesofSourceFiles(SequenceNode<BlockItemNode> root, List<BlockItemNode> srcFileNodes) {
		Path civlIncludePath = CIVLConstants.CIVL_LIB_SRC_PATH.toPath();
		Path frontendIncludePath = CIVLConstants.CIVL_LIB_INCLUDE_PATH.toPath();
		boolean hasMPI = false;

		for (BlockItemNode node : root) {
			if (node == null)
				continue;

			Path sourceFilePath = node.getSource().getFirstToken().getSourceFile().getFile().toPath();
			String sourceFileName = sourceFilePath.getFileName().toString();

			if (sourceFilePath.startsWith(civlIncludePath) || sourceFilePath.startsWith(frontendIncludePath)) {
				if (!hasMPI && sourceFileName.equals("mpi.h"))
					hasMPI = true;
				continue;
			}
			srcFileNodes.add(node);

			assert sourceFileName.endsWith(".c") || sourceFileName.endsWith(".cvl") || sourceFileName.endsWith(".h")
					|| sourceFileName.endsWith(".cvh");
		}
		return hasMPI;
	}

	/**
	 * <p>
	 * <b>Summary: </b> Creates an $havoc function call:<code>
	 * $mpi_snapshot(&var);</code>
	 * </p>
	 * 
	 * @param var An {@link ExpressionNode} representing an variable.
	 * @return The created $havoc call expression node.
	 */
	private ExpressionNode createHavocCall(ExpressionNode var, NodeFactory nodeFactory) {
		Source source = var.getSource();
		ExpressionNode callIdentifier = identifierExpression(source, MPIContractUtilities.HAVOC);
		ExpressionNode addressOfVar = nodeFactory.newOperatorNode(var.getSource(), Operator.ADDRESSOF, var.copy());
		FunctionCallNode call = nodeFactory.newFunctionCallNode(source, callIdentifier, Arrays.asList(addressOfVar));

		return call;
	}

	/**
	 * <p>
	 * <b>Summary: </b> Transform the parameters of the target function into a
	 * sequence of variable declarations. All of them will be initialized with
	 * arbitrary values.
	 * </p>
	 * 
	 * @param targetFuncType A {@link FunctionTypeNode} which represents the
	 *                       function type of the target function.
	 * @return
	 */
	private List<BlockItemNode> createVariableDeclsAndInitsForDriver(FunctionTypeNode targetFuncType) {
		SequenceNode<VariableDeclarationNode> formals = targetFuncType.getParameters();
		List<BlockItemNode> results = new LinkedList<>();

		// create an variable for each formal parameter
		for (VariableDeclarationNode varDecl : formals) {
			VariableDeclarationNode actualDecl = varDecl.copy();
			StatementNode havoc;
			results.add(actualDecl);
			// $havoc for the actual parameter declaration:
			havoc = nodeFactory.newExpressionStatementNode(
					createHavocCall(identifierExpression(actualDecl.getName()), nodeFactory));
			results.add(havoc);
		}
		return results;
	}

	/**
	 * Find out variable declarations in the given list of block item nodes, do
	 * $havoc for them.
	 * 
	 * @param root
	 * @return
	 */
	private List<BlockItemNode> havocForGlobalVariables(List<BlockItemNode> root) {
		List<BlockItemNode> havocs = new LinkedList<>();

		for (BlockItemNode item : root) {
			if (item.nodeKind() == NodeKind.VARIABLE_DECLARATION) {
				VariableDeclarationNode decl = ((VariableDeclarationNode) item);
				String name = ((VariableDeclarationNode) item).getName();

				globalVarDecls.add(decl);
				havocs.add(nodeFactory
						.newExpressionStatementNode(createHavocCall(identifierExpression(name), nodeFactory)));
			}
		}
		return havocs;
	}

	/**
	 * <p>
	 * This class represents a transformation guide for a whole function contract. A
	 * function contract guide consists of {@link ClauseTransformGuide}s for
	 * contract clauses and an instance of {@link MemoryLocationManager} which is a
	 * stateful object deals with allocation and refreshment.
	 * </p>
	 * 
	 * TODO: make a guide for assigns and waitsfor too ?
	 * 
	 * @author ziqingluo
	 *
	 */
	class FunctionContractTransformGuide {

		/**
		 * a reference to the function declaration
		 */
		FunctionDeclarationNode function;

		/**
		 * a sole local block, a function will have at most one local function contract
		 * block
		 */
		FunctionContractBlock localBlock;

		/**
		 * a pair of {@link ClauseTransformGuide}s for requirements and ensures in the
		 * local contract block
		 */
		REGuidePair localREGuides;

		/**
		 * a instance of a {@link MemoryLocationManager}
		 */
		MemoryLocationManager memoryLocationManager;

		FunctionContractTransformGuide(FunctionDeclarationNode function, MemoryLocationManager memoryLocationManager) {
			this.function = function;
			this.localREGuides = new REGuidePair();
			this.memoryLocationManager = memoryLocationManager;
			localBlock = null;
		}

		void addGuide(FunctionContractBlock block, List<ClauseTransformGuide> requiresGuides,
				List<ClauseTransformGuide> ensuresGuides) {
			assert localBlock == null;
			localBlock = block;
			localREGuides.requiresGuides.addAll(requiresGuides);
			localREGuides.ensuresGuides.addAll(ensuresGuides);
		}

		/**
		 * @return the name of the function that contains the original function body of
		 *         the corresponding function
		 */
		String getFunctionNameForOriginalBody() {
			return this.function.getName() + originalSuffix;
		}

		/**
		 * @return the name of the function that launches the contract verification of
		 *         this function
		 */
		String getDriverNameForVerification() {
			return this.function.getName() + driverSuffix;
		}

		/**
		 * a simple data structure for clause transform guides of <code>requires</code>
		 * and <code>ensures</code>
		 * 
		 * @author ziqingluo
		 *
		 */
		class REGuidePair {
			List<ClauseTransformGuide> requiresGuides;
			List<ClauseTransformGuide> ensuresGuides;

			REGuidePair() {
				this.requiresGuides = new LinkedList<>();
				this.ensuresGuides = new LinkedList<>();
			}
		}
	}

	/**
	 * <p>
	 * This class is a data structure represents the source files (excludes CIVL-C
	 * libraries). The ASTNodes of source files are organized in three
	 * (non-overlapping) groups:
	 * <ul>
	 * <li>The target function definitions and their contracts,
	 * {@link SourceFileWithContractedFunctions#targets}</li>
	 * <li>The first encountered contracted callee function declarations and their
	 * contracts, {@link SourceFileWithContractedFunctions#callees}</li>
	 * <li>The rest of the ASTNodes in the source files,
	 * {@link SourceFileWithContractedFunctions#others}</li>
	 * </ul>
	 * </p>
	 * 
	 * note that group 1 and group 2 may have overlaps.
	 * 
	 * TODO: think about conjunctions of contracts over multiple declarations of the
	 * same function
	 * 
	 * @author ziqingluo
	 */
	class SourceFileWithContractedFunctions {
		/**
		 * A contracted function data structure, including a set of
		 * {@link FunctionContractBlock} and a {@link FunctionDeclarationNode} of the
		 * function.
		 * 
		 * @author ziqingluo
		 */
		class ContractedFunction {

			final List<FunctionContractBlock> contracts;

			final FunctionDeclarationNode function;

			ContractedFunction(FunctionDeclarationNode function) {
				this.function = function;
				this.contracts = FunctionContractBlock.parseContract(function.getContract(), nodeFactory);
			}
		}

		/**
		 * the target function definitions and its contracts
		 */
		final List<ContractedFunction> targets;

		/**
		 * the first encountered contracted callee function declarations and their
		 * contracts
		 */
		final List<ContractedFunction> callees;

		/**
		 * the rest of the ASTNodes in the source files
		 */
		final List<BlockItemNode> others;

		SourceFileWithContractedFunctions(List<FunctionDefinitionNode> targets, List<FunctionDeclarationNode> callees,
				List<BlockItemNode> others) {
			this.targets = new LinkedList<>();
			for (FunctionDefinitionNode target : targets)
				this.targets.add(new ContractedFunction(target));
			this.callees = new LinkedList<>();
			for (FunctionDeclarationNode callee : callees)
				this.callees.add(new ContractedFunction(callee));
			this.others = others;
		}
	}

	/**
	 * If the given expression is a cast-expression: <code>(T) expr</code>, return
	 * an expression representing <code>expr</code>, otherwise no-op.
	 * 
	 * @param expression An instance of {@link ExpressionNode}
	 * @return An expression who is the argument of a cast expression iff the input
	 *         is a cast expression, otherwise returns input itself.(i.e. no-op).
	 */
	static ExpressionNode decast(ExpressionNode expression) {
		if (expression.expressionKind() == ExpressionKind.CAST) {
			CastNode castNode = (CastNode) expression;

			return castNode.getArgument();
		}
		return expression;
	}
}
