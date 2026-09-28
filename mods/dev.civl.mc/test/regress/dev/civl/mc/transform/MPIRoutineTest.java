package dev.civl.mc.transform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import dev.civl.mc.run.IF.UserInterface;

public class MPIRoutineTest {
	@Rule
	public Timeout globalTimeout = Timeout.seconds(30);

	/* *************************** Static Fields *************************** */

	private static File rootDir = new File(new File("examples"),
			"mpi/routines");

	private static UserInterface ui = new UserInterface();

	/* *************************** Helper Methods ************************** */

	private static String filename(String name) {
		return new File(rootDir, name).getPath();
	}

	/**
	 * Run test with <code>args</code>. The test is expected to have violation
	 * report. The test passes iff strings in <code>checkList</code> appear in
	 * order in the violation report output of the run.
	 */
	private static void checkBadOutput(String testName, String[] args,
			String[] checkList) {
		// cleanup checkList (sometimes if one copies output from Eclipse's
		// console, there will be invisible control characters):
		for (int i = 0; i < checkList.length; ++i)
			checkList[i] = checkList[i].trim().replaceAll("\\p{Cntrl}", "");
		// run test, write output to a tmp file, and check:
		try {
			File tmpFile = Files.createTempFile(testName, null).toFile();
			FileOutputStream streamToTmp = new FileOutputStream(tmpFile);
			PrintStream ps = new PrintStream(streamToTmp);
			UserInterface ui = new UserInterface(ps, ps);

			assertFalse(ui.run(args));

			ps.flush();
			ps.close();
			streamToTmp.close();
			tmpFile.deleteOnExit();

			BufferedReader reader = new BufferedReader(
					new FileReader(tmpFile, Charset.defaultCharset()));
			int checkIdx = 0;

			for (String line : reader.lines().toList()) {
				if (checkList.length == checkIdx)
					break;
				if (line.trim().contains(checkList[checkIdx])) {
					++checkIdx;
				}
			}
			if (checkIdx < checkList.length)
				assertTrue("Expect to see \"" + checkList[checkIdx]
						+ "\" but not found", false);
			reader.close();
		} catch (IOException e) {
			assertTrue("Unexpected IO exception: " + e.getMessage(), false);
		}
	}

	/* **************************** Test Methods *************************** */

	@Test
	public void mpiReduceLocal() {
		assertTrue(ui.run("verify -input_mpi_nprocs=1 -quiet",
				filename("mpi_reduce_local.c")));
	}

	@Test
	public void mpiReduceLocalBad() {
		checkBadOutput("mpiReduceLocalBad",
				new String[]{"verify  -errorBound=4 -input_mpi_nprocs=1",
						filename("mpi_reduce_local-bad.c")},
				new String[]{"ASSERTION_VIOLATION",
						"mpi_reduce_local-bad.c:12:4", "ASSERTION_VIOLATION",
						"mpi_reduce_local-bad.c:21:4", "ASSERTION_VIOLATION",
						"mpi_reduce_local-bad.c:32:4", "ASSERTION_VIOLATION",
						"mpi_reduce_local-bad.c:41:4"});
	}

	@Test
	public void mpiTypeSize() {
		assertTrue(ui.run("verify -input_mpi_nprocs=1 -quiet",
				filename("mpi_type_size.c")));
	}

	@Test
	public void mpiTypeSizeBad() {
		checkBadOutput("mpiTypeSizeBad",
				new String[]{"verify  -errorBound=4 -input_mpi_nprocs=1",
						filename("mpi_type_size-bad.c")},
				new String[]{"ASSERTION_VIOLATION", "mpi_type_size-bad.c:16",
						"ASSERTION_VIOLATION", "mpi_type_size-bad.c:18",
						"ASSERTION_VIOLATION", "mpi_type_size-bad.c:20",
						"ASSERTION_VIOLATION", "mpi_type_size-bad.c:22"});
	}

	@Test
	public void sendrecvToSelfNoPotentialDeadlock() {
		assertTrue(ui.run(
				"verify -input_mpi_nprocs=1 -checkDeadlock=potential -quiet",
				filename("sendrecv_to_self_no_potential_deadlock.c")));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		ui = null;
		rootDir = null;
	}
}
