#!/usr/bin/python

"""
Example network of Quagga routers
(QuaggaTopo + QuaggaService)
"""

import sys
import atexit

# patch isShellBuiltin
import mininet.util
import mininext.util
mininet.util.isShellBuiltin = mininext.util.isShellBuiltin
sys.modules['mininet.util'] = mininet.util

from mininet.util import dumpNodeConnections
from mininet.node import OVSController
from mininet.log import setLogLevel, info

from mininext.cli import CLI
from mininext.net import MiniNExT

from topo import QuaggaTopo

net = None
node_dict = {}

def startNetwork():
    "instantiates a topo, then starts the network and prints debug information"

    info('** Creating Quagga network topology\n')
    topo = QuaggaTopo()

    info('** Starting the network\n')
    global net, node_dict 
    net = MiniNExT(topo, controller=OVSController)

    net.start()

    info('** Dumping host connections\n')
    dumpNodeConnections(net.hosts)


    node_dict['H1'] = net.hosts[0]
    node_dict['R1'] = net.hosts[1]
    node_dict['R2'] = net.hosts[2]
    node_dict['R3'] = net.hosts[3]
    node_dict['R4'] = net.hosts[4]
    node_dict['R5'] = net.hosts[5]
    node_dict['R6'] = net.hosts[6]
    node_dict['R7'] = net.hosts[7]
    node_dict['R8'] = net.hosts[8]

    info('** Enabling ip forwarding on all hosts')
    for host in net.hosts:
        host.cmdPrint("echo 1 > /proc/sys/net/ipv4/ip_forward")

    setup_ip()
    add_static_routes()    
    setup_relaying()

    info('** Testing network connectivity\n')
    net.ping(net.hosts)

    info('** Running CLI\n')
    CLI(net)

def setup_ip():
    global node_dict
    info('** Setting up ip address\n')
    node_dict['R1'].cmdPrint('ip addr add 193.0.0.2/20 dev R1-eth1')
    node_dict['R1'].cmdPrint('ip addr add 200.0.0.2/20 dev R1-eth2')
    node_dict['R2'].cmdPrint('ip addr add 194.0.0.2/20 dev R2-eth1')
    node_dict['R3'].cmdPrint('ip addr add 195.0.0.2/20 dev R3-eth1')
    node_dict['R4'].cmdPrint('ip addr add 196.0.0.2/20 dev R4-eth1')
    node_dict['R5'].cmdPrint('ip addr add 197.0.0.2/20 dev R5-eth1')
    node_dict['R6'].cmdPrint('ip addr add 198.0.0.2/20 dev R6-eth1')
    node_dict['R7'].cmdPrint('ip addr add 199.0.0.2/20 dev R7-eth1')
    node_dict['R8'].cmdPrint('ip addr add 200.0.0.1/20 dev R8-eth1')


