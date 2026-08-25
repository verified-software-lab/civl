package dev.civl.mc.transform.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static dev.civl.mc.config.IF.CIVLConstants.MPI_NPROCS_MAX;

import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.TypedefDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.StringLiteralNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.DeclarationListNode;
import dev.civl.abc.ast.node.IF.statement.ExpressionStatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.StructureOrUnionTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.front.IF.CivlcTokenConstant;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.mc.transform.IF.MPI2CIVLTransformer;
import dev.civl.mc.util.IF.Pair;

/**
 * MPI2CIVLTransformer transforms an AST of an MPI program into an AST of an
 * equivalent CIVL-C program. See {@linkplain #transform(AST)}. TODO: copy
 * output files only for the mpi process with rank 0.
 * 
 * 
 * The MPI transformer tries to move the main function and file scope stuffs of
 * the original MPI program into the scope of the function MPI_Process(). Most
 * unstateful functions remain in the file scope, which are usually functions of
 * included headers. The Pthread library implementation contains a global
 * variable for the thread pool, which however, if in a MPI+Pthread program,
 * should go to the scope of the MPI process, instead of the file scope. And
 * ditto for pthread_create, pthread_exit, pthread_is_terminated function,
 * because they access the thread pool.
 * 
 * CIVL currently uses this order for transformers: general, IO, OpenMP,
 * Pthread, CUDA, MPI.
 * 
 * At this point, the order matters. Because if MPI transformer goes first,
 * Pthread transformer is not be able to insert the thread pool variable in the
 * right place. Since MPI is modifying the program structure more than other
 * transformers (moving the original file scope stuffs to a function), it is
 * easier to make it always the last transformer to apply.
 * 
 * @author Manchun Zheng (zmanchun)
 * 
 */
public class MPI2CIVLWorker extends BaseWorker {

	/*
	 * ************************** Private Static Fields **********************
	 */

	private final static String MPI_DATATYPE = "MPI_Datatype";

	private final static String MPI_DATASIZE_T = "$mpi_data_size_t";

	private final static String MPI_DATASIZE_STRUCT = "_mpi_data_size";

	private final static String MPI_SIZE_MAP = "_mpi_size_map";

	/**
	 * The file name of the MPI standard header
	 */
	private final static String MPI_HEADER = "mpi.h";

	/**
	 * The name of the standard exit function
	 */
	private final static String EXIT = "exit";

	/**
	 * The prefix of identifiers created by this transformer
	 */
	private final static String MPI_PREFIX = "_mpi_";

	/**
	 * The name of the identifier of the MPI_Comm variable MPI_COMM_WORLD in the
	 * final CIVL program.
	 */
	private final static String MPI_COMM_WORLD = "MPI_COMM_WORLD";

	/**
	 * The name of the identifier of the MPI_Comm variable MPI_COMM_SELF in the
	 * final CIVL program.
	 */
	private final static String MPI_COMM_SELF = "MPI_COMM_SELF";

	/**
	 * The name of the identifier of the CMPI_Gcomm variable in the final CIVL
	 * program.
	 */
	private final static String GCOMM_WORLD = MPI_PREFIX + "gcomm";

	/**
	 * The name of the identifier of the CMPI_Gcomm variable _mpi_gcomm_self in the
	 * final CIVL program.
	 */
	private final static String GCOMM_SELF = MPI_PREFIX + "gcomm_self";

	/**
	 * The name of the identifier of the CMPI_Gcomm sequence variable in the final
	 * CIVL-MPI program
	 */
	private final static String GCOMMS = MPI_PREFIX + "gcomms";

	/**
	 * The name of the function call for initializing a sequence.
	 */
	private final static String SEQ_INIT = "$seq_init";

	/**
	 * The name of CMPI_Gcomm type in the final CIVL-C program.
	 */
	private final static String GCOMM_TYPE = "$mpi_gcomm";

	/**
	 * The name of MPI_Comm type in both the original program and the final CIVL-C
	 * program.
	 */
	private final static String COMM_TYPE = "MPI_Comm";

	/**
	 * The name of the function to create a new CMPI_Gcomm object in the final
	 * CIVL-C program.
	 */
	private final static String GCOMM_CREATE = "$mpi_gcomm_create";

	/**
	 * The name of the function to create a new MPI_Comm object in the final CIVL-C
	 * program.
	 */
	private final static String COMM_CREATE = "$mpi_comm_create";

	/**
	 * The name of the function to create a new MPI_Comm object MPI_COMM_SELF in the
	 * final CIVL-C program.
	 */
	private final static String COMM_SELF_CREATE = "$mpi_comm_self_create";

	/**
	 * The name of the function to free a CMPI_Gcomm object in the final CIVL-C
	 * program.
	 */
	private final static String GCOMM_DESTROY = "$mpi_gcomm_destroy";

	/**
	 * The name of the function to free a MPI_Comm object in the final CIVL-C
	 * program.
	 */
	private final static String COMM_DESTROY = "$mpi_comm_destroy";

	/**
	 * The name of the parameter of a MPI procedure.
	 */
	private final String MPI_RANK = MPI_PREFIX + "rank";

	/**
	 * The name of the function MPI_Init in the original MPI program.
	 */
	private final static String MPI_INIT = "MPI_Init";

	/**
	 * The name of the function translating MPI_Init in the final CIVL-C program.
	 */
	private final static String MPI_INIT_NEW = "$mpi_init";

	/**
	 * The name of the variable representing the status of an MPI process, which is
	 * modified by MPI_Init() and MPI_Finalized().
	 */
	private final static String MPI_STATE_VAR = MPI_PREFIX + "state";

