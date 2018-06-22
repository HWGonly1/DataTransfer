package Zookeeper;

import FTP.LoadInfo;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
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


    public ZKUtil(String zkServers,String rootNode,String addr,Boolean isServer){
        System.setProperty("log4j.configuration","log4j.properties");
        this.zkServers=zkServers;
        this.rootNode=rootNode;
        this.addr=addr;
        this.zkClient=new ZkClient(zkServers,3000,3000,new SerializableSerializer());
        this.isServer=isServer;
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
            zkClient.createEphemeral(rootNode+"/"+addr,info);
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
        new Thread(new Balancer()).start();

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
                        validServer.addAll(load.keySet());
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
                        validServer.addAll(load.keySet());
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

        //WRR
        public void next(){

        }
    }
}
