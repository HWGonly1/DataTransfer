package Test;

import Director.Director;
import FTP.LoadInfo;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class Test {
    //public static ZkClient zkClient=new ZkClient("localhost:2181",3000,3000,new SerializableSerializer());

    public static void main(String[] args){
        /*
        LoadInfo info=new LoadInfo();
        info.net=0;
        info.disk=0;
        info.mem=0;
        info.net=0;
        zkClient.subscribeChildChanges("/Servers", new IZkChildListener() {
            public void handleChildChange(String s, List<String> list) throws Exception {
                List<String> childs=zkClient.getChildren("/Servers");
                for(String str:childs){
                    System.out.println(str);
                }
            }
        });
        try {
            Thread.sleep(1000);
            zkClient.createEphemeral("/Servers/server1",info);
            Thread.sleep(10000);
        }catch (Exception e){
            e.printStackTrace();
        }
        zkClient.close();
        */
        //new Uploader("192.168.3.152",21,"root","beap123","/home/hwg","Topic","/Users/hwg/Downloads/TopicReport-5.docx").upload();

        /*
        Process pro1;
        Runtime r=Runtime.getRuntime();
        try {
            String command = "cat /proc/net/dev";
            pro1 = r.exec(command);
            BufferedReader in1 = new BufferedReader(new InputStreamReader(pro1.getInputStream()));
            String line;
            //long inSize1 = 0;
            while ((line = in1.readLine()) != null) {
                line = line.trim();

                System.out.println(line);
                System.out.println();

                String[] splits = line.split(":");
                System.out.println(splits.length);


                if (splits.length > 1) {
                    String[] temp = splits[1].split("\\s+");
                    System.out.println(temp[0]);
                    System.out.println();

                    //inSize1 = Long.parseLong(temp[1]);
                    //dls.add(inSize1);
                }

            }
        }catch (IOException e){
            e.printStackTrace();
        }
        */
        /*
        String line="   em4:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0\n";
        String[] splits = line.split(":");

        if (splits.length > 1) {
            System.out.println("+"+splits[1]);

            String[] temp = splits[1].trim().split("\\s+");
            System.out.println(temp[0]);

            for(String s:temp){
                System.out.println("*"+s);
            }

            //long inSize1 = Long.parseLong(temp[0]);
        }
        */
        /*
        Director director=new Director("192.168.10.10:2181,192.168.10.14:2181,192.168.10.22:2181","/Servers");
        new Thread(director).start();
        while(true){
            //System.out.println(director.zkUtil.validServer);
            //System.out.println(director.zkUtil.infoMap);
            if(director.zkUtil.validServer.size()>0){
                System.out.println(director.responseServer());
            }
            try{
                Thread.sleep(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        */
        Process pro;
        Runtime r=Runtime.getRuntime();
        try {
            pro = r.exec("jps");
            BufferedReader in=new BufferedReader(new InputStreamReader(pro.getInputStream()));
            String line=null;
            while((line=in.readLine())!=null){
                line=line.trim();
                String[] splits=line.split("\\s+");
                System.out.println(splits[0]+","+splits[1]);
            }
            in.close();
            pro.destroy();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
