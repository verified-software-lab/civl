/*******************************************************************************
 * Copyright (c) 2013 Stephen F. Siegel, University of Delaware.
 * 
 * This file is part of SARL.
 * 
 * SARL is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * SARL is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with SARL. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package dev.civl.sarl.prove.smt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import dev.civl.sarl.IF.SARLException;
import dev.civl.sarl.IF.TheoremProverException;
import dev.civl.sarl.IF.ValidityResult;
import dev.civl.sarl.IF.config.ProverInfo;
import dev.civl.sarl.IF.config.ProverInfo.ProverKind;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.preuniverse.IF.PreUniverse;
import dev.civl.sarl.prove.IF.Prove;
import dev.civl.sarl.prove.IF.ProverFunctionInterpretation;
import dev.civl.sarl.prove.IF.TheoremProver;
import dev.civl.sarl.util.FastList;
import dev.civl.sarl.util.ProcessControl;

/**
 * An implementation of {@link TheoremProver} for an SMT-based prover that
 * follows the SMT-LIB standard. Transforms a theorem proving query into
 * SMT-LIB, invokes the tool through its command line interface in a new
 * process, and interprets the output.
 * 
 * Commands:
 * 
 * To check whether predicate is unsatisfiable under context:
 * 
 * <pre>
 * (assert context)
 * (assert predicate)
 * (check-sat)
 * </pre>
 * 
 * To check whether predicate is entailed by context:
 * 
 * <pre>
 * (assert context)
 * (assert (not predicate))
 * (check-sat)
 * </pre>
 * 
 * In both cases, if result is "sat", answer is NO. If result is "unsat", answer
 * is YES. Otherwise, MAYBE.
 * 
 * Note: need a temporary directory to store the prover input files. Should this
 * be another argument to the constructor?
 * </p>
 * 
 * @author Stephen F. Siegel
 */
public class SMTProver implements TheoremProver {

	// ************************** Static Fields *************************** //

	/**
	 * Nick-name for <code>stderr</code>, where warnings and error messages will be
	 * sent.
	 */
	public static final PrintStream err = System.err;

	// ************************** Static Methods ************************** //

	// TODO: probably better to make this a base class and introduce new subclasses
	// Z3Prover, CVC5Prover, etc.

	private static SMTTranslator newSMTTranslator(ProverKind kind, SMTTranslator startingContext,
			SymbolicExpression theExpression) {
		switch (kind) {
		case Z3:
			return new Z3Translator((Z3Translator) startingContext, theExpression);
		case CVC5:
			return new CVC5Translator((CVC5Translator) startingContext, theExpression);
		default:
			return new SMTTranslator(startingContext, theExpression);
		}
	}

	private static SMTTranslator newSMTTranslator(ProverKind kind, PreUniverse universe,
			SymbolicExpression theExpression, ProverFunctionInterpretation logicFunctions[]) {
		switch (kind) {
		case Z3:
			return new Z3Translator(universe, theExpression, logicFunctions);
		case CVC5:
			return new CVC5Translator(universe, theExpression, logicFunctions);
		default:
			return new SMTTranslator(universe, kind, theExpression, logicFunctions);
		}
	}

	private static void print(String str, PrintStream stream1, PrintStream stream2) {
		stream1.print(str);
		if (stream2 != null)
			stream2.print(str);
	}

	private static void println(String str, PrintStream stream1, PrintStream stream2) {
		stream1.println(str);
		if (stream2 != null)
			stream2.println(str);
	}

	private static void print(FastList<String> fl, PrintStream stream1, PrintStream stream2) {
		fl.print(stream1);
		if (stream2 != null)
			fl.print(stream2);
	}

	private static void printProverUnexpectedException(BufferedReader proverErr, PrintStream exp) throws IOException {
		String errline;

		do {
			errline = proverErr.readLine();
			if (errline == null)
				break;
			exp.append(errline + "\n");
		} while (errline != null);
	}

	// **************************** Instance Fields **************************** //

	/**
	 * Info object on underlying SMT theorem prover.
	 */
	private ProverInfo info;

	private Path workingDirectory;

	/**
	 * The symbolic universe used for managing symbolic expressions. Initialized by
	 * constructor and never changes.
	 */
	private PreUniverse universe;

	/**
	 * The translation of the given context to an SMT expression. Created once
	 * during instantiation and never modified.
	 */
	private SMTTranslator assumptionTranslator;

	/**
	 * First part of command to execute: the path to the executable tool and the
	 * command line options. The filename must be appended to complete the command.
	 */
	private LinkedList<String> command0 = new LinkedList<>();

	// *************************** Constructors *************************** //

