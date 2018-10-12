PARSE=parse
CONVERT=convert

CC = gcc
WARNINGS = -Wall -Wextra -Werror -Wno-error=unused-parameter -Wmissing-declarations -Wmissing-variable-declarations
CFLAGS   = -std=gnu11 
DEBUG	 = -g

all: $(PARSE) $(CONVERT)

$(PARSE): parse.c
	$(CC) $^ $(CFLAGS) -o $(PARSE)

$(CONVERT): convert.c
	$(CC) $^ $(CFLAGS) -o $(CONVERT)

clean:
	$(RM) $(PARSE) $(CONVERT)
