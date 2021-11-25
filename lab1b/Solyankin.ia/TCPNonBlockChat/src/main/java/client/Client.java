package client;

import resources.Phrases;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
    private String host;
    private int port;
    private User user;
    private BufferedReader reader;
    private Selector selector;
    private SocketChannel socketChannel;
    private ByteBuffer buffer;
    private String userName;

    public Client(String host, int port) {
        this.port = port;
        this.host = host;
        reader = new BufferedReader(new InputStreamReader(System.in));
        user = new User(null, false);
        buffer = ByteBuffer.allocate(1024);
    }

    public void start() {
        try {
            initial();
            System.out.println(Phrases.CLIENT_WELCOME.getPhrase());

            while (true) {
                selector.select(100);
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        System.out.println("Not valid");
                        continue;
                    }

                    if (key.isConnectable()) {
                        System.out.println("connect");
                        connect(key);
                    }

                    if (key.isReadable()) {
                        System.out.println("read");
                        read(key);
                    }

                    if (key.isWritable()) {
                        System.out.println("write");
                        write(key);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(Phrases.CLIENT_WELCOME_ERROR.getPhrase());
        }
    }

    private void initial() throws IOException {
        selector = Selector.open();
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(host, port));
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
    }

    private void connect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.isConnectionPending()) {
            channel.finishConnect();
        }
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        if (!user.getNameStatus()) {
            try {
                channel.read(buffer);
                String response = new String(buffer.array());
                System.out.println(response);
                readMessage(response);
                if (!response.equals(Phrases.SERVER_CONNECT_ERROR.getPhrase())) {
                    user.setUserName(userName);
                    user.setNameStatus(true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    private void write(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            buffer = ByteBuffer.allocate(1024);
            buffer.clear();

            if (!user.getNameStatus()) {
                enterUserName();
                buffer = ByteBuffer.wrap((userName.trim()).getBytes());
                channel.write(buffer);
            } else {

            }
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            readMessage("Client " + Phrases.SEND_MESSAGE_ERROR.getPhrase());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void enterUserName() {
        try {
            System.out.println(Phrases.CLIENT_ENTER_NAME.getPhrase());
            userName = reader.readLine();
            boolean availableUserName = false;
            while (!availableUserName) {
                if (userNameIsSuitable(userName)) {
                    availableUserName = true;
                } else {
                    userName = reader.readLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean userNameIsSuitable(String userName) {
        String regex = "[A-Za-z0-9\\n\\r\\t]*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(userName);
        if (matcher.matches()) {
            return true;
        } else {
            readMessage(Phrases.CLIENT_INCORRECT_NAME_ERROR.getPhrase());
            return false;
        }
    }

    public void readMessage(String response) {
        String[] partsMessage = response.split(" ", 2);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        String message = String.format("<%s>[%s]: %s", dateTimeFormatter.format(now), partsMessage[0], partsMessage[1]);
        System.out.println(message);
    }
}
