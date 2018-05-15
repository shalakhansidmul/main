package fcn.project.chord.node;

public class NodeFactory {
	private static NodeFactory nf = null;
	
	private NodeFactory(){
		
	}
	public  static  NodeFactory getInstance(){
		
		if (nf == null){
			nf = new NodeFactory();
		}
		return nf;
	}
	
	public ChordNode getNode(String port, String ip){
		
		int portNumber = Integer.parseInt(port);
		ChordNode c = new ChordNode(portNumber, ip);
		return c;
	}
}
