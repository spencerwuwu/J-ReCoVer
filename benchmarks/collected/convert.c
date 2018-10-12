#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>

void print(char *buff) {
    for (size_t i = 0; i < strlen(buff); i++) {
        if (buff[i] == '\t') printf("\\t");
        else if (buff[i] == '\"') {
            printf("\\\"");
        } else if (buff[i] == '\'') {
            printf("\\\"");
        } else if (buff[i] == '\n') {
            printf("\\n\n");
        } else if (buff[i] == '\r') {
        }
        else printf("%c", buff[i]);
    }
}

int main(int argc, char** argv) {
    if (argc < 2) {
        fprintf(stderr, "./parse filename\n");
        exit(1);
    }

    FILE *file = fopen(argv[1], "r");
    char *buff = NULL;
    size_t size;

    while (getline(&buff, &size, file) > 0) {
        print(buff);
    }

    fclose(file);
    return 0;
}
