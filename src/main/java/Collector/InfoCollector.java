package Collector;

import FTP.LoadInfo;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class InfoCollector implements Runnable{
    private String path="/home/hwg/InfoCollection";
    private String zkServers;
    private String rootNode;
    public HashMap<String,LoadInfo> infoMap=new HashMap<String, LoadInfo>();
    private ZkClient zkClient;

    public HashMap<String,LoadInfo> getServerList(){
        List<String> childs=zkClient.getChildren(rootNode);
        HashMap<String, LoadInfo> map=new HashMap<String, LoadInfo>();
        boolean alive=false;
        zkClient.close();
        return map;
    }

    public void run(){
        zkClient=new ZkClient(zkServers,3000,3000,new SerializableSerializer());
        zkClient.subscribeChildChanges(rootNode, new IZkChildListener() {
            public void handleChildChange(String s, List<String> list) throws Exception {
                infoMap.clear();
                infoMap = getServerList();

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
        });

    }
}
