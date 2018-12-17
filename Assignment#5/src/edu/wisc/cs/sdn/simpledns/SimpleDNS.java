package edu.wisc.cs.sdn.simpledns;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.math.BigInteger;
import java.util.*;
import edu.wisc.cs.sdn.simpledns.packet.DNS;
import edu.wisc.cs.sdn.simpledns.packet.DNSRdata;
import edu.wisc.cs.sdn.simpledns.packet.*;
import edu.wisc.cs.sdn.simpledns.ReadCSV;
import edu.wisc.cs.sdn.simpledns.EC2Record;
public class SimpleDNS 
{
	/* Data Members */
	DatagramSocket serverSocket;
	DatagramPacket questionPacket;
	byte[] questionData;
	DatagramPacket answerPacket;
	byte[] answerData;

	DatagramSocket clientSocket;
	DatagramPacket reqPacket;
	DatagramPacket respPacket;

	/* Server parameters */
	int port;
	InetAddress rootNS;
	String ec2File;
	InetAddress serverAddress;
	List<EC2Record> ec2list;

	public SimpleDNS(int port, String rootNS, String ec2File) {
		this.port = port;
		try {
			this.rootNS = InetAddress.getByName(rootNS);
			//this.serverAddress = InetAddress.getLocalHost();
			this.serverAddress = InetAddress.getByName("localhost");
		} catch(Exception e) {
			System.out.println(e);
			System.exit(0);
		}

		// EC2 File configuration
		this.ec2File = ec2File;
		ReadCSV csvReader = new ReadCSV();
                this.ec2list = csvReader.init(this.ec2File);
		//System.out.println(ec2list);

		// Question & Answer data byte array
		this.questionData = new byte[1500];
		this.answerData = new byte[1500];

		try {
			serverSocket = new DatagramSocket(port, serverAddress);
		} catch(Exception e) {
			System.out.println(e);
			System.exit(0);
		}
	}

	void initServer() {
		while(true) {
			try {
				System.out.println("Waiting for data...!");
				Arrays.fill(questionData, (byte)0);
				questionPacket = new DatagramPacket(questionData, questionData.length);
				serverSocket.receive(questionPacket);
				System.out.println("Rcvd packet");
			}catch(IOException e) {
				return;
			}catch(Exception e) {
				System.out.println("Error : Exception " + e);
			}

			/* Unwrapping UDP Packet */
			DNS dnsQuestionPacket = new DNS();
			dnsQuestionPacket = dnsQuestionPacket.deserialize(questionPacket.getData(), questionPacket.getLength());

			// Is Standard Query Check
			if(dnsQuestionPacket.getOpcode() == DNS.OPCODE_STANDARD_QUERY) {

				short type = dnsQuestionPacket.getQuestions().get(0).getType();
				// Is Expected query type
				if(type != DNS.TYPE_A && type != DNS.TYPE_AAAA && type != DNS.TYPE_CNAME && type != DNS.TYPE_NS) {
					System.out.println("Not an expected DNS query type!");
					continue;
				}

				System.out.println("Received DNS Questions:");
				System.out.println(dnsQuestionPacket.getQuestions());
				DNS resolvedPacket = null;
				// If recursive bit set
				if(dnsQuestionPacket.isRecursionDesired() == true) {
					resolvedPacket = recurseDNSRequest(dnsQuestionPacket, type);
				} else {
					resolvedPacket = sendDNSRequest(dnsQuestionPacket, this.rootNS);
					sendNonRecurseReply(resolvedPacket, questionPacket.getAddress(), questionPacket.getPort());
					continue;
				}

				if(resolvedPacket == null) {
					// Could not resolve for some reason
					sendDummyReply(dnsQuestionPacket, questionPacket.getAddress(), questionPacket.getPort());
					continue;
				}

				DNSRdataAddress data = null;
				String domainName = "";
				List<DNSResourceRecord> locanswers = new ArrayList<>();

				// Checking for EC2 Record matches
				for(DNSResourceRecord rec : resolvedPacket.getAnswers()) {
					if(rec.getType() == DNS.TYPE_A) {
						domainName = rec.getName();
						data = (DNSRdataAddress)rec.getData();
						
						String finalARecord = data.getAddress().getHostAddress();
						String ec2location = checkEC2Address(finalARecord);
						if(!ec2location.equals("")){
							String textString = ec2location + "-" + data; 
							DNSRdata ARecordWithEC2loc = new DNSRdataString(textString);
							DNSResourceRecord locationRec = new DNSResourceRecord(domainName, DNS.TYPE_TXT, ARecordWithEC2loc); 
							locationRec.setTtl(60);
							locanswers.add(locationRec);
						}
					}
				}
				if(locanswers.size() > 0){
					for(DNSResourceRecord rec : locanswers)
						resolvedPacket.addAnswer(rec);
				}
				sendDNSReply(resolvedPacket, questionPacket.getAddress(), questionPacket.getPort());
			}
		}
	}
	
