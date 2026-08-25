package dev.civl.mc.transform.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
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
import dev.civl.abc.ast.node.IF.statement.ReturnNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.value.IF.StringValue;
import dev.civl.abc.err.IF.ABCUnsupportedException;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.StringLiteral;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.model.IF.ModelConfiguration;
import dev.civl.mc.transform.IF.IOTransformer;
import dev.civl.mc.transform.IF.TransformerFactory;

/**
 * The IO transformer transforms<br>
 * <ul>
 * <li>function calls
 * <ul>
 * <li>all function calls printf(...) into frpintf(stdout, ...)</li>
 * <li>all function calls scanf(...) into fscanf(stdin, ...)</li>
 * <li>all function calls fopen(...) into $fopen(...)</li>
 * </ul>
 * </li>
 * <li>new global variable declaration
 * <ul>
 * <li><code>$filesystem CIVL_filesystem = $filesystem_create($here);</code>
 * </li>
 * <li><code>$output $file  CIVL_output_filesystem[];</code></li>
 * <li>
 * <code>extern FILE* stdout = $fopen(CIVL_filesystem, "CIVL_stdout", CIVL_FILE_MODE_WX);</code>
 * </li>
 * <li><code></code>extern FILE* stderr = $fopen(CIVL_filesystem, "CIVL_stderr",
 * CIVL_FILE_MODE_WX);</li>
 * <li>
 * <code>extern FILE* stdin = $fopen(CIVL_filesystem, "CIVL_stdin", CIVL_FILE_MODE_R);</code>
 * </li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author zmanchun
 * 
 */
public class IOWorker extends BaseWorker {

	/*
	 * ************************** Private Static Fields **********************
	 */
	/**
	 * The name of printf function.
	 */
	private static String PRINTF = "printf";

	/**
	 * The name of fprintf function.
	 */
	private static String FPRINTF = "fprintf";

	/**
	 * The name of scanf function.
	 */
	private static String SCANF = "scanf";

	/**
	 * The name of fscanf function.
	 */
	private static String FSCANF = "fscanf";

	private static String FFLUSH = "fflush";

	private static String FFLUSH_NEW = "_fflush";

	/**
	 * The name of fopen function.
	 */
	private static String FOPEN = "fopen";

	/**
	 * The name of $fopen function (CIVL's stdio implementation).
	 */
	private static String FOPEN_NEW = "$fopen";

	/**
	 * The name of $text_file_length function.
	 */
	private static String TEXT_FILE_LENGTH = "$text_file_length";

	/**
	 * The name of $text_file_length function (CIVL's stdio implementation).
	 */
	private static String TEXT_FILE_LENGTH_NEW = "$civl_text_file_length";

	private static String IO_HEADER = "stdio.h";

	private static final String FILE_SYSTEM_TYPE = "$filesystem";

	private static final String STD_FILE_TYPE = "FILE";

	private static final String FILESYSTEM_CREATE = "$filesystem_create";

	private static final String FILE_TYPE = "$file";

	private static final String OUTPUT_FILESYSTEM = "_io_output_filesystem";

	/**
	 * The representation of the 16 modes of opening a file in CIVL.
	 */
	private static String CIVL_FILE_MODE_R = "CIVL_FILE_MODE_R";// r
	private static String CIVL_FILE_MODE_W = "CIVL_FILE_MODE_W";// w
	private static String CIVL_FILE_MODE_WX = "CIVL_FILE_MODE_WX";// wx
	private static String CIVL_FILE_MODE_A = "CIVL_FILE_MODE_A";// a
	private static String CIVL_FILE_MODE_RB = "CIVL_FILE_MODE_RB";// rb
	private static String CIVL_FILE_MODE_WB = "CIVL_FILE_MODE_WB";// wb
	private static String CIVL_FILE_MODE_WBX = "CIVL_FILE_MODE_WBX";// wbx
	private static String CIVL_FILE_MODE_AB = "CIVL_FILE_MODE_AB";// ab
	private static String CIVL_FILE_MODE_RP = "CIVL_FILE_MODE_RP";// r+
	private static String CIVL_FILE_MODE_WP = "CIVL_FILE_MODE_WP";// w+
	private static String CIVL_FILE_MODE_WPX = "CIVL_FILE_MODE_WPX";// w+x
	private static String CIVL_FILE_MODE_AP = "CIVL_FILE_MODE_AP";// a+
	private static String CIVL_FILE_MODE_RPB = "CIVL_FILE_MODE_RPB";// r+b or
																	// rb+
	private static String CIVL_FILE_MODE_WPB = "CIVL_FILE_MODE_WPB";// w+b or
																	// wb+
	private static String CIVL_FILE_MODE_WPBX = "CIVL_FILE_MODE_WPBX";// w+bx or
																		// wb+x
	private static String CIVL_FILE_MODE_APB = "CIVL_FILE_MODE_APB";// a+b or
																	// ab+

