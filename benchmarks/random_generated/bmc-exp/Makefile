BGENERATOR=bmc-generator
BGENERATOR-DEBUG=bmc-generator-debug
SMT=smt-bmc-generator
SMT-DEBUG=smt-bmc-generator-debug

CC = gcc
WARNINGS = -Wall -Wextra -Werror -Wno-error=unused-parameter -Wmissing-declarations
CFLAGS   = -D_GNU_SOURCE -std=gnu11 
DEBUG	 = -g

all: $(BGENERATOR) $(SMT)
debug: $(SMT-DEBUG) $(BGENERATOR-DEBUG)
bmc: $(BGENERATOR) $(SMT)

$(BGENERATOR): bmc-generator.c
	$(CC) $^ $(CFLAGS) -o $(BGENERATOR)

$(BGENERATOR-DEBUG): bmc-generator.c
	$(CC) $^ $(CFLAGS) $(DEBUG) -o $(BGENERATOR-DEBUG)

$(SMT): smt-bmc-generator.c
	$(CC) $^ $(CFLAGS) $(WARNINGS) -o $(SMT)

$(SMT-DEBUG): smt-bmc-generator.c
	$(CC) $^ $(CFLAGS) $(DEBUG) $(WARNINGS) -o $(SMT-DEBUG)

clean:
	$(RM) $(BGENERATOR) $(BGENERATOR-DEBUG) $(SMT) $(SMT-DEBUG)
