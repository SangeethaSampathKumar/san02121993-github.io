package edu.wisc.cs.sdn.simpledns;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
class EC2Record{
	int ipAddress;
	int networkNumber;
	int subnetMask;
	String location;
	int length;

	public EC2Record(int ipAddress, int networkNumber, int subnetMask, String location, int length){
		this.ipAddress = ipAddress;
		this.networkNumber = networkNumber;
		this.subnetMask = subnetMask;
		this.location = location;
		this.length = length;
	}
	
	public String toString(){
		return ReadCSV.fromIPv4Address(ipAddress) + " , " + ReadCSV.fromIPv4Address(networkNumber) + " , " + ReadCSV.fromIPv4Address(subnetMask) + " , " + location + " , " + length; 
	}
	
}
class ReadCSV {
    
    public int setFirstNBits(int n){
    	int nthBitSet = 0;
        nthBitSet = nthBitSet | 1 << (32-n);
        int compliment = nthBitSet - 1;
        compliment = ~compliment;
	return compliment;
        
    }
    public static String fromIPv4Address(int ipAddress) {
        StringBuffer sb = new StringBuffer();
        int result = 0;
        for (int i = 0; i < 4; ++i) {
            result = (ipAddress >> ((3-i)*8)) & 0xff;
            sb.append(Integer.valueOf(result).toString());
            if (i != 3)
                sb.append(".");
        }
        return sb.toString();
    }

    public static int toIPv4Address(String ipAddress) {
        if (ipAddress == null)
            throw new IllegalArgumentException("Specified IPv4 address must" +
                "contain 4 sets of numerical digits separated by periods");
        String[] octets = ipAddress.split("\\.");
        if (octets.length != 4) 
            throw new IllegalArgumentException("Specified IPv4 address must" +
                "contain 4 sets of numerical digits separated by periods");

        int result = 0;
        for (int i = 0; i < 4; ++i) {
            result |= Integer.valueOf(octets[i]) << ((3-i)*8);
        }
        return result;
    }

    public List<EC2Record> init(String fileLocation) {

        String csvFile = fileLocation;
        String line = "";
        String cvsSplitBy = ",";
	List<EC2Record> ec2list = new ArrayList<EC2Record>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

            while ((line = br.readLine()) != null) {

                // use comma as separator
                String[] record = line.split(cvsSplitBy);
		String[] subnetInfo = record[0].split("/");
		int ip = toIPv4Address(subnetInfo[0]);
		int len = Integer.valueOf(subnetInfo[1]);
		int subnetmask = setFirstNBits(len);
		int networkNumber = ip & subnetmask;
		EC2Record ec2rec = new EC2Record(ip, networkNumber, subnetmask, record[1], len);
		ec2list.add(ec2rec);
		//System.out.println(ec2rec); 
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
	return ec2list;
    }

}
