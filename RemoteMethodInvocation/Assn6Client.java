import java.rmi.*;
     
public class Assn6Client {
    public static void main (String[] args) {
        MethodInterface hello;
        try {
            String rmi= args[0];
            hello = (MethodInterface)Naming.lookup(rmi);
            
            int result = 0;
            if(args[1].contentEquals("factorial")){
                int input = Integer.parseInt(args[2]);
                result=hello.factorial(input);
                System.out.println("Result is: "+ result);
            }
            else if(args[1].contentEquals("fibonacci")){
                int input = Integer.parseInt(args[2]);
                result=hello.fibonacci(input);
                System.out.println("Result is: "+ result);
            } 
            else{
                System.out.println(args[1] + " not a valid method.");
            }
            }catch (Exception e) {
                System.out.println("HelloClient exception: " + e);
                }
        }
}