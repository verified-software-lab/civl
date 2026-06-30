/*
 * Points-to test (IntraProceduralPointsToTest.compoundInitPtrField):
 * a struct compound initializer with one pointer field and one int field.
 * The analysis should resolve s.p to point to `a` (and ignore the scalar
 * field x).
 */
int a;

struct S {
    int *p;
    int x;
};

int main() {
    struct S s = {&a, 1};
    return *s.p;
}
