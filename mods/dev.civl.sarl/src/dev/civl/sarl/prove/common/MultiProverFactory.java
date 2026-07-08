package dev.civl.sarl.prove.common;

import java.nio.file.Path;
import java.util.ArrayList;

import dev.civl.sarl.IF.TheoremProverException;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.preuniverse.IF.PreUniverse;
import dev.civl.sarl.prove.IF.ProverFunctionInterpretation;
import dev.civl.sarl.prove.IF.TheoremProver;
import dev.civl.sarl.prove.IF.TheoremProverFactory;

/**
 * A factory for producing instances of {@link MultiProver}.
 * 
 * @author Stephen F. Siegel
 *
 */
public class MultiProverFactory implements TheoremProverFactory {

	private TheoremProverFactory[] factories;

	/**
	 * Working directory for this multi-prover. Note that each constituent prover
	 * has its own working directory, which may be different from this one, or may
	 * be the same as this one.
	 */
	private Path workingDirectory;

	/**
	 * The controlling symbolic universe, used by the produced {@link MultiProver}
	 * to access output settings such as the query explanation.
	 */
	private PreUniverse universe;

	public MultiProverFactory(TheoremProverFactory[] factories, Path workingDirectory, PreUniverse universe) {
		this.factories = factories;
		this.workingDirectory = workingDirectory;
		this.universe = universe;
	}

	@Override
	public TheoremProver newProver(BooleanExpression context) {
		return newProver(context, new ProverFunctionInterpretation[0]);
	}

	@Override
	public TheoremProver newProver(BooleanExpression context, ProverFunctionInterpretation[] logicFunctions) {
		int numProvers = factories.length;
		ArrayList<TheoremProver> provers = new ArrayList<>();

		for (int i = 0; i < numProvers; i++) {
			try {
				TheoremProver prover = factories[i].newProver(context, logicFunctions);

				provers.add(prover);
			} catch (TheoremProverException e) {
				// thrown if the context contained something that class
				// of theorem prover just can't handle
				// ignore this prover.
			}
		}
		return new MultiProver(provers.toArray(new TheoremProver[0]), universe);
	}

	@Override
	public Path workingDirectory() {
		return workingDirectory;
	}

}
