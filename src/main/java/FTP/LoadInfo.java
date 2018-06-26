package FTP;

import Server.SystemUtil;

import java.io.Serializable;

public class LoadInfo implements Serializable{

    public static int weight=0;

    public float net=0;
    public float cpu=0;
    public float disk=0;
    public float mem=0;

    public void refresh(){
        float[] res=new float[4];
        SystemUtil.refresh(res);
        net=res[0];
        cpu=res[1];
        disk=res[2];
        mem=res[3];
    }

    public int getWeight(){
        return weight;
    }
}
