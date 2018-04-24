import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TCPend {
    public static void main(String args[]) throws UnknownHostException {

        int port = 0;
        String remote_IP = null;
        int remote_port = 0;
        String file_name = null;
        int mtu = 0;
        int sws = 0;

        boolean remote = true;

        for(int i = 0; i < args.length; i++){
            switch(args[i]){
                case "-p":
                    port = Integer.parseInt(args[i+1]);
                    i++;
                    break;
                case "-s":
                    remote_IP = args[i+1];
                    i++;
                    remote = false;
                    break;
                case "-a":
                    remote_port = Integer.parseInt(args[i+1]);
                    i++;
                    remote = false;
                    break;
                case "-f":
                    file_name = args[i+1];
                    i++;
                    remote = false;
                    break;
                case "-m":
                    mtu = Integer.parseInt(args[i+1]);
                    i++;
                    break;
                case "-c":
                    sws = Integer.parseInt(args[i+1]);
                    i++;
                    break;
            }
        }

        System.out.println("-p: " + port);
        System.out.println("-s: " + remote_IP);
        System.out.println("-a: " + remote_port);
        System.out.println("-f: " + file_name);
        System.out.println("-m: " + mtu);
        System.out.println("-c: " + sws);

        int slidingWindow = 0;

        //for Sender
        if(remote == false) {
            //read the file from fileName
            byte[] data = null;
            try {
                Path path = Paths.get(file_name);
                data = Files.readAllBytes(path);
            } catch (java.io.IOException e) {
                System.out.println("Could not read in file.");
            }
            //send first SYN packet
            //TCP_send();

            //create TCP segments size of mtu accounting for the 24 byte header
            int i = 0;
            int s = 50881;
            TCP_segm[] segArr = new TCP_segm[data.length/(mtu - 24)];
            TCP_send sender = new TCP_send(port, remote_IP, remote_port, slidingWindow, 'D');

            //while(i < data.length) {
            byte[] d = data;
            int length = data.length;
            long currTime = System.currentTimeMillis();
            TCP_segm segment = new TCP_segm(s, (s+1), currTime, length, (short) 0, d, 'D');

            //TODO: timer for timeout
            segment.serialize();
            sender.send(segment);
            s += length;

            //if this is how much we want to send, then send it
            i += mtu;
            //}
        }

        //for Receiver
        if(remote == true){
            TCP_recv receiver = new TCP_recv(port, mtu, slidingWindow);
            receiver.receive();
        }

        //send data in units of mtu -> http://www.baeldung.com/udp-in-java


        //keep track of packets sent so far which have not been acknowledged


    }
}

