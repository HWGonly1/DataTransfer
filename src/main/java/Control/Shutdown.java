package Control;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Shutdown {
    public static void main(String[] args){
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
                    Process pro2;
                    pro2=r.exec("kill -9 "+splits[0]);
                    pro2.destroy();
                }
            }
            in.close();
            pro.destroy();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
