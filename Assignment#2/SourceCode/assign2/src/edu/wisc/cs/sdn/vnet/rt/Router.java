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

		if(etherPacket.getEtherType() != 0x800) {
			System.out.println("Not IPv4");
			return;
		}
		System.out.println("My packet");
		System.out.println(etherPacket.toString());
		System.out.println("My packet");
		IPv4 pkt = (IPv4)etherPacket.getPayload();
	
		short pktChecksum = pkt.getChecksum();
		System.out.println("From sender : " + pktChecksum);

		pkt.resetChecksum();
		System.out.println("At Reset : " + pkt.getChecksum());
		pkt.serialize();
		short checksumAtReciever = pkt.getChecksum();
		System.out.println("At Reciever : " + checksumAtReciever);

		if(pktChecksum != checksumAtReciever) {
			System.out.println("Checksum mismatch");
			return;
		}

		byte currentTTL = pkt.getTtl();
		currentTTL--;
		System.out.println("TTL : " + currentTTL);
		if(currentTTL == 0) {
			System.out.println("TTL 0");
			return;
		}
		System.out.println(pkt.getTtl());
		pkt.setTtl(currentTTL);
		System.out.println(pkt.getTtl());
		pkt.resetChecksum();
		System.out.println("checksum: " + pkt.getChecksum());
		pkt.serialize();
		System.out.println("checksum22: " + pkt.getChecksum());
	
      		System.out.println(pkt.getDestinationAddress());
		for(Map.Entry<String, Iface> entry: interfaces.entrySet()){
			System.out.println(entry.getKey() + " " + entry.getValue().getIpAddress());
			if(pkt.getDestinationAddress()  == entry.getValue().getIpAddress()){
				System.out.println("Matching Router IP - dropping !");
				return;
			}
		}
	
		RouteEntry rEntry = routeTable.lookup(pkt.getDestinationAddress());
		if(rEntry == null) {
			System.out.println("No matching interface found - dropping!");
			return;
		}

		/* Route Table entries */
		System.out.println(routeTable.toString());

		/* ARP Cache entried */
		System.out.println(arpCache.toString());

		/* Find next hop MAC address from ARP Cache */
		MACAddress destinationMac = arpCache.lookup(pkt.getDestinationAddress()).getMac();
		/* Find the source MAC of router interface */
		MACAddress sourceMac = rEntry.getInterface().getMacAddress();
		System.out.println("source: " + sourceMac.toString() + " desti:" + destinationMac.toString());

		Ethernet newFrame = new Ethernet();
		newFrame.setDestinationMACAddress(destinationMac.toString());
		newFrame.setSourceMACAddress(sourceMac.toString());
		newFrame.setEtherType((short)0x800);
		newFrame.setPayload(pkt);

		System.out.println(newFrame.toString());
		sendPacket(newFrame, rEntry.getInterface());
		/********************************************************************/
	}
}
