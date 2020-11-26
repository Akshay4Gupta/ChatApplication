
import socket
import time
import pickle

s = socket.socket(socket.AF_INET,socket.SOCK_STREAM)
print ("Socket successfully created")

HEADERSIZE = 10
port = 12347

s.bind((socket.gethostname(), port))
print ("socket binded to %s" %(port))

s.listen(5)
print ("socket is listening")

while True:
   c, addr = s.accept()
   print (f'Got connection from {addr}')

   d = {1: "hey", 2:"there", 3:"I", 4:"am", 5:"akshay"}
   msg = pickle.dumps(d)

   msg = bytes(f'{len(msg):<{HEADERSIZE}}',"utf-8")+msg

   c.send(msg)

   # while True:
   #     time.sleep(3)
   #     msg = f"this time is! {time.time()}"
   #     msg = f'{len(msg):<{HEADERSIZE}}'+msg
   #     c.send(bytes(msg,'utf-8'))
