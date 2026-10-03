package dev.civl.abc;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import dev.civl.abc.err.IF.ABCException;
import dev.civl.abc.main.ABCExecutor;
import dev.civl.abc.main.TranslationTask;
import dev.civl.abc.token.IF.SyntaxException;

/**
 * Tests that malformed nested designators in compound literal initializers are
 * rejected with the expected {@link SyntaxException} error messages.
 */
public class CTranslationErrorMsgTest {

	/**
	 * The transformations which will be applied to each example.
	 */
	private static List<String> codes = Arrays.asList("prune", "sef");

	/**
	 * Runs ABC on the given source, expecting analysis to fail with a
	 * {@link SyntaxException} whose message contains {@code expectedMessage}.
	 *
	 * @param source
	 *                        the C source code to translate
	 * @param expectedMessage
	 *                        a substring expected to appear in the error message
	 */
	private void expectError(String source, String expectedMessage)
			throws ABCException {
		File file;

		try {
			file = File.createTempFile("compoundInitErr_", ".c");
			file.deleteOnExit();
			Files.writeString(file.toPath(), source);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			TranslationTask task = new TranslationTask(file);

			task.addAllTransformCodes(codes);
			ABCExecutor.execute(task);
		} catch (SyntaxException e) {
			String message = e.getMessage();

			if (message == null || !message.equals(expectedMessage)) {
				System.err.println("=== actual message ===\n" + message
						+ "\n======================");
				fail("expected error message containing:\n  " + expectedMessage);
			}
			return;
		} finally {
			file.delete();
		}
		fail("expected a SyntaxException containing:\n  " + expectedMessage);
	}

	/**
	 * A field designator applied to a member that is not a structure or union
	 * (here .y on the int field x) must be rejected.
	 */
	@Test
	public void fieldDesignatorOnScalar() throws ABCException {
		expectError(/* Source code: */ """
				struct S {
				  int x;
				};

				int main() {
				  struct S s = {.x.y = 3};
				  return s.x;
				}
				""", /* Expected error: */
				"Field designator .y applied to non-structure/union type int");
	}

	/**
	 * An array designator applied to a member that is not an array (here [0] on
	 * the int field x) must be rejected.
	 */
	@Test
	public void arrayDesignatorOnScalar() throws ABCException {
		expectError(/* Source code: */ """
				struct S {
				  int x;
				};

				int main() {
				  struct S s = {.x[0] = 3};
				  return s.x;
				}
				""", /* Expected error: */
				"Array designator applied to non-array type int");
	}

}