	/**
	 * The name of the MPI procedure in the final CIVL-C program.
	 */
	private final static String MPI_PROCESS = MPI_PREFIX + "process";

	/**
	 * The name of the input variable denoting the number of MPI processes in the
	 * final CIVL-C program.
	 */
	private final static String NPROCS = MPI_PREFIX + "nprocs";

	/**
	 * The name of the input variable denoting the upper bound of the number of MPI
	 * processes in the final CIVL-C program.
	 */
	private final static String NPROCS_UPPER_BOUND = MPI_PREFIX + "nprocs_hi";

	/**
	 * The name of the input variable denoting the lower bound of the number of MPI
	 * processes in the final CIVL-C program.
	 */
	private final static String NPROCS_LOWER_BOUND = MPI_PREFIX + "nprocs_lo";

	/**
	 * The name of the variable that represents the root scope of an MPI process
	 */
	private final static String PROCESS_ROOT = MPI_PREFIX + "root";

	/**
	 * The name of the type that represents the state of MPI routine calls
	 */
	private static final String MPI_STATE_TYPE = "$mpi_state";

	/**
	 * The name of the function $mpi_coroutine_name which is a helper function for
	 * gcomm destroy
	 */
	private static final String MPI_COROUTINE_NAME = "$mpi_coroutine_name";

	/* ****************************** Constructor ************************** */
	/**
	 * Creates a new instance of MPITransformer.
	 * 
	 * @param astFactory The ASTFactory that will be used to create new nodes.
	 */
	public MPI2CIVLWorker(ASTFactory astFactory) {
		super(MPI2CIVLTransformer.LONG_NAME, astFactory);
		this.identifierPrefix = MPI_PREFIX;
	}

	/* *************************** Private Methods ************************* */

	/**
	 * Creates a bound assumption node in the form of:
	 * <code>$assume lowerBound < variable && variable <= upperBound</code>.
	 * 
	 * @param lowerBound The lower bound of the variable.
	 * @param variable   The variable to be bounded.
	 * @param upperBound The upper bound of the variable.
	 * @return The node representing of the assumption on the bound of the variable.
	 * @throws SyntaxException
	 */
	private ExpressionStatementNode boundAssumption(String lowerBound, String variable, String upperBound)
			throws SyntaxException {
		ExpressionNode variableExpression = this.identifierExpression(variable);
		ExpressionNode upperBoundExpression = this.identifierExpression(upperBound);
		ExpressionNode lowerBoundExpression = this.identifierExpression(lowerBound);
		ExpressionNode lowerPart, upperPart;
		Source source = this.newSource("assumption on the bound of variable " + variable,
				CivlcTokenConstant.EXPRESSION_STATEMENT);

		lowerPart = nodeFactory.newOperatorNode(
				this.newSource("lower bound of variable " + variable, CivlcTokenConstant.EXPR), Operator.LTE,
				Arrays.asList(lowerBoundExpression, variableExpression));
		variableExpression = variableExpression.copy();
		upperPart = nodeFactory.newOperatorNode(
				this.newSource("upper bound of variable " + variable, CivlcTokenConstant.EXPR), Operator.LTE,
				Arrays.asList(variableExpression, upperBoundExpression));
		return nodeFactory.newExpressionStatementNode(this.functionCall(source, ASSUME,
				Arrays.asList((ExpressionNode) nodeFactory.newOperatorNode(
						this.newSource("logical and ", CivlcTokenConstant.EXPR), Operator.LAND,
						Arrays.asList(lowerPart, upperPart)))));
	}

	/**
	 * Creates the declaration node for the variable <code>MPI_COMM_WORLD</code> ,
	 * which is of <code>MPI_Comm</code> type and has an initializer to call
	 * <code>$comm_create()</code>. That is:
	 * <code>MPI_Comm MPI_COMM_WORLD = $comm_create($here, GCOMM_WORLD, _rank)</code>
	 * .
	 * 
	 * @return The declaration node of the variable <code>MPI_COMM_WORLD</code>.
	 */
	private VariableDeclarationNode commDeclaration() {
		TypeNode commType;
		List<ExpressionNode> commCreateArgs;
		ExpressionNode commCreate;
		VariableDeclarationNode commVar;

		commType = nodeFactory.newTypedefNameNode(
				nodeFactory.newIdentifierNode(this.newSource("$comm type", CivlcTokenConstant.IDENTIFIER), COMM_TYPE));
		commCreateArgs = new ArrayList<>(3);
		commCreateArgs.add(this.hereNode());
		commCreateArgs.add(this.identifierExpression(GCOMM_WORLD));
		commCreateArgs.add(this.identifierExpression(MPI_RANK));
		commCreate = nodeFactory.newFunctionCallNode(
				this.newSource("function call " + COMM_CREATE, CivlcTokenConstant.CALL),
				this.identifierExpression(COMM_CREATE), commCreateArgs);
		commVar = this.variableDeclaration(MPI_COMM_WORLD, commType, commCreate);
		// commVar.setExternStorage(true);
		return commVar;
	}

