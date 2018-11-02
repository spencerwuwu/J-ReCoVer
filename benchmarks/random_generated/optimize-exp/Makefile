GENERATOR=generator
GENERATOR-DEBUG=generator-debug

CC = gcc
WARNINGS = -Wall -Wextra -Werror -Wno-error=unused-parameter -Wmissing-declarations
CFLAGS   = -D_GNU_SOURCE -std=gnu11 
DEBUG	 = -g

all: $(GENERATOR)

$(GENERATOR): generator.c
	$(CC) $^ $(CFLAGS) -o $(GENERATOR)

$(GENERATOR-DEBUG): generator.c
	$(CC) $^ $(CFLAGS) $(DEBUG) -o $(GENERATOR-DEBUG)

clean:
	$(RM) $(GENERATOR) $(GENERATOR-DEBUG)
