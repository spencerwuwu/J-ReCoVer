#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <time.h>

/* 
 * Convert .pre to bounded model checking 
 * formula in z3
 */

/* 
 * Structs 
 */

// Pair
typedef struct Pair {
    int index;
    char* name;
} Pair;

Pair* clone_pair(Pair* pair) {
    Pair* new = malloc(sizeof(Pair));
    new->name = pair->name;
    new->index = pair->index;
    return new;
}


// Map
typedef struct Map {
    Pair** pairs;
    int size;
} Map;

Map* clone_map(Map* map) {
    Map* new = malloc(sizeof(Map));
    new->pairs = malloc(sizeof(Pair *) * map->size);
    for (int i = 0; i < map->size; i++) {
        new->pairs[i] = clone_pair(map->pairs[i]);
    }
    new->size = map->size;
    return new;
}

// Stage
typedef struct Stage {
    char* formula;
    Map* map;
} Stage;

Stage* new_stage(Stage* stage) {
    Stage* new = malloc(sizeof(Stage));
    new->formula = NULL;
    new->map = clone_map(stage->map);
    return new;
}

/* 
 * Global Variables 
 */
char** Pre_lines = NULL;
int Pre_line_num = 0;
int Bmc_fd;


/* 
 * Functions 
 */
int open_filefd(char* filename) {
    int fd = open(filename, O_CREAT | O_WRONLY | O_TRUNC, 0666);
    if (fd < 0) {
        fprintf(stderr, "./generator filename <Variable Baseline If-else>\n");
        exit(1);
    }
    return fd;
}




int main(int argc, char** argv) {
    if (argc != 2) {
        fprintf(stderr, "./z3-bmc-generator filename\n");
        exit(1);
    }
    char *bmc_file = strdup(argv[1]);
    int b_index = strlen(bmc_file);
    bmc_file[--b_index] = 'c';
    bmc_file[--b_index] = 'm';
    bmc_file[--b_index] = 'b';
    Bmc_fd = open_filefd(bmc_file);

    free(bmc_file);
    bmc_file = NULL;

    FILE *pre_file = fopen(argv[1], "r");
    char *buff = NULL;
    size_t size;
    while (getline(&buff, &size, pre_file) > 0) {
        Pre_line_num++;
        Pre_lines = realloc(Pre_lines, sizeof(char *) * Pre_line_num);
        Pre_lines[Pre_line_num - 1] = strdup(buff);
        int n = strlen(buff);
        if (buff[n - 1] == '\n')
            Pre_lines[Pre_line_num - 1][n - 1] = '\0';
    }
    fclose(pre_file);




    for (int i = 0; i < Pre_line_num; i++) {
        free(Pre_lines[i]);
    }
    free(Pre_lines);

    close(Bmc_fd);

    return 0;
}