	/**
	 *
	 * Generates the following code:
	 * 
	 * <code>
	 * 
	 * MPI_Comm MPI_COMM_SELF;
	 * $mpi_gcomm _mpi_gcomm_self = $mpi_comm_self_create($here,
	 *                                 &MPI_COMM_SELF);
	 * 
	 * </code>
	 * 
	 * @return
	 */
	private List<VariableDeclarationNode> commSelfDeclaration() {
		List<VariableDeclarationNode> result = new LinkedList<>();
		TypeNode commType = nodeFactory.newTypedefNameNode(
				nodeFactory.newIdentifierNode(this.newSource("$comm type", CivlcTokenConstant.IDENTIFIER), COMM_TYPE));
		VariableDeclarationNode commSelfVar = variableDeclaration(MPI_COMM_SELF, commType);
		ExpressionNode addrOfCommSelfVar = nodeFactory.newOperatorNode(
				newSource("&" + MPI_COMM_SELF, CivlcTokenConstant.OPERATOR), Operator.ADDRESSOF,
				identifierExpression(MPI_COMM_SELF));
		ExpressionNode commSelfCreate = nodeFactory.newFunctionCallNode(
				newSource("function call " + COMM_SELF_CREATE, CivlcTokenConstant.CALL),
				identifierExpression(COMM_SELF_CREATE), Arrays.asList(hereNode(), addrOfCommSelfVar));
		TypeNode gcommType = nodeFactory.newTypedefNameNode(
				nodeFactory.newIdentifierNode(this.newSource("$gcomm type", CivlcTokenConstant.IDENTIFIER), GCOMM_TYPE));
		VariableDeclarationNode gcommSelfVar = variableDeclaration(GCOMM_SELF, gcommType, commSelfCreate);

		result.add(commSelfVar);
		result.add(gcommSelfVar);
		return result;
	}

	/**
	 * Creates an expression statement node of a function call node for destroying a
	 * communicator, either global or local.
	 * 
	 * @param destroy  The name of the function call, either
	 *                 <code>$gcomm_destroy</code> or <code>$comm_destroy</code>.
	 * @param commName The name of the variable of the communicator to be destroy,
	 *                 either <code>GCOMM_WORLD</code> or
	 *                 <code>MPI_COMM_WORLD</code> .
	 * @return The expression statement node of the function call for destroying the
	 *         specified communicator.
	 */
	private ExpressionStatementNode commDestroy(String destroy, String commName) {
		ExpressionNode function = this.identifierExpression(destroy);
		List<ExpressionNode> arguments;

		if (destroy.equals(COMM_DESTROY)) {
			arguments = Arrays.asList(this.identifierExpression(commName), this.identifierExpression(MPI_STATE_VAR));
		} else
			arguments = Arrays.asList(this.identifierExpression(commName));
		return nodeFactory.newExpressionStatementNode(nodeFactory.newFunctionCallNode(
				this.newSource("function call " + destroy, CivlcTokenConstant.CALL), function, arguments));
	}

	/**
	 * Creates the declaration node for the variable <code>GCOMM_WORLD</code> ,
	 * which is of <code>$gcomm</code> type and has an initializer to call
	 * <code>$gcomm_create()</code>. That is:
	 * <code>$gcomm GCOMM_WORLD = $gcomm_create($here, NPROCS)</code> .
	 * 
	 * @return The declaration node of the variable <code>GCOMM_WORLD</code>.
	 */
	private VariableDeclarationNode gcommDeclaration() {
		TypeNode gcommType;
		// ExpressionNode gcommCreate;

		gcommType = nodeFactory.newTypedefNameNode(this.identifier(GCOMM_TYPE));
		// gcommCreate = nodeFactory.newFunctionCallNode(this.newSource(
		// "function call " + GCOMM_CREATE, CivlcTokenConstant.CALL), this
		// .identifierExpression(GCOMM_CREATE), Arrays.asList(
		// this.hereNode(), this.identifierExpression(NPROCS)), null);
		return this.variableDeclaration(GCOMM_WORLD, gcommType);
	}

	private VariableDeclarationNode gcommsSeqDeclaration() {
		TypeNode gcommType, gcommArrayType;
		VariableDeclarationNode node;

		gcommType = nodeFactory.newTypedefNameNode(this.identifier(GCOMM_TYPE));
		gcommArrayType = nodeFactory.newArrayTypeNode((Source) null, gcommType, null);
		node = this.variableDeclaration(GCOMMS, gcommArrayType);
		return node;
	}

	private ExpressionStatementNode gcommsSeqInitCalling() throws SyntaxException {
		FunctionCallNode node;
		OperatorNode gcommsPtrNode, gcommworldPtrNode;

		gcommsPtrNode = nodeFactory.newOperatorNode(this.newSource("&", CivlcTokenConstant.OPERATOR),
				Operator.ADDRESSOF, this.identifierExpression(GCOMMS));
		gcommworldPtrNode = nodeFactory.newOperatorNode(this.newSource("&", CivlcTokenConstant.OPERATOR),
				Operator.ADDRESSOF, this.identifierExpression(GCOMM_WORLD));
		node = nodeFactory.newFunctionCallNode(this.newSource("function call " + SEQ_INIT, CivlcTokenConstant.CALL),
				this.identifierExpression(SEQ_INIT),
				Arrays.asList(gcommsPtrNode, nodeFactory.newIntegerConstantNode(null, "1"), gcommworldPtrNode));
		return nodeFactory.newExpressionStatementNode(node);
	}

	// private ExpressionStatementNode elaborateNPROCS() {
	// FunctionCallNode elaborateCall = nodeFactory.newFunctionCallNode(
	// this.newSource("elaborate _NPROCS", CParser.CALL),
	// this.identifierExpression("$elaborate"),
	// Arrays.asList(this.identifierExpression(NPROCS)), null);
	//
	// return nodeFactory.newExpressionStatementNode(elaborateCall);
	// }

