import socket
import pickle
s = socket.socket(socket.AF_INET,socket.SOCK_STREAM)

port = 12347
HEADERSIZE = 10

s.connect((socket.gethostname(), port))

while True:
    fullmsg = b''
    new_msg = True
    while True:
        msg = s.recv(8)
        if new_msg:
            print(f'new message length: {msg[:HEADERSIZE]}')
            msglen = int(msg[:HEADERSIZE].decode("utf-8"))
            new_msg = False
        fullmsg += msg
        print(f'full message length: {msglen}')
        print(len(fullmsg))
        if((len(fullmsg)-HEADERSIZE) == msglen):
            print("full msg recieved")
            print(fullmsg[HEADERSIZE:])

            d = pickle.loads(fullmsg[HEADERSIZE:])
            print(d)

            new_msg = True
            fullmsg = b''
print(fullmsg)
