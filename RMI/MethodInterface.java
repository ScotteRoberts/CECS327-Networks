import java.rmi.*;

public interface MethodInterface extends Remote {
	public int fibonacci(int n) throws RemoteException;
	public int factorial(int n) throws RemoteException;
}