	/**
	 * Creates the main function for the final program, which is: <br>
	 * 
	 * <pre>
	 * void main(){
	 *   for(int i = 0; i &lt; 10; i++)
	 *     _argv[i]=&CIVL_argv[i][0];
	 *   $parfor(int i: 0 .. NPROCS-1)
	 *     MPI_Process(i);
	 *   $gcomm_destroy(GCOMM_WORLD);
	 * }
	 * </pre>
	 * 
	 * @return The function definition node representing the main function of the
	 *         final program.
	 * @throws SyntaxException
	 */
	private FunctionDefinitionNode mainFunction() throws SyntaxException {
		List<BlockItemNode> items = new LinkedList<BlockItemNode>();
		DeclarationListNode iterator;
		ExpressionNode upperBound;
		CompoundStatementNode mainBody;
		SequenceNode<VariableDeclarationNode> formals;
		FunctionTypeNode mainType;
		FunctionDefinitionNode mainFunction;
		ExpressionStatementNode gcommDestroy = this.commDestroy(GCOMM_DESTROY, GCOMM_WORLD);
		ExpressionNode domain;
		FunctionCallNode callMPIprocess;
		StatementNode parforMPIproc;

		Source boundSource = newSource("$assert(_mpi_nprocs <= " + MPI_NPROCS_MAX + ");", CivlcTokenConstant.CALL);
		ExpressionNode boundExpr = nodeFactory.newOperatorNode(boundSource, Operator.LTE, identifierExpression(NPROCS),
				integerConstant(MPI_NPROCS_MAX));
		StringLiteralNode boundMsg = stringLiteral(
				"An upper bound or exact value for the number of MPI processes must be specified.\n"
						+ "This number can be at most " + MPI_NPROCS_MAX + ".\n"
						+ "Type \"civl help\" to see how to specify this information on the command line.");
		ExpressionNode boundAssertion = nodeFactory.newFunctionCallNode(boundSource, identifierExpression("$assert"),
				Arrays.asList(boundExpr, boundMsg));
		StatementNode boundStmt = nodeFactory.newExpressionStatementNode(boundAssertion);
		items.add(boundStmt);

		iterator = nodeFactory.newForLoopInitializerNode(
				newSource("$parfor loop variable", CivlcTokenConstant.INIT_DECLARATOR_LIST),
				Arrays.asList(this.variableDeclaration("i", this.basicType(BasicTypeKind.INT))));
		upperBound = nodeFactory.newOperatorNode(this.newSource("post increment", CivlcTokenConstant.POST_INCREMENT),
				Operator.MINUS, Arrays.asList(this.identifierExpression(NPROCS), this.integerConstant(1)));
		domain = nodeFactory.newRegularRangeNode(this.newSource("regular range", CivlcTokenConstant.RANGE),
				this.integerConstant(0), upperBound);
		callMPIprocess = nodeFactory.newFunctionCallNode(
				this.newSource("function call " + MPI_PROCESS, CivlcTokenConstant.CALL),
				this.identifierExpression(MPI_PROCESS), Arrays.asList(this.identifierExpression("i")));
		parforMPIproc = nodeFactory.newCivlForNode(this.newSource("$parfor MPI_Process", CivlcTokenConstant.PARFOR),
				true, iterator, domain, nodeFactory.newExpressionStatementNode(callMPIprocess), null);

		ExpressionNode gcommCreate = nodeFactory.newFunctionCallNode(
				this.newSource("function call " + GCOMM_CREATE, CivlcTokenConstant.CALL),
				this.identifierExpression(GCOMM_CREATE),
				Arrays.asList(this.identifierExpression(PROCESS_ROOT), this.identifierExpression(NPROCS))),

				assignGcomm = nodeFactory.newOperatorNode(gcommCreate.getSource(), Operator.ASSIGN,
						Arrays.asList(this.identifierExpression(GCOMM_WORLD), gcommCreate));
		items.add(nodeFactory.newExpressionStatementNode(assignGcomm));
		items.add(this.gcommsSeqInitCalling());
		items.add(parforMPIproc);
		// destroying GCOMM_WROLD;
		items.add(gcommDestroy);
		// constructing the function definition node.
		mainBody = nodeFactory
				.newCompoundStatementNode(this.newSource("main body", CivlcTokenConstant.COMPOUND_STATEMENT), items);
		formals = nodeFactory.newSequenceNode(
				this.newSource("formal parameter of the declaration of the main function",
						CivlcTokenConstant.DECLARATION_LIST),
				"FormalParameterDeclarations", new ArrayList<VariableDeclarationNode>());
		mainType = nodeFactory.newFunctionTypeNode(this.newSource("type of the main function", CivlcTokenConstant.TYPE),
				this.basicType(BasicTypeKind.INT), formals, true);
		mainFunction = nodeFactory.newFunctionDefinitionNode(
				this.newSource("definition of the main function", CivlcTokenConstant.FUNCTION_DEFINITION),
				this.identifier("main"), mainType, null, mainBody);
		return mainFunction;
	}

