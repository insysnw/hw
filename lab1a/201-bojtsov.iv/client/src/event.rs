use std::io;
use std::path::PathBuf;
use std::sync::{mpsc, Arc};
use std::thread;

use tcp_chat::room_common::*;
use termion::event::Key;
use termion::input::TermRead;

use tcp_chat::room_client::*;

// #[allow(dead_code)]

pub enum Event {
    Input(Key),
    Recv {
        desc: Descriptor,
        header: ServerHeader,
        data: Vec<u8>,
    },
    Tick,
}

pub struct Events {
    rx: mpsc::Receiver<Event>,
    client: Arc<Client>,
}

impl Events {
    pub fn new(client: Client) -> Events {
        let client = Arc::new(client);
        let recv_client = Arc::clone(&client);

        let (tx, rx) = mpsc::channel();
        {
            let tx = tx.clone();
            thread::spawn(move || {
                let stdin = io::stdin();
                for evt in stdin.keys() {
                    if let Ok(key) = evt {
                        if let Err(err) = tx.send(Event::Input(key)) {
                            eprintln!("{}", err);
                            return;
                        }
                    }
                }
            })
        };

        {
            let tx = tx.clone();
            thread::spawn(move || loop {
                thread::sleep(std::time::Duration::from_millis(100));
                if let Err(err) = tx.send(Event::Tick) {
                    eprintln!("{}", err);
                    return;
                }
            })
        };

        {
            let tx = tx.clone();
            thread::spawn(move || {
                while let Ok((desc, header, data)) = recv_client.receive() {
                    if let Err(err) = tx.send(Event::Recv { desc, header, data }) {
                        eprintln!("{}", err);
                        return;
                    }
                }
            })
        };

        Events { rx, client }
    }

    pub fn next(&self) -> Result<Event, mpsc::RecvError> {
        self.rx.recv()
    }

    pub fn send(&mut self, to: Option<String>, message: String) {
        let _ = self.client.send_text(to, message);
    }

    pub fn send_file(&mut self, to: Option<String>, file: PathBuf) {
        let _ = self.client.send_file(to, file);
    }
}
