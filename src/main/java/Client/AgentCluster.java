package Client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgentCluster {
    public static void main(String[] args){
        ExecutorService pool= Executors.newFixedThreadPool(Integer.parseInt(args[0]));
        //int interval=1000/Integer.parseInt(args[0]);
        int interval=10;
        for(int i=0;i<Integer.parseInt(args[0]);i++){
            pool.execute(new Agent(args[1],Integer.parseInt(args[2]),Integer.parseInt(args[3]),args[4],args[5],Integer.parseInt(args[6])));
            try{
                Thread.sleep(interval);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}
