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
		/* Handle packets */

		/* CHECK 1 : Ethernet Packet */
		if(etherPacket.getEtherType() != 0x800) {
			//System.out.println("Not IPv4");
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
			//System.out.println("Checksum mismatch");
			/* Checksum mismatch - Dropping */
			return;
		}

		/* CHECK 3 : TTL Validation */
		byte currentTTL = pkt.getTtl();
		currentTTL--;
		if(currentTTL == 0) {
			//System.out.println("TTL 0");
			/* TTL 0 - Dropping */
			return;
		}

		/* Updating Packet : New TTL & Checksum */
		pkt.setTtl(currentTTL);
		pkt.resetChecksum();
		pkt.serialize();

		/* CHECK 4 : Is packet destined for router interface IP Address */
      		//System.out.println("PKT Destination IP : " + pkt.getDestinationAddress());
		for(Map.Entry<String, Iface> entry: interfaces.entrySet()){
			//System.out.println(entry.getKey() + " " + entry.getValue().getIpAddress());
			if(pkt.getDestinationAddress() == entry.getValue().getIpAddress()){
				//System.out.println("Matching Router IP - dropping !");
				/* Packet Destined for Routers IP - Dropping */
				return;
			}
		}

		/* Forwarding Packets */
		/* STEP 1 : Route Table Look up */
		RouteEntry rEntry = routeTable.lookup(pkt.getDestinationAddress());
		if(rEntry == null) {
			//System.out.println("No matching LCP interface found - dropping!");
			/* No matching route table entry */
			return;
		}

		/* CHECK 5 : Check if incoming and outgoing interfaces are same */
		if(inIface.getName().equals(rEntry.getInterface().getName())){
                         //System.out.println("Same Interface - Dropping packet!!!");
			/* Incoming Interface is same as outgoing interface - dropping */
                         return;
                 }

		/* Route Table entries */
		//System.out.println(routeTable.toString());
		/* ARP Cache entried */
		//System.out.println(arpCache.toString());
		/* STEP 2 : Find the next hop IP Address */
		int nextHopIPAddress = rEntry.getGatewayAddress();
		if(nextHopIPAddress == 0){
			nextHopIPAddress = pkt.getDestinationAddress();
		}

		/* Find the next hop MAC address from ARP Cache */
		/* CHECK 6 : Checking non-existent Host in any network connected to Router */
		ArpEntry ae = arpCache.lookup(nextHopIPAddress);
		if(ae == null) {
			//System.out.println("Invalid IP Address : 404");
			/* No such host in the network - Dropping */
			return;
		}

		/* Next hop MAC addresses */
		MACAddress destinationMac = ae.getMac();
		/* Outgoing router Interface MAC address */
		MACAddress sourceMac = rEntry.getInterface().getMacAddress();

		//System.out.println("PKT Source MAC : " + sourceMac.toString() + "\nPKT Dest MAC : " + destinationMac.toString());

		/* STEP 3 : Update Ethernet Pakcet to send */
		etherPacket.setDestinationMACAddress(destinationMac.toString());
		etherPacket.setSourceMACAddress(sourceMac.toString());
		
		//IPv4 pktFinal = (IPv4)etherPacket.getPayload();
		//System.out.println("CS : " + pktFinal.getChecksum());
		//System.out.println("TTL : " + pktFinal.getTtl());
		
		/* Send Packet on the interface found from Route Table */
		sendPacket(etherPacket, rEntry.getInterface());

		/********************************************************************/
	}
}
