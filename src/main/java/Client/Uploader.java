package Client;

import FTP.FTPUtil;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class Uploader {
    public static Uploader uploader;

    private String currentServer;
    private int port;
    private String username;
    private String password;
    private String pathname;
    private String filename;
    private String localPath;

    public Uploader(String currentServer,int port,String username,String password,String pathname,String filename,String localPath){
        this.currentServer=currentServer;
        this.port=port;
        this.username=username;
        this.password=password;
        this.pathname=pathname;
        this.filename=filename;
        this.localPath=localPath;
    }

    public static void setCurrentServer(String currentServer){
        uploader.currentServer=currentServer;
    }

    public void upload(){
        try{
            InputStream in=new BufferedInputStream(new FileInputStream(localPath));
            FTPUtil.uploadFile(currentServer,port,username,password,pathname,filename,in);
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
    }
}
