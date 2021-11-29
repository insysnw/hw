### Protocol
The protocols are almost the same as in <a href="https://github.com/Keita18/hw/tree/lab1a/lab1a/202-keita.as" style="font-style: italic">
    lab1a
</a>, so let's see what new here:

#### select:
for this, we will use *select*, 
The select module gives us OS-level monitoring operations 
for things, including for sockets. 
It is especially useful in cases where we're attempting 
to monitor many connections simultaneously.

Next, we can set the following to overcome the "Address already in use" so we can reuse the address:
```python
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
```

Next from client side we set blocking to false

```python
client_socket.setblocking(False)
```
### Conclusion :
the difference between blocking and non-blocking is just that the former uses a for loop to simply iterate over all sockets. or select module witch is going to be far more efficient and will scale much better, and the second use threading to handle differents clients

