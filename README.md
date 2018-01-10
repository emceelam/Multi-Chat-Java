# NAME

Multi Chat Java - A socket-based multi-client chat written with Java code

# SYNOPSIS

Open terminal

    javac MultiChat.java
    java MultiChat

From other terminal

    telnet 127.0.0.1 4022

From yet another terminal

    telnet 127.0.0.1 4022

Start typing text in your telnet sessions. Typing "quit" will cause the connection to terminate. Typing nothing for 10 seconds will cause connection to terminate.

# DESCRIPTION

This is a socket-based multi-client chat that echoes one user's chat
to other user's chat telnet sessions.

# FEATURES

* Multiple telnet chat sessions
    * select()
    * non-blocking read/write sockets
* Time out inactive sockets
    * 10 seconds for idle client to be disconnected.

# AUTHOR

Lambert Lum

![email address](http://sjsutech.com/small_email.png)

# COPYRIGHT AND LICENSE

This software is copyright (c) 2018 by Lambert Lum

This is free software; you can redistribute it and/or modify it under the same terms as the Perl 5 programming language system itself.
