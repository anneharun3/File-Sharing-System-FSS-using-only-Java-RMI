/**Sets up the server
 * @author Harun Anne */
import java.rmi.registry.LocateRegistry;
import java.rmi.Naming;

public class FileServer {
	private int portNo=0;
	static String name;	
//Main File
	public static void main(String[] args) {
		String cmd="";
		int port=0;
		if(args.length==2) {
			cmd=args[0];
			port = Integer.parseInt(args[1]);
			if(cmd.equals("start")) {		
				FileServer f = new FileServer(port);
				f.runServer();
			}
		}
		else {
			System.out.println("Usage: java FileServer start <portnumber>");
		}
	}
	public FileServer(int p) {
	 portNo=p;
	 name="rmi://0.0.0.0:"+portNo+"/FileServer";
	}
//runs the server
	public void runServer() {
		try {
			LocateRegistry.createRegistry(portNo);
			Naming.rebind(name,	new ClientHandler());
			System.err.println("Server ready");
		}catch(Exception e) {
			System.err.println("Server Exception: "+e.toString());;
			e.printStackTrace();
		}
	}
}