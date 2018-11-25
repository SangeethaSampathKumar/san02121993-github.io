package edu.wisc.cs.sdn.apps.l3routing;

import java.util.*;
import edu.wisc.cs.sdn.apps.l3routing.L3Routing;
import net.floodlightcontroller.core.IOFSwitch;
import edu.wisc.cs.sdn.apps.util.Host;
import net.floodlightcontroller.routing.Link;

public class NetworkGraph {

	class Edge {
		String src, dst;
		int srcPort, dstPort;
		int weight;

		Edge(String src, String dst, int srcPort, int dstPort) {
			this.src = src;
			this.dst = dst;
			this.weight = 1;
			this.srcPort = srcPort;
			this.dstPort = dstPort;
		}
	}

	int V, E;
	List<Edge> edges;
	List<String> vertices;

	NetworkGraph(int V, int E) {
		this.V = V;
		this.E = E;
		this.edges = new ArrayList<Edge>(E);
		this.vertices = new ArrayList<String>(V);
	}

	public void addVertex(String vertexName) {
		for(String vertex : vertices) {
			if(vertex.equals(vertexName)) {
				System.out.println(vertexName + " already in graph");
				return;
			}
		}

		this.V++;
		this.vertices.add(vertexName);
	}

	public void addEdge(String u, String v, int srcPort, int dstPort) {
		for(Edge e : edges) {
			if(e.src.equals(u) && e.dst.equals(v)) {
				System.out.println("Duplicate edge");
				return;
			}
		}

		this.E++;
		this.edges.add(new Edge(u, v, srcPort, dstPort));
	}

	public void removeEdge(String u, String v) {
		Iterator itr = edges.iterator();
		while(itr.hasNext()) {
			Edge e = (Edge)itr.next();
			if(e.src.equals(u) && e.dst.equals(v)) {
				itr.remove();
				this.E--;
				return;
			}
		}
	}

	public void removeVertex(String vertexName) {
		Iterator itr = vertices.iterator();
		while(itr.hasNext()) {
			String v = (String)itr.next();
			if(v.equals(vertexName)) {
				itr.remove();
				this.V--;
				return;
			}
		}
	}

