package threads;

import client.User;
import resources.Phrases;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ServerThread extends Thread {
    private final ServerSocketChannel server;
    private final Selector selector;
    private final Set<String> userNames;
    private final Set<SocketChannel> acceptedUsers;
    private ByteBuffer buffer;
    private String userName;
    private User user;

    public ServerThread(ServerSocketChannel server, Selector selector) {
        this.server = server;
        this.selector = selector;
        userNames = new HashSet<>();
        acceptedUsers = new HashSet<>();
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

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        System.out.println("Accept");
                        accept(key);
                    }

                    if (key.isReadable()) {
                        System.out.println("read");
                        read(key);
                    }

//                    if (key.isWritable()) {
//                        System.out.println("write");
//                        write(key);
//                    }
                }
            } catch (IOException e) {
                System.out.println(Phrases.SERVER_STOPPED.getPhrase());
            }
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
            buffer.clear();
            int count = socketChannel.read(buffer);
            System.out.println(count);
            if (count > 0) {
                if (!acceptedUsers.contains(socketChannel)) {
                    System.out.println("not contains");
                    user = new User(null, false);
                    while (!user.getNameStatus()) {
                        System.out.println("get name");
                        userName = new String(buffer.array()).trim();
                        System.out.println(userName);
                        if (!userNames.contains(userName)) {
                            System.out.println("not contains userName");
                            user = new User(userName, true);
                            acceptedUsers.add(socketChannel);
                            userNames.add(userName);
                            System.out.println(String.format("<%s>[%s]:%s", getCurrentTime(), userName, Phrases.USER_JOINED.getPhrase()));
                            broadcast(userName + Phrases.USER_JOINED.getPhrase(), socketChannel);
                        } else {
                            buffer.clear();
                            buffer = ByteBuffer.wrap(Phrases.SERVER_CONNECT_ERROR.getPhrase().getBytes());
                            buffer.flip();
                            socketChannel.write(buffer);
                        }
                    }
                } else {
                    System.out.println("contains");
                    String clientMessages = new String(buffer.array());
                    if (!clientMessages.trim().toLowerCase().equals(Phrases.CLIENT_COMMAND_QUIT.getPhrase())) {
                        if (clientMessages.split(" ", 3)[0].equals(Phrases.CLIENT_COMMAND_FILE.getPhrase())) {
                            fileProcessing(clientMessages);
                        } else {
                            broadcast(userName + " " + clientMessages);
                            String message = String.format("<%s>[%s]: %s", getCurrentTime(), userName, clientMessages);
                            System.out.println(message);
                        }
                    } else {
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

    private void fileProcessing(String clientMessages) throws IOException {
        String byteLength = clientMessages.split(" ", 3)[1];
        String fileName = clientMessages.split(" ", 3)[2];
        byte[] fileBytes = new byte[Integer.parseInt(byteLength)];
        System.out.println(String.format("<%s>[%s]: %s", getCurrentTime(), userName, Phrases.SEND_FILE.getPhrase() + fileName));
//        for (int i = 0; i < fileBytes.length; i++) {
//            byte aByte = input.readByte();
//            fileBytes[i] = aByte;
//        }
        broadcast(userName + " " + Phrases.SEND_FILE.getPhrase() + fileName + " ( " + fileBytes.length + " bytes)");
//        broadcastBytes(fileBytes);
    }

    private static String getCurrentTime() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();
        return dateTimeFormatter.format(now);
    }

    private void broadcast(String message) throws IOException {
        buffer.clear();
        System.out.println(message);
        buffer = ByteBuffer.wrap(message.getBytes());
        for (SelectionKey key : selector.keys()) {

//            System.out.println("write");
//            buffer.flip();
//            channel.write(buffer);


            Channel channel = key.channel();
            if (channel instanceof SocketChannel) {
                SocketChannel sc = (SocketChannel) channel;
                System.out.println("write");
                buffer.flip();
                sc.write(buffer);
            }
        }
    }

    private void broadcast(String message, SocketChannel socketChannel) throws IOException {
        buffer.clear().limit(message.length());
        System.out.println(message);
        buffer = ByteBuffer.wrap(message.getBytes());
        for (SelectionKey key : selector.keys()) {
            Channel channel = key.channel();
            if (channel instanceof SocketChannel) {
                SocketChannel sc = (SocketChannel) channel;
                System.out.println("write");
                buffer.flip();
                sc.write(buffer);
            }
        }
//        System.out.println("write");
//        buffer.flip();
//        socketChannel.write(buffer);
    }

    public void removeUser(String userName, SocketChannel socketChannel) {
        boolean removed = userNames.remove(userName);
        if (removed) {
            acceptedUsers.remove(socketChannel);
        }
    }

    public Set<String> getUserNames() {
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
