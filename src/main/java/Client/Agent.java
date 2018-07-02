package Client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

public class Agent implements Runnable{
    private String director="192.168.10.10";
    private int directPort=9529;
    private int ftpPort;
    private int interval;
    private long lastTime=-1;

    private String pathname;
    private String localpath;
    private int sleeptime;
    private int no=0;

    public Agent(String director,int directPort,int ftpPort,String pathname,String localpath,int sleeptime){
        this.director=director;
        this.directPort=directPort;
        this.ftpPort=ftpPort;
        this.pathname=pathname;
        this.localpath=localpath;
        this.sleeptime=sleeptime;
    }

    public void run(){
        try {
            String address= InetAddress.getLocalHost().getHostAddress();
            Socket socket = new Socket(director, directPort);
            BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String readline=reader.readLine();

            String server=readline.split(";")[0];
            reader.close();
            socket.close();

            while(true){
                if(System.currentTimeMillis()-lastTime>interval){
                    socket = new Socket(server, directPort);
                    reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    readline=reader.readLine();

                    server = readline.split(";")[0];
                    interval=Integer.parseInt(readline.split(";")[1]);
                    reader.close();
                    socket.close();
                }

                new Uploader(server,ftpPort,"root","beap123",pathname,address+Thread.currentThread().getName()+"_"+no++,localpath).upload();
                Thread.sleep(sleeptime);
            }
        }catch (IOException e){
            e.printStackTrace();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
