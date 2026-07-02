/*
 * Points-to test (IntraProceduralPointsToTest.compoundLiteralPtrField):
 * a compound literal used as an expression (not a declaration initializer)
 * with one pointer field and one int field. Reading the pointer field of
 * the literal should resolve r to point to `a`.
 */
int a;

struct S {
    int *p;
    int x;
};

int main() {
    int *r = (struct S){&a, 1}.p;
    return *r;
}
