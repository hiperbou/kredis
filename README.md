# kredis
Toy Redis in Kotlin in 120 lines

## Start server:

```
java -jar kredis-1.0.0.jar [PORT]
``` 

examples:

```
java -jar kredis-1.0.0.jar
java -jar kredis-1.0.0.jar 3000 
```

## Kotlin Client:

```
java -cp .\kredis-1.0.0.jar main.SocketClient [HOST] [PORT]
```

examples

```
java -cp .\kredis-1.0.0.jar main.SocketClient 
java -cp .\kredis-1.0.0.jar main.SocketClient 127.0.0.1
java -cp .\kredis-1.0.0.jar main.SocketClient localhost 3000
```

## NodeJs Client:

```
node client.js [HOST] [PORT]
```

examples

```
node client.js
node client.js 127.0.0.1
node client.js localhost 3000
```





Refactored from [this](https://github.com/pjambet/tcp-servers) based on this blog post 
https://blog.pjam.me/posts/toy-redis-go/
