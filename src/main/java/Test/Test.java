package Test;

import Client.Uploader;
import FTP.LoadInfo;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;

public class Test {
    //public static ZkClient zkClient=new ZkClient("localhost:2181",3000,3000,new SerializableSerializer());

    public static void main(String[] args){
        /*
        LoadInfo info=new LoadInfo();
        info.net=0;
        info.disk=0;
        info.mem=0;
        info.net=0;
        zkClient.createEphemeral("/Servers/server1",info);
        zkClient.close();
        */
        new Uploader("192.168.3.152",21,"root","beap123","/home/hwg","Topic","/Users/hwg/Downloads/TopicReport-5.docx").upload();
    }
}
