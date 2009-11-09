import java.net.URL;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;

public class Getter {
    public static void main(String args[]) throws Exception {
        byte[] buf = new byte[1024*1024];
        int rd;

        System.setProperty("java.protocol.handler.pkgs", "org.ualberta");
        URL u = new URL(args[0]);
        FileOutputStream out = new FileOutputStream((new File(u.getFile())).getName());
        InputStream is = u.openStream();

        while(is.available() > 0) {
            rd = is.read(buf);
            out.write(buf, 0, rd);
        }

        out.close();
        is.close();
    }
}
