import java.io.FileInputStream;

public class FTPSend extends FTP {
    public static void main(String args[]) throws Exception {
        String devname = args[0];
        int msize = Integer.parseInt(args[1]);
        int nblocks = Integer.parseInt(args[2]);
        int nchunks = Integer.parseInt(args[3]);

        FTPSend s = new FTPSend(devname, msize, nblocks, nchunks);
    }

    public FTPSend(String devname, int msize, int nblocks, int nchunks) throws Exception {
        super(devname, msize, nblocks, nchunks);

        int receiver;
        int me;
        int block;
        String sendfile;
        int full, empty;
        byte bytes[] = new byte[CHUNK_SZ];
        int idx;
        long total, sent;
        boolean quit = false;

        //System.out.println("[SEND] Opening device " + devname);

        /* What is my VM number? */
        me = mem.getPosition();
        block = me - 1;
        //System.out.println("[SEND] I am VM number " + String.valueOf(me));

        mem.initLock(SYNC(me) + SLOCK);
        /* For now, we will always use the same block */
        mem.writeInt(block, SYNC(me) + BLK);
       
        while(!quit) {

            /* Wait for a client */
            //System.out.println("[SEND] Waiting for a client.");
            mem.waitEvent();
            /* Read the client's ID */
            receiver = mem.readInt(SYNC(me) + CLIENT);
            //System.out.println("[SEND] Got a client, it is VM number " + String.valueOf(receiver));

            /* Initialize the block data */
            mem.spinLock(BASE(block) + LOCK);
            mem.initLock(BASE(block) + FLOCK);
            mem.writeInt(0, BASE(block) + FULL);
            mem.initLock(BASE(block) + ELOCK);
            mem.writeInt(NCHUNKS, BASE(block) + EMPTY);

            /* Already have the block number written, so irq the client immediately and wait for a filename */
            mem.waitEventIrq(receiver);
            //System.out.println("[SEND] Waiting for client to write a filename.");
            mem.waitEvent();

            /* Read the filename */
            sendfile = mem.readString(BASE(block) + FNAME);

            /* Open the file and get its size */
            FileInputStream file = new FileInputStream(sendfile);
            total = (long)file.getChannel().size();

            /* Send the size and wait for an ack */
            //System.out.println("[SEND] Sending size to sender: " + String.valueOf(total));
            mem.writeLong(total, BASE(block) + SIZE);
            mem.waitEventIrq(receiver);
            //System.out.println("[SEND] Waiting for ack");
            mem.waitEvent();

            System.out.println("[SEND] Sending " + sendfile + " to client " + String.valueOf(receiver));

            /* Do the send! */
            sent = 0;
            for(idx = 0; sent < total; idx = NEXT(idx)) {
                //System.out.println("[SEND] Waiting for empty slot");
                do {
                    Thread.sleep(50);
                    empty = mem.readInt(BASE(block) + EMPTY);
                } while(empty == 0);

                while(mem.spinLock(BASE(block) + ELOCK) != 0);
                empty = empty - 1;
                mem.writeInt(empty, BASE(block) + EMPTY);
                mem.spinUnlock(BASE(block) + ELOCK);
                //System.out.println("[SEND] Decremented empty to " + String.valueOf(empty));

                file.read(bytes);
                mem.writeBytes(bytes, OFFSET(block, idx), CHUNK_SZ);
                sent += CHUNK_SZ;
                //System.out.println("[SEND] Sent bytes in slot " + String.valueOf(idx) + " sent = " + String.valueOf(sent));
                System.out.println("[SEND] " + sent + "B / " + total + "B = " + String.valueOf((float)sent / (float)total * 100.0) + "%");

                while(mem.spinLock(BASE(block) + FLOCK) != 0);
                full = mem.readInt(BASE(block) + FULL);
                full = full + 1;
                mem.writeInt(full, BASE(block) + FULL);
                mem.spinUnlock(BASE(block) + FLOCK);
                //System.out.println("[SEND] Incremented full to " + String.valueOf(full));
            }

            //System.out.println("[SEND] Done, closing file.");
            file.close();
            /* Unlock my memory */
            mem.spinUnlock(BASE(block) + LOCK);
        }

        mem.closeDevice();
    }
}