	public boolean sendDNSReply(DNS packet, InetAddress IPAddress, int port) {
		try {
			answerData = constructAnswerPacket(packet).serialize();
			answerPacket = new DatagramPacket(answerData, answerData.length, IPAddress, port);
			serverSocket.send(answerPacket);
			System.out.println("Sent DNS answer to client IP [" + IPAddress + "] @ Port[" + port + "]");
		} catch(Exception e) {
			System.out.println(e);
			return false;
		}

		return true;
	}

	public boolean sendDummyReply(DNS packet, InetAddress IPAddress, int port) {
		try {
			List<DNSResourceRecord> ans = new ArrayList<>();
			packet.setAnswers(ans);
			answerData = packet.serialize();
			answerPacket = new DatagramPacket(answerData, answerData.length, IPAddress, port);
			serverSocket.send(answerPacket);
			System.out.println("Sent DNS answer to client IP [" + IPAddress + "] @ Port[" + port + "]");
		} catch(Exception e) {
			System.out.println(e);
			return false;
		}

		return true;
	}

	public boolean sendNonRecurseReply(DNS packet, InetAddress IPAddress, int port) {
		try {
			answerData = constructNonRecurseAnswerPacket(packet).serialize();
			answerPacket = new DatagramPacket(answerData, answerData.length, IPAddress, port);
			serverSocket.send(answerPacket);
			System.out.println("Sent DNS answer to client IP [" + IPAddress + "] @ Port[" + port + "]");
		} catch(Exception e) {
			System.out.println(e);
			return false;
		}

		return true;
	}

	public DNS constructAnswerPacket(DNS packet) {
		DNS ans = new DNS();
		ans.setId(packet.getId());
		ans.setQuery(false);
		ans.setOpcode(packet.getOpcode());
		/* Setting the DNS Answers */
		ans.setAnswers(packet.getAnswers());
		ans.setAuthorities(packet.getAuthorities());
		ans.setAdditional(packet.getAdditional());
		ans.setQuestions(packet.getQuestions());

		System.out.println("--------------------");
		System.out.println(ans);
		System.out.println("--------------------");
		return ans;
	}

	public DNS constructNonRecurseAnswerPacket(DNS packet) {
		DNS ans = new DNS();
		ans.setId(packet.getId());
		ans.setQuery(false);
		ans.setOpcode(packet.getOpcode());
		/* Setting the DNS Answers */
		ans.setAnswers(packet.getAnswers());
		ans.setQuestions(packet.getQuestions());
		ans.setAdditional(packet.getAdditional());
		ans.setAuthorities(packet.getAuthorities());

		System.out.println("--------------------");
		System.out.println(ans);
		System.out.println("--------------------");
		return ans;
	}

	public String checkEC2Address(String ipAddress){
		int ip = ReadCSV.toIPv4Address(ipAddress);
		int maxlength = Integer.MIN_VALUE;
		String location = "";
		for(EC2Record rec : ec2list){
			int mynetworknumber = rec.subnetMask & ip;
			if(mynetworknumber == rec.networkNumber && rec.length > maxlength){
				maxlength = rec.length;
				location = rec.location;
			}
		}
		System.out.println("location when match found: " + location);
		return location;
	}

