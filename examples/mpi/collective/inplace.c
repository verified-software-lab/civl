#include <assert.h>
#include <mpi.h>

int main(int argc, char **argv) {
  int rank, size;
  double x;
  MPI_Init(&argc, &argv);
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  MPI_Comm_size(MPI_COMM_WORLD, &size);
  x = rank;
  MPI_Allreduce(MPI_IN_PLACE, &x, 1, MPI_DOUBLE, MPI_SUM, MPI_COMM_WORLD);
  assert(x == size * (size - 1) / 2);
  MPI_Finalize();
  return 0;
}
