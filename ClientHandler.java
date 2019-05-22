import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientHandler extends UnicastRemoteObject implements ClientHandlerInterface {
	private static final long serialVersionUID = 1L;
	private String msg="";
	private int code=0,read=0;
	private final static String root= System.getProperty("user.dir")+"/";
	protected ClientHandler() throws RemoteException {
		super();
	}
	//Sends the client the list of files and/or directories in f
	public String[] dir(String path) {
		File f = FileSetup(path);
		String[] list=null;
		msg="";
		code=0;
		if(!f.exists()) {
			msg = f.getName() + " not exist";
			code= 1;
		}
		else if(!f.isDirectory()) {
			msg=f.getName()+" is not a directory";
			code= 1;
		}
		else {
			list =f.list();
		}
		
		return list;
	}	
	//Creates a new directory
	public void mkdir(String path) {
		File f = FileSetup(path);
		if(f.exists()) {
			msg = "Directory "+f.getName()+" exists";
			code =1;
		}
		else if(!f.mkdir()) {
			msg = "cannot create directory";
			code= 1;
		}else
			msg="Directory "+f.getName()+" is created";
	}
	//Deletes the given directory 
	public void rmdir(String path) {
		File f = FileSetup(path);
		if(!f.isDirectory()) {
			msg = f.getName() + " is not a directory";
			code =1;
		}
		else if(f.list().length>0) {
			msg = "Directory "+f.getName()+ " is not empty";
			code =1;
		}
		else if(!f.delete()) {
			msg = "Cannot Delete "+f.getName();
			code =1;
		}
		else
			msg="Directory " +f.getName()+" deleted";
	}
	//Deletes the file 
	public void rm(String path) {
		File f = FileSetup(path);
		if(!f.exists()) {
			msg = f.getName() + " does not exist";
			code= 1;
		
		}
		else if(!f.isFile()) {
			msg=f.getName()+" is not a file";
			code= 1;
		}
		else if(!f.delete()) {
			msg = "Cannot Delete "+f.getName();
			code= 1;
		}
		else
			msg ="File "+ f.getName() +" deleted";	
	}
	//Returns the file size of the given file from the path
	@Override public int getFsize(String path) {
		return (int)FileSetup(path).length(); }
	//returns the number of bytes 
	@Override public int getRead() { return read;	}
	@Override
	public int checkResend(String path,int size) {
		Boolean resend=false;
		int filePos=0;
		File f = FileSetup(path);
		if((int)f.length()<size && f.length()!=0){
			
			filePos=(int)f.length();
			resend=true;
		}
		if(resend) {
			System.out.printf("Reuploading from %.1f%%\n",((double)f.length()/size)*100);
		}
		return filePos;
	}
	//Gets the file in bytes from the client and writes to file 
	public double upload(String path,byte[] arr, int r,int size,int p) {
		File f = FileSetup(path);
		BufferedOutputStream bOS=null;
		double progress=0.0;
		try {
			f.createNewFile();
		}catch(IOException e) {}
		try {
			bOS = new BufferedOutputStream(new FileOutputStream(f,true));
		}catch(FileNotFoundException e) {System.out.println("File Not Found"); msg="File not found";code =-1;return -1;}
		
		int remaining = size-p;
		try {
			if(remaining >0) {
				//filePos += read;
				remaining -= r;
				p+=r;
				msg="";
				progress=((double)p/size)*100;
				//System.out.printf("Received ....%.1f%%\r",progress);
				bOS.write(arr, 0, r);
			}
		}catch(IOException e ) {
			System.out.println("Error sending file. Please retry.");
			msg="Error sending file. Please retry.";
			code=-1;
		}
		msg="File upload successful.";
		code=0;
		CloseFile(bOS);
		return progress;
	}
	//Sends the file in a byte array to the client
	public byte[] download(String path,int p) {
		File f = FileSetup(path);
		byte[] fileArr=new byte[10*1024];
		BufferedInputStream bIN=null;

		int size=(int) f.length(), pos=p;
		int remaining = size-pos;
		try {
			 bIN= new BufferedInputStream(new FileInputStream(f));
		}catch(FileNotFoundException e) {
			msg = "File not found";
			code =-1;
		}
		try {
			bIN.skip(pos);
			if((read=bIN.read(fileArr,0,Math.min(fileArr.length,remaining)))>0) {

				remaining -=read;
				pos += read;
				bIN.close();
				return fileArr;
			}	
		}catch(IOException e) {
			System.out.println("Error sending file. ");
			msg = "Error downloading file.";code=-1;	
		}
		return null;
	}
	
	//Shuts down the RMI server
	@Override
	public boolean shutdown() throws RemoteException {
		boolean shutdown=false;
		 try{
		        Naming.unbind(FileServer.name);
		 }
		 catch(Exception e){System.out.println(e.getMessage()); return false;}
		 
		 UnicastRemoteObject.unexportObject(this, true);
         System.out.println("FileServer exiting.");
         shutdown=true;
         return shutdown;
	}
	
	//Closes the file stream
	private void CloseFile(BufferedOutputStream bOS) {
		try {
			bOS.flush();
			bOS.close();
		}
		catch(IOException e ) {
			System.out.println("Error closing file: "+e.getMessage());
		}
	}
	//Helper function to create a new file object
	public File FileSetup(String path) {
		String serverPath="";
		File f=null;
		if(path.equals("/") || path.equals("\\"))
			serverPath=root;
		else 
			serverPath=root+path;
		if(!path.equals(" "))
			f = new File(serverPath);
		return f;	
	}
	//Sends the client the return code and message
	public String getExitStatus() {
		return msg+" "+code;
	}
}