package Server;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class SystemUtil{

    public CountDownLatch latch;
    public float[] array;

    public SystemUtil(CountDownLatch latch,float[] array){
        this.latch=latch;
        this.array=array;
    }
    /**
     * 获取带宽及服务器接收速度
     * @return
     */
    public static float getReceiveSpeed() {
        boolean result = false;
        float dl=0;
        Process pro1,pro2;
        Runtime r = Runtime.getRuntime();
        ArrayList<Long> dls=new ArrayList<Long>();
        try {
            String command = "cat /proc/net/dev";
            //第一次采集流量数据
            long startTime = System.currentTimeMillis();
            pro1 = r.exec(command);
            BufferedReader in1 = new BufferedReader(new InputStreamReader(pro1.getInputStream()));
            String line = null;
            long inSize1 = 0;
            while((line=in1.readLine()) != null){
                line = line.trim();

                String[] temp = line.split("\\s+");
                inSize1 = Long.parseLong(temp[1]);
                dls.add(inSize1);

                /*
                if(line.startsWith("eth0")||line.startsWith("em1")){
                    String[] temp = line.split("\\s+");
                    inSize1 = Long.parseLong(temp[1]);              //Receive bytes,单位为Byte
                    //outSize1 = Long.parseLong(temp[9]);             //Transmit bytes,单位为Byte
                    break;
                }
                */
            }
            in1.close();
            pro1.destroy();
            try {
                Thread.sleep(Server.collectInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //第二次采集流量数据
            long endTime = System.currentTimeMillis();
            pro2 = r.exec(command);
            BufferedReader in2 = new BufferedReader(new InputStreamReader(pro2.getInputStream()));
            long inSize2 = 0;
            int i=0;
            while((line=in2.readLine()) != null){
                line = line.trim();

                String[] temp = line.split("\\s+");
                inSize2 = Long.parseLong(temp[1]);
                Long lastDL=dls.get(i);
                dls.set(i++,inSize2-lastDL);

                /*
                if(line.startsWith("eth0")||line.startsWith("em1")){
                    //System.out.println(line);
                    String[] temp = line.split("\\s+");
                    inSize2 = Long.parseLong(temp[1]);
                    //outSize2 = Long.parseLong(temp[9]);
                    break;
                }
                */
            }

            long max=0;
            for(Long dlInTime:dls){
                max=Math.max(max,dlInTime);
            }

            //cal dl speed
            float interval = (float)(endTime - startTime)/1000;

            float currentDlSpeed=(float)((float)max/1024/interval);

            //float currentDlSpeed = (float) ((float)(inSize2 - inSize1)/1024/interval);
            //float currentUlSpeed = (float) ((float)(outSize2 - outSize1)/1024/interval);
            /*
            if((float)(currentDlSpeed/1024) >= 1){
                currentDlSpeed = (float)(currentDlSpeed/1024);
                dl = df.format(currentDlSpeed) + "Mb/s";
            }else{
                dl = df.format(currentDlSpeed) + "Kb/s";
            }

            if((float)(currentUlSpeed/1024) >= 1){
                currentUlSpeed = (float)(currentUlSpeed/1024);
                ul = df.format(currentUlSpeed) + "Mb/s";
            }else{
                ul = df.format(currentUlSpeed) + "Kb/s";
            }
            */

            dl=currentDlSpeed;
            in2.close();
            pro2.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dl;
    }

    /**
     * 获取带宽使使用率
     * @return
     */
    public class BandWidth implements Runnable{
        public void run(){
            float TotalBandwidth=0;
            float netUsage = 0.0f;
            Process pro1,pro2,pro3;
            Runtime r = Runtime.getRuntime();

            //ArrayList<Long> dls=new ArrayList<Long>();
            HashMap<String,Long> map=new HashMap<String, Long>();

            try {
                String command = "cat /proc/net/dev";
                //第一次采集流量数据
                long startTime = System.currentTimeMillis();
                pro1 = r.exec(command);
                BufferedReader in1 = new BufferedReader(new InputStreamReader(pro1.getInputStream()));
                String line;
                long inSize1;
                long outSize1;
                while((line=in1.readLine()) != null){
                    line = line.trim();
                    String[] splits=line.split(":");
                    if(splits.length>1&&(!splits[0].trim().equals("lo"))){
                        String[] temp = splits[1].trim().split("\\s+");
                        inSize1 = Long.parseLong(temp[0]);
                        outSize1 = Long.parseLong(temp[8]);
                        map.put(splits[0].trim(),inSize1+outSize1);
                    }
                }
                in1.close();
                pro1.destroy();
                try {
                    Thread.sleep(Server.collectInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //第二次采集流量数据
                long endTime = System.currentTimeMillis();
                pro2 = r.exec(command);
                BufferedReader in2 = new BufferedReader(new InputStreamReader(pro2.getInputStream()));
                long inSize2;
                long outSize2;
                int i=0;
                while((line=in2.readLine()) != null){
                    line = line.trim();
                    String[] splits=line.split(":");
                    if(splits.length>1&&(!splits[0].trim().equals("lo"))){
                        String[] temp = splits[1].trim().split("\\s+");
                        inSize2 = Long.parseLong(temp[0]);
                        outSize2 = Long.parseLong(temp[8]);
                        Long last=map.get(splits[0].trim());
                        map.put(splits[0].trim(),inSize2+outSize2-last);
                        //Long lastDL=dls.get(i);
                        //dls.set(i++,inSize2-lastDL);
                    }
                }

                long max=0;
                String card="lo";
                for(String s:map.keySet()){
                    if(map.get(s)>max){
                        card=s;
                        max=map.get(s);
                    }
                }
                /*
                if(inSize1 != 0 &&inSize2 != 0){
                    float interval = (float)(endTime - startTime)/1000;
                    //网口传输速度,单位为bps
                    float curRate = (float)(inSize2 - inSize1)*8/(1000000*interval);
                    netUsage = curRate/TotalBandwidth;
                }
                */

                float interval=(float)(endTime-startTime)/1000;
                float curRate=(float)max*8/(interval*1000000);

                //获取流量最大的网卡带宽
                pro3=r.exec("ethtool "+card);
                BufferedReader in3 = new BufferedReader(new InputStreamReader(pro3.getInputStream()));
                while((line=in1.readLine())!=null){
                    if(line.contains("Speed:")){
                        TotalBandwidth=Integer.parseInt(line.substring(line.indexOf(":")+1,line.indexOf("Mb")).trim());
                        break;
                    }
                }

                netUsage=curRate/TotalBandwidth;
                in2.close();
                pro2.destroy();

            } catch (IOException e) {
                e.printStackTrace();
            }
            array[0]=netUsage;
            latch.countDown();
        }
    }

    /**
     * 功能：内存使用率
     * */
    public class Memory implements Runnable{
        public void run(){
            Map<String, Object> map = new HashMap<String,Object>();
            InputStreamReader inputs = null;
            BufferedReader buffer = null;
            try {
                inputs = new InputStreamReader(new FileInputStream("/proc/meminfo"));
                buffer = new BufferedReader(inputs);
                String line = "";
                while (true) {
                    line = buffer.readLine();
                    if (line == null)
                        break;
                    int beginIndex = 0;
                    int endIndex = line.indexOf(":");
                    if (endIndex != -1) {
                        String key = line.substring(beginIndex, endIndex);
                        beginIndex = endIndex + 1;
                        endIndex = line.length();
                        String memory = line.substring(beginIndex, endIndex);
                        String value = memory.replace("kB", "").trim();
                        map.put(key, value);
                    }
                }

                long memTotal = Long.parseLong(map.get("MemTotal").toString());
                long memFree = Long.parseLong(map.get("MemFree").toString());
                long memused = memTotal - memFree;
                //long buffers = Long.parseLong(map.get("Buffers").toString());
                //long cached = Long.parseLong(map.get("Cached").toString());
                //float usage = (float) (memused - buffers - cached) / memTotal;
                //return usage;
                array[3]=((float)memused)/memTotal;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    buffer.close();
                    inputs.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
            latch.countDown();
        }
    }

    /**
     * 获取磁盘IO使用率
     * @return
     */
    public class Disk implements Runnable{
        public void run()  {
            float ioUsage = 0.0f;
            Process pro;
            Runtime r = Runtime.getRuntime();
            try {
                String command = "iostat -d -x";
                pro = r.exec(command);
                BufferedReader in = new BufferedReader(new InputStreamReader(pro.getInputStream()));
                String line;
                int count = 0;
                while((line=in.readLine()) != null){
                    if(++count >= 4){
                        String[] temp = line.split("\\s+");
                        if(temp.length > 1){
                            float util =  Float.parseFloat(temp[temp.length-1]);
                            ioUsage = (ioUsage>util)?ioUsage:util;
                        }
                    }
                }
                if(ioUsage > 0){
                    ioUsage /= 100;
                }
                in.close();
                pro.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
            array[2]=ioUsage;
            latch.countDown();
        }
    }

    /**
     * 获取CPU使用率
     * @return
     */
    public class CPU implements Runnable{
        public void run() {
            float cpuUsage = 0;
            Process pro1,pro2;
            Runtime r = Runtime.getRuntime();
            try {
                String command = "cat /proc/stat";
                //第一次采集CPU时间
                long startTime = System.currentTimeMillis();
                pro1 = r.exec(command);
                BufferedReader in1 = new BufferedReader(new InputStreamReader(pro1.getInputStream()));
                String line;
                long idleCpuTime1 = 0, totalCpuTime1 = 0;   //分别为系统启动后空闲的CPU时间和总的CPU时间
                while((line=in1.readLine()) != null){
                    if(line.startsWith("cpu ")){
                        line = line.trim();
                        String[] temp = line.split("\\s+");
                        idleCpuTime1 = Long.parseLong(temp[4]);
                        for(String s : temp){
                            if(!s.equals("cpu")){
                                totalCpuTime1 += Long.parseLong(s);
                            }
                        }
                        break;
                    }
                }
                in1.close();
                pro1.destroy();
                try {
                    Thread.sleep(Server.collectInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //第二次采集CPU时间
                long endTime = System.currentTimeMillis();
                pro2 = r.exec(command);
                BufferedReader in2 = new BufferedReader(new InputStreamReader(pro2.getInputStream()));
                long idleCpuTime2 = 0, totalCpuTime2 = 0;   //分别为系统启动后空闲的CPU时间和总的CPU时间
                while((line=in2.readLine()) != null){
                    if(line.startsWith("cpu ")){
                        line = line.trim();
                        String[] temp = line.split("\\s+");
                        idleCpuTime2 = Long.parseLong(temp[4]);
                        for(String s : temp){
                            if(!s.equals("cpu")){
                                totalCpuTime2 += Long.parseLong(s);
                            }
                        }
                        break;
                    }
                }
                if(idleCpuTime1 != 0 && totalCpuTime1 !=0 && idleCpuTime2 != 0 && totalCpuTime2 !=0){
                    cpuUsage = 1 - (float)(idleCpuTime2 - idleCpuTime1)/(float)(totalCpuTime2 - totalCpuTime1);
                }
                in2.close();
                pro2.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
            array[1]=cpuUsage;
            latch.countDown();
        }
    }

    public static void refresh(float[] arr){
        CountDownLatch latch=new CountDownLatch(4);
        SystemUtil util=new SystemUtil(latch,arr);
        try{
            new Thread(util.new BandWidth()).start();
            new Thread(util.new CPU()).start();
            new Thread(util.new Disk()).start();
            new Thread(util.new Memory()).start();
            latch.await();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