	public DNS recurseDNSRequest(DNS dnsQuestionPacket, short type) {
		/* Start with RootNS */
		InetAddress requestIPAddress = this.rootNS;
		InetAddress resultIPAddress = null;
		try {
			resultIPAddress = InetAddress.getByName("");
		} catch(Exception e) {
			System.out.println(e);
		}

		DNS dnsReply = new DNS();
		boolean isResolved = false;

		System.out.println("------------------------------");
		System.out.println("Resolving DNS Request");
		System.out.println("------------------------------");

		/* Recursively get the A record of Domain Name */
		while(isResolved != true) {
			System.out.println("Requesting IP Address : " + requestIPAddress);
			System.out.println(dnsQuestionPacket.getQuestions());
			dnsReply = sendDNSRequest(dnsQuestionPacket, requestIPAddress);

			System.out.println("=====================");
			System.out.println("Ans : " + dnsReply.getAnswers());
			//System.out.println("Auth : " + dnsReply.getAuthorities());
			//System.out.println("Additional : " + dnsReply.getAdditional());
			System.out.println("=====================");

			/* If the answers section is not empty, DNS request resolved */
			if(dnsReply.getAnswers().size() != 0) {
				System.out.println("Final Answer : ");
				System.out.println(dnsReply.getAnswers());
				isResolved = true;
				break;
			}

			DNSRdataAddress data = null;
			boolean hasRequiredRecordInReply = false;
			/* Get the A record corresponding to the NS record from Additional section */
			System.out.println("Checking the additional section for A records");
			for(DNSResourceRecord rec : dnsReply.getAdditional()) {
				if(rec.getType() == DNS.TYPE_A) {
					data = (DNSRdataAddress)rec.getData();
					hasRequiredRecordInReply = true;
					break;
				}
			}

			/* TBD:Check if the addition section is empty */
			/* If so, use authoritative sections to resolve and get the IP */
			if(hasRequiredRecordInReply == true) {
				System.out.println("Found A Record : " + data);
				resultIPAddress = data.getAddress();
			} else {
				System.out.println("No A record in additional section");
				if(dnsReply.getAuthorities().size() == 0) {
					System.out.println("No authoritative section! Unexpected condition!");
				} else {
					System.out.println("Using Authoritative section - Getting A record for NS");
					System.out.println(dnsReply.getAuthorities());
					System.out.println("Type : " + dnsReply.getAuthorities().get(0).getType());
					if(dnsReply.getAuthorities().get(0).getType() == 6) {
						System.out.println("Could not resolve NS! Authorities have unknown type record");
						return null;
					}
					DNSRdataName NSName = null;
					try {
						NSName = (DNSRdataName)dnsReply.getAuthorities().get(0).getData();
					} catch(Exception e) {
						System.out.println(e);
						System.exit(1);
					}
					System.out.println("Frist NS record : " + NSName.getName());
					resultIPAddress = resolveNSRecord(dnsQuestionPacket, NSName.getName(), DNS.TYPE_A);
					System.out.println("Found IP for NS : " + resultIPAddress);
				}
			}
			if(isResolved != true) {
				System.out.println("Next Name Server IP to contact : " + resultIPAddress);
				requestIPAddress = resultIPAddress;
				dnsReply = null;
			}
		}

		/* If the answer is only CNAME, resolve it again to get the A record */
		boolean gotRequiredRecord = false;
		DNSResourceRecord cnameRec = null;
		for(DNSResourceRecord rec : dnsReply.getAnswers()) {
			if(rec.getType() == type) {
				gotRequiredRecord = true;
				break;
			}
			else if(cnameRec == null && rec.getType() == DNS.TYPE_CNAME) {
				cnameRec = rec;
			}
		}

		if(gotRequiredRecord == false) {

			// Recursively resolve the CNAME record for reuiqred record
			while(gotRequiredRecord == false) {
				DNS cnameDNSReply = dnsReply;
				DNSRdataName aliasName = (DNSRdataName)cnameRec.getData();
				DNS cnameResolvePacket = constructDNSPacket(dnsQuestionPacket.getId(), dnsQuestionPacket.getOpcode(), aliasName.getName(), type);

				System.out.println("--------------------------");
				System.out.println("Resolving CNAME record");
				System.out.println("--------------------------");
				cnameDNSReply = recurseDNSRequest(cnameResolvePacket, type);
				System.out.println("--------------------------");
				System.out.println("Answer : ");
				if(cnameDNSReply == null)
				{
					gotRequiredRecord = true;
					break;
				}
				System.out.println(cnameDNSReply.getAnswers());

				// Add CNAME reply to the final reply to client
				for(DNSResourceRecord rec : cnameDNSReply.getAnswers()) {
					dnsReply.addAnswer(rec);
					if(rec.getType() == type) {
						gotRequiredRecord = true;
					}
					else if(rec.getType() == DNS.TYPE_CNAME) {
						cnameRec = rec;
					}
				}
			}
		}
		System.out.println("--------------------------");
		return dnsReply;
	}

