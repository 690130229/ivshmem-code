#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <fcntl.h>
#include <string.h>
#include <openssl/sha.h>
#include <unistd.h>
#include <sys/stat.h>
#include <errno.h>
#include "ivshmem.h"

enum ivshmem_registers {
    IntrMask = 0,
    IntrStatus = 4,
    IVPosition = 8,
    Doorbell = 12,
    IVLiveList = 16
};

int main(int argc, char ** argv){

    void * memptr;
    unsigned int * map_array;
    int i, fd;
    int count;
    int msg, cmd, dest;

    if (argc != 5) {
        printf("USAGE: uio_ioctl <filename> <count> <cmd> <dest>\n");
        exit(-1);
    }

    fd=open(argv[1], O_RDWR);
    printf("[UIO] opening file %s\n", argv[1]);
    count = atol(argv[2]);
    cmd = (unsigned short) atoi(argv[3]);
    dest = (unsigned short) atoi(argv[4]);

    printf("[UIO] count is %d\n", count);
    printf("[UIO] size of short %d\n", sizeof(unsigned short));

    if ((memptr = mmap(NULL, 256, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0)) == -1){
        printf("mmap failed (0x%x)\n", memptr);
        close (fd);
        exit (-1);
    }

    map_array = (unsigned int *)memptr;

//    msg = ((dest & 0xff) << 8) + (cmd & 0xff);
    msg = cmd;
    printf("[UIO] writing %u\n", msg);

    for (i = 0; i < count; i++) {
        printf("[UIO] ping #%d\n", i);
        map_array[(Doorbell + dest * sizeof(int))/sizeof(int)] = msg;
        sleep(1);
    }

    munmap(memptr, 256);
    close(fd);

    printf("[UIO] Exiting...\n");
}
