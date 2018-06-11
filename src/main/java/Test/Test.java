package Test;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;

public class Test {
    public static ZkClient zkClient=new ZkClient("192.168.3.152:2181",3000,3000,new SerializableSerializer());

    public static void main(String[] args){
        System.out.println(zkClient.exists("/Servers"));
        zkClient.createPersistent("/Servers");
        System.out.println(zkClient.exists("/Servers"));
        zkClient.close();
    }
}
