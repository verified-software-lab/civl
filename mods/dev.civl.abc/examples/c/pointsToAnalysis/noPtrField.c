/*
 * Points-to test (IntraProceduralPointsToTest.compoundInitNoPtrField):
 * a struct compound initializer with only scalar (non-pointer) fields.
 * The analysis should produce an empty points-to set for s, confirming
 * scalar initializers create no spurious pointer assignments.
 */
struct S {
    int x;
    int y;
};

int main() {
    struct S s = {1, 2};
    return s.x + s.y;
}