	/**
	 * 
	 * Constructs the function MPI_Process() from the original MPI program. It is a
	 * wrapper of the original MPI program with some additional features: <br>
	 * 
	 * <pre>
	 * void _mpi_process(int _mpi_rank){
	 *   $comm MPI_COMM_WORLD = $comm_create(...);
	 *   //SLIGHTLY-MODIFIED ORIGINAL PROGRAM;
	 *   int a, b, ...;
	 *   ... function(){...}
	 *   ...
	 *   ... _main(){...} // renamed main() to _main()
	 *   ....
	 *   //ORIGINAL PROGRAM ENDS HERE;
	 *   _main();
	 *   $comm_destroy(MPI_COMM_WORLD);
	 * }
	 * </pre>
	 * 
	 * @param root The root node of the AST of the original MPI program.
	 * @return The function definition node of MPI_Process, the list of AST nodes
	 *         that are parsed from header files and will be moved up to the higher
	 *         scope (i.e., the file scope of the final AST), and variable
	 *         declaration nodes that are the arguments of the original main
	 *         function which will be moved up to the higher scope (i.e., the file
	 *         scope of the final AST) and become $input variables of the final AST.
	 * @throws SyntaxException
	 */
	private Pair<FunctionDefinitionNode, List<BlockItemNode>> transformMPIProcess(SequenceNode<BlockItemNode> root)
			throws SyntaxException {
		List<BlockItemNode> filescopeList = new LinkedList<>();
		List<BlockItemNode> processList = new LinkedList<>();
		int commTypeIndex = -1, mpiInitIndex = -1;
		boolean commCreated = false, newMPIinitMoved = false, commDestroyedMoved = false;
		String gcommStructName = null;
		// If MPI_COMM_SELF is used, we create it:
		boolean useMpiCommSelf = useMpiCommSelf(root);
		// Need to move the declaration of the function $mpi_comm_self_create
		// forward, so we take the decl off the tree:
		BlockItemNode mpiCommSelfCreateFunDecl = null;

		for (BlockItemNode child : root) {
			if (child == null)
				continue;
			child.remove();

			String sourceName = child.getSource().getFirstToken().getSourceFile().getName();

			// Any definition in a file listed here will be moved to file scope:
			switch (sourceName) {
			case "civlc.cvh":
			case "civlc.cvl":
			case "collate.cvh":
			case "collate.cvl":
			case "bundle.cvh":
			case "bundle.cvl": // added by SFS
			case "cmon.cvh": // added by SFS
			case "cmon.cvl": // added by SFS
			case "comm.cvh":
			case "comm.cvl":
			case "complex.cvh": // added by SFS
			case "complex.cvl": // added by SFS
			case "complex.h": // added by SFS
			case "concurrency.cvh": // added by SFS
			case "concurrency.cvl": // added by SFS
			case "math.cvl": // added by SFS
			case "math.h": // added by SFS
			case "mpi-defs.cvh": // added by SFS
			case "mpi-defs.cvl": // added by SFS
			case "op.h":
			case "pointer.cvh":
			case "seq.cvh":
			case "seq.cvl":
			case "string.h": // added by SFS
			case "string.cvl": // added by SFS
				filescopeList.add(child);
				continue;
			default:
			}
			if (child instanceof VariableDeclarationNode) {
				VariableDeclarationNode variable = (VariableDeclarationNode) child;
				TypeNode type = variable.getTypeNode();
				String name = variable.getName();

				if (type.isInputQualified() || type.isOutputQualified())
					filescopeList.add(child);
				else if (name.equals(MPI_SIZE_MAP)) {
					filescopeList.add(variable);
					filescopeList
							.add(nodeFactory
									.newExpressionStatementNode(functionCall(variable.getSource(), SEQ_INIT,
											Arrays.asList(nodeFactory.newOperatorNode(variable.getSource(),
													Operator.ADDRESSOF, this.identifierExpression(variable.getName())),
													this.integerConstant(0), nullPointer()))));
				} else if (name.equals(MPI_STATE_VAR)) {
					processList.add(mpiInitIndex - 1, child);
				} else if (name.equals(MPI_COMM_WORLD)) {
					// ignore original MPI_COMM_WORLD declaration
				} else if (useMpiCommSelf && name.equals(MPI_COMM_SELF)) {
					// ignore original MPI_COMM_SELF declaration, if the source
					// program uses it because we are going to create it in
					// _mpi_process_()
				} else
					processList.add(child);
			} else if (child instanceof ExpressionStatementNode) {
				ExpressionStatementNode exprStmt = (ExpressionStatementNode) child;
				ExpressionNode expression = exprStmt.getExpression();
				boolean assumptionOnInputs = false;

				if (expression instanceof FunctionCallNode) {
					FunctionCallNode functionCall = (FunctionCallNode) expression;
					ExpressionNode functionExpr = functionCall.getFunction();

					if (functionExpr instanceof IdentifierExpressionNode
							&& ((IdentifierExpressionNode) functionExpr).getIdentifier().name().equals(ASSUME)
							&& this.refersInputVariable(functionCall.getArguments())) {
						assumptionOnInputs = true;
					}
				}
				if (assumptionOnInputs)
					filescopeList.add(child);
				else
					processList.add(child);
			} else if (child instanceof FunctionDeclarationNode) {
				FunctionDeclarationNode function = (FunctionDeclarationNode) child;
				String name = function.getName();

				if (name.equals(GCOMM_CREATE) || name.equals(GCOMM_DESTROY) || name.equals(MPI_COROUTINE_NAME)
						|| name.equals(ASSUME))
					filescopeList.add(child);
				else if (!commCreated && name.equals(COMM_CREATE)) {
					commCreated = true;
					processList.add(commTypeIndex, child);
					processList.add(commTypeIndex + 1, this.commDeclaration());
				} else if (useMpiCommSelf && mpiCommSelfCreateFunDecl == null && name.equals(COMM_SELF_CREATE)) {
					mpiCommSelfCreateFunDecl = child;
					child.remove();
				} else if (!commDestroyedMoved && name.equals(COMM_DESTROY)) {
					int commDestroyIndex = commTypeIndex + 2, mpiInitNextIndex = mpiInitIndex + 1;

					if (mpiInitNextIndex > commDestroyIndex)
						commDestroyIndex = mpiInitNextIndex;
					commDestroyedMoved = true;
					processList.add(commDestroyIndex, child);
				} else if (!newMPIinitMoved && name.equals(MPI_INIT_NEW)) {
					newMPIinitMoved = true;
					processList.add(mpiInitIndex, child);
				} else if (child.nodeKind() == NodeKind.FUNCTION_DEFINITION && name.equals(MAIN)) {
					FunctionDefinitionNode functionNode = (FunctionDefinitionNode) child;

					processList.add(functionNode.getBody().copy());
				} else
					processList.add(child);
				if (mpiInitIndex == -1 && name.equals(MPI_INIT))
					mpiInitIndex = processList.size();
			} else if (child instanceof TypedefDeclarationNode) {
				TypedefDeclarationNode typedef = (TypedefDeclarationNode) child;
				String name = typedef.getName();

				if (name.equals(MPI_DATATYPE) || name.equals(MPI_DATASIZE_T))
					filescopeList.add(child);
				else if (name.equals(GCOMM_TYPE)) {
					// gcommStructName
					StructureOrUnionTypeNode struct = (StructureOrUnionTypeNode) typedef.getTypeNode();

					gcommStructName = struct.getName();
					filescopeList.add(child);
				} else if (name.equals(MPI_STATE_TYPE)) {
					processList.add(mpiInitIndex - 5, child);
				} else
					processList.add(child);
				if (commTypeIndex == -1 && name.equals(COMM_TYPE))
					commTypeIndex = processList.size();
			} else if (gcommStructName != null && child instanceof StructureOrUnionTypeNode) {
				StructureOrUnionTypeNode struct = (StructureOrUnionTypeNode) child;
				String name = struct.getName();

				if (name.equals(gcommStructName) || name.equals(MPI_DATASIZE_STRUCT))
					filescopeList.add(child);
				else
					processList.add(child);
			} else
				processList.add(child);
		}
		processList.add(this.commDestroy(COMM_DESTROY, MPI_COMM_WORLD));
		if (useMpiCommSelf) {
			processList.add(this.commDestroy(COMM_DESTROY, MPI_COMM_SELF));
			processList.add(this.commDestroy(GCOMM_DESTROY, GCOMM_SELF));
			processList.addAll(commTypeIndex + 1, commSelfDeclaration());
			processList.add(commTypeIndex + 1, mpiCommSelfCreateFunDecl);
		}

		CompoundStatementNode mpiProcessBody = nodeFactory.newCompoundStatementNode(
				this.newSource("function body of " + MPI_PROCESS, CivlcTokenConstant.COMPOUND_STATEMENT), processList);
		SequenceNode<VariableDeclarationNode> formals = nodeFactory.newSequenceNode(
				this.newSource("formal parameters of function " + MPI_PROCESS, CivlcTokenConstant.DECLARATION_LIST),
				"FormalParameterDeclarations",
				Arrays.asList(this.variableDeclaration(MPI_RANK, this.basicType(BasicTypeKind.INT))));
		FunctionTypeNode mpiProcessType = nodeFactory.newFunctionTypeNode(
				this.newSource("type of function " + MPI_PROCESS, CivlcTokenConstant.TYPE), this.voidType(), formals,
				true);
		FunctionDefinitionNode mpiProcess = nodeFactory.newFunctionDefinitionNode(
				this.newSource("definition of function", CivlcTokenConstant.FUNCTION_DEFINITION),
				this.identifier(MPI_PROCESS), mpiProcessType, null, mpiProcessBody);

		return new Pair<>(mpiProcess, filescopeList);
	}

