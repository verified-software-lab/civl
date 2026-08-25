#include <assert.h>
int main(void) {
  int x = 1;
  {
    int x = 2;
    assert(x==2);
  }
  assert(x==1);
}
