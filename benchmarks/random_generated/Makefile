GENERATOR=generator
GENERATOR-DEBUG=generator-debug
DGENERATOR=dual-generator
DGENERATOR-DEBUG=dual-generator-debug
BGENERATOR=bmc-generator
BGENERATOR-DEBUG=bmc-generator-debug

CC = gcc
WARNINGS = -Wall -Wextra -Werror -Wno-error=unused-parameter -Wmissing-declarations -Wmissing-variable-declarations
CFLAGS   = -D_GNU_SOURCE -std=gnu11 
DEBUG	 = -g

all: $(GENERATOR) $(DGENERATOR) $(BGENERATOR)
debug: $(GENERATOR-DEBUG) $(DGENERATOR-DEBUG) $(BGENERATOR-DEBUG)
bmc: $(BGENERATOR)

$(GENERATOR): generator.c
	$(CC) $^ $(CFLAGS) -o $(GENERATOR)

$(GENERATOR-DEBUG): generator.c
	$(CC) $^ $(CFLAGS) $(DEBUG) -o $(GENERATOR-DEBUG)

$(DGENERATOR): dual-generator.c
	$(CC) $^ $(CFLAGS) -o $(DGENERATOR)

$(DGENERATOR-DEBUG): dual-generator.c
	$(CC) $^ $(CFLAGS) $(DEBUG) -o $(DGENERATOR-DEBUG)

$(BGENERATOR): bmc-generator.c
	$(CC) $^ $(CFLAGS) -o $(BGENERATOR)

$(BGENERATOR-DEBUG): bmc-generator.c
	$(CC) $^ $(CFLAGS) $(DEBUG) -o $(BGENERATOR-DEBUG)

clean:
	$(RM) $(GENERATOR) $(GENERATOR-DEBUG) $(DGENERATOR) $(DGENERATOR-DEBUG) $(BGENERATOR) $(BGENERATOR-DEBUG)
