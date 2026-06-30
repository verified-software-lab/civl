/*
 * Points-to test (IntraProceduralPointsToTest.compoundInitTwoPtrFields):
 * a struct compound initializer with two pointer fields. The analysis
 * should map each field to its own target: s.p -> `a` and s.q -> `b`,
 * keeping the two fields separated.
 */
int a;
int b;

struct S {
    int *p;
    int *q;
};

int main() {
    struct S s = {&a, &b};
    return *s.p + *s.q;
}
