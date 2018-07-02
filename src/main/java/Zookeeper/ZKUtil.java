package Zookeeper;

import FTP.LoadInfo;
import Server.StaticUtil;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.I0Itec.zkclient.serialize.SerializableSerializer;

import java.util.*;

public class ZKUtil{
    /**
     * 获取当前可用的所有FTP Server地址端口及其负载信息
     */
    public Hashtable<String,LoadInfo> infoMap=new Hashtable<String, LoadInfo>();
    public ArrayList<String> validServer=new ArrayList<String>();
    public String zkServers;
    public String rootNode;
    public String addr;
    public Boolean valid=true;

    public Boolean isServer;

    public int no=0;

    public ZkClient zkClient;

    //负载情况差值的阈值
    public float threshold=0.2f;
    //根据是否超出阈值 获取 每个客户端分配的服务器是否发生了变化
    public boolean changed=false;
    //当前服务器更新间隔
    public int interval=10000;
    //当前服务器静态性能
    public StaticUtil su;
    public int weight;

    public ZKUtil(String zkServers,String rootNode,String addr,Boolean isServer){
        System.setProperty("log4j.configuration","log4j.properties");
        this.zkServers=zkServers;
        this.rootNode=rootNode;
        this.addr=addr;
        this.zkClient=new ZkClient(zkServers,3000,3000,new SerializableSerializer());
        this.isServer=isServer;

        if(isServer) {
            this.su = new StaticUtil();
            this.weight = getWeight();
        }
    }

    public Hashtable<String,LoadInfo> getServerList(){
        List<String> childs=zkClient.getChildren(rootNode);
        Hashtable<String, LoadInfo> map=new Hashtable<String, LoadInfo>();
        boolean alive=false;
        for(String server:childs){
            map.put(server,(LoadInfo)zkClient.readData(rootNode+"/"+server));
            if(server.equals(addr))
                alive=true;
        }
        if(!alive&&isServer){
            regServer(infoMap.get(addr));
        }
        return map;
    }

    /**
     * 创建FTP Server子节点
     */
    public void regServer(LoadInfo info){
        if(!zkClient.exists(rootNode)){
            zkClient.createPersistent(rootNode);
        }

        if(isServer){
            try {
                zkClient.createEphemeral(rootNode + "/" + addr, info);
            }catch (ZkNodeExistsException e){
            }
        }

        zkClient.subscribeChildChanges(rootNode, new ChildListener());
        infoMap=getServerList();
        for(String server:infoMap.keySet()){
            if(!server.equals(addr)){
                zkClient.subscribeDataChanges(rootNode+"/"+server,new DataListener());
            }
        }

        Thread balancer=new Thread(new Balancer());
        balancer.start();

        while(!balancer.isAlive()){
            try {
                Thread.sleep(10);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        synchronized (valid){
            valid.notifyAll();
        }
    }

    /**
     * 删除FTP Server子节点
     */
    public void deleteServer(String zkServers,String rootNode,String server){
        zkClient.delete(rootNode+"/"+server);
    }

    /**
     * 上传负载数据
     */
    public void report(LoadInfo info){
        zkClient.writeData(rootNode+"/"+addr,info);
        synchronized (valid){
            valid.notifyAll();
        }
    }

    public int getWeight(){
        try {
            zkClient.createEphemeral("/Static", su);
        }catch (ZkNodeExistsException e){
        }
        StaticUtil refer=zkClient.readData("/Static");

        //System.out.println(su.bandwith+"\t"+su.cpuCores+"\t"+su.diskIOSpeed+"\t"+su.memCapcity);

        int weight=Math.round(1*(float)su.cpuCores/refer.cpuCores+2*(float)su.diskIOSpeed/refer.diskIOSpeed+2*(float)su.memCapcity/refer.memCapcity+5*(float)su.bandwith/refer.bandwith);

        //System.out.println("weight:"+weight);

        return weight;
    }

    class ChildListener implements IZkChildListener{
        public void handleChildChange(String s, List<String> list) throws Exception {
            for(String server:list){
                if(!infoMap.containsKey(server)){
                    zkClient.subscribeDataChanges(s+"/"+server,new DataListener());
                }
            }

            infoMap.clear();
            infoMap=getServerList();

            synchronized (valid){
                valid.notifyAll();
            }
        }
    }

    class DataListener implements IZkDataListener{
        public void handleDataChange(String s, Object o) throws Exception {
            infoMap.put(s.substring(s.lastIndexOf("/")+1),(LoadInfo)o);
            synchronized (valid){
                valid.notifyAll();
            }
        }

        public void handleDataDeleted(String s) throws Exception {
            infoMap.remove(s);
            synchronized (valid){
                valid.notifyAll();
            }
        }
    }

    class Balancer implements Runnable{

        public void run(){
            while(true){
                try {
                    synchronized(valid){
                        valid.wait();
                    }
                    rebalance();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }

        /**
         * 算法实现核心
         */
        public void rebalance(){
            Map<String,LoadInfo> copy=new HashMap<String, LoadInfo>();
            Map<String,Float> load=new HashMap<String,Float>();

            //存将被进行分配的服务器负载状态，传递给WRR算法实现形成新的ValidServer列表
            Map<String,Node> servers=new HashMap<String, Node>();

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
                                //validServer.add(host);

                                servers.put(host,new Node(copy.get(host).getWeight()));
                            }
                        }
                        changed=true;
                        interval=Math.max(interval/2,5000);
                    }else{
                        //validServer.addAll(load.keySet());

                        for(String host:copy.keySet()){
                            servers.put(host,new Node(copy.get(host).getWeight()));
                        }
                        changed=false;
                        interval=Math.min(interval*2,40000);
                    }
                }else{
                    if(max-min>threshold){
                        for(String host:load.keySet()){
                            if(load.get(host)<0.9&&load.get(host)-min<threshold){
                                //validServer.add(host);

                                servers.put(host,new Node(copy.get(host).getWeight()));
                            }
                        }
                        changed=true;
                        interval=Math.max(interval/2,50);
                    }else{
                        //validServer.addAll(load.keySet());

                        for(String host:copy.keySet()){
                            servers.put(host,new Node(copy.get(host).getWeight()));
                        }
                        changed=false;
                        interval=Math.min(interval*2,40000);
                    }
                }

                int total=0;
                for(Node node:servers.values()){
                    total+=node.weight;
                }

                for(int i=0;i<total;i++){
                    validServer.add(next(servers,total));
                }

                /*
                for(String s:validServer){
                    System.out.print(s);
                }
                System.out.println(total);
                */

                no=0;
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

        //WRR
        public String next(Map<String,Node> servers,int total){
            int max=0;
            String server="";
            for(String s:servers.keySet()){
                if(servers.get(s).curr_weight>max){
                    server=s;
                    max=servers.get(s).curr_weight;
                    servers.get(s).curr_weight+=servers.get(s).weight-total;
                }else{
                    servers.get(s).curr_weight+=servers.get(s).weight;
                }
            }
            return server;
        }
    }

    class Node{
        public int weight;
        public int curr_weight;
        public Node(int weight){
            this.weight=weight;
            this.curr_weight=weight;
        }
    }
}
