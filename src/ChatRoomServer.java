import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

class UserList{
    int index;
    String name;

    public UserList(int index,String name) {
        this.index = index;
        this.name = name;
    }

    public UserList(int index) {
        this.index = index;
    }

    public String toString() {
        return "用户" + index + ":" + name;
    }
}

class User {
    int index;
    Socket socket;
    String name;

    public User(int index, Socket socket) {
        this.index = index;
        this.socket = socket;
    }
}

class ServerThread implements Runnable {
    ArrayList<User> users;
    User user;
    ArrayList<UserList> userLists;
    UserList userList;
    File file = new File("D:\\Code\\java\\Java实验\\ChatRoom\\src\\List.txt");
  //  FileReader in;
    FileWriter out;

    public ServerThread(ArrayList<User> user, User u, ArrayList<UserList> userLists, UserList userList) {
        this.users = user;
        this.user = u;
        this.userList = userList;
        this.userLists = userLists;
    }

    public void run() {
        try {
            DataInputStream dis = new DataInputStream(user.socket.getInputStream());
            FileOutputStream fis = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fis, StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(writer);
            user.name = dis.readUTF();
            userList.name = user.name;
            System.out.println(user.name + "已连接");
            synchronized (users) {
                for (User u : users) {
                    DataOutputStream dos = new DataOutputStream(u.socket.getOutputStream());
                    dos.writeUTF("JOIN");
                    dos.writeUTF(user.name + "已连接");
                }
            }
            synchronized (users) {
                synchronized (file){
                    for (UserList ul : userLists) {
                        bw.write(ul.index+"\t"+ ul.name+"\r\n");
                    }
                }
            }
            bw.close();
            boolean flag = true;
            while (flag)//每读到一句 都转发给所有客户端
            {
                String type = dis.readUTF();
                System.out.println(type);
                if (type.equals("退出")) {
                    flag = false;
                    continue;
                }
                if(type.equals("PIC")){
                    print("PIC");
                    print(user.name);
                    Transmit(dis);
                    System.out.println("end");
                }
                if(type.equals("FILE")){
                    print("FILE");
                    print(user.name);
                    String filename = dis.readUTF();
                    print(filename);
                    Transmit(dis);
                }
                if(type.equals("STR")){
                    String line = dis.readUTF();
                    String msg = user.name + ":" + line;
                    System.out.println(msg);
                    print("STR");
                    print(msg);
                }
            }
            closeConnect();
        } catch (IOException e) {
            e.getStackTrace();
        }
    }

    private void Transmit(DataInputStream dis) throws IOException {
        int len;
        byte[] buffer = new byte[1024];
        int end = dis.readInt();
        synchronized (users) {
            for (int i = 0; i < users.size(); i++) {
                if (i != user.index) {
                    DataOutputStream dos = new DataOutputStream(users.get(i).socket.getOutputStream());
                    dos.writeInt(end);
                }
            }
        }
        len = lenJudge(dis, buffer);
        if(len == 1024 && end != 0){
            while(true){
                len = lenJudge(dis, buffer);
                if(len == end){
                    break;
                }
            }
        }
    }

    private int lenJudge(DataInputStream dis, byte[] buffer) throws IOException {
        int len;
        len = dis.read(buffer);
        synchronized (users) {
            for (int i = 0; i < users.size(); i++) {
                if (i != user.index) {
                    DataOutputStream dos = new DataOutputStream(users.get(i).socket.getOutputStream());
                    dos.write(buffer,0,len);
                }
            }
        }
        return len;
    }

    private void print(String msg) throws IOException {
        synchronized (users) {
            for (int i = 0; i < users.size(); i++) {
                if (i != user.index) {
                    DataOutputStream dos = new DataOutputStream(users.get(i).socket.getOutputStream());
                    dos.writeUTF(msg);
                }
            }
        }
    }

    public void closeConnect() throws IOException {
        FileOutputStream fis = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fis, StandardCharsets.UTF_8); //最后的"GBK"根据文件属性而定，如果不行，改成"UTF-8"试试
        BufferedWriter bw = new BufferedWriter(writer);
        PrintWriter pw = new PrintWriter(file);
        System.out.println(user.name + "已退出");


        synchronized (users) {
            for (User u : users) {
                DataOutputStream dos = new DataOutputStream(u.socket.getOutputStream());
                dos.writeUTF("EXIT");
                dos.writeUTF(user.name + "已退出");
            }
        }


        userLists.remove(userList);
        users.remove(user);
        for(User u:users){
            u.index = users.indexOf(u);
        }
        pw.write("");
        pw.flush();
        pw.close();
        synchronized (users) {
            synchronized (file){
                for (UserList ul : userLists) {
                    bw.write(ul.index + "\t" + ul.name+"\r\n");
                }
            }
        }
        bw.close();
        System.out.println(users);
        System.out.println(userLists);
        user.socket.close();
    }
}

public class ChatRoomServer {
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(8083);
        System.out.println("服务已启动 等待连接");
        ArrayList<User> users = new ArrayList<>();
        ArrayList<UserList> userLists = new ArrayList<>();
        boolean flag = true;
        while (flag) {
            try {
                Socket accept = ss.accept();
                User u = new User(users.size(), accept);
                UserList ul = new UserList(userLists.size());
                synchronized (users) {
                    users.add(u);
                    userLists.add(ul);
                }
                Thread thread = new Thread(new ServerThread(users, u, userLists, ul));
                thread.start();
            } catch (Exception e) {
                flag = false;
                e.printStackTrace();
            }
        }
    }
}