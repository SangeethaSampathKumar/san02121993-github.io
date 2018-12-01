package edu.wisc.cs.sdn.apps.loadbalancer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.*;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.openflow.protocol.*;
import org.openflow.protocol.instruction.*;
import org.openflow.protocol.action.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wisc.cs.sdn.apps.util.ArpServer;
import edu.wisc.cs.sdn.apps.util.SwitchCommands;
import edu.wisc.cs.sdn.apps.l3routing.L3Routing;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch.PortChangeType;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.ImmutablePort;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.internal.DeviceManagerImpl;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.util.MACAddress;
import net.floodlightcontroller.devicemanager.internal.Device;
import net.floodlightcontroller.devicemanager.SwitchPort;

public class LoadBalancer implements IFloodlightModule, IOFSwitchListener,
		IOFMessageListener
{
	public static final String MODULE_NAME = LoadBalancer.class.getSimpleName();
	
	private static final byte TCP_FLAG_SYN = 0x02;
	
	private static final short IDLE_TIMEOUT = 20;
	
	// Interface to the logging system
    private static Logger log = LoggerFactory.getLogger(MODULE_NAME);
    
    // Interface to Floodlight core for interacting with connected switches
    private IFloodlightProviderService floodlightProv;
    
    // Interface to device manager service
    private IDeviceService deviceProv;
    
    // Switch table in which rules should be installed
    private byte table;
    
    // Set of virtual IPs and the load balancer instances they correspond with
    private Map<Integer,LoadBalancerInstance> instances;

    /**
     * Loads dependencies and initializes data structures.
     */
	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException 
	{
		log.info(String.format("Initializing %s...", MODULE_NAME));
		
		// Obtain table number from config
		Map<String,String> config = context.getConfigParams(this);
        this.table = Byte.parseByte(config.get("table"));
        
        // Create instances from config
        this.instances = new HashMap<Integer,LoadBalancerInstance>();
        String[] instanceConfigs = config.get("instances").split(";");
        for (String instanceConfig : instanceConfigs)
        {
        	String[] configItems = instanceConfig.split(" ");
        	if (configItems.length != 3)
        	{ 
        		log.error("Ignoring bad instance config: " + instanceConfig);
        		continue;
        	}
        	LoadBalancerInstance instance = new LoadBalancerInstance(
        			configItems[0], configItems[1], configItems[2].split(","));
            this.instances.put(instance.getVirtualIP(), instance);
            log.info("Added load balancer instance: " + instance);
        }
        
		this.floodlightProv = context.getServiceImpl(
				IFloodlightProviderService.class);
        this.deviceProv = context.getServiceImpl(IDeviceService.class);
        
        /*********************************************************************/
        /* TODO: Initialize other class variables, if necessary              */
        
        /*********************************************************************/
	}

	/**
     * Subscribes to events and performs other startup tasks.
     */
	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException 
	{
		log.info(String.format("Starting %s...", MODULE_NAME));
		this.floodlightProv.addOFSwitchListener(this);
		this.floodlightProv.addOFMessageListener(OFType.PACKET_IN, this);
		
		/*********************************************************************/
		/* TODO: Perform other tasks, if necessary                           */
		//System.out.println("List of instances");
		//System.out.println(instances);
		/*********************************************************************/
	}

	public void addDefaultRule(IOFSwitch currSwitch) {
		OFMatch match = new OFMatch();

		OFInstructionGotoTable OFGotoTable = new OFInstructionGotoTable((byte)1);
		List<OFInstruction> instr = new ArrayList<OFInstruction>();
		instr.add(OFGotoTable);
		boolean ret = SwitchCommands.installRule(currSwitch, table, (short)1, match, instr, (short)0, (short)0, OFPacketOut.BUFFER_ID_NONE);
		//System.out.println("Return Value : " + ret);
	}

	public void addRuleToSendToController(IOFSwitch currSwitch, short type) {
		OFMatch match = new OFMatch();
		match.setDataLayerType(type);
		//match.setField(OFOXMFieldType.ARP_TPA, virtualIP);

		OFActionOutput OFOut = new OFActionOutput();
		OFOut.setPort(OFPort.OFPP_CONTROLLER);

		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(OFOut);

		OFInstructionApplyActions applyAction = new OFInstructionApplyActions(actions);

		List<OFInstruction> instr = new ArrayList<OFInstruction>();
		instr.add(applyAction);

		boolean ret = SwitchCommands.installRule(currSwitch, table, (short)2, match, instr, (short)0, (short)0, OFPacketOut.BUFFER_ID_NONE);
		//System.out.println("Controller Return Value : " + ret);
	}

	public void addRuleNewTCPConnection(IOFSwitch currSwitch, int ip, byte type) {
		OFMatch match = new OFMatch();
		match.setDataLayerType((short)0x800);
		match.setNetworkProtocol((byte)type);
		match.setNetworkDestination(ip);

		OFActionOutput OFOut = new OFActionOutput();
		OFOut.setPort(OFPort.OFPP_CONTROLLER);

		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(OFOut);

		OFInstructionApplyActions action = new OFInstructionApplyActions(actions);

		List<OFInstruction> instr = new ArrayList<OFInstruction>();
		instr.add(action);

		boolean ret = SwitchCommands.installRule(currSwitch, table, (short)2, match, instr, (short)0, (short)0, OFPacketOut.BUFFER_ID_NONE);
		//System.out.println("Return Value : " + ret);
	}

	public void addConnectionSpecificRule(IOFSwitch currSwitch, int srcIP, int dstIP, byte protocol, short srcPort, short dstPort, int serverIP, byte serverMAC[],
											int virtualIP, byte[] virtualMAC) {
		OFMatch match = new OFMatch();
		match.setDataLayerType((short)0x800);
		match.setNetworkSource(srcIP);
		match.setNetworkDestination(dstIP);
		match.setNetworkProtocol(protocol);
		match.setTransportSource(srcPort);
		match.setTransportDestination(dstPort);

		OFActionSetField OFSetField1 = new OFActionSetField();
		OFSetField1.setField(new OFOXMField(OFOXMFieldType.ETH_DST, serverMAC));
		OFActionSetField OFSetField2 = new OFActionSetField();
		OFSetField2.setField(new OFOXMField(OFOXMFieldType.IPV4_DST, serverIP));

		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(OFSetField1);
		actions.add(OFSetField2);
		OFInstructionApplyActions applyActions = new OFInstructionApplyActions(actions);

		OFInstructionGotoTable OFGotoTable = new OFInstructionGotoTable(L3Routing.table);
		List<OFInstruction> instr = new ArrayList<OFInstruction>();
		instr.add(applyActions);
		instr.add(OFGotoTable);
		boolean ret = SwitchCommands.installRule(currSwitch, table, (short)3, match, instr, (short)0, (short)20, OFPacketOut.BUFFER_ID_NONE);
		//System.out.println("Return Value : " + ret);

		OFMatch reverseMatch = new OFMatch();
		reverseMatch.setDataLayerType((short)0x800);
		reverseMatch.setNetworkSource(serverIP);
		reverseMatch.setNetworkDestination(srcIP);
		reverseMatch.setNetworkProtocol(protocol);
		reverseMatch.setTransportSource(dstPort);
		reverseMatch.setTransportDestination(srcPort);

		OFActionSetField reverseOFSetField1 = new OFActionSetField();
		reverseOFSetField1.setField(new OFOXMField(OFOXMFieldType.ETH_SRC, virtualMAC));
		OFActionSetField reverseOFSetField2 = new OFActionSetField();
		reverseOFSetField2.setField(new OFOXMField(OFOXMFieldType.IPV4_SRC, virtualIP));
		List<OFAction> reverseActions = new ArrayList<OFAction>();
		reverseActions.add(reverseOFSetField1);
		reverseActions.add(reverseOFSetField2);
		OFInstructionApplyActions reverseApplyActions = new OFInstructionApplyActions(reverseActions);

		OFInstructionGotoTable reverseOFGotoTable = new OFInstructionGotoTable((byte)1);
		List<OFInstruction> reverseInstr = new ArrayList<OFInstruction>();
		reverseInstr.add(reverseApplyActions);
		reverseInstr.add(reverseOFGotoTable);

		ret = SwitchCommands.installRule(currSwitch, table, (short)3, reverseMatch, reverseInstr, (short)0, (short)20, OFPacketOut.BUFFER_ID_NONE);
		//System.out.println("Return Value : " + ret);
	}
	
	/**
     * Event handler called when a switch joins the network.
     * @param DPID for the switch
     */
	@Override
	public void switchAdded(long switchId) 
	{
		IOFSwitch sw = this.floodlightProv.getSwitch(switchId);
		log.info(String.format("Switch s%d added", switchId));
		
		/*********************************************************************/
		/* TODO: Install rules to send:                                      */
		/*       (1) packets from new connections to each virtual load       */
		/*       balancer IP to the controller                               */
		/*       (2) ARP packets to the controller, and                      */
		/*       (3) all other packets to the next rule table in the switch  */

		/* 1. Default rule to Controller for New TCP Connection */
		//System.out.println("Install Default TCP rule");
		//System.out.println("Install ARP Rule");
		for(Integer virtualIP : instances.keySet()) {
			//System.out.println("For : " + fromIPv4Address(virtualIP));
			addRuleNewTCPConnection(sw, virtualIP, (byte)0x6);
			//addRuleToSendToController(sw, virtualIP, (short)0x806);
		}

		/* 2 - Default ARP Rule */
		addRuleToSendToController(sw, (short)0x806);

		/* 3 - Default Rule - All other packets */	
		//System.out.println("Installing default rules");
		addDefaultRule(sw);
		/*********************************************************************/
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
	/**
	 * Handle incoming packets sent from switches.
	 * @param sw switch on which the packet was received
	 * @param msg message from the switch
	 * @param cntx the Floodlight context in which the message should be handled
	 * @return indication whether another module should also process the packet
	 */
	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) 
	{
		// We're only interested in packet-in messages
		if (msg.getType() != OFType.PACKET_IN)
		{ return Command.CONTINUE; }
		OFPacketIn pktIn = (OFPacketIn)msg;
		
		// Handle the packet
		Ethernet ethPkt = new Ethernet();
		ethPkt.deserialize(pktIn.getPacketData(), 0,
				pktIn.getPacketData().length);
		
		/*********************************************************************/
		/* TODO: Send an ARP reply for ARP requests for virtual IPs; for TCP */
		/*       SYNs sent to a virtual IP, select a host and install        */
		/*       connection-specific rules to rewrite IP and MAC addresses;  */
		/*       ignore all other packets                                    */
		
		/*********************************************************************/
		//System.out.println();
		//System.out.println("Recived a packet");
		//System.out.println();
		if(ethPkt.getEtherType() == 0x806) {
			ARP arpPacket = (ARP)ethPkt.getPayload();
			if(arpPacket.getOpCode() == ARP.OP_REQUEST) {
					//System.out.println("Received a ARP request");
					int reqIPAddress = IPv4.toIPv4Address(arpPacket.getTargetProtocolAddress());
					byte[] virtualMAC = new byte[6];
					for(Integer ip : instances.keySet()) {
						if(ip == reqIPAddress) {
							virtualMAC = instances.get(ip).getVirtualMAC();
							break;
						}
					}
					int port = pktIn.getInPort();
					//System.out.println("Sending ARP Reply");
					sendARPReply(ethPkt, arpPacket, sw, (short)port, virtualMAC, reqIPAddress);
			}
		}
		else if(ethPkt.getEtherType() == Ethernet.TYPE_IPv4) {
			//System.out.println(ethPkt);
			IPv4 packet = (IPv4)ethPkt.getPayload();
			if(packet.getProtocol() == IPv4.PROTOCOL_TCP) {
				TCP tcpPacket = (TCP)packet.getPayload();
				short flags = tcpPacket.getFlags();
				if(flags == 0x02) {
						//System.out.println("TCP SYN Packet");
						MACAddress srcMAC = ethPkt.getSourceMAC();
						long sourceMAC = srcMAC.toLong();
						MACAddress dstMAC = ethPkt.getDestinationMAC();
						long destinationMAC = dstMAC.toLong();

						int srcIP = packet.getSourceAddress();
						int dstIP = packet.getDestinationAddress();
						byte protocol = packet.getProtocol();

						short srcPort = tcpPacket.getSourcePort();
						short dstPort = tcpPacket.getDestinationPort();

						LoadBalancerInstance inst = instances.get(dstIP);
						byte[] virtualMAC = inst.getVirtualMAC();
						int serverIP = inst.getNextHostIP();
						System.out.println("Adding connection specific rules");
						addConnectionSpecificRule(sw, srcIP, dstIP, protocol, srcPort, dstPort, serverIP, getHostMACAddress(serverIP), dstIP, virtualMAC);
				}
			}
		}
		
		// We don't care about other packets
		return Command.CONTINUE;
	}

	/* ARP Reply */

	public void sendARPReply(Ethernet inEtherPkt, ARP inArpPkt, IOFSwitch sw, short port, byte[] virtualMAC, int ip) {
		Ethernet ether = new Ethernet();
		ARP arpPkt = new ARP();

		/* Construct Ethernet header */
		ether.setEtherType(Ethernet.TYPE_ARP);
		ether.setSourceMACAddress(virtualMAC);
		ether.setDestinationMACAddress(inEtherPkt.getSourceMACAddress());

		/* ARP Header */
		arpPkt.setHardwareType(ARP.HW_TYPE_ETHERNET);
		arpPkt.setProtocolType(ARP.PROTO_TYPE_IP);
		arpPkt.setHardwareAddressLength((byte)Ethernet.DATALAYER_ADDRESS_LENGTH);
		arpPkt.setProtocolAddressLength((byte)4);
		arpPkt.setOpCode(ARP.OP_REPLY);
		arpPkt.setSenderHardwareAddress(virtualMAC);
		arpPkt.setSenderProtocolAddress(ip);
		arpPkt.setTargetHardwareAddress(inArpPkt.getSenderHardwareAddress());
		arpPkt.setTargetProtocolAddress(inArpPkt.getSenderProtocolAddress());

		/* Set Ethernet Payload */
		ether.setPayload(arpPkt);
		//System.out.println("--------------- PACKET --------------");
		//System.out.println(arpPkt);
		//System.out.println(ether);
		//System.out.println("Port : " + port);
		//System.out.println("--------------- PACKET --------------");
		/* Send ARP Reply */
		boolean b = SwitchCommands.sendPacket(sw, port, ether);
		//System.out.println("RET : " + b);
	}

	/**
	 * Returns the MAC address for a host, given the host's IP address.
	 * @param hostIPAddress the host's IP address
	 * @return the hosts's MAC address, null if unknown
	 */
	private byte[] getHostMACAddress(int hostIPAddress)
	{
		Iterator<? extends IDevice> iterator = this.deviceProv.queryDevices(
				null, null, hostIPAddress, null, null);
		if (!iterator.hasNext())
		{ return null; }
		IDevice device = iterator.next();
		return MACAddress.valueOf(device.getMACAddress()).toBytes();
	}

	/**
	 * Event handler called when a switch leaves the network.
	 * @param DPID for the switch
	 */
	@Override
	public void switchRemoved(long switchId) 
	{ /* Nothing we need to do, since the switch is no longer active */ }

	/**
	 * Event handler called when the controller becomes the master for a switch.
	 * @param DPID for the switch
	 */
	@Override
	public void switchActivated(long switchId)
	{ /* Nothing we need to do, since we're not switching controller roles */ }

	/**
	 * Event handler called when a port on a switch goes up or down, or is
	 * added or removed.
	 * @param DPID for the switch
	 * @param port the port on the switch whose status changed
	 * @param type the type of status change (up, down, add, remove)
	 */
	@Override
	public void switchPortChanged(long switchId, ImmutablePort port,
			PortChangeType type) 
	{ /* Nothing we need to do, since load balancer rules are port-agnostic */}

	/**
	 * Event handler called when some attribute of a switch changes.
	 * @param DPID for the switch
	 */
	@Override
	public void switchChanged(long switchId) 
	{ /* Nothing we need to do */ }
	
    /**
     * Tell the module system which services we provide.
     */
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() 
	{ return null; }

	/**
     * Tell the module system which services we implement.
     */
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> 
			getServiceImpls() 
	{ return null; }

	/**
     * Tell the module system which modules we depend on.
     */
	@Override
	public Collection<Class<? extends IFloodlightService>> 
			getModuleDependencies() 
	{
		Collection<Class<? extends IFloodlightService >> floodlightService =
	            new ArrayList<Class<? extends IFloodlightService>>();
        floodlightService.add(IFloodlightProviderService.class);
        floodlightService.add(IDeviceService.class);
        return floodlightService;
	}

	/**
	 * Gets a name for this module.
	 * @return name for this module
	 */
	@Override
	public String getName() 
	{ return MODULE_NAME; }

	/**
	 * Check if events must be passed to another module before this module is
	 * notified of the event.
	 */
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) 
	{
		return (OFType.PACKET_IN == type 
				&& (name.equals(ArpServer.MODULE_NAME) 
					|| name.equals(DeviceManagerImpl.MODULE_NAME))); 
	}

	/**
	 * Check if events must be passed to another module after this module has
	 * been notified of the event.
	 */
	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) 
	{ return false; }
}
