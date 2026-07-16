package dev.civl.mc.run.common;

import static dev.civl.mc.config.IF.CIVLConstants.bar;
import static dev.civl.mc.config.IF.CIVLConstants.macroO;
import static dev.civl.mc.config.IF.CIVLConstants.showProverQueriesO;
import static dev.civl.mc.config.IF.CIVLConstants.showQueriesO;
import static dev.civl.mc.config.IF.CIVLConstants.sysIncludePathO;
import static dev.civl.mc.config.IF.CIVLConstants.userIncludePathO;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.config.IF.Configurations.Language;
import dev.civl.abc.err.IF.ABCException;
import dev.civl.abc.front.IF.PreprocessorException;
import dev.civl.abc.main.ABCExecutor;
import dev.civl.abc.main.FrontEnd;
import dev.civl.abc.main.TranslationTask;
import dev.civl.abc.main.TranslationTask.TranslationStage;
import dev.civl.abc.main.UnitTask;
import dev.civl.abc.program.IF.Program;
import dev.civl.abc.token.IF.FileIndexer;
import dev.civl.abc.transform.common.ExternLinkageVariableRenamer;
import dev.civl.abc.transform.common.GenericSelectionRemover;
import dev.civl.abc.transform.common.Pruner;
import dev.civl.abc.transform.common.SideEffectRemover;
import dev.civl.gmc.CommandLineException;
import dev.civl.gmc.GMCConfiguration;
import dev.civl.gmc.GMCSection;
import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.config.IF.CIVLConstants;
import dev.civl.mc.model.IF.Model;
import dev.civl.mc.model.IF.ModelBuilder;
import dev.civl.mc.model.IF.Models;
import dev.civl.mc.transform.IF.ContractTransformer;
import dev.civl.mc.transform.IF.LoopContractTransformer;
import dev.civl.mc.transform.IF.TransformerFactory;
import dev.civl.mc.transform.IF.Transforms;
import dev.civl.sarl.SARL;
import dev.civl.sarl.IF.SymbolicUniverse;

/**
 * <p>
 * A model translator parses, links, transforms a sequence of source files (C or
 * CIVL-C programs) into a ABC program; and then build a CIVL model from that
 * program. Command line options are also taken into account including macros,
 * transformer settings (e.g., -ompNoSimplify), input variables, system/user
 * include path, etc.
 * </p>
 * 
 * <p>
 * A model translator takes into account a command line section. E.g., the
 * command line
 * 
 * <pre>
   civl compare -D_CIVL -spec -DN=5 sum.c -impl -DCUDA -inputNB=8 sum.c sum_cuda.c
 * </pre>
 * 
 * contains three command line sections: the common section, the "spec" section
 * and the "impl" section. Two model translators will be invoked for translating
 * the specification and the implementation, respectively, taking into account
 * the common command line section and the corresponding specific section
 * (different lists of files, different macros, input variables, etc).
 * </p>
 * 
 * <p>
 * Non-compare command line contains one command line section, and thus only one
 * model translator is created.
 * </p>
 * 
 * <p>
 * Orders of applying transformers:
 * <ol>
 * <li>General Transformer</li>
 * <li>IO Transformer</li>
 * <li>OpenMP Transformer, CUDA Transformer, Pthreads Transformer</li>
 * <li>MPI Transformer</li>
 * <li>Side-effect remover</li>
 * <li>Pruner</li>
 * </ol>
 * </p>
 * 
 * @author Manchun Zheng
 */
public class ModelTranslator {

	// private final static fields (constants)...

	/**
	 * The default macro for CIVL-C programs. Could be disable by the setting the
	 * option _CIVL to false: <code>-_CIVL=false</code>.
	 */
	private static final String CIVL_MACRO = "_CIVL";

	/**
	 * A macro for MPI contract features. Once the option "-mpi=contract" or
	 * "-contract" is set in command line, such a macro should be enabled.
	 */
	private static final String MPI_CONTRACT_MACRO = "_MPI_CONTRACT";

	/**
	 * A macro for MPI blocking model based implementation. Such a macro is by
	 * default on or when the option "-mpi=blocking" is set in command line.
	 */
	private static final String MPI_BLOCKING_MACRO = "_MPI_BLOCKING";

	/**
	 * A macro for MPI implementation supporting both blocking and non-blocking.
	 * Once the option "-mpi=nonblocking" is set in command line, such a macro
	 * should be enabled.
	 */
	private static final String MPI_NON_BLOCKING_MACRO = "_MPI_NON_BLOCKING";

	// package-private fields, which are accessed by UserInterface...

	/**
	 * The GMC configuration that this model translator associates with.
	 */
	public GMCConfiguration gmcConfig;

	/**
	 * The command line section for this model translator.
	 */
	public GMCSection cmdSection;

