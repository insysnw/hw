package threads;

import client.User;
import resources.Phrases;

import java.io.IOException;
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
    private final ServerSocketChannel server;
    private final Selector selector;
    private final ArrayList<String> userNames;
    private final ArrayList<SocketChannel> acceptedUsers;
    private ByteBuffer buffer;
    private String userName;
    private User user;

    public ServerThread(ServerSocketChannel server, Selector selector) {
        this.server = server;
        this.selector = selector;
        userNames = new ArrayList<>();
        acceptedUsers = new ArrayList<>();
        buffer = ByteBuffer.allocate(123456789);
    }

    @Override
    public void run() {
        while (server.isOpen()) {
            try {
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
            } catch (IOException e) {
                System.out.println(Phrases.SERVER_STOPPED.getPhrase());
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
    }

    private void accept(SelectionKey key) {
        ServerSocketChannel ssChannel = (ServerSocketChannel) key.channel();
        try {
            SocketChannel socketChannel = ssChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(key.selector(), SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key) {
        try {
            SocketChannel socketChannel = (SocketChannel) key.channel();
            buffer = ByteBuffer.allocate(1024);
            buffer.clear();
            if (socketChannel.read(buffer) > 0) {
                if (!acceptedUsers.contains(socketChannel)) {
                    user = new User(null, false);
                    while (!user.getNameStatus()) {
                        userName = new String(buffer.array()).trim();

                        System.out.println(userName);
                        if (!userNames.contains(userName)) {
                            user = new User(userName, true);
                            acceptedUsers.add(socketChannel);
                            userNames.add(userName);
                            System.out.println(String.format("<%s>[%s]:%s", getCurrentTime(), userName, Phrases.USER_JOINED.getPhrase()));
                            broadcast(userName + Phrases.USER_JOINED.getPhrase());
                        } else {
                            buffer = ByteBuffer.wrap(Phrases.SERVER_CONNECT_ERROR.getPhrase().getBytes());
                            socketChannel.write(buffer);
                            break;
                        }
                    }
                } else {
                    String clientMessages = new String(buffer.array()).trim();
                    if (!clientMessages.trim().toLowerCase().equals(Phrases.CLIENT_COMMAND_QUIT.getPhrase())) {
                        if (clientMessages.split(" ", 3)[0].equals(Phrases.CLIENT_COMMAND_FILE.getPhrase())) {
                            fileProcessing(clientMessages, socketChannel);
                        } else {
                            broadcast(userName + " " + clientMessages);
                            String message = String.format("<%s>[%s]: %s", getCurrentTime(), userName, clientMessages);

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
            System.out.println("ERROR " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void write(SelectionKey key) {
//        SocketChannel socketChannel = (SocketChannel) key.channel();
//        buffer.clear();
//        buffer = ByteBuffer.wrap(message.getBytes());
//        for (SelectionKey key : selector.keys()) {
//            Channel channel = key.channel();
//            if (channel instanceof SocketChannel) {
//                SocketChannel sc = (SocketChannel) channel;
//                //Write
//                System.out.println("write");
//                sc.write(buffer.flip());
//            }
////            socketChannel.write(buffer);
//        }
    }

    private void fileProcessing(String clientMessages, SocketChannel channel) throws IOException {
        String byteLength = clientMessages.split(" ", 3)[1];
        String fileName = clientMessages.split(" ", 3)[2];
        byte[] fileBytes = new byte[Integer.parseInt(byteLength)];
        System.out.println(String.format("<%s>[%s]: %s", getCurrentTime(), userName, Phrases.SEND_FILE.getPhrase() + fileName));
        buffer = ByteBuffer.allocate(5*1024*1024);
        buffer.clear();
        channel.read(buffer);
//        for (int i = 0; i < fileBytes.length; i++) {
//            byte aByte = input.readByte();
//            fileBytes[i] = aByte;
//        }
        broadcast(userName + " " + Phrases.SEND_FILE.getPhrase() + fileName + " ( " + fileBytes.length + " bytes)");
        broadcast(fileBytes);
    }

    private static String getCurrentTime() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        return dateTimeFormatter.format(now);
    }

    private void broadcast(String message) throws IOException {
        buffer = ByteBuffer.allocate(1024);
        buffer.clear();

        System.out.println(message);
        buffer = ByteBuffer.wrap(message.getBytes());
        for (SocketChannel channel : acceptedUsers) {
            channel.write(buffer);
        }
    }

    private void broadcast(byte[] bytes) throws IOException {
        buffer = ByteBuffer.allocate(1024);
        buffer.clear();
        System.out.println("file");
        buffer = ByteBuffer.wrap(bytes);
        for (SocketChannel channel : acceptedUsers) {
            channel.write(buffer);
        }
    }

    public void removeUser(String userName, SocketChannel socketChannel) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            acceptedUsers.remove(socketChannel);
        }
    }

    public ArrayList<String> getUserNames() {
        return userNames;
    }

    public void closeServerThread() {
//        try {
//            for (SocketChannel socketChannel : acceptedUsers) {
//                socketChannel.w(Phrases.SERVER_COMMAND_STOP_SERVER.getPhrase());
//                userThread.closeUserThread();
//            }
//            System.out.println(Phrases.SERVER_STOPPED.getPhrase());
//            userNames.clear();
//            userThreads.clear();
//            server.close();
//            System.exit(-1);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
