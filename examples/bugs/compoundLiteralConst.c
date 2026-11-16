/* This generates an assertion violation in the CompoundLiteralAnalyzer.
   It seems the analyzer does not expect a QualifiedType like "const int".
*/
int main(void) {
  const int a[] = {1, 2, 3};
}
