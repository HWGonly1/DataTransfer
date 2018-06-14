package Server;

import FTP.LoadInfo;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

import Zookeeper.ZKUtil;
/*
public class Server implements Runnable{
    public static int collectInterval=2000;
    public static String zkServers;
    public static String rootNode;
    //public ZKUtil zkUtil;
    public LoadInfo info;
    public String addr;

    public Hashtable<String,LoadInfo> infoMap=new Hashtable<String, LoadInfo>();
    public ArrayList<String> validServer=new ArrayList<String>();
    public Boolean valid=true;
    public int no=0;
    public ZkClient zkClient;

    //负载情况差值的阈值
    public float threshold=0.2f;
    //根据是否超出阈值 获取 每个客户端分配的服务器是否发生了变化
    public boolean changed=false;
    //当前服务器更新间隔
    public int interval=10000;

    public boolean alive=true;

    Server(int collectInterval,String zkServers,String rootNode){
        Server.collectInterval=collectInterval;
        this.zkServers=zkServers;
        this.rootNode=rootNode;
        try{
            addr= InetAddress.getLocalHost().getHostAddress();
        }catch(UnknownHostException e){
            e.printStackTrace();
        }
        this.zkClient=new ZkClient(zkServers,3000,3000,new SerializableSerializer());
        //zkUtil=new ZKUtil(zkServers,rootNode,addr,true);

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
        this.zkClient=new ZkClient(zkServers,3000,3000,new SerializableSerializer());
        //zkUtil=new ZKUtil(zkServers,rootNode,addr,true);
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

                        StringBuffer sb=new StringBuffer();
                        sb.append(responseServer());
                        sb.append(";");
                        sb.append(interval);

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
        synchronized (validServer){
            server=validServer.get(no);
            no++;
            no%=validServer.size();
        }
        return server;
    }


    public Hashtable<String,LoadInfo> getServerList(){
        List<String> childs=zkClient.getChildren(rootNode);
        Hashtable<String, LoadInfo> map=new Hashtable<String, LoadInfo>();
        alive=false;
        for(String server:childs){
            map.put(server,(LoadInfo)zkClient.readData(rootNode+"/"+server));
            if(server.equals(addr))
                alive=true;
        }
        return map;
    }

    public class Reporter implements Runnable{
        public void run(){

            if(!zkClient.exists(rootNode)){
                zkClient.createPersistent(rootNode);
            }
            zkClient.createEphemeral(rootNode+"/"+addr,info);

            try {
                Thread.sleep(2000);
            }catch (Exception e){
                e.printStackTrace();
            }
            zkClient.subscribeChildChanges(rootNode, new IZkChildListener() {
                public void handleChildChange(String s, List<String> list) throws Exception {
                    infoMap.clear();
                    infoMap=getServerList();
                }
            });
            infoMap=getServerList();
            new Thread(new Balancer()).start();

            while(true){
                info.refresh();
                infoMap.put(addr,info);
                zkClient.writeData(rootNode+"/"+addr,info);
            }
        }
    }

    class Balancer implements Runnable{

        public void run(){
            synchronized (valid){
                while(valid==true){
                    try {
                        valid.wait();
                        rebalance();
                        valid=true;
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }

        //算法实现核心
        public void rebalance(){
            Map<String,LoadInfo> copy=new HashMap<String, LoadInfo>();
            Map<String,Float> load=new HashMap<String,Float>();
            synchronized (infoMap){
                copy.putAll(infoMap);
            }
            synchronized (validServer){
                validServer.clear();
                float max=0,min=1;
                for(String host:copy.keySet()){
                    float l=calLoad(copy.get(host));
                    load.put(host,l);
                    max=Math.max(max,l);
                    min=Math.min(min,l);
                }
                if(min>0.9-threshold){
                    if(max-min>threshold){
                        for(String host:load.keySet()){
                            if(load.get(host)-min<threshold){
                                validServer.add(host);
                            }
                        }
                        changed=true;
                        interval=Math.max(interval/2,5000);
                    }else{
                        changed=false;
                        interval=Math.min(interval*2,40000);
                    }
                }else{
                    if(max-min>threshold){
                        for(String host:load.keySet()){
                            if(load.get(host)<0.9&&load.get(host)-min<threshold){
                                validServer.add(host);
                            }
                        }
                        changed=true;
                        interval=Math.max(interval/2,50);
                    }else{
                        changed=false;
                        interval=Math.min(interval*2,40000);
                    }
                }
            }
        }

        public float calLoad(LoadInfo info){
            float memFactor=1.0f;
            float cpuFactor=1.0f;
            float diskFactor=1.0f;
            float netFactor=1.0f;

            float load=1-(1-memFactor*info.mem)*(1-cpuFactor*info.cpu)*(1-diskFactor*info.disk)*(1-netFactor*info.net);
            return load;
        }
    }

    public static void main(String[] args){
        //if(args.length==3){
        //    new Thread(new Server(Integer.parseInt(args[0]),args[1],args[2])).start();
        //}else if(args.length==2){
        new Thread(new Server(args[0],args[1])).start();
        //}else{
        //    throw new IllegalArgumentException("Wrong arguments to launch Server!");
        //}
    }
}
*/

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
        zkUtil=new ZKUtil(zkServers,rootNode,addr,true);

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
        zkUtil=new ZKUtil(zkServers,rootNode,addr,true);
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

                        StringBuffer sb=new StringBuffer();
                        sb.append(responseServer());
                        sb.append(";");
                        sb.append(zkUtil.interval);

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

    public class Reporter implements Runnable{
        public void run(){
            zkUtil.regServer(info);

            /*
            try {
                Thread.sleep(2000);
            }catch (Exception e){
                e.printStackTrace();
            }
            */

            while(true){
                info.refresh();
                zkUtil.infoMap.put(addr,info);
                zkUtil.report(info);
            }
        }
    }

    public static void main(String[] args){
        //if(args.length==3){
        //    new Thread(new Server(Integer.parseInt(args[0]),args[1],args[2])).start();
        //}else if(args.length==2){
        new Thread(new Server(args[0],args[1])).start();
        //}else{
        //    throw new IllegalArgumentException("Wrong arguments to launch Server!");
        //}
    }
}