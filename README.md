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

##  Netcat

```
nc [HOST] [PORT]
```

example

```
nc localhost 3000
```

## Kotlin Client:

```
java -cp kredis-1.0.0.jar main.SocketClient [HOST] [PORT]
```

examples

```
java -cp kredis-1.0.0.jar main.SocketClient 
java -cp kredis-1.0.0.jar main.SocketClient 127.0.0.1
java -cp kredis-1.0.0.jar main.SocketClient localhost 3000
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


## **Commands**
kredis keeps it simple with just a handful of commands. Here’s the full list:

###  **SET key value**
Stores a value under a key.

Keys must not contain spaces.

Example: 

    set hello world
    Response: OK

###  **GET key**
Retrieves the value stored under a key.

Example: 

    get hello
    Response: world

###  **DEL key**
Deletes a key and its value.

Example: 

    del hello
    Response: OK

###  **INCR key [value]**
Increments the numeric value of a key by the specified [value]. If the [value] is not provided, it defaults to 1. If the key doesn’t exist, it starts at 0.

Examples:

Increment by 1 (default):

    incr counter
    Response: 1

Increment by a specific value:

    incr score 10
    Response: 10

###  **DECR key [value]**
Decrements the numeric value of a key by the specified [value]. If the [value] is not provided, it defaults to 1. If the key doesn’t exist, it starts at 0.

Examples: 

Decrement by 1 (default):

    decr counter
    Response: -1

Decrement by a specific value:

    decr score 2
    Response: 8



###  **STOP**
Closes the session and disconnects from HippieDB. Use this when you’re done sharing the vibes.

Example: 

    stop
    Response: (connection closes)

---



Refactored from [this](https://github.com/pjambet/tcp-servers) based on this blog post 
https://blog.pjam.me/posts/toy-redis-go/
