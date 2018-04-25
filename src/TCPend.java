import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TCPend {
    public static void main(String args[]) throws IOException {

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

            //TODO: NO SLOW START

            int seqNum = 6969;
            int totalBytesLoaded = 0;
            int neededNumOfSegments = (int)Math.ceil((double)data.length/(double)(mtu - 24));
            DatagramSocket socket = new DatagramSocket(port);
            TCP_send sender = new TCP_send(socket, remote_IP, remote_port, sws, 'D');
            sender.handshake(seqNum);

            //TODO: sliding window

            for(int segCnt = 0; segCnt < neededNumOfSegments; segCnt++){
                //put data to be sent into a smaller container for transmission
                byte[] dataToBeSent;
                if(segCnt == neededNumOfSegments - 1) {
                    System.out.println("Last send.");
                    System.out.println("subtraction: " + (data.length - totalBytesLoaded -1));
                    dataToBeSent = new byte[data.length - 1 - totalBytesLoaded];
                }
                 else {
                    dataToBeSent = new byte[mtu];
                }
                int dataLoadedForSeg;
                for(dataLoadedForSeg = 0; dataLoadedForSeg < (mtu-24) && totalBytesLoaded < data.length-1; dataLoadedForSeg++) {
                    dataToBeSent[dataLoadedForSeg] = data[totalBytesLoaded];
                    totalBytesLoaded++;
                }
                //send segment
                TCP_segm toSend = new TCP_segm(seqNum, (seqNum+1), System.nanoTime(), dataLoadedForSeg * 4, (short) 0, dataToBeSent, "D");
                toSend.serialize(); //computes the checksum

                TCP_segm ack = sender.send(toSend);
            }
        }

        //for Receiver
        if(remote == true){
            DatagramSocket socket = new DatagramSocket(port);
            TCP_recv receiver = new TCP_recv(socket, mtu, sws);
            receiver.handshake(500);
            receiver.receive();
        }

        //send data in units of mtu -> http://www.baeldung.com/udp-in-java


        //keep track of packets sent so far which have not been acknowledged

    }
}

