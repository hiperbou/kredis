const net = require('net');

const client = new net.Socket();
const host = 'localhost';
const port = 9669;

client.connect(port, host, () => {
    console.log('Connected to server');
    console.log("Usage: STOP, GET key, SET key value, DEL key, INCR key, DECR key");
    process.stdin.on('data', (data) => {
        console.log("Sending: " + data.toString().trim());
        client.write(data.toString());
    });
});

client.on('data', (data) => {
    console.log('Received: ' + data);
});

client.on('close', () => {
    console.log('Connection closed');
});
