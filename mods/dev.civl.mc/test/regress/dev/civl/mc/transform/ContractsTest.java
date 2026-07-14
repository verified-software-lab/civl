package dev.civl.mc.transform;

import static dev.civl.mc.TestConstants.QUIET;
import static dev.civl.mc.TestConstants.VERIFY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import dev.civl.mc.run.IF.UserInterface;

public class ContractsTest {
	@Rule
	public Timeout globalTimeout = Timeout.seconds(30);

	/* *************************** Static Fields *************************** */

	private static UserInterface ui = new UserInterface();

	private static File rootDir = new File(new File("examples"), "contracts");

	private static String filename(String name) {
		return new File(rootDir, name).getPath();
	}

	@Test
	public void atomicPure() {
		assertTrue(ui.run(VERIFY, QUIET, filename("atomicPure.cvl")));
	}

	@Test
	public void atomicNoPure() {
		assertFalse(ui.run(VERIFY, QUIET, filename("atomicNoPure.cvl")));
	}

}
