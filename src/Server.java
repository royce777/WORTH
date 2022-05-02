import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {

    private static ThreadPoolExecutor executor;
    private static LinkedBlockingQueue<Runnable> q;
    
    public static void main(String[] args) {
        System.out.println("Server starting...");
		UsersManager uManager = null;
		ProjectsManager pManager = null;
        try{
			uManager = new UsersManager();
			pManager = new ProjectsManager(uManager);
        } catch(IOException e){
			System.out.println("WORTH: Fatal error -> Could not setup persistency system");
			System.exit(0);
        }
        try{
            //export remote obj
            RegInterface stub = (RegInterface) UnicastRemoteObject.exportObject(uManager, 0);
            //create registry on port 6789
			Registry r = LocateRegistry.createRegistry(6789);
            //rebind
			r.rebind("USER-REG", stub);
        } catch (RemoteException e) {
            System.out.println("RMI procedure error");
            e.printStackTrace();
        }
        q = new LinkedBlockingQueue<Runnable>();
		// max idling 32sec
		executor = new ThreadPoolExecutor(50, 1000, 320000, TimeUnit.MILLISECONDS, q);
		System.out.println("WORTHServer Ready!");
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(6790);
			while(true){
				Socket socket = serverSocket.accept();
				// create new thread to manage client requests
				ClientTask t = new ClientTask(uManager,pManager,socket);
				executor.execute(t);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
