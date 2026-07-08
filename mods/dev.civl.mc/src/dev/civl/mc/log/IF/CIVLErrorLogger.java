package dev.civl.mc.log.IF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Map;

import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.model.IF.CIVLException;
import dev.civl.mc.model.IF.CIVLException.Certainty;
import dev.civl.mc.model.IF.CIVLProperty;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.StateFactory;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.gmc.ErrorLog;
import dev.civl.gmc.ExcessiveErrorException;
import dev.civl.gmc.GMCConfiguration;
import dev.civl.sarl.IF.ModelResult;
import dev.civl.sarl.IF.Reasoner;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.ValidityResult;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.SymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicExpression;

/**
 * CIVLErrorLogger logs all errors of the system.
 * 
 * @author Manchun Zheng
 * 
 */
public class CIVLErrorLogger extends ErrorLog {

	private GMCConfiguration gmcConfig;

	private CIVLConfiguration civlConfig;

	/**
	 * The unique symbolic universe used in the system.
	 */
	private SymbolicUniverse universe;

	/**
	 * A reference to the {@link StateFactory}
	 */
	private StateFactory stateFactory;

	/**
	 * Reasoner with trivial context "true". Used to determine satisfiability of
	 * path conditions.
	 */
	private Reasoner trueReasoner;

	/**
	 * Solve for concrete counterexamples?
	 */
	private boolean solve = false;

	/**
	 * creates a new instance of error logger.
	 * 
	 * @param directory
	 * @param sessionName
	 * @param out
	 * @param config
	 * @param universe
	 * @param solve
	 */
	public CIVLErrorLogger(File directory, String sessionName, PrintStream out,
			CIVLConfiguration civlConfig, GMCConfiguration gmcConfig,
			StateFactory stateFactory, SymbolicUniverse universe,
			boolean solve) {
		super(directory, sessionName, out);
		this.civlConfig = civlConfig;
		this.gmcConfig = gmcConfig;
		this.universe = universe;
		this.trueReasoner = universe.reasoner(universe.trueExpression());
		this.solve = solve;
		this.stateFactory = stateFactory;
	}

	/**
	 * <p>
	 * Report a (possible) error detected in the course of evaluating an
	 * expression. This is the method that should normally be used for logging,
	 * reporting, and recovering from errors.
	 * </p>
	 * 
	 * <p>
	 * This is the general protocol for checking conditions and reporting and
	 * recovering from errors: first, check some condition holds and call the
	 * result of that check "condsat", which may be YES, NO, or MAYBE. If
	 * condsat is YES, proceed. Otherwise, there is a problem and you should
	 * call this method.
	 * </p>
	 * 
	 * <p>
	 * This method first checks the satisfiability of the path condition, call
	 * the result "pcsat". Logs a violation with certainty determined as
	 * follows:
	 * <ul>
	 * <li>pcsat=YES && condsat=NO : certainty=PROVEABLE</li>
	 * <li>pcsat=YES && condsat=MAYBE : certainty=MAYBE</li>
	 * <li>pcsat=MAYBE && condsat=NO : certainty=MAYBE</li>
	 * <li>pcsat=MAYBE && condsat=MAYBE : certainty=MAYBE</li>
	 * <li>pcsat=NO: no error to report</li>
	 * </ul>
	 * </p>
	 * 
	 * <p>
	 * Returns the state obtained by adding the claim to the pc of the given
	 * state.
	 * </p>
	 * 
	 * @param source
	 *            the source of the expression being evaluated
	 * @param state
	 *            the state in which the evaluation is taking place
	 * @param process
	 *            a string representation of the process that is evaluating the
	 *            expression; used for reporting errors
	 * @param stateString
	 *            a string representation of the state which is used in
	 *            CIVLExecutionException that is recorded in the log
	 * @param claim
	 *            the boolean expression which was expected to hold. This should
	 *            already have been checked for validity before invoking this
	 *            method. The result of that check should not have been "YES"
	 *            (i.e., definitely valid); that is why you are calling this
	 *            method
	 * @param resultType
	 *            the result of evaluating the validity of the claim (which you
	 *            did before you called this method).
	 * @param property
	 *            the kind of error you want to report if it turns out there is
	 *            an error
	 * @param message
	 *            the message you want to include in the error report if it
	 *            turns out there is an error
	 * @throws UnsatisfiablePathConditionException
	 *             if it turns out the path condition is unsatisfiable; in this
	 *             case no error will be reported since the current path is
	 *             infeasible
	 * @return the state obtained by adding the claim to the pc of the given
	 *         state.
	 * 
	 */
	public State logError(CIVLSource source, State state, int pid,
			StringBuffer stateString, BooleanExpression claim,
			ResultType resultType, CIVLProperty property, String message)
			throws UnsatisfiablePathConditionException {
		BooleanExpression pc = state.getPathCondition(universe), newPc;
		BooleanExpression npc = universe.not(pc);

		if (universe.getShowProverQueries())
			universe.setQueryExplanation("checking path-condition feasibility before reporting " + property + " at "
					+ source.getLocation());

		ValidityResult validityResult = trueReasoner.valid(npc);
		ResultType nsat = validityResult.getResultType();
		Certainty certainty;
		CIVLExecutionException error;
		String process = state.getProcessState(pid).name();

		assert resultType != ResultType.YES;
		// performance! need to cache the satisfiability of each pc somewhere
		// negation is slow
		// maybe add "nsat" to Reasoner.
		if (nsat == ResultType.YES)
			// no error to report---an infeasible path
			throw new UnsatisfiablePathConditionException();
		if (nsat == ResultType.MAYBE)
			certainty = Certainty.MAYBE;
		else { // pc is definitely satisfiable
			certainty = null;
			if (resultType == ResultType.NO) {
				// need something satisfying PC and not claim...
				if (solve) {
					if (universe.getShowProverQueries())
						universe.setQueryExplanation("solving for counterexample to " + property + " at "
								+ source.getLocation());

					ValidityResult claimResult = trueReasoner
							.validOrModel(universe.or(npc, claim));

					if (claimResult.getResultType() == ResultType.NO) {
						Map<SymbolicConstant, SymbolicExpression> model = ((ModelResult) claimResult)
								.getModel();

						if (model != null) {
							certainty = Certainty.CONCRETE;
							message += "\nCounterexample:\n" + model + "\n";
						}
					}
				}
				if (certainty == null)
					certainty = Certainty.PROVEABLE;
			} else {
				certainty = Certainty.MAYBE;
			}
		}
		error = new CIVLExecutionException(property, certainty, process,
				message, state, pid, source, stateString);
		reportError(error);
		newPc = universe.and(pc, claim);
		// need to check satisfiability again because failure to do so
		// could lead to a SARLException when some subsequent evaluation
		// takes place
		nsat = trueReasoner.valid(universe.not(newPc)).getResultType();
		if (nsat == ResultType.YES)
			throw new UnsatisfiablePathConditionException();
		state = stateFactory.addToPathcondition(state, pid, claim);
		return state;
	}

