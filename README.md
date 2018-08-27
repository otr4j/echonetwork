The otr4j "Echo network" is a "chat network" that is defined to provide a trivial infrastructure for cross-platform testing of OTR implementations.

# Network protocol

__Message format__

* LENGTH (4-byte big-endian unsigned integer)  
  Indicates the length of the upcoming message payload.
* PAYLOAD (<LENGTH> bytes of message, encoded in UTF-8)

# Session identifiers

Session identifiers are simply the local and remote address and port of the established connection.

* Local user session ID: `<local-address-of-connection>:<local-port-of-connection>`  
  For instance, an _otr4j-echoserver_ would use `localhost:8080`.
* Remote user session ID: `<remove-address-of-connection>:<remote-port-of-connection>`  
  For instance, an _otr4j-echoclient_ would use `localhost:31645`. (Typically an arbitrary port.)
* If a network name is needed, use `echo`. (Probably not, should double-check.)

_NOTE_ that we use the local address instead of the host name. This avoids the possibility for ambiguity.
