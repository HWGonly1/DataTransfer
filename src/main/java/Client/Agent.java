package Client;

import java.io.*;
import java.net.Socket;

public class Agent implements Runnable{
    private String director="192.168.3.152";
    private int directPort=9529;
    private int ftpPort;
    private int interval;
    private long lastTime=-1;

    public void run(){
        try {
            Socket socket = new Socket(director, directPort);
            BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String readline=reader.readLine();
            String server=readline.split(";")[0];
            reader.close();
            socket.close();

            while(true){
                if(System.currentTimeMillis()-lastTime>interval){
                    socket=new Socket(server,directPort);
                    reader=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    readline=reader.readLine();
                    server=readline.split(";")[0];
                    interval=Integer.parseInt(readline.split(";")[1]);
                    reader.close();
                    socket.close();
                }

                new Uploader(server,ftpPort,"","","",Thread.currentThread().getName(),"").upload();
                Thread.sleep(10);
            }
        }catch (IOException e){
            e.printStackTrace();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
