import java.rmi.*;
import java.rmi.server.*;

public class Assn6Server{
    public static void main(String[] argv){
        try{
            System.setProperty("java.rmi.server.hostname", argv[0]);
            MethodInterface method = new Method();
            Naming.rebind("rmi://localhost:1099/cecs327", method);
            String ip = "rmi://"+ argv[0] + "/cecs327";
            System.out.println(" The server is ready... ");
            System.out.println(" Use " + ip);
        }
        catch(Exception e) {
            System.out.println("Method Server failed: " + e);
        }

    }
}