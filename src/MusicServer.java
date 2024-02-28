import java.io.*;
import java.net.*;
import java.util.*;

public class MusicServer {
    ArrayList<ObjectOutputStream> clientOutputStreams;

    public static void main(String[] args) {
        new MusicServer().go();
    }

    public class ClientHandler implements Runnable{
        ObjectInputStream in;
        Socket clinetSocket;

        public ClientHandler(Socket socket){
            try {
                clinetSocket = socket;
                in = new ObjectInputStream(clinetSocket.getInputStream());
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            Object o2 = null;
            Object o1 = null;

            try{
                while ((o1 = in.readObject()) != null){
                    o2 = in.readObject();

                    System.out.println("read two objects");
                    tellEveryone(o1,o2);
                }
            } catch (Exception el){
                el.printStackTrace();
            }
        }
    }

    public void tellEveryone(Object first, Object two){
        Iterator it = clientOutputStreams.iterator();
        while(it.hasNext()){
            try{
                ObjectOutputStream out = (ObjectOutputStream) it.next();
                out.writeObject(first);
                out.writeObject(two);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    public void go(){
        clientOutputStreams = new ArrayList<ObjectOutputStream>();

        try{
            ServerSocket serverSock = new ServerSocket(4242);
            while(true){
                Socket clientSocket = serverSock.accept();
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                clientOutputStreams.add(out);

                Thread thread = new Thread(new ClientHandler(clientSocket));
                thread.start();
                System.out.println("got a connection");
            }
        }catch (Exception ad){
            ad.printStackTrace();
        }
    }
}