def add_static_routes():
    global node_dict
    info('** Adding static routes\n')
    node_dict['H1'].cmdPrint('ip route add 193.0.0.0/20 via 192.0.0.2 dev H1-eth0')
    node_dict['H1'].cmdPrint('ip route add 194.0.0.0/20 via 192.0.0.2 dev H1-eth0')
    node_dict['H1'].cmdPrint('ip route add 195.0.0.0/20 via 192.0.0.2 dev H1-eth0')
    node_dict['H1'].cmdPrint('ip route add 196.0.0.0/20 via 192.0.0.2 dev H1-eth0')
    node_dict['H1'].cmdPrint('ip route add 197.0.0.0/20 via 192.0.0.2 dev H1-eth0')
    node_dict['H1'].cmdPrint('ip route add 198.0.0.0/20 via 192.0.0.2 dev H1-eth0')
    node_dict['H1'].cmdPrint('ip route add 199.0.0.0/20 via 192.0.0.2 dev H1-eth0')
    node_dict['H1'].cmdPrint('ip route add 200.0.0.0/20 via 192.0.0.2 dev H1-eth0')
    
                              
    node_dict['R1'].cmdPrint('ip route add 194.0.0.0/20 via 193.0.0.1 dev R1-eth1')
    node_dict['R1'].cmdPrint('ip route add 195.0.0.0/20 via 193.0.0.1 dev R1-eth1')
    node_dict['R1'].cmdPrint('ip route add 196.0.0.0/20 via 193.0.0.1 dev R1-eth1')
    node_dict['R1'].cmdPrint('ip route add 197.0.0.0/20 via 200.0.0.1 dev R1-eth2')
    node_dict['R1'].cmdPrint('ip route add 198.0.0.0/20 via 200.0.0.1 dev R1-eth2')
    node_dict['R1'].cmdPrint('ip route add 199.0.0.0/20 via 200.0.0.1 dev R1-eth2')
                              
    node_dict['R2'].cmdPrint('ip route add 192.0.0.0/20 via 193.0.0.2 dev R2-eth0')
    node_dict['R2'].cmdPrint('ip route add 195.0.0.0/20 via 194.0.0.1 dev R2-eth1')
    node_dict['R2'].cmdPrint('ip route add 196.0.0.0/20 via 194.0.0.1 dev R2-eth1')
    node_dict['R2'].cmdPrint('ip route add 197.0.0.0/20 via 194.0.0.1 dev R2-eth1') 
    node_dict['R2'].cmdPrint('ip route add 198.0.0.0/20 via 193.0.0.2 dev R2-eth0')
    node_dict['R2'].cmdPrint('ip route add 199.0.0.0/20 via 193.0.0.2 dev R2-eth0')
    node_dict['R2'].cmdPrint('ip route add 200.0.0.0/20 via 193.0.0.2 dev R2-eth0')

    node_dict['R3'].cmdPrint('ip route add 192.0.0.0/20 via 194.0.0.2 dev R3-eth0')                              
    node_dict['R3'].cmdPrint('ip route add 193.0.0.0/20 via 194.0.0.2 dev R3-eth0')                              
    node_dict['R3'].cmdPrint('ip route add 196.0.0.0/20 via 195.0.0.1 dev R3-eth1')
    node_dict['R3'].cmdPrint('ip route add 197.0.0.0/20 via 195.0.0.1 dev R3-eth1')
    node_dict['R3'].cmdPrint('ip route add 198.0.0.0/20 via 195.0.0.1 dev R3-eth1')
    node_dict['R3'].cmdPrint('ip route add 199.0.0.0/20 via 194.0.0.2 dev R3-eth0')
    node_dict['R3'].cmdPrint('ip route add 200.0.0.0/20 via 194.0.0.2 dev R3-eth0')

                              
    node_dict['R4'].cmdPrint('ip route add 192.0.0.0/20 via 195.0.0.2 dev R4-eth0')
    node_dict['R4'].cmdPrint('ip route add 193.0.0.0/20 via 195.0.0.2 dev R4-eth0')
    node_dict['R4'].cmdPrint('ip route add 194.0.0.0/20 via 195.0.0.2 dev R4-eth0')
    node_dict['R4'].cmdPrint('ip route add 197.0.0.0/20 via 196.0.0.1 dev R4-eth1')
    node_dict['R4'].cmdPrint('ip route add 198.0.0.0/20 via 196.0.0.1 dev R4-eth1')
    node_dict['R4'].cmdPrint('ip route add 199.0.0.0/20 via 196.0.0.1 dev R4-eth1')
    node_dict['R4'].cmdPrint('ip route add 200.0.0.0/20 via 195.0.0.2 dev R4-eth0')                              
   

                              
    node_dict['R5'].cmdPrint('ip route add 192.0.0.0/20 via 196.0.0.2 dev R5-eth0')
    node_dict['R5'].cmdPrint('ip route add 193.0.0.0/20 via 196.0.0.2 dev R5-eth0')
    node_dict['R5'].cmdPrint('ip route add 194.0.0.0/20 via 196.0.0.2 dev R5-eth0')
    node_dict['R5'].cmdPrint('ip route add 195.0.0.0/20 via 196.0.0.2 dev R5-eth0')
    node_dict['R5'].cmdPrint('ip route add 198.0.0.0/20 via 197.0.0.1 dev R5-eth1')
    node_dict['R5'].cmdPrint('ip route add 199.0.0.0/20 via 197.0.0.1 dev R5-eth1')
    node_dict['R5'].cmdPrint('ip route add 200.0.0.0/20 via 197.0.0.1 dev R5-eth1')                              


    node_dict['R6'].cmdPrint('ip route add 192.0.0.0/20 via 198.0.0.1 dev R6-eth1')
    node_dict['R6'].cmdPrint('ip route add 193.0.0.0/20 via 198.0.0.1 dev R6-eth1')
    node_dict['R6'].cmdPrint('ip route add 194.0.0.0/20 via 197.0.0.2 dev R6-eth0')
    node_dict['R6'].cmdPrint('ip route add 195.0.0.0/20 via 197.0.0.2 dev R6-eth0')
    node_dict['R6'].cmdPrint('ip route add 196.0.0.0/20 via 197.0.0.2 dev R6-eth0')
    node_dict['R6'].cmdPrint('ip route add 199.0.0.0/20 via 198.0.0.1 dev R6-eth1')
    node_dict['R6'].cmdPrint('ip route add 200.0.0.0/20 via 198.0.0.1 dev R6-eth1')                              

    node_dict['R7'].cmdPrint('ip route add 192.0.0.0/20 via 199.0.0.1 dev R7-eth1')
    node_dict['R7'].cmdPrint('ip route add 193.0.0.0/20 via 199.0.0.1 dev R7-eth1')
    node_dict['R7'].cmdPrint('ip route add 194.0.0.0/20 via 199.0.0.1 dev R7-eth1')
    node_dict['R7'].cmdPrint('ip route add 195.0.0.0/20 via 198.0.0.2 dev R7-eth0')
    node_dict['R7'].cmdPrint('ip route add 196.0.0.0/20 via 198.0.0.2 dev R7-eth0')
    node_dict['R7'].cmdPrint('ip route add 197.0.0.0/20 via 198.0.0.2 dev R7-eth0')
    node_dict['R7'].cmdPrint('ip route add 200.0.0.0/20 via 199.0.0.1 dev R7-eth1')                              

    node_dict['R8'].cmdPrint('ip route add 192.0.0.0/20 via 200.0.0.2 dev R8-eth1')
    node_dict['R8'].cmdPrint('ip route add 193.0.0.0/20 via 200.0.0.2 dev R8-eth1')
    node_dict['R8'].cmdPrint('ip route add 194.0.0.0/20 via 200.0.0.2 dev R8-eth1')
    node_dict['R8'].cmdPrint('ip route add 195.0.0.0/20 via 200.0.0.2 dev R8-eth1')
    node_dict['R8'].cmdPrint('ip route add 196.0.0.0/20 via 199.0.0.2 dev R8-eth0')
    node_dict['R8'].cmdPrint('ip route add 197.0.0.0/20 via 199.0.0.2 dev R8-eth0')
    node_dict['R8'].cmdPrint('ip route add 198.0.0.0/20 via 199.0.0.2 dev R8-eth0')                              

	
