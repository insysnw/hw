package threads;

import client.User;
import resources.Phrases;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;

public class ServerThread extends Thread {
    private final String host;
    private final int port;
    private final ArrayList<String> userNames;
    private final ArrayList<SocketChannel> acceptedUsers;
    private ByteBuffer buffer;
    private String userName;

    public ServerThread(String host, int port) {
        this.port = port;
        this.host = host;
        userNames = new ArrayList<>();
        acceptedUsers = new ArrayList<>();
        buffer = ByteBuffer.allocate(5 * 1024 * 1024);
    }

    @Override
    public void run() {
        try {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(host, port));
            Selector selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println(Phrases.SERVER_WELCOME.getPhrase() + port);

            while (server.isOpen()) {
                int channels = selector.select();
                if (channels == 0) {
                    continue;
                }

                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        accept(key);
                    }

                    if (key.isReadable()) {
                        read(key);
                    }
                }
            }
            try {
                selector.close();
                for (SocketChannel channel : acceptedUsers) {
                    channel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println(Phrases.SERVER_STOPPED.getPhrase());
        }
    }

    private void accept(SelectionKey key) {
        try {
            ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
            SocketChannel socketChannel = ssChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(key.selector(), SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key) {
        SocketChannel socketChannel = null;
        try {
            socketChannel = (SocketChannel) key.channel();
            buffer = ByteBuffer.allocate(1024);
            buffer.clear();
            if (socketChannel.read(buffer) > 0) {
                if (!acceptedUsers.contains(socketChannel)) {
                    User user = new User(null, false);
                    while (!user.getNameStatus()) {
                        userName = new String(buffer.array()).trim();
                        if (!userNames.contains(userName)) {
                            user = new User(userName, true);
                            acceptedUsers.add(socketChannel);
                            userNames.add(userName);
                            System.out.println(String.format("<%s>[%s]:%s", getCurrentTime(), userName, Phrases.USER_JOINED.getPhrase()));
                            broadcast(userName + Phrases.USER_JOINED.getPhrase());
                        } else {
                            buffer = ByteBuffer.allocate(1024);
                            buffer.clear();
                            buffer = ByteBuffer.wrap(Phrases.SERVER_CONNECT_ERROR.getPhrase().getBytes());
                            socketChannel.write(buffer);
                            break;
                        }
                    }
                } else {
                    String clientMessages = new String(buffer.array()).trim();
                    String[] messageParts = clientMessages.split(" ", 2);
                    userName = messageParts[0];
                    String userMessage = messageParts[1];
                    if (!userMessage.trim().toLowerCase().equals(Phrases.CLIENT_COMMAND_QUIT.getPhrase())) {
                        if (userMessage.split(" ", 3)[0].equals(Phrases.CLIENT_COMMAND_FILE.getPhrase())) {
                            fileProcessing(userMessage, socketChannel);
                        } else {
                            broadcast(userName + " " + userMessage);
                            String message = String.format("<%s>[%s]: %s", getCurrentTime(), userName, userMessage);
                            System.out.println(message);
                        }
                    } else {
                        socketChannel.close();
                        key.cancel();
                        removeUser(userName, socketChannel);
                        System.out.println(String.format("<%s>[%s]:%s", getCurrentTime(), userName, Phrases.USER_QUITED.getPhrase()));
                        broadcast(userName + Phrases.USER_QUITED.getPhrase());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(String.format("<%s>[%s]:%s", getCurrentTime(), userName, Phrases.USER_QUITED.getPhrase()));
            try {
                socketChannel.close();
                key.cancel();
                removeUser(userName, socketChannel);
                broadcast(userName + Phrases.USER_QUITED.getPhrase());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void fileProcessing(String clientMessages, SocketChannel channel) throws IOException {
        String[] messageParts = clientMessages.split(" ");
        Integer nameLength = Integer.valueOf(messageParts[1]);
        StringBuilder fileName = new StringBuilder();
        int wordIndex = 2;
        while (fileName.length() < nameLength) {
            fileName.append(messageParts[wordIndex++]).append(" ");
        }
        byte[] fileBytes = new byte[Integer.parseInt(messageParts[wordIndex])];
        System.out.println(String.format("<%s>[%s]: %s", getCurrentTime(), userName, Phrases.SEND_FILE.getPhrase() + fileName));
        broadcast(userName + " " + Phrases.SEND_FILE.getPhrase() + fileName + " ( " + fileBytes.length + " bytes) ");
        buffer = ByteBuffer.allocate(fileBytes.length);
        buffer.clear();
        while (buffer.position() < fileBytes.length) {
            channel.read(buffer);
        }
        fileBytes = buffer.array();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        broadcast(fileBytes);
    }

    private static String getCurrentTime() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        return dateTimeFormatter.format(now);
    }

    private void broadcast(String message) throws IOException {
        for (SocketChannel channel : acceptedUsers) {
            buffer = ByteBuffer.allocate(1024);
            buffer.clear();
            buffer = ByteBuffer.wrap(message.getBytes());
            channel.write(buffer);
        }
    }

    private void broadcast(byte[] bytes) throws IOException {
        for (SocketChannel channel : acceptedUsers) {
            buffer = ByteBuffer.allocate(bytes.length);
            buffer.clear();
            buffer = ByteBuffer.wrap(bytes);
            channel.write(buffer);
        }
    }

    private void removeUser(String userName, SocketChannel socketChannel) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            acceptedUsers.remove(socketChannel);
        }
    }

    public ArrayList<String> getUserNames() {
        return userNames;
    }

    public void closeServerThread() {
        try {
            for (SocketChannel socketChannel : acceptedUsers) {
                buffer = ByteBuffer.allocate(Phrases.SERVER_COMMAND_STOP_SERVER.getPhrase().length());
                buffer.clear();
                buffer = ByteBuffer.wrap(Phrases.SERVER_COMMAND_STOP_SERVER.getPhrase().getBytes());
                socketChannel.write(buffer);
            }
            System.out.println(Phrases.SERVER_STOPPED.getPhrase());
            userNames.clear();
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