	/**
	 * Writes the given execution exception to the log.
	 * 
	 * @param err
	 *            a CIVL execution exception
	 * @throws ExcessiveErrorException
	 *             if the number of errors reported has exceeded the specified
	 *             bound
	 */
	private void reportError(CIVLExecutionException err) {
		try {
			gmcConfig.setQuiet(civlConfig.isQuiet());
			report(new CIVLLogEntry(civlConfig, gmcConfig, err, universe));
		} catch (FileNotFoundException e) {
			throw new CIVLException(e.toString(), err.getSource());
		}
	}

	/**
	 * Checks whether the path condition is satisfiable and logs an error if it
	 * is (or might be). If the path condition is definitely unsatisfiable,
	 * there is no error to log, and an UnsatisfiablePathConditionException is
	 * thrown.
	 * 
	 * @param source
	 *            source code element used to report the error
	 * @param state
	 *            the current state in which the possible error is detected
	 * @param process
	 *            the process name, i.e, "p"+process identifier
	 * @param stateString
	 *            the string representation of state where the error occurs
	 * @param civlProp
	 *            the kind of error (e.g., DEREFERENCE)
	 * @param message
	 *            the message to include in the error report
	 * @throws UnsatisfiablePathConditionException
	 *             if the path condition is definitely unsatisfiable
	 */
	public void logSimpleError(CIVLSource source, State state, int pid,
			String process, StringBuffer stateString, CIVLProperty property,
			String message) throws UnsatisfiablePathConditionException {
		BooleanExpression pc = state.getPathCondition(universe);
		BooleanExpression npc = universe.not(pc);

		if (universe.getShowProverQueries())
			universe.setQueryExplanation("checking path-condition feasibility before reporting " + property + " at "
					+ source.getLocation());

		ValidityResult validityResult = trueReasoner.valid(npc);
		ResultType nsat = validityResult.getResultType();
		Certainty certainty;
		CIVLExecutionException error;

		// performance! need to cache the satisfiability of each pc somewhere
		// negation is slow
		// maybe add "nsat" to Reasoner.
		if (nsat == ResultType.YES)
			// no error to report---an infeasible path
			throw new UnsatisfiablePathConditionException();
		if (nsat == ResultType.MAYBE)
			certainty = Certainty.MAYBE;
		else { // pc is definitely satisfiable
			certainty = Certainty.PROVEABLE;
		}
		// TODO if pc has no symbolic constant
		error = new CIVLExecutionException(property, certainty, process,
				message, state, pid, source, stateString);
		reportError(error);
	}
}