	public HashMap<String, HashMap<String, String>> runBellmanFord() {
		// Generate shortest path between vertices(switches) in the network topology graph
		HashMap<String, HashMap<String, String>> shortestPathTable = new HashMap<String, HashMap<String, String>>();
		// If no vertices(switches) in the network, return empty shortestPathTablele
		if(this.V == 0) {
			System.out.println("No vertices in graph");
			return shortestPathTable;
		}
		for(String vertex: this.vertices) {
			System.out.println("For, " + vertex);
			HashMap<String, String> shortestPathTablele = bellmanFord(this, vertex);
			shortestPathTable.put(vertex, shortestPathTablele);
		}

		return shortestPathTable;
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

	public void printGraph() {
		System.out.println("Graph");
		System.out.println("---------------------");
		System.out.println("Vertex : " + this.V);
		System.out.println(this.vertices);
		System.out.println("Edges : " + this.E);
		System.out.println("---------------------");
		for(Edge e : this.edges) {
			System.out.println(e.src + "-->" + e.dst + " : " + e.weight) ;
			System.out.println(e.srcPort + "-->" + e.dstPort);
		}
		System.out.println("---------------------");
	}

	public void addNewEdges() {
		/*
		edges.add(0, new Edge("s1", "s2")); 
		edges.add(1, new Edge("s2", "s1"));
		edges.add(2, new Edge("s2", "s3")); 
		edges.add(3, new Edge("s3", "s2")); 
		*/

		/*
		edges.add(new Edge("s1", "s2"));
		edges.add(new Edge("s2", "s1"));
		edges.add(new Edge("s2", "s3"));
		edges.add(new Edge("s3", "s2"));
		edges.add(new Edge("s1", "s4"));
		edges.add(new Edge("s4", "s1"));
		edges.add(new Edge("s3", "s4"));
		edges.add(new Edge("s4", "s3"));
		edges.add(new Edge("s3", "s5"));
		edges.add(new Edge("s5", "s3"));
		edges.add(new Edge("s4", "s5"));
		edges.add(new Edge("s5", "s4"));
		*/
		/*
		edges.add(new Edge("s1", "s2"));
		edges.add(new Edge("s2", "s1"));
		edges.add(new Edge("s2", "s3"));
		edges.add(new Edge("s3", "s2"));
		edges.add(new Edge("s2", "s5"));
		edges.add(new Edge("s5", "s2"));
		edges.add(new Edge("s3", "s5"));
		edges.add(new Edge("s5", "s3"));
		edges.add(new Edge("s3", "s4"));
		edges.add(new Edge("s4", "s3"));
		edges.add(new Edge("s4", "s7"));
		edges.add(new Edge("s7", "s4"));
		edges.add(new Edge("s4", "s6"));
		edges.add(new Edge("s6", "s4"));
		edges.add(new Edge("s5", "s6"));
		edges.add(new Edge("s6", "s5"));*/
	}

	void addVertices(String vertices[]) {
		for(int i = 0; i < vertices.length; i++) {
			this.vertices.add(vertices[i]);
		}
	}

	HashMap<String, String> bellmanFord(NetworkGraph g, String vertex) {
		HashMap<String, Integer> distance = new HashMap<String, Integer>(g.V);
		HashMap<String, List<String>> path = new HashMap<String, List<String>>(g.V);

		for(int i = 0; i < g.V; i++) {
			distance.put(vertices.get(i), Integer.MAX_VALUE);
			path.put(vertices.get(i), new ArrayList<String>());
		}

		int i, j;
		distance.put(vertex, 0);
		List<String> currPath = new ArrayList<String>();
		currPath.add(vertex);
		path.put(vertex, currPath);
		for(i = 1; i < g.V; i++) {
			for(j = 0; j < g.E; j++) {
				String src = edges.get(j).src;
				String dst = edges.get(j).dst;
				if(distance.get(src) != Integer.MAX_VALUE && distance.get(src) + edges.get(j).weight < distance.get(dst)) {
					distance.put(dst, distance.get(src) + edges.get(j).weight);
					List<String> newPath = new ArrayList<String>(path.get(src));
					newPath.add(dst);
					path.put(dst, newPath);
				}
			}
		}

		System.out.println(distance);
		System.out.println(path);

		System.out.println();
		HashMap<String, String> table = new HashMap<String, String>();

		for(Map.Entry<String, List<String>> entry: path.entrySet()) {
			List<String> myPath = entry.getValue();
			String nextHop = "";
			if(myPath.size() == 1)
				nextHop = myPath.get(0);
			else if(myPath.size() == 2)
				nextHop = myPath.get(1);
			else if(myPath.size() > 2) 
				nextHop = myPath.get(1);
			if(vertex.equals(nextHop))
				continue;
			table.put(entry.getKey(), String.valueOf(getOutPort(vertex, nextHop)));
			System.out.println(entry.getKey() + " : " + nextHop + " : " + getOutPort(vertex, nextHop));
		}
		System.out.println();

		return table;
	}

	public int getOutPort(String u, String v) {
		for(Edge e : edges) {
			if(e.src.equals(u) && e.dst.equals(v))
				return e.srcPort;
		}
		System.out.println(u + "-->" + v);
		System.out.println("Invalid case!");
		return 0;
	}

	public void addNewVertex(String vertexName) {} 
/*
	public static void main(String args[]) {
		Graph g = new Graph(7, 16);
		String vertices[] = {"s1", "s2", "s3", "s4", "s5", "s6", "s7"};
		//String vertices[] = {"s1", "s2", "s3", "s4", "s5"};
		//String vertices[] = {"s1", "s2", "s3"};
		g.addVertices(vertices);
		g.addNewEdges();
		g.printGraph();

		for(int i = 0; i < vertices.length; i++) {
			System.out.println("----------------------------------");
			System.out.println("For : " + vertices[i]);
			System.out.println("----------------------------------");
			g.bellmanFord(g, vertices[i]);
			System.out.println("----------------------------------");
		}
	}
*/
}
