#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>

void print(char *buff) {
    for (size_t i = 0; i < strlen(buff); i++) {
        /*
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
        */
        printf("%c", buff[i]);
    }
}

int main(int argc, char** argv) {
    FILE *file = fopen(argv[1], "r");
    char *buff = NULL;
    size_t size;
    int start = 0;
    setbuf(stdout, NULL);
    while (getline(&buff, &size, file) > 0) {
        if ((strstr(buff, "public void reduce(") != NULL)
                || (strstr(buff, "public void reduce (") != NULL)) {
            start = 1;
        /*
        } else if (strstr(buff, "public") != NULL && 
                !(strstr(buff, "main") != NULL || strstr(buff, "class") != NULL)) {
            print(buff);
            //printf("\\n\n");
        } else if (strstr(buff, "static") != NULL) {
            print(buff);
        } else if (strstr(buff, "private") != NULL) {
            print(buff);
            //printf("\\n\n");
        } else if (strstr(buff, "//") != NULL) {
            print(buff);
            //printf("\\n\n");
        */
        }

        if (start != 0 && strstr(buff, "{") != NULL) {
            start += 1;
        }
        if (start != 0 && strstr(buff, "}") != NULL) {
            start -= 1;
        }
        if (start != 0) {
            char *tmp = strdup(buff);
            print(tmp);
            free(tmp);
        }
    }
    //fflush(stdout);
    fclose(file);

}
