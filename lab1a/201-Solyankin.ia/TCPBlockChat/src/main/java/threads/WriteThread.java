package threads;

import client.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class WriteThread extends Thread {
    private DataOutputStream writer;
    private DataInputStream input;
    private User user;
    private Scanner scanner;

//    public WriteThread(User user) {
//        this.scanner = new Scanner(System.in, StandardCharsets.UTF_8);
//        this.user = user;
//        try {
//            writer = new DataOutputStream(user.getSocket().getOutputStream());
//            input = new DataInputStream(user.getSocket().getInputStream());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void run() {
//        enterName();
//
//        while (isAlive()) {
//            if (!user.getSocket().isClosed()) {
//                String text = scanner.nextLine();
//                if (text != null) { // Add sending file
//                    try {
//                        writer.writeUTF(text);
//                    } catch (IOException e) {
//                        System.out.println("Error while sending message: " + e.getMessage());
//                        e.printStackTrace();
//                    }
//                    if (text.toLowerCase().trim().equals("bye")) {
//                        closeWriteThread();
//                        System.exit(-1);
//                        break;
//                    }
//                }
//            } else {
//                closeWriteThread();
//                break;
//            }
//        }
//    }
//
//    private void enterName() {
//        System.out.println("Enter your name: ");
//        while (!user.getNameStatus()) {
//            String userName = scanner.nextLine();
//            try {
//                writer.writeUTF(userName);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private void closeWriteThread() {
//        try {
//            writer.close();
//            input.close();
//            user.getSocket().close();
//        } catch (IOException e) {
//            System.out.println("Error closing WriteThread: " + e.getMessage());
//            e.printStackTrace();
//        }
//        interrupt();
//    }
}

