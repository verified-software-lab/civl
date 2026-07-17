
all: verify run

verify: complex_basics complex1 complex_funs \
   mpi_complex fourth complex_ops complex_quant

run: run_complex_basics run_complex_funs

complex_basics: complex_basics.c
	civl verify $<

run_complex_basics: complex_basics.c
	cc -Wall -o complex_basics.exec $<
	./complex_basics.exec

complex1: complex1.cvl
	civl verify $<

complex_funs: complex_funs.c
	civl verify $<

run_complex_funs: complex_funs.c
	cc -Wall -o complex_funs.exec $<
	./complex_funs.exec

mpi_complex: mpi_complex.cvl
	civl verify -input_mpi_nprocs=2 $<

fourth: fourth.c
	civl verify -input_mpi_nprocs=4 $<

complex_ops: complex_ops.c
	civl verify $<

unity: unity.cvl
	civl verify -inputN=4 $<

complex_quant: complex_quant.cvl
	civl verify $<

clean:
	rm -rf *.exec *~ CIVLREP a.out

_PHONY: complex_basics run_complex_basics \
   complex1 \
   complex_funs run_complex_funs \
   mpi_complex fourth complex_ops \
   all verify run clean
