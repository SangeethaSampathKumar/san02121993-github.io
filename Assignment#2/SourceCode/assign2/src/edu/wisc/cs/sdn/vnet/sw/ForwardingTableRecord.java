package edu.wisc.cs.sdn.vnet.sw;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.MACAddress;

class ForwardingTableRecord {
    
	MACAddress inputMAC;
	Iface inIface;
	int timeOut;
	long startTime;

	ForwardingTableRecord(MACAddress inputMAC, Iface inIface){
		this.inputMAC = inputMAC;
		this.inIface = inIface;
		this.timeOut = 15;
		this.startTime = System.nanoTime();
	}
}
