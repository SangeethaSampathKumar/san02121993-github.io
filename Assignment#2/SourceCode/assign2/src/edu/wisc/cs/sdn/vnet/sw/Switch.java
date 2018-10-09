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
class ForwardingTable{
    List<ForwardingTableRecord> fTable;
    ForwardingTable(){
        fTable  = new ArrayList<ForwardingTableRecord>();
    }
    
    public void learnForwarding(MACAddress input, Iface intf){
	System.out.println("Learning");

	if(fTable.size() == 0) {
		ForwardingTableRecord r = new ForwardingTableRecord(input, intf);
		fTable.add(r);
	} else {
		for(ForwardingTableRecord record: this.fTable){
			if(record.inputMAC.equals(input)){
				System.out.println("Found Entry");
				return;
			}
		}
		ForwardingTableRecord r = new ForwardingTableRecord(input, intf);
		fTable.add(r);
	}
    }

	public Iface getIFaceForMAC(MACAddress inputMAC) {
		for(ForwardingTableRecord r:fTable) {
			if(r.inputMAC.equals(inputMAC)) {
				return r.inIface;
			}
		}
		return null;
	}
}
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
		for(ForwardingTableRecord r:ft.fTable) {
			System.out.println(r.inputMAC + " " + r.inIface + " " + r.timeOut);
		}

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
		/********************************************************************/
	}
}