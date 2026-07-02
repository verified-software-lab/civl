/*
 * Points-to test (IntraProceduralPointsToTest.compoundLiteralTwoPtrFields):
 * a compound literal used as an expression with two pointer fields. Reading
 * each field of the literal must stay separated: x -> `a` and y -> `b`.
 * This guards the compound-literal expression path against merging the
 * fields of the aggregate.
 */
int a;
int b;

struct S {
    int *p;
    int *q;
};

int main() {
    int *x = (struct S){&a, &b}.p;
    int *y = (struct S){&a, &b}.q;
    return *x + *y;
}