	/**
	 * Declare a variable of a basic type with a specific name.
	 * 
	 * @param type The kind of basic type.
	 * @param name The name of the variable.
	 * @return The variable declaration node.
	 */
	private VariableDeclarationNode basicTypeVariableDeclaration(BasicTypeKind type, String name) {
		TypeNode typeNode = this.basicType(type);

		return this.variableDeclaration(name, typeNode);
	}

	/**
	 * Creates the declaration node for the input variable <code>NPROCS</code>.
	 * 
	 * @return The declaration node of the input variable <code>NPROCS</code>.
	 */
	private VariableDeclarationNode nprocsDeclaration() {
		TypeNode nprocsType = this.basicType(BasicTypeKind.INT);
		nprocsType.setInputQualified(true);
		return this.variableDeclaration(NPROCS, nprocsType);
	}

	/**
	 * Scans all children nodes to do preprocessing. Currently, only one kind of
	 * processing is performed, i.e., translating all <code>MPI_Init(...)</code>
	 * function call into <code>$mpi_init()</code>.
	 * 
	 * @param node The AST node to be checked and all its children will be scanned.
	 * @throws SyntaxException
	 */
	private void transformMPI_Init(ASTNode node) throws SyntaxException {
		int numChildren = node.numChildren();

		for (int i = 0; i < numChildren; i++) {
			ASTNode child = node.child(i);

			if (child != null)
				this.transformMPI_Init(node.child(i));
		}
		if (node instanceof FunctionCallNode) {
			this.transformMPI_InitCall((FunctionCallNode) node);
		}

	}

