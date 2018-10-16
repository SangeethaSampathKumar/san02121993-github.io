package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.MACAddress;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import java.util.*;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{
	ForwardingTable ft;
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
		ft = new ForwardingTable();
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
		/* Learing */
		System.out.println("source mac:" + etherPacket.getSourceMAC());
		ft.learnForwarding(etherPacket.getSourceMAC(), inIface);

		/* Forwarding */
		System.out.println("dest mac:" + etherPacket.getDestinationMAC());
		Iface outIface = ft.getIFaceForMAC(etherPacket.getDestinationMAC());
		if(outIface == null) {
			System.out.println("Broadcast packet");
			for(Map.Entry<String,Iface> entry: interfaces.entrySet()) {
				if(entry.getKey().equals(inIface.getName())) {
					System.out.println("Input interface");
				} else {
					System.out.println("Send packet : " + entry.getKey());
					sendPacket(etherPacket, entry.getValue());
				}
				//System.out.println(entry.getKey() + " " + entry.getValue());
			}
		} else {
			System.out.println("Send packet");
			sendPacket(etherPacket, outIface);
		}
		System.out.println(ft);
		/********************************************************************/
	}
}
