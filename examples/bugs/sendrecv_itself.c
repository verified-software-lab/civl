#include <mpi.h>
#include <stdio.h>

int main(int argc, char** argv) {
  int rank;
  MPI_Init(&argc, &argv);
  MPI_Comm_rank(MPI_COMM_WORLD, &rank);

  if (rank == 0) { 
    MPI_Sendrecv(
        NULL, 0, MPI_INT, 0, 1,
        NULL, 0, MPI_INT, 0, 1,
        MPI_COMM_WORLD, MPI_STATUS_IGNORE);
  }

  MPI_Finalize();
  return 0;
}