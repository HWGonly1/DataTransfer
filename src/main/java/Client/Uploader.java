package Client;

import FTP.FTPUtil;
import FTP.LoadInfo;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

public class Uploader {
    public static Uploader uploader;

    private String zkServers;
    private String rootNode;
    private String currentServer;
    private int port;
    private String username;
    private String password;
    private String pathname;
    private String filename;
    private String localPath;

    private Uploader(String zkServers,String rootNode,int port,String username,String password,String pathname,String filename,String localPath){
        this.zkServers=zkServers;
        this.rootNode=rootNode;
        this.currentServer="";
        this.port=port;
        this.username=username;
        this.password=password;
        this.pathname=pathname;
        this.filename=filename;
        this.localPath=localPath;
    }

    public static void setUploader(String zkServers,String rootNode,int port,String username,String password,String pathname,String filename,String localPath){
        uploader=new Uploader(zkServers,rootNode,port,username,password,pathname,filename,localPath);
    }
    public static void setCurrentServer(String currentServer){
        uploader.currentServer=currentServer;
    }

    public void upload(){
        //Map<String, LoadInfo> ftpServers= ZKUtil.getServerList(zkServers,rootNode);
        //String server=Uploader.bestServer(ftpServers);

        String server="10.210.66.13";
        setCurrentServer(server);
        try{
            InputStream in=new BufferedInputStream(new FileInputStream(localPath));
            FTPUtil.uploadFile(server,port,username,password,pathname,filename,in);
        }catch (FileNotFoundException e){

        }
    }

    public void failover(){
        //ZKUtil.deleteServer(uploader.zkServers,uploader.rootNode,uploader.currentServer);
        upload();
    }

    public static String bestServer(Map<String, LoadInfo> map){
        return "";
    }

    public static void main(String[] args){
        String zkServers=args[0];
        String rootNode=args[1];
        int port=Integer.parseInt(args[2]);
        String username=args[3];
        String password=args[4];
        String pathname=args[5];
        String filename=args[6];
        String localPath=args[7];

        setUploader(zkServers,rootNode,port,username,password,pathname,filename,localPath);
        uploader.upload();
    }

}
