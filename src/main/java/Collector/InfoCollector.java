package Collector;

import FTP.LoadInfo;
import Zookeeper.ZKUtil;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class InfoCollector implements Runnable{
    private String path="/home/hwg/DataTransfer/InfoCollection";
    private String zkServers;
    private String rootNode;
    public HashMap<String,LoadInfo> infoMap=new HashMap<String, LoadInfo>();
    private ZkClient zkClient;

    public InfoCollector(String zkServers,String rootNode,String path){
        this.zkServers=zkServers;
        this.rootNode=rootNode;
        this.path=path;
    }

    public HashMap<String,LoadInfo> getServerList(){
        List<String> childs=zkClient.getChildren(rootNode);
        HashMap<String, LoadInfo> map=new HashMap<String, LoadInfo>();
        for(String server:childs){
            map.put(server,(LoadInfo)zkClient.readData(rootNode+"/"+server));
        }
        return map;
    }

    public void run(){
        zkClient=new ZkClient(zkServers,3000,3000,new SerializableSerializer());
        zkClient.subscribeChildChanges(rootNode, new ChildListener());
        infoMap=getServerList();
        for(String s:infoMap.keySet()){
            zkClient.subscribeDataChanges(rootNode+"/"+s,new DataListener());
        }

        while(true){
            try {
                Thread.sleep(10);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    class ChildListener implements IZkChildListener{
        public void handleChildChange(String s, List<String> list) throws Exception {
            for(String str:list){
                if(!infoMap.containsKey(str)){
                    zkClient.subscribeDataChanges(s+"/"+str,new DataListener());
                }
            }
            infoMap.clear();
            infoMap = getServerList();

            FileWriter writer=null;
            try{
                writer=new FileWriter(path,true);
                for(String key:infoMap.keySet()){
                    writer.write(System.currentTimeMillis()+"\r\n");
                    writer.write(s+"/"+key+"\t");
                    writer.write(infoMap.get(key).cpu+"\t"+infoMap.get(key).disk+"\t"+infoMap.get(key).mem+"\t"+infoMap.get(key).net);
                    writer.write("\r\n");
                }
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                writer.close();
            }
        }
    }
    class DataListener implements IZkDataListener {
        public void handleDataChange(String s, Object o) throws Exception {
            infoMap.put(s,(LoadInfo)o);
            FileWriter writer=null;
            try{
                writer=new FileWriter(path,true);
                for(String key:infoMap.keySet()){
                    writer.write(System.currentTimeMillis()+"\r\n");
                    writer.write(key+"\t");
                    writer.write(infoMap.get(key).cpu+"\t"+infoMap.get(key).disk+"\t"+infoMap.get(key).mem+"\t"+infoMap.get(key).net);
                    writer.write("\r\n");
                }
            }catch (IOException e){
                e.printStackTrace();
            }finally {
                writer.close();
            }
        }

        public void handleDataDeleted(String s) throws Exception {
            infoMap.remove(s);
        }
    }


    public static void main(String[] args){
        Thread collector=new Thread(new InfoCollector(args[0],args[1],args[2]));
        collector.start();
        /*
        try {
            collector.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        */
    }
}