	/**
	 * The string representation of the 16 modes of opening of a file in C.
	 */
	private static String FOPEN_R = "r";
	private static String FOPEN_W = "w";
	private static String FOPEN_WX = "wx";
	private static String FOPEN_A = "a";
	private static String FOPEN_RB = "rb";
	private static String FOPEN_WB = "wb";
	private static String FOPEN_WBX = "wbx";
	private static String FOPEN_AB = "ab";
	private static String FOPEN_RP = "r+";
	private static String FOPEN_WP = "w+";
	private static String FOPEN_WPX = "w+x";
	private static String FOPEN_AP = "a+";
	private static String FOPEN_RPB = "r+b";
	private static String FOPEN_RBP = "rb+";
	private static String FOPEN_WPB = "w+b";
	private static String FOPEN_WBP = "wb+";
	private static String FOPEN_WPBX = "w+bx";
	private static String FOPEN_WBPX = "wb+x";
	private static String FOPEN_APB = "a+b";
	private static String FOPEN_ABP = "ab+";
	private static String TEXTFILE_MODE = "t";

	/**
	 * The CIVL system function to destroy a file system.
	 */
	final static String FILESYSTEM_DESTROY = "$filesystem_destroy";

	/**
	 * The CIVL system function to copy all files of a file system to an array.
	 */
	private final static String FILESYSTEM_COPY_OUTPUT = "$filesystem_copy_output";

	/**
	 * The CIVL system function to destroy a memory unit through a pointer.
	 */
	final static String FREE = "$free";

	/**
	 * The name of the default file system in the root scope.
	 */
	final static String FILESYSTEM = ModelConfiguration.FILESYSTEM;

	// /**
	// * The name of the output array of files.
	// */
	// private final static String CIVL_OUTPUT_FILESYSTEM =
	// "CIVL_output_filesystem";

	/**
	 * The name of stdin.
	 */
	static final String STDIN = "stdin";
	/**
	 * The name of stdout.
	 */
	static final String STDOUT = "stdout";
	/**
	 * The name of stderr.
	 */
	static final String STDERR = "stderr";

	private final static String EXIT = "exit";

	private final static String CIVL_EXIT = "$exit";

	private boolean hasCIVLfileSystem = false;

	private boolean hasStdout = false;
	private boolean hasStdin = false;
	private boolean hasStderr = false;
	private TypeNode filesystemType;
	private TypeNode fileType;
	private TypeNode stdFileType;
	private BlockItemNode fopen_declaration;

	// private BlockItemNode filesystem_variable;

	/* ****************************** Constructor ************************** */

	/**
	 * Creates a new instance of IO transformer.
	 * 
	 * @param astFactory     The AST factory to be used.
	 * @param inputVariables The input variables specified from command line.
	 * @param config         The CIVL configuration.
	 */
	public IOWorker(ASTFactory astFactory, CIVLConfiguration config) {
		super(IOTransformer.LONG_NAME, astFactory);
		this.identifierPrefix = "_io_";
	}

	/* *************************** Private Methods ************************* */

	/**
	 * Adds function calls before every return statement of the main function to
	 * copy the file system to the output variable and deallocate the memory space
	 * created previously for file system and files (otherwise, there will be memory
	 * leak problem).
	 * 
	 * @param rootNode The root node of the AST.
	 * @throws SyntaxException
	 */
	private void transformMain(ASTNode rootNode) throws SyntaxException {
		int numChildren = rootNode.numChildren();

		for (int i = 0; i < numChildren; i++) {
			ASTNode child = rootNode.child(i);

			if (child instanceof FunctionDefinitionNode) {
				FunctionDefinitionNode function = (FunctionDefinitionNode) child;
				String functionName = function.getName();

				if (functionName.equals("main")) {
					List<BlockItemNode> finalCalls = new ArrayList<>();
					Source source = rootNode.getSource();
					List<StatementNode> freeStatements = this.freeCalls(rootNode.getSource());
					CompoundStatementNode body = function.getBody();
					BlockItemNode lastStatement = body.getSequenceChild(body.numChildren() - 1);
					StatementNode finalStatements;

					finalCalls.add(this.copyFilesToOutput(source));
					finalCalls.addAll(freeStatements);
					finalStatements = nodeFactory.newCompoundStatementNode(source, finalCalls);
					if (!(lastStatement instanceof ReturnNode)) {
						body.addSequenceChild(finalStatements.copy());
					}
					this.addFreeBeforeReturn(body, finalStatements);
				}
			}
		}
	}