	/**
	 * 
	 * Translates an <code>MPI_Init(arg0, arg1)</code> function call into
	 * <code>$mpi_init()</code>.
	 * 
	 * 
	 * @param functionCall
	 */
	private void transformMPI_InitCall(FunctionCallNode functionCall) {
		if (functionCall.getFunction().expressionKind() == ExpressionKind.IDENTIFIER_EXPRESSION) {
			IdentifierExpressionNode functionExpression = (IdentifierExpressionNode) functionCall.getFunction();
			String functionName = functionExpression.getIdentifier().name();
			SequenceNode<ExpressionNode> emptyArgNode = nodeFactory.newSequenceNode(
					newSource("empty parameter list of " + MPI_INIT_NEW, CivlcTokenConstant.ARGUMENT_LIST),
					"EmptyParameterList", new LinkedList<ExpressionNode>());

			if (functionName.equals(MPI_INIT)) {
				functionExpression.getIdentifier().setName(MPI_INIT_NEW);
				functionCall.setArguments(emptyArgNode);
			}
			// else if (functionName.equals(MPI_FINALIZE)) {
			// functionExpression.getIdentifier().setName(MPI_FINALIZE_NEW);
			// functionCall.setArguments(emptyArgNode);
			// }
		}
	}

	/**
	 * Creates the assumption node for NPROCS.
	 * 
	 * @return the assumption node of NPROCS, null if the input variable list
	 *         already contains NPROCS.
	 * @throws SyntaxException
	 */
	private ExpressionStatementNode nprocsAssumption() throws SyntaxException {
		return this.boundAssumption(NPROCS_LOWER_BOUND, NPROCS, NPROCS_UPPER_BOUND);
	}

	private VariableDeclarationNode getVariabledeclaration(ASTNode root, String name) {
		for (ASTNode node : root.children()) {
			if (node != null && node.nodeKind() == NodeKind.VARIABLE_DECLARATION) {
				VariableDeclarationNode varNode = (VariableDeclarationNode) node;

				if (varNode.getName().equals(name)) {
					return varNode;
				}
			}
		}
		return null;
	}

	/**
	 * transforms exit(k); to $mpi_destroy(MPI_COMM_WORLD, $mpi_state); exit(k);
	 * 
	 * @param node the node to be transformed
	 */
	private void transformExit(ASTNode node) {
		if (node instanceof ExpressionStatementNode) {
			ExpressionNode expr = ((ExpressionStatementNode) node).getExpression();

			if (expr instanceof FunctionCallNode) {
				ExpressionNode function = ((FunctionCallNode) expr).getFunction();

				if (function instanceof IdentifierExpressionNode) {
					String funcName = ((IdentifierExpressionNode) function).getIdentifier().name();

					if (funcName.equals(EXIT)) {
						BlockItemNode commDestroy = this.commDestroy(COMM_DESTROY, MPI_COMM_WORLD);
						int nodeIndex = node.childIndex();
						ASTNode parent = node.parent();
						List<BlockItemNode> newItems = new LinkedList<>();

						node.remove();
						newItems.add(commDestroy);
						newItems.add((BlockItemNode) node);
						parent.setChild(nodeIndex, nodeFactory.newCompoundStatementNode(node.getSource(), newItems));
					}
				}
			}
		} else
			for (ASTNode child : node.children()) {
				if (child != null)
					transformExit(child);
			}
	}

	/**
	 * @return true iff ast contains an identifier expression of name
	 *         "MPI_COMM_SELF".
	 */
	private boolean useMpiCommSelf(ASTNode node) {
		while (node != null) {
			if (node instanceof IdentifierExpressionNode) {
				Entity entity = ((IdentifierExpressionNode) node).getIdentifier().getEntity();
				if (entity != null && entity.getName().equals(MPI_COMM_SELF))
					return true;
			}
			node = node.nextDFS();
		}
		return false;
	}

	/* ********************* Methods From BaseTransformer ****************** */

