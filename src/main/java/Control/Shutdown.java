package Control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

public class Shutdown {
    public static void main(String[] args){
        Hashtable<String,String> processs=new Hashtable<String, String>();
        Process pro;
        Runtime r=Runtime.getRuntime();
        try {
            pro = r.exec("jps");
            BufferedReader in=new BufferedReader(new InputStreamReader(pro.getInputStream()));
            String line=null;
            while((line=in.readLine())!=null){
                line=line.trim();
                String[] splits=line.split("\\s+");
                if(splits[1].equals("Director")||splits[1].equals("InfoCollector")||splits[1].equals("Server")||splits[1].equals("AgentCluster")){
                    processs.put(splits[0],splits[1]);
                }
            }
            in.close();
            pro.destroy();
            for(String key:processs.keySet()){
                Process pro2;
                pro2=r.exec("kill -9 "+key);
                pro2.waitFor();
            }
        }catch (IOException e){
            e.printStackTrace();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