def setup_relaying():
    global node_dict
    node_dict['R1'].cmdPrint('iptables -t nat -A POSTROUTING -o R1-eth1 -j MASQUERADE')
    node_dict['R1'].cmdPrint('iptables -t nat -A POSTROUTING -o R1-eth2 -j MASQUERADE')

    node_dict['R2'].cmdPrint('iptables -t nat -A POSTROUTING -o R2-eth1 -j MASQUERADE')
    node_dict['R2'].cmdPrint('iptables -t nat -A POSTROUTING -o R2-eth0 -j MASQUERADE')

    node_dict['R3'].cmdPrint('iptables -t nat -A POSTROUTING -o R3-eth1 -j MASQUERADE')
    node_dict['R3'].cmdPrint('iptables -t nat -A POSTROUTING -o R3-eth0 -j MASQUERADE')

    node_dict['R4'].cmdPrint('iptables -t nat -A POSTROUTING -o R4-eth0 -j MASQUERADE')
    node_dict['R4'].cmdPrint('iptables -t nat -A POSTROUTING -o R4-eth1 -j MASQUERADE')
    
    node_dict['R5'].cmdPrint('iptables -t nat -A POSTROUTING -o R5-eth1 -j MASQUERADE')
    node_dict['R5'].cmdPrint('iptables -t nat -A POSTROUTING -o R5-eth0 -j MASQUERADE')

    node_dict['R6'].cmdPrint('iptables -t nat -A POSTROUTING -o R6-eth1 -j MASQUERADE')
    node_dict['R6'].cmdPrint('iptables -t nat -A POSTROUTING -o R6-eth0 -j MASQUERADE')

    node_dict['R7'].cmdPrint('iptables -t nat -A POSTROUTING -o R7-eth0 -j MASQUERADE')
    node_dict['R7'].cmdPrint('iptables -t nat -A POSTROUTING -o R7-eth1 -j MASQUERADE')
    
    node_dict['R8'].cmdPrint('iptables -t nat -A POSTROUTING -o R8-eth0 -j MASQUERADE')
    node_dict['R8'].cmdPrint('iptables -t nat -A POSTROUTING -o R8-eth1 -j MASQUERADE')

                    
def stopNetwork():
    "stops a network (only called on a forced cleanup)"

    if net is not None:
        info('** Tearing down Quagga network\n')
        net.stop()

if __name__ == '__main__':
    # Force cleanup on exit by registering a cleanup function
    atexit.register(stopNetwork)

    # Tell mininet to print useful information
    setLogLevel('info')
    startNetwork()
