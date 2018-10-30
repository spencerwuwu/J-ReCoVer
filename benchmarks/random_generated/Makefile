GENERATOR=generator
GENERATOR-DEBUG=generator-debug
DGENERATOR=dual-generator
DGENERATOR-DEBUG=dual-generator-debug

CC = gcc
WARNINGS = -Wall -Wextra -Werror -Wno-error=unused-parameter -Wmissing-declarations -Wmissing-variable-declarations
CFLAGS   = -D_GNU_SOURCE -std=gnu11 
DEBUG	 = -g

all: $(GENERATOR) $(DGENERATOR)
debug: $(GENERATOR-DEBUG) $(DGENERATOR-DEBUG)

$(GENERATOR): generator.c
	$(CC) $^ $(CFLAGS) -o $(GENERATOR)

$(GENERATOR-DEBUG): generator.c
	$(CC) $^ $(CFLAGS) $(DEBUG) -o $(GENERATOR-DEBUG)

$(DGENERATOR): dual-generator.c
	$(CC) $^ $(CFLAGS) -o $(DGENERATOR)

$(DGENERATOR-DEBUG): dual-generator.c
	$(CC) $^ $(CFLAGS) $(DEBUG) -o $(DGENERATOR-DEBUG)

clean:
	$(RM) $(GENERATOR) $(GENERATOR-DEBUG) $(DGENERATOR) $(DGENERATOR-DEBUG)
