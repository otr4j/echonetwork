# README

A trivially simple set-up for a communication network to exchange messages. This is close to the simplest possible implementation for exchanging messages, such that various OTR implementation can be set up to communicate.

The "echo-network" is a "chat network" that is defined to provide a trivial infrastructure for cross-platform testing of OTR implementations. So far, interaction is manual. The clients are set up to either wait for stdin-input (stdin-client) or respond to incoming messages (echo-client). Echo-clients will respond to the OTR query-message by automatically initiating a session.

Note: this is very "low-tech". The primary goal is a low-complexity way to have different libraries interact.

## Supported libraries

- [otr4j/otr4j](<https://github.com/otr4j/otr4j>)
- [jitsi/otr4j](<https://github.com/jitsi/otr4j>)
- [cobratbq/otrr (in-dev, experiment)](<https://github.com/cobratbq/otrr>)

## Network protocol

The server/relay is responsible for passing on any message from sender to specified recipient. In the process, the server/relay replaces recipient address with sender (originator) address.

In the future, we may adapt the format (if needed) with more fields. The server/relay can be adapted with behaviors such as dropping/corrupting/bad-relaying of messages, shuffling/mixing, duplicating, and such to simulate various disadvantageous circumstances.

__Conceptual__

- "address": `<ip-address>:<port>` (user-typeable such that user can input on stdin)
  - NOTE: "address" is the recipient address for messages being sent, and sender address for messages being received. 
- "content": any series of bytes, usually UTF-8 encoded for plain messages typed in on the console.

__Message format__

Each message consists of two parts: an address and message-content. The raw format of the message is defined below. 

- `LENGTH_ADDRESS` (4-byte big-endian unsigned integer)  
  Indicates the length of the upcoming address.
- `ADDRESS` (`LENGTH_ADDRESS` bytes of address, encoded in UTF-8)  
  The address represents the destination address when sending to the echo-server. When the server forwards the content, the address-part is replaced with the origin address.
- `LENGTH_CONTENT` (4-byte big-endian unsigned integer)  
  Indicates the length of the upcoming message payload.
- `CONTENT` (`LENGTH_CONTENT` bytes of message, encoded in UTF-8)

## Session identifiers

Session identifiers are simply the local and remote address and port of the established connection.

- Local user session ID: `<local-address-of-connection>:<local-port-of-connection>`  
  For instance, an _otr4j-echoserver_ would use `192.168.0.1:8080`.
- Remote user session ID: `<remove-address-of-connection>:<remote-port-of-connection>`  
  For instance, an _otr4j-echoclient_ would use `192.168.0.1:31645`. (Typically an arbitrary port.)
- If a network name is needed, use `echo`. (Probably not, should double-check.)

_NOTE_ that we use the local address instead of the host name. This avoids issues due to multiple host names.

## TODO

- Design:
  - define a common format for issuing actions such as initiating/ending SMP via stdin (i.e. not every stdin input is a message)
- Server:
  - basic server-client signaling that can send notifications if, e.g. client disconnects.
- client-otr4j-jitsi: add logging to identify session state changes.
- server: if needed, future versions may include setting boundaries on message length, pretending lossy operations, shuffling/mixing messages etc.
- `FIXME etc.`