	/**
	 * Transform an AST of a pure MPI program in C into an equivalent AST of CIVL-C
	 * program.<br>
	 * Given an MPI program:<br>
	 * 
	 * <pre>
	 * #include &lt;mpi.h>
	 * ...
	 * #include &lt;stdio.h>
	 * int a, b, ...;
	 * ... function(){
	 *   ...
	 * }
	 * ...
	 * int main(){
	 *   ....
	 * }
	 * </pre>
	 * 
	 * It is translated to the following program:<br>
	 * 
	 * <pre>
	 * #include &lt;mpi.h> // all included files are moved above to the new file scope.
	 * ...
	 * #include &lt;stdio.h>
	 * $input int argc;//optional, only necessary when the original main function has arguments.
	 * $input char** argv;//optional, only necessary when the original main function has arguments.
	 * $input int NPROCS;
	 * $gcomm GCOMM_WORLD = $gcomm_create($here, NPROCS);
	 * 
	 * void MPI_Process(int _rank){
	 *   ...
	 * }
	 * void main(){
	 *   $proc procs[NPROCS];
	 *   for(int i = 0; i < NPROCS; i++)
	 *     procs[i] = $spawn MPI_Process(i);
	 *   for(int i = 0; i < NPROCS; i++)
	 *     $wait(procs[i]);
	 *   $gcomm_destroy(GCOMM_WORLD);
	 * }
	 * </pre>
	 * 
	 * Whereas MPI_Process() is a wrapper of the original MPI program with some
	 * special handling:<br>
	 * 
	 * <pre>
	 * void MPI_Process(){
	 *   $comm MPI_COMM_WORLD = $comm_create(...);
	 *   //SLIGHTLY-MODIFIED ORIGINAL PROGRAM;
	 *   int a, b, ...;
	 *   ... function(){...}
	 *   ...
	 *   ... __main(){...} // renamed main() to __main()
	 *   ....
	 *   //ORIGINAL PROGRAM ENDS HERE;
	 *   __main();
	 *   $comm_destroy(MPI_COMM_WORLD);
	 * }
	 * </pre>
	 * 
	 * @param ast The AST of the original MPI program in C.
	 * @return An AST of CIVL-C program equivalent to the original MPI program.
	 * @throws SyntaxException
	 */
	@Override
	protected AST transformCore(AST ast) throws SyntaxException {
		if (!this.hasHeader(ast, MPI_HEADER))
			return ast;

		SequenceNode<BlockItemNode> root = ast.getRootNode();
		AST newAst;
		FunctionDefinitionNode mpiProcess, mainFunction;
		VariableDeclarationNode gcommWorld;
		List<BlockItemNode> externalList = new LinkedList<>();
		SequenceNode<BlockItemNode> newRootNode;
		List<BlockItemNode> mainParametersAndAssumps = new ArrayList<>();
		int count;
		StatementNode nprocsAssumption = null;
		Pair<FunctionDefinitionNode, List<BlockItemNode>> result;
		VariableDeclarationNode nprocsVar = this.getVariabledeclaration(root, NPROCS);
		VariableDeclarationNode nprocsUpperBoundVar = this.getVariabledeclaration(root, NPROCS_UPPER_BOUND);
		VariableDeclarationNode nprocsLowerBoundVar = this.getVariabledeclaration(root, NPROCS_LOWER_BOUND);
		VariableDeclarationNode rootVar = this.variableDeclaration(PROCESS_ROOT,
				this.typeNode(this.astFactory.getTypeFactory().scopeType()),
				this.nodeFactory.newHereNode(this.newSource("root", 0)));

		assert this.astFactory == ast.getASTFactory();
		assert this.nodeFactory == astFactory.getNodeFactory();
		ast.release();
		if (!this.has_gen_mainFunction(root)) {
			transformMainFunction(root);
			createNewMainFunction(root);
		}
		transformMPI_Init(root);
		transformExit(root);
		if (nprocsVar == null) {
			// declaring $input int _mpi_nprocs;
			nprocsVar = this.nprocsDeclaration();
			if (nprocsUpperBoundVar == null) {
				// declaring $input int _mpi_nprocs_hi;
				nprocsUpperBoundVar = this.basicTypeVariableDeclaration(BasicTypeKind.INT, NPROCS_UPPER_BOUND);
				nprocsUpperBoundVar.getTypeNode().setInputQualified(true);
			} else {
				nprocsUpperBoundVar.parent().removeChild(nprocsUpperBoundVar.childIndex());
			}
			if (nprocsLowerBoundVar == null) {
				Source lowerBoundSource = this.newSource("constant integer: one", CivlcTokenConstant.INT);
				Source intTypeSource = this.newSource("int", CivlcTokenConstant.TYPE);

				// declaring $input int _mpi_nprocs_lo;
				nprocsLowerBoundVar = this.variableDeclaration(NPROCS_LOWER_BOUND,
						nodeFactory.newBasicTypeNode(intTypeSource, BasicTypeKind.INT),
						this.nodeFactory.newIntegerConstantNode(lowerBoundSource, "1"));
				nprocsLowerBoundVar.getTypeNode().setInputQualified(true);
			} else {
				nprocsLowerBoundVar.parent().removeChild(nprocsLowerBoundVar.childIndex());
			}
			// assuming _mpi_nprocs_lo <= _mpi_nprocs && _mpi_nprocs <=
			// _mpi_nprocs_hi
			nprocsAssumption = this.nprocsAssumption();
		} else {
			nprocsVar.parent().removeChild(nprocsVar.childIndex());
		}
		// declaring $gcomm
		gcommWorld = this.gcommDeclaration();
		result = this.transformMPIProcess(root);
		mpiProcess = result.left;
		mainParametersAndAssumps = result.right;
		// creating the new main function;
		mainFunction = mainFunction();
		count = mainParametersAndAssumps.size();
		// A flag indicating if the _mpi_nprocs declaration is inserted already
		// for the following code:
		boolean nprocsTouched = false;

		// adding nodes from the arguments of the original main function.
		for (int i = 0; i < count; i++) {
			BlockItemNode node = mainParametersAndAssumps.get(i);

			// The _mpi_nprocs declaration must be inserted before it is
			// referenced
			if (!nprocsTouched && isRelatedAssumptionNode(node, Arrays.asList(NPROCS))) {
				externalList.add(nprocsVar);
				nprocsTouched = true;
			}
			externalList.add(node);
		}
		// If _mpi_nprocs declaration is not touched previously, then add it
		// here:
		if (!nprocsTouched)
			externalList.add(nprocsVar);
		if (nprocsLowerBoundVar != null)
			externalList.add(nprocsLowerBoundVar);
		if (nprocsUpperBoundVar != null)
			externalList.add(nprocsUpperBoundVar);
		if (nprocsAssumption != null)
			externalList.add(nprocsAssumption);
		externalList.add(rootVar);
		externalList.add(gcommWorld);
		externalList.add(this.gcommsSeqDeclaration());
		externalList.add(mpiProcess);
		externalList.add(mainFunction);
		newRootNode = nodeFactory.newSequenceNode(null, "TranslationUnit", externalList);
		newAst = astFactory.newAST(newRootNode, ast.getSourceFiles(), ast.isWholeProgram());
		// newAst.prettyPrint(System.out, false);
		return newAst;
	}
}