	/**
	 * The CIVL configuration for this model translator, which is dependent on the
	 * command line section.
	 */
	public CIVLConfiguration config;

	/**
	 * The symbolic universe.
	 */
	public SymbolicUniverse universe;

	/**
	 * This is the main ABC class used to compile a program.
	 */
	public FrontEnd frontEnd;

	// private fields

	private ArrayList<File> files = new ArrayList<>();

	/**
	 * The system include paths specified in the command line section.
	 */
	private File[] systemIncludes;

	/**
	 * The user include paths specified in the command line section.
	 */
	private File[] userIncludes;

	/**
	 * The output stream for printing error messages.
	 */
	private PrintStream out = System.out;

	/**
	 * The file name of the user file, which is the first file specified in the
	 * command line section.
	 */
	private String userFileName;

	/**
	 * The transformer factory which provides transformers.
	 */
	private TransformerFactory transformerFactory;

	private FileIndexer fileIndexer;

	// Constructors...

	/**
	 * Creates a new instance of model translator.
	 * 
	 * @param gmcConfig  The GMC configuration which corresponds to the command
	 *                   line.
	 * @param gmcSection The GMC section which corresponds to the command line
	 *                   section this model translator associates with.
	 * @param filenames  The list of file names for parsing, which are specified in
	 *                   the command line.
	 * @param coreName   The core name of the user file. It is assumed that the
	 *                   first file in the file list from the command line is the
	 *                   core user file, which usually is the one that contains the
	 *                   main function.
	 * @throws PreprocessorException if there is a problem processing any macros
	 *                               defined in the command line
	 */
	public ModelTranslator(GMCConfiguration gmcConfig, GMCSection gmcSection, String[] filenames, String coreName)
			throws PreprocessorException {
		this(gmcConfig, gmcSection, filenames, coreName, SARL.newStandardUniverse(), null);
	}

	/**
	 * Creates a new instance of model translator.
	 * 
	 * @param gmcConfig   The GMC configuration which corresponds to the command
	 *                    line.
	 * @param gmcSection  The GMC section which corresponds to the command line
	 *                    section this model translator associates with.
	 * @param filenames   The list of file names for parsing, which are specified in
	 *                    the command line.
	 * @param coreName    The core name of the user file. It is assumed that the
	 *                    first file in the file list from the command line is the
	 *                    core user file, which usually is the one that contains the
	 *                    main function.
	 * @param universe    The symbolic universe, the unique one used by this run.
	 * @param fileIndexer the file indexer to use, can be null
	 * @throws PreprocessorException if there is a problem processing any macros
	 *                               defined in the command line
	 */
	public ModelTranslator(GMCConfiguration gmcConfig, GMCSection cmdSection, String[] filenames, String coreName,
			SymbolicUniverse universe, FileIndexer fileIndexer) throws PreprocessorException {
		this.cmdSection = cmdSection;
		this.gmcConfig = gmcConfig;
		this.universe = universe;
		this.fileIndexer = fileIndexer;
		if (cmdSection.isTrue(showProverQueriesO))
			universe.setShowProverQueries(true);
		if (cmdSection.isTrue(showQueriesO))
			universe.setShowQueries(true);
		config = new CIVLConfiguration(cmdSection);
		userFileName = filenames[0];
		for (int i = 0; i < filenames.length; i++) {
			this.files.add(new File(filenames[i]));
		}
		systemIncludes = this.getSysIncludes(cmdSection);
		userIncludes = this.getUserIncludes(cmdSection);
	}

	public Program buildProgram() throws ABCException {
		TranslationTask task;
		UnitTask[] unitTasks;
		Map<String, String> macros = this.getMacros();

		if (config.loopInvariantEnabled())
			files.addAll(0, Arrays.asList(LoopContractTransformer.additionalLibraries));
		else if (config.isEnableMpiContract())
			// contract transformer requires a subset of the libs that loop
			// transformer requires:
			files.addAll(0, Arrays.asList(ContractTransformer.additionalLibraries));
		unitTasks = new UnitTask[files.size()];
		for (int i = 0; i < unitTasks.length; i++) {
			unitTasks[i] = new UnitTask(new File[] { files.get(i) });
			unitTasks[i].setMacros(macros);
			unitTasks[i].setSystemIncludes(systemIncludes);
			unitTasks[i].setUserIncludes(userIncludes);
		}
		task = new TranslationTask(unitTasks);
		task.setPrettyPrint(true);
		task.setLinkLanguage(Language.CIVL_C);
		task.setStage(TranslationStage.TRANSFORM_PROGRAM);
		task.setVerbose(config.debugOrVerbose());

		ABCExecutor executor;

		if (fileIndexer == null) {
			executor = new ABCExecutor(task);
			frontEnd = executor.getFrontEnd();
			fileIndexer = frontEnd.getFileIndexer();
		} else {
			executor = new ABCExecutor(task, fileIndexer);
			frontEnd = executor.getFrontEnd();
		}
		task.setDynamicTask(new ParseSystemLibrary(executor, macros));
		this.transformerFactory = Transforms.newTransformerFactory(frontEnd.getASTFactory());
		addTransformations(task, macros);
		executor.execute();
		return executor.getProgram();
	}

