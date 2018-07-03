package Director;

import FTP.LoadInfo;
import Zookeeper.ZKUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class Director implements Runnable{
    String zkServers,rootNode;
    public ZKUtil zkUtil;

    public Director(String zkServers,String rootNode){
        this.zkServers=zkServers;
        this.rootNode=rootNode;
        String addr="";
        try{
            addr= InetAddress.getLocalHost().getHostAddress();
        }catch(UnknownHostException e){
            e.printStackTrace();
        }
        zkUtil=new ZKUtil(zkServers,rootNode,addr,false);
    }

    public String responseServer(){
        String server="";
        synchronized (zkUtil.validServer){
            zkUtil.no%=zkUtil.validServer.size();
            server=zkUtil.validServer.get(zkUtil.no);
            zkUtil.no++;
        }
        return server;
    }

    public void run(){
        zkUtil.regServer(null);
        ServerSocket server=null;
        try{
            server=new ServerSocket(9529);
        }catch(IOException e){
            e.printStackTrace();
        }
        while(true){
            Socket socket=null;
            try{
                socket=server.accept();
                new Thread(new SocketDealer(socket)).start();
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public class SocketDealer implements Runnable{
        Socket socket;
        SocketDealer(Socket socket){
            this.socket=socket;
        }
        public void run() {
            if(socket!=null){
                try{
                    PrintWriter writer=new PrintWriter(socket.getOutputStream());
                    StringBuffer sb=new StringBuffer();
                    sb.append(responseServer());
                    sb.append(";");
                    writer.println(sb.toString());
                    writer.flush();

                    /*
                    while(!socket.isClosed()){
                        try {
                            Thread.sleep(10);
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                    */
                    writer.close();
                    socket.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args){
        if(args.length!=2){
            System.out.println("Wrong arguments to launch Director!");
            System.exit(1);
        }
        new Thread(new Director(args[0],args[1])).start();
    }
}