	/**
	 * Constructs new SMT theorem prover for the given context.
	 * 
	 * @param universe       the controlling symbolic universe
	 * @param context        the assumption(s) the prover will use for queries
	 * @param ProverInfo     information object on the underlying theorem prover,
	 *                       which must be an SMT prover
	 * @param logicFunctions a list of {@link ProverFunctionInterpretation}s which
	 *                       are the logic function definitions
	 */
	SMTProver(PreUniverse universe, BooleanExpression context, ProverInfo info, Path workingDirectory,
			ProverFunctionInterpretation logicFunctions[]) {
		assert universe != null;
		assert context != null;
		assert info != null;
		this.universe = universe;
		this.info = info;
		this.workingDirectory = workingDirectory;
		context = (BooleanExpression) universe.cleanBoundVariables(context);
		this.assumptionTranslator = newSMTTranslator(info.getKind(), universe, context, logicFunctions);
		command0.add(info.getPath().getAbsolutePath()); // the name of the tool
		command0.addAll(info.getOptions()); // tool-specific options
	}

	// ************************* Instance Methods ************************* //

	// Prover query 15:32 (cvc5)

	private String queryName(int id1, int id2) {
		return "Prover query " + id1 + ":" + id2 + " (" + info.getFirstAlias() + ")";
	}

	private String queryFilename(int id1, int id2) {
		return "query_" + id2 + ".smt2";
	}

	/**
	 * Parse SMT tool's output. Whether asking about unsatisfiability or validity, a
	 * result of "unsat" means the answer is "yes", "sat" means the answer is "no",
	 * and anything else means the answer is "maybe".
	 * 
	 * @param smtOut stdout output from prover
	 * @param smtErr stderr output from prover
	 * @return the answer to the unsatisfiability or validity question
	 */
	private ValidityResult readSmtOutput(BufferedReader smtOut, BufferedReader smtErr, int id1, int id2,
			PrintStream out2) {
		if (out2 != null) {
			out2.println(queryName(id1, id2) + " output:");
		}
		try {
			while (true) {
				// save a copy of the original line for error reporting...
				String line = smtOut.readLine(), originalLine = line;
				if (line == null) {
					if (universe.getShowProverQueries() || info.getShowQueries() || info.getShowErrors()
							|| info.getShowInconclusives()) {
						try {
							if (smtErr.ready()) {
								PrintStream exp = new PrintStream(new File(universe.getErrFile()));
								printProverUnexpectedException(smtErr, exp);
								exp.close();
							}
						} catch (IOException e) {
							printProverUnexpectedException(smtErr, err);
						}
					}
					return Prove.RESULT_MAYBE;
				} else if (out2 != null) {
					out2.println(line);
				}
				int commentStart = line.indexOf(';');
				if (commentStart >= 0)
					line = line.substring(0, commentStart);
				line = line.trim();
				if (line.isEmpty())
					continue;
				if ("unsat".equals(line))
					return Prove.RESULT_YES;
				if ("sat".equals(line))
					return Prove.RESULT_NO;
				if ("timeout".equals(line) || "unknown".equals(line))
					return Prove.RESULT_MAYBE;
				throw new SARLException(queryName(id1, id2) + " unexpected message: " + originalLine);
			}
		} catch (IOException e) {
			if (info.getShowErrors())
				err.println(queryName(id1, id2) + " I/O error reading output: " + e.getMessage());
			return Prove.RESULT_MAYBE;
		}
	}

