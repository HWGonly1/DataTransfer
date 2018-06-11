package Client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgentCluster {
    public void main(String[] args){
        ExecutorService pool= Executors.newFixedThreadPool(Integer.parseInt(args[0]));
        for(int i=0;i<Integer.parseInt(args[0]);i++){
            pool.execute(new Agent());
        }
    }
}
