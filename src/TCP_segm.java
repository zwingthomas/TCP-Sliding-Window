import java.nio.ByteBuffer;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TCP_segm{
    protected int sequence;
    protected int acknowledgment;
    protected long timeStamp;

    //length: the length of the data portion in bytes
    protected int length; //least three significant bits are used for flags S (SYN), A (ACK), and F (FIN)
    protected char flag;
    protected short checksum;
    protected byte[] data;
    protected int totalLength;

    TCP_segm(int s, int a, long t, int l, short c, byte[] d, char f){
        this.sequence = s;
        this.acknowledgment = a;
        this.timeStamp = t;
        this.length = l;
        this.flag = f;
        this.checksum = c;
        this.data = d;
    }


    public int getSequence(){return this.sequence;}
    public int getAcknowlegment(){return this.acknowledgment;}
    public long getTimeStamp(){return this.timeStamp;}
    public int getLength(){return this.length;}
    public short getChecksum(){return this.checksum;}
    public byte[] getData(){return this.data;}

    public void setSequence(int sequence){
        this.sequence = sequence;
    }
    public void setAcknowledgement(int acknowledgement){
        this.acknowledgment = acknowledgement;
    }
    public void setTimeStamp(long timeStamp){
        this.timeStamp = timeStamp;
    }
    public void setLength(int length){
        this.length = length;
    }
    public void setChecksum(short checksum){ this.checksum = checksum; }
    public void setData(byte[] data){ this.data = data; }

    public String toString(){
        String data = new String(this.data);
        String str = "Sequence Number: " + this.getSequence() + "\n" +
                    "Acknowledgement Number: " + this.getAcknowlegment() + "\n" +
                    "TimeStamp: " + this.convertTime(this.timeStamp) + "\n" +
                    "Length: " + this.length + "\n" +
                    "Checksum: " + this.checksum + "\n" +
                    "Data: " + data;
        return str;
    }

    public String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy MM dd HH:mm:ss");
        return format.format(date);
    }

    public byte[] serialize(){

        this.totalLength = this.length + 32 * 6; //data length +all the header values

        byte[] data = new byte[this.totalLength];
        ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putInt(this.sequence);       //index: 0 - 3
        bb.putInt(this.acknowledgment); //index: 4 - 7
        bb.putLong(this.timeStamp);     //index: 8 - 15
        bb.putInt(this.length + this.flag);         //index: 16 - 19

        //concat 16 bits of zeros
        short allZero = 0;
        bb.putShort(allZero);           //index: 20 - 21

        //compute checksum
        short half = (short) this.sequence;
        short rest = (short) (this.sequence >> 16);
        if((short)(half + rest) < half || (short)(half + rest) < rest)
            this.checksum = (short) (half + rest + 1);
        else
            this.checksum = (short) (half + rest);
        bb.putShort(this.checksum);     //index: 22 - 23
        bb.put(this.data);              //index: 24

        return data;
    }

    public TCP_segm deserialize(byte[] data){
        String s = new String(data);
        System.out.println("Length: " + data.length);
        System.out.println("Data: " + s);
        ByteBuffer bb = ByteBuffer.wrap(data, 0, data.length);
        this.sequence = bb.getInt();
        this.acknowledgment = bb.getInt();
        this.timeStamp = bb.getLong();
        this.length = bb.getInt();
        int flagNum = this.length & 7;        //bit operation to get the flag bits
        //TODO: change to array of flags to account for cases of multiple flags
        if(flagNum == 1)
            this.flag = 'F';
        if(flagNum == 2)
            this.flag = 'A';
        if(flagNum == 4)
            this.flag = 'S';
        this.length = this.length & Integer.MAX_VALUE - 7; //strip the last three bits
        System.out.println("this.length: " + this.length);
        short allZeros = bb.getShort();
        assert(allZeros == 0);
        this.checksum = bb.getShort();
        System.out.println("checksum" + this.checksum);
        int i = 0;
        //TODO: change this size \/
        this.data = new byte[1000];
        while(bb.remaining() != 0) {
            this.data[i] = bb.get();
            i++;
        }
        return this;
    }

}