	public InetAddress resolveNSRecord(DNS packet, String name, short type) {
		System.out.println("-> Resolving NS record for " + name + " for type " + type);
		DNS nsRecordPacket = constructDNSPacket(packet.getId(), packet.getOpcode(), name, type);
		DNS nsReplyPacket = recurseDNSRequest(nsRecordPacket, DNS.TYPE_A);
		
		DNSRdataAddress data = null;
		for(DNSResourceRecord rec : nsReplyPacket.getAnswers()) {
			if(rec.getType() == DNS.TYPE_A) {
				data = (DNSRdataAddress)rec.getData();
				break;
			}
		}
		if(data == null){
			System.out.println("No A record found - not able to resolve NS record");
			System.exit(1);
		}
		return data.getAddress();
	}

	public DNS constructDNSPacket(short id, byte opcode, String name, short type) {
		DNS packet = new DNS();
		packet.setId(id);
		packet.setQuery(true);
		packet.setOpcode(opcode);
		packet.setRecursionDesired(true);
		packet.setRecursionAvailable(false);

		System.out.println("Construction DNS packet");
		//System.out.println("Name : " + name);
		//System.out.println("Type : " + type);

		/* Setting the DNS question */
		DNSQuestion question = new DNSQuestion(name, type);
		System.out.println("DNS Qs : " + question);
		List<DNSQuestion> qs = new ArrayList<DNSQuestion>();
		qs.add(question);
		packet.setQuestions(qs);

		//packet.setAdditional(additional);
		//System.out.println("Constructed packet");
		//System.out.println("--------------------");
		//System.out.println(packet);
		//System.out.println("--------------------");
		return packet;
	}


	public DNS sendDNSRequest(DNS inPacket, InetAddress reqIPAddress) {
		// Sending DNS Request packet
		/* Acting as a client */
		byte respData[] = new byte[3000];

		DNS dnsReply = new DNS();
		try {
			clientSocket = new DatagramSocket();
			byte[] requestData = inPacket.serialize();
			reqPacket = new DatagramPacket(requestData, requestData.length, reqIPAddress, 53);
			clientSocket.send(reqPacket);
			System.out.println("Sent DNS request to [" + reqIPAddress + "] @ [" + 53 + "]");

			// Receiving DNS Reply
			System.out.println("Waiting for UDP reply");
			respPacket = new DatagramPacket(respData, respData.length);
			clientSocket.receive(respPacket);
		} catch(Exception e) {
			System.out.println(e);
		}
		dnsReply = dnsReply.deserialize(respPacket.getData(), respPacket.getLength());

		System.out.println(dnsReply.getAnswers());
		System.out.println(dnsReply.getAuthorities());
		System.out.println(dnsReply.getAdditional());
		return dnsReply;
	}

	public static int toIPv4Address(String ipAddress) {
		if (ipAddress == null)
		    throw new IllegalArgumentException("Specified IPv4 serverAddress must" +
			"contain 4 sets of numerical digits separated by periods");
		String[] octets = ipAddress.split("\\.");
		if (octets.length != 4)
		    throw new IllegalArgumentException("Specified IPv4 serverAddress must" +
			"contain 4 sets of numerical digits separated by periods");

		int result = 0;
		for (int i = 0; i < 4; ++i) {
		    result |= Integer.valueOf(octets[i]) << ((3-i)*8);
		}
		return result;
	}

	public static void main(String[] args)
	{
		System.out.println("Hello, DNS Sever!");
		if(args.length < 4) {
			System.out.println("Error: missing or additional arguments");
			System.out.println("Usage : java <path>/SimpleDNS -r <root server ip> -e <ec2 csv>");
			System.exit(0);
		}

		if(!args[0].equals("-r") || !args[2].equals("-e")) {
			System.out.println("Usage : java <path>/SimpleDNS -r <root server ip> -e <ec2 csv>");
			System.exit(0);
		}

		SimpleDNS dns = new SimpleDNS(8053, args[1], args[3]);
		dns.initServer();
	}
}
