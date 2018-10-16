package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.MACAddress;
import net.floodlightcontroller.packet.IPv4;
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
		/* TODO: Handle packets                                             */
		System.out.println("------PACKET PROCESSING START--------");

		MACAddress pktMAC = etherPacket.getDestinationMAC();
		MACAddress ifaceMAC = inIface.getMacAddress();
		if(!pktMAC.equals(ifaceMAC)) {
			System.out.println("Not my packet! Dropping!");
			return;
		}
		if(etherPacket.getEtherType() != 0x800) {
			System.out.println("Not IPv4");
			return;
		}
		System.out.println(etherPacket.toString());
		IPv4 pkt = (IPv4)etherPacket.getPayload();

		/* Checksum validation */	
		short actualCheckSum = pkt.getChecksum();
		System.out.println("From sender CS : " + actualCheckSum);
		pkt.resetChecksum();
		System.out.println("At Reset CS : " + pkt.getChecksum());
		pkt.serialize();
		short currentChecksum = pkt.getChecksum();
		System.out.println("At Reciever CS : " + currentChecksum);
		if(actualCheckSum != currentChecksum) {
			System.out.println("Checksum mismatch");
			return;
		}

		/* TTL Validation */
		byte currentTTL = pkt.getTtl();
		System.out.println("Prev TTL : " + currentTTL);
		currentTTL--;
		System.out.println("Current TTL : " + currentTTL);
		if(currentTTL == 0) {
			System.out.println("TTL 0");
			return;
		}

		/* Updating Packet : New TTL & Checksum */
		System.out.println("Current TTL : " + pkt.getTtl());
		pkt.setTtl(currentTTL);
		System.out.println("Updated TTL : " + pkt.getTtl());
		pkt.resetChecksum();
		System.out.println("Before TTL update CS: " + pkt.getChecksum());
		pkt.serialize();
		System.out.println("After TTL update CS : " + pkt.getChecksum());

		/* Idnetify if packet is destined for router interface IP Address */
      		System.out.println("PKT Destination IP : " + pkt.getDestinationAddress());
		for(Map.Entry<String, Iface> entry: interfaces.entrySet()){
			//System.out.println(entry.getKey() + " " + entry.getValue().getIpAddress());
			if(pkt.getDestinationAddress()  == entry.getValue().getIpAddress()){
				System.out.println("Matching Router IP - dropping !");
				return;
			}
		}

		/* Frowarding Packets */	
		RouteEntry rEntry = routeTable.lookup(pkt.getDestinationAddress());
		if(rEntry == null) {
			System.out.println("No matching LCP interface found - dropping!");
			return;
		}

		/* Route Table entries */
		System.out.println(routeTable.toString());
		/* ARP Cache entried */
		System.out.println(arpCache.toString());
		/**new code */
		int nextHopIPAddress = rEntry.getGatewayAddress();
		if(nextHopIPAddress == 0){
			nextHopIPAddress = pkt.getDestinationAddress();
		}

		/**new code end */
		/* Find next hop MAC address from ARP Cache */
		/* Checking non-existent Host in any network connected to Router */
		ArpEntry ae = arpCache.lookup(nextHopIPAddress);
		if(ae == null) {
			System.out.println("Invalid IP Address : 404");
			return;
		}
		MACAddress destinationMac = ae.getMac();
		/* Find the source MAC of router interface */
		MACAddress sourceMac = rEntry.getInterface().getMacAddress();
		if(destinationMac.equals(sourceMac)){
			System.out.println("Same Interfce - Dropping packet!!!");
			return;
		}
		System.out.println("PKT Source MAC : " + sourceMac.toString() + "\nPKT Dest MAC : " + destinationMac.toString());

		/* Construct Ethernet Pakcet to send */
		//Ethernet newFrame = new Ethernet();
		etherPacket.setDestinationMACAddress(destinationMac.toString());
		etherPacket.setSourceMACAddress(sourceMac.toString());
		//newFrame.setEtherType((short)0x0800);
		//newFrame.setPayload(pkt);

		System.out.println(etherPacket.toString());

		/* Send Packet on the interface found from Route Table */
		sendPacket(etherPacket, rEntry.getInterface());
		System.out.println("------------PACKET SENT-------------");
		/********************************************************************/
	}
}
