package client;

import resources.Phrases;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private boolean onlyOneThread = false;

    public Client(String host, int port) {
        this.port = port;
        this.host = host;
        reader = new BufferedReader(new InputStreamReader(System.in));
        user = new User(null, false);
        buffer = ByteBuffer.allocate(5 * 1024 * 1024);
    }

    public void start() {
        initial();
        System.out.println(Phrases.CLIENT_WELCOME.getPhrase());

        while (true) {
            try {
                if (selector.select() > 0) {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if (!key.isValid()) {
                            continue;
                        }

                        if (key.isConnectable()) {
                            connect(key);
                        }

                        if (key.isReadable()) {
                            read(key);
                        }

                        if (key.isWritable()) {
                            write(key);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(Phrases.CLIENT_WELCOME_ERROR.getPhrase());
            }
        }
    }

    private void initial() {
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(host, port));
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        buffer = ByteBuffer.allocate(1024);
        buffer.clear();

        try {
            if (channel.read(buffer) > 0) {
                String response = new String(buffer.array()).trim();
                System.out.println(response);
                if (!user.getNameStatus()) {
                    readMessage(response);
                    if (!response.equals(Phrases.SERVER_CONNECT_ERROR.getPhrase())) {
                        user.setUserName(userName);
                        user.setNameStatus(true);
                    }
                } else {
                    if (response.contains(Phrases.SEND_FILE.getPhrase())) {
                        receiveFile(response, channel);
                    } else {
                        if (response.toLowerCase().trim().equals(Phrases.SERVER_COMMAND_STOP_SERVER.getPhrase())) {
                            System.out.println(Phrases.SERVER_STOPPED.getPhrase());
                            System.out.println(Phrases.CLIENT_CLOSED.getPhrase());
                            System.exit(-1);
                        } else {
                            readMessage(response);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void receiveFile(String response, SocketChannel channel) throws IOException {
        readMessage(response);
        String[] words = response.split(" ");
        int count = 4;
        StringBuilder fileName = new StringBuilder();
        while (!words[count].equals("(")) {
            fileName.append(words[count]).append(" ");
            count++;
        }
        buffer = ByteBuffer.allocate(5 * 1024 * 1024);
        buffer.clear();
        channel.read(buffer);
        saveFile(fileName.toString().trim(), buffer.array());
    }

    private void saveFile(String fileName, byte[] bytes) {
        String directoryPath = createDirectory();
        File file = new File(directoryPath + File.separator + fileName);
        try {
            if (file.createNewFile()) {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(bytes);
                fileOutputStream.close();
                readMessage(Phrases.CLIENT_FILE_SAVED.getPhrase() + fileName);
            } else {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(bytes);
                fileOutputStream.close();
                readMessage(Phrases.CLIENT_FILE_OVERWRITING.getPhrase() + fileName);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private String createDirectory() {
        File directory = new File(System.getProperty("user.home") + File.separator +
                "Desktop" + File.separator + user.getUserName());
        if (!directory.exists()) {
            if (directory.mkdir()) {
                readMessage(Phrases.CLIENT_DIRECTORY_CREATED.getPhrase() + directory.getAbsolutePath());
            }
        }
        return directory.getAbsolutePath();
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
                String message = reader.readLine();
                if (message.split(" ", 2)[0].equals(Phrases.CLIENT_COMMAND_FILE.getPhrase())) {
                    sendFile(message.split(" ", 2)[1], channel);
                } else {
                    buffer = ByteBuffer.wrap((message.trim()).getBytes());
                    channel.write(buffer);
                    if (message.toLowerCase().trim().equals(Phrases.CLIENT_COMMAND_QUIT.getPhrase())) {
                        socketChannel.close();
                        key.cancel();
                        readMessage(Phrases.CLIENT_CLOSED.getPhrase());
                        System.exit(-1);
                    }
                }
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

    private void sendFile(String fileName, SocketChannel channel) throws IOException {
        File file = new File(fileName);
        if (file.length() > 5 * 1024 * 1024) {
            System.out.println(Phrases.CLIENT_INVALID_FILE_SIZE.getPhrase());
        } else {
            if (file.exists() && !file.isDirectory()) {
                byte[] fileBytes = Files.readAllBytes(Paths.get(file.getAbsolutePath().trim()));
                buffer = ByteBuffer.wrap((Phrases.CLIENT_COMMAND_FILE.getPhrase() + " " + fileBytes.length + " " + file.getName()).getBytes());
                channel.write(buffer);

                buffer = ByteBuffer.allocate(5 * 1024 * 1024);
                buffer.clear();
                buffer = ByteBuffer.wrap(fileBytes);
                channel.write(buffer);
                readMessage(Phrases.CLIENT_FILE_SENDING.getPhrase() + fileBytes.length + " bytes)");

            } else {
                System.out.println(Phrases.CLIENT_INVALID_FILE_ERROR.getPhrase());
            }
        }
    }

    private void readMessage(String response) {
        String[] partsMessage = response.split(" ", 2);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        String message = String.format("<%s>[%s]: %s", dateTimeFormatter.format(now), partsMessage[0], partsMessage[1]);
        System.out.println(message);
    }
}
