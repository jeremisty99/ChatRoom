import com.vdurmont.emoji.EmojiParser;
import org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper;
import org.jb2011.lnf.beautyeye.ch3_button.BEButtonUI;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

class PrintThread implements Runnable {
    public DataInputStream in;
    public boolean exit = false;
    public StyledDocument doc;
    public JTextPane TextOutput;
    public String name;

    public PrintThread(DataInputStream in, StyledDocument doc, JTextPane TextOutput,String name) {
        this.in = in;
        this.doc = doc;
        this.TextOutput = TextOutput;
        this.name = name;
    }

    public void run() {
        try {
            int pcount = 0;
            while (!exit) {
                String type = in.readUTF();
                System.out.println(type);
                if (type.equals("PIC")) {
                    String name = in.readUTF();
                    File path = new File(this.name);
                    if (!path.exists()) {
                        path.mkdir();
                    }
                    File img = new File(path + "/" + pcount + ".jpg");
                    System.out.println("img");
                    FileOutputStream fos = new FileOutputStream(img);
                    System.out.println(img.getPath());
                    byte[] buffer = new byte[1024];
                    int len;
                    int end = in.readInt();
                    System.out.println(end);
                    len = in.read(buffer);
                    fos.write(buffer, 0, len);
                    System.out.println(len);
                    if(len == 1024 && end != 0){
                        while (true) {
                            len = in.read(buffer);
                            fos.write(buffer, 0, len);
                            System.out.println(len);
                            if(len == end){
                                break;
                            }
                        }
                    }
                    SimpleAttributeSet Left = new SimpleAttributeSet();
                    StyleConstants.setForeground(Left, new Color(0, 0, 109));
                    StyleConstants.setAlignment(Left, StyleConstants.ALIGN_LEFT);
                    doc.setParagraphAttributes(doc.getLength(), 1, Left, false);
                    try {
                        doc.insertString(doc.getLength(), name + ":", Left);
                        TextOutput.setCaretPosition(doc.getLength()); // 设置插入位置
                        TextOutput.insertIcon(new ImageIcon(img.getPath())); //插入图片
                        doc.insertString(doc.getLength(), "\n", Left);
                    } catch (BadLocationException e) {
                        e.getStackTrace();
                    }
                    pcount++;
                }
                if (type.equals("EXIT") | type.equals("JOIN")) {
                    String input = in.readUTF();
                    SimpleAttributeSet Center = new SimpleAttributeSet();
                    StyleConstants.setForeground(Center, new Color(50, 50, 50));
                    StyleConstants.setAlignment(Center, StyleConstants.ALIGN_CENTER);
                    doc.setParagraphAttributes(doc.getLength(), 1, Center, false);
                    try {
                        doc.insertString(doc.getLength(), input + "\n", Center);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }

                if (type.equals("FILE")) {
                    String name = in.readUTF();
                    String filename = in.readUTF();
                    File path = new File(this.name);
                    if (!path.exists()) {
                        path.mkdir();
                    }
                    File file = new File(path + "/" + filename);
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int len;
                    int end = in.readInt();
                    System.out.println(end);
                    len = in.read(buffer);
                    fos.write(buffer, 0, len);
                    System.out.println(len);
                    if(len == 1024 && end != 0){
                        while (true) {
                            len = in.read(buffer);
                            fos.write(buffer, 0, len);
                            System.out.println(len);
                            if(len == end){
                                break;
                            }
                        }
                    }
                    JButton ButtonFile = new JButton(filename);
                    ButtonFile.setIcon(new ImageIcon("D:\\Code\\java\\Java实验\\ChatRoom\\src\\file.png"));
                    ButtonFile.setBackground(Color.white);
                    ButtonFile.addActionListener(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            try {
                                Desktop.getDesktop().open(file);
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }
                    });
                    SimpleAttributeSet Left = new SimpleAttributeSet();
                    StyleConstants.setForeground(Left, new Color(0, 0, 109));
                    StyleConstants.setAlignment(Left, StyleConstants.ALIGN_LEFT);
                    doc.setParagraphAttributes(doc.getLength(), 2, Left, false);
                    try {//插入文本
                        doc.insertString(doc.getLength(), name + ":文件" + filename + "已接收\n", Left);
                        TextOutput.setCaretPosition(doc.getLength()); // 设置插入位置
                        TextOutput.insertComponent(ButtonFile);
                        doc.insertString(doc.getLength(), "\n", Left);
                    } catch (BadLocationException e) {
                        e.getStackTrace();
                    }
                }

                if (type.equals("STR")) {
                    String input = in.readUTF();
                    SimpleAttributeSet Left = new SimpleAttributeSet();
                    StyleConstants.setForeground(Left, new Color(0, 0, 109));
                    StyleConstants.setAlignment(Left, StyleConstants.ALIGN_LEFT);
                    doc.setParagraphAttributes(doc.getLength(), 1, Left, false);
                    try {
                        doc.insertString(doc.getLength(), input + "\n", Left);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


public class ChatRoomClient {
    private ArrayList<UserList> userLists;
    private Socket s;
    private DataInputStream in;
    private DataOutputStream out;
    private PrintThread pt;
    private File file = new File("D:\\Code\\java\\Java实验\\ChatRoom\\src\\List.txt");
    private ScheduledFuture taskFuture;
    private long lastSize = 0;
    private JFrame ClientJFrame;
    private JPanel LinkPanel;
    private JPanel InputPanel;
    private JList userList;
    private JTextPane TextOutput;
    private JLabel LabelPort = new JLabel("端口");
    private JLabel LabelHostIp = new JLabel("服务器IP");
    private JLabel LabelName = new JLabel("姓名");
    private JTextField TextInput;
    private JTextField TextPort;
    private JTextField TextHostIp;
    private JTextField TextName;
    private JButton ButtonStart;
    private JButton ButtonStop;
    private JButton ButtonSend;
    private JButton ButtonPicture;
    private JButton ButtonFile;
    private JButton ButtonEmoji;
    private JWindow WindowEmoji;
    private JScrollPane rightScroll;
    private JScrollPane leftScroll;
    private JSplitPane centerSplit;
    private DefaultListModel listModel;
    private boolean isConnected = false;
    private StyledDocument doc = null;

    public static void main(String[] args) {
        InitGlobalFont(new Font("微软雅黑", Font.PLAIN, 15));
        new ChatRoomClient();
    }

    public void send() {
        String message = TextInput.getText().trim();
        if (message == null || message.equals("")) {
            JOptionPane.showMessageDialog(ClientJFrame, "消息不能为空！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            out.writeUTF("STR");
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String name = TextName.getText();
        SimpleAttributeSet Right = new SimpleAttributeSet();
        StyleConstants.setForeground(Right, new Color(139, 0, 0));
        StyleConstants.setAlignment(Right, StyleConstants.ALIGN_RIGHT);
        doc.setParagraphAttributes(doc.getLength(), 1, Right, false);
        try {//插入文本
            doc.insertString(doc.getLength(), name + ":" + message + "\n", Right);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        TextInput.setText(null);
    }

    public void sendEmoji(String emoji) {
        try {
            out.writeUTF("STR");
            out.writeUTF(emoji);
            String name = TextName.getText();
            SimpleAttributeSet Right = new SimpleAttributeSet();
            StyleConstants.setForeground(Right, new Color(139, 0, 0));
            StyleConstants.setAlignment(Right, StyleConstants.ALIGN_RIGHT);
            doc.setParagraphAttributes(doc.getLength(), 1, Right, false);
            try {//插入文本
                doc.insertString(doc.getLength(), name + ":" + emoji + "\n", Right);
            } catch (BadLocationException f) {
                f.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sendPicture(File Picturefile) {
        try {
            out.writeUTF("PIC");
            FileInputStream fis = new FileInputStream(Picturefile);
            byte[] buffer = new byte[1024];
            int len;
            long l = Picturefile.length();
            out.writeInt(Math.toIntExact(l % 1024));
            while (((len = fis.read(buffer)) > 0)){
                out.write(buffer, 0, len);
                System.out.println(len);
            }
            insertIcon(Picturefile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sendFile(File file) {
        try {
            out.writeUTF("FILE");
            out.writeUTF(file.getName());

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            long l = file.length();
            out.writeInt(Math.toIntExact(l % 1024));
            while (((len = fis.read(buffer)) > 0)){
                out.write(buffer, 0, len);
                System.out.println(len);
            }
            JButton ButtonFile = new JButton(file.getName());
            ButtonFile.setIcon(new ImageIcon("D:\\Code\\java\\Java实验\\实验七\\src\\file.png"));
            ButtonFile.setBackground(Color.white);
            ButtonFile.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        Desktop.getDesktop().open(file);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
            });
            SimpleAttributeSet Right = new SimpleAttributeSet();
            StyleConstants.setForeground(Right, new Color(139, 0, 0));
            StyleConstants.setAlignment(Right, StyleConstants.ALIGN_RIGHT);
            doc.setParagraphAttributes(doc.getLength(), 2, Right, false);
            try {//插入文本
                doc.insertString(doc.getLength(), TextName.getText() + ":文件" + file.getName() + "已发送\n", Right);
                TextOutput.setCaretPosition(doc.getLength()); // 设置插入位置
                TextOutput.insertComponent(ButtonFile);
                doc.insertString(doc.getLength(), "\n", Right);
            } catch (BadLocationException f) {
                f.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ChatRoomClient() {
        try
        {
            BeautyEyeLNFHelper.frameBorderStyle = BeautyEyeLNFHelper.FrameBorderStyle.translucencyAppleLike;
            org.jb2011.lnf.beautyeye.BeautyEyeLNFHelper.launchBeautyEyeLNF();
            UIManager.put("RootPane.setupButtonVisible", false);
        }
        catch(Exception e)
        {
            //TODO exception
        }
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
//            e.printStackTrace();
//        }
        ClientJFrame = new JFrame("客户端");
        ClientJFrame.setLayout(new BorderLayout());
        TextOutput = new JTextPane();
        TextOutput.setEditable(false);
        TextInput = new JTextField();
        TextPort = new JTextField("8083");
        TextHostIp = new JTextField("127.0.0.1");
        TextName = new JTextField("杰瑞");
        ButtonStart = new JButton("连接");
        ButtonStart.setUI(new BEButtonUI().setNormalColor(BEButtonUI.NormalColor.green));
        ButtonStop = new JButton("断开");
        ButtonStop.setUI(new BEButtonUI().setNormalColor(BEButtonUI.NormalColor.red));
        ButtonStop.setEnabled(false);
        ButtonSend = new JButton("发送");
        ButtonSend.setUI(new BEButtonUI().setNormalColor(BEButtonUI.NormalColor.lightBlue));
        ButtonPicture = new JButton("图片");
        ButtonEmoji = new JButton("表情");
        ButtonFile = new JButton("文件");
        LinkPanel = new JPanel();
        GridBagLayout GBL = new GridBagLayout();
        LinkPanel.setLayout(GBL);
        GridBagConstraints style = new GridBagConstraints();
        GridBagConstraints style2 = new GridBagConstraints();
        style.fill = GridBagConstraints.BOTH;
        style.gridwidth = 1;//该方法是设置组件水平所占用的格子数，如果为0，就说明该组件是该行的最后一个
        style.weightx = 1;//该方法设置组件水平的拉伸幅度，如果为0就说明不拉伸，不为0就随着窗口增大进行拉伸，0到1之间
        style.weighty = 0;//该方法设置组件垂直的拉伸幅度，如果为0就说明不拉伸，不为0就随着窗口增大进行拉伸，0到1之间
        style.insets = new Insets(0, 5, 0, 5);
        style2.fill = GridBagConstraints.BOTH;
        style2.gridwidth = 0;
        style2.weightx = 1;
        style2.weighty = 0;
        style2.insets = new Insets(0, 5, 0, 5);
        GBL.setConstraints(LabelPort, style);
        GBL.setConstraints(TextPort, style);
        GBL.setConstraints(LabelHostIp, style);
        GBL.setConstraints(TextHostIp, style);
        GBL.setConstraints(LabelName, style);
        GBL.setConstraints(TextName, style);
        GBL.setConstraints(ButtonStart, style);
        GBL.setConstraints(ButtonStop, style2);
        LinkPanel.add(LabelPort);
        LinkPanel.add(TextPort);
        LinkPanel.add(LabelHostIp);
        LinkPanel.add(TextHostIp);
        LinkPanel.add(LabelName);
        LinkPanel.add(TextName);
        LinkPanel.add(ButtonStart);
        LinkPanel.add(ButtonStop);
        LinkPanel.setBorder(new TitledBorder("连接信息"));
        rightScroll = new JScrollPane(TextOutput);
        rightScroll.setBorder(new TitledBorder("消息显示区"));
        listModel = new DefaultListModel();
        userList = new JList(listModel);
        leftScroll = new JScrollPane(userList);
        leftScroll.setBorder(new TitledBorder("在线用户"));
        InputPanel = new JPanel(GBL);
        GridBagConstraints style3 = new GridBagConstraints();
        style3.fill = GridBagConstraints.BOTH;
        style3.gridwidth = 10;
        style3.weightx = 1;
        style3.weighty = 0;
        style3.insets = new Insets(0, 5, 0, 5);
        GridBagConstraints style4 = new GridBagConstraints();
        style4.fill = GridBagConstraints.BOTH;
        style4.gridwidth = 1;
        style4.weightx = 0;
        style4.weighty = 0;
        style4.insets = new Insets(0, 5, 0, 5);
        GridBagConstraints style5 = new GridBagConstraints();
        style5.fill = GridBagConstraints.BOTH;
        style5.gridwidth = 0;
        style5.weightx = 0;
        style5.weighty = 0;
        style5.insets = new Insets(0, 5, 0, 5);
        GBL.setConstraints(TextInput, style3);
        GBL.setConstraints(ButtonEmoji, style4);
        GBL.setConstraints(ButtonPicture, style4);
        GBL.setConstraints(ButtonFile, style4);
        GBL.setConstraints(ButtonSend, style5);
        InputPanel.add(TextInput);
        InputPanel.add(ButtonFile);
        InputPanel.add(ButtonPicture);
        InputPanel.add(ButtonEmoji);
        InputPanel.add(ButtonSend);
        doc = TextOutput.getStyledDocument();
        WindowEmoji = new JWindow(ClientJFrame);
        JPanel JPanelEmoji = new JPanel(new GridLayout(2, 2));
        String[] str = {":smile:", ":cry:", ":grinning:", ":wink:"};
        for (int i = 0; i < 4; i++) {
            String e = EmojiParser.parseToUnicode(str[i]);
            JButton E = new JButton(e);
            E.setBorderPainted(false);
            E.setMargin(new Insets(-10, -10, -10, -10));
            E.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 25));
            E.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent f) {
                    if (isConnected) {
                        sendEmoji(e);
                    } else {
                        JOptionPane.showMessageDialog(ClientJFrame, "未连接！", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            JPanelEmoji.add(E);
        }
        WindowEmoji.add(JPanelEmoji);
        WindowEmoji.setSize(100, 100);
        WindowEmoji.setVisible(false);
        InputPanel.setBorder(new TitledBorder("消息输入"));
        centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        centerSplit.setDividerLocation(130);
        ClientJFrame.add(LinkPanel, "North");
        ClientJFrame.add(centerSplit, "Center");
        ClientJFrame.add(InputPanel, "South");
        ClientJFrame.setSize(640, 480);
        ClientJFrame.setLocationRelativeTo(null);
        ClientJFrame.setVisible(true);

        // 写消息的文本框中按回车键时事件
        TextInput.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });

        // 单击表情按钮时事件
        ButtonEmoji.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                WindowEmoji.setLocation(ClientJFrame.getLocation().x + ClientJFrame.getBounds().width - 150, ClientJFrame.getLocation().y + ClientJFrame.getBounds().height - 185);
                WindowEmoji.setVisible(!WindowEmoji.isVisible());
            }
        });

        // 单击文件按钮时事件
        ButtonFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser F = new JFileChooser(new File("D:\\Code\\java\\Java实验\\ChatRoom\\src"));
                int value = F.showOpenDialog(null);
                if (value == JFileChooser.APPROVE_OPTION) {
                    if (isConnected) {
                        sendFile(F.getSelectedFile());
                    } else {
                        JOptionPane.showMessageDialog(ClientJFrame, "未连接！", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        // 单击图片按钮时事件
        ButtonPicture.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser F = new JFileChooser(new File("D:\\Code\\java\\Java实验\\实验七\\src"));
                F.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        String name = f.getName();
                        return f.isDirectory() || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png");
                    }

                    @Override
                    public String getDescription() {
                        return "*.jpg;*.png";
                    }
                });
                int value = F.showOpenDialog(null);
                if (value == JFileChooser.APPROVE_OPTION) {
                    if (isConnected) {
                        sendPicture(F.getSelectedFile());
                    } else {
                        JOptionPane.showMessageDialog(ClientJFrame, "未连接！", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // 单击发送按钮时事件
        ButtonSend.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isConnected) {
                    send();
                } else {
                    JOptionPane.showMessageDialog(ClientJFrame, "未连接！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 单击连接按钮时事件
        ButtonStart.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String host = TextHostIp.getText();
                String name = TextName.getText();
                int port = Integer.parseInt(TextPort.getText());
                try {
                    userLists = new ArrayList<>();
                    s = new Socket(host, port);
                    in = new DataInputStream(s.getInputStream());
                    out = new DataOutputStream(s.getOutputStream());
                    pt = new PrintThread(in, doc, TextOutput,name);
                    out.writeUTF(name);
                    ButtonStart.setEnabled(false);
                    ButtonStop.setEnabled(true);
                    isConnected = true;

                } catch (IOException f) {
                    JOptionPane.showMessageDialog(ClientJFrame, "连接失败！", "错误", JOptionPane.ERROR_MESSAGE);
                }


                Thread thread = new Thread(pt);
                thread.start();

                final RandomAccessFile randomAccessFile;
                try {
                    randomAccessFile = new RandomAccessFile(file, "rw");
                    ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
                    taskFuture = exec.scheduleWithFixedDelay(new Runnable() {
                        public void run() {
                            try {
                                String tmp = "";
                                if (lastSize != randomAccessFile.length()) {
                                    userLists.clear();
                                    randomAccessFile.seek(0);
                                    while ((tmp = randomAccessFile.readLine()) != null) {
                                        String line = new String(tmp.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                                        String[] str = line.split("\t");
                                        UserList u = new UserList(Integer.parseInt(str[0]), str[1]);
                                        //System.out.println(u);
                                        userLists.add(u);
                                    }
                                    //System.out.println(userLists);
                                    userList.setListData(userLists.toArray());
                                }
                                lastSize = randomAccessFile.length();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, 0, 500, TimeUnit.MICROSECONDS);
                } catch (FileNotFoundException fileNotFoundException) {
                    fileNotFoundException.printStackTrace();
                }
                //启动一个线程每500ms读取一次存储在线成员列表的文件
            }
        });

        // 单击断开按钮时事件
        ButtonStop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pt.exit = true;
                if(isConnected){
                    try {
                        out.writeUTF("退出");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }
                isConnected = false;
                taskFuture.cancel(true);
                lastSize = 0;
                userLists.clear();
                userList.setListData(userLists.toArray());
                ButtonStart.setEnabled(true);
                ButtonStop.setEnabled(false);
            }
        });

        //点击关闭时的事件
        ClientJFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if(isConnected){
                    pt.exit = true;
                    try {
                        out.writeUTF("退出");
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                    taskFuture.cancel(true);
                }
                super.windowClosing(e);
                System.exit(0);
            }
        });
    }

    public static void InitGlobalFont(Font fnt) {
        FontUIResource fontRes = new FontUIResource(fnt);
        for (Enumeration keys = UIManager.getDefaults().keys(); keys.hasMoreElements(); ) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource)
                UIManager.put(key, fontRes);
        }
    }

    private void insertIcon(File file) {
        TextOutput.setCaretPosition(doc.getLength()); // 设置插入位置
        SimpleAttributeSet Right = new SimpleAttributeSet();
        StyleConstants.setForeground(Right, new Color(139, 0, 0));
        StyleConstants.setAlignment(Right, StyleConstants.ALIGN_RIGHT);
        doc.setParagraphAttributes(doc.getLength(), 1, Right, false);
        try {//插入文本
            doc.insertString(doc.getLength(), TextName.getText() + ":", Right);
            TextOutput.insertIcon(new ImageIcon(file.getPath())); //插入图片
            doc.insertString(doc.getLength(), "\n", Right);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}







