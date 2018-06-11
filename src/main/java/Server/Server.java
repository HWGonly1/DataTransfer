package Server;

import FTP.LoadInfo;
import Zookeeper.ZKUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;

public class Server implements Runnable{
    public static int collectInterval=2000;
    public static String zkServers;
    public static String rootNode;
    public ZKUtil zkUtil;
    public LoadInfo info;
    public String addr;

    Server(int collectInterval,String zkServers,String rootNode){
        Server.collectInterval=collectInterval;
        this.zkServers=zkServers;
        this.rootNode=rootNode;
        try{
            addr= InetAddress.getLocalHost().getHostAddress();
        }catch(UnknownHostException e){
            e.printStackTrace();
        }
        zkUtil=new ZKUtil(zkServers,rootNode,addr);
        info=new LoadInfo();
        info.refresh();
    }

    Server(String zkServers,String rootNode){
        Server.collectInterval=2000;
        this.zkServers=zkServers;
        this.rootNode=rootNode;
        try{
            addr= InetAddress.getLocalHost().getHostAddress();
        }catch(UnknownHostException e){
            e.printStackTrace();
        }
        zkUtil=new ZKUtil(zkServers,rootNode,addr);
        info=new LoadInfo();
        info.refresh();
    }


    public static LoadInfo getInfo(){
        LoadInfo info=new LoadInfo();
        info.refresh();
        return info;
    }

    public void run(){
        new Thread(new Reporter()).start();
        ServerSocket server=null;
        try{
            server=new ServerSocket(9529);
        }catch (IOException e){
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

                        StringBuffer sb=new StringBuffer();
                        sb.append(responseServer());
                        sb.append(";");
                        sb.append(zkUtil.interval);
                        /*
                        if(new Random().nextBoolean()){
                            sb.append("realServer1");
                        }else{
                            sb.append("realServer2");
                        }
                        */

                        writer.println(sb.toString());
                        writer.flush();
                        writer.close();
                        socket.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String responseServer(){
        String server="";
        synchronized (zkUtil.validServer){
            server=zkUtil.validServer.get(zkUtil.no);
            zkUtil.no++;
            zkUtil.no%=zkUtil.validServer.size();
        }
        return server;
    }

    public static void main(String[] args){
        if(args.length==3){
            new Thread(new Server(Integer.parseInt(args[0]),args[1],args[2])).start();
        }else if(args.length==2){
            new Thread(new Server(args[0],args[1])).start();
        }else{
            throw new IllegalArgumentException("Wrong arguments to launch Server!");
            //new Thread(new Server()).start();
        }
    }

    public class Reporter implements Runnable{
        public void run(){
            String addr="";
            try{
                addr= InetAddress.getLocalHost().getHostAddress();
            }catch(UnknownHostException e){
                e.printStackTrace();
            }
            zkUtil.regServer(info);
            zkUtil.infoMap.put(addr,info);
            while(true){
                info.refresh();
                zkUtil.infoMap.put(addr,info);
                zkUtil.report(info);
            }
        }
    }

}
