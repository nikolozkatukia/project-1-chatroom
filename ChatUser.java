import java.io.*;
import java.net.*;

public class ChatUser {
    public static void main(String[] args) {
        System.out.println("გთხოვთ შეიყვანოთ სერვერის მისამართი და პორტი :");
        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            String[] serverInfo=consoleReader.readLine().split(" ");
            String serverAddress=serverInfo[0];
            int port=Integer.parseInt(serverInfo[1]);

            while(true){
                try(Socket socket=new Socket(serverAddress,port);
                    ObjectOutputStream out=new ObjectOutputStream(socket.getOutputStream());
                    ObjectInputStream in=new ObjectInputStream(socket.getInputStream())){

                    System.out.println("დაკავშირებულია სერვერთან");

                    new Thread(()->{
                        try{
                            String serverMessage;
                            while((serverMessage=(String) in.readObject()) != null) {
                                System.out.println(serverMessage);
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            System.err.println("სერვერთან კავშირი გაწყვეტილია");
                        }
                    }).start();

                    String userInput;
                    while((userInput=consoleReader.readLine()) !=null) {
                        out.writeObject(userInput);
                        out.flush();
                    }
                }catch(IOException e){
                    System.err.println("დაკავშირება ვერ მოხერხდა ");
                    Thread.sleep(2000);
                }
            }
        }catch(IOException | InterruptedException e){
            e.printStackTrace();
        }
    }
}