	private void addTransformations(TranslationTask task, Map<String, String> macros) throws ABCException {
		task.addTransformCode(GenericSelectionRemover.CODE);
		if (config.loopInvariantEnabled()) {
			task.addTransformRecord(transformerFactory.getAnnotationTransformerRecord());
			task.addTransformRecord(transformerFactory.getLoopContractTransformerRecord(config));
		}
		if (config.isEnableMpiContract())
			task.addTransformRecord(
					transformerFactory.getContractTransformerRecord(config.mpiContractFunction(), config));
		task.addTransformRecord(transformerFactory.getGeneralTransformerRecord());
		//task.addTransformRecord(transformerFactory.getComplexTransformerRecord());
		task.addTransformRecord(transformerFactory.getIOTransformerRecord(config));
		// Add renamer for external-linkage variables that have declarations in
		// block scope:
		task.addTransformCode(ExternLinkageVariableRenamer.CODE);
		task.addTransformRecord(transformerFactory.getOpenMPSimplifierRecord(config));
		task.addTransformRecord(transformerFactory.getOpenMP2CIVLTransformerRecord(config));
		task.addTransformRecord(transformerFactory.getPthread2CIVLTransformerRecord());
		task.addTransformRecord(transformerFactory.getMPI2CIVLTransformerRecord());
		task.addTransformCode(SideEffectRemover.CODE);
		task.addTransformRecord(transformerFactory.getComplexTransformerRecord());

		task.addTransformRecord(transformerFactory.getCuda2CIVLTransformerRecord());
		if (config.isIntOperationTransiformer())
			task.addTransformRecord(transformerFactory.getIntOperationTransformerRecord(macros, config));
		task.addTransformCode(SideEffectRemover.CODE);
		task.addTransformRecord(transformerFactory.getShortCircuitTransformerRecord(config));
		task.addTransformCode(Pruner.CODE);
	}

	/**
	 * Translates command line marcos into ABC macro objects.
	 * 
	 * @return a map of macro keys and objects.
	 * @throws PreprocessorExceptions if there is a problem preprocessing the
	 *                                macros.
	 */
	private Map<String, String> getMacros() throws PreprocessorException {
		Map<String, Object> macroDefMap = cmdSection.getMapValue(macroO);
		Map<String, String> macroDefs = new HashMap<String, String>();

		if (this.cmdSection.isTrue(CIVLConstants.CIVLMacroO))
			macroDefs.put(CIVL_MACRO, "");
		if (this.config.isEnableMpiContract())
			macroDefs.put(MPI_CONTRACT_MACRO, "");
		switch (this.config.mpiModel()) {
		case BLOCKING:
			macroDefs.put(MPI_BLOCKING_MACRO, "");
			break;
		case CONTRACT:
			macroDefs.put(MPI_CONTRACT_MACRO, "");
			break;
		case NON_BLOCKING:
			macroDefs.put(MPI_NON_BLOCKING_MACRO, "");
			break;
		default:
			macroDefs.put(MPI_BLOCKING_MACRO, "");
		}
		if (macroDefMap != null) {
			for (String name : macroDefMap.keySet()) {
				macroDefs.put(name, (String) macroDefMap.get(name));
			}
		}
		return macroDefs;
	}

	/**
	 * Parse, link, apply transformers and build CIVL-C model for a certain CIVL-C
	 * compiling task.
	 * 
	 * @return the CIVL-C model of this compiling task specified by the command line
	 * @throws CommandLineException if there is a problem interpreting the command
	 *                              line section
	 * @throws IOException          if there is a problem reading source files.
	 * @throws ABCException
	 */
	public Model translate() throws CommandLineException, IOException, ABCException {
		long startTime = System.currentTimeMillis();
		Program program = this.buildProgram();
		long endTime = System.currentTimeMillis();
		long totalTime;

		if (config.showAST())
			program.print(out);
		if (config.showProgram())
			program.prettyPrint(out);
		if (config.showTime()) {
			totalTime = (endTime - startTime);
			out.println(totalTime + "ms: total time for building the whole program");
		}
		if (program != null && config.showInputVars()) {
			List<VariableDeclarationNode> inputs = this.inputVariablesOfProgram(program);

			out.println("input variables:");
			for (VariableDeclarationNode input : inputs) {
				input.prettyPrint(out);
				out.println();
			}
		}
		if (program != null) {
			Model model;

			startTime = System.currentTimeMillis();
			model = this.buildModel(program);
			endTime = System.currentTimeMillis();
			if (config.showTime()) {
				totalTime = (endTime - startTime);
				out.println(totalTime + "ms: CIVL model builder builds model from program");
			}
			return model;
		}
		return null;
	}

