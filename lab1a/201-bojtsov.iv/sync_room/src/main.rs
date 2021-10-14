use std::{net::SocketAddr, str::FromStr};

use structopt::StructOpt;
use tcp_chat::sync_room::*;

#[derive(Debug, StructOpt)]
#[structopt(name = "Room", about = "Simple TCP chat room.")]
struct Opt {
    /// Set the address of the server
    #[structopt(default_value = "127.0.0.1:6969")]
    address: String,
}

fn main() {
    let opt = Opt::from_args();
    let addr = SocketAddr::from_str(opt.address.as_str()).unwrap();
    let server = SyncRoom::new(addr).unwrap();
    server.run().unwrap();
}
