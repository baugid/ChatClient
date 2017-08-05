import javax.swing.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class MainGui {

    private JTextArea output;
    private JPanel panel;
    private JTextField input;
    private JButton send;
    private String name;
    private Socket soc = null;
    private BufferedReader in = null;
    private PrintStream out = null;
    private Thread receiverThread;

    private MainGui(Socket soc) {
        this.soc = soc;
        //Init the Streamreader and writer
        try {
            this.in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            this.out = new PrintStream(soc.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        name = getName();
        //receive messages in another thread
        receiverThread = new Thread(this::receiveMessages);
        receiverThread.start();
        //send message
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (input.getText() != null && !input.getText().equals("")) {
                    out.println("[" + name + "]:" + input.getText());
                    input.setText("");
                }
            }
        });
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && input.getText() != null && !input.getText().equals("")) {
                    out.println("[" + name + "]:" + input.getText());
                    input.setText("");
                }
            }
        });
    }

    private String getName() {
        //ask for valid name
        do {
            //fetch name
            do {
                name = null;
                name = JOptionPane.showInputDialog("Enter your display name: ");
            } while (name == null || name.equals(""));
            //send name
            out.println(name);
            //check if name was accepted
            try {
                if (!in.readLine().equals("accepted!")) {
                    JOptionPane.showMessageDialog(null, "Please enter a different name.", "Error", JOptionPane.ERROR_MESSAGE);
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return name;
        } while (true);
    }

    public static void main(String[] args) {
        Socket soc = null;
        int port;
        do {
            String[] address = getAddress();
            //check if port is an integer
            port = extractPort(address[1]);
            if (port == -1) {
                continue;
            }
            //try to open socket
            soc = openSocket(address[0], port);
        } while (soc == null);
        openWindow(soc);
    }

    public static void openWindow(Socket soc) {
        MainGui mG = new MainGui(soc);
        JFrame frame = new JFrame("Chat");
        frame.setContentPane(mG.panel);
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        //call the closingHandler on close
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mG.closingHandler();
            }
        });
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static String[] getAddress() {
        boolean anotherRun;
        String[] address;
        do {
            anotherRun = false;
            address = JOptionPane.showInputDialog("Enter IP and Port\n(e.g. \"127.0.0.1:1234\")").split(":");
            //check for correct length
            if (address.length != 2) {
                JOptionPane.showMessageDialog(null, "Please use the correct Format!", "Error", JOptionPane.ERROR_MESSAGE);
                anotherRun = true;
            }
        } while (anotherRun);
        return address;
    }

    public static int extractPort(String portString) {
        int port;
        //try to parse port
        try {
            port = Integer.parseInt(portString);
        } catch (NumberFormatException e) {
            //Inform user and caller about error
            JOptionPane.showMessageDialog(null, "The port must be a number!", "Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
        return port;
    }

    public static Socket openSocket(String IP, int port) {
        try {
            //try to open socket
            return new Socket(IP, port);
        } catch (IOException e) {
            //inform about error
            JOptionPane.showMessageDialog(null, "Can't connect!\nPlease enter valid information!", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private void receiveMessages() {
        String message = "";
        while (!receiverThread.isInterrupted()) {
            //try to read a message
            try {
                message = in.readLine();
            } catch (IOException e) {
                closingHandler();
            }
            //if message equals disconnect close application
            if (message.equals("disconnect!")) {
                closingHandler();
                return;
            }
            //update text
            output.setText(output.getText() + message + "\n");
        }
    }

    public void closingHandler() {
        if (!receiverThread.isInterrupted()) {
            receiverThread.interrupt();
            out.println("closing!");
            //close socket and streams
            try {
                soc.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            out.close();
            //exit
            System.exit(0);
        }
    }
}