	/**
	 * Add calls to deallocate stdout/stdin/stderr before any exit(k).
	 * 
	 * @param node
	 * @throws SyntaxException
	 */
	private void transformExit(ASTNode node) throws SyntaxException {
		if (node instanceof ExpressionStatementNode) {
			ExpressionNode expr = ((ExpressionStatementNode) node).getExpression();

			if (expr instanceof FunctionCallNode) {
				ExpressionNode function = ((FunctionCallNode) expr).getFunction();

				if (function instanceof IdentifierExpressionNode) {
					String funcName = ((IdentifierExpressionNode) function).getIdentifier().name();

					if (funcName.equals(EXIT) || funcName.equals(CIVL_EXIT)) {
						String sourceFile = node.getSource().getFirstToken().getSourceFile().getName();

						if (sourceFile.equals("pthread.cvl"))
							return;
						// dont transform $exit() when it is in the function
						// body of exit() in stdlib.cvl
						else if (funcName.equals(CIVL_EXIT) && sourceFile.equals("stdlib.cvl")
								&& node.parent().parent() instanceof FunctionDefinitionNode) {
							FunctionDefinitionNode funcDef = (FunctionDefinitionNode) node.parent().parent();

							if (funcDef.getName().equals(EXIT))
								return;
						}

						int nodeIndex = node.childIndex();
						ASTNode parent = node.parent();
						Source source = node.getSource();
						List<BlockItemNode> newItems = new LinkedList<>();

						node.remove();
						if (this.hasCIVLfileSystem)
							newItems.add(this.copyFilesToOutput(source));
						newItems.addAll(this.freeCalls(source));
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
	 * Creates the statement to copy the file system to the corresponding output
	 * array of files, which is a function call to the system function
	 * <code>$filesystem_copy_output</code>.
	 * 
	 * @param source The source to be used for creating new nodes.
	 * @return The statement to copy the file system to the corresponding output
	 *         variable.
	 */
	private StatementNode copyFilesToOutput(Source source) {
		ExpressionNode civlFileSystem = nodeFactory.newIdentifierExpressionNode(source,
				nodeFactory.newIdentifierNode(source, FILESYSTEM));
		ExpressionNode outputArray = nodeFactory.newIdentifierExpressionNode(source,
				nodeFactory.newIdentifierNode(source, OUTPUT_FILESYSTEM));
		FunctionCallNode copyCall = nodeFactory.newFunctionCallNode(source,
				nodeFactory.newIdentifierExpressionNode(source,
						nodeFactory.newIdentifierNode(source, FILESYSTEM_COPY_OUTPUT)),
				Arrays.asList(civlFileSystem, outputArray));

		return nodeFactory.newExpressionStatementNode(copyCall);
	}

	/**
	 * Creates all necessary function calls of deallocating memory space, including:
	 * 
	 * <pre>
	 * $filesystem_destroy(CIVL_filesystem);
	 * if (stdout != null)
	 * 	$free(stdout);
	 * if (stdin != null)
	 * 	$free(stdin);
	 * if (stderr != null)
	 * 	$free(stderr);
	 * </pre>
	 * 
	 * @param source The source to be used for creating new nodes.
	 * @return A list of necessary function calls for deallocating memory space.
	 * @throws SyntaxException
	 */
	private List<StatementNode> freeCalls(Source source) throws SyntaxException {
		List<StatementNode> result = new ArrayList<>();
		ExpressionStatementNode freeFilesystemStatement, freeStdoutStatement, freeStdinStatement, freeStderrStatement;
		StatementNode ifStdoutStatement, ifStdinStatement, ifStderrStatement;
		FunctionCallNode freeFilesystem, freeStdout, freeStdin, freeStderr;
		ExpressionNode condition;
		ExpressionNode argument = nodeFactory.newIdentifierExpressionNode(source,
				nodeFactory.newIdentifierNode(source, FILESYSTEM));
		ExpressionNode nullPointer;

		if (this.hasCIVLfileSystem) {
			freeFilesystem = nodeFactory.newFunctionCallNode(source, nodeFactory.newIdentifierExpressionNode(source,
					nodeFactory.newIdentifierNode(source, FILESYSTEM_DESTROY)), Arrays.asList(argument));
			freeFilesystemStatement = nodeFactory.newExpressionStatementNode(freeFilesystem);
			result.add(freeFilesystemStatement);
		}
		if (this.hasStdout) {
			argument = nodeFactory.newIdentifierExpressionNode(source, nodeFactory.newIdentifierNode(source, STDOUT));
			freeStdout = nodeFactory.newFunctionCallNode(source,
					nodeFactory.newIdentifierExpressionNode(source, nodeFactory.newIdentifierNode(source, FREE)),
					Arrays.asList(argument));
			freeStdoutStatement = nodeFactory.newExpressionStatementNode(freeStdout);
			nullPointer = nodeFactory.newCastNode(source,
					nodeFactory.newPointerTypeNode(source, nodeFactory.newVoidTypeNode(source)),
					nodeFactory.newIntegerConstantNode(source, "0"));
			condition = nodeFactory.newOperatorNode(source, Operator.NEQ, Arrays.asList(argument.copy(), nullPointer));
			ifStdoutStatement = nodeFactory.newIfNode(source, condition, freeStdoutStatement);
			result.add(ifStdoutStatement);
		}
		if (this.hasStdin) {
			argument = nodeFactory.newIdentifierExpressionNode(source, nodeFactory.newIdentifierNode(source, STDIN));
			freeStdin = nodeFactory.newFunctionCallNode(source,
					nodeFactory.newIdentifierExpressionNode(source, nodeFactory.newIdentifierNode(source, FREE)),
					Arrays.asList(argument));
			freeStdinStatement = nodeFactory.newExpressionStatementNode(freeStdin);
			nullPointer = nodeFactory.newCastNode(source,
					nodeFactory.newPointerTypeNode(source, nodeFactory.newVoidTypeNode(source)),
					nodeFactory.newIntegerConstantNode(source, "0"));
			condition = nodeFactory.newOperatorNode(source, Operator.NEQ, Arrays.asList(argument.copy(), nullPointer));
			ifStdinStatement = nodeFactory.newIfNode(source, condition, freeStdinStatement);
			result.add(ifStdinStatement);
		}
		if (this.hasStderr) {
			argument = nodeFactory.newIdentifierExpressionNode(source, nodeFactory.newIdentifierNode(source, STDERR));
			freeStderr = nodeFactory.newFunctionCallNode(source,
					nodeFactory.newIdentifierExpressionNode(source, nodeFactory.newIdentifierNode(source, FREE)),
					Arrays.asList(argument));
			freeStderrStatement = nodeFactory.newExpressionStatementNode(freeStderr);
			nullPointer = nodeFactory.newCastNode(source,
					nodeFactory.newPointerTypeNode(source, nodeFactory.newVoidTypeNode(source)),
					nodeFactory.newIntegerConstantNode(source, "0"));
			condition = nodeFactory.newOperatorNode(source, Operator.NEQ, Arrays.asList(argument.copy(), nullPointer));
			ifStderrStatement = nodeFactory.newIfNode(source, condition, freeStderrStatement);
			result.add(ifStderrStatement);
		}
		return result;
	}

	/**
	 * Translates all function call nodes of <code>printf</code>/
	 * <code>scanf</code>/<code>fopen</code> into <code>fprintf</code>/
	 * <code>fscanf</code>/<code>$fopen</code>.
	 * 
	 * @param node
	 * @throws SyntaxException
	 */
	private void renameFunctionCalls(ASTNode node) throws SyntaxException {
		int numChildren = node.numChildren();

		for (int i = 0; i < numChildren; i++) {
			ASTNode child = node.child(i);

			if (child != null)
				this.renameFunctionCalls(node.child(i));
		}
		if (node instanceof FunctionCallNode) {
			this.processFunctionCall((FunctionCallNode) node);
		}
	}

	private void removeFflushCalls(ASTNode node) throws SyntaxException {
		int numChildren = node.numChildren();

		for (int i = 0; i < numChildren; i++) {
			ASTNode child = node.child(i);

			if (child != null)
				this.removeFflushCalls(node.child(i));
		}
		if (node instanceof FunctionCallNode) {
			this.removeFflushCall((FunctionCallNode) node);
		}
	}

	private void removeFflushCall(FunctionCallNode functionCall) {
		if (functionCall.getFunction().expressionKind() == ExpressionKind.IDENTIFIER_EXPRESSION) {
			IdentifierExpressionNode functionExpression = (IdentifierExpressionNode) functionCall.getFunction();
			String functionName = functionExpression.getIdentifier().name();
			if (functionName.equals(FFLUSH)) {
				functionCall.parent().removeChild(functionCall.childIndex());
			}
		}
	}

	private void addFreeBeforeReturn(ASTNode node, StatementNode functionCallStatement) {
		int numChildren = node.numChildren();

		for (int i = 0; i < numChildren; i++) {
			ASTNode child = node.child(i);

			if (child == null)
				continue;
			if (child instanceof ReturnNode) {
				List<BlockItemNode> statements = new ArrayList<>(2);

				statements.add(functionCallStatement.copy());
				statements.add((ReturnNode) child);
				node.removeChild(i);
				node.setChild(i, nodeFactory.newCompoundStatementNode(child.getSource(), statements));
			} else
				this.addFreeBeforeReturn(node.child(i), functionCallStatement);
		}
	}

	private void processFunctionCall(FunctionCallNode functionCall) throws SyntaxException {
		if (functionCall.getFunction().expressionKind() == ExpressionKind.IDENTIFIER_EXPRESSION) {
			IdentifierExpressionNode functionExpression = (IdentifierExpressionNode) functionCall.getFunction();
			String functionName = functionExpression.getIdentifier().name();
			String firstArgName = "";
			IdentifierNode functionNameIdentifer = functionExpression.getIdentifier();

			if (functionName.equals(PRINTF) || functionName.equals(SCANF)) {
				if (functionName.equals(PRINTF)) {
					functionNameIdentifer.setName(FPRINTF);
					firstArgName = STDOUT;
				} else if (functionName.equals(SCANF)) {
					functionNameIdentifer.setName(FSCANF);
					firstArgName = STDIN;
				}
				processPrintfOrScanf(functionCall, firstArgName);
			} else if (functionName.equals(FOPEN)) {
				functionNameIdentifer.setName(FOPEN_NEW);
				processFopen(functionCall);
			} else if (functionName.equals(FFLUSH)) {
				SequenceNode<ExpressionNode> arguments = nodeFactory.newSequenceNode(
						functionCall.getArgument(0).getSource(), "Actual Parameters", new ArrayList<ExpressionNode>(0));

				functionNameIdentifer.setName(FFLUSH_NEW);
				functionCall.setArguments(arguments);
			} else if (functionName.equals(TEXT_FILE_LENGTH)) {
				// deal with $text_file_length function
				functionNameIdentifer.setName(TEXT_FILE_LENGTH_NEW);
				processTextFileLenth(functionCall);
			}
		}
	}

	/**
	 * $text_file_length(char* file) --> $text_file_length($filesystem fs, char*
	 * file)
	 * 
	 * @param functionCall
	 * @throws SyntaxException
	 */
	private void processTextFileLenth(FunctionCallNode functionCall) throws SyntaxException {
		Source source = functionCall.getFunction().getSource();
		IdentifierExpressionNode civlFileSystem = nodeFactory.newIdentifierExpressionNode(source,
				nodeFactory.newIdentifierNode(source, FILESYSTEM));
		int oldCount = functionCall.getNumberOfArguments();
		List<ExpressionNode> arguments = new ArrayList<>(oldCount + 1);
		ExpressionNode fileNameArg = functionCall.getArgument(0);

		arguments.add(civlFileSystem);
		if (fileNameArg instanceof StringLiteralNode) {
			arguments.add(fileNameArg);
		} else {
			throw new ABCUnsupportedException("non-string-literal file name of $text_file_length");
		}
		for (int i = 0; i < oldCount; i++) {
			ExpressionNode argument = functionCall.getArgument(i);

			argument.parent().removeChild(argument.childIndex());
		}
		functionCall.setArguments(nodeFactory.newSequenceNode(source, "ActualParameterList", arguments));
	}

	/**
	 * fopen(filename, "mode") --> $fopen(CIVL_filesystem, filename, MODE_CONSTANT)
	 * 
	 * @param functionCall
	 * @throws SyntaxException
	 */
	private void processFopen(FunctionCallNode functionCall) throws SyntaxException {
		Source source = functionCall.getFunction().getSource();
		IdentifierExpressionNode civlFileSystem = nodeFactory.newIdentifierExpressionNode(source,
				nodeFactory.newIdentifierNode(source, FILESYSTEM));
		int oldCount = functionCall.getNumberOfArguments();
		List<ExpressionNode> arguments = new ArrayList<>(oldCount + 1);
		ExpressionNode modeArg = functionCall.getArgument(1);

		arguments.add(civlFileSystem);
		arguments.add(functionCall.getArgument(0));
		if (modeArg instanceof StringLiteralNode) {
			StringValue value = ((StringLiteralNode) modeArg).getConstantValue();
			String modeString = this.processFopenMode(stringLiteralToString(value.getLiteral()), modeArg.getSource());

			source = modeArg.getSource();
			arguments.add(nodeFactory.newEnumerationConstantNode(nodeFactory.newIdentifierNode(source, modeString)));
		} else {
			throw new ABCUnsupportedException("non-string-literal file mode of fopen");
		}
		for (int i = 0; i < oldCount; i++) {
			ExpressionNode argument = functionCall.getArgument(i);

			argument.parent().removeChild(argument.childIndex());
		}
		functionCall.setArguments(nodeFactory.newSequenceNode(source, "ActualParameterList", arguments));
	}

	private String stringLiteralToString(StringLiteral stringLiteral) {
		StringBuffer buffer = new StringBuffer();
		int numOfChars = stringLiteral.getNumCharacters();

		for (int i = 0; i < numOfChars - 1; i++) {
			buffer.append(stringLiteral.getCharacter(i));
		}
		return buffer.toString();
	}

	private String processFopenMode(String mode, Source source) throws SyntaxException {
		if (mode.endsWith(TEXTFILE_MODE))
			mode = mode.substring(0, mode.length() - 1);
		if (mode.equals(FOPEN_R))
			return CIVL_FILE_MODE_R;
		if (mode.equals(FOPEN_W))
			return CIVL_FILE_MODE_W;
		if (mode.equals(FOPEN_WX))
			return CIVL_FILE_MODE_WX;
		if (mode.equals(FOPEN_A))
			return CIVL_FILE_MODE_A;
		if (mode.equals(FOPEN_RB))
			return CIVL_FILE_MODE_RB;
		if (mode.equals(FOPEN_WB))
			return CIVL_FILE_MODE_WB;
		if (mode.equals(FOPEN_WBX))
			return CIVL_FILE_MODE_WBX;
		if (mode.equals(FOPEN_AB))
			return CIVL_FILE_MODE_AB;
		if (mode.equals(FOPEN_RP))
			return CIVL_FILE_MODE_RP;
		if (mode.equals(FOPEN_WP))
			return CIVL_FILE_MODE_WP;
		if (mode.equals(FOPEN_WPX))
			return CIVL_FILE_MODE_WPX;
		if (mode.equals(FOPEN_AP))
			return CIVL_FILE_MODE_AP;
		if (mode.equals(FOPEN_RPB) || mode.equals(FOPEN_RBP))
			return CIVL_FILE_MODE_RPB;
		if (mode.equals(FOPEN_WPB) || mode.equals(FOPEN_WBP))
			return CIVL_FILE_MODE_WPB;
		if (mode.equals(FOPEN_WPBX) || mode.equals(FOPEN_WBPX))
			return CIVL_FILE_MODE_WPBX;
		if (mode.equals(FOPEN_APB) || mode.equals(FOPEN_ABP))
			return CIVL_FILE_MODE_APB;
		throw new SyntaxException("Invalid mode " + mode + " of fopen.", source);
	}

	private void processPrintfOrScanf(FunctionCallNode functionCall, String firstArgName) {
		Source source = functionCall.getFunction().getSource();
		IdentifierExpressionNode firstArg = nodeFactory.newIdentifierExpressionNode(source,
				nodeFactory.newIdentifierNode(source, firstArgName));
		int oldCount = functionCall.getNumberOfArguments();
		List<ExpressionNode> arguments = new ArrayList<>(oldCount + 1);

		arguments.add(firstArg);
		for (int i = 0; i < oldCount; i++) {
			ExpressionNode argument = functionCall.getArgument(i);

			argument.parent().removeChild(argument.childIndex());
			arguments.add(argument);
		}
		functionCall.setArguments(nodeFactory.newSequenceNode(source, "ActualParameterList", arguments));
	}

	private void removeNodes(ASTNode node) {
		String sourceFile = node.getSource().getFirstToken().getSourceFile().getName();

		if (sourceFile.equals("stdio.cvl")) {
			node.parent().removeChild(node.childIndex());
		} else {
			for (ASTNode child : node.children()) {
				if (child != null)
					removeNodes(child);
			}
		}
	}

	/**
	 * Checks if IO transformation is necessary. It is necessary if one of the
	 * following is satisfied:
	 * 
	 * <ul>
	 * <li>there is at least one function call of <code>scanf</code>/
	 * <code>fscanf</code>, <code>fopen</code>;
	 * <li>the reference to <code>stderr</code> is present;
	 * <li>there is at least one function call to <code>fprintf</code> whose first
	 * argument is NOT <code>stdout</code>.
	 * </ul>
	 * 
	 * @param unit
	 * @return
	 */
	private boolean isTransformationNeeded(AST unit) {
		boolean hasScanf = TransformerFactory.hasFunctionCalls(unit, Arrays.asList(SCANF, FSCANF, FOPEN));
		boolean hasStderr;

		if (hasScanf)
			return true;
		hasStderr = hasStderr(unit.getRootNode());
		if (hasStderr)
			return true;
		return has_nt_fprintf(unit.getRootNode());
	}

	/**
	 * checks if the given node contains any non-trivial call of fprintf. a fprintf
	 * call is trivial if its first argument is stdout/stderr.
	 * 
	 * @param node
	 * @return
	 */
	private boolean has_nt_fprintf(ASTNode node) {
		if (node instanceof FunctionCallNode) {
			FunctionCallNode funcCall = (FunctionCallNode) node;
			ExpressionNode funcName = funcCall.getFunction();

			if (funcName.expressionKind() == ExpressionKind.IDENTIFIER_EXPRESSION) {
				String name = ((IdentifierExpressionNode) funcName).getIdentifier().name();

				if (name.equals(FPRINTF)) {
					IdentifierExpressionNode arg0 = (IdentifierExpressionNode) funcCall.getArgument(0);

					if (!arg0.getIdentifier().name().equals(STDOUT))
						return true;
				}
			}
		} else {
			for (ASTNode child : node.children()) {
				if (child != null) {
					boolean result = has_nt_fprintf(child);

					if (result)
						return true;
				}
			}
		}
		return false;
	}

	private boolean hasStderr(ASTNode node) {
		if (node instanceof IdentifierExpressionNode) {
			IdentifierExpressionNode idExpr = (IdentifierExpressionNode) node;
			String name = idExpr.getIdentifier().name();

			if (name.equals(STDERR))
				return true;
		} else {
			for (ASTNode child : node.children())
				if (child != null) {
					boolean childResult = hasStderr(child);

					if (childResult)
						return true;
				}
		}
		return false;
	}

	private void processFprintf(ASTNode node) {
		if (node instanceof FunctionCallNode) {
			FunctionCallNode funcCall = (FunctionCallNode) node;
			ExpressionNode funcName = funcCall.getFunction();

			if (funcName.expressionKind() == ExpressionKind.IDENTIFIER_EXPRESSION) {
				String name = ((IdentifierExpressionNode) funcName).getIdentifier().name();

				if (name.equals(FPRINTF)) {
					Source source = node.getSource();
					List<ExpressionNode> arguments = new ArrayList<>(funcCall.getNumberOfArguments() - 1);
					FunctionCallNode printfCall;

					for (int i = 1; i < funcCall.getNumberOfArguments(); i++)
						arguments.add(funcCall.getArgument(i).copy());
					printfCall = nodeFactory.newFunctionCallNode(source,
							this.identifierExpression(funcName.getSource(), PRINTF), arguments);
					node.parent().setChild(node.childIndex(), printfCall);
				}
			}
		} else {
			for (ASTNode child : node.children())
				if (child != null)
					processFprintf(child);
		}
	}

	private void checkStdioVariables(SequenceNode<BlockItemNode> rootNode) {
		int count = 0;

		for (BlockItemNode node : rootNode) {
			if (node instanceof VariableDeclarationNode) {
				String name = ((VariableDeclarationNode) node).getName();

				if (name.equals(FILESYSTEM)) {
					if (!this.hasCIVLfileSystem)
						count++;
					this.hasCIVLfileSystem = true;
				} else if (name.equals(STDOUT)) {
					if (!this.hasStdout)
						count++;
					this.hasStdout = true;
				} else if (name.equals(STDIN)) {
					if (!this.hasStdin)
						count++;
					this.hasStdin = true;
				} else if (name.equals(STDERR)) {
					if (!this.hasStderr)
						count++;
					this.hasStderr = true;
				}
				if (count == 4)
					break;
			}
		}
	}

	private void addGlobalVariables(SequenceNode<BlockItemNode> root) throws SyntaxException {
		List<BlockItemNode> variables = new LinkedList<>();
		VariableDeclarationNode variable;

		variables.add(this.variableDeclaration(FILESYSTEM, this.filesystemType, this.functionCall(
				this.newSource("$filesystem_create", 0), FILESYSTEM_CREATE, Arrays.asList(this.hereNode()))));
		variables.add(this.variableDeclaration(OUTPUT_FILESYSTEM,
				this.nodeFactory.newArrayTypeNode(this.newSource("_io_output_filesystem", 0), this.fileType, null)));
		// CIVL_filesystem, "CIVL_stdout", CIVL_FILE_MODE_WX)
		variable = this.variableDeclaration(STDOUT,
				this.nodeFactory.newPointerTypeNode(this.newSource("type of stdout", 0), this.stdFileType.copy()),
				this.functionCall(this.newSource("create stdout file", 0), FOPEN_NEW,
						Arrays.asList(this.identifierExpression(FILESYSTEM), this.stringLiteral("_stdout"),
								this.nodeFactory.newEnumerationConstantNode(this.identifier(CIVL_FILE_MODE_WX)))));
		variable.setExternStorage(true);
		variables.add(variable);
		variable = this.variableDeclaration(STDIN,
				this.nodeFactory.newPointerTypeNode(this.newSource("type of stdin", 0), this.stdFileType.copy()),
				this.functionCall(this.newSource("create stdin file", 0), FOPEN_NEW,
						Arrays.asList(this.identifierExpression(FILESYSTEM), this.stringLiteral("_stdin"),
								this.nodeFactory.newEnumerationConstantNode(this.identifier(CIVL_FILE_MODE_R)))));
		variable.setExternStorage(true);
		variables.add(variable);
		variable = this.variableDeclaration(STDERR,
				this.nodeFactory.newPointerTypeNode(this.newSource("type of stderr", 0), this.stdFileType.copy()),
				this.functionCall(this.newSource("create stderr file", 0), FOPEN_NEW,
						Arrays.asList(this.identifierExpression(FILESYSTEM), this.stringLiteral("_stderr"),
								this.nodeFactory.newEnumerationConstantNode(this.identifier(CIVL_FILE_MODE_WX)))));
		variable.setExternStorage(true);
		variables.add(variable);
		root.insertChildren(fopen_declaration.childIndex() + 1, variables);
	}

	/* ********************* Methods From BaseTransformer ****************** */

	@Override
	protected AST transformCore(AST unit) throws SyntaxException {
		if (!this.hasHeader(unit, IO_HEADER))
			return unit;

		boolean transformationNeeded = false;
		SequenceNode<BlockItemNode> rootNode = unit.getRootNode();

		transformationNeeded = this.isTransformationNeeded(unit);
		assert this.astFactory == unit.getASTFactory();
		assert this.nodeFactory == astFactory.getNodeFactory();
		unit.release();
		preprocess(rootNode);
		removeFflushCalls(rootNode);
		if (transformationNeeded) {
			addGlobalVariables(rootNode);
			checkStdioVariables(rootNode);
			this.renameFunctionCalls(rootNode);
			this.transformMain(rootNode);
			this.transformExit(rootNode);
		} else {
			// remove nodes from stdio.cvl
			removeNodes(rootNode);
			processFprintf(rootNode);
		}

		AST result = astFactory.newAST(rootNode, unit.getSourceFiles(), unit.isWholeProgram());

		// result.prettyPrint(System.out, false);
		return result;
	}

	private void preprocess(SequenceNode<BlockItemNode> rootNode) {
		for (BlockItemNode item : rootNode) {
			if (item instanceof TypedefDeclarationNode) {
				if (!(this.filesystemType != null && this.fileType != null && this.stdFileType != null)) {
					TypedefDeclarationNode typedef = (TypedefDeclarationNode) item;
					String typeName = typedef.getName();

					if (typeName.equals(FILE_SYSTEM_TYPE)) {
						if (this.filesystemType == null) {
							filesystemType = typedef.getTypeNode().copy();
						}
					} else if (typeName.equals(FILE_TYPE)) {
						if (this.fileType == null) {
							fileType = typedef.getTypeNode().copy();
						}
					} else if (typeName.equals(STD_FILE_TYPE)) {
						if (this.stdFileType == null) {
							stdFileType = typedef.getTypeNode().copy();
						}
					}
				}
			} else if (item instanceof FunctionDeclarationNode) {
				if (((FunctionDeclarationNode) item).getName().equals(FOPEN_NEW))
					this.fopen_declaration = item;
			}
			if (this.filesystemType != null && this.fileType != null && this.stdFileType != null
					&& this.fopen_declaration != null)
				break;
		}
	}
}
