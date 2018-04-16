package FTP;

import Client.Uploader;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.io.InputStream;

public class FTPUtil {
    /**
     * @param hostname
     * FTP服务器地址
     * @param port
     * FTP服务器端口号
     * @param username
     * FTP登陆用户名
     * @param password
     * FTP登陆密码
     * @param pathname
     * FTP服务器保存目录
     * @param fileName
     * 上传到FTP服务器后文件名称
     * @param inputStream
     * 输入文件流
     */
    public static boolean uploadFile(String hostname, int port, String username, String password, String pathname, String fileName, InputStream inputStream){
        boolean flag=false;
        FTPClient ftpClient=new FTPClient();
        ftpClient.setControlEncoding("UTF-8");
        try{
            ftpClient.connect(hostname,port);
            ftpClient.login(username,password);
            int replyCode=ftpClient.getReplyCode();
            if(!FTPReply.isPositiveCompletion(replyCode)){
                return flag;
            }
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.makeDirectory(pathname);
            ftpClient.changeWorkingDirectory(pathname);
            ftpClient.storeFile(fileName,inputStream);
            inputStream.close();
            ftpClient.logout();
            flag=true;
        }catch(IOException e){
            //连接异常，需要重新请求可用FTP服务器列表
            Uploader.uploader.failover();
        }finally{
            if(ftpClient.isConnected()){
                try {
                    ftpClient.disconnect();
                }catch(IOException e){
                    //断开连接异常，处理
                }
            }
        }
        return flag;
    }
}
