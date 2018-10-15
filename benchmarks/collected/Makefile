PARSE=parse

CC = gcc
WARNINGS = -Wall -Wextra -Werror -Wno-error=unused-parameter -Wmissing-declarations -Wmissing-variable-declarations
CFLAGS   = -std=gnu11 
DEBUG	 = -g

all: $(PARSE)

$(PARSE): parse.c
	$(CC) $^ $(CFLAGS) -o $(PARSE)

clean:
	$(RM) $(PARSE) $(CONVERT)
