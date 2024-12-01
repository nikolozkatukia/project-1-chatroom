import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ChatRoom {
    private static final ConcurrentHashMap<String, ObjectOutputStream> clients=new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("გთხოვთ შეიყვანოთ პორტი რომელზეც სერვერი გაეშვება:");
        try(BufferedReader consoleReader=new BufferedReader(new InputStreamReader(System.in))){
            int port=Integer.parseInt(consoleReader.readLine());
            ServerSocket serverSocket=new ServerSocket(port);
            System.out.println("სერვერი ჩართულია პორტზე: "+port);

            while(true){
                Socket clientSocket=serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private String clientName="anonym";
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public ClientHandler (Socket socket) {
            this.socket=socket;
        }

        @Override
        public void run() {
            try {
                out=new ObjectOutputStream(socket.getOutputStream());
                in=new ObjectInputStream(socket.getInputStream());

                synchronized(clients){
                    clients.put(clientName,out);
                    broadcast("მომხმარებელი "+clientName+" შემოვიდა ჩათში ჯამში მომხმარებლების რაოდენობაა: "+clients.size());
                }

                out.writeObject("მოგესალმებით ჩათში");
                out.flush();

                String message;
                while((message=(String)in.readObject())!=null) {
                    handleMessage(message);
                }
            }catch(IOException | ClassNotFoundException e){
                System.err.println("კავშირის პრობლემა: "+clientName);
            }finally{
                disconnectClient();
            }
        }

        private void handleMessage(String message) throws IOException{
            if(message.startsWith("/name ")){
                String newName=message.substring(6).trim();
                if(!newName.isEmpty()){
                    synchronized (clients){
                        clients.remove(clientName);
                        clientName=newName;
                        clients.put(clientName,out);
                    }
                    broadcast("მომხმარებელმა შეცვალა სახელი: "+clientName);
                }
            }else if(message.startsWith("/private ")) {
                String[] parts=message.split(" ",3);
                if(parts.length==3){
                    sendPrivateMessage(parts[1],parts[2]);
                }
            }else if(message.equalsIgnoreCase("/leave chat")) {
                disconnectClient();
            }else {
                broadcast(clientName+": "+message);
            }
        }

        private void broadcast(String message){
            synchronized(clients) {
                clients.forEach((name,clientOut)->{
                    try{
                        clientOut.writeObject(message);
                        clientOut.flush();
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }



        private void sendPrivateMessage(String targetName,String privateMessage)throws IOException{
            ObjectOutputStream targetOut = clients.get(targetName);
            if(targetOut !=null){
                targetOut.writeObject("[პირადი] "+clientName+": "+privateMessage);
                targetOut.flush();
            }else{
                out.writeObject("მომხმარებელი "+targetName+" არ მოიძებნა.");
                out.flush();
            }
        }

        private void disconnectClient(){
            try{
                synchronized (clients){
                    clients.remove(clientName);
                    broadcast("მომხმარებელი "+clientName+" გავიდა ჩათიდან ჩათში დარჩენილია: "+clients.size()+"მომხმარებელი ");
                }
                socket.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
