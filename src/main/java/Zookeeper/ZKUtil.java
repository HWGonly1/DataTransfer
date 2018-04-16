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
    public HashMap<String,LoadInfo> infoMap=new HashMap<String, LoadInfo>();
    public ArrayList<String> validServer=new ArrayList<String>();
    public String zkServers;
    public String rootNode;
    public String addr;
    public Boolean valid;

    public int no=0;

    public ZkClient zkClient;

    //负责情况差值的阈值
    public float threshold=0.2f;
    //根据是否超出阈值 获取 每个客户端分配的服务器是否发生了变化
    public boolean changed=false;
    //当前服务器更新间隔
    public int interval;


    public ZKUtil(String zkServers,String rootNode,String addr){
        this.zkServers=zkServers;
        this.rootNode=rootNode;
        this.addr=addr;
        this.zkClient=new ZkClient(zkServers,3000,3000,new SerializableSerializer());
    }

    public HashMap<String,LoadInfo> getServerList(){
        List<String> childs=zkClient.getChildren(rootNode);
        HashMap<String, LoadInfo> map=new HashMap<String, LoadInfo>();
        boolean alive=false;
        for(String server:childs){
            if(!server.equals(addr))
                map.put(server,(LoadInfo)zkClient.readData(rootNode+"/"+server));
            else
                alive=true;
        }
        if(!alive){
            regServer(infoMap.get(addr));
        }
        zkClient.close();
        return map;
    }

    /**
     * 创建FTP Server子节点
     */
    public void regServer(LoadInfo info){
        if(!zkClient.exists(rootNode)){
            zkClient.createPersistent(rootNode);
        }

        zkClient.createEphemeral(rootNode+"/"+addr,info);
        zkClient.subscribeChildChanges(rootNode, new ChildListener());
        infoMap=getServerList();

        new Thread(new Balancer()).start();

        for(String host:infoMap.keySet()){
            if(!addr.equals(host)){
                zkClient.subscribeDataChanges(rootNode+"/"+addr,new DataListener());
            }
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
            infoMap.clear();
            infoMap=getServerList();

            for(String host:infoMap.keySet()){
                if(!addr.equals(host)){
                    zkClient.subscribeDataChanges(rootNode+"/"+addr,new DataListener());
                }
            }
        }
    }

    class DataListener implements IZkDataListener{
        public void handleDataChange(String s, Object o) throws Exception {
            infoMap.put(s,(LoadInfo)o);
            synchronized (valid){
                valid=false;
                valid.notifyAll();
            }
        }

        public void handleDataDeleted(String s) throws Exception {
            infoMap.put(s,null);
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
                    }
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
                    load.put(host,calLoad(copy.get(host)));
                    max=Math.max(max,l);
                    min=Math.min(min,l);
                }
                if(min>0.9){
                    if(max-min>threshold){
                        for(String host:load.keySet()){
                            if(load.get(host)-min<threshold){
                                validServer.add(host);
                            }
                        }
                        changed=true;
                    }else{
                        changed=false;
                    }
                }else{
                    if(max-min>threshold){
                        for(String host:load.keySet()){
                            if(load.get(host)<0.9&&load.get(host)-min<threshold){
                                validServer.add(host);
                            }
                        }
                        changed=true;
                    }else{
                        changed=false;
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
}