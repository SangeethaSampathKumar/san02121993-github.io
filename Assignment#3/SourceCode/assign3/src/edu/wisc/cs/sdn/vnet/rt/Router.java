package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.MACAddress;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.Data;
import java.util.*;
/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
	/** Routing table for the router */
	private RouteTable routeTable;
	
	/** ARP cache for the router */
	private ArpCache arpCache;
	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
	}
	
	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable()
	{ return this.routeTable; }
	
	/**
	 * Load a new routing table from a file.
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile)
	{
		if (!routeTable.load(routeTableFile, this))
		{
			System.err.println("Error setting up routing table from file "
					+ routeTableFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}
	
	/**
	 * Load a new ARP cache from a file.
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile)
	{
		if (!arpCache.load(arpCacheFile))
		{
			System.err.println("Error setting up ARP cache from file "
					+ arpCacheFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
                etherPacket.toString().replace("\n", "\n\t"));
		
		/********************************************************************/
		/* Handle packets */

		/* CHECK 1 : Ethernet Packet */
		if(etherPacket.getEtherType() != 0x800) {
			/* Not IP Packet - Dropping */
			return;
		}
		IPv4 pkt = (IPv4)etherPacket.getPayload();

		/* CHECK 2 : Checksum Validation */
		short actualCheckSum = pkt.getChecksum();
		pkt.resetChecksum();
		pkt.serialize();
		short currentChecksum = pkt.getChecksum();
		if(actualCheckSum != currentChecksum) {
			/* Checksum mismatch - Dropping */
			return;
		}

		/* CHECK 3 : TTL Validation */
		byte currentTTL = pkt.getTtl();
		currentTTL--;
		System.out.println("TTL : " + pkt.getTtl());
		if(currentTTL == 0) {
			/* TTL 0 - Dropping */
			/* TBD: Construct a ICMP TLE packet */
			this.sendICMPPacket(pkt, inIface, (byte)11, (byte)0);
			return;
		}

		/* Updating Packet : New TTL & Checksum */
		pkt.setTtl(currentTTL);
		pkt.resetChecksum();
		pkt.serialize();

		/* CHECK 4 : Is packet destined for router interface IP Address */
		for(Map.Entry<String, Iface> entry: interfaces.entrySet()){
			if(pkt.getDestinationAddress() == entry.getValue().getIpAddress()){
				/* Packet Destined for Routers IP - Dropping */
				byte protocolType = pkt.getProtocol();
				if(protocolType == IPv4.PROTOCOL_TCP || protocolType == IPv4.PROTOCOL_UDP){
					System.out.println("Protocol type is TCP or UDP");
					this.sendICMPPacket(pkt, inIface, (byte)3, (byte)3);
				}
				else if(protocolType == IPv4.PROTOCOL_ICMP) {
					System.out.println("Protocol type is ICMP");
					ICMP icmpPkt = (ICMP)pkt.getPayload();
					if(icmpPkt.getIcmpType() == (byte)8) {
						this.sendEchoReplyPacket(pkt, inIface, (byte)0, (byte)0);
					}
				}
				return;
			}
		}

		/* Forwarding Packets */
		/* STEP 1 : Route Table Look up */
		RouteEntry rEntry = routeTable.lookup(pkt.getDestinationAddress());
		//System.out.println("RouteEntry obj: " + rEntry.toString());
		if(rEntry == null) {
			/* No matching route table entry */
			System.out.println("RouteEntry is null");
			this.sendICMPPacket(pkt, inIface, (byte)3, (byte)0);
			return;
		}

		/* CHECK 5 : Check if incoming and outgoing interfaces are same */
		if(inIface.getName().equals(rEntry.getInterface().getName())){
			/* Incoming Interface is same as outgoing interface - dropping */
                         return;
                 }

		/* STEP 2 : Find the next hop IP Address */
		int nextHopIPAddress = rEntry.getGatewayAddress();
		if(nextHopIPAddress == 0){
			nextHopIPAddress = pkt.getDestinationAddress();
		}

		/* Find the next hop MAC address from ARP Cache */
		/* CHECK 6 : Checking non-existent Host in any network connected to Router */
		ArpEntry ae = arpCache.lookup(nextHopIPAddress);
		if(ae == null) {
			System.out.println("No arp entry");
			this.sendICMPPacket(pkt, inIface, (byte)3, (byte)1);
			/* No such host in the network - Dropping */
			return;
		}

		/* Next hop MAC addresses */
		MACAddress destinationMac = ae.getMac();
		/* Outgoing router Interface MAC address */
		MACAddress sourceMac = rEntry.getInterface().getMacAddress();

		/* STEP 3 : Update Ethernet Pakcet to send */
		etherPacket.setDestinationMACAddress(destinationMac.toString());
		etherPacket.setSourceMACAddress(sourceMac.toString());
		
		/* Send Packet on the interface found from Route Table */
		sendPacket(etherPacket, rEntry.getInterface());

		/********************************************************************/
	}

	public void sendICMPPacket(IPv4 pktIn, Iface inIface, byte type, byte code) {
		Ethernet ether = new Ethernet();
		IPv4 ip = new IPv4();
		ICMP icmp = new ICMP();
		Data data = new Data();

		/* ICMP Header construction */
		icmp.setIcmpType(type);
		icmp.setIcmpCode(code);

		/* ICMP Data construction */
		int len = 4 /* 4 byte padding */ +
			+ (pktIn.getHeaderLength() * 4) /* IP Header length*/ +
			+ 8 /* 8 bytes of IP payload */;
		System.out.println("Data Length : " + len);
		byte[] icmpData = new byte[len];
		/* Padding */
		Arrays.fill(icmpData, 0, 4, (byte)0);
		/* IP Header copying */
		byte[] serializedIPPkt = pktIn.serialize();
		int i, j, k;
		System.out.println("PK Header length : " + pktIn.getHeaderLength());
		for(i = 0, j = 4; i < (pktIn.getHeaderLength() * 4); i++, j++) {
			//System.out.println("Data Index(HD) : j : " + j);
			icmpData[j] = serializedIPPkt[i];
		}
		/* 8 byte of IP playload */
		k = i;
		while(k < (i + 8)) {
			//System.out.println("Data Index(PL) : j : " + j);
			icmpData[j] = serializedIPPkt[k];
			j++;
			k++;
		}
		data.setData(icmpData);

		/* IPv4 header construction */
		ip.setTtl((byte)64);
		ip.setProtocol(IPv4.PROTOCOL_ICMP);
		ip.setSourceAddress(inIface.getIpAddress());
		ip.setDestinationAddress(pktIn.getSourceAddress());

		/* Ethernet header construction */
		ether.setEtherType(Ethernet.TYPE_IPv4);
		ether.setSourceMACAddress(inIface.getMacAddress().toString());
		RouteEntry rEntry = routeTable.lookup(pktIn.getSourceAddress());
		if(rEntry == null) {
			/* No matching route table entry */
			/* Ideally won't hit */
			return;
		}
		/* Find the next hop IP Address */
		int nextHopIPAddress = rEntry.getGatewayAddress();
		if(nextHopIPAddress == 0){
			nextHopIPAddress = pktIn.getSourceAddress();
		}
		/* Find the next hop MAC address from ARP Cache */
		ArpEntry ae = arpCache.lookup(nextHopIPAddress);
		if(ae == null) {
			/* No such host in the network - Dropping */
			/* Ideally won't hit */
			return;
		}
		/* Next hop MAC addresses */
		ether.setDestinationMACAddress(ae.getMac().toString());

		ether.setPayload(ip);
		ip.setPayload(icmp);
		icmp.setPayload(data);

		System.out.println("Type: " + icmp.getIcmpType());
		System.out.println("Code: " + icmp.getIcmpCode());
		/* Ether packet constructed */
		/*
		System.out.println(ether);
		System.out.println("ET : " + ether.getEtherType());

		System.out.println("TTL : " + ip.getTtl());
		System.out.println("Protocol : " + ip.getProtocol());
		byte[] test = data.getData();
		System.out.println("ICMP Data");
		for(i = 0; i < len; i++)
			System.out.print(test[i] + " ");

		System.out.println("PKT");
		for(i = 0; i < serializedIPPkt.length; i++)
			System.out.print(serializedIPPkt[i] + " ");
		*/
		/* Send ICMP packet */
		sendPacket(ether, inIface);
	}

	public void sendEchoReplyPacket(IPv4 pktIn, Iface inIface, byte type, byte code) {
		Ethernet ether = new Ethernet();
		IPv4 ip = new IPv4();
		ICMP icmp = new ICMP();
		Data data = new Data();

		/* ICMP Header construction */
		icmp.setIcmpType(type);
		icmp.setIcmpCode(code);

		/* ICMP Data construction */
		ICMP inIcmpPkt = (ICMP)pktIn.getPayload();
		byte[] inIcmpPktPayload = inIcmpPkt.getPayload().serialize();
		data.setData(inIcmpPktPayload);

		/* IPv4 header construction */
		ip.setTtl((byte)64);
		ip.setProtocol(IPv4.PROTOCOL_ICMP);
		ip.setSourceAddress(pktIn.getDestinationAddress());
		ip.setDestinationAddress(pktIn.getSourceAddress());

		/* Ethernet header construction */
		ether.setEtherType(Ethernet.TYPE_IPv4);
		ether.setSourceMACAddress(inIface.getMacAddress().toString());
		RouteEntry rEntry = routeTable.lookup(pktIn.getSourceAddress());
		if(rEntry == null) {
			/* No matching route table entry */
			/* Ideally won't hit */
			return;
		}
		/* Find the next hop IP Address */
		int nextHopIPAddress = rEntry.getGatewayAddress();
		if(nextHopIPAddress == 0){
			nextHopIPAddress = pktIn.getSourceAddress();
		}
		/* Find the next hop MAC address from ARP Cache */
		ArpEntry ae = arpCache.lookup(nextHopIPAddress);
		if(ae == null) {
			/* No such host in the network - Dropping */
			/* Ideally won't hit */
			return;
		}
		/* Next hop MAC addresses */
		ether.setDestinationMACAddress(ae.getMac().toString());

		ether.setPayload(ip);
		ip.setPayload(icmp);
		icmp.setPayload(data);

		System.out.println("Type: " + icmp.getIcmpType());
		System.out.println("Code: " + icmp.getIcmpCode());
		/* Ether packet constructed */
		/*
		System.out.println(ether);
		System.out.println("ET : " + ether.getEtherType());

		System.out.println("TTL : " + ip.getTtl());
		System.out.println("Protocol : " + ip.getProtocol());
		byte[] test = data.getData();
		int i;
		System.out.println("ICMP Data");
		for(i = 0; i < test.length; i++)
			System.out.print(test[i] + " ");

		System.out.println();
		System.out.println("PKT");
		for(i = 0; i < inIcmpPktPayload.length; i++)
			System.out.print(inIcmpPktPayload[i] + " ");
		*/
		/* Send ICMP packet */
		sendPacket(ether, inIface);
	}
}
