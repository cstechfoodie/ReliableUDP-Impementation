# COMP445 HttpClient/Server/ReliableUDP-Implementation
A simple implementation for reliable UDP socket for ConcordiaU COMP 445 Assignment3 specification

## Getting Started

Import client, server, and reliableUDP project as maven project due to the previous depending on the reliable udp socket. (other build config is okay)
### Attention

This project is a good reference for your assignment one, two and three in COMP445 Computer Networking and Data Communications at **Concordia University, Montreal**. This implementation is not perfect in terms of code complexity and performace, but it is good enough to give you a 10/10 mark. But before you decide to refer to its idea, please understand the following issues:

1. This targets for assignment3, which requires a selective repeat reliable udp.
2. client and server code are the same as assignment1 and 2, the only difference is we need to use our reliableUDP to send and receive data instead of using the originals.
3. There is a router that is responsible for mimic an environment of droping packets and delaying packets. Be sure to take that code from course website. 
4. Even though I try to make the code simple and adhere to OO principle. But I don't think it is good enough. Many aspects could be improved, such code modularity and usage of threads.

## Authors

* **Shunyu Wang** - *Initial work* - Initial work for comp445 newtworking class, assignment 3

