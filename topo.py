"""
Example topology of Quagga routers
"""

import inspect
import os
from mininext.topo import Topo
from mininext.services.quagga import QuaggaService

from collections import namedtuple
from subprocess import call
QuaggaHost = namedtuple("QuaggaHost", "name ip loIP")
net = None


class QuaggaTopo(Topo):

    "Creates a topology of Quagga routers"

    def __init__(self):
        """Initialize a Quagga topology with 5 routers, configure their IP
           addresses, loop back interfaces, and paths to their private
           configuration directories."""
        Topo.__init__(self)

        # Directory where this file / script is located"
        selfPath = os.path.dirname(os.path.abspath(
            inspect.getfile(inspect.currentframe())))  # script directory

        # Initialize a service helper for Quagga with default options
        quaggaSvc = QuaggaService(autoStop=False)

        # Path configurations for mounts
        quaggaBaseConfigPath = selfPath + '/configs/'

	h1 = QuaggaHost(name='H1', ip='192.0.0.1/20',
                                      loIP='10.0.1.1/20')
	r1 = QuaggaHost(name='R1', ip='192.0.0.2/20',
                                      loIP='10.0.2.1/20')
	
	r2 = QuaggaHost(name='R2', ip='193.0.0.1/20',
                                      loIP='10.0.3.1/20')

	
        # List of Quagga host configs
        quaggaHosts = []	
        quaggaHosts.append(h1)
        quaggaHosts.append(r1)
        quaggaHosts.append(r2)
        quaggaHosts.append(QuaggaHost(name='R3', ip='194.0.0.1/20',
                                      loIP='10.0.3.1/20'))
        quaggaHosts.append(QuaggaHost(name='R4', ip='195.0.0.1/20',
                                      loIP='10.0.4.1/20'))
        quaggaHosts.append(QuaggaHost(name='R5', ip='196.0.0.1/20',
                                      loIP='10.0.3.1/20'))
        quaggaHosts.append(QuaggaHost(name='R6', ip='197.0.0.1/20',
                                      loIP='10.0.4.1/20'))
	quaggaHosts.append(QuaggaHost(name='R7', ip='198.0.0.1/20',
                                      loIP='10.0.3.1/20'))
        quaggaHosts.append(QuaggaHost(name='R8', ip='199.0.0.1/20',
                                      loIP='10.0.4.1/20'))
	qcontainerList = []	

        # Setup each Quagga router, add a link between it and the IXP fabric
        for host in quaggaHosts:

            # Create an instance of a host, called a quaggaContainer
            qcontainerList.append(self.addHost(name=host.name,
                                           ip=host.ip,
                                           hostname=host.name,
                                           privateLogDir=True,
                                           privateRunDir=True,
                                           inMountNamespace=True,
                                           inPIDNamespace=True,
                                           inUTSNamespace=True))
            
            # Configure and setup the Quagga service for this node
            quaggaSvcConfig = \
                {'quaggaConfigPath': quaggaBaseConfigPath + host.name}
            self.addNodeService(node=host.name, service=quaggaSvc,
                                nodeConfig=quaggaSvcConfig)

	# Attach the quaggaContainer to the IXP Fabric Switch
	self.addLink(qcontainerList[0], qcontainerList[1])
	self.addLink(qcontainerList[1], qcontainerList[2])
	self.addLink(qcontainerList[2], qcontainerList[3])	
	self.addLink(qcontainerList[3], qcontainerList[4])
	self.addLink(qcontainerList[4], qcontainerList[5])
	self.addLink(qcontainerList[5], qcontainerList[6])
	self.addLink(qcontainerList[6], qcontainerList[7])	
	self.addLink(qcontainerList[7], qcontainerList[8])
	self.addLink(qcontainerList[8], qcontainerList[1])
	
