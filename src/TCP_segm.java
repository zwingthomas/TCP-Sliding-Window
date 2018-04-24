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
    public char getFlag(){return this.flag;}

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
    public void setFlag(char flag){
        this.length = this.length & Integer.MAX_VALUE - 7;
        if(flag == 'F'){
            this.length = this.length + 1;
            this.flag = 'F';
        }
        if(flag == 'A'){
            this.length = this.length + 2;
            this.flag = 'A';
        }
        if(flag == 'S'){
            this.length = this.length + 4;
            this.flag = 'S';
        }
    }

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

        this.totalLength = this.length + 4 * 6; //data length +all the header values

        byte[] data = new byte[this.totalLength];
        ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putInt(this.sequence);                     //index: 0 - 3
        bb.putInt(this.acknowledgment);               //index: 4 - 7
        bb.putLong(this.timeStamp);                   //index: 8 - 15
        bb.putInt(this.length + this.flag);           //index: 16 - 19
        short allZero = 0;                            //concat 16 bits of zeros
        bb.putShort(allZero);                         //index: 20 - 21
        this.checksum = computeChecksum(this.sequence);
        bb.putShort(this.checksum);                   //index: 22 - 23
        bb.put(this.data);                            //index: 24

        return data;
    }

    public TCP_segm deserialize(byte[] data){
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
        if(flagNum == 6)
            this.flag = 'B'; //combination of A and S???? either this or we use an integer representation
        this.length = this.length & Integer.MAX_VALUE - 7; //strip the last three bits
        short allZeros = bb.getShort();
        assert(allZeros == 0);
        this.checksum = bb.getShort();
        int i = 0;
        //TODO: change this size \/
        this.data = new byte[1000];
        while(bb.remaining() != 0) {
            this.data[i] = bb.get();
            i++;
        }
        return this;
    }

    public short computeChecksum(int sequenceNum){

        //Get two bit strings
        String input_str = Integer.toBinaryString(sequenceNum);
        while(input_str.length() < 32)
            input_str = "0"+input_str;
        //System.out.println("input: " + input_str);
        String first = input_str.substring(0, 16);
        //System.out.println("1st half: \t" + first);
        String second = input_str.substring(16, input_str.length());
        //System.out.println("2nd half: \t" + second);

        //Add the two bit strings together
        String sum = addBinary(first, second);
        //System.out.println("Shortsum: " + sum);

        //remember the carry!
        if(sum.length() > 16){
            sum = sum.substring(0, 16);
            int flag = 1;
            for(int i = sum.length()-1; i >= 0; i--){
                if(sum.charAt(i) == '0'){
                    StringBuilder sumBuilder = new StringBuilder(sum);
                    sumBuilder.setCharAt(i, '1');
                    sum = sumBuilder.toString();
                    flag = 0;
                    break;
                }
            }
            if(flag == 1)
                sum = "0000000000000000"; //just 16 zeros
        }
        //System.out.println("carry: " + sum);

        //flip all the bits
        String result_str = "";
        for(int i = 0; i < sum.length(); i++){
            if(sum.charAt(i) == '1')
                result_str = result_str + '0';
            if(sum.charAt(i) == '0')
                result_str = result_str + '1';
        }
        //System.out.println("result_str: " + result_str);

        //Add multiples of two depending on what '1' bits are set
        short result = 0;
        for(int i = 0; i < result_str.length(); i++){
            if(result_str.charAt(result_str.length() - 1 - i) == '1'){
                result += Math.pow(2, i);
            }
        }
        //System.out.println("result:" + result);

        return result;
    }

    public String addBinary(String a, String b){
        StringBuilder stringBuilder = new StringBuilder();
        int carry = 0;

        for(int i = a.length()-1; i >= 0; i--){
            int sum=0;
            if(a.charAt(i)=='1')
                sum++;
            if(b.charAt(i)=='1')
                sum++;
            sum += carry;
            if(sum>=2)
                carry=1;
            else
                carry=0;
            stringBuilder.insert(0,  (char) ((sum%2) + '0'));
        }
        if(carry==1)
            stringBuilder.insert(0, '1');
        return stringBuilder.toString();
    }
}
