// Tests for nested and mixed designators in compound literal initializers.
#include<assert.h>

struct Pt {
    int x;
    int y;
};

struct Line {
    struct Pt s;
    struct Pt e;
};

struct Buf {
    int data[4];
    int len;
};

struct Poly {
    struct Line first;
    int n;
};

int main() {
    struct Line L = {1, 2, .s.x = 3};
    assert(L.s.x == 3 && L.s.y == 2 && L.e.x == 0 && L.e.y == 0);

    struct Line L2 = {.e.y = 7};
    assert(L2.s.x == 0 && L2.s.y == 0 && L2.e.x == 0 && L2.e.y == 7);

    struct Buf b = {.data[2] = 9, .len = 4};
    assert(b.data[0] == 0 && b.data[1] == 0 && b.data[2] == 9
           && b.data[3] == 0 && b.len == 4);

    struct Pt pts[3] = {[1].y = 5};
    assert(pts[0].x == 0 && pts[0].y == 0 && pts[1].x == 0
           && pts[1].y == 5 && pts[2].x == 0 && pts[2].y == 0);

    struct Poly p = {.first.s.x = 42};
    assert(p.first.s.x == 42 && p.first.s.y == 0 && p.first.e.x == 0
           && p.n == 0);

    struct Line L3 = {.s.x = 1, .s.y = 2, .e.x = 3, .e.y = 4};
    assert(L3.s.x == 1 && L3.s.y == 2 && L3.e.x == 3 && L3.e.y == 4);

    return 0;
}
