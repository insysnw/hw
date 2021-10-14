mod event;

use chrono::Utc;
use event::*;
use regex::Regex;
use std::path::PathBuf;
use std::{error::Error, io};
use std::{net::SocketAddr, str::FromStr};
use structopt::StructOpt;
use tcp_chat::room_common::MessageType;
use tcp_chat::{room_client::Client, room_common::Username};
use termion::{event::Key, input::MouseTerminal, raw::IntoRawMode, screen::AlternateScreen};
use tui::{
    backend::TermionBackend,
    layout::{Alignment, Constraint, Direction, Layout},
    style::{Color, Modifier, Style},
    text::{Span, Spans},
    widgets::{Block, Borders, Paragraph, Wrap},
    Terminal,
};

#[derive(Debug, StructOpt)]
#[structopt(name = "Room", about = "Simple TCP chat room.")]
struct Opt {
    /// Set address of the server
    #[structopt(short, long, default_value = "127.0.0.1:6969")]
    address: String,

    /// Username can contain only english characters, numbers and underscores and must be encoded with UTF-8 and less or equal to 32 characters
    #[structopt(short, long)]
    username: String,

    #[structopt(short, long, default_value = ".")]
    save_directory: PathBuf,
}

fn main() -> Result<(), Box<dyn Error>> {
    let Opt {
        address,
        username,
        save_directory,
    } = Opt::from_args();
    let addr = SocketAddr::from_str(address.as_str()).unwrap();
    let title_text = format!("{} as {}", address, username);
    let uname = Username::from_string(username.clone()).expect("Invalid username");
    let client = Client::new(uname, addr, save_directory.clone()).unwrap();

    // Terminal initialization
    let stdout = io::stdout().into_raw_mode()?;
    let stdout = MouseTerminal::from(stdout);
    let stdout = AlternateScreen::from(stdout);
    let backend = TermionBackend::new(stdout);
    let mut terminal = Terminal::new(backend)?;

    let mut events = Events::new(client);
    let mut messages = vec![];
    let mut curr_text = String::new();

    let mut offset = 0u16;

    loop {
        let p_m = messages.iter().cloned().collect::<Vec<_>>();
        terminal.draw(|f| {
            let chunks = Layout::default()
                .direction(Direction::Vertical)
                .constraints(
                    [
                        Constraint::Percentage(10),
                        Constraint::Percentage(80),
                        Constraint::Percentage(10),
                    ]
                    .as_ref(),
                )
                .split(f.size());

            let title_area = Paragraph::new(Span::raw(&title_text))
                .block(Block::default().title("Chat Room").borders(Borders::ALL))
                .alignment(Alignment::Center)
                .wrap(Wrap { trim: true });
            let messages_area = Paragraph::new(p_m)
                .block(Block::default().title("Paragraph").borders(Borders::ALL))
                .alignment(Alignment::Left)
                .scroll((offset, 0))
                .wrap(Wrap { trim: true });
            let type_area = Paragraph::new(Span::raw(curr_text.as_str()))
                .block(
                    Block::default()
                        .title("Type your message here")
                        .borders(Borders::ALL),
                )
                .alignment(Alignment::Left)
                .wrap(Wrap { trim: true });
            f.render_widget(title_area, chunks[0]);
            f.render_widget(messages_area, chunks[1]);
            f.render_widget(type_area, chunks[2]);
        })?;

        match events.next()? {
            Event::Input(Key::Char('\n')) => {
                lazy_static::lazy_static! {
                    static ref RE: Regex = Regex::new(r"((/whisper) (?P<uname>[a-zA-Z0-9_]+) )?((/file (?P<file>((?:[a-zA-Z]|\\)(\\[\w\- \.:]+\.(\w+))|((/[\w\- \.:]+)+)))$)|(?P<msg>.*))").unwrap();
                }
                let (to, file, message) = {
                    let c = RE.captures(&curr_text).unwrap();
                    (
                        c.name("uname").map(|m| m.as_str().to_string()),
                        c.name("file").map(|m| PathBuf::from(m.as_str())),
                        c.name("msg").map(|m| m.as_str().to_string()),
                    )
                };

                let msg = if let Some(ref to) = to {
                    let time = Utc::now()
                        .naive_local()
                        .time()
                        .format("%H:%M:%S")
                        .to_string();
                    let (s1, s2) = if let Some(ref file) = file {
                        (
                            format!("[{} -> {}] send file ", username, to),
                            Span::styled(
                                file.to_string_lossy().to_string(),
                                Style::default().add_modifier(Modifier::ITALIC),
                            ),
                        )
                    } else {
                        (
                            format!("[{} -> {}]: ", username, to),
                            Span::raw(message.as_ref().unwrap().to_string()),
                        )
                    };
                    Some(Spans::from(vec![
                        Span::styled(
                            format!("<{}> ", time),
                            Style::default().add_modifier(Modifier::BOLD),
                        ),
                        Span::styled(s1, Style::default().fg(Color::Green)),
                        s2,
                    ]))
                } else {
                    None
                };

                if let Some(file) = file {
                    events.send_file(to, file);
                } else {
                    events.send(to, message.unwrap());
                }
                curr_text.clear();
                if let Some(msg) = msg {
                    messages.push(msg);
                }
            }
            Event::Input(Key::Backspace) => {
                curr_text.pop();
            }
            Event::Input(Key::Char(ch)) => {
                curr_text.push(ch);
            }
            Event::Input(Key::Down) => {
                offset += 1;
            }
            Event::Input(Key::Up) => {
                offset = offset.saturating_sub(1);
            }
            Event::Input(Key::Esc) => {
                break;
            }
            Event::Recv { desc, header, data } => {
                let time = header
                    .time
                    .naive_local()
                    .time()
                    .format("%H:%M:%S")
                    .to_string();
                let user: String = header.from.into();
                let user_color = if user == username {
                    Color::Yellow
                } else if header.private {
                    Color::Red
                } else {
                    Color::Blue
                };
                match desc.msg_type {
                    MessageType::File => {
                        let file = save_directory
                            .join(header.filename.unwrap())
                            .to_str()
                            .unwrap()
                            .to_string();
                        messages.push(Spans::from(vec![
                            Span::styled(
                                format!("<{}> ", time),
                                Style::default().add_modifier(Modifier::BOLD),
                            ),
                            Span::styled(
                                format!("[{}] send file: ", user),
                                Style::default().fg(user_color),
                            ),
                            Span::styled(file, Style::default().add_modifier(Modifier::ITALIC)),
                        ]));
                    }
                    MessageType::Utf8 => {
                        messages.push(Spans::from(vec![
                            Span::styled(
                                format!("<{}> ", time),
                                Style::default().add_modifier(Modifier::BOLD),
                            ),
                            Span::styled(format!("[{}]: ", user), Style::default().fg(user_color)),
                            Span::raw(String::from_utf8(data).unwrap()),
                        ]));
                    }
                    MessageType::Login => {
                        messages.push(Spans::from(vec![
                            Span::styled(
                                format!("<{}> ", time),
                                Style::default().add_modifier(Modifier::BOLD),
                            ),
                            Span::raw("Welcome our new user! "),
                            Span::styled(user, Style::default().fg(Color::Red)),
                        ]));
                    }
                    MessageType::Logout => {
                        messages.push(Spans::from(vec![
                            Span::styled(
                                format!("<{}> ", time),
                                Style::default().add_modifier(Modifier::BOLD),
                            ),
                            Span::styled(user, Style::default().fg(Color::Red)),
                            Span::raw(" left the chat."),
                        ]));
                    }
                    _ => continue,
                }
            }
            _ => {}
        }
    }

    Ok(())
}