	/**
	 * Obtains the input variables declared in the given program
	 * 
	 * @return the input variables declared in the given program
	 * @throws IOException  if there is a problem reading source files.
	 * @throws ABCException
	 */
	List<VariableDeclarationNode> getInputVariables() throws IOException, ABCException {
		Program program;

		program = this.buildProgram();
		return this.inputVariablesOfProgram(program);
	}

	/**
	 * Builds a CIVL model from an ABC program, which is the result of parsing,
	 * linking and transforming source files.
	 * 
	 * @param program the ABC program.
	 * @return the CIVL model representation of the given ABC program.
	 * @throws CommandLineException if there is a problem in the format of input
	 *                              variable values in the command line.
	 */
	Model buildModel(Program program) throws CommandLineException {
		Model model;
		ModelBuilder modelBuilder = Models.newModelBuilder(this.universe, this.config);
		String modelName = coreName(userFileName);
		boolean hasFscanf = TransformerFactory.hasFunctionCalls(program.getAST(), Arrays.asList("scanf", "fscanf"));

		model = modelBuilder.buildModel(cmdSection, program, modelName, config.debugOrVerbose(), out);
		model.setHasFscanf(hasFscanf);
		if (config.debugOrVerbose() || config.showModel()) {
			out.println(bar + "The CIVL model is:" + bar);
			model.print(out, config.debugOrVerbose());
			out.println();
			out.flush();
		}
		return model;
	}

	// private methods

	/**
	 * Gets the list of input variables declared in the given program.
	 * 
	 * @param program the program, which is the result of parsing, linking and
	 *                transforming.
	 * @return the list of input variables declared in the given program.
	 */
	private List<VariableDeclarationNode> inputVariablesOfProgram(Program program) {
		LinkedList<VariableDeclarationNode> result = new LinkedList<>();
		ASTNode root = program.getAST().getRootNode();

		for (ASTNode child : root.children()) {
			if (child != null && child.nodeKind() == NodeKind.VARIABLE_DECLARATION) {
				VariableDeclarationNode variable = (VariableDeclarationNode) child;

				if (variable.getTypeNode().isInputQualified()) {
					result.add(variable);
				}
			}
		}
		return result;
	}

	/**
	 * Extracts from a string the "core" part of a filename by removing any
	 * directory prefixes and removing any file suffix. For example, invoking on
	 * "users/siegel/gcd/gcd1.cvl" will return "gcd1". This is the name used to name
	 * the model and other structures; it is used in the log, to name generated
	 * files, and for error reporting.
	 * 
	 * @param filename a filename
	 * @return the core part of that filename
	 */
	private static String coreName(String filename) {
		String result = filename;
		char sep = File.separatorChar;
		int lastSep = filename.lastIndexOf(sep);
		int lastDot;

		if (lastSep >= 0)
			result = result.substring(lastSep + 1);
		lastDot = result.lastIndexOf('.');
		if (lastDot >= 0)
			result = result.substring(0, lastDot);
		return result;
	}

	/**
	 * Given a colon-separated list of filenames as a single string, this splits it
	 * up and returns an array of File objects, one for each name.
	 * 
	 * @param string null or colon-separated list of filenames
	 * @return array of File
	 */
	private File[] extractPaths(String string) {
		if (string == null)
			return new File[0];
		else {
			String[] pieces = string.split(":");
			int numPieces = pieces.length;
			File[] result = new File[numPieces];

			for (int i = 0; i < numPieces; i++)
				result[i] = new File(pieces[i]);
			return result;
		}
	}

	/**
	 * Gets the user include paths, which are specified in the command line
	 * 
	 * @param section the command line section this model translator corresponds to.
	 * @return the user include paths.
	 */
	private File[] getUserIncludes(GMCSection section) {
		return extractPaths((String) section.getValue(userIncludePathO));
	}

	/**
	 * This adds the default CIVL include path to the list of system includes.
	 *
	 * @param config
	 * @return list of system include directories specified in the (command line)
	 *         config object with the default CIVL include directory tacked on at
	 *         the end
	 */
	private File[] getSysIncludes(GMCSection config) {
		File[] sysIncludes = extractPaths((String) config.getValue(sysIncludePathO));
		int numIncludes = sysIncludes.length;
		File[] newSysIncludes = new File[numIncludes + 1];

		System.arraycopy(sysIncludes, 0, newSysIncludes, 0, numIncludes);
		newSysIncludes[numIncludes] = CIVLConstants.CIVL_LIB_SRC_PATH;
		return newSysIncludes;
	}

}
