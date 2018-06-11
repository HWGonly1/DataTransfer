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
        }
        zkUtil=new ZKUtil(zkServers,rootNode,addr);
    }

    public void run(){
        ServerSocket server=null;
        try{
            server=new ServerSocket(9527);
        }catch(IOException e){
            e.printStackTrace();
        }
        while(true){
            Socket socket=null;
            try{
                socket=server.accept();
            }catch(IOException e){
                e.printStackTrace();
            }finally {
                if(socket!=null){
                    try{
                        PrintWriter writer=new PrintWriter(socket.getOutputStream());
                        Map<String, LoadInfo> allServers=zkUtil.getServerList();
                        StringBuilder sb=new StringBuilder();

                        List<String> list=Arrays.asList((String[])allServers.keySet().toArray());
                        Collections.shuffle(list);
                        for(String s:list){
                            sb.append(s);
                            sb.append(";");
                        }

                        writer.println(sb.toString());
                        writer.flush();
                        writer.close();
                        socket.close();
                    }catch (IOException e){
                    }
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