	private boolean createSmtFile(BooleanExpression predicate, boolean checkUNSAT, Path inputPath, PrintStream out2) {
		// First, create the SMT input file. This first creates a new empty file
		// if a file named inputFilename doesn't exist; or truncates to 0 if it does.
		// The try-with-resources thing automatically closes the file when it goes
		// out of scope.
		try (PrintStream out1 = new PrintStream(Files.newOutputStream(inputPath))) {
			FastList<String> assumptionDecls = assumptionTranslator.getDeclarations();
			FastList<String> assumptionText = assumptionTranslator.getTranslation();
			predicate = (BooleanExpression) universe.cleanBoundVariables(predicate);
			SMTTranslator translator = newSMTTranslator(info.getKind(), assumptionTranslator, predicate);
			FastList<String> predicateDecls = translator.getDeclarations();
			FastList<String> predicateText = translator.getTranslation();

			// set-logic harms Z3. takes away ability to use special relations...
			if (info.getKind() != ProverKind.Z3)
				println("(set-logic ALL)", out1, out2);
			print(assumptionDecls, out1, out2);
			print("(assert ", out1, out2);
			print(assumptionText, out1, out2);
			println(")", out1, out2);
			print(predicateDecls, out1, out2);
			if (checkUNSAT) {
				// p is unsat in context c iff p && c is UNSAT:
				println("(assert  ", out1, out2);
				print(predicateText, out1, out2);
				println(")", out1, out2);
			} else {
				// p is entailed by context c iff c && !p is UNSAT:
				println("(assert (not ", out1, out2);
				print(predicateText, out1, out2);
				println("))", out1, out2);
			}
			println("(check-sat)", out1, out2);
			if (out2 != null)
				out2.flush();
		} catch (IOException e) {
			err.println("I/O exception occurred writing to " + inputPath + ":");
			err.println(e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Given timeout in seconds, returns the command line option for the tool.
	 * 
	 * @param timeout desired timeout, in second
	 * @return strings to append to the command to invoke the prover. If there is no
	 *         known way to specify the timeout, returns an empty list.
	 */
	protected List<String> timeoutOption(double timeout) {
		List<String> result = new LinkedList<>();
		if (timeout > 0) {
			switch (info.getKind()) {
			case CVC5:
				result.add("--tlimit=" + (int) (1000.0 * timeout));
				break;
			case Z3:
				result.add("-T:" + (int) Math.ceil(timeout));
				break;
			case ALT_ERGO:
				result.add("-t");
				result.add("" + timeout);
				break;
			default:
			}
		}
		return result;
	}

	/**
	 * Invokes prover and captures output.
	 * 
	 * @param inputPath
	 * @param id
	 * @return
	 */
	private ValidityResult executeCommand(Path inputPath, int id1, int id2, PrintStream out2) {
		LinkedList<String> command = new LinkedList<>(command0);
		double timeout = info.getTimeout();
		command.addAll(timeoutOption(timeout));
		command.add(inputPath.toString());
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		if (out2 != null) {
			out2.println();
			out2.println(queryName(id1, id2) + " command:");
			out2.println(String.join(" ", command));
		}
		Process process = null;
		ValidityResult result;
		try {
			process = processBuilder.start();
			BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			if (info.getTimeout() > 0 && !ProcessControl.waitForProcess(process, 1.0 + timeout)) {
				result = Prove.RESULT_MAYBE;
//				if (info.getShowQueries() || info.getShowErrors() || info.getShowInconclusives()
//						|| universe.getShowProverQueries())
//					err.println(queryName(id1, id2) + ": process had to be forcibly destroyed");
				process.destroyForcibly();
			} else {
				result = readSmtOutput(stdout, stderr, id1, id2, out2);
			}
		} catch (IOException e) {
			result = Prove.RESULT_MAYBE;
			err.println("IO exception occurred executing " + command + ":");
			err.println(e.getMessage());
		}
		return result;
	}

	/**
	 * <p>
	 * Run the SMT prover to reason about the given predicate <code>p</code> under
	 * the context <code>c</code>.
	 * </p>
	 * 
	 * <p>
	 * if the purpose is to test if <code>p</code> is unsatisfiable (the argument
	 * testUNSAT set to true), then the tool checks if <code>c && p</code> is UNSAT.
	 * </p>
	 * 
	 * <p>
	 * if the purpose is to test if <code>p</code> is valid under the context (the
	 * argument testUNSAT set to false), then the tool checks if
	 * <code>c && !p</code> is UNSAT.
	 * </p>
	 * 
	 * @param predicate  the boolean expression representing the predicate
	 * @param checkUNSAT true means check unsatisfiability of the given predicate in
	 *                   the context; false means check if the context entails the
	 *                   predicate.
	 * @param id         the ID number of this prover call
	 * @param show       a flag indicating whether printing the prover script
	 * @param out        the output stream
	 * @return a {@link ValidityResult}
	 */
	private ValidityResult runProver(BooleanExpression predicate, boolean checkUNSAT) {
		boolean show = universe.getShowProverQueries() || info.getShowQueries();
		// out2 is used only if showing prover queries...
		PrintStream out2 = show ? universe.getOutputStream() : null;
		int id1 = universe.numValidCalls() - 1; // number of valid calls made to SARL
		int id2 = universe.numProverValidCalls(); // number of valid calls made to provers
		universe.incrementProverValidCount();
		if (show) {
			String queryKind = checkUNSAT ? "unsat" : "valid";
			String explanation = universe.getQueryExplanation();
			out2.println();
			out2.println(queryName(id1, id2) + "(" + queryKind + "):");
			if (explanation != null)
				out2.println("  reason: " + explanation);
			out2.println();
			// clear so an un-annotated later query does not inherit a stale reason
			universe.setQueryExplanation(null);
		}
		String inputFilename = queryFilename(id1, id2);
		Path inputPath = workingDirectory.resolve(inputFilename);
		ValidityResult result;
		if (!createSmtFile(predicate, checkUNSAT, inputPath, out2))
			result = Prove.RESULT_MAYBE;
		else
			result = executeCommand(inputPath, id1, id2, out2);
		if (show || (result == Prove.RESULT_MAYBE && info.getShowInconclusives())) {
			out2.println();
			out2.println(queryName(id1, id2) + " result: " + result);
		}
		// TODO: make this an option: -keepQueries (default: false)
		try {
			Files.delete(inputPath);
		} catch (Exception e) {
			err.println("Error attempting to delete " + inputPath + ":");
			err.println(e);
		}
		return result;
	}

	@Override
	public String toString() {
		return "SMTProver[" + info.getFirstAlias() + "]";
	}

	@Override
	public ValidityResult valid(BooleanExpression predicate) {
		return runProver(predicate, false);
	}

	@Override
	public ValidityResult unsat(BooleanExpression predicate) throws TheoremProverException {
		return runProver(predicate, true);
	}

	@Override
	public ValidityResult validOrModel(BooleanExpression predicate) {
		// TODO: not yet implemented.
		return Prove.RESULT_MAYBE;
	}
}
