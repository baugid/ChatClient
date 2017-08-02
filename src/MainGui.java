import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
        boolean error;
        //Init the Streamreader and writer
        try {
            this.in = new BufferedReader(new InputStreamReader(soc.getInputStream()));
            this.out = new PrintStream(soc.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //ask for valid name
        do {
            error = false;
            //fetch name
            do {
                name=null;
                name = JOptionPane.showInputDialog("Enter your display name: ");
            } while (name == null || name.equals(""));
            //send name
            out.println(name);
            //check if name was accepted
            try {
                if (!in.readLine().equals("accepted!")) {
                    error = true;
                    JOptionPane.showMessageDialog(null, "Please enter a different name.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while (error);

        //receive messages in another thread
        receiverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                receiveMessages();
            }
        });
        //send message
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!input.getText().equals("")) {
                    out.println("[" + name + "]:" + input.getText());
                    input.setText("");
                }
            }
        });
        receiverThread.start();
    }

    public static void main(String[] args) {
        boolean error;
        Socket soc = null;
        int port = 0;
        do {
            error = false;
            String[] ipPort = JOptionPane.showInputDialog("Enter IP and Port\n(e.g. \"127.0.0.1:1234\")").split(":");
            //check for correct length
            if (ipPort.length != 2) {
                JOptionPane.showMessageDialog(null, "Please use the correct Format!", "Error", JOptionPane.ERROR_MESSAGE);
                error = true;
                continue;
            }
            //check if port is an integer
            try {
                port = Integer.parseInt(ipPort[1]);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "The port must be a number!", "Error", JOptionPane.ERROR_MESSAGE);
                error = true;
                continue;
            }
            //try to open socket
            try {
                soc = new Socket(ipPort[0], port);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Can't connect!\nPlease enter valid information!", "Error", JOptionPane.ERROR_MESSAGE);
                error = true;
            }
        }
        while (error);

        MainGui mG = new MainGui(soc);
        JFrame frame = new JFrame("Chat");
        frame.setContentPane(mG.panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

    public void receiveMessages() {
        String message = "";
        while (true) {
            //try to read a message
            try {
                message = in.readLine();
            } catch (IOException e) {
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
