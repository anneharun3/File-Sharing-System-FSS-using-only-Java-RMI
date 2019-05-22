/**Connects to the FileServer
Client File
 * @author Harun Anne */
import java.io.*;
import java.rmi.*;
import java.util.Arrays;
public class Client {
	private BufferedInputStream  Bin;
	private String env = System.getenv("PA1_SERVER");
	private File f;
	private byte[] fileArr;
	private int Fsize,code,filePos=0;
	private ClientHandlerInterface server;
	private boolean resend=false, isShutdown=false;
	private BufferedOutputStream Bos;
	private FileOutputStream fileOS;
	private String cmd,serverPath= "",clientPath="",msg="";
	public Client(String c, String p1,String p2) {
		this.cmd=c.toLowerCase();
		if(cmd.equals("upload")) {
			this.clientPath=System.getProperty("user.dir")+"/"+p1;
			this.serverPath=p2;
		}else {
			this.serverPath=p1;
			this.clientPath=System.getProperty("user.dir")+"/"+p2;
		}
	}
	public Client(String c,String p1) {
		this.cmd=c;
		this.serverPath=p1;	
	}
	public Client(String c) {
		this.cmd=c;
	}
	public void runClient() {
		String[] list;
		String message="";
		Connect();
		try {
			switch(cmd) {
				case "dir":
					list=server.dir(serverPath);
					if(list==null) {
						msg="Error listing directories";
						code =1;
					}else {
						Arrays.stream(list).forEach((String s) ->System.out.println(s));
					}
					break;
				case "upload":
					Upload();break;
				case "download":
					Download();break;
				case "shutdown":
					shutdown();
					break;
				case "mkdir":
					server.mkdir(serverPath);break;
				case "rmdir":
					server.rmdir(serverPath);break;
				case "rm":
					server.rm(serverPath);break;
				default:
					Close(1,"Command incorrect. Please retry.");
			}
			message = server.getExitStatus();
			msg = message.replaceAll("-*?\\d$","");
			code=Integer.parseInt(message.substring(message.length()-2).trim());
		}catch(RemoteException e) {
			if(isShutdown) {
				msg="server offline";
				code=-1;
			}
			else
				System.out.println("Error: "+e.getMessage());
		}
		Close(code,msg);
	}
	//Connects the client to the server
	private void Connect() {
		String name = "rmi://"+ env+"/FileServer";
		try {
			server = (ClientHandlerInterface)Naming.lookup(name);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(-1);
		}
	}
	public void Upload() {
		int read=0,pos=0,remaining=0;
		f = new File(clientPath);  
		Fsize = (int)f.length();
		fileArr = new byte[10*1024];

		try {
			Bin = new BufferedInputStream(new FileInputStream(f));
		}catch(FileNotFoundException e) {
			Close(-1,"File not found"); 
		}
		try {
			filePos = server.checkResend(serverPath,Fsize);
			pos=filePos;
			remaining = Fsize-filePos;
			Bin.skip(pos);
			while((read=Bin.read(fileArr,0,Math.min(fileArr.length,remaining)))>0) {
				double progress=server.upload(serverPath,fileArr, read,Fsize,pos);
				pos += read;
				remaining -=read;
				if(progress==-1)
					break;
				System.out.printf("Uploading ....%.1f%%\r",progress);
			}

		}catch(IOException e) {
			if(isShutdown) {
				Close(-1,"server offline");
			}
			else
				Close(-1,"Error sending file");
		}
	}
	//download process
	public void Download() {
		try {
			Fsize = server.getFsize(serverPath);
		}catch(RemoteException e) {e.printStackTrace();}

		fileArr=new byte[10*1024];
		f=new File(clientPath);
		
		if(f.exists() &&(int)f.length()==Fsize) {
			Close(1, "File already exists!");
		}
		else if(Fsize==0) {
			Close(-1,"File not found");
		}
		try {
			if((int)f.length()<Fsize && f.length()!=0){
				fileOS = new FileOutputStream(f,true);
				filePos=(int)f.length();
				resend=true;
			}
			else {
				fileOS =new FileOutputStream(f);
			}

		} catch (FileNotFoundException e) {
			Close(-1,"File not found");
		} 
		if(resend) {
			System.out.printf("Redownloading from %.1f%%\n",((double)filePos/Fsize)*100);
		}
		Bos= new BufferedOutputStream(fileOS);

		int pos=filePos,read=0, remaining = Fsize-filePos;
		while(remaining>0) {
			try {
				fileArr=server.download(serverPath,pos);
				read=server.getRead();
				pos += read;
				remaining -= read;
				System.out.printf("Downloading....%.1f%%\r",((double)pos/Fsize)*100);
				Bos.write(fileArr, 0, read);
			}catch(IOException e) {
				if(isShutdown)
					Close(-1,"Server offline");
				else
					Close(-1,"Error downloading file.Try again");
			}
		}
		CloseFile();
		Close(0,"File Downloaded");
	}		
	//Calls the remote method to shutdown the server
	public void shutdown() {	
		try {
			isShutdown=server.shutdown();
		}catch(RemoteException e) {
			Close(-1,"Cannot shutdown server");
		}
		if(isShutdown)
			Close(0,"Shutting down server.");
		else
			Close(-1,"Cannot shutdown server");
	}
	//Closes the file stream
	private void CloseFile() {
		try {
			Bos.flush();
			Bos.close();
		}
		catch(IOException e ) {
			System.out.println("Error closing file: "+e.getMessage());
		}
	}
	//Sends message to the user and calls system exit with the code
	private void Close(int code, String message) {
		if(!message.equals(" ")) 
			System.out.println("\n"+message);
		System.exit(code);
	}
	public static void main(String[] args) {
		String path1="",path2="",cmd="";
		cmd=args[0];
		Client c=null;

		if(args.length==3) {
			path1=args[1];
			path2=args[2];
			c = new Client(cmd,path1,path2);
		}
		else if(args.length==2) {
			path1=args[1];
			c = new Client(cmd,path1);
		}
		else if(args.length==1) {
			c=new Client(cmd);
		}
		else {
			System.out.println("Usage:java Client <command> [<path1>] [<path2>]");
			System.exit(1);
		}
		c.runClient();
	}
}