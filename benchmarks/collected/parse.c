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
    if (argc < 2) {
        fprintf(stderr, "./parse filename\n");
        exit(1);
    }

    FILE *file = fopen(argv[1], "r");
    char *buff = NULL;
    size_t size;
    int start = 0;
    int end_brace = 0;
    int start_brace = 0;


    int ignore_start = 0;
    int ignore_end_brace = 0;
    int ignore_start_brace = 0;
    setbuf(stdout, NULL);
    while (getline(&buff, &size, file) > 0) {
        if (strstr(buff, "@Override") != NULL) {
            continue;
        } else if (strstr(buff, "fail(") != NULL) {
            continue;
        }

        if (strstr(buff, "implements Reducer") != NULL) {
            printf("++++++++\n");
            start = 1;
            start_brace = 0;
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
            if (start_brace == 0) {
                start_brace = 1;
            } else if  (start_brace == 2) {
                start += 1;
            }
        }
        if (start != 0 && strstr(buff, "}") != NULL) {
            start -= 1;
        }


        // Ignore JobConf
        if (ignore_start == 0 && strstr(buff, "JobConf") != NULL) {
            if (strstr(buff, ";") != NULL) {
                continue;
            }
            ignore_start = 1;
            ignore_start_brace = 0;
        }
        if (ignore_start != 0 && strstr(buff, "{") != NULL) {
            ignore_start += 1;
        }
        if (ignore_start != 0 && strstr(buff, "}") != NULL) {
            ignore_start -= 1;
            if (ignore_start == 1) ignore_end_brace = 1;
        }


        int doprint = 0;
        /*
        if (ignore_start != 0) {
            if (ignore_start_brace == 1) {
                ignore_start_brace = 0;
                ignore_start = 0;
            }
            continue;
        }
        */
        if (start != 0) {
            if (start_brace == 1) {
                start_brace = 2;
            } else if (start_brace == 2) {
                doprint = 1;
            }
        }

        if (doprint != 0) {
            char *tmp = strdup(buff);
            print(tmp);
            free(tmp);
        }
    }
    //fflush(stdout);
    fclose(file);

}